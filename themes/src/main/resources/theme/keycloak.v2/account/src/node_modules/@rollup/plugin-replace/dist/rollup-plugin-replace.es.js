import MagicString from 'magic-string';
import { createFilter } from '@rollup/pluginutils';

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

  var filter = createFilter(options.include, options.exclude);
  var delimiters = options.delimiters;
  var preventAssignment = options.preventAssignment;
  var functionValues = mapToFunctions(getReplacements(options));
  var keys = Object.keys(functionValues).sort(longest).map(escape);
  var lookahead = preventAssignment ? '(?!\\s*=[^=])' : '';
  var pattern = delimiters
    ? new RegExp(
        ((escape(delimiters[0])) + "(" + (keys.join('|')) + ")" + (escape(delimiters[1])) + lookahead),
        'g'
      )
    : new RegExp(("\\b(" + (keys.join('|')) + ")\\b" + lookahead), 'g');

  return {
    name: 'replace',

    buildStart: function buildStart() {
      if (![true, false].includes(preventAssignment)) {
        this.warn({
          message:
            "@rollup/plugin-replace: 'preventAssignment' currently defaults to false. It is recommended to set this option to `true`, as the next major version will default this option to `true`."
        });
      }
    },

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

export default replace;
