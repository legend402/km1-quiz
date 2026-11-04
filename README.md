# 科目一刷题（VSCode 扩展 + 安卓 App）

在 VSCode 左侧侧边栏随时刷驾照科目一题目：顺序练习、随机练习、全真模拟考试、错题本、收藏夹。零依赖，开箱即用。

> 📱 **安卓版**：原生 Jetpack Compose 重写，像素风 UI + 全程灵动动画，题库图片全离线。详见下方[安卓 App](#安卓appcompose--像素风) 章节。

## 功能

| 模式 | 说明 |
|------|------|
| 顺序练习 | 按顺序过全库，进度自动保存，下次接着刷 |
| 随机练习 | 全库随机打乱 |
| 模拟考试 | 100 题（判断 40 + 单选 60）、45 分钟倒计时、答题卡跳题，90 分及格；错题自动进错题本，交卷后可回顾解析 |
| 错题重练 | 练习/模拟中答错自动收录；重练答对即移出 |
| 收藏夹 | 练习中点星标收藏，针对薄弱题反复刷 |

- 练习中即时判对错并显示解析与法条依据
- 键盘操作：`1-4` 选答案，`←` `→` 切题
- 进度、错题、收藏、模拟考成绩（最近 20 次）持久化保存
- 深浅主题跟随 VSCode

## 使用

### 方式一：调试运行（开发）

用 VSCode 打开本项目，按 `F5` 启动扩展开发宿主，点击左侧活动栏的方向盘图标 🛞。

### 方式二：打包安装（日常使用）

```bash
npx @vscode/vsce package --no-dependencies --allow-missing-repository
# 生成 km1-quiz-0.1.0.vsix
code --install-extension km1-quiz-0.1.0.vsix
```

> 本扩展零 npm 依赖，`--no-dependencies` 可跳过依赖检测（某些 npm 源环境下 vsce 默认依赖检测会静默失败导致打包清单为空）。

或直接用开发模式运行：

```bash
code --extensionDevelopmentPath <本项目路径>
```

## 题库

- 来源：开源项目 [xfhxly/KeMuONEXueKao](https://github.com/xfhxly/KeMuONEXueKao)（2026-07 从驾考宝典导入，符合公安部 162 号令记分规则），经 `scripts/upgrade-bank.js` 转换为本科目一插件格式
- 共 **1757 题**（判断 914 / 单选 843，已过滤真实考试不考的多选题），解析覆盖率 100%，71% 附法规原文；新增"【速记】"口诀
- **981 张题目图片已本地化**（`data/images/`，约 39MB），完全离线可用，不依赖任何 CDN
- 题库由社区从公开渠道整理，归原权利人所有，仅供学习备考参考，答案以官方最新法规为准
- 题库文件为 `data/bank.json`，字段：`id / type(judge|single) / question / image(相对路径) / options / answer(索引) / explain / law / category`。更换题库：`node scripts/upgrade-bank.js <KeMuONEXueKao 仓库路径>` 或按此格式替换该文件

## 校验题库

```bash
npm run verify    # 等价于 node scripts/verify-bank.js
```

## 项目结构

```
km1-quiz/
├── src/extension.js          # 扩展入口：注册侧边栏 webview、下发题库、持久化状态
├── media/main.js             # webview 应用（练习/考试/错题/收藏逻辑，原生 JS）
├── media/style.css           # webview 样式（VSCode CSS 变量主题适配）
├── media/icon.svg            # 活动栏图标
├── data/bank.json            # 题库（唯一数据源，VSCode 扩展与安卓 App 共用）
├── data/images/              # 题目图片（本地化，离线可用）
├── scripts/                  # verify-bank 校验 / upgrade-bank 升级 / test-webview 测试
└── android/                  # 安卓 App（Compose 原生，构建期从 data/ 复制题库）
```

## 安卓 App（Compose + 像素风）

原生 Jetpack Compose 重写，业务逻辑与 VSCode 扩展版逐条对齐（同一套单测断言口径），UI 为像素复古风：

- **像素设计系统**：Fusion Pixel 像素字体（OFL 开源）、硬边框 + 硬阴影卡片、分格块状进度条、网格字符串像素图标、深浅双主题跟随系统
- **全程动画**：屏幕方向性滑动转场、首页统计数字跳动 + 卡片依次弹入、答对✓弹出/答错抖动、解析展开、答题卡弹跳、倒计时脉冲、成绩分数累加 + 及格像素五彩纸屑 + PASS 印章、像素小汽车吉祥物
- **触屏适配**：答题触感震动（答对/答错两种反馈）、8-bit 音效（首页可静音）、考试中屏幕常亮、返回键退出考试二次确认
- **完全离线**：题库与 875 张图片全部打包进 APK，无任何权限

### 构建

要求：JDK 17+、Android SDK（platform 36）。国内网络已配好华为云 Gradle 镜像与阿里云 Maven 镜像。

```bash
cd android
gradlew.bat assembleRelease          # Windows（首次会自动下载 Gradle 发行版）
# 产物：app/build/outputs/apk/release/app-release.apk（约 45MB，debug 签名，可直接安装）
gradlew.bat :app:testDebugUnitTest   # 运行 17 个单元测试
```

安装到手机：将 APK 传到手机后点击安装（需允许"未知来源"），或 `adb install -r app-release.apk`。

更换图标/字体后重新生成：`node android/scripts/gen-icon.js`（图标，零依赖）。

### 工程说明

```
android/
├── app/src/main/java/com/km1quiz/app/
│   ├── data/       # Bank（题库解析）、Prefs（SharedPreferences 持久化，字段与扩展版同构）
│   ├── logic/      # QuizEngine：纯 Kotlin 业务逻辑（可 JVM 单测，语义对齐 media/main.js）
│   └── ui/         # ViewModel、AppRoot 路由、theme 像素主题、pixel 组件库、五屏 UI
├── app/src/test/   # QuizLogicTest + BankTest（真实题库全量校验）
├── scripts/gen-icon.js   # 像素方向盘启动图标生成器（手写 PNG 编码器）
└── FONT_LICENSE_OFL.txt  # Fusion Pixel 字体许可
```

- 题库不重复存放：Gradle 任务 `copyQuizData` 在构建期从仓库 `data/` 复制进 assets
- release 用 debug 签名（个人安装即插即用）；上架商店请替换为自己的 keystore
