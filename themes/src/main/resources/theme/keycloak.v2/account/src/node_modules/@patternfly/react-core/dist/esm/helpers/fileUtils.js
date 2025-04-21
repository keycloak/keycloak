export var fileReaderType;
(function (fileReaderType) {
    fileReaderType["text"] = "text";
    fileReaderType["dataURL"] = "dataURL";
})(fileReaderType || (fileReaderType = {}));
/**
 * Read a file using the FileReader API, either as a plain text string or as a DataURL string.
 * Returns a promise which will resolve with the file contents as a string or reject with a DOMException.
 *
 * @param {File} fileHandle - File object to read
 * @param {fileReaderType} type - How to read it
 */
export function readFile(fileHandle, type) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(reader.result);
        reader.onerror = () => reject(reader.error);
        if (type === fileReaderType.text) {
            reader.readAsText(fileHandle);
        }
        else if (type === fileReaderType.dataURL) {
            reader.readAsDataURL(fileHandle);
        }
        else {
            reject('unknown type');
        }
    });
}
//# sourceMappingURL=fileUtils.js.map