/// <reference types="node" />
import { ClientRequestArgs } from 'http';
interface Options {
    body?: unknown;
    headers: ClientRequestArgs['headers'];
}
declare const _default: (options: Options) => Promise<number | undefined>;
export default _default;
