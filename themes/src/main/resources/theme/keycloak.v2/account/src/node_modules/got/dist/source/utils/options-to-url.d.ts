/// <reference types="node" />
import { URL, URLSearchParams } from 'url';
export interface URLOptions {
    href?: string;
    origin?: string;
    protocol?: string;
    username?: string;
    password?: string;
    host?: string;
    hostname?: string;
    port?: string | number;
    pathname?: string;
    search?: string;
    searchParams?: Record<string, string | number | boolean | null> | URLSearchParams | string;
    hash?: string;
    path?: string;
}
declare const _default: (options: URLOptions) => URL;
export default _default;
