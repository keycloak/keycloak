'use strict';

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _importType = require('../core/importType');

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const enumValues = { enum: ['always', 'ignorePackages', 'never'] };
const patternProperties = {
  type: 'object',
  patternProperties: { '.*': enumValues }
};
const properties = {
  type: 'object',
  properties: {
    'pattern': patternProperties,
    'ignorePackages': { type: 'boolean' }
  }
};

function buildProperties(context) {

  const result = {
    defaultConfig: 'never',
    pattern: {},
    ignorePackages: false
  };

  context.options.forEach(obj => {

    // If this is a string, set defaultConfig to its value
    if (typeof obj === 'string') {
      result.defaultConfig = obj;
      return;
    }

    // If this is not the new structure, transfer all props to result.pattern
    if (obj.pattern === undefined && obj.ignorePackages === undefined) {
      Object.assign(result.pattern, obj);
      return;
    }

    // If pattern is provided, transfer all props
    if (obj.pattern !== undefined) {
      Object.assign(result.pattern, obj.pattern);
    }

    // If ignorePackages is provided, transfer it to result
    if (obj.ignorePackages !== undefined) {
      result.ignorePackages = obj.ignorePackages;
    }
  });

  return result;
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('extensions')
    },

    schema: {
      anyOf: [{
        type: 'array',
        items: [enumValues],
        additionalItems: false
      }, {
        type: 'array',
        items: [enumValues, properties],
        additionalItems: false
      }, {
        type: 'array',
        items: [properties],
        additionalItems: false
      }, {
        type: 'array',
        items: [patternProperties],
        additionalItems: false
      }, {
        type: 'array',
        items: [enumValues, patternProperties],
        additionalItems: false
      }]
    }
  },

  create: function (context) {

    const props = buildProperties(context);

    function getModifier(extension) {
      return props.pattern[extension] || props.defaultConfig;
    }

    function isUseOfExtensionRequired(extension, isPackageMain) {
      return getModifier(extension) === 'always' && (!props.ignorePackages || !isPackageMain);
    }

    function isUseOfExtensionForbidden(extension) {
      return getModifier(extension) === 'never';
    }

    function isResolvableWithoutExtension(file) {
      const extension = _path2.default.extname(file);
      const fileWithoutExtension = file.slice(0, -extension.length);
      const resolvedFileWithoutExtension = (0, _resolve2.default)(fileWithoutExtension, context);

      return resolvedFileWithoutExtension === (0, _resolve2.default)(file, context);
    }

    function checkFileExtension(node) {
      const source = node.source;

      // bail if the declaration doesn't have a source, e.g. "export { foo };"

      if (!source) return;

      const importPath = source.value;

      // don't enforce anything on builtins
      if ((0, _importType.isBuiltIn)(importPath, context.settings)) return;

      const resolvedPath = (0, _resolve2.default)(importPath, context);

      // get extension from resolved path, if possible.
      // for unresolved, use source value.
      const extension = _path2.default.extname(resolvedPath || importPath).substring(1);

      // determine if this is a module
      const isPackageMain = (0, _importType.isExternalModuleMain)(importPath, context.settings) || (0, _importType.isScopedMain)(importPath);

      if (!extension || !importPath.endsWith(`.${extension}`)) {
        const extensionRequired = isUseOfExtensionRequired(extension, isPackageMain);
        const extensionForbidden = isUseOfExtensionForbidden(extension);
        if (extensionRequired && !extensionForbidden) {
          context.report({
            node: source,
            message: `Missing file extension ${extension ? `"${extension}" ` : ''}for "${importPath}"`
          });
        }
      } else if (extension) {
        if (isUseOfExtensionForbidden(extension) && isResolvableWithoutExtension(importPath)) {
          context.report({
            node: source,
            message: `Unexpected use of file extension "${extension}" for "${importPath}"`
          });
        }
      }
    }

    return {
      ImportDeclaration: checkFileExtension,
      ExportNamedDeclaration: checkFileExtension
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL2V4dGVuc2lvbnMuanMiXSwibmFtZXMiOlsiZW51bVZhbHVlcyIsImVudW0iLCJwYXR0ZXJuUHJvcGVydGllcyIsInR5cGUiLCJwcm9wZXJ0aWVzIiwiYnVpbGRQcm9wZXJ0aWVzIiwiY29udGV4dCIsInJlc3VsdCIsImRlZmF1bHRDb25maWciLCJwYXR0ZXJuIiwiaWdub3JlUGFja2FnZXMiLCJvcHRpb25zIiwiZm9yRWFjaCIsIm9iaiIsInVuZGVmaW5lZCIsIk9iamVjdCIsImFzc2lnbiIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImFueU9mIiwiaXRlbXMiLCJhZGRpdGlvbmFsSXRlbXMiLCJjcmVhdGUiLCJwcm9wcyIsImdldE1vZGlmaWVyIiwiZXh0ZW5zaW9uIiwiaXNVc2VPZkV4dGVuc2lvblJlcXVpcmVkIiwiaXNQYWNrYWdlTWFpbiIsImlzVXNlT2ZFeHRlbnNpb25Gb3JiaWRkZW4iLCJpc1Jlc29sdmFibGVXaXRob3V0RXh0ZW5zaW9uIiwiZmlsZSIsInBhdGgiLCJleHRuYW1lIiwiZmlsZVdpdGhvdXRFeHRlbnNpb24iLCJzbGljZSIsImxlbmd0aCIsInJlc29sdmVkRmlsZVdpdGhvdXRFeHRlbnNpb24iLCJjaGVja0ZpbGVFeHRlbnNpb24iLCJub2RlIiwic291cmNlIiwiaW1wb3J0UGF0aCIsInZhbHVlIiwic2V0dGluZ3MiLCJyZXNvbHZlZFBhdGgiLCJzdWJzdHJpbmciLCJlbmRzV2l0aCIsImV4dGVuc2lvblJlcXVpcmVkIiwiZXh0ZW5zaW9uRm9yYmlkZGVuIiwicmVwb3J0IiwibWVzc2FnZSIsIkltcG9ydERlY2xhcmF0aW9uIiwiRXhwb3J0TmFtZWREZWNsYXJhdGlvbiJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7OztBQUVBOzs7O0FBQ0E7O0FBQ0E7Ozs7OztBQUVBLE1BQU1BLGFBQWEsRUFBRUMsTUFBTSxDQUFFLFFBQUYsRUFBWSxnQkFBWixFQUE4QixPQUE5QixDQUFSLEVB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LElBQTFCLEdBQWdDLEVBQUcsUUFBT2UsVUFBVztBQUhwRSxXQUFmO0FBS0Q7QUFDRixPQVZELE1BVU8sSUFBSWYsU0FBSixFQUFlO0FBQ3BCLFlBQUlHLDBCQUEwQkgsU0FBMUIsS0FBd0NJLDZCQUE2QlcsVUFBN0IsQ0FBNUMsRUFBc0Y7QUFDcEZ0QyxrQkFBUThDLE1BQVIsQ0FBZTtBQUNiVixrQkFBTUMsTUFETztBQUViVSxxQkFBVSxxQ0FBb0N4QixTQUFVLFVBQVNlLFVBQVc7QUFGL0QsV0FBZjtBQUlEO0FBQ0Y7QUFDRjs7QUFFRCxXQUFPO0FBQ0xVLHlCQUFtQmIsa0JBRGQ7QUFFTGMsOEJBQXdCZDtBQUZuQixLQUFQO0FBSUQ7QUFqSGMsQ0FBakIiLCJmaWxlIjoicnVsZXMvZXh0ZW5zaW9ucy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBwYXRoIGZyb20gJ3BhdGgnXG5cbmltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSdcbmltcG9ydCB7IGlzQnVpbHRJbiwgaXNFeHRlcm5hbE1vZHVsZU1haW4sIGlzU2NvcGVkTWFpbiB9IGZyb20gJy4uL2NvcmUvaW1wb3J0VHlwZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmNvbnN0IGVudW1WYWx1ZXMgPSB7IGVudW06IFsgJ2Fsd2F5cycsICdpZ25vcmVQYWNrYWdlcycsICduZXZlcicgXSB9XG5jb25zdCBwYXR0ZXJuUHJvcGVydGllcyA9IHtcbiAgdHlwZTogJ29iamVjdCcsXG4gIHBhdHRlcm5Qcm9wZXJ0aWVzOiB7ICcuKic6IGVudW1WYWx1ZXMgfSxcbn1cbmNvbnN0IHByb3BlcnRpZXMgPSB7XG4gIHR5cGU6ICdvYmplY3QnLFxuICBwcm9wZXJ0aWVzOiB7XG4gICAgJ3BhdHRlcm4nOiBwYXR0ZXJuUHJvcGVydGllcyxcbiAgICAnaWdub3JlUGFja2FnZXMnOiB7IHR5cGU6ICdib29sZWFuJyB9LFxuICB9LFxufVxuXG5mdW5jdGlvbiBidWlsZFByb3BlcnRpZXMoY29udGV4dCkge1xuXG4gICAgY29uc3QgcmVzdWx0ID0ge1xuICAgICAgZGVmYXVsdENvbmZpZzogJ25ldmVyJyxcbiAgICAgIHBhdHRlcm46IHt9LFxuICAgICAgaWdub3JlUGFja2FnZXM6IGZhbHNlLFxuICAgIH1cblxuICAgIGNvbnRleHQub3B0aW9ucy5mb3JFYWNoKG9iaiA9PiB7XG5cbiAgICAgIC8vIElmIHRoaXMgaXMgYSBzdHJpbmcsIHNldCBkZWZhdWx0Q29uZmlnIHRvIGl0cyB2YWx1ZVxuICAgICAgaWYgKHR5cGVvZiBvYmogPT09ICdzdHJpbmcnKSB7XG4gICAgICAgIHJlc3VsdC5kZWZhdWx0Q29uZmlnID0gb2JqXG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBJZiB0aGlzIGlzIG5vdCB0aGUgbmV3IHN0cnVjdHVyZSwgdHJhbnNmZXIgYWxsIHByb3BzIHRvIHJlc3VsdC5wYXR0ZXJuXG4gICAgICBpZiAob2JqLnBhdHRlcm4gPT09IHVuZGVmaW5lZCAmJiBvYmouaWdub3JlUGFja2FnZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgICBPYmplY3QuYXNzaWduKHJlc3VsdC5wYXR0ZXJuLCBvYmopXG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICAvLyBJZiBwYXR0ZXJuIGlzIHByb3ZpZGVkLCB0cmFuc2ZlciBhbGwgcHJvcHNcbiAgICAgIGlmIChvYmoucGF0dGVybiAhPT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIE9iamVjdC5hc3NpZ24ocmVzdWx0LnBhdHRlcm4sIG9iai5wYXR0ZXJuKVxuICAgICAgfVxuXG4gICAgICAvLyBJZiBpZ25vcmVQYWNrYWdlcyBpcyBwcm92aWRlZCwgdHJhbnNmZXIgaXQgdG8gcmVzdWx0XG4gICAgICBpZiAob2JqLmlnbm9yZVBhY2thZ2VzICE9PSB1bmRlZmluZWQpIHtcbiAgICAgICAgcmVzdWx0Lmlnbm9yZVBhY2thZ2VzID0gb2JqLmlnbm9yZVBhY2thZ2VzXG4gICAgICB9XG4gICAgfSlcblxuICAgIHJldHVybiByZXN1bHRcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdleHRlbnNpb25zJyksXG4gICAgfSxcblxuICAgIHNjaGVtYToge1xuICAgICAgYW55T2Y6IFtcbiAgICAgICAge1xuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgaXRlbXM6IFtlbnVtVmFsdWVzXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW1xuICAgICAgICAgICAgZW51bVZhbHVlcyxcbiAgICAgICAgICAgIHByb3BlcnRpZXMsXG4gICAgICAgICAgXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW3Byb3BlcnRpZXNdLFxuICAgICAgICAgIGFkZGl0aW9uYWxJdGVtczogZmFsc2UsXG4gICAgICAgIH0sXG4gICAgICAgIHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIGl0ZW1zOiBbcGF0dGVyblByb3BlcnRpZXNdLFxuICAgICAgICAgIGFkZGl0aW9uYWxJdGVtczogZmFsc2UsXG4gICAgICAgIH0sXG4gICAgICAgIHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIGl0ZW1zOiBbXG4gICAgICAgICAgICBlbnVtVmFsdWVzLFxuICAgICAgICAgICAgcGF0dGVyblByb3BlcnRpZXMsXG4gICAgICAgICAgXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgXSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcblxuICAgIGNvbnN0IHByb3BzID0gYnVpbGRQcm9wZXJ0aWVzKGNvbnRleHQpXG5cbiAgICBmdW5jdGlvbiBnZXRNb2RpZmllcihleHRlbnNpb24pIHtcbiAgICAgIHJldHVybiBwcm9wcy5wYXR0ZXJuW2V4dGVuc2lvbl0gfHwgcHJvcHMuZGVmYXVsdENvbmZpZ1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzVXNlT2ZFeHRlbnNpb25SZXF1aXJlZChleHRlbnNpb24sIGlzUGFja2FnZU1haW4pIHtcbiAgICAgIHJldHVybiBnZXRNb2RpZmllcihleHRlbnNpb24pID09PSAnYWx3YXlzJyAmJiAoIXByb3BzLmlnbm9yZVBhY2thZ2VzIHx8ICFpc1BhY2thZ2VNYWluKVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzVXNlT2ZFeHRlbnNpb25Gb3JiaWRkZW4oZXh0ZW5zaW9uKSB7XG4gICAgICByZXR1cm4gZ2V0TW9kaWZpZXIoZXh0ZW5zaW9uKSA9PT0gJ25ldmVyJ1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzUmVzb2x2YWJsZVdpdGhvdXRFeHRlbnNpb24oZmlsZSkge1xuICAgICAgY29uc3QgZXh0ZW5zaW9uID0gcGF0aC5leHRuYW1lKGZpbGUpXG4gICAgICBjb25zdCBmaWxlV2l0aG91dEV4dGVuc2lvbiA9IGZpbGUuc2xpY2UoMCwgLWV4dGVuc2lvbi5sZW5ndGgpXG4gICAgICBjb25zdCByZXNvbHZlZEZpbGVXaXRob3V0RXh0ZW5zaW9uID0gcmVzb2x2ZShmaWxlV2l0aG91dEV4dGVuc2lvbiwgY29udGV4dClcblxuICAgICAgcmV0dXJuIHJlc29sdmVkRmlsZVdpdGhvdXRFeHRlbnNpb24gPT09IHJlc29sdmUoZmlsZSwgY29udGV4dClcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBjaGVja0ZpbGVFeHRlbnNpb24obm9kZSkge1xuICAgICAgY29uc3QgeyBzb3VyY2UgfSA9IG5vZGVcblxuICAgICAgLy8gYmFpbCBpZiB0aGUgZGVjbGFyYXRpb24gZG9lc24ndCBoYXZlIGEgc291cmNlLCBlLmcuIFwiZXhwb3J0IHsgZm9vIH07XCJcbiAgICAgIGlmICghc291cmNlKSByZXR1cm5cblxuICAgICAgY29uc3QgaW1wb3J0UGF0aCA9IHNvdXJjZS52YWx1ZVxuXG4gICAgICAvLyBkb24ndCBlbmZvcmNlIGFueXRoaW5nIG9uIGJ1aWx0aW5zXG4gICAgICBpZiAoaXNCdWlsdEluKGltcG9ydFBhdGgsIGNvbnRleHQuc2V0dGluZ3MpKSByZXR1cm5cblxuICAgICAgY29uc3QgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShpbXBvcnRQYXRoLCBjb250ZXh0KVxuXG4gICAgICAvLyBnZXQgZXh0ZW5zaW9uIGZyb20gcmVzb2x2ZWQgcGF0aCwgaWYgcG9zc2libGUuXG4gICAgICAvLyBmb3IgdW5yZXNvbHZlZCwgdXNlIHNvdXJjZSB2YWx1ZS5cbiAgICAgIGNvbnN0IGV4dGVuc2lvbiA9IHBhdGguZXh0bmFtZShyZXNvbHZlZFBhdGggfHwgaW1wb3J0UGF0aCkuc3Vic3RyaW5nKDEpXG5cbiAgICAgIC8vIGRldGVybWluZSBpZiB0aGlzIGlzIGEgbW9kdWxlXG4gICAgICBjb25zdCBpc1BhY2thZ2VNYWluID0gaXNFeHRlcm5hbE1vZHVsZU1haW4oaW1wb3J0UGF0aCwgY29udGV4dC5zZXR0aW5ncylcbiAgICAgICAgfHwgaXNTY29wZWRNYWluKGltcG9ydFBhdGgpXG5cbiAgICAgIGlmICghZXh0ZW5zaW9uIHx8ICFpbXBvcnRQYXRoLmVuZHNXaXRoKGAuJHtleHRlbnNpb259YCkpIHtcbiAgICAgICAgY29uc3QgZXh0ZW5zaW9uUmVxdWlyZWQgPSBpc1VzZU9mRXh0ZW5zaW9uUmVxdWlyZWQoZXh0ZW5zaW9uLCBpc1BhY2thZ2VNYWluKVxuICAgICAgICBjb25zdCBleHRlbnNpb25Gb3JiaWRkZW4gPSBpc1VzZU9mRXh0ZW5zaW9uRm9yYmlkZGVuKGV4dGVuc2lvbilcbiAgICAgICAgaWYgKGV4dGVuc2lvblJlcXVpcmVkICYmICFleHRlbnNpb25Gb3JiaWRkZW4pIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlOiBzb3VyY2UsXG4gICAgICAgICAgICBtZXNzYWdlOlxuICAgICAgICAgICAgICBgTWlzc2luZyBmaWxlIGV4dGVuc2lvbiAke2V4dGVuc2lvbiA/IGBcIiR7ZXh0ZW5zaW9ufVwiIGAgOiAnJ31mb3IgXCIke2ltcG9ydFBhdGh9XCJgLFxuICAgICAgICAgIH0pXG4gICAgICAgIH1cbiAgICAgIH0gZWxzZSBpZiAoZXh0ZW5zaW9uKSB7XG4gICAgICAgIGlmIChpc1VzZU9mRXh0ZW5zaW9uRm9yYmlkZGVuKGV4dGVuc2lvbikgJiYgaXNSZXNvbHZhYmxlV2l0aG91dEV4dGVuc2lvbihpbXBvcnRQYXRoKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGU6IHNvdXJjZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGBVbmV4cGVjdGVkIHVzZSBvZiBmaWxlIGV4dGVuc2lvbiBcIiR7ZXh0ZW5zaW9ufVwiIGZvciBcIiR7aW1wb3J0UGF0aH1cImAsXG4gICAgICAgICAgfSlcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBJbXBvcnREZWNsYXJhdGlvbjogY2hlY2tGaWxlRXh0ZW5zaW9uLFxuICAgICAgRXhwb3J0TmFtZWREZWNsYXJhdGlvbjogY2hlY2tGaWxlRXh0ZW5zaW9uLFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==