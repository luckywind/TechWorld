有时候我们会遇到这样的场景： 脱离Spring容器创建的类，它怎么注入Spring的Bean呢？AutowireCapableBeanFactory就是为了解决这个问题的

它是BeanFactory的扩展，可以通过ApplicationContext的getAutowireCapableBeanFactory()方法获取。

下面给出一个demo

```java
public final class AutowiringSpringBeanJobFactory extends SpringBeanJobFactory implements ApplicationContextAware {

    private transient AutowireCapableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(final ApplicationContext context) {
        beanFactory = context.getAutowireCapableBeanFactory();//获取实例
    }
/**
 这里的场景是注入quartz框架创建Job实例
*/
    @Override
    protected Object createJobInstance(final TriggerFiredBundle bundle) throws Exception {
        final Object job = super.createJobInstance(bundle);
        beanFactory.autowireBean(job);     //自动织入外部框架创建的类
        return job;
    }
}
```

