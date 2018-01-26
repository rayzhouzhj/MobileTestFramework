package com.github.framework.context;

import com.github.framework.device.AbstractTestDevice;
import com.github.framework.device.MobilePlatform;
import com.github.framework.device.android.AndroidDeviceManager;
import com.github.framework.device.ios.IOSDeviceManager;
import com.github.framework.testng.model.TestInfo;

public class TestingDevice
{
	private static ThreadLocal<String> deviceUDID = new ThreadLocal<>();
	private static ThreadLocal<AbstractTestDevice> device = new ThreadLocal<>();
	
	public static String getDeviceUDID()
	{
		return deviceUDID.get();
	}

	public static AbstractTestDevice get()
	{
		return device.get();
	}
	
	public static void assignDeviceAndTask(String UDID, TestInfo testInfo)
	{
		deviceUDID.set(UDID);
		
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

	public static MobilePlatform getMobilePlatform() 
	{
		if (RunTimeContext.isIOSPlatform()) 
		{
			return MobilePlatform.IOS;
		}
		else 
		{
			return MobilePlatform.ANDROID;
		}
	}
}
