import { createFilter, dataToEsm } from '@rollup/pluginutils';

function json(options) {
  if ( options === void 0 ) options = {};

  var filter = createFilter(options.include, options.exclude);
  var indent = 'indent' in options ? options.indent : '\t';

  return {
    name: 'json',

    // eslint-disable-next-line no-shadow
    transform: function transform(json, id) {
      if (id.slice(-5) !== '.json' || !filter(id)) { return null; }

      return {
        code: dataToEsm(JSON.parse(json), {
          preferConst: options.preferConst,
          compact: options.compact,
          namedExports: options.namedExports,
          indent: indent
        }),
        map: { mappings: '' }
      };
    }
  };
}

export default json;
//# sourceMappingURL=index.es.js.map
