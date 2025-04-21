/// <reference types="node" />
import EventEmitter = require('events');
import { ProxyStream } from './as-stream';
import { RequestError, TimeoutError } from './errors';
import { NormalizedOptions } from './types';
export interface RequestAsEventEmitter extends EventEmitter {
    retry: (error: TimeoutError | RequestError) => boolean;
    abort: () => void;
}
declare const _default: (options: NormalizedOptions) => RequestAsEventEmitter;
export default _default;
export declare const proxyEvents: (proxy: EventEmitter | ProxyStream<unknown>, emitter: RequestAsEventEmitter) => void;
