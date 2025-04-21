import Keyv = require('keyv');
import {Resolver, LookupAddress} from 'dns';
import {Agent} from 'http';

type IPFamily = 4 | 6;

export interface Options {
	/**
	 * A Keyv adapter which stores the cache.
	 * @default new Map()
	 */
	cacheAdapter?: Keyv;
	/**
	 * Limits the cache time (TTL). If set to `0`, it will make a new DNS query each time.
	 * @default Infinity
	 */
	maxTtl?: number;
	/**
	 * DNS Resolver used to make DNS queries.
	 * @default new dns.Resolver()
	 */
	resolver?: Resolver;
}

interface EntryObject {
	/**
	 * The IP address (can be an IPv4 or IPv5 address).
	 */
	readonly address: string;
	/**
	 * The IP family.
	 */
	readonly family: IPFamily;
	/**
	 * The original TTL.
	 */
	readonly ttl: number;
	/**
	 * The expiration timestamp.
	 */
	readonly expires: number;
}

interface LookupOptions {
	/**
	 * One or more supported getaddrinfo flags. Multiple flags may be passed by bitwise ORing their values.
	 */
	hints?: number;
	/**
	 * The record family. Must be `4` or `6`. IPv4 and IPv6 addresses are both returned by default.
	 */
	family?: IPFamily;
	/**
	 * When `true`, the callback returns all resolved addresses in an array. Otherwise, returns a single address.
	 * @default false
	 */
	all?: boolean;
	/**
	 * Throw when there's no match. If set to `false` and it gets no match, it will return `undefined`.
	 * @default false
	 */
	throwNotFound?: boolean;
}

export default class CacheableLookup {
	constructor(options?: Options);
	/**
	 * DNS servers used to make the query. Can be overridden - then the new servers will be used.
	 */
	servers: string[];
	/**
	 * @see https://nodejs.org/api/dns.html#dns_dns_lookup_hostname_options_callback
	 */
	lookup(hostname: string, family: IPFamily, callback: (error: NodeJS.ErrnoException, address: string, family: IPFamily) => void): void;
	lookup(hostname: string, callback: (error: NodeJS.ErrnoException, address: string, family: IPFamily) => void): void;
	lookup(hostname: string, options: LookupOptions & {all: true}, callback: (error: NodeJS.ErrnoException, result: ReadonlyArray<EntryObject>) => void): void;
	lookup(hostname: string, options: LookupOptions, callback: (error: NodeJS.ErrnoException, address: string, family: IPFamily) => void): void;
	/**
	 * The asynchronous version of `dns.lookup(…)`.
	 */
	lookupAsync(hostname: string, options: LookupOptions & {all: true}): Promise<ReadonlyArray<EntryObject>>;
	lookupAsync(hostname: string, options: LookupOptions): Promise<EntryObject>;
	lookupAsync(hostname: string): Promise<EntryObject>;
	lookupAsync(hostname: string, family: IPFamily): Promise<EntryObject>;
	/**
	 * An asynchronous function which returns cached DNS lookup entries. This is the base for `lookupAsync(hostname, options)` and `lookup(hostname, options, callback)`.
	 */
	query(hostname: string, family: IPFamily): Promise<ReadonlyArray<EntryObject>>;
	/**
	 * An asynchronous function which makes a new DNS lookup query and updates the database. This is used by `query(hostname, family)` if no entry in the database is present. Returns an array of objects with `address`, `family`, `ttl` and `expires` properties.
	 */
	queryAndCache(hostname: string, family: IPFamily): Promise<ReadonlyArray<EntryObject>>;
	/**
	 * Attaches itself to an Agent instance.
	 */
	install(agent: Agent): void;
	/**
	 * Removes itself from an Agent instance.
	 */
	uninstall(agent: Agent): void;
	/**
	 * Updates interface info. For example, you need to run this when you plug or unplug your WiFi driver.
	 */
	updateInterfaceInfo(): void;
}
