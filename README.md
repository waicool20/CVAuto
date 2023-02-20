# CVAuto

This library provides computer vision aided automation to various platforms, such as:

- Android (through ADB)
- Desktop (through Robot)

# Documentation

[Read the KDoc here on GitHub Pages](https://waicool20.github.io/CVAuto/)

# Example

Using the android device module we can automate actions on an emulator through ADB

In this example we get a device using the serial `127.0.0.1:5555`, then grab its screen,
we try to send a click action where the click will happen within a rectangular region
of `50, 50, 100, 100`

All regions in CVAuto are defined by `x, y, width, height` where x and y are the top-left coordinate
of the rectangle and the width and height define the dimensions of the rectangle

```kotlin
val device = ADB.getDevice("127.0.0.1:5555") ?: error("Device not found")
val screen = device.screens.first()
screen.subRegion(50, 50, 100, 100).click()
```

# License

Licensed under MIT
