# compress-brotli

![Last version](https://img.shields.io/github/tag/Kikobeats/compress-brotli.svg?style=flat-square)
[![Coverage Status](https://img.shields.io/coveralls/Kikobeats/compress-brotli.svg?style=flat-square)](https://coveralls.io/github/Kikobeats/compress-brotli)
[![NPM Status](https://img.shields.io/npm/dm/compress-brotli.svg?style=flat-square)](https://www.npmjs.org/package/compress-brotli)

> Compress/Decompress using Brotli in a simple way.

## Highlights

- Handle edge cases (such as try to compress `undefined`).
- JSON serialization/deserialization with Buffer support by default.
- Easy tu customize (e.g., using [v8 serialization](https://nodejs.org/api/v8.html#v8_v8_serialize_value)).

## Install

```bash
$ npm install compress-brotli --save
```

## Usage

```js
const createCompress = require('compress-brotli')

// It exposes compress/decompress methods
const { compress, decompress } = createCompress()
```

using [v8 serialization](https://nodejs.org/api/v8.html#v8_v8_serialize_value):

```js
const createCompress = require('compress-brotli')
const v8 = require('v8')

const { compress, decompress } = createCompress({
  serialize: v8.serialize,
  deserialize: v8.deserialize
})
```
customizing compress options:
```js
const createCompress = require('compress-brotli')

const {
  constants: {
    BROTLI_MODE_TEXT,
    BROTLI_PARAM_MODE,
    BROTLI_PARAM_QUALITY
  }
} = require('zlib')

// Provide factory level default options
const { compress, decompress } = createCompress({
  compressOptions: {
    chunkSize: 1024,
    parameters: {
      [BROTLI_PARAM_MODE]: BROTLI_MODE_TEXT
    }
  },
  decompressOptions: {
    chunkSize: 1024,
    parameters: {
      [BROTLI_PARAM_MODE]: BROTLI_MODE_TEXT
    }
  }
})
const data = 'whatever'

// Override call level options (deep merge for parameters)
const compressed = compress(data, {
  parameters: {
    [BROTLI_PARAM_QUALITY]: 7
  }
})
decompress(compressed, {
  chunkSize: 2048
})
```

## API

### compressBrotli([options])

#### enable

Type: `boolean`<br>
Default: `false`

If pass disable, it will return a noop compress/decompress methods.

#### serialize

Type: `function`<br>
Default: `JSONB.stringify`

It determines the serialize method to use before compress the data.

#### deserialize

Type: `function`<br>
Default: `JSONB.parse`

It determines the deserialize method to use after decompress the data.

#### compressOptions

Type: `zlib.BrotliOptions`<br>
Default: `{}` i.e. default *zlib.brotliCompress* options will be used

It  defines default options to be used in wrapped *zlib.brotliCompress* call

#### decompressOptions

Type: `zlib.BrotliOptions`<br>
Default: `{}` i.e. default *zlib.brotliDecompress* options will be used

It defines default options to be used in wrapped *zlib.brotliDecompress* call

## License

**compress-brotli** © [Kiko Beats](https://kikobeats.com), released under the [MIT](https://github.com/Kikobeats/compress-brotli/blob/master/LICENSE.md) License.<br>
Authored and maintained by Kiko Beats with help from [contributors](https://github.com/Kikobeats/compress-brotli/contributors).

> [kikobeats.com](https://kikobeats.com) · GitHub [Kiko Beats](https://github.com/Kikobeats) · Twitter [@Kikobeats](https://twitter.com/Kikobeats)
