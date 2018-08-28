package com.rayzhou.framework.context;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.rayzhou.framework.device.MobilePlatform;
import com.rayzhou.framework.utils.ConfigFileReader;

public class RunTimeContext 
{
	private static RunTimeContext instance;
	private List<String> packages;
	private Set<String> taskSet;
	
	private RunTimeContext()
	{
		taskSet = new HashSet<>();
		packages = new ArrayList<>();
	} 
	
	public static synchronized RunTimeContext getInstance() 
	{
		if (instance == null) 
		{
			instance = new RunTimeContext();
		}
		
		return instance;
	}
	
	public void setPackages(List<String> packages)
	{
		this.packages = packages;
	}
	
	public List<String> getTestcasePackages()
	{
		return this.packages;
	}
	
	public boolean isAllTaskDone()
	{
		System.out.println("[RunTimeContext] Pending tasks in background:");
		taskSet.forEach(task -> System.out.println(task));
		System.out.println("*********************************");
		
		return taskSet.isEmpty();
	}
	
	public void addTask(String task)
	{
		taskSet.add(task);
	}
	
	public void removeTask(String task)
	{
		taskSet.remove(task);
	}
	
	public boolean isParallelExecution()
	{
		return "parallel".equalsIgnoreCase(this.getProperty("RUNNER", "").trim());
	}
	
	public static MobilePlatform currentPlatform()
	{
		String platform = RunTimeContext.getInstance().getProperty("PLATFORM");
		
		if(platform.equalsIgnoreCase("ANDROID"))
		{
			return MobilePlatform.ANDROID;
		}
		else if(platform.equalsIgnoreCase("IOS"))
		{
			return MobilePlatform.IOS;
		}
		else
		{
			return MobilePlatform.UNDEFINED;
		}
	}
	
	public static boolean isAndroidPlatform()
	{
		return MobilePlatform.ANDROID == currentPlatform();
	}
	
	public static boolean isIOSPlatform()
	{
		return MobilePlatform.IOS == currentPlatform();
	} 
	
	public boolean isVideoLogEnabled()
	{
		return this.getProperty("VIDEO_LOGS", "ON").equalsIgnoreCase("ON") || getProperty("VIDEO_LOGS", "ON").equalsIgnoreCase("TRUE");
	} 
	
	public String getProperty(String name) 
	{
		return this.getProperty(name, null);
	}

	public String getProperty(String key, String defaultValue) 
	{
		String value = System.getenv(key);
		if(value == null || value.isEmpty())
		{
			value = ConfigFileReader.getInstance().getProperty(key, defaultValue);
		}
		
		return value;
	}
	
	public synchronized String getLogPath(String category, String className, String methodName)
	{
		String path = System.getProperty("user.dir") 
		+ File.separator + "target"
		+ File.separator + category 
		+ File.separator + TestingDevice.getDeviceUDID() 
		+ File.separator + className 
		+ File.separator + methodName;
		
		File file = new File(path);
		if (!file.exists()) 
		{
			if (file.mkdirs()) 
			{
				System.out.println("Directory [" + path + "] is created!");
			} 
			else
			{
				System.out.println("Failed to create directory!");
			}
		}
		
		return path;
	}

	public static File getApp() 
	{
		String filePath = "";
		
		if(RunTimeContext.isAndroidPlatform())
		{
			filePath = RunTimeContext.getInstance().getProperty("ANDROID_APP_PATH");
		}
		else
		{
			filePath = RunTimeContext.getInstance().getProperty("IOS_APP_PATH");
		}
		
		return new File(filePath);
	}
	
    public static String currentDateAndTime()
    {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        return now.truncatedTo(ChronoUnit.SECONDS).format(dtf).replace(":", "-");
    }
}
