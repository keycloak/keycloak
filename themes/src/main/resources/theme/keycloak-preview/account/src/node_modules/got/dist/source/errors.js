"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const is_1 = require("@sindresorhus/is");
class GotError extends Error {
    constructor(message, error, options) {
        super(message);
        Error.captureStackTrace(this, this.constructor);
        this.name = 'GotError';
        if (!is_1.default.undefined(error.code)) {
            this.code = error.code;
        }
        Object.defineProperty(this, 'options', {
            // This fails because of TS 3.7.2 useDefineForClassFields
            // Ref: https://github.com/microsoft/TypeScript/issues/34972
            enumerable: false,
            value: options
        });
        // Recover the original stacktrace
        if (!is_1.default.undefined(error.stack)) {
            const indexOfMessage = this.stack.indexOf(this.message) + this.message.length;
            const thisStackTrace = this.stack.slice(indexOfMessage).split('\n').reverse();
            const errorStackTrace = error.stack.slice(error.stack.indexOf(error.message) + error.message.length).split('\n').reverse();
            // Remove duplicated traces
            while (errorStackTrace.length !== 0 && errorStackTrace[0] === thisStackTrace[0]) {
                thisStackTrace.shift();
            }
            this.stack = `${this.stack.slice(0, indexOfMessage)}${thisStackTrace.reverse().join('\n')}${errorStackTrace.reverse().join('\n')}`;
        }
    }
}
exports.GotError = GotError;
class CacheError extends GotError {
    constructor(error, options) {
        super(error.message, error, options);
        this.name = 'CacheError';
    }
}
exports.CacheError = CacheError;
class RequestError extends GotError {
    constructor(error, options) {
        super(error.message, error, options);
        this.name = 'RequestError';
    }
}
exports.RequestError = RequestError;
class ReadError extends GotError {
    constructor(error, options) {
        super(error.message, error, options);
        this.name = 'ReadError';
    }
}
exports.ReadError = ReadError;
class ParseError extends GotError {
    constructor(error, response, options) {
        super(`${error.message} in "${options.url.toString()}"`, error, options);
        this.name = 'ParseError';
        Object.defineProperty(this, 'response', {
            enumerable: false,
            value: response
        });
    }
}
exports.ParseError = ParseError;
class HTTPError extends GotError {
    constructor(response, options) {
        super(`Response code ${response.statusCode} (${response.statusMessage})`, {}, options);
        this.name = 'HTTPError';
        Object.defineProperty(this, 'response', {
            enumerable: false,
            value: response
        });
    }
}
exports.HTTPError = HTTPError;
class MaxRedirectsError extends GotError {
    constructor(response, maxRedirects, options) {
        super(`Redirected ${maxRedirects} times. Aborting.`, {}, options);
        this.name = 'MaxRedirectsError';
        Object.defineProperty(this, 'response', {
            enumerable: false,
            value: response
        });
    }
}
exports.MaxRedirectsError = MaxRedirectsError;
class UnsupportedProtocolError extends GotError {
    constructor(options) {
        super(`Unsupported protocol "${options.url.protocol}"`, {}, options);
        this.name = 'UnsupportedProtocolError';
    }
}
exports.UnsupportedProtocolError = UnsupportedProtocolError;
class TimeoutError extends GotError {
    constructor(error, timings, options) {
        super(error.message, error, options);
        this.name = 'TimeoutError';
        this.event = error.event;
        this.timings = timings;
    }
}
exports.TimeoutError = TimeoutError;
var p_cancelable_1 = require("p-cancelable");
exports.CancelError = p_cancelable_1.CancelError;
