export declare const COMMON_MIME_TYPES: Map<string, string>;
export declare function toFileWithPath(file: FileWithPath, path?: string): FileWithPath;
interface DOMFile extends Blob {
    readonly lastModified: number;
    readonly name: string;
}
export interface FileWithPath extends DOMFile {
    readonly path?: string;
}
export {};
