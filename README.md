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
npx @vscode/vsce package        # 生成 km1-quiz-0.1.0.vsix
code --install-extension km1-quiz-0.1.0.vsix
```

或直接用开发模式运行：

```bash
code --extensionDevelopmentPath <本项目路径>
```

## 题库

- 来源：开源项目 [AgentMystia/driving-test-subject1](https://github.com/AgentMystia/driving-test-subject1) 的 `data/bank.json`（2115 题，判断 1073 / 单选 1042，含解析），其上游 [doupoa/DrivingTestSubjectOne](https://github.com/doupoa/DrivingTestSubjectOne) 为 MIT 许可的 2022-07 题库快照
- 题库为社区整理快照，**未包含 2025 年新增考点**，答案以官方最新法规为准，仅供学习备考参考
- 约 1/3 题目带图，图片从 CDN 在线加载，离线时显示占位提示（题干文字仍可作答）
- 题库文件为 `data/bank.json`，字段：`id / type(judge|single) / question / image / options / answer(索引) / explain / law`。如需更换题库，保持此格式替换该文件即可

## 校验题库

```bash
npm run verify    # 等价于 node scripts/verify-bank.js
```

## 项目结构

```
km1-quiz/
├── src/extension.js        # 扩展入口：注册侧边栏 webview、下发题库、持久化状态
├── media/main.js           # webview 应用（练习/考试/错题/收藏逻辑，原生 JS）
├── media/style.css         # webview 样式（VSCode CSS 变量主题适配）
├── media/icon.svg          # 活动栏图标
├── data/bank.json          # 题库（唯一数据源）
└── scripts/verify-bank.js  # 题库完整性校验脚本
```
