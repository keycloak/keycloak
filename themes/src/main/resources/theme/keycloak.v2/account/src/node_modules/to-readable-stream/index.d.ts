/// <reference types="node"/>
import {Readable as ReadableStream} from 'stream';

declare const toReadableStream: {
	/**
	Convert a `string`/`Buffer`/`Uint8Array` to a [readable stream](https://nodejs.org/api/stream.html#stream_readable_streams).

	@param input - Value to convert to a stream.

	@example
	```
	import toReadableStream = require('to-readable-stream');

	toReadableStream('🦄🌈').pipe(process.stdout);
	```
	*/
	(input: string | Buffer | Uint8Array): ReadableStream;

	// TODO: Remove this for the next major release, refactor the whole definition to:
	// declare function toReadableStream(
	// 	input: string | Buffer | Uint8Array
	// ): ReadableStream;
	// export = toReadableStream;
	default: typeof toReadableStream;
};

export = toReadableStream;
