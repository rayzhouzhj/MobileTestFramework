package com.rayzhou.framework.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class TaskManager 
{
	public static enum Signal
	{
		// Force kill
		FORCE_KILL("-9"),
		// -INT: Terminates the program (like Ctrl+C)
		CTRL_C("-INT");

		public final String signal;

		Signal(String signal)
		{
			this.signal = signal;
		}

		public String toString()
		{
			return this.signal;
		}
	}

	public static void main(String[] args)
	{
		killAll("a23df87b76e7463ea022f53bf5a07f4663535fe4");
		System.out.println("===================================");
		findProcess("a23df87b76e7463ea022f53bf5a07f4663535fe4");
	}

	public static List<String> findProcess(String processName) 
	{
		List<String> processList = new ArrayList<String>();
		CommandPrompt cmd = new CommandPrompt();

		BufferedReader br = null;
		try {
			// For windows
			if(System.getProperty("os.name").contains("Windows"))
			{
				String command = "tasklist /FI \"IMAGENAME eq " + processName + "\"";
				Process proc = cmd.runProcess(command);

				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;

				System.out.println("[TaskManager] Query processes with command: " + command);
				while ((line = br.readLine()) != null) 
				{
					System.out.println(line);
					if (line.contains(processName)) 
					{
						line = line.replaceAll("\\s+", " ");
						String[] list = line.split(" ");
						processList.add(list[1]);
					}
				}
			}
			// For Mac
			else
			{
				String command = "ps aux | grep \"" + processName + "\" | grep -v 'grep' | awk '{print $2}'";
				Process proc = cmd.runProcess(command);

				br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String line = null;
				System.out.println("[TaskManager] Query processes with command: " + command);

				while ((line = br.readLine()) != null) 
				{
					System.out.println(line);
					processList.add(line);
				}
			}

			return processList;
		} 
		catch (Exception e) 
		{
			e.printStackTrace();
			return processList;
		} 
		finally 
		{
			if (br != null) 
			{
				try 
				{
					br.close();
				} 
				catch (Exception ex) {}
			}
		}
	}

	public static void killAll(String processName) 
	{
		List<String> processList = findProcess(processName);
		processList.forEach(process -> 
		{
			new Thread(() -> killProcess(process)).start();
		});
	}

	public static void killProcess(String processID, Signal signal) 
	{
		BufferedReader br = null;
		CommandPrompt cmd = new CommandPrompt();
		try 
		{
			String command;
			// For windows
			if(System.getProperty("os.name").contains("Windows"))
			{
				command = "Taskkill /F /PID " + processID;	
			}
			// For MAC
			else
			{
				command = "kill " + signal + " " + processID;
			}

			Process proc = cmd.runProcess(command);

			br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			String line = null;
			while ((line = br.readLine()) != null) 
			{
				System.out.println(line);

				BufferedReader in = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				String string_Temp = in.readLine();
				while (string_Temp != null)
				{
					System.out.println(string_Temp);
					string_Temp = in.readLine();
				}
			}

		} 
		catch (Exception e) 
		{
			e.printStackTrace();
		} 
		finally
		{
			if (br != null) 
			{
				try 
				{
					br.close();
				} 
				catch (Exception ex) {}
			}
		}
	}

	public static void killProcess(String processID)
	{
		killProcess(processID, Signal.FORCE_KILL);
	}

	public static int getPid(Process process)
	{
		try 
		{
			Class<?> cProcessImpl = process.getClass();
			Field fPid = cProcessImpl.getDeclaredField("pid");
			if (!fPid.isAccessible())
			{
				fPid.setAccessible(true);
			}

			return fPid.getInt(process);

		} 
		catch (Exception e)
		{
			return -1;
		}
	}
}
