# restful设计

好了，我们现在再来看看如何实现Restful API。实际上Restful本身不是一项什么高深的技术，而只是一种编程风格，或者说是一种设计风格。在传统的http接口设计中，我们一般只使用了get和post两个方法，然后用我们自己定义的词汇来表示不同的操作，比如上面查询文章的接口，我们定义了article/list.json来表示查询文章列表，可以通过get或者post方法来访问。而Restful API的设计则通过HTTP的方法来表示CRUD相关的操作。因此，除了get和post方法外，还会用到其他的HTTP方法，如PUT、DELETE、HEAD等，通过不同的HTTP方法来表示不同含义的操作。下面是我设计的一组对文章的增删改查的Restful API：

| 接口URL       | HTTP方法 | 接口说明     |
| ------------- | -------- | ------------ |
| /article      | POST     | 保存文章     |
| /article/{id} | GET      | 查询文章列表 |
| /article/{id} | DELETE   | 删除文章     |
| /article/{id} | PUT      | 更新文章信息 |

 　这里可以看出，URL仅仅是标识资源的路劲，而具体的行为由HTTP方法来指定。

# restful实现

直接贴代码了哈，学生的增删改查操作

```java
package com.cxf.batishelper.controller;

import com.cxf.batishelper.model.Student;
import com.cxf.batishelper.pojo.ResponseData;
import com.cxf.batishelper.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;


@RestController
@RequestMapping(value = "student")
public class StudentController {
    @Autowired
    StudentService studentService;

    /**
     * 增
     * @param student
     * @return
     */
    @PostMapping(value = "/insert")
    public ResponseData<Integer> insert(@RequestBody Student student) {
        int insert = studentService.insert(student);
        ResponseData<Integer> responseData = new ResponseData<>();
        responseData.setData(insert);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

    /**
     * 删
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.DELETE,produces = "application/json")
    public ResponseData<Object> delStudent(@PathVariable Integer id) {
        int i = studentService.deleteByPrimaryKey(id);
        ResponseData<Object> responseData = new ResponseData<>();
        responseData.setData(i);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

    /**
     * 改
     * @param id
     * @param student
     * @return
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = "application/json")
    public ResponseData<Integer> update(@PathVariable Integer id, @RequestBody Student student) {
        student.setId(id);
        int insert = studentService.insertOrUpdateSelective(student);
        ResponseData<Integer> responseData = new ResponseData<>();
        responseData.setData(insert);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }



    /**
     * 查
     * @param id
     * @return
     */
    @RequestMapping(value = "/{id}",method = RequestMethod.GET,produces = "application/json")
    public ResponseData<Student> getStudent(@PathVariable Integer id) {
        Student student = studentService.selectByPrimaryKey(id);
        ResponseData<Student> responseData = new ResponseData<>();
        responseData.setData(student);
        responseData.setStatus(HttpStatus.OK.value());
        responseData.setSuccess(true);
        return responseData;
    }

}

```

我们再来分析一下这段代码，这段代码和非rest代码的区别在于：

　　（1）我们使用的是@RestController这个注解，而不是@Controller，不过这个注解同样不是Spring boot提供的，而是Spring MVC4中的提供的注解，表示一个支持Restful的控制器。

　　（2）这个类中有三个URL映射是相同的，即都是/student/{id}，这在@Controller标识的类中是不允许出现的。这里的可以通过method来进行区分，produces的作用是表示返回结果的类型是JSON。

　　（3）@PathVariable这个注解，也是Spring MVC提供的，其作用是表示该变量的值是从访问路径中获取。

　　所以看来看去，这个代码还是跟Spring boot没太多的关系，Spring boot也仅仅是提供自动配置的功能，这也是Spring boot用起来很舒服的一个很重要的原因，因为它的侵入性非常非常小，你基本感觉不到它的存在。

# 测试

推荐使用junit测试，结合MockMvc可以方便的测试http接口

```java
@RunWith(SpringRunner.class)
@SpringBootTest
class StudentControllerTest {
    @Autowired
    private StudentController studentController; //一定要注入controller，不要new!
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(studentController).build();
    }

    @Test
    void insert() throws Exception {
        Student student = new Student();
        student.setName("mock");
        student.setAge(18);
        Gson gson = new Gson();
        RequestBuilder builder = MockMvcRequestBuilders
                .post("/student/insert")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(gson.toJson(student));
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void deleteTest() throws Exception {
        RequestBuilder builder = MockMvcRequestBuilders
                .delete("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }


    @Test
    void update() throws Exception {
        Student student = new Student();
        student.setName("update");
        student.setAge(18);
        Gson gson = new Gson();
        RequestBuilder builder = MockMvcRequestBuilders
                .put("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(gson.toJson(student));
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }

    @Test
    void select() throws Exception {
        RequestBuilder builder = MockMvcRequestBuilders
                .get("/student/1")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON_UTF8);
        MvcResult result = mvc.perform(builder).andReturn();
        System.out.println(result.getResponse().getContentAsString());
    }
}

```

完整代码已上传github: [batis-helper](https://github.com/luckywind/TechWorld/tree/master/code/mybatis/batis-helper)

