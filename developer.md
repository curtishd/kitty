## 开发者日志

目前已实现的功能：

- [x] 睡觉，游走，提起
- [x] 点击睡觉或趴下时的猫冒出爱心
- [x] 提供四种皮肤
- [x] 实现托盘功能
- [x] 白天猫咪喜爱睡觉，晚上猫咪喜欢游走

计划实现的功能包括但不限于：



```cmd
cd build
jpackage -t app-image -n kitty -p '.\libs\kotlin-stdlib-2.0.21.jar;.\libs\annotations-13.0.jar;.\classes\java\main' -m me.cdh/me.cdh.MainKt --jlink-options '--compress zip-9 --no-header-files --strip-native-commands --no-man-pages --strip-debug' --java-options '-Xms50m -Xmx80m -XX:+UseZGC' --icon .\res\kitty.ico --resource-dir .\res
```