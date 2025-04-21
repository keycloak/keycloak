import { Plugin } from 'rollup';

interface RollupCommonJSOptions {
	/**
	 * non-CommonJS modules will be ignored, but you can also
	 * specifically include/exclude files
	 * @default undefined
	 */
	include?: string | RegExp | ReadonlyArray<string | RegExp>,
	/**
	 * non-CommonJS modules will be ignored, but you can also
	 * specifically include/exclude files
	 * @default undefined
	 */
	exclude?: string | RegExp | ReadonlyArray<string | RegExp>,
	/**
	 * search for files other than .js files (must already
	 * be transpiled by a previous plugin!)
	 * @default [ '.js' ]
	 */
	extensions?: ReadonlyArray<string | RegExp>,
	/**
	 * if true then uses of `global` won't be dealt with by this plugin
	 * @default false
	 */
	ignoreGlobal?: boolean,
	/**
	 * if false then skip sourceMap generation for CommonJS modules
	 * @default true
	 */
	sourceMap?: boolean,
	/**
	 * explicitly specify unresolvable named exports
	 * ([see below for more details](https://github.com/rollup/plugins/tree/master/packages/commonjs#named-exports))
	 * @default undefined
	 */
	namedExports?: { [package: string]: ReadonlyArray<string> },
	/**
	 * sometimes you have to leave require statements
	 * unconverted. Pass an array containing the IDs
	 * or a `id => boolean` function. Only use this
	 * option if you know what you're doing!
	 */
	ignore?: ReadonlyArray<string | ((id: string) => boolean)>,
}

/**
 * Convert CommonJS modules to ES6, so they can be included in a Rollup bundle
 */
export default function commonjs(options?: RollupCommonJSOptions): Plugin;
