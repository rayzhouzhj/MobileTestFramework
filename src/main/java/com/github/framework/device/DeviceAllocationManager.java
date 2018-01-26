package com.github.framework.device;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.framework.context.RunTimeContext;
import com.github.framework.context.TestingDevice;
import com.github.framework.device.android.AndroidDeviceManager;
import com.github.framework.device.ios.IOSDeviceManager;
import com.github.framework.testng.model.TestInfo;

/**
 *  DeviceAllocationManager - Handles device initialization, allocation and de-allocation
 */
public class DeviceAllocationManager 
{
	private AndroidDeviceManager androidDeviceManager;
	private IOSDeviceManager iOSDeviceManager;

	private static DeviceAllocationManager instance;

	private DeviceAllocationManager() 
	{
		iOSDeviceManager = IOSDeviceManager.getInstance();
		androidDeviceManager = AndroidDeviceManager.getInstance();
	}

	public synchronized static DeviceAllocationManager getInstance() 
	{
		if (instance == null) 
		{
			instance = new DeviceAllocationManager();
		}

		return instance;
	}

	public void initializeDevices() 
	{
		if (RunTimeContext.currentPlatform() == MobilePlatform.UNDEFINED) 
		{
			throw new IllegalArgumentException("Please update config 'PLATFORM' with android/ios "
					+ "or execute with Platform environment,"
					+ "=> Platform=android/ios mvn clean -Dtest=Runner test");
		}

		if (RunTimeContext.isIOSPlatform()) 
		{
			this.iOSDeviceManager.getIOSDevices();
		} 
		else 
		{
			this.androidDeviceManager.getAndroidDevices();
		}
	}

	public AndroidDeviceManager getAndroidDeviceManager()
	{
		return androidDeviceManager;
	}

	public IOSDeviceManager getIOSDeviceManager()
	{
		return iOSDeviceManager;
	}

	public List<AbstractTestDevice> getDevices()
	{
		if (RunTimeContext.isIOSPlatform()) 
		{
			return this.iOSDeviceManager.getIOSDevices().values().stream().collect(Collectors.toList());
		} 
		else 
		{
			return this.androidDeviceManager.getAndroidDevices().values().stream().collect(Collectors.toList());
		}
	}
	
	public void setDevices(List<AbstractTestDevice> device) 
	{
		if(RunTimeContext.isAndroidPlatform())
		{
			androidDeviceManager.setDevices(device);
		}
		else
		{
			iOSDeviceManager.setDevices(device);
		}
	}
	
	public void setSpecifiedDevices(List<String> deviceID) 
	{
		if(RunTimeContext.isAndroidPlatform())
		{
			androidDeviceManager.setSpecifiedDevices(deviceID);
		}
		else if(RunTimeContext.isIOSPlatform())
		{
			iOSDeviceManager.setSpecifiedDevices(deviceID);
		}
		else
		{
			throw new IllegalArgumentException("Unexpected environment variable Platform = [" + RunTimeContext.currentPlatform() + "]");
		}
	}

	public Set<String> getDeviceUDIDs() 
	{
		if(RunTimeContext.isAndroidPlatform())
		{
			return androidDeviceManager.getAndroidDevices().keySet();
		}
		else if(RunTimeContext.isIOSPlatform())
		{
			return iOSDeviceManager.getIOSDevices().keySet();
		}
		else
		{
			throw new IllegalArgumentException("Unexpected environment variable Platform = [" + RunTimeContext.currentPlatform() + "]");
		}
	}

	public synchronized AbstractTestDevice getNextAvailableDevice() 
	{
		List<AbstractTestDevice> devices = getDevices();

		for (AbstractTestDevice device : devices)
		{
			if (!device.isBusy())
			{
				return device;
			}
		}

		System.out.println("[INFO] No Available Devices, wait for 15 seconds and retry...");
		try 
		{
			Thread.sleep(15000);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}

		return getNextAvailableDevice();
	}

	public void freeDevice() 
	{
		TestingDevice.get().freeDevice();
	}

	/**
	 * Allocate device
	 * If deviceid is specified, then use the specified id
	 * else use allocate next available id
	 * @param device
	 * @param testInfo
	 */
	public void allocateDeviceAndTask(String device, TestInfo testInfo) 
	{
		if (device.isEmpty())
		{
			TestingDevice.assignDeviceAndTask(this.getNextAvailableDevice().getUDID(), testInfo);
		}
		else
		{
			TestingDevice.assignDeviceAndTask(device, testInfo);
		}
	}
}
