package com.rayzhou.framework.device.ios;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.rayzhou.framework.utils.CommandPrompt;

public class IOSSimulatorManager 
{
	private static CommandPrompt commandPrompt = new CommandPrompt();

	public static String getRuntimeVersion()
	{
		String runtimesJSONString = commandPrompt.runCommand("xcrun simctl list -j runtimes");

		JSONObject runtimesJSON = (JSONObject) new JSONObject(runtimesJSONString).getJSONArray("runtimes").get(0);

		return runtimesJSON.getString("name");
	}

	public static List<IOSSimulator> getAllSimulators()
	{
		List<IOSSimulator> simulators = new ArrayList<>();
		String productVersion = getRuntimeVersion();

		String jsonString = commandPrompt.runCommand("xcrun simctl list -j devices");
		JSONObject devicesJson = new JSONObject(jsonString).getJSONObject("devices");
		JSONArray devices = new JSONObject(devicesJson.toString()).getJSONArray(productVersion);

		devices.forEach(json ->
		{
			String udid = ((JSONObject)json).getString("udid");
			String state = ((JSONObject)json).getString("state");
			String name = ((JSONObject)json).getString("name");
			String availablility = ((JSONObject)json).getString("availability"); 

			simulators.add(new IOSSimulator(udid, name, name, productVersion.toUpperCase().replace("IOS", "").trim(), state, availablility));
		});

		return simulators;
	}

	public static void main(String... args)
	{
		getAllSimulators().forEach(device ->
		{
			if(device.getState().equalsIgnoreCase("Booted"))
			{
				System.out.println(device.getState());
				System.out.println(device.getAvailability());
				System.out.println(device.getProductType());
				System.out.println(device.getDeviceName());
				System.out.println(device.getOSVersion());
				System.out.println("=============================");
			}
		});
	}
}
