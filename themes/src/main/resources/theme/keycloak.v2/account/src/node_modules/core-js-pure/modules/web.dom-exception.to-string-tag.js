var getBuiltIn = require('../internals/get-built-in');
var setToStringTag = require('../internals/set-to-string-tag');

var DOM_EXCEPTION = 'DOMException';

setToStringTag(getBuiltIn(DOM_EXCEPTION), DOM_EXCEPTION);
