'use strict';




var _ignore = require('eslint-module-utils/ignore');
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _path = require('path');var _path2 = _interopRequireDefault(_path);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

/**
                                                                                                                                                                                       * convert a potentially relative path from node utils into a true
                                                                                                                                                                                       * relative path.
                                                                                                                                                                                       *
                                                                                                                                                                                       * ../ -> ..
                                                                                                                                                                                       * ./ -> .
                                                                                                                                                                                       * .foo/bar -> ./.foo/bar
                                                                                                                                                                                       * ..foo/bar -> ./..foo/bar
                                                                                                                                                                                       * foo/bar -> ./foo/bar
                                                                                                                                                                                       *
                                                                                                                                                                                       * @param relativePath {string} relative posix path potentially missing leading './'
                                                                                                                                                                                       * @returns {string} relative posix path that always starts with a ./
                                                                                                                                                                                       **/
function toRelativePath(relativePath) {
  var stripped = relativePath.replace(/\/$/g, ''); // Remove trailing /

  return (/^((\.\.)|(\.))($|\/)/.test(stripped) ? stripped : './' + String(stripped));
} /**
   * @fileOverview Ensures that there are no useless path segments
   * @author Thomas Grainger
   */function normalize(fn) {return toRelativePath(_path2['default'].posix.normalize(fn));
}

