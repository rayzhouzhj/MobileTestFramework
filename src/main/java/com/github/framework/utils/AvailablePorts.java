package com.github.framework.utils;

import java.io.IOException;
import java.net.ServerSocket;

public class AvailablePorts {

    /*
     * Generates Random ports
     * Used during starting appium server
     */
    public static int get()
    {
    	int port = 0;
        ServerSocket socket;
		try
		{
			socket = new ServerSocket(0);
			socket.setReuseAddress(true);
	        port = socket.getLocalPort();
	        socket.close();
		}
		catch (IOException e) 
		{
			e.printStackTrace();
		}
        
        return port;
    }
}
