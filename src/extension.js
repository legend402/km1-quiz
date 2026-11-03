'use strict';

/**
 * 科目一刷题 — 扩展入口
 * 职责单一：注册侧边栏 WebviewView、加载题库、转发持久化状态。
 * 判分/切题/倒计时等逻辑全部在 webview（media/main.js）内完成。
 */
const vscode = require('vscode');
const fs = require('fs');
const path = require('path');

const VIEW_ID = 'km1Quiz.view';
const STATE_KEY = 'km1Quiz.state';

/** 读取题库并裁剪为 webview 需要的最小字段集，减小 postMessage 体积 */
function loadBank(extensionPath) {
    const file = path.join(extensionPath, 'data', 'bank.json');
    const raw = JSON.parse(fs.readFileSync(file, 'utf8'));
    if (!Array.isArray(raw) || raw.length === 0) {
        throw new Error(`题库文件异常：${file}`);
    }
    return raw.map((q) => ({
        id: q.id,
        type: q.type,            // 'judge' 判断题 | 'single' 单选题
        question: q.question,
        image: q.image || null,  // 本地相对路径（questions/xxx.jpg）或 null
        options: q.options,
        answer: q.answer,        // 正确选项索引
        explain: q.explain || '',
        law: q.law || ''
    }));
}

/** 把题库中的相对图片路径转换为 webview 可访问的本地 URI */
function bankForWebview(webview, extensionUri, bank) {
    return bank.map((q) => {
        if (!q.image) { return q; }
        const uri = webview.asWebviewUri(
            vscode.Uri.joinPath(extensionUri, 'data', 'images', q.image)
        );
        return { ...q, image: uri.toString() };
    });
}

/** 生成 webview HTML 壳（nonce + CSP，只允许本地资源与 https 图片） */
function buildHtml(webview, extensionUri) {
    const nonce = Math.random().toString(36).slice(2) + Date.now().toString(36);
    const styleUri = webview.asWebviewUri(vscode.Uri.joinPath(extensionUri, 'media', 'style.css'));
    const scriptUri = webview.asWebviewUri(vscode.Uri.joinPath(extensionUri, 'media', 'main.js'));
    const csp = [
        "default-src 'none'",
        `img-src ${webview.cspSource} https: http: data:`, // 本地图片 + 兼容外链题库
        `style-src ${webview.cspSource}`,
        `script-src 'nonce-${nonce}'`,
        `font-src ${webview.cspSource}`
    ].join('; ');

    return `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta http-equiv="Content-Security-Policy" content="${csp}">
<meta name="referrer" content="no-referrer">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<link rel="stylesheet" href="${styleUri}">
<title>科目一刷题</title>
</head>
<body>
<div id="app"><div class="loading">题库加载中…</div></div>
<script nonce="${nonce}" src="${scriptUri}"></script>
</body>
</html>`;
}

function activate(context) {
    const questions = loadBank(context.extensionPath);

    const provider = {
        resolveWebviewView(view) {
            view.webview.options = {
                enableScripts: true,
                localResourceRoots: [
                    vscode.Uri.joinPath(context.extensionUri, 'media'),
                    vscode.Uri.joinPath(context.extensionUri, 'data', 'images')
                ]
            };
            view.webview.html = buildHtml(view.webview, context.extensionUri);

            view.webview.onDidReceiveMessage(async (msg) => {
                if (msg.type === 'ready') {
                    view.webview.postMessage({
                        type: 'init',
                        questions: bankForWebview(view.webview, context.extensionUri, questions),
                        state: context.globalState.get(STATE_KEY) || null
                    });
                } else if (msg.type === 'saveState') {
                    await context.globalState.update(STATE_KEY, msg.state);
                }
            }, undefined, context.subscriptions);
        }
    };

    context.subscriptions.push(
        vscode.window.registerWebviewViewProvider(
            VIEW_ID,
            provider,
            // 侧边栏隐藏时保留 webview 状态（模拟考倒计时切窗口不丢失）
            { webviewOptions: { retainContextWhenHidden: true } }
        )
    );
}

function deactivate() { /* 无需清理 */ }

module.exports = { activate, deactivate };
