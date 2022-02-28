declare const got: import("./create").Got;
export default got;
export * from './types';
export { Got, GotStream, ReturnStream, GotRequestMethod, GotReturn } from './create';
export { ProxyStream as ResponseStream } from './as-stream';
export { GotError, CacheError, RequestError, ReadError, ParseError, HTTPError, MaxRedirectsError, UnsupportedProtocolError, TimeoutError, CancelError } from './errors';
export { InitHook, BeforeRequestHook, BeforeRedirectHook, BeforeRetryHook, BeforeErrorHook, AfterResponseHook, HookType, Hooks, HookEvent } from './known-hook-events';
