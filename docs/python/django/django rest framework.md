# 安装

settings.py文件里

```python
REST_FRAMEWORK = {
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAdminUser',  #API只能由管理员使用
    ],
    'PAGE_SIZE': 10
}


INSTALLED_APPS = [
     ... ...
    'rest_framework',   #获取一个图形化的页面来操作API
    'your_app', # 你自己的app
]
```



# 序列化器

**Serializer 与 Model 的区别：**
 Serializer 是数据验证、序列化和反序列化的工具，而 Model 类关注数据持久化。Serializer 提供了一层抽象，让你可以灵活控制 API 数据的输入输出，而不直接操作数据库模型。

## Django自带的序列化器


```python
# Django Queryset数据 to Json
from django.core import serializers
data = serializers.serialize("json", SomeModel.objects.all())
data1 = serializers.serialize("json", SomeModel.objects.all(), fields=('name','id'))
data2 = serializers.serialize("json", SomeModel.objects.filter(field = some_value))

# 只序列化部分字段
import json
from django.core.serializers.json import DjangoJSONEncoder
queryset = myModel.objects.filter(foo_icontains=bar).values('f1', 'f2', 'f3')
data4 = json.dumps(list(queryset), cls=DjangoJSONEncoder)
```

## DRF序列化器

尽管Django自带的serializers类也能将Django的查询集QuerySet序列化成json格式数据，Django REST Framework才是你真正需要的序列化工具。与django自带的serializers类相比，DRF的序列化器更强大，可以根据模型生成序列化器，还能对客户端发送过来的数据进行验证。



就像Django提供了`Form`类和`ModelForm`类两种方式自定义表单一样，REST framework提供了`Serializer`类和`ModelSerializer`类两种方式供你自定义序列化器。前者需手动指定需要序列化和反序列化的字段，后者根据模型(model)生成需要序列化和反序列化的字段，可以使代码更简洁。

假如我们有一个Model

```python
class Snippet(models.Model):
    created = models.DateTimeField(auto_now_add=True)
    title = models.CharField(max_length=100, blank=True, default='')
    code = models.TextField()
    linenos = models.BooleanField(default=False)
    language = models.CharField(choices=LANGUAGE_CHOICES, default='python', max_length=100)
    style = models.CharField(choices=STYLE_CHOICES, default='friendly', max_length=100)

    class Meta:
        ordering = ('created',)
```



### 使用Serializer类

```python
class SnippetSerializer(serializers.Serializer):
    # 定义序列化、反序列化字段， 注意这里对输入数据的验证
    id = serializers.IntegerField(read_only=True)
    title = serializers.CharField(required=False, allow_blank=True, max_length=10
    code = serializers.CharField(style={'base_template': 'textarea.html'})
    linenos = serializers.BooleanField(required=False)
    language = serializers.ChoiceField(choices=LANGUAGE_CHOICES, default='python'
    style = serializers.ChoiceField(choices=STYLE_CHOICES, default='friendly')
    def create(self, validated_data):
        """
        定义了在调用serializer.save()时如何创建一个实例，
        根据提供的验证过的数据创建并返回一个新的`Snippet`实例。
        """
        return Snippet.objects.create(**validated_data)
    def update(self, instance, validated_data):
        """
        定义了在调用serializer.save()时如何修改一个实例
        根据提供的验证过的数据更新和返回一个已经存在的`Snippet`实例。
        """
        instance.title = validated_data.get('title', instance.title)
        instance.code = validated_data.get('code', instance.code)
        instance.linenos = validated_data.get('linenos', instance.linenos)
        instance.language = validated_data.get('language', instance.language)
        instance.style = validated_data.get('style', instance.style)
        instance.save()
        return instance
```



### 使用ModelSerializer类

继承这两个类的类就是序列化器了

```python
serializers.ModelSerializer
						HyperlinkedModelSerializer
```

会根据模型（Model）自动生成一系列字段。它主要是将模型实例转换为 Python 的原生数据类型（如字典、列表等），以便能够将这些数据轻松地转换为 JSON、XML 或其他内容类型，然后在 API 中返回给客户端。

HyperlinkedModelSerializer 的主要特点是在序列化数据时，会使用超链接（Hyperlinks）来表示模型之间的关系，也就是会提供一个url字段。

例如

```python
class SnippetSerializer(serializers.ModelSerializer):
    class Meta:
        model = Snippet
        fields = ('id', 'title', 'code', 'linenos', 'language', 'style')
        #fields = '__all__'  所有字段
        # 自定义验证器，检查 age 字段
        def validate_age(self, value):
            if value < 1 or value > 120:
                raise serializers.ValidationError("年龄必须在 1 到 120 之间。")
            return value
```

fields定义了get请求返回的字段，不在这里出现的字段会被忽略。

它负责：

- 将 JSON 输入数据反序列化为 Python 数据类型
- 自动检查字段类型和必填项
- 执行自定义验证逻辑（例如验证年龄范围）
- 最终通过 `serializer.save()` 创建模型实例

# 视图

## 函数视图

### @api_view注解

- **RDF引入了一个扩展了常规`HttpRequest`的`Request`对象**，并提供了更灵活的请求解析。`Request`对象的核心功能是`request.data`属性，它与`request.POST`类似，但对于使用Web API更为有用。

```python
request.POST  # 只处理表单数据  只适用于'POST'方法
request.data  # 处理任意数据  适用于'POST'，'PUT'和'PATCH'方法
```

- **RDF还引入了一个`Response`对象**
  我们不再显式地将请求或响应绑定到给定的内容类型比如HttpResponse和JSONResponse，我们统一使用Response方法返回响应，该方法支持内容协商，可根据客户端请求的内容类型返回不同的响应数据。

```python
from rest_framework.response import Response



@api_view(['GET', 'POST'])
def snippet_list(request):
    """
    列出所有的snippets，或者创建一个新的snippet。
    """
    if request.method == 'GET':
        snippets = Snippet.objects.all()
        				# ❤️使用序列化器对模型进行序列化
        serializer = SnippetSerializer(snippets, many=True)
        				# ❤️统一Response, 不再区分HTTPResponse还是JSONResponse
        return Response(serializer.data)

    elif request.method == 'POST':
								# ❤️request.data 获取任意请求数据， 代替POST
        serializer = SnippetSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

      
@api_view(['GET', 'PUT', 'DELETE'])
def snippet_detail(request, pk):
    """
    获取，更新或删除一个snippet实例。
    """
    try:
        snippet = Snippet.objects.get(pk=pk)
    except Snippet.DoesNotExist:
        return Response(status=status.HTTP_404_NOT_FOUND)

    if request.method == 'GET':
        serializer = SnippetSerializer(snippet)
        return Response(serializer.data)

    elif request.method == 'PUT':
        serializer = SnippetSerializer(snippet, data=request.data)#这里无需对request进行JSON解析，其data就是json请求
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data) #也无需指定内容类型
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

    elif request.method == 'DELETE':
        snippet.delete()
        return Response(status=status.HTTP_204_NO_CONTENT)
```



## 基于类的视图

### why?

类可以被继承、拓展，提高代码的复用性，特别是将一些可以共用的功能抽象成Mixin类或基类后可以减少重复造轮子的工作。

DRF推荐使用基于类的视图(CBV)来开发API, 并提供了4种开发CBV开发模式。

- 使用基础的`APIView`类
- 使用Mixins类和`GenericAPI`类混配
- 使用通用视图`generics.*`类, 比如`generics.ListCreateAPIView`
- 使用视图集`ViewSet`和`ModelViewSet`

**注意**：类视图需要调用`as_view()`的方法才能在视图中实现查找指定方法

### 使用APIView类

```python
class SnippetList(APIView):
    """
    列出所有的snippets或者创建一个新的snippet。
    """
    def get(self, request, format=None):
        snippets = Snippet.objects.all()
        serializer = SnippetSerializer(snippets, many=True)
        return Response(serializer.data)

    def post(self, request, format=None):
        serializer = SnippetSerializer(data=request.data)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_201_CREATED)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
      
    url(r'^snippets/$', views.SnippetList.as_view()),      
```

注意：

1. APIView类继承了Django自带的View类，它不仅支持更多请求方法，而且它的Response对Django的HTTPRequest对象进行了封装，可以使用request.data获取用户通过POST, PUT和PATCH方法发过来的数据，而且支持插拔式地配置认证、权限和限流类。

2. 它的Response代替了Django的HttpResponse可根据客户端请求的内容类型返回不同的响应数据

3. 所哟API异常会被自动捕获

4. 不同的HTTP方法由不同的函数来实现，逻辑上更清晰

5. url里对类调用as_view()函数，从而在视图中实现查找指定方法

6. Response可以直接返回json, 例如：

   ```python
   # 返回普通JSON
   return Response({'some': 'data'})
   # 之间返回Model
   usernames = [user.username for user in User.objects.all()]
   return Response(usernames)
   # 自定义返回格式
   return Response({
               'data':serializer.data,
               'status':"SUCCESS"
           })
   ```

   

### 用Mixin类和GenericAPI类混配

使用基于类视图的最大优势之一是它可以轻松地创建可复用的行为。mixin类中已经实现基本的增删改查功能。

**使用`GenericAPIView`类来提供APIView的核心功能，不过它比APIView类更强大，因为它还可以通过`queryset`和`serializer_class`属性指定需要序列化与反序列化的模型或queryset及所用到的序列化器类。并添加mixins来提供具体的`.retrieve()`，`.update()`和`.destroy()`操作。**

```python
from snippets.models import Snippet
from snippets.serializers import SnippetSerializer
from rest_framework import mixins
from rest_framework import generics

class SnippetList(mixins.ListModelMixin,
                  mixins.CreateModelMixin,
                  generics.GenericAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer

    def get(self, request, *args, **kwargs):
        return self.list(request, *args, **kwargs)

    def post(self, request, *args, **kwargs):
        return self.create(request, *args, **kwargs)
      
      
class SnippetDetail(mixins.RetrieveModelMixin,
                    mixins.UpdateModelMixin,
                    mixins.DestroyModelMixin,
                    generics.GenericAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer

    def get(self, request, *args, **kwargs):
        return self.retrieve(request, *args, **kwargs)

    def put(self, request, *args, **kwargs):
        return self.update(request, *args, **kwargs)

    def delete(self, request, *args, **kwargs):
        return self.destroy(request, *args, **kwargs)      
```

### 使用通用视图generics.*APIView 类

还可以更简洁：

```python
from rest_framework import generics

from rest_framewk.models import Snippet
from rest_framewk.serializers import SnippetSerializer


class SnippetList(generics.ListCreateAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer


class SnippetDetail(generics.RetrieveUpdateDestroyAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer
```

其它常用generics类视图还包括`ListAPIView`, `RetrieveAPIView`, `RetrieveUpdateAPIView`等等。你可以根据实际需求使用，为你的API写视图时只需要定义`queryset`和`serializer_class`即可。



测试URL

```python
    re_path(r'^cbv/snippets/$', cbv.SnippetList.as_view()),
    # 捕获命名组pk是参数名
    re_path(r'^cbv/snippets/(?P<pk>[0-9]+)/$', cbv.SnippetDetail.as_view()),
```





### 使用视图集

感觉丢失了灵活性，增加了代码复杂度

# [认证与权限](https://pythondjango.cn/django/rest-framework/5-permissions/)

无论是Django还是DRF, 当用户成功通过身份验证以后，系统会把已通过验证的用户对象与request请求绑定，这样一来你就可以使用`request.user`获取这个用户对象的所有信息了。







# 参考

[大江狗的DRF](https://pythondjango.cn/django/rest-framework/1-RESTfull-API-why-DRF/)
