package com.github.framework.device.android;

import java.io.File;
import java.io.IOException;

import com.github.framework.context.RunTimeContext;
import com.github.framework.device.AbstractTestDevice;
import com.github.framework.device.logreader.AndroidLogReader;
import com.github.framework.device.logreader.IDeviceLogReader;
import com.github.framework.utils.CommandPrompt;
import com.github.framework.utils.TaskManager;

public class AndroidDevice extends AbstractTestDevice
{
	private String model = "";
	private String brand = "";
	private String osVersion = "";
	private String apiLevel = "";
	private String manufacturer = "";
	private Boolean isRecordable = null;
	private CommandPrompt cmd = new CommandPrompt();

	public AndroidDevice(String udid)
	{
		super(udid);
		initDevice();
	}

	public AndroidDevice(String uDID, String model, String brand, String osVersion, String deviceName, String apiLevel)
	{
		super(uDID);

		this.model = model;
		this.brand = brand;
		this.osVersion = osVersion;
		this.deviceName = deviceName;
		this.apiLevel = apiLevel;

		initDevice();
	}

	public void initDevice()
	{
		new Thread(() -> 
		{
			this.getDeviceName();
		}).start();
	}

	public boolean isRecordable()
	{
		if(this.isRecordable == null)
		{
			String screenrecord = cmd.runCommand("adb -s " + this.getUDID() + " shell ls /system/bin/screenrecord");
			if (screenrecord.trim().equals("/system/bin/screenrecord")) 
			{
				this.isRecordable = true;
			} 
			else 
			{
				this.isRecordable = false;
			}
		}

		return this.isRecordable;
	}

	public String getManufacturer() 
	{
		if(this.manufacturer.isEmpty())
		{
			this.manufacturer = cmd.runCommand("adb -s " + this.getUDID() + " shell getprop ro.product.manufacturer").trim();
		}

		return this.manufacturer;
	}

	public synchronized String getModel() 
	{
		if(this.model.isEmpty())
		{
			this.model = cmd.runCommand("adb -s " + this.getUDID() + " shell getprop ro.product.model").replaceAll("\\s+", "");
		}

		return this.model;
	}

	public void setModel(String model) 
	{
		this.model = model;
	}

	public synchronized String getBrand() 
	{
		if(this.brand.isEmpty())
		{
			this.brand = cmd.runCommand("adb -s " + this.getUDID() + " shell getprop ro.product.brand").replaceAll("\\s+", "");
		}

		return brand;
	}

	public void setBrand(String brand) 
	{
		this.brand = brand;
	}

	@Override
	public String getOSVersion() 
	{
		if(this.osVersion.isEmpty())
		{
			this.osVersion = cmd.runCommand("adb -s " + this.getUDID() + " shell getprop ro.build.version.release").replaceAll("\\s+", "");
		}

		return osVersion;
	}

	public void setVersion(String osVersion) 
	{
		this.osVersion = osVersion;
	}

	@Override
	public String getDeviceName() 
	{
		if(this.deviceName.isEmpty())
		{
			this.deviceName = this.getBrand() + "-" + this.getModel();
		}

		return deviceName;
	}

	@Override
	public String getProductTypeAndVersion()
	{
		return this.getDeviceName() + "-" + this.getOSVersion();
	}

	public String getApiLevel() 
	{
		if(this.osVersion.isEmpty())
		{
			this.apiLevel = cmd.runCommand("adb -s " + this.getUDID() + " shell getprop ro.build.version.sdk").replaceAll("\n", "");
		}

		return apiLevel;
	}

	public void setApiLevel(String apiLevel) 
	{
		this.apiLevel = apiLevel;
	}

	@Override
	public IDeviceLogReader getLogReader()
	{
		if(this.logReader == null)
		{
			this.logReader = AndroidLogReader.get(this);
		}

		return this.logReader;
	}

	public void startVideoRecording(String originalFile) 
	{
		if (!this.getManufacturer().equals("unknown") && this.isRecordable()) 
		{
			String command = "adb -s " + this.getUDID()
			+ " shell screenrecord --bit-rate 3000000 /sdcard/" + originalFile
			+ ".mp4";

			this.setScreenRecordProcess(cmd.runProcess(command));
		} 
		else
		{
			String command = "flick video -a start -p android -u " + this.getUDID();
			this.setScreenRecordProcess(cmd.runProcess(command));
		}
	}

	public String stopVideoRecording(String originalFile, String videoLocation, String videoFileName) 
	{
		String videoFile = videoLocation + File.separator + videoFileName + ".mp4";

		if (!this.getManufacturer().equals("unknown") && this.isRecordable())
		{
			new Thread(() -> 
			{
				String task = "[pullVideoFromDevice] " + videoFile;
				RunTimeContext.getInstance().addTask(task);
				try
				{
					// Kill recording process
					TaskManager.killProcess(TaskManager.getPid(this.getScreenRecordProcess()) + "", TaskManager.Signal.CTRL_C);

					// Pull out video from device
					this.pullVideoFromDevice(originalFile, videoFile);
				} 
				catch (IOException | InterruptedException e)
				{
					e.printStackTrace();
				}
				finally
				{
					RunTimeContext.getInstance().removeTask(task);
				}
			}).start();
		} 
		else 
		{
			String androidCommand = "flick video -a stop -p android -o "
					+ videoLocation + " -n " + videoFileName
					+ " -u " + this.getUDID() + " --trace";

			// To reduce execution time, use another thread to stop recording
			new Thread(() -> {cmd.runCommand(androidCommand);}).start();

		}

		return videoFile;
	}

	public void pullVideoFromDevice(String fileName, String destination) throws IOException, InterruptedException
	{
		System.out.println("Moving file: [/sdcard/" + fileName + ".mp4] from device: [" + this.getUDID() + "] to [" + destination + "]");
		Process pc = cmd.runProcess("adb", "-s", this.getUDID(), "pull", "/sdcard/" + fileName + ".mp4", destination);
		pc.waitFor();
		System.out.println("Exited with Code::" + pc.exitValue());
		Thread.sleep(5000);

		// Remove video file from device
		System.out.println("Removing file: [/sdcard/" + fileName + ".mp4] from device: [" + this.getUDID() + "]");
		cmd.runCommand("adb -s " + this.getUDID() + " shell rm -f /sdcard/" + fileName + ".mp4");
	}

	@Override
	public void startVideoRecording(String className, String methodName) 
	{
		this.startVideoRecording(methodName);
	}

	@Override
	public String stopVideoRecording(String className, String methodName) 
	{
		String videoLocation = RunTimeContext.getInstance().getLogPath("screenshot", className, methodName);
		String videoFileName = methodName + RunTimeContext.currentDateAndTime();

		return this.stopVideoRecording(methodName, videoLocation, videoFileName);
	}

	public void setupAppiumForTest() throws Exception
	{
		// Start Appium server
		this.startAppiumServer();
		this.startAppiumDriver();

		// Update testing device process id for Android
		((AndroidLogReader)this.getLogReader()).updateAppProcessID();
	}
	
	public void startAppiumServer() throws Exception
	{
		// Start Appium server
		appiumServer = appiumServerManager.startAppiumServerForAndroid(testInfo);
	}

	@Override
	public void stopAppiumServer()
	{
		if(appiumServer != null)
		{
			appiumServer.stop();
			if (appiumServer.isRunning()) 
			{
				System.out.println("AppiumServer didn't shutdown... Trying to quit again....");
				appiumServer.stop();
			}
		}

		appiumServer = null;
	}
}
