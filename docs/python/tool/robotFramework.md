- Enables easy-to-use tabular syntax for [creating test cases](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-test-cases) in a uniform way.
- Provides ability to create reusable [higher-level keywords](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-user-keywords) from the existing keywords.
- Provides easy-to-read result [reports](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#report-file) and [logs](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#log-file) in HTML format.
- Is platform and application independent.
- Provides a simple [library API](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-test-libraries) for creating customized test libraries which can be implemented natively with Python.
- Provides a [command line interface](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#executing-test-cases-1) and XML based [output files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#output-file) for integration into existing build infrastructure (continuous integration systems).
- Provides support for testing web applications, rest APIs, mobile applications, running processes, connecting to remote systems via Telnet or SSH, and so on.
- Supports creating [data-driven test cases](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#data-driven-style).
- Has built-in support for [variables](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variables), practical particularly for testing in different environments.
- Provides [tagging](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#tagging-test-cases) to categorize and [select test cases](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#selecting-test-cases) to be executed.
- Enables easy integration with source control: [test suites](http://robot-framework.readthedocs.org/en/master/autodoc/robot.running.html#robot.running.model.TestSuite) are just files and directories that can be versioned with the production code.
- Provides [test-case](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#test-setup-and-teardown) and [test-suite](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#suite-setup-and-teardown) -level setup and teardown.
- The modular architecture supports creating tests even for applications with several diverse interfaces.

架构图：

![src/GettingStarted/architecture.png](https://piggo-picture.oss-cn-hangzhou.aliyuncs.com/architecture-20250422175348506.png)



test data是表格形式的，Robot框架处理数据、执行test case然后生成logs和reports， 通过libraries与Robot框架交互。Libraries可以使用应用接口，也可以使用底层测试工具作为驱动。



安装

```shell
pip install robotframework

robot --version
```



# 创建测试数据

##[ Test data syntax](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#test-data-syntax)

- 包含test case 文件的目录形成一个高阶test suite， test suite 目录可以嵌套。

- test suite 目录可以有一个特殊的初始化文件用于配置test suite

- [Test libraries](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-test-libraries) containing the lowest-level keywords.
- [Resource files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#resource-files) with [variables](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variables) and higher-level [user keywords](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-user-keywords).
- [Variable files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variable-files) to provide more flexible ways to create variables than resource files.



### 测试数据的sections:

| Section    | Used for                                                     |
| ---------- | ------------------------------------------------------------ |
| Settings   | 1) Importing [test libraries](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-test-libraries), [resource files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#resource-files) and [variable files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variable-files).2) Defining metadata for [test suites](http://robot-framework.readthedocs.org/en/master/autodoc/robot.running.html#robot.running.model.TestSuite) and [test cases](http://robot-framework.readthedocs.org/en/master/autodoc/robot.running.html#robot.running.model.TestCase). |
| Variables  | Defining [variables](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variables) that can be used elsewhere in the test data. |
| Test Cases | [Creating test cases](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-test-cases) from available keywords. |
| Tasks      | [Creating tasks](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-tasks) using available keywords. Single file can only contain either tests or tasks. |
| Keywords   | [Creating user keywords](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-user-keywords) from existing lower-level keywords |
| Comments   | Additional comments or data. Ignored by Robot Framework.     |

不同的section通过header行识别，例如

```shell
*** Settings ***
*settings*
```



### 支持的文件格式

- 数据可以用空格(2个及以上)分割，也可以用管道|(前后需要一个空格)分割，文件通常使用.robot作为后缀，资源文件也可以使用.robot。需要用UTF-8编码保存。

  ```python
  *** Settings ***
  Documentation     Example using the space separated format.
  Library           OperatingSystem
  
  *** Variables ***
  ${MESSAGE}        Hello, world!
  
  *** Test Cases ***
  My Test
      [Documentation]    Example test.
      Log    ${MESSAGE}
      My Keyword    ${CURDIR}
  
  Another Test
      Should Be Equal    ${MESSAGE}    Hello, world!
  
  *** Keywords ***
  My Keyword
      [Arguments]    ${path}
      Directory Should Exist    ${path}
  ```


  管道分割更美观：

  ```python
  | *** Settings ***   |
  | Documentation      | Example using the pipe separated format.
  | Library            | OperatingSystem
  
  | *** Variables ***  |
  | ${MESSAGE}         | Hello, world!
  
  | *** Test Cases *** |                 |               |
  | My Test            | [Documentation] | Example test. |
  |                    | Log             | ${MESSAGE}    |
  |                    | My Keyword      | ${CURDIR}     |
  | Another Test       | Should Be Equal | ${MESSAGE}    | Hello, world!
  
  | *** Keywords ***   |                        |         |
  | My Keyword         | [Arguments]            | ${path} |
  |                    | Directory Should Exist | ${path} |
  ```

- 也支持 [reStructuredText](https://en.wikipedia.org/wiki/ReStructuredText) 文件，以便将常规 Robot Framework 数据[嵌入到代码块中 ](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#restructuredtext-format)。
  使用 Robot Framework 的 [reStructuredText](https://en.wikipedia.org/wiki/ReStructuredText) 文件需要安装 Python [docutils](https://pypi.python.org/pypi/docutils) 模块。

- 也支持JSON，默认解析.rbt后缀的JSON文件

  - suite 转JSON

    ```python
    from robot.running import TestSuite
    
    
    # Create suite based on data on the file system.
    suite = TestSuite.from_file_system('/path/to/data')
    
    # Get JSON data as a string.
    data = suite.to_json()
    
    # Save JSON data to a file with custom indentation.
    suite.to_json('data.rbt', indent=2)
    
    ```

  - Json转suite

    ```python
    from robot.running import TestSuite
    
    
    # Create suite from JSON data in a file.
    suite = TestSuite.from_json('data.rbt')
    
    # Create suite from a JSON string.
    suite = TestSuite.from_json('{"name": "Suite", "tests": [{"name": "Test"}]}')
    
    # Execute suite. Notice that log and report needs to be created separately.
    suite.run(output='example.xml')
    ```

  - 执行JSON文件
    robot命令执行测试时，.rbt后缀的JSON文件会自动解析，如果要解析其他后缀的文件，需要配置

  - 调整suite 源

    ```python
    from robot.running import TestSuite
    
    
    # Create a suite, adjust source and convert to JSON.
    suite = TestSuite.from_file_system('/path/to/data')
    suite.adjust_source(relative_to='/path/to')
    suite.to_json('data.rbt')
    
    # Recreate suite elsewhere and adjust source accordingly.
    suite = TestSuite.from_json('data.rbt')
    suite.adjust_source(root='/new/path/to')
    ```

    

### 解析数据的规则

1. 忽略
   - 第一个test data section之前的所有数据
   - 评论section
   - 空行
   - 管道行尾的空
   - `#`开头的单元格
2. 

##[ Creating test cases](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-test-cases)

测试用例语法：
关键字可以从测试库、资源文件或者test case文件自身的keyword section中导入。

第一列是test case 名称，第二列通常是关键字，后续列是参数

```python
*** Test Cases ***
Valid Login
    Open Login Page
    Input Username    demo
    Input Password    mode
    Submit Credentials
    Welcome Page Should Be Open

Setting Variables
    Do Something    first argument    second argument
    ${value} =    Get Some Value
    Should Be Equal    ${value}    Expected value
```



##[ Creating tasks](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-tasks)

除了测试自动化，Robot框架还用于其他的自动化，包括RPA（机器人流程自动化）。

创建task非常类似创建test case, task也可以向test case一样组织进suites。

Task的语法与test case的语法最大的区别是Task使用Task section: 

```python
*** Tasks ***
Process invoice
    Read information from PDF
    Validate information
    Submit information to backend system
    Validate information is visible in web UI
```





##[ Creating test suites](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-test-suites)

test case文件以及目录创建了层级测试套件结构。

- 套件文件：

在套件文件中使用test case section创建测试用例，也称为test case 文件。这样的文件自动从它包含的所有test case创建一个测试套件。

- 套件目录：

  - .开头、_开头的目录、文件会被忽略
  - cvs目录被忽略
  - 特定格式的文件被处理
  - 其他文件被忽略

  初始化文件：
  `__init__.ext`文件是类似python模块的初始化文件一样的东西

##[ Using test libraries](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#using-test-libraries)

##[ Variables](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#variables)
##[ Creating user keywords](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#creating-user-keywords)
##[ Resource and variable files](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#resource-and-variable-files)
##[ Control structures](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#control-structures)
##[  Advanced features](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#advanced-features)

# 执行测试用例

三种语法：

```python
robot [options] data
python -m robot [options] data
python path/to/robot/ [options] data
```

也可以提供多个test case 文件或者目录，空格分割即可；这样会自动创建一个顶层的套件,自动生成的名字是他们名字的组合，可能会很长，但可以使用--name来指定。

```shell
robot my_tests.robot your_tests.robot
robot --name Example path/to/tests/pattern_*.robot
```

1. 参数文件
   `--argumentfile` 可以指定参数文件，语法：

   ```shell
   robot --argumentfile all_arguments.robot
   robot --name Example --argumentfile other_options_and_paths.robot
   robot --argumentfile default_options.txt --name Example my_tests.robot
   robot -A first.txt -A second.txt -A third.txt tests.robot
   ```

   参数文件的写法：

   ```shell
   --doc This is an example (where "special characters" are ok!)
   --metadata X:Value with spaces
   --variable VAR:Hello, world!
   # This is a comment
   path/to/my/tests
   ```

   也可以像下面这样分割参数和值：

   ```
   --name An Example
   --name=An Example
   --name       An Example
   ```

   







# 快速开始

## 执行测试

1. 示例应用
   一个基于命令行的注册、登录服务`python sut/login.py`

```shell
python sut/login.py create fred P4ssw0rd
SUCCESS
python sut/login.py login fred P4ssw0rd
Logged In
python sut/login.py change-password fred P4ssw0rd NewP4ss
SUCCESS
```



2. 依赖
   该demo使用 [reStructuredText](http://docutils.sourceforge.net/rst.html) 标记语言写的，Robot Framework测试数据嵌入到代码块中，

```shell
pip install robotframework
pip install docutils
```



3. 执行

   ```shell
   robot QuickStart.rst
   ```

   

## 测试用例

1. workflow测试

下面的表格语法创建了两个测试：

```python
*** Test Cases ***
User can create an account and log in
    Create Valid User    fred    P4ssw0rd
    Attempt to Login with Credentials    fred    P4ssw0rd
    Status Should Be    Logged In

User cannot log in with bad password
    Create Valid User    betty    P4ssw0rd
    Attempt to Login with Credentials    betty    wrong
    Status Should Be    Access Denied
```



2. 高阶测试

不需要位置参数，适合给非技术人员交流。 没有特殊的书写风格，一个通用的风格是given-when-then格式：

> given  when then and 后面都是关键字

```python
*** Test Cases ***
User can change password
    Given a user has a valid account
    When she changes her password
    Then she can log in with the new password
    And she cannot use the old password anymore
```

3. Data-driven测试
   多个非常相似的测试用例，仅仅是输入输出数据不同，适合用Data-driven测试，无需复制workflow。
   `[Template]` setting turns a test case into a data-driven test 。

```python
*** Variables ***
${USERNAME}               janedoe
${PASSWORD}               J4n3D0e
${NEW PASSWORD}           e0D3n4J
${DATABASE FILE}          ${TEMPDIR}${/}robotframework-quickstart-db.txt
${PWD INVALID LENGTH}     Password must be 7-12 characters long
${PWD INVALID CONTENT}    Password must be a combination of lowercase and uppercase letters and numbers


*** Test Cases ***
Invalid password
    [Template]    Creating user with invalid password should fail
    abCD5            ${PWD INVALID LENGTH}
    abCD567890123    ${PWD INVALID LENGTH}
    123DEFG          ${PWD INVALID CONTENT}
    abcd56789        ${PWD INVALID CONTENT}
    AbCdEfGh         ${PWD INVALID CONTENT}
    abCD56+          ${PWD INVALID CONTENT}
```

## Keywords

有两种来源

Library Keywords和User keywords

### Library Keywords

1. 标准库
   - OperatingSystem
   - Screenshot
   - BuiltIn
2. 扩展库
   - [Selenium2Library](https://github.com/rtomac/robotframework-selenium2library/#readme)  用于web测试，需要单独安装
3. 自定义库



导入

```python
*** Settings ***
Library           OperatingSystem
Library           lib/LoginLibrary.py
```



### User keywords

可以使用keywords组合成高阶keywords,  语法类似创建测试用例。

自定义keywords可以有参数，可以返回值，甚至可以包含FOR循环。

前面的测试用例用到的关键字都是在下面这个关键字表里创建的：

```properties
*** Keywords ***
# 定义几个关键字：
Clear login database
    Remove file    ${DATABASE FILE}
   # 该关键字接收两个参数
Create valid user 
    [Arguments]    ${username}    ${password} 
    # Create user 和Status should be关键字来自Settings引入的LoginLibrary.py里的_分割命名的函数
    Create user    ${username}    ${password}
    Status should be    SUCCESS

Creating user with invalid password should fail
    [Arguments]    ${password}    ${error}
    Create user    example    ${password}
    Status should be    Creating user failed: ${error}

Login
    [Arguments]    ${username}    ${password}
    Attempt to login with credentials    ${username}    ${password}
    Status should be    Logged In

# Keywords below used by higher level tests. Notice how given/when/then/and
# prefixes can be dropped. And this is a comment.

# 定义高阶关键字
A user has a valid account
    # 调用前面定义的关键字Create valid user
    Create valid user    ${USERNAME}    ${PASSWORD}

She changes her password
    Change password    ${USERNAME}    ${PASSWORD}    ${NEW PASSWORD}
    Status should be    SUCCESS

She can log in with the new password
    Login    ${USERNAME}    ${NEW PASSWORD}

She cannot use the old password anymore
    Attempt to login with credentials    ${USERNAME}    ${PASSWORD}
    Status should be    Access Denied
```

### 变量

#### 定义变量

语法

```shell
*** Variables ***
${USERNAME}               janedoe
${PASSWORD}               J4n3D0e
${NEW PASSWORD}           e0D3n4J
${DATABASE FILE}          ${TEMPDIR}${/}robotframework-quickstart-db.txt
${PWD INVALID LENGTH}     Password must be 7-12 characters long
${PWD INVALID CONTENT}    Password must be a combination of lowercase and uppercase letters and numbers
```

也可以从命令行传入

```shell
robot --variable USERNAME:johndoe --variable PASSWORD:J0hnD0e QuickStart.rst
```

除了用户定义的变量外，还有一些始终可用的内置变量。这些变量包括上例中使用的 `${TEMPDIR}` 和 `${/}` 。

## 组织测试用例

### 测试套件

在 Robot Framework 中，测试用例的集合称为测试套件。每个包含测试用例的输入文件都构成一个测试套件

由于测试套件只是文件和目录，因此可以轻松地将它们放入任何版本控制系统中。

### 启停

### 使用标签

可以使用 `Force Tags` 和 `Default` **为文件中的所有测试用例设置标签**。也可以使用 `[Tags]` 设置为单个测试用例定义标签。

```shell
*** Settings ***
Force Tags        quickstart
Default Tags      example    smoke
```

用途：可以用于选择要执行的测试：

```shell
robot --include smoke QuickStart.rst
robot --exclude database QuickStart.rst
```

## 创建测试库

示例，可以查看关键字Create User如何映射到方法的实际实现：create_user:

```python
import os.path
import subprocess
import sys


class LoginLibrary(object):

    def __init__(self):
        self._sut_path = os.path.join(os.path.dirname(__file__),
                                      '..', 'sut', 'login.py')
        self._status = ''

    def create_user(self, username, password):
        self._run_command('create', username, password)

    def change_password(self, username, old_pwd, new_pwd):
        self._run_command('change-password', username, old_pwd, new_pwd)

    def attempt_to_login_with_credentials(self, username, password):
        self._run_command('login', username, password)

    def status_should_be(self, expected_status):
        if expected_status != self._status:
            raise AssertionError("Expected status to be '%s' but was '%s'."
                                 % (expected_status, self._status))

    def _run_command(self, command, *args):
        command = [sys.executable, self._sut_path, command] + list(args)
        process = subprocess.Popen(command, stdout=subprocess.PIPE,
                                   stderr=subprocess.STDOUT)
        self._status = process.communicate()[0].strip()
```









[官方文档](https://robotframework.org/robotframework/latest/RobotFrameworkUserGuide.html#introduction)

[中文](https://www.cnblogs.com/xyztank/category/2417650.html)