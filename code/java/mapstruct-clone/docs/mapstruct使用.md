# 简介

在一个成熟的工程中，尤其是现在的分布式系统中，应用与应用之间，还有单独的应用细分模块之后，DO 一般不会让外部依赖，这时候需要在提供对外接口的模块里放 DTO 用于对象传输，也即是 DO 对象对内，DTO对象对外，DTO 可以根据业务需要变更，并不需要映射 DO 的全部属性。

这种 对象与对象之间的互相转换，就需要有一个专门用来解决转换问题的工具，毕竟每一个字段都 get/set 会很麻烦。

MapStruct 就是这样的一个属性映射工具，只需要定义一个 Mapper 接口，MapStruct 就会自动实现这个映射接口，避免了复杂繁琐的映射实现。MapStruct官网地址： http://mapstruct.org/

#依赖

如果你使用maven，加入mapstruct依赖，另外，如果你还用了lombok，这里踩到一个坑，mapstruct和lombok一起使用不太友好，因为maven默认只使用了mapstruct的处理器，没有使用lombok的，所以需要加上path那段构建配置

```xml
   <dependencies>   
   <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-jdk8</artifactId>
        <version>${mapstruct.version}</version>
      </dependency>
      <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
      </dependency>
    </dependencies>

<build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.8.1</version>
                <configuration>
                    <source>1.8</source>
                    <target>1.8</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>${org.mapstruct.version}</version>
                        </path>
<!--mapstruct需要配置如下path-->
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
```

另外，对于@Data生成的get\set也不能识别(但在springboot项目是可以识别的)，需要使用@Getter/@Setter代替



# 和springboot集成

其实就是加入spring容器，成为一个可注入的组件

mapper接口的@Mapper注解，加一个参数componentModel="spring"即可

```java
@Mapper(componentModel="spring")
public interface BusinessFilterMapper {

  List<BusinessFilterBO> toBo(List<BusinessFilterVO> vos);

}
```

使用方法

```java
@RunWith(SpringRunner.class)
@SpringBootTest(classes= DatapiWebApplication.class)
@ActiveProfiles("dev")
public class PersonConverterTest {
  @Autowired
  private PersonConverter personConverter; //把Mapper注入进来
  @Test
  public void test() {
    Person person = new Person(1L,"zhige","zhige.me@gmail.com",new Date(),new User(1));
    PersonDTO personDTO = personConverter.domain2dto(person);
    System.out.println(person);
    System.out.println(personDTO);
    assertNotNull(personDTO);
    assertEquals(personDTO.getId(), person.getId());
    assertEquals(personDTO.getName(), person.getName());
    assertEquals(personDTO.getBirth(), person.getBirthday());
    String format = DateFormatUtils.format(personDTO.getBirth(), "yyyy-MM-dd HH:mm:ss");
    assertEquals(personDTO.getBirthDateFormat(),format);
    assertEquals(personDTO.getBirthExpressionFormat(),format);

  }
}
```

# 使用

## 定义基本的mapper

```java
@Mapper//注意这个注解
public interface CarMapper {

    @Mapping(source = "make", target = "manufacturer")
    @Mapping(source = "numberOfSeats", target = "seatCount")
    CarDto carToCarDto(Car car);

    @Mapping(source = "name", target = "fullName")
    PersonDto personToPersonDto(Person person);
}
```

@Mapper注解让MapStruct代码生成器在构建阶段创建一个CarMapper接口的实现

在生成的实现方法中，source 类型(Car)的所有可读属性都会被拷贝到target类型(CarDto)的相应属性：

1. 同名属性会自动隐式映射
2. 不同名的属性可以通过@Mapping注解指定
3. 如果某个属性你不想映射，可以加个 ignore=true

MapStruct的一般原理是生成尽可能多的代码，就像你自己亲自编写代码一样。特别地，这意味着通过普通的getter / setter调用而不是反射或类似的方法将值从源复制到目标。

### 给mapper添加自定义方法

如果使用java8,则我们可以直接在mapper里实现自定义方法，注意使用default修饰。如果参数和返回值均一样，则生成的实现代码将会调用这个default方法，例如

```java
@Mapper
public interface CarMapper {
    @Mapping(...)
    CarDto carToCarDto(Car car);
    default PersonDto personToPersonDto(Person person) {
        //自定义实现逻辑
    }
}
```

## 多对一

就是将多个do转成一个dto

```java
@Mapper
public interface ItemConverter {
    ItemConverter INSTANCE = Mappers.getMapper(ItemConverter.class);

    @Mappings({
            @Mapping(source = "sku.id",target = "skuId"),
            @Mapping(source = "sku.code",target = "skuCode"),
            @Mapping(source = "sku.price",target = "skuPrice"),
            @Mapping(source = "item.id",target = "itemId"),
            @Mapping(source = "item.title",target = "itemName")
    })
    SkuDTO domain2dto(Item item, Sku sku);   //注意这里，只要把接口写好，mapstruct也会自动生成实现
}
```



## 数据类型转换

当源和目标类型不一致时，例如在源中是int类型，而在目标中是Long类型。另外一种情况是对象属性映射到目标的其他类型的对象属性。

例如，Car类的Person类型的属性driver,在映射Car对象时需要转成PersonDto对象。

### 隐式类型转换

