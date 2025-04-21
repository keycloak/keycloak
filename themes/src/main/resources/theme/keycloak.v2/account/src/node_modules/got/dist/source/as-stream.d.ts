/// <reference types="node" />
import { Duplex as DuplexStream } from 'stream';
import { GotEvents, NormalizedOptions } from './types';
export declare class ProxyStream<T = unknown> extends DuplexStream implements GotEvents<ProxyStream<T>> {
    isFromCache?: boolean;
}
export default function asStream<T>(options: NormalizedOptions): ProxyStream<T>;
