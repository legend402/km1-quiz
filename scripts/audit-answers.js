// 单选题多答案/口径冲突审计：node scripts/audit-answers.js
// 检测：
//  [严重] 同题干同选项集但答案不同 —— 题库内部口径冲突
//  [严重] 错误选项与正确选项文本相同（归一化后）—— 明确的多答案 bug
//  [警告] 同题干但答案文本不同（选项不同）—— 需人工判断口径
//  [提示] 正确答案文本是其他选项的子串 —— 弱信号，供人工复核
'use strict';

const fs = require('fs');
const path = require('path');

const file = path.join(__dirname, '..', 'data', 'bank.json');
const bank = JSON.parse(fs.readFileSync(file, 'utf8'));

const norm = (s) => String(s).toLowerCase()
    .replace(/[\s，。、；：""''？！（）【】《》,.;:?!()"'[\]{}<>{}\-—_·~～]/g, '');

const answerText = (q) => norm(q.options[q.answer]);

let severe = 0, warn = 0, hint = 0;

function show(q, tag, extra) {
    console.log(`\n[${tag}] ${q.id} (${q.type})`);
    console.log('  题干: ' + q.question);
    q.options.forEach((o, i) => {
        console.log(`    ${'ABCD'[i]}${i === q.answer ? '(✓正确)' : '      '}: ${o}`);
    });
    if (extra) { console.log('  说明: ' + extra); }
}

// ---- 1. 同题干+同图分组，检测答案冲突 ----
// 注：图片题题干常相同（"这个标志是何含义？"），配图不同答案自然不同，属正常题组；
//     分组键纳入 image 后仍然冲突的才是真问题。
const groups = new Map();
for (const q of bank) {
    const k = norm(q.question) + '||' + (q.image || '');
    if (!groups.has(k)) { groups.set(k, []); }
    groups.get(k).push(q);
}
for (const [, qs] of groups) {
    if (qs.length < 2) { continue; }
    const answers = new Set(qs.map(answerText));
    const sameOptions = qs.every((q) =>
        norm(q.options.join('|')) === norm(qs[0].options.join('|')));
    if (answers.size > 1) {
        if (sameOptions) {
            severe++;
            console.log('\n===== [严重] 同题干同选项、答案不同 =====');
            qs.forEach((q) => show(q, '冲突题', '本题正确答案: ' + q.options[q.answer]));
        } else {
            warn++;
            console.log('\n===== [警告] 同题干、选项不同、答案文本不同（需人工判断） =====');
            qs.forEach((q) => show(q, '疑似', '本题正确答案: ' + q.options[q.answer]));
        }
    } else if (!sameOptions) {
        // 题干相同、选项顺序不同但答案文本一致：正常重复，仅统计
    }
}

// ---- 2. 错误选项与正确选项文本相同 ----
for (const q of bank) {
    const at = answerText(q);
    for (let i = 0; i < q.options.length; i++) {
        if (i !== q.answer && norm(q.options[i]) === at) {
            severe++;
            console.log('\n===== [严重] 错误选项与正确选项文本相同 =====');
            show(q, '多答案', `选项 ${'ABCD'[i]} 与正确选项 ${'ABCD'[q.answer]} 归一化后相同`);
        }
    }
}

// ---- 3. 正确答案是其他选项的子串（弱信号，只列单选题） ----
const hints = [];
for (const q of bank) {
    if (q.type !== 'single') { continue; }
    const at = answerText(q);
    for (let i = 0; i < q.options.length; i++) {
        if (i === q.answer) { continue; }
        const ot = norm(q.options[i]);
        // 长度防误报：正确答案至少 4 字且是另一选项子串
        if (at.length >= 4 && ot.includes(at)) {
            hints.push({ q, i });
            break; // 每题只记一次
        }
    }
}
if (hints.length) {
    console.log('\n===== [提示] 正确答案文本是其他选项的子串（人工复核是否歧义） =====');
    for (const { q, i } of hints) {
        hint++;
        show(q, '子串', `正确选项内容包含于选项 ${'ABCD'[i]} 中`);
    }
}

console.log(`\n========== 汇总 ==========`);
console.log(`总题数 ${bank.length}；严重问题 ${severe}，口径警告 ${warn}，子串提示 ${hint}`);
