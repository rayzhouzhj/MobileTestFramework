package com.github.framework.device.logreader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.github.framework.context.RunTimeContext;
import com.github.framework.device.AbstractTestDevice;
import com.github.framework.utils.CommandPrompt;
import com.github.framework.utils.TaskManager;

public class IOSLogReader implements IDeviceLogReader
{
	private static ConcurrentHashMap<String, IOSLogReader> instance = new ConcurrentHashMap<>();

	private String appName = "";
	private String trackingKeyword = "";
	private String deviceID = "";
	private ConcurrentLinkedQueue<String> appLogs = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<String> appLogQueue = new ConcurrentLinkedQueue<>();
	private ConcurrentLinkedQueue<String> trackingLogQueue = new ConcurrentLinkedQueue<>();
	private Process logProcess;
	private boolean stopProcess = true;
	private AbstractTestDevice device;

	private IOSLogReader(AbstractTestDevice device)
	{
		this.device = device;
		this.deviceID = device.getUDID();
		this.appName = RunTimeContext.getInstance().getProperty("IOS_APP_NAME_FOR_FILTER");
		this.trackingKeyword = RunTimeContext.getInstance().getProperty("IOS_APP_TRACKING_FOR_FILTER");
	}

	public static IOSLogReader get(AbstractTestDevice device)
	{
		if(!instance.containsKey(device.getUDID()))
		{
			instance.put(device.getUDID(), new IOSLogReader(device));
		}

		return instance.get(device.getUDID());
	}

	private String getCommand()
	{
		if(device.isRealDevice())
		{
			return "idevicesyslog --udid " + deviceID;
		}
		else
		{
			return "xcrun simctl spawn " + deviceID + " log stream";
		}
	}

	@Override
	public void startCaptureLog()
	{
		if(stopProcess)
		{
			new Thread(() -> 
			{
				this.captureLogProcess();
			}).start();

			stopProcess = false;
		}

		System.out.println("[IOSLogReader] IOS logger is already running on [" + "deviceID" + "]");

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

	private void captureLogProcess()
	{
		try 
		{
			logProcess = new CommandPrompt().runProcess(getCommand());

			// get std output
			BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream()));
			String line = reader.readLine();

			while(line != null)
			{
				if(line.contains(this.appName))
				{
					if(line.contains(this.trackingKeyword))
					{
						String trackingLine = line;

						while(!trackingLine.endsWith(")"))
						{
							// Loop to capture the details of current log entry
							trackingLine += reader.readLine();
						}
						
						this.appLogs.add(trackingLine);
						this.appLogQueue.add(trackingLine);
						this.trackingLogQueue.add(trackingLine);
					}
					else
					{
						this.appLogs.add(line);
						this.appLogQueue.add(line);
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
		//		IOSLogReader reader = IOSLogReader.get("db2f5f0385c8637512caf056c8de78a663c8d2c7");
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
