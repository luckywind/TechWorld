# SpringData Repository

Spring Data Repository抽象大大减少了数据获取层的实现代码

## 核心概念

核心抽象接口是Repository，把领域类和ID类型作为参数。它主要是作为一个标记接口用于获取类型获取以及帮助我们发现子接口。CrudRepositry提供了CRUD功能。

CrudRepository接口

```scala
public interface CrudRepository<T, ID> extends Repository<T, ID> {
  <S extends T> S save(S entity);      
  Optional<T> findById(ID primaryKey); 
  Iterable<T> findAll();               
  long count();                        
  void delete(T entity);               
  boolean existsById(ID primaryKey);   
  // … more functionality omitted.
}
```

同时我们提供了特定技术的持久化抽象，例如JpaRepository和MongoRepository，他们都继承自CrudRepository。

此外，还有PagingAndSortingRepository抽象继承自CrudRepository。

PagingAndSortingRepository接口

```java
public interface PagingAndSortingRepository<T, ID> extends CrudRepository<T, ID> {
  Iterable<T> findAll(Sort sort);
  Page<T> findAll(Pageable pageable);
}
//获取第二页User(page size=20)
PagingAndSortingRepository<User, Long> repository = // … get access to a bean
Page<User> users = repository.findAll(PageRequest.of(1, 20));
```

统计接口

```java
interface UserRepository extends CrudRepository<User, Long> {
  long countByLastname(String lastname);
}
```

删除接口

```java
interface UserRepository extends CrudRepository<User, Long> {
  long deleteByLastname(String lastname);
  List<User> removeByLastname(String lastname);
}
```

## 查询方法

查询接口的声明分为四个步骤

1. 声明一个接口，继承Repository或者它的子接口，并传入领域对象类型和它的ID类型

   ```java 
   interface PersonRepository extends Repository<Person, Long> { … }
   ```

2. 声明查询方法

     ```java
   interface PersonRepository extends Repository<Person, Long> {
     List<Person> findByLastname(String lastname);
   }
   ```

3. 为这些接口设置Spring代理实例，可使用JavaConfig或者XML配置

​             a. 使用JavaConfig

```java
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
@EnableJpaRepositories
class Config { … }
```

​	         b.使用XML配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://www.springframework.org/schema/beans"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xmlns:jpa="http://www.springframework.org/schema/data/jpa"
   xsi:schemaLocation="http://www.springframework.org/schema/beans
     https://www.springframework.org/schema/beans/spring-beans.xsd
     http://www.springframework.org/schema/data/jpa
     https://www.springframework.org/schema/data/jpa/spring-jpa.xsd">
   <jpa:repositories base-package="com.acme.repositories"/>
</beans>
```

4. 注入repository实例并使用

```java
class SomeClient {
  private final PersonRepository repository;
  SomeClient(PersonRepository repository) {
    this.repository = repository;
  }
  void doSomething() {
    List<Person> persons = repository.findByLastname("Matthews");
  }
}
```

## 定义Repository接口

通常repository接口要继承Repository`, `CrudRepository`, or `PagingAndSortingRepository，但是也可以不继承，而使用注解@RepositoryDefinition注释你的接口类。

### 自定义接口

使用注解@NoRepositoryBean阻止Spring Data创建实例

```java
@NoRepositoryBean
interface MyBaseRepository<T, ID> extends Repository<T, ID> {
  Optional<T> findById(ID id);
  <S extends T> S save(S entity);
}
interface UserRepository extends MyBaseRepository<User, Long> {
  User findByEmailAddress(EmailAddress emailAddress);
}
```

### 多个Spring Data模块下使用Repository

当有多个SpringData模块时，repository定义必须在持久化技术之间区分开。当检测到多个repository工厂时，Spring Data进入严格reposiory配置模式，严格模式使用repository细节或者领域类细节决定绑定到repository定义的Spring Data模块：

1. 如果repository定义继承了模块特定repository，那它就是该Spring Data模块的候选者。
2. 如果领域类使用特定类型注解注解了，那它就是特定Spring Data模块的有效候选者。

下面的例子是一个使用特定模块接口(本例是JPA)的repository

```java
interface MyRepository extends JpaRepository<User, Long> { }

@NoRepositoryBean
interface MyBaseRepository<T, ID> extends JpaRepository<T, ID> { … }

