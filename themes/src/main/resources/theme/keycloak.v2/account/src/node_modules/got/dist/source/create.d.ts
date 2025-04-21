/// <reference types="node" />
import { Merge, Except } from 'type-fest';
import { ProxyStream } from './as-stream';
import * as errors from './errors';
import { CancelableRequest, Defaults, ExtendOptions, HandlerFunction, NormalizedOptions, Options, Response, URLOrOptions, PaginationOptions } from './types';
export declare type HTTPAlias = 'get' | 'post' | 'put' | 'patch' | 'head' | 'delete';
export declare type ReturnStream = <T>(url: string | Merge<Options, {
    isStream?: true;
}>, options?: Merge<Options, {
    isStream?: true;
}>) => ProxyStream<T>;
export declare type GotReturn<T = unknown> = CancelableRequest<T> | ProxyStream<T>;
export declare type OptionsOfDefaultResponseBody = Merge<Options, {
    isStream?: false;
    resolveBodyOnly?: false;
    responseType?: 'default';
}>;
declare type OptionsOfTextResponseBody = Merge<Options, {
    isStream?: false;
    resolveBodyOnly?: false;
    responseType: 'text';
}>;
declare type OptionsOfJSONResponseBody = Merge<Options, {
    isStream?: false;
    resolveBodyOnly?: false;
    responseType: 'json';
}>;
declare type OptionsOfBufferResponseBody = Merge<Options, {
    isStream?: false;
    resolveBodyOnly?: false;
    responseType: 'buffer';
}>;
declare type ResponseBodyOnly = {
    resolveBodyOnly: true;
};
/**
Can be used to match methods explicitly or parameters extraction: `Parameters<GotRequestMethod>`.
*/
export interface GotRequestMethod {
    <T = string>(url: string | OptionsOfDefaultResponseBody, options?: OptionsOfDefaultResponseBody): CancelableRequest<Response<T>>;
    (url: string | OptionsOfTextResponseBody, options?: OptionsOfTextResponseBody): CancelableRequest<Response<string>>;
    <T>(url: string | OptionsOfJSONResponseBody, options?: OptionsOfJSONResponseBody): CancelableRequest<Response<T>>;
    (url: string | OptionsOfBufferResponseBody, options?: OptionsOfBufferResponseBody): CancelableRequest<Response<Buffer>>;
    <T = string>(url: string | Merge<OptionsOfDefaultResponseBody, ResponseBodyOnly>, options?: Merge<OptionsOfDefaultResponseBody, ResponseBodyOnly>): CancelableRequest<T>;
    (url: string | Merge<OptionsOfTextResponseBody, ResponseBodyOnly>, options?: Merge<OptionsOfTextResponseBody, ResponseBodyOnly>): CancelableRequest<string>;
    <T>(url: string | Merge<OptionsOfJSONResponseBody, ResponseBodyOnly>, options?: Merge<OptionsOfJSONResponseBody, ResponseBodyOnly>): CancelableRequest<T>;
    (url: string | Merge<OptionsOfBufferResponseBody, ResponseBodyOnly>, options?: Merge<OptionsOfBufferResponseBody, ResponseBodyOnly>): CancelableRequest<Buffer>;
    <T>(url: string | Merge<Options, {
        isStream: true;
    }>, options?: Merge<Options, {
        isStream: true;
    }>): ProxyStream<T>;
}
export declare type GotPaginateOptions<T> = Except<Options, keyof PaginationOptions<unknown>> & PaginationOptions<T>;
export declare type URLOrGotPaginateOptions<T> = string | GotPaginateOptions<T>;
export interface GotPaginate {
    <T>(url: URLOrGotPaginateOptions<T>, options?: GotPaginateOptions<T>): AsyncIterableIterator<T>;
    all<T>(url: URLOrGotPaginateOptions<T>, options?: GotPaginateOptions<T>): Promise<T[]>;
}
export interface Got extends Record<HTTPAlias, GotRequestMethod>, GotRequestMethod {
    stream: GotStream;
    paginate: GotPaginate;
    defaults: Defaults;
    GotError: typeof errors.GotError;
    CacheError: typeof errors.CacheError;
    RequestError: typeof errors.RequestError;
    ReadError: typeof errors.ReadError;
    ParseError: typeof errors.ParseError;
    HTTPError: typeof errors.HTTPError;
    MaxRedirectsError: typeof errors.MaxRedirectsError;
    UnsupportedProtocolError: typeof errors.UnsupportedProtocolError;
    TimeoutError: typeof errors.TimeoutError;
    CancelError: typeof errors.CancelError;
    extend(...instancesOrOptions: Array<Got | ExtendOptions>): Got;
    mergeInstances(parent: Got, ...instances: Got[]): Got;
    mergeOptions(...sources: Options[]): NormalizedOptions;
}
export interface GotStream extends Record<HTTPAlias, ReturnStream> {
    (url: URLOrOptions, options?: Options): ProxyStream;
}
export declare const defaultHandler: HandlerFunction;
declare const create: (defaults: Defaults) => Got;
export default create;
