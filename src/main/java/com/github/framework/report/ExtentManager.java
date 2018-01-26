package com.github.framework.report;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;
import com.aventstack.extentreports.reporter.KlovReporter;
import com.aventstack.extentreports.reporter.configuration.ChartLocation;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.github.framework.context.RunTimeContext;
import com.github.framework.context.TestingDevice;
import com.github.framework.utils.CommandPrompt;

public class ExtentManager 
{
	private static String filePath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "MobileTestReport.html";
	private static String extentXML = System.getProperty("user.dir") + File.separator + "extent.xml";
	private static CommandPrompt commandPrompt = new CommandPrompt();
	
	private static ExtentReports extent = ExtentManager.init();
	private static ConcurrentHashMap<String, ExtentTest> extentReportMap = new ConcurrentHashMap<>();

	private static ExtentReports init()
	{
		if (extent == null)
		{
			extent = new ExtentReports();
			extent.attachReporter(getHtmlReporter());
			if (RunTimeContext.getInstance().getProperty("EXTENT_SERVER", "OFF").equalsIgnoreCase("ON"))
			{
				extent.attachReporter(getKlovReporter());
			}

			String command = "node " + RunTimeContext.getInstance().getProperty("APPIUM_JS_PATH") + " -v";
			String appiumVersion = commandPrompt.runCommand(command).replace("\n", "");
			String executionMode = RunTimeContext.getInstance().getProperty("RUNNER");
			String platform = RunTimeContext.getInstance().getProperty("PLATFORM");
			String build = RunTimeContext.getInstance().getProperty("BUILD_NUMBER");
			if(build == null) build = "";
			
			extent.setSystemInfo("AppiumServer", appiumVersion);
			extent.setSystemInfo("Runner", executionMode);
			extent.setSystemInfo("Platform", platform);
			extent.setSystemInfo("Build", build);

			List<Status> statusHierarchy = Arrays.asList(
					Status.SKIP,
					Status.FATAL,
					Status.FAIL,
					Status.ERROR,
					Status.WARNING,
					Status.PASS,
					Status.DEBUG,
					Status.INFO
					);

			extent.config().statusConfigurator().setStatusHierarchy(statusHierarchy);
		}

		return extent;
	}

	private static ExtentHtmlReporter getHtmlReporter()
	{
		ExtentHtmlReporter htmlReporter = new ExtentHtmlReporter(filePath);
		htmlReporter.loadXMLConfig(extentXML);
		// make the charts visible on report open
		htmlReporter.config().setChartVisibilityOnOpen(true);

		// report title
		htmlReporter.config().setDocumentTitle("Mobile Test Report");
		htmlReporter.config().setReportName("Mobile Test Report");
		htmlReporter.config().setTestViewChartLocation(ChartLocation.TOP);
		htmlReporter.config().setTheme(Theme.STANDARD);

		return htmlReporter;
	}

	private static KlovReporter getKlovReporter() 
	{
		String host = RunTimeContext.getInstance().getProperty("MONGODB_SERVER");
		Integer port = Integer.parseInt(RunTimeContext.getInstance().getProperty("MONGODB_PORT"));
		
		KlovReporter klov = new KlovReporter();
		klov.initMongoDbConnection(host, port);

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

		String product = RunTimeContext.getInstance().getProperty("PRODUCT_NAME");
		String platform = RunTimeContext.getInstance().getProperty("PLATFORM");
		String buildNum = RunTimeContext.getInstance().getProperty("BUILD_NUMBER");
		String projectName = (product == null)? platform + "_Test" : product + "_" + platform;
		String reportName = (buildNum == null)? formatter.format(LocalDateTime.now()) : buildNum;
		
		// project name
		klov.setProjectName(projectName);
		// report or build name
		klov.setReportName(reportName);

		// server URL
		// ! must provide this to be able to upload snapshots
		String url = RunTimeContext.getInstance().getProperty("KLOV_URL", "");
		if (!url.isEmpty()) 
		{
			klov.setKlovUrl(url);
		}

		return klov;
	}

	public static void flush() 
	{
		extent.flush();
	}
	
	public static void removeTest(ExtentTest test) 
	{
		extent.removeTest(test);
	}
	
	public synchronized static ExtentTest createTest(String name, String description) 
	{
		ExtentTest test;
		String testNodeName = name;
		
		/*
		 * For Parallel Execution
		 * Create ONE Test Class Node for each device
		 * 
		 * For Distribution Execution
		 * Create ONE Test Class Node for all devices
		 */
		
		// Update ExtentReport Map key with unique device id for parallel execution
		if(RunTimeContext.getInstance().isParallelExecution())
		{
			testNodeName = testNodeName + "-" + TestingDevice.getDeviceUDID();
		}
		
		if(extentReportMap.containsKey(testNodeName))
		{
			System.out.println("Reuse Test Thread ID: "+ Thread.currentThread().getId() + ", Key: " + name);
			test = extentReportMap.get(testNodeName);
		}
		else
		{
			System.out.println("Create new Test Thread ID: "+ Thread.currentThread().getId() + ", Key: " + name);
			test = extent.createTest(name, description);
			extentReportMap.put(testNodeName, test);
		}

		return test;
	}
}
