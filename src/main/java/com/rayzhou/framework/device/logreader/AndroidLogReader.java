package com.rayzhou.framework.device.logreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.device.AbstractTestDevice;
import com.rayzhou.framework.utils.ADBShell;
import com.rayzhou.framework.utils.CommandPrompt;
import com.rayzhou.framework.utils.TaskManager;

public class AndroidLogReader implements IDeviceLogReader
{
	private static ConcurrentHashMap<String, AndroidLogReader> instance = new ConcurrentHashMap<>();

	private String appPackageName = "";
	private String appProcessID = "";
	private String trackingKeyword = "";
	private ConcurrentLinkedQueue<String> appLogs = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<String> appLogQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<String> trackingLogQueue = new ConcurrentLinkedQueue<>();
	private AbstractTestDevice device;
	private Process logProcess;
	private boolean stopProcess = true;

	private AndroidLogReader(AbstractTestDevice device)
	{
		this.device = device;
		this.appPackageName = RunTimeContext.getInstance().getProperty("APP_PACKAGE");
		this.trackingKeyword = RunTimeContext.getInstance().getProperty("ANDROID_APP_TRACKING_FOR_FILTER");
	}

	public static AndroidLogReader get(AbstractTestDevice device)
	{
		if(!instance.containsKey(device.getUDID()))
		{
			instance.put(device.getUDID(), new AndroidLogReader(device));
		}

		return instance.get(device.getUDID());
	}

	private String getCommand()
	{
		return "adb -s " + this.device.getUDID() + " logcat -v threadtime";
	}

	public void updateAppProcessID()
	{
		this.appProcessID = ADBShell.findProcess(this.device.getUDID(), this.appPackageName);
	}

	@Override
	public void startCaptureLog()
	{
		// Update App process
		this.updateAppProcessID();

		if(stopProcess)
		{
			new Thread(() -> 
			{
				this.captureLogProcess();
			}).start();

			stopProcess = false;
		}
		else
		{
			System.out.println("[AndroidLogReader] Android logger is already running on [" + this.device.getUDID() + "]");
		}

		// Clear logs
		this.appLogs.clear();
		this.appLogQueue.clear();
		this.trackingLogQueue.clear();
	}

	@Override
	public void stopCaptureLog()
	{
		stopProcess = true;

		List<String> processes = TaskManager.findProcess(getCommand());

		System.out.println();
		processes.forEach(process ->
		{
			TaskManager.killProcess(process);
		});
	}

	private void clearLogcat()
	{
		new CommandPrompt().runCommand("adb -s " + this.device.getUDID() + " logcat -c");
	}

	private void captureLogProcess()
	{
		try 
		{
			this.clearLogcat();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			logProcess = new CommandPrompt().runProcess(getCommand());

			// get std output
			BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
			String line = reader.readLine();

			while(line != null)
			{
				// Filter app for testing app only
				if(line.contains(this.appProcessID))
				{
					this.appLogs.add(line);
					this.appLogQueue.add(line);

					// Filter tracking app
					if(this.trackingKeyword != null && line.contains(this.trackingKeyword))
					{
						this.trackingLogQueue.add(line);
					}
				}

				if(stopProcess)
				{
					break;
				}

				line = reader.readLine();
			}

			reader.close();
			logProcess.destroy();

			System.out.println("Stopped log capturing process");
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}
	}

	@Override
	public List<String> getTrackingLogs()
	{
		List<String> result = new ArrayList<>();
		while(!trackingLogQueue.isEmpty())
		{
			result.add(trackingLogQueue.poll());
		}

		return result;
	}

	@Override
	public List<String> getFullLogs()
	{
		List<String> list = new ArrayList<>();

		while(!this.appLogs.isEmpty())
		{
			list.add(this.appLogs.poll());
		}

		return list;
	}

	@Override
	public List<String> getFullLogsFromQueue()
	{
		List<String> list = new ArrayList<>();

		while(!this.appLogQueue.isEmpty())
		{
			list.add(this.appLogQueue.poll());
		}

		return list;
	}

	public static void main(String[] arg)
	{
		//		AndroidLogReader reader = AndroidLogReader.get("db2f5f0385c8637512caf056c8de78a663c8d2c7");
		//		reader.startCaptureLog();
		//
		//		new Thread(() -> 
		//		{
		//			while(!reader.stopProcess)
		//			{
		//				if(reader.trackingLogQueue.isEmpty())
		//				{
		//					try {
		//						Thread.sleep(1000);
		//					} catch (InterruptedException e) {
		//						// TODO Auto-generated catch block
		//						e.printStackTrace();
		//					}
		//				}
		//				else
		//				{
		//					System.out.println(reader.trackingLogQueue.poll());
		//				}
		//			}
		//		}).start();
		//
		//		reader.stopCaptureLog();

		System.out.println();
	}
}
