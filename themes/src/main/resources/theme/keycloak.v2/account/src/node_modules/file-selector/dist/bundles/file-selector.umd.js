(function (global, factory) {
    typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports) :
    typeof define === 'function' && define.amd ? define(['exports'], factory) :
    (global = typeof globalThis !== 'undefined' ? globalThis : global || self, factory(global.fileSelector = {}));
}(this, (function (exports) { 'use strict';

    /*! *****************************************************************************
    Copyright (c) Microsoft Corporation.

    Permission to use, copy, modify, and/or distribute this software for any
    purpose with or without fee is hereby granted.

    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
    REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
    AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
    INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
    LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
    OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
    PERFORMANCE OF THIS SOFTWARE.
    ***************************************************************************** */

    function __awaiter(thisArg, _arguments, P, generator) {
        function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
        return new (P || (P = Promise))(function (resolve, reject) {
            function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
            function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
            function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
            step((generator = generator.apply(thisArg, _arguments || [])).next());
        });
    }

    function __generator(thisArg, body) {
        var _ = { label: 0, sent: function() { if (t[0] & 1) throw t[1]; return t[1]; }, trys: [], ops: [] }, f, y, t, g;
        return g = { next: verb(0), "throw": verb(1), "return": verb(2) }, typeof Symbol === "function" && (g[Symbol.iterator] = function() { return this; }), g;
        function verb(n) { return function (v) { return step([n, v]); }; }
        function step(op) {
            if (f) throw new TypeError("Generator is already executing.");
            while (_) try {
                if (f = 1, y && (t = op[0] & 2 ? y["return"] : op[0] ? y["throw"] || ((t = y["return"]) && t.call(y), 0) : y.next) && !(t = t.call(y, op[1])).done) return t;
                if (y = 0, t) op = [op[0] & 2, t.value];
                switch (op[0]) {
                    case 0: case 1: t = op; break;
                    case 4: _.label++; return { value: op[1], done: false };
                    case 5: _.label++; y = op[1]; op = [0]; continue;
                    case 7: op = _.ops.pop(); _.trys.pop(); continue;
                    default:
                        if (!(t = _.trys, t = t.length > 0 && t[t.length - 1]) && (op[0] === 6 || op[0] === 2)) { _ = 0; continue; }
                        if (op[0] === 3 && (!t || (op[1] > t[0] && op[1] < t[3]))) { _.label = op[1]; break; }
                        if (op[0] === 6 && _.label < t[1]) { _.label = t[1]; t = op; break; }
                        if (t && _.label < t[2]) { _.label = t[2]; _.ops.push(op); break; }
                        if (t[2]) _.ops.pop();
                        _.trys.pop(); continue;
                }
                op = body.call(thisArg, _);
            } catch (e) { op = [6, e]; y = 0; } finally { f = t = 0; }
            if (op[0] & 5) throw op[1]; return { value: op[0] ? op[1] : void 0, done: true };
        }
    }

    function __read(o, n) {
        var m = typeof Symbol === "function" && o[Symbol.iterator];
        if (!m) return o;
        var i = m.call(o), r, ar = [], e;
        try {
            while ((n === void 0 || n-- > 0) && !(r = i.next()).done) ar.push(r.value);
        }
        catch (error) { e = { error: error }; }
        finally {
            try {
                if (r && !r.done && (m = i["return"])) m.call(i);
            }
            finally { if (e) throw e.error; }
        }
        return ar;
    }

    function __spread() {
        for (var ar = [], i = 0; i < arguments.length; i++)
            ar = ar.concat(__read(arguments[i]));
        return ar;
    }

    var COMMON_MIME_TYPES = new Map([
        ['avi', 'video/avi'],
        ['gif', 'image/gif'],
        ['ico', 'image/x-icon'],
        ['jpeg', 'image/jpeg'],
        ['jpg', 'image/jpeg'],
        ['mkv', 'video/x-matroska'],
        ['mov', 'video/quicktime'],
        ['mp4', 'video/mp4'],
        ['pdf', 'application/pdf'],
        ['png', 'image/png'],
        ['zip', 'application/zip'],
        ['doc', 'application/msword'],
        ['docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
    ]);
    function toFileWithPath(file, path) {
        var f = withMimeType(file);
        if (typeof f.path !== 'string') { // on electron, path is already set to the absolute path
            var webkitRelativePath = file.webkitRelativePath;
            Object.defineProperty(f, 'path', {
                value: typeof path === 'string'
                    ? path
                    // If <input webkitdirectory> is set,
                    // the File will have a {webkitRelativePath} property
                    // https://developer.mozilla.org/en-US/docs/Web/API/HTMLInputElement/webkitdirectory
                    : typeof webkitRelativePath === 'string' && webkitRelativePath.length > 0
                        ? webkitRelativePath
                        : file.name,
                writable: false,
                configurable: false,
                enumerable: true
            });
        }
        return f;
    }
    function withMimeType(file) {
        var name = file.name;
        var hasExtension = name && name.lastIndexOf('.') !== -1;
        if (hasExtension && !file.type) {
            var ext = name.split('.')
                .pop().toLowerCase();
            var type = COMMON_MIME_TYPES.get(ext);
            if (type) {
                Object.defineProperty(file, 'type', {
                    value: type,
                    writable: false,
                    configurable: false,
                    enumerable: true
                });
            }
        }
        return file;
    }

    var FILES_TO_IGNORE = [
        // Thumbnail cache files for macOS and Windows
        '.DS_Store',
        'Thumbs.db' // Windows
    ];
    /**
     * Convert a DragEvent's DataTrasfer object to a list of File objects
     * NOTE: If some of the items are folders,
     * everything will be flattened and placed in the same list but the paths will be kept as a {path} property.
     * @param evt
     */
    function fromEvent(evt) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                return [2 /*return*/, isDragEvt(evt) && evt.dataTransfer
                        ? getDataTransferFiles(evt.dataTransfer, evt.type)
                        : getInputFiles(evt)];
            });
        });
    }
    function isDragEvt(value) {
        return !!value.dataTransfer;
    }
    function getInputFiles(evt) {
        var files = isInput(evt.target)
            ? evt.target.files
                ? fromList(evt.target.files)
                : []
            : [];
        return files.map(function (file) { return toFileWithPath(file); });
    }
    function isInput(value) {
        return value !== null;
    }
    function getDataTransferFiles(dt, type) {
        return __awaiter(this, void 0, void 0, function () {
            var items, files;
            return __generator(this, function (_a) {
                switch (_a.label) {
                    case 0:
                        if (!dt.items) return [3 /*break*/, 2];
                        items = fromList(dt.items)
                            .filter(function (item) { return item.kind === 'file'; });
                        // According to https://html.spec.whatwg.org/multipage/dnd.html#dndevents,
                        // only 'dragstart' and 'drop' has access to the data (source node)
                        if (type !== 'drop') {
                            return [2 /*return*/, items];
                        }
                        return [4 /*yield*/, Promise.all(items.map(toFilePromises))];
                    case 1:
                        files = _a.sent();
                        return [2 /*return*/, noIgnoredFiles(flatten(files))];
                    case 2: return [2 /*return*/, noIgnoredFiles(fromList(dt.files)
                            .map(function (file) { return toFileWithPath(file); }))];
                }
            });
        });
    }
    function noIgnoredFiles(files) {
        return files.filter(function (file) { return FILES_TO_IGNORE.indexOf(file.name) === -1; });
    }
    // IE11 does not support Array.from()
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/from#Browser_compatibility
    // https://developer.mozilla.org/en-US/docs/Web/API/FileList
    // https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItemList
    function fromList(items) {
        var files = [];
        // tslint:disable: prefer-for-of
        for (var i = 0; i < items.length; i++) {
            var file = items[i];
            files.push(file);
        }
        return files;
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem
    function toFilePromises(item) {
        if (typeof item.webkitGetAsEntry !== 'function') {
            return fromDataTransferItem(item);
        }
        var entry = item.webkitGetAsEntry();
        // Safari supports dropping an image node from a different window and can be retrieved using
        // the DataTransferItem.getAsFile() API
        // NOTE: FileSystemEntry.file() throws if trying to get the file
        if (entry && entry.isDirectory) {
            return fromDirEntry(entry);
        }
        return fromDataTransferItem(item);
    }
    function flatten(items) {
        return items.reduce(function (acc, files) { return __spread(acc, (Array.isArray(files) ? flatten(files) : [files])); }, []);
    }
    function fromDataTransferItem(item) {
        var file = item.getAsFile();
        if (!file) {
            return Promise.reject(item + " is not a File");
        }
        var fwp = toFileWithPath(file);
        return Promise.resolve(fwp);
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemEntry
    function fromEntry(entry) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                return [2 /*return*/, entry.isDirectory ? fromDirEntry(entry) : fromFileEntry(entry)];
            });
        });
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryEntry
    function fromDirEntry(entry) {
        var reader = entry.createReader();
        return new Promise(function (resolve, reject) {
            var entries = [];
            function readEntries() {
                var _this = this;
                // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryEntry/createReader
                // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryReader/readEntries
                reader.readEntries(function (batch) { return __awaiter(_this, void 0, void 0, function () {
                    var files, err_1, items;
                    return __generator(this, function (_a) {
                        switch (_a.label) {
                            case 0:
                                if (!!batch.length) return [3 /*break*/, 5];
                                _a.label = 1;
                            case 1:
                                _a.trys.push([1, 3, , 4]);
                                return [4 /*yield*/, Promise.all(entries)];
                            case 2:
                                files = _a.sent();
                                resolve(files);
                                return [3 /*break*/, 4];
                            case 3:
                                err_1 = _a.sent();
                                reject(err_1);
                                return [3 /*break*/, 4];
                            case 4: return [3 /*break*/, 6];
                            case 5:
                                items = Promise.all(batch.map(fromEntry));
                                entries.push(items);
                                // Continue reading
                                readEntries();
                                _a.label = 6;
                            case 6: return [2 /*return*/];
                        }
                    });
                }); }, function (err) {
                    reject(err);
                });
            }
            readEntries();
        });
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemFileEntry
    function fromFileEntry(entry) {
        return __awaiter(this, void 0, void 0, function () {
            return __generator(this, function (_a) {
                return [2 /*return*/, new Promise(function (resolve, reject) {
                        entry.file(function (file) {
                            var fwp = toFileWithPath(file, entry.fullPath);
                            resolve(fwp);
                        }, function (err) {
                            reject(err);
                        });
                    })];
            });
        });
    }

    exports.fromEvent = fromEvent;

    Object.defineProperty(exports, '__esModule', { value: true });

})));
//# sourceMappingURL=file-selector.umd.js.map
