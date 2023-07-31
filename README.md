# ViewDebug-Trans
Android studio 插件，用于推送文件到指定目录
## 使用方式
1. 本地安装打包后的插件，重启Android studio
2. Android项目依赖[SkinSwitch](https://github.com/DFFXT/SkinSwitch/tree/develop_sink_load)插件，该插件使用方式见对应库
3. 构建运行一次Android项目
4. 打开要更改或者更改后的xml、kt代码
5. 右键，选择pushToDevices
6. 配置推送路径（sdcard/Android/data/包名，该路径必须为externalCache的父目录）
7. 如果是推送kotlin文件，如果没有自动获取dx目录，需手动配置
8. 选择应用安装的设备，会直接推送对应文件到目录
## 作用：
推送文件到Android客户端的指定目录
1. 如果推送的是xml文件，会尝试将/build/intermediates/incremental/mergeXXXXResources/merge.xml也推送到指定文件夹<p>
2. 如果推送的是kotlin代码，首先会尝试获取R文件，解析R文件，生成R.java，然后会尝试将kotlin进行编译成jar，最后再通过dx工具转换为dex文件，最后再推送到指定文件夹
3. 目前不支持java代码的编译

条件：
1. 必须有adb环境
2. 需要指定dx工具路径，如果adb是使用的AndroidSDK/platform-tools/目录下的adb，则会自动选择sdk/build-tools目录下第一个文件夹内的dx工具
3. 项目需要经过一次正常的编译，否则可能不存在R文件和merger.xml

## 期望效果：
如果应用继承了SkinSwitch插件，则可进行快速开发
将各种文件推送到手机
1. 如果是xml文件，可直接替换布局或者drwable，重新触发xml的加载即可看到效果
2. 如果是dex文件，则重启应用，补丁会自动应用。

主要是方便开发者快速在真机或者模拟器上看到xml和少量代码修改的效果，而不用重新构建项目重新安装，特别适合构建花费很多时间的大型项目。


