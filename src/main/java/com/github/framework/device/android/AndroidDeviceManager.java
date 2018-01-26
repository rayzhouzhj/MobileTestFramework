package com.github.framework.device.android;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.framework.device.AbstractTestDevice;
import com.github.framework.utils.CommandPrompt;
import com.github.framework.utils.FigletWriter;

public class AndroidDeviceManager 
{
	private static AndroidDeviceManager instance = new AndroidDeviceManager();
	private CommandPrompt cmd = new CommandPrompt();
	private Map<String, AndroidDevice> androidDevices = new ConcurrentHashMap<>();
	private List<String> specifiedDevices = new ArrayList<>();

	private AndroidDeviceManager(){}

	public static AndroidDeviceManager getInstance()
	{
		return instance;
	}

	private void startADB()
	{
		String output = cmd.runCommand("adb start-server");
		String[] lines = output.split("\n");
		if (lines[0].contains("internal or external command")) 
		{
			System.out.println("Please set ANDROID_HOME in your system variables");
		}
	}

	/**
	 * This method stop adb server
	 */
	private void stopADB()
	{
		cmd.runCommand("adb kill-server");
	}

	/**
	 * This method return connected android devices
	 * @return hashmap of connected devices information
	 */
	public synchronized Map<String, AndroidDevice> getAndroidDevices()
	{
		// If the android device map is already initialized, return it directly
		if(this.androidDevices.size() > 0)
		{
			return this.androidDevices;
		}

		// If android map is not empty, query the device list
		startADB();
		String output = cmd.runCommand("adb devices");
		String[] lines = output.split("\n");

		if (lines.length <= 1)
		{
			stopADB();
		} 
		else 
		{
			for (int i = 1; i < lines.length; i++) 
			{
				lines[i] = lines[i].replaceAll("\\s+", "");

				if (lines[i].contains("device")) 
				{
					lines[i] = lines[i].replaceAll("device", "");
					String deviceID = lines[i];

					if (specifiedDevices.size() > 0) 
					{
						if (specifiedDevices.contains(deviceID)) 
						{
							this.androidDevices.put(deviceID, new AndroidDevice(deviceID));
						}
					} 
					else
					{
						this.androidDevices.put(deviceID, new AndroidDevice(deviceID));
					}

				}
			}
		}

		if(this.androidDevices.size() == 0)
		{
			FigletWriter.print("No Android Devices Connected");
			throw new IllegalArgumentException("No connected Android devices.");
		}

		return this.androidDevices;
	}

	public void setSpecifiedDevices(List<String> devices) 
	{
		devices.forEach(deviceList -> 
		{
			specifiedDevices.add(deviceList);
		});
	}
	
	public void setDevices(List<AbstractTestDevice> devices) 
	{
		androidDevices.clear();
		
		devices.forEach(device -> 
		{
			androidDevices.put(device.getUDID(), (AndroidDevice) device);
		});
	}
}
