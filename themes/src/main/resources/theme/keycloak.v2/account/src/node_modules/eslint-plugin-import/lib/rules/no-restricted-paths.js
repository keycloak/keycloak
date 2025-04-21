'use strict';var _path = require('path');var _path2 = _interopRequireDefault(_path);

var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _isGlob = require('is-glob');var _isGlob2 = _interopRequireDefault(_isGlob);
var _minimatch = require('minimatch');var _minimatch2 = _interopRequireDefault(_minimatch);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);
var _importType = require('../core/importType');var _importType2 = _interopRequireDefault(_importType);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var containsPath = function containsPath(filepath, target) {
  var relative = _path2['default'].relative(target, filepath);
  return relative === '' || !relative.startsWith('..');
};

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('no-restricted-paths') },


    schema: [
    {
      type: 'object',
      properties: {
        zones: {
          type: 'array',
          minItems: 1,
          items: {
            type: 'object',
            properties: {
              target: { type: 'string' },
              from: { type: 'string' },
              except: {
                type: 'array',
                items: {
                  type: 'string' },

                uniqueItems: true },

              message: { type: 'string' } },

            additionalProperties: false } },


        basePath: { type: 'string' } },

      additionalProperties: false }] },




  create: function () {function noRestrictedPaths(context) {
      var options = context.options[0] || {};
      var restrictedPaths = options.zones || [];
      var basePath = options.basePath || process.cwd();
      var currentFilename = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();
      var matchingZones = restrictedPaths.filter(function (zone) {
        var targetPath = _path2['default'].resolve(basePath, zone.target);

        if ((0, _isGlob2['default'])(targetPath)) {
          return (0, _minimatch2['default'])(currentFilename, targetPath);
        }

        return containsPath(currentFilename, targetPath);
      });

      function isValidExceptionPath(absoluteFromPath, absoluteExceptionPath) {
        var relativeExceptionPath = _path2['default'].relative(absoluteFromPath, absoluteExceptionPath);

        return (0, _importType2['default'])(relativeExceptionPath, context) !== 'parent';
      }

      function reportInvalidExceptionPath(node) {
        context.report({
          node: node,
          message: 'Restricted path exceptions must be descendants of the configured `from` path for that zone.' });

      }

      function reportInvalidExceptionGlob(node) {
        context.report({
          node: node,
          message: 'Restricted path exceptions must be glob patterns when`from` is a glob pattern' });

      }

      var makePathValidator = function () {function makePathValidator(zoneFrom) {var zoneExcept = arguments.length > 1 && arguments[1] !== undefined ? arguments[1] : [];
          var absoluteFrom = _path2['default'].resolve(basePath, zoneFrom);
          var isGlobPattern = (0, _isGlob2['default'])(zoneFrom);
          var isPathRestricted = void 0;
          var hasValidExceptions = void 0;
          var isPathException = void 0;
          var reportInvalidException = void 0;

          if (isGlobPattern) {
            var mm = new _minimatch.Minimatch(absoluteFrom);
            isPathRestricted = function () {function isPathRestricted(absoluteImportPath) {return mm.match(absoluteImportPath);}return isPathRestricted;}();

            hasValidExceptions = zoneExcept.every(_isGlob2['default']);

            if (hasValidExceptions) {
              var exceptionsMm = zoneExcept.map(function (except) {return new _minimatch.Minimatch(except);});
              isPathException = function () {function isPathException(absoluteImportPath) {return exceptionsMm.some(function (mm) {return mm.match(absoluteImportPath);});}return isPathException;}();
            }

            reportInvalidException = reportInvalidExceptionGlob;
          } else {
            isPathRestricted = function () {function isPathRestricted(absoluteImportPath) {return containsPath(absoluteImportPath, absoluteFrom);}return isPathRestricted;}();

            var absoluteExceptionPaths = zoneExcept.
            map(function (exceptionPath) {return _path2['default'].resolve(absoluteFrom, exceptionPath);});
            hasValidExceptions = absoluteExceptionPaths.
            every(function (absoluteExceptionPath) {return isValidExceptionPath(absoluteFrom, absoluteExceptionPath);});

            if (hasValidExceptions) {
              isPathException = function () {function isPathException(absoluteImportPath) {return absoluteExceptionPaths.some(
                  function (absoluteExceptionPath) {return containsPath(absoluteImportPath, absoluteExceptionPath);});}return isPathException;}();

            }

            reportInvalidException = reportInvalidExceptionPath;
          }

          return {
            isPathRestricted: isPathRestricted,
            hasValidExceptions: hasValidExceptions,
            isPathException: isPathException,
            reportInvalidException: reportInvalidException };

        }return makePathValidator;}();

      var validators = [];

      function checkForRestrictedImportPath(importPath, node) {
        var absoluteImportPath = (0, _resolve2['default'])(importPath, context);

        if (!absoluteImportPath) {
          return;
        }

        matchingZones.forEach(function (zone, index) {
          if (!validators[index]) {
            validators[index] = makePathValidator(zone.from, zone.except);
          }var _validators$index =






          validators[index],isPathRestricted = _validators$index.isPathRestricted,hasValidExceptions = _validators$index.hasValidExceptions,isPathException = _validators$index.isPathException,reportInvalidException = _validators$index.reportInvalidException;

          if (!isPathRestricted(absoluteImportPath)) {
            return;
          }

          if (!hasValidExceptions) {
            reportInvalidException(node);
            return;
          }

          var pathIsExcepted = isPathException(absoluteImportPath);
          if (pathIsExcepted) {
            return;
          }

          context.report({
            node: node,
            message: 'Unexpected path "{{importPath}}" imported in restricted zone.' + (zone.message ? ' ' + String(zone.message) : ''),
            data: { importPath: importPath } });

        });
      }

      return (0, _moduleVisitor2['default'])(function (source) {
        checkForRestrictedImportPath(source.value, source);
      }, { commonjs: true });
    }return noRestrictedPaths;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1yZXN0cmljdGVkLXBhdGhzLmpzIl0sIm5hbWVzIjpbImNvbnRhaW5zUGF0aCIsImZpbGVwYXRoIiwidGFyZ2V0IiwicmVsYXRpdmUiLCJwYXRoIiwic3RhcnRzV2l0aCIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiem9uZXMiLCJtaW5JdGVtcyIsIml0ZW1zIiwiZnJvbSIsImV4Y2VwdCIsInVuaXF1ZUl0ZW1zIiwibWVzc2FnZSIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwiYmFzZVBhdGgiLCJjcmVhdGUiLCJub1Jlc3RyaWN0ZWRQYXRocyIsImNvbnRleHQiLCJvcHRpb25zIiwicmVzdHJpY3RlZFBhdGhzIiwicHJvY2VzcyIsImN3ZCIsImN1cnJlbnRGaWxlbmFtZSIsImdldFBoeXNpY2FsRmlsZW5hbWUiLCJnZXRGaWxlbmFtZSIsIm1hdGNoaW5nWm9uZXMiLCJmaWx0ZXIiLCJ6b25lIiwidGFyZ2V0UGF0aCIsInJlc29sdmUiLCJpc1ZhbGlkRXhjZXB0aW9uUGF0aCIsImFic29sdXRlRnJvbVBhdGgiLCJhYnNvbHV0ZUV4Y2VwdGlvblBhdGgiLCJyZWxhdGl2ZUV4Y2VwdGlvblBhdGgiLCJyZXBvcnRJbnZhbGlkRXhjZXB0aW9uUGF0aCIsIm5vZGUiLCJyZXBvcnQiLCJyZXBvcnRJbnZhbGlkRXhjZXB0aW9uR2xvYiIsIm1ha2VQYXRoVmFsaWRhdG9yIiwiem9uZUZyb20iLCJ6b25lRXhjZXB0IiwiYWJzb2x1dGVGcm9tIiwiaXNHbG9iUGF0dGVybiIsImlzUGF0aFJlc3RyaWN0ZWQiLCJoYXNWYWxpZEV4Y2VwdGlvbnMiLCJpc1BhdGhFeGNlcHRpb24iLCJyZXBvcnRJbnZhbGlkRXhjZXB0aW9uIiwibW0iLCJNaW5pbWF0Y2giLCJhYnNvbHV0ZUltcG9ydFBhdGgiLCJtYXRjaCIsImV2ZXJ5IiwiaXNHbG9iIiwiZXhjZXB0aW9uc01tIiwibWFwIiwic29tZSIsImFic29sdXRlRXhjZXB0aW9uUGF0aHMiLCJleGNlcHRpb25QYXRoIiwidmFsaWRhdG9ycyIsImNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgiLCJpbXBvcnRQYXRoIiwiZm9yRWFjaCIsImluZGV4IiwicGF0aElzRXhjZXB0ZWQiLCJkYXRhIiwic291cmNlIiwidmFsdWUiLCJjb21tb25qcyJdLCJtYXBwaW5ncyI6ImFBQUEsNEI7O0FBRUEsc0Q7QUFDQSxrRTtBQUNBLGlDO0FBQ0Esc0M7QUFDQSxxQztBQUNBLGdEOztBQUVBLElBQU1BLGVBQWUsU0FBZkEsWUFBZSxDQUFDQyxRQUFELEVBQVdDLE1BQVgsRUFBc0I7QUFDekMsTUFBTUMsV0FBV0Msa0JBQUtELFFBQUwsQ0FBY0QsTUFBZCxFQUFzQkQsUUFBdEIsQ0FBakI7QUFDQSxTQUFPRSxhQUFhLEVBQWIsSUFBbUIsQ0FBQ0EsU0FBU0UsVUFBVCxDQUFvQixJQUFwQixDQUEzQjtBQUNELENBSEQ7O0FBS0FDLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLHFCQUFSLENBREQsRUFGRjs7O0FBTUpDLFlBQVE7QUFDTjtBQUNFSCxZQUFNLFFBRFI7QUFFRUksa0JBQVk7QUFDVkMsZUFBTztBQUNMTCxnQkFBTSxPQUREO0FBRUxNLG9CQUFVLENBRkw7QUFHTEMsaUJBQU87QUFDTFAsa0JBQU0sUUFERDtBQUVMSSx3QkFBWTtBQUNWWCxzQkFBUSxFQUFFTyxNQUFNLFFBQVIsRUFERTtBQUVWUSxvQkFBTSxFQUFFUixNQUFNLFFBQVIsRUFGSTtBQUdWUyxzQkFBUTtBQUNOVCxzQkFBTSxPQURBO0FBRU5PLHVCQUFPO0FBQ0xQLHdCQUFNLFFBREQsRUFGRDs7QUFLTlUsNkJBQWEsSUFMUCxFQUhFOztBQVVWQyx1QkFBUyxFQUFFWCxNQUFNLFFBQVIsRUFWQyxFQUZQOztBQWNMWSxrQ0FBc0IsS0FkakIsRUFIRixFQURHOzs7QUFxQlZDLGtC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LHFCQVFuQ0EsZUFSbUMsQ0FTbkNDLHNCQVRtQyxxQkFTbkNBLHNCQVRtQzs7QUFZckMsY0FBSSxDQUFDSCxpQkFBaUJNLGtCQUFqQixDQUFMLEVBQTJDO0FBQ3pDO0FBQ0Q7O0FBRUQsY0FBSSxDQUFDTCxrQkFBTCxFQUF5QjtBQUN2QkUsbUNBQXVCWCxJQUF2QjtBQUNBO0FBQ0Q7O0FBRUQsY0FBTTRCLGlCQUFpQmxCLGdCQUFnQkksa0JBQWhCLENBQXZCO0FBQ0EsY0FBSWMsY0FBSixFQUFvQjtBQUNsQjtBQUNEOztBQUVEOUMsa0JBQVFtQixNQUFSLENBQWU7QUFDYkQsc0JBRGE7QUFFYnZCLHdGQUF5RWUsS0FBS2YsT0FBTCxnQkFBbUJlLEtBQUtmLE9BQXhCLElBQW9DLEVBQTdHLENBRmE7QUFHYm9ELGtCQUFNLEVBQUVKLHNCQUFGLEVBSE8sRUFBZjs7QUFLRCxTQS9CRDtBQWdDRDs7QUFFRCxhQUFPLGdDQUFjLFVBQUNLLE1BQUQsRUFBWTtBQUMvQk4scUNBQTZCTSxPQUFPQyxLQUFwQyxFQUEyQ0QsTUFBM0M7QUFDRCxPQUZNLEVBRUosRUFBRUUsVUFBVSxJQUFaLEVBRkksQ0FBUDtBQUdELEtBOUhELE9BQWlCbkQsaUJBQWpCLElBdENlLEVBQWpCIiwiZmlsZSI6Im5vLXJlc3RyaWN0ZWQtcGF0aHMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgcGF0aCBmcm9tICdwYXRoJztcblxuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJztcbmltcG9ydCBtb2R1bGVWaXNpdG9yIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvbW9kdWxlVmlzaXRvcic7XG5pbXBvcnQgaXNHbG9iIGZyb20gJ2lzLWdsb2InO1xuaW1wb3J0IHsgTWluaW1hdGNoLCBkZWZhdWx0IGFzIG1pbmltYXRjaCB9IGZyb20gJ21pbmltYXRjaCc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcbmltcG9ydCBpbXBvcnRUeXBlIGZyb20gJy4uL2NvcmUvaW1wb3J0VHlwZSc7XG5cbmNvbnN0IGNvbnRhaW5zUGF0aCA9IChmaWxlcGF0aCwgdGFyZ2V0KSA9PiB7XG4gIGNvbnN0IHJlbGF0aXZlID0gcGF0aC5yZWxhdGl2ZSh0YXJnZXQsIGZpbGVwYXRoKTtcbiAgcmV0dXJuIHJlbGF0aXZlID09PSAnJyB8fCAhcmVsYXRpdmUuc3RhcnRzV2l0aCgnLi4nKTtcbn07XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tcmVzdHJpY3RlZC1wYXRocycpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IFtcbiAgICAgIHtcbiAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICB6b25lczoge1xuICAgICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICAgIG1pbkl0ZW1zOiAxLFxuICAgICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICAgICAgICB0YXJnZXQ6IHsgdHlwZTogJ3N0cmluZycgfSxcbiAgICAgICAgICAgICAgICBmcm9tOiB7IHR5cGU6ICdzdHJpbmcnIH0sXG4gICAgICAgICAgICAgICAgZXhjZXB0OiB7XG4gICAgICAgICAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgICAgdW5pcXVlSXRlbXM6IHRydWUsXG4gICAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgICAgICBtZXNzYWdlOiB7IHR5cGU6ICdzdHJpbmcnIH0sXG4gICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgfSxcbiAgICAgICAgICBiYXNlUGF0aDogeyB0eXBlOiAnc3RyaW5nJyB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiBub1Jlc3RyaWN0ZWRQYXRocyhjb250ZXh0KSB7XG4gICAgY29uc3Qgb3B0aW9ucyA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7fTtcbiAgICBjb25zdCByZXN0cmljdGVkUGF0aHMgPSBvcHRpb25zLnpvbmVzIHx8IFtdO1xuICAgIGNvbnN0IGJhc2VQYXRoID0gb3B0aW9ucy5iYXNlUGF0aCB8fCBwcm9jZXNzLmN3ZCgpO1xuICAgIGNvbnN0IGN1cnJlbnRGaWxlbmFtZSA9IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSA/IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSgpIDogY29udGV4dC5nZXRGaWxlbmFtZSgpO1xuICAgIGNvbnN0IG1hdGNoaW5nWm9uZXMgPSByZXN0cmljdGVkUGF0aHMuZmlsdGVyKCh6b25lKSA9PiB7XG4gICAgICBjb25zdCB0YXJnZXRQYXRoID0gcGF0aC5yZXNvbHZlKGJhc2VQYXRoLCB6b25lLnRhcmdldCk7XG5cbiAgICAgIGlmIChpc0dsb2IodGFyZ2V0UGF0aCkpIHtcbiAgICAgICAgcmV0dXJuIG1pbmltYXRjaChjdXJyZW50RmlsZW5hbWUsIHRhcmdldFBhdGgpO1xuICAgICAgfVxuXG4gICAgICByZXR1cm4gY29udGFpbnNQYXRoKGN1cnJlbnRGaWxlbmFtZSwgdGFyZ2V0UGF0aCk7XG4gICAgfSk7XG5cbiAgICBmdW5jdGlvbiBpc1ZhbGlkRXhjZXB0aW9uUGF0aChhYnNvbHV0ZUZyb21QYXRoLCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGgpIHtcbiAgICAgIGNvbnN0IHJlbGF0aXZlRXhjZXB0aW9uUGF0aCA9IHBhdGgucmVsYXRpdmUoYWJzb2x1dGVGcm9tUGF0aCwgYWJzb2x1dGVFeGNlcHRpb25QYXRoKTtcblxuICAgICAgcmV0dXJuIGltcG9ydFR5cGUocmVsYXRpdmVFeGNlcHRpb25QYXRoLCBjb250ZXh0KSAhPT0gJ3BhcmVudCc7XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gcmVwb3J0SW52YWxpZEV4Y2VwdGlvblBhdGgobm9kZSkge1xuICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICBub2RlLFxuICAgICAgICBtZXNzYWdlOiAnUmVzdHJpY3RlZCBwYXRoIGV4Y2VwdGlvbnMgbXVzdCBiZSBkZXNjZW5kYW50cyBvZiB0aGUgY29uZmlndXJlZCBgZnJvbWAgcGF0aCBmb3IgdGhhdCB6b25lLicsXG4gICAgICB9KTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiByZXBvcnRJbnZhbGlkRXhjZXB0aW9uR2xvYihub2RlKSB7XG4gICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgIG5vZGUsXG4gICAgICAgIG1lc3NhZ2U6ICdSZXN0cmljdGVkIHBhdGggZXhjZXB0aW9ucyBtdXN0IGJlIGdsb2IgcGF0dGVybnMgd2hlbmBmcm9tYCBpcyBhIGdsb2IgcGF0dGVybicsXG4gICAgICB9KTtcbiAgICB9XG5cbiAgICBjb25zdCBtYWtlUGF0aFZhbGlkYXRvciA9ICh6b25lRnJvbSwgem9uZUV4Y2VwdCA9IFtdKSA9PiB7XG4gICAgICBjb25zdCBhYnNvbHV0ZUZyb20gPSBwYXRoLnJlc29sdmUoYmFzZVBhdGgsIHpvbmVGcm9tKTtcbiAgICAgIGNvbnN0IGlzR2xvYlBhdHRlcm4gPSBpc0dsb2Ioem9uZUZyb20pO1xuICAgICAgbGV0IGlzUGF0aFJlc3RyaWN0ZWQ7XG4gICAgICBsZXQgaGFzVmFsaWRFeGNlcHRpb25zO1xuICAgICAgbGV0IGlzUGF0aEV4Y2VwdGlvbjtcbiAgICAgIGxldCByZXBvcnRJbnZhbGlkRXhjZXB0aW9uO1xuXG4gICAgICBpZiAoaXNHbG9iUGF0dGVybikge1xuICAgICAgICBjb25zdCBtbSA9IG5ldyBNaW5pbWF0Y2goYWJzb2x1dGVGcm9tKTtcbiAgICAgICAgaXNQYXRoUmVzdHJpY3RlZCA9IChhYnNvbHV0ZUltcG9ydFBhdGgpID0+IG1tLm1hdGNoKGFic29sdXRlSW1wb3J0UGF0aCk7XG5cbiAgICAgICAgaGFzVmFsaWRFeGNlcHRpb25zID0gem9uZUV4Y2VwdC5ldmVyeShpc0dsb2IpO1xuXG4gICAgICAgIGlmIChoYXNWYWxpZEV4Y2VwdGlvbnMpIHtcbiAgICAgICAgICBjb25zdCBleGNlcHRpb25zTW0gPSB6b25lRXhjZXB0Lm1hcCgoZXhjZXB0KSA9PiBuZXcgTWluaW1hdGNoKGV4Y2VwdCkpO1xuICAgICAgICAgIGlzUGF0aEV4Y2VwdGlvbiA9IChhYnNvbHV0ZUltcG9ydFBhdGgpID0+IGV4Y2VwdGlvbnNNbS5zb21lKChtbSkgPT4gbW0ubWF0Y2goYWJzb2x1dGVJbXBvcnRQYXRoKSk7XG4gICAgICAgIH1cblxuICAgICAgICByZXBvcnRJbnZhbGlkRXhjZXB0aW9uID0gcmVwb3J0SW52YWxpZEV4Y2VwdGlvbkdsb2I7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICBpc1BhdGhSZXN0cmljdGVkID0gKGFic29sdXRlSW1wb3J0UGF0aCkgPT4gY29udGFpbnNQYXRoKGFic29sdXRlSW1wb3J0UGF0aCwgYWJzb2x1dGVGcm9tKTtcblxuICAgICAgICBjb25zdCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGhzID0gem9uZUV4Y2VwdFxuICAgICAgICAgIC5tYXAoKGV4Y2VwdGlvblBhdGgpID0+IHBhdGgucmVzb2x2ZShhYnNvbHV0ZUZyb20sIGV4Y2VwdGlvblBhdGgpKTtcbiAgICAgICAgaGFzVmFsaWRFeGNlcHRpb25zID0gYWJzb2x1dGVFeGNlcHRpb25QYXRoc1xuICAgICAgICAgIC5ldmVyeSgoYWJzb2x1dGVFeGNlcHRpb25QYXRoKSA9PiBpc1ZhbGlkRXhjZXB0aW9uUGF0aChhYnNvbHV0ZUZyb20sIGFic29sdXRlRXhjZXB0aW9uUGF0aCkpO1xuXG4gICAgICAgIGlmIChoYXNWYWxpZEV4Y2VwdGlvbnMpIHtcbiAgICAgICAgICBpc1BhdGhFeGNlcHRpb24gPSAoYWJzb2x1dGVJbXBvcnRQYXRoKSA9PiBhYnNvbHV0ZUV4Y2VwdGlvblBhdGhzLnNvbWUoXG4gICAgICAgICAgICAoYWJzb2x1dGVFeGNlcHRpb25QYXRoKSA9PiBjb250YWluc1BhdGgoYWJzb2x1dGVJbXBvcnRQYXRoLCBhYnNvbHV0ZUV4Y2VwdGlvblBhdGgpLFxuICAgICAgICAgICk7XG4gICAgICAgIH1cblxuICAgICAgICByZXBvcnRJbnZhbGlkRXhjZXB0aW9uID0gcmVwb3J0SW52YWxpZEV4Y2VwdGlvblBhdGg7XG4gICAgICB9XG5cbiAgICAgIHJldHVybiB7XG4gICAgICAgIGlzUGF0aFJlc3RyaWN0ZWQsXG4gICAgICAgIGhhc1ZhbGlkRXhjZXB0aW9ucyxcbiAgICAgICAgaXNQYXRoRXhjZXB0aW9uLFxuICAgICAgICByZXBvcnRJbnZhbGlkRXhjZXB0aW9uLFxuICAgICAgfTtcbiAgICB9O1xuXG4gICAgY29uc3QgdmFsaWRhdG9ycyA9IFtdO1xuXG4gICAgZnVuY3Rpb24gY2hlY2tGb3JSZXN0cmljdGVkSW1wb3J0UGF0aChpbXBvcnRQYXRoLCBub2RlKSB7XG4gICAgICBjb25zdCBhYnNvbHV0ZUltcG9ydFBhdGggPSByZXNvbHZlKGltcG9ydFBhdGgsIGNvbnRleHQpO1xuXG4gICAgICBpZiAoIWFic29sdXRlSW1wb3J0UGF0aCkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIG1hdGNoaW5nWm9uZXMuZm9yRWFjaCgoem9uZSwgaW5kZXgpID0+IHtcbiAgICAgICAgaWYgKCF2YWxpZGF0b3JzW2luZGV4XSkge1xuICAgICAgICAgIHZhbGlkYXRvcnNbaW5kZXhdID0gbWFrZVBhdGhWYWxpZGF0b3Ioem9uZS5mcm9tLCB6b25lLmV4Y2VwdCk7XG4gICAgICAgIH1cblxuICAgICAgICBjb25zdCB7XG4gICAgICAgICAgaXNQYXRoUmVzdHJpY3RlZCxcbiAgICAgICAgICBoYXNWYWxpZEV4Y2VwdGlvbnMsXG4gICAgICAgICAgaXNQYXRoRXhjZXB0aW9uLFxuICAgICAgICAgIHJlcG9ydEludmFsaWRFeGNlcHRpb24sXG4gICAgICAgIH0gPSB2YWxpZGF0b3JzW2luZGV4XTtcblxuICAgICAgICBpZiAoIWlzUGF0aFJlc3RyaWN0ZWQoYWJzb2x1dGVJbXBvcnRQYXRoKSkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIGlmICghaGFzVmFsaWRFeGNlcHRpb25zKSB7XG4gICAgICAgICAgcmVwb3J0SW52YWxpZEV4Y2VwdGlvbihub2RlKTtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICBjb25zdCBwYXRoSXNFeGNlcHRlZCA9IGlzUGF0aEV4Y2VwdGlvbihhYnNvbHV0ZUltcG9ydFBhdGgpO1xuICAgICAgICBpZiAocGF0aElzRXhjZXB0ZWQpIHtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZSxcbiAgICAgICAgICBtZXNzYWdlOiBgVW5leHBlY3RlZCBwYXRoIFwie3tpbXBvcnRQYXRofX1cIiBpbXBvcnRlZCBpbiByZXN0cmljdGVkIHpvbmUuJHt6b25lLm1lc3NhZ2UgPyBgICR7em9uZS5tZXNzYWdlfWAgOiAnJ31gLFxuICAgICAgICAgIGRhdGE6IHsgaW1wb3J0UGF0aCB9LFxuICAgICAgICB9KTtcbiAgICAgIH0pO1xuICAgIH1cblxuICAgIHJldHVybiBtb2R1bGVWaXNpdG9yKChzb3VyY2UpID0+IHtcbiAgICAgIGNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgoc291cmNlLnZhbHVlLCBzb3VyY2UpO1xuICAgIH0sIHsgY29tbW9uanM6IHRydWUgfSk7XG4gIH0sXG59O1xuIl19