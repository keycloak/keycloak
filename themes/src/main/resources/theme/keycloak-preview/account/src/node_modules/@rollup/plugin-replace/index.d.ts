import { Plugin } from 'rollup';

type Replacement = string | ((id: string) => string);

export interface RollupReplaceOptions {
	/**
	 * A minimatch pattern, or array of patterns, of files that should be
	 * processed by this plugin (if omitted, all files are included by default)
	 */
	include?: string | RegExp | ReadonlyArray<string | RegExp> | null;
	/**
	 * Files that should be excluded, if `include` is otherwise too permissive.
	 */
	exclude?: string | RegExp | ReadonlyArray<string | RegExp> | null;
	/**
	 * To replace every occurrence of `<@foo@>` instead of every occurrence
	 * of `foo`, supply delimiters
	 */
	delimiters?: [string, string];
	/**
	 * You can separate values to replace from other options.
	 */
	values?: { [str: string]: Replacement };

	/**
	 * All other options are treated as `string: replacement` replacers,
	 * or `string: (id) => replacement` functions.
	 */
	[str: string]: Replacement | RollupReplaceOptions['include'] | RollupReplaceOptions['values'];
}

/**
 * Replace strings in files while bundling them.
 */
export default function replace(options?: RollupReplaceOptions): Plugin;
