'use strict';

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _ModuleCache = require('eslint-module-utils/ModuleCache');

var _ModuleCache2 = _interopRequireDefault(_ModuleCache);

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * @fileOverview Ensures that an imported path exists, given resolution rules.
 * @author Ben Mosher
 */

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-unresolved')
    },

    schema: [(0, _moduleVisitor.makeOptionsSchema)({
      caseSensitive: { type: 'boolean', default: true }
    })]
  },

  create: function (context) {

    function checkSourceValue(source) {
      const shouldCheckCase = !_resolve.CASE_SENSITIVE_FS && (!context.options[0] || context.options[0].caseSensitive !== false);

      const resolvedPath = (0, _resolve2.default)(source.value, context);

      if (resolvedPath === undefined) {
        context.report(source, `Unable to resolve path to module '${source.value}'.`);
      } else if (shouldCheckCase) {
        const cacheSettings = _ModuleCache2.default.getSettings(context.settings);
        if (!(0, _resolve.fileExistsWithCaseSync)(resolvedPath, cacheSettings)) {
          context.report(source, `Casing of ${source.value} does not match the underlying filesystem.`);
        }
      }
    }

    return (0, _moduleVisitor2.default)(checkSourceValue, context.options[0]);
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXVucmVzb2x2ZWQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNhc2VTZW5zaXRpdmUiLCJkZWZhdWx0IiwiY3JlYXRlIiwiY29udGV4dCIsImNoZWNrU291cmNlVmFsdWUiLCJzb3VyY2UiLCJzaG91bGRDaGVja0Nhc2UiLCJDQVNFX1NFTlNJVElWRV9GUyIsIm9wdGlvbnMiLCJyZXNvbHZlZFBhdGgiLCJ2YWx1ZSIsInVuZGVmaW5lZCIsInJlcG9ydCIsImNhY2hlU2V0dGluZ3MiLCJNb2R1bGVDYWNoZSIsImdldFNldHRpbmdzIiwic2V0dGluZ3MiXSwibWFwcGluZ3MiOiI7O0FBS0E7Ozs7QUFDQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQVJBOzs7OztBQVVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxlQUFSO0FBREQsS0FGRjs7QUFNSkMsWUFBUSxDQUFFLHNDQUFrQjtBQUMxQkMscUJBQWUsRUFBRUosTUFBTSxTQUFSLEVBQW1CSyxTQUFTLElBQTVCO0FBRFcsS0FBbEIsQ0FBRjtBQU5KLEdBRFM7O0FBWWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjs7QUFFekIsYUFBU0MsZ0JBQVQsQ0FBMEJDLE1BQTFCLEVBQWtDO0FBQ2hDLFlBQU1DLGtCQUFrQixDQUFDQywwQkFBRCxLQUNyQixDQUFDSixRQUFRSyxPQUFSLENBQWdCLENBQWhCLENBQUQsSUFBdUJMLFFBQVFLLE9BQVIsQ0FBZ0IsQ0FBaEIsRUFBbUJSLGFBQW5CLEtBQXFDLEtBRHZDLENBQXhCOztBQUdBLFlBQU1TLGVBQWUsdUJBQVFKLE9BQU9LLEtBQWYsRUFBc0JQLE9BQXRCLENBQXJCOztBQUVBLFVBQUlNLGlCQUFpQkUsU0FBckIsRUFBZ0M7QUFDOUJSLGdCQUFRUyxNQUFSLENBQWVQLE1BQWYsRUFDRyxxQ0FBb0NBLE9BQU9LLEtBQU0sSUFEcEQ7QUFFRCxPQUhELE1BS0ssSUFBSUosZUFBSixFQUFxQjtBQUN4QixjQUFNTyxnQkFBZ0JDLHNCQUFZQyxXQUFaLENBQXdCWixRQUFRYSxRQUFoQyxDQUF0QjtBQUNBLFlBQUksQ0FBQyxxQ0FBdUJQLFlBQXZCLEVBQXFDSSxhQUFyQyxDQUFMLEVBQTBEO0FBQ3hEVixrQkFBUVMsTUFBUixDQUFlUCxNQUFmLEVBQ0csYUFBWUEsT0FBT0ssS0FBTSw0Q0FENUI7QUFFRDtBQUVGO0FBQ0Y7O0FBRUQsV0FBTyw2QkFBY04sZ0JBQWQsRUFBZ0NELFFBQVFLLE9BQVIsQ0FBZ0IsQ0FBaEIsQ0FBaEMsQ0FBUDtBQUVEO0FBckNjLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLXVucmVzb2x2ZWQuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlT3ZlcnZpZXcgRW5zdXJlcyB0aGF0IGFuIGltcG9ydGVkIHBhdGggZXhpc3RzLCBnaXZlbiByZXNvbHV0aW9uIHJ1bGVzLlxuICogQGF1dGhvciBCZW4gTW9zaGVyXG4gKi9cblxuaW1wb3J0IHJlc29sdmUsIHsgQ0FTRV9TRU5TSVRJVkVfRlMsIGZpbGVFeGlzdHNXaXRoQ2FzZVN5bmMgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnXG5pbXBvcnQgTW9kdWxlQ2FjaGUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9Nb2R1bGVDYWNoZSdcbmltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby11bnJlc29sdmVkJyksXG4gICAgfSxcblxuICAgIHNjaGVtYTogWyBtYWtlT3B0aW9uc1NjaGVtYSh7XG4gICAgICBjYXNlU2Vuc2l0aXZlOiB7IHR5cGU6ICdib29sZWFuJywgZGVmYXVsdDogdHJ1ZSB9LFxuICAgIH0pXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG5cbiAgICBmdW5jdGlvbiBjaGVja1NvdXJjZVZhbHVlKHNvdXJjZSkge1xuICAgICAgY29uc3Qgc2hvdWxkQ2hlY2tDYXNlID0gIUNBU0VfU0VOU0lUSVZFX0ZTICYmXG4gICAgICAgICghY29udGV4dC5vcHRpb25zWzBdIHx8IGNvbnRleHQub3B0aW9uc1swXS5jYXNlU2Vuc2l0aXZlICE9PSBmYWxzZSlcblxuICAgICAgY29uc3QgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShzb3VyY2UudmFsdWUsIGNvbnRleHQpXG5cbiAgICAgIGlmIChyZXNvbHZlZFBhdGggPT09IHVuZGVmaW5lZCkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydChzb3VyY2UsXG4gICAgICAgICAgYFVuYWJsZSB0byByZXNvbHZlIHBhdGggdG8gbW9kdWxlICcke3NvdXJjZS52YWx1ZX0nLmApXG4gICAgICB9XG5cbiAgICAgIGVsc2UgaWYgKHNob3VsZENoZWNrQ2FzZSkge1xuICAgICAgICBjb25zdCBjYWNoZVNldHRpbmdzID0gTW9kdWxlQ2FjaGUuZ2V0U2V0dGluZ3MoY29udGV4dC5zZXR0aW5ncylcbiAgICAgICAgaWYgKCFmaWxlRXhpc3RzV2l0aENhc2VTeW5jKHJlc29sdmVkUGF0aCwgY2FjaGVTZXR0aW5ncykpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydChzb3VyY2UsXG4gICAgICAgICAgICBgQ2FzaW5nIG9mICR7c291cmNlLnZhbHVlfSBkb2VzIG5vdCBtYXRjaCB0aGUgdW5kZXJseWluZyBmaWxlc3lzdGVtLmApXG4gICAgICAgIH1cblxuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiBtb2R1bGVWaXNpdG9yKGNoZWNrU291cmNlVmFsdWUsIGNvbnRleHQub3B0aW9uc1swXSlcblxuICB9LFxufVxuIl19