# js-sha256
[![Build Status](https://travis-ci.org/emn178/js-sha256.svg?branch=master)](https://travis-ci.org/emn178/js-sha256)
[![Coverage Status](https://coveralls.io/repos/emn178/js-sha256/badge.svg?branch=master)](https://coveralls.io/r/emn178/js-sha256?branch=master)
[![CDNJS](https://img.shields.io/cdnjs/v/js-sha256.svg)](https://cdnjs.com/libraries/js-sha256/)  
[![NPM](https://nodei.co/npm/js-sha256.png?stars&downloads)](https://nodei.co/npm/js-sha256/)  
A simple SHA-256 / SHA-224 hash function for JavaScript supports UTF-8 encoding.

## Demo
[SHA256 Online](http://emn178.github.io/online-tools/sha256.html)  
[SHA224 Online](http://emn178.github.io/online-tools/sha224.html)  

## Download
[Compress](https://raw.github.com/emn178/js-sha256/master/build/sha256.min.js)  
[Uncompress](https://raw.github.com/emn178/js-sha256/master/src/sha256.js)

## Installation
You can also install js-sha256 by using Bower.

    bower install js-sha256

For node.js, you can use this command to install:

    npm install js-sha256

## Usage
You could use like this:
```JavaScript
sha256('Message to hash');
sha224('Message to hash');

var hash = sha256.create();
hash.update('Message to hash');
hash.hex();

var hash2 = sha256.update('Message to hash');
hash2.update('Message2 to hash');
hash2.array();

// HMAC
sha256.hmac('key', 'Message to hash');
sha224.hmac('key', 'Message to hash');

var hash = sha256.hmac.create('key');
hash.update('Message to hash');
hash.hex();

var hash2 = sha256.hmac.update('key', 'Message to hash');
hash2.update('Message2 to hash');
hash2.array();
```
If you use node.js, you should require the module first:
```JavaScript
var sha256 = require('js-sha256');
```
or 
```JavaScript
var sha256 = require('js-sha256').sha256;
var sha224 = require('js-sha256').sha224;
```
It supports AMD:
```JavaScript
require(['your/path/sha256.js'], function(sha256) {
// ...
});
```
or TypeScript
```TypeScript
import { sha256, sha224 } from 'js-sha256';
```
## Example
```JavaScript
sha256(''); // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
sha256('The quick brown fox jumps over the lazy dog'); // d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592
sha256('The quick brown fox jumps over the lazy dog.'); // ef537f25c895bfa782526529a9b63d97aa631564d5d789c2b765448c8635fb6c
sha224(''); // d14a028c2a3a2bc9476102bb288234c415a2b01f828ea62ac5b3e42f
sha224('The quick brown fox jumps over the lazy dog'); // 730e109bd7a8a32b1cb9d9a09aa2325d2430587ddbc0c38bad911525
sha224('The quick brown fox jumps over the lazy dog.'); // 619cba8e8e05826e9b8c519c0a5c68f4fb653e8a3d8aa04bb2c8cd4c

// It also supports UTF-8 encoding
sha256('中文'); // 72726d8818f693066ceb69afa364218b692e62ea92b385782363780f47529c21
sha224('中文'); // dfbab71afdf54388af4d55f8bd3de8c9b15e0eb916bf9125f4a959d4

// It also supports byte `Array`, `Uint8Array`, `ArrayBuffer` input
sha256([]); // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
sha256(new Uint8Array([211, 212])); // 182889f925ae4e5cc37118ded6ed87f7bdc7cab5ec5e78faef2e50048999473f

// Different output
sha256(''); // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
sha256.hex(''); // e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
sha256.array(''); // [227, 176, 196, 66, 152, 252, 28, 20, 154, 251, 244, 200, 153, 111, 185, 36, 39, 174, 65, 228, 100, 155, 147, 76, 164, 149, 153, 27, 120, 82, 184, 85]
sha256.digest(''); // [227, 176, 196, 66, 152, 252, 28, 20, 154, 251, 244, 200, 153, 111, 185, 36, 39, 174, 65, 228, 100, 155, 147, 76, 164, 149, 153, 27, 120, 82, 184, 85]
sha256.arrayBuffer(''); // ArrayBuffer
```

## License
The project is released under the [MIT license](http://www.opensource.org/licenses/MIT).

## Contact
The project's website is located at https://github.com/emn178/js-sha256  
Author: Chen, Yi-Cyuan (emn178@gmail.com)
