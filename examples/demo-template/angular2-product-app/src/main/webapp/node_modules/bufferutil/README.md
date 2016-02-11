# bufferutil

[![Build Status](https://travis-ci.org/websockets/bufferutil.svg?branch=master)](https://travis-ci.org/websockets/bufferutil)

Buffer utils is one of the modules that makes `ws` fast. It's optimized for
certain buffer based operations such as merging buffers, generating WebSocket
masks and unmasking.

As the module consists of binary components, it should be used an
`optionalDependency` so when installation fails, it doesn't halt the
installation of your module. There are fallback files available in this
repository. See `fallback.js` for the suggest fallback implementation if
installation fails. 

## Installation

```
npm install bufferutil
```

## API

In all examples we assume that you've already required the BufferUtil as
followed:

```js
'use strict';

var bu = require('bufferutil').BufferUtil;
```

The module exposes 3 different functions:

#### merge

Merge multiple buffers in the first supplied buffer argument:

```js
bu.merge(buffer, [buffer1, buffer2]);
```

This merges buffer1 and buffer2 which are in an array into buffer.

#### mask

Apply a WebSocket mask on the given data.

```js
bu.mask(buffer, mask);
```

#### unmask

Remove a WebSocket mask on the given data.;w

```js
bu.unmask(buffer, mask);
```

## License

MIT
