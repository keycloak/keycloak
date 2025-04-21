# File Selector

> A small package for converting a [DragEvent](https://developer.mozilla.org/en-US/docs/Web/API/DragEvent) or [file input](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file) to a list of File objects.

[![npm](https://img.shields.io/npm/v/file-selector.svg?style=flat-square)](https://www.npmjs.com/package/file-selector)
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/react-dropzone/file-selector/Test?label=tests&style=flat-square)](https://github.com/react-dropzone/file-selector/actions?query=workflow%3ATest)
[![Coveralls github branch](https://img.shields.io/coveralls/github/react-dropzone/file-selector/master?style=flat-square)](https://coveralls.io/github/react-dropzone/file-selector?branch=master)

# Table of Contents

* [Installation](#installation)
* [Usage](#usage)
* [Browser Support](#browser-support)
* [Contribute](#contribute)
* [Credits](#credits)


### Installation
----------------
You can install this package from [NPM](https://www.npmjs.com):
```bash
npm add file-selector
```

Or with [Yarn](https://yarnpkg.com/en):
```bash
yarn add file-selector
```

#### CDN
For CDN, you can use [unpkg](https://unpkg.com):

[https://unpkg.com/file-selector/dist/bundles/file-selector.umd.min.js](https://unpkg.com/file-selector/dist/bundles/file-selector.umd.min.js)

The global namespace for file-selector is `fileSelector`:
```js
const {fromEvent} = fileSelector;
document.addEventListener('drop', async evt => {
    const files = await fromEvent(evt);
    console.log(files);
});
```


### Usage
---------

#### ES6
Convert a `DragEvent` to File objects:
```ts
import {fromEvent} from 'file-selector';
document.addEventListener('drop', async evt => {
    const files = await fromEvent(evt);
    console.log(files);
});
```

Convert a file input to File objects:
```ts
import {fromEvent} from 'file-selector';
const input = document.getElementById('myInput');
input.addEventListener('change', async evt => {
    const files = await fromEvent(evt);
    console.log(files);
});
```

#### CommonJS
Convert a `DragEvent` to File objects:
```ts
const {fromEvent} = require('file-selector');
document.addEventListener('drop', async evt => {
    const files = await fromEvent(evt);
    console.log(files);
});
```


### Browser Support
-------------------
Most browser support basic File selection with drag 'n' drop or file input:
* [File API](https://developer.mozilla.org/en-US/docs/Web/API/File#Browser_compatibility)
* [Drag Event](https://developer.mozilla.org/en-US/docs/Web/API/DragEvent#Browser_compatibility)
* [DataTransfer](https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer#Browser_compatibility)
* [`<input type="file">`](https://developer.mozilla.org/en-US/docs/Web/HTML/Element/input/file#Browser_compatibility)

For folder drop we use the [FileSystem API](https://developer.mozilla.org/en-US/docs/Web/API/FileSystem) which has very limited support:
* [DataTransferItem.getAsFile()](https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem/getAsFile#Browser_compatibility)
* [DataTransferItem.webkitGetAsEntry()](https://developer.mozilla.org/en-US/docs/Web/API/DataTransferItem/webkitGetAsEntry#Browser_compatibility)
* [FileSystemEntry](https://developer.mozilla.org/en-US/docs/Web/API/FileSystemEntry#Browser_compatibility)
* [FileSystemFileEntry.file()](https://developer.mozilla.org/en-US/docs/Web/API/FileSystemFileEntry/file#Browser_compatibility)
* [FileSystemDirectoryEntry.createReader()](https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryEntry/createReader#Browser_compatibility)
* [FileSystemDirectoryReader.readEntries()](https://developer.mozilla.org/en-US/docs/Web/API/FileSystemDirectoryReader/readEntries#Browser_compatibility)


### Contribute
--------------
If you wish to contribute, please use the following guidelines:
* Use [Conventional Commits](https://conventionalcommits.org)
* Use `[ci skip]` in commit messages to skip a build


### Credits
-----------
* [html5-file-selector](https://github.com/quarklemotion/html5-file-selector)