interface UserRepository extends MyBaseRepository<User, Long> { … }
```

MyRepository和 UserRepository 都继承了JpaRepository，都是SpringData JPA模块的有效候选者。

下面的例子里，AmbiguousRepository和AmbiguousUserRepository只继承了Repository和CrudRepository，在单个SpringData模块下是没有问题的，但是多个Spring Data模块时就无法区分这些repository该绑定到哪个Spring Data上了。


```java
interface AmbiguousRepository extends Repository<User, Long> { … }
@NoRepositoryBean
interface MyBaseRepository<T, ID> extends CrudRepository<T, ID> { … }
interface AmbiguousUserRepository extends MyBaseRepository<User, Long> { … }
```

下面的例子里，PersonRepository引用的Person类使用了注解@Entity，表明这个repository属于Spring Data JPA 。 而UserRepository引用的User类使用了注解@Document，表明这个repository属于Spring Data MongoDB。

最后一种区分不同repository的方法是使用base package，base package定义了repository接口的扫描入口。

注解驱动配置默认使用配置类所在的包作为base package。下面的例子配置了base package

```java
@EnableJpaRepositories(basePackages = "com.acme.repositories.jpa")
@EnableMongoRepositories(basePackages = "com.acme.repositories.mongo")
class Configuration { … }
```

## 定义查询方法

repository代理有两种方法从方法名生成存储sql：

1. 直接从方法名生成查询
2. 使用手动定义查询

以下描述可用的选项

### Query Lookup策略

xml配置里，可以在anmespace里通过query-lookup-strategy属性配置；如果使用Java配置，可以使用注解Enable${store}Repositories的queryLookupStrategy属性配置。

1. CREATE 尝试从方法名解析查询，方法是删除方法前缀，并解析剩余部分
2. USE_DECLARED_QUERY 尝试查找声明的查询。找不到则报错
3. CREATE_IF_NOT_FOUND(默认的)，组合了CREATE和USE_DECLARED_QUERY。

### 查询创建

例子

```java 
interface PersonRepository extends Repository<Person, Long> {

  List<Person> findByEmailAddressAndLastname(EmailAddress emailAddress, String lastname);

  // Enables the distinct flag for the query
  List<Person> findDistinctPeopleByLastnameOrFirstname(String lastname, String firstname);
  List<Person> findPeopleDistinctByLastnameOrFirstname(String lastname, String firstname);

  // Enabling ignoring case for an individual property
  List<Person> findByLastnameIgnoreCase(String lastname);
  // Enabling ignoring case for all suitable properties
  List<Person> findByLastnameAndFirstnameAllIgnoreCase(String lastname, String firstname);

  // Enabling static ORDER BY for a query
  List<Person> findByLastnameOrderByFirstnameAsc(String lastname);
  List<Person> findByLastnameOrderByFirstnameDesc(String lastname);
}
```

### 属性表达式

考虑如下查询方法

```java
List<Person> findByAddressZipCode(ZipCode zipCode);
```

假设Person有Address和ZipCode两个属性，这个方法会创建x.address.zipCode的查询。算法是这样的：首先把第一个By后的AddressZipCode作为一个属性并检查Person是否有这个属性，如果有则使用。否则，算法从后往前按照驼峰把AddressZipCode切分为两部分，即AddressZip和Code。如果Person有第一部分AddressZip，则把第二部分继续为分。如果第一部分不是一个属性，则按照驼峰向左移动切点(Address, ZipCode)并继续。感觉有点像贪婪算法啊。。。。即从最左边最长匹配一个属性。

大多数情况下，这是正确的，这中方法是有可能选择错误的属性的，例如，Person有addressZip属性，这种算法最后会把Code作为addressZip的一个属性，但是addressZip很可能没有这个属性。 解决办法是使用_手动定义切点：

```java
List<Person> findByAddress_ZipCode(ZipCode zipCode);
```

### 特殊参数处理

使用Pagable/Slice和Sort查询

```java
Page<User> findByLastname(String lastname, Pageable pageable);
Slice<User> findByLastname(String lastname, Pageable pageable);
List<User> findByLastname(String lastname, Sort sort);
List<User> findByLastname(String lastname, Pageable pageable);
```

如果不想排序和分页，可以传入`sort.unsorted()` 和 `Pageable.unpaged()`.

Page需要知道总记录数，开销较大，而Slice只需要知道是否还有下一个Slice，因此开销较小。

### Limit语句

使用Top和First

```java
User findFirstByOrderByLastnameAsc();

User findTopByOrderByAgeDesc();

Page<User> queryFirst10ByLastname(String lastname, Pageable pageable);

Slice<User> findTop3ByLastname(String lastname, Pageable pageable);

List<User> findFirst10ByLastname(String lastname, Sort sort);

List<User> findTop10ByLastname(String lastname, Pageable pageable);
```

## 创建查询实例

有xml方式和Java配置方式，推荐Java配置

### xml配置

通过xml开启Spring Data repository

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans:beans xmlns:beans="http://www.springframework.org/schema/beans"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns="http://www.springframework.org/schema/data/jpa"
  xsi:schemaLocation="http://www.springframework.org/schema/beans
    https://www.springframework.org/schema/beans/spring-beans.xsd
    http://www.springframework.org/schema/data/jpa
    https://www.springframework.org/schema/data/jpa/spring-jpa.xsd">
  <repositories base-package="com.acme.repositories" />
</beans:beans>
```

Spring会扫描base-package下面的repository并使用每个repository相关的FactoryBean创建相应的代理处理查询方法。每个代理bean的名字是首字母小写的repository的名字。例如UserRepository的代理会被注册为名字为userRepository的bean.

### java配置

可以通过在Java配置类上使用注解@Enable${store}Repositories，下面开启Spring repository

```java
@Configuration
@EnableJpaRepositories("com.acme.repositories")
class ApplicationConfiguration {
  @Bean
  EntityManagerFactory entityManagerFactory() {
    // …
  }
}
```

### 独立使用

可以脱离spring容器使用repository,使用RepositoryFactory

```java
RepositoryFactorySupport factory = … // Instantiate factory here
UserRepository repository = factory.getRepository(UserRepository.class);
```

## 自定义Spring Data Repository的实现

### 自定义repository

为了使用自定义的功能增强一个repository,需要定义一个接口和一个实现，例如；

```java
interface CustomizedUserRepository {
  void someCustomMethod(User user);
}
//实现---注意名字的Impl后缀
class CustomizedUserRepositoryImpl implements CustomizedUserRepository {
  public void someCustomMethod(User user) {
    // Your custom implementation
  }
}
```

这个实现类不依赖于Spring Data，且可以注册为一个bean。然后注入到其他bean中。

然后我们可以让repository接口继承这个接口（同时也可以继承crudRepository接口）：

```java
interface UserRepository extends CrudRepository<User, Long>, CustomizedUserRepository {
  // Declare query methods here
}
```

这样这个repository不仅有了CurdRepository的功能也有了自定义的someCustomMethod功能。

通过这种办法，我们可以方便的给我们的repository增加功能：只要把自定义功能生声明到一个接口中，提供它的实现类，并让我们的repository继承这个接口即可！！

自定义接口中的方法会覆盖base实现，且按照其声明顺序引入

# Spring Data扩展

### web支持

在Java配置类上添加注解@EnableSpringDataWebSupport开启

```java
@Configuration
@EnableWebMvc
@EnableSpringDataWebSupport
class WebConfiguration {}
```

#### 基本的web支持

@EnableSpringDataWebSupport会注册一些基本组件：

1. DomainClassConverter让springMVC从请求参数或者路径变量中解析repository管理的领域类
2. HandlerMethodArgumentResolvers实现让SpringMVC从请求参数解析Pageable和Sort实例

**DomainClassConverter**

让我们在SpringMVC控制方法签名中直接使用领域类型

下面的实例中，方法直接接收一个User实例。SpringMVC首先把路径变量id解析为User的id类型，然后在repository实例上调用findById获得实例

```java
@Controller
@RequestMapping("/users")
class UserController {

  @RequestMapping("/{id}")
  String showUserForm(@PathVariable("id") User user, Model model) {
    model.addAttribute("user", user);
    return "userForm";
  }
}
```

# @EnableJpaRepositories配置详解

## 简单配置

```
1 @EnableJpaRepositories("com.spr.repository")
```

简单配置支持多个package，格式如下：

```
1 @EnableJpaRepositories({"com.cshtong.sample.repository", "com.cshtong.tower.repository"})
```

注：单值和多组值配置方式

大部分注解可以都支持单个注解方式和多个注解，多个注解通常采用"{}"符号包含的一组数据。

比如：字符串形式的  "x.y.z"  =>  {"x.y.z","a.b.c"}

类别： A.class => {A.class, B.class}

## 完整注解

```java
1 @EnableJpaRepositories(
 2     basePackages = {},//配置扫描Repositories所在的package及子package
 3     basePackageClasses = {},//指定 Repository 类
 4     includeFilters = {},//过滤器，该过滤区采用ComponentScan的过滤器类includeFilters={@ComponentScan.Filter(type=FilterType.ANNOTATION, value=Repository.class)}
 5     excludeFilters = {},//不包含过滤器
 6     repositoryImplementationPostfix = "Impl",//实现类名后缀
 7     namedQueriesLocation = "",//META-INF/jpa-named-queries.properties
    /**
      构建条件查询的策略，包含三种方式CREATE，USE_DECLARED_QUERY，CREATE_IF_NOT_FOUND
    CREATE：按照接口名称自动构建查询
    USE_DECLARED_QUERY：用户声明查询
    CREATE_IF_NOT_FOUND：先搜索用户声明的，不存在则自动构建
      */
 8     queryLookupStrategy=QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND, //QueryLookupStrategy.Key.x
 9     repositoryFactoryBeanClass=JpaRepositoryFactoryBean.class, //指定Repository的工厂类
10     entityManagerFactoryRef="entityManagerFactory",  //实体管理工厂引用名称，对应到@Bean注解对应的方法
11     transactionManagerRef="transactionManager",  //事务管理工厂引用名称，对应到@Bean注解对应的方法
12     considerNestedRepositories=false, 
13     enableDefaultTransactions=true
14 )
```

