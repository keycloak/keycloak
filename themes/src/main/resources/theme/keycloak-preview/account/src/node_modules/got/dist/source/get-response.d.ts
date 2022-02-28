/// <reference types="node" />
import EventEmitter = require('events');
import { IncomingMessage } from 'http';
import { NormalizedOptions } from './types';
declare const _default: (response: IncomingMessage, options: NormalizedOptions, emitter: EventEmitter) => Promise<void>;
export default _default;
