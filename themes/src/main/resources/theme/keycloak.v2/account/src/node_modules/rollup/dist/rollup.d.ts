import * as ESTree from 'estree';
import { EventEmitter } from 'events';

export const VERSION: string;

export interface RollupError extends RollupLogProps {
	parserError?: Error;
	stack?: string;
	watchFiles?: string[];
}

export interface RollupWarning extends RollupLogProps {
	chunkName?: string;
	cycle?: string[];
	exporter?: string;
	exportName?: string;
	guess?: string;
	importer?: string;
	missing?: string;
	modules?: string[];
	names?: string[];
	reexporter?: string;
	source?: string;
	sources?: string[];
}

export interface RollupLogProps {
	code?: string;
	frame?: string;
	hook?: string;
	id?: string;
	loc?: {
		column: number;
		file?: string;
		line: number;
	};
	message: string;
	name?: string;
	plugin?: string;
	pluginCode?: string;
	pos?: number;
	url?: string;
}

export type SourceMapSegment =
	| [number]
	| [number, number, number, number]
	| [number, number, number, number, number];

export interface ExistingDecodedSourceMap {
	file?: string;
	mappings: SourceMapSegment[][];
	names: string[];
	sourceRoot?: string;
	sources: string[];
	sourcesContent?: string[];
	version: number;
}

export interface ExistingRawSourceMap {
	file?: string;
	mappings: string;
	names: string[];
	sourceRoot?: string;
	sources: string[];
	sourcesContent?: string[];
	version: number;
}

export type DecodedSourceMapOrMissing =
	| {
			mappings?: never;
			missing: true;
			plugin: string;
	  }
	| ExistingDecodedSourceMap;

export interface SourceMap {
	file: string;
	mappings: string;
	names: string[];
	sources: string[];
	sourcesContent: string[];
	version: number;
	toString(): string;
	toUrl(): string;
}

export type SourceMapInput = ExistingRawSourceMap | string | null | { mappings: '' };

export interface SourceDescription {
	ast?: ESTree.Program;
	code: string;
	map?: SourceMapInput;
	moduleSideEffects?: boolean | null;
	syntheticNamedExports?: boolean;
}

export interface TransformSourceDescription extends SourceDescription {
	dependencies?: string[];
}

export interface TransformModuleJSON {
	ast: ESTree.Program;
	code: string;
	// note if plugins use new this.cache to opt-out auto transform cache
	customTransformCache: boolean;
	moduleSideEffects: boolean | null;
	originalCode: string;
	originalSourcemap: ExistingDecodedSourceMap | null;
	resolvedIds?: ResolvedIdMap;
	sourcemapChain: DecodedSourceMapOrMissing[];
	syntheticNamedExports: boolean | null;
	transformDependencies: string[];
}

export interface ModuleJSON extends TransformModuleJSON {
	dependencies: string[];
	id: string;
	transformFiles: EmittedFile[] | undefined;
}

export interface PluginCache {
	delete(id: string): boolean;
	get<T = any>(id: string): T;
	has(id: string): boolean;
	set<T = any>(id: string, value: T): void;
}

export interface MinimalPluginContext {
	meta: PluginContextMeta;
}

export interface EmittedAsset {
	fileName?: string;
	name?: string;
	source?: string | Buffer;
	type: 'asset';
}

export interface EmittedChunk {
	fileName?: string;
	id: string;
	name?: string;
	type: 'chunk';
}

export type EmittedFile = EmittedAsset | EmittedChunk;

export type EmitAsset = (name: string, source?: string | Buffer) => string;

export type EmitChunk = (id: string, options?: { name?: string }) => string;

export type EmitFile = (emittedFile: EmittedFile) => string;

