declare enum Context {
	POSTS = -2,
	PREPS = -1,
	UNKWN = 0,
	PROPS = 1,
	BLCKS = 2,
	ATRUL = 3
}

interface Stylis {
	new(options?: Options): Stylis
	(namescope: string, input: string): string | any
	set: Set
	use: Use
}

interface Options {
	keyframe?: boolean
	global?: boolean
	cascade?: boolean
	compress?: boolean
	prefix?: boolean | ((key: string, value: string, context: number) => boolean)
	semicolon?: boolean
	preserve?: boolean
}

interface Set {
	(options?: Options): Set
}

interface Plugin {
	(this: Stylis,
		context: Context,
		content: string,
		selector: Selectors,
		parent: Selectors,
		line: number,
		column: number,
		length: number,
		at: number,
		depth: number): any
}

interface Use {
	(plugin?: Array<Plugin> | Plugin | null): Use
}

type Selectors = Array<string>

declare const stylis: Stylis
export = stylis

declare global {
	export const stylis: Stylis
}
