package com.rayzhou.framework.device.ios;

import java.io.File;
import java.io.IOException;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.device.AbstractTestDevice;
import com.rayzhou.framework.device.MobilePlatform;
import com.rayzhou.framework.device.logreader.IDeviceLogReader;
import com.rayzhou.framework.device.logreader.IOSLogReader;
import com.rayzhou.framework.utils.AvailablePorts;
import com.rayzhou.framework.utils.CommandPrompt;
import com.rayzhou.framework.utils.TaskManager;
import com.rayzhou.framework.utils.TaskManager.Signal;

public class IOSDevice extends AbstractTestDevice
{
	public CommandPrompt commandPrompt = new CommandPrompt();
	private String productType = "";
	private String productVersion = "";
	private int webkitProxyPort;
	private int webkitProxyProcessID;

	public IOSDevice(String udid) 
	{
		super(udid);
		initDevice();
	}

	public IOSDevice(String udid, String deviceName, String productType, String productVersion, boolean isRealDevice) 
	{
		super(udid);

		this.deviceName = deviceName;
		this.productType = productType;
		this.productVersion = productVersion;
		this.setRealDevice(isRealDevice);
	}

	public void initDevice()
	{
		new Thread(() -> {
			this.getProductTypeAndVersion();
		}).start();
	}

	@Override
	public boolean isConnected()
	{
		CommandPrompt cmd = new CommandPrompt();
		String output = cmd.runCommand("idevice_id -l");
		System.out.println(output);

		if(output.contains(this.getUDID()))
		{
			return true;
		}

		return false;
	}
	
	@Override
	public MobilePlatform getPlatform()
	{
		return MobilePlatform.IOS;
	}
	
	@Override
	public IDeviceLogReader getLogReader()
	{
		if(this.logReader == null)
		{
			this.logReader = IOSLogReader.get(this);
		}

		return this.logReader;
	}

	@Override
	public String getProductTypeAndVersion()
	{
		return this.getProductType() + "-" + this.getOSVersion();
	}

	@Override
	public String getDeviceName()
	{
		if(deviceName.isEmpty())
		{
			deviceName = commandPrompt.runCommand("idevicename --udid " + this.getUDID());
		}

		return deviceName;
	}

	@Override
	public String getOSVersion()
	{
		if(productVersion.isEmpty())
		{
			productVersion = commandPrompt
					.runCommand("ideviceinfo --udid " + this.getUDID()  + " | grep ProductVersion")
					.split(":")[1].replace("\n", "").trim();
		}

		return productVersion;
	}

	public String getProductType()
	{
		if(productType.isEmpty())
		{
			productType = commandPrompt
					.runCommand("ideviceinfo --udid " + this.getUDID()  + " | grep ProductType")
					.split(":")[1].replace("\n", "").trim();

			productType = this.findProductFromMap(productType);
		}

		return productType;
	}

	private String findProductFromMap(String identifier)
	{
		switch (identifier)
		{
		case "iPod5,1":			return "iPod Touch 5";
		case "iPod7,1":      	return "iPod Touch 6";
		case "iPhone3,1":
		case "iPhone3,2":
		case "iPhone3,3":    	return "iPhone 4";
		case "iPhone4,1":    	return "iPhone 4s";
		case "iPhone5,1":
		case "iPhone5,2":    	return "iPhone 5";
		case "iPhone5,3":
		case "iPhone5,4":    	return "iPhone 5c";
		case "iPhone6,1":
		case "iPhone6,2":		return "iPhone 5s";
		case "iPhone7,2":		return "iPhone 6";
		case "iPhone7,1":		return "iPhone 6 Plus";
		case "iPhone8,1":		return "iPhone 6s";
		case "iPhone8,2":		return "iPhone 6s Plus";
		case "iPhone9,1":
		case "iPhone9,3":		return "iPhone 7";
		case "iPhone9,2":
		case "iPhone9,4":		return "iPhone 7 Plus";
		case "iPhone8,4":		return "iPhone SE";
		case "iPhone10,1":		return "iPhone 8";
		case "iPhone10,2":		return "iPhone 8 plus";
		case "iPhone10,3":		return "iPhone X";
		case "iPad2,1":
		case "iPad2,2":
		case "iPad2,3":
		case "iPad2,4":			return "iPad 2";
		case "iPad3,1":
		case "iPad3,2":
		case "iPad3,3":			return "iPad 3";
		case "iPad3,4":
		case "iPad3,5":
		case "iPad3,6":			return "iPad 4";
		case "iPad4,1":
		case "iPad4,2":
		case "iPad4,3":			return "iPad Air";
		case "iPad5,3":
		case "iPad5,4":			return "iPad Air 2";
		case "iPad6,11":
		case "iPad6,12":		return "iPad 5";
		case "iPad2,5":
		case "iPad2,6":
		case "iPad2,7":			return "iPad Mini";
		case "iPad4,4":
		case "iPad4,5":
		case "iPad4,6":			return "iPad Mini 2";
		case "iPad4,7":
		case "iPad4,8":
		case "iPad4,9":			return "iPad Mini 3";
		case "iPad5,1":
		case "iPad5,2":			return "iPad Mini 4";
		case "iPad6,3":
		case "iPad6,4":			return "iPad Pro 9.7 Inch";
		case "iPad6,7":
		case "iPad6,8":			return "iPad Pro 12.9 Inch";
		case "iPad7,1":
		case "iPad7,2":			return "iPad Pro 12.9 Inch 2. Generation";
		case "iPad7,3":
		case "iPad7,4":			return "iPad Pro 10.5 Inch";
		case "AppleTV5,3":		return "Apple TV";
		case "i386":
		case "x86_64":			return "Simulator";

		default:				return identifier;
		}
	}

	public void startRealDeviceVideoRecording() 
	{
		String iosCommand = "flick video -a start -p ios -u " + this.getUDID();
		commandPrompt.runCommand(iosCommand);
	}

	public String stopRealDeviceVideoRecording(String videoLocation, String videoFileName) 
	{
		String videoFile = videoLocation + File.separator + videoFileName + ".mp4";

		String iosCommand = "flick video -a stop -p ios -o " + videoLocation + " -n "
				+ videoFileName + " -u " + this.getUDID();

		// To reduce execution time, use another thread to stop recording
		new Thread(() -> {commandPrompt.runCommand(iosCommand);}).start();

		return videoFile;
	}

	@Override
	public void startVideoRecording(String className, String methodName)
	{
		this.startRealDeviceVideoRecording();
	}

	@Override
	public String stopVideoRecording(String className, String methodName) 
	{
		String videoLocation = RunTimeContext.getInstance().getLogPath("screenshot", className, methodName);
		String videoFileName = methodName + RunTimeContext.currentDateAndTime();

		return this.stopRealDeviceVideoRecording(videoLocation, videoFileName);
	}

	public String startIOSWebKit() throws IOException 
	{
		// Set IOS WebKit Proxy
		webkitProxyPort = AvailablePorts.get();

		String serverPath = RunTimeContext.getInstance().getProperty("APPIUM_JS_PATH");
		File file = new File(serverPath);
		File currentPath = new File(file.getParent());
		System.out.println(currentPath);
		file = new File(currentPath + "/.." + "/..");
		String ios_web_lit_proxy_runner = file.getCanonicalPath() + "/bin/ios-webkit-debug-proxy-launcher.js";

		String webkitRunner = ios_web_lit_proxy_runner + " -c " + TestingDevice.getDeviceUDID() + ":" + webkitProxyPort + " -d";
		System.out.println(webkitRunner);
		Process process = Runtime.getRuntime().exec(webkitRunner);
		System.out.println("WebKit Proxy is started on device " 
				+ TestingDevice.getDeviceUDID() + " and with port number " + webkitProxyPort);

		//Add the Process ID to hashMap, which would be needed to kill IOSwebProxywhen required
		webkitProxyProcessID = TaskManager.getPid(process);
		System.out.println("WebKit Proxy Process ID's: " + webkitProxyProcessID);

		return String.valueOf(webkitProxyPort);
	}

	public void destroyIOSWebKitProxy()
	{
		try 
		{
			Thread.sleep(3000);
		} 
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}

		if (webkitProxyProcessID != -1) 
		{
			System.out.println("Kills webkit proxy on port: " + webkitProxyProcessID);
			TaskManager.killProcess(webkitProxyProcessID + "", Signal.FORCE_KILL);
			
			webkitProxyProcessID = -1;
		}
	}

	public void setupAppiumForTest() throws Exception
	{
		// Start Appium server
		this.startAppiumServer();
		this.startAppiumDriver();
	}

	@Override
	public void startAppiumServer() throws Exception
	{
		// Start IOS WebKit
		startIOSWebKit();

		// Start Appium server
		appiumServer = appiumServerManager.startAppiumServerForIOS(testInfo, String.valueOf(this.webkitProxyPort));
	}

	@Override
	public void stopAppiumServer()
	{
		if(appiumServer != null)
		{
			appiumServer.stop();
			if (appiumServer.isRunning()) 
			{
				System.out.println("AppiumServer didn't shutdown... Trying to quit again....");
				appiumServer.stop();
			}
		}

		appiumServer = null;
		
		this.destroyIOSWebKitProxy();
	}
}
