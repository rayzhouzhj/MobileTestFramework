package com.rayzhou.framework.appium;

import java.util.concurrent.TimeUnit;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.utils.ADBShell;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.service.local.AppiumDriverLocalService;

public class AppiumDriverManager 
{
	private DesiredCapabilityBuilder desiredCapabilityBuilder;
	private AppiumDriverLocalService appiumServer;
	
	public AppiumDriverManager()
	{
		desiredCapabilityBuilder = new DesiredCapabilityBuilder();
	}

	private AppiumDriver<MobileElement> createAppiumDriverForWebApp()
	{
		AppiumDriver<MobileElement> appiumDriver;
		
		// For android web
		if(RunTimeContext.getInstance().getProperty("Platform").equalsIgnoreCase("Android"))
		{
			appiumDriver = new AndroidDriver<>(
					this.appiumServer.getUrl(), 
					desiredCapabilityBuilder.build(DesiredCapability.ANDROID_WEB));
		}
		// For IOS web
		else
		{
			appiumDriver = new IOSDriver<>(
					this.appiumServer.getUrl(), 
					desiredCapabilityBuilder.build(DesiredCapability.IOS_WEB));
		}
		
		appiumDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
		
		return appiumDriver;
	}
	
	private AppiumDriver<MobileElement> createAppiumDriverForNativeApp()
	{
		AppiumDriver<MobileElement> appiumDriver;
		
		// For IOS
		if (RunTimeContext.isIOSPlatform()) 
		{
			appiumDriver = new IOSDriver<>(
					this.appiumServer.getUrl(), 
					desiredCapabilityBuilder.build(DesiredCapability.IOS_NATIVE));
		} 
		// For Android
		else 
		{
			// Activate screen before execution if needed
			ADBShell.activateScreen(TestingDevice.getDeviceUDID());

			appiumDriver = new AndroidDriver<>(
					this.appiumServer.getUrl(), 
					desiredCapabilityBuilder.build(DesiredCapability.ANDROID_NATIVE));

			// Activate screen before execution if needed
			ADBShell.setScreenTimeout(TestingDevice.getDeviceUDID(), 10);
		}
		
		appiumDriver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
		
		return appiumDriver;
	}
	
	private AppiumDriver<MobileElement> startAppiumDriverInstance()
	{
		// For Web App
		if (RunTimeContext.getInstance().getProperty("APP_TYPE").equalsIgnoreCase("web")) 
		{
			return this.createAppiumDriverForWebApp();
		}
		else 
		{
			return this.createAppiumDriverForNativeApp();
		}
	}

	public AppiumDriver<MobileElement> startAppiumDriverInstance(AppiumDriverLocalService service, boolean reInstallApp)
	{
		this.appiumServer = service;
		
		if(reInstallApp)
		{
			this.desiredCapabilityBuilder.setDeviceInstallationStatus(false);
		}
		else
		{
			this.desiredCapabilityBuilder.setDeviceInstallationStatus(true);
		}

		AppiumDriver<MobileElement> appiumDriver = startAppiumDriverInstance();
		
		// Update device size
		TestingDevice.get().updateDeviceSize(appiumDriver);
		
		return appiumDriver;
	}
}
