'use strict';

var pluginutils = require('@rollup/pluginutils');

function json(options) {
  if ( options === void 0 ) options = {};

  var filter = pluginutils.createFilter(options.include, options.exclude);
  var indent = 'indent' in options ? options.indent : '\t';

  return {
    name: 'json',

    // eslint-disable-next-line no-shadow
    transform: function transform(json, id) {
      if (id.slice(-5) !== '.json' || !filter(id)) { return null; }

      try {
        var parsed = JSON.parse(json);
        return {
          code: pluginutils.dataToEsm(parsed, {
            preferConst: options.preferConst,
            compact: options.compact,
            namedExports: options.namedExports,
            indent: indent
          }),
          map: { mappings: '' }
        };
      } catch (err) {
        var message = 'Could not parse JSON file';
        var position = parseInt(/[\d]/.exec(err.message)[0], 10);
        this.warn({ message: message, id: id, position: position });
        return null;
      }
    }
  };
}

module.exports = json;
//# sourceMappingURL=index.js.map
