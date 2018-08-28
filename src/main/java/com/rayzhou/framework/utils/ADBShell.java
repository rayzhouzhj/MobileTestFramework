package com.rayzhou.framework.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ADBShell 
{
	public static boolean isScreenDisplayOff(String deviceID)
	{
		String[] output = getScreenDisplayStatus(deviceID);
		if(output.length == 2 && output[1].contains("false"))
		{
			return true;
		}
		else
		{
			return false;
		}

	}
	
	public static void swipe(int startX, int startY, int endX, int endY, int millis)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand(
				"adb",
				"shell",
				"input",
				"swipe",
				"" + startX,
				"" + startY,
				"" + endX,
				"" + endY,
				"" + millis);
	}
	
	public static String[] getScreenDisplayStatus(String deviceID)
	{
		CommandPrompt cmd = new CommandPrompt();
		String output = "";

		String os = System.getProperty("os.name");

		if(os.contains("Windows")) // if windows
		{
			output = cmd.runCommand("adb -s " + deviceID + " shell dumpsys power | find \"mHolding\"");
		}
		else 
		{// If Mac
			output = cmd.runCommand("adb -s " + deviceID + " shell dumpsys power | grep \"mHolding\"");
		}

		System.out.println(output);


		return output.split("\n");
	}

	public static void pressPowerButton(String deviceID)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand("adb -s " + deviceID + " shell input keyevent KEYCODE_POWER");

	}

	public static String[] getDisplaySize(String deviceID)
	{
		CommandPrompt cmd = new CommandPrompt();

		String output = cmd.runCommand("adb -s " + deviceID + " shell wm size");
		System.out.println(output);
		output = output.replace("Physical size:", "").trim();
		return output.split("x");
	}

	public static String getCurrentActivity(String deviceID)
	{
		CommandPrompt cmd = new CommandPrompt();
		String output;

		output = cmd.runCommand("adb -s " + deviceID + " shell dumpsys window windows");
		String[] lines = output.split("\n");
		for(String line : lines)
		{
			if(line.trim().startsWith("mCurrentFocus"))
			{
				System.out.println(line);
				if(line.contains("/"))
				{
					String activityName = line.split("/")[1];
					return activityName.substring(0, activityName.length() - 1);
				}
				else
				{
					return "";
				}
			}
		}

		return "";
	}

	public static boolean setScreenTimeout(String deviceID, long timeoutInMinutes)
	{
		CommandPrompt cmd = new CommandPrompt();

		String output = cmd.runCommand("adb -s " + deviceID + " shell settings put system screen_off_timeout " + timeoutInMinutes * 60 * 1000);

		if(!output.toLowerCase().contains("error"))
		{
			return true;
		}

		return false;
	}

	public static boolean isDeviceConnected(String deviceID)
	{
		CommandPrompt cmd = new CommandPrompt();
		String output = cmd.runCommand("adb devices");
		System.out.println(output);

		if(output.contains(deviceID))
		{
			return true;
		}

		return false;
	}

	public static boolean captureScreen(String deviceID, String fileName, String localPath)
	{
		CommandPrompt cmd = new CommandPrompt();
		try
		{
			// Capture Screen
			cmd.runCommand("adb -s " + deviceID + " shell screencap -p /sdcard/" + fileName);
			// Move it to local folder
			Path folderPath = Paths.get(localPath).toAbsolutePath();
			String output = cmd.runCommand("adb -s " + deviceID + " pull /sdcard/" + fileName + " \"" + folderPath.toString() + File.separator + fileName + "\"");
			System.out.println(output);
			if(output.contains("100%"))
			{
				return true;
			}
		}
		catch(Exception e)
		{
			System.out.println("ERROR: error when capturing screen.");
		}

		return false;
	}

	public static void tap(String deviceID, int x, int y)
	{
		CommandPrompt cmd = new CommandPrompt();
		// Tap on screen to keep it active
		try {
			cmd.runCommand("adb -s " + deviceID + " shell input tap " + x + " " + y);
			Thread.sleep(15000);
		} catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

	public static void activateScreen(String deviceID)
	{
		try
		{
			// Check if the device is black out
			while(ADBShell.isScreenDisplayOff(deviceID))
			{
				System.out.println("Screen is sleeping, try to activate screen...");

				// Press Power Button to activate screen
				ADBShell.pressPowerButton(deviceID);
				ADBShell.setScreenTimeout(deviceID, 10);
				Thread.sleep(3000);
			}

			ADBShell.setScreenTimeout(deviceID, 20);
		} 
		catch (InterruptedException e) 
		{
			e.printStackTrace();
		}
	}

	public static void startApp(String deviceID, String bundleID, String activity)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand("adb -s " + deviceID + " shell am start -n " + bundleID + "/" + activity);
	}

	public static void killApp(String deviceID, String bundleID)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand("adb -s " + deviceID + " shell am force-stop " + bundleID);
	}

	public static void killAppAndClearData(String deviceID, String bundleID)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand("adb -s " + deviceID + " shell pm clear " + bundleID);
	}

	public static void reinstallApp(String deviceID, String appPath)
	{
		CommandPrompt cmd = new CommandPrompt();
		cmd.runCommand("adb -s " + deviceID + " install -r " + appPath);
	}

	public static String getAppVersionName(String deviceID, String bundleID)
	{
		CommandPrompt cmd = new CommandPrompt();
		return cmd.runCommand("adb -s " + deviceID + " shell dumpsys package " + bundleID + " | grep versionName");
	}

	public static String getAppVersionCode(String deviceID, String bundleID)
	{
		CommandPrompt cmd = new CommandPrompt();
		return cmd.runCommand("adb -s " + deviceID + " shell dumpsys package " + bundleID + " | grep versionCode");
	}

	public static String findProcess(String deviceID, String packageName) 
	{
		String processID = "";
		CommandPrompt cmd = new CommandPrompt();
		
		BufferedReader br = null;
		try 
		{
			String command = "adb -s " + deviceID + " shell ps | grep \"" + packageName + "\" | grep -v 'grep' | awk '{print $2}'";
			Process proc = cmd.runProcess(command);

			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			System.out.println("[TaskManager] Query processes with command: " + command);

			while ((line = br.readLine()) != null) 
			{
				System.out.println(line);
				processID = line;
			}
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally 
		{
			if (br != null) 
			{
				try 
				{
					br.close();
				} 
				catch (Exception ex) {}
			}
		}
		
		return processID;
	}
	
	public static void main(String[] arg)
	{
		System.out.println(ADBShell.findProcess("0715f76455ae3036", "com.scmp.news.dev"));
	}
}
