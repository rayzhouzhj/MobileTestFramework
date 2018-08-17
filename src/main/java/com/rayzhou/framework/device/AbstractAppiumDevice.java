package com.rayzhou.framework.device;

import java.util.concurrent.atomic.AtomicBoolean;

import com.rayzhou.framework.appium.AppiumDriverManager;
import com.rayzhou.framework.appium.AppiumServerManager;
import com.rayzhou.framework.testng.model.TestInfo;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import io.appium.java_client.service.local.AppiumDriverLocalService;

public abstract class AbstractAppiumDevice 
{
	protected TestInfo testInfo;
	protected AtomicBoolean isBusy = new AtomicBoolean(false);

	protected AppiumServerManager appiumServerManager;
	protected AppiumDriverManager appiumDriverManager;
	protected AppiumDriver<MobileElement> appiumDriver;
	protected AppiumDriverLocalService appiumServer;

	public AbstractAppiumDevice()
	{
		appiumServerManager = new AppiumServerManager();
		appiumDriverManager = new AppiumDriverManager();
	}

	public boolean isBusy() 
	{
		return isBusy.get();
	}

	public void assignTask(TestInfo testInfo) 
	{
		this.testInfo = testInfo;
		this.isBusy.set(true);
	}

	public void freeDevice()
	{
		this.testInfo = null;
		this.isBusy.set(false);
	}

	public AppiumDriver<MobileElement> getDriver()
	{
		return appiumDriver;
	}

	public abstract void setupAppiumForTest() throws Exception;

	public void shutdownAppium()
	{
		this.stopAppiumDriver();
		this.stopAppiumServer();
	}
	
	public abstract void startAppiumServer() throws Exception;

	public abstract void stopAppiumServer();

	public void startAppiumDriver()
	{
		// Start Appium Driver
		appiumDriver = appiumDriverManager.startAppiumDriverInstance(appiumServer, testInfo.needReInstallApp());
	}
	
	public void stopAppiumDriver()
	{
		if(this.appiumDriver != null)
		{
			this.appiumDriver.quit();
		}

		this.appiumDriver = null;
	}
}
