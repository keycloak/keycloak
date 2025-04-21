"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const stream_1 = require("stream");
const is_1 = require("@sindresorhus/is");
function createProgressStream(name, emitter, totalBytes) {
    let transformedBytes = 0;
    if (is_1.default.string(totalBytes)) {
        totalBytes = Number(totalBytes);
    }
    const progressStream = new stream_1.Transform({
        transform(chunk, _encoding, callback) {
            transformedBytes += chunk.length;
            const percent = totalBytes ? transformedBytes / totalBytes : 0;
            // Let `flush()` be responsible for emitting the last event
            if (percent < 1) {
                emitter.emit(name, {
                    percent,
                    transferred: transformedBytes,
                    total: totalBytes
                });
            }
            callback(undefined, chunk);
        },
        flush(callback) {
            emitter.emit(name, {
                percent: 1,
                transferred: transformedBytes,
                total: totalBytes
            });
            callback();
        }
    });
    emitter.emit(name, {
        percent: 0,
        transferred: 0,
        total: totalBytes
    });
    return progressStream;
}
exports.createProgressStream = createProgressStream;
