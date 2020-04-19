/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function(root, factory) {
    if ( typeof exports === 'object' ) {
        if ( typeof module === 'object' ) {
            module.exports = factory( require("js-sha256"), require("base64-js") );    
        } else {
            exports["keycloak"] = factory( require("js-sha256"), require("base64-js") );    
        }
    } else {
        /**
        * [js-sha256]{@link https://github.com/emn178/js-sha256}
        *
        * @version 0.9.0
        * @author Chen, Yi-Cyuan [emn178@gmail.com]
        * @copyright Chen, Yi-Cyuan 2014-2017
        * @license MIT
        */
        !function () { "use strict"; function t(t, i) { i ? (d[0] = d[16] = d[1] = d[2] = d[3] = d[4] = d[5] = d[6] = d[7] = d[8] = d[9] = d[10] = d[11] = d[12] = d[13] = d[14] = d[15] = 0, this.blocks = d) : this.blocks = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0], t ? (this.h0 = 3238371032, this.h1 = 914150663, this.h2 = 812702999, this.h3 = 4144912697, this.h4 = 4290775857, this.h5 = 1750603025, this.h6 = 1694076839, this.h7 = 3204075428) : (this.h0 = 1779033703, this.h1 = 3144134277, this.h2 = 1013904242, this.h3 = 2773480762, this.h4 = 1359893119, this.h5 = 2600822924, this.h6 = 528734635, this.h7 = 1541459225), this.block = this.start = this.bytes = this.hBytes = 0, this.finalized = this.hashed = !1, this.first = !0, this.is224 = t } function i(i, r, s) { var e, n = typeof i; if ("string" === n) { var o, a = [], u = i.length, c = 0; for (e = 0; e < u; ++e)(o = i.charCodeAt(e)) < 128 ? a[c++] = o : o < 2048 ? (a[c++] = 192 | o >> 6, a[c++] = 128 | 63 & o) : o < 55296 || o >= 57344 ? (a[c++] = 224 | o >> 12, a[c++] = 128 | o >> 6 & 63, a[c++] = 128 | 63 & o) : (o = 65536 + ((1023 & o) << 10 | 1023 & i.charCodeAt(++e)), a[c++] = 240 | o >> 18, a[c++] = 128 | o >> 12 & 63, a[c++] = 128 | o >> 6 & 63, a[c++] = 128 | 63 & o); i = a } else { if ("object" !== n) throw new Error(h); if (null === i) throw new Error(h); if (f && i.constructor === ArrayBuffer) i = new Uint8Array(i); else if (!(Array.isArray(i) || f && ArrayBuffer.isView(i))) throw new Error(h) } i.length > 64 && (i = new t(r, !0).update(i).array()); var y = [], p = []; for (e = 0; e < 64; ++e) { var l = i[e] || 0; y[e] = 92 ^ l, p[e] = 54 ^ l } t.call(this, r, s), this.update(p), this.oKeyPad = y, this.inner = !0, this.sharedMemory = s } var h = "input is invalid type", r = "object" == typeof window, s = r ? window : {}; s.JS_SHA256_NO_WINDOW && (r = !1); var e = !r && "object" == typeof self, n = !s.JS_SHA256_NO_NODE_JS && "object" == typeof process && process.versions && process.versions.node; n ? s = global : e && (s = self); var o = !s.JS_SHA256_NO_COMMON_JS && "object" == typeof module && module.exports, a = "function" == typeof define && define.amd, f = !s.JS_SHA256_NO_ARRAY_BUFFER && "undefined" != typeof ArrayBuffer, u = "0123456789abcdef".split(""), c = [-2147483648, 8388608, 32768, 128], y = [24, 16, 8, 0], p = [1116352408, 1899447441, 3049323471, 3921009573, 961987163, 1508970993, 2453635748, 2870763221, 3624381080, 310598401, 607225278, 1426881987, 1925078388, 2162078206, 2614888103, 3248222580, 3835390401, 4022224774, 264347078, 604807628, 770255983, 1249150122, 1555081692, 1996064986, 2554220882, 2821834349, 2952996808, 3210313671, 3336571891, 3584528711, 113926993, 338241895, 666307205, 773529912, 1294757372, 1396182291, 1695183700, 1986661051, 2177026350, 2456956037, 2730485921, 2820302411, 3259730800, 3345764771, 3516065817, 3600352804, 4094571909, 275423344, 430227734, 506948616, 659060556, 883997877, 958139571, 1322822218, 1537002063, 1747873779, 1955562222, 2024104815, 2227730452, 2361852424, 2428436474, 2756734187, 3204031479, 3329325298], l = ["hex", "array", "digest", "arrayBuffer"], d = []; !s.JS_SHA256_NO_NODE_JS && Array.isArray || (Array.isArray = function (t) { return "[object Array]" === Object.prototype.toString.call(t) }), !f || !s.JS_SHA256_NO_ARRAY_BUFFER_IS_VIEW && ArrayBuffer.isView || (ArrayBuffer.isView = function (t) { return "object" == typeof t && t.buffer && t.buffer.constructor === ArrayBuffer }); var A = function (i, h) { return function (r) { return new t(h, !0).update(r)[i]() } }, w = function (i) { var h = A("hex", i); n && (h = b(h, i)), h.create = function () { return new t(i) }, h.update = function (t) { return h.create().update(t) }; for (var r = 0; r < l.length; ++r) { var s = l[r]; h[s] = A(s, i) } return h }, b = function (t, i) { var r = eval("require('crypto')"), s = eval("require('buffer').Buffer"), e = i ? "sha224" : "sha256", n = function (i) { if ("string" == typeof i) return r.createHash(e).update(i, "utf8").digest("hex"); if (null === i || void 0 === i) throw new Error(h); return i.constructor === ArrayBuffer && (i = new Uint8Array(i)), Array.isArray(i) || ArrayBuffer.isView(i) || i.constructor === s ? r.createHash(e).update(new s(i)).digest("hex") : t(i) }; return n }, v = function (t, h) { return function (r, s) { return new i(r, h, !0).update(s)[t]() } }, _ = function (t) { var h = v("hex", t); h.create = function (h) { return new i(h, t) }, h.update = function (t, i) { return h.create(t).update(i) }; for (var r = 0; r < l.length; ++r) { var s = l[r]; h[s] = v(s, t) } return h }; t.prototype.update = function (t) { if (!this.finalized) { var i, r = typeof t; if ("string" !== r) { if ("object" !== r) throw new Error(h); if (null === t) throw new Error(h); if (f && t.constructor === ArrayBuffer) t = new Uint8Array(t); else if (!(Array.isArray(t) || f && ArrayBuffer.isView(t))) throw new Error(h); i = !0 } for (var s, e, n = 0, o = t.length, a = this.blocks; n < o;) { if (this.hashed && (this.hashed = !1, a[0] = this.block, a[16] = a[1] = a[2] = a[3] = a[4] = a[5] = a[6] = a[7] = a[8] = a[9] = a[10] = a[11] = a[12] = a[13] = a[14] = a[15] = 0), i) for (e = this.start; n < o && e < 64; ++n)a[e >> 2] |= t[n] << y[3 & e++]; else for (e = this.start; n < o && e < 64; ++n)(s = t.charCodeAt(n)) < 128 ? a[e >> 2] |= s << y[3 & e++] : s < 2048 ? (a[e >> 2] |= (192 | s >> 6) << y[3 & e++], a[e >> 2] |= (128 | 63 & s) << y[3 & e++]) : s < 55296 || s >= 57344 ? (a[e >> 2] |= (224 | s >> 12) << y[3 & e++], a[e >> 2] |= (128 | s >> 6 & 63) << y[3 & e++], a[e >> 2] |= (128 | 63 & s) << y[3 & e++]) : (s = 65536 + ((1023 & s) << 10 | 1023 & t.charCodeAt(++n)), a[e >> 2] |= (240 | s >> 18) << y[3 & e++], a[e >> 2] |= (128 | s >> 12 & 63) << y[3 & e++], a[e >> 2] |= (128 | s >> 6 & 63) << y[3 & e++], a[e >> 2] |= (128 | 63 & s) << y[3 & e++]); this.lastByteIndex = e, this.bytes += e - this.start, e >= 64 ? (this.block = a[16], this.start = e - 64, this.hash(), this.hashed = !0) : this.start = e } return this.bytes > 4294967295 && (this.hBytes += this.bytes / 4294967296 << 0, this.bytes = this.bytes % 4294967296), this } }, t.prototype.finalize = function () { if (!this.finalized) { this.finalized = !0; var t = this.blocks, i = this.lastByteIndex; t[16] = this.block, t[i >> 2] |= c[3 & i], this.block = t[16], i >= 56 && (this.hashed || this.hash(), t[0] = this.block, t[16] = t[1] = t[2] = t[3] = t[4] = t[5] = t[6] = t[7] = t[8] = t[9] = t[10] = t[11] = t[12] = t[13] = t[14] = t[15] = 0), t[14] = this.hBytes << 3 | this.bytes >>> 29, t[15] = this.bytes << 3, this.hash() } }, t.prototype.hash = function () { var t, i, h, r, s, e, n, o, a, f = this.h0, u = this.h1, c = this.h2, y = this.h3, l = this.h4, d = this.h5, A = this.h6, w = this.h7, b = this.blocks; for (t = 16; t < 64; ++t)i = ((s = b[t - 15]) >>> 7 | s << 25) ^ (s >>> 18 | s << 14) ^ s >>> 3, h = ((s = b[t - 2]) >>> 17 | s << 15) ^ (s >>> 19 | s << 13) ^ s >>> 10, b[t] = b[t - 16] + i + b[t - 7] + h << 0; for (a = u & c, t = 0; t < 64; t += 4)this.first ? (this.is224 ? (e = 300032, w = (s = b[0] - 1413257819) - 150054599 << 0, y = s + 24177077 << 0) : (e = 704751109, w = (s = b[0] - 210244248) - 1521486534 << 0, y = s + 143694565 << 0), this.first = !1) : (i = (f >>> 2 | f << 30) ^ (f >>> 13 | f << 19) ^ (f >>> 22 | f << 10), r = (e = f & u) ^ f & c ^ a, w = y + (s = w + (h = (l >>> 6 | l << 26) ^ (l >>> 11 | l << 21) ^ (l >>> 25 | l << 7)) + (l & d ^ ~l & A) + p[t] + b[t]) << 0, y = s + (i + r) << 0), i = (y >>> 2 | y << 30) ^ (y >>> 13 | y << 19) ^ (y >>> 22 | y << 10), r = (n = y & f) ^ y & u ^ e, A = c + (s = A + (h = (w >>> 6 | w << 26) ^ (w >>> 11 | w << 21) ^ (w >>> 25 | w << 7)) + (w & l ^ ~w & d) + p[t + 1] + b[t + 1]) << 0, i = ((c = s + (i + r) << 0) >>> 2 | c << 30) ^ (c >>> 13 | c << 19) ^ (c >>> 22 | c << 10), r = (o = c & y) ^ c & f ^ n, d = u + (s = d + (h = (A >>> 6 | A << 26) ^ (A >>> 11 | A << 21) ^ (A >>> 25 | A << 7)) + (A & w ^ ~A & l) + p[t + 2] + b[t + 2]) << 0, i = ((u = s + (i + r) << 0) >>> 2 | u << 30) ^ (u >>> 13 | u << 19) ^ (u >>> 22 | u << 10), r = (a = u & c) ^ u & y ^ o, l = f + (s = l + (h = (d >>> 6 | d << 26) ^ (d >>> 11 | d << 21) ^ (d >>> 25 | d << 7)) + (d & A ^ ~d & w) + p[t + 3] + b[t + 3]) << 0, f = s + (i + r) << 0; this.h0 = this.h0 + f << 0, this.h1 = this.h1 + u << 0, this.h2 = this.h2 + c << 0, this.h3 = this.h3 + y << 0, this.h4 = this.h4 + l << 0, this.h5 = this.h5 + d << 0, this.h6 = this.h6 + A << 0, this.h7 = this.h7 + w << 0 }, t.prototype.hex = function () { this.finalize(); var t = this.h0, i = this.h1, h = this.h2, r = this.h3, s = this.h4, e = this.h5, n = this.h6, o = this.h7, a = u[t >> 28 & 15] + u[t >> 24 & 15] + u[t >> 20 & 15] + u[t >> 16 & 15] + u[t >> 12 & 15] + u[t >> 8 & 15] + u[t >> 4 & 15] + u[15 & t] + u[i >> 28 & 15] + u[i >> 24 & 15] + u[i >> 20 & 15] + u[i >> 16 & 15] + u[i >> 12 & 15] + u[i >> 8 & 15] + u[i >> 4 & 15] + u[15 & i] + u[h >> 28 & 15] + u[h >> 24 & 15] + u[h >> 20 & 15] + u[h >> 16 & 15] + u[h >> 12 & 15] + u[h >> 8 & 15] + u[h >> 4 & 15] + u[15 & h] + u[r >> 28 & 15] + u[r >> 24 & 15] + u[r >> 20 & 15] + u[r >> 16 & 15] + u[r >> 12 & 15] + u[r >> 8 & 15] + u[r >> 4 & 15] + u[15 & r] + u[s >> 28 & 15] + u[s >> 24 & 15] + u[s >> 20 & 15] + u[s >> 16 & 15] + u[s >> 12 & 15] + u[s >> 8 & 15] + u[s >> 4 & 15] + u[15 & s] + u[e >> 28 & 15] + u[e >> 24 & 15] + u[e >> 20 & 15] + u[e >> 16 & 15] + u[e >> 12 & 15] + u[e >> 8 & 15] + u[e >> 4 & 15] + u[15 & e] + u[n >> 28 & 15] + u[n >> 24 & 15] + u[n >> 20 & 15] + u[n >> 16 & 15] + u[n >> 12 & 15] + u[n >> 8 & 15] + u[n >> 4 & 15] + u[15 & n]; return this.is224 || (a += u[o >> 28 & 15] + u[o >> 24 & 15] + u[o >> 20 & 15] + u[o >> 16 & 15] + u[o >> 12 & 15] + u[o >> 8 & 15] + u[o >> 4 & 15] + u[15 & o]), a }, t.prototype.toString = t.prototype.hex, t.prototype.digest = function () { this.finalize(); var t = this.h0, i = this.h1, h = this.h2, r = this.h3, s = this.h4, e = this.h5, n = this.h6, o = this.h7, a = [t >> 24 & 255, t >> 16 & 255, t >> 8 & 255, 255 & t, i >> 24 & 255, i >> 16 & 255, i >> 8 & 255, 255 & i, h >> 24 & 255, h >> 16 & 255, h >> 8 & 255, 255 & h, r >> 24 & 255, r >> 16 & 255, r >> 8 & 255, 255 & r, s >> 24 & 255, s >> 16 & 255, s >> 8 & 255, 255 & s, e >> 24 & 255, e >> 16 & 255, e >> 8 & 255, 255 & e, n >> 24 & 255, n >> 16 & 255, n >> 8 & 255, 255 & n]; return this.is224 || a.push(o >> 24 & 255, o >> 16 & 255, o >> 8 & 255, 255 & o), a }, t.prototype.array = t.prototype.digest, t.prototype.arrayBuffer = function () { this.finalize(); var t = new ArrayBuffer(this.is224 ? 28 : 32), i = new DataView(t); return i.setUint32(0, this.h0), i.setUint32(4, this.h1), i.setUint32(8, this.h2), i.setUint32(12, this.h3), i.setUint32(16, this.h4), i.setUint32(20, this.h5), i.setUint32(24, this.h6), this.is224 || i.setUint32(28, this.h7), t }, i.prototype = new t, i.prototype.finalize = function () { if (t.prototype.finalize.call(this), this.inner) { this.inner = !1; var i = this.array(); t.call(this, this.is224, this.sharedMemory), this.update(this.oKeyPad), this.update(i), t.prototype.finalize.call(this) } }; var B = w(); B.sha256 = B, B.sha224 = w(!0), B.sha256.hmac = _(), B.sha224.hmac = _(!0), o ? module.exports = B : (s.sha256 = B.sha256, s.sha224 = B.sha224, a && define(function () { return B })) }();

        /**
         * [base64-js]{@link https://github.com/beatgammit/base64-js}
         *
         * @version v1.3.0 
         * @author Kirill, Fomichev
         * @copyright Kirill, Fomichev 2014
         * @license MIT
         */
        (function (r) { if (typeof exports === "object" && typeof module !== "undefined") { module.exports = r() } else if (typeof define === "function" && define.amd) { define([], r) } else { var e; if (typeof window !== "undefined") { e = window } else if (typeof global !== "undefined") { e = global } else if (typeof self !== "undefined") { e = self } else { e = this } e.base64js = r() } })(function () { var r, e, n; return function () { function r(e, n, t) { function o(f, i) { if (!n[f]) { if (!e[f]) { var u = "function" == typeof require && require; if (!i && u) return u(f, !0); if (a) return a(f, !0); var v = new Error("Cannot find module '" + f + "'"); throw v.code = "MODULE_NOT_FOUND", v } var d = n[f] = { exports: {} }; e[f][0].call(d.exports, function (r) { var n = e[f][1][r]; return o(n || r) }, d, d.exports, r, e, n, t) } return n[f].exports } for (var a = "function" == typeof require && require, f = 0; f < t.length; f++)o(t[f]); return o } return r }()({ "/": [function (r, e, n) { "use strict"; n.byteLength = d; n.toByteArray = h; n.fromByteArray = p; var t = []; var o = []; var a = typeof Uint8Array !== "undefined" ? Uint8Array : Array; var f = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"; for (var i = 0, u = f.length; i < u; ++i) { t[i] = f[i]; o[f.charCodeAt(i)] = i } o["-".charCodeAt(0)] = 62; o["_".charCodeAt(0)] = 63; function v(r) { var e = r.length; if (e % 4 > 0) { throw new Error("Invalid string. Length must be a multiple of 4") } var n = r.indexOf("="); if (n === -1) n = e; var t = n === e ? 0 : 4 - n % 4; return [n, t] } function d(r) { var e = v(r); var n = e[0]; var t = e[1]; return (n + t) * 3 / 4 - t } function c(r, e, n) { return (e + n) * 3 / 4 - n } function h(r) { var e; var n = v(r); var t = n[0]; var f = n[1]; var i = new a(c(r, t, f)); var u = 0; var d = f > 0 ? t - 4 : t; for (var h = 0; h < d; h += 4) { e = o[r.charCodeAt(h)] << 18 | o[r.charCodeAt(h + 1)] << 12 | o[r.charCodeAt(h + 2)] << 6 | o[r.charCodeAt(h + 3)]; i[u++] = e >> 16 & 255; i[u++] = e >> 8 & 255; i[u++] = e & 255 } if (f === 2) { e = o[r.charCodeAt(h)] << 2 | o[r.charCodeAt(h + 1)] >> 4; i[u++] = e & 255 } if (f === 1) { e = o[r.charCodeAt(h)] << 10 | o[r.charCodeAt(h + 1)] << 4 | o[r.charCodeAt(h + 2)] >> 2; i[u++] = e >> 8 & 255; i[u++] = e & 255 } return i } function s(r) { return t[r >> 18 & 63] + t[r >> 12 & 63] + t[r >> 6 & 63] + t[r & 63] } function l(r, e, n) { var t; var o = []; for (var a = e; a < n; a += 3) { t = (r[a] << 16 & 16711680) + (r[a + 1] << 8 & 65280) + (r[a + 2] & 255); o.push(s(t)) } return o.join("") } function p(r) { var e; var n = r.length; var o = n % 3; var a = []; var f = 16383; for (var i = 0, u = n - o; i < u; i += f) { a.push(l(r, i, i + f > u ? u : i + f)) } if (o === 1) { e = r[n - 1]; a.push(t[e >> 2] + t[e << 4 & 63] + "==") } else if (o === 2) { e = (r[n - 2] << 8) + r[n - 1]; a.push(t[e >> 10] + t[e >> 4 & 63] + t[e << 2 & 63] + "=") } return a.join("") } }, {}] }, {}, [])("/") });

        /**
         * [promise-polyfill]{@link https://github.com/taylorhakes/promise-polyfill}
         *
         * @version v8.1.3 
         * @author Hakes, Taylor
         * @copyright Hakes, Taylor 2014
         * @license MIT
         */
        !function(e,n){"object"==typeof exports&&"undefined"!=typeof module?n():"function"==typeof define&&define.amd?define(n):n()}(0,function(){"use strict";function e(e){var n=this.constructor;return this.then(function(t){return n.resolve(e()).then(function(){return t})},function(t){return n.resolve(e()).then(function(){return n.reject(t)})})}function n(e){return!(!e||"undefined"==typeof e.length)}function t(){}function o(e){if(!(this instanceof o))throw new TypeError("Promises must be constructed via new");if("function"!=typeof e)throw new TypeError("not a function");this._state=0,this._handled=!1,this._value=undefined,this._deferreds=[],c(e,this)}function r(e,n){for(;3===e._state;)e=e._value;0!==e._state?(e._handled=!0,o._immediateFn(function(){var t=1===e._state?n.onFulfilled:n.onRejected;if(null!==t){var o;try{o=t(e._value)}catch(r){return void f(n.promise,r)}i(n.promise,o)}else(1===e._state?i:f)(n.promise,e._value)})):e._deferreds.push(n)}function i(e,n){try{if(n===e)throw new TypeError("A promise cannot be resolved with itself.");if(n&&("object"==typeof n||"function"==typeof n)){var t=n.then;if(n instanceof o)return e._state=3,e._value=n,void u(e);if("function"==typeof t)return void c(function(e,n){return function(){e.apply(n,arguments)}}(t,n),e)}e._state=1,e._value=n,u(e)}catch(r){f(e,r)}}function f(e,n){e._state=2,e._value=n,u(e)}function u(e){2===e._state&&0===e._deferreds.length&&o._immediateFn(function(){e._handled||o._unhandledRejectionFn(e._value)});for(var n=0,t=e._deferreds.length;t>n;n++)r(e,e._deferreds[n]);e._deferreds=null}function c(e,n){var t=!1;try{e(function(e){t||(t=!0,i(n,e))},function(e){t||(t=!0,f(n,e))})}catch(o){if(t)return;t=!0,f(n,o)}}var a=setTimeout;o.prototype["catch"]=function(e){return this.then(null,e)},o.prototype.then=function(e,n){var o=new this.constructor(t);return r(this,new function(e,n,t){this.onFulfilled="function"==typeof e?e:null,this.onRejected="function"==typeof n?n:null,this.promise=t}(e,n,o)),o},o.prototype["finally"]=e,o.all=function(e){return new o(function(t,o){function r(e,n){try{if(n&&("object"==typeof n||"function"==typeof n)){var u=n.then;if("function"==typeof u)return void u.call(n,function(n){r(e,n)},o)}i[e]=n,0==--f&&t(i)}catch(c){o(c)}}if(!n(e))return o(new TypeError("Promise.all accepts an array"));var i=Array.prototype.slice.call(e);if(0===i.length)return t([]);for(var f=i.length,u=0;i.length>u;u++)r(u,i[u])})},o.resolve=function(e){return e&&"object"==typeof e&&e.constructor===o?e:new o(function(n){n(e)})},o.reject=function(e){return new o(function(n,t){t(e)})},o.race=function(e){return new o(function(t,r){if(!n(e))return r(new TypeError("Promise.race accepts an array"));for(var i=0,f=e.length;f>i;i++)o.resolve(e[i]).then(t,r)})},o._immediateFn="function"==typeof setImmediate&&function(e){setImmediate(e)}||function(e){a(e,0)},o._unhandledRejectionFn=function(e){void 0!==console&&console&&console.warn("Possible Unhandled Promise Rejection:",e)};var l=function(){if("undefined"!=typeof self)return self;if("undefined"!=typeof window)return window;if("undefined"!=typeof global)return global;throw Error("unable to locate global object")}();"Promise"in l?l.Promise.prototype["finally"]||(l.Promise.prototype["finally"]=e):l.Promise=o});

        var Keycloak = factory( root["sha256"], root["base64js"] );
        root["Keycloak"] = Keycloak;

        if ( typeof define === "function" && define.amd ) { 
            define( "keycloak", [], function () { return Keycloak; } );
        }
    }
})(window, function (sha256_imported, base64js_imported) {
    if (typeof Promise === 'undefined') {
        throw Error('Keycloak requires an environment that supports Promises. Make sure that you include the appropriate polyfill.');
    }

    var loggedPromiseDeprecation = false;

    function logPromiseDeprecation() {
        if (!loggedPromiseDeprecation) {
            loggedPromiseDeprecation = true;
            console.warn('[KEYCLOAK] Usage of legacy style promise methods such as `.error()` and `.success()` has been deprecated and support will be removed in future versions. Use standard style promise methods such as `.then() and `.catch()` instead.');
        }
    }

    function toKeycloakPromise(promise) {
        promise.__proto__ = KeycloakPromise.prototype;
        return promise;
    }

    function KeycloakPromise(executor) {
        return toKeycloakPromise(new Promise(executor));
    }

    KeycloakPromise.prototype = Object.create(Promise.prototype);
    KeycloakPromise.prototype.constructor = KeycloakPromise;

    KeycloakPromise.prototype.success = function(callback) {
        logPromiseDeprecation();

        var promise = this.then(function handleSuccess(value) {
            callback(value);
        });
        
        return toKeycloakPromise(promise);
    };

    KeycloakPromise.prototype.error = function(callback) {
        logPromiseDeprecation();

        var promise = this.catch(function handleError(error) {
            callback(error);
        });

        return toKeycloakPromise(promise);
    };

    function Keycloak (config) {
        if (!(this instanceof Keycloak)) {
            return new Keycloak(config);
        }

        var kc = this;
        var adapter;
        var refreshQueue = [];
        var callbackStorage;

        var loginIframe = {
            enable: true,
            callbackList: [],
            interval: 5
        };

        var scripts = document.getElementsByTagName('script');
        for (var i = 0; i < scripts.length; i++) {
            if ((scripts[i].src.indexOf('keycloak.js') !== -1 || scripts[i].src.indexOf('keycloak.min.js') !== -1) && scripts[i].src.indexOf('version=') !== -1) {
                kc.iframeVersion = scripts[i].src.substring(scripts[i].src.indexOf('version=') + 8).split('&')[0];
            }
        }

        var useNonce = true;
        var logInfo = createLogger(console.info);
        var logWarn = createLogger(console.warn);
        
        kc.init = function (initOptions) {
            kc.authenticated = false;

            callbackStorage = createCallbackStorage();
            var adapters = ['default', 'cordova', 'cordova-native'];

            if (initOptions && adapters.indexOf(initOptions.adapter) > -1) {
                adapter = loadAdapter(initOptions.adapter);
            } else if (initOptions && typeof initOptions.adapter === "object") {
                adapter = initOptions.adapter;
            } else {
                if (window.Cordova || window.cordova) {
                    adapter = loadAdapter('cordova');
                } else {
                    adapter = loadAdapter();
                }
            }

            if (initOptions) {
                if (typeof initOptions.useNonce !== 'undefined') {
                    useNonce = initOptions.useNonce;
                }

                if (typeof initOptions.checkLoginIframe !== 'undefined') {
                    loginIframe.enable = initOptions.checkLoginIframe;
                }

                if (initOptions.checkLoginIframeInterval) {
                    loginIframe.interval = initOptions.checkLoginIframeInterval;
                }

                if (initOptions.onLoad === 'login-required') {
                    kc.loginRequired = true;
                }

                if (initOptions.responseMode) {
                    if (initOptions.responseMode === 'query' || initOptions.responseMode === 'fragment') {
                        kc.responseMode = initOptions.responseMode;
                    } else {
                        throw 'Invalid value for responseMode';
                    }
                }

                if (initOptions.flow) {
                    switch (initOptions.flow) {
                        case 'standard':
                            kc.responseType = 'code';
                            break;
                        case 'implicit':
                            kc.responseType = 'id_token token';
                            break;
                        case 'hybrid':
                            kc.responseType = 'code id_token token';
                            break;
                        default:
                            throw 'Invalid value for flow';
                    }
                    kc.flow = initOptions.flow;
                }

                if (initOptions.timeSkew != null) {
                    kc.timeSkew = initOptions.timeSkew;
                }

                if(initOptions.redirectUri) {
                    kc.redirectUri = initOptions.redirectUri;
                }

                if (initOptions.silentCheckSsoRedirectUri) {
                    kc.silentCheckSsoRedirectUri = initOptions.silentCheckSsoRedirectUri;
                }

                if (initOptions.pkceMethod) {
                    if (initOptions.pkceMethod !== "S256") {
                        throw 'Invalid value for pkceMethod';
                    }
                    kc.pkceMethod = initOptions.pkceMethod;
                }

                if (typeof initOptions.enableLogging === 'boolean') {
                    kc.enableLogging = initOptions.enableLogging;
                } else {
                    kc.enableLogging = false;
                }
            }

            if (!kc.responseMode) {
                kc.responseMode = 'fragment';
            }
            if (!kc.responseType) {
                kc.responseType = 'code';
                kc.flow = 'standard';
            }

            var promise = createPromise();

            var initPromise = createPromise();
            initPromise.promise.then(function() {
                kc.onReady && kc.onReady(kc.authenticated);
                promise.setSuccess(kc.authenticated);
            }).catch(function(errorData) {
                promise.setError(errorData);
            });

            var configPromise = loadConfig(config);

            function onLoad() {
                var doLogin = function(prompt) {
                    if (!prompt) {
                        options.prompt = 'none';
                    }

                    kc.login(options).then(function () {
                        initPromise.setSuccess();
                    }).catch(function () {
                        initPromise.setError();
                    });
                }

                var checkSsoSilently = function() {
                    var ifrm = document.createElement("iframe");
                    var src = kc.createLoginUrl({prompt: 'none', redirectUri: kc.silentCheckSsoRedirectUri});
                    ifrm.setAttribute("src", src);
                    ifrm.setAttribute("title", "keycloak-silent-check-sso");
                    ifrm.style.display = "none";
                    document.body.appendChild(ifrm);

                    var messageCallback = function(event) {
                        if (event.origin !== window.location.origin || ifrm.contentWindow !== event.source) {
                            return;
                        }

                        var oauth = parseCallback(event.data);
                        processCallback(oauth, initPromise);

                        document.body.removeChild(ifrm);
                        window.removeEventListener("message", messageCallback);
                    };

                    window.addEventListener("message", messageCallback);
                };

                var options = {};
                switch (initOptions.onLoad) {
                    case 'check-sso':
                        if (loginIframe.enable) {
                            setupCheckLoginIframe().then(function() {
                                checkLoginIframe().then(function (unchanged) {
                                    if (!unchanged) {
                                        kc.silentCheckSsoRedirectUri ? checkSsoSilently() : doLogin(false);
                                    } else {
                                        initPromise.setSuccess();
                                    }
                                }).catch(function () {
                                    initPromise.setError();
                                });
                            });
                        } else {
                            kc.silentCheckSsoRedirectUri ? checkSsoSilently() : doLogin(false);
                        }
                        break;
                    case 'login-required':
                        doLogin(true);
                        break;
                    default:
                        throw 'Invalid value for onLoad';
                }
            }

            function processInit() {
                var callback = parseCallback(window.location.href);

                if (callback) {
                    window.history.replaceState(window.history.state, null, callback.newUrl);
                }

                if (callback && callback.valid) {
                    return setupCheckLoginIframe().then(function() {
                        processCallback(callback, initPromise);
                    }).catch(function (e) {
                        initPromise.setError();
                    });
                } else if (initOptions) {
                    if (initOptions.token && initOptions.refreshToken) {
                        setToken(initOptions.token, initOptions.refreshToken, initOptions.idToken);

                        if (loginIframe.enable) {
                            setupCheckLoginIframe().then(function() {
                                checkLoginIframe().then(function (unchanged) {
                                    if (unchanged) {
                                        kc.onAuthSuccess && kc.onAuthSuccess();
                                        initPromise.setSuccess();
                                        scheduleCheckIframe();
                                    } else {
                                        initPromise.setSuccess();
                                    }
                                }).catch(function () {
                                    initPromise.setError();
                                });
                            });
                        } else {
                            kc.updateToken(-1).then(function() {
                                kc.onAuthSuccess && kc.onAuthSuccess();
                                initPromise.setSuccess();
                            }).catch(function() {
                                kc.onAuthError && kc.onAuthError();
                                if (initOptions.onLoad) {
                                    onLoad();
                                } else {
                                    initPromise.setError();
                                }
                            });
                        }
                    } else if (initOptions.onLoad) {
                        onLoad();
                    } else {
                        initPromise.setSuccess();
                    }
                } else {
                    initPromise.setSuccess();
                }
            }

            configPromise.then(processInit);
            configPromise.catch(function() {
                promise.setError();
            });

            return promise.promise;
        }

        kc.login = function (options) {
            return adapter.login(options);
        }

        function generateRandomData(len) {
            // use web crypto APIs if possible
            var array = null;
            var crypto = window.crypto || window.msCrypto;
            if (crypto && crypto.getRandomValues && window.Uint8Array) {
                array = new Uint8Array(len);
                crypto.getRandomValues(array);
                return array;
            }

            // fallback to Math random
            array = new Array(len);
            for (var j = 0; j < array.length; j++) {
                array[j] = Math.floor(256 * Math.random());
            }
            return array;
        }

        function generateCodeVerifier(len) {
            return generateRandomString(len, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789');
        }

        function generateRandomString(len, alphabet){
            var randomData = generateRandomData(len);
            var chars = new Array(len);
            for (var i = 0; i < len; i++) {
                chars[i] = alphabet.charCodeAt(randomData[i] % alphabet.length);
            }
            return String.fromCharCode.apply(null, chars);
        }

        function generatePkceChallenge(pkceMethod, codeVerifier) {
            switch (pkceMethod) {
                // The use of the "plain" method is considered insecure and therefore not supported.
                case "S256":
                    // hash codeVerifier, then encode as url-safe base64 without padding
                    var hashBytes = new Uint8Array(sha256_imported.arrayBuffer(codeVerifier));
                    var encodedHash = base64js_imported.fromByteArray(hashBytes)
                        .replace(/\+/g, '-')
                        .replace(/\//g, '_')
                        .replace(/\=/g, '');
                    return encodedHash;
                default:
                    throw 'Invalid value for pkceMethod';
            }
        }

        kc.createLoginUrl = function(options) {
            var state = createUUID();
            var nonce = createUUID();

            var redirectUri = adapter.redirectUri(options);

            var callbackState = {
                state: state,
                nonce: nonce,
                redirectUri: encodeURIComponent(redirectUri)
            };

            if (options && options.prompt) {
                callbackState.prompt = options.prompt;
            }

            var baseUrl;
            if (options && options.action == 'register') {
                baseUrl = kc.endpoints.register();
            } else {
                baseUrl = kc.endpoints.authorize();
            }

            var scope;
            if (options && options.scope) {
                if (options.scope.indexOf("openid") != -1) {
                    scope = options.scope;
                } else {
                    scope = "openid " + options.scope;
                }
            } else {
                scope = "openid";
            }

            var url = baseUrl
                + '?client_id=' + encodeURIComponent(kc.clientId)
                + '&redirect_uri=' + encodeURIComponent(redirectUri)
                + '&state=' + encodeURIComponent(state)
                + '&response_mode=' + encodeURIComponent(kc.responseMode)
                + '&response_type=' + encodeURIComponent(kc.responseType)
                + '&scope=' + encodeURIComponent(scope);
            if (useNonce) {
                url = url + '&nonce=' + encodeURIComponent(nonce);
            }

            if (options && options.prompt) {
                url += '&prompt=' + encodeURIComponent(options.prompt);
            }

            if (options && options.maxAge) {
                url += '&max_age=' + encodeURIComponent(options.maxAge);
            }

            if (options && options.loginHint) {
                url += '&login_hint=' + encodeURIComponent(options.loginHint);
            }

            if (options && options.idpHint) {
                url += '&kc_idp_hint=' + encodeURIComponent(options.idpHint);
            }

            if (options && options.action && options.action != 'register') {
                url += '&kc_action=' + encodeURIComponent(options.action);
            }

            if (options && options.locale) {
                url += '&ui_locales=' + encodeURIComponent(options.locale);
            }

            if (kc.pkceMethod) {
                var codeVerifier = generateCodeVerifier(96);
                callbackState.pkceCodeVerifier = codeVerifier;
                var pkceChallenge = generatePkceChallenge(kc.pkceMethod, codeVerifier);
                url += '&code_challenge=' + pkceChallenge;
                url += '&code_challenge_method=' + kc.pkceMethod;
            }

            callbackStorage.add(callbackState);

            return url;
        }

        kc.logout = function(options) {
            return adapter.logout(options);
        }

        kc.createLogoutUrl = function(options) {
            var url = kc.endpoints.logout()
                + '?redirect_uri=' + encodeURIComponent(adapter.redirectUri(options, false));

            return url;
        }

        kc.register = function (options) {
            return adapter.register(options);
        }

        kc.createRegisterUrl = function(options) {
            if (!options) {
                options = {};
            }
            options.action = 'register';
            return kc.createLoginUrl(options);
        }

        kc.createAccountUrl = function(options) {
            var realm = getRealmUrl();
            var url = undefined;
            if (typeof realm !== 'undefined') {
                url = realm
                + '/account'
                + '?referrer=' + encodeURIComponent(kc.clientId)
                + '&referrer_uri=' + encodeURIComponent(adapter.redirectUri(options));
            }
            return url;
        }

        kc.accountManagement = function() {
            return adapter.accountManagement();
        }

        kc.hasRealmRole = function (role) {
            var access = kc.realmAccess;
            return !!access && access.roles.indexOf(role) >= 0;
        }

        kc.hasResourceRole = function(role, resource) {
            if (!kc.resourceAccess) {
                return false;
            }

            var access = kc.resourceAccess[resource || kc.clientId];
            return !!access && access.roles.indexOf(role) >= 0;
        }

        kc.loadUserProfile = function() {
            var url = getRealmUrl() + '/account';
            var req = new XMLHttpRequest();
            req.open('GET', url, true);
            req.setRequestHeader('Accept', 'application/json');
            req.setRequestHeader('Authorization', 'bearer ' + kc.token);

            var promise = createPromise();

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        kc.profile = JSON.parse(req.responseText);
                        promise.setSuccess(kc.profile);
                    } else {
                        promise.setError();
                    }
                }
            }

            req.send();

            return promise.promise;
        }

        kc.loadUserInfo = function() {
            var url = kc.endpoints.userinfo();
            var req = new XMLHttpRequest();
            req.open('GET', url, true);
            req.setRequestHeader('Accept', 'application/json');
            req.setRequestHeader('Authorization', 'bearer ' + kc.token);

            var promise = createPromise();

            req.onreadystatechange = function () {
                if (req.readyState == 4) {
                    if (req.status == 200) {
                        kc.userInfo = JSON.parse(req.responseText);
                        promise.setSuccess(kc.userInfo);
                    } else {
                        promise.setError();
                    }
                }
            }

            req.send();

            return promise.promise;
        }

        kc.isTokenExpired = function(minValidity) {
            if (!kc.tokenParsed || (!kc.refreshToken && kc.flow != 'implicit' )) {
                throw 'Not authenticated';
            }

            if (kc.timeSkew == null) {
                logInfo('[KEYCLOAK] Unable to determine if token is expired as timeskew is not set');
                return true;
            }

            var expiresIn = kc.tokenParsed['exp'] - Math.ceil(new Date().getTime() / 1000) + kc.timeSkew;
            if (minValidity) {
                if (isNaN(minValidity)) {
                    throw 'Invalid minValidity';
                }
                expiresIn -= minValidity;
            }
            return expiresIn < 0;
        }

        kc.updateToken = function(minValidity) {
            var promise = createPromise();

            if (!kc.refreshToken) {
                promise.setError();
                return promise.promise;
            }

            minValidity = minValidity || 5;

            var exec = function() {
                var refreshToken = false;
                if (minValidity == -1) {
                    refreshToken = true;
                    logInfo('[KEYCLOAK] Refreshing token: forced refresh');
                } else if (!kc.tokenParsed || kc.isTokenExpired(minValidity)) {
                    refreshToken = true;
                    logInfo('[KEYCLOAK] Refreshing token: token expired');
                }

                if (!refreshToken) {
                    promise.setSuccess(false);
                } else {
                    var params = 'grant_type=refresh_token&' + 'refresh_token=' + kc.refreshToken;
                    var url = kc.endpoints.token();

                    refreshQueue.push(promise);

                    if (refreshQueue.length == 1) {
                        var req = new XMLHttpRequest();
                        req.open('POST', url, true);
                        req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
                        req.withCredentials = true;

                        params += '&client_id=' + encodeURIComponent(kc.clientId);

                        var timeLocal = new Date().getTime();

                        req.onreadystatechange = function () {
                            if (req.readyState == 4) {
                                if (req.status == 200) {
                                    logInfo('[KEYCLOAK] Token refreshed');

                                    timeLocal = (timeLocal + new Date().getTime()) / 2;

                                    var tokenResponse = JSON.parse(req.responseText);

                                    setToken(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], timeLocal);

                                    kc.onAuthRefreshSuccess && kc.onAuthRefreshSuccess();
                                    for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
                                        p.setSuccess(true);
                                    }
                                } else {
                                    logWarn('[KEYCLOAK] Failed to refresh token');

                                    if (req.status == 400) {
                                        kc.clearToken();
                                    }

                                    kc.onAuthRefreshError && kc.onAuthRefreshError();
                                    for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
                                        p.setError(true);
                                    }
                                }
                            }
                        };

                        req.send(params);
                    }
                }
            }

            if (loginIframe.enable) {
                var iframePromise = checkLoginIframe();
                iframePromise.then(function() {
                    exec();
                }).catch(function() {
                    promise.setError();
                });
            } else {
                exec();
            }

            return promise.promise;
        }

        kc.clearToken = function() {
            if (kc.token) {
                setToken(null, null, null);
                kc.onAuthLogout && kc.onAuthLogout();
                if (kc.loginRequired) {
                    kc.login();
                }
            }
        }

        function getRealmUrl() {
            if (typeof kc.authServerUrl !== 'undefined') {
                if (kc.authServerUrl.charAt(kc.authServerUrl.length - 1) == '/') {
                    return kc.authServerUrl + 'realms/' + encodeURIComponent(kc.realm);
                } else {
                    return kc.authServerUrl + '/realms/' + encodeURIComponent(kc.realm);
                }
            } else {
            	return undefined;
            }
        }

        function getOrigin() {
            if (!window.location.origin) {
                return window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
            } else {
                return window.location.origin;
            }
        }

        function processCallback(oauth, promise) {
            var code = oauth.code;
            var error = oauth.error;
            var prompt = oauth.prompt;

            var timeLocal = new Date().getTime();

            if (oauth['kc_action_status']) {
                kc.onActionUpdate && kc.onActionUpdate(oauth['kc_action_status']);
            }

            if (error) {
                if (prompt != 'none') {
                    var errorData = { error: error, error_description: oauth.error_description };
                    kc.onAuthError && kc.onAuthError(errorData);
                    promise && promise.setError(errorData);
                } else {
                    promise && promise.setSuccess();
                }
                return;
            } else if ((kc.flow != 'standard') && (oauth.access_token || oauth.id_token)) {
                authSuccess(oauth.access_token, null, oauth.id_token, true);
            }

            if ((kc.flow != 'implicit') && code) {
                var params = 'code=' + code + '&grant_type=authorization_code';
                var url = kc.endpoints.token();

                var req = new XMLHttpRequest();
                req.open('POST', url, true);
                req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

                params += '&client_id=' + encodeURIComponent(kc.clientId);
                params += '&redirect_uri=' + oauth.redirectUri;

                if (oauth.pkceCodeVerifier) {
                    params += '&code_verifier=' + oauth.pkceCodeVerifier;
                }

                req.withCredentials = true;

                req.onreadystatechange = function() {
                    if (req.readyState == 4) {
                        if (req.status == 200) {

                            var tokenResponse = JSON.parse(req.responseText);
                            authSuccess(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], kc.flow === 'standard');
                            scheduleCheckIframe();
                        } else {
                            kc.onAuthError && kc.onAuthError();
                            promise && promise.setError();
                        }
                    }
                };

                req.send(params);
            }

            function authSuccess(accessToken, refreshToken, idToken, fulfillPromise) {
                timeLocal = (timeLocal + new Date().getTime()) / 2;

                setToken(accessToken, refreshToken, idToken, timeLocal);

                if (useNonce && ((kc.tokenParsed && kc.tokenParsed.nonce != oauth.storedNonce) ||
                    (kc.refreshTokenParsed && kc.refreshTokenParsed.nonce != oauth.storedNonce) ||
                    (kc.idTokenParsed && kc.idTokenParsed.nonce != oauth.storedNonce))) {

                    logInfo('[KEYCLOAK] Invalid nonce, clearing token');
                    kc.clearToken();
                    promise && promise.setError();
                } else {
                    if (fulfillPromise) {
                        kc.onAuthSuccess && kc.onAuthSuccess();
                        promise && promise.setSuccess();
                    }
                }
            }

        }

        function loadConfig(url) {
            var promise = createPromise();
            var configUrl;

            if (!config) {
                configUrl = 'keycloak.json';
            } else if (typeof config === 'string') {
                configUrl = config;
            }

            function setupOidcEndoints(oidcConfiguration) {
                if (! oidcConfiguration) {
                    kc.endpoints = {
                        authorize: function() {
                            return getRealmUrl() + '/protocol/openid-connect/auth';
                        },
                        token: function() {
                            return getRealmUrl() + '/protocol/openid-connect/token';
                        },
                        logout: function() {
                            return getRealmUrl() + '/protocol/openid-connect/logout';
                        },
                        checkSessionIframe: function() {
                            var src = getRealmUrl() + '/protocol/openid-connect/login-status-iframe.html';
                            if (kc.iframeVersion) {
                              src = src + '?version=' + kc.iframeVersion;
                            }
                            return src;
                        },
                        register: function() {
                            return getRealmUrl() + '/protocol/openid-connect/registrations';
                        },
                        userinfo: function() {
                            return getRealmUrl() + '/protocol/openid-connect/userinfo';
                        }
                    };
                } else {
                    kc.endpoints = {
                        authorize: function() {
                            return oidcConfiguration.authorization_endpoint;
                        },
                        token: function() {
                            return oidcConfiguration.token_endpoint;
                        },
                        logout: function() {
                            if (!oidcConfiguration.end_session_endpoint) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.end_session_endpoint;
                        },
                        checkSessionIframe: function() {
                            if (!oidcConfiguration.check_session_iframe) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.check_session_iframe;
                        },
                        register: function() {
                            throw 'Redirection to "Register user" page not supported in standard OIDC mode';
                        },
                        userinfo: function() {
                            if (!oidcConfiguration.userinfo_endpoint) {
                                throw "Not supported by the OIDC server";
                            }
                            return oidcConfiguration.userinfo_endpoint;
                        }
                    }
                }
            }

            if (configUrl) {
                var req = new XMLHttpRequest();
                req.open('GET', configUrl, true);
                req.setRequestHeader('Accept', 'application/json');

                req.onreadystatechange = function () {
                    if (req.readyState == 4) {
                        if (req.status == 200 || fileLoaded(req)) {
                            var config = JSON.parse(req.responseText);

                            kc.authServerUrl = config['auth-server-url'];
                            kc.realm = config['realm'];
                            kc.clientId = config['resource'];
                            setupOidcEndoints(null);
                            promise.setSuccess();
                        } else {
                            promise.setError();
                        }
                    }
                };

                req.send();
            } else {
                if (!config.clientId) {
                    throw 'clientId missing';
                }

                kc.clientId = config.clientId;

                var oidcProvider = config['oidcProvider'];
                if (!oidcProvider) {
                    if (!config['url']) {
                        var scripts = document.getElementsByTagName('script');
                        for (var i = 0; i < scripts.length; i++) {
                            if (scripts[i].src.match(/.*keycloak\.js/)) {
                                config.url = scripts[i].src.substr(0, scripts[i].src.indexOf('/js/keycloak.js'));
                                break;
                            }
                        }
                    }
                    if (!config.realm) {
                        throw 'realm missing';
                    }

                    kc.authServerUrl = config.url;
                    kc.realm = config.realm;
                    setupOidcEndoints(null);
                    promise.setSuccess();
                } else {
                    if (typeof oidcProvider === 'string') {
                        var oidcProviderConfigUrl;
                        if (oidcProvider.charAt(oidcProvider.length - 1) == '/') {
                            oidcProviderConfigUrl = oidcProvider + '.well-known/openid-configuration';
                        } else {
                            oidcProviderConfigUrl = oidcProvider + '/.well-known/openid-configuration';
                        }
                        var req = new XMLHttpRequest();
                        req.open('GET', oidcProviderConfigUrl, true);
                        req.setRequestHeader('Accept', 'application/json');

                        req.onreadystatechange = function () {
                            if (req.readyState == 4) {
                                if (req.status == 200 || fileLoaded(req)) {
                                    var oidcProviderConfig = JSON.parse(req.responseText);
                                    setupOidcEndoints(oidcProviderConfig);
                                    promise.setSuccess();
                                } else {
                                    promise.setError();
                                }
                            }
                        };

                        req.send();
                    } else {
                        setupOidcEndoints(oidcProvider);
                        promise.setSuccess();
                    }
                }
            }

            return promise.promise;
        }

        function fileLoaded(xhr) {
            return xhr.status == 0 && xhr.responseText && xhr.responseURL.startsWith('file:');
        }

        function setToken(token, refreshToken, idToken, timeLocal) {
            if (kc.tokenTimeoutHandle) {
                clearTimeout(kc.tokenTimeoutHandle);
                kc.tokenTimeoutHandle = null;
            }

            if (refreshToken) {
                kc.refreshToken = refreshToken;
                kc.refreshTokenParsed = decodeToken(refreshToken);
            } else {
                delete kc.refreshToken;
                delete kc.refreshTokenParsed;
            }

            if (idToken) {
                kc.idToken = idToken;
                kc.idTokenParsed = decodeToken(idToken);
            } else {
                delete kc.idToken;
                delete kc.idTokenParsed;
            }

            if (token) {
                kc.token = token;
                kc.tokenParsed = decodeToken(token);
                kc.sessionId = kc.tokenParsed.session_state;
                kc.authenticated = true;
                kc.subject = kc.tokenParsed.sub;
                kc.realmAccess = kc.tokenParsed.realm_access;
                kc.resourceAccess = kc.tokenParsed.resource_access;

                if (timeLocal) {
                    kc.timeSkew = Math.floor(timeLocal / 1000) - kc.tokenParsed.iat;
                }

                if (kc.timeSkew != null) {
                    logInfo('[KEYCLOAK] Estimated time difference between browser and server is ' + kc.timeSkew + ' seconds');

                    if (kc.onTokenExpired) {
                        var expiresIn = (kc.tokenParsed['exp'] - (new Date().getTime() / 1000) + kc.timeSkew) * 1000;
                        logInfo('[KEYCLOAK] Token expires in ' + Math.round(expiresIn / 1000) + ' s');
                        if (expiresIn <= 0) {
                            kc.onTokenExpired();
                        } else {
                            kc.tokenTimeoutHandle = setTimeout(kc.onTokenExpired, expiresIn);
                        }
                    }
                }
            } else {
                delete kc.token;
                delete kc.tokenParsed;
                delete kc.subject;
                delete kc.realmAccess;
                delete kc.resourceAccess;

                kc.authenticated = false;
            }
        }

        function decodeToken(str) {
            str = str.split('.')[1];

            str = str.replace('/-/g', '+');
            str = str.replace('/_/g', '/');
            switch (str.length % 4)
            {
                case 0:
                    break;
                case 2:
                    str += '==';
                    break;
                case 3:
                    str += '=';
                    break;
                default:
                    throw 'Invalid token';
            }

            str = (str + '===').slice(0, str.length + (str.length % 4));
            str = str.replace(/-/g, '+').replace(/_/g, '/');

            str = decodeURIComponent(escape(atob(str)));

            str = JSON.parse(str);
            return str;
        }

        function createUUID() {
            var hexDigits = '0123456789abcdef';
            var s = generateRandomString(36, hexDigits).split("");
            s[14] = '4';
            s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
            s[8] = s[13] = s[18] = s[23] = '-';
            var uuid = s.join('');
            return uuid;
        }

        function parseCallback(url) {
            var oauth = parseCallbackUrl(url);
            if (!oauth) {
                return;
            }

            var oauthState = callbackStorage.get(oauth.state);

            if (oauthState) {
                oauth.valid = true;
                oauth.redirectUri = oauthState.redirectUri;
                oauth.storedNonce = oauthState.nonce;
                oauth.prompt = oauthState.prompt;
                oauth.pkceCodeVerifier = oauthState.pkceCodeVerifier;
            }

            return oauth;
        }

        function parseCallbackUrl(url) {
            var supportedParams;
            switch (kc.flow) {
                case 'standard':
                    supportedParams = ['code', 'state', 'session_state', 'kc_action_status'];
                    break;
                case 'implicit':
                    supportedParams = ['access_token', 'token_type', 'id_token', 'state', 'session_state', 'expires_in', 'kc_action_status'];
                    break;
                case 'hybrid':
                    supportedParams = ['access_token', 'id_token', 'code', 'state', 'session_state', 'kc_action_status'];
                    break;
            }

            supportedParams.push('error');
            supportedParams.push('error_description');
            supportedParams.push('error_uri');

            var queryIndex = url.indexOf('?');
            var fragmentIndex = url.indexOf('#');

            var newUrl;
            var parsed;

            if (kc.responseMode === 'query' && queryIndex !== -1) {
                newUrl = url.substring(0, queryIndex);
                parsed = parseCallbackParams(url.substring(queryIndex + 1, fragmentIndex !== -1 ? fragmentIndex : url.length), supportedParams);
                if (parsed.paramsString !== '') {
                    newUrl += '?' + parsed.paramsString;
                }
                if (fragmentIndex !== -1) {
                    newUrl += url.substring(fragmentIndex);
                }
            } else if (kc.responseMode === 'fragment' && fragmentIndex !== -1) {
                newUrl = url.substring(0, fragmentIndex);
                parsed = parseCallbackParams(url.substring(fragmentIndex + 1), supportedParams);
                if (parsed.paramsString !== '') {
                    newUrl += '#' + parsed.paramsString;
                }
            }

            if (parsed && parsed.oauthParams) {
                if (kc.flow === 'standard' || kc.flow === 'hybrid') {
                    if ((parsed.oauthParams.code || parsed.oauthParams.error) && parsed.oauthParams.state) {
                        parsed.oauthParams.newUrl = newUrl;
                        return parsed.oauthParams;
                    }
                } else if (kc.flow === 'implicit') {
                    if ((parsed.oauthParams.access_token || parsed.oauthParams.error) && parsed.oauthParams.state) {
                        parsed.oauthParams.newUrl = newUrl;
                        return parsed.oauthParams;
                    }
                }
            }
        }

        function parseCallbackParams(paramsString, supportedParams) {
            var p = paramsString.split('&');
            var result = {
                paramsString: '',
                oauthParams: {}
            }
            for (var i = 0; i < p.length; i++) {
                var split = p[i].indexOf("=");
                var key = p[i].slice(0, split);
                if (supportedParams.indexOf(key) !== -1) {
                    result.oauthParams[key] = p[i].slice(split + 1);
                } else {
                    if (result.paramsString !== '') {
                        result.paramsString += '&';
                    }
                    result.paramsString += p[i];
                }
            }
            return result;
        }

        function createPromise() {
            // Need to create a native Promise which also preserves the
            // interface of the custom promise type previously used by the API
            var p = {
                setSuccess: function(result) {
                    p.resolve(result);
                },

                setError: function(result) {
                    p.reject(result);
                }
            };
            p.promise = new KeycloakPromise(function(resolve, reject) {
                p.resolve = resolve;
                p.reject = reject;
            });
            return p;
        }


        function setupCheckLoginIframe() {
            var promise = createPromise();

            if (!loginIframe.enable) {
                promise.setSuccess();
                return promise.promise;
            }

            if (loginIframe.iframe) {
                promise.setSuccess();
                return promise.promise;
            }

            var iframe = document.createElement('iframe');
            loginIframe.iframe = iframe;

            iframe.onload = function() {
                var authUrl = kc.endpoints.authorize();
                if (authUrl.charAt(0) === '/') {
                    loginIframe.iframeOrigin = getOrigin();
                } else {
                    loginIframe.iframeOrigin = authUrl.substring(0, authUrl.indexOf('/', 8));
                }
                promise.setSuccess();
            }

            var src = kc.endpoints.checkSessionIframe();
            iframe.setAttribute('src', src );
            iframe.setAttribute('title', 'keycloak-session-iframe' );
            iframe.style.display = 'none';
            document.body.appendChild(iframe);

            var messageCallback = function(event) {
                if ((event.origin !== loginIframe.iframeOrigin) || (loginIframe.iframe.contentWindow !== event.source)) {
                    return;
                }

                if (!(event.data == 'unchanged' || event.data == 'changed' || event.data == 'error')) {
                    return;
                }


                if (event.data != 'unchanged') {
                    kc.clearToken();
                }

                var callbacks = loginIframe.callbackList.splice(0, loginIframe.callbackList.length);

                for (var i = callbacks.length - 1; i >= 0; --i) {
                    var promise = callbacks[i];
                    if (event.data == 'error') {
                        promise.setError();
                    } else {
                        promise.setSuccess(event.data == 'unchanged');
                    }
                }
            };

            window.addEventListener('message', messageCallback, false);

            return promise.promise;
        }

        function scheduleCheckIframe() {
            if (loginIframe.enable) {
                if (kc.token) {
                    setTimeout(function() {
                        checkLoginIframe().then(function(unchanged) {
                            if (unchanged) {
                                scheduleCheckIframe();
                            }
                        });
                    }, loginIframe.interval * 1000);
                }
            }
        }

        function checkLoginIframe() {
            var promise = createPromise();

            if (loginIframe.iframe && loginIframe.iframeOrigin ) {
                var msg = kc.clientId + ' ' + (kc.sessionId ? kc.sessionId : '');
                loginIframe.callbackList.push(promise);
                var origin = loginIframe.iframeOrigin;
                if (loginIframe.callbackList.length == 1) {
                    loginIframe.iframe.contentWindow.postMessage(msg, origin);
                }
            } else {
                promise.setSuccess();
            }

            return promise.promise;
        }

        function loadAdapter(type) {
            if (!type || type == 'default') {
                return {
                    login: function(options) {
                        window.location.replace(kc.createLoginUrl(options));
                        return createPromise().promise;
                    },

                    logout: function(options) {
                        window.location.replace(kc.createLogoutUrl(options));
                        return createPromise().promise;
                    },

                    register: function(options) {
                        window.location.replace(kc.createRegisterUrl(options));
                        return createPromise().promise;
                    },

                    accountManagement : function() {
                        var accountUrl = kc.createAccountUrl();
                        if (typeof accountUrl !== 'undefined') {
                            window.location.href = accountUrl;
                        } else {
                            throw "Not supported by the OIDC server";
                        }
                        return createPromise().promise;
                    },

                    redirectUri: function(options, encodeHash) {
                        if (arguments.length == 1) {
                            encodeHash = true;
                        }

                        if (options && options.redirectUri) {
                            return options.redirectUri;
                        } else if (kc.redirectUri) {
                            return kc.redirectUri;
                        } else {
                            return location.href;
                        }
                    }
                };
            }

            if (type == 'cordova') {
                loginIframe.enable = false;
                var cordovaOpenWindowWrapper = function(loginUrl, target, options) {
                    if (window.cordova && window.cordova.InAppBrowser) {
                        // Use inappbrowser for IOS and Android if available
                        return window.cordova.InAppBrowser.open(loginUrl, target, options);
                    } else {
                        return window.open(loginUrl, target, options);
                    }
                };

                var shallowCloneCordovaOptions = function (userOptions) {
                    if (userOptions && userOptions.cordovaOptions) {
                        return Object.keys(userOptions.cordovaOptions).reduce(function (options, optionName) {
                            options[optionName] = userOptions.cordovaOptions[optionName];
                            return options;
                        }, {});
                    } else {
                        return {};
                    }
                };

                var formatCordovaOptions = function (cordovaOptions) {
                    return Object.keys(cordovaOptions).reduce(function (options, optionName) {
                        options.push(optionName+"="+cordovaOptions[optionName]);
                        return options;
                    }, []).join(",");
                };

                var createCordovaOptions = function (userOptions) {
                    var cordovaOptions = shallowCloneCordovaOptions(userOptions);
                    cordovaOptions.location = 'no';
                    if (userOptions && userOptions.prompt == 'none') {
                        cordovaOptions.hidden = 'yes';
                    }
                    return formatCordovaOptions(cordovaOptions);
                };

                return {
                    login: function(options) {
                        var promise = createPromise();

                        var cordovaOptions = createCordovaOptions(options);
                        var loginUrl = kc.createLoginUrl(options);
                        var ref = cordovaOpenWindowWrapper(loginUrl, '_blank', cordovaOptions);
                        var completed = false;

                        var closed = false;
                        var closeBrowser = function() {
                            closed = true;
                            ref.close();
                        };

                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                var callback = parseCallback(event.url);
                                processCallback(callback, promise);
                                closeBrowser();
                                completed = true;
                            }
                        });

                        ref.addEventListener('loaderror', function(event) {
                            if (!completed) {
                                if (event.url.indexOf('http://localhost') == 0) {
                                    var callback = parseCallback(event.url);
                                    processCallback(callback, promise);
                                    closeBrowser();
                                    completed = true;
                                } else {
                                    promise.setError();
                                    closeBrowser();
                                }
                            }
                        });

                        ref.addEventListener('exit', function(event) {
                            if (!closed) {
                                promise.setError({
                                    reason: "closed_by_user"
                                });
                            }
                        });

                        return promise.promise;
                    },

                    logout: function(options) {
                        var promise = createPromise();

                        var logoutUrl = kc.createLogoutUrl(options);
                        var ref = cordovaOpenWindowWrapper(logoutUrl, '_blank', 'location=no,hidden=yes');

                        var error;

                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                            }
                        });

                        ref.addEventListener('loaderror', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                            } else {
                                error = true;
                                ref.close();
                            }
                        });

                        ref.addEventListener('exit', function(event) {
                            if (error) {
                                promise.setError();
                            } else {
                                kc.clearToken();
                                promise.setSuccess();
                            }
                        });

                        return promise.promise;
                    },

                    register : function(options) {
                        var promise = createPromise();
                        var registerUrl = kc.createRegisterUrl();
                        var cordovaOptions = createCordovaOptions(options);
                        var ref = cordovaOpenWindowWrapper(registerUrl, '_blank', cordovaOptions);
                        ref.addEventListener('loadstart', function(event) {
                            if (event.url.indexOf('http://localhost') == 0) {
                                ref.close();
                                var oauth = parseCallback(event.url);
                                processCallback(oauth, promise);
                            }
                        });
                        return promise.promise;
                    },

                    accountManagement : function() {
                        var accountUrl = kc.createAccountUrl();
                        if (typeof accountUrl !== 'undefined') {
                            var ref = cordovaOpenWindowWrapper(accountUrl, '_blank', 'location=no');
                            ref.addEventListener('loadstart', function(event) {
                                if (event.url.indexOf('http://localhost') == 0) {
                                    ref.close();
                                }
                            });
                        } else {
                            throw "Not supported by the OIDC server";
                        }
                    },

                    redirectUri: function(options) {
                        return 'http://localhost';
                    }
                }
            }

            if (type == 'cordova-native') {
                loginIframe.enable = false;

                return {
                    login: function(options) {
                        var promise = createPromise();
                        var loginUrl = kc.createLoginUrl(options);

                        universalLinks.subscribe('keycloak', function(event) {
                            universalLinks.unsubscribe('keycloak');
                            window.cordova.plugins.browsertab.close();
                            var oauth = parseCallback(event.url);
                            processCallback(oauth, promise);
                        });

                        window.cordova.plugins.browsertab.openUrl(loginUrl);
                        return promise.promise;
                    },

                    logout: function(options) {
                        var promise = createPromise();
                        var logoutUrl = kc.createLogoutUrl(options);

                        universalLinks.subscribe('keycloak', function(event) {
                            universalLinks.unsubscribe('keycloak');
                            window.cordova.plugins.browsertab.close();
                            kc.clearToken();
                            promise.setSuccess();
                        });

                        window.cordova.plugins.browsertab.openUrl(logoutUrl);
                        return promise.promise;
                    },

                    register : function(options) {
                        var promise = createPromise();
                        var registerUrl = kc.createRegisterUrl(options);
                        universalLinks.subscribe('keycloak' , function(event) {
                            universalLinks.unsubscribe('keycloak');
                            window.cordova.plugins.browsertab.close();
                            var oauth = parseCallback(event.url);
                            processCallback(oauth, promise);
                        });
                        window.cordova.plugins.browsertab.openUrl(registerUrl);
                        return promise.promise;

                    },

                    accountManagement : function() {
                        var accountUrl = kc.createAccountUrl();
                        if (typeof accountUrl !== 'undefined') {
                            window.cordova.plugins.browsertab.openUrl(accountUrl);
                        } else {
                            throw "Not supported by the OIDC server";
                        }
                    },

                    redirectUri: function(options) {
                        if (options && options.redirectUri) {
                            return options.redirectUri;
                        } else if (kc.redirectUri) {
                            return kc.redirectUri;
                        } else {
                            return "http://localhost";
                        }
                    }
                }
            }

            throw 'invalid adapter type: ' + type;
        }

        var LocalStorage = function() {
            if (!(this instanceof LocalStorage)) {
                return new LocalStorage();
            }

            localStorage.setItem('kc-test', 'test');
            localStorage.removeItem('kc-test');

            var cs = this;

            function clearExpired() {
                var time = new Date().getTime();
                for (var i = 0; i < localStorage.length; i++)  {
                    var key = localStorage.key(i);
                    if (key && key.indexOf('kc-callback-') == 0) {
                        var value = localStorage.getItem(key);
                        if (value) {
                            try {
                                var expires = JSON.parse(value).expires;
                                if (!expires || expires < time) {
                                    localStorage.removeItem(key);
                                }
                            } catch (err) {
                                localStorage.removeItem(key);
                            }
                        }
                    }
                }
            }

            cs.get = function(state) {
                if (!state) {
                    return;
                }

                var key = 'kc-callback-' + state;
                var value = localStorage.getItem(key);
                if (value) {
                    localStorage.removeItem(key);
                    value = JSON.parse(value);
                }

                clearExpired();
                return value;
            };

            cs.add = function(state) {
                clearExpired();

                var key = 'kc-callback-' + state.state;
                state.expires = new Date().getTime() + (60 * 60 * 1000);
                localStorage.setItem(key, JSON.stringify(state));
            };
        };

        var CookieStorage = function() {
            if (!(this instanceof CookieStorage)) {
                return new CookieStorage();
            }

            var cs = this;

            cs.get = function(state) {
                if (!state) {
                    return;
                }

                var value = getCookie('kc-callback-' + state);
                setCookie('kc-callback-' + state, '', cookieExpiration(-100));
                if (value) {
                    return JSON.parse(value);
                }
            };

            cs.add = function(state) {
                setCookie('kc-callback-' + state.state, JSON.stringify(state), cookieExpiration(60));
            };

            cs.removeItem = function(key) {
                setCookie(key, '', cookieExpiration(-100));
            };

            var cookieExpiration = function (minutes) {
                var exp = new Date();
                exp.setTime(exp.getTime() + (minutes*60*1000));
                return exp;
            };

            var getCookie = function (key) {
                var name = key + '=';
                var ca = document.cookie.split(';');
                for (var i = 0; i < ca.length; i++) {
                    var c = ca[i];
                    while (c.charAt(0) == ' ') {
                        c = c.substring(1);
                    }
                    if (c.indexOf(name) == 0) {
                        return c.substring(name.length, c.length);
                    }
                }
                return '';
            };

            var setCookie = function (key, value, expirationDate) {
                var cookie = key + '=' + value + '; '
                    + 'expires=' + expirationDate.toUTCString() + '; ';
                document.cookie = cookie;
            }
        };

        function createCallbackStorage() {
            try {
                return new LocalStorage();
            } catch (err) {
            }

            return new CookieStorage();
        }

        function createLogger(fn) {
            return function() {
                if (kc.enableLogging) {
                    fn.apply(console, Array.prototype.slice.call(arguments));
                }
            };
        }
    }

    return Keycloak;
})
