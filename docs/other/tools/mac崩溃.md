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





# OCLD升级

## 3rd party NVMe controller

1. [使用NVMeFix](https://github.com/acidanthera/NVMeFix)

2. [解决方案？ ](https://discussions.apple.com/thread/255695101?sortBy=rank)

   > 1. Shut down your Mac.
   > 2. Turn on your Mac, then immediately press and hold these four keys together: Option, Command, P, R.
   > 3. Release the keys after about 20 seconds, during which your Mac might appear to restart. For example, you might hear a startup sound more than once, or see the Apple logo more than once.
   > 4. When your Mac finishes starting up, you might need to adjust any system settings that were reset.





### 升级ssd固件

1. 安装unetbootin.github.io
2. 把优盘格式化为FAT32文件系统 
3. 把固件iso烧录进优盘



### Loss of MMIO space

panic(cpu 0 caller 0xffffff80045886a2): nvme: "3rd party NVMe controller. Loss of MMIO space. Write. fBuiltIn=1 MODEL=INTEL SSDPEKNW010T8 FW=002C CSTS=0xffffffff US[1]=0x0 US[0]=0x109 VID=0x8086 DID=0xf1a8 CRITICAL_WARNING=0x0.\n" @IONVMeController.cpp:6090
Panicked task 0xffffff8b6b664670: 113 threads: pid 0: kernel_task
Backtrace (CPU 0), panicked thread: 0xffffff9037ea0aa0, Frame : Return Address
0xfffffff2af0eba20 : 0xffffff8001c79a3d mach_kernel : _handle_debugger_trap + 0x41d
0xfffffff2af0eba70 : 0xffffff8001ddcd26 mach_kernel : _kdp_i386_trap + 0x116
0xfffffff2af0ebab0 : 0xffffff8001dcc093 mach_kernel : _kernel_trap + 0x4d3
0xfffffff2af0ebb00 : 0xffffff8001c19a90 mach_kernel : _return_from_trap + 0xe0
0xfffffff2af0ebb20 : 0xffffff8001c79e0d mach_kernel : _DebuggerTrapWithState + 0xad
0xfffffff2af0ebc40 : 0xffffff8001c795c6 mach_kernel : _panic_trap_to_debugger + 0x2b6
0xfffffff2af0ebca0 : 0xffffff8002514a73 mach_kernel : _panic + 0x84
0xfffffff2af0ebd90 : 0xffffff80045886a2 com.apple.iokit.IONVMeFamily : __ZN16IONVMeController14CommandTimeoutEP16AppleNVMeRequest.cold.1
0xfffffff2af0ebda0 : 0xffffff800456b7cb com.apple.iokit.IONVMeFamily : __ZN16IONVMeController13FatalHandlingEv + 0x141
0xfffffff2af0ebdd0 : 0xffffff8002449d25 mach_kernel : __ZN18IOTimerEventSource15timeoutSignaledEPvS0_ + 0xa5
0xfffffff2af0ebe40 : 0xffffff8002449c28 mach_kernel : __ZN18IOTimerEventSource17timeoutAndReleaseEPvS0_ + 0xc8
0xfffffff2af0ebe70 : 0xffffff8001cccad5 mach_kernel : _thread_call_delayed_timer + 0x505
0xfffffff2af0ebee0 : 0xffffff8001ccdba2 mach_kernel : _thread_call_delayed_timer + 0x15d2
0xfffffff2af0ebfa0 : 0xffffff8001c1919e mach_kernel : _call_continuation + 0x2e
      Kernel Extensions in backtrace:
         com.apple.iokit.IONVMeFamily(2.1)[590B0D1B-8800-3889-8F5D-8822DE9317F0]@0xffffff8004563000->0xffffff800458ffff
            dependency: com.apple.driver.AppleMobileFileIntegrity(1.0.5)[3D149A6E-2547-3ACE-95BF-6B1FE333E6E7]@0xffffff80033b1000->0xffffff80033d3fff
            dependency: com.apple.iokit.IOPCIFamily(2.9)[9B40C2BD-55D8-3572-912C-A55E1B34C673]@0xffffff8004838000->0xffffff8004864fff
            dependency: com.apple.iokit.IOReportFamily(47)[BD75A768-680E-3DE4-A887-EFF19D68BE27]@0xffffff8004876000->0xffffff8004878fff
            dependency: com.apple.iokit.IOStorageFamily(2.1)[5006F6CE-5418-3CE0-8B78-BA86707895E7]@0xffffff800497d000->0xffffff8004993fff

Process name corresponding to current thread (0xffffff9037ea0aa0): kernel_task
Boot args: keepsyms=1 debug=0x100 -lilubetaall ipc_control_port_options=0 -nokcmismatchpanic

Mac OS version:
21H1320

Kernel version:
Darwin Kernel Version 21.6.0: Mon Jun 24 00:56:10 PDT 2024; root:xnu-8020.240.18.709.2~1/RELEASE_X86_64
Kernel UUID: AAF3C70C-3331-335A-96FB-D338CFE178F0
KernelCache slide: 0x0000000001a00000
KernelCache base:  0xffffff8001c00000
Kernel slide:      0x0000000001a10000
Kernel text base:  0xffffff8001c10000
__HIB  text base: 0xffffff8001b00000
System model name: MacBookPro11,4 (Mac-06F11FD93F0323C5)
System shutdown begun: NO
Panic diags file available: YES (0x0)
Hibernation exit count: 0

System uptime in nanoseconds: 75688355050
Last Sleep:           absolute           base_tsc          base_nano
  Uptime  : 0x000000119f60bc0d
  Sleep   : 0x0000000000000000 0x0000000000000000 0x0000000000000000
  Wake    : 0x0000000000000000 0x000000050baacc66 0x0000000000000000
Compressor Info: 0% of compressed pages limit (OK) and 0% of segments limit (OK) with 0 swapfiles and OK swap space
Zone info:
  Zone map: 0xffffff8036f50000 - 0xffffffa036f50000
  . PGZ   : 0xffffff8036f50000 - 0xffffff8038f51000
  . VM    : 0xffffff8038f51000 - 0xffffff8505750000
  . RO    : 0xffffff8505750000 - 0xffffff869ef50000
  . GEN0  : 0xffffff869ef50000 - 0xffffff8b6b750000
  . GEN1  : 0xffffff8b6b750000 - 0xffffff9037f50000
  . GEN2  : 0xffffff9037f50000 - 0xffffff9504750000
  . GEN3  : 0xffffff9504750000 - 0xffffff99d0f50000
  . DATA  : 0xffffff99d0f50000 - 0xffffffa036f50000
  Metadata: 0xffffffa06f962000 - 0xffffffa08f962000
  Bitmaps : 0xffffffa08f962000 - 0xffffffa095962000

#### 解决方法

[参考](https://github.com/daliansky/XiaoMi-Pro-Hackintosh/issues/681)

- Try to enable NVMeFix.kext

- Set different `SetApfsTrimTimeout`, see OC Configuration for more detail.

  > sudo nvram -p | grep SetApfsTimeout   查看默认，默认是没这个设置的
  >
  > sudo nvram SetApfsTimeout=600 
  >
  > sudo nvram -d SetApfsTimeout 删除设置

- Change a compatible SSD

[这里有个参考](https://www.insanelymac.com/forum/topic/358947-possible-solution-loss-mmio-space/)

### PCI link down

panic(cpu 2 caller 0xffffff80033886a2): nvme: "3rd party NVMe controller. PCI link down. Delete IO submission queue. fBuiltIn=1 MODEL=INTEL SSDPEKNW010T8 FW=002C CSTS=0xffffffff US[1]=0x0 US[0]=0x110 VID=0xffff DID=0xffff CRITICAL_WARNING=0x0.\n" @IONVMeController.cpp:6090
