package com.github.framework.executor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.framework.context.RunTimeContext;
import com.github.framework.device.AbstractTestDevice;
import com.github.framework.device.DeviceAllocationManager;
import com.github.framework.utils.ChooseDeviceDialog;
import com.github.framework.utils.FigletWriter;

public class TestExecutor 
{
	/**
	 * Build ParallelExecutor base on environment variables
	 * @return ParallelExecutor
	 * @throws IOException
	 */
	public static TestExecutor build()
	{
		TestExecutor executor = null;
		
		if("ON".equals(RunTimeContext.getInstance().getProperty("DEBUG_MODE")))
		{
			ChooseDeviceDialog chooseDevice = new ChooseDeviceDialog();
			chooseDevice.setLocationRelativeTo(null);
			chooseDevice.setVisible(true);
			AbstractTestDevice device = chooseDevice.waitForSelectedDevice();
			chooseDevice.setVisible(false);
			chooseDevice.dispose();
			
			executor = new TestExecutor(device);
		}
		else
		{
			String[] testDevices = RunTimeContext.getInstance().getProperty("TEST_DEVICES", "").split(",");
			
			// If test devices have not been specified
			if(testDevices.length == 1 && testDevices[0].isEmpty())
	        {
	        	executor = new TestExecutor();
	        }
			// If test devices are specified
	        else
	        {
	        	List<String> deviceList = new ArrayList<>();
	        	for(String device : testDevices)
	        	{
	        		Pattern regex = Pattern.compile("\\[(.+?)\\]");
	        		
	        		Matcher matcher = regex.matcher(device);

	    			if (matcher.find()) 
	    			{
	    				deviceList.add(matcher.group(1).trim());
	    			}
	        	}
	        	
	        	executor = new TestExecutor(deviceList);
	        }
		}
		
		return executor;
	}
	
	/**
	 * ParallelExecutor for no specified test devices, all connected devices will be assigned for execution
	 * @throws IOException
	 */
	private TestExecutor()
	{
		DeviceAllocationManager.getInstance().initializeDevices();
	}

	/**
	 * ParallelExecutor for specific test devices, only specified devices will be assigned for execution
	 * @param validDeviceIds
	 * @throws IOException
	 */
	private TestExecutor(List<String> validDeviceIds)
	{
		DeviceAllocationManager.getInstance().setValidDevices(validDeviceIds);
		DeviceAllocationManager.getInstance().initializeDevices();
	}

	/**
	 * ParallelExecutor for specific test devices, only specified devices will be assigned for execution
	 * @param device
	 * @throws IOException
	 */
	private TestExecutor(AbstractTestDevice device) 
	{
		DeviceAllocationManager.getInstance().setDevices(Arrays.asList(device));
		DeviceAllocationManager.getInstance().initializeDevices();
	}
	
	/**
	 * @param packages Package list, separate by comma, e.g. com.test.package1, com.test.package2
	 * @param tests test classes
	 * @return
	 * @throws Exception
	 */
	public boolean runTests(String packages, List<String> tests) throws Exception 
	{
		FigletWriter.print(RunTimeContext.getInstance().getProperty("RUNNER"));
		return startTestExecution(packages, tests);
	}

	/**
	 * @param packages Package list, separate by comma, e.g. com.test.package1, com.test.package2
	 * @return
	 * @throws Exception
	 */
	public boolean runTests(String packages) throws Exception 
	{
		FigletWriter.print(RunTimeContext.getInstance().getProperty("RUNNER"));
		return runTests(packages, new ArrayList<String>());
	}

	/**
	 * @param packages Package list, separate by comma, e.g. com.test.package1, com.test.package2
	 * @param tests test classes
	 * @return
	 * @throws Exception
	 */
	private boolean startTestExecution(String packages, List<String> tests) throws Exception 
	{
		RunTimeContext.getInstance().setPackages(packages);
		// Check available devices
		int deviceCount = DeviceAllocationManager.getInstance().getDeviceUDIDs().size();
		if(deviceCount == 0)
		{
			FigletWriter.print("No Devices Connected");
			throw new IllegalArgumentException("No connected " + System.getenv("Platform") + " devices.");
		}

		// Check execution permission for IOS
		if(System.getenv("Platform").equalsIgnoreCase("ios"))
		{
			DeviceAllocationManager.getInstance().getIOSDeviceManager().checkExecutePermissionForIOSDebugProxyLauncher();
		}

		System.out.println("***************************************************");
		System.out.println("Total Number of devices detected:: " + deviceCount);
		System.out.println("***************************************************");
		System.out.println("starting running tests in threads");

		boolean hasFailures = false;
		if (RunTimeContext.getInstance().getProperty("FRAMEWORK").equalsIgnoreCase("testng")) 
		{
			TestNGExecutor testNGExecutor = new TestNGExecutor();
			
			// For parallel test
			if (RunTimeContext.getInstance().isParallelExecution()) 
			{
				hasFailures = testNGExecutor.startTestExecution(tests, packages, ExecutionType.PARALLEL);
			}
			// For distribute test
			else
			{
				hasFailures = testNGExecutor.startTestExecution(tests, packages, ExecutionType.DISTRIBUTE);
			}
		}

		while(!RunTimeContext.getInstance().isAllTaskDone())
		{
			Thread.sleep(1000);
		}
		
		return hasFailures;
	}
}