function countRelativeParents(pathSegments) {
  return pathSegments.reduce(function (sum, pathSegment) {return pathSegment === '..' ? sum + 1 : sum;}, 0);
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-useless-path-segments') },


    fixable: 'code',

    schema: [
    {
      type: 'object',
      properties: {
        commonjs: { type: 'boolean' },
        noUselessIndex: { type: 'boolean' } },

      additionalProperties: false }] },




  create: function () {function create(context) {
      var currentDir = _path2['default'].dirname(context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename());
      var options = context.options[0];

      function checkSourceValue(source) {var
        importPath = source.value;

        function reportWithProposedPath(proposedPath) {
          context.report({
            node: source,
            // Note: Using messageIds is not possible due to the support for ESLint 2 and 3
            message: 'Useless path segments for "' + String(importPath) + '", should be "' + String(proposedPath) + '"',
            fix: function () {function fix(fixer) {return proposedPath && fixer.replaceText(source, JSON.stringify(proposedPath));}return fix;}() });

        }

        // Only relative imports are relevant for this rule --> Skip checking
        if (!importPath.startsWith('.')) {
          return;
        }

        // Report rule violation if path is not the shortest possible
        var resolvedPath = (0, _resolve2['default'])(importPath, context);
        var normedPath = normalize(importPath);
        var resolvedNormedPath = (0, _resolve2['default'])(normedPath, context);
        if (normedPath !== importPath && resolvedPath === resolvedNormedPath) {
          return reportWithProposedPath(normedPath);
        }

        var fileExtensions = (0, _ignore.getFileExtensions)(context.settings);
        var regexUnnecessaryIndex = new RegExp('.*\\/index(\\' + String(
        Array.from(fileExtensions).join('|\\')) + ')?$');


        // Check if path contains unnecessary index (including a configured extension)
        if (options && options.noUselessIndex && regexUnnecessaryIndex.test(importPath)) {
          var parentDirectory = _path2['default'].dirname(importPath);

          // Try to find ambiguous imports
          if (parentDirectory !== '.' && parentDirectory !== '..') {var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
              for (var _iterator = fileExtensions[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var fileExtension = _step.value;
                if ((0, _resolve2['default'])('' + String(parentDirectory) + String(fileExtension), context)) {
                  return reportWithProposedPath(String(parentDirectory) + '/');
                }
              }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
          }

          return reportWithProposedPath(parentDirectory);
        }

        // Path is shortest possible + starts from the current directory --> Return directly
        if (importPath.startsWith('./')) {
          return;
        }

        // Path is not existing --> Return directly (following code requires path to be defined)
        if (resolvedPath === undefined) {
          return;
        }

        var expected = _path2['default'].relative(currentDir, resolvedPath); // Expected import path
        var expectedSplit = expected.split(_path2['default'].sep); // Split by / or \ (depending on OS)
        var importPathSplit = importPath.replace(/^\.\//, '').split('/');
        var countImportPathRelativeParents = countRelativeParents(importPathSplit);
        var countExpectedRelativeParents = countRelativeParents(expectedSplit);
        var diff = countImportPathRelativeParents - countExpectedRelativeParents;

        // Same number of relative parents --> Paths are the same --> Return directly
        if (diff <= 0) {
          return;
        }

        // Report and propose minimal number of required relative parents
        return reportWithProposedPath(
        toRelativePath(
        importPathSplit.
        slice(0, countExpectedRelativeParents).
        concat(importPathSplit.slice(countImportPathRelativeParents + diff)).
        join('/')));


      }

      return (0, _moduleVisitor2['default'])(checkSourceValue, options);
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11c2VsZXNzLXBhdGgtc2VnbWVudHMuanMiXSwibmFtZXMiOlsidG9SZWxhdGl2ZVBhdGgiLCJyZWxhdGl2ZVBhdGgiLCJzdHJpcHBlZCIsInJlcGxhY2UiLCJ0ZXN0Iiwibm9ybWFsaXplIiwiZm4iLCJwYXRoIiwicG9zaXgiLCJjb3VudFJlbGF0aXZlUGFyZW50cyIsInBhdGhTZWdtZW50cyIsInJlZHVjZSIsInN1bSIsInBhdGhTZWdtZW50IiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImZpeGFibGUiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiY29tbW9uanMiLCJub1VzZWxlc3NJbmRleCIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwiY3JlYXRlIiwiY29udGV4dCIsImN1cnJlbnREaXIiLCJkaXJuYW1lIiwiZ2V0UGh5c2ljYWxGaWxlbmFtZSIsImdldEZpbGVuYW1lIiwib3B0aW9ucyIsImNoZWNrU291cmNlVmFsdWUiLCJzb3VyY2UiLCJpbXBvcnRQYXRoIiwidmFsdWUiLCJyZXBvcnRXaXRoUHJvcG9zZWRQYXRoIiwicHJvcG9zZWRQYXRoIiwicmVwb3J0Iiwibm9kZSIsIm1lc3NhZ2UiLCJmaXgiLCJmaXhlciIsInJlcGxhY2VUZXh0IiwiSlNPTiIsInN0cmluZ2lmeSIsInN0YXJ0c1dpdGgiLCJyZXNvbHZlZFBhdGgiLCJub3JtZWRQYXRoIiwicmVzb2x2ZWROb3JtZWRQYXRoIiwiZmlsZUV4dGVuc2lvbnMiLCJzZXR0aW5ncyIsInJlZ2V4VW5uZWNlc3NhcnlJbmRleCIsIlJlZ0V4cCIsIkFycmF5IiwiZnJvbSIsImpvaW4iLCJwYXJlbnREaXJlY3RvcnkiLCJmaWxlRXh0ZW5zaW9uIiwidW5kZWZpbmVkIiwiZXhwZWN0ZWQiLCJyZWxhdGl2ZSIsImV4cGVjdGVkU3BsaXQiLCJzcGxpdCIsInNlcCIsImltcG9ydFBhdGhTcGxpdCIsImNvdW50SW1wb3J0UGF0aFJlbGF0aXZlUGFyZW50cyIsImNvdW50RXhwZWN0ZWRSZWxhdGl2ZVBhcmVudHMiLCJkaWZmIiwic2xpY2UiLCJjb25jYXQiXSwibWFwcGluZ3MiOiI7Ozs7O0FBS0E7QUFDQSxrRTtBQUNBLHNEO0FBQ0EsNEI7QUFDQSxxQzs7QUFFQTs7Ozs7Ozs7Ozs7OztBQWFBLFNBQVNBLGNBQVQsQ0FBd0JDLFlBQXhCLEVBQXNDO0FBQ3BDLE1BQU1DLFdBQVdELGFBQWFFLE9BQWIsQ0FBcUIsTUFBckIsRUFBNkIsRUFBN0IsQ0FBakIsQ0FEb0MsQ0FDZTs7QUFFbkQsU0FBTyx3QkFBdUJDLElBQXZCLENBQTRCRixRQUE1QixJQUF3Q0EsUUFBeEMsaUJBQXdEQSxRQUF4RCxDQUFQO0FBQ0QsQyxDQTVCRDs7O0tBOEJBLFNBQVNHLFNBQVQsQ0FBbUJDLEVBQW5CLEVBQXVCLENBQ3JCLE9BQU9OLGVBQWVPLGtCQUFLQyxLQUFMLENBQVdILFNBQVgsQ0FBcUJDLEVBQXJCLENBQWYsQ0FBUDtBQUNEOztBQUVELFNBQVNHLG9CQUFULENBQThCQyxZQUE5QixFQUE0QztBQUMxQyxTQUFPQSxhQUFhQyxNQUFiLENBQW9CLFVBQUNDLEdBQUQsRUFBTUMsV0FBTixVQUFzQkEsZ0JBQWdCLElBQWhCLEdBQXVCRCxNQUFNLENBQTdCLEdBQWlDQSxHQUF2RCxFQUFwQixFQUFnRixDQUFoRixDQUFQO0FBQ0Q7O0FBRURFLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLDBCQUFSLENBREQsRUFGRjs7O0FBTUpDLGFBQVMsTUFOTDs7QUFRSkMsWUFBUTtBQUNOO0FBQ0VKLFlBQU0sUUFEUjtBQUVFSyxrQkFBWTtBQUNWQyxrQkFBVSxFQUFFTixNQUFNLFNBQVIsRUFEQTtBQUVWTyx3QkFBZ0IsRUFBRVAsTUFBTSxTQUFSLEVBRk4sRUFGZDs7QUFNRVEsNEJBQXNCLEtBTnhCLEVBRE0sQ0FSSixFQURTOzs7OztBQXFCZkMsUUFyQmUsK0JBcUJSQyxPQXJCUSxFQXFCQztBQUNkLFVBQU1DLGFBQWFyQixrQkFBS3NCLE9BQUwsQ0FBYUYsUUFBUUcsbUJBQVIsR0FBOEJILFFBQVFHLG1CQUFSLEVBQTlCLEdBQThESCxRQUFRSSxXQUFSLEVBQTNFLENBQW5CO0FBQ0EsVUFBTUMsVUFBVUwsUUFBUUssT0FBUixDQUFnQixDQUFoQixDQUFoQjs7QUFFQSxlQUFTQyxnQkFBVCxDQUEwQkMsTUFBMUIsRUFBa0M7QUFDakJDLGtCQURpQixHQUNGRCxNQURFLENBQ3hCRSxLQUR3Qjs7QUFHaEMsaUJBQVNDLHNCQUFULENBQWdDQyxZQUFoQyxFQUE4QztBQUM1Q1gsa0JBQVFZLE1BQVIsQ0FBZTtBQUNiQyxrQkFBTU4sTUFETztBQUViO0FBQ0FPLDREQUF1Q04sVUFBdkMsOEJBQWtFRyxZQUFsRSxPQUhhO0FBSWJJLDhCQUFLLDRCQUFTSixnQkFBZ0JLLE1BQU1DLFdBQU4sQ0FBa0JWLE1BQWxCLEVBQTBCVyxLQUFLQyxTQUFMLENBQWVSLFlBQWYsQ0FBMUIsQ0FBekIsRUFBTCxjQUphLEVBQWY7O0FBTUQ7O0FBRUQ7QUFDQSxZQUFJLENBQUNILFdBQVdZLFVBQVgsQ0FBc0IsR0FBdEIsQ0FBTCxFQUFpQztBQUMvQjtBQUNEOztBQUVEO0FBQ0EsWUFBTUMsZUFBZSwwQkFBUWIsVUFBUixFQUFvQlIsT0FBcEIsQ0FBckI7QUFDQSxZQUFNc0IsYUFBYTVDLFVBQVU4QixVQUFWLENBQW5CO0FBQ0EsWUFBTWUscUJBQXFCLDBCQUFRRCxVQUFSLEVBQW9CdEIsT0FBcEIsQ0FBM0I7QUFDQSxZQUFJc0IsZUFBZWQsVUFBZixJQUE2QmEsaUJBQWlCRSxrQkFBbEQsRUFBc0U7QUFDcEUsaUJBQU9iLHVCQUF1QlksVUFBdkIsQ0FBUDtBQUNEOztBQUVELFlBQU1FLGlCQUFpQiwrQkFBa0J4QixRQUFReUIsUUFBMUIsQ0FBdkI7QUFDQSxZQUFNQyx3QkFBd0IsSUFBSUMsTUFBSjtBQUNaQyxjQUFNQyxJQUFOLENBQVdMLGNBQVgsRUFBMkJNLElBQTNCLENBQWdDLEtBQWhDLENBRFksVUFBOUI7OztBQUlBO0FBQ0EsWUFBSXpCLFdBQVdBLFFBQVFSLGNBQW5CLElBQXFDNkIsc0JBQXNCakQsSUFBdEIsQ0FBMkIrQixVQUEzQixDQUF6QyxFQUFpRjtBQUMvRSxjQUFNdUIsa0JBQWtCbkQsa0JBQUtzQixPQUFMLENBQWFNLFVBQWIsQ0FBeEI7O0FBRUE7QUFDQSxjQUFJdUIsb0JBQW9CLEdBQXBCLElBQTJCQSxvQkFBb0IsSUFBbkQsRUFBeUQ7QUFDdkQsbUNBQTRCUCxjQUE1Qiw4SEFBNEMsS0FBakNRLGFBQWlDO0FBQzFDLG9CQUFJLHNDQUFXRCxlQUFYLFdBQTZCQyxhQUE3QixHQUE4Q2hDLE9BQTlDLENBQUosRUFBNEQ7QUFDMUQseUJBQU9VLDhCQUEwQnFCLGVBQTFCLFFBQVA7QUFDRDtBQUNGLGVBTHNEO0FBTXhEOztBQUVELGlCQUFPckIsdUJBQXVCcUIsZUFBdkIsQ0FBUDtBQUNEOztBQUVEO0FBQ0EsWUFBSXZCLFdBQVdZLFVBQVgsQ0FBc0IsSUFBdEIsQ0FBSixFQUFpQztBQUMvQjtBQUNEOztBQUVEO0FBQ0EsWUFBSUMsaUJBQWlCWSxTQUFyQixFQUFnQztBQUM5QjtBQUNEOztBQUVELFlBQU1DLFdBQVd0RCxrQkFBS3VELFFBQUwsQ0FBY2xDLFVBQWQsRUFBMEJvQixZQUExQixDQUFqQixDQXhEZ0MsQ0F3RDBCO0FBQzFELFlBQU1lLGdCQUFnQkYsU0FBU0csS0FBVCxDQUFlekQsa0JBQUswRCxHQUFwQixDQUF0QixDQXpEZ0MsQ0F5RGdCO0FBQ2hELFlBQU1DLGtCQUFrQi9CLFdBQVdoQyxPQUFYLENBQW1CLE9BQW5CLEVBQTRCLEVBQTVCLEVBQWdDNkQsS0FBaEMsQ0FBc0MsR0FBdEMsQ0FBeEI7QUFDQSxZQUFNRyxpQ0FBaUMxRCxxQkFBcUJ5RCxlQUFyQixDQUF2QztBQUNBLFlBQU1FLCtCQUErQjNELHFCQUFxQnNELGFBQXJCLENBQXJDO0FBQ0EsWUFBTU0sT0FBT0YsaUNBQWlDQyw0QkFBOUM7O0FBRUE7QUFDQSxZQUFJQyxRQUFRLENBQVosRUFBZTtBQUNiO0FBQ0Q7O0FBRUQ7QUFDQSxlQUFPaEM7QUFDTHJDO0FBQ0VrRTtBQUNHSSxhQURILENBQ1MsQ0FEVCxFQUNZRiw0QkFEWjtBQUVHRyxjQUZILENBRVVMLGdCQUFnQkksS0FBaEIsQ0FBc0JILGlDQUFpQ0UsSUFBdkQsQ0FGVjtBQUdHWixZQUhILENBR1EsR0FIUixDQURGLENBREssQ0FBUDs7O0FBUUQ7O0FBRUQsYUFBTyxnQ0FBY3hCLGdCQUFkLEVBQWdDRCxPQUFoQyxDQUFQO0FBQ0QsS0F6R2MsbUJBQWpCIiwiZmlsZSI6Im5vLXVzZWxlc3MtcGF0aC1zZWdtZW50cy5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVPdmVydmlldyBFbnN1cmVzIHRoYXQgdGhlcmUgYXJlIG5vIHVzZWxlc3MgcGF0aCBzZWdtZW50c1xuICogQGF1dGhvciBUaG9tYXMgR3JhaW5nZXJcbiAqL1xuXG5pbXBvcnQgeyBnZXRGaWxlRXh0ZW5zaW9ucyB9IGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvaWdub3JlJztcbmltcG9ydCBtb2R1bGVWaXNpdG9yIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvbW9kdWxlVmlzaXRvcic7XG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuLyoqXG4gKiBjb252ZXJ0IGEgcG90ZW50aWFsbHkgcmVsYXRpdmUgcGF0aCBmcm9tIG5vZGUgdXRpbHMgaW50byBhIHRydWVcbiAqIHJlbGF0aXZlIHBhdGguXG4gKlxuICogLi4vIC0+IC4uXG4gKiAuLyAtPiAuXG4gKiAuZm9vL2JhciAtPiAuLy5mb28vYmFyXG4gKiAuLmZvby9iYXIgLT4gLi8uLmZvby9iYXJcbiAqIGZvby9iYXIgLT4gLi9mb28vYmFyXG4gKlxuICogQHBhcmFtIHJlbGF0aXZlUGF0aCB7c3RyaW5nfSByZWxhdGl2ZSBwb3NpeCBwYXRoIHBvdGVudGlhbGx5IG1pc3NpbmcgbGVhZGluZyAnLi8nXG4gKiBAcmV0dXJucyB7c3RyaW5nfSByZWxhdGl2ZSBwb3NpeCBwYXRoIHRoYXQgYWx3YXlzIHN0YXJ0cyB3aXRoIGEgLi9cbiAqKi9cbmZ1bmN0aW9uIHRvUmVsYXRpdmVQYXRoKHJlbGF0aXZlUGF0aCkge1xuICBjb25zdCBzdHJpcHBlZCA9IHJlbGF0aXZlUGF0aC5yZXBsYWNlKC9cXC8kL2csICcnKTsgLy8gUmVtb3ZlIHRyYWlsaW5nIC9cblxuICByZXR1cm4gL14oKFxcLlxcLil8KFxcLikpKCR8XFwvKS8udGVzdChzdHJpcHBlZCkgPyBzdHJpcHBlZCA6IGAuLyR7c3RyaXBwZWR9YDtcbn1cblxuZnVuY3Rpb24gbm9ybWFsaXplKGZuKSB7XG4gIHJldHVybiB0b1JlbGF0aXZlUGF0aChwYXRoLnBvc2l4Lm5vcm1hbGl6ZShmbikpO1xufVxuXG5mdW5jdGlvbiBjb3VudFJlbGF0aXZlUGFyZW50cyhwYXRoU2VnbWVudHMpIHtcbiAgcmV0dXJuIHBhdGhTZWdtZW50cy5yZWR1Y2UoKHN1bSwgcGF0aFNlZ21lbnQpID0+IHBhdGhTZWdtZW50ID09PSAnLi4nID8gc3VtICsgMSA6IHN1bSwgMCk7XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tdXNlbGVzcy1wYXRoLXNlZ21lbnRzJyksXG4gICAgfSxcblxuICAgIGZpeGFibGU6ICdjb2RlJyxcblxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgIGNvbW1vbmpzOiB7IHR5cGU6ICdib29sZWFuJyB9LFxuICAgICAgICAgIG5vVXNlbGVzc0luZGV4OiB7IHR5cGU6ICdib29sZWFuJyB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBjdXJyZW50RGlyID0gcGF0aC5kaXJuYW1lKGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSA/IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSgpIDogY29udGV4dC5nZXRGaWxlbmFtZSgpKTtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdO1xuXG4gICAgZnVuY3Rpb24gY2hlY2tTb3VyY2VWYWx1ZShzb3VyY2UpIHtcbiAgICAgIGNvbnN0IHsgdmFsdWU6IGltcG9ydFBhdGggfSA9IHNvdXJjZTtcblxuICAgICAgZnVuY3Rpb24gcmVwb3J0V2l0aFByb3Bvc2VkUGF0aChwcm9wb3NlZFBhdGgpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgIG5vZGU6IHNvdXJjZSxcbiAgICAgICAgICAvLyBOb3RlOiBVc2luZyBtZXNzYWdlSWRzIGlzIG5vdCBwb3NzaWJsZSBkdWUgdG8gdGhlIHN1cHBvcnQgZm9yIEVTTGludCAyIGFuZCAzXG4gICAgICAgICAgbWVzc2FnZTogYFVzZWxlc3MgcGF0aCBzZWdtZW50cyBmb3IgXCIke2ltcG9ydFBhdGh9XCIsIHNob3VsZCBiZSBcIiR7cHJvcG9zZWRQYXRofVwiYCxcbiAgICAgICAgICBmaXg6IGZpeGVyID0+IHByb3Bvc2VkUGF0aCAmJiBmaXhlci5yZXBsYWNlVGV4dChzb3VyY2UsIEpTT04uc3RyaW5naWZ5KHByb3Bvc2VkUGF0aCkpLFxuICAgICAgICB9KTtcbiAgICAgIH1cblxuICAgICAgLy8gT25seSByZWxhdGl2ZSBpbXBvcnRzIGFyZSByZWxldmFudCBmb3IgdGhpcyBydWxlIC0tPiBTa2lwIGNoZWNraW5nXG4gICAgICBpZiAoIWltcG9ydFBhdGguc3RhcnRzV2l0aCgnLicpKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgLy8gUmVwb3J0IHJ1bGUgdmlvbGF0aW9uIGlmIHBhdGggaXMgbm90IHRoZSBzaG9ydGVzdCBwb3NzaWJsZVxuICAgICAgY29uc3QgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShpbXBvcnRQYXRoLCBjb250ZXh0KTtcbiAgICAgIGNvbnN0IG5vcm1lZFBhdGggPSBub3JtYWxpemUoaW1wb3J0UGF0aCk7XG4gICAgICBjb25zdCByZXNvbHZlZE5vcm1lZFBhdGggPSByZXNvbHZlKG5vcm1lZFBhdGgsIGNvbnRleHQpO1xuICAgICAgaWYgKG5vcm1lZFBhdGggIT09IGltcG9ydFBhdGggJiYgcmVzb2x2ZWRQYXRoID09PSByZXNvbHZlZE5vcm1lZFBhdGgpIHtcbiAgICAgICAgcmV0dXJuIHJlcG9ydFdpdGhQcm9wb3NlZFBhdGgobm9ybWVkUGF0aCk7XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGZpbGVFeHRlbnNpb25zID0gZ2V0RmlsZUV4dGVuc2lvbnMoY29udGV4dC5zZXR0aW5ncyk7XG4gICAgICBjb25zdCByZWdleFVubmVjZXNzYXJ5SW5kZXggPSBuZXcgUmVnRXhwKFxuICAgICAgICBgLipcXFxcL2luZGV4KFxcXFwke0FycmF5LmZyb20oZmlsZUV4dGVuc2lvbnMpLmpvaW4oJ3xcXFxcJyl9KT8kYCxcbiAgICAgICk7XG5cbiAgICAgIC8vIENoZWNrIGlmIHBhdGggY29udGFpbnMgdW5uZWNlc3NhcnkgaW5kZXggKGluY2x1ZGluZyBhIGNvbmZpZ3VyZWQgZXh0ZW5zaW9uKVxuICAgICAgaWYgKG9wdGlvbnMgJiYgb3B0aW9ucy5ub1VzZWxlc3NJbmRleCAmJiByZWdleFVubmVjZXNzYXJ5SW5kZXgudGVzdChpbXBvcnRQYXRoKSkge1xuICAgICAgICBjb25zdCBwYXJlbnREaXJlY3RvcnkgPSBwYXRoLmRpcm5hbWUoaW1wb3J0UGF0aCk7XG5cbiAgICAgICAgLy8gVHJ5IHRvIGZpbmQgYW1iaWd1b3VzIGltcG9ydHNcbiAgICAgICAgaWYgKHBhcmVudERpcmVjdG9yeSAhPT0gJy4nICYmIHBhcmVudERpcmVjdG9yeSAhPT0gJy4uJykge1xuICAgICAgICAgIGZvciAoY29uc3QgZmlsZUV4dGVuc2lvbiBvZiBmaWxlRXh0ZW5zaW9ucykge1xuICAgICAgICAgICAgaWYgKHJlc29sdmUoYCR7cGFyZW50RGlyZWN0b3J5fSR7ZmlsZUV4dGVuc2lvbn1gLCBjb250ZXh0KSkge1xuICAgICAgICAgICAgICByZXR1cm4gcmVwb3J0V2l0aFByb3Bvc2VkUGF0aChgJHtwYXJlbnREaXJlY3Rvcnl9L2ApO1xuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIHJldHVybiByZXBvcnRXaXRoUHJvcG9zZWRQYXRoKHBhcmVudERpcmVjdG9yeSk7XG4gICAgICB9XG5cbiAgICAgIC8vIFBhdGggaXMgc2hvcnRlc3QgcG9zc2libGUgKyBzdGFydHMgZnJvbSB0aGUgY3VycmVudCBkaXJlY3RvcnkgLS0+IFJldHVybiBkaXJlY3RseVxuICAgICAgaWYgKGltcG9ydFBhdGguc3RhcnRzV2l0aCgnLi8nKSkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIC8vIFBhdGggaXMgbm90IGV4aXN0aW5nIC0tPiBSZXR1cm4gZGlyZWN0bHkgKGZvbGxvd2luZyBjb2RlIHJlcXVpcmVzIHBhdGggdG8gYmUgZGVmaW5lZClcbiAgICAgIGlmIChyZXNvbHZlZFBhdGggPT09IHVuZGVmaW5lZCkge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGV4cGVjdGVkID0gcGF0aC5yZWxhdGl2ZShjdXJyZW50RGlyLCByZXNvbHZlZFBhdGgpOyAvLyBFeHBlY3RlZCBpbXBvcnQgcGF0aFxuICAgICAgY29uc3QgZXhwZWN0ZWRTcGxpdCA9IGV4cGVjdGVkLnNwbGl0KHBhdGguc2VwKTsgLy8gU3BsaXQgYnkgLyBvciBcXCAoZGVwZW5kaW5nIG9uIE9TKVxuICAgICAgY29uc3QgaW1wb3J0UGF0aFNwbGl0ID0gaW1wb3J0UGF0aC5yZXBsYWNlKC9eXFwuXFwvLywgJycpLnNwbGl0KCcvJyk7XG4gICAgICBjb25zdCBjb3VudEltcG9ydFBhdGhSZWxhdGl2ZVBhcmVudHMgPSBjb3VudFJlbGF0aXZlUGFyZW50cyhpbXBvcnRQYXRoU3BsaXQpO1xuICAgICAgY29uc3QgY291bnRFeHBlY3RlZFJlbGF0aXZlUGFyZW50cyA9IGNvdW50UmVsYXRpdmVQYXJlbnRzKGV4cGVjdGVkU3BsaXQpO1xuICAgICAgY29uc3QgZGlmZiA9IGNvdW50SW1wb3J0UGF0aFJlbGF0aXZlUGFyZW50cyAtIGNvdW50RXhwZWN0ZWRSZWxhdGl2ZVBhcmVudHM7XG5cbiAgICAgIC8vIFNhbWUgbnVtYmVyIG9mIHJlbGF0aXZlIHBhcmVudHMgLS0+IFBhdGhzIGFyZSB0aGUgc2FtZSAtLT4gUmV0dXJuIGRpcmVjdGx5XG4gICAgICBpZiAoZGlmZiA8PSAwKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgLy8gUmVwb3J0IGFuZCBwcm9wb3NlIG1pbmltYWwgbnVtYmVyIG9mIHJlcXVpcmVkIHJlbGF0aXZlIHBhcmVudHNcbiAgICAgIHJldHVybiByZXBvcnRXaXRoUHJvcG9zZWRQYXRoKFxuICAgICAgICB0b1JlbGF0aXZlUGF0aChcbiAgICAgICAgICBpbXBvcnRQYXRoU3BsaXRcbiAgICAgICAgICAgIC5zbGljZSgwLCBjb3VudEV4cGVjdGVkUmVsYXRpdmVQYXJlbnRzKVxuICAgICAgICAgICAgLmNvbmNhdChpbXBvcnRQYXRoU3BsaXQuc2xpY2UoY291bnRJbXBvcnRQYXRoUmVsYXRpdmVQYXJlbnRzICsgZGlmZikpXG4gICAgICAgICAgICAuam9pbignLycpLFxuICAgICAgICApLFxuICAgICAgKTtcbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBvcHRpb25zKTtcbiAgfSxcbn07XG4iXX0=