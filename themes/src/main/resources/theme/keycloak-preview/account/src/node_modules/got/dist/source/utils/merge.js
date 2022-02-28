"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const url_1 = require("url");
const is_1 = require("@sindresorhus/is");
function merge(target, ...sources) {
    for (const source of sources) {
        for (const [key, sourceValue] of Object.entries(source)) {
            const targetValue = target[key];
            if (is_1.default.urlInstance(targetValue) && is_1.default.string(sourceValue)) {
                // @ts-ignore TS doesn't recognise Target accepts string keys
                target[key] = new url_1.URL(sourceValue, targetValue);
            }
            else if (is_1.default.plainObject(sourceValue)) {
                if (is_1.default.plainObject(targetValue)) {
                    // @ts-ignore TS doesn't recognise Target accepts string keys
                    target[key] = merge({}, targetValue, sourceValue);
                }
                else {
                    // @ts-ignore TS doesn't recognise Target accepts string keys
                    target[key] = merge({}, sourceValue);
                }
            }
            else if (is_1.default.array(sourceValue)) {
                // @ts-ignore TS doesn't recognise Target accepts string keys
                target[key] = sourceValue.slice();
            }
            else {
                // @ts-ignore TS doesn't recognise Target accepts string keys
                target[key] = sourceValue;
            }
        }
    }
    return target;
}
exports.default = merge;
