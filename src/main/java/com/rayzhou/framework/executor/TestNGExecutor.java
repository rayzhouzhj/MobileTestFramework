package com.rayzhou.framework.executor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.testng.TestNG;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlSuite.ParallelMode;
import org.testng.xml.XmlTest;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.device.DeviceAllocationManager;
import com.rayzhou.framework.utils.FigletWriter;

public class TestNGExecutor 
{
	private final RunTimeContext context;
	private List<String> packages;
	private List<String> suiteFiles = new ArrayList<>();
	private List<String> groupsInclude = new ArrayList<>();
	private List<String> groupsExclude = new ArrayList<>();
	
	public TestNGExecutor(List<String> packages)
	{
		this.context = RunTimeContext.getInstance();
		this.packages = packages;
		
		initGroups();
	}

	/**
	 * Initialize testing groups: include groups and exclude groups
	 */
	private void initGroups() 
	{
		if (context.getProperty("INCLUDE_GROUPS") != null) 
		{
			Collections.addAll(groupsInclude, context.getProperty("INCLUDE_GROUPS").split("\\s*,\\s*"));
		} 

		if (context.getProperty("EXCLUDE_GROUPS") != null) 
		{
			Collections.addAll(groupsExclude, context.getProperty("EXCLUDE_GROUPS").split("\\s*,\\s*"));
		} 
	}

	/**
	 * Start Test Execution
	 * @param executionType Parallel or distribute
	 * @return
	 * @throws Exception
	 */
	protected boolean startTestExecution(ExecutionType executionType) throws Exception 
	{
		URL testClassUrl = null;
		List<URL> testClassUrls = new ArrayList<>();
		String testClassPackagePath ="file:" + System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes" + File.separator;

		// Add URL for each test package
		for (int i = 0; i < this.packages.size(); i++) 
		{
			testClassUrl = new URL(testClassPackagePath + this.packages.get(i).replaceAll("\\.", "/"));
			testClassUrls.add(testClassUrl);
		}

		// Find test class by annotation: org.testng.annotations.Test.class
		Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(testClassUrls).setScanners(new MethodAnnotationsScanner()));
		Set<Method> resources = reflections.getMethodsAnnotatedWith(org.testng.annotations.Test.class);

		Map<String, List<Method>> classMethodMap = createTestsMap(resources);

		boolean hasFailure;
		// For distribute test
		if (executionType == ExecutionType.DISTRIBUTE) 
		{
			constructXmlSuiteForDeviceSetup(classMethodMap);
			constructXmlSuiteForDistribution(classMethodMap);
			hasFailure = triggerTestNGTest();
		}
		// For parallel test
		else 
		{
			constructXmlSuiteForParallel(classMethodMap);
			hasFailure = triggerTestNGTest();
		}

		System.out.println("Finally complete");
		FigletWriter.print("Test Completed");

		return hasFailure;
	}

	/**
	 * Trigger TestNG Test
	 * @return test result
	 */
	private boolean triggerTestNGTest() 
	{
		TestNG testNG = new TestNG();
		testNG.setTestSuites(this.suiteFiles);
		testNG.run();

		return testNG.hasFailure();
	}

	/**
	 * Construct TestNG XML for device setup
	 * @param classMethodMap Class and Methods Map
	 * @return XML suite
	 */
	private XmlSuite constructXmlSuiteForDeviceSetup(Map<String, List<Method>> classMethodMap) 
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
		listeners.add(com.rayzhou.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.rayzhou.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Generate test details
		for (String udid : deviceUDIDs) 
		{
			XmlTest test = new XmlTest(suite);
			test.setName("Mobile Setup - " + udid);
			test.setPreserveOrder(false);
			test.addParameter("device", udid);

			// Add test class and methods
			test.setXmlClasses(createXmlClasses(testcases, classMethodMap));
		}

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "setupsuite");

		// Remove setup class from test suite
		classMethodMap.remove(className);

		return suite;
	}

	/**
	 * Construct XML for Parallel test
	 * @param classMethodMap Map for Class and Methods
	 * @return XML suite
	 */
	private XmlSuite constructXmlSuiteForParallel(Map<String, List<Method>> classMethodMap) 
	{
		Set<String> deviceUDIDs = DeviceAllocationManager.getInstance().getDeviceUDIDs();

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
		listeners.add(com.rayzhou.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.rayzhou.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Generate test details
		for (String udid : deviceUDIDs) 
		{
			XmlTest test = new XmlTest(suite);
			test.setName("Mobile Test - " + udid);
			test.setPreserveOrder(false);
			test.addParameter("device", udid);
			test.setIncludedGroups(this.groupsInclude);
			test.setExcludedGroups(this.groupsExclude);

			// Add test class and methods
			test.setXmlClasses(createXmlClasses(null, classMethodMap));
		}

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "testsuite");

		return suite;
	}

	/**
	 * Construct XML for Distribution test
	 * @param classMethodMap Map for Class and Methods
	 * @return
	 */
	private XmlSuite constructXmlSuiteForDistribution(Map<String, List<Method>> classMethodMap) 
	{
		Set<String> deviceUDIDs = DeviceAllocationManager.getInstance().getDeviceUDIDs();

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
		listeners.add(com.rayzhou.framework.testng.listeners.InvokedMethodListener.class.getName());
		listeners.add(com.rayzhou.framework.testng.listeners.RetryListener.class.getName());
		suite.setListeners(listeners);

		// Initialize the XML Test Suite
		XmlTest test = new XmlTest(suite);
		test.setName("Mobile Test");
		// Device is not associate to specific test or class
		test.addParameter("device", "");
		test.setIncludedGroups(this.groupsInclude);
		test.setExcludedGroups(this.groupsExclude);

		// Add test class and methods
		test.setXmlClasses(createXmlClasses(null, classMethodMap));

		System.out.println(suite.toXml());
		writeTestNGFile(suite, "testsuite");

		return suite;
	}

	/**
	 * Create XML classes base on testcases and class methods
	 * @param testcases
	 * @param classMethodMap Map for Class and Methods
	 * @return
	 */
	private List<XmlClass> createXmlClasses(List<String> testcases, Map<String, List<Method>> classMethodMap) 
	{
		List<XmlClass> xmlClassList = new ArrayList<>();

		for (String className : classMethodMap.keySet()) 
		{
			if (className.contains("Test")) 
			{
				if (testcases != null && testcases.size() == 0) 
				{
					XmlClass clazz = new XmlClass();
					clazz.setName(className);
					xmlClassList.add(clazz);
				} 
				else 
				{
					for (String testCase : testcases) 
					{
						for (int index = 0; index < this.packages.size(); index++)
						{
							String testName = this.packages.get(index).concat("." + testCase).toString();
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

	/**
	 * Write suite to xml
	 * @param suite
	 * @param fileName
	 */
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

	/**
	 * Create TestNG class to methods mapping
	 * @param methods
	 * @return
	 */
	private Map<String, List<Method>> createTestsMap(Set<Method> methods) 
	{
		StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
		final String runnerClass = stElements[3].getClassName();
		System.out.println("TestExecutor: Runner Class => " + runnerClass);
		Map<String, List<Method>> testsMap = new HashMap<>();
		methods.stream().forEach(method -> 
		{
			String className = method.getDeclaringClass().getPackage().getName() + "." + method.getDeclaringClass().getSimpleName();

			// Skip runner class
			if(runnerClass.equals(className))
			{
				return;
			}

			// Get method list from specific test class
			List<Method> methodsList = testsMap.get(className);

			// If the method list is empty, initialize it and add it to test class map
			if (methodsList == null)
			{
				methodsList = new ArrayList<>();
				testsMap.put(className, methodsList);
			}

			// If current method is duplicated
			if(methodsList.contains(method))
			{
				return;
			}

			// Skip the method with the exclude groups
			// Added this filter because TestNG sometimes does not filter the exclude correctly
			if (method.isAnnotationPresent(Test.class))
			{
				boolean isIncluded = false;
				Test test = method.getAnnotation(Test.class);
				String[] groups = test.groups();
				for(String group : groups)
				{
					if(this.groupsExclude.contains(group))
					{
						// If no test method is included for the test class
						if(methodsList.isEmpty())
						{
							// Remove test class from test map
							testsMap.remove(className);
						}

						return;
					}

					// If include groups are not specified or current test group is in the include groups
					if(this.groupsInclude.size() == 0 || this.groupsInclude.contains(group))
					{
						isIncluded = true;
					}
				}

				// Add method to list
				if(isIncluded)
				{
					methodsList.add(method);
				}
				else
				{
					// If no test method is included for the test class
					if(methodsList.isEmpty())
					{
						// Remove test class from test map
						testsMap.remove(className);
					}
				}
			}
		});

		return testsMap;
	}

}
