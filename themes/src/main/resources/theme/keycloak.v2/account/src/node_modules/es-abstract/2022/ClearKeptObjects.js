'use strict';

var SLOT = require('internal-slot');
var keptObjects = [];

// https://ecma-international.org/ecma-262/12.0/#sec-clear-kept-objects

module.exports = function ClearKeptObjects() {
	keptObjects.length = 0;
};

SLOT.set(module.exports, '[[es-abstract internal: KeptAlive]]', keptObjects);
