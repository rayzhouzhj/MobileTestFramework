package com.rayzhou.framework.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.aventstack.extentreports.Status;
import com.rayzhou.framework.context.RunTimeContext;
import com.rayzhou.framework.context.TestingDevice;

public class ScreenShotManager 
{

    public ScreenShotManager(){}

    public String captureScreenShot(Status status, String className, String methodName, String deviceModel) 
    {
    	// If driver is not setup properly
    	if(TestingDevice.get().getDriver() == null)
    	{
    		return "";
    	}
    	
        File scrFile = ((TakesScreenshot) TestingDevice.get().getDriver()).getScreenshotAs(OutputType.FILE);
        String screenShotNameWithTimeStamp = RunTimeContext.currentDateAndTime() + "_" + deviceModel.replace(" ", "-");
        
        return copyscreenShotToTarget(status, scrFile, methodName, className, screenShotNameWithTimeStamp);
    }

    public String captureScreenShot(String screenShotName) throws InterruptedException, IOException
    {
        String className = new Exception().getStackTrace()[1].getClassName();
        String deviceModel = TestingDevice.get().getProductTypeAndVersion();
        
        return captureScreenShot(Status.INFO, className, screenShotName, deviceModel);
    }

    private String copyscreenShotToTarget(Status status,
                                    File scrFile, String methodName,
                                    String className, String screenShotNameWithTimeStamp) 
    {
    	String filePath = RunTimeContext.getInstance().getLogPath("screenshot", className, methodName);
    	
        String failedScreen = filePath + File.separator + screenShotNameWithTimeStamp + "_" + methodName + "_failed.png";
        String capturedScreen = filePath + File.separator  + screenShotNameWithTimeStamp + "_" + methodName + "_results.png";

        try 
        {
            if (status == Status.FAIL)
            {
                FileUtils.copyFile(scrFile, new File(failedScreen.trim()));
                
                return failedScreen.trim();
            } 
            else
            {
                FileUtils.copyFile(scrFile, new File(capturedScreen.trim()));
                
                return capturedScreen.trim();
            }
        } 
        catch (IOException e) 
        {
            e.printStackTrace();
        }
        
        return "";
    }

}
