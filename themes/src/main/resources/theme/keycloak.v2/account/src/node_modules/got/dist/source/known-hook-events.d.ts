import { CancelableRequest, GeneralError, NormalizedOptions, Options, Response } from './types';
/**
Called with plain request options, right before their normalization. This is especially useful in conjunction with `got.extend()` when the input needs custom handling.

**Note:** This hook must be synchronous.

@see [Request migration guide](https://github.com/sindresorhus/got/blob/master/migration-guides.md#breaking-changes) for an example.
*/
export declare type InitHook = (options: Options) => void;
/**
Called with normalized [request options](https://github.com/sindresorhus/got#options). Got will make no further changes to the request before it is sent (except the body serialization). This is especially useful in conjunction with [`got.extend()`](https://github.com/sindresorhus/got#instances) when you want to create an API client that, for example, uses HMAC-signing.

@see [AWS section](https://github.com/sindresorhus/got#aws) for an example.
*/
export declare type BeforeRequestHook = (options: NormalizedOptions) => void | Promise<void>;
/**
Called with normalized [request options](https://github.com/sindresorhus/got#options). Got will make no further changes to the request. This is especially useful when you want to avoid dead sites.
*/
export declare type BeforeRedirectHook = (options: NormalizedOptions, response: Response) => void | Promise<void>;
/**
Called with normalized [request options](https://github.com/sindresorhus/got#options), the error and the retry count. Got will make no further changes to the request. This is especially useful when some extra work is required before the next try.
*/
export declare type BeforeRetryHook = (options: NormalizedOptions, error?: GeneralError, retryCount?: number) => void | Promise<void>;
/**
Called with an `Error` instance. The error is passed to the hook right before it's thrown. This is especially useful when you want to have more detailed errors.

**Note:** Errors thrown while normalizing input options are thrown directly and not part of this hook.
*/
export declare type BeforeErrorHook = <ErrorLike extends GeneralError>(error: ErrorLike) => GeneralError | Promise<GeneralError>;
/**
Called with [response object](https://github.com/sindresorhus/got#response) and a retry function.

Each function should return the response. This is especially useful when you want to refresh an access token.
*/
export declare type AfterResponseHook = (response: Response, retryWithMergedOptions: (options: Options) => CancelableRequest<Response>) => Response | CancelableRequest<Response> | Promise<Response | CancelableRequest<Response>>;
export declare type HookType = BeforeErrorHook | InitHook | BeforeRequestHook | BeforeRedirectHook | BeforeRetryHook | AfterResponseHook;
/**
Hooks allow modifications during the request lifecycle. Hook functions may be async and are run serially.
*/
export interface Hooks {
    /**
    Called with plain request options, right before their normalization. This is especially useful in conjunction with `got.extend()` when the input needs custom handling.

    **Note:** This hook must be synchronous.

    @see [Request migration guide](https://github.com/sindresorhus/got/blob/master/migration-guides.md#breaking-changes) for an example.
    @default []
    */
    init?: InitHook[];
    /**
    Called with normalized [request options](https://github.com/sindresorhus/got#options). Got will make no further changes to the request before it is sent (except the body serialization). This is especially useful in conjunction with [`got.extend()`](https://github.com/sindresorhus/got#instances) when you want to create an API client that, for example, uses HMAC-signing.

    @see [AWS section](https://github.com/sindresorhus/got#aws) for an example.
    @default []
    */
    beforeRequest?: BeforeRequestHook[];
    /**
    Called with normalized [request options](https://github.com/sindresorhus/got#options). Got will make no further changes to the request. This is especially useful when you want to avoid dead sites.

    @default []
    */
    beforeRedirect?: BeforeRedirectHook[];
    /**
    Called with normalized [request options](https://github.com/sindresorhus/got#options), the error and the retry count. Got will make no further changes to the request. This is especially useful when some extra work is required before the next try.

    @default []
    */
    beforeRetry?: BeforeRetryHook[];
    /**
    Called with an `Error` instance. The error is passed to the hook right before it's thrown. This is especially useful when you want to have more detailed errors.

    **Note:** Errors thrown while normalizing input options are thrown directly and not part of this hook.

    @default []
    */
    beforeError?: BeforeErrorHook[];
    /**
    Called with [response object](https://github.com/sindresorhus/got#response) and a retry function.

    Each function should return the response. This is especially useful when you want to refresh an access token.

    @default []
    */
    afterResponse?: AfterResponseHook[];
}
export declare type HookEvent = keyof Hooks;
declare const knownHookEvents: readonly HookEvent[];
export default knownHookEvents;
