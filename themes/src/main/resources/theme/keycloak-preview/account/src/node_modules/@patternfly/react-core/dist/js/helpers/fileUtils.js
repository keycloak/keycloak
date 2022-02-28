"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.readFile = readFile;
exports.fileReaderType = void 0;
var fileReaderType;
/**
 * Read a file using the FileReader API, either as a plain text string or as a DataURL string.
 * Returns a promise which will resolve with the file contents as a string or reject with a DOMException.
 *
 * @param {File} fileHandle - File object to read
 * @param {fileReaderType} type - How to read it
 */

exports.fileReaderType = fileReaderType;

(function (fileReaderType) {
  fileReaderType["text"] = "text";
  fileReaderType["dataURL"] = "dataURL";
})(fileReaderType || (exports.fileReaderType = fileReaderType = {}));

function readFile(fileHandle, type) {
  return new Promise(function (resolve, reject) {
    var reader = new FileReader();

    reader.onload = function () {
      return resolve(reader.result);
    };

    reader.onerror = function () {
      return reject(reader.error);
    };

    if (type === fileReaderType.text) {
      reader.readAsText(fileHandle);
    } else if (type === fileReaderType.dataURL) {
      reader.readAsDataURL(fileHandle);
    } else {
      reject('unknown type');
    }
  });
}
//# sourceMappingURL=fileUtils.js.map