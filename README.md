## 开发者日志

## 目标

- [x] 睡觉，游走，提起
- [x] 点击睡觉或趴下时的猫冒出爱心
- [x] 四种皮肤
- [x] 托盘功能
- [x] 昼夜行为不同
- [x] 心情系统
- [ ] 饮水

## 打包方法
将资源目录下的资源复制到\classes\java\main目录下

```cmd
cd build
jpackage -t app-image -n kitty -p '.\libs\kotlin-stdlib-2.1.0.jar;.\libs\annotations-13.0.jar;.\classes\java\main' -m me.cdh/me.cdh.MainKt --jlink-options '--compress zip-9 --no-header-files --strip-native-commands --no-man-pages --strip-debug' --java-options '-Xms50m -Xmx80m -XX:+UseZGC' --icon .\resources\main\kitty.ico --resource-dir .\resources\main
```