/// <reference types="node" />
/// <reference lib="es2018" />
/// <reference lib="dom" />
export declare type Class<T = unknown> = new (...args: any[]) => T;
export declare const enum TypeName {
    null = "null",
    boolean = "boolean",
    undefined = "undefined",
    string = "string",
    number = "number",
    bigint = "bigint",
    symbol = "symbol",
    Function = "Function",
    Generator = "Generator",
    AsyncGenerator = "AsyncGenerator",
    GeneratorFunction = "GeneratorFunction",
    AsyncGeneratorFunction = "AsyncGeneratorFunction",
    AsyncFunction = "AsyncFunction",
    Observable = "Observable",
    Array = "Array",
    Buffer = "Buffer",
    Object = "Object",
    RegExp = "RegExp",
    Date = "Date",
    Error = "Error",
    Map = "Map",
    Set = "Set",
    WeakMap = "WeakMap",
    WeakSet = "WeakSet",
    Int8Array = "Int8Array",
    Uint8Array = "Uint8Array",
    Uint8ClampedArray = "Uint8ClampedArray",
    Int16Array = "Int16Array",
    Uint16Array = "Uint16Array",
    Int32Array = "Int32Array",
    Uint32Array = "Uint32Array",
    Float32Array = "Float32Array",
    Float64Array = "Float64Array",
    BigInt64Array = "BigInt64Array",
    BigUint64Array = "BigUint64Array",
    ArrayBuffer = "ArrayBuffer",
    SharedArrayBuffer = "SharedArrayBuffer",
    DataView = "DataView",
    Promise = "Promise",
    URL = "URL"
}
declare function is(value: unknown): TypeName;
declare namespace is {
    var undefined: (value: unknown) => value is undefined;
    var string: (value: unknown) => value is string;
    var number: (value: unknown) => value is number;
    var bigint: (value: unknown) => value is bigint;
    var function_: (value: unknown) => value is Function;
    var null_: (value: unknown) => value is null;
    var class_: (value: unknown) => value is Class<unknown>;
    var boolean: (value: unknown) => value is boolean;
    var symbol: (value: unknown) => value is symbol;
    var numericString: (value: unknown) => value is string;
    var array: (arg: any) => arg is any[];
    var buffer: (value: unknown) => value is Buffer;
    var nullOrUndefined: (value: unknown) => value is null | undefined;
    var object: (value: unknown) => value is object;
    var iterable: <T = unknown>(value: unknown) => value is IterableIterator<T>;
    var asyncIterable: <T = unknown>(value: unknown) => value is AsyncIterableIterator<T>;
    var generator: (value: unknown) => value is Generator<unknown, any, unknown>;
    var asyncGenerator: (value: unknown) => value is AsyncGenerator<unknown, any, unknown>;
    var nativePromise: <T = unknown>(value: unknown) => value is Promise<T>;
    var promise: <T = unknown>(value: unknown) => value is Promise<T>;
    var generatorFunction: (value: unknown) => value is GeneratorFunction;
    var asyncGeneratorFunction: (value: unknown) => value is (...args: any[]) => Promise<unknown>;
    var asyncFunction: <T = unknown>(value: unknown) => value is (...args: any[]) => Promise<T>;
    var boundFunction: (value: unknown) => value is Function;
    var regExp: (value: unknown) => value is RegExp;
    var date: (value: unknown) => value is Date;
    var error: (value: unknown) => value is Error;
    var map: <Key = unknown, Value = unknown>(value: unknown) => value is Map<Key, Value>;
    var set: <T = unknown>(value: unknown) => value is Set<T>;
    var weakMap: <Key extends object = object, Value = unknown>(value: unknown) => value is WeakMap<Key, Value>;
    var weakSet: (value: unknown) => value is WeakSet<object>;
    var int8Array: (value: unknown) => value is Int8Array;
    var uint8Array: (value: unknown) => value is Uint8Array;
    var uint8ClampedArray: (value: unknown) => value is Uint8ClampedArray;
    var int16Array: (value: unknown) => value is Int16Array;
    var uint16Array: (value: unknown) => value is Uint16Array;
    var int32Array: (value: unknown) => value is Int32Array;
    var uint32Array: (value: unknown) => value is Uint32Array;
    var float32Array: (value: unknown) => value is Float32Array;
    var float64Array: (value: unknown) => value is Float64Array;
    var bigInt64Array: (value: unknown) => value is BigInt64Array;
    var bigUint64Array: (value: unknown) => value is BigUint64Array;
    var arrayBuffer: (value: unknown) => value is ArrayBuffer;
    var sharedArrayBuffer: (value: unknown) => value is SharedArrayBuffer;
    var dataView: (value: unknown) => value is DataView;
    var directInstanceOf: <T>(instance: unknown, class_: Class<T>) => instance is T;
    var urlInstance: (value: unknown) => value is URL;
    var urlString: (value: unknown) => value is string;
    var truthy: (value: unknown) => boolean;
    var falsy: (value: unknown) => boolean;
    var nan: (value: unknown) => boolean;
    var primitive: (value: unknown) => value is Primitive;
    var integer: (value: unknown) => value is number;
    var safeInteger: (value: unknown) => value is number;
    var plainObject: <Value = unknown>(value: unknown) => value is Record<string, Value>;
    var typedArray: (value: unknown) => value is TypedArray;
    var arrayLike: <T = unknown>(value: unknown) => value is ArrayLike<T>;
    var inRange: (value: number, range: number | number[]) => value is number;
    var domElement: (value: unknown) => value is Element;
    var observable: (value: unknown) => value is ObservableLike;
    var nodeStream: (value: unknown) => value is NodeStream;
    var infinite: (value: unknown) => value is number;
    var evenInteger: (value: number) => value is number;
    var oddInteger: (value: number) => value is number;
    var emptyArray: (value: unknown) => value is never[];
    var nonEmptyArray: (value: unknown) => value is unknown[];
    var emptyString: (value: unknown) => value is "";
    var nonEmptyString: (value: unknown) => value is string;
    var emptyStringOrWhitespace: (value: unknown) => value is string;
    var emptyObject: <Key extends string | number | symbol = string>(value: unknown) => value is Record<Key, never>;
    var nonEmptyObject: <Key extends string | number | symbol = string, Value = unknown>(value: unknown) => value is Record<Key, Value>;
    var emptySet: (value: unknown) => value is Set<never>;
    var nonEmptySet: <T = unknown>(value: unknown) => value is Set<T>;
    var emptyMap: (value: unknown) => value is Map<never, never>;
    var nonEmptyMap: <Key = unknown, Value = unknown>(value: unknown) => value is Map<Key, Value>;
    var any: (predicate: Predicate | Predicate[], ...values: unknown[]) => boolean;
    var all: (predicate: Predicate, ...values: unknown[]) => boolean;
}
export declare type Primitive = null | undefined | string | number | bigint | boolean | symbol;
export declare type TypedArray = Int8Array | Uint8Array | Uint8ClampedArray | Int16Array | Uint16Array | Int32Array | Uint32Array | Float32Array | Float64Array | BigInt64Array | BigUint64Array;
export interface ArrayLike<T> {
    readonly [index: number]: T;
    readonly length: number;
}
export interface ObservableLike {
    subscribe(observer: (value: unknown) => void): void;
    [Symbol.observable](): ObservableLike;
}
export interface NodeStream extends NodeJS.EventEmitter {
    pipe<T extends NodeJS.WritableStream>(destination: T, options?: {
        end?: boolean;
    }): T;
}
export declare type Predicate = (value: unknown) => boolean;
export declare const enum AssertionTypeDescription {
    class_ = "Class",
    numericString = "string with a number",
    nullOrUndefined = "null or undefined",
    iterable = "Iterable",
    asyncIterable = "AsyncIterable",
    nativePromise = "native Promise",
    urlString = "string with a URL",
    truthy = "truthy",
    falsy = "falsy",
    nan = "NaN",
    primitive = "primitive",
    integer = "integer",
    safeInteger = "integer",
    plainObject = "plain object",
    arrayLike = "array-like",
    typedArray = "TypedArray",
    domElement = "Element",
    nodeStream = "Node.js Stream",
    infinite = "infinite number",
    emptyArray = "empty array",
    nonEmptyArray = "non-empty array",
    emptyString = "empty string",
    nonEmptyString = "non-empty string",
    emptyStringOrWhitespace = "empty string or whitespace",
    emptyObject = "empty object",
    nonEmptyObject = "non-empty object",
    emptySet = "empty set",
    nonEmptySet = "non-empty set",
    emptyMap = "empty map",
    nonEmptyMap = "non-empty map",
    evenInteger = "even integer",
    oddInteger = "odd integer",
    directInstanceOf = "T",
    inRange = "in range",
    any = "predicate returns truthy for any value",
    all = "predicate returns truthy for all values"
}
interface Assert {
    undefined: (value: unknown) => asserts value is undefined;
    string: (value: unknown) => asserts value is string;
    number: (value: unknown) => asserts value is number;
    bigint: (value: unknown) => asserts value is bigint;
    function_: (value: unknown) => asserts value is Function;
    null_: (value: unknown) => asserts value is null;
    class_: (value: unknown) => asserts value is Class;
    boolean: (value: unknown) => asserts value is boolean;
    symbol: (value: unknown) => asserts value is symbol;
    numericString: (value: unknown) => asserts value is string;
    array: <T = unknown>(value: unknown) => asserts value is T[];
    buffer: (value: unknown) => asserts value is Buffer;
    nullOrUndefined: (value: unknown) => asserts value is null | undefined;
    object: <Key extends keyof any = string, Value = unknown>(value: unknown) => asserts value is Record<Key, Value>;
    iterable: <T = unknown>(value: unknown) => asserts value is Iterable<T>;
    asyncIterable: <T = unknown>(value: unknown) => asserts value is AsyncIterable<T>;
    generator: (value: unknown) => asserts value is Generator;
    asyncGenerator: (value: unknown) => asserts value is AsyncGenerator;
    nativePromise: <T = unknown>(value: unknown) => asserts value is Promise<T>;
    promise: <T = unknown>(value: unknown) => asserts value is Promise<T>;
    generatorFunction: (value: unknown) => asserts value is GeneratorFunction;
    asyncGeneratorFunction: (value: unknown) => asserts value is AsyncGeneratorFunction;
    asyncFunction: (value: unknown) => asserts value is Function;
    boundFunction: (value: unknown) => asserts value is Function;
    regExp: (value: unknown) => asserts value is RegExp;
    date: (value: unknown) => asserts value is Date;
    error: (value: unknown) => asserts value is Error;
    map: <Key = unknown, Value = unknown>(value: unknown) => asserts value is Map<Key, Value>;
    set: <T = unknown>(value: unknown) => asserts value is Set<T>;
    weakMap: <Key extends object = object, Value = unknown>(value: unknown) => asserts value is WeakMap<Key, Value>;
    weakSet: <T extends object = object>(value: unknown) => asserts value is WeakSet<T>;
    int8Array: (value: unknown) => asserts value is Int8Array;
    uint8Array: (value: unknown) => asserts value is Uint8Array;
    uint8ClampedArray: (value: unknown) => asserts value is Uint8ClampedArray;
    int16Array: (value: unknown) => asserts value is Int16Array;
    uint16Array: (value: unknown) => asserts value is Uint16Array;
    int32Array: (value: unknown) => asserts value is Int32Array;
    uint32Array: (value: unknown) => asserts value is Uint32Array;
    float32Array: (value: unknown) => asserts value is Float32Array;
    float64Array: (value: unknown) => asserts value is Float64Array;
    bigInt64Array: (value: unknown) => asserts value is BigInt64Array;
    bigUint64Array: (value: unknown) => asserts value is BigUint64Array;
    arrayBuffer: (value: unknown) => asserts value is ArrayBuffer;
    sharedArrayBuffer: (value: unknown) => asserts value is SharedArrayBuffer;
    dataView: (value: unknown) => asserts value is DataView;
    urlInstance: (value: unknown) => asserts value is URL;
    urlString: (value: unknown) => asserts value is string;
    truthy: (value: unknown) => asserts value is unknown;
    falsy: (value: unknown) => asserts value is unknown;
    nan: (value: unknown) => asserts value is unknown;
    primitive: (value: unknown) => asserts value is Primitive;
    integer: (value: unknown) => asserts value is number;
    safeInteger: (value: unknown) => asserts value is number;
    plainObject: <Value = unknown>(value: unknown) => asserts value is Record<string, Value>;
    typedArray: (value: unknown) => asserts value is TypedArray;
    arrayLike: <T = unknown>(value: unknown) => asserts value is ArrayLike<T>;
    domElement: (value: unknown) => asserts value is Element;
    observable: (value: unknown) => asserts value is ObservableLike;
    nodeStream: (value: unknown) => asserts value is NodeStream;
    infinite: (value: unknown) => asserts value is number;
    emptyArray: (value: unknown) => asserts value is never[];
    nonEmptyArray: (value: unknown) => asserts value is unknown[];
    emptyString: (value: unknown) => asserts value is '';
    nonEmptyString: (value: unknown) => asserts value is string;
    emptyStringOrWhitespace: (value: unknown) => asserts value is string;
    emptyObject: <Key extends keyof any = string>(value: unknown) => asserts value is Record<Key, never>;
    nonEmptyObject: <Key extends keyof any = string, Value = unknown>(value: unknown) => asserts value is Record<Key, Value>;
    emptySet: (value: unknown) => asserts value is Set<never>;
    nonEmptySet: <T = unknown>(value: unknown) => asserts value is Set<T>;
    emptyMap: (value: unknown) => asserts value is Map<never, never>;
    nonEmptyMap: <Key = unknown, Value = unknown>(value: unknown) => asserts value is Map<Key, Value>;
    evenInteger: (value: number) => asserts value is number;
    oddInteger: (value: number) => asserts value is number;
    directInstanceOf: <T>(instance: unknown, class_: Class<T>) => asserts instance is T;
    inRange: (value: number, range: number | number[]) => asserts value is number;
    any: (predicate: Predicate | Predicate[], ...values: unknown[]) => void | never;
    all: (predicate: Predicate, ...values: unknown[]) => void | never;
}
export declare const assert: Assert;
export default is;
