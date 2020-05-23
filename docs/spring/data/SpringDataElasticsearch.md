SpingData通常使用高级抽象：[Elasticsearch Operations](https://docs.spring.io/spring-data/elasticsearch/docs/3.2.4.RELEASE/reference/html/#elasticsearch.operations) and [Elasticsearch Repositories](https://docs.spring.io/spring-data/elasticsearch/docs/3.2.4.RELEASE/reference/html/#elasticsearch.repositories).

# Transport Client

该接口会在es8中被删除掉，推荐使用高级Rest Client代替

 **Transport Client**

```java
static class Config {

  @Bean
  Client client() {
  	Settings settings = Settings.builder()
  	  .put("cluster.name", "elasticsearch")   
      .build();
  	TransportClient client = new PreBuiltTransportClient(settings);
    client.addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1")
      , 9300));                               
    return client;
  }
}

// ...

IndexRequest request = new IndexRequest("spring-data", "elasticsearch", randomID())
 .source(someObject)
 .setRefreshPolicy(IMMEDIATE);

IndexResponse response = client.index(request);
```

# 高级REST Client

异步调用使用客户端管理的线程池且需要一个callback来告知请求完成。

高级REST Client

```java
import org.springframework.beans.factory.annotation.Autowired;@Configuration
static class Config {
  @Bean
  RestHighLevelClient client() {
    ClientConfiguration clientConfiguration = ClientConfiguration.builder() 
      .connectedTo("localhost:9200", "localhost:9201")
      .build();
    return RestClients.create(clientConfiguration).rest();                  
  }
}

// ...

  @Autowired
  RestHighLevelClient highLevelClient;
//通过highLevelClient也可以获得一个lowLevelClient
  RestClient lowLevelClient = highLevelClient.lowLevelClient();             
// ...

IndexRequest request = new IndexRequest("spring-data", "elasticsearch", randomID())
  .source(singletonMap("feature", "high-level-rest-client"))
  .setRefreshPolicy(IMMEDIATE);

IndexResponse response = highLevelClient.index(request);
```

# Reactive Client

ReactiveElasticsearchClient是基于WebClient的非官方驱动，使用es提供的request/response对象，调用是直接操作reactive stack

```java
static class Config {

  @Bean
  ReactiveElasticsearchClient client() {

    ClientConfiguration clientConfiguration = ClientConfiguration.builder()   
      .connectedTo("localhost:9200", "localhost:9291")
      .withWebClientConfigurer(webClient -> {                                 
        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
            .codecs(configurer -> configurer.defaultCodecs()
                .maxInMemorySize(-1))
            .build();
        return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
       })
      .build();

    return ReactiveRestClients.create(clientConfiguration);
  }
}

// ...

Mono<IndexResponse> response = client.index(request ->

  request.index("spring-data")
    .type("elasticsearch")
    .id(randomID())
    .source(singletonMap("feature", "reactive-client"))
    .setRefreshPolicy(IMMEDIATE);
);
```

# 客户端配置

```java
// optional if Basic Auhtentication is needed
HttpHeaders defaultHeaders = new HttpHeaders();
defaultHeaders.setBasicAuth(USER_NAME, USER_PASS);                      

ClientConfiguration clientConfiguration = ClientConfiguration.builder()
  .connectedTo("localhost:9200", "localhost:9291")                      
  .withConnectTimeout(Duration.ofSeconds(5))                            
  .withSocketTimeout(Duration.ofSeconds(3))                             
  .useSsl()                                                             
  .withDefaultHeaders(defaultHeaders)                                   
  .withBasicAuth(username, password)                                    
  . // ... other options
  .build();
```

# 客户端日志

**Enable transport layer logging**

```java
<logger name="org.springframework.data.elasticsearch.client.WIRE" level="trace"/>
```

# Es 对象映射

Springdata es 通过如下两个EntityMapper接口允许两种映射实现：

- [Jackson Object Mapping](https://docs.spring.io/spring-data/elasticsearch/docs/3.2.4.RELEASE/reference/html/#elasticsearch.mapping.jackson2)
- [Meta Model Object Mapping](https://docs.spring.io/spring-data/elasticsearch/docs/3.2.4.RELEASE/reference/html/#elasticsearch.mapping.meta-model)

## Jackson Object Mapping

基于Jackson2的方法(默认)使用一个自定义的ObjectMapper实例配合Springdata特定模块。对实际mapping的扩展需要通过jackson注解如@JsonInclude自定义

**Jackson2 Object Mapping Configuration**

```java
@Configuration
public class Config extends AbstractElasticsearchConfiguration { 
/**
AbstractElasticsearchConfiguration already defines a Jackson2 based entityMapper 
via ElasticsearchConfigurationSupport.
*/
  @Override
  public RestHighLevelClient elasticsearchClient() {
    return RestClients.create(ClientConfiguration.create("localhost:9200")).rest();
  }
}
```

## 元数据模型对象映射

基于元数据模型的方法使用领域类型信息读写es，为特定领域类型映射注册Converter 实例

**Meta Model Object Mapping Configuration**

```java
@Configuration
public class Config extends AbstractElasticsearchConfiguration {

  @Override
  public RestHighLevelClient elasticsearchClient() {
    return RestClients.create(ClientConfiguration.create("localhost:9200")).rest()
  }

  @Bean
  @Override
  public EntityMapper entityMapper() {  //覆盖默认的EntityMapper                               
    ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(
      elasticsearchMappingContext(), new DefaultConversionService()    
    );
    entityMapper.setConversions(elasticsearchCustomConversions());     

  	return entityMapper;
  }
}
```

### 映射注解一览

ElasticsearchEntityMapper可以使用元数据驱动对象到文档的映射。

- `@Id`: Applied at the field level to mark the field used for identity purpose.
- `@Document`: Applied at the class level to indicate this class is a candidate for mapping to the database. The most important attributes are:
  - `indexName`: the name of the index to store this entity in
  - `type`: the mapping type. If not set, the lowercased simple name of the class is used.
  - `shards`: the number of shards for the index.
  - `replicas`: the number of replicas for the index.
  - `refreshIntervall`: Refresh interval for the index. Used for index creation. Default value is *"1s"*.
  - `indexStoreType`: Index storage type for the index. Used for index creation. Default value is *"fs"*.
  - `createIndex`: Configuration whether to create an index on repository bootstrapping. Default value is *true*.
  - `versionType`: Configuration of version management. Default value is *EXTERNAL*.
- `@Transient`: By default all private fields are mapped to the document, this annotation excludes the field where it is applied from being stored in the database
- `@PersistenceConstructor`: Marks a given constructor - even a package protected one - to use when instantiating the object from the database. Constructor arguments are mapped by name to the key values in the retrieved Document.
- `@Field`: Applied at the field level and defines properties of the field, most of the attributes map to the respective [Elasticsearch Mapping](https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html) definitions:
  - `name`: The name of the field as it will be represented in the Elasticsearch document, if not set, the Java field name is used.
  - `type`: the field type, can be one of *Text, Integer, Long, Date, Float, Double, Boolean, Object, Auto, Nested, Ip, Attachment, Keyword*.
  - `format` and `pattern` custom definitions for the *Date* type.
  - `store`: Flag wether the original field value should be store in Elasticsearch, default value is *false*.
  - `analyzer`, `searchAnalyzer`, `normalizer` for specifying custom custom analyzers and normalizer.
  - `copy_to`: the target field to copy multiple document fields to.
- `@GeoPoint`: marks a field as *geo_point* datatype. Can be omitted if the field is an instance of the `GeoPoint` class.

## 映射规则

### 类型提示

Mapping使用类型提示嵌入文档中发送给服务器以允许范型类型mapping。这些类型提示使用_class属性表示

```java
public class Person {              

  @Id String id;
  String firstname;
  String lastname;
}

{
  "_class" : "com.example.Person", //类型提示
  "id" : "cb7bef",
  "firstname" : "Sarah",
  "lastname" : "Connor"
}
  
  
```

也可使用别名

```java
@TypeAlias("human")                
public class Person {

  @Id String id;
  // ...
}
{
  "_class" : "human",              
  "id" : ...
}
```

# Elasticsearch 操作


ElasticsearchOperations和ReactiveElasticsearchOperations两个接口，前者使用同步实现，后者使用reactive

# ElasticsearchTemplate

ElasticsearchTemplate是ElasticsearchOperations接口的一个实现(使用Transport Client)

ElasticsearchTemplate配置

```java
@Configuration
public class TransportClientConfig extends ElasticsearchConfigurationSupport {

  @Bean
  public Client elasticsearchClient() throws UnknownHostException {                 
    Settings settings = Settings.builder().put("cluster.name", "elasticsearch").build();
    TransportClient client = new PreBuiltTransportClient(settings);
    client.addTransportAddress(new TransportAddress(InetAddress.getByName("127.0.0.1"), 9300));
    return client;
  }

  @Bean(name = {"elasticsearchOperations", "elasticsearchTemplate"})
  public ElasticsearchTemplate elasticsearchTemplate() throws UnknownHostException { 
  	return new ElasticsearchTemplate(elasticsearchClient(), entityMapper());
  }

  // use the ElasticsearchEntityMapper
  @Bean
  @Override
  public EntityMapper entityMapper() {                                               
    ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(elasticsearchMappingContext(),
  	  new DefaultConversionService());
    entityMapper.setConversions(elasticsearchCustomConversions());
    return entityMapper;
  }
}

```

# ElasticsearchRestTemplate

ElasticsearchRestTemplate是ElasticsearchOperations接口的实现，使用高级REST Client。

**ElasticsearchRestTemplate configuration**

```java
@Configuration
public class RestClientConfig extends AbstractElasticsearchConfiguration {
  @Override
  public RestHighLevelClient elasticsearchClient() {       
    return RestClients.create(ClientConfiguration.localhost()).rest();
  }

  // no special bean creation needed                       
//因为AbstractElasticsearchConfiguration已经提供elasticsearchTemplate bean了
  // use the ElasticsearchEntityMapper
  @Bean
  @Override
  public EntityMapper entityMapper() {                     
    ElasticsearchEntityMapper entityMapper = new ElasticsearchEntityMapper(elasticsearchMappingContext(),
        new DefaultConversionService());
    entityMapper.setConversions(elasticsearchCustomConversions());

    return entityMapper;
  }
}
```

# 使用示例

ElasticsearchTemplate和ElasticsearchRestTemplate都实现了ElasticsearchOperations接口，所以使用代码都是一样的，下面的例子展示了如何在Spring REST Controller中使用注入的ElasticsearchOperations实例。 具体使用哪个实现是根据配置来的。

**ElasticsearchOperations 使用**

```java
@RestController
@RequestMapping("/")
public class TestController {

  private  ElasticsearchOperations elasticsearchOperations;

  public TestController(ElasticsearchOperations elasticsearchOperations) { 
    this.elasticsearchOperations = elasticsearchOperations;
  }

  @PostMapping("/person")
  public String save(@RequestBody Person person) {                         

    IndexQuery indexQuery = new IndexQueryBuilder()
      .withId(person.getId().toString())
      .withObject(person)
      .build();
    String documentId = elasticsearchOperations.index(indexQuery);
    return documentId;
  }

  @GetMapping("/person/{id}")
  public Person findById(@PathVariable("id")  Long id) {                   
    Person person = elasticsearchOperations
      .queryForObject(GetQuery.getById(id.toString()), Person.class);
    return person;
  }
}
```

# Elasticsearch Repository

本章主要包含elastic search repository实现

# 查询方法

## Query lookup策略

 Elasticsearch module支持所有基本查询构建特性，如string查询，原生搜索查询和方法名查询。

### 查询创建

下面的例子解释es query method怎么转化为查询

```java
interface BookRepository extends Repository<Book, String> {
  List<Book> findByNameAndPrice(String name, Integer price);
}
```

这个方法会被转化为es的json 查询

```javascript
{ "bool" :
    { "must" :
        [
            { "field" : {"name" : "?"} },
            { "field" : {"price" : "?"} }
        ]
    }
}
```

## 使用@Query注解

```java
interface BookRepository extends ElasticsearchRepository<Book, String> {
    @Query("{\"bool\" : {\"must\" : {\"field\" : {\"name\" : \"?0\"}}}}")
    Page<Book> findByName(String name,Pageable pageable);
}
```

# 基于注解的配置

spring Data es repository基于Java配置的使用

```java
@Configuration
@EnableElasticsearchRepositories(                             
  basePackages = "org.springframework.data.elasticsearch.repositories"
  )
static class Config {

  @Bean
  public ElasticsearchOperations elasticsearchTemplate() {    
      // ...
  }
}

class ProductService {

  private ProductRepository repository;                       

  public ProductService(ProductRepository repository) {
    this.repository = repository;
  }

  public Page<Product> findAvailableBookByName(String name, Pageable pageable) {
    return repository.findByAvailableTrueAndNameStartingWith(name, pageable);
  }
}
```

# Filter Builder

```java
private ElasticsearchTemplate elasticsearchTemplate;

SearchQuery searchQuery = new NativeSearchQueryBuilder()
  .withQuery(matchAllQuery())
  .withFilter(boolFilter().must(termFilter("id", documentId)))
  .build();

Page<SampleEntity> sampleEntities =
  elasticsearchTemplate.queryForPage(searchQuery,SampleEntity.class);
```

# 查询大量结果集使用Scroll

```java
SearchQuery searchQuery = new NativeSearchQueryBuilder()
  .withQuery(matchAllQuery())
  .withIndices(INDEX_NAME)
  .withTypes(TYPE_NAME)
  .withFields("message")
  .withPageable(PageRequest.of(0, 10))
  .build();

ScrolledPage<SampleEntity> scroll = elasticsearchTemplate.startScroll(1000, searchQuery, SampleEntity.class);

String scrollId = scroll.getScrollId();
List<SampleEntity> sampleEntities = new ArrayList<>();
while (scroll.hasContent()) {
  sampleEntities.addAll(scroll.getContent());
  scrollId = scroll.getScrollId();
  scroll = elasticsearchTemplate.continueScroll(scrollId, 1000, SampleEntity.class);
}
elasticsearchTemplate.clearScroll(scrollId);
```

使用Stream

```java
SearchQuery searchQuery = new NativeSearchQueryBuilder()
  .withQuery(matchAllQuery())
  .withIndices(INDEX_NAME)
  .withTypes(TYPE_NAME)
  .withFields("message")
  .withPageable(PageRequest.of(0, 10))
  .build();

CloseableIterator<SampleEntity> stream = elasticsearchTemplate.stream(searchQuery, SampleEntity.class);

List<SampleEntity> sampleEntities = new ArrayList<>();
while (stream.hasNext()) {
  sampleEntities.add(stream.next());
}
```

# demo

https://blog.csdn.net/wang926454/article/details/99649556

