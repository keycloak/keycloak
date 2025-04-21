"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const decompressResponse = require("decompress-response");
const mimicResponse = require("mimic-response");
const stream = require("stream");
const util_1 = require("util");
const progress_1 = require("./progress");
const pipeline = util_1.promisify(stream.pipeline);
exports.default = async (response, options, emitter) => {
    var _a;
    const downloadBodySize = Number(response.headers['content-length']) || undefined;
    const progressStream = progress_1.createProgressStream('downloadProgress', emitter, downloadBodySize);
    mimicResponse(response, progressStream);
    const newResponse = (options.decompress &&
        options.method !== 'HEAD' ? decompressResponse(progressStream) : progressStream);
    if (!options.decompress && ['gzip', 'deflate', 'br'].includes((_a = newResponse.headers['content-encoding'], (_a !== null && _a !== void 0 ? _a : '')))) {
        options.responseType = 'buffer';
    }
    emitter.emit('response', newResponse);
    return pipeline(response, progressStream).catch(error => {
        if (error.code !== 'ERR_STREAM_PREMATURE_CLOSE') {
            throw error;
        }
    });
};
