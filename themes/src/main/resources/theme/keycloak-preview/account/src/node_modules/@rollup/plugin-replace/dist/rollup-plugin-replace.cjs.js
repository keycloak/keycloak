'use strict';

function _interopDefault (ex) { return (ex && (typeof ex === 'object') && 'default' in ex) ? ex['default'] : ex; }

var MagicString = _interopDefault(require('magic-string'));
var pluginutils = require('@rollup/pluginutils');

function escape(str) {
  return str.replace(/[-[\]/{}()*+?.\\^$|]/g, '\\$&');
}

function ensureFunction(functionOrValue) {
  if (typeof functionOrValue === 'function') { return functionOrValue; }
  return function () { return functionOrValue; };
}

function longest(a, b) {
  return b.length - a.length;
}

function getReplacements(options) {
  if (options.values) {
    return Object.assign({}, options.values);
  }
  var values = Object.assign({}, options);
  delete values.delimiters;
  delete values.include;
  delete values.exclude;
  delete values.sourcemap;
  delete values.sourceMap;
  return values;
}

function mapToFunctions(object) {
  return Object.keys(object).reduce(function (fns, key) {
    var functions = Object.assign({}, fns);
    functions[key] = ensureFunction(object[key]);
    return functions;
  }, {});
}

function replace(options) {
  if ( options === void 0 ) options = {};

  var filter = pluginutils.createFilter(options.include, options.exclude);
  var delimiters = options.delimiters;
  var functionValues = mapToFunctions(getReplacements(options));
  var keys = Object.keys(functionValues)
    .sort(longest)
    .map(escape);
  var pattern = delimiters
    ? new RegExp(((escape(delimiters[0])) + "(" + (keys.join('|')) + ")" + (escape(delimiters[1]))), 'g')
    : new RegExp(("\\b(" + (keys.join('|')) + ")\\b"), 'g');

  return {
    name: 'replace',

    renderChunk: function renderChunk(code, chunk) {
      var id = chunk.fileName;
      if (!keys.length) { return null; }
      if (!filter(id)) { return null; }
      return executeReplacement(code, id);
    },

    transform: function transform(code, id) {
      if (!keys.length) { return null; }
      if (!filter(id)) { return null; }
      return executeReplacement(code, id);
    }
  };

  function executeReplacement(code, id) {
    var magicString = new MagicString(code);
    if (!codeHasReplacements(code, id, magicString)) {
      return null;
    }

    var result = { code: magicString.toString() };
    if (isSourceMapEnabled()) {
      result.map = magicString.generateMap({ hires: true });
    }
    return result;
  }

  function codeHasReplacements(code, id, magicString) {
    var result = false;
    var match;

    // eslint-disable-next-line no-cond-assign
    while ((match = pattern.exec(code))) {
      result = true;

      var start = match.index;
      var end = start + match[0].length;
      var replacement = String(functionValues[match[1]](id));
      magicString.overwrite(start, end, replacement);
    }
    return result;
  }

  function isSourceMapEnabled() {
    return options.sourceMap !== false && options.sourcemap !== false;
  }
}

module.exports = replace;
