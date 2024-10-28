[mac服务开关](https://www.stigviewer.com/stig/apple_macos_11_big_sur/2020-11-27/finding/V-230787)

domain: 管理service集合的执行策略

- system/[service-name]
- user/<uid>/[service-name]
- login/<asid>/[service-name]
- gui/<uid>/[service-name]
- pid/<pid>/[service-name]

例如名字com.apple.example的service加载到UID 501的用户的GUI domain, 那么domain-target就是gui/501, service-name是com.apple.example， service-target是gui/501/com.apple.example

service：虚拟进程

endpoints：每个service有一个endpoints集合





```shell
#打印关闭的系统服务
/bin/launchctl print-disabled system 
#打印关闭的用户服务
/bin/launchctl print-disabled user/501
#关闭一个系统服务
/usr/bin/sudo /bin/launchctl disable system/com.apple.smbd

# 卸载一个内核扩展
sudo kextunload -b com.apple.filesystems.smbfs                                             
# 查看是否卸载成功
kextstat |grep smbfs
```

这个卸载是[临时的](https://www.decisivetactics.com/support/view?article=disable-driver)， 会在机器重启时自动加载，但是我没有重启它也自动加载了。直接把kext目录重命名`mv /System/Library/Extensions/smbfs.kext /System/Library/Extensions/smbfs.kext.bak` 会提示操作不允许Operation not permitted。这是系统开启了SIP(系统完整性保护)，网上说可以先关闭











```shell
bootstrap       Bootstraps a domain or a service into a domain.
bootout         Tears down a domain or removes a service from a domain.#从某个domain卸载服务
enable          Enables an existing service.
disable         Disables an existing service.
kickstart       Forces an existing service to start.
attach          Attach the system's debugger to a service.
debug           Configures the next invocation of a service for debugging.
kill            Sends a signal to the service instance.
blame           Prints the reason a service is running.
print           Prints a description of a domain or service.
print-cache     Prints information about the service cache.
print-disabled  Prints which services are disabled.
plist           Prints a property list embedded in a binary (targets the Info.plist by default).
procinfo        Prints port information about a process.
hostinfo        Prints port information about the host.
resolveport     Resolves a port name from a process to an endpoint in launchd.
limit           Reads or modifies launchd's resource limits.
examine         Runs the specified analysis tool against launchd in a non-reentrant manner.
config          Modifies persistent configuration parameters for launchd domains.
dumpstate       Dumps launchd state to stdout.
dumpjpcategory  Dumps the jetsam properties category for all services.
reboot          Initiates a system reboot of the specified type.
bootshell       Brings the system up from single-user mode with a console shell.
load            Recommended alternatives: bootstrap | enable. Bootstraps a service or directory of services.
unload          Recommended alternatives: bootout | disable. Unloads a service or directory of services.
remove          Unloads the specified service name.
list            Lists information about services. #列出加载到launchd的服务信息
				第一列是PID，第三列是label
start           Starts the specified service.
stop            Stops the specified service if it is running.
setenv          Sets the specified environment variables for all services within the domain.
unsetenv        Unsets the specified environment variables for all services within the domain.
getenv          Gets the value of an environment variable from within launchd.
bsexec          Execute a program in another process' bootstrap context.
asuser          Execute a program in the bootstrap context of a given user.
submit          Submit a basic job from the command line.
managerpid      Prints the PID of the launchd controlling the session.
manageruid      Prints the UID of the current launchd session.
managername     Prints the name of the current launchd session.
error           Prints a description of an error.
variant         Prints the launchd variant.
version         Prints the launchd version.
help            Prints the usage for a given subcommand.
```





# com.apple.filesystems.smbfs bug

[com.apple.filesystems.smbfs](https://apple.stackexchange.com/questions/469376/why-mac-m1-m2-crashed-while-connecting-to-win-7-pc-via-smb)

# 关闭SIP

1、关机

2、按住开机键不松手直到出现下图的画面，然后点击【选项】

3、点击【继续】



![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/5ebc43650db26ef343db2089d161c5ca.jpeg)



4、点击菜单栏的【实用工具】，再点击【启动安全性实用工具】



![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/b184699c9763778441a74769ca0dad00.jpeg)



5、勾选 允许用户管理来自被认可开发者的内核扩展



![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/3a02f47c392ef80e90dea8525af3799e.jpeg)



6、输入电脑密码，点击好。



![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/b691781dfc5845d5718254cbf698a2ce.jpeg)



7、重启电脑即可。





