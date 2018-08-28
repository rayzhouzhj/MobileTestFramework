package com.rayzhou.framework.utils;

/**
 * Command Prompt - this class contains method to run windows and mac commands
 */
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CommandPrompt 
{
	private static final String[] WIN_RUNTIME = { "cmd.exe", "/C" };
	private static final String[] OS_LINUX_RUNTIME = { "/bin/bash", "-l", "-c" };

	private static <T> T[] concat(T[] first, T[] second)
	{
		T[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}

	public String runCommand(String... commands)
	{
		ProcessBuilder tempBuilder;
		String allLine = "";

		try
		{
			String os = System.getProperty("os.name");
			String command = Arrays.asList(commands).stream().collect(Collectors.joining(" "));
			System.out.println("INFO: Run Command on [" + os + "]: " + command);

			String[] allCommand;
			// build cmd process according to os
			if(os.contains("Windows")) // if windows
			{
				allCommand = concat(WIN_RUNTIME, new String[]{command});
			} else 
			{
				allCommand = concat(OS_LINUX_RUNTIME, new String[]{command});
			}

			tempBuilder = new ProcessBuilder(allCommand);
			tempBuilder.redirectErrorStream(true);
			Thread.sleep(1000);
			Process process = tempBuilder.start();

			// get std output
			BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = "";
			while ((line = r.readLine()) != null) 
			{
				allLine = allLine + "" + line.trim() + "\n";
				if (line.contains("Appium REST http interface listener started"))
				{
					break;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Unexpected error in command line.");
		}

		return allLine;
	}

	public Process runProcess(String... commands)
	{
		try
		{
			String os = System.getProperty("os.name");
			String command = Arrays.asList(commands).stream().collect(Collectors.joining(" "));
			System.out.println("INFO: Run Command on [" + os + "]: " + command);

			String[] allCommand;
			// build cmd process according to os
			if(os.contains("Windows")) // if windows
			{
				allCommand = concat(WIN_RUNTIME, new String[]{command});
			} else 
			{
				allCommand = concat(OS_LINUX_RUNTIME, new String[]{command});
			}

			ProcessBuilder tempBuilder = new ProcessBuilder(allCommand);
			tempBuilder.redirectErrorStream(true);
			Thread.sleep(1000);
			return tempBuilder.start();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Unexpected error in command line.");
		}
	}
}
