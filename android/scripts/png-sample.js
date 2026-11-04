// PNG 像素采样：node scripts/png-sample.js <png> <x> <y> [x2 y2 ...]
// 自带解码（inflate + 反滤波），用于验证主题颜色是否生效。
'use strict';
const fs = require('fs');
const zlib = require('zlib');

function decode(file) {
    const buf = fs.readFileSync(file);
    // 扫描 chunk
    let off = 8, w = 0, h = 0, bitDepth = 0, colorType = 0, idat = [];
    while (off < buf.length) {
        const len = buf.readUInt32BE(off);
        const type = buf.toString('ascii', off + 4, off + 8);
        const data = buf.subarray(off + 8, off + 8 + len);
        if (type === 'IHDR') {
            w = data.readUInt32BE(0); h = data.readUInt32BE(4);
            bitDepth = data[8]; colorType = data[9];
        } else if (type === 'IDAT') { idat.push(data); }
        else if (type === 'IEND') { break; }
        off += 12 + len;
    }
    if (bitDepth !== 8 || (colorType !== 6 && colorType !== 2)) {
        throw new Error(`仅支持 8bit RGBA/RGB，当前 depth=${bitDepth} color=${colorType}`);
    }
    const bpp = colorType === 6 ? 4 : 3;
    const raw = zlib.inflateSync(Buffer.concat(idat));
    const stride = w * bpp;
    const out = Buffer.alloc(h * stride);
    // 反滤波
    for (let y = 0; y < h; y++) {
        const f = raw[y * (stride + 1)];
        const row = raw.subarray(y * (stride + 1) + 1, (y + 1) * (stride + 1));
        const prev = y > 0 ? out.subarray((y - 1) * stride, y * stride) : null;
        const cur = out.subarray(y * stride, (y + 1) * stride);
        for (let i = 0; i < stride; i++) {
            const a = i >= bpp ? cur[i - bpp] : 0;
            const b = prev ? prev[i] : 0;
            const c = (i >= bpp && prev) ? prev[i - bpp] : 0;
            let v = row[i];
            switch (f) {
                case 0: break;
                case 1: v = (v + a) & 0xff; break;
                case 2: v = (v + b) & 0xff; break;
                case 3: v = (v + ((a + b) >> 1)) & 0xff; break;
                case 4: {
                    const p = a + b - c, pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                    v = (v + (pa <= pb && pa <= pc ? a : pb <= pc ? b : c)) & 0xff;
                    break;
                }
            }
            cur[i] = v;
        }
    }
    return { w, h, bpp, data: out };
}

const file = process.argv[2];
const img = decode(file);
console.log(`尺寸 ${img.w}x${img.h}`);
for (let i = 3; i < process.argv.length; i += 2) {
    const x = parseInt(process.argv[i], 10), y = parseInt(process.argv[i + 1], 10);
    const o = (y * img.w + x) * img.bpp;
    const r = img.data[o], g = img.data[o + 1], b = img.data[o + 2];
    console.log(`(${x},${y}) = #${[r, g, b].map(v => v.toString(16).padStart(2, '0')).join('')}`);
}
