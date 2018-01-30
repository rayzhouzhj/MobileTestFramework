# MobileTestFramework
Mobile test framework with Selenium + Appium + ExtentReport + TestNG

## What the framework can do
It can help you:
* Run scripts in different mode Parallel/Distribute
* Run scripts in different devices
  - Android emulator
  - Android real device
  - IOS simulator
  - IOS real device
* Script on device level, which means you can simply use below statement to access the running device info(both android and ios):
```
TestingDevice.get().getDeviceName();
TestingDevice.get().getOSVersion();
TestingDevice.get().getDeviceHight();
```
* ExtentReport and Klov server for better reporting 

## How to start  
Just simply copy the config.properities and Runner.java to your project and make necessary update:
* [config.properities](https://github.com/rayzhouzhj/MobileTestFramework/blob/master/config.properties)
  - configuration of your runtime context, e.g. parallel/distribute execution, platform as android/ios, log configs, etc
* [Runner.java](https://github.com/rayzhouzhj/MobileTestFramework/blob/master/src/test/java/com/github/demo/test/runner/Runner.java)
  - Packages of the test class or event specify test class to be executed
  
#### Run below command to start test execution:
```
mvn clean test -Dtest=Runner
```

#### Or override any configs at runtime:
```
PLATFORM=Android DEBUG_MODE=OFF mvn clean test -Dtest=Runner
```

#### Turn on debug mode to select any available devices for debugging
<img src="https://github.com/rayzhouzhj/MobileTestFramework/blob/master/screenshot-refs/choosedevice.png" width="350">


## Check out a quick demo on how this framework works [MobiletestDemo](https://github.com/rayzhouzhj/MobileTestDemo)
