/* global acquireVsCodeApi */
(function () {
    'use strict';

    /** 科目一刷题 — webview 应用（原生 JS，无框架）
     * 视图：home 首页 / practice 练习 / exam 模拟考试 / result 考试结果
     * 判分、切题、倒计时全部在本地完成；扩展宿主仅负责题库下发与状态持久化。
     */
    const vscode = acquireVsCodeApi();

    const LABELS = ['A', 'B', 'C', 'D'];
    const EXAM_TOTAL = 100;        // 模拟考题数：判断 40 + 单选 60
    const EXAM_JUDGE = 40;
    const EXAM_MINUTES = 45;       // 考试时长（分钟）
    const PASS_SCORE = 90;         // 及格线
    const HISTORY_LIMIT = 20;      // 保留模拟考历史条数

    const MODE_NAMES = {
        sequential: '顺序练习',
        random: '随机练习',
        wrong: '错题重练',
        starred: '收藏夹'
    };

    // ---------- 全局状态 ----------
    let questions = [];            // 全量题库 [{id,type,question,image,options,answer,explain,law}]
    let byId = new Map();
    let state = defaultState();    // 持久化状态（进度/错题/收藏/成绩）
    let view = { name: 'home' };   // 当前视图
    let session = null;            // 当前练习/考试会话
    let timerId = null;

    function defaultState() {
        return { answers: {}, wrong: [], starred: [], sequentialIndex: 0, examHistory: [] };
    }

    /** 合并默认值并做类型防御；自动清掉题库更换后失效的旧题目记录 */
    function normalizeState(s) {
        const d = defaultState();
        if (!s || typeof s !== 'object') { return d; }
        const answers = {};
        if (s.answers && typeof s.answers === 'object') {
            for (const id of Object.keys(s.answers)) {
                if (isValidId(id)) { answers[id] = s.answers[id]; }
            }
        }
        return {
            answers: answers,
            wrong: Array.isArray(s.wrong) ? s.wrong.filter(isValidId) : [],
            starred: Array.isArray(s.starred) ? s.starred.filter(isValidId) : [],
            sequentialIndex: Number.isInteger(s.sequentialIndex) && s.sequentialIndex >= 0 ? s.sequentialIndex : 0,
            examHistory: Array.isArray(s.examHistory) ? s.examHistory.slice(0, HISTORY_LIMIT) : []
        };
    }

    function isValidId(id) {
        return typeof id === 'string' && byId.has(id);
    }

    // ---------- 持久化（防抖） ----------
    let saveTimer = null;
    function persist() {
        clearTimeout(saveTimer);
        saveTimer = setTimeout(() => vscode.postMessage({ type: 'saveState', state }), 500);
    }

    // ---------- 工具 ----------
    function esc(s) {
        return String(s == null ? '' : s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function shuffle(arr) {
        const a = arr.slice();
        for (let i = a.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [a[i], a[j]] = [a[j], a[i]];
        }
        return a;
    }

    function fmtTime(sec) {
        const m = Math.floor(sec / 60);
        const s = sec % 60;
        return m + ':' + String(s).padStart(2, '0');
    }

    function isStarred(id) { return state.starred.indexOf(id) >= 0; }

    // ---------- 视图切换 ----------
    function goHome() {
        stopTimer();
        session = null;
        view = { name: 'home' };
        render();
    }

    function startPractice(mode) {
        let queue = [];
        let index = 0;
        if (mode === 'sequential') {
            queue = questions.map((q) => q.id);
            index = Math.min(state.sequentialIndex, Math.max(queue.length - 1, 0));
        } else if (mode === 'random') {
            queue = shuffle(questions.map((q) => q.id));
        } else if (mode === 'wrong') {
            queue = state.wrong.slice();
        } else if (mode === 'starred') {
            queue = state.starred.slice();
        }
        if (queue.length === 0) {
            view = { name: 'empty', mode };
            render();
            return;
        }
        session = { kind: 'practice', mode, queue, index };
        view = { name: 'practice' };
        render();
    }

    function startExam() {
        const judges = shuffle(questions.filter((q) => q.type === 'judge')).slice(0, EXAM_JUDGE);
        const singles = shuffle(questions.filter((q) => q.type === 'single')).slice(0, EXAM_TOTAL - EXAM_JUDGE);
        const paper = shuffle(judges.concat(singles));
        if (paper.length < EXAM_TOTAL) {
            view = { name: 'empty', mode: 'exam' };
            render();
            return;
        }
        session = {
            kind: 'exam',
            paper,
            picked: new Array(paper.length).fill(-1),
            index: 0,
            deadline: Date.now() + EXAM_MINUTES * 60 * 1000,
            confirming: false
        };
        view = { name: 'exam' };
        startTimer();
        render();
    }

    function startTimer() {
        stopTimer();
        timerId = setInterval(() => {
            const el = document.getElementById('examTime');
            const remain = remainSeconds();
            if (el) { el.textContent = fmtTime(Math.max(remain, 0)); }
            if (remain <= 0) { submitExam(true); }
        }, 1000);
    }

    function stopTimer() {
        if (timerId) { clearInterval(timerId); timerId = null; }
    }

    function remainSeconds() {
        return Math.ceil((session.deadline - Date.now()) / 1000);
    }

    // ---------- 练习逻辑 ----------
    function handlePracticePick(pickedIdx) {
        const id = session.queue[session.index];
        const q = byId.get(id);
        const correct = pickedIdx === q.answer;
        state.answers[id] = { picked: pickedIdx, correct };

        if (!correct) {
            if (state.wrong.indexOf(id) < 0) { state.wrong.push(id); }
        } else {
            state.wrong = state.wrong.filter((x) => x !== id);
            if (session.mode === 'wrong') {
                const qi = session.queue.indexOf(id);
                if (qi >= 0) { session.queue.splice(qi, 1); }
                // splice 后 index 天然指向下一题，无需自增
            }
        }
        persist();
        render();
    }

    function practiceStep(delta) {
        session.index += delta;
        if (session.mode === 'sequential') {
            state.sequentialIndex = session.index;
            persist();
        }
        if (session.index >= session.queue.length) {
            goHome(); // 最后一题点"完成"
            return;
        }
        render();
    }

    function toggleStar() {
        const id = session.queue[session.index];
        const i = state.starred.indexOf(id);
        if (i >= 0) { state.starred.splice(i, 1); } else { state.starred.push(id); }
        persist();
        render();
    }

    // ---------- 考试逻辑 ----------
    function handleExamPick(pickedIdx) {
        session.picked[session.index] = pickedIdx;
        session.confirming = false;
        render();
    }

    function examGoto(i) {
        session.index = i;
        session.confirming = false;
        render();
    }

    /** 交卷评分；force=true 表示倒计时到点自动交卷 */
    function submitExam(force) {
        if (!session || session.kind !== 'exam') { return; }
        const unanswered = session.picked.filter((p) => p < 0).length;
        if (unanswered > 0 && !force && !session.confirming) {
            session.confirming = true;
            render();
            return;
        }

        const usedSec = Math.max(EXAM_MINUTES * 60 - remainSeconds(), 0);
        let correctCount = 0;
        const wrongList = [];
        session.paper.forEach((q, i) => {
            const picked = session.picked[i];
            if (picked === q.answer) {
                correctCount++;
            } else {
                wrongList.push({ q: q, picked: picked });
                if (state.wrong.indexOf(q.id) < 0) { state.wrong.push(q.id); }
            }
        });

        state.examHistory.push({
            date: new Date().toISOString(),
            score: correctCount,
            total: session.paper.length,
            durationSec: usedSec
        });
        if (state.examHistory.length > HISTORY_LIMIT) {
            state.examHistory = state.examHistory.slice(-HISTORY_LIMIT);
        }
        persist();

        session.result = {
            score: correctCount,
            total: session.paper.length,
            durationSec: usedSec,
            wrongList: wrongList
        };
        stopTimer();
        view = { name: 'result' };
        render();
    }

    // ---------- 渲染 ----------
    const app = document.getElementById('app');

    function render() {
        let html = '';
        if (view.name === 'home') { html = renderHome(); }
        else if (view.name === 'practice') { html = renderPractice(); }
        else if (view.name === 'exam') { html = renderExam(); }
        else if (view.name === 'result') { html = renderResult(); }
        else if (view.name === 'empty') { html = renderEmpty(); }
        app.innerHTML = html;
        const img = app.querySelector('img.qimage');
        if (img) { img.focus(); }
    }

    function renderHome() {
        const total = questions.length;
        const answered = Object.keys(state.answers).length;
        const correctList = Object.values(state.answers).filter((a) => a && a.correct);
        const accuracy = answered ? Math.round(correctList.length / answered * 100) : 0;
        const lastExam = state.examHistory.length
            ? state.examHistory[state.examHistory.length - 1] : null;

        const menu = [
            { mode: 'sequential', icon: '📖', name: '顺序练习', desc: '按顺序过一遍全库' },
            { mode: 'random', icon: '🎲', name: '随机练习', desc: '随机打乱刷题' },
            { mode: 'exam', icon: '📝', name: '模拟考试', desc: EXAM_TOTAL + '题 / ' + EXAM_MINUTES + '分钟 / ' + PASS_SCORE + '分及格' },
            { mode: 'wrong', icon: '❌', name: '错题重练', desc: '答对自动移出错题本', badge: state.wrong.length },
            { mode: 'starred', icon: '⭐', name: '收藏夹', desc: '练习中点击☆收藏', badge: state.starred.length }
        ].map((m) => {
            const badge = m.badge
                ? '<span class="menu-badge">' + m.badge + '</span>' : '';
            return '<button class="menu-btn" data-action="mode:' + m.mode + '">' +
                '<span class="menu-icon">' + m.icon + '</span>' +
                '<span class="menu-text"><span class="menu-name">' + m.name + badge + '</span>' +
                '<span class="menu-desc">' + esc(m.desc) + '</span></span></button>';
        }).join('');

        const lastExamHtml = lastExam
            ? '<span class="' + (lastExam.score >= PASS_SCORE ? 'ok' : 'bad') + '">' +
              lastExam.score + '分 ' + (lastExam.score >= PASS_SCORE ? '及格' : '不及格') + '</span>'
            : '<span class="dim">未测试</span>';

        return '<div class="home">' +
            '<div class="stat-card">' +
              '<div class="stat-row"><span>学习进度</span><span>' + answered + ' / ' + total + '</span></div>' +
              '<div class="progress"><div class="progress-bar" style="width:' +
                (total ? (answered / total * 100).toFixed(1) : 0) + '%"></div></div>' +
            '</div>' +
            '<div class="stat-grid">' +
              '<div class="stat-item"><span class="stat-num">' + accuracy + '%</span><span class="stat-label">练习正确率</span></div>' +
              '<div class="stat-item"><span class="stat-num">' + state.wrong.length + '</span><span class="stat-label">错题</span></div>' +
              '<div class="stat-item"><span class="stat-num">' + state.starred.length + '</span><span class="stat-label">收藏</span></div>' +
            '</div>' +
            '<div class="stat-line">最近模拟：' + lastExamHtml + '</div>' +
            '<div class="menu">' + menu + '</div>' +
            '<div class="footnote">题库 ' + questions.length + ' 题 · 2026-07 更新 · 键盘：1-4 选项，← → 切题</div>' +
        '</div>';
    }

    function renderEmpty() {
        const texts = {
            wrong: ['错题本是空的 🎉', '练习或模拟考试中答错的题会自动收进来'],
            starred: ['还没有收藏题目', '练习时点击顶部 ☆ 即可收藏'],
            exam: ['题库不足', '题库题目数量不足以组成模拟卷']
        };
        const t = texts[view.mode] || texts.wrong;
        return '<div class="empty">' +
            '<div class="empty-title">' + t[0] + '</div>' +
            '<div class="empty-desc">' + esc(t[1]) + '</div>' +
            '<button class="btn" data-action="exit">返回首页</button>' +
        '</div>';
    }

    /** 当前练习题（错题重练中 index 可能越界 = 队列已清空） */
    function renderPractice() {
        if (session.index >= session.queue.length) {
            let done = '本组题目已完成 🎉';
            if (session.mode === 'wrong') {
                done = state.wrong.length === 0
                    ? '错题本已清空 🎉'
                    : '本轮重练完成，还剩 ' + state.wrong.length + ' 道错题';
            }
            return '<div class="empty">' +
                '<div class="empty-title">' + done + '</div>' +
                (state.wrong.length && session.mode === 'wrong'
                    ? '<button class="btn" data-action="mode:wrong">再来一轮</button>' : '') +
                '<button class="btn" data-action="exit">返回首页</button></div>';
        }

        const id = session.queue[session.index];
        const q = byId.get(id);
        const record = state.answers[id]; // {picked, correct} | undefined
        const starredNow = isStarred(id);
        const i = session.index + 1;
        const n = session.queue.length;

        const optionsHtml = q.options.map((opt, idx) => {
            let cls = 'option';
            let tag = '';
            if (record) {
                if (idx === q.answer) { cls += ' correct'; tag = '✓'; }
                else if (idx === record.picked) { cls += ' wrong'; tag = '✗'; }
                else { cls += ' dim'; }
            }
            return '<button class="' + cls + '" data-action="pick:' + idx + '"' +
                (record ? ' disabled' : '') + '>' +
                '<span class="option-label">' + LABELS[idx] + '</span>' +
                '<span class="option-text">' + esc(opt) + '</span>' +
                (tag ? '<span class="option-tag">' + tag + '</span>' : '') +
            '</button>';
        }).join('');

        let explainHtml = '';
        if (record) {
            const head = record.correct
                ? '<span class="ok">✓ 回答正确</span>'
                : '<span class="bad">✗ 回答错误，正确答案 ' + LABELS[q.answer] + '</span>';
            explainHtml = '<div class="explain"><div class="explain-head">' + head + '</div>' +
                (q.explain ? '<div class="explain-body">' + esc(q.explain) + '</div>' : '') +
                (q.law ? '<div class="explain-law">' + esc(q.law) + '</div>' : '') +
            '</div>';
        }

        const imageHtml = q.image
            ? '<img class="qimage" src="' + esc(q.image) + '" alt="题目图片" loading="lazy">'
            : '';

        const last = session.index === session.queue.length - 1;
        const modeTag = session.mode === 'wrong' ? ' · 答对自动移出' : '';

        return '<div class="practice">' +
            '<div class="topbar">' +
                '<span class="topbar-title">' + MODE_NAMES[session.mode] + modeTag + '</span>' +
                '<span>' +
                    '<button class="icon-btn' + (starredNow ? ' starred' : '') + '" data-action="toggleStar" title="收藏">' +
                        (starredNow ? '★' : '☆') + '</button> ' +
                    '<button class="icon-btn" data-action="exit" title="退出">✕</button>' +
                '</span>' +
            '</div>' +
            '<div class="progress-info">' +
                '<span>第 ' + i + ' / ' + n + ' 题</span>' +
                '<span class="badge">' + (q.type === 'judge' ? '判断题' : '单选题') + '</span>' +
            '</div>' +
            '<div class="progress"><div class="progress-bar" style="width:' +
                (session.index / n * 100).toFixed(1) + '%"></div></div>' +
            imageHtml +
            '<div class="qtext">' + esc(q.question) + '</div>' +
            '<div class="options">' + optionsHtml + '</div>' +
            explainHtml +
            '<div class="navbar">' +
                '<button class="btn secondary" data-action="prev"' +
                    (session.index === 0 ? ' disabled' : '') + '>← 上一题</button>' +
                '<button class="btn" data-action="next">' + (last ? '完成' : '下一题 →') + '</button>' +
            '</div>' +
        '</div>';
    }

    function renderExam() {
        const q = session.paper[session.index];
        const i = session.index + 1;
        const n = session.paper.length;
        const remain = Math.max(remainSeconds(), 0);
        const answered = session.picked.filter((p) => p >= 0).length;

        const optionsHtml = q.options.map((opt, idx) => {
            const picked = session.picked[session.index] === idx;
            return '<button class="option' + (picked ? ' picked' : '') + '" data-action="examPick:' + idx + '">' +
                '<span class="option-label">' + LABELS[idx] + '</span>' +
                '<span class="option-text">' + esc(opt) + '</span>' +
            '</button>';
        }).join('');

        const sheetHtml = '<div class="sheet">' + session.paper.map((qq, idx) => {
            const cls = 'sheet-cell' +
                (session.picked[idx] >= 0 ? ' done' : '') +
                (idx === session.index ? ' current' : '');
            return '<button class="' + cls + '" data-action="goto:' + idx + '">' + (idx + 1) + '</button>';
        }).join('') + '</div>';

        const confirmHtml = session.confirming
            ? '<div class="confirm-bar">还有 ' + (n - answered) + ' 题未作答，确认交卷？' +
              '<button class="btn danger small" data-action="submitForce">确认交卷</button>' +
              '<button class="btn secondary small" data-action="cancelSubmit">继续答题</button></div>'
            : '';

        const imageHtml = q.image
            ? '<img class="qimage" src="' + esc(q.image) + '" alt="题目图片" loading="lazy">'
            : '';

        return '<div class="exam">' +
            '<div class="topbar">' +
                '<span class="topbar-title">📝 模拟考试</span>' +
                '<span>' +
                    '<span class="timer' + (remain <= 300 ? ' warn' : '') + '" id="examTime">' + fmtTime(remain) + '</span> ' +
                    '<button class="btn small" data-action="submit">交卷</button>' +
                '</span>' +
            '</div>' +
            '<div class="progress-info"><span>第 ' + i + ' / ' + n + ' 题 · 已答 ' + answered + '</span>' +
                '<span class="badge">' + (q.type === 'judge' ? '判断题' : '单选题') + '</span></div>' +
            imageHtml +
            '<div class="qtext">' + esc(q.question) + '</div>' +
            '<div class="options">' + optionsHtml + '</div>' +
            '<div class="navbar">' +
                '<button class="btn secondary" data-action="examPrev"' +
                    (session.index === 0 ? ' disabled' : '') + '>← 上一题</button>' +
                '<button class="btn secondary" data-action="examNext"' +
                    (session.index === n - 1 ? ' disabled' : '') + '>下一题 →</button>' +
            '</div>' +
            confirmHtml +
            '<div class="sheet-title">答题卡</div>' +
            '<div class="sheet-grid">' + sheetHtml + '</div>' +
        '</div>';
    }

    function renderResult() {
        const r = session.result;
        const passed = r.score >= PASS_SCORE;
        const wrongHtml = r.wrongList.map((w) => {
            const yourPick = w.picked >= 0 ? LABELS[w.picked] + ' ' + w.q.options[w.picked] : '未作答';
            const rightPick = LABELS[w.q.answer] + ' ' + w.q.options[w.q.answer];
            return '<div class="review-item">' +
                '<div class="review-q"><span class="bad">✗</span> ' + esc(w.q.question) + '</div>' +
                '<div class="review-line">你的答案：<span class="bad">' + esc(yourPick) + '</span></div>' +
                '<div class="review-line">正确答案：<span class="ok">' + esc(rightPick) + '</span></div>' +
                (w.q.explain ? '<div class="review-explain">' + esc(w.q.explain) + '</div>' : '') +
                (w.q.law ? '<div class="explain-law">' + esc(w.q.law) + '</div>' : '') +
            '</div>';
        }).join('');

        return '<div class="result">' +
            '<div class="score-hero">' +
                '<div class="score-num ' + (passed ? 'ok' : 'bad') + '">' + r.score + '</div>' +
                '<div class="score-sub">' + (passed ? '🎉 恭喜及格' : '未及格，继续加油') + '（及格线 ' + PASS_SCORE + '分）</div>' +
            '</div>' +
            '<div class="result-meta">' +
                '<span>对 ' + r.score + ' 错 ' + (r.total - r.score) + '</span>' +
                '<span>用时 ' + fmtTime(r.durationSec) + '</span>' +
            '</div>' +
            (r.wrongList.length
                ? '<div class="sheet-title">错题回顾（已自动加入错题本）</div><div class="review-list">' + wrongHtml + '</div>'
                : '<div class="empty-desc">全对，太强了！</div>') +
            '<div class="navbar"><button class="btn" data-action="exit">返回首页</button></div>' +
        '</div>';
    }

    // ---------- 图片加载失败占位（error 不冒泡，用捕获） ----------
    window.addEventListener('error', (e) => {
        const img = e.target;
        if (img && img.tagName === 'IMG' && img.classList.contains('qimage')) {
            const tip = document.createElement('div');
            tip.className = 'qimage-fallback';
            tip.textContent = '⚠ 图片加载失败（图片题需联网）';
            img.replaceWith(tip);
        }
    }, true);

    // ---------- 事件委托 ----------
    document.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-action]');
        if (!btn) { return; }
        const [action, arg] = btn.dataset.action.split(':');

        switch (action) {
            case 'mode':
                if (arg === 'exam') { startExam(); } else { startPractice(arg); }
                break;
            case 'pick': handlePracticePick(Number(arg)); break;
            case 'prev': practiceStep(-1); break;
            case 'next': practiceStep(1); break;
            case 'toggleStar': toggleStar(); break;
            case 'exit': goHome(); break;
            case 'examPick': handleExamPick(Number(arg)); break;
            case 'examPrev': examGoto(session.index - 1); break;
            case 'examNext': examGoto(session.index + 1); break;
            case 'goto': examGoto(Number(arg)); break;
            case 'submit': submitExam(false); break;
            case 'submitForce': submitExam(true); break;
            case 'cancelSubmit': session.confirming = false; render(); break;
            default: break;
        }
    });

    // ---------- 键盘快捷键：1-4 选答案，←/→ 切题 ----------
    document.addEventListener('keydown', (e) => {
        if (!session) { return; }
        if (view.name === 'practice') {
            if (e.key >= '1' && e.key <= '4') {
                const idx = Number(e.key) - 1;
                const q = byId.get(session.queue[session.index]);
                if (q && idx < q.options.length && !state.answers[q.id]) { handlePracticePick(idx); }
            } else if (e.key === 'ArrowLeft' && session.index > 0) { practiceStep(-1); }
            else if (e.key === 'ArrowRight') { practiceStep(1); }
        } else if (view.name === 'exam') {
            if (e.key >= '1' && e.key <= '4') {
                const idx = Number(e.key) - 1;
                const q = session.paper[session.index];
                if (q && idx < q.options.length) { handleExamPick(idx); }
            } else if (e.key === 'ArrowLeft' && session.index > 0) { examGoto(session.index - 1); }
            else if (e.key === 'ArrowRight' && session.index < session.paper.length - 1) { examGoto(session.index + 1); }
        }
    });

    // ---------- 启动 ----------
    window.addEventListener('message', (e) => {
        const msg = e.data;
        if (msg && msg.type === 'init') {
            questions = msg.questions || [];
            byId = new Map(questions.map((q) => [q.id, q]));
            state = normalizeState(msg.state);
            render();
        }
    });

    vscode.postMessage({ type: 'ready' });
})();
