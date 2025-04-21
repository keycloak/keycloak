"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.css = void 0;
/** Joins args into a className string
 *
 * @param {any} args list of objects, string, or arrays to reduce
 */
function css(...args) {
    // Adapted from https://github.com/JedWatson/classnames/blob/master/index.js
    const classes = [];
    const hasOwn = {}.hasOwnProperty;
    args.filter(Boolean).forEach((arg) => {
        const argType = typeof arg;
        if (argType === 'string' || argType === 'number') {
            classes.push(arg);
        }
        else if (Array.isArray(arg) && arg.length) {
            const inner = css(...arg);
            if (inner) {
                classes.push(inner);
            }
        }
        else if (argType === 'object') {
            for (const key in arg) {
                if (hasOwn.call(arg, key) && arg[key]) {
                    classes.push(key);
                }
            }
        }
    });
    return classes.join(' ');
}
exports.css = css;
//# sourceMappingURL=index.js.map