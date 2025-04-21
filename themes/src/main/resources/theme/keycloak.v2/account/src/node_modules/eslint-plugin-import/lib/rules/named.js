'use strict';var _slicedToArray = function () {function sliceIterator(arr, i) {var _arr = [];var _n = true;var _d = false;var _e = undefined;try {for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {_arr.push(_s.value);if (i && _arr.length === i) break;}} catch (err) {_d = true;_e = err;} finally {try {if (!_n && _i["return"]) _i["return"]();} finally {if (_d) throw _e;}}return _arr;}return function (arr, i) {if (Array.isArray(arr)) {return arr;} else if (Symbol.iterator in Object(arr)) {return sliceIterator(arr, i);} else {throw new TypeError("Invalid attempt to destructure non-iterable instance");}};}();var _path = require('path');var path = _interopRequireWildcard(_path);
var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}function _interopRequireWildcard(obj) {if (obj && obj.__esModule) {return obj;} else {var newObj = {};if (obj != null) {for (var key in obj) {if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key];}}newObj['default'] = obj;return newObj;}}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('named') },

    schema: [
    {
      type: 'object',
      properties: {
        commonjs: {
          type: 'boolean' } },


      additionalProperties: false }] },




  create: function () {function create(context) {
      var options = context.options[0] || {};

      function checkSpecifiers(key, type, node) {
        // ignore local exports and type imports/exports
        if (
        node.source == null ||
        node.importKind === 'type' ||
        node.importKind === 'typeof' ||
        node.exportKind === 'type')
        {
          return;
        }

        if (!node.specifiers.some(function (im) {return im.type === type;})) {
          return; // no named imports/exports
        }

        var imports = _ExportMap2['default'].get(node.source.value, context);
        if (imports == null || imports.parseGoal === 'ambiguous') {
          return;
        }

        if (imports.errors.length) {
          imports.reportErrors(context, node);
          return;
        }

        node.specifiers.forEach(function (im) {
          if (
          im.type !== type
          // ignore type imports
          || im.importKind === 'type' || im.importKind === 'typeof')
          {
            return;
          }

          var name = im[key].name || im[key].value;

          var deepLookup = imports.hasDeep(name);

          if (!deepLookup.found) {
            if (deepLookup.path.length > 1) {
              var deepPath = deepLookup.path.
              map(function (i) {return path.relative(path.dirname(context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename()), i.path);}).
              join(' -> ');

              context.report(im[key], String(name) + ' not found via ' + String(deepPath));
            } else {
              context.report(im[key], name + ' not found in \'' + node.source.value + '\'');
            }
          }
        });
      }

      function checkRequire(node) {
        if (
        !options.commonjs ||
        node.type !== 'VariableDeclarator'
        // return if it's not an object destructure or it's an empty object destructure
        || !node.id || node.id.type !== 'ObjectPattern' || node.id.properties.length === 0
        // return if there is no call expression on the right side
        || !node.init || node.init.type !== 'CallExpression')
        {
          return;
        }

        var call = node.init;var _call$arguments = _slicedToArray(
        call.arguments, 1),source = _call$arguments[0];
        var variableImports = node.id.properties;
        var variableExports = _ExportMap2['default'].get(source.value, context);

        if (
        // return if it's not a commonjs require statement
        call.callee.type !== 'Identifier' || call.callee.name !== 'require' || call.arguments.length !== 1
        // return if it's not a string source
        || source.type !== 'Literal' ||
        variableExports == null ||
        variableExports.parseGoal === 'ambiguous')
        {
          return;
        }

        if (variableExports.errors.length) {
          variableExports.reportErrors(context, node);
          return;
        }

        variableImports.forEach(function (im) {
          if (im.type !== 'Property' || !im.key || im.key.type !== 'Identifier') {
            return;
          }

          var deepLookup = variableExports.hasDeep(im.key.name);

          if (!deepLookup.found) {
            if (deepLookup.path.length > 1) {
              var deepPath = deepLookup.path.
              map(function (i) {return path.relative(path.dirname(context.getFilename()), i.path);}).
              join(' -> ');

              context.report(im.key, String(im.key.name) + ' not found via ' + String(deepPath));
            } else {
              context.report(im.key, im.key.name + ' not found in \'' + source.value + '\'');
            }
          }
        });
      }

      return {
        ImportDeclaration: checkSpecifiers.bind(null, 'imported', 'ImportSpecifier'),

        ExportNamedDeclaration: checkSpecifiers.bind(null, 'local', 'ExportSpecifier'),

        VariableDeclarator: checkRequire };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uYW1lZC5qcyJdLCJuYW1lcyI6WyJwYXRoIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJjb21tb25qcyIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwiY3JlYXRlIiwiY29udGV4dCIsIm9wdGlvbnMiLCJjaGVja1NwZWNpZmllcnMiLCJrZXkiLCJub2RlIiwic291cmNlIiwiaW1wb3J0S2luZCIsImV4cG9ydEtpbmQiLCJzcGVjaWZpZXJzIiwic29tZSIsImltIiwiaW1wb3J0cyIsIkV4cG9ydHMiLCJnZXQiLCJ2YWx1ZSIsInBhcnNlR29hbCIsImVycm9ycyIsImxlbmd0aCIsInJlcG9ydEVycm9ycyIsImZvckVhY2giLCJuYW1lIiwiZGVlcExvb2t1cCIsImhhc0RlZXAiLCJmb3VuZCIsImRlZXBQYXRoIiwibWFwIiwicmVsYXRpdmUiLCJkaXJuYW1lIiwiZ2V0UGh5c2ljYWxGaWxlbmFtZSIsImdldEZpbGVuYW1lIiwiaSIsImpvaW4iLCJyZXBvcnQiLCJjaGVja1JlcXVpcmUiLCJpZCIsImluaXQiLCJjYWxsIiwiYXJndW1lbnRzIiwidmFyaWFibGVJbXBvcnRzIiwidmFyaWFibGVFeHBvcnRzIiwiY2FsbGVlIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJiaW5kIiwiRXhwb3J0TmFtZWREZWNsYXJhdGlvbiIsIlZhcmlhYmxlRGVjbGFyYXRvciJdLCJtYXBwaW5ncyI6InFvQkFBQSw0QixJQUFZQSxJO0FBQ1oseUM7QUFDQSxxQzs7QUFFQUMsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sU0FERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsT0FBUixDQURELEVBRkY7O0FBS0pDLFlBQVE7QUFDTjtBQUNFSCxZQUFNLFFBRFI7QUFFRUksa0JBQVk7QUFDVkMsa0JBQVU7QUFDUkwsZ0JBQU0sU0FERSxFQURBLEVBRmQ7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a0JBQU1PLFdBQVdILFdBQVdqQyxJQUFYO0FBQ2RxQyxpQkFEYyxDQUNWLHFCQUFLckMsS0FBS3NDLFFBQUwsQ0FBY3RDLEtBQUt1QyxPQUFMLENBQWEzQixRQUFRNkIsV0FBUixFQUFiLENBQWQsRUFBbURDLEVBQUUxQyxJQUFyRCxDQUFMLEVBRFU7QUFFZDJDLGtCQUZjLENBRVQsTUFGUyxDQUFqQjs7QUFJQS9CLHNCQUFRZ0MsTUFBUixDQUFldEIsR0FBR1AsR0FBbEIsU0FBMEJPLEdBQUdQLEdBQUgsQ0FBT2lCLElBQWpDLCtCQUF1REksUUFBdkQ7QUFDRCxhQU5ELE1BTU87QUFDTHhCLHNCQUFRZ0MsTUFBUixDQUFldEIsR0FBR1AsR0FBbEIsRUFBdUJPLEdBQUdQLEdBQUgsQ0FBT2lCLElBQVAsR0FBYyxrQkFBZCxHQUFtQ2YsT0FBT1MsS0FBMUMsR0FBa0QsSUFBekU7QUFDRDtBQUNGO0FBQ0YsU0FsQkQ7QUFtQkQ7O0FBRUQsYUFBTztBQUNMMkIsMkJBQW1CdkMsZ0JBQWdCd0MsSUFBaEIsQ0FBcUIsSUFBckIsRUFBMkIsVUFBM0IsRUFBdUMsaUJBQXZDLENBRGQ7O0FBR0xDLGdDQUF3QnpDLGdCQUFnQndDLElBQWhCLENBQXFCLElBQXJCLEVBQTJCLE9BQTNCLEVBQW9DLGlCQUFwQyxDQUhuQjs7QUFLTEUsNEJBQW9CWCxZQUxmLEVBQVA7O0FBT0QsS0F2SWMsbUJBQWpCIiwiZmlsZSI6Im5hbWVkLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0ICogYXMgcGF0aCBmcm9tICdwYXRoJztcbmltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduYW1lZCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgY29tbW9uanM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdib29sZWFuJyxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9O1xuXG4gICAgZnVuY3Rpb24gY2hlY2tTcGVjaWZpZXJzKGtleSwgdHlwZSwgbm9kZSkge1xuICAgICAgLy8gaWdub3JlIGxvY2FsIGV4cG9ydHMgYW5kIHR5cGUgaW1wb3J0cy9leHBvcnRzXG4gICAgICBpZiAoXG4gICAgICAgIG5vZGUuc291cmNlID09IG51bGxcbiAgICAgICAgfHwgbm9kZS5pbXBvcnRLaW5kID09PSAndHlwZSdcbiAgICAgICAgfHwgbm9kZS5pbXBvcnRLaW5kID09PSAndHlwZW9mJ1xuICAgICAgICB8fCBub2RlLmV4cG9ydEtpbmQgPT09ICd0eXBlJ1xuICAgICAgKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgaWYgKCFub2RlLnNwZWNpZmllcnMuc29tZSgoaW0pID0+IGltLnR5cGUgPT09IHR5cGUpKSB7XG4gICAgICAgIHJldHVybjsgLy8gbm8gbmFtZWQgaW1wb3J0cy9leHBvcnRzXG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGltcG9ydHMgPSBFeHBvcnRzLmdldChub2RlLnNvdXJjZS52YWx1ZSwgY29udGV4dCk7XG4gICAgICBpZiAoaW1wb3J0cyA9PSBudWxsIHx8IGltcG9ydHMucGFyc2VHb2FsID09PSAnYW1iaWd1b3VzJykge1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGlmIChpbXBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgbm9kZSk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgbm9kZS5zcGVjaWZpZXJzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgIGlmIChcbiAgICAgICAgICBpbS50eXBlICE9PSB0eXBlXG4gICAgICAgICAgLy8gaWdub3JlIHR5cGUgaW1wb3J0c1xuICAgICAgICAgIHx8IGltLmltcG9ydEtpbmQgPT09ICd0eXBlJyB8fCBpbS5pbXBvcnRLaW5kID09PSAndHlwZW9mJ1xuICAgICAgICApIHtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICBjb25zdCBuYW1lID0gaW1ba2V5XS5uYW1lIHx8IGltW2tleV0udmFsdWU7XG5cbiAgICAgICAgY29uc3QgZGVlcExvb2t1cCA9IGltcG9ydHMuaGFzRGVlcChuYW1lKTtcblxuICAgICAgICBpZiAoIWRlZXBMb29rdXAuZm91bmQpIHtcbiAgICAgICAgICBpZiAoZGVlcExvb2t1cC5wYXRoLmxlbmd0aCA+IDEpIHtcbiAgICAgICAgICAgIGNvbnN0IGRlZXBQYXRoID0gZGVlcExvb2t1cC5wYXRoXG4gICAgICAgICAgICAgIC5tYXAoaSA9PiBwYXRoLnJlbGF0aXZlKHBhdGguZGlybmFtZShjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUgPyBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUoKSA6IGNvbnRleHQuZ2V0RmlsZW5hbWUoKSksIGkucGF0aCkpXG4gICAgICAgICAgICAgIC5qb2luKCcgLT4gJyk7XG5cbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KGltW2tleV0sIGAke25hbWV9IG5vdCBmb3VuZCB2aWEgJHtkZWVwUGF0aH1gKTtcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1ba2V5XSwgbmFtZSArICcgbm90IGZvdW5kIGluIFxcJycgKyBub2RlLnNvdXJjZS52YWx1ZSArICdcXCcnKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGNoZWNrUmVxdWlyZShub2RlKSB7XG4gICAgICBpZiAoXG4gICAgICAgICFvcHRpb25zLmNvbW1vbmpzXG4gICAgICAgIHx8IG5vZGUudHlwZSAhPT0gJ1ZhcmlhYmxlRGVjbGFyYXRvcidcbiAgICAgICAgLy8gcmV0dXJuIGlmIGl0J3Mgbm90IGFuIG9iamVjdCBkZXN0cnVjdHVyZSBvciBpdCdzIGFuIGVtcHR5IG9iamVjdCBkZXN0cnVjdHVyZVxuICAgICAgICB8fCAhbm9kZS5pZCB8fCBub2RlLmlkLnR5cGUgIT09ICdPYmplY3RQYXR0ZXJuJyB8fCBub2RlLmlkLnByb3BlcnRpZXMubGVuZ3RoID09PSAwXG4gICAgICAgIC8vIHJldHVybiBpZiB0aGVyZSBpcyBubyBjYWxsIGV4cHJlc3Npb24gb24gdGhlIHJpZ2h0IHNpZGVcbiAgICAgICAgfHwgIW5vZGUuaW5pdCB8fCBub2RlLmluaXQudHlwZSAhPT0gJ0NhbGxFeHByZXNzaW9uJ1xuICAgICAgKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY29uc3QgY2FsbCA9IG5vZGUuaW5pdDtcbiAgICAgIGNvbnN0IFtzb3VyY2VdID0gY2FsbC5hcmd1bWVudHM7XG4gICAgICBjb25zdCB2YXJpYWJsZUltcG9ydHMgPSBub2RlLmlkLnByb3BlcnRpZXM7XG4gICAgICBjb25zdCB2YXJpYWJsZUV4cG9ydHMgPSBFeHBvcnRzLmdldChzb3VyY2UudmFsdWUsIGNvbnRleHQpO1xuXG4gICAgICBpZiAoXG4gICAgICAgIC8vIHJldHVybiBpZiBpdCdzIG5vdCBhIGNvbW1vbmpzIHJlcXVpcmUgc3RhdGVtZW50XG4gICAgICAgIGNhbGwuY2FsbGVlLnR5cGUgIT09ICdJZGVudGlmaWVyJyB8fCBjYWxsLmNhbGxlZS5uYW1lICE9PSAncmVxdWlyZScgfHwgY2FsbC5hcmd1bWVudHMubGVuZ3RoICE9PSAxXG4gICAgICAgIC8vIHJldHVybiBpZiBpdCdzIG5vdCBhIHN0cmluZyBzb3VyY2VcbiAgICAgICAgfHwgc291cmNlLnR5cGUgIT09ICdMaXRlcmFsJ1xuICAgICAgICB8fCB2YXJpYWJsZUV4cG9ydHMgPT0gbnVsbFxuICAgICAgICB8fCB2YXJpYWJsZUV4cG9ydHMucGFyc2VHb2FsID09PSAnYW1iaWd1b3VzJ1xuICAgICAgKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgaWYgKHZhcmlhYmxlRXhwb3J0cy5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgIHZhcmlhYmxlRXhwb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgbm9kZSk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgdmFyaWFibGVJbXBvcnRzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgIGlmIChpbS50eXBlICE9PSAnUHJvcGVydHknIHx8ICFpbS5rZXkgfHwgaW0ua2V5LnR5cGUgIT09ICdJZGVudGlmaWVyJykge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIGNvbnN0IGRlZXBMb29rdXAgPSB2YXJpYWJsZUV4cG9ydHMuaGFzRGVlcChpbS5rZXkubmFtZSk7XG5cbiAgICAgICAgaWYgKCFkZWVwTG9va3VwLmZvdW5kKSB7XG4gICAgICAgICAgaWYgKGRlZXBMb29rdXAucGF0aC5sZW5ndGggPiAxKSB7XG4gICAgICAgICAgICBjb25zdCBkZWVwUGF0aCA9IGRlZXBMb29rdXAucGF0aFxuICAgICAgICAgICAgICAubWFwKGkgPT4gcGF0aC5yZWxhdGl2ZShwYXRoLmRpcm5hbWUoY29udGV4dC5nZXRGaWxlbmFtZSgpKSwgaS5wYXRoKSlcbiAgICAgICAgICAgICAgLmpvaW4oJyAtPiAnKTtcblxuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW0ua2V5LCBgJHtpbS5rZXkubmFtZX0gbm90IGZvdW5kIHZpYSAke2RlZXBQYXRofWApO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydChpbS5rZXksIGltLmtleS5uYW1lICsgJyBub3QgZm91bmQgaW4gXFwnJyArIHNvdXJjZS52YWx1ZSArICdcXCcnKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0pO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBJbXBvcnREZWNsYXJhdGlvbjogY2hlY2tTcGVjaWZpZXJzLmJpbmQobnVsbCwgJ2ltcG9ydGVkJywgJ0ltcG9ydFNwZWNpZmllcicpLFxuXG4gICAgICBFeHBvcnROYW1lZERlY2xhcmF0aW9uOiBjaGVja1NwZWNpZmllcnMuYmluZChudWxsLCAnbG9jYWwnLCAnRXhwb3J0U3BlY2lmaWVyJyksXG5cbiAgICAgIFZhcmlhYmxlRGVjbGFyYXRvcjogY2hlY2tSZXF1aXJlLFxuICAgIH07XG4gIH0sXG59O1xuIl19