"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const url_1 = require("url");
function validateSearchParams(searchParams) {
    for (const value of Object.values(searchParams)) {
        if (typeof value !== 'string' && typeof value !== 'number' && typeof value !== 'boolean' && value !== null) {
            throw new TypeError(`The \`searchParams\` value '${String(value)}' must be a string, number, boolean or null`);
        }
    }
}
const keys = [
    'protocol',
    'username',
    'password',
    'host',
    'hostname',
    'port',
    'pathname',
    'search',
    'hash'
];
exports.default = (options) => {
    var _a, _b;
    let origin;
    if (options.path) {
        if (options.pathname) {
            throw new TypeError('Parameters `path` and `pathname` are mutually exclusive.');
        }
        if (options.search) {
            throw new TypeError('Parameters `path` and `search` are mutually exclusive.');
        }
        if (options.searchParams) {
            throw new TypeError('Parameters `path` and `searchParams` are mutually exclusive.');
        }
    }
    if (Reflect.has(options, 'auth')) {
        throw new TypeError('Parameter `auth` is deprecated. Use `username` / `password` instead.');
    }
    if (options.search && options.searchParams) {
        throw new TypeError('Parameters `search` and `searchParams` are mutually exclusive.');
    }
    if (options.href) {
        return new url_1.URL(options.href);
    }
    if (options.origin) {
        origin = options.origin;
    }
    else {
        if (!options.protocol) {
            throw new TypeError('No URL protocol specified');
        }
        origin = `${options.protocol}//${_b = (_a = options.hostname, (_a !== null && _a !== void 0 ? _a : options.host)), (_b !== null && _b !== void 0 ? _b : '')}`;
    }
    const url = new url_1.URL(origin);
    if (options.path) {
        const searchIndex = options.path.indexOf('?');
        if (searchIndex === -1) {
            options.pathname = options.path;
        }
        else {
            options.pathname = options.path.slice(0, searchIndex);
            options.search = options.path.slice(searchIndex + 1);
        }
    }
    if (Reflect.has(options, 'path')) {
        delete options.path;
    }
    for (const key of keys) {
        if (Reflect.has(options, key)) {
            url[key] = options[key].toString();
        }
    }
    if (options.searchParams) {
        if (typeof options.searchParams !== 'string' && !(options.searchParams instanceof url_1.URLSearchParams)) {
            validateSearchParams(options.searchParams);
        }
        (new url_1.URLSearchParams(options.searchParams)).forEach((value, key) => {
            url.searchParams.append(key, value);
        });
    }
    return url;
};
