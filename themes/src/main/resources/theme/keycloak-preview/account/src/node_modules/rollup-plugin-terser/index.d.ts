import { Plugin } from 'rollup';
import { MinifyOptions } from 'terser';

export interface Options extends Omit<MinifyOptions, "sourceMap"> {

	/**
	 * Specifically include/exclude chunk files names (minimatch pattern, or array of minimatch patterns), By default all chunk files will be minify.
	 */
	include?: Array<string | RegExp> | string | RegExp | null;
	exclude?: Array<string | RegExp> | string | RegExp | null;

	/**
	 * Amount of workers to spawn. Defaults to the number of CPUs minus 1.
	 */
	numWorkers?: number;

	/**
	 * Generates source maps and passes them to rollup.
	 *
	 * @default true
	 */
	sourcemap?: boolean;

}

export declare function terser(options?: Options): Plugin;
