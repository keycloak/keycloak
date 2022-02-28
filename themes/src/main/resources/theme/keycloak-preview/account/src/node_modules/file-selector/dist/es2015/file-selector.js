import * as tslib_1 from "tslib";
import { toFileWithPath } from './file';
const FILES_TO_IGNORE = [
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
export function fromEvent(evt) {
    return tslib_1.__awaiter(this, void 0, void 0, function* () {
        return isDragEvt(evt) && evt.dataTransfer
            ? getDataTransferFiles(evt.dataTransfer, evt.type)
            : getInputFiles(evt);
    });
}
function isDragEvt(value) {
    return !!value.dataTransfer;
}
function getInputFiles(evt) {
    const files = isInput(evt.target)
        ? evt.target.files
            ? fromList(evt.target.files)
            : []
        : [];
    return files.map(file => toFileWithPath(file));
}
function isInput(value) {
    return value !== null;
}
function getDataTransferFiles(dt, type) {
    return tslib_1.__awaiter(this, void 0, void 0, function* () {
        // IE11 does not support dataTransfer.items
        // See https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/items#Browser_compatibility
        if (dt.items) {
            const items = fromList(dt.items)
                .filter(item => item.kind === 'file');
            // According to https://html.spec.whatwg.org/multipage/dnd.html#dndevents,
            // only 'dragstart' and 'drop' has access to the data (source node)
            if (type !== 'drop') {
                return items;
            }
            const files = yield Promise.all(items.map(toFilePromises));
            return noIgnoredFiles(flatten(files));
        }
        return noIgnoredFiles(fromList(dt.files)
            .map(file => toFileWithPath(file)));
    });
}
function noIgnoredFiles(files) {
    return files.filter(file => FILES_TO_IGNORE.indexOf(file.name) === -1);
}
// IE11 does not support Array.from()
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/from#Browser_compatibility
// https://developer.mozilla.org/en-US/docs/Web/API/FileList
// https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItemList
function fromList(items) {
    const files = [];
    // tslint:disable: prefer-for-of
    for (let i = 0; i < items.length; i++) {
        const file = items[i];
        files.push(file);
    }
    return files;
}
// https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem
function toFilePromises(item) {
    if (typeof item.webkitGetAsEntry !== 'function') {
        return fromDataTransferItem(item);
    }
    const entry = item.webkitGetAsEntry();
    // Safari supports dropping an image node from a different window and can be retrieved using
    // the DataTransferItem.getAsFile() API
    // NOTE: FileSystemEntry.file() throws if trying to get the file
    if (entry && entry.isDirectory) {
        return fromDirEntry(entry);
    }
    return fromDataTransferItem(item);
}
function flatten(items) {
    return items.reduce((acc, files) => [
        ...acc,
        ...(Array.isArray(files) ? flatten(files) : [files])
    ], []);
}
function fromDataTransferItem(item) {
    const file = item.getAsFile();
    if (!file) {
        return Promise.reject(`${item} is not a File`);
    }
    const fwp = toFileWithPath(file);
    return Promise.resolve(fwp);
}
// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemEntry
function fromEntry(entry) {
    return tslib_1.__awaiter(this, void 0, void 0, function* () {
        return entry.isDirectory ? fromDirEntry(entry) : fromFileEntry(entry);
    });
}
// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryEntry
function fromDirEntry(entry) {
    const reader = entry.createReader();
    return new Promise((resolve, reject) => {
        const entries = [];
        function readEntries() {
            // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryEntry/createReader
            // https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryReader/readEntries
            reader.readEntries((batch) => tslib_1.__awaiter(this, void 0, void 0, function* () {
                if (!batch.length) {
                    // Done reading directory
                    try {
                        const files = yield Promise.all(entries);
                        resolve(files);
                    }
                    catch (err) {
                        reject(err);
                    }
                }
                else {
                    const items = Promise.all(batch.map(fromEntry));
                    entries.push(items);
                    // Continue reading
                    readEntries();
                }
            }), (err) => {
                reject(err);
            });
        }
        readEntries();
    });
}
// https://developer.mozilla.org/en-US/docs/Web/API/FileSystemFileEntry
function fromFileEntry(entry) {
    return tslib_1.__awaiter(this, void 0, void 0, function* () {
        return new Promise((resolve, reject) => {
            entry.file((file) => {
                const fwp = toFileWithPath(file, entry.fullPath);
                resolve(fwp);
            }, (err) => {
                reject(err);
            });
        });
    });
}
//# sourceMappingURL=file-selector.js.map