package com.github.framework.executor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

import com.github.framework.context.RunTimeContext;
import com.github.framework.device.DeviceAllocationManager;
import com.github.framework.utils.FigletWriter;

public class TestNGExecutor 
{
	private List<String> suiteFiles = new ArrayList<>();
	private ArrayList<String> packages = new ArrayList<String>();

	public TestNGExecutor()
	{
	}

	/**
	 * 
	 * @param test test classes
	 * @param pack Package list, separate by comma, e.g. com.test.package1, com.test.package2
	 * @param executionType Parallel or distribute
	 * @return
	 * @throws Exception
	 */
	protected boolean startTestExecution(List<String> test, String pack, ExecutionType executionType) throws Exception 
	{
		URL testClassUrl = null;
		List<URL> testClassUrls = new ArrayList<>();
		String testClassPackagePath ="file:" + System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes" + File.separator;
		// Add test packages to item list
		Collections.addAll(packages, pack.split("\\s*,\\s*"));

		// Add URL for each test package
		for (int i = 0; i < packages.size(); i++) 
		{
			testClassUrl = new URL(testClassPackagePath + packages.get(i).replaceAll("\\.", "/"));
			testClassUrls.add(testClassUrl);
		}

		// Find test class by annotation: org.testng.annotations.Test.class
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(testClassUrls).setScanners(new MethodAnnotationsScanner()));
		Set<Method> resources = reflections.getMethodsAnnotatedWith(org.testng.annotations.Test.class);

		Map<String, List<Method>> methods = createTestsMap(resources);

		boolean hasFailure;
		// For distribute test
		if (executionType == ExecutionType.DISTRIBUTE) 
		{
			constructXmlSuiteForDeviceSetup(methods);
			constructXmlSuiteForDistribution(pack, test, methods);
			hasFailure = triggerTestNGTest();
		}
		// For parallel test
		else 
		{
			constructXmlSuiteForParallel(pack, test, methods);
			hasFailure = triggerTestNGTest();
		}

		System.out.println("Finally complete");
		FigletWriter.print("Test Completed");

		return hasFailure;
	}

	private boolean triggerTestNGTest() 
	{
		TestNG testNG = new TestNG();
		testNG.setTestSuites(suiteFiles);
		testNG.run();

		return testNG.hasFailure();
	}

	private XmlSuite constructXmlSuiteForDeviceSetup(Map<String, List<Method>> methods) 
	{
		List<String> testcases = new ArrayList<>();
		String className = RunTimeContext.getInstance().getProperty("APP_SETUP");
		testcases.add(className.substring(className.lastIndexOf(".") + 1));

		ArrayList<String> listeners = new ArrayList<>();
		Set<String> deviceUDIDs = DeviceAllocationManager.getInstance().getDeviceUDIDs();

		// Initialize XML Suite
		XmlSuite suite = new XmlSuite();
		suite.setName("Setup Suite");
		suite.setPreserveOrder(true);

		/*
		 *  Set parallel mode to TESTS level
		 *  Each test will be taken care of by 1 thread
		 */
		suite.setThreadCount(deviceUDIDs.size());
		suite.setParallel(ParallelMode.TESTS);
		suite.setVerbose(2);

		// Add listeners
		listeners.add(com.github.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.github.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Generate test details
		for (String udid : deviceUDIDs) 
		{
			XmlTest test = new XmlTest(suite);
			test.setName("Mobile Setup - " + udid);
			test.setPreserveOrder(false);
			test.addParameter("device", udid);

			// Add test class and methods
			test.setXmlClasses(getXmlClasses(testcases, methods));
		}

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "setupsuite");

		// Remove setup class from test suite
		methods.remove(className);

		return suite;
	}

	private XmlSuite constructXmlSuiteForParallel(String pack, List<String> testcases, Map<String, List<Method>> methods) 
	{
		Set<String> deviceUDIDs = DeviceAllocationManager.getInstance().getDeviceUDIDs();

		// Add groups
		ArrayList<String> groupsInclude = new ArrayList<>(
				Arrays.asList(RunTimeContext.getInstance().getProperty("INCLUDE_GROUPS", "").split("\\s*,\\s*")));
		ArrayList<String> groupsExclude = new ArrayList<>(
				Arrays.asList(RunTimeContext.getInstance().getProperty("EXCLUDE_GROUPS", "").split("\\s*,\\s*")));

		// Initialize XML Suite
		XmlSuite suite = new XmlSuite();
		suite.setName("Test Suite");
		suite.setPreserveOrder(true);

		/*
		 *  Set parallel mode to TESTS level
		 *  Each test will be taken care of by 1 thread
		 */
		suite.setThreadCount(deviceUDIDs.size());
		suite.setParallel(ParallelMode.TESTS);
		suite.setVerbose(2);

		// Add listeners
		ArrayList<String> listeners = new ArrayList<>();
		listeners.add(com.github.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.github.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Generate test details
		for (String udid : deviceUDIDs) 
		{
			XmlTest test = new XmlTest(suite);
			test.setName("Mobile Test - " + udid);
			test.setPreserveOrder(false);
			test.addParameter("device", udid);
			test.setIncludedGroups(groupsInclude);
			test.setExcludedGroups(groupsExclude);

			// Add test class and methods
			test.setXmlClasses(getXmlClasses(testcases, methods));
		}

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "testsuite");

		return suite;
	}

	private XmlSuite constructXmlSuiteForDistribution(String pack, List<String> testcases, Map<String, List<Method>> methods) 
	{
		Set<String> deviceUDIDs = DeviceAllocationManager.getInstance().getDeviceUDIDs();

		// Add groups
		ArrayList<String> groupsInclude = new ArrayList<>(
				Arrays.asList(RunTimeContext.getInstance().getProperty("INCLUDE_GROUPS", "").split("\\s*,\\s*")));
		ArrayList<String> groupsExclude = new ArrayList<>(
				Arrays.asList(RunTimeContext.getInstance().getProperty("EXCLUDE_GROUPS", "").split("\\s*,\\s*")));

		// Initialize XML Suite
		XmlSuite suite = new XmlSuite();
		suite.setName("Test Suite");
		suite.setPreserveOrder(true);

		/*
		 *  Set parallel mode to METHODS level
		 *  Each method will be taken care of by 1 thread
		 */
		suite.setThreadCount(deviceUDIDs.size());
		suite.setDataProviderThreadCount(deviceUDIDs.size());
		suite.setParallel(ParallelMode.METHODS);
		suite.setVerbose(2);

		// Add listeners
		ArrayList<String> listeners = new ArrayList<>();
		listeners.add(com.github.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.github.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Initialize the XML Test Suite
		XmlTest test = new XmlTest(suite);
		test.setName("Mobile Test");
		// Device is not associate to specific test or class
		test.addParameter("device", "");
		test.setIncludedGroups(groupsInclude);
		test.setExcludedGroups(groupsExclude);

		// Add test class and methods
		test.setXmlClasses(getXmlClasses(testcases, methods));

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "testsuite");

		return suite;
	}

	private List<XmlClass> getXmlClasses(List<String> testcases, Map<String, List<Method>> methods) 
	{
		List<XmlClass> xmlClassList = new ArrayList<>();

		for (String className : methods.keySet()) 
		{
			if (className.contains("Test")) 
			{
				if (testcases.size() == 0) 
				{
					XmlClass clazz = new XmlClass();
					clazz.setName(className);
					xmlClassList.add(clazz);
				} 
				else 
				{
					for (String testCase : testcases) 
					{
						for (int index = 0; index < packages.size(); index++)
						{
							String testName = packages.get(index).concat("." + testCase).toString();
							if (testName.equals(className)) 
							{
								XmlClass clazz = new XmlClass();
								clazz.setName(className);
								xmlClassList.add(clazz);
							}
						}
					}
				}

			}
		}

		return xmlClassList;
	}

	private void writeTestNGFile(XmlSuite suite, String fileName)
	{
		try 
		{
			String suiteXML = System.getProperty("user.dir") + "/target/" + fileName + ".xml";
			suiteFiles.add(suiteXML);

			FileWriter writer = new FileWriter(new File(suiteXML));
			writer.write(suite.toXml());
			writer.flush();
			writer.close();
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	private Map<String, List<Method>> createTestsMap(Set<Method> methods) 
	{
		Map<String, List<Method>> testsMap = new HashMap<>();
		methods.stream().forEach(method -> 
		{
			// Get method list from specific test class
			List<Method> methodsList = testsMap.get(method.getDeclaringClass().getPackage().getName() + "." + method.getDeclaringClass().getSimpleName());

			// If the method list is empty, initialize it and add it to test class map
			if (methodsList == null)
			{
				methodsList = new ArrayList<>();
				testsMap.put(method.getDeclaringClass().getPackage().getName() + "." + method.getDeclaringClass().getSimpleName(), methodsList);
			}

			// Add method to list
			methodsList.add(method);
		});

		return testsMap;
	}

}
