package com.rayzhou.framework.device;

public enum MobilePlatform 
{
    IOS("IOS"),
    ANDROID("ANDROID"),
	UNDEFINED("UNDEFINED");

    public final String platformName;
    
    MobilePlatform(String platformName) 
    {
        this.platformName = platformName;
    }
    
    public String toString()
    {
    	return this.platformName;
    }

}
