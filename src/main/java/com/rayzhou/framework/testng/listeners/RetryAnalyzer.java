package com.rayzhou.framework.testng.listeners;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import com.rayzhou.framework.annotations.RetryCount;
import com.rayzhou.framework.context.RunTimeContext;

public class RetryAnalyzer implements IRetryAnalyzer 
{
	private ConcurrentHashMap<String, RetryMethod> retryMap = new ConcurrentHashMap<>();

	public RetryAnalyzer() 
	{
	}

	@Override 
	public boolean retry(ITestResult iTestResult)
	{
		if (iTestResult.getStatus() == ITestResult.FAILURE) 
		{
			RetryMethod method = getRetryMethod(iTestResult);
			
			System.out.println("Test Failed - " + method.methodName);
			if (method.needRetry()) 
			{
				method.increaseRetryCount();
				System.out.println("Retrying Failed Test Cases " + method.retryCount + " out of " + method.maxRetryCount);

				return true;
			}
			else
			{
				System.out.println("Meet maximum retry count [ " + method.maxRetryCount + " ]");

				return false;
			}
		}

		return false;
	}

	public RetryMethod getRetryMethod(ITestResult iTestResult)
	{
		String methodName = iTestResult.getMethod().getMethodName();
		String key = methodName + Thread.currentThread().getId();

		if(this.retryMap.containsKey(key))
		{
			return this.retryMap.get(key);
		}
		else
		{
			int maxRetryCount = 0;
			Method[] methods = iTestResult.getInstance().getClass().getMethods();
			for (Method m : methods) 
			{
				if (m.getName().equals(methodName)) 
				{
					if (m.isAnnotationPresent(RetryCount.class)) 
					{
						RetryCount ta = m.getAnnotation(RetryCount.class);
						maxRetryCount = ta.maxRetryCount();
					} 
					else 
					{
						try
						{
							maxRetryCount = Integer.parseInt(RunTimeContext.getInstance().getProperty("MAX_RETRY_COUNT"));
						} 
						catch (Exception e)
						{
							maxRetryCount = 0;
						}
					}
					
					break;
				}
			}

			this.retryMap.put(key, new RetryMethod(0, maxRetryCount, methodName));
			
			return this.retryMap.get(key);
		}
	}

	class RetryMethod
	{
		int retryCount = 0;
		int maxRetryCount = 0;
		String methodName = "";

		public RetryMethod(int retryCount, int maxRetryCount, String methodName)
		{
			this.retryCount = retryCount;
			this.maxRetryCount = maxRetryCount;
			this.methodName = methodName;
		}

		public boolean needRetry()
		{
			return retryCount < maxRetryCount;
		}
		
		public void increaseRetryCount()
		{
			retryCount++;
		}
	}
}
