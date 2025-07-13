# subprocess

| 参数   | 含义    | 用法      |
| -------- | -------------- | --------------------------------- |
| `args`   | 命令及参数     | 列表形式（如`["ls", "-l"]`）      |
| `cwd`    | 工作目录       | 直接指定路径，避免使用`cd`命令    |
| `check`  | 检查返回码     | `True`（配合`try-except`使用，当子进程返回非零退出码时抛出 subprocess.CalledProcessError 异常）    |
| `shell`  | 是否使用 Shell | `False`（除非需要 Shell 特性）    |
| `stdout` | 标准输出       | `subprocess.PIPE`（捕获输出）     |
| `stderr` | 标准错误       | `subprocess.PIPE`（捕获错误信息） |
| `text`   | 文本模式       | `True`(避免处理字节流)            |

# selenium

Selenium 是一个广泛使用的自动化测试工具，主要用于 Web 应用程序的自动化测试。

- **Selenium WebDriver**：这是 Selenium 的核心组件，用于直接与浏览器进行交互。WebDriver 提供了丰富的 API，允许开发者通过代码控制浏览器的行为，如打开网页、点击按钮、填写表单等。
- **Selenium IDE**：这是一个浏览器插件，主要用于录制和回放用户的操作。Selenium IDE 适合初学者快速创建简单的测试脚本，但它不支持复杂的逻辑和条件判断。
- **Selenium Grid**：这是一个用于并行执行测试的工具。通过 Selenium Grid，你可以在多个浏览器和操作系统上同时运行测试，从而加快测试速度并提高测试覆盖率。



## 元素定位

[详细请参考](https://www.runoob.com/selenium/selenium-element-positioning.html)

```python
from selenium.webdriver.common.by import By

element = driver.find_element(By.ID, "username")
driver.find_element(By.NAME, "password")
driver.find_element(By.CLASS_NAME, "submit-btn")
driver.find_element(By.TAG_NAME, "input") # 通过元素的标签名定位（如 <div>、<input> 等）。
driver.find_element(By.CSS_SELECTOR, "input#username")
driver.find_element(By.XPATH, "//input[@id='username']") #通过XPath表达式定位
```

![image-20250603111730450](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/image-20250603111730450.png)

```python
1. 类选择器
<input class="login-input username" placeholder="请输入账号">

# 用类名定位（支持链式选择）
username = driver.find_element(By.CSS_SELECTOR, ".login-input.username")

2. ID选择器
<button id="submit-order">立即支付</button>
# 用ID定位（速度快且精准）
pay_btn = driver.find_element(By.CSS_SELECTOR, "#submit-order")

3. 属性选择器
<a href="javascript:;" data-sku="1001">加入购物车</a>
# 用自定义属性定位（比 XPath 清爽多了）
add_cart = driver.find_element(By.CSS_SELECTOR, "a[data-sku='1001']")

4.组合选择器
<ul class="user-list">
  <li>
    <span class="name">张三</span>
    <button class="edit-btn">编辑</button>
  </li>
</ul>

# 父子级+类名联合定位
edit_zhangsan = driver.find_element(By.CSS_SELECTOR, ".user-list li .edit-btn")
```



高级技巧

1. 等待机制

2. 动态类名处理，*=可实现模糊匹配
   ```python
   # 用属性包含符定位（模糊匹配）
   dynamic_btn = driver.find_element(By.CSS_SELECTOR, "[class*='btn']")
   ```

3. Selenium默认只返回第一个匹配，如果要都返回，用find_elements





### XPath表达式

XPath 使用路径表达式在 XML 文档中进行导航



```xml
.//div[contains(@class, "sen-eng")]
```

1. 基本结构
   //tagname：选择文档中所有指定标签名的元素（不考虑位置）。
   /tagname：从根节点开始选择指定标签名的子元素。
2. 属性匹配
   [@attribute=value]：选择具有特定属性和值的元素。 示例：`//div[@class="example"]`
   contains(@attribute, substring)：检查属性值是否包含某个子字符串。 示例：`//li[contains(@class, "mcols")]`
3. 逻辑运算符
   and、or、not()：用于组合多个条件。 示例：`//li[contains(@class, "sen-eng") and contains(@class, "sen-ch")]`
4. 嵌套查询
   .//tagname：在当前节点下查找指定标签名的子元素。 示例：.//div[contains(@class, "sen-eng")]
5. 组合表达式
   可以通过括号和逻辑操作符构建复杂的表达式，如上面的valid_li_selector所示。

## 元素操作

[详细请参考](https://www.runoob.com/selenium/selenium-ele-operator.html)

```python
# 定位按钮元素
element = driver.find_element_by_id("submit-button")
# 点击按钮
element.click()
element.clear()# 清除输入框
element.get_attribute("class")#获取属性
element.text #获取文本

```

## 等待机制

### 隐式等待

隐式等待是一种全局性的等待机制，它会在查找元素时等待一定的时间。如果在指定的时间内找到了元素，Selenium 会立即继续执行后续操作；如果超时仍未找到元素，则会抛出 `NoSuchElementException` 异常。

```python
# 设置隐式等待时间为 10 秒
driver.implicitly_wait(10)
```



### 显示等待

显式等待是一种更为灵活的等待机制，它允许你为特定的操作设置等待条件。显式等待通常与 `WebDriverWait` 类和 `expected_conditions` 模块一起使用。

```python
# 设置显式等待，最多等待 10 秒，直到元素出现
element = WebDriverWait(driver, 10).until(
    EC.presence_of_element_located((By.ID, "element_id"))
)


# 等待所有 class 为 "def" 的 span 元素加载完成
    elements = WebDriverWait(driver, 10).until(
        EC.presence_of_all_elements_located((By.CSS_SELECTOR, 'span.def'))
    )
```

## 无头模式

### 为什么使用无头浏览器模式？

- **节省资源**：无头模式不需要渲染图形界面，因此可以节省 CPU 和内存资源。
- **提高速度**：由于不需要加载和渲染图形界面，无头模式通常比普通模式更快。
- **适合自动化**：在自动化测试和网页抓取中，无头模式可以避免干扰，并且可以在没有显示器的服务器上运行。
- **便于调试**：在某些情况下，无头模式可以帮助开发者更快地调试和定位问题。

```python
# 设置 Chrome 无头模式
chrome_options = Options()
chrome_options.add_argument("--headless")  # 启用无头模式
chrome_options.add_argument("--disable-gpu")  # 禁用 GPU 加速
```