export interface PluginContext extends MinimalPluginContext {
	addWatchFile: (id: string) => void;
	cache: PluginCache;
	/** @deprecated Use `this.emitFile` instead */
	emitAsset: EmitAsset;
	/** @deprecated Use `this.emitFile` instead */
	emitChunk: EmitChunk;
	emitFile: EmitFile;
	error: (err: RollupError | string, pos?: number | { column: number; line: number }) => never;
	/** @deprecated Use `this.getFileName` instead */
	getAssetFileName: (assetReferenceId: string) => string;
	/** @deprecated Use `this.getFileName` instead */
	getChunkFileName: (chunkReferenceId: string) => string;
	getFileName: (fileReferenceId: string) => string;
	getModuleInfo: (
		moduleId: string
	) => {
		hasModuleSideEffects: boolean;
		id: string;
		importedIds: string[];
		isEntry: boolean;
		isExternal: boolean;
	};
	/** @deprecated Use `this.resolve` instead */
	isExternal: IsExternal;
	moduleIds: IterableIterator<string>;
	parse: (input: string, options: any) => ESTree.Program;
	resolve: (
		source: string,
		importer: string,
		options?: { skipSelf: boolean }
	) => Promise<ResolvedId | null>;
	/** @deprecated Use `this.resolve` instead */
	resolveId: (source: string, importer: string) => Promise<string | null>;
	setAssetSource: (assetReferenceId: string, source: string | Buffer) => void;
	warn: (warning: RollupWarning | string, pos?: number | { column: number; line: number }) => void;
	/** @deprecated Use `this.addWatchFile` and the `watchChange` hook instead  */
	watcher: EventEmitter;
}

export interface PluginContextMeta {
	rollupVersion: string;
}

export interface ResolvedId {
	external: boolean;
	id: string;
	moduleSideEffects: boolean;
	syntheticNamedExports: boolean;
}

export interface ResolvedIdMap {
	[key: string]: ResolvedId;
}

interface PartialResolvedId {
	external?: boolean;
	id: string;
	moduleSideEffects?: boolean | null;
	syntheticNamedExports?: boolean;
}

export type ResolveIdResult = string | false | null | undefined | PartialResolvedId;

export type ResolveIdHook = (
	this: PluginContext,
	source: string,
	importer: string | undefined
) => Promise<ResolveIdResult> | ResolveIdResult;

export type IsExternal = (
	source: string,
	importer: string,
	isResolved: boolean
) => boolean | null | undefined;

export type IsPureModule = (id: string) => boolean | null | undefined;

export type HasModuleSideEffects = (id: string, external: boolean) => boolean;

type LoadResult = SourceDescription | string | null | undefined;

export type LoadHook = (this: PluginContext, id: string) => Promise<LoadResult> | LoadResult;

export type TransformResult = string | null | undefined | TransformSourceDescription;

export type TransformHook = (
	this: PluginContext,
	code: string,
	id: string
) => Promise<TransformResult> | TransformResult;

export type TransformChunkHook = (
	this: PluginContext,
	code: string,
	options: OutputOptions
) =>
	| Promise<{ code: string; map?: SourceMapInput } | null | undefined>
	| { code: string; map?: SourceMapInput }
	| null
	| undefined;

export type RenderChunkHook = (
	this: PluginContext,
	code: string,
	chunk: RenderedChunk,
	options: OutputOptions
) =>
	| Promise<{ code: string; map?: SourceMapInput } | null>
	| { code: string; map?: SourceMapInput }
	| string
	| null;

export type ResolveDynamicImportHook = (
	this: PluginContext,
	specifier: string | ESTree.Node,
	importer: string
) => Promise<ResolveIdResult> | ResolveIdResult;

export type ResolveImportMetaHook = (
	this: PluginContext,
	prop: string | null,
	options: { chunkId: string; format: string; moduleId: string }
) => string | null | undefined;

export type ResolveAssetUrlHook = (
	this: PluginContext,
	options: {
		assetFileName: string;
		chunkId: string;
		format: string;
		moduleId: string;
		relativeAssetPath: string;
	}
) => string | null | undefined;

export type ResolveFileUrlHook = (
	this: PluginContext,
	options: {
		assetReferenceId: string | null;
		chunkId: string;
		chunkReferenceId: string | null;
		fileName: string;
		format: string;
		moduleId: string;
		referenceId: string;
		relativePath: string;
	}
) => string | null | undefined;

export type AddonHook = string | ((this: PluginContext) => string | Promise<string>);

/**
 * use this type for plugin annotation
 * @example
 * ```ts
 * interface Options {
 * ...
 * }
 * const myPlugin: PluginImpl<Options> = (options = {}) => { ... }
 * ```
 */
export type PluginImpl<O extends object = object> = (options?: O) => Plugin;

export interface OutputBundle {
	[fileName: string]: OutputAsset | OutputChunk;
}

export interface FilePlaceholder {
	type: 'placeholder';
}

export interface OutputBundleWithPlaceholders {
	[fileName: string]: OutputAsset | OutputChunk | FilePlaceholder;
}

interface OnGenerateOptions extends OutputOptions {
	bundle: OutputChunk;
}

interface OnWriteOptions extends OutputOptions {
	bundle: RollupBuild;
}

interface OutputPluginHooks {
	augmentChunkHash: (this: PluginContext, chunk: PreRenderedChunk) => string | void;
	generateBundle: (
		this: PluginContext,
		options: OutputOptions,
		bundle: OutputBundle,
		isWrite: boolean
	) => void | Promise<void>;
	/** @deprecated Use `generateBundle` instead */
	ongenerate: (
		this: PluginContext,
		options: OnGenerateOptions,
		chunk: OutputChunk
	) => void | Promise<void>;
	/** @deprecated Use `writeBundle` instead */
	onwrite: (
		this: PluginContext,
		options: OnWriteOptions,
		chunk: OutputChunk
	) => void | Promise<void>;
	outputOptions: (this: PluginContext, options: OutputOptions) => OutputOptions | null | undefined;
	renderChunk: RenderChunkHook;
	renderError: (this: PluginContext, err?: Error) => Promise<void> | void;
	renderStart: (
		this: PluginContext,
		outputOptions: OutputOptions,
		inputOptions: InputOptions
	) => Promise<void> | void;
	/** @deprecated Use `resolveFileUrl` instead */
	resolveAssetUrl: ResolveAssetUrlHook;
	resolveDynamicImport: ResolveDynamicImportHook;
	resolveFileUrl: ResolveFileUrlHook;
	/** @deprecated Use `renderChunk` instead */
	transformBundle: TransformChunkHook;
	/** @deprecated Use `renderChunk` instead */
	transformChunk: TransformChunkHook;
	writeBundle: (this: PluginContext, bundle: OutputBundle) => void | Promise<void>;
}

export interface PluginHooks extends OutputPluginHooks {
	buildEnd: (this: PluginContext, err?: Error) => Promise<void> | void;
	buildStart: (this: PluginContext, options: InputOptions) => Promise<void> | void;
	load: LoadHook;
	options: (this: MinimalPluginContext, options: InputOptions) => InputOptions | null | undefined;
	resolveId: ResolveIdHook;
	resolveImportMeta: ResolveImportMetaHook;
	transform: TransformHook;
	watchChange: (id: string) => void;
}

interface OutputPluginValueHooks {
	banner: AddonHook;
	cacheKey: string;
	footer: AddonHook;
	intro: AddonHook;
	outro: AddonHook;
}

export interface Plugin extends Partial<PluginHooks>, Partial<OutputPluginValueHooks> {
	name: string;
}

export interface OutputPlugin extends Partial<OutputPluginHooks>, Partial<OutputPluginValueHooks> {
	name: string;
}

export interface TreeshakingOptions {
	annotations?: boolean;
	moduleSideEffects?: ModuleSideEffectsOption;
	propertyReadSideEffects?: boolean;
	/** @deprecated Use `moduleSideEffects` instead */
	pureExternalModules?: PureModulesOption;
	tryCatchDeoptimization?: boolean;
	unknownGlobalSideEffects?: boolean;
}

export type GetManualChunk = (id: string) => string | null | undefined;

export type ExternalOption = string[] | IsExternal;
export type PureModulesOption = boolean | string[] | IsPureModule;
export type GlobalsOption = { [name: string]: string } | ((name: string) => string);
export type InputOption = string | string[] | { [entryAlias: string]: string };
export type ManualChunksOption = { [chunkAlias: string]: string[] } | GetManualChunk;
export type ModuleSideEffectsOption = boolean | 'no-external' | string[] | HasModuleSideEffects;

export interface InputOptions {
	acorn?: any;
	acornInjectPlugins?: Function[];
	cache?: false | RollupCache;
	chunkGroupingSize?: number;
	context?: string;
	experimentalCacheExpiry?: number;
	experimentalOptimizeChunks?: boolean;
	external?: ExternalOption;
	inlineDynamicImports?: boolean;
	input?: InputOption;
	manualChunks?: ManualChunksOption;
	moduleContext?: ((id: string) => string) | { [id: string]: string };
	onwarn?: WarningHandlerWithDefault;
	perf?: boolean;
	plugins?: Plugin[];
	preserveModules?: boolean;
	preserveSymlinks?: boolean;
	shimMissingExports?: boolean;
	strictDeprecations?: boolean;
	treeshake?: boolean | TreeshakingOptions;
	watch?: WatcherOptions;
}

export type ModuleFormat =
	| 'amd'
	| 'cjs'
	| 'commonjs'
	| 'es'
	| 'esm'
	| 'iife'
	| 'module'
	| 'system'
	| 'umd';

export type OptionsPaths = Record<string, string> | ((id: string) => string);

export interface OutputOptions {
	amd?: {
		define?: string;
		id?: string;
	};
	assetFileNames?: string;
	banner?: string | (() => string | Promise<string>);
	chunkFileNames?: string;
	compact?: boolean;
	// only required for bundle.write
	dir?: string;
	dynamicImportFunction?: string;
	entryFileNames?: string;
	esModule?: boolean;
	exports?: 'default' | 'named' | 'none' | 'auto';
	extend?: boolean;
	externalLiveBindings?: boolean;
	// only required for bundle.write
	file?: string;
	footer?: string | (() => string | Promise<string>);
	// this is optional at the base-level of RollupWatchOptions,
	// which extends from this interface through config merge
	format?: ModuleFormat;
	freeze?: boolean;
	globals?: GlobalsOption;
	hoistTransitiveImports?: boolean;
	indent?: boolean;
	interop?: boolean;
	intro?: string | (() => string | Promise<string>);
	name?: string;
	namespaceToStringTag?: boolean;
	noConflict?: boolean;
	outro?: string | (() => string | Promise<string>);
	paths?: OptionsPaths;
	plugins?: OutputPlugin[];
	preferConst?: boolean;
	sourcemap?: boolean | 'inline' | 'hidden';
	sourcemapExcludeSources?: boolean;
	sourcemapFile?: string;
	sourcemapPathTransform?: (sourcePath: string) => string;
	strict?: boolean;
}

export type WarningHandlerWithDefault = (
	warning: RollupWarning,
	defaultHandler: WarningHandler
) => void;
export type WarningHandler = (warning: RollupWarning) => void;

export interface SerializedTimings {
	[label: string]: [number, number, number];
}

export interface OutputAsset {
	fileName: string;
	/** @deprecated Accessing "isAsset" on files in the bundle is deprecated, please use "type === \'asset\'" instead */
	isAsset: true;
	source: string | Buffer;
	type: 'asset';
}

export interface RenderedModule {
	originalLength: number;
	removedExports: string[];
	renderedExports: string[];
	renderedLength: number;
}

export interface PreRenderedChunk {
	dynamicImports: string[];
	exports: string[];
	facadeModuleId: string | null;
	imports: string[];
	isDynamicEntry: boolean;
	isEntry: boolean;
	modules: {
		[id: string]: RenderedModule;
	};
	name: string;
}

export interface RenderedChunk extends PreRenderedChunk {
	fileName: string;
}

export interface OutputChunk extends RenderedChunk {
	code: string;
	map?: SourceMap;
	type: 'chunk';
}

export interface SerializablePluginCache {
	[key: string]: [number, any];
}

export interface RollupCache {
	modules: ModuleJSON[];
	plugins?: Record<string, SerializablePluginCache>;
}

export interface RollupOutput {
	output: [OutputChunk, ...(OutputChunk | OutputAsset)[]];
}

export interface RollupBuild {
	cache: RollupCache;
	generate: (outputOptions: OutputOptions) => Promise<RollupOutput>;
	getTimings?: () => SerializedTimings;
	watchFiles: string[];
	write: (options: OutputOptions) => Promise<RollupOutput>;
}

export interface RollupOptions extends InputOptions {
	// This is included for compatibility with config files but ignored by rollup.rollup
	output?: OutputOptions | OutputOptions[];
}

export function rollup(options: RollupOptions): Promise<RollupBuild>;
// chokidar watch options
export interface WatchOptions {
	alwaysStat?: boolean;
	atomic?: boolean | number;
	awaitWriteFinish?:
		| {
				pollInterval?: number;
				stabilityThreshold?: number;
		  }
		| boolean;
	binaryInterval?: number;
	cwd?: string;
	depth?: number;
	disableGlobbing?: boolean;
	followSymlinks?: boolean;
	ignored?: any;
	ignoreInitial?: boolean;
	ignorePermissionErrors?: boolean;
	interval?: number;
	persistent?: boolean;
	useFsEvents?: boolean;
	usePolling?: boolean;
}

export interface WatcherOptions {
	chokidar?: boolean | WatchOptions;
	clearScreen?: boolean;
	exclude?: string[];
	include?: string[];
}

export interface RollupWatchOptions extends InputOptions {
	output?: OutputOptions | OutputOptions[];
	watch?: WatcherOptions;
}

interface TypedEventEmitter<T> {
	addListener<K extends keyof T>(event: K, listener: T[K]): this;
	emit<K extends keyof T>(event: K, ...args: any[]): boolean;
	eventNames(): Array<keyof T>;
	getMaxListeners(): number;
	listenerCount(type: keyof T): number;
	listeners<K extends keyof T>(event: K): Array<T[K]>;
	off<K extends keyof T>(event: K, listener: T[K]): this;
	on<K extends keyof T>(event: K, listener: T[K]): this;
	once<K extends keyof T>(event: K, listener: T[K]): this;
	prependListener<K extends keyof T>(event: K, listener: T[K]): this;
	prependOnceListener<K extends keyof T>(event: K, listener: T[K]): this;
	rawListeners<K extends keyof T>(event: K): Array<T[K]>;
	removeAllListeners<K extends keyof T>(event?: K): this;
	removeListener<K extends keyof T>(event: K, listener: T[K]): this;
	setMaxListeners(n: number): this;
}

export type RollupWatcherEvent =
	| { code: 'START' }
	| { code: 'BUNDLE_START'; input: InputOption; output: readonly string[] }
	| {
			code: 'BUNDLE_END';
			duration: number;
			input: InputOption;
			output: readonly string[];
			result: RollupBuild;
	  }
	| { code: 'END' }
	| { code: 'ERROR'; error: RollupError };

export interface RollupWatcher
	extends TypedEventEmitter<{
		change: (id: string) => void;
		event: (event: RollupWatcherEvent) => void;
		restart: () => void;
	}> {
	close(): void;
}

export function watch(configs: RollupWatchOptions[]): RollupWatcher;
