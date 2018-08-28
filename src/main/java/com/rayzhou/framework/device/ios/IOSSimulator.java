package com.rayzhou.framework.device.ios;

import java.io.File;

import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.utils.TaskManager;

public class IOSSimulator extends IOSDevice 
{
	private String state;
	private String availability;
	private String screenRecordFilePath;

	public IOSSimulator(String udid, String deviceName, String productType, String productVersion, String state, String availability) 
	{
		super(udid, deviceName, productType, productVersion, false);

		this.setState(state);
		this.setAvailability(availability);
	}

	@Override
	public boolean isConnected()
	{
		// Always return true as is simulator is down, appium will boot it automatically
		return true;
	}
	
	public boolean isBooted()
	{
		return state.equalsIgnoreCase("Booted");
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getAvailability() {
		return availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}

	public void startSimulatorVideoRecording(String videoLocation, String videoFileName) 
	{
		screenRecordFilePath = videoLocation + File.separator + videoFileName + ".mp4";
		String command = "xcrun simctl io " + this.getUDID() + " recordVideo " + screenRecordFilePath;
		this.setScreenRecordProcess(this.commandPrompt.runProcess(command));
	}

	public String stopSimulatorVideoRecording() 
	{
		System.out.println("Stop video recording on [" + this.getUDID() + "]");

		new Thread(() ->
		{
			// Kill recording process
			TaskManager.killProcess(TaskManager.getPid(this.getScreenRecordProcess()) + "", TaskManager.Signal.CTRL_C);
		}).start();

		return screenRecordFilePath;
	}
	
	@Override
	public void startVideoRecording(String className, String methodName) 
	{
		String videoLocation = RunTimeContext.getInstance().getLogPath("screenshot", className, methodName);
		String videoFileName = methodName + RunTimeContext.currentDateAndTime();
		
		this.startSimulatorVideoRecording(videoLocation, videoFileName);
	}

	@Override
	public String stopVideoRecording(String className, String methodName)
	{
		return this.stopSimulatorVideoRecording();
	}
}
