# 引入

```xml
 <!--  引入swagger包 -->
        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger2</artifactId>
            <version>2.6.1</version>
        </dependency>
<!-- 以下两个ui包，引入一个即可，建议使用第一个，更好看-->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>swagger-bootstrap-ui</artifactId>
            <version>1.5</version>
        </dependency>

        <dependency>
            <groupId>io.springfox</groupId>
            <artifactId>springfox-swagger-ui</artifactId>
            <version>2.6.1</version>
        </dependency>
```

# 配置

增加配置类即可，只是要注意，配置扫描的包

```java
/**
 * Swagger2配置类
 * 在与spring boot集成时，放在与Application.java同级的目录下。
 * 或者通过 @Import 导入配置
 */
@Configuration
@EnableSwagger2
public class Swagger2 {
    
    /**
     * 创建API应用
     * apiInfo() 增加API相关信息
     * 通过select()函数返回一个ApiSelectorBuilder实例,用来控制哪些接口暴露给Swagger来展现，
     * 本例采用指定扫描的包路径来定义指定要建立API的目录。
     * @return
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.cxf.demo.controller"))
                .paths(PathSelectors.any())
                .build();
    }
    
    /**
     * 创建该API的基本信息（这些基本信息会展现在文档页面中）
     * 访问地址：http://项目实际地址/swagger-ui.html
     * @return
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("RESTful APIs")
                .description("")
                .termsOfServiceUrl("")
                .contact(new Contact("","","@.com"))
                .version("1.0")
                .build();
    }
}

```

# API注解

## @Api

用在类上，该注解将一个Controller（Class）标注为一个swagger资源（API）

### @ApiOperation

在指定的（路由）路径上，对一个操作或HTTP方法进行描述。具有相同路径的不同操作会被归组为同一个操作对象。不同的HTTP请求方法及路径组合构成一个唯一操作。此注解的属性有：

- value 对操作的简单说明，长度为120个字母，60个汉字。
- notes 对操作的详细说明。
- httpMethod HTTP请求的动作名，可选值有："GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS" and "PATCH"。
- code 默认为200

### @ApiImplicitParams

用在方法上，注解ApiImplicitParam的容器类，以数组方式存储。

### @ApiParam

增加对参数的元信息说明。这个注解只能被使用在JAX-RS 1.x/2.x的综合环境下。其主要的属性有

- required 是否为必传参数，默认为false
- value 参数简短说明

### @ApiResponses

注解@ApiResponse的包装类，数组结构。即使需要使用一个@ApiResponse注解，也需要将@ApiResponse注解包含在注解@ApiResponses内。

### @ApiResponse

描述一个操作可能的返回结果。当REST API请求发生时，这个注解可用于描述所有可能的成功与错误码。可以用，也可以不用这个注解去描述操作的返回类型，但成功操作的返回类型必须在@ApiOperation中定义。如果API具有不同的返回类型，那么需要分别定义返回值，并将返回类型进行关联。但Swagger不支持同一返回码，多种返回类型的注解。注意：这个注解必须被包含在@ApiResponses注解中。

- code HTTP请求返回码。有效值必须符合标准的[HTTP Status Code Definitions](https://link.jianshu.com/?t=http://www.w3.org/Protocols/rfc2616/rfc2616-sec10.html)。
- message 更加易于理解的文本消息
- response 返回类型信息，必须使用完全限定类名，比如“com.xyz.cc.Person.class”。
- responseContainer 如果返回类型为容器类型，可以设置相应的值。有效值为 "List", "Set" or "Map"，其他任何无效的值都会被忽略。



## 注解实例

```java
@AllArgsConstructor
@RestController
@RequestMapping("/api/category")
@Api(value = "/category", tags = "组件分类")
public class BizCategoryController {

    private IBizCategoryService bizCategoryService;

    @GetMapping("/list")
    @ApiOperation(value = "列表", notes = "分页列表")
    public R<PageModel<BizCategory>> list(PageQuery pageQuery,
                                          @RequestParam @ApiParam("组件分类名称") String name) {
        IPage<BizCategory> page = bizCategoryService.page(pageQuery.loadPage(),
                new LambdaQueryWrapper<BizCategory>().like(BizCategory::getName, name));
        return R.success(page);
    }

    @GetMapping("/list/all")
    @ApiOperation(value = "查询所有", notes = "分页列表")
    public R<List<BizCategory>> listAll() {
        List<BizCategory> categories = bizCategoryService.list();
        return R.success(categories);
    }

    @GetMapping("/{categoryId}")
    @ApiOperation(value = "详情", notes = "组件分类详情")
    public R<BizCategory> detail(@PathVariable @ApiParam("分类Id") Long categoryId) {
        BizCategory category = bizCategoryService.getById(categoryId);
        return R.success(category);
    }

    @PostMapping("/save")
    @ApiOperation(value = "保存", notes = "新增或修改")
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "form", name = "categoryId", value = "组件id（修改时为必填）"),
            @ApiImplicitParam(paramType = "form", name = "name", value = "组件分类名称", required = true)
    })
    public R<BizCategory> save(Long categoryId, String name) {
        BizCategory category = new BizCategory();
        category.setId(categoryId);
        category.setName(name);
        bizCategoryService.saveOrUpdate(category);
        return R.success(category);
    }

    @DeleteMapping("/{categoryId}")
    @ApiOperation(value = "删除", notes = "删除")
    public R delete(@PathVariable @ApiParam("分类Id") Long categoryId) {
        bizCategoryService.delete(categoryId);
        return R.success();
    }
}

```

# ui地址

/doc.html

