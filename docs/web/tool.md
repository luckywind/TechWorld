# npm

**Npm 是随同 Nodejs 一起安装的 js 包管理工具。**



### 常用命令

- **安装包**2：使用`npm install <package_name>`安装指定包，若要安装到全局则使用`npm install -g <package_name>`。
- **初始化项目**：在项目目录中执行`npm init`，按照提示输入信息，生成`package.json`文件，用于记录项目的依赖关系和脚本等信息。也可以使用`npm init -y`直接生成默认的`package.json`文件。
- **更新包**：`npm update <package_name>`用于更新指定包到最新版本，`npm update`不加包名则更新所有依赖包。
- **卸载包**：`npm uninstall <package_name>`用于卸载指定包。
- **搜索包**2：`npm search <keyword>`可在公共注册表中搜索包含指定关键词的包。
- **查看已安装包**2：`npm ls`可查看当前项目或全局安装的所有包，`npm ls -g`用于查看全局安装的包。
- `npm cache clear`可以清空 Npm 本地缓存，用于对付使用相同版本号发布新版本代码的人
- `npm run` 用于执行脚本。在 `package.json` 文件中的 `scripts` 字段定义了命令
- 配置文件：使用 npm 来管理的 javascript 项目一般都有一个`package.json`文件。它定义了这个项目所依赖的各种包，以及项目的配置信息（比如名称、版本、依赖等元数据）。
  - 重要字段：
    - `name` - 包名。
    - `version` - 包的版本号。
    - `description` - 包的描述。
    - `homepage` - 包的官网 url 。
    - `author` - 包的作者姓名。
    - `contributors` - 包的其他贡献者姓名。
    - `dependencies` - 指定项目运行所依赖的模块。
    - `devDependencies` - 指定项目开发所依赖的模块。
    - `repository` - 包代码存放的地方的类型，可以是 git 或 svn，git 可在 Github 上。
    - `main` - main 字段是一个模块 ID，它是一个指向你程序的主要项目。就是说，如果你包的名字叫 express，然后用户安装它，然后 require("express")。
    - `keywords` - 关键字
    - `bin` - 用来指定各个内部命令对应的可执行文件的位置。
    - `scripts` - 指定了运行脚本命令的 npm 命令行缩写。