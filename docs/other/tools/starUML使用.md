[mac破解staruml](https://www.cnblogs.com/gujiande/p/9412027.html)

[StarUML使用说明-指导手册](https://blog.csdn.net/monkey_d_meng/article/details/5995610)

# 介绍

- project： 顶层元素存储在一个.mdj文件中，  包含多个Model
- Model: 软件构建块儿，包含多个图表
- Diagram:图表是模型的可视化表示，用于呈现和展示模型的特定方面。

# 画类图：

双击Class可以看到这么多快捷添加按钮，每个按钮对应不同的东西

![image-20231102102936935](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231102102936935.png)

[视频](https://www.bilibili.com/video/BV1Kh4y1S72z/?p=3&spm_id_from=pageDriver&vd_source=fa2aaef8ece31d2c310d46092c301b46), [官方文档](https://docs.staruml.io/working-with-uml-diagrams/class-diagram)

类可右键添加Attribute、Operation

Operation(方法)，右键可添加Parameter，Attribute(返回值类型)

![image-20231030155112841](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231030155112841.png)

## 接口的显示

![image-20231031144549293](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231031144549293.png)

### 显示接口里的方法：

format->Supress Operations

## markdown documentation

是一个extension，作用其实就是给一个Class以markdown的语法写document

## 常用关系

- dependency: 依赖关系，成员变量、局部变量
- association: 关联关系，比较弱的关系，如森林里有老虎
- aggregation: 聚合关系，是关联关系的一种，强相关，如雁群与雁
- composition: 组合/复合/合成关系，是关联关系的一种，如人与头
- realization: 实现，implements
- generalization:泛化，extends

# 用例图

作用：从不同的系统角色角度展示了系统的功能需求

常用的关系：
include:从保护用例指向被包含用例， 如A包含B，不代表B是A的子用例， 而表示B这个用例是执行A的前提或B是公用用例

extend: 从扩展也给了你指向被扩展用例，扩展用例是在一定条件下触发的，如交罚金用例扩展自归还书籍用例

![image-20231101091210682](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20231101091210682.png)

# 时序图

箭头表示：谁调用谁































