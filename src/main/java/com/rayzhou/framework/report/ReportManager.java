package com.rayzhou.framework.report;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.ITestResult;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.testng.model.TestInfo;
import com.rayzhou.framework.utils.ScreenShotManager;

public class ReportManager
{
	private TestLogManager testLogger;
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
		testLogger = new TestLogManager();
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

		// Start logging
		testLogger.startLogging(testInfo.getMethodName(), testInfo.getClassName());
	}

	public void stopLogging(ITestResult result) throws IOException 
	{
		testLogger.stopLogging(result, TestingDevice.get().getProductTypeAndVersion(), currentTestMethod.get());
		
		ExtentManager.flush();
		
		if (result.getStatus() == ITestResult.SKIP) 
		{
			// Remove previous log data for retry test
			this.removeTest();
		}
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
			String[] classAndMethod = getTestClassNameAndMethodName().split(",");
			String screenShot = screenshotManager.captureScreenShot(Status.INFO, classAndMethod[0], classAndMethod[1], TestingDevice.get().getProductTypeAndVersion());
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
		String[] classAndMethod = getTestClassNameAndMethodName().split(",");
		try 
		{
			String screenShot = screenshotManager.captureScreenShot(Status.FAIL, classAndMethod[0], classAndMethod[1], TestingDevice.get().getProductTypeAndVersion());
			this.currentTestMethod.get().log(Status.FAIL, message);
			this.currentTestMethod.get().addScreenCaptureFromPath(screenShot);
			this.testResult.get().setStatus(ITestResult.FAILURE);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private String getTestClassNameAndMethodName()
	{
		List<String> packages = RunTimeContext.getInstance().getTestcasePackages();
		
		Exception ex = new Exception();
		StackTraceElement[] stacks = ex.getStackTrace();
		for(StackTraceElement e : stacks)
		{
			for(int i = 0; i < packages.size(); i++)
			{
				if(e.getClassName().startsWith(packages.get(i)) && e.getMethodName().startsWith("test"))
				{
					return e.getClassName().substring(e.getClassName().lastIndexOf(".") + 1) + "," + e.getMethodName();
				}
			}
		}

		return "UnknowClass,UnknowMethod";
	}
}
