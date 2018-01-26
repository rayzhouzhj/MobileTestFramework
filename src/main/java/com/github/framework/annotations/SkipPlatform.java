package com.github.framework.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.github.framework.device.MobilePlatform;

/**
 * Use this annotation if you want to skip a test based plaform.
 */
@Target(ElementType.METHOD) 
@Retention(RetentionPolicy.RUNTIME) 
public @interface SkipPlatform 
{
    MobilePlatform platform();
}
