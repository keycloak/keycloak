/// <reference types="node" />
import http = require('http');
import https = require('https');
import stream = require('stream');
import { Merge } from 'type-fest';
import { Defaults, NormalizedOptions, RequestFunction, URLOrOptions, requestSymbol } from './types';
export declare const preNormalizeArguments: (options: Merge<https.RequestOptions, Merge<import("./types").GotOptions, import("./utils/options-to-url").URLOptions>>, defaults?: NormalizedOptions | undefined) => NormalizedOptions;
export declare const mergeOptions: (...sources: Merge<https.RequestOptions, Merge<import("./types").GotOptions, import("./utils/options-to-url").URLOptions>>[]) => NormalizedOptions;
export declare const normalizeArguments: (url: URLOrOptions, options?: Merge<https.RequestOptions, Merge<import("./types").GotOptions, import("./utils/options-to-url").URLOptions>> | undefined, defaults?: Defaults | undefined) => NormalizedOptions;
export declare type NormalizedRequestArguments = Merge<https.RequestOptions, {
    body?: stream.Readable;
    [requestSymbol]: RequestFunction;
    url: Pick<NormalizedOptions, 'url'>;
}>;
export declare const normalizeRequestArguments: (options: NormalizedOptions) => Promise<Merge<https.RequestOptions, {
    body?: stream.Readable | undefined;
    url: Pick<NormalizedOptions, "url">;
    [requestSymbol]: typeof http.request;
}>>;
