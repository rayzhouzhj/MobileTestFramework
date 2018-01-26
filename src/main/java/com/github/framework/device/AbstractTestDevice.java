package com.github.framework.device;

import org.openqa.selenium.Dimension;

import com.github.framework.device.logreader.IDeviceLogReader;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;

public abstract class AbstractTestDevice extends AbstractAppiumDevice
{
	private String UDID = "";
	protected String deviceName = "";
	private Integer deviceHight = null;
	private Integer deviceWidth = null;
	private boolean isRealDevice = true; 
	private String videoFilePath = "";
	private Process screenRecordProcess = null;
	protected IDeviceLogReader logReader = null;
	
	public AbstractTestDevice(String udid)
	{
		super();
		
		this.UDID = udid;
	}
	
	public String getUDID() {
		return UDID;
	}

	public void setUDID(String uDID) {
		UDID = uDID;
	}
	
	public void setDeviceName(String deviceName)
	{
		this.deviceName = deviceName;
	}
	
	public Integer getDeviceHight() {
		return deviceHight;
	}

	public void setDeviceHight(Integer deviceHight) {
		this.deviceHight = deviceHight;
	}

	public Integer getDeviceWidth() {
		return deviceWidth;
	}

	public void setDeviceWidth(Integer deviceWidth) {
		this.deviceWidth = deviceWidth;
	}
	
	public void updateDeviceSize(AppiumDriver<MobileElement> driver)
	{
		if(deviceHight == null || deviceWidth == null)
		{
			Dimension dimension = driver.manage().window().getSize();
			
			this.setDeviceHight(dimension.getHeight());
			this.setDeviceWidth(dimension.getWidth());
		}
	}

	public boolean isRealDevice() 
	{
		return isRealDevice;
	}

	public void setRealDevice(boolean isRealDevice) 
	{
		this.isRealDevice = isRealDevice;
	}
	
	public String getVideoFilePath() {
		return videoFilePath;
	}

	public void setVideoFilePath(String videoFilePath) {
		this.videoFilePath = videoFilePath;
	}

	public Process getScreenRecordProcess() {
		return screenRecordProcess;
	}

	public void setScreenRecordProcess(Process screenRecordProcess) {
		this.screenRecordProcess = screenRecordProcess;
	}

	public abstract IDeviceLogReader getLogReader();
	
	public abstract String getDeviceName();
	
	public abstract String getProductTypeAndVersion();
	
	public abstract String getOSVersion();
	
	public abstract void initDevice();
	
	public abstract void startVideoRecording(String className, String methodName);
	
	public abstract String stopVideoRecording(String className, String methodName);
}
