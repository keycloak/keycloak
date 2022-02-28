/// <reference types="node" />
import EventEmitter = require('events');
import { Transform as TransformStream } from 'stream';
export declare function createProgressStream(name: 'downloadProgress' | 'uploadProgress', emitter: EventEmitter, totalBytes?: number | string): TransformStream;
