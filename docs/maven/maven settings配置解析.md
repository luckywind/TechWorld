# settings文件解析

settings.xml是maven的全局配置文件。而pom.xml文件是所在项目的局部配置。

## setting.xml文件位置

settings.xml文件一般存在于两个位置：

- 全局配置: ${M2_HOME}/conf/settings.xml
- 用户配置: user.home/.m2/settings.xml

前者叫做全局配置，对操作系统的所有使用者生效；后者为用户配置，只对当前操作系统的使用者生效。

## 配置文件优先级

局部配置优先于全局配置。

配置优先级从高到低：pom.xml> user settings > global settings

如果这些文件同时存在，在应用配置时，会合并它们的内容，如果有重复的配置，优先级高的配置会覆盖优先级低的。如果全局配置和用户配置都存在，它们的内容将被合并，并且用户范围的settings.xml会覆盖全局的settings.xml。







[原文](https://sq.163yun.com/blog/article/170713992193314816)