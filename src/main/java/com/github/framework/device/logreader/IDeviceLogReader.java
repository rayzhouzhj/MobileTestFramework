package com.github.framework.device.logreader;

import java.util.List;

public interface IDeviceLogReader
{
	public void startCaptureLog();
	public void stopCaptureLog();
	public List<String> getTrackingLogs();
	public List<String> getFullLogs();
	public List<String> getFullLogsFromQueue();
}
