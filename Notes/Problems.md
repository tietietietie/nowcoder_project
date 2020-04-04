# Problems

## Chapter 1

### 1.不能访问阿里云镜像

在settings-->Maven-->importing-->VM importer options处添加如下代码，修改安全协议。

> -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

### 2.ctrl+函数名不能访问其源代码

热键冲突，在settings-->keymap-->main view --> navigation中找到declaration，发现其快捷键其实是ctrl+alt+鼠标右键。

### 3.编译Test项目时，卡在Resolving Maven dependencies

在settings-->Maven-->importing-->VM importer options修改JVM参数为 -Xms1024m -Xmx2048m。修改了heap值。

### 4.idea找不到new interface

点new-->class，然后选interface

### 5.修改了代码没有生效

忘记recompile直接run了，点击build--->rebuild project即可。

### 6.如何快速处理异常（快捷方式）

参考[这里](https://blog.csdn.net/a200822146085/article/details/92805214)，快捷键是alt+enter