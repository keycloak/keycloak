'use strict';

Object.defineProperty(exports, '__esModule', { value: true });

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var createEmotion = _interopDefault(require('create-emotion'));

var context = typeof global !== 'undefined' ? global : {};

var _createEmotion = createEmotion(context),
    flush = _createEmotion.flush,
    hydrate = _createEmotion.hydrate,
    cx = _createEmotion.cx,
    merge = _createEmotion.merge,
    getRegisteredStyles = _createEmotion.getRegisteredStyles,
    injectGlobal = _createEmotion.injectGlobal,
    keyframes = _createEmotion.keyframes,
    css = _createEmotion.css,
    sheet = _createEmotion.sheet,
    caches = _createEmotion.caches;

exports.flush = flush;
exports.hydrate = hydrate;
exports.cx = cx;
exports.merge = merge;
exports.getRegisteredStyles = getRegisteredStyles;
exports.injectGlobal = injectGlobal;
exports.keyframes = keyframes;
exports.css = css;
exports.sheet = sheet;
exports.caches = caches;
