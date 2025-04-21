export declare enum fileReaderType {
    text = "text",
    dataURL = "dataURL"
}
/**
 * Read a file using the FileReader API, either as a plain text string or as a DataURL string.
 * Returns a promise which will resolve with the file contents as a string or reject with a DOMException.
 *
 * @param {File} fileHandle - File object to read
 * @param {fileReaderType} type - How to read it
 */
export declare function readFile(fileHandle: File, type: fileReaderType): Promise<unknown>;
//# sourceMappingURL=fileUtils.d.ts.map