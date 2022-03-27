# XPERIAccess

XPERIAccess provides some convenience features to the Sony Xperia 5 III Android device. It is
provided in a form of an accessibility service.XPERIAccess

Inspired by https://github.com/ivaniskandar/shouko.

## Installation

After installing the apk, run the following through adb:

```
adb shell pm grant xyz.gordonszeto.xperiaccess android.permission.READ_LOGS
```

The READ_LOGS permission is required so that the service can detect back button presses universally.

## Features

Features implemented so far:

* Lock screen on back button press-and-hold
