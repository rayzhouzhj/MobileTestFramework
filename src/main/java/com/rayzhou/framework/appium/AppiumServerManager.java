package com.rayzhou.framework.appium;

import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.testng.model.TestInfo;
import com.rayzhou.framework.utils.AvailablePorts;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.AndroidServerFlag;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import io.appium.java_client.service.local.flags.ServerArgument;

/**
 * Appium Manager - this class contains method to start and stops appium server
 * To execute the tests from eclipse, you need to set PATH as
 * /usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin in run configuration
 */
public class AppiumServerManager 
{
	public AppiumServerManager()
	{
	}

	/**
	 * start appium with auto generated ports : appium port, chrome port,
	 * bootstrap port and device UDID
	 */
	ServerArgument webKitProxy = new ServerArgument() 
	{
		@Override
		public String getArgument() 
		{
			return "--webkit-debug-proxy-port";
		}
	};

	/**
	 * start appium with auto generated ports : appium port, chrome port,
	 * bootstrap port and device UDID
	 */
	public AppiumDriverLocalService startAppiumServerForAndroid(TestInfo testInfo) throws Exception 
	{
		System.out.println("**************************************************************************");
		System.out.println("Starting Appium Server to handle Android Device:: " + TestingDevice.getDeviceUDID());
		System.out.println("**************************************************************************\n");

		AppiumDriverLocalService service;
		int port = AvailablePorts.get();
		int chromePort = AvailablePorts.get();
		int bootstrapPort = AvailablePorts.get();
		int selendroidPort = AvailablePorts.get();

		String logFilePath = RunTimeContext.getInstance().getLogPath("appiumlogs", testInfo.getClassName(), "");
		logFilePath = logFilePath + testInfo.getMethodName() + ".txt";
		File logFile = new File(logFilePath);

		AppiumServiceBuilder builder = new AppiumServiceBuilder()
				.withAppiumJS(new File(RunTimeContext.getInstance().getProperty("APPIUM_JS_PATH")))
				.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				.withLogFile(logFile)
				.withArgument(AndroidServerFlag.CHROME_DRIVER_PORT, Integer.toString(chromePort))
				.withArgument(AndroidServerFlag.BOOTSTRAP_PORT_NUMBER, Integer.toString(bootstrapPort))
				.withIPAddress("127.0.0.1")
				.withArgument(AndroidServerFlag.SUPPRESS_ADB_KILL_SERVER)
				.withArgument(AndroidServerFlag.SELENDROID_PORT, Integer.toString(selendroidPort))
				.usingPort(port);

		service = builder.build();
		this.removeSystemOutPutForProd(service);

		service.start();
		
		return service;
	}

	public AppiumDriverLocalService startAppiumServerForIOS(TestInfo testInfo, String webKitPort) throws Exception 
	{
		System.out.println("***********************************************************");
		System.out.println("Starting Appium Server to handle IOS:: " + TestingDevice.getDeviceUDID());
		System.out.println("***********************************************************");

		String logFilePath = RunTimeContext.getInstance().getLogPath("appiumlogs", testInfo.getClassName(), "");
		logFilePath = logFilePath + testInfo.getMethodName() + ".txt";

		File logFile = new File(logFilePath);

		int port = AvailablePorts.get();
		File classPathRoot = new File(System.getProperty("user.dir"));
		String tempFileDir = new File(String.valueOf(classPathRoot)).getAbsolutePath() + "/target/ios_temp/" + "tmp_" + port;

		AppiumDriverLocalService service;
		AppiumServiceBuilder builder = new AppiumServiceBuilder()
				.withAppiumJS(new File(RunTimeContext.getInstance().getProperty("APPIUM_JS_PATH")))
				.withArgument(GeneralServerFlag.LOG_LEVEL, "info")
				.withLogFile(logFile)
				.withArgument(webKitProxy, webKitPort)
				.withIPAddress("127.0.0.1")
				.withArgument(GeneralServerFlag.LOG_LEVEL, "debug")
				.withArgument(GeneralServerFlag.TEMP_DIRECTORY, tempFileDir)
				.withArgument(GeneralServerFlag.SESSION_OVERRIDE)
				.usingPort(port);

		service = builder.build();
		this.removeSystemOutPutForProd(service);

		service.start();
		
		return service;
	}

	private void removeSystemOutPutForProd(AppiumDriverLocalService service)
	{
		// Print server log for local debugging only
		if(RunTimeContext.getInstance().getProperty("APPIUM_DEBUG_LOG", "ON").equalsIgnoreCase("ON"))
		{
			return;
		}

		Field streamField;
		Field streamsField;
		try 
		{
			streamField = AppiumDriverLocalService.class.getDeclaredField("stream");
			streamField.setAccessible(true);
			streamsField = Class.forName("io.appium.java_client.service.local.ListOutputStream").getDeclaredField("streams");
			streamsField.setAccessible(true);

			// remove System.out logging
			((ArrayList<OutputStream>) streamsField.get(streamField.get(service))).clear(); 
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		}
	}
}
