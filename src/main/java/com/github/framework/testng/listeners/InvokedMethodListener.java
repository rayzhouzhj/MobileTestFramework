package com.github.framework.testng.listeners;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.SkipException;

import com.github.framework.context.TestingDevice;
import com.github.framework.device.DeviceAllocationManager;
import com.github.framework.report.ReportManager;
import com.github.framework.testng.model.TestInfo;

public final class InvokedMethodListener implements IInvokedMethodListener
{
	public InvokedMethodListener() 
	{
	}

	private void setupDeviceForTest(TestInfo testInfo, ITestResult testResult)
	{
		// Allocate device to test
		String device = testResult.getTestClass().getXmlClass().getAllParameters().get("device").toString();
		DeviceAllocationManager.getInstance().allocateDeviceAndTask(device, testInfo);

		// Create test node for test class in test report
		try 
		{
			// Create test node at test class level
			ReportManager.getInstance().setupReportForTestSet(testInfo);
			ReportManager.getInstance().setTestResult(testResult);
			// Start reporting
			ReportManager.getInstance().startLogging(testInfo);
		}
		catch (Exception e) 
		{
			e.printStackTrace();

			// Throw skip exception to skip test execution if error with appium setup
			throw new RuntimeException("Exception with logging set up, skip test execution.");
		}
		
		try 
		{
			// Setup Appium
			TestingDevice.get().setupAppiumForTest();
		}
		catch (Exception e) 
		{
			e.printStackTrace();

			// Throw skip exception to skip test execution if error with appium setup
			throw new RuntimeException("Exception with appium start up, skip test execution.");
		}
	}

	/**
	 * Before each method invocation
	 * Initialize Appium Driver and Report Manager
	 */
	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) 
	{
		TestInfo testInfo = new TestInfo(method);

		// Skip beforeInvocation if current method is not with Annotation Test
		if(!testInfo.isTestMethod())
		{
			return;
		}
		// If current test need to be skipped, e.g. Test only available in Android/IOS
		else if(testInfo.isSkippedTest())
		{
			throw new SkipException("Skipped because property was set to : " + testInfo.getSkipPlatform());
		}
		else
		{
			// Do Nothing
		}

		
		System.out.println("[BeforeInvocation] Start running test [" + testInfo.getMethodName() + "]");
		// Start setup Appium and Device
		setupDeviceForTest(testInfo, testResult);
	}

	/**
	 * After each method invocation
	 * Update test result to report manager and stop Appium Driver
	 */
	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) 
	{
		TestInfo testInfo = new TestInfo(method);

		// Skip afterInvocation if current method is not with Annotation Test
		if(!testInfo.isTestMethod())
		{
			return;
		}
		// If current test need to be skipped, e.g. Test only available in Android/IOS
		else if(testInfo.isSkippedTest())
		{
			return;
		}
		else
		{
			// Do Nothing
		}

		
		System.out.println("[AfterInvocation] Completed running test [" + testInfo.getMethodName() + "]");
		
		try 
		{
			// Stop logging -> print console log/adb log/appium log, stop video recording
			ReportManager.getInstance().stopLogging(testResult);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		
		TestingDevice.get().shutdownAppium();
		TestingDevice.get().freeDevice();
	}
}
