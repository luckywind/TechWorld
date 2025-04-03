# URL 

## path() 函数

Django path() 可以接收四个参数，分别是两个必选参数：route、view 和两个可选参数：kwargs、name。

语法格式：

```
path(route, view, kwargs=None, name=None)
```

- **route：** 字符串，定义 URL 的路径部分。可以包含变量，例如 `<int:my_variable>`，以从 URL 中捕获参数并将其传递给视图函数。
- **view：** 视图函数，处理与给定路由匹配的请求。可以是一个函数或一个基于类的视图。
- **kwargs（可选）：** 一个字典，包含传递给视图函数的额外关键字参数。
- **name（可选）：** 为 URL 路由指定一个唯一的名称，以便在代码的其他地方引用它。这对于在模板中生成 URL 或在代码中进行重定向等操作非常有用。

## 示例

```python
Function views
    1. Add an import:  from my_app import views
    2. Add a URL to urlpatterns:  path('', views.home, name='home')
Class-based views
    1. Add an import:  from other_app.views import Home
    2. Add a URL to urlpatterns:  path('', Home.as_view(), name='home')
Including another URLconf
    1. Import the include() function: from django.urls import include, path
    2. Add a URL to urlpatterns:  path('blog/', include('blog.urls'))
```



# 模板



## 变量

view：｛"HTML变量名" : "views变量名"｝
HTML：｛｛变量名｝｝

## 列表

```html
<p>{{ views_list }}</p>   # 取出整个列表
<p>{{ views_list.0 }}</p> # 取出列表的第一个元素
```

## 字典

```html
<p>{{ views_dict }}</p>
<p>{{ views_dict.name }}</p>
```

## 过滤器

```html
语法：
{{ 变量名 | 过滤器：可选参数 | ...}}

管道处理
{{ my_list|first|upper }}

default值
{{ name|default:"菜鸟教程666" }}
```

处理器：

- safe： 将字符串标记为安全，不需要转义。Django 会自动对 views.py 传到HTML文件中的标签语法进行转义，令其语义失效。
- **truncatechars**： 截断
- date：日期格式化
- length

## if/else

```python
{% if condition1 %}
   ... display 1
{% elif condition2 %}
   ... display 2
{% else %}
   ... display 3
{% endif %}
```

## for

```python
<ul>
{% for athlete in athlete_list %}
    <li>{{ athlete.name }}</li>
{% endfor %}
</ul>

```

## include标签

包含其他模板内容

{% include "nav.html" %}



## 自定义标签和过滤器

在应用目录下创建 **templatetags** 目录(与 templates 目录同级，目录名只能是 templatetags)。



## 配置静态文件

## 模板继承

模板可以用继承的方式来实现复用，减少冗余内容。

网页的头部和尾部内容一般都是一致的，我们就可以通过模板继承来实现复用。

父模板用于放置可重复利用的内容，子模板继承父模板的内容，并放置自己的内容。

**父模板**:父模板中的预留区域，该区域留给子模板填充差异性的内容，不同预留区域名字不能相同。

```python
{% block 名称 %} 
预留给子模板的区域，可以设置默认内容
{% endblock 名称 %}
```

**子模板**：子模板使用标签 extends 继承父模板：

```python
{% extends "父模板路径"%} 
```

覆盖父模板预留内容:

```python
{ % block 名称 % }
内容 
{% endblock 名称 %}
```

# 模型

<font color=red>如果要使用模型，必须要创建一个 app,  每次在app里新增模型时，都需要执行migrate来创建表结构</font>

```shell
$ django-admin startapp TestModel

$ python3 manage.py migrate   # 创建表结构

$ python3 manage.py makemigrations TestModel  # 让 Django 知道我们在我们的模型有一些变更
$ python3 manage.py migrate TestModel   # 创建表结构

```

表名组成结构为：应用名_类名（如：TestModel_test）。

**注意：**尽管我们没有在 models 给表设置主键，但是 Django 会自动添加一个 id 作为主键。

## 获取数据

```python
# -*- coding: utf-8 -*-
 
from django.http import HttpResponse
 
from TestModel.models import Test
 
# 数据库操作
def testdb(request):
    # 初始化
    response = ""
    response1 = ""
    
    
    # 通过objects这个模型管理器的all()获得所有数据行，相当于SQL中的SELECT * FROM
    listTest = Test.objects.all()
        
    # filter相当于SQL中的WHERE，可设置条件过滤结果
    response2 = Test.objects.filter(id=1) 
    
    # 获取单个对象
    response3 = Test.objects.get(id=1) 
    
    # 限制返回的数据 相当于 SQL 中的 OFFSET 0 LIMIT 2;
    Test.objects.order_by('name')[0:2]
    
    #数据排序
    Test.objects.order_by("id")
    
    # 上面的方法可以连锁使用
    Test.objects.filter(name="runoob").order_by("id")
    
    # 输出所有数据
    for var in listTest:
        response1 += var.name + " "
    response = response1
    return HttpResponse("<p>" + response + "</p>")
```



## 更新数据

```python
# -*- coding: utf-8 -*-
 
from django.http import HttpResponse
 
from TestModel.models import Test
 
# 数据库操作
def testdb(request):
    # 修改其中一个id=1的name字段，再save，相当于SQL中的UPDATE
    test1 = Test.objects.get(id=1)
    test1.name = 'Google'
    test1.save()
    
    # 另外一种方式
    #Test.objects.filter(id=1).update(name='Google')
    
    # 修改所有的列
    # Test.objects.all().update(name='Google')
    
    return HttpResponse("<p>修改成功</p>")
```



## 删除数据

```python
# -*- coding: utf-8 -*-
 
from django.http import HttpResponse
 
from TestModel.models import Test
 
# 数据库操作
def testdb(request):
    # 删除id=1的数据
    test1 = Test.objects.get(id=1)
    test1.delete()
    
    # 另外一种方式
    # Test.objects.filter(id=1).delete()
    
    # 删除所有数据
    # Test.objects.all().delete()
    
    return HttpResponse("<p>删除成功</p>")
```

## 新增数据

```python
Membership.objects.create(
...     person=paul,
...     group=beatles,
...     date_joined=date(1960, 8, 1),
...     invite_reason="Wanted to form a band.",
... )
```



# 表单

HTML表单是网站交互性的经典方式。

## HTTP请求

### GET方法

url、表单、业务逻辑跳转如下图：

![image-20241121173238207](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Django-orm2_1.png)

视图显示(search_form)和请求处理(search)分成两个函数处理。

### POST方法

![image-20241121174259346](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Django-orm2_1.png)



# 视图

一个视图函数，简称视图，是一个简单的 Python 函数，它接受 Web 请求并且返回 Web 响应。

响应可以是一个 HTML 页面、一个 404 错误页面、重定向页面、XML 文档、或者一张图片...

视图层中有两个重要的对象：请求对象(request)与响应对象(HttpResponse)。

## request对象

1. GET
   数据类型是 QueryDict，一个类似于字典的对象，包含 HTTP GET 的所有参数。
   request.GET.get("name")

2. POST

   数据类型是 QueryDict，一个类似于字典的对象，包含 HTTP POST 的所有参数。

   常用于 form 表单，form 表单里的标签 name 属性对应参数的键，value 属性对应参数的值。

3. body

   数据类型是二进制字节流，是原生请求体里的参数内容，在 HTTP 中用于 POST，因为 GET 没有请求体。

   在 HTTP 中不常用，而在处理非 HTTP 形式的报文时非常有用，例如：二进制图片、XML、Json 等。

4. path
   获取 URL 中的路径部分，数据类型是字符串。

5. method
   获取当前请求的方式，数据类型是字符串，且结果为大写。

6. request.get_full_path()  # /upload/?id=1 　　

7. request.META   网页原信息 　　

8. request.FILES   文件

### postman发送post请求

Headers添加`"Content-Type": "application/json"`

Body选择

- raw

选择text，则请求头是： text/plain
选择javascript，则请求头是： application/javascript
选择json，则请求头是： application/json (如果想以json格式传参，就用raw+json就行了)
选择html，则请求头是： text/html
选择application/xml，则请求头是： application/xml

![image-20241218150429470](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20241218150429470.png)







## response对象

响应对象主要有三种形式：HttpResponse()、render()、redirect()。

**HttpResponse():** 返回文本，参数为字符串，字符串中写文本内容。如果参数为字符串里含有 html 标签，也可以渲染。

1. ` HttpResponse("<a href='https://www.runoob.com/'>菜鸟教程</a>")`
2. render(request,"runoob.html",{"name":name})
3. redirect("/index/")

## [基于类的视图](https://docs.djangoproject.com/zh-hans/3.2/topics/class-based-views/)





# 路由

路由简单的来说就是根据用户请求的 URL 链接来判断对应的处理程序，并返回处理结果，也就是 URL 与 Django 的视图建立映射关系。

Django 路由在 urls.py 配置，urls.py 中的每一条配置对应相应的处理方法。

**Django 2.2.x 之后的版本**

- path：用于普通路径，不需要自己手动添加正则首位限制符号，底层已经添加。
- re_path：用于正则路径，需要自己手动添加正则首位限制符号。



<font color=red>注意</font>： 

url后面的$一定要加，否则可能会匹配到更长的url



## 正则路径中的分组

### 无名分组

无名分组按位置传参，一一对应。

views 中除了 request，其他形参的数量要与 urls 中的分组数量一致。

```python
urlpatterns = [ 
    path('admin/', admin.site.urls), 
    re_path("^index/([0-9]{4})/$", views.index), 
]



view.py
from django.shortcuts import HttpResponse

def index(request, year): 
    print(year) # 一个形参代表路径中一个分组的内容，按顺序匹配
    return HttpResponse('菜鸟教程')
```



### 有名分组

语法：

```
(?P<组名>正则表达式)
```

有名分组按关键字传参，与位置顺序无关。

```python
re_path("^index/(?P<year>[0-9]{4})/(?P<month>[0-9]{2})/$", views.index),


view.py
from django.shortcuts import HttpResponse
def index(request, year, month): 
    print(year,month) # 一个形参代表路径中一个分组的内容，按关键字对应匹配 
    return HttpResponse('菜鸟教程')
```

views 中除了 request，其他形参的数量要与 urls 中的分组数量一致， 并且 views 中的形参名称要与 urls 中的组名对应。

## 路由分发

**存在问题**：Django 项目里多个app目录共用一个 urls 容易造成混淆，后期维护也不方便。

**解决**：使用路由分发（include），让每个app目录都单独拥有自己的 urls。

**步骤：**

- 1、在每个 app 目录里都创建一个 urls.py 文件。
- 2、在项目名称目录下的 urls 文件里，统一将路径分发给各个 app 目录。

```python
from django.contrib import admin 
from django.urls import path,include # 从 django.urls 引入 include 
urlpatterns = [ 
    path('admin/', admin.site.urls), 
    path("app01/", include("app01.urls")), 
    path("app02/", include("app02.urls")), 
]
```

## 反向解析

随着功能的增加，路由层的 url 发生变化，就需要去更改对应的视图层和模板层的 url，非常麻烦，不便维护。

这时我们可以利用反向解析，当路由层 url 发生改变，在视图层和模板层动态反向解析出更改后的 url，免去修改的操作。

反向解析一般用在模板中的超链接及视图中的重定向。

### 普通路径

在 urls.py 中给路由起别名，**name="路由别名"**。

```python
path("login1/", views.login, name="login")
```

在 views.py 中，从 django.urls 中引入 reverse，利用 **reverse("路由别名")** 反向解析:

```python
return redirect(reverse("login"))
```

在模板 templates 中的 HTML 文件中，利用 **{% url "路由别名" %}** 反向解析。

```html
<form action="{% url 'login' %}" method="post"> 
```



### 正则路径

#### 无名分组

在 views.py 中，从 django.urls 中引入 reverse，利用 **reverse("路由别名"，args=(符合正则匹配的参数,))** 反向解析。

```python
return redirect(reverse("login",args=(10,)))
```

在模板 templates 中的 HTML 文件中利用 **{% url "路由别名" 符合正则匹配的参数 %}** 反向解析。

```html
<form action="{% url 'login' 10 %}" method="post"> 
```



#### 有名分组

在 views.py 中，从 django.urls 中引入 reverse，利用 **reverse("路由别名"，kwargs={"分组名":符合正则匹配的参数})** 反向解析。

```python
return redirect(reverse("login",kwargs={"year":3333}))
```

在模板 templates 中的 HTML 文件中，利用 **{% url "路由别名" 分组名=符合正则匹配的参数 %}** 反向解析。

```html
<form action="{% url 'login' year=3333 %}" method="post">
```

## 命名空间

**存在问题：**路由别名 name 没有作用域，Django 在反向解析 URL 时，会在项目全局顺序搜索，当查找到第一个路由别名 name 指定 URL 时，立即返回。当在不同的 app 目录下的urls 中定义相同的路由别名 name 时，可能会导致 URL 反向解析错误。

**解决：**使用命名空间。

### 普通路径

定义命名空间（include 里面是一个元组）格式如下：

```python
include(("app名称：urls"，"app名称"))
```

实例：

```python
path("app01/", include(("app01.urls","app01"))) 
path("app02/", include(("app02.urls","app02")))
```

在 app01/urls.py 中起相同的路由别名。

```python
path("login/", views.login, name="login")
```

- 在 views.py 中使用名称空间，语法格式如下：

```python
reverse("app名称：路由别名")
```

- 在 templates 模板的 HTML 文件中使用名称空间，语法格式如下：

```html
{% url "app名称：路由别名" %}
```

实例：

```html
<form action="{% url 'app01:login' %}" method="post">
```

# Django Admin

Django 提供了基于 web 的管理工具。

Django 自动管理工具是 django.contrib 的一部分。你可以在项目的 settings.py 中的 INSTALLED_APPS 看到它,

django.contrib是一套庞大的功能集，它是Django基本代码的组成部分。

## 激活管理工具

```python
# urls.py
from django.conf.urls import url
from django.contrib import admin
 
urlpatterns = [
    re_path(r'^admin/', admin.site.urls),
]
```

如果写成`re_path(r'^admin/$', admin.site.urls),`也就是url后面有个$符号，

访问http://localhost:8000/admin/

报错：django.urls.exceptions.NoReverseMatch: Reverse for 'logout' with no arguments not found.

原因是$意味着结尾，但是我们需要在正则的url后面追加included的url。

## 注册模型到admin

app/admin.py里进行注册,例如注册TestModel这个app的Test模型：

```python
from django.contrib import admin
from TestModel.models import Test
 
# Register your models here.
admin.site.register(Test)
```

## 权限

权限这部分实践可参考HelloWorld.app01， 我们通过Django admin给用户chengxf授权Book模型的CRUD权限。

### group权限

![image-20250214112229291](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250214112229291.png)



### user权限

![image-20250214112611502](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250214112611502.png)

用户加入组后就自动拥有组的权限。

### 权限的验证

#### 在视图中检查权限

**方法一**： 使用user.has_perm

[参考](https://keepsimple.dev/p/django-permissions.html)

```python
from datetime import date
from django.http import HttpResponse
from app01 import models
def add_book(request):
    # 这里先校验权限，注意权限的写法 
    if request.user.has_perm("app01.add_book") :
        pub_obj=models.Publish.objects.filter(pk=1).first()
        book=models.Book.objects.create(title="菜鸟教程", price=200, pub_date=date.today(), publish=pub_obj)
        print(book, type(book))
        return HttpResponse(book.pub_date)
    else:
        return HttpResponse("没有权限")

```

有个问题，这种权限粒度太粗，用户要么对所有Book都有新增权限，要么都没有新增权限。这也是Django权限系统的缺陷，后续介绍基于规则的对象级别的权限控制。

**方法二**：使用@permission_required装饰器

这种方法要求先登录

```python
@permission_required('app01.add_book')
def add_book(request):
    print(request.user)
```



### note

1. 重置管理员密码`python manage.py changepassword admin`
2. 用户是否可登录，需要勾选Staff status
   ![image-20250214141516608](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250214141516608.png)

3. 自定义模型展示的字段

我们可以自定义管理页面，来取代默认的页面。

在app的admin.py里定义模型的显示字段

```python
class ContactAdmin(admin.ModelAdmin):
    fields = ('name', 'email')


# 注册模型时，同时注册其管理页面显示格式
admin.site.register(Contact, ContactAdmin)
admin.site.register([Test, Tag])
```

### 登录登出

#### login

[参考](https://developer.mozilla.org/zh-CN/docs/Learn_web_development/Extensions/Server-side/Django/Authentication) Django 提供了创建身份验证页面所需的几乎所有功能，让处理登录，注销和密码管理等工作，都能“开箱即用”。这些相关功能包括了 url 映射器，视图和表单，但它不包括模板 。

[How to log a user in](https://docs.djangoproject.com/en/5.1/topics/auth/default/#top):  login方法把用户ID保存到Django的Session框架中的session中。

```python
from django.contrib.auth import authenticate, login

def my_view(request):
    username = request.POST['username']
    password = request.POST['password']
    user = authenticate(request, username=username, password=password)
    if user is not None:
        login(request, user)
        # Redirect to a success page.
        ...
    else:
        # Return an 'invalid login' error message.
        ...
```



#### logout

```python
from django.contrib.auth import logout

def logout_view(request):
    logout(request)
    # Redirect to a success page.
```

#### note

[Create a react login page that authenticates with Django auth token](https://medium.com/@preciousimoniakemu/create-a-react-login-page-that-authenticates-with-django-auth-token-8de489d2f751)

[LDAP Authentication with Django](https://medium.com/@itsayushbansal/ldap-authentication-with-django-a2b4f00c9a04)

> create a login/logout REST API in django-rest-framework using the rest framework’s SessionAuthentication to authenticate users with LDAP server.

[Combine LDAP and classical authentication in django](https://fle.github.io/combine-ldap-and-classical-authentication-in-django.html)



## [rules](https://github.com/dfunckt/django-rules?tab=readme-ov-file#creating-predicates)

对象级的权限控制[Django Permissions - Complete Guide](https://keepsimple.dev/p/django-permissions.html#Django_Rules)

ruleset 就是字典，key是类似id的东西，value是称为断言(Predicate)的函数。

有两个预定义的rule sets:

- 默认的rule set存储共享rules
- 作为Django上下文权限的rules

1. 创建断言只需要使用一个注解
   **django-rules 会将当前用户（通常是 request.user）作为第一个参数传递给断言函数**

```python
@rules.Predicate
def is_book_author(user,book):
    return book.author == user
```

2. 动态断言:根据参数动态创建断言

   ```python
   import rules
   
   def role_is(role_id):
       @rules.predicate
       def user_has_role(user):
           return user.role.id == role_id
   
       return user_has_role
   
   # 动态设置权限
   rules.add_perm("reports.view_report_abc", role_is(12))
   rules.add_perm("reports.view_report_xyz", role_is(13))
   ```

### 设置rule

add_rule用于向共享ruleSet中添加规则

```python
@rules.Predicate
def is_book_author(user,book):
    return book.author == user

rules.add_rule('can_edit_book', is_book_author)
rules.add_rule('can_delete_book', is_book_author)

#校验权限
rules.test_rule('can_edit_book', user, book)
```

### 断言的组合

断言本身不够有用，但是它组合起来就很有用，支持`& | ^ ~` 二元操作。

```python
>>> is_editor = rules.is_group_member('editors')
>>> is_editor
<Predicate:is_group_member:editors object at 0x10eee1350>

#创建一个新的断言
>>> is_book_author_or_editor = is_book_author | is_editor
>>> is_book_author_or_editor
<Predicate:(is_book_author | is_group_member:editors) object at 0x10eee1390>

# 设置权限并测试    
>>> rules.set_rule('can_edit_book', is_book_author_or_editor)
>>> rules.test_rule('can_edit_book', adrian, guidetodjango)
True
>>> rules.test_rule('can_delete_book', adrian, guidetodjango)
True 
```

### django中使用rule

#### api

- `add_perm(name, predicate)`

  Adds a rule to the permissions rule set. See `RuleSet.add_rule`.

- `set_perm(name, predicate)`

  Replace a rule from the permissions rule set. See `RuleSet.set_rule`.

- `remove_perm(name)`

  Remove a rule from the permissions rule set. See `RuleSet.remove_rule`.

- `perm_exists(name)`

  Returns whether a rule exists in the permissions rule set. See `RuleSet.rule_exists`.

- `has_perm(name, user=None, obj=None)`

  Tests the rule with the given name. See `RuleSet.test_rule`.



1. 创建权限

Django的权限名称是`app名称.动作_对象` 格式，可以这么创建权限

```python
>>> rules.add_perm('books.change_book', is_book_author | is_editor)
>>> rules.add_perm('books.delete_book', is_book_author)
```

add_perm用于向权限ruleSet添加规则

2. 校验权限

```python
>>> adrian.has_perm('books.change_book', somebook)
False
```

#### model中设置权限规则

**model改为继承RulesModel，新增一个Meta**，  [也可参考这里](https://keepsimple.dev/p/django-permissions.html)

用于权限的断言，就是接收一个user和一个对象作为参数的函数，例如把publish操作集成对象级权限。

```python
import rules
from rules.contrib.models import RulesModel

class Book(RulesModel):
    class Meta:
        rules_permissions = {
            "add": rules.is_staff,
            "read": rules.is_authenticated,
        }
```

这段代码和下面的等价

```python
rules.add_perm("app_label.add_book", rules.is_staff)
rules.add_perm("app_label.read_book", rules.is_authenticated)
```



#### view中使用

使用注解`from rules.contrib.views import permission_required`

```python
from django.shortcuts import get_object_or_404
from rules.contrib.views import permission_required
from posts.models import Post

def get_post_by_pk(request, post_id):
    return get_object_or_404(Post, pk=post_id)

@permission_required('posts.change_post', fn=get_post_by_pk)
def post_update(request, post_id):
    # ...
```

这里get_post_by_pk根据view函数的参数获取到Post对象，注解再校验该对象的权限，这个场景太常见，因此有一个泛型方法可用：

```python
from rules.contrib.views import permission_required, objectgetter
from posts.models import Post

@permission_required('posts.change_post', fn=objectgetter(Post, 'post_id'))
def post_update(request, post_id):
    # ...
```





# ORM

## 单表实例

### objects.create

项目路由分发： 注意正则url后面没有$

```python
re_path(r'^orm/', include("ORM.urls"))
```

app路由

```python
 re_path('^add_book/',views.add_book),
```

app.views.add_book:

```python
def add_book(request):
    books = models.Book.objects.create(title="如来神掌",price=200,publish="功夫出版社",pub_date="2010-10-10")
    print(books, type(books)) # Book object (18)
    return HttpResponse("<p>数据添加成功！</p>")
```

访问路径http://localhost:8000/orm/add_book/

查找objects.all

### 修改

方式一
save
方式二
update(字段名=更改的数据)（推荐）  返回受影响的行

### QuerySet查询方法

[参考](https://docs.djangoproject.com/en/5.1/ref/models/querysets/#database-time-zone-definitions)

1. 逻辑运算

   ```python
   # &   以下两句等价
   Model.objects.filter(x=1) & Model.objects.filter(y=2)
   Model.objects.filter(x=1).filter(y=2)
   # |   QuerySet或操作， 以下两种方式等价
   Model.objects.filter(x=1) | Model.objects.filter(y=2)
   
   from django.db.models import Q
   Model.objects.filter(Q(x=1) | Q(y=2))
   ```

2. date

   把日期强转为date，

   ```python
   TIME_ZONE = "Asia/Shanghai"    #默认时区
   USE_I18N = True
   USE_TZ = False
   ```

   - USE_TZ :  datetime是否支持时区，为True，Django将在内部使用时区感知的日期时间。
     当USE_TZ=False, 则Django会使用TIME_ZONE时区存储所有datetimes字段，当USE_TZ=True，这是 Django 将用来在模板中显示日期时间并解释在表单中输入的日期时间的默认时区

3. 示例

```python
    books = models.Book.objects.all()
    books = models.Book.objects.filter(pk=5)
    books = models.Book.objects.filter(price__in=[200,300]) # in 字句
    books = models.Book.objects.filter(price__gt=200) # >
    books = models.Book.objects.filter(price__gte=200)# >=
    books=models.Book.objects.filter(price__lt=300)   # <
    table.objects.filter(string__contains='pattern')
    books = models.Book.objects.exclude(pk=5)
    books = models.Book.objects.get(pk=5)
    books = models.Book.objects.order_by("-price")  # 查询所有，按照价格降序排列
    books = models.Book.objects.count()  # 查询所有数据的数量
    books = models.Book.objects.first()  # 返回所有数据的第一条数据
    books = models.Book.objects.last()  # 返回所有数据的最后一条数据
    books = models.Book.objects.exists()
    books=models.Book.objects.filter(pk=8).first().delete()
    books = models.Book.objects.values("pk", "price")#类似select 类型是字典
    books = models.Book.objects.values_list("price", "publish")
    books = models.Book.objects.values_list("publish").distinct()
    .filter(start_time__date__gte=build_date_from,start_time__date__lte=build_date_to)
```

**注意：**

1. filter 中运算符号只能使用等于号 =,  __模糊查询支持
   in、gt、gte、lt、lte、range、contains/icontains、startswith、endswith、year、month、day

2. values可以查询部分字段，且返回的就是字典









## 多表实例

### 梳理实体及实体关系

**书籍表 Book**：title 、 price 、 pub_date 、 publish（外键，多对一） 、 authors（多对多）

**出版社表 Publish**：name 、 city 、 email

**作者表 Author**：name 、 age 、 au_detail（一对一）

**作者详情表 AuthorDetail**：gender 、 tel 、 addr 、 birthday

以下是表格关联说明：

[![img](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/Django-orm2_1.png)](https://www.runoob.com/wp-content/uploads/2020/05/Django-orm2_1.png)

**说明：**

- 1、EmailField 数据类型是邮箱格式，底层继承 CharField，进行了封装，相当于 MySQL 中的 varchar。
- 2、Django1.1 版本不需要联级删除：on_delete=models.CASCADE，Django2.2 需要。
- 3、一般不需要设置联级更新.
- 4、**外键**在一对多的**多中设置**：**models.ForeignKey("关联类名", on_delete=models.CASCADE)**。
     <font color=red>外键也是键，也就是有唯一值的属性设置外键，比如一个出版社对应多本书，但一本书只对应一个出版社，   是对书做约束，所以给书设置外键。</font>
- 5、ManyToManyField 多对多关系会生成一张关系表
- 6、OneToOneField = ForeignKey(...，unique=True)设置一对一。
- 7、若有模型类存在外键，创建数据时，要先创建外键关联的模型类的数据，不然创建包含外键的模型类的数据时，外键的关联模型类的数据会找不到。

### 创建模型

```python
class Book(models.Model):
    title = models.CharField(max_length=32)
    price = models.DecimalField(max_digits=5, decimal_places=2)
    pub_date = models.DateField()
    publish = models.ForeignKey("Publish", on_delete=models.CASCADE) #外键：只对应一个
    authors = models.ManyToManyField("Author") # 多对多关系会生成一张关系表


class Publish(models.Model):
    name = models.CharField(max_length=32)
    city = models.CharField(max_length=64)
    email = models.EmailField()


class Author(models.Model):
    name = models.CharField(max_length=32)
    age = models.SmallIntegerField()
    au_detail = models.OneToOneField("AuthorDetail", on_delete=models.CASCADE)


class AuthorDetail(models.Model):
    gender_choices = (
        (0, "女"),
        (1, "男"),
        (2, "保密"),
    )
    gender = models.SmallIntegerField(choices=gender_choices)
    tel = models.CharField(max_length=32)
    addr = models.CharField(max_length=64)
    birthday = models.DateField()
```

### ORM-添加数据

#### 一对多

一对多场景中，创建“多”端对象时，可以把“一”端对象/id传入

##### 传对象

```python
def add_book(request):
    # 获取"一"对象
    pub_obj=models.Publish.objects.filter(pk=1).first()
    # 把"一"对象当做属性
    book=models.Book.objects.create(title="菜鸟教程", price=200, pub_date="2010-10-10", publish=pub_obj)
    print(book, type(book))
    return HttpResponse(book)
```

##### 传id

```python
def add_book(request):
    #  获取出版社对象
    pub_obj = models.Publish.objects.filter(pk=1).first()
    #  获取出版社对象的id
    pk = pub_obj.pk
    #  给书籍的关联出版社字段 publish_id 传出版社对象的id
    book = models.Book.objects.create(title="冲灵剑法", price=100, pub_date="2004-04-04", publish_id=pk)
    print(book, type(book)) 
    return HttpResponse(book)
```

#### 多对多

对可以有多个值的属性直接add，支持对象和id, 但在一对多中不能传id。

##### 传对象

```python
def add_book(request):
    chong = models.Author.objects.filter(name="令狐冲").first()
    ying = models.Author.objects.filter(name="任盈盈").first()
    book = models.Book.objects.filter(title="菜鸟教程").first()
    #  给书籍对象的 authors 属性用 add 方法传作者对象
    book.authors.add(chong, ying)
    return HttpResponse(book)
```

##### 传id

```python
def add_book(request):
    chong = models.Author.objects.filter(name="令狐冲").first()
    pk = chong.pk
    book = models.Book.objects.filter(title="冲灵剑法").first()
    #  给书籍对象的 authors 属性用 add 方法传作者对象的id
    book.authors.add(pk)
```

#### 关联管理器

**前提：**

- 多对多（双向均有关联管理器）
- 一对多（只有多的那个类的对象有关联管理器，即反向才有）

**语法格式：**

```
正向：属性名.add
反向：小写类名_set.add/remove/clear
```

正向（添加进来）：

```python
book_obj = models.Book.objects.get(id=10)
author_list = models.Author.objects.filter(id__gt=2)
# 方式一：传对象
book_obj.authors.add(*author_list)  # 将 id 大于2的作者对象添加到这本书的作者集合中
# 方式二：传对象 id
book_obj.authors.add(*[1,3]) # 将 id=1 和 id=3 的作者对象添加到这本书的作者集合中
return HttpResponse("ok")
```

反向（加入进去）：小写表名_set

```python
ying = models.Author.objects.filter(name="任盈盈").first()
book = models.Book.objects.filter(title="冲灵剑法").first()
# 加入集合
ying.book_set.add(book)
# 退出集合
ying.book_set.remove(book)
return HttpResponse("ok")
```

### ORM-查询

#### 一对多

正向：直接使用.语法即可查询

```python
book = models.Book.objects.filter(pk=10).first()
res = book.publish.city
print(res, type(res))
return HttpResponse("ok")
```

反向：需要使用_set

```python
pub = models.Publish.objects.filter(name="明教出版社").first()
res = pub.book_set.all()
for i in res:
    print(i.title)
return HttpResponse("ok")
```

#### 一对一

正向：

```python
author = models.Author.objects.filter(name="令狐冲").first()
res = author.au_detail.tel
print(res, type(res))
return HttpResponse("ok")
```

反向：小写类名

```python
addr = models.AuthorDetail.objects.filter(addr="黑木崖").first()
res = addr.author.name
print(res, type(res))
return HttpResponse("ok")
```

#### 多对多

正向：

```python
book = models.Book.objects.filter(title="菜鸟教程").first()
res = book.authors.all()
for i in res:
    print(i.name, i.au_detail.tel)
return HttpResponse("ok")
```

反向：小写类名_set

```python
author = models.Author.objects.filter(name="任我行").first()
res = author.book_set.all()
for i in res:
    print(i.title)
return HttpResponse("ok")
```

### __跨表查询

正向：`属性名称__跨表的属性名称 __`

反向：小写类名__跨表的属性名称

#### 一对多

正向：

```python
res = models.Book.objects.filter(publish__name="菜鸟出版社").values_list("title", "price")
```

publish属于book表的属性，name属于跨表的属性

反向：

```python
res = models.Publish.objects.filter(name="菜鸟出版社").values_list("book__title","book__price")
return HttpResponse("ok")
```



#### 多对多

查询任我行出过的所有书籍的名字。

正向：通过` 属性名称__跨表的属性名称(authors__name) `跨表获取数据：

```python
res = models.Book.objects.filter(authors__name="任我行").values_list("title")
```

反向：通过` 小写类名__跨表的属性名称（book__title） `跨表获取数据：

```python
res = models.Author.objects.filter(name="任我行").values_list("book__title")
```

#### 一对一

查询任我行的手机号。

正向：通过` 属性名称__跨表的属性名称(au_detail__tel) `跨表获取数据。

```python
res = models.Author.objects.filter(name="任我行").values_list("au_detail__tel")
```

反向：通过` 小写类名__跨表的属性名称（author__name） `跨表获取数据。

```python
res = models.AuthorDetail.objects.filter(author__name="任我行").values_list("tel")
```



## 聚合查询



## managed=False

这样的Model不会自动创建表，这样我们可以先根据已有表自动生成Model类，而且`from django.forms import model_to_dict`函数可以把model类直接转为dict。

`from django.http import JsonResponse` 可以把dict封装为json响应。

# sql操作

[参考](https://www.cnblogs.com/kristin/p/10984798.html)

```python
from django.db import connection
def research(request):
    cursor = connection.cursor()
    cursor.execute("SELECT * FROM app01_book")
    sql = cursor.fetchall()
    print(sql)
    return HttpResponse(sql)
```

# session

[用户模块与权限系统](https://ebook-django-study.readthedocs.io/zh-cn/latest/django%E5%85%A5%E9%97%A8%E8%BF%9B%E9%98%B607%E7%94%A8%E6%88%B7%E6%A8%A1%E5%9D%97%E4%B8%8E%E6%9D%83%E9%99%90%E7%B3%BB%E7%BB%9F.html)

## [session 框架](https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Server-side/Django/Sessions)

Django使用Session机制跟踪网站和浏览器之间的状态。浏览器存储的数据在连接网站时，对网站可见。

Django使用一个包含特殊session id的cookie识别浏览器以及它的本网站相关的session。实际的session数据默认存储在网站数据库，可以配置Django把session数据存储到其他位置，但推荐默认。

1. 启用session

```python
INSTALLED_APPS = [
    # …
    'django.contrib.sessions',
    # …

MIDDLEWARE = [
    # …
    'django.contrib.sessions.middleware.SessionMiddleware',
    # 
```

2. 使用session
   从视图函数的request参数中获取session, 该session代表了当前浏览器的连接(使用浏览器保存的网站的cookie中的session id标记)。
   session属性是字典对象，可以做增删改查

3. 保存会话数据
   默认情况下，Django会在session修改/删除时保存到session数据库并发送session cookie到客户端。但如果我们修改的是session内部的数据，则需要我们明确标记已修改。

   ```python
   # Session object not directly modified, only data within the session. Session changes not saved!
   request.session['my_car']['wheels'] = 'alloy'
   
   # Set session as modified to force data updates/cookie to be saved.
   request.session.modified = True
   ```

   







# Django From组件









# 常用命令

1. **新增app**

   ```python
   django-admin startapp app名称
   ```

   修改settings文件的INSTALLED_APPS

2. 创建模型
   在app的models.py里新增模型后，建表：

   ```python
   $ python3 manage.py makemigrations app名称  # 让 Django 知道我们在我们的模型有一些变更
   $ python3 manage.py migrate app名称   # 创建表结构
   ```

3. **启动项目**

   ```shell
   python3 manage.py runserver 0.0.0.0:8000
   
   nohup python manage.py runserver 0.0.0.0:8000 &
   jobs -l #查看pid
   ```

​		封装一个启动脚本

```shell
cat   start_server.sh
set -eux
#pyenv activate hados-env
pwd
cd `pwd`/hados_vmp

#!/bin/bash
current_env=$(pyenv version-name)
target_env=hados-env
if [ "$current_env"!= "$target_env" ]; then
    pyenv activate "$target_env"
else
    echo "Virtual environment $target_env is already activated."
fi
nohup python manage.py runserver 0.0.0.0:8000 &
```



4. **根据表自动生成models类**

```shell
python manage.py inspectdb 表名 --settings=hados_vmp.settings.dev  
```

自增id列不会生成，

## django-admin

[参考](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#top)

`manage.py` 会在每个 Django 项目中自动创建。它做的事情和 `django-admin` 一样，但也设置了 [`DJANGO_SETTINGS_MODULE`](https://docs.djangoproject.com/zh-hans/5.1/topics/settings/#envvar-DJANGO_SETTINGS_MODULE) 环境变量，使其指向你的项目的 `settings.py` 文件。

当你在一个 Django 项目中工作时，使用 `manage.py` 比使用 `django-admin` 更容易。如果你需要在多个 Django 配置文件之间切换，可以使用 `django-admin` 与 [`DJANGO_SETTINGS_MODULE`](https://docs.djangoproject.com/zh-hans/5.1/topics/settings/#envvar-DJANGO_SETTINGS_MODULE) 或 [`--settings`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#cmdoption-settings) 命令行选项。

用法

```python
$ django-admin <command> [options]
$ manage.py <command> [options]
$ python -m django <command> [options]
```

所有命令

```shell
$django-admin help
    check  #检查整个 Django 项目的常见问题。
    compilemessages
    createcachetable #使用你的配置文件中的信息创建用于数据库缓存后台的缓存表。
    dbshell #运行你的 ENGINE 配置中指定的数据库引擎的命令行客户端，
    diffsettings
    dumpdata
    flush
    inspectdb
    loaddata
    makemessages
    makemigrations
    migrate
    optimizemigration
    runserver
    sendtestemail
    shell
    showmigrations
    sqlflush
    sqlmigrate
    sqlsequencereset
    squashmigrations
    startapp [directory] #新建一个app,可以指定其目录
    startproject
    test
    testserver
```



### 迁移

迁移是 Django 将你对模型的修改（例如增加一个字段，删除一个模型）应用至数据库架构中的方式。

以下是几个常用的与迁移交互的命令，即 Django 处理数据库架构的方式：

- [`migrate`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#django-admin-migrate)，负责应用和撤销迁移。
- [`makemigrations`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#django-admin-makemigrations)，基于模型的修改创建迁移。
- [`sqlmigrate`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#django-admin-sqlmigrate)，展示迁移使用的 SQL 语句。
- [`showmigrations`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#django-admin-showmigrations)，列出项目的迁移和迁移的状态。

你应该将迁移看作是数据库架构的版本控制系统。`makemigrations` 负责将模型修改打包进独立的迁移文件中——类似提交修改，而 `migrate` 负责将其应用至数据库。每个应用的迁移文件位于该应用的 "migrations" 目录中，他们被设计成应用代码的一部分，与应用代码一起被提交，被发布。你只需在开发机上构建一次，就可以在同事的电脑或测试机上运行同样的迁移而保证结果一致。最后在生产环境运行同样的迁移。







# 日志记录

参考[日志配置](https://bbs.huaweicloud.com/blogs/372260)

1. 通过`logging.getLogger(name)`来获取logger对象
2. loggers对象是有父子关系的，`logging.getLogger("abc.xyz")` 会创建两个logger对象，一个是abc父对象，一个是xyz子对象。子对象会复用父对象的日志配置

多模块项目有两种方式配置logging:

1. 通过继承关系实现
2. 通过yaml配置文件实现

## 自定义装饰器

[How to Create Decorators in Django](https://dev.to/pymeister/how-to-create-decorators-in-django-2f8d)

1. 创建一个包装视图函数的函数

   ```python
   def my_decorator(view_func):
       def wrapper(request, *args, **kwargs):
           # code to be executed before the view
           response = view_func(request, *args, **kwargs)
           # code to be executed after the view
           return response
       return wrapper
   ```

2. 使用装饰器

   ```python
   @my_decorator
   def my_view(request):
       # code for the view
       return HttpResponse("Hello, World!")
   ```

   可以写多个装饰器，会按顺序执行











# requests

requests[封装](https://juejin.cn/post/7274536210730991652)

# 跨域及ALLOWD_HOST

## allowed_host

在 Django 项目的配置文件 `settings.py` 中，有一个 `ALLOWED_HOSTS` 变量，它用于指定允许访问该 Django 项目的主机名或 IP 地址列表。

## 允许跨域

[参考](https://pypi.org/project/django-cors-headers/3.12.0/)

为了适配django3.2.25,需要安装较老版本

```shell
pip install django-cors-headers==3.12.0
```

必须设置以下三者之一：

- CORS_ALLOWED_ORIGINS： Sequence[str]  授权源列表
- CORS_ALLOWED_ORIGIN_REGEXES：Sequence[str | Pattern[str]]  支持正则，在第一个无法实现时使用
- CORS_ALLOW_ALL_ORIGINS:bool,  默认False,如果设置为True，则允许所有源，危险。

例如

```python
CORS_ALLOWED_ORIGINS = [
    "https://example.com",
    "https://sub.example.com",
    "http://localhost:8080",
    "http://127.0.0.1:9000",
]
```

## 问题

### [ModuleNotFoundError: No module named 'corsheaders](https://www.cnblogs.com/jingzaixin/p/13575231.html)

## 后端重定向遇到cors错误

Access to fetch at 'http://devhub.yusur.tech/login?c_url=http%3a%2f%2fvmp-dev.yusur.tech%3a3000' (redirected from 'http://vmp-dev.yusur.tech:8000/pipeline/list?type=1') from origin 'http://vmp-dev.yusur.tech:3000' has been blocked by CORS policy: Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource. If an opaque response serves your needs, set the request's mode to 'no-cors' to fetch the resource with CORS disabled.





# 问题

## ImportError: cannot import name 'url' from 'django.conf.urls'

[参考](https://stackoverflow.com/questions/70319606/importerror-cannot-import-name-url-from-django-conf-urls-after-upgrading-to)

## django.contrib.auth.middleware.AuthenticationMiddleware' must be in MIDDLEWARE in order to use the admin application











## Watching for file changes with StatReloade

虚拟环境缺少依赖包



# 参考

[`django-admin` 和 `manage.py`](https://docs.djangoproject.com/zh-hans/5.1/ref/django-admin/#top)

[Django框架教程](https://c.biancheng.net/django/)

[日志配置](https://bbs.huaweicloud.com/blogs/372260)

[Django settings多环境配置](https://www.cnblogs.com/dannyyao/p/10345905.html)

[大讲狗](https://pythondjango.cn/python/tools)

[django tutorial](https://developer.mozilla.org/en-US/docs/Learn_web_development/Extensions/Server-side/Django/Sessions)