'use strict';var _minimatch = require('minimatch');var _minimatch2 = _interopRequireDefault(_minimatch);

var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _importType = require('../core/importType');var _importType2 = _interopRequireDefault(_importType);
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-internal-modules') },


    schema: [
    {
      oneOf: [
      {
        type: 'object',
        properties: {
          allow: {
            type: 'array',
            items: {
              type: 'string' } } },



        additionalProperties: false },

      {
        type: 'object',
        properties: {
          forbid: {
            type: 'array',
            items: {
              type: 'string' } } },



        additionalProperties: false }] }] },






  create: function () {function noReachingInside(context) {
      var options = context.options[0] || {};
      var allowRegexps = (options.allow || []).map(function (p) {return _minimatch2['default'].makeRe(p);});
      var forbidRegexps = (options.forbid || []).map(function (p) {return _minimatch2['default'].makeRe(p);});

      // minimatch patterns are expected to use / path separators, like import
      // statements, so normalize paths to use the same
      function normalizeSep(somePath) {
        return somePath.split('\\').join('/');
      }

      function toSteps(somePath) {
        return normalizeSep(somePath).
        split('/').
        reduce(function (acc, step) {
          if (!step || step === '.') {
            return acc;
          } else if (step === '..') {
            return acc.slice(0, -1);
          } else {
            return acc.concat(step);
          }
        }, []);
      }

      // test if reaching to this destination is allowed
      function reachingAllowed(importPath) {
        return allowRegexps.some(function (re) {return re.test(importPath);});
      }

      // test if reaching to this destination is forbidden
      function reachingForbidden(importPath) {
        return forbidRegexps.some(function (re) {return re.test(importPath);});
      }

      function isAllowViolation(importPath) {
        var steps = toSteps(importPath);

        var nonScopeSteps = steps.filter(function (step) {return step.indexOf('@') !== 0;});
        if (nonScopeSteps.length <= 1) return false;

        // before trying to resolve, see if the raw import (with relative
        // segments resolved) matches an allowed pattern
        var justSteps = steps.join('/');
        if (reachingAllowed(justSteps) || reachingAllowed('/' + String(justSteps))) return false;

        // if the import statement doesn't match directly, try to match the
        // resolved path if the import is resolvable
        var resolved = (0, _resolve2['default'])(importPath, context);
        if (!resolved || reachingAllowed(normalizeSep(resolved))) return false;

        // this import was not allowed by the allowed paths, and reaches
        // so it is a violation
        return true;
      }

      function isForbidViolation(importPath) {
        var steps = toSteps(importPath);

        // before trying to resolve, see if the raw import (with relative
        // segments resolved) matches a forbidden pattern
        var justSteps = steps.join('/');

        if (reachingForbidden(justSteps) || reachingForbidden('/' + String(justSteps))) return true;

        // if the import statement doesn't match directly, try to match the
        // resolved path if the import is resolvable
        var resolved = (0, _resolve2['default'])(importPath, context);
        if (resolved && reachingForbidden(normalizeSep(resolved))) return true;

        // this import was not forbidden by the forbidden paths so it is not a violation
        return false;
      }

      // find a directory that is being reached into, but which shouldn't be
      var isReachViolation = options.forbid ? isForbidViolation : isAllowViolation;

      function checkImportForReaching(importPath, node) {
        var potentialViolationTypes = ['parent', 'index', 'sibling', 'external', 'internal'];
        if (potentialViolationTypes.indexOf((0, _importType2['default'])(importPath, context)) !== -1 &&
        isReachViolation(importPath))
        {
          context.report({
            node: node,
            message: 'Reaching to "' + String(importPath) + '" is not allowed.' });

        }
      }

      return (0, _moduleVisitor2['default'])(function (source) {
        checkImportForReaching(source.value, source);
      }, { commonjs: true });
    }return noReachingInside;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1pbnRlcm5hbC1tb2R1bGVzLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJvbmVPZiIsInByb3BlcnRpZXMiLCJhbGxvdyIsIml0ZW1zIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJmb3JiaWQiLCJjcmVhdGUiLCJub1JlYWNoaW5nSW5zaWRlIiwiY29udGV4dCIsIm9wdGlvbnMiLCJhbGxvd1JlZ2V4cHMiLCJtYXAiLCJtaW5pbWF0Y2giLCJtYWtlUmUiLCJwIiwiZm9yYmlkUmVnZXhwcyIsIm5vcm1hbGl6ZVNlcCIsInNvbWVQYXRoIiwic3BsaXQiLCJqb2luIiwidG9TdGVwcyIsInJlZHVjZSIsImFjYyIsInN0ZXAiLCJzbGljZSIsImNvbmNhdCIsInJlYWNoaW5nQWxsb3dlZCIsImltcG9ydFBhdGgiLCJzb21lIiwicmUiLCJ0ZXN0IiwicmVhY2hpbmdGb3JiaWRkZW4iLCJpc0FsbG93VmlvbGF0aW9uIiwic3RlcHMiLCJub25TY29wZVN0ZXBzIiwiZmlsdGVyIiwiaW5kZXhPZiIsImxlbmd0aCIsImp1c3RTdGVwcyIsInJlc29sdmVkIiwiaXNGb3JiaWRWaW9sYXRpb24iLCJpc1JlYWNoVmlvbGF0aW9uIiwiY2hlY2tJbXBvcnRGb3JSZWFjaGluZyIsIm5vZGUiLCJwb3RlbnRpYWxWaW9sYXRpb25UeXBlcyIsInJlcG9ydCIsIm1lc3NhZ2UiLCJzb3VyY2UiLCJ2YWx1ZSIsImNvbW1vbmpzIl0sIm1hcHBpbmdzIjoiYUFBQSxzQzs7QUFFQSxzRDtBQUNBLGdEO0FBQ0Esa0U7QUFDQSxxQzs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEscUJBQVIsQ0FERCxFQUZGOzs7QUFNSkMsWUFBUTtBQUNOO0FBQ0VDLGFBQU87QUFDTDtBQUNFSixjQUFNLFFBRFI7QUFFRUssb0JBQVk7QUFDVkMsaUJBQU87QUFDTE4sa0JBQU0sT0FERDtBQUVMTyxtQkFBTztBQUNMUCxvQkFBTSxRQURELEVBRkYsRUFERyxFQUZkOzs7O0FBVUVRLDhCQUFzQixLQVZ4QixFQURLOztBQWFMO0FBQ0VSLGNBQU0sUUFEUjtBQUVFSyxvQkFBWTtBQUNWSSxrQkFBUTtBQUNOVCxrQkFBTSxPQURBO0FBRU5PLG1CQUFPO0FBQ0xQLG9CQUFNLFFBREQsRUFGRCxFQURFLEVBRmQ7Ozs7QUFVRVEsOEJBQXNCLEtBVnhCLEVBYkssQ0FEVCxFQURNLENBTkosRUFEUzs7Ozs7OztBQXVDZkUsdUJBQVEsU0FBU0MsZ0JBQVQsQ0FBMEJDLE9BQTFCLEVBQW1DO0FBQ3pDLFVBQU1DLFVBQVVELFFBQVFDLE9BQVIsQ0FBZ0IsQ0FBaEIsS0FBc0IsRUFBdEM7QUFDQSxVQUFNQyxlQUFlLENBQUNELFFBQVFQLEtBQVIsSUFBaUIsRUFBbEIsRUFBc0JTLEdBQXRCLENBQTBCLHFCQUFLQyx1QkFBVUMsTUFBVixDQUFpQkMsQ0FBakIsQ0FBTCxFQUExQixDQUFyQjtBQUNBLFVBQU1DLGdCQUFnQixDQUFDTixRQUFRSixNQUFSLElBQWtCLEVBQW5CLEVBQXVCTSxHQUF2QixDQUEyQixxQkFBS0MsdUJBQVVDLE1BQVYsQ0FBaUJDLENBQWpCLENBQUwsRUFBM0IsQ0FBdEI7O0FBRUE7QUFDQTtBQUNBLGVBQVNFLFlBQVQsQ0FBc0JDLFFBQXRCLEVBQWdDO0FBQzlCLGVBQU9BLFNBQVNDLEtBQVQsQ0FBZSxJQUFmLEVBQXFCQyxJQUFyQixDQUEwQixHQUExQixDQUFQO0FBQ0Q7O0FBRUQsZUFBU0MsT0FBVCxDQUFpQkgsUUFBakIsRUFBMkI7QUFDekIsZUFBUUQsYUFBYUMsUUFBYjtBQUNMQyxhQURLLENBQ0MsR0FERDtBQUVMRyxjQUZLLENBRUUsVUFBQ0MsR0FBRCxFQUFNQyxJQUFOLEVBQWU7QUFDckIsY0FBSSxDQUFDQSxJQUFELElBQVNBLFNBQVMsR0FBdEIsRUFBMkI7QUFDekIsbUJBQU9ELEdBQVA7QUFDRCxXQUZELE1BRU8sSUFBSUMsU0FBUyxJQUFiLEVBQW1CO0FBQ3hCLG1CQUFPRCxJQUFJRSxLQUFKLENBQVUsQ0FBVixFQUFhLENBQUMsQ0FBZCxDQUFQO0FBQ0QsV0FGTSxNQUVBO0FBQ0wsbUJBQU9GLElBQUlHLE1BQUosQ0FBV0YsSUFBWCxDQUFQO0FBQ0Q7QUFDRixTQVZLLEVBVUgsRUFWRyxDQUFSO0FBV0Q7O0FBRUQ7QUFDQSxlQUFTRyxlQUFULENBQXlCQyxVQUF6QixFQUFxQztBQUNuQyxlQUFPakIsYUFBYWtCLElBQWIsQ0FBa0Isc0JBQU1DLEdBQUdDLElBQUgsQ0FBUUgsVUFBUixDQUFOLEVBQWxCLENBQVA7QUFDRDs7QUFFRDtBQUNBLGVBQVNJLGlCQUFULENBQTJCSixVQUEzQixFQUF1QztBQUNyQyxlQUFPWixjQUFjYSxJQUFkLENBQW1CLHNCQUFNQyxHQUFHQyxJQUFILENBQVFILFVBQVIsQ0FBTixFQUFuQixDQUFQO0FBQ0Q7O0FBRUQsZUFBU0ssZ0JBQVQsQ0FBMEJMLFVBQTFCLEVBQXNDO0FBQ3BDLFlBQU1NLFFBQVFiLFFBQVFPLFVBQVIsQ0FBZDs7QUFFQSxZQUFNTyxnQkFBZ0JELE1BQU1FLE1BQU4sQ0FBYSx3QkFBUVosS0FBS2EsT0FBTCxDQUFhLEdBQWIsTUFBc0IsQ0FBOUIsRUFBYixDQUF0QjtBQUNBLFlBQUlGLGNBQWNHLE1BQWQsSUFBd0IsQ0FBNUIsRUFBK0IsT0FBTyxLQUFQOztBQUUvQjtBQUNBO0FBQ0EsWUFBTUMsWUFBWUwsTUFBTWQsSUFBTixDQUFXLEdBQVgsQ0FBbEI7QUFDQSxZQUFJTyxnQkFBZ0JZLFNBQWhCLEtBQThCWiw2QkFBb0JZLFNBQXBCLEVBQWxDLEVBQW9FLE9BQU8sS0FBUDs7QUFFcEU7QUFDQTtBQUNBLFlBQU1DLFdBQVcsMEJBQVFaLFVBQVIsRUFBb0JuQixPQUFwQixDQUFqQjtBQUNBLFlBQUksQ0FBQytCLFFBQUQsSUFBYWIsZ0JBQWdCVixhQUFhdUIsUUFBYixDQUFoQixDQUFqQixFQUEwRCxPQUFPLEtBQVA7O0FBRTFEO0FBQ0E7QUFDQSxlQUFPLElBQVA7QUFDRDs7QUFFRCxlQUFTQyxpQkFBVCxDQUEyQmIsVUFBM0IsRUFBdUM7QUFDckMsWUFBTU0sUUFBUWIsUUFBUU8sVUFBUixDQUFkOztBQUVBO0FBQ0E7QUFDQSxZQUFNVyxZQUFZTCxNQUFNZCxJQUFOLENBQVcsR0FBWCxDQUFsQjs7QUFFQSxZQUFJWSxrQkFBa0JPLFNBQWxCLEtBQWdDUCwrQkFBc0JPLFNBQXRCLEVBQXBDLEVBQXdFLE9BQU8sSUFBUDs7QUFFeEU7QUFDQTtBQUNBLFlBQU1DLFdBQVcsMEJBQVFaLFVBQVIsRUFBb0JuQixPQUFwQixDQUFqQjtBQUNBLFlBQUkrQixZQUFZUixrQkFBa0JmLGFBQWF1QixRQUFiLENBQWxCLENBQWhCLEVBQTJELE9BQU8sSUFBUDs7QUFFM0Q7QUFDQSxlQUFPLEtBQVA7QUFDRDs7QUFFRDtBQUNBLFVBQU1FLG1CQUFtQmhDLFFBQVFKLE1BQVIsR0FBaUJtQyxpQkFBakIsR0FBcUNSLGdCQUE5RDs7QUFFQSxlQUFTVSxzQkFBVCxDQUFnQ2YsVUFBaEMsRUFBNENnQixJQUE1QyxFQUFrRDtBQUNoRCxZQUFNQywwQkFBMEIsQ0FBQyxRQUFELEVBQVcsT0FBWCxFQUFvQixTQUFwQixFQUErQixVQUEvQixFQUEyQyxVQUEzQyxDQUFoQztBQUNBLFlBQUlBLHdCQUF3QlIsT0FBeEIsQ0FBZ0MsNkJBQVdULFVBQVgsRUFBdUJuQixPQUF2QixDQUFoQyxNQUFxRSxDQUFDLENBQXRFO0FBQ0ZpQyx5QkFBaUJkLFVBQWpCLENBREY7QUFFRTtBQUNBbkIsa0JBQVFxQyxNQUFSLENBQWU7QUFDYkYsc0JBRGE7QUFFYkcsOENBQXlCbkIsVUFBekIsdUJBRmEsRUFBZjs7QUFJRDtBQUNGOztBQUVELGFBQU8sZ0NBQWMsVUFBQ29CLE1BQUQsRUFBWTtBQUMvQkwsK0JBQXVCSyxPQUFPQyxLQUE5QixFQUFxQ0QsTUFBckM7QUFDRCxPQUZNLEVBRUosRUFBRUUsVUFBVSxJQUFaLEVBRkksQ0FBUDtBQUdELEtBNUZELE9BQWlCMUMsZ0JBQWpCLElBdkNlLEVBQWpCIiwiZmlsZSI6Im5vLWludGVybmFsLW1vZHVsZXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgbWluaW1hdGNoIGZyb20gJ21pbmltYXRjaCc7XG5cbmltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSc7XG5pbXBvcnQgaW1wb3J0VHlwZSBmcm9tICcuLi9jb3JlL2ltcG9ydFR5cGUnO1xuaW1wb3J0IG1vZHVsZVZpc2l0b3IgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWludGVybmFsLW1vZHVsZXMnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIG9uZU9mOiBbXG4gICAgICAgICAge1xuICAgICAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgICAgIGFsbG93OiB7XG4gICAgICAgICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICAgICAgICBpdGVtczoge1xuICAgICAgICAgICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICAgICAgfSxcbiAgICAgICAgICB7XG4gICAgICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICAgICAgZm9yYmlkOiB7XG4gICAgICAgICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICAgICAgICBpdGVtczoge1xuICAgICAgICAgICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICAgICAgfSxcbiAgICAgICAgXSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIG5vUmVhY2hpbmdJbnNpZGUoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge307XG4gICAgY29uc3QgYWxsb3dSZWdleHBzID0gKG9wdGlvbnMuYWxsb3cgfHwgW10pLm1hcChwID0+IG1pbmltYXRjaC5tYWtlUmUocCkpO1xuICAgIGNvbnN0IGZvcmJpZFJlZ2V4cHMgPSAob3B0aW9ucy5mb3JiaWQgfHwgW10pLm1hcChwID0+IG1pbmltYXRjaC5tYWtlUmUocCkpO1xuXG4gICAgLy8gbWluaW1hdGNoIHBhdHRlcm5zIGFyZSBleHBlY3RlZCB0byB1c2UgLyBwYXRoIHNlcGFyYXRvcnMsIGxpa2UgaW1wb3J0XG4gICAgLy8gc3RhdGVtZW50cywgc28gbm9ybWFsaXplIHBhdGhzIHRvIHVzZSB0aGUgc2FtZVxuICAgIGZ1bmN0aW9uIG5vcm1hbGl6ZVNlcChzb21lUGF0aCkge1xuICAgICAgcmV0dXJuIHNvbWVQYXRoLnNwbGl0KCdcXFxcJykuam9pbignLycpO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIHRvU3RlcHMoc29tZVBhdGgpIHtcbiAgICAgIHJldHVybiAgbm9ybWFsaXplU2VwKHNvbWVQYXRoKVxuICAgICAgICAuc3BsaXQoJy8nKVxuICAgICAgICAucmVkdWNlKChhY2MsIHN0ZXApID0+IHtcbiAgICAgICAgICBpZiAoIXN0ZXAgfHwgc3RlcCA9PT0gJy4nKSB7XG4gICAgICAgICAgICByZXR1cm4gYWNjO1xuICAgICAgICAgIH0gZWxzZSBpZiAoc3RlcCA9PT0gJy4uJykge1xuICAgICAgICAgICAgcmV0dXJuIGFjYy5zbGljZSgwLCAtMSk7XG4gICAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICAgIHJldHVybiBhY2MuY29uY2F0KHN0ZXApO1xuICAgICAgICAgIH1cbiAgICAgICAgfSwgW10pO1xuICAgIH1cblxuICAgIC8vIHRlc3QgaWYgcmVhY2hpbmcgdG8gdGhpcyBkZXN0aW5hdGlvbiBpcyBhbGxvd2VkXG4gICAgZnVuY3Rpb24gcmVhY2hpbmdBbGxvd2VkKGltcG9ydFBhdGgpIHtcbiAgICAgIHJldHVybiBhbGxvd1JlZ2V4cHMuc29tZShyZSA9PiByZS50ZXN0KGltcG9ydFBhdGgpKTtcbiAgICB9XG5cbiAgICAvLyB0ZXN0IGlmIHJlYWNoaW5nIHRvIHRoaXMgZGVzdGluYXRpb24gaXMgZm9yYmlkZGVuXG4gICAgZnVuY3Rpb24gcmVhY2hpbmdGb3JiaWRkZW4oaW1wb3J0UGF0aCkge1xuICAgICAgcmV0dXJuIGZvcmJpZFJlZ2V4cHMuc29tZShyZSA9PiByZS50ZXN0KGltcG9ydFBhdGgpKTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBpc0FsbG93VmlvbGF0aW9uKGltcG9ydFBhdGgpIHtcbiAgICAgIGNvbnN0IHN0ZXBzID0gdG9TdGVwcyhpbXBvcnRQYXRoKTtcblxuICAgICAgY29uc3Qgbm9uU2NvcGVTdGVwcyA9IHN0ZXBzLmZpbHRlcihzdGVwID0+IHN0ZXAuaW5kZXhPZignQCcpICE9PSAwKTtcbiAgICAgIGlmIChub25TY29wZVN0ZXBzLmxlbmd0aCA8PSAxKSByZXR1cm4gZmFsc2U7XG5cbiAgICAgIC8vIGJlZm9yZSB0cnlpbmcgdG8gcmVzb2x2ZSwgc2VlIGlmIHRoZSByYXcgaW1wb3J0ICh3aXRoIHJlbGF0aXZlXG4gICAgICAvLyBzZWdtZW50cyByZXNvbHZlZCkgbWF0Y2hlcyBhbiBhbGxvd2VkIHBhdHRlcm5cbiAgICAgIGNvbnN0IGp1c3RTdGVwcyA9IHN0ZXBzLmpvaW4oJy8nKTtcbiAgICAgIGlmIChyZWFjaGluZ0FsbG93ZWQoanVzdFN0ZXBzKSB8fCByZWFjaGluZ0FsbG93ZWQoYC8ke2p1c3RTdGVwc31gKSkgcmV0dXJuIGZhbHNlO1xuXG4gICAgICAvLyBpZiB0aGUgaW1wb3J0IHN0YXRlbWVudCBkb2Vzbid0IG1hdGNoIGRpcmVjdGx5LCB0cnkgdG8gbWF0Y2ggdGhlXG4gICAgICAvLyByZXNvbHZlZCBwYXRoIGlmIHRoZSBpbXBvcnQgaXMgcmVzb2x2YWJsZVxuICAgICAgY29uc3QgcmVzb2x2ZWQgPSByZXNvbHZlKGltcG9ydFBhdGgsIGNvbnRleHQpO1xuICAgICAgaWYgKCFyZXNvbHZlZCB8fCByZWFjaGluZ0FsbG93ZWQobm9ybWFsaXplU2VwKHJlc29sdmVkKSkpIHJldHVybiBmYWxzZTtcblxuICAgICAgLy8gdGhpcyBpbXBvcnQgd2FzIG5vdCBhbGxvd2VkIGJ5IHRoZSBhbGxvd2VkIHBhdGhzLCBhbmQgcmVhY2hlc1xuICAgICAgLy8gc28gaXQgaXMgYSB2aW9sYXRpb25cbiAgICAgIHJldHVybiB0cnVlO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzRm9yYmlkVmlvbGF0aW9uKGltcG9ydFBhdGgpIHtcbiAgICAgIGNvbnN0IHN0ZXBzID0gdG9TdGVwcyhpbXBvcnRQYXRoKTtcblxuICAgICAgLy8gYmVmb3JlIHRyeWluZyB0byByZXNvbHZlLCBzZWUgaWYgdGhlIHJhdyBpbXBvcnQgKHdpdGggcmVsYXRpdmVcbiAgICAgIC8vIHNlZ21lbnRzIHJlc29sdmVkKSBtYXRjaGVzIGEgZm9yYmlkZGVuIHBhdHRlcm5cbiAgICAgIGNvbnN0IGp1c3RTdGVwcyA9IHN0ZXBzLmpvaW4oJy8nKTtcblxuICAgICAgaWYgKHJlYWNoaW5nRm9yYmlkZGVuKGp1c3RTdGVwcykgfHwgcmVhY2hpbmdGb3JiaWRkZW4oYC8ke2p1c3RTdGVwc31gKSkgcmV0dXJuIHRydWU7XG5cbiAgICAgIC8vIGlmIHRoZSBpbXBvcnQgc3RhdGVtZW50IGRvZXNuJ3QgbWF0Y2ggZGlyZWN0bHksIHRyeSB0byBtYXRjaCB0aGVcbiAgICAgIC8vIHJlc29sdmVkIHBhdGggaWYgdGhlIGltcG9ydCBpcyByZXNvbHZhYmxlXG4gICAgICBjb25zdCByZXNvbHZlZCA9IHJlc29sdmUoaW1wb3J0UGF0aCwgY29udGV4dCk7XG4gICAgICBpZiAocmVzb2x2ZWQgJiYgcmVhY2hpbmdGb3JiaWRkZW4obm9ybWFsaXplU2VwKHJlc29sdmVkKSkpIHJldHVybiB0cnVlO1xuXG4gICAgICAvLyB0aGlzIGltcG9ydCB3YXMgbm90IGZvcmJpZGRlbiBieSB0aGUgZm9yYmlkZGVuIHBhdGhzIHNvIGl0IGlzIG5vdCBhIHZpb2xhdGlvblxuICAgICAgcmV0dXJuIGZhbHNlO1xuICAgIH1cblxuICAgIC8vIGZpbmQgYSBkaXJlY3RvcnkgdGhhdCBpcyBiZWluZyByZWFjaGVkIGludG8sIGJ1dCB3aGljaCBzaG91bGRuJ3QgYmVcbiAgICBjb25zdCBpc1JlYWNoVmlvbGF0aW9uID0gb3B0aW9ucy5mb3JiaWQgPyBpc0ZvcmJpZFZpb2xhdGlvbiA6IGlzQWxsb3dWaW9sYXRpb247XG5cbiAgICBmdW5jdGlvbiBjaGVja0ltcG9ydEZvclJlYWNoaW5nKGltcG9ydFBhdGgsIG5vZGUpIHtcbiAgICAgIGNvbnN0IHBvdGVudGlhbFZpb2xhdGlvblR5cGVzID0gWydwYXJlbnQnLCAnaW5kZXgnLCAnc2libGluZycsICdleHRlcm5hbCcsICdpbnRlcm5hbCddO1xuICAgICAgaWYgKHBvdGVudGlhbFZpb2xhdGlvblR5cGVzLmluZGV4T2YoaW1wb3J0VHlwZShpbXBvcnRQYXRoLCBjb250ZXh0KSkgIT09IC0xICYmXG4gICAgICAgIGlzUmVhY2hWaW9sYXRpb24oaW1wb3J0UGF0aClcbiAgICAgICkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZSxcbiAgICAgICAgICBtZXNzYWdlOiBgUmVhY2hpbmcgdG8gXCIke2ltcG9ydFBhdGh9XCIgaXMgbm90IGFsbG93ZWQuYCxcbiAgICAgICAgfSk7XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIG1vZHVsZVZpc2l0b3IoKHNvdXJjZSkgPT4ge1xuICAgICAgY2hlY2tJbXBvcnRGb3JSZWFjaGluZyhzb3VyY2UudmFsdWUsIHNvdXJjZSk7XG4gICAgfSwgeyBjb21tb25qczogdHJ1ZSB9KTtcbiAgfSxcbn07XG4iXX0=