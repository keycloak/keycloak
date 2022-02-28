export declare const COMMON_MIME_TYPES: Map<string, string>;
export declare function toFileWithPath(file: FileWithPath, path?: string): FileWithPath;
export interface FileWithPath extends File {
    readonly path?: string;
}
