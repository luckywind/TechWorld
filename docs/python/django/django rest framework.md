# 相关设置

settings.py文件里

```python
REST_FRAMEWORK = {
    'DEFAULT_PERMISSION_CLASSES': [
        'rest_framework.permissions.IsAdminUser',  #API只能由管理员使用
    ],
    'PAGE_SIZE': 10
}
```



# 序列化器

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
```

## 根据序列化器创建视图

```python
def snippet_list(request):
    """
    列出所有的code snippet，或创建一个新的snippet。
    """
    if request.method == 'GET':
        snippets = Snippet.objects.all()
        serializer = SnippetSerializer(snippets, many=True)
        return JSONResponse(serializer.data)

    elif request.method == 'POST':
        data = JSONParser().parse(request)
        serializer = SnippetSerializer(data=data)
        if serializer.is_valid():
            serializer.save()
            return JSONResponse(serializer.data, status=201)
        return JSONResponse(serializer.errors, status=400)

```

# 请求和响应

- REST框架引入了一个扩展了常规`HttpRequest`的`Request`对象，并提供了更灵活的请求解析。`Request`对象的核心功能是`request.data`属性，它与`request.POST`类似，但对于使用Web API更为有用。

```python
request.POST  # 只处理表单数据  只适用于'POST'方法
request.data  # 处理任意数据  适用于'POST'，'PUT'和'PATCH'方法
```

- REST框架还引入了一个`Response`对象

```python
@api_view(['GET', 'POST'])
def snippet_list(request):
    """
    列出所有的snippets，或者创建一个新的snippet。
    """
    if request.method == 'GET':
        snippets = Snippet.objects.all()
        serializer = SnippetSerializer(snippets, many=True)
        return Response(serializer.data)

    elif request.method == 'POST':
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

我们不再显式地将请求或响应绑定到给定的内容类型。`request.data`可以处理传入的`json`请求。

# 基于类的视图

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

1. 继承APIView类
2. 不同的HTTP方法由不同的函数来实现
3. url里对类调用as_view()函数

## 使用混合

使用基于类视图的最大优势之一是它可以轻松地创建可复用的行为。mixin类中已经实现基本的增删改查功能。

使用`GenericAPIView`类来提供核心功能，并添加mixins来提供具体的`.retrieve()`，`.update()`和`.destroy()`操作。

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

还可以更简洁：

```python
from snippets.models import Snippet
from snippets.serializers import SnippetSerializer
from rest_framework import generics


class SnippetList(generics.ListCreateAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer


class SnippetDetail(generics.RetrieveUpdateDestroyAPIView):
    queryset = Snippet.objects.all()
    serializer_class = SnippetSerializer
```

