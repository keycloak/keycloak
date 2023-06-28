/* eslint-disable no-use-before-define */
import { FilterPattern } from '@rollup/pluginutils';
import { Plugin } from 'rollup';
import {
  CompilerOptions,
  CompilerOptionsValue,
  CustomTransformers,
  Program,
  TsConfigSourceFile,
  TypeChecker
} from 'typescript';

type ElementType<T extends Array<any> | undefined> = T extends (infer U)[] ? U : never;

export type TransformerStage = keyof CustomTransformers;
type StagedTransformerFactory<T extends TransformerStage> = ElementType<CustomTransformers[T]>;
type TransformerFactory<T extends TransformerStage> =
  | StagedTransformerFactory<T>
  | ProgramTransformerFactory<T>
  | TypeCheckerTransformerFactory<T>;

export type CustomTransformerFactories = {
  [stage in TransformerStage]?: Array<TransformerFactory<stage>>;
};

interface ProgramTransformerFactory<T extends TransformerStage> {
  type: 'program';

  factory(program: Program): StagedTransformerFactory<T>;
}

interface TypeCheckerTransformerFactory<T extends TransformerStage> {
  type: 'typeChecker';

  factory(typeChecker: TypeChecker): StagedTransformerFactory<T>;
}

export interface RollupTypescriptPluginOptions {
  /**
   * If using incremental this is the folder where the cached
   * files will be created and kept for Typescript incremental
   * compilation.
   */
  cacheDir?: string;
  /**
   * Determine which files are transpiled by Typescript (all `.ts` and
   * `.tsx` files by default).
   */
  include?: FilterPattern;
  /**
   * Determine which files are ignored by Typescript
   */
  exclude?: FilterPattern;
  /**
   * Sets the `resolve` value for the underlying filter function.  If not set will use the `rootDir` property
   * @see {@link https://github.com/rollup/plugins/tree/master/packages/pluginutils#createfilter} @rollup/pluginutils `createFilter`
   */
  filterRoot?: string | false;
  /**
   * When set to false, ignores any options specified in the config file.
   * If set to a string that corresponds to a file path, the specified file
   * will be used as config file.
   */
  tsconfig?: string | false;
  /**
   * Overrides TypeScript used for transpilation
   */
  typescript?: typeof import('typescript');
  /**
   * Overrides the injected TypeScript helpers with a custom version.
   */
  tslib?: Promise<string> | string;
  /**
   * TypeScript custom transformers
   */
  transformers?: CustomTransformerFactories;
  /**
   * When set to false, force non-cached files to always be emitted in the output directory.output
   * If not set, will default to true with a warning.
   */
  outputToFilesystem?: boolean;
}

export interface FlexibleCompilerOptions extends CompilerOptions {
  [option: string]: CompilerOptionsValue | TsConfigSourceFile | undefined | any;
}

/** Properties of `CompilerOptions` that are normally enums */
export type EnumCompilerOptions = 'module' | 'moduleResolution' | 'newLine' | 'jsx' | 'target';

/** JSON representation of Typescript compiler options */
export type JsonCompilerOptions = Omit<FlexibleCompilerOptions, EnumCompilerOptions> &
  Record<EnumCompilerOptions, string>;

/** Compiler options set by the plugin user. */
export type PartialCompilerOptions =
  | Partial<FlexibleCompilerOptions>
  | Partial<JsonCompilerOptions>;

export type RollupTypescriptOptions = RollupTypescriptPluginOptions & PartialCompilerOptions;

/**
 * Seamless integration between Rollup and Typescript.
 */
export default function typescript(options?: RollupTypescriptOptions): Plugin;
