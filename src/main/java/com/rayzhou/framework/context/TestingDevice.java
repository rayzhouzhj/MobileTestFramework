package com.rayzhou.framework.context;

import com.rayzhou.framework.device.AbstractTestDevice;
import com.rayzhou.framework.device.MobilePlatform;
import com.rayzhou.framework.device.android.AndroidDeviceManager;
import com.rayzhou.framework.device.ios.IOSDeviceManager;
import com.rayzhou.framework.device.logreader.IDeviceLogReader;
import com.rayzhou.framework.testng.model.TestInfo;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

public class TestingDevice
{
	private static ThreadLocal<AbstractTestDevice> device = new ThreadLocal<>();
	
	public static String getDeviceUDID()
	{
		return device.get().getUDID();
	}

	public static AbstractTestDevice get()
	{
		return device.get();
	}

	public static AppiumDriver<MobileElement> getDriver() 
	{
		return device.get().getDriver();
	}
	
	public static IDeviceLogReader getLogReader() 
	{
		return device.get().getLogReader();
	}
	
	public static boolean isConnected() 
	{
		return device.get().isConnected();
	}
	
	public static boolean isRealDevice() 
	{
		return device.get().isRealDevice();
	}
	
	public static String getDeviceName()
	{
		return device.get().getDeviceName();
	}
	
	public static String getOSVersion()
	{
		return device.get().getOSVersion();
	}
	
	public static Integer getDeviceHight()
	{
		return device.get().getDeviceHight();
	}

	public static Integer getDeviceWidth() 
	{
		return device.get().getDeviceWidth();
	}
	
	public static MobilePlatform getMobilePlatform() 
	{
		return device.get().getPlatform();
	}
	
	public static void assignDeviceAndTask(String UDID, TestInfo testInfo)
	{
		if(RunTimeContext.isAndroidPlatform())
		{
			device.set(AndroidDeviceManager.getInstance().getAndroidDevices().get(UDID));
		}
		else
		{
			device.set(IOSDeviceManager.getInstance().getIOSDevices().get(UDID));
		}
		
		device.get().assignTask(testInfo);
	}
}
