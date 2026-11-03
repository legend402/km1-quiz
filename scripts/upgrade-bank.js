// 题库升级脚本：从 xfhxly/KeMuONEXueKao 的 questions.json 生成 data/bank.json 并本地化图片
// 用法：node scripts/upgrade-bank.js <源仓库路径>     （源仓库须含 src/data/questions.json 与 public/images/）
// 输出：data/bank.json（数组，字段 id/type/question/image/options/answer/explain/law/category）
//       data/images/questions/*.{jpg,png}
'use strict';

const fs = require('fs');
const path = require('path');

const srcRepo = process.argv[2];
if (!srcRepo) {
    console.error('用法: node scripts/upgrade-bank.js <KeMuONEXueKao 仓库路径>');
    process.exit(1);
}

const root = path.join(__dirname, '..');
const srcJson = path.join(srcRepo, 'src', 'data', 'questions.json');
const srcImgDir = path.join(srcRepo, 'public', 'images');
const outJson = path.join(root, 'data', 'bank.json');
const outImgDir = path.join(root, 'data', 'images');

const SEP = '<br/><br/>相关法规参考：';
const raw = JSON.parse(fs.readFileSync(srcJson, 'utf8'));

// 科一 + 判断/单选（科一真实考试无多选）
const ke1 = raw.filter((q) => q.subject === 'ke1' && (q.type === 'judge' || q.type === 'single'));
console.log(`科一判断+单选: ${ke1.length} 题（源库共 ${raw.length} 题）`);

fs.mkdirSync(path.join(outImgDir, 'questions'), { recursive: true });

let copied = 0, missed = 0;
const bank = ke1.map((q) => {
    // 解析拆分：主体解析 vs 法条原文
    let explain = q.analysis || '';
    let law = '';
    const idx = explain.indexOf(SEP);
    if (idx >= 0) {
        law = explain.slice(idx + SEP.length).trim();
        explain = explain.slice(0, idx).trim();
    }
    // 速记口诀追加到解析尾部（不与解析重复时）
    const mnemonic = (q.mnemonic || '').trim();
    if (mnemonic && !explain.includes(mnemonic)) {
        explain += '\n💡 速记：' + mnemonic;
    }

    // 图片本地化
    let image = '';
    if (q.image) {
        const src = path.join(srcImgDir, q.image);
        const dst = path.join(outImgDir, q.image);
        try {
            fs.copyFileSync(src, dst);
            image = q.image; // 存相对路径 questions/xxx.jpg
            copied++;
        } catch (e) {
            missed++;
            console.warn(`  图片缺失: ${q.image}`);
        }
    }

    return {
        id: 'k1-' + q.qid,
        type: q.type,
        question: q.question,
        image: image,
        options: q.options,
        answer: q.answer,
        explain: explain,
        law: law,
        category: q.chapter_name || ''
    };
});

fs.writeFileSync(outJson, JSON.stringify(bank), 'utf8');
console.log(`图片本地化: 复制 ${copied} 张${missed ? `，缺失 ${missed} 张` : ''}`);
console.log(`写出: ${outJson}（${(fs.statSync(outJson).size / 1048576).toFixed(2)} MB）`);
