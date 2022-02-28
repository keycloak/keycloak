"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const fs_1 = require("fs");
const CacheableRequest = require("cacheable-request");
const EventEmitter = require("events");
const http = require("http");
const stream = require("stream");
const url_1 = require("url");
const util_1 = require("util");
const is_1 = require("@sindresorhus/is");
const http_timer_1 = require("@szmarczak/http-timer");
const calculate_retry_delay_1 = require("./calculate-retry-delay");
const errors_1 = require("./errors");
const get_response_1 = require("./get-response");
const normalize_arguments_1 = require("./normalize-arguments");
const progress_1 = require("./progress");
const timed_out_1 = require("./utils/timed-out");
const types_1 = require("./types");
const url_to_options_1 = require("./utils/url-to-options");
const pEvent = require("p-event");
const setImmediateAsync = async () => new Promise(resolve => setImmediate(resolve));
const pipeline = util_1.promisify(stream.pipeline);
const redirectCodes = new Set([300, 301, 302, 303, 304, 307, 308]);
exports.default = (options) => {
    const emitter = new EventEmitter();
    const requestUrl = options.url.toString();
    const redirects = [];
    let retryCount = 0;
    let currentRequest;
    // `request.aborted` is a boolean since v11.0.0: https://github.com/nodejs/node/commit/4b00c4fafaa2ae8c41c1f78823c0feb810ae4723#diff-e3bc37430eb078ccbafe3aa3b570c91a
    const isAborted = () => typeof currentRequest.aborted === 'number' || currentRequest.aborted;
    const emitError = async (error) => {
        try {
            for (const hook of options.hooks.beforeError) {
                // eslint-disable-next-line no-await-in-loop
                error = await hook(error);
            }
            emitter.emit('error', error);
        }
        catch (error_) {
            emitter.emit('error', error_);
        }
    };
    const get = async () => {
        let httpOptions = await normalize_arguments_1.normalizeRequestArguments(options);
        const handleResponse = async (response) => {
            var _a;
            try {
                /* istanbul ignore next: fixes https://github.com/electron/electron/blob/cbb460d47628a7a146adf4419ed48550a98b2923/lib/browser/api/net.js#L59-L65 */
                if (options.useElectronNet) {
                    response = new Proxy(response, {
                        get: (target, name) => {
                            if (name === 'trailers' || name === 'rawTrailers') {
                                return [];
                            }
                            const value = target[name];
                            return is_1.default.function_(value) ? value.bind(target) : value;
                        }
                    });
                }
                const typedResponse = response;
                const { statusCode } = typedResponse;
                typedResponse.statusMessage = is_1.default.nonEmptyString(typedResponse.statusMessage) ? typedResponse.statusMessage : http.STATUS_CODES[statusCode];
                typedResponse.url = options.url.toString();
                typedResponse.requestUrl = requestUrl;
                typedResponse.retryCount = retryCount;
                typedResponse.redirectUrls = redirects;
                typedResponse.request = { options };
                typedResponse.isFromCache = (_a = typedResponse.fromCache, (_a !== null && _a !== void 0 ? _a : false));
                delete typedResponse.fromCache;
                if (!typedResponse.isFromCache) {
                    typedResponse.ip = response.socket.remoteAddress;
                }
                const rawCookies = typedResponse.headers['set-cookie'];
                if (Reflect.has(options, 'cookieJar') && rawCookies) {
                    let promises = rawCookies.map(async (rawCookie) => options.cookieJar.setCookie(rawCookie, typedResponse.url));
                    if (options.ignoreInvalidCookies) {
                        promises = promises.map(async (p) => p.catch(() => { }));
                    }
                    await Promise.all(promises);
                }
                if (options.followRedirect && Reflect.has(typedResponse.headers, 'location') && redirectCodes.has(statusCode)) {
                    typedResponse.resume(); // We're being redirected, we don't care about the response.
                    // eslint-disable-next-line @typescript-eslint/no-unnecessary-boolean-literal-compare
                    if (statusCode === 303 || options.methodRewriting === false) {
                        if (options.method !== 'GET' && options.method !== 'HEAD') {
                            // Server responded with "see other", indicating that the resource exists at another location,
                            // and the client should request it from that location via GET or HEAD.
                            options.method = 'GET';
                        }
                        if (Reflect.has(options, 'body')) {
                            delete options.body;
                        }
                        if (Reflect.has(options, 'json')) {
                            delete options.json;
                        }
                        if (Reflect.has(options, 'form')) {
                            delete options.form;
                        }
                    }
                    if (redirects.length >= options.maxRedirects) {
                        throw new errors_1.MaxRedirectsError(typedResponse, options.maxRedirects, options);
                    }
                    // Handles invalid URLs. See https://github.com/sindresorhus/got/issues/604
                    const redirectBuffer = Buffer.from(typedResponse.headers.location, 'binary').toString();
                    const redirectUrl = new url_1.URL(redirectBuffer, options.url);
                    // Redirecting to a different site, clear cookies.
                    if (redirectUrl.hostname !== options.url.hostname && Reflect.has(options.headers, 'cookie')) {
                        delete options.headers.cookie;
                    }
                    redirects.push(redirectUrl.toString());
                    options.url = redirectUrl;
                    for (const hook of options.hooks.beforeRedirect) {
                        // eslint-disable-next-line no-await-in-loop
                        await hook(options, typedResponse);
                    }
                    emitter.emit('redirect', response, options);
                    await get();
                    return;
                }
                await get_response_1.default(typedResponse, options, emitter);
            }
            catch (error) {
                emitError(error);
            }
        };
        const handleRequest = async (request) => {
            let isPiped = false;
            let isFinished = false;
            // `request.finished` doesn't indicate whether this has been emitted or not
            request.once('finish', () => {
                isFinished = true;
            });
            currentRequest = request;
            const onError = (error) => {
                if (error instanceof timed_out_1.TimeoutError) {
                    error = new errors_1.TimeoutError(error, request.timings, options);
                }
                else {
                    error = new errors_1.RequestError(error, options);
                }
                if (!emitter.retry(error)) {
                    emitError(error);
                }
            };
            request.on('error', error => {
                if (isPiped) {
                    // Check if it's caught by `stream.pipeline(...)`
                    if (!isFinished) {
                        return;
                    }
                    // We need to let `TimedOutTimeoutError` through, because `stream.pipeline(…)` aborts the request automatically.
                    if (isAborted() && !(error instanceof timed_out_1.TimeoutError)) {
                        return;
                    }
                }
                onError(error);
            });
            try {
                http_timer_1.default(request);
                timed_out_1.default(request, options.timeout, options.url);
                emitter.emit('request', request);
                const uploadStream = progress_1.createProgressStream('uploadProgress', emitter, httpOptions.headers['content-length']);
                isPiped = true;
                await pipeline(httpOptions.body, uploadStream, request);
                request.emit('upload-complete');
            }
            catch (error) {
                if (isAborted() && error.message === 'Premature close') {
                    // The request was aborted on purpose
                    return;
                }
                onError(error);
            }
        };
        if (options.cache) {
            // `cacheable-request` doesn't support Node 10 API, fallback.
            httpOptions = {
                ...httpOptions,
                ...url_to_options_1.default(options.url)
            };
            // @ts-ignore `cacheable-request` has got invalid types
            const cacheRequest = options.cacheableRequest(httpOptions, handleResponse);
            cacheRequest.once('error', (error) => {
                if (error instanceof CacheableRequest.RequestError) {
                    emitError(new errors_1.RequestError(error, options));
                }
                else {
                    emitError(new errors_1.CacheError(error, options));
                }
            });
            cacheRequest.once('request', handleRequest);
        }
        else {
            // Catches errors thrown by calling `requestFn(…)`
            try {
                handleRequest(httpOptions[types_1.requestSymbol](options.url, httpOptions, handleResponse));
            }
            catch (error) {
                emitError(new errors_1.RequestError(error, options));
            }
        }
    };
    emitter.retry = error => {
        let backoff;
        retryCount++;
        try {
            backoff = options.retry.calculateDelay({
                attemptCount: retryCount,
                retryOptions: options.retry,
                error,
                computedValue: calculate_retry_delay_1.default({
                    attemptCount: retryCount,
                    retryOptions: options.retry,
                    error,
                    computedValue: 0
                })
            });
        }
        catch (error_) {
            emitError(error_);
            return false;
        }
        if (backoff) {
            const retry = async (options) => {
                try {
                    for (const hook of options.hooks.beforeRetry) {
                        // eslint-disable-next-line no-await-in-loop
                        await hook(options, error, retryCount);
                    }
                    await get();
                }
                catch (error_) {
                    emitError(error_);
                }
            };
            setTimeout(retry, backoff, { ...options, forceRefresh: true });
            return true;
        }
        return false;
    };
    emitter.abort = () => {
        emitter.prependListener('request', (request) => {
            request.abort();
        });
        if (currentRequest) {
            currentRequest.abort();
        }
    };
    (async () => {
        try {
            if (options.body instanceof fs_1.ReadStream) {
                await pEvent(options.body, 'open');
            }
            // Promises are executed immediately.
            // If there were no `setImmediate` here,
            // `promise.json()` would have no effect
            // as the request would be sent already.
            await setImmediateAsync();
            for (const hook of options.hooks.beforeRequest) {
                // eslint-disable-next-line no-await-in-loop
                await hook(options);
            }
            await get();
        }
        catch (error) {
            emitError(error);
        }
    })();
    return emitter;
};
exports.proxyEvents = (proxy, emitter) => {
    const events = [
        'request',
        'redirect',
        'uploadProgress',
        'downloadProgress'
    ];
    for (const event of events) {
        emitter.on(event, (...args) => {
            proxy.emit(event, ...args);
        });
    }
};
