'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

var helperPluginUtils = require('@babel/helper-plugin-utils');
var syntaxObjectRestSpread = require('@babel/plugin-syntax-object-rest-spread');
var core = require('@babel/core');
var pluginTransformParameters = require('@babel/plugin-transform-parameters');
var helperCompilationTargets = require('@babel/helper-compilation-targets');

function _interopDefaultLegacy (e) { return e && typeof e === 'object' && 'default' in e ? e : { 'default': e }; }

var syntaxObjectRestSpread__default = /*#__PURE__*/_interopDefaultLegacy(syntaxObjectRestSpread);

var require$$0 = {
	"es6.array.copy-within": {
	chrome: "45",
	opera: "32",
	edge: "12",
	firefox: "32",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "5",
	rhino: "1.7.13",
	electron: "0.31"
},
	"es6.array.every": {
	chrome: "5",
	opera: "10.10",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.fill": {
	chrome: "45",
	opera: "32",
	edge: "12",
	firefox: "31",
	safari: "7.1",
	node: "4",
	ios: "8",
	samsung: "5",
	rhino: "1.7.13",
	electron: "0.31"
},
	"es6.array.filter": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.array.find": {
	chrome: "45",
	opera: "32",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "4",
	ios: "8",
	samsung: "5",
	rhino: "1.7.13",
	electron: "0.31"
},
	"es6.array.find-index": {
	chrome: "45",
	opera: "32",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "4",
	ios: "8",
	samsung: "5",
	rhino: "1.7.13",
	electron: "0.31"
},
	"es7.array.flat-map": {
	chrome: "69",
	opera: "56",
	edge: "79",
	firefox: "62",
	safari: "12",
	node: "11",
	ios: "12",
	samsung: "10",
	electron: "4.0"
},
	"es6.array.for-each": {
	chrome: "5",
	opera: "10.10",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.from": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "36",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es7.array.includes": {
	chrome: "47",
	opera: "34",
	edge: "14",
	firefox: "102",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.36"
},
	"es6.array.index-of": {
	chrome: "5",
	opera: "10.10",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.is-array": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "4",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.iterator": {
	chrome: "66",
	opera: "53",
	edge: "12",
	firefox: "60",
	safari: "9",
	node: "10",
	ios: "9",
	samsung: "9",
	rhino: "1.7.13",
	electron: "3.0"
},
	"es6.array.last-index-of": {
	chrome: "5",
	opera: "10.10",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.map": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.array.of": {
	chrome: "45",
	opera: "32",
	edge: "12",
	firefox: "25",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "5",
	rhino: "1.7.13",
	electron: "0.31"
},
	"es6.array.reduce": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "3",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.reduce-right": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "3",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.slice": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.array.some": {
	chrome: "5",
	opera: "10.10",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.array.sort": {
	chrome: "63",
	opera: "50",
	edge: "12",
	firefox: "5",
	safari: "12",
	node: "10",
	ie: "9",
	ios: "12",
	samsung: "8",
	rhino: "1.7.13",
	electron: "3.0"
},
	"es6.array.species": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.date.now": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "2",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.date.to-iso-string": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "3.5",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.date.to-json": {
	chrome: "5",
	opera: "12.10",
	edge: "12",
	firefox: "4",
	safari: "10",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "10",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.date.to-primitive": {
	chrome: "47",
	opera: "34",
	edge: "15",
	firefox: "44",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.36"
},
	"es6.date.to-string": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "2",
	safari: "3.1",
	node: "0.4",
	ie: "10",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.function.bind": {
	chrome: "7",
	opera: "12",
	edge: "12",
	firefox: "4",
	safari: "5.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.function.has-instance": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "50",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.function.name": {
	chrome: "5",
	opera: "10.50",
	edge: "14",
	firefox: "2",
	safari: "4",
	node: "0.4",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.map": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "53",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.math.acosh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.asinh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.atanh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.cbrt": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.clz32": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "31",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.cosh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.expm1": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.fround": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "26",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.hypot": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "27",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.imul": {
	chrome: "30",
	opera: "17",
	edge: "12",
	firefox: "23",
	safari: "7",
	node: "0.12",
	android: "4.4",
	ios: "7",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.log1p": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.log10": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.log2": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.sign": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.sinh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.tanh": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.math.trunc": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "25",
	safari: "7.1",
	node: "0.12",
	ios: "8",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.constructor": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "36",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.number.epsilon": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "25",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.number.is-finite": {
	chrome: "19",
	opera: "15",
	edge: "12",
	firefox: "16",
	safari: "9",
	node: "0.8",
	android: "4.1",
	ios: "9",
	samsung: "1.5",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.is-integer": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "16",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.is-nan": {
	chrome: "19",
	opera: "15",
	edge: "12",
	firefox: "15",
	safari: "9",
	node: "0.8",
	android: "4.1",
	ios: "9",
	samsung: "1.5",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.is-safe-integer": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "32",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.max-safe-integer": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "31",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.min-safe-integer": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "31",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.number.parse-float": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "25",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.number.parse-int": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "25",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "2",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.object.assign": {
	chrome: "49",
	opera: "36",
	edge: "13",
	firefox: "36",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.object.create": {
	chrome: "5",
	opera: "12",
	edge: "12",
	firefox: "4",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es7.object.define-getter": {
	chrome: "62",
	opera: "49",
	edge: "16",
	firefox: "48",
	safari: "9",
	node: "8.10",
	ios: "9",
	samsung: "8",
	electron: "3.0"
},
	"es7.object.define-setter": {
	chrome: "62",
	opera: "49",
	edge: "16",
	firefox: "48",
	safari: "9",
	node: "8.10",
	ios: "9",
	samsung: "8",
	electron: "3.0"
},
	"es6.object.define-property": {
	chrome: "5",
	opera: "12",
	edge: "12",
	firefox: "4",
	safari: "5.1",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.object.define-properties": {
	chrome: "5",
	opera: "12",
	edge: "12",
	firefox: "4",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es7.object.entries": {
	chrome: "54",
	opera: "41",
	edge: "14",
	firefox: "47",
	safari: "10.1",
	node: "7",
	ios: "10.3",
	samsung: "6",
	rhino: "1.7.14",
	electron: "1.4"
},
	"es6.object.freeze": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.get-own-property-descriptor": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es7.object.get-own-property-descriptors": {
	chrome: "54",
	opera: "41",
	edge: "15",
	firefox: "50",
	safari: "10.1",
	node: "7",
	ios: "10.3",
	samsung: "6",
	electron: "1.4"
},
	"es6.object.get-own-property-names": {
	chrome: "40",
	opera: "27",
	edge: "12",
	firefox: "33",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.object.get-prototype-of": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es7.object.lookup-getter": {
	chrome: "62",
	opera: "49",
	edge: "79",
	firefox: "36",
	safari: "9",
	node: "8.10",
	ios: "9",
	samsung: "8",
	electron: "3.0"
},
	"es7.object.lookup-setter": {
	chrome: "62",
	opera: "49",
	edge: "79",
	firefox: "36",
	safari: "9",
	node: "8.10",
	ios: "9",
	samsung: "8",
	electron: "3.0"
},
	"es6.object.prevent-extensions": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.to-string": {
	chrome: "57",
	opera: "44",
	edge: "15",
	firefox: "51",
	safari: "10",
	node: "8",
	ios: "10",
	samsung: "7",
	electron: "1.7"
},
	"es6.object.is": {
	chrome: "19",
	opera: "15",
	edge: "12",
	firefox: "22",
	safari: "9",
	node: "0.8",
	android: "4.1",
	ios: "9",
	samsung: "1.5",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.object.is-frozen": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.is-sealed": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.is-extensible": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.keys": {
	chrome: "40",
	opera: "27",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.object.seal": {
	chrome: "44",
	opera: "31",
	edge: "12",
	firefox: "35",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "4",
	rhino: "1.7.13",
	electron: "0.30"
},
	"es6.object.set-prototype-of": {
	chrome: "34",
	opera: "21",
	edge: "12",
	firefox: "31",
	safari: "9",
	node: "0.12",
	ie: "11",
	ios: "9",
	samsung: "2",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es7.object.values": {
	chrome: "54",
	opera: "41",
	edge: "14",
	firefox: "47",
	safari: "10.1",
	node: "7",
	ios: "10.3",
	samsung: "6",
	rhino: "1.7.14",
	electron: "1.4"
},
	"es6.promise": {
	chrome: "51",
	opera: "38",
	edge: "14",
	firefox: "45",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es7.promise.finally": {
	chrome: "63",
	opera: "50",
	edge: "18",
	firefox: "58",
	safari: "11.1",
	node: "10",
	ios: "11.3",
	samsung: "8",
	electron: "3.0"
},
	"es6.reflect.apply": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.construct": {
	chrome: "49",
	opera: "36",
	edge: "13",
	firefox: "49",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.define-property": {
	chrome: "49",
	opera: "36",
	edge: "13",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.delete-property": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.get": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.get-own-property-descriptor": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.get-prototype-of": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.has": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.is-extensible": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.own-keys": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.prevent-extensions": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.set": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.reflect.set-prototype-of": {
	chrome: "49",
	opera: "36",
	edge: "12",
	firefox: "42",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "0.37"
},
	"es6.regexp.constructor": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "40",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "1.1"
},
	"es6.regexp.flags": {
	chrome: "49",
	opera: "36",
	edge: "79",
	firefox: "37",
	safari: "9",
	node: "6",
	ios: "9",
	samsung: "5",
	electron: "0.37"
},
	"es6.regexp.match": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "49",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	rhino: "1.7.13",
	electron: "1.1"
},
	"es6.regexp.replace": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "49",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "1.1"
},
	"es6.regexp.split": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "49",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "1.1"
},
	"es6.regexp.search": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "49",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	rhino: "1.7.13",
	electron: "1.1"
},
	"es6.regexp.to-string": {
	chrome: "50",
	opera: "37",
	edge: "79",
	firefox: "39",
	safari: "10",
	node: "6",
	ios: "10",
	samsung: "5",
	electron: "1.1"
},
	"es6.set": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "53",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.symbol": {
	chrome: "51",
	opera: "38",
	edge: "79",
	firefox: "51",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es7.symbol.async-iterator": {
	chrome: "63",
	opera: "50",
	edge: "79",
	firefox: "57",
	safari: "12",
	node: "10",
	ios: "12",
	samsung: "8",
	electron: "3.0"
},
	"es6.string.anchor": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.big": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.blink": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.bold": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.code-point-at": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "29",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.ends-with": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "29",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.fixed": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.fontcolor": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.fontsize": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.from-code-point": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "29",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.includes": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "40",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.italics": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.iterator": {
	chrome: "38",
	opera: "25",
	edge: "12",
	firefox: "36",
	safari: "9",
	node: "0.12",
	ios: "9",
	samsung: "3",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.string.link": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es7.string.pad-start": {
	chrome: "57",
	opera: "44",
	edge: "15",
	firefox: "48",
	safari: "10",
	node: "8",
	ios: "10",
	samsung: "7",
	rhino: "1.7.13",
	electron: "1.7"
},
	"es7.string.pad-end": {
	chrome: "57",
	opera: "44",
	edge: "15",
	firefox: "48",
	safari: "10",
	node: "8",
	ios: "10",
	samsung: "7",
	rhino: "1.7.13",
	electron: "1.7"
},
	"es6.string.raw": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "34",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.14",
	electron: "0.21"
},
	"es6.string.repeat": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "24",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.small": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.starts-with": {
	chrome: "41",
	opera: "28",
	edge: "12",
	firefox: "29",
	safari: "9",
	node: "4",
	ios: "9",
	samsung: "3.4",
	rhino: "1.7.13",
	electron: "0.21"
},
	"es6.string.strike": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.sub": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.sup": {
	chrome: "5",
	opera: "15",
	edge: "12",
	firefox: "17",
	safari: "6",
	node: "0.4",
	android: "4",
	ios: "7",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.14",
	electron: "0.20"
},
	"es6.string.trim": {
	chrome: "5",
	opera: "10.50",
	edge: "12",
	firefox: "3.5",
	safari: "4",
	node: "0.4",
	ie: "9",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es7.string.trim-left": {
	chrome: "66",
	opera: "53",
	edge: "79",
	firefox: "61",
	safari: "12",
	node: "10",
	ios: "12",
	samsung: "9",
	rhino: "1.7.13",
	electron: "3.0"
},
	"es7.string.trim-right": {
	chrome: "66",
	opera: "53",
	edge: "79",
	firefox: "61",
	safari: "12",
	node: "10",
	ios: "12",
	samsung: "9",
	rhino: "1.7.13",
	electron: "3.0"
},
	"es6.typed.array-buffer": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.data-view": {
	chrome: "5",
	opera: "12",
	edge: "12",
	firefox: "15",
	safari: "5.1",
	node: "0.4",
	ie: "10",
	android: "4",
	ios: "6",
	phantom: "1.9",
	samsung: "1",
	rhino: "1.7.13",
	electron: "0.20"
},
	"es6.typed.int8-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.uint8-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.uint8-clamped-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.int16-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.uint16-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.int32-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.uint32-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.float32-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.typed.float64-array": {
	chrome: "51",
	opera: "38",
	edge: "13",
	firefox: "48",
	safari: "10",
	node: "6.5",
	ios: "10",
	samsung: "5",
	electron: "1.2"
},
	"es6.weak-map": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "53",
	safari: "9",
	node: "6.5",
	ios: "9",
	samsung: "5",
	electron: "1.2"
},
	"es6.weak-set": {
	chrome: "51",
	opera: "38",
	edge: "15",
	firefox: "53",
	safari: "9",
	node: "6.5",
	ios: "9",
	samsung: "5",
	electron: "1.2"
}
};

var corejs2BuiltIns = require$$0;

const {
  isObjectProperty: isObjectProperty$1,
  isArrayPattern,
  isObjectPattern,
  isAssignmentPattern: isAssignmentPattern$1,
  isRestElement,
  isIdentifier
} = core.types;
function shouldStoreRHSInTemporaryVariable(node) {
  if (isArrayPattern(node)) {
    const nonNullElements = node.elements.filter(element => element !== null);
    if (nonNullElements.length > 1) return true;else return shouldStoreRHSInTemporaryVariable(nonNullElements[0]);
  } else if (isObjectPattern(node)) {
    const {
      properties
    } = node;
    if (properties.length > 1) return true;else if (properties.length === 0) return false;else {
      const firstProperty = properties[0];

      if (isObjectProperty$1(firstProperty)) {
        return shouldStoreRHSInTemporaryVariable(firstProperty.value);
      } else {
        return shouldStoreRHSInTemporaryVariable(firstProperty);
      }
    }
  } else if (isAssignmentPattern$1(node)) {
    return shouldStoreRHSInTemporaryVariable(node.left);
  } else if (isRestElement(node)) {
    if (isIdentifier(node.argument)) return true;
    return shouldStoreRHSInTemporaryVariable(node.argument);
  } else {
    return false;
  }
}

const {
  isAssignmentPattern,
  isObjectProperty
} = core.types;
{
  const node = core.types.identifier("a");
  const property = core.types.objectProperty(core.types.identifier("key"), node);
  const pattern = core.types.objectPattern([property]);
  var ZERO_REFS = core.types.isReferenced(node, property, pattern) ? 1 : 0;
}
var index = helperPluginUtils.declare((api, opts) => {
  var _api$assumption, _api$assumption2, _api$assumption3, _api$assumption4;

  api.assertVersion(7);
  const targets = api.targets();
  const supportsObjectAssign = !helperCompilationTargets.isRequired("es6.object.assign", targets, {
    compatData: corejs2BuiltIns
  });
  const {
    useBuiltIns = supportsObjectAssign,
    loose = false
  } = opts;

  if (typeof loose !== "boolean") {
    throw new Error(".loose must be a boolean, or undefined");
  }

  const ignoreFunctionLength = (_api$assumption = api.assumption("ignoreFunctionLength")) != null ? _api$assumption : loose;
  const objectRestNoSymbols = (_api$assumption2 = api.assumption("objectRestNoSymbols")) != null ? _api$assumption2 : loose;
  const pureGetters = (_api$assumption3 = api.assumption("pureGetters")) != null ? _api$assumption3 : loose;
  const setSpreadProperties = (_api$assumption4 = api.assumption("setSpreadProperties")) != null ? _api$assumption4 : loose;

  function getExtendsHelper(file) {
    return useBuiltIns ? core.types.memberExpression(core.types.identifier("Object"), core.types.identifier("assign")) : file.addHelper("extends");
  }

  function hasRestElement(path) {
    let foundRestElement = false;
    visitRestElements(path, restElement => {
      foundRestElement = true;
      restElement.stop();
    });
    return foundRestElement;
  }

  function hasObjectPatternRestElement(path) {
    let foundRestElement = false;
    visitRestElements(path, restElement => {
      if (restElement.parentPath.isObjectPattern()) {
        foundRestElement = true;
        restElement.stop();
      }
    });
    return foundRestElement;
  }

  function visitRestElements(path, visitor) {
    path.traverse({
      Expression(path) {
        const {
          parent,
          key
        } = path;

        if (isAssignmentPattern(parent) && key === "right" || isObjectProperty(parent) && parent.computed && key === "key") {
          path.skip();
        }
      },

      RestElement: visitor
    });
  }

  function hasSpread(node) {
    for (const prop of node.properties) {
      if (core.types.isSpreadElement(prop)) {
        return true;
      }
    }

    return false;
  }

  function extractNormalizedKeys(node) {
    const props = node.properties;
    const keys = [];
    let allLiteral = true;
    let hasTemplateLiteral = false;

    for (const prop of props) {
      if (core.types.isIdentifier(prop.key) && !prop.computed) {
        keys.push(core.types.stringLiteral(prop.key.name));
      } else if (core.types.isTemplateLiteral(prop.key)) {
        keys.push(core.types.cloneNode(prop.key));
        hasTemplateLiteral = true;
      } else if (core.types.isLiteral(prop.key)) {
        keys.push(core.types.stringLiteral(String(prop.key.value)));
      } else {
        keys.push(core.types.cloneNode(prop.key));
        allLiteral = false;
      }
    }

    return {
      keys,
      allLiteral,
      hasTemplateLiteral
    };
  }

  function replaceImpureComputedKeys(properties, scope) {
    const impureComputedPropertyDeclarators = [];

    for (const propPath of properties) {
      const key = propPath.get("key");

      if (propPath.node.computed && !key.isPure()) {
        const name = scope.generateUidBasedOnNode(key.node);
        const declarator = core.types.variableDeclarator(core.types.identifier(name), key.node);
        impureComputedPropertyDeclarators.push(declarator);
        key.replaceWith(core.types.identifier(name));
      }
    }

    return impureComputedPropertyDeclarators;
  }

  function removeUnusedExcludedKeys(path) {
    const bindings = path.getOuterBindingIdentifierPaths();
    Object.keys(bindings).forEach(bindingName => {
      const bindingParentPath = bindings[bindingName].parentPath;

      if (path.scope.getBinding(bindingName).references > ZERO_REFS || !bindingParentPath.isObjectProperty()) {
        return;
      }

      bindingParentPath.remove();
    });
  }

  function createObjectRest(path, file, objRef) {
    const props = path.get("properties");
    const last = props[props.length - 1];
    core.types.assertRestElement(last.node);
    const restElement = core.types.cloneNode(last.node);
    last.remove();
    const impureComputedPropertyDeclarators = replaceImpureComputedKeys(path.get("properties"), path.scope);
    const {
      keys,
      allLiteral,
      hasTemplateLiteral
    } = extractNormalizedKeys(path.node);

    if (keys.length === 0) {
      return [impureComputedPropertyDeclarators, restElement.argument, core.types.callExpression(getExtendsHelper(file), [core.types.objectExpression([]), core.types.cloneNode(objRef)])];
    }

    let keyExpression;

    if (!allLiteral) {
      keyExpression = core.types.callExpression(core.types.memberExpression(core.types.arrayExpression(keys), core.types.identifier("map")), [file.addHelper("toPropertyKey")]);
    } else {
      keyExpression = core.types.arrayExpression(keys);

      if (!hasTemplateLiteral && !core.types.isProgram(path.scope.block)) {
        const program = path.findParent(path => path.isProgram());
        const id = path.scope.generateUidIdentifier("excluded");
        program.scope.push({
          id,
          init: keyExpression,
          kind: "const"
        });
        keyExpression = core.types.cloneNode(id);
      }
    }

    return [impureComputedPropertyDeclarators, restElement.argument, core.types.callExpression(file.addHelper(`objectWithoutProperties${objectRestNoSymbols ? "Loose" : ""}`), [core.types.cloneNode(objRef), keyExpression])];
  }

  function replaceRestElement(parentPath, paramPath, container) {
    if (paramPath.isAssignmentPattern()) {
      replaceRestElement(parentPath, paramPath.get("left"), container);
      return;
    }

    if (paramPath.isArrayPattern() && hasRestElement(paramPath)) {
      const elements = paramPath.get("elements");

      for (let i = 0; i < elements.length; i++) {
        replaceRestElement(parentPath, elements[i], container);
      }
    }

    if (paramPath.isObjectPattern() && hasRestElement(paramPath)) {
      const uid = parentPath.scope.generateUidIdentifier("ref");
      const declar = core.types.variableDeclaration("let", [core.types.variableDeclarator(paramPath.node, uid)]);

      if (container) {
        container.push(declar);
      } else {
        parentPath.ensureBlock();
        parentPath.get("body").unshiftContainer("body", declar);
      }

      paramPath.replaceWith(core.types.cloneNode(uid));
    }
  }

  return {
    name: "proposal-object-rest-spread",
    inherits: syntaxObjectRestSpread__default["default"].default,
    visitor: {
      Function(path) {
        const params = path.get("params");
        const paramsWithRestElement = new Set();
        const idsInRestParams = new Set();

        for (let i = 0; i < params.length; ++i) {
          const param = params[i];

          if (hasRestElement(param)) {
            paramsWithRestElement.add(i);

            for (const name of Object.keys(param.getBindingIdentifiers())) {
              idsInRestParams.add(name);
            }
          }
        }

        let idInRest = false;

        const IdentifierHandler = function (path, functionScope) {
          const name = path.node.name;

          if (path.scope.getBinding(name) === functionScope.getBinding(name) && idsInRestParams.has(name)) {
            idInRest = true;
            path.stop();
          }
        };

        let i;

        for (i = 0; i < params.length && !idInRest; ++i) {
          const param = params[i];

          if (!paramsWithRestElement.has(i)) {
            if (param.isReferencedIdentifier() || param.isBindingIdentifier()) {
              IdentifierHandler(param, path.scope);
            } else {
              param.traverse({
                "Scope|TypeAnnotation|TSTypeAnnotation": path => path.skip(),
                "ReferencedIdentifier|BindingIdentifier": IdentifierHandler
              }, path.scope);
            }
          }
        }

        if (!idInRest) {
          for (let i = 0; i < params.length; ++i) {
            const param = params[i];

            if (paramsWithRestElement.has(i)) {
              replaceRestElement(path, param);
            }
          }
        } else {
          const shouldTransformParam = idx => idx >= i - 1 || paramsWithRestElement.has(idx);

          pluginTransformParameters.convertFunctionParams(path, ignoreFunctionLength, shouldTransformParam, replaceRestElement);
        }
      },

      VariableDeclarator(path, file) {
        if (!path.get("id").isObjectPattern()) {
          return;
        }

        let insertionPath = path;
        const originalPath = path;
        visitRestElements(path.get("id"), path => {
          if (!path.parentPath.isObjectPattern()) {
            return;
          }

          if (shouldStoreRHSInTemporaryVariable(originalPath.node.id) && !core.types.isIdentifier(originalPath.node.init)) {
            const initRef = path.scope.generateUidIdentifierBasedOnNode(originalPath.node.init, "ref");
            originalPath.insertBefore(core.types.variableDeclarator(initRef, originalPath.node.init));
            originalPath.replaceWith(core.types.variableDeclarator(originalPath.node.id, core.types.cloneNode(initRef)));
            return;
          }

          let ref = originalPath.node.init;
          const refPropertyPath = [];
          let kind;
          path.findParent(path => {
            if (path.isObjectProperty()) {
              refPropertyPath.unshift(path);
            } else if (path.isVariableDeclarator()) {
              kind = path.parentPath.node.kind;
              return true;
            }
          });
          const impureObjRefComputedDeclarators = replaceImpureComputedKeys(refPropertyPath, path.scope);
          refPropertyPath.forEach(prop => {
            const {
              node
            } = prop;
            ref = core.types.memberExpression(ref, core.types.cloneNode(node.key), node.computed || core.types.isLiteral(node.key));
          });
          const objectPatternPath = path.findParent(path => path.isObjectPattern());
          const [impureComputedPropertyDeclarators, argument, callExpression] = createObjectRest(objectPatternPath, file, ref);

          if (pureGetters) {
            removeUnusedExcludedKeys(objectPatternPath);
          }

          core.types.assertIdentifier(argument);
          insertionPath.insertBefore(impureComputedPropertyDeclarators);
          insertionPath.insertBefore(impureObjRefComputedDeclarators);
          insertionPath = insertionPath.insertAfter(core.types.variableDeclarator(argument, callExpression))[0];
          path.scope.registerBinding(kind, insertionPath);

          if (objectPatternPath.node.properties.length === 0) {
            objectPatternPath.findParent(path => path.isObjectProperty() || path.isVariableDeclarator()).remove();
          }
        });
      },

      ExportNamedDeclaration(path) {
        const declaration = path.get("declaration");
        if (!declaration.isVariableDeclaration()) return;
        const hasRest = declaration.get("declarations").some(path => hasObjectPatternRestElement(path.get("id")));
        if (!hasRest) return;
        const specifiers = [];

        for (const name of Object.keys(path.getOuterBindingIdentifiers(true))) {
          specifiers.push(core.types.exportSpecifier(core.types.identifier(name), core.types.identifier(name)));
        }

        path.replaceWith(declaration.node);
        path.insertAfter(core.types.exportNamedDeclaration(null, specifiers));
      },

      CatchClause(path) {
        const paramPath = path.get("param");
        replaceRestElement(path, paramPath);
      },

      AssignmentExpression(path, file) {
        const leftPath = path.get("left");

        if (leftPath.isObjectPattern() && hasRestElement(leftPath)) {
          const nodes = [];
          const refName = path.scope.generateUidBasedOnNode(path.node.right, "ref");
          nodes.push(core.types.variableDeclaration("var", [core.types.variableDeclarator(core.types.identifier(refName), path.node.right)]));
          const [impureComputedPropertyDeclarators, argument, callExpression] = createObjectRest(leftPath, file, core.types.identifier(refName));

          if (impureComputedPropertyDeclarators.length > 0) {
            nodes.push(core.types.variableDeclaration("var", impureComputedPropertyDeclarators));
          }

          const nodeWithoutSpread = core.types.cloneNode(path.node);
          nodeWithoutSpread.right = core.types.identifier(refName);
          nodes.push(core.types.expressionStatement(nodeWithoutSpread));
          nodes.push(core.types.toStatement(core.types.assignmentExpression("=", argument, callExpression)));
          nodes.push(core.types.expressionStatement(core.types.identifier(refName)));
          path.replaceWithMultiple(nodes);
        }
      },

      ForXStatement(path) {
        const {
          node,
          scope
        } = path;
        const leftPath = path.get("left");
        const left = node.left;

        if (!hasObjectPatternRestElement(leftPath)) {
          return;
        }

        if (!core.types.isVariableDeclaration(left)) {
          const temp = scope.generateUidIdentifier("ref");
          node.left = core.types.variableDeclaration("var", [core.types.variableDeclarator(temp)]);
          path.ensureBlock();
          const body = path.node.body;

          if (body.body.length === 0 && path.isCompletionRecord()) {
            body.body.unshift(core.types.expressionStatement(scope.buildUndefinedNode()));
          }

          body.body.unshift(core.types.expressionStatement(core.types.assignmentExpression("=", left, core.types.cloneNode(temp))));
        } else {
          const pattern = left.declarations[0].id;
          const key = scope.generateUidIdentifier("ref");
          node.left = core.types.variableDeclaration(left.kind, [core.types.variableDeclarator(key, null)]);
          path.ensureBlock();
          const body = node.body;
          body.body.unshift(core.types.variableDeclaration(node.left.kind, [core.types.variableDeclarator(pattern, core.types.cloneNode(key))]));
        }
      },

      ArrayPattern(path) {
        const objectPatterns = [];
        visitRestElements(path, path => {
          if (!path.parentPath.isObjectPattern()) {
            return;
          }

          const objectPattern = path.parentPath;
          const uid = path.scope.generateUidIdentifier("ref");
          objectPatterns.push(core.types.variableDeclarator(objectPattern.node, uid));
          objectPattern.replaceWith(core.types.cloneNode(uid));
          path.skip();
        });

        if (objectPatterns.length > 0) {
          const statementPath = path.getStatementParent();
          const statementNode = statementPath.node;
          const kind = statementNode.type === "VariableDeclaration" ? statementNode.kind : "var";
          statementPath.insertAfter(core.types.variableDeclaration(kind, objectPatterns));
        }
      },

      ObjectExpression(path, file) {
        if (!hasSpread(path.node)) return;
        let helper;

        if (setSpreadProperties) {
          helper = getExtendsHelper(file);
        } else {
          try {
            helper = file.addHelper("objectSpread2");
          } catch (_unused) {
            this.file.declarations["objectSpread2"] = null;
            helper = file.addHelper("objectSpread");
          }
        }

        let exp = null;
        let props = [];

        function make() {
          const hadProps = props.length > 0;
          const obj = core.types.objectExpression(props);
          props = [];

          if (!exp) {
            exp = core.types.callExpression(helper, [obj]);
            return;
          }

          if (pureGetters) {
            if (hadProps) {
              exp.arguments.push(obj);
            }

            return;
          }

          exp = core.types.callExpression(core.types.cloneNode(helper), [exp, ...(hadProps ? [core.types.objectExpression([]), obj] : [])]);
        }

        for (const prop of path.node.properties) {
          if (core.types.isSpreadElement(prop)) {
            make();
            exp.arguments.push(prop.argument);
          } else {
            props.push(prop);
          }
        }

        if (props.length) make();
        path.replaceWith(exp);
      }

    }
  };
});

exports["default"] = index;
//# sourceMappingURL=index.js.map
