package com.github.demo.test.runner;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.github.framework.context.RunTimeContext;
import com.github.framework.executor.TestExecutor;

public class Runner 
{
    @Test
    public static void testApp() throws Exception 
    {
    	if(!RunTimeContext.getApp().exists())
    	{
    		Assert.fail("App file does not exist: " + RunTimeContext.getApp().getAbsolutePath());
    	}
    	
        TestExecutor executor = TestExecutor.build();
        
        boolean hasFailures = executor.runTests("com.github.demo.app.test");
        
        Assert.assertFalse(hasFailures, "Testcases have failed in parallel execution");
    }
}
