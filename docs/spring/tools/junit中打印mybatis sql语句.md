只需要两步就可以： 

1. 定义一个sql语句拦截器
2. 把拦截器注入到容器中

# 拦截器

```java
package com..data.config;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.util.Properties;


/**
 * 打印sql语句的插件
 * 在方法上有一个很重要的注解@Intercepts，
 * 在此注解上配置的注解说明了
 * 要拦截的类（type=StatementHandler.class），
 * 拦截的方法（method="prepare"），
 * 方法中的参数（args={Connection.class,Integer.class}），
 * 也就是此拦截器会拦截StatementHandler类中的如下方法：
 * Statement prepare(Connection connection, Integer transactionTimeout)
 */
@Intercepts({@Signature(type= StatementHandler.class,method="prepare",args={Connection.class,Integer.class})})
public class SQLStatsInterceptor implements Interceptor {

    /**
     * 在这个方法中可以获取到对应的绑定的sql
     * @param invocation
     * @return
     * @throws Throwable
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler= (StatementHandler) invocation.getTarget();
        BoundSql boundSql = statementHandler.getBoundSql();
        System.out.println(boundSql.getSql());
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
        String dialect = properties.getProperty("dialect");
        System.out.println("dialect="+dialect);
    }
}

```



# 配置类

```java
package com.xiaomi.data.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@Configuration
public class MyBatisConfiguration {
    @Bean
    public SQLStatsInterceptor sqlStatsInterceptor(){
        SQLStatsInterceptor sqlStatsInterceptor = new SQLStatsInterceptor();
        Properties properties = new Properties();
        properties.setProperty("dialect", "mysql");
        sqlStatsInterceptor.setProperties(properties);
        return sqlStatsInterceptor;
    }
}

```

# 测试

```java
package com.xiaomi.data;

import com.xiaomi.data.model.OdcHiveTable;
import com.xiaomi.data.service.HiveTableService;
import com.xiaomi.data.service.OdcHiveTableService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("dev")
public class SqlTool {
    @Autowired
    OdcHiveTableService odcHiveTableService;

    @Test
    public void sqlTest() {
        OdcHiveTable odcHiveTable = odcHiveTableService.selectByPrimaryKey(1);
        System.out.println(odcHiveTable);
    }

    @Test
    public void printSql() {
        OdcHiveTable odcHiveTable = odcHiveTableService.selectByPrimaryKey(1);
        System.out.println(odcHiveTable);
    }
}
```

将打印sql语句：

```sql
select 
    id, `name`, cn_name, app_id, datatype, hdfs_file_struct_id, keytab, `state`, table_id, 
    perm_state, preserve_time, cluster_id, service, create_time, update_time
    from odc_hive_table
    where id = ?
```

