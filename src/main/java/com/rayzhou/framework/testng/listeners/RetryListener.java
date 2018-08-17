package com.rayzhou.framework.testng.listeners;

import org.testng.IAnnotationTransformer;
import org.testng.IRetryAnalyzer;
import org.testng.annotations.ITestAnnotation;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 *
 */
public class RetryListener implements IAnnotationTransformer 
{
    @Override
    public void transform(ITestAnnotation iTestAnnotation, Class aClass, Constructor constructor, Method method) 
    {
        IRetryAnalyzer retry = iTestAnnotation.getRetryAnalyzer();

        if (retry == null) 
        {
            iTestAnnotation.setRetryAnalyzer(RetryAnalyzer.class);
        }
    }
}
