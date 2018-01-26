package com.github.framework.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.github.framework.context.RunTimeContext;
import com.github.framework.context.TestingDevice;
import com.github.framework.device.MobilePlatform;
import com.github.framework.testng.model.TestInfo;
import com.github.framework.utils.ScreenShotManager;

public class ReportManager
{
	private static ReportManager manager = new ReportManager();
	private ThreadLocal<ExtentTest> parentTestClass = new ThreadLocal<>();
	private ThreadLocal<ExtentTest> currentTestMethod = new ThreadLocal<>();
	private ThreadLocal<ITestResult> testResult = new ThreadLocal<>();
	private ScreenShotManager screenshotManager = new ScreenShotManager();

	private ConcurrentHashMap<String, Boolean> retryMap = new ConcurrentHashMap<>();

	public static ReportManager getInstance()
	{
		return manager;
	}

	private ReportManager() 
	{
	}

	public void removeTest()
	{
		ExtentManager.removeTest(currentTestMethod.get());

	}

	public boolean isRetryMethod(String methodName, String className)
	{
		String key = className + ":" + methodName + Thread.currentThread().getId();
		if(!this.retryMap.containsKey(key))
		{
			this.retryMap.put(key, false);
		}

		return this.retryMap.get(key);
	}

	public void setMethodRetryStatus(String methodName, String className, boolean status)
	{
		String key = className + ":" + methodName + Thread.currentThread().getId();
		this.retryMap.put(key, status);
	}

	public void startLogging(TestInfo testInfo)
	{
		// Update Author and set category
		this.setupReporterForTest(testInfo);

		// Start video logging
		if (RunTimeContext.getInstance().isVideoLogEnabled()) 
		{
			System.out.println("**************Starting Video Recording**************");
			TestingDevice.get().startVideoRecording(testInfo.getMethodName(), testInfo.getClassName());
		}

		// Start console/adb log
		TestingDevice.get().getLogReader().startCaptureLog();
	}

	public void setTestResult(ITestResult testResult)
	{
		this.testResult.set(testResult);
	}

	public ExtentTest setupReportForTestSet(TestInfo testInfo)
	{
		ExtentTest parent = ExtentManager.createTest(testInfo.getClassName(), testInfo.getClassDescription());
		parentTestClass.set(parent);

		return parent;
	}

	private void setupReporterForTest(TestInfo testInfo)
	{
		String category = TestingDevice.get().getProductTypeAndVersion();

		ExtentTest child = parentTestClass.get().createNode(
				testInfo.getTestName(), 
				category + "_" + TestingDevice.getDeviceUDID());
		child.assignCategory(category + "_" + TestingDevice.getDeviceUDID());

		// Assign Author if presented
		if (testInfo.getAuthorName() != null)
		{
			child.assignAuthor(testInfo.getAuthorName());
		} 

		currentTestMethod.set(child);
	}

	public void logInfoWithScreenShot(String message)
	{
		try 
		{
			TestInfo testInfo = TestingDevice.get().getTestInfo();
			String screenShot = screenshotManager.captureScreenShot(
													Status.INFO, 
													testInfo.getClassName(), 
													testInfo.getMethodName(), 
													TestingDevice.get().getProductTypeAndVersion());
			this.currentTestMethod.get().log(Status.INFO, message);
			this.currentTestMethod.get().addScreenCaptureFromPath(screenShot);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void logInfo(String message)
	{
		this.currentTestMethod.get().log(Status.INFO, message);
	}

	public void logPass(String message)
	{
		this.currentTestMethod.get().log(Status.PASS, message);
	}

	public void logFail(String message)
	{
		TestInfo testInfo = TestingDevice.get().getTestInfo();
		try 
		{
			String screenShot = screenshotManager.captureScreenShot(
													Status.FAIL, 
													testInfo.getClassName(),
													testInfo.getMethodName(), 
													TestingDevice.get().getProductTypeAndVersion());
			
			this.currentTestMethod.get().log(Status.FAIL, message);
			this.currentTestMethod.get().addScreenCaptureFromPath(screenShot);
			this.testResult.get().setStatus(ITestResult.FAILURE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public void stopLogging(ITestResult result) throws FileNotFoundException 
	{
		String className = result.getInstance().getClass().getSimpleName();

		if(result.isSuccess())
		{
			currentTestMethod.get().log(Status.PASS, "Test Passed: " + result.getMethod().getMethodName());
		}
		else if (result.getStatus() == ITestResult.FAILURE) 
		{
			this.handleTestFailure(result);
		}
		else if (result.getStatus() == ITestResult.SKIP) 
		{
			currentTestMethod.get().log(Status.SKIP, "Test skipped");
		}
		else
		{
			// DO NOTHING
		}

		if (RunTimeContext.getInstance().isVideoLogEnabled())
		{
			// Stop video log
			this.stopVideoRecording(result, className);
		}

		// Set adb log and server log
		this.getRelatedLogs(result);
		
		ExtentManager.flush();
		
		if (result.getStatus() == ITestResult.SKIP) 
		{
			// Remove previous log data for retry test
			this.removeTest();
		}
	}

	private String stopVideoRecording(ITestResult result, String className)
	{
		String videoPath = "";

		if (RunTimeContext.getInstance().isVideoLogEnabled()) 
		{
			System.out.println("**************Stopping Video Recording**************");
			videoPath = TestingDevice.get().stopVideoRecording(className, result.getMethod().getMethodName());
			currentTestMethod.get().log(Status.INFO, "<a target=\"_parent\" href=" + videoPath + ">Videologs</a>");
		}

		if(RunTimeContext.getInstance().getProperty("KEEP_VIDEO_LOGS") == null)
		{
			deleteSuccessVideos(result, videoPath);
		}

		return videoPath;
	}

	private void deleteSuccessVideos(ITestResult result, String videoPath) 
	{
		if (result.isSuccess()) 
		{
			File videoFile = new File(videoPath);
			System.out.println(videoFile);

			if (videoFile.exists())
			{
				videoFile.delete();
			}
		}
	}

	private void getRelatedLogs(ITestResult result) throws FileNotFoundException
	{
		String serverLogFilePath = RunTimeContext.getInstance().getLogPath(
				"appiumlogs", 
				result.getMethod().getRealClass().getSimpleName(), 
				"");
		serverLogFilePath = serverLogFilePath + result.getMethod().getMethodName() + ".txt";

		if (TestingDevice.getMobilePlatform().equals(MobilePlatform.ANDROID)
				&& TestingDevice.get().getDriver().getCapabilities().getCapability("browserName") == null) 
		{
			String adbLogFilePath = RunTimeContext.getInstance().getLogPath(
					"adblogs", 
					result.getMethod().getRealClass().getSimpleName() , 
					"");
			adbLogFilePath = adbLogFilePath + result.getMethod().getMethodName() + ".txt";

			List<String> logEntries = TestingDevice.get().getLogReader().getFullLogs();
			PrintWriter log_file_writer = new PrintWriter(new File(adbLogFilePath));
			for(String line : logEntries)
			{
				log_file_writer.println(line);
			}
			log_file_writer.close();

			currentTestMethod.get().log(Status.INFO, "<a target=\"_blank\" href=" + adbLogFilePath + ">AdbLogs</a>");
			currentTestMethod.get().log(Status.INFO, "<a target=\"_blank\" href=" + serverLogFilePath + ">AppiumServerLogs</a>");
		}
		else
		{
			String consoleLogFilePath = RunTimeContext.getInstance().getLogPath(
					"consolelogs", 
					result.getMethod().getRealClass().getSimpleName() , 
					"");
			consoleLogFilePath = consoleLogFilePath + result.getMethod().getMethodName() + ".txt";

			List<String> logEntries = TestingDevice.get().getLogReader().getFullLogs();
			PrintWriter log_file_writer = new PrintWriter(new File(consoleLogFilePath));
			for(String line : logEntries)
			{
				log_file_writer.println(line);
			}
			log_file_writer.close();

			currentTestMethod.get().log(Status.INFO, "<a target=\"_blank\" href=" + consoleLogFilePath + ">ConsoleLogs</a>");
			currentTestMethod.get().log(Status.INFO, "<a target=\"_blank\" href=" + serverLogFilePath + ">AppiumServerLogs</a>");
		}
	}

	private void handleTestFailure(ITestResult result)
	{
		if (result.getStatus() == ITestResult.FAILURE) 
		{
			// Print exception stack trace if any
			Throwable throwable = result.getThrowable();
			if(throwable != null)
			{
				throwable.printStackTrace();
				currentTestMethod.get().log(Status.FAIL, "<pre>" + result.getThrowable().getMessage() + "</pre>");
			}

			try
			{
				TestInfo testInfo = TestingDevice.get().getTestInfo();
				
				ScreenShotManager screenShotManager = new ScreenShotManager();
				String screenShotPath = screenShotManager.captureScreenShot(
						Status.FAIL,
						testInfo.getClassName(),
						testInfo.getMethodName(), 
						TestingDevice.get().getProductTypeAndVersion());

				System.out.println("Screenshot: " + screenShotPath);
				currentTestMethod.get().addScreenCaptureFromPath(screenShotPath);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
