// webview 核心逻辑测试：node scripts/test-webview.js
// 用 vm 加载 media/main.js，mock DOM / acquireVsCodeApi，模拟点击与消息，断言状态流转。
'use strict';

const vm = require('vm');
const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const code = fs.readFileSync(path.join(root, 'media', 'main.js'), 'utf8');
const bank = JSON.parse(fs.readFileSync(path.join(root, 'data', 'bank.json'), 'utf8'));

let passed = 0;
function ok(cond, name) {
    if (cond) { passed++; console.log('  ✓ ' + name); }
    else { console.error('  ✗ FAIL: ' + name); process.exitCode = 1; }
}

// ---------- mock 环境 ----------
const messages = [];        // webview → 扩展 消息记录
const listeners = { document: {}, window: {} };
const pendingTimers = [];

function makeEl() {
    return {
        html: '',
        set innerHTML(v) { this.html = v; },
        get innerHTML() { return this.html; },
        querySelector() { return null; },
        focus() { }
    };
}
const appEl = makeEl();

const sandbox = {
    console,
    setInterval: () => 1,
    clearInterval: () => { },
    setTimeout: (fn) => { pendingTimers.push(fn); return pendingTimers.length; },
    clearTimeout: () => { },
    document: {
        getElementById: () => appEl,
        addEventListener: (t, h) => { listeners.document[t] = h; },
        createElement: () => makeEl()
    },
    window: {
        addEventListener: (t, h) => { listeners.window[t] = h; }
    },
    acquireVsCodeApi: () => ({ postMessage: (m) => messages.push(m) })
};
vm.createContext(sandbox);
vm.runInContext(code, sandbox, { filename: 'main.js' });

function init(state) {
    listeners.window.message({ data: { type: 'init', questions: bank, state: state || null } });
}
function click(action) {
    listeners.document.click({ target: { closest: () => ({ dataset: { action } }) } });
}
function flush() { // 执行防抖的 persist
    while (pendingTimers.length) { pendingTimers.shift()(); }
}
function lastSavedState() {
    for (let i = messages.length - 1; i >= 0; i--) {
        if (messages[i].type === 'saveState') { return messages[i].state; }
    }
    return null;
}

console.log('— 启动与首页 —');
ok(messages.some((m) => m.type === 'ready'), 'webview 发送 ready');
init(null);
ok(appEl.html.includes('顺序练习') && appEl.html.includes('模拟考试'), 'init 后渲染首页菜单');

console.log('— 顺序练习：答对 —');
const q1 = bank[0];
click('mode:sequential');
ok(appEl.html.includes('第 1 / ' + bank.length + ' 题'), '进入顺序练习显示第 1 题');
click('pick:' + q1.answer);
ok(appEl.html.includes('回答正确'), '答对显示"回答正确"');
ok(appEl.html.includes(q1.explain ? q1.explain.slice(0, 6) : '单选题'), '显示解析');
flush();
let s = lastSavedState();
ok(s && s.answers[q1.id] && s.answers[q1.id].correct === true, 'answers 记录答对');
ok(!s.wrong.includes(q1.id), '答对不进错题本');

console.log('— 顺序练习：答错 —');
click('next');
const q2 = bank[1];
const wrongPick = (q2.answer + 1) % q2.options.length;
click('pick:' + wrongPick);
ok(appEl.html.includes('回答错误'), '答错显示"回答错误"');
flush();
s = lastSavedState();
ok(s.wrong.includes(q2.id), '答错自动进错题本');
ok(s.sequentialIndex === 1, '顺序练习进度持久化');

console.log('— 收藏 —');
click('toggleStar');
flush();
s = lastSavedState();
ok(s.starred.includes(q2.id), 'toggleStar 收藏当前题');
click('toggleStar');
flush();
s = lastSavedState();
ok(!s.starred.includes(q2.id), '再次 toggleStar 取消收藏');

console.log('— 错题重练：答对移出 —');
click('exit');
click('mode:wrong');
ok(appEl.html.includes('错题重练'), '进入错题重练');
click('pick:' + q2.answer); // 队列里唯一错题答对
ok(appEl.html.includes('错题本已清空'), '答对后错题清空提示');
flush();
s = lastSavedState();
ok(!s.wrong.includes(q2.id), '答对从错题本移除');

console.log('— 空错题本 —');
click('exit');
click('mode:wrong');
ok(appEl.html.includes('错题本是空的'), '空错题本友好提示');
click('exit');

console.log('— 模拟考试 —');
click('mode:exam');
const cellCount = (appEl.html.match(/sheet-cell/g) || []).length;
ok(cellCount === 100, '答题卡 100 格，实际 ' + cellCount);
ok(/第 1 \/ 100 题/.test(appEl.html), '考试卷 100 题');
const paperFirst = null; // 无法直接取 paper，通过渲染交互验证
click('examPick:0');
ok(/option picked/.test(appEl.html), '选项高亮 picked');
click('goto:5');
ok(/第 6 \/ 100 题/.test(appEl.html), '答题卡跳转到第 6 题');
click('submit');
ok(appEl.html.includes('确认交卷'), '有未答题时提示确认');
click('submitForce');
ok(/及格线 90/.test(appEl.html), '交卷后显示成绩结果页');
flush();
s = lastSavedState();
ok(s.examHistory.length === 1 && s.examHistory[0].total === 100, '模拟考成绩入历史');
const wrongInPaper = s.examHistory[0].score < 100;
ok(!wrongInPaper || s.wrong.length > 0, '考试错题并入错题本');
click('exit');
ok(appEl.html.includes('学习进度'), '退出回首页');

console.log('— 状态恢复（重启模拟）—');
init(JSON.parse(JSON.stringify(s)));
ok(appEl.html.includes('最近模拟'), '重载后恢复状态渲染首页');
click('mode:sequential');
ok(new RegExp('第 ' + (s.sequentialIndex + 1) + ' / ' + bank.length + ' 题').test(appEl.html), '顺序练习恢复到上次进度');

console.log('\n' + (process.exitCode ? '存在失败断言' : '全部 ' + passed + ' 项断言通过'));
