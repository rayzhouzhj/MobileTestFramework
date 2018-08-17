package com.rayzhou.framework.device.ios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.device.AbstractTestDevice;
import com.rayzhou.framework.utils.CommandPrompt;
import com.rayzhou.framework.utils.FigletWriter;

public class IOSDeviceManager 
{
	private CommandPrompt commandPrompt = new CommandPrompt();
	private Map<String, IOSDevice> iosDeviceMap = new ConcurrentHashMap<>();
	private List<String> validDeviceIds = new ArrayList<>();

	private static IOSDeviceManager instance = new IOSDeviceManager();

	private IOSDeviceManager()
	{
	}

	public static IOSDeviceManager getInstance()
	{
		return instance;
	}

	public synchronized Map<String, IOSDevice> getIOSDevices() 
	{
		// If the android device map is already initialized, return it directly
		if(this.iosDeviceMap.size() > 0)
		{
			return this.iosDeviceMap;
		}

		// Handle Simulators
		List<IOSSimulator> simulators = IOSSimulatorManager.getAllSimulators();
		simulators.forEach(simulator ->
		{
			if (validDeviceIds.size() > 0) 
			{
				if (validDeviceIds.contains(simulator.getUDID())) 
				{
					iosDeviceMap.put(simulator.getUDID(), simulator);
				}
			} 
			else 
			{
				if (simulator.isBooted()) 
				{
					iosDeviceMap.put(simulator.getUDID(), simulator);
				}
			}
		});

		// For real device
		try 
		{
			String iOSDeviceIDs = commandPrompt.runCommand("idevice_id -l");
			// If no real device connected
			if (iOSDeviceIDs == null || iOSDeviceIDs.isEmpty()) 
			{
				if(validDeviceIds.size() > 0)
				{
					System.out.println("No real IOS device is connected, use simulators for testing.");
					return iosDeviceMap;
				}
				else
				{
					FigletWriter.print("No IOS Devices Connected");
					System.out.println("===========================================");
					System.out.println("None of below IOS devices is connected:    ");
					System.out.println("===========================================");

					validDeviceIds.forEach(id -> System.out.println(id));

					System.out.println("===========================================");
					throw new IllegalArgumentException("No connected IOS devices.");
				}
			} 
			else 
			{
				String[] deviceIDs = iOSDeviceIDs.split("\n");
				String deviceUDID = "";
				for (int i = 0; i < deviceIDs.length; i++) 
				{
					deviceUDID = deviceIDs[i];
					if (validDeviceIds.size() > 0) 
					{
						if (validDeviceIds.contains(deviceUDID)) 
						{
							iosDeviceMap.put(deviceUDID, new IOSDevice(deviceUDID));
						}
					} 
					else 
					{
						iosDeviceMap.put(deviceUDID, new IOSDevice(deviceUDID));
					}
				}

				return iosDeviceMap;
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			throw new IllegalStateException("Failed to fetch iOS device connected");
		}
	}

	public void checkExecutePermissionForIOSDebugProxyLauncher() throws IOException 
	{
		String serverPath = RunTimeContext.getInstance().getProperty("APPIUM_JS_PATH");
		File file = new File(serverPath);
		File currentPath = new File(file.getParent());
		System.out.println(currentPath);
		file = new File(currentPath + "/.." + "/..");
		File executePermission = new File(file.getCanonicalPath() + "/bin/ios-webkit-debug-proxy-launcher.js");

		if (executePermission.exists()) 
		{
			if (executePermission.canExecute() == false) 
			{
				executePermission.setExecutable(true);
				System.out.println("Access Granted for iOSWebKitProxyLauncher");
			} 
			else 
			{
				System.out.println("iOSWebKitProxyLauncher File already has access to execute");
			}
		}
	}

	public void setValidDevices(List<String> deviceID)
	{
		deviceID.forEach(deviceList -> 
		{
			validDeviceIds.add(deviceList);
		});
	}

	public void setDevices(List<AbstractTestDevice> devices) 
	{
		iosDeviceMap.clear();

		devices.forEach(device -> 
		{
			iosDeviceMap.put(device.getUDID(), (IOSDevice) device);
		});
	}
}
