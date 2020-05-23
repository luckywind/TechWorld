# 请求方法

[使用请参考resttemplate使用](https://juejin.im/post/5cd680eff265da037b612e28)

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

