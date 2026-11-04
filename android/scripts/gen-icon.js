// 生成像素风启动图标：node scripts/gen-icon.js（零依赖，Node 内置 zlib 手写 PNG 编码）
// 在 54x54 网格上程序化绘制像素方向盘，最近邻放大到各密度 foreground PNG。
'use strict';

const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

// ---------- 极简 PNG 编码器（RGBA 8bit，无压缩依赖） ----------
const CRC_TABLE = (() => {
    const t = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
        let c = n;
        for (let k = 0; k < 8; k++) { c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1); }
        t[n] = c >>> 0;
    }
    return t;
})();

function crc32(buf) {
    let c = 0xffffffff;
    for (let i = 0; i < buf.length; i++) { c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8); }
    return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const body = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(body), 0);
    return Buffer.concat([len, body, crc]);
}

function encodePng(w, h, rgba) {
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(w, 0);
    ihdr.writeUInt32BE(h, 4);
    ihdr[8] = 8;  // bit depth
    ihdr[9] = 6;  // color type: RGBA
    const raw = Buffer.alloc((w * 4 + 1) * h);
    for (let y = 0; y < h; y++) {
        const off = y * (w * 4 + 1);
        raw[off] = 0; // filter: none
        rgba.copy(raw, off + 1, y * w * 4, (y + 1) * w * 4);
    }
    return Buffer.concat([
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
        chunk('IHDR', ihdr),
        chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
        chunk('IEND', Buffer.alloc(0)),
    ]);
}

// ---------- 54x54 像素方向盘 ----------
const G = 54;
const CX = 26.5, CY = 26.5;
const R_OUT = 23, R_IN = 19.5;   // 外圈环
const HUB = 6.2, HUB_IN = 4;     // 中心毂

function hex(c) {
    return [parseInt(c.slice(1, 3), 16), parseInt(c.slice(3, 5), 16), parseInt(c.slice(5, 7), 16)];
}
const IVORY = hex('#F5EDD8');   // 圈与幅条
const YELLOW = hex('#FFD23F');  // 毂心
const BG = hex('#1B2040');      // 预览底色（正式 adaptive 背景走 drawable）

const grid = new Uint8Array(G * G); // 0 空 / 1 象牙 / 2 黄
for (let y = 0; y < G; y++) {
    for (let x = 0; x < G; x++) {
        const dx = x + 0.5 - CX, dy = y + 0.5 - CY;
        const d = Math.hypot(dx, dy);
        let v = 0;
        if (d >= R_IN && d <= R_OUT) { v = 1; }                                  // 外圈
        if (Math.abs(dy) <= 1.8 && Math.abs(dx) >= HUB && Math.abs(dx) <= R_IN) { v = 1; } // 左右幅条
        if (Math.abs(dx) <= 1.8 && dy >= HUB && dy <= R_IN) { v = 1; }           // 底部幅条
        if (d <= HUB) { v = 1; }                                                 // 毂
        if (d <= HUB_IN) { v = 2; }                                              // 毂心
        grid[y * G + x] = v;
    }
}

/** 最近邻放大：54 网格 → size×size RGBA。withBg=true 时透明区填底色（预览用） */
function render(size, withBg) {
    const out = Buffer.alloc(size * size * 4);
    for (let y = 0; y < size; y++) {
        for (let x = 0; x < size; x++) {
            const v = grid[Math.floor(y * G / size) * G + Math.floor(x * G / size)];
            const rgb = v === 0 ? (withBg ? BG : [0, 0, 0]) : (v === 1 ? IVORY : YELLOW);
            const o = (y * size + x) * 4;
            out[o] = rgb[0]; out[o + 1] = rgb[1]; out[o + 2] = rgb[2];
            out[o + 3] = (v === 0 && !withBg) ? 0 : 255;
        }
    }
    return encodePng(size, size, out);
}

const resDir = path.join(__dirname, '..', 'app', 'src', 'main', 'res');
const densities = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };
for (const [name, size] of Object.entries(densities)) {
    const dir = path.join(resDir, 'mipmap-' + name);
    fs.mkdirSync(dir, { recursive: true });
    fs.writeFileSync(path.join(dir, 'ic_launcher_fg.png'), render(size, false));
    console.log('✓ mipmap-' + name + '/ic_launcher_fg.png (' + size + 'px)');
}

// 预览图（带底色），供人工核对
const previewDir = path.join(__dirname, '..', 'build');
fs.mkdirSync(previewDir, { recursive: true });
fs.writeFileSync(path.join(previewDir, 'icon-preview.png'), render(324, true));
console.log('✓ build/icon-preview.png');
