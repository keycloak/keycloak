(function (global, factory) {
	typeof exports === 'object' && typeof module !== 'undefined' ? module.exports = factory() :
	typeof define === 'function' && define.amd ? define('keycloak', factory) :
	(global = typeof globalThis !== 'undefined' ? globalThis : global || self, global.Keycloak = factory());
})(this, (function () { 'use strict';

	var commonjsGlobal = typeof globalThis !== 'undefined' ? globalThis : typeof window !== 'undefined' ? window : typeof global !== 'undefined' ? global : typeof self !== 'undefined' ? self : {};

	function commonjsRequire (path) {
		throw new Error('Could not dynamically require "' + path + '". Please configure the dynamicRequireTargets or/and ignoreDynamicRequires option of @rollup/plugin-commonjs appropriately for this require call to work.');
	}

	var es6Promise_min = {exports: {}};

	(function (module, exports) {
	!function(t,e){module.exports=e();}(commonjsGlobal,function(){function t(t){var e=typeof t;return null!==t&&("object"===e||"function"===e)}function e(t){return "function"==typeof t}function n(t){W=t;}function r(t){z=t;}function o(){return function(){return process.nextTick(a)}}function i(){return "undefined"!=typeof U?function(){U(a);}:c()}function s(){var t=0,e=new H(a),n=document.createTextNode("");return e.observe(n,{characterData:!0}),function(){n.data=t=++t%2;}}function u(){var t=new MessageChannel;return t.port1.onmessage=a,function(){return t.port2.postMessage(0)}}function c(){var t=setTimeout;return function(){return t(a,1)}}function a(){for(var t=0;t<N;t+=2){var e=Q[t],n=Q[t+1];e(n),Q[t]=void 0,Q[t+1]=void 0;}N=0;}function f(){try{var t=Function("return this")().require("vertx");return U=t.runOnLoop||t.runOnContext,i()}catch(e){return c()}}function l(t,e){var n=this,r=new this.constructor(v);void 0===r[V]&&x(r);var o=n._state;if(o){var i=arguments[o-1];z(function(){return T(o,r,i,n._result)});}else j(n,r,t,e);return r}function h(t){var e=this;if(t&&"object"==typeof t&&t.constructor===e)return t;var n=new e(v);return w(n,t),n}function v(){}function p(){return new TypeError("You cannot resolve a promise with itself")}function d(){return new TypeError("A promises callback cannot return that same promise.")}function _(t,e,n,r){try{t.call(e,n,r);}catch(o){return o}}function y(t,e,n){z(function(t){var r=!1,o=_(n,e,function(n){r||(r=!0,e!==n?w(t,n):A(t,n));},function(e){r||(r=!0,S(t,e));},"Settle: "+(t._label||" unknown promise"));!r&&o&&(r=!0,S(t,o));},t);}function m(t,e){e._state===Z?A(t,e._result):e._state===$?S(t,e._result):j(e,void 0,function(e){return w(t,e)},function(e){return S(t,e)});}function b(t,n,r){n.constructor===t.constructor&&r===l&&n.constructor.resolve===h?m(t,n):void 0===r?A(t,n):e(r)?y(t,n,r):A(t,n);}function w(e,n){if(e===n)S(e,p());else if(t(n)){var r=void 0;try{r=n.then;}catch(o){return void S(e,o)}b(e,n,r);}else A(e,n);}function g(t){t._onerror&&t._onerror(t._result),E(t);}function A(t,e){t._state===X&&(t._result=e,t._state=Z,0!==t._subscribers.length&&z(E,t));}function S(t,e){t._state===X&&(t._state=$,t._result=e,z(g,t));}function j(t,e,n,r){var o=t._subscribers,i=o.length;t._onerror=null,o[i]=e,o[i+Z]=n,o[i+$]=r,0===i&&t._state&&z(E,t);}function E(t){var e=t._subscribers,n=t._state;if(0!==e.length){for(var r=void 0,o=void 0,i=t._result,s=0;s<e.length;s+=3)r=e[s],o=e[s+n],r?T(n,r,o,i):o(i);t._subscribers.length=0;}}function T(t,n,r,o){var i=e(r),s=void 0,u=void 0,c=!0;if(i){try{s=r(o);}catch(a){c=!1,u=a;}if(n===s)return void S(n,d())}else s=o;n._state!==X||(i&&c?w(n,s):c===!1?S(n,u):t===Z?A(n,s):t===$&&S(n,s));}function M(t,e){try{e(function(e){w(t,e);},function(e){S(t,e);});}catch(n){S(t,n);}}function P(){return tt++}function x(t){t[V]=tt++,t._state=void 0,t._result=void 0,t._subscribers=[];}function C(){return new Error("Array Methods must be provided an Array")}function O(t){return new et(this,t).promise}function k(t){var e=this;return new e(L(t)?function(n,r){for(var o=t.length,i=0;i<o;i++)e.resolve(t[i]).then(n,r);}:function(t,e){return e(new TypeError("You must pass an array to race."))})}function F(t){var e=this,n=new e(v);return S(n,t),n}function Y(){throw new TypeError("You must pass a resolver function as the first argument to the promise constructor")}function q(){throw new TypeError("Failed to construct 'Promise': Please use the 'new' operator, this object constructor cannot be called as a function.")}function D(){var t=void 0;if("undefined"!=typeof commonjsGlobal)t=commonjsGlobal;else if("undefined"!=typeof self)t=self;else try{t=Function("return this")();}catch(e){throw new Error("polyfill failed because global object is unavailable in this environment")}var n=t.Promise;if(n){var r=null;try{r=Object.prototype.toString.call(n.resolve());}catch(e){}if("[object Promise]"===r&&!n.cast)return}t.Promise=nt;}var K=void 0;K=Array.isArray?Array.isArray:function(t){return "[object Array]"===Object.prototype.toString.call(t)};var L=K,N=0,U=void 0,W=void 0,z=function(t,e){Q[N]=t,Q[N+1]=e,N+=2,2===N&&(W?W(a):R());},B="undefined"!=typeof window?window:void 0,G=B||{},H=G.MutationObserver||G.WebKitMutationObserver,I="undefined"==typeof self&&"undefined"!=typeof process&&"[object process]"==={}.toString.call(process),J="undefined"!=typeof Uint8ClampedArray&&"undefined"!=typeof importScripts&&"undefined"!=typeof MessageChannel,Q=new Array(1e3),R=void 0;R=I?o():H?s():J?u():void 0===B&&"function"==typeof commonjsRequire?f():c();var V=Math.random().toString(36).substring(2),X=void 0,Z=1,$=2,tt=0,et=function(){function t(t,e){this._instanceConstructor=t,this.promise=new t(v),this.promise[V]||x(this.promise),L(e)?(this.length=e.length,this._remaining=e.length,this._result=new Array(this.length),0===this.length?A(this.promise,this._result):(this.length=this.length||0,this._enumerate(e),0===this._remaining&&A(this.promise,this._result))):S(this.promise,C());}return t.prototype._enumerate=function(t){for(var e=0;this._state===X&&e<t.length;e++)this._eachEntry(t[e],e);},t.prototype._eachEntry=function(t,e){var n=this._instanceConstructor,r=n.resolve;if(r===h){var o=void 0,i=void 0,s=!1;try{o=t.then;}catch(u){s=!0,i=u;}if(o===l&&t._state!==X)this._settledAt(t._state,e,t._result);else if("function"!=typeof o)this._remaining--,this._result[e]=t;else if(n===nt){var c=new n(v);s?S(c,i):b(c,t,o),this._willSettleAt(c,e);}else this._willSettleAt(new n(function(e){return e(t)}),e);}else this._willSettleAt(r(t),e);},t.prototype._settledAt=function(t,e,n){var r=this.promise;r._state===X&&(this._remaining--,t===$?S(r,n):this._result[e]=n),0===this._remaining&&A(r,this._result);},t.prototype._willSettleAt=function(t,e){var n=this;j(t,void 0,function(t){return n._settledAt(Z,e,t)},function(t){return n._settledAt($,e,t)});},t}(),nt=function(){function t(e){this[V]=P(),this._result=this._state=void 0,this._subscribers=[],v!==e&&("function"!=typeof e&&Y(),this instanceof t?M(this,e):q());}return t.prototype["catch"]=function(t){return this.then(null,t)},t.prototype["finally"]=function(t){var n=this,r=n.constructor;return e(t)?n.then(function(e){return r.resolve(t()).then(function(){return e})},function(e){return r.resolve(t()).then(function(){throw e})}):n.then(t,t)},t}();return nt.prototype.then=l,nt.all=O,nt.race=k,nt.resolve=h,nt.reject=F,nt._setScheduler=n,nt._setAsap=r,nt._asap=z,nt.polyfill=D,nt.Promise=nt,nt});
	}(es6Promise_min));

	var base64Js = {};

	base64Js.byteLength = byteLength;
	base64Js.toByteArray = toByteArray;
	base64Js.fromByteArray = fromByteArray;

	var lookup = [];
	var revLookup = [];
	var Arr = typeof Uint8Array !== 'undefined' ? Uint8Array : Array;

	var code = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
	for (var i = 0, len = code.length; i < len; ++i) {
	  lookup[i] = code[i];
	  revLookup[code.charCodeAt(i)] = i;
	}

	// Support decoding URL-safe base64 strings, as Node.js does.
	// See: https://en.wikipedia.org/wiki/Base64#URL_applications
	revLookup['-'.charCodeAt(0)] = 62;
	revLookup['_'.charCodeAt(0)] = 63;

	function getLens (b64) {
	  var len = b64.length;

	  if (len % 4 > 0) {
	    throw new Error('Invalid string. Length must be a multiple of 4')
	  }

	  // Trim off extra bytes after placeholder bytes are found
	  // See: https://github.com/beatgammit/base64-js/issues/42
	  var validLen = b64.indexOf('=');
	  if (validLen === -1) validLen = len;

	  var placeHoldersLen = validLen === len
	    ? 0
	    : 4 - (validLen % 4);

	  return [validLen, placeHoldersLen]
	}

	// base64 is 4/3 + up to two characters of the original data
	function byteLength (b64) {
	  var lens = getLens(b64);
	  var validLen = lens[0];
	  var placeHoldersLen = lens[1];
	  return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
	}

	function _byteLength (b64, validLen, placeHoldersLen) {
	  return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
	}

	function toByteArray (b64) {
	  var tmp;
	  var lens = getLens(b64);
	  var validLen = lens[0];
	  var placeHoldersLen = lens[1];

	  var arr = new Arr(_byteLength(b64, validLen, placeHoldersLen));

	  var curByte = 0;

	  // if there are placeholders, only get up to the last complete 4 chars
	  var len = placeHoldersLen > 0
	    ? validLen - 4
	    : validLen;

	  var i;
	  for (i = 0; i < len; i += 4) {
	    tmp =
	      (revLookup[b64.charCodeAt(i)] << 18) |
	      (revLookup[b64.charCodeAt(i + 1)] << 12) |
	      (revLookup[b64.charCodeAt(i + 2)] << 6) |
	      revLookup[b64.charCodeAt(i + 3)];
	    arr[curByte++] = (tmp >> 16) & 0xFF;
	    arr[curByte++] = (tmp >> 8) & 0xFF;
	    arr[curByte++] = tmp & 0xFF;
	  }

	  if (placeHoldersLen === 2) {
	    tmp =
	      (revLookup[b64.charCodeAt(i)] << 2) |
	      (revLookup[b64.charCodeAt(i + 1)] >> 4);
	    arr[curByte++] = tmp & 0xFF;
	  }

	  if (placeHoldersLen === 1) {
	    tmp =
	      (revLookup[b64.charCodeAt(i)] << 10) |
	      (revLookup[b64.charCodeAt(i + 1)] << 4) |
	      (revLookup[b64.charCodeAt(i + 2)] >> 2);
	    arr[curByte++] = (tmp >> 8) & 0xFF;
	    arr[curByte++] = tmp & 0xFF;
	  }

	  return arr
	}

	function tripletToBase64 (num) {
	  return lookup[num >> 18 & 0x3F] +
	    lookup[num >> 12 & 0x3F] +
	    lookup[num >> 6 & 0x3F] +
	    lookup[num & 0x3F]
	}

	function encodeChunk (uint8, start, end) {
	  var tmp;
	  var output = [];
	  for (var i = start; i < end; i += 3) {
	    tmp =
	      ((uint8[i] << 16) & 0xFF0000) +
	      ((uint8[i + 1] << 8) & 0xFF00) +
	      (uint8[i + 2] & 0xFF);
	    output.push(tripletToBase64(tmp));
	  }
	  return output.join('')
	}

	function fromByteArray (uint8) {
	  var tmp;
	  var len = uint8.length;
	  var extraBytes = len % 3; // if we have 1 byte left, pad 2 bytes
	  var parts = [];
	  var maxChunkLength = 16383; // must be multiple of 3

	  // go through the array every three bytes, we'll deal with trailing stuff later
	  for (var i = 0, len2 = len - extraBytes; i < len2; i += maxChunkLength) {
	    parts.push(encodeChunk(uint8, i, (i + maxChunkLength) > len2 ? len2 : (i + maxChunkLength)));
	  }

	  // pad the end with zeros, but make sure to not forget the extra bytes
	  if (extraBytes === 1) {
	    tmp = uint8[len - 1];
	    parts.push(
	      lookup[tmp >> 2] +
	      lookup[(tmp << 4) & 0x3F] +
	      '=='
	    );
	  } else if (extraBytes === 2) {
	    tmp = (uint8[len - 2] << 8) + uint8[len - 1];
	    parts.push(
	      lookup[tmp >> 10] +
	      lookup[(tmp >> 4) & 0x3F] +
	      lookup[(tmp << 2) & 0x3F] +
	      '='
	    );
	  }

	  return parts.join('')
	}

	var sha256$1 = {exports: {}};

	/**
	 * [js-sha256]{@link https://github.com/emn178/js-sha256}
	 *
	 * @version 0.9.0
	 * @author Chen, Yi-Cyuan [emn178@gmail.com]
	 * @copyright Chen, Yi-Cyuan 2014-2017
	 * @license MIT
	 */

	(function (module) {
	/*jslint bitwise: true */
	(function () {

	  var ERROR = 'input is invalid type';
	  var WINDOW = typeof window === 'object';
	  var root = WINDOW ? window : {};
	  if (root.JS_SHA256_NO_WINDOW) {
	    WINDOW = false;
	  }
	  var WEB_WORKER = !WINDOW && typeof self === 'object';
	  var NODE_JS = !root.JS_SHA256_NO_NODE_JS && typeof process === 'object' && process.versions && process.versions.node;
	  if (NODE_JS) {
	    root = commonjsGlobal;
	  } else if (WEB_WORKER) {
	    root = self;
	  }
	  var COMMON_JS = !root.JS_SHA256_NO_COMMON_JS && 'object' === 'object' && module.exports;
	  var ARRAY_BUFFER = !root.JS_SHA256_NO_ARRAY_BUFFER && typeof ArrayBuffer !== 'undefined';
	  var HEX_CHARS = '0123456789abcdef'.split('');
	  var EXTRA = [-2147483648, 8388608, 32768, 128];
	  var SHIFT = [24, 16, 8, 0];
	  var K = [
	    0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
	    0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
	    0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
	    0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
	    0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
	    0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
	    0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
	    0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2
	  ];
	  var OUTPUT_TYPES = ['hex', 'array', 'digest', 'arrayBuffer'];

	  var blocks = [];

	  if (root.JS_SHA256_NO_NODE_JS || !Array.isArray) {
	    Array.isArray = function (obj) {
	      return Object.prototype.toString.call(obj) === '[object Array]';
	    };
	  }

	  if (ARRAY_BUFFER && (root.JS_SHA256_NO_ARRAY_BUFFER_IS_VIEW || !ArrayBuffer.isView)) {
	    ArrayBuffer.isView = function (obj) {
	      return typeof obj === 'object' && obj.buffer && obj.buffer.constructor === ArrayBuffer;
	    };
	  }

	  var createOutputMethod = function (outputType, is224) {
	    return function (message) {
	      return new Sha256(is224, true).update(message)[outputType]();
	    };
	  };

	  var createMethod = function (is224) {
	    var method = createOutputMethod('hex', is224);
	    if (NODE_JS) {
	      method = nodeWrap(method, is224);
	    }
	    method.create = function () {
	      return new Sha256(is224);
	    };
	    method.update = function (message) {
	      return method.create().update(message);
	    };
	    for (var i = 0; i < OUTPUT_TYPES.length; ++i) {
	      var type = OUTPUT_TYPES[i];
	      method[type] = createOutputMethod(type, is224);
	    }
	    return method;
	  };

	  var nodeWrap = function (method, is224) {
	    var crypto = eval("require('crypto')");
	    var Buffer = eval("require('buffer').Buffer");
	    var algorithm = is224 ? 'sha224' : 'sha256';
	    var nodeMethod = function (message) {
	      if (typeof message === 'string') {
	        return crypto.createHash(algorithm).update(message, 'utf8').digest('hex');
	      } else {
	        if (message === null || message === undefined) {
	          throw new Error(ERROR);
	        } else if (message.constructor === ArrayBuffer) {
	          message = new Uint8Array(message);
	        }
	      }
	      if (Array.isArray(message) || ArrayBuffer.isView(message) ||
	        message.constructor === Buffer) {
	        return crypto.createHash(algorithm).update(new Buffer(message)).digest('hex');
	      } else {
	        return method(message);
	      }
	    };
	    return nodeMethod;
	  };

	  var createHmacOutputMethod = function (outputType, is224) {
	    return function (key, message) {
	      return new HmacSha256(key, is224, true).update(message)[outputType]();
	    };
	  };

	  var createHmacMethod = function (is224) {
	    var method = createHmacOutputMethod('hex', is224);
	    method.create = function (key) {
	      return new HmacSha256(key, is224);
	    };
	    method.update = function (key, message) {
	      return method.create(key).update(message);
	    };
	    for (var i = 0; i < OUTPUT_TYPES.length; ++i) {
	      var type = OUTPUT_TYPES[i];
	      method[type] = createHmacOutputMethod(type, is224);
	    }
	    return method;
	  };

	  function Sha256(is224, sharedMemory) {
	    if (sharedMemory) {
	      blocks[0] = blocks[16] = blocks[1] = blocks[2] = blocks[3] =
	        blocks[4] = blocks[5] = blocks[6] = blocks[7] =
	        blocks[8] = blocks[9] = blocks[10] = blocks[11] =
	        blocks[12] = blocks[13] = blocks[14] = blocks[15] = 0;
	      this.blocks = blocks;
	    } else {
	      this.blocks = [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0];
	    }

	    if (is224) {
	      this.h0 = 0xc1059ed8;
	      this.h1 = 0x367cd507;
	      this.h2 = 0x3070dd17;
	      this.h3 = 0xf70e5939;
	      this.h4 = 0xffc00b31;
	      this.h5 = 0x68581511;
	      this.h6 = 0x64f98fa7;
	      this.h7 = 0xbefa4fa4;
	    } else { // 256
	      this.h0 = 0x6a09e667;
	      this.h1 = 0xbb67ae85;
	      this.h2 = 0x3c6ef372;
	      this.h3 = 0xa54ff53a;
	      this.h4 = 0x510e527f;
	      this.h5 = 0x9b05688c;
	      this.h6 = 0x1f83d9ab;
	      this.h7 = 0x5be0cd19;
	    }

	    this.block = this.start = this.bytes = this.hBytes = 0;
	    this.finalized = this.hashed = false;
	    this.first = true;
	    this.is224 = is224;
	  }

	  Sha256.prototype.update = function (message) {
	    if (this.finalized) {
	      return;
	    }
	    var notString, type = typeof message;
	    if (type !== 'string') {
	      if (type === 'object') {
	        if (message === null) {
	          throw new Error(ERROR);
	        } else if (ARRAY_BUFFER && message.constructor === ArrayBuffer) {
	          message = new Uint8Array(message);
	        } else if (!Array.isArray(message)) {
	          if (!ARRAY_BUFFER || !ArrayBuffer.isView(message)) {
	            throw new Error(ERROR);
	          }
	        }
	      } else {
	        throw new Error(ERROR);
	      }
	      notString = true;
	    }
	    var code, index = 0, i, length = message.length, blocks = this.blocks;

	    while (index < length) {
	      if (this.hashed) {
	        this.hashed = false;
	        blocks[0] = this.block;
	        blocks[16] = blocks[1] = blocks[2] = blocks[3] =
	          blocks[4] = blocks[5] = blocks[6] = blocks[7] =
	          blocks[8] = blocks[9] = blocks[10] = blocks[11] =
	          blocks[12] = blocks[13] = blocks[14] = blocks[15] = 0;
	      }

	      if (notString) {
	        for (i = this.start; index < length && i < 64; ++index) {
	          blocks[i >> 2] |= message[index] << SHIFT[i++ & 3];
	        }
	      } else {
	        for (i = this.start; index < length && i < 64; ++index) {
	          code = message.charCodeAt(index);
	          if (code < 0x80) {
	            blocks[i >> 2] |= code << SHIFT[i++ & 3];
	          } else if (code < 0x800) {
	            blocks[i >> 2] |= (0xc0 | (code >> 6)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | (code & 0x3f)) << SHIFT[i++ & 3];
	          } else if (code < 0xd800 || code >= 0xe000) {
	            blocks[i >> 2] |= (0xe0 | (code >> 12)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | ((code >> 6) & 0x3f)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | (code & 0x3f)) << SHIFT[i++ & 3];
	          } else {
	            code = 0x10000 + (((code & 0x3ff) << 10) | (message.charCodeAt(++index) & 0x3ff));
	            blocks[i >> 2] |= (0xf0 | (code >> 18)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | ((code >> 12) & 0x3f)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | ((code >> 6) & 0x3f)) << SHIFT[i++ & 3];
	            blocks[i >> 2] |= (0x80 | (code & 0x3f)) << SHIFT[i++ & 3];
	          }
	        }
	      }

	      this.lastByteIndex = i;
	      this.bytes += i - this.start;
	      if (i >= 64) {
	        this.block = blocks[16];
	        this.start = i - 64;
	        this.hash();
	        this.hashed = true;
	      } else {
	        this.start = i;
	      }
	    }
	    if (this.bytes > 4294967295) {
	      this.hBytes += this.bytes / 4294967296 << 0;
	      this.bytes = this.bytes % 4294967296;
	    }
	    return this;
	  };

	  Sha256.prototype.finalize = function () {
	    if (this.finalized) {
	      return;
	    }
	    this.finalized = true;
	    var blocks = this.blocks, i = this.lastByteIndex;
	    blocks[16] = this.block;
	    blocks[i >> 2] |= EXTRA[i & 3];
	    this.block = blocks[16];
	    if (i >= 56) {
	      if (!this.hashed) {
	        this.hash();
	      }
	      blocks[0] = this.block;
	      blocks[16] = blocks[1] = blocks[2] = blocks[3] =
	        blocks[4] = blocks[5] = blocks[6] = blocks[7] =
	        blocks[8] = blocks[9] = blocks[10] = blocks[11] =
	        blocks[12] = blocks[13] = blocks[14] = blocks[15] = 0;
	    }
	    blocks[14] = this.hBytes << 3 | this.bytes >>> 29;
	    blocks[15] = this.bytes << 3;
	    this.hash();
	  };

	  Sha256.prototype.hash = function () {
	    var a = this.h0, b = this.h1, c = this.h2, d = this.h3, e = this.h4, f = this.h5, g = this.h6,
	      h = this.h7, blocks = this.blocks, j, s0, s1, maj, t1, t2, ch, ab, da, cd, bc;

	    for (j = 16; j < 64; ++j) {
	      // rightrotate
	      t1 = blocks[j - 15];
	      s0 = ((t1 >>> 7) | (t1 << 25)) ^ ((t1 >>> 18) | (t1 << 14)) ^ (t1 >>> 3);
	      t1 = blocks[j - 2];
	      s1 = ((t1 >>> 17) | (t1 << 15)) ^ ((t1 >>> 19) | (t1 << 13)) ^ (t1 >>> 10);
	      blocks[j] = blocks[j - 16] + s0 + blocks[j - 7] + s1 << 0;
	    }

	    bc = b & c;
	    for (j = 0; j < 64; j += 4) {
	      if (this.first) {
	        if (this.is224) {
	          ab = 300032;
	          t1 = blocks[0] - 1413257819;
	          h = t1 - 150054599 << 0;
	          d = t1 + 24177077 << 0;
	        } else {
	          ab = 704751109;
	          t1 = blocks[0] - 210244248;
	          h = t1 - 1521486534 << 0;
	          d = t1 + 143694565 << 0;
	        }
	        this.first = false;
	      } else {
	        s0 = ((a >>> 2) | (a << 30)) ^ ((a >>> 13) | (a << 19)) ^ ((a >>> 22) | (a << 10));
	        s1 = ((e >>> 6) | (e << 26)) ^ ((e >>> 11) | (e << 21)) ^ ((e >>> 25) | (e << 7));
	        ab = a & b;
	        maj = ab ^ (a & c) ^ bc;
	        ch = (e & f) ^ (~e & g);
	        t1 = h + s1 + ch + K[j] + blocks[j];
	        t2 = s0 + maj;
	        h = d + t1 << 0;
	        d = t1 + t2 << 0;
	      }
	      s0 = ((d >>> 2) | (d << 30)) ^ ((d >>> 13) | (d << 19)) ^ ((d >>> 22) | (d << 10));
	      s1 = ((h >>> 6) | (h << 26)) ^ ((h >>> 11) | (h << 21)) ^ ((h >>> 25) | (h << 7));
	      da = d & a;
	      maj = da ^ (d & b) ^ ab;
	      ch = (h & e) ^ (~h & f);
	      t1 = g + s1 + ch + K[j + 1] + blocks[j + 1];
	      t2 = s0 + maj;
	      g = c + t1 << 0;
	      c = t1 + t2 << 0;
	      s0 = ((c >>> 2) | (c << 30)) ^ ((c >>> 13) | (c << 19)) ^ ((c >>> 22) | (c << 10));
	      s1 = ((g >>> 6) | (g << 26)) ^ ((g >>> 11) | (g << 21)) ^ ((g >>> 25) | (g << 7));
	      cd = c & d;
	      maj = cd ^ (c & a) ^ da;
	      ch = (g & h) ^ (~g & e);
	      t1 = f + s1 + ch + K[j + 2] + blocks[j + 2];
	      t2 = s0 + maj;
	      f = b + t1 << 0;
	      b = t1 + t2 << 0;
	      s0 = ((b >>> 2) | (b << 30)) ^ ((b >>> 13) | (b << 19)) ^ ((b >>> 22) | (b << 10));
	      s1 = ((f >>> 6) | (f << 26)) ^ ((f >>> 11) | (f << 21)) ^ ((f >>> 25) | (f << 7));
	      bc = b & c;
	      maj = bc ^ (b & d) ^ cd;
	      ch = (f & g) ^ (~f & h);
	      t1 = e + s1 + ch + K[j + 3] + blocks[j + 3];
	      t2 = s0 + maj;
	      e = a + t1 << 0;
	      a = t1 + t2 << 0;
	    }

	    this.h0 = this.h0 + a << 0;
	    this.h1 = this.h1 + b << 0;
	    this.h2 = this.h2 + c << 0;
	    this.h3 = this.h3 + d << 0;
	    this.h4 = this.h4 + e << 0;
	    this.h5 = this.h5 + f << 0;
	    this.h6 = this.h6 + g << 0;
	    this.h7 = this.h7 + h << 0;
	  };

	  Sha256.prototype.hex = function () {
	    this.finalize();

	    var h0 = this.h0, h1 = this.h1, h2 = this.h2, h3 = this.h3, h4 = this.h4, h5 = this.h5,
	      h6 = this.h6, h7 = this.h7;

	    var hex = HEX_CHARS[(h0 >> 28) & 0x0F] + HEX_CHARS[(h0 >> 24) & 0x0F] +
	      HEX_CHARS[(h0 >> 20) & 0x0F] + HEX_CHARS[(h0 >> 16) & 0x0F] +
	      HEX_CHARS[(h0 >> 12) & 0x0F] + HEX_CHARS[(h0 >> 8) & 0x0F] +
	      HEX_CHARS[(h0 >> 4) & 0x0F] + HEX_CHARS[h0 & 0x0F] +
	      HEX_CHARS[(h1 >> 28) & 0x0F] + HEX_CHARS[(h1 >> 24) & 0x0F] +
	      HEX_CHARS[(h1 >> 20) & 0x0F] + HEX_CHARS[(h1 >> 16) & 0x0F] +
	      HEX_CHARS[(h1 >> 12) & 0x0F] + HEX_CHARS[(h1 >> 8) & 0x0F] +
	      HEX_CHARS[(h1 >> 4) & 0x0F] + HEX_CHARS[h1 & 0x0F] +
	      HEX_CHARS[(h2 >> 28) & 0x0F] + HEX_CHARS[(h2 >> 24) & 0x0F] +
	      HEX_CHARS[(h2 >> 20) & 0x0F] + HEX_CHARS[(h2 >> 16) & 0x0F] +
	      HEX_CHARS[(h2 >> 12) & 0x0F] + HEX_CHARS[(h2 >> 8) & 0x0F] +
	      HEX_CHARS[(h2 >> 4) & 0x0F] + HEX_CHARS[h2 & 0x0F] +
	      HEX_CHARS[(h3 >> 28) & 0x0F] + HEX_CHARS[(h3 >> 24) & 0x0F] +
	      HEX_CHARS[(h3 >> 20) & 0x0F] + HEX_CHARS[(h3 >> 16) & 0x0F] +
	      HEX_CHARS[(h3 >> 12) & 0x0F] + HEX_CHARS[(h3 >> 8) & 0x0F] +
	      HEX_CHARS[(h3 >> 4) & 0x0F] + HEX_CHARS[h3 & 0x0F] +
	      HEX_CHARS[(h4 >> 28) & 0x0F] + HEX_CHARS[(h4 >> 24) & 0x0F] +
	      HEX_CHARS[(h4 >> 20) & 0x0F] + HEX_CHARS[(h4 >> 16) & 0x0F] +
	      HEX_CHARS[(h4 >> 12) & 0x0F] + HEX_CHARS[(h4 >> 8) & 0x0F] +
	      HEX_CHARS[(h4 >> 4) & 0x0F] + HEX_CHARS[h4 & 0x0F] +
	      HEX_CHARS[(h5 >> 28) & 0x0F] + HEX_CHARS[(h5 >> 24) & 0x0F] +
	      HEX_CHARS[(h5 >> 20) & 0x0F] + HEX_CHARS[(h5 >> 16) & 0x0F] +
	      HEX_CHARS[(h5 >> 12) & 0x0F] + HEX_CHARS[(h5 >> 8) & 0x0F] +
	      HEX_CHARS[(h5 >> 4) & 0x0F] + HEX_CHARS[h5 & 0x0F] +
	      HEX_CHARS[(h6 >> 28) & 0x0F] + HEX_CHARS[(h6 >> 24) & 0x0F] +
	      HEX_CHARS[(h6 >> 20) & 0x0F] + HEX_CHARS[(h6 >> 16) & 0x0F] +
	      HEX_CHARS[(h6 >> 12) & 0x0F] + HEX_CHARS[(h6 >> 8) & 0x0F] +
	      HEX_CHARS[(h6 >> 4) & 0x0F] + HEX_CHARS[h6 & 0x0F];
	    if (!this.is224) {
	      hex += HEX_CHARS[(h7 >> 28) & 0x0F] + HEX_CHARS[(h7 >> 24) & 0x0F] +
	        HEX_CHARS[(h7 >> 20) & 0x0F] + HEX_CHARS[(h7 >> 16) & 0x0F] +
	        HEX_CHARS[(h7 >> 12) & 0x0F] + HEX_CHARS[(h7 >> 8) & 0x0F] +
	        HEX_CHARS[(h7 >> 4) & 0x0F] + HEX_CHARS[h7 & 0x0F];
	    }
	    return hex;
	  };

	  Sha256.prototype.toString = Sha256.prototype.hex;

	  Sha256.prototype.digest = function () {
	    this.finalize();

	    var h0 = this.h0, h1 = this.h1, h2 = this.h2, h3 = this.h3, h4 = this.h4, h5 = this.h5,
	      h6 = this.h6, h7 = this.h7;

	    var arr = [
	      (h0 >> 24) & 0xFF, (h0 >> 16) & 0xFF, (h0 >> 8) & 0xFF, h0 & 0xFF,
	      (h1 >> 24) & 0xFF, (h1 >> 16) & 0xFF, (h1 >> 8) & 0xFF, h1 & 0xFF,
	      (h2 >> 24) & 0xFF, (h2 >> 16) & 0xFF, (h2 >> 8) & 0xFF, h2 & 0xFF,
	      (h3 >> 24) & 0xFF, (h3 >> 16) & 0xFF, (h3 >> 8) & 0xFF, h3 & 0xFF,
	      (h4 >> 24) & 0xFF, (h4 >> 16) & 0xFF, (h4 >> 8) & 0xFF, h4 & 0xFF,
	      (h5 >> 24) & 0xFF, (h5 >> 16) & 0xFF, (h5 >> 8) & 0xFF, h5 & 0xFF,
	      (h6 >> 24) & 0xFF, (h6 >> 16) & 0xFF, (h6 >> 8) & 0xFF, h6 & 0xFF
	    ];
	    if (!this.is224) {
	      arr.push((h7 >> 24) & 0xFF, (h7 >> 16) & 0xFF, (h7 >> 8) & 0xFF, h7 & 0xFF);
	    }
	    return arr;
	  };

	  Sha256.prototype.array = Sha256.prototype.digest;

	  Sha256.prototype.arrayBuffer = function () {
	    this.finalize();

	    var buffer = new ArrayBuffer(this.is224 ? 28 : 32);
	    var dataView = new DataView(buffer);
	    dataView.setUint32(0, this.h0);
	    dataView.setUint32(4, this.h1);
	    dataView.setUint32(8, this.h2);
	    dataView.setUint32(12, this.h3);
	    dataView.setUint32(16, this.h4);
	    dataView.setUint32(20, this.h5);
	    dataView.setUint32(24, this.h6);
	    if (!this.is224) {
	      dataView.setUint32(28, this.h7);
	    }
	    return buffer;
	  };

	  function HmacSha256(key, is224, sharedMemory) {
	    var i, type = typeof key;
	    if (type === 'string') {
	      var bytes = [], length = key.length, index = 0, code;
	      for (i = 0; i < length; ++i) {
	        code = key.charCodeAt(i);
	        if (code < 0x80) {
	          bytes[index++] = code;
	        } else if (code < 0x800) {
	          bytes[index++] = (0xc0 | (code >> 6));
	          bytes[index++] = (0x80 | (code & 0x3f));
	        } else if (code < 0xd800 || code >= 0xe000) {
	          bytes[index++] = (0xe0 | (code >> 12));
	          bytes[index++] = (0x80 | ((code >> 6) & 0x3f));
	          bytes[index++] = (0x80 | (code & 0x3f));
	        } else {
	          code = 0x10000 + (((code & 0x3ff) << 10) | (key.charCodeAt(++i) & 0x3ff));
	          bytes[index++] = (0xf0 | (code >> 18));
	          bytes[index++] = (0x80 | ((code >> 12) & 0x3f));
	          bytes[index++] = (0x80 | ((code >> 6) & 0x3f));
	          bytes[index++] = (0x80 | (code & 0x3f));
	        }
	      }
	      key = bytes;
	    } else {
	      if (type === 'object') {
	        if (key === null) {
	          throw new Error(ERROR);
	        } else if (ARRAY_BUFFER && key.constructor === ArrayBuffer) {
	          key = new Uint8Array(key);
	        } else if (!Array.isArray(key)) {
	          if (!ARRAY_BUFFER || !ArrayBuffer.isView(key)) {
	            throw new Error(ERROR);
	          }
	        }
	      } else {
	        throw new Error(ERROR);
	      }
	    }

	    if (key.length > 64) {
	      key = (new Sha256(is224, true)).update(key).array();
	    }

	    var oKeyPad = [], iKeyPad = [];
	    for (i = 0; i < 64; ++i) {
	      var b = key[i] || 0;
	      oKeyPad[i] = 0x5c ^ b;
	      iKeyPad[i] = 0x36 ^ b;
	    }

	    Sha256.call(this, is224, sharedMemory);

	    this.update(iKeyPad);
	    this.oKeyPad = oKeyPad;
	    this.inner = true;
	    this.sharedMemory = sharedMemory;
	  }
	  HmacSha256.prototype = new Sha256();

	  HmacSha256.prototype.finalize = function () {
	    Sha256.prototype.finalize.call(this);
	    if (this.inner) {
	      this.inner = false;
	      var innerHash = this.array();
	      Sha256.call(this, this.is224, this.sharedMemory);
	      this.update(this.oKeyPad);
	      this.update(innerHash);
	      Sha256.prototype.finalize.call(this);
	    }
	  };

	  var exports = createMethod();
	  exports.sha256 = exports;
	  exports.sha224 = createMethod(true);
	  exports.sha256.hmac = createHmacMethod();
	  exports.sha224.hmac = createHmacMethod(true);

	  if (COMMON_JS) {
	    module.exports = exports;
	  } else {
	    root.sha256 = exports.sha256;
	    root.sha224 = exports.sha224;
	  }
	})();
	}(sha256$1));

	var sha256 = sha256$1.exports;

	if (typeof es6Promise_min.exports.Promise === 'undefined') {
	    throw Error('Keycloak requires an environment that supports Promises. Make sure that you include the appropriate polyfill.');
	}

	var loggedPromiseDeprecation = false;

	function logPromiseDeprecation() {
	    if (!loggedPromiseDeprecation) {
	        loggedPromiseDeprecation = true;
	        console.warn('[KEYCLOAK] Usage of legacy style promise methods such as `.error()` and `.success()` has been deprecated and support will be removed in future versions. Use standard style promise methods such as `.then() and `.catch()` instead.');
	    }
	}

	function Keycloak (config) {
	    if (!(this instanceof Keycloak)) {
	        return new Keycloak(config);
	    }

	    var kc = this;
	    var adapter;
	    var refreshQueue = [];
	    var callbackStorage;

	    var loginIframe = {
	        enable: true,
	        callbackList: [],
	        interval: 5
	    };

	    var scripts = document.getElementsByTagName('script');
	    for (var i = 0; i < scripts.length; i++) {
	        if ((scripts[i].src.indexOf('keycloak.js') !== -1 || scripts[i].src.indexOf('keycloak.min.js') !== -1) && scripts[i].src.indexOf('version=') !== -1) {
	            kc.iframeVersion = scripts[i].src.substring(scripts[i].src.indexOf('version=') + 8).split('&')[0];
	        }
	    }

	    var useNonce = true;
	    var logInfo = createLogger(console.info);
	    var logWarn = createLogger(console.warn);

	    kc.init = function (initOptions) {
	        kc.authenticated = false;

	        callbackStorage = createCallbackStorage();
	        var adapters = ['default', 'cordova', 'cordova-native'];

	        if (initOptions && adapters.indexOf(initOptions.adapter) > -1) {
	            adapter = loadAdapter(initOptions.adapter);
	        } else if (initOptions && typeof initOptions.adapter === "object") {
	            adapter = initOptions.adapter;
	        } else {
	            if (window.Cordova || window.cordova) {
	                adapter = loadAdapter('cordova');
	            } else {
	                adapter = loadAdapter();
	            }
	        }

	        if (initOptions) {
	            if (typeof initOptions.useNonce !== 'undefined') {
	                useNonce = initOptions.useNonce;
	            }

	            if (typeof initOptions.checkLoginIframe !== 'undefined') {
	                loginIframe.enable = initOptions.checkLoginIframe;
	            }

	            if (initOptions.checkLoginIframeInterval) {
	                loginIframe.interval = initOptions.checkLoginIframeInterval;
	            }

	            if (initOptions.onLoad === 'login-required') {
	                kc.loginRequired = true;
	            }

	            if (initOptions.responseMode) {
	                if (initOptions.responseMode === 'query' || initOptions.responseMode === 'fragment') {
	                    kc.responseMode = initOptions.responseMode;
	                } else {
	                    throw 'Invalid value for responseMode';
	                }
	            }

	            if (initOptions.flow) {
	                switch (initOptions.flow) {
	                    case 'standard':
	                        kc.responseType = 'code';
	                        break;
	                    case 'implicit':
	                        kc.responseType = 'id_token token';
	                        break;
	                    case 'hybrid':
	                        kc.responseType = 'code id_token token';
	                        break;
	                    default:
	                        throw 'Invalid value for flow';
	                }
	                kc.flow = initOptions.flow;
	            }

	            if (initOptions.timeSkew != null) {
	                kc.timeSkew = initOptions.timeSkew;
	            }

	            if(initOptions.redirectUri) {
	                kc.redirectUri = initOptions.redirectUri;
	            }

	            if (initOptions.silentCheckSsoRedirectUri) {
	                kc.silentCheckSsoRedirectUri = initOptions.silentCheckSsoRedirectUri;
	            }

	            if (typeof initOptions.silentCheckSsoFallback === 'boolean') {
	                kc.silentCheckSsoFallback = initOptions.silentCheckSsoFallback;
	            } else {
	                kc.silentCheckSsoFallback = true;
	            }

	            if (initOptions.pkceMethod) {
	                if (initOptions.pkceMethod !== "S256") {
	                    throw 'Invalid value for pkceMethod';
	                }
	                kc.pkceMethod = initOptions.pkceMethod;
	            }

	            if (typeof initOptions.enableLogging === 'boolean') {
	                kc.enableLogging = initOptions.enableLogging;
	            } else {
	                kc.enableLogging = false;
	            }

	            if (typeof initOptions.scope === 'string') {
	                kc.scope = initOptions.scope;
	            }

	            if (typeof initOptions.messageReceiveTimeout === 'number' && initOptions.messageReceiveTimeout > 0) {
	                kc.messageReceiveTimeout = initOptions.messageReceiveTimeout;
	            } else {
	                kc.messageReceiveTimeout = 10000;
	            }
	        }

	        if (!kc.responseMode) {
	            kc.responseMode = 'fragment';
	        }
	        if (!kc.responseType) {
	            kc.responseType = 'code';
	            kc.flow = 'standard';
	        }

	        var promise = createPromise();

	        var initPromise = createPromise();
	        initPromise.promise.then(function() {
	            kc.onReady && kc.onReady(kc.authenticated);
	            promise.setSuccess(kc.authenticated);
	        }).catch(function(error) {
	            promise.setError(error);
	        });

	        var configPromise = loadConfig();

	        function onLoad() {
	            var doLogin = function(prompt) {
	                if (!prompt) {
	                    options.prompt = 'none';
	                }

	                kc.login(options).then(function () {
	                    initPromise.setSuccess();
	                }).catch(function (error) {
	                    initPromise.setError(error);
	                });
	            };

	            var checkSsoSilently = function() {
	                var ifrm = document.createElement("iframe");
	                var src = kc.createLoginUrl({prompt: 'none', redirectUri: kc.silentCheckSsoRedirectUri});
	                ifrm.setAttribute("src", src);
	                ifrm.setAttribute("title", "keycloak-silent-check-sso");
	                ifrm.style.display = "none";
	                document.body.appendChild(ifrm);

	                var messageCallback = function(event) {
	                    if (event.origin !== window.location.origin || ifrm.contentWindow !== event.source) {
	                        return;
	                    }

	                    var oauth = parseCallback(event.data);
	                    processCallback(oauth, initPromise);

	                    document.body.removeChild(ifrm);
	                    window.removeEventListener("message", messageCallback);
	                };

	                window.addEventListener("message", messageCallback);
	            };

	            var options = {};
	            switch (initOptions.onLoad) {
	                case 'check-sso':
	                    if (loginIframe.enable) {
	                        setupCheckLoginIframe().then(function() {
	                            checkLoginIframe().then(function (unchanged) {
	                                if (!unchanged) {
	                                    kc.silentCheckSsoRedirectUri ? checkSsoSilently() : doLogin(false);
	                                } else {
	                                    initPromise.setSuccess();
	                                }
	                            }).catch(function (error) {
	                                initPromise.setError(error);
	                            });
	                        });
	                    } else {
	                        kc.silentCheckSsoRedirectUri ? checkSsoSilently() : doLogin(false);
	                    }
	                    break;
	                case 'login-required':
	                    doLogin(true);
	                    break;
	                default:
	                    throw 'Invalid value for onLoad';
	            }
	        }

	        function processInit() {
	            var callback = parseCallback(window.location.href);

	            if (callback) {
	                window.history.replaceState(window.history.state, null, callback.newUrl);
	            }

	            if (callback && callback.valid) {
	                return setupCheckLoginIframe().then(function() {
	                    processCallback(callback, initPromise);
	                }).catch(function (error) {
	                    initPromise.setError(error);
	                });
	            } else if (initOptions) {
	                if (initOptions.token && initOptions.refreshToken) {
	                    setToken(initOptions.token, initOptions.refreshToken, initOptions.idToken);

	                    if (loginIframe.enable) {
	                        setupCheckLoginIframe().then(function() {
	                            checkLoginIframe().then(function (unchanged) {
	                                if (unchanged) {
	                                    kc.onAuthSuccess && kc.onAuthSuccess();
	                                    initPromise.setSuccess();
	                                    scheduleCheckIframe();
	                                } else {
	                                    initPromise.setSuccess();
	                                }
	                            }).catch(function (error) {
	                                initPromise.setError(error);
	                            });
	                        });
	                    } else {
	                        kc.updateToken(-1).then(function() {
	                            kc.onAuthSuccess && kc.onAuthSuccess();
	                            initPromise.setSuccess();
	                        }).catch(function(error) {
	                            kc.onAuthError && kc.onAuthError();
	                            if (initOptions.onLoad) {
	                                onLoad();
	                            } else {
	                                initPromise.setError(error);
	                            }
	                        });
	                    }
	                } else if (initOptions.onLoad) {
	                    onLoad();
	                } else {
	                    initPromise.setSuccess();
	                }
	            } else {
	                initPromise.setSuccess();
	            }
	        }

	        function domReady() {
	            var promise = createPromise();

	            var checkReadyState = function () {
	                if (document.readyState === 'interactive' || document.readyState === 'complete') {
	                    document.removeEventListener('readystatechange', checkReadyState);
	                    promise.setSuccess();
	                }
	            };
	            document.addEventListener('readystatechange', checkReadyState);

	            checkReadyState(); // just in case the event was already fired and we missed it (in case the init is done later than at the load time, i.e. it's done from code)

	            return promise.promise;
	        }

	        configPromise.then(function () {
	            domReady()
	                .then(check3pCookiesSupported)
	                .then(processInit)
	                .catch(function (error) {
	                    promise.setError(error);
	                });
	        });
	        configPromise.catch(function (error) {
	            promise.setError(error);
	        });

	        return promise.promise;
	    };

	    kc.login = function (options) {
	        return adapter.login(options);
	    };

	    function generateRandomData(len) {
	        // use web crypto APIs if possible
	        var array = null;
	        var crypto = window.crypto || window.msCrypto;
	        if (crypto && crypto.getRandomValues && window.Uint8Array) {
	            array = new Uint8Array(len);
	            crypto.getRandomValues(array);
	            return array;
	        }

	        // fallback to Math random
	        array = new Array(len);
	        for (var j = 0; j < array.length; j++) {
	            array[j] = Math.floor(256 * Math.random());
	        }
	        return array;
	    }

	    function generateCodeVerifier(len) {
	        return generateRandomString(len, 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789');
	    }

	    function generateRandomString(len, alphabet){
	        var randomData = generateRandomData(len);
	        var chars = new Array(len);
	        for (var i = 0; i < len; i++) {
	            chars[i] = alphabet.charCodeAt(randomData[i] % alphabet.length);
	        }
	        return String.fromCharCode.apply(null, chars);
	    }

	    function generatePkceChallenge(pkceMethod, codeVerifier) {
	        switch (pkceMethod) {
	            // The use of the "plain" method is considered insecure and therefore not supported.
	            case "S256":
	                // hash codeVerifier, then encode as url-safe base64 without padding
	                var hashBytes = new Uint8Array(sha256.arrayBuffer(codeVerifier));
	                var encodedHash = base64Js.fromByteArray(hashBytes)
	                    .replace(/\+/g, '-')
	                    .replace(/\//g, '_')
	                    .replace(/\=/g, '');
	                return encodedHash;
	            default:
	                throw 'Invalid value for pkceMethod';
	        }
	    }

	    function buildClaimsParameter(requestedAcr){
	        var claims = {
	            id_token: {
	                acr: requestedAcr
	            }
	        };
	        return JSON.stringify(claims);
	    }

	    kc.createLoginUrl = function(options) {
	        var state = createUUID();
	        var nonce = createUUID();

	        var redirectUri = adapter.redirectUri(options);

	        var callbackState = {
	            state: state,
	            nonce: nonce,
	            redirectUri: encodeURIComponent(redirectUri)
	        };

	        if (options && options.prompt) {
	            callbackState.prompt = options.prompt;
	        }

	        var baseUrl;
	        if (options && options.action == 'register') {
	            baseUrl = kc.endpoints.register();
	        } else {
	            baseUrl = kc.endpoints.authorize();
	        }

	        var scope = options && options.scope || kc.scope;
	        if (!scope) {
	            // if scope is not set, default to "openid"
	            scope = "openid";
	        } else if (scope.indexOf("openid") === -1) {
	            // if openid scope is missing, prefix the given scopes with it
	            scope = "openid " + scope;
	        }

	        var url = baseUrl
	            + '?client_id=' + encodeURIComponent(kc.clientId)
	            + '&redirect_uri=' + encodeURIComponent(redirectUri)
	            + '&state=' + encodeURIComponent(state)
	            + '&response_mode=' + encodeURIComponent(kc.responseMode)
	            + '&response_type=' + encodeURIComponent(kc.responseType)
	            + '&scope=' + encodeURIComponent(scope);
	        if (useNonce) {
	            url = url + '&nonce=' + encodeURIComponent(nonce);
	        }

	        if (options && options.prompt) {
	            url += '&prompt=' + encodeURIComponent(options.prompt);
	        }

	        if (options && options.maxAge) {
	            url += '&max_age=' + encodeURIComponent(options.maxAge);
	        }

	        if (options && options.loginHint) {
	            url += '&login_hint=' + encodeURIComponent(options.loginHint);
	        }

	        if (options && options.idpHint) {
	            url += '&kc_idp_hint=' + encodeURIComponent(options.idpHint);
	        }

	        if (options && options.action && options.action != 'register') {
	            url += '&kc_action=' + encodeURIComponent(options.action);
	        }

	        if (options && options.locale) {
	            url += '&ui_locales=' + encodeURIComponent(options.locale);
	        }

	        if (options && options.acr) {
	            var claimsParameter = buildClaimsParameter(options.acr);
	            url += '&claims=' + encodeURIComponent(claimsParameter);
	        }

	        if (kc.pkceMethod) {
	            var codeVerifier = generateCodeVerifier(96);
	            callbackState.pkceCodeVerifier = codeVerifier;
	            var pkceChallenge = generatePkceChallenge(kc.pkceMethod, codeVerifier);
	            url += '&code_challenge=' + pkceChallenge;
	            url += '&code_challenge_method=' + kc.pkceMethod;
	        }

	        callbackStorage.add(callbackState);

	        return url;
	    };

	    kc.logout = function(options) {
	        return adapter.logout(options);
	    };

	    kc.createLogoutUrl = function(options) {
	        var url = kc.endpoints.logout()
	            + '?redirect_uri=' + encodeURIComponent(adapter.redirectUri(options, false));

	        return url;
	    };

	    kc.register = function (options) {
	        return adapter.register(options);
	    };

	    kc.createRegisterUrl = function(options) {
	        if (!options) {
	            options = {};
	        }
	        options.action = 'register';
	        return kc.createLoginUrl(options);
	    };

	    kc.createAccountUrl = function(options) {
	        var realm = getRealmUrl();
	        var url = undefined;
	        if (typeof realm !== 'undefined') {
	            url = realm
	            + '/account'
	            + '?referrer=' + encodeURIComponent(kc.clientId)
	            + '&referrer_uri=' + encodeURIComponent(adapter.redirectUri(options));
	        }
	        return url;
	    };

	    kc.accountManagement = function() {
	        return adapter.accountManagement();
	    };

	    kc.hasRealmRole = function (role) {
	        var access = kc.realmAccess;
	        return !!access && access.roles.indexOf(role) >= 0;
	    };

	    kc.hasResourceRole = function(role, resource) {
	        if (!kc.resourceAccess) {
	            return false;
	        }

	        var access = kc.resourceAccess[resource || kc.clientId];
	        return !!access && access.roles.indexOf(role) >= 0;
	    };

	    kc.loadUserProfile = function() {
	        var url = getRealmUrl() + '/account';
	        var req = new XMLHttpRequest();
	        req.open('GET', url, true);
	        req.setRequestHeader('Accept', 'application/json');
	        req.setRequestHeader('Authorization', 'bearer ' + kc.token);

	        var promise = createPromise();

	        req.onreadystatechange = function () {
	            if (req.readyState == 4) {
	                if (req.status == 200) {
	                    kc.profile = JSON.parse(req.responseText);
	                    promise.setSuccess(kc.profile);
	                } else {
	                    promise.setError();
	                }
	            }
	        };

	        req.send();

	        return promise.promise;
	    };

	    kc.loadUserInfo = function() {
	        var url = kc.endpoints.userinfo();
	        var req = new XMLHttpRequest();
	        req.open('GET', url, true);
	        req.setRequestHeader('Accept', 'application/json');
	        req.setRequestHeader('Authorization', 'bearer ' + kc.token);

	        var promise = createPromise();

	        req.onreadystatechange = function () {
	            if (req.readyState == 4) {
	                if (req.status == 200) {
	                    kc.userInfo = JSON.parse(req.responseText);
	                    promise.setSuccess(kc.userInfo);
	                } else {
	                    promise.setError();
	                }
	            }
	        };

	        req.send();

	        return promise.promise;
	    };

	    kc.isTokenExpired = function(minValidity) {
	        if (!kc.tokenParsed || (!kc.refreshToken && kc.flow != 'implicit' )) {
	            throw 'Not authenticated';
	        }

	        if (kc.timeSkew == null) {
	            logInfo('[KEYCLOAK] Unable to determine if token is expired as timeskew is not set');
	            return true;
	        }

	        var expiresIn = kc.tokenParsed['exp'] - Math.ceil(new Date().getTime() / 1000) + kc.timeSkew;
	        if (minValidity) {
	            if (isNaN(minValidity)) {
	                throw 'Invalid minValidity';
	            }
	            expiresIn -= minValidity;
	        }
	        return expiresIn < 0;
	    };

	    kc.updateToken = function(minValidity) {
	        var promise = createPromise();

	        if (!kc.refreshToken) {
	            promise.setError();
	            return promise.promise;
	        }

	        minValidity = minValidity || 5;

	        var exec = function() {
	            var refreshToken = false;
	            if (minValidity == -1) {
	                refreshToken = true;
	                logInfo('[KEYCLOAK] Refreshing token: forced refresh');
	            } else if (!kc.tokenParsed || kc.isTokenExpired(minValidity)) {
	                refreshToken = true;
	                logInfo('[KEYCLOAK] Refreshing token: token expired');
	            }

	            if (!refreshToken) {
	                promise.setSuccess(false);
	            } else {
	                var params = 'grant_type=refresh_token&' + 'refresh_token=' + kc.refreshToken;
	                var url = kc.endpoints.token();

	                refreshQueue.push(promise);

	                if (refreshQueue.length == 1) {
	                    var req = new XMLHttpRequest();
	                    req.open('POST', url, true);
	                    req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');
	                    req.withCredentials = true;

	                    params += '&client_id=' + encodeURIComponent(kc.clientId);

	                    var timeLocal = new Date().getTime();

	                    req.onreadystatechange = function () {
	                        if (req.readyState == 4) {
	                            if (req.status == 200) {
	                                logInfo('[KEYCLOAK] Token refreshed');

	                                timeLocal = (timeLocal + new Date().getTime()) / 2;

	                                var tokenResponse = JSON.parse(req.responseText);

	                                setToken(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], timeLocal);

	                                kc.onAuthRefreshSuccess && kc.onAuthRefreshSuccess();
	                                for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
	                                    p.setSuccess(true);
	                                }
	                            } else {
	                                logWarn('[KEYCLOAK] Failed to refresh token');

	                                if (req.status == 400) {
	                                    kc.clearToken();
	                                }

	                                kc.onAuthRefreshError && kc.onAuthRefreshError();
	                                for (var p = refreshQueue.pop(); p != null; p = refreshQueue.pop()) {
	                                    p.setError(true);
	                                }
	                            }
	                        }
	                    };

	                    req.send(params);
	                }
	            }
	        };

	        if (loginIframe.enable) {
	            var iframePromise = checkLoginIframe();
	            iframePromise.then(function() {
	                exec();
	            }).catch(function(error) {
	                promise.setError(error);
	            });
	        } else {
	            exec();
	        }

	        return promise.promise;
	    };

	    kc.clearToken = function() {
	        if (kc.token) {
	            setToken(null, null, null);
	            kc.onAuthLogout && kc.onAuthLogout();
	            if (kc.loginRequired) {
	                kc.login();
	            }
	        }
	    };

	    function getRealmUrl() {
	        if (typeof kc.authServerUrl !== 'undefined') {
	            if (kc.authServerUrl.charAt(kc.authServerUrl.length - 1) == '/') {
	                return kc.authServerUrl + 'realms/' + encodeURIComponent(kc.realm);
	            } else {
	                return kc.authServerUrl + '/realms/' + encodeURIComponent(kc.realm);
	            }
	        } else {
	            return undefined;
	        }
	    }

	    function getOrigin() {
	        if (!window.location.origin) {
	            return window.location.protocol + "//" + window.location.hostname + (window.location.port ? ':' + window.location.port: '');
	        } else {
	            return window.location.origin;
	        }
	    }

	    function processCallback(oauth, promise) {
	        var code = oauth.code;
	        var error = oauth.error;
	        var prompt = oauth.prompt;

	        var timeLocal = new Date().getTime();

	        if (oauth['kc_action_status']) {
	            kc.onActionUpdate && kc.onActionUpdate(oauth['kc_action_status']);
	        }

	        if (error) {
	            if (prompt != 'none') {
	                var errorData = { error: error, error_description: oauth.error_description };
	                kc.onAuthError && kc.onAuthError(errorData);
	                promise && promise.setError(errorData);
	            } else {
	                promise && promise.setSuccess();
	            }
	            return;
	        } else if ((kc.flow != 'standard') && (oauth.access_token || oauth.id_token)) {
	            authSuccess(oauth.access_token, null, oauth.id_token, true);
	        }

	        if ((kc.flow != 'implicit') && code) {
	            var params = 'code=' + code + '&grant_type=authorization_code';
	            var url = kc.endpoints.token();

	            var req = new XMLHttpRequest();
	            req.open('POST', url, true);
	            req.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

	            params += '&client_id=' + encodeURIComponent(kc.clientId);
	            params += '&redirect_uri=' + oauth.redirectUri;

	            if (oauth.pkceCodeVerifier) {
	                params += '&code_verifier=' + oauth.pkceCodeVerifier;
	            }

	            req.withCredentials = true;

	            req.onreadystatechange = function() {
	                if (req.readyState == 4) {
	                    if (req.status == 200) {

	                        var tokenResponse = JSON.parse(req.responseText);
	                        authSuccess(tokenResponse['access_token'], tokenResponse['refresh_token'], tokenResponse['id_token'], kc.flow === 'standard');
	                        scheduleCheckIframe();
	                    } else {
	                        kc.onAuthError && kc.onAuthError();
	                        promise && promise.setError();
	                    }
	                }
	            };

	            req.send(params);
	        }

	        function authSuccess(accessToken, refreshToken, idToken, fulfillPromise) {
	            timeLocal = (timeLocal + new Date().getTime()) / 2;

	            setToken(accessToken, refreshToken, idToken, timeLocal);

	            if (useNonce && ((kc.tokenParsed && kc.tokenParsed.nonce != oauth.storedNonce) ||
	                (kc.refreshTokenParsed && kc.refreshTokenParsed.nonce != oauth.storedNonce) ||
	                (kc.idTokenParsed && kc.idTokenParsed.nonce != oauth.storedNonce))) {

	                logInfo('[KEYCLOAK] Invalid nonce, clearing token');
	                kc.clearToken();
	                promise && promise.setError();
	            } else {
	                if (fulfillPromise) {
	                    kc.onAuthSuccess && kc.onAuthSuccess();
	                    promise && promise.setSuccess();
	                }
	            }
	        }

	    }

	    function loadConfig(url) {
	        var promise = createPromise();
	        var configUrl;

	        if (!config) {
	            configUrl = 'keycloak.json';
	        } else if (typeof config === 'string') {
	            configUrl = config;
	        }

	        function setupOidcEndoints(oidcConfiguration) {
	            if (! oidcConfiguration) {
	                kc.endpoints = {
	                    authorize: function() {
	                        return getRealmUrl() + '/protocol/openid-connect/auth';
	                    },
	                    token: function() {
	                        return getRealmUrl() + '/protocol/openid-connect/token';
	                    },
	                    logout: function() {
	                        return getRealmUrl() + '/protocol/openid-connect/logout';
	                    },
	                    checkSessionIframe: function() {
	                        var src = getRealmUrl() + '/protocol/openid-connect/login-status-iframe.html';
	                        if (kc.iframeVersion) {
	                            src = src + '?version=' + kc.iframeVersion;
	                        }
	                        return src;
	                    },
	                    thirdPartyCookiesIframe: function() {
	                        var src = getRealmUrl() + '/protocol/openid-connect/3p-cookies/step1.html';
	                        if (kc.iframeVersion) {
	                            src = src + '?version=' + kc.iframeVersion;
	                        }
	                        return src;
	                    },
	                    register: function() {
	                        return getRealmUrl() + '/protocol/openid-connect/registrations';
	                    },
	                    userinfo: function() {
	                        return getRealmUrl() + '/protocol/openid-connect/userinfo';
	                    }
	                };
	            } else {
	                kc.endpoints = {
	                    authorize: function() {
	                        return oidcConfiguration.authorization_endpoint;
	                    },
	                    token: function() {
	                        return oidcConfiguration.token_endpoint;
	                    },
	                    logout: function() {
	                        if (!oidcConfiguration.end_session_endpoint) {
	                            throw "Not supported by the OIDC server";
	                        }
	                        return oidcConfiguration.end_session_endpoint;
	                    },
	                    checkSessionIframe: function() {
	                        if (!oidcConfiguration.check_session_iframe) {
	                            throw "Not supported by the OIDC server";
	                        }
	                        return oidcConfiguration.check_session_iframe;
	                    },
	                    register: function() {
	                        throw 'Redirection to "Register user" page not supported in standard OIDC mode';
	                    },
	                    userinfo: function() {
	                        if (!oidcConfiguration.userinfo_endpoint) {
	                            throw "Not supported by the OIDC server";
	                        }
	                        return oidcConfiguration.userinfo_endpoint;
	                    }
	                };
	            }
	        }

	        if (configUrl) {
	            var req = new XMLHttpRequest();
	            req.open('GET', configUrl, true);
	            req.setRequestHeader('Accept', 'application/json');

	            req.onreadystatechange = function () {
	                if (req.readyState == 4) {
	                    if (req.status == 200 || fileLoaded(req)) {
	                        var config = JSON.parse(req.responseText);

	                        kc.authServerUrl = config['auth-server-url'];
	                        kc.realm = config['realm'];
	                        kc.clientId = config['resource'];
	                        setupOidcEndoints(null);
	                        promise.setSuccess();
	                    } else {
	                        promise.setError();
	                    }
	                }
	            };

	            req.send();
	        } else {
	            if (!config.clientId) {
	                throw 'clientId missing';
	            }

	            kc.clientId = config.clientId;

	            var oidcProvider = config['oidcProvider'];
	            if (!oidcProvider) {
	                if (!config['url']) {
	                    var scripts = document.getElementsByTagName('script');
	                    for (var i = 0; i < scripts.length; i++) {
	                        if (scripts[i].src.match(/.*keycloak\.js/)) {
	                            config.url = scripts[i].src.substr(0, scripts[i].src.indexOf('/js/keycloak.js'));
	                            break;
	                        }
	                    }
	                }
	                if (!config.realm) {
	                    throw 'realm missing';
	                }

	                kc.authServerUrl = config.url;
	                kc.realm = config.realm;
	                setupOidcEndoints(null);
	                promise.setSuccess();
	            } else {
	                if (typeof oidcProvider === 'string') {
	                    var oidcProviderConfigUrl;
	                    if (oidcProvider.charAt(oidcProvider.length - 1) == '/') {
	                        oidcProviderConfigUrl = oidcProvider + '.well-known/openid-configuration';
	                    } else {
	                        oidcProviderConfigUrl = oidcProvider + '/.well-known/openid-configuration';
	                    }
	                    var req = new XMLHttpRequest();
	                    req.open('GET', oidcProviderConfigUrl, true);
	                    req.setRequestHeader('Accept', 'application/json');

	                    req.onreadystatechange = function () {
	                        if (req.readyState == 4) {
	                            if (req.status == 200 || fileLoaded(req)) {
	                                var oidcProviderConfig = JSON.parse(req.responseText);
	                                setupOidcEndoints(oidcProviderConfig);
	                                promise.setSuccess();
	                            } else {
	                                promise.setError();
	                            }
	                        }
	                    };

	                    req.send();
	                } else {
	                    setupOidcEndoints(oidcProvider);
	                    promise.setSuccess();
	                }
	            }
	        }

	        return promise.promise;
	    }

	    function fileLoaded(xhr) {
	        return xhr.status == 0 && xhr.responseText && xhr.responseURL.startsWith('file:');
	    }

	    function setToken(token, refreshToken, idToken, timeLocal) {
	        if (kc.tokenTimeoutHandle) {
	            clearTimeout(kc.tokenTimeoutHandle);
	            kc.tokenTimeoutHandle = null;
	        }

	        if (refreshToken) {
	            kc.refreshToken = refreshToken;
	            kc.refreshTokenParsed = decodeToken(refreshToken);
	        } else {
	            delete kc.refreshToken;
	            delete kc.refreshTokenParsed;
	        }

	        if (idToken) {
	            kc.idToken = idToken;
	            kc.idTokenParsed = decodeToken(idToken);
	        } else {
	            delete kc.idToken;
	            delete kc.idTokenParsed;
	        }

	        if (token) {
	            kc.token = token;
	            kc.tokenParsed = decodeToken(token);
	            kc.sessionId = kc.tokenParsed.session_state;
	            kc.authenticated = true;
	            kc.subject = kc.tokenParsed.sub;
	            kc.realmAccess = kc.tokenParsed.realm_access;
	            kc.resourceAccess = kc.tokenParsed.resource_access;

	            if (timeLocal) {
	                kc.timeSkew = Math.floor(timeLocal / 1000) - kc.tokenParsed.iat;
	            }

	            if (kc.timeSkew != null) {
	                logInfo('[KEYCLOAK] Estimated time difference between browser and server is ' + kc.timeSkew + ' seconds');

	                if (kc.onTokenExpired) {
	                    var expiresIn = (kc.tokenParsed['exp'] - (new Date().getTime() / 1000) + kc.timeSkew) * 1000;
	                    logInfo('[KEYCLOAK] Token expires in ' + Math.round(expiresIn / 1000) + ' s');
	                    if (expiresIn <= 0) {
	                        kc.onTokenExpired();
	                    } else {
	                        kc.tokenTimeoutHandle = setTimeout(kc.onTokenExpired, expiresIn);
	                    }
	                }
	            }
	        } else {
	            delete kc.token;
	            delete kc.tokenParsed;
	            delete kc.subject;
	            delete kc.realmAccess;
	            delete kc.resourceAccess;

	            kc.authenticated = false;
	        }
	    }

	    function decodeToken(str) {
	        str = str.split('.')[1];

	        str = str.replace(/-/g, '+');
	        str = str.replace(/_/g, '/');
	        switch (str.length % 4) {
	            case 0:
	                break;
	            case 2:
	                str += '==';
	                break;
	            case 3:
	                str += '=';
	                break;
	            default:
	                throw 'Invalid token';
	        }

	        str = decodeURIComponent(escape(atob(str)));

	        str = JSON.parse(str);
	        return str;
	    }

	    function createUUID() {
	        var hexDigits = '0123456789abcdef';
	        var s = generateRandomString(36, hexDigits).split("");
	        s[14] = '4';
	        s[19] = hexDigits.substr((s[19] & 0x3) | 0x8, 1);
	        s[8] = s[13] = s[18] = s[23] = '-';
	        var uuid = s.join('');
	        return uuid;
	    }

	    function parseCallback(url) {
	        var oauth = parseCallbackUrl(url);
	        if (!oauth) {
	            return;
	        }

	        var oauthState = callbackStorage.get(oauth.state);

	        if (oauthState) {
	            oauth.valid = true;
	            oauth.redirectUri = oauthState.redirectUri;
	            oauth.storedNonce = oauthState.nonce;
	            oauth.prompt = oauthState.prompt;
	            oauth.pkceCodeVerifier = oauthState.pkceCodeVerifier;
	        }

	        return oauth;
	    }

	    function parseCallbackUrl(url) {
	        var supportedParams;
	        switch (kc.flow) {
	            case 'standard':
	                supportedParams = ['code', 'state', 'session_state', 'kc_action_status'];
	                break;
	            case 'implicit':
	                supportedParams = ['access_token', 'token_type', 'id_token', 'state', 'session_state', 'expires_in', 'kc_action_status'];
	                break;
	            case 'hybrid':
	                supportedParams = ['access_token', 'token_type', 'id_token', 'code', 'state', 'session_state', 'expires_in', 'kc_action_status'];
	                break;
	        }

	        supportedParams.push('error');
	        supportedParams.push('error_description');
	        supportedParams.push('error_uri');

	        var queryIndex = url.indexOf('?');
	        var fragmentIndex = url.indexOf('#');

	        var newUrl;
	        var parsed;

	        if (kc.responseMode === 'query' && queryIndex !== -1) {
	            newUrl = url.substring(0, queryIndex);
	            parsed = parseCallbackParams(url.substring(queryIndex + 1, fragmentIndex !== -1 ? fragmentIndex : url.length), supportedParams);
	            if (parsed.paramsString !== '') {
	                newUrl += '?' + parsed.paramsString;
	            }
	            if (fragmentIndex !== -1) {
	                newUrl += url.substring(fragmentIndex);
	            }
	        } else if (kc.responseMode === 'fragment' && fragmentIndex !== -1) {
	            newUrl = url.substring(0, fragmentIndex);
	            parsed = parseCallbackParams(url.substring(fragmentIndex + 1), supportedParams);
	            if (parsed.paramsString !== '') {
	                newUrl += '#' + parsed.paramsString;
	            }
	        }

	        if (parsed && parsed.oauthParams) {
	            if (kc.flow === 'standard' || kc.flow === 'hybrid') {
	                if ((parsed.oauthParams.code || parsed.oauthParams.error) && parsed.oauthParams.state) {
	                    parsed.oauthParams.newUrl = newUrl;
	                    return parsed.oauthParams;
	                }
	            } else if (kc.flow === 'implicit') {
	                if ((parsed.oauthParams.access_token || parsed.oauthParams.error) && parsed.oauthParams.state) {
	                    parsed.oauthParams.newUrl = newUrl;
	                    return parsed.oauthParams;
	                }
	            }
	        }
	    }

	    function parseCallbackParams(paramsString, supportedParams) {
	        var p = paramsString.split('&');
	        var result = {
	            paramsString: '',
	            oauthParams: {}
	        };
	        for (var i = 0; i < p.length; i++) {
	            var split = p[i].indexOf("=");
	            var key = p[i].slice(0, split);
	            if (supportedParams.indexOf(key) !== -1) {
	                result.oauthParams[key] = p[i].slice(split + 1);
	            } else {
	                if (result.paramsString !== '') {
	                    result.paramsString += '&';
	                }
	                result.paramsString += p[i];
	            }
	        }
	        return result;
	    }

	    function createPromise() {
	        // Need to create a native Promise which also preserves the
	        // interface of the custom promise type previously used by the API
	        var p = {
	            setSuccess: function(result) {
	                p.resolve(result);
	            },

	            setError: function(result) {
	                p.reject(result);
	            }
	        };
	        p.promise = new es6Promise_min.exports.Promise(function(resolve, reject) {
	            p.resolve = resolve;
	            p.reject = reject;
	        });

	        p.promise.success = function(callback) {
	            logPromiseDeprecation();

	            this.then(function handleSuccess(value) {
	                callback(value);
	            });

	            return this;
	        };

	        p.promise.error = function(callback) {
	            logPromiseDeprecation();

	            this.catch(function handleError(error) {
	                callback(error);
	            });

	            return this;
	        };

	        return p;
	    }

	    // Function to extend existing native Promise with timeout
	    function applyTimeoutToPromise(promise, timeout, errorMessage) {
	        var timeoutHandle = null;
	        var timeoutPromise = new es6Promise_min.exports.Promise(function (resolve, reject) {
	            timeoutHandle = setTimeout(function () {
	                reject({ "error": errorMessage || "Promise is not settled within timeout of " + timeout + "ms" });
	            }, timeout);
	        });

	        return es6Promise_min.exports.Promise.race([promise, timeoutPromise]).finally(function () {
	            clearTimeout(timeoutHandle);
	        });
	    }

	    function setupCheckLoginIframe() {
	        var promise = createPromise();

	        if (!loginIframe.enable) {
	            promise.setSuccess();
	            return promise.promise;
	        }

	        if (loginIframe.iframe) {
	            promise.setSuccess();
	            return promise.promise;
	        }

	        var iframe = document.createElement('iframe');
	        loginIframe.iframe = iframe;

	        iframe.onload = function() {
	            var authUrl = kc.endpoints.authorize();
	            if (authUrl.charAt(0) === '/') {
	                loginIframe.iframeOrigin = getOrigin();
	            } else {
	                loginIframe.iframeOrigin = authUrl.substring(0, authUrl.indexOf('/', 8));
	            }
	            promise.setSuccess();
	        };

	        var src = kc.endpoints.checkSessionIframe();
	        iframe.setAttribute('src', src );
	        iframe.setAttribute('title', 'keycloak-session-iframe' );
	        iframe.style.display = 'none';
	        document.body.appendChild(iframe);

	        var messageCallback = function(event) {
	            if ((event.origin !== loginIframe.iframeOrigin) || (loginIframe.iframe.contentWindow !== event.source)) {
	                return;
	            }

	            if (!(event.data == 'unchanged' || event.data == 'changed' || event.data == 'error')) {
	                return;
	            }


	            if (event.data != 'unchanged') {
	                kc.clearToken();
	            }

	            var callbacks = loginIframe.callbackList.splice(0, loginIframe.callbackList.length);

	            for (var i = callbacks.length - 1; i >= 0; --i) {
	                var promise = callbacks[i];
	                if (event.data == 'error') {
	                    promise.setError();
	                } else {
	                    promise.setSuccess(event.data == 'unchanged');
	                }
	            }
	        };

	        window.addEventListener('message', messageCallback, false);

	        return promise.promise;
	    }

	    function scheduleCheckIframe() {
	        if (loginIframe.enable) {
	            if (kc.token) {
	                setTimeout(function() {
	                    checkLoginIframe().then(function(unchanged) {
	                        if (unchanged) {
	                            scheduleCheckIframe();
	                        }
	                    });
	                }, loginIframe.interval * 1000);
	            }
	        }
	    }

	    function checkLoginIframe() {
	        var promise = createPromise();

	        if (loginIframe.iframe && loginIframe.iframeOrigin ) {
	            var msg = kc.clientId + ' ' + (kc.sessionId ? kc.sessionId : '');
	            loginIframe.callbackList.push(promise);
	            var origin = loginIframe.iframeOrigin;
	            if (loginIframe.callbackList.length == 1) {
	                loginIframe.iframe.contentWindow.postMessage(msg, origin);
	            }
	        } else {
	            promise.setSuccess();
	        }

	        return promise.promise;
	    }

	    function check3pCookiesSupported() {
	        var promise = createPromise();

	        if (loginIframe.enable || kc.silentCheckSsoRedirectUri) {
	            var iframe = document.createElement('iframe');
	            iframe.setAttribute('src', kc.endpoints.thirdPartyCookiesIframe());
	            iframe.setAttribute('title', 'keycloak-3p-check-iframe' );
	            iframe.style.display = 'none';
	            document.body.appendChild(iframe);

	            var messageCallback = function(event) {
	                if (iframe.contentWindow !== event.source) {
	                    return;
	                }

	                if (event.data !== "supported" && event.data !== "unsupported") {
	                    return;
	                } else if (event.data === "unsupported") {
	                    loginIframe.enable = false;
	                    if (kc.silentCheckSsoFallback) {
	                        kc.silentCheckSsoRedirectUri = false;
	                    }
	                    logWarn("[KEYCLOAK] 3rd party cookies aren't supported by this browser. checkLoginIframe and " +
	                        "silent check-sso are not available.");
	                }

	                document.body.removeChild(iframe);
	                window.removeEventListener("message", messageCallback);
	                promise.setSuccess();
	            };

	            window.addEventListener('message', messageCallback, false);
	        } else {
	            promise.setSuccess();
	        }

	        return applyTimeoutToPromise(promise.promise, kc.messageReceiveTimeout, "Timeout when waiting for 3rd party check iframe message.");
	    }

	    function loadAdapter(type) {
	        if (!type || type == 'default') {
	            return {
	                login: function(options) {
	                    window.location.replace(kc.createLoginUrl(options));
	                    return createPromise().promise;
	                },

	                logout: function(options) {
	                    window.location.replace(kc.createLogoutUrl(options));
	                    return createPromise().promise;
	                },

	                register: function(options) {
	                    window.location.replace(kc.createRegisterUrl(options));
	                    return createPromise().promise;
	                },

	                accountManagement : function() {
	                    var accountUrl = kc.createAccountUrl();
	                    if (typeof accountUrl !== 'undefined') {
	                        window.location.href = accountUrl;
	                    } else {
	                        throw "Not supported by the OIDC server";
	                    }
	                    return createPromise().promise;
	                },

	                redirectUri: function(options, encodeHash) {

	                    if (options && options.redirectUri) {
	                        return options.redirectUri;
	                    } else if (kc.redirectUri) {
	                        return kc.redirectUri;
	                    } else {
	                        return location.href;
	                    }
	                }
	            };
	        }

	        if (type == 'cordova') {
	            loginIframe.enable = false;
	            var cordovaOpenWindowWrapper = function(loginUrl, target, options) {
	                if (window.cordova && window.cordova.InAppBrowser) {
	                    // Use inappbrowser for IOS and Android if available
	                    return window.cordova.InAppBrowser.open(loginUrl, target, options);
	                } else {
	                    return window.open(loginUrl, target, options);
	                }
	            };

	            var shallowCloneCordovaOptions = function (userOptions) {
	                if (userOptions && userOptions.cordovaOptions) {
	                    return Object.keys(userOptions.cordovaOptions).reduce(function (options, optionName) {
	                        options[optionName] = userOptions.cordovaOptions[optionName];
	                        return options;
	                    }, {});
	                } else {
	                    return {};
	                }
	            };

	            var formatCordovaOptions = function (cordovaOptions) {
	                return Object.keys(cordovaOptions).reduce(function (options, optionName) {
	                    options.push(optionName+"="+cordovaOptions[optionName]);
	                    return options;
	                }, []).join(",");
	            };

	            var createCordovaOptions = function (userOptions) {
	                var cordovaOptions = shallowCloneCordovaOptions(userOptions);
	                cordovaOptions.location = 'no';
	                if (userOptions && userOptions.prompt == 'none') {
	                    cordovaOptions.hidden = 'yes';
	                }
	                return formatCordovaOptions(cordovaOptions);
	            };

	            return {
	                login: function(options) {
	                    var promise = createPromise();

	                    var cordovaOptions = createCordovaOptions(options);
	                    var loginUrl = kc.createLoginUrl(options);
	                    var ref = cordovaOpenWindowWrapper(loginUrl, '_blank', cordovaOptions);
	                    var completed = false;

	                    var closed = false;
	                    var closeBrowser = function() {
	                        closed = true;
	                        ref.close();
	                    };

	                    ref.addEventListener('loadstart', function(event) {
	                        if (event.url.indexOf('http://localhost') == 0) {
	                            var callback = parseCallback(event.url);
	                            processCallback(callback, promise);
	                            closeBrowser();
	                            completed = true;
	                        }
	                    });

	                    ref.addEventListener('loaderror', function(event) {
	                        if (!completed) {
	                            if (event.url.indexOf('http://localhost') == 0) {
	                                var callback = parseCallback(event.url);
	                                processCallback(callback, promise);
	                                closeBrowser();
	                                completed = true;
	                            } else {
	                                promise.setError();
	                                closeBrowser();
	                            }
	                        }
	                    });

	                    ref.addEventListener('exit', function(event) {
	                        if (!closed) {
	                            promise.setError({
	                                reason: "closed_by_user"
	                            });
	                        }
	                    });

	                    return promise.promise;
	                },

	                logout: function(options) {
	                    var promise = createPromise();

	                    var logoutUrl = kc.createLogoutUrl(options);
	                    var ref = cordovaOpenWindowWrapper(logoutUrl, '_blank', 'location=no,hidden=yes,clearcache=yes');

	                    var error;

	                    ref.addEventListener('loadstart', function(event) {
	                        if (event.url.indexOf('http://localhost') == 0) {
	                            ref.close();
	                        }
	                    });

	                    ref.addEventListener('loaderror', function(event) {
	                        if (event.url.indexOf('http://localhost') == 0) {
	                            ref.close();
	                        } else {
	                            error = true;
	                            ref.close();
	                        }
	                    });

	                    ref.addEventListener('exit', function(event) {
	                        if (error) {
	                            promise.setError();
	                        } else {
	                            kc.clearToken();
	                            promise.setSuccess();
	                        }
	                    });

	                    return promise.promise;
	                },

	                register : function(options) {
	                    var promise = createPromise();
	                    var registerUrl = kc.createRegisterUrl();
	                    var cordovaOptions = createCordovaOptions(options);
	                    var ref = cordovaOpenWindowWrapper(registerUrl, '_blank', cordovaOptions);
	                    ref.addEventListener('loadstart', function(event) {
	                        if (event.url.indexOf('http://localhost') == 0) {
	                            ref.close();
	                            var oauth = parseCallback(event.url);
	                            processCallback(oauth, promise);
	                        }
	                    });
	                    return promise.promise;
	                },

	                accountManagement : function() {
	                    var accountUrl = kc.createAccountUrl();
	                    if (typeof accountUrl !== 'undefined') {
	                        var ref = cordovaOpenWindowWrapper(accountUrl, '_blank', 'location=no');
	                        ref.addEventListener('loadstart', function(event) {
	                            if (event.url.indexOf('http://localhost') == 0) {
	                                ref.close();
	                            }
	                        });
	                    } else {
	                        throw "Not supported by the OIDC server";
	                    }
	                },

	                redirectUri: function(options) {
	                    return 'http://localhost';
	                }
	            }
	        }

	        if (type == 'cordova-native') {
	            loginIframe.enable = false;

	            return {
	                login: function(options) {
	                    var promise = createPromise();
	                    var loginUrl = kc.createLoginUrl(options);

	                    universalLinks.subscribe('keycloak', function(event) {
	                        universalLinks.unsubscribe('keycloak');
	                        window.cordova.plugins.browsertab.close();
	                        var oauth = parseCallback(event.url);
	                        processCallback(oauth, promise);
	                    });

	                    window.cordova.plugins.browsertab.openUrl(loginUrl);
	                    return promise.promise;
	                },

	                logout: function(options) {
	                    var promise = createPromise();
	                    var logoutUrl = kc.createLogoutUrl(options);

	                    universalLinks.subscribe('keycloak', function(event) {
	                        universalLinks.unsubscribe('keycloak');
	                        window.cordova.plugins.browsertab.close();
	                        kc.clearToken();
	                        promise.setSuccess();
	                    });

	                    window.cordova.plugins.browsertab.openUrl(logoutUrl);
	                    return promise.promise;
	                },

	                register : function(options) {
	                    var promise = createPromise();
	                    var registerUrl = kc.createRegisterUrl(options);
	                    universalLinks.subscribe('keycloak' , function(event) {
	                        universalLinks.unsubscribe('keycloak');
	                        window.cordova.plugins.browsertab.close();
	                        var oauth = parseCallback(event.url);
	                        processCallback(oauth, promise);
	                    });
	                    window.cordova.plugins.browsertab.openUrl(registerUrl);
	                    return promise.promise;

	                },

	                accountManagement : function() {
	                    var accountUrl = kc.createAccountUrl();
	                    if (typeof accountUrl !== 'undefined') {
	                        window.cordova.plugins.browsertab.openUrl(accountUrl);
	                    } else {
	                        throw "Not supported by the OIDC server";
	                    }
	                },

	                redirectUri: function(options) {
	                    if (options && options.redirectUri) {
	                        return options.redirectUri;
	                    } else if (kc.redirectUri) {
	                        return kc.redirectUri;
	                    } else {
	                        return "http://localhost";
	                    }
	                }
	            }
	        }

	        throw 'invalid adapter type: ' + type;
	    }

	    var LocalStorage = function() {
	        if (!(this instanceof LocalStorage)) {
	            return new LocalStorage();
	        }

	        localStorage.setItem('kc-test', 'test');
	        localStorage.removeItem('kc-test');

	        var cs = this;

	        function clearExpired() {
	            var time = new Date().getTime();
	            for (var i = 0; i < localStorage.length; i++)  {
	                var key = localStorage.key(i);
	                if (key && key.indexOf('kc-callback-') == 0) {
	                    var value = localStorage.getItem(key);
	                    if (value) {
	                        try {
	                            var expires = JSON.parse(value).expires;
	                            if (!expires || expires < time) {
	                                localStorage.removeItem(key);
	                            }
	                        } catch (err) {
	                            localStorage.removeItem(key);
	                        }
	                    }
	                }
	            }
	        }

	        cs.get = function(state) {
	            if (!state) {
	                return;
	            }

	            var key = 'kc-callback-' + state;
	            var value = localStorage.getItem(key);
	            if (value) {
	                localStorage.removeItem(key);
	                value = JSON.parse(value);
	            }

	            clearExpired();
	            return value;
	        };

	        cs.add = function(state) {
	            clearExpired();

	            var key = 'kc-callback-' + state.state;
	            state.expires = new Date().getTime() + (60 * 60 * 1000);
	            localStorage.setItem(key, JSON.stringify(state));
	        };
	    };

	    var CookieStorage = function() {
	        if (!(this instanceof CookieStorage)) {
	            return new CookieStorage();
	        }

	        var cs = this;

	        cs.get = function(state) {
	            if (!state) {
	                return;
	            }

	            var value = getCookie('kc-callback-' + state);
	            setCookie('kc-callback-' + state, '', cookieExpiration(-100));
	            if (value) {
	                return JSON.parse(value);
	            }
	        };

	        cs.add = function(state) {
	            setCookie('kc-callback-' + state.state, JSON.stringify(state), cookieExpiration(60));
	        };

	        cs.removeItem = function(key) {
	            setCookie(key, '', cookieExpiration(-100));
	        };

	        var cookieExpiration = function (minutes) {
	            var exp = new Date();
	            exp.setTime(exp.getTime() + (minutes*60*1000));
	            return exp;
	        };

	        var getCookie = function (key) {
	            var name = key + '=';
	            var ca = document.cookie.split(';');
	            for (var i = 0; i < ca.length; i++) {
	                var c = ca[i];
	                while (c.charAt(0) == ' ') {
	                    c = c.substring(1);
	                }
	                if (c.indexOf(name) == 0) {
	                    return c.substring(name.length, c.length);
	                }
	            }
	            return '';
	        };

	        var setCookie = function (key, value, expirationDate) {
	            var cookie = key + '=' + value + '; '
	                + 'expires=' + expirationDate.toUTCString() + '; ';
	            document.cookie = cookie;
	        };
	    };

	    function createCallbackStorage() {
	        try {
	            return new LocalStorage();
	        } catch (err) {
	        }

	        return new CookieStorage();
	    }

	    function createLogger(fn) {
	        return function() {
	            if (kc.enableLogging) {
	                fn.apply(console, Array.prototype.slice.call(arguments));
	            }
	        };
	    }
	}

	return Keycloak;

}));