大多数情况下，mapstruct会自动处理类型转换，例如，源的int属性转成目的的String类型属性时，会自动调用String.valueOf(int)；反之会调用Integer.parseInt(String)。

目前如下场景会自动转换：

1. Java原生类型和其包装类型之间，例如int和Integer。
2. 原生number类型和包装类型
3. Java原生类型(包括包装类型)和String之间。例如，int和String, Boolean和String，可以使用java.text.DecimalFormat指定格式
4. 枚举类和String
5. BigXXX类型和String
6. Java.util.Date和String，java.text.SimpleDateFormat可以通过dateFormat指定

### 引用类型属性映射

例如，Car类有一个Person类型的属性，要转成CarDto类的PersonDto类型的属性。

这种情况，只需要定义相应对象类型的映射方法就行了：

```java
@Mapper
public interface CarMapper {
    CarDto carToCarDto(Car car);
    PersonDto personToPersonDto(Person person);
}
```

carToCarDto接口的实现方法在映射driver属性时，会调用personToPersonDto方法，而personToPersonDto的实现负责执行Person到PersonDto的转换。

事实上，在实现映射方法时，MapStruct会对源和目标的每个属性对儿执行如下路由:

1. 如果源和目标属性的类型相同，则直接拷贝值。集合类型也会拷贝。
2. 如果源和目标属性的类型不相同，检查是否存在相应的映射方法(把源类型转成目标类型)，存在则调用这个方法。
3. 如果不存在，则检查是否有内置转换，有则使用内置转换。
4. 如果也没有内置转换，MapStruct将尝试生成一个自动子映射方法完成属性映射
5. 如果MapStruct无法创建基于名称的映射方法，则会抛出一个异常。

可以使用@Mapper( disableSubMappingMethodsGeneration = true )阻止MapStruct生成自动子映射方法

### 控制内嵌bean映射

直接使用.操作符就行

```java
@Mapper
public interface FishTankMapper {

    @Mapping(target = "fish.kind", source = "fish.type")
    @Mapping(target = "fish.name", ignore = true)
    @Mapping(target = "ornament", source = "interior.ornament")
    @Mapping(target = "material.materialType", source = "material")
    @Mapping(target = "quality.report.organisation.name", source = "quality.report.organisationName")
    FishTankDto map( FishTank source );
}
```

### 使用表达式

例如把String属性转成LocalDateTime属性，可以通过表达式指定我们自己写的方法

```java
  @Mapping(target = "createTime", expression = "java(com.java.mmzsblog.util.DateTransform.strToDate(source.getCreateTime()))"),
```



### 调用其他映射器

除了在同一映射器类型上定义的方法之外，MapStruct还可以调用其他类中定义的映射方法，无论是MapStruct生成的映射器还是手写映射方法。这对于在多个类中构建映射代码（例如，每个应用程序模块使用一个映射器类型）或者如果要提供MapStruct无法生成的自定义映射逻辑非常有用。

例如，`Car`类可能包含一个属性，`manufacturingDate`而相应的DTO属性是String类型。为了映射这个属性，你可以像这样实现一个mapper类：

```java
public class DateMapper {

    public String asString(Date date) {
        return date != null ? new SimpleDateFormat( "yyyy-MM-dd" )
            .format( date ) : null;
    }

    public Date asDate(String date) {
        try {
            return date != null ? new SimpleDateFormat( "yyyy-MM-dd" )
                .parse( date ) : null;
        }
        catch ( ParseException e ) {
            throw new RuntimeException( e );
        }
    }
}
```

在接口的`@Mapper`注释中`CarMapper`引用`DateMapper`类如下

```java
@Mapper(uses=DateMapper.class)
public class CarMapper {
    CarDto carToCarDto(Car car);
}
```

在为`carToCarDto()`方法的实现生成代码时，MapStruct将寻找一种方法，该方法将`Date`对象映射到String中，在`DateMapper`类上找到它并生成`asString()`用于映射`manufacturingDate`属性的调用。

## 映射集合

集合类型的映射方式和bean类型一样，即在mapper接口中定义相应的映射方法(参数和返回类型)。生成的代码会循环源集合，把每个元素转换后放到目的集合中。例如

```java
@Mapper
public interface CarMapper {
    Set<String> integerSetToStringSet(Set<Integer> integers);
    List<CarDto> carsToCarDtos(List<Car> cars); //会用到下面👇的那个方法
    CarDto carToCarDto(Car car);
}
```

### 映射map

使用@MapMapping注解

```java
public interface SourceTargetMapper {
    @MapMapping(valueDateFormat = "dd.MM.yyyy")
    Map<String, String> longDateMapToStringStringMap(Map<Long, Date> source);
}
```

# 总结

通过上面的介绍，可以发现MapStruct 是一个属性映射工具，只需要定义一个 Mapper 接口，MapStruct 就会自动实现这个映射接口，避免了复杂繁琐的映射实现。比Spring的BeanUtils更方便，功能更强大。

这个注解，我猜测是往向反的方向转时，映射关系和正向映射正好相反，即正向的source是反向的target，反之亦然。

```java
@InheritInverseConfiguration
```

### 参考

[参考](https://juejin.im/entry/6844903620874338317)

[参考2](https://juejin.im/entry/6844903661907214344)

[中文指南](http://www.kailing.pub/MapStruct1.3/index.html#mapping-collections)