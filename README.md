# 科目一刷题（VSCode 扩展）

在 VSCode 左侧侧边栏随时刷驾照科目一题目：顺序练习、随机练习、全真模拟考试、错题本、收藏夹。零依赖，开箱即用。

## 功能

| 模式 | 说明 |
|------|------|
| 📖 顺序练习 | 按顺序过全库，进度自动保存，下次接着刷 |
| 🎲 随机练习 | 全库随机打乱 |
| 📝 模拟考试 | 100 题（判断 40 + 单选 60）、45 分钟倒计时、答题卡跳题，90 分及格；错题自动进错题本，交卷后可回顾解析 |
| ❌ 错题重练 | 练习/模拟中答错自动收录；重练答对即移出 |
| ⭐ 收藏夹 | 练习中点 ☆ 收藏，针对薄弱题反复刷 |

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
- 共 **1757 题**（判断 914 / 单选 843，已过滤真实考试不考的多选题），解析覆盖率 100%，71% 附法规原文；新增"💡 速记"口诀
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
├── data/bank.json            # 题库（唯一数据源）
├── data/images/              # 题目图片（本地化，离线可用）
└── scripts/                  # verify-bank 校验 / upgrade-bank 升级 / test-webview 测试
```
