import type { DecodedSourceMap, EncodedSourceMap, Pos, Mapping } from './types';
export type { DecodedSourceMap, EncodedSourceMap, Mapping };
export declare type Options = {
    file?: string | null;
    sourceRoot?: string | null;
};
/**
 * A low-level API to associate a generated position with an original source position. Line and
 * column here are 0-based, unlike `addMapping`.
 */
export declare let addSegment: {
    (map: GenMapping, genLine: number, genColumn: number, source?: null, sourceLine?: null, sourceColumn?: null, name?: null): void;
    (map: GenMapping, genLine: number, genColumn: number, source: string, sourceLine: number, sourceColumn: number, name?: null): void;
    (map: GenMapping, genLine: number, genColumn: number, source: string, sourceLine: number, sourceColumn: number, name: string): void;
};
/**
 * A high-level API to associate a generated position with an original source position. Line is
 * 1-based, but column is 0-based, due to legacy behavior in `source-map` library.
 */
export declare let addMapping: {
    (map: GenMapping, mapping: {
        generated: Pos;
        source?: null;
        original?: null;
        name?: null;
    }): void;
    (map: GenMapping, mapping: {
        generated: Pos;
        source: string;
        original: Pos;
        name?: null;
    }): void;
    (map: GenMapping, mapping: {
        generated: Pos;
        source: string;
        original: Pos;
        name: string;
    }): void;
};
/**
 * Adds/removes the content of the source file to the source map.
 */
export declare let setSourceContent: (map: GenMapping, source: string, content: string | null) => void;
/**
 * Returns a sourcemap object (with decoded mappings) suitable for passing to a library that expects
 * a sourcemap, or to JSON.stringify.
 */
export declare let decodedMap: (map: GenMapping) => DecodedSourceMap;
/**
 * Returns a sourcemap object (with encoded mappings) suitable for passing to a library that expects
 * a sourcemap, or to JSON.stringify.
 */
export declare let encodedMap: (map: GenMapping) => EncodedSourceMap;
/**
 * Returns an array of high-level mapping objects for every recorded segment, which could then be
 * passed to the `source-map` library.
 */
export declare let allMappings: (map: GenMapping) => Mapping[];
/**
 * Provides the state to generate a sourcemap.
 */
export declare class GenMapping {
    private _names;
    private _sources;
    private _sourcesContent;
    private _mappings;
    file: string | null | undefined;
    sourceRoot: string | null | undefined;
    constructor({ file, sourceRoot }?: Options);
}
