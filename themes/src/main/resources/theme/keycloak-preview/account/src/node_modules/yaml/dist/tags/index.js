"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.tags = exports.schemas = void 0;

var _core = _interopRequireWildcard(require("./core"));

var _failsafe = _interopRequireDefault(require("./failsafe"));

var _json = _interopRequireDefault(require("./json"));

var _yaml = _interopRequireDefault(require("./yaml-1.1"));

var _map = _interopRequireDefault(require("./failsafe/map"));

var _seq = _interopRequireDefault(require("./failsafe/seq"));

var _binary = _interopRequireDefault(require("./yaml-1.1/binary"));

var _omap = _interopRequireDefault(require("./yaml-1.1/omap"));

var _pairs = _interopRequireDefault(require("./yaml-1.1/pairs"));

var _set = _interopRequireDefault(require("./yaml-1.1/set"));

var _timestamp = require("./yaml-1.1/timestamp");

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function () { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } if (obj === null || typeof obj !== "object" && typeof obj !== "function") { return { default: obj }; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } newObj.default = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

const schemas = {
  core: _core.default,
  failsafe: _failsafe.default,
  json: _json.default,
  yaml11: _yaml.default
};
exports.schemas = schemas;
const tags = {
  binary: _binary.default,
  bool: _core.boolObj,
  float: _core.floatObj,
  floatExp: _core.expObj,
  floatNaN: _core.nanObj,
  floatTime: _timestamp.floatTime,
  int: _core.intObj,
  intHex: _core.hexObj,
  intOct: _core.octObj,
  intTime: _timestamp.intTime,
  map: _map.default,
  null: _core.nullObj,
  omap: _omap.default,
  pairs: _pairs.default,
  seq: _seq.default,
  set: _set.default,
  timestamp: _timestamp.timestamp
};
exports.tags = tags;