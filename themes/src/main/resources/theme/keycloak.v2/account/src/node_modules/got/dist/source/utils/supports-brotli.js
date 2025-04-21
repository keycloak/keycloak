"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const zlib = require("zlib");
exports.default = typeof zlib.createBrotliDecompress === 'function';
