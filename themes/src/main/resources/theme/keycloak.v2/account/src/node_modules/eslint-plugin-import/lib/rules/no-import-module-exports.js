'use strict';var _minimatch = require('minimatch');var _minimatch2 = _interopRequireDefault(_minimatch);
var _path = require('path');var _path2 = _interopRequireDefault(_path);
var _pkgUp = require('eslint-module-utils/pkgUp');var _pkgUp2 = _interopRequireDefault(_pkgUp);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function getEntryPoint(context) {
  var pkgPath = (0, _pkgUp2['default'])({ cwd: context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename() });
  try {
    return require.resolve(_path2['default'].dirname(pkgPath));
  } catch (error) {
    // Assume the package has no entrypoint (e.g. CLI packages)
    // in which case require.resolve would throw.
    return null;
  }
}

function findScope(context, identifier) {var _context$getSourceCod =
  context.getSourceCode(),scopeManager = _context$getSourceCod.scopeManager;

  return scopeManager && scopeManager.scopes.slice().reverse().find(function (scope) {return scope.variables.some(function (variable) {return variable.identifiers.some(function (node) {return node.name === identifier;});});});
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Disallow import statements with module.exports',
      category: 'Best Practices',
      recommended: true },

    fixable: 'code',
    schema: [
    {
      'type': 'object',
      'properties': {
        'exceptions': { 'type': 'array' } },

      'additionalProperties': false }] },



  create: function () {function create(context) {
      var importDeclarations = [];
      var entryPoint = getEntryPoint(context);
      var options = context.options[0] || {};
      var alreadyReported = false;

      function report(node) {
        var fileName = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();
        var isEntryPoint = entryPoint === fileName;
        var isIdentifier = node.object.type === 'Identifier';
        var hasKeywords = /^(module|exports)$/.test(node.object.name);
        var objectScope = hasKeywords && findScope(context, node.object.name);
        var hasCJSExportReference = hasKeywords && (!objectScope || objectScope.type === 'module');
        var isException = !!options.exceptions && options.exceptions.some(function (glob) {return (0, _minimatch2['default'])(fileName, glob);});

        if (isIdentifier && hasCJSExportReference && !isEntryPoint && !isException) {
          importDeclarations.forEach(function (importDeclaration) {
            context.report({
              node: importDeclaration,
              message: 'Cannot use import declarations in modules that export using ' + 'CommonJS (module.exports = \'foo\' or exports.bar = \'hi\')' });


          });
          alreadyReported = true;
        }
      }

      return {
        ImportDeclaration: function () {function ImportDeclaration(node) {
            importDeclarations.push(node);
          }return ImportDeclaration;}(),
        MemberExpression: function () {function MemberExpression(node) {
            if (!alreadyReported) {
              report(node);
            }
          }return MemberExpression;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1pbXBvcnQtbW9kdWxlLWV4cG9ydHMuanMiXSwibmFtZXMiOlsiZ2V0RW50cnlQb2ludCIsImNvbnRleHQiLCJwa2dQYXRoIiwiY3dkIiwiZ2V0UGh5c2ljYWxGaWxlbmFtZSIsImdldEZpbGVuYW1lIiwicmVxdWlyZSIsInJlc29sdmUiLCJwYXRoIiwiZGlybmFtZSIsImVycm9yIiwiZmluZFNjb3BlIiwiaWRlbnRpZmllciIsImdldFNvdXJjZUNvZGUiLCJzY29wZU1hbmFnZXIiLCJzY29wZXMiLCJzbGljZSIsInJldmVyc2UiLCJmaW5kIiwic2NvcGUiLCJ2YXJpYWJsZXMiLCJzb21lIiwidmFyaWFibGUiLCJpZGVudGlmaWVycyIsIm5vZGUiLCJuYW1lIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsImRlc2NyaXB0aW9uIiwiY2F0ZWdvcnkiLCJyZWNvbW1lbmRlZCIsImZpeGFibGUiLCJzY2hlbWEiLCJjcmVhdGUiLCJpbXBvcnREZWNsYXJhdGlvbnMiLCJlbnRyeVBvaW50Iiwib3B0aW9ucyIsImFscmVhZHlSZXBvcnRlZCIsInJlcG9ydCIsImZpbGVOYW1lIiwiaXNFbnRyeVBvaW50IiwiaXNJZGVudGlmaWVyIiwib2JqZWN0IiwiaGFzS2V5d29yZHMiLCJ0ZXN0Iiwib2JqZWN0U2NvcGUiLCJoYXNDSlNFeHBvcnRSZWZlcmVuY2UiLCJpc0V4Y2VwdGlvbiIsImV4Y2VwdGlvbnMiLCJnbG9iIiwiZm9yRWFjaCIsImltcG9ydERlY2xhcmF0aW9uIiwibWVzc2FnZSIsIkltcG9ydERlY2xhcmF0aW9uIiwicHVzaCIsIk1lbWJlckV4cHJlc3Npb24iXSwibWFwcGluZ3MiOiJhQUFBLHNDO0FBQ0EsNEI7QUFDQSxrRDs7QUFFQSxTQUFTQSxhQUFULENBQXVCQyxPQUF2QixFQUFnQztBQUM5QixNQUFNQyxVQUFVLHdCQUFNLEVBQUVDLEtBQUtGLFFBQVFHLG1CQUFSLEdBQThCSCxRQUFRRyxt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LFdBSEk7QUFJTGtDLHdCQUpLLHlDQUlZbEMsSUFKWixFQUlrQjtBQUNyQixnQkFBSSxDQUFDZ0IsZUFBTCxFQUFzQjtBQUNwQkMscUJBQU9qQixJQUFQO0FBQ0Q7QUFDRixXQVJJLDZCQUFQOztBQVVELEtBeERjLG1CQUFqQiIsImZpbGUiOiJuby1pbXBvcnQtbW9kdWxlLWV4cG9ydHMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgbWluaW1hdGNoIGZyb20gJ21pbmltYXRjaCc7XG5pbXBvcnQgcGF0aCBmcm9tICdwYXRoJztcbmltcG9ydCBwa2dVcCBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3BrZ1VwJztcblxuZnVuY3Rpb24gZ2V0RW50cnlQb2ludChjb250ZXh0KSB7XG4gIGNvbnN0IHBrZ1BhdGggPSBwa2dVcCh7IGN3ZDogY29udGV4dC5nZXRQaHlzaWNhbEZpbGVuYW1lID8gY29udGV4dC5nZXRQaHlzaWNhbEZpbGVuYW1lKCkgOiBjb250ZXh0LmdldEZpbGVuYW1lKCkgfSk7XG4gIHRyeSB7XG4gICAgcmV0dXJuIHJlcXVpcmUucmVzb2x2ZShwYXRoLmRpcm5hbWUocGtnUGF0aCkpO1xuICB9IGNhdGNoIChlcnJvcikge1xuICAgIC8vIEFzc3VtZSB0aGUgcGFja2FnZSBoYXMgbm8gZW50cnlwb2ludCAoZS5nLiBDTEkgcGFja2FnZXMpXG4gICAgLy8gaW4gd2hpY2ggY2FzZSByZXF1aXJlLnJlc29sdmUgd291bGQgdGhyb3cuXG4gICAgcmV0dXJuIG51bGw7XG4gIH1cbn1cblxuZnVuY3Rpb24gZmluZFNjb3BlKGNvbnRleHQsIGlkZW50aWZpZXIpIHtcbiAgY29uc3QgeyBzY29wZU1hbmFnZXIgfSA9IGNvbnRleHQuZ2V0U291cmNlQ29kZSgpO1xuXG4gIHJldHVybiBzY29wZU1hbmFnZXIgJiYgc2NvcGVNYW5hZ2VyLnNjb3Blcy5zbGljZSgpLnJldmVyc2UoKS5maW5kKChzY29wZSkgPT4gc2NvcGUudmFyaWFibGVzLnNvbWUodmFyaWFibGUgPT4gdmFyaWFibGUuaWRlbnRpZmllcnMuc29tZSgobm9kZSkgPT4gbm9kZS5uYW1lID09PSBpZGVudGlmaWVyKSkpO1xufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICBkZXNjcmlwdGlvbjogJ0Rpc2FsbG93IGltcG9ydCBzdGF0ZW1lbnRzIHdpdGggbW9kdWxlLmV4cG9ydHMnLFxuICAgICAgY2F0ZWdvcnk6ICdCZXN0IFByYWN0aWNlcycsXG4gICAgICByZWNvbW1lbmRlZDogdHJ1ZSxcbiAgICB9LFxuICAgIGZpeGFibGU6ICdjb2RlJyxcbiAgICBzY2hlbWE6IFtcbiAgICAgIHtcbiAgICAgICAgJ3R5cGUnOiAnb2JqZWN0JyxcbiAgICAgICAgJ3Byb3BlcnRpZXMnOiB7XG4gICAgICAgICAgJ2V4Y2VwdGlvbnMnOiB7ICd0eXBlJzogJ2FycmF5JyB9LFxuICAgICAgICB9LFxuICAgICAgICAnYWRkaXRpb25hbFByb3BlcnRpZXMnOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBpbXBvcnREZWNsYXJhdGlvbnMgPSBbXTtcbiAgICBjb25zdCBlbnRyeVBvaW50ID0gZ2V0RW50cnlQb2ludChjb250ZXh0KTtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9O1xuICAgIGxldCBhbHJlYWR5UmVwb3J0ZWQgPSBmYWxzZTtcblxuICAgIGZ1bmN0aW9uIHJlcG9ydChub2RlKSB7XG4gICAgICBjb25zdCBmaWxlTmFtZSA9IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSA/IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSgpIDogY29udGV4dC5nZXRGaWxlbmFtZSgpO1xuICAgICAgY29uc3QgaXNFbnRyeVBvaW50ID0gZW50cnlQb2ludCA9PT0gZmlsZU5hbWU7XG4gICAgICBjb25zdCBpc0lkZW50aWZpZXIgPSBub2RlLm9iamVjdC50eXBlID09PSAnSWRlbnRpZmllcic7XG4gICAgICBjb25zdCBoYXNLZXl3b3JkcyA9ICgvXihtb2R1bGV8ZXhwb3J0cykkLykudGVzdChub2RlLm9iamVjdC5uYW1lKTtcbiAgICAgIGNvbnN0IG9iamVjdFNjb3BlID0gaGFzS2V5d29yZHMgJiYgZmluZFNjb3BlKGNvbnRleHQsIG5vZGUub2JqZWN0Lm5hbWUpO1xuICAgICAgY29uc3QgaGFzQ0pTRXhwb3J0UmVmZXJlbmNlID0gaGFzS2V5d29yZHMgJiYgKCFvYmplY3RTY29wZSB8fCBvYmplY3RTY29wZS50eXBlID09PSAnbW9kdWxlJyk7XG4gICAgICBjb25zdCBpc0V4Y2VwdGlvbiA9ICEhb3B0aW9ucy5leGNlcHRpb25zICYmIG9wdGlvbnMuZXhjZXB0aW9ucy5zb21lKGdsb2IgPT4gbWluaW1hdGNoKGZpbGVOYW1lLCBnbG9iKSk7XG5cbiAgICAgIGlmIChpc0lkZW50aWZpZXIgJiYgaGFzQ0pTRXhwb3J0UmVmZXJlbmNlICYmICFpc0VudHJ5UG9pbnQgJiYgIWlzRXhjZXB0aW9uKSB7XG4gICAgICAgIGltcG9ydERlY2xhcmF0aW9ucy5mb3JFYWNoKGltcG9ydERlY2xhcmF0aW9uID0+IHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlOiBpbXBvcnREZWNsYXJhdGlvbixcbiAgICAgICAgICAgIG1lc3NhZ2U6IGBDYW5ub3QgdXNlIGltcG9ydCBkZWNsYXJhdGlvbnMgaW4gbW9kdWxlcyB0aGF0IGV4cG9ydCB1c2luZyBgICtcbiAgICAgICAgICAgICAgYENvbW1vbkpTIChtb2R1bGUuZXhwb3J0cyA9ICdmb28nIG9yIGV4cG9ydHMuYmFyID0gJ2hpJylgLFxuICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcbiAgICAgICAgYWxyZWFkeVJlcG9ydGVkID0gdHJ1ZTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgSW1wb3J0RGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBpbXBvcnREZWNsYXJhdGlvbnMucHVzaChub2RlKTtcbiAgICAgIH0sXG4gICAgICBNZW1iZXJFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKCFhbHJlYWR5UmVwb3J0ZWQpIHtcbiAgICAgICAgICByZXBvcnQobm9kZSk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=