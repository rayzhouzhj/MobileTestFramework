package com.github.framework.testng.model;

import java.lang.reflect.Method;

import org.testng.IInvokedMethod;
import org.testng.annotations.Test;

import com.github.framework.annotations.Author;
import com.github.framework.annotations.ClassDescription;
import com.github.framework.annotations.InstallApp;
import com.github.framework.annotations.SkipAppInstall;
import com.github.framework.annotations.SkipPlatform;
import com.github.framework.device.MobilePlatform;

public class TestInfo 
{
    private IInvokedMethod invokedMethod;
    private Method declaredMethod;
    
    private String description;
    
    public TestInfo(IInvokedMethod methodName) 
    {
        this.invokedMethod = methodName;
        this.declaredMethod = this.invokedMethod.getTestMethod().getConstructorOrMethod().getMethod();
        this.description = this.declaredMethod.getAnnotation(Test.class).description();
    }

    public String getClassName()
    {
    	return this.declaredMethod.getDeclaringClass().getSimpleName();
    }
    
    public String getClassDescription()
    {
    	ClassDescription description = this.declaredMethod.getDeclaringClass().getAnnotation(ClassDescription.class);
    	
    	return description == null? "" : description.value();
    }
    
    public String getMethodName()
    {
    	return this.declaredMethod.getName();
    }
    
    public boolean isTestMethod()
    {
    	return this.declaredMethod.getAnnotation(Test.class) != null;
    }
    
    public String getAuthorName() 
    {
        return declaredMethod.getAnnotation(Author.class) == null? null : declaredMethod.getAnnotation(Author.class).name();
    }
    
    public String getTestName()
    {
    	String testName = "";
    	
    	if(this.description == null || this.description.isEmpty())
        {
    		testName = this.declaredMethod.getName();
        }
        else
        {
        	testName = this.declaredMethod.getName() + "[" + this.description + "]";
        }
        
        return testName;
    }
    
    public boolean isSkippedTest()
    {
    	return this.declaredMethod.getAnnotation(SkipPlatform.class) != null;
    }
    
    public MobilePlatform getSkipPlatform()
    {
    	if(this.isSkippedTest())
    	{
    		return this.declaredMethod.getAnnotation(SkipPlatform.class).platform();
    	}
    	else
    	{
    		return null;
    	}
    }
    
    public boolean needReInstallApp()
    {
    	return this.declaredMethod.getAnnotation(InstallApp.class) != null;
    }
    
    public boolean skipReInstallApp()
    {
    	return this.declaredMethod.getAnnotation(SkipAppInstall.class) != null;
    }
}
