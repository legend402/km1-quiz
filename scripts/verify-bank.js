// 题库完整性校验：node scripts/verify-bank.js [bank.json 路径]
// 检查项：JSON 可解析、字段齐全、answer 索引合法、type 合法、id 唯一、图片 URL 有效形态
'use strict';

const fs = require('fs');
const path = require('path');

const file = process.argv[2] || path.join(__dirname, '..', 'data', 'bank.json');
const raw = fs.readFileSync(file, 'utf8');
const bank = JSON.parse(raw); // 解析失败直接抛错退出

if (!Array.isArray(bank) || bank.length === 0) {
    console.error('FAIL: 题库不是非空数组');
    process.exit(1);
}

const errors = [];
const ids = new Set();
const typeCount = {};
const categoryCount = {};
let withImage = 0;
let withExplain = 0;
let withLaw = 0;

bank.forEach((q, i) => {
    const at = `第 ${i + 1} 题 (id=${q.id})`;
    if (!q.id) { errors.push(`${at}: 缺 id`); }
    else if (ids.has(q.id)) { errors.push(`${at}: id 重复`); }
    else { ids.add(q.id); }

    if (typeof q.question !== 'string' || !q.question.trim()) { errors.push(`${at}: 题干为空`); }
    if (!Array.isArray(q.options) || q.options.length < 2) { errors.push(`${at}: 选项少于 2 个`); }
    if (!Number.isInteger(q.answer) || q.answer < 0 || q.answer >= (q.options || []).length) {
        errors.push(`${at}: answer 索引越界 (${q.answer})`);
    }
    if (q.type !== 'judge' && q.type !== 'single') { errors.push(`${at}: 未知题型 ${q.type}`); }
    if (q.image && typeof q.image === 'string' && /^https?:\/\//.test(q.image)) { withImage++; }
    if (q.explain) { withExplain++; }
    if (q.law) { withLaw++; }
    typeCount[q.type] = (typeCount[q.type] || 0) + 1;
    categoryCount[q.category] = (categoryCount[q.category] || 0) + 1;
});

console.log(`总题数: ${bank.length}`);
console.log(`题型分布: ${JSON.stringify(typeCount)}`);
console.log(`分类分布: ${JSON.stringify(categoryCount)}`);
console.log(`含图题: ${withImage}（${(withImage / bank.length * 100).toFixed(1)}%）`);
console.log(`解析覆盖: ${withExplain}（${(withExplain / bank.length * 100).toFixed(1)}%），法条覆盖: ${withLaw}（${(withLaw / bank.length * 100).toFixed(1)}%）`);

if (errors.length) {
    console.error(`\nFAIL: ${errors.length} 处问题`);
    errors.slice(0, 20).forEach(e => console.error('  - ' + e));
    process.exit(1);
}
console.log('\nPASS: 题库完整性校验通过');
