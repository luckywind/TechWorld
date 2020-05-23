# 请求方法

[使用请参考resttemplate使用](https://juejin.im/post/5cd680eff265da037b612e28)

`RestTemplate` 类是在 Spring Framework 3.0 开始引入的，

默认的构造器使用java.net.HttpURLConnection执行请求，可以切换到不同的实现ClientHttpRequestFactory的HTTP库。内置的有：

- Apache HttpComponents
- Netty
- OkHttp

例如，切换到HttpComponents

```java
RestTemplate template = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
```

## URI

大多数方法都接受URI模版和模版变量，例如String变量，Map<String,String>变量

```java
//string模板
String result = restTemplate.getForObject(
        "https://example.com/hotels/{hotel}/bookings/{booking}", String.class, "42", "21");
//map模板
Map<String, String> vars = Collections.singletonMap("hotel", "42");
String result = restTemplate.getForObject(
        "https://example.com/hotels/{hotel}/rooms/{hotel}", String.class, vars);
```

## headers

可以使用exchange()方法指定请求header，例如：

```java
//URI构造器
String uriTemplate = "https://example.com/hotels/{hotel}";
URI uri = UriComponentsBuilder.fromUriString(uriTemplate).build(42);
//请求实体requestEntity构造器
RequestEntity<Void> requestEntity = RequestEntity.get(uri)
        .header(("MyRequestHeader", "MyValue")
        .build();

ResponseEntity<String> response = template.exchange(requestEntity, String.class);

String responseHeader = response.getHeaders().getFirst("MyResponseHeader");
String body = response.getBody();
```

## Body

RestTemplate的方法使用HttpMessageConverter完成对象和原始内容的转换。

例如，Post请求中对象会被序列化到请求体中、

```java
URI location = template.postForLocation("https://example.com/people", person);
```

通常是不需要显示指定Content-Type请求头的，spring会自动选择相应的message converter 。 如果需要，exchange方法也支持在header里指定Content-Type。

GET请求中，响应体会自动反序列化到对象，例如：

```java
Person person = restTemplate.getForObject("https://example.com/people/{id}", Person.class, 42);
```

同样的，通常Accept请求头不需要显示指定，也可以使用cxchange方法显示指定。

默认RestTemplate根据classpath注册内置message converters。可以手动指定message converters。

## Message conversion

Spring-web模块包含HttpMessageConverter通过INputStream和OutputStream读写HTTP请求体。HttpMessageConverter实例用在客户端(例如，RestTemplate)和服务端(例如Spring mvc Rest controllers)。

HttpMessageConverter实现

| MessageConverter                       | Description                                            |
| -------------------------------------- | ------------------------------------------------------ |
| StringHttpMessageConverter             | 从HTTP请求和响应中读写String                           |
| FormHttpMessageConverter               |                                                        |
| ByteArrayHttpMessageConverter          |                                                        |
| MarshallingHttpMessageConverter        |                                                        |
| MappingJackson2HttpMessageConverter    | 使用jackson的ObjectMapper读写JSON。 可以选择序列化字段 |
| MappingJackson2XmlHttpMessageConverter |                                                        |
| SourceHttpMessageConverter             |                                                        |
| BufferedImageHttpMessageConverter      |                                                        |

# demo

```java
    @RequestMapping(value = "/create_table", method = RequestMethod.POST)
    public Result createHiveTable(@RequestBody TableRegistData tableRegistData) throws URISyntaxException {
        URIBuilder uriBuilder = getUriBuilder(Constants.URI_REGISTER);
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<TableRegistData> entity = new HttpEntity<TableRegistData>(tableRegistData, headers);
        ResponseEntity<Result> responseEntity = restTemplate.exchange(
                uriBuilder.build(), HttpMethod.POST, entity, Result.class);
        Result result = responseEntity.getBody();
        return result;
    }

    /**
     * 获取带token参数的URIBuilder
     * @param path
     * @return
     * @throws URISyntaxException
     */
    private URIBuilder getUriBuilder(String path) throws URISyntaxException {
        init();
        URIBuilder uriBuilder;
        uriBuilder = new URIBuilder(REMOTE_SERVER);
        uriBuilder.setPath(path);
        uriBuilder.setParameter(IAuthConstants.APP_ID, String.valueOf(info.getAppId()));
        uriBuilder.setParameter(IAuthConstants.TOKEN, info.getToken());
        return uriBuilder;
    }
```

我在使用过程中遇到一个问题，参数token的值包含了特殊字符&，传入exchange的URI为uriBuilder.build().toAscIIString()。结果服务端说无法解析token,服务端接收到的token并不是我传过去的token。 后来我把toAscIIString()这个调用去掉了，问题就解决了，由此可见springboot会自动做url的转码，不需要我们做。

# 更多用例

```java
package com.one.learn.resttemplate;

import com.one.learn.resttemplate.bean.Product;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.util.Assert.isTrue;

/**
 * @author One
 * @Description RestTemplate 请求测试类
 * @date 2019/05/09
 */
public class RestTemplateTests {
    RestTemplate restTemplate = new RestTemplate();
    RestTemplate customRestTemplate = null;

    @Before
    public void setup() {
        customRestTemplate = new RestTemplate(getClientHttpRequestFactory());
    }

    private SimpleClientHttpRequestFactory getClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory clientHttpRequestFactory
                = new SimpleClientHttpRequestFactory();
        // 连接超时设置 10s
        clientHttpRequestFactory.setConnectTimeout(10_000);

        // 读取超时设置 10s
        clientHttpRequestFactory.setReadTimeout(10_000);
        return clientHttpRequestFactory;
    }

    @Test
    public void testGet_product1() {
        //方式一：GET 方式获取 JSON 串数据
        String url = "http://localhost:8081/product/get_product1";
        String result = restTemplate.getForObject(url, String.class);
        System.out.println("get_product1返回结果：" + result);
        Assert.hasText(result, "get_product1返回结果为空");
        //方式二：GET 方式获取 JSON 数据映射后的 Product 实体对象
        Product product = restTemplate.getForObject(url, Product.class);
        System.out.println("get_product1返回结果：" + product);
        Assert.notNull(product, "get_product1返回结果为空");
        //方式三：GET 方式获取包含 Product 实体对象 的响应实体 ResponseEntity 对象,用 getBody() 获取
        ResponseEntity<Product> responseEntity = restTemplate.getForEntity(url, Product.class);
        System.out.println("get_product1返回结果：" + responseEntity);
        Assert.isTrue(responseEntity.getStatusCode().equals(HttpStatus.OK), "get_product1响应不成功");

        MultiValueMap header = new LinkedMultiValueMap();
        header.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Object> requestEntity = new HttpEntity<>(header);
        ResponseEntity<Product> exchangeResult = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Product.class);
        System.out.println("get_product1返回结果：" + exchangeResult);
        Assert.isTrue(exchangeResult.getStatusCode().equals(HttpStatus.OK), "get_product1响应不成功");
//        exchangeResult.getBody()
//        方式四： 根据 RequestCallback 接口实现类设置Header信息,用 ResponseExtractor 接口实现类读取响应数据
        String executeResult = restTemplate.execute(url, HttpMethod.GET, request -> {
            request.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        }, (clientHttpResponse) -> {
            InputStream body = clientHttpResponse.getBody();
            byte[] bytes = new byte[body.available()];
            body.read(bytes);
            return new String(bytes);
        });
        System.out.println("get_product1返回结果：" + executeResult);
        Assert.hasText(executeResult, "get_product1返回结果为空");
    }

    /**
     * 测试带参数的get
     */
    @Test
    public void testGet_product2() {
        // 方式一：将参数的值存在可变长度参数里，按照顺序进行参数匹配
        String url = "http://localhost:8081/product/get_product2?id={id}";
        ResponseEntity<Product> responseEntity = restTemplate.getForEntity(url, Product.class, 101);
        System.out.println(responseEntity);
        isTrue(responseEntity.getStatusCode().equals(HttpStatus.OK), "get_product2 请求不成功");
        Assert.notNull(responseEntity.getBody().getId(), "get_product2  传递参数不成功");
        //方式二：将请求参数以键值对形式存储到 Map 集合中，用于请求时URL上的拼接
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("id", 101);
        Product result = restTemplate.getForObject(url, Product.class, uriVariables);
        System.out.println(result);
        Assert.notNull(result.getId(), "get_product2  传递参数不成功");
    }

    @Test
    public void testPost_product1() {
        //方式二： 将请求参数值以 K=V 方式用 & 拼接，发送请求使用
        String url = "http://localhost:8081/product/post_product1";
        MultiValueMap<String, String> header = new LinkedMultiValueMap();
        header.add(HttpHeaders.CONTENT_TYPE, (MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        Product product = new Product(201, "Macbook", BigDecimal.valueOf(10000));
        String productStr = "id=" + product.getId() + "&name=" + product.getName() + "&price=" + product.getPrice();
        HttpEntity<String> request = new HttpEntity<>(productStr, header);
        ResponseEntity<String> exchangeResult = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        System.out.println("post_product1: " + exchangeResult);
        Assert.isTrue(exchangeResult.getStatusCode().equals(HttpStatus.OK), "post_product1 请求不成功");

        //方式一： 将请求参数以键值对形式存储在 MultiValueMap 集合，发送请求时使用
        MultiValueMap<String, Object> map = new LinkedMultiValueMap();
        map.add("id", (product.getId()));
        map.add("name", (product.getName()));
        map.add("price", (product.getPrice()));
        HttpEntity<MultiValueMap> request2 = new HttpEntity<>(map, header);
        ResponseEntity<String> exchangeResult2 = restTemplate.exchange(url, HttpMethod.POST, request2, String.class);
        System.out.println("post_product1： " + exchangeResult2);
        Assert.isTrue(exchangeResult.getStatusCode().equals(HttpStatus.OK), "post_product1 请求不成功");
    }

    @Test
    public void testPost_product2() {
        String url = "http://localhost:8081/product/post_product2";
        MultiValueMap<String, String> header = new LinkedMultiValueMap();
        // 设置请求的 Content-Type 为 application/json
        header.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_JSON_VALUE));
        // 设置 Accept 向服务器表明客户端可处理的内容类型
        header.put(HttpHeaders.ACCEPT, Arrays.asList(MediaType.APPLICATION_JSON_VALUE));
        HttpEntity<Product> request = new HttpEntity<>(new Product(2, "Macbook", BigDecimal.valueOf(10000)), header);
        ResponseEntity<String> exchangeResult = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
        System.out.println("post_product2: " + exchangeResult);
        Assert.isTrue(exchangeResult.getStatusCode().equals(HttpStatus.OK), "post_product2 请求不成功");
    }

    @Test
    public void testDelete() {
        String url = "http://localhost:8081/product/delete/{id}";
        restTemplate.delete(url, 101);
    }

    @Test
    public void testPut() {
        String url = "http://localhost:8081/product/update";
        Map<String, ?> variables = new HashMap<>();
        MultiValueMap<String, String> header = new LinkedMultiValueMap();
        header.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.APPLICATION_FORM_URLENCODED_VALUE));
        Product product = new Product(101, "iWatch", BigDecimal.valueOf(2333));
        String productStr = "id=" + product.getId() + "&name=" + product.getName() + "&price=" + product.getPrice();
        HttpEntity<String> request = new HttpEntity<>(productStr, header);
        restTemplate.put(url, request);
    }

    /**
     * 上传文件
     */
    @Test
    public void testUploadFile() {
        String url = "http://localhost:8081/product/upload";
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        Object file = new FileSystemResource(new File("/Users/chengxingfu/work/http/resttemplate/src/main/resources/application.properties"));
        body.add("file", file);

        MultiValueMap<String, String> header = new LinkedMultiValueMap();
        header.put(HttpHeaders.CONTENT_TYPE, Arrays.asList(MediaType.MULTIPART_FORM_DATA_VALUE));
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, header);
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
        System.out.println("upload: " + responseEntity);
        Assert.isTrue(responseEntity.getStatusCode().equals(HttpStatus.OK), "upload 请求不成功");
    }

}

```

