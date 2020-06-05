# No bean named 'org.springframework.context.annotation.ConfigurationClassPostProcessor.importRegistry' available

这是因为springboot检测到没有配置数据库导致的，可以在启动类加如下配置解决

(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})

