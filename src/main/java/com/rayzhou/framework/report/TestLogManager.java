package com.rayzhou.framework.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.device.MobilePlatform;
import com.rayzhou.framework.utils.ScreenShotManager;

public class TestLogManager 
{
	private ScreenShotManager screenShotManager;

	public TestLogManager() 
	{
		screenShotManager = new ScreenShotManager();
	}

	public void startLogging(String methodName, String className)
	{
		if (RunTimeContext.getInstance().isVideoLogEnabled()) 
		{
			startVideoRecording(methodName, className);
		}

		TestingDevice.get().getLogReader().startCaptureLog();
	}

	private void startVideoRecording(String methodName, String className) 
	{
		if (RunTimeContext.getInstance().isVideoLogEnabled()) 
		{
			System.out.println("**************Starting Video Recording**************");
			TestingDevice.get().startVideoRecording(className, methodName);
		}
	}

	public void stopLogging(ITestResult result, String deviceModel, ExtentTest test) throws FileNotFoundException 
	{
		String className = result.getInstance().getClass().getSimpleName();

		if(result.isSuccess())
		{
			test.log(Status.PASS, "Test Passed: " + result.getMethod().getMethodName());
		}
		else if (result.getStatus() == ITestResult.FAILURE) 
		{
			handleTestFailure(result, className, test, deviceModel);
		}
		else if (result.getStatus() == ITestResult.SKIP) 
		{
			test.log(Status.SKIP, "Test skipped");
		}
		else
		{
			// DO NOTHING
		}

		if (RunTimeContext.getInstance().isVideoLogEnabled())
		{
			// Stop video log
			String videoFilePath = stopVideoRecording(result, className);
			test.log(Status.INFO, "<a target=\"_parent\" href=" + videoFilePath + ">Videologs</a>");
		}

		// Set adb log and server log
		getRelatedLogs(result, test);
	}

	private String stopVideoRecording(ITestResult result, String className)
	{
		String videoPath = "";

		if (RunTimeContext.getInstance().isVideoLogEnabled()) 
		{
			System.out.println("**************Stopping Video Recording**************");
			videoPath = TestingDevice.get().stopVideoRecording(className, result.getMethod().getMethodName());
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

	private void getRelatedLogs(ITestResult result, ExtentTest test) throws FileNotFoundException
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

			test.log(Status.INFO, "<a target=\"_blank\" href=" + adbLogFilePath + ">AdbLogs</a>");
			test.log(Status.INFO, "<a target=\"_blank\" href=" + serverLogFilePath + ">AppiumServerLogs</a>");
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

			test.log(Status.INFO, "<a target=\"_blank\" href=" + consoleLogFilePath + ">ConsoleLogs</a>");
			test.log(Status.INFO, "<a target=\"_blank\" href=" + serverLogFilePath + ">AppiumServerLogs</a>");
		}
	}

	private void handleTestFailure(ITestResult result, String className, ExtentTest test, String deviceModel)
	{
		if (result.getStatus() == ITestResult.FAILURE) 
		{
			// Print exception stack trace if any
			Throwable throwable = result.getThrowable();
			if(throwable != null)
			{
				throwable.printStackTrace();
				test.log(Status.FAIL, "<pre>" + result.getThrowable().getMessage() + "</pre>");
			}

			try
			{
				String screenShotPath = screenShotManager.captureScreenShot(
						Status.FAIL,
						result.getInstance().getClass().getSimpleName(),
						result.getMethod().getMethodName(), deviceModel);

				System.out.println("Screenshot: " + screenShotPath);
				test.addScreenCaptureFromPath(screenShotPath);
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
