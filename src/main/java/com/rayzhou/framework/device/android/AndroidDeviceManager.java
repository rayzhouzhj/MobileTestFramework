package com.rayzhou.framework.device.android;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.rayzhou.framework.context.TestingDevice;
import com.rayzhou.framework.device.AbstractTestDevice;
import com.rayzhou.framework.utils.CommandPrompt;
import com.rayzhou.framework.utils.FigletWriter;

public class AndroidDeviceManager 
{
	private static AndroidDeviceManager instance = new AndroidDeviceManager();
	private CommandPrompt cmd = new CommandPrompt();
	private Map<String, AndroidDevice> androidDevices = new ConcurrentHashMap<>();
	private List<String> validDeviceIds = new ArrayList<>();

	private AndroidDeviceManager(){}

	public static AndroidDeviceManager getInstance()
	{
		return instance;
	}

	/**
	 * This method start adb server
	 */
	public void startADB()
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
	public void stopADB()
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

		// If andoid map is not initialized, query the device list
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

					if (validDeviceIds.size() > 0) 
					{
						if (validDeviceIds.contains(deviceID)) 
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
			System.out.println("===========================================");
			System.out.println("None of below android devices is connected:");
			System.out.println("===========================================");
			
			validDeviceIds.forEach(id -> System.out.println(id));
			
			System.out.println("===========================================");
			
			throw new IllegalArgumentException("No connected Android devices.");
		}

		return this.androidDevices;
	}

	/**
	 * This method will close the running app
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void closeRunningApp(String deviceID, String app_package) throws InterruptedException, IOException 
	{
		cmd.runCommand("adb -s " + deviceID + " shell am force-stop " + app_package);
	}

	/**
	 * This method clears the app data only for android
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void clearAppData(String deviceID, String app_package) throws InterruptedException, IOException 
	{
		cmd.runCommand("adb -s " + deviceID + " shell pm clear " + app_package);
	}

	/**
	 * This method removes apk from the devices attached
	 *
	 * @param app_package
	 * @throws Exception
	 */

	public void removeApkFromDevices(String deviceID, String app_package) throws Exception 
	{
		cmd.runCommand("adb -s " + deviceID + " uninstall " + app_package);
	}

	public Process screenRecord(String fileName)
	{
		String command = "adb -s " + TestingDevice.getDeviceUDID()
		+ " shell screenrecord --bit-rate 3000000 /sdcard/" + fileName
		+ ".mp4";

		return cmd.runProcess(command);
	}

	public static void pullVideoFromDevice(String deviceID, String fileName, String destination) throws IOException, InterruptedException 
	{
		ProcessBuilder pb = new ProcessBuilder("adb", "-s", deviceID, "pull", "/sdcard/" + fileName + ".mp4", destination);
		Process pc = pb.start();
		pc.waitFor();
		System.out.println("Exited with Code::" + pc.exitValue());
		System.out.println("Done");
		Thread.sleep(5000);
	}

	public static void removeVideoFileFromDevice(String deviceID, String fileName) throws IOException, InterruptedException 
	{
		new CommandPrompt().runCommand("adb -s " + deviceID + " shell rm -f /sdcard/" + fileName + ".mp4");
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
		androidDevices.clear();
		
		devices.forEach(device -> 
		{
			androidDevices.put(device.getUDID(), (AndroidDevice) device);
		});
	}
}
