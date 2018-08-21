package com.rayzhou.demo.test.runner;

import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.rayzhou.framework.executor.TestExecutor;

public class Runner 
{
    @Test
    public static void testApp() throws Exception 
    { 	
        TestExecutor executor = TestExecutor.build();
        
        List<String> packageList = new ArrayList<>();
        packageList.add("com.github.demo.app.test");
        
        boolean hasFailures = executor.runTests(packageList);
        
        Assert.assertFalse(hasFailures, "Testcases have failed in parallel execution");
    }
}