# My GH Notebook：需求与实施记录

## 1. 项目背景

当前博客通过 `new.bat` / `new.sh` 创建 Markdown 文件，编写完成后执行 Git 操作并推送到 GitHub。为了方便在 Android 手机上写作和发布，创建一个个人使用的 Markdown 发布工具。

项目目录：`C:\Users\zen82746.LI\Desktop\tmp777\myGhNb`

博客仓库：`kzeng/kzeng.github.io`

发布分支：`clash`

## 2. 已确认需求

### 2.1 文章

- 支持新建 Markdown 文章。
- 支持 Markdown 编辑。
- 第一版只支持 Markdown，不做富文本编辑器。
- 支持 Markdown 预览和文章详情浏览。
- 文章目录保持博客仓库现有结构：`content/posts`。
- 文件名沿用：`pYYYY-MM-DD-NNN.md`。
- 同一天的文章编号自动递增，例如 `p2026-09-18-001.md`。
- Front Matter 沿用现有格式：`title`、`date`、`draft`、`tags`。
- 同名文章发布时允许覆盖远程文件。

### 2.2 草稿与离线

- 支持离线写作。
- 自动或手动保存本地草稿。
- 本地草稿不应被远程文章同步覆盖。
- 无网络时可以继续编辑和浏览已缓存内容。

### 2.3 GitHub

- 拉取 `kzeng/kzeng.github.io` 仓库 `clash` 分支的最新文章列表。
- 对应的等价流程是读取 GitHub API，而不是在手机内执行完整的 `git pull --rebase`。
- 文章列表支持刷新、缓存和点击查看详情。
- 发布时把 Markdown 文件提交到：`clash/content/posts/`。
- 新文章使用创建操作；同名文章使用覆盖操作。
- 发布时生成 Git commit。
- GitHub Token 只能由用户在 App 内配置，不能写入源码。

### 2.4 图片

- 图片使用 Markdown 格式插入。
- 继续使用 `imgchr.com` 作为图床。
- App 点击“插入图片”后打开 imgchr 网页。
- 用户在网页完成图片上传并复制图片 URL 或 Markdown。
- 返回 App 后将链接粘贴到 Markdown 编辑区。
- 不在 App 中保存或硬编码图床账号密码。

现阶段采用网页上传方案，是因为 imgchr 官方页面说明其公共 API 已关闭，不能把非公开网页请求当作稳定接口使用。

### 2.5 UI 与品牌

- 使用 Google Material 3 风格。
- 支持浅色和深色主题。
- 默认跟随系统主题，并允许用户手动切换。
- 设计简洁、大气的产品 Logo。
- Logo 包含 GitHub 和 Notebook 的视觉信息。
- 提供 Android 开屏 Logo。

### 2.6 About 页面

- Author：`Zengkai001@gmail.com`
- Version：`0.0.1`

## 3. 页面规划

### 首页

- 展示本地草稿和已同步文章。
- 显示标题、日期和文件名。
- 支持刷新 GitHub 文章列表。
- 点击文章进入详情。
- 点击右下角按钮新建文章。
- 顶部提供明暗主题切换和 About 入口。

### 编辑页

- 标题输入框。
- Markdown 正文编辑区。
- 打开 imgchr 按钮。
- 粘贴图片链接按钮。
- 保存草稿按钮。
- 发布按钮。

### 详情页

- 显示文章标题、日期和 Markdown 正文。
- 后续可替换为完整 Markdown 渲染器。

### About 页

- 展示产品 Logo。
- 展示作者和版本信息。
- 提供 GitHub Fine-grained Token 配置入口。
- Token 仅保存到 Android 本地存储。

## 4. 已实施内容

当前项目已创建以下内容：

- Kotlin + Jetpack Compose 项目骨架。
- Material 3 依赖和主题结构。
- 浅色/深色主题切换，并持久化主题选择。
- GitHub + Notebook Vector Drawable Logo。
- Android 12 Splash Logo 配置。
- 首页、编辑页、详情页和 About 页。
- SharedPreferences 本地草稿保存。
- GitHub 文章列表读取骨架。
- GitHub Markdown 创建/覆盖提交骨架。
- GitHub Token 本地配置入口。
- imgchr 网页打开入口。
- Markdown 图片链接粘贴入口。
- `.gitignore`。
- 项目 README。

## 5. 计划中的实施步骤

### 阶段一：项目基础

1. 配置 Android 项目、包名、版本号和 Material 3。
2. 完成产品 Logo、Launcher Icon 和 Splash Logo。
3. 建立浅色/深色主题。
4. 创建首页、编辑页、详情页和 About 页。

### 阶段二：本地写作

1. 实现文章标题、日期、文件名和 Front Matter 生成。
2. 实现本地草稿保存和恢复。
3. 实现离线草稿列表。
4. 增加 Markdown 预览和常用编辑辅助操作。

### 阶段三：GitHub 同步与发布

1. 配置 GitHub Fine-grained Token。
2. 读取 `clash/content/posts` 文章列表。
3. 读取远程 Markdown 内容并缓存。
4. 根据文件是否存在决定创建或覆盖。
5. 发布前读取远程 SHA，避免覆盖过期版本。
6. 通过 GitHub Contents API 提交 commit 到 `clash` 分支。
7. 显示提交成功或失败状态。

### 阶段四：图片流程

1. 打开 imgchr 网页。
2. 用户上传图片并复制 URL 或 Markdown。
3. 返回 App 后粘贴到 Markdown 编辑器。
4. 后续可增加返回 App 时自动检测剪贴板的辅助功能。

### 阶段五：验证与发布

1. 在 Android Studio/JDK 17/Android SDK 环境构建 Debug APK。
2. 在真实 Android 手机上验证离线编辑。
3. 验证 GitHub 文章列表刷新。
4. 验证新文章创建。
5. 验证同名文章覆盖。
6. 验证 imgchr 图片链接插入。
7. 验证浅色、深色主题和开屏 Logo。
8. 验证 GitHub Actions 构建完成后的网页显示。

## 6. 当前状态与已知限制

- 项目文件已经创建在指定目录。
- 已使用本机 Android SDK 35、JDK 17 和 Gradle 8.8 成功构建 Debug APK。
- APK 输出：`app/build/outputs/apk/debug/app-debug.apk`。
- 包名：`com.kzeng.myghnb`；版本：`0.0.1`。
- 当前详情页可以显示 Markdown 原文，完整 Markdown 渲染仍需补充。
- 当前图片流程提供打开 imgchr 和粘贴链接入口，返回 App 后自动识别剪贴板仍可增强。
- GitHub 发布需要用户在 About 页面配置有效的 Fine-grained Token。
- 未保存或记录任何图床账号密码。

构建命令：

```powershell
$env:ANDROID_SDK_ROOT = "C:\Work\Apps\android-sdk"
$env:JAVA_HOME = "C:\Work\Apps\openjdk-17.0.0.1+2_windows-x64_bin\jdk-17.0.0.1"
& "C:\Work\Apps\gradle-8.8-bin\gradle-8.8\bin\gradle.bat" :app:assembleDebug
```

## 7. Latest implementation update

- The editor now provides Code, WYSIWYG, and Preview modes.
- WYSIWYG mode supports basic headings, bold, italic, and images, then converts the result back to Markdown when saving or publishing.
- The editor includes a dedicated Preview article button.
- GitHub sync is executed through the IO dispatcher to avoid Android main-thread network failures.
- GitHub requests now trim the token, send a User-Agent, and handle HTTP errors more safely.
- The user-provided GitHub Notebook image is used for the app icon, About page logo, and Android 12 splash logo.
- The latest Debug APK was built successfully with Gradle 8.8, AGP 8.6.1, JDK 17, and compileSdk 35.

## 8. 安全约束

- 不在源码、README、日志或 Git 历史中保存密码。
- 不把 GitHub Token 写入源码或提交到仓库。
- 优先使用权限限制到单个仓库的 Fine-grained Token。
- 图床网页登录由用户在网页中完成。
- App 只处理用户主动复制回来的图片 URL 或 Markdown。

## 9. DeepSeek AI 写作

- 编辑器工具栏增加 AI 星光图标。
- 点击后打开 Material 3 底部浮动面板，支持文本输入和 Android 语音输入。
- 当前文章标题、标签和 Markdown 正文作为上下文发送给 DeepSeek。
- 使用 DeepSeek Chat Completions 流式接口，要求返回 Markdown。
- 生成结果先在面板中预览，用户确认后追加到正文末尾，不覆盖原文。
- DeepSeek Token 在 About 页面配置，使用 Android Keystore 加密保存。
- AI 服务错误、Token 缺失、语音权限和识别不可用时显示可恢复提示。
