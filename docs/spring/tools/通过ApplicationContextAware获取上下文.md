其实以实现`ApplicationContextAware`接口的方式获取`ApplicationContext`对象实例并不是SpringBoot特有的功能，早在Spring3.0x版本之后就存在了这个接口，在传统的`Spring`项目内同样是可以获取到`ApplicationContext`实例的

# 实现ApplicationContextAware接口

创建一个实体类并实现`ApplicationContextAware`接口，重写接口内的`setApplicationContext`方法来完成获取`ApplicationContext`实例的方法，代码如下所示：

这里要注意`ApplicationContextProvider`类上的`@Component`注解是不可以去掉的，去掉后`Spring`就不会自动调用`setApplicationContext`方法来为我们设置上下文实例。

```java
package com.xunmei.api;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class ApplicationContextProvider
    implements ApplicationContextAware
{
    /**
     * 上下文对象实例
     */
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * 获取applicationContext
     * @return
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 通过name获取 Bean.
     * @param name
     * @return
     */
    public Object getBean(String name){
        return getApplicationContext().getBean(name);
    }

    /**
     * 通过class获取Bean.
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }

    /**
     * 通过name,以及Clazz返回指定的Bean
     * @param name
     * @param clazz
     * @param <T>
     * @return
     */
    public <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }
}
```

