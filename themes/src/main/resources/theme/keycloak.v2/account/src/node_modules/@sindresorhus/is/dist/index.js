"use strict";
/// <reference lib="es2018"/>
/// <reference lib="dom"/>
/// <reference types="node"/>
Object.defineProperty(exports, "__esModule", { value: true });
const { toString } = Object.prototype;
const isOfType = (type) => (value) => typeof value === type;
const getObjectType = (value) => {
    const objectName = toString.call(value).slice(8, -1);
    if (objectName) {
        return objectName;
    }
    return undefined;
};
const isObjectOfType = (type) => (value) => getObjectType(value) === type;
function is(value) {
    switch (value) {
        case null:
            return "null" /* null */;
        case true:
        case false:
            return "boolean" /* boolean */;
        default:
    }
    switch (typeof value) {
        case 'undefined':
            return "undefined" /* undefined */;
        case 'string':
            return "string" /* string */;
        case 'number':
            return "number" /* number */;
        case 'bigint':
            return "bigint" /* bigint */;
        case 'symbol':
            return "symbol" /* symbol */;
        default:
    }
    if (is.function_(value)) {
        return "Function" /* Function */;
    }
    if (is.observable(value)) {
        return "Observable" /* Observable */;
    }
    if (is.array(value)) {
        return "Array" /* Array */;
    }
    if (is.buffer(value)) {
        return "Buffer" /* Buffer */;
    }
    const tagType = getObjectType(value);
    if (tagType) {
        return tagType;
    }
    if (value instanceof String || value instanceof Boolean || value instanceof Number) {
        throw new TypeError('Please don\'t use object wrappers for primitive types');
    }
    return "Object" /* Object */;
}
is.undefined = isOfType('undefined');
is.string = isOfType('string');
const isNumberType = isOfType('number');
is.number = (value) => isNumberType(value) && !is.nan(value);
is.bigint = isOfType('bigint');
// eslint-disable-next-line @typescript-eslint/ban-types
is.function_ = isOfType('function');
is.null_ = (value) => value === null;
is.class_ = (value) => is.function_(value) && value.toString().startsWith('class ');
is.boolean = (value) => value === true || value === false;
is.symbol = isOfType('symbol');
is.numericString = (value) => is.string(value) && !is.emptyStringOrWhitespace(value) && !Number.isNaN(Number(value));
is.array = Array.isArray;
is.buffer = (value) => { var _a, _b, _c, _d; return (_d = (_c = (_b = (_a = value) === null || _a === void 0 ? void 0 : _a.constructor) === null || _b === void 0 ? void 0 : _b.isBuffer) === null || _c === void 0 ? void 0 : _c.call(_b, value)) !== null && _d !== void 0 ? _d : false; };
is.nullOrUndefined = (value) => is.null_(value) || is.undefined(value);
is.object = (value) => !is.null_(value) && (typeof value === 'object' || is.function_(value));
is.iterable = (value) => { var _a; return is.function_((_a = value) === null || _a === void 0 ? void 0 : _a[Symbol.iterator]); };
is.asyncIterable = (value) => { var _a; return is.function_((_a = value) === null || _a === void 0 ? void 0 : _a[Symbol.asyncIterator]); };
is.generator = (value) => is.iterable(value) && is.function_(value.next) && is.function_(value.throw);
is.asyncGenerator = (value) => is.asyncIterable(value) && is.function_(value.next) && is.function_(value.throw);
is.nativePromise = (value) => isObjectOfType("Promise" /* Promise */)(value);
const hasPromiseAPI = (value) => {
    var _a, _b;
    return is.function_((_a = value) === null || _a === void 0 ? void 0 : _a.then) &&
        is.function_((_b = value) === null || _b === void 0 ? void 0 : _b.catch);
};
is.promise = (value) => is.nativePromise(value) || hasPromiseAPI(value);
is.generatorFunction = isObjectOfType("GeneratorFunction" /* GeneratorFunction */);
is.asyncGeneratorFunction = (value) => getObjectType(value) === "AsyncGeneratorFunction" /* AsyncGeneratorFunction */;
is.asyncFunction = (value) => getObjectType(value) === "AsyncFunction" /* AsyncFunction */;
// eslint-disable-next-line no-prototype-builtins, @typescript-eslint/ban-types
is.boundFunction = (value) => is.function_(value) && !value.hasOwnProperty('prototype');
is.regExp = isObjectOfType("RegExp" /* RegExp */);
is.date = isObjectOfType("Date" /* Date */);
is.error = isObjectOfType("Error" /* Error */);
is.map = (value) => isObjectOfType("Map" /* Map */)(value);
is.set = (value) => isObjectOfType("Set" /* Set */)(value);
is.weakMap = (value) => isObjectOfType("WeakMap" /* WeakMap */)(value);
is.weakSet = (value) => isObjectOfType("WeakSet" /* WeakSet */)(value);
is.int8Array = isObjectOfType("Int8Array" /* Int8Array */);
is.uint8Array = isObjectOfType("Uint8Array" /* Uint8Array */);
is.uint8ClampedArray = isObjectOfType("Uint8ClampedArray" /* Uint8ClampedArray */);
is.int16Array = isObjectOfType("Int16Array" /* Int16Array */);
is.uint16Array = isObjectOfType("Uint16Array" /* Uint16Array */);
is.int32Array = isObjectOfType("Int32Array" /* Int32Array */);
is.uint32Array = isObjectOfType("Uint32Array" /* Uint32Array */);
is.float32Array = isObjectOfType("Float32Array" /* Float32Array */);
is.float64Array = isObjectOfType("Float64Array" /* Float64Array */);
is.bigInt64Array = isObjectOfType("BigInt64Array" /* BigInt64Array */);
is.bigUint64Array = isObjectOfType("BigUint64Array" /* BigUint64Array */);
is.arrayBuffer = isObjectOfType("ArrayBuffer" /* ArrayBuffer */);
is.sharedArrayBuffer = isObjectOfType("SharedArrayBuffer" /* SharedArrayBuffer */);
is.dataView = isObjectOfType("DataView" /* DataView */);
is.directInstanceOf = (instance, class_) => Object.getPrototypeOf(instance) === class_.prototype;
is.urlInstance = (value) => isObjectOfType("URL" /* URL */)(value);
is.urlString = (value) => {
    if (!is.string(value)) {
        return false;
    }
    try {
        new URL(value); // eslint-disable-line no-new
        return true;
    }
    catch (_a) {
        return false;
    }
};
// TODO: Use the `not` operator with a type guard here when it's available.
// Example: `is.truthy = (value: unknown): value is (not false | not 0 | not '' | not undefined | not null) => Boolean(value);`
is.truthy = (value) => Boolean(value);
// Example: `is.falsy = (value: unknown): value is (not true | 0 | '' | undefined | null) => Boolean(value);`
is.falsy = (value) => !value;
is.nan = (value) => Number.isNaN(value);
const primitiveTypeOfTypes = new Set([
    'undefined',
    'string',
    'number',
    'bigint',
    'boolean',
    'symbol'
]);
is.primitive = (value) => is.null_(value) || primitiveTypeOfTypes.has(typeof value);
is.integer = (value) => Number.isInteger(value);
is.safeInteger = (value) => Number.isSafeInteger(value);
is.plainObject = (value) => {
    // From: https://github.com/sindresorhus/is-plain-obj/blob/master/index.js
    if (getObjectType(value) !== "Object" /* Object */) {
        return false;
    }
    const prototype = Object.getPrototypeOf(value);
    return prototype === null || prototype === Object.getPrototypeOf({});
};
const typedArrayTypes = new Set([
    "Int8Array" /* Int8Array */,
    "Uint8Array" /* Uint8Array */,
    "Uint8ClampedArray" /* Uint8ClampedArray */,
    "Int16Array" /* Int16Array */,
    "Uint16Array" /* Uint16Array */,
    "Int32Array" /* Int32Array */,
    "Uint32Array" /* Uint32Array */,
    "Float32Array" /* Float32Array */,
    "Float64Array" /* Float64Array */,
    "BigInt64Array" /* BigInt64Array */,
    "BigUint64Array" /* BigUint64Array */
]);
is.typedArray = (value) => {
    const objectType = getObjectType(value);
    if (objectType === undefined) {
        return false;
    }
    return typedArrayTypes.has(objectType);
};
const isValidLength = (value) => is.safeInteger(value) && value >= 0;
is.arrayLike = (value) => !is.nullOrUndefined(value) && !is.function_(value) && isValidLength(value.length);
is.inRange = (value, range) => {
    if (is.number(range)) {
        return value >= Math.min(0, range) && value <= Math.max(range, 0);
    }
    if (is.array(range) && range.length === 2) {
        return value >= Math.min(...range) && value <= Math.max(...range);
    }
    throw new TypeError(`Invalid range: ${JSON.stringify(range)}`);
};
const NODE_TYPE_ELEMENT = 1;
const DOM_PROPERTIES_TO_CHECK = [
    'innerHTML',
    'ownerDocument',
    'style',
    'attributes',
    'nodeValue'
];
is.domElement = (value) => is.object(value) && value.nodeType === NODE_TYPE_ELEMENT && is.string(value.nodeName) &&
    !is.plainObject(value) && DOM_PROPERTIES_TO_CHECK.every(property => property in value);
