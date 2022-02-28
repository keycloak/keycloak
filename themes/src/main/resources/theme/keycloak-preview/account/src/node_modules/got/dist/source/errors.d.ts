import { Timings } from '@szmarczak/http-timer';
import { TimeoutError as TimedOutError } from './utils/timed-out';
import { Response, NormalizedOptions } from './types';
export declare class GotError extends Error {
    code?: string;
    stack: string;
    readonly options: NormalizedOptions;
    constructor(message: string, error: Partial<Error & {
        code?: string;
    }>, options: NormalizedOptions);
}
export declare class CacheError extends GotError {
    constructor(error: Error, options: NormalizedOptions);
}
export declare class RequestError extends GotError {
    constructor(error: Error, options: NormalizedOptions);
}
export declare class ReadError extends GotError {
    constructor(error: Error, options: NormalizedOptions);
}
export declare class ParseError extends GotError {
    readonly response: Response;
    constructor(error: Error, response: Response, options: NormalizedOptions);
}
export declare class HTTPError extends GotError {
    readonly response: Response;
    constructor(response: Response, options: NormalizedOptions);
}
export declare class MaxRedirectsError extends GotError {
    readonly response: Response;
    constructor(response: Response, maxRedirects: number, options: NormalizedOptions);
}
export declare class UnsupportedProtocolError extends GotError {
    constructor(options: NormalizedOptions);
}
export declare class TimeoutError extends GotError {
    timings: Timings;
    event: string;
    constructor(error: TimedOutError, timings: Timings, options: NormalizedOptions);
}
export { CancelError } from 'p-cancelable';
