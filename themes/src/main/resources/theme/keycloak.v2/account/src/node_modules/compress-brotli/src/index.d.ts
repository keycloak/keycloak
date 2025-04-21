import { BrotliOptions, InputType, CompressCallback } from 'zlib'
import { stringify as JSONBstringify, parse as JSONBparse } from 'json-buffer'

declare module 'compress-brotli'

type CompressResult = Promise<Parameters<CompressCallback>[1]>
type DecompressResult = Promise<ReturnType<typeof JSONBparse>>

type Serialize<T> = (source: InputType) => T
type Deserialize<T> = (source: CompressResult) => T

declare function createCompress<
  SerializeResult = ReturnType<typeof JSONBstringify>,
  DeserializeResult = ReturnType<typeof JSONBparse>
>(
  options?: {
    enable?: boolean,
    serialize?: Serialize<SerializeResult>,
    deserialize?: Deserialize<DeserializeResult>,
    iltorb?: any,
    compressOptions?: BrotliOptions,
    decompressOptions?: BrotliOptions
  }
): {
  serialize: Serialize<SerializeResult>,
  deserialize: Deserialize<DeserializeResult>,
  compress: (data: InputType, optioins?: BrotliOptions) => CompressResult
  decompress: (data: InputType, optioins?: BrotliOptions) => DecompressResult
}

declare namespace createCompress {
  const stringify: typeof JSONBstringify
  const parse: typeof JSONBparse
}

export default createCompress
