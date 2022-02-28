"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.default = void 0;

var _constants = require("../../constants");

var _errors = require("../../errors");

var _stringify = require("../../stringify");

var _string = require("../failsafe/string");

var _options = require("../options");

/* global atob, btoa, Buffer */
var _default = {
  identify: value => value instanceof Uint8Array,
  // Buffer inherits from Uint8Array
  default: false,
  tag: 'tag:yaml.org,2002:binary',

  /**
   * Returns a Buffer in node and an Uint8Array in browsers
   *
   * To use the resulting buffer as an image, you'll want to do something like:
   *
   *   const blob = new Blob([buffer], { type: 'image/jpeg' })
   *   document.querySelector('#photo').src = URL.createObjectURL(blob)
   */
  resolve: (doc, node) => {
    if (typeof Buffer === 'function') {
      const src = (0, _string.resolveString)(doc, node);
      return Buffer.from(src, 'base64');
    } else if (typeof atob === 'function') {
      const src = atob((0, _string.resolveString)(doc, node));
      const buffer = new Uint8Array(src.length);

      for (let i = 0; i < src.length; ++i) buffer[i] = src.charCodeAt(i);

      return buffer;
    } else {
      doc.errors.push(new _errors.YAMLReferenceError(node, 'This environment does not support reading binary tags; either Buffer or atob is required'));
      return null;
    }
  },
  options: _options.binaryOptions,
  stringify: ({
    comment,
    type,
    value
  }, ctx, onComment, onChompKeep) => {
    let src;

    if (typeof Buffer === 'function') {
      src = value instanceof Buffer ? value.toString('base64') : Buffer.from(value.buffer).toString('base64');
    } else if (typeof btoa === 'function') {
      let s = '';

      for (let i = 0; i < value.length; ++i) s += String.fromCharCode(value[i]);

      src = btoa(s);
    } else {
      throw new Error('This environment does not support writing binary tags; either Buffer or btoa is required');
    }

    if (!type) type = _options.binaryOptions.defaultType;

    if (type === _constants.Type.QUOTE_DOUBLE) {
      value = src;
    } else {
      const {
        lineWidth
      } = _options.binaryOptions;
      const n = Math.ceil(src.length / lineWidth);
      const lines = new Array(n);

      for (let i = 0, o = 0; i < n; ++i, o += lineWidth) {
        lines[i] = src.substr(o, lineWidth);
      }

      value = lines.join(type === _constants.Type.BLOCK_LITERAL ? '\n' : ' ');
    }

    return (0, _stringify.stringifyString)({
      comment,
      type,
      value
    }, ctx, onComment, onChompKeep);
  }
};
exports.default = _default;