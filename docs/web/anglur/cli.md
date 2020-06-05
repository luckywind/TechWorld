Angular CLI 是一个命令行界面工具，可用于初始化、开发、构建和维护 Angular 应用

安装cli

```shell
npm install -g @angular/cli
```

基本工作流

```shell
ng help
ng generate --help
ng new my-first-project
cd my-first-project
ng serve          #会自动重新构建项目
```

工作区和项目文件

ng new 命令会创建一个 Angular 工作区目录，并生成一个新的应用骨架,创建的初始应用位于工作区顶层。

每个工作区中可以包含多个应用和库，在工作区中生成的其它应用或库，会放在 `projects/` 子目录下。

```shell
  add Adds support for an external library to your project.
  analytics Configures the gathering of Angular CLI usage metrics. See https://v8.angular.io/cli/usage-analytics-gathering.
  build (b) Compiles an Angular app into an output directory named dist/ at the given output path. Must be executed from within a workspace directory.
  deploy Invokes the deploy builder for a specified project or for the default project in the workspace.
  config Retrieves or sets Angular configuration values in the angular.json file for the workspace.
  doc (d) Opens the official Angular documentation (angular.io) in a browser, and searches for a given keyword.
  e2e (e) Builds and serves an Angular app, then runs end-to-end tests using Protractor.
  generate (g) Generates and/or modifies files based on a schematic.
  help Lists available commands and their short descriptions.
  lint (l) Runs linting tools on Angular app code in a given project folder.
  new (n) Creates a new workspace and an initial Angular app.
  run Runs an Architect target with an optional custom builder configuration defined in your project.
  serve (s) Builds and serves your app, rebuilding on file changes.
  test (t) Runs unit tests in a project.
  update Updates your application and its dependencies. See https://update.angular.io/
  version (v) Outputs Angular CLI version.
  xi18n (i18n-extract) Extracts i18n messages from source code.
```

