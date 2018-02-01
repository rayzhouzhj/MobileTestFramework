package com.github.framework.appium;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.openqa.selenium.remote.DesiredCapabilities;

import com.github.framework.context.RunTimeContext;
import com.github.framework.context.TestingDevice;
import com.github.framework.utils.AvailablePorts;

import io.appium.java_client.remote.AndroidMobileCapabilityType;
import io.appium.java_client.remote.AutomationName;
import io.appium.java_client.remote.IOSMobileCapabilityType;
import io.appium.java_client.remote.MobileCapabilityType;

public class DesiredCapabilityBuilder 
{
	private ConcurrentHashMap<String, Boolean> appInstallationStatus = new ConcurrentHashMap<>();

	public DesiredCapabilityBuilder()
	{
	}

	public void setDeviceInstallationStatus(boolean status)
	{
		appInstallationStatus.put(TestingDevice.getDeviceUDID(), status);
	}

	public DesiredCapabilities build(DesiredCapability capability)
	{
		switch(capability)
		{
		case ANDROID_NATIVE: return this.androidNative();
		case IOS_NATIVE: return this.iosNative();
		case ANDROID_WEB: return this.androidWeb();
		case IOS_WEB: return this.iOSWeb();
		default: return null;
		}
	}

	private DesiredCapabilities androidNative() 
	{
		System.out.println("Setting Android Desired Capabilities:");
		DesiredCapabilities androidCapabilities = new DesiredCapabilities();
		androidCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
		androidCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android");
		androidCapabilities.setCapability(AndroidMobileCapabilityType.APP_ACTIVITY, RunTimeContext.getInstance().getProperty("APP_ACTIVITY"));
		androidCapabilities.setCapability(AndroidMobileCapabilityType.APP_PACKAGE, RunTimeContext.getInstance().getProperty("APP_PACKAGE"));
		androidCapabilities.setCapability(AndroidMobileCapabilityType.SYSTEM_PORT, AvailablePorts.get());
		androidCapabilities.setCapability(MobileCapabilityType.UDID, TestingDevice.getDeviceUDID());
		androidCapabilities.setCapability(AndroidMobileCapabilityType.AUTO_GRANT_PERMISSIONS, true);

		// Use ANDROID_UIAUTOMATOR2 for OS system greater than 5.0
		String version = TestingDevice.get().getOSVersion();
		if (Integer.parseInt(version.split("\\.")[0]) >= 5) 
		{
			androidCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
		}
		
		// If new app installation is required
		Boolean status = appInstallationStatus.get(TestingDevice.getDeviceUDID());
		if(status == null || status == Boolean.FALSE)
		{
			// Check app path in config
			String appPath = RunTimeContext.getInstance().getProperty("ANDROID_APP_PATH");
			if(appPath == null || appPath.isEmpty())
			{
				throw new IllegalArgumentException("Variable [ANDROID_APP_PATH] is not provided.");
			}

			// Set Mobile App to Capability
			Path path = FileSystems.getDefault().getPath(appPath);
			if (!path.isAbsolute()) 
			{
				androidCapabilities.setCapability(MobileCapabilityType.APP, path.normalize().toAbsolutePath().toString());
			} 
			else
			{
				androidCapabilities.setCapability(MobileCapabilityType.APP, appPath);
			}

			androidCapabilities.setCapability(MobileCapabilityType.NO_RESET, false);

			// Set installation status to true -> no installation for next test case
			appInstallationStatus.put(TestingDevice.getDeviceUDID(), Boolean.TRUE);
		}
		else
		{
			// No reset -> no app installation
			androidCapabilities.setCapability(MobileCapabilityType.NO_RESET, true);
		}

		return androidCapabilities;
	}

	private DesiredCapabilities androidWeb() 
	{
		DesiredCapabilities androidWebCapabilities = new DesiredCapabilities();
		androidWebCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "Android");
		androidWebCapabilities.setCapability(MobileCapabilityType.BROWSER_NAME, RunTimeContext.getInstance().getProperty("BROWSER_TYPE"));
		androidWebCapabilities.setCapability(MobileCapabilityType.TAKES_SCREENSHOT, true);
		androidWebCapabilities.setCapability(MobileCapabilityType.UDID, TestingDevice.getDeviceUDID());

		// Use ANDROID_UIAUTOMATOR2 for OS system greater than 5.0
		String version = TestingDevice.get().getOSVersion();
		if (Integer.parseInt(version.split("\\.")[0]) >= 5) 
		{
			androidWebCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.ANDROID_UIAUTOMATOR2);
		}
		
		return androidWebCapabilities;
	}

	private DesiredCapabilities iOSWeb() 
	{
		DesiredCapabilities iOSWebCapabilities = new DesiredCapabilities();
		iOSWebCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
		iOSWebCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "iPhone");
		iOSWebCapabilities.setCapability(MobileCapabilityType.BROWSER_NAME, RunTimeContext.getInstance().getProperty("BROWSER_TYPE"));
		iOSWebCapabilities.setCapability(MobileCapabilityType.TAKES_SCREENSHOT, true);
		iOSWebCapabilities.setCapability(MobileCapabilityType.UDID, TestingDevice.getDeviceUDID());

		// Use IOS_XCUI_TEST for OS system greater than 10.0
		String version = TestingDevice.get().getOSVersion();
		if (Integer.parseInt(version.split("\\.")[0]) >= 10)
		{
			iOSWebCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.IOS_XCUI_TEST);
			try 
			{
				iOSWebCapabilities.setCapability(IOSMobileCapabilityType.WDA_LOCAL_PORT, AvailablePorts.get());
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}

		return iOSWebCapabilities;
	}

	private DesiredCapabilities iosNative()
	{
		DesiredCapabilities iOSCapabilities = new DesiredCapabilities();
		System.out.println("Setting iOS Desired Capabilities:");
		iOSCapabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "iOS");
		iOSCapabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, TestingDevice.get().getOSVersion());
		iOSCapabilities.setCapability(IOSMobileCapabilityType.AUTO_ACCEPT_ALERTS, true);
		iOSCapabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "iPhone");

		System.out.println(TestingDevice.getDeviceUDID() + Thread.currentThread().getId());
		iOSCapabilities.setCapability(MobileCapabilityType.UDID, TestingDevice.getDeviceUDID());
		iOSCapabilities.setCapability(MobileCapabilityType.CLEAR_SYSTEM_FILES, true);
		iOSCapabilities.setCapability(IOSMobileCapabilityType.XCODE_ORG_ID, RunTimeContext.getInstance().getProperty("XCODE_ORG_ID", ""));
		iOSCapabilities.setCapability(IOSMobileCapabilityType.XCODE_SIGNING_ID, RunTimeContext.getInstance().getProperty("XCODE_SIGNING_ID", "iPhone Developer"));

		// If new app installation is required
		Boolean status = appInstallationStatus.get(TestingDevice.getDeviceUDID());
		if(status == null || status == Boolean.FALSE)
		{
			if(TestingDevice.get().isRealDevice())
			{
				// Check ipa path in config
				String appPath = RunTimeContext.getInstance().getProperty("IOS_APP_IPA_PATH");
				if(appPath == null || appPath.isEmpty())
				{
					throw new IllegalArgumentException("Variable [IOS_APP_IPA_PATH] is not provided.");
				}

				// Set App path
				Path path = FileSystems.getDefault().getPath(appPath);
				if (!path.isAbsolute())
				{
					iOSCapabilities.setCapability(MobileCapabilityType.APP, path.normalize().toAbsolutePath().toString());
				}
				else 
				{
					iOSCapabilities.setCapability(MobileCapabilityType.APP, appPath);
				}
			}
			else
			{
				// Check app path in config
				String appPath = RunTimeContext.getInstance().getProperty("IOS_APP_PATH");
				if(appPath == null || appPath.isEmpty())
				{
					throw new IllegalArgumentException("Variable [IOS_APP_PATH] is not provided.");
				}

				// Set App path
				Path path = FileSystems.getDefault().getPath(appPath);
				if (!path.getParent().isAbsolute())
				{
					iOSCapabilities.setCapability(MobileCapabilityType.APP, path.normalize().toAbsolutePath().toString());
				}
				else 
				{
					iOSCapabilities.setCapability(MobileCapabilityType.APP, appPath);
				}
			}

			iOSCapabilities.setCapability(MobileCapabilityType.NO_RESET, false);

			// Set installation status to true -> no installation for next test case
			appInstallationStatus.put(TestingDevice.getDeviceUDID(), Boolean.TRUE);
		}
		else
		{
			iOSCapabilities.setCapability(MobileCapabilityType.APP, RunTimeContext.getInstance().getProperty("BUNDLE_ID"));
			iOSCapabilities.setCapability(MobileCapabilityType.NO_RESET, true);
		}

		// Use IOS_XCUI_TEST for OS system greater than 10.0
		String version = TestingDevice.get().getOSVersion();
		if (Integer.parseInt(version.split("\\.")[0]) >= 10) 
		{
			iOSCapabilities.setCapability(MobileCapabilityType.AUTOMATION_NAME, AutomationName.IOS_XCUI_TEST);
			try 
			{
				iOSCapabilities.setCapability(IOSMobileCapabilityType.WDA_LOCAL_PORT, AvailablePorts.get());
			} 
			catch (Exception e) 
			{
				e.printStackTrace();
			}
		}

		return iOSCapabilities;
	}
}