is.observable = (value) => {
    var _a, _b, _c, _d;
    if (!value) {
        return false;
    }
    // eslint-disable-next-line no-use-extend-native/no-use-extend-native
    if (value === ((_b = (_a = value)[Symbol.observable]) === null || _b === void 0 ? void 0 : _b.call(_a))) {
        return true;
    }
    if (value === ((_d = (_c = value)['@@observable']) === null || _d === void 0 ? void 0 : _d.call(_c))) {
        return true;
    }
    return false;
};
is.nodeStream = (value) => is.object(value) && is.function_(value.pipe) && !is.observable(value);
is.infinite = (value) => value === Infinity || value === -Infinity;
const isAbsoluteMod2 = (remainder) => (value) => is.integer(value) && Math.abs(value % 2) === remainder;
is.evenInteger = isAbsoluteMod2(0);
is.oddInteger = isAbsoluteMod2(1);
is.emptyArray = (value) => is.array(value) && value.length === 0;
is.nonEmptyArray = (value) => is.array(value) && value.length > 0;
is.emptyString = (value) => is.string(value) && value.length === 0;
// TODO: Use `not ''` when the `not` operator is available.
is.nonEmptyString = (value) => is.string(value) && value.length > 0;
const isWhiteSpaceString = (value) => is.string(value) && !/\S/.test(value);
is.emptyStringOrWhitespace = (value) => is.emptyString(value) || isWhiteSpaceString(value);
is.emptyObject = (value) => is.object(value) && !is.map(value) && !is.set(value) && Object.keys(value).length === 0;
// TODO: Use `not` operator here to remove `Map` and `Set` from type guard:
// - https://github.com/Microsoft/TypeScript/pull/29317
is.nonEmptyObject = (value) => is.object(value) && !is.map(value) && !is.set(value) && Object.keys(value).length > 0;
is.emptySet = (value) => is.set(value) && value.size === 0;
is.nonEmptySet = (value) => is.set(value) && value.size > 0;
is.emptyMap = (value) => is.map(value) && value.size === 0;
is.nonEmptyMap = (value) => is.map(value) && value.size > 0;
const predicateOnArray = (method, predicate, values) => {
    if (!is.function_(predicate)) {
        throw new TypeError(`Invalid predicate: ${JSON.stringify(predicate)}`);
    }
    if (values.length === 0) {
        throw new TypeError('Invalid number of values');
    }
    return method.call(values, predicate);
};
is.any = (predicate, ...values) => {
    const predicates = is.array(predicate) ? predicate : [predicate];
    return predicates.some(singlePredicate => predicateOnArray(Array.prototype.some, singlePredicate, values));
};
is.all = (predicate, ...values) => predicateOnArray(Array.prototype.every, predicate, values);
const assertType = (condition, description, value) => {
    if (!condition) {
        throw new TypeError(`Expected value which is \`${description}\`, received value of type \`${is(value)}\`.`);
    }
};
exports.assert = {
    // Unknowns.
    undefined: (value) => assertType(is.undefined(value), "undefined" /* undefined */, value),
    string: (value) => assertType(is.string(value), "string" /* string */, value),
    number: (value) => assertType(is.number(value), "number" /* number */, value),
    bigint: (value) => assertType(is.bigint(value), "bigint" /* bigint */, value),
    // eslint-disable-next-line @typescript-eslint/ban-types
    function_: (value) => assertType(is.function_(value), "Function" /* Function */, value),
    null_: (value) => assertType(is.null_(value), "null" /* null */, value),
    class_: (value) => assertType(is.class_(value), "Class" /* class_ */, value),
    boolean: (value) => assertType(is.boolean(value), "boolean" /* boolean */, value),
    symbol: (value) => assertType(is.symbol(value), "symbol" /* symbol */, value),
    numericString: (value) => assertType(is.numericString(value), "string with a number" /* numericString */, value),
    array: (value) => assertType(is.array(value), "Array" /* Array */, value),
    buffer: (value) => assertType(is.buffer(value), "Buffer" /* Buffer */, value),
    nullOrUndefined: (value) => assertType(is.nullOrUndefined(value), "null or undefined" /* nullOrUndefined */, value),
    object: (value) => assertType(is.object(value), "Object" /* Object */, value),
    iterable: (value) => assertType(is.iterable(value), "Iterable" /* iterable */, value),
    asyncIterable: (value) => assertType(is.asyncIterable(value), "AsyncIterable" /* asyncIterable */, value),
    generator: (value) => assertType(is.generator(value), "Generator" /* Generator */, value),
    asyncGenerator: (value) => assertType(is.asyncGenerator(value), "AsyncGenerator" /* AsyncGenerator */, value),
    nativePromise: (value) => assertType(is.nativePromise(value), "native Promise" /* nativePromise */, value),
    promise: (value) => assertType(is.promise(value), "Promise" /* Promise */, value),
    generatorFunction: (value) => assertType(is.generatorFunction(value), "GeneratorFunction" /* GeneratorFunction */, value),
    asyncGeneratorFunction: (value) => assertType(is.asyncGeneratorFunction(value), "AsyncGeneratorFunction" /* AsyncGeneratorFunction */, value),
    // eslint-disable-next-line @typescript-eslint/ban-types
    asyncFunction: (value) => assertType(is.asyncFunction(value), "AsyncFunction" /* AsyncFunction */, value),
    // eslint-disable-next-line @typescript-eslint/ban-types
    boundFunction: (value) => assertType(is.boundFunction(value), "Function" /* Function */, value),
    regExp: (value) => assertType(is.regExp(value), "RegExp" /* RegExp */, value),
    date: (value) => assertType(is.date(value), "Date" /* Date */, value),
    error: (value) => assertType(is.error(value), "Error" /* Error */, value),
    map: (value) => assertType(is.map(value), "Map" /* Map */, value),
    set: (value) => assertType(is.set(value), "Set" /* Set */, value),
    weakMap: (value) => assertType(is.weakMap(value), "WeakMap" /* WeakMap */, value),
    weakSet: (value) => assertType(is.weakSet(value), "WeakSet" /* WeakSet */, value),
    int8Array: (value) => assertType(is.int8Array(value), "Int8Array" /* Int8Array */, value),
    uint8Array: (value) => assertType(is.uint8Array(value), "Uint8Array" /* Uint8Array */, value),
    uint8ClampedArray: (value) => assertType(is.uint8ClampedArray(value), "Uint8ClampedArray" /* Uint8ClampedArray */, value),
    int16Array: (value) => assertType(is.int16Array(value), "Int16Array" /* Int16Array */, value),
    uint16Array: (value) => assertType(is.uint16Array(value), "Uint16Array" /* Uint16Array */, value),
    int32Array: (value) => assertType(is.int32Array(value), "Int32Array" /* Int32Array */, value),
    uint32Array: (value) => assertType(is.uint32Array(value), "Uint32Array" /* Uint32Array */, value),
    float32Array: (value) => assertType(is.float32Array(value), "Float32Array" /* Float32Array */, value),
    float64Array: (value) => assertType(is.float64Array(value), "Float64Array" /* Float64Array */, value),
    bigInt64Array: (value) => assertType(is.bigInt64Array(value), "BigInt64Array" /* BigInt64Array */, value),
    bigUint64Array: (value) => assertType(is.bigUint64Array(value), "BigUint64Array" /* BigUint64Array */, value),
    arrayBuffer: (value) => assertType(is.arrayBuffer(value), "ArrayBuffer" /* ArrayBuffer */, value),
    sharedArrayBuffer: (value) => assertType(is.sharedArrayBuffer(value), "SharedArrayBuffer" /* SharedArrayBuffer */, value),
    dataView: (value) => assertType(is.dataView(value), "DataView" /* DataView */, value),
    urlInstance: (value) => assertType(is.urlInstance(value), "URL" /* URL */, value),
    urlString: (value) => assertType(is.urlString(value), "string with a URL" /* urlString */, value),
    truthy: (value) => assertType(is.truthy(value), "truthy" /* truthy */, value),
    falsy: (value) => assertType(is.falsy(value), "falsy" /* falsy */, value),
    nan: (value) => assertType(is.nan(value), "NaN" /* nan */, value),
    primitive: (value) => assertType(is.primitive(value), "primitive" /* primitive */, value),
    integer: (value) => assertType(is.integer(value), "integer" /* integer */, value),
    safeInteger: (value) => assertType(is.safeInteger(value), "integer" /* safeInteger */, value),
    plainObject: (value) => assertType(is.plainObject(value), "plain object" /* plainObject */, value),
    typedArray: (value) => assertType(is.typedArray(value), "TypedArray" /* typedArray */, value),
    arrayLike: (value) => assertType(is.arrayLike(value), "array-like" /* arrayLike */, value),
    domElement: (value) => assertType(is.domElement(value), "Element" /* domElement */, value),
    observable: (value) => assertType(is.observable(value), "Observable" /* Observable */, value),
    nodeStream: (value) => assertType(is.nodeStream(value), "Node.js Stream" /* nodeStream */, value),
    infinite: (value) => assertType(is.infinite(value), "infinite number" /* infinite */, value),
    emptyArray: (value) => assertType(is.emptyArray(value), "empty array" /* emptyArray */, value),
    nonEmptyArray: (value) => assertType(is.nonEmptyArray(value), "non-empty array" /* nonEmptyArray */, value),
    emptyString: (value) => assertType(is.emptyString(value), "empty string" /* emptyString */, value),
    nonEmptyString: (value) => assertType(is.nonEmptyString(value), "non-empty string" /* nonEmptyString */, value),
    emptyStringOrWhitespace: (value) => assertType(is.emptyStringOrWhitespace(value), "empty string or whitespace" /* emptyStringOrWhitespace */, value),
    emptyObject: (value) => assertType(is.emptyObject(value), "empty object" /* emptyObject */, value),
    nonEmptyObject: (value) => assertType(is.nonEmptyObject(value), "non-empty object" /* nonEmptyObject */, value),
    emptySet: (value) => assertType(is.emptySet(value), "empty set" /* emptySet */, value),
    nonEmptySet: (value) => assertType(is.nonEmptySet(value), "non-empty set" /* nonEmptySet */, value),
    emptyMap: (value) => assertType(is.emptyMap(value), "empty map" /* emptyMap */, value),
    nonEmptyMap: (value) => assertType(is.nonEmptyMap(value), "non-empty map" /* nonEmptyMap */, value),
    // Numbers.
    evenInteger: (value) => assertType(is.evenInteger(value), "even integer" /* evenInteger */, value),
    oddInteger: (value) => assertType(is.oddInteger(value), "odd integer" /* oddInteger */, value),
    // Two arguments.
    directInstanceOf: (instance, class_) => assertType(is.directInstanceOf(instance, class_), "T" /* directInstanceOf */, instance),
    inRange: (value, range) => assertType(is.inRange(value, range), "in range" /* inRange */, value),
    // Variadic functions.
    any: (predicate, ...values) => assertType(is.any(predicate, ...values), "predicate returns truthy for any value" /* any */, values),
    all: (predicate, ...values) => assertType(is.all(predicate, ...values), "predicate returns truthy for all values" /* all */, values)
};
// Some few keywords are reserved, but we'll populate them for Node.js users
// See https://github.com/Microsoft/TypeScript/issues/2536
Object.defineProperties(is, {
    class: {
        value: is.class_
    },
    function: {
        value: is.function_
    },
    null: {
        value: is.null_
    }
});
Object.defineProperties(exports.assert, {
    class: {
        value: exports.assert.class_
    },
    function: {
        value: exports.assert.function_
    },
    null: {
        value: exports.assert.null_
    }
});
exports.default = is;
// For CommonJS default export support
module.exports = is;
module.exports.default = is;
module.exports.assert = exports.assert;
