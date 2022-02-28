'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _importDeclaration = require('../importDeclaration');

var _importDeclaration2 = _interopRequireDefault(_importDeclaration);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-named-as-default')
    }
  },

  create: function (context) {
    function checkDefault(nameKey, defaultSpecifier) {
      // #566: default is a valid specifier
      if (defaultSpecifier[nameKey].name === 'default') return;

      var declaration = (0, _importDeclaration2.default)(context);

      var imports = _ExportMap2.default.get(declaration.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, declaration);
        return;
      }

      if (imports.has('default') && imports.has(defaultSpecifier[nameKey].name)) {

        context.report(defaultSpecifier, 'Using exported name \'' + defaultSpecifier[nameKey].name + '\' as identifier for default export.');
      }
    }
    return {
      'ImportDefaultSpecifier': checkDefault.bind(null, 'local'),
      'ExportDefaultSpecifier': checkDefault.bind(null, 'exported')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5hbWVkLWFzLWRlZmF1bHQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImNyZWF0ZSIsImNvbnRleHQiLCJjaGVja0RlZmF1bHQiLCJuYW1lS2V5IiwiZGVmYXVsdFNwZWNpZmllciIsIm5hbWUiLCJkZWNsYXJhdGlvbiIsImltcG9ydHMiLCJFeHBvcnRzIiwiZ2V0Iiwic291cmNlIiwidmFsdWUiLCJlcnJvcnMiLCJsZW5ndGgiLCJyZXBvcnRFcnJvcnMiLCJoYXMiLCJyZXBvcnQiLCJiaW5kIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7QUFDQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLHFCQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekIsYUFBU0MsWUFBVCxDQUFzQkMsT0FBdEIsRUFBK0JDLGdCQUEvQixFQUFpRDtBQUMvQztBQUNBLFVBQUlBLGlCQUFpQkQsT0FBakIsRUFBMEJFLElBQTFCLEtBQW1DLFNBQXZDLEVBQWtEOztBQUVsRCxVQUFJQyxjQUFjLGlDQUFrQkwsT0FBbEIsQ0FBbEI7O0FBRUEsVUFBSU0sVUFBVUMsb0JBQVFDLEdBQVIsQ0FBWUgsWUFBWUksTUFBWixDQUFtQkMsS0FBL0IsRUFBc0NWLE9BQXRDLENBQWQ7QUFDQSxVQUFJTSxXQUFXLElBQWYsRUFBcUI7O0FBRXJCLFVBQUlBLFFBQVFLLE1BQVIsQ0FBZUMsTUFBbkIsRUFBMkI7QUFDekJOLGdCQUFRTyxZQUFSLENBQXFCYixPQUFyQixFQUE4QkssV0FBOUI7QUFDQTtBQUNEOztBQUVELFVBQUlDLFFBQVFRLEdBQVIsQ0FBWSxTQUFaLEtBQ0FSLFFBQVFRLEdBQVIsQ0FBWVgsaUJBQWlCRCxPQUFqQixFQUEwQkUsSUFBdEMsQ0FESixFQUNpRDs7QUFFL0NKLGdCQUFRZSxNQUFSLENBQWVaLGdCQUFmLEVBQ0UsMkJBQTJCQSxpQkFBaUJELE9BQWpCLEVBQTBCRSxJQUFyRCxHQUNBLHNDQUZGO0FBSUQ7QUFDRjtBQUNELFdBQU87QUFDTCxnQ0FBMEJILGFBQWFlLElBQWIsQ0FBa0IsSUFBbEIsRUFBd0IsT0FBeEIsQ0FEckI7QUFFTCxnQ0FBMEJmLGFBQWFlLElBQWIsQ0FBa0IsSUFBbEIsRUFBd0IsVUFBeEI7QUFGckIsS0FBUDtBQUlEO0FBcENjLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLW5hbWVkLWFzLWRlZmF1bHQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgRXhwb3J0cyBmcm9tICcuLi9FeHBvcnRNYXAnXG5pbXBvcnQgaW1wb3J0RGVjbGFyYXRpb24gZnJvbSAnLi4vaW1wb3J0RGVjbGFyYXRpb24nXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLW5hbWVkLWFzLWRlZmF1bHQnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBjaGVja0RlZmF1bHQobmFtZUtleSwgZGVmYXVsdFNwZWNpZmllcikge1xuICAgICAgLy8gIzU2NjogZGVmYXVsdCBpcyBhIHZhbGlkIHNwZWNpZmllclxuICAgICAgaWYgKGRlZmF1bHRTcGVjaWZpZXJbbmFtZUtleV0ubmFtZSA9PT0gJ2RlZmF1bHQnKSByZXR1cm5cblxuICAgICAgdmFyIGRlY2xhcmF0aW9uID0gaW1wb3J0RGVjbGFyYXRpb24oY29udGV4dClcblxuICAgICAgdmFyIGltcG9ydHMgPSBFeHBvcnRzLmdldChkZWNsYXJhdGlvbi5zb3VyY2UudmFsdWUsIGNvbnRleHQpXG4gICAgICBpZiAoaW1wb3J0cyA9PSBudWxsKSByZXR1cm5cblxuICAgICAgaWYgKGltcG9ydHMuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICBpbXBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBkZWNsYXJhdGlvbilcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGlmIChpbXBvcnRzLmhhcygnZGVmYXVsdCcpICYmXG4gICAgICAgICAgaW1wb3J0cy5oYXMoZGVmYXVsdFNwZWNpZmllcltuYW1lS2V5XS5uYW1lKSkge1xuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KGRlZmF1bHRTcGVjaWZpZXIsXG4gICAgICAgICAgJ1VzaW5nIGV4cG9ydGVkIG5hbWUgXFwnJyArIGRlZmF1bHRTcGVjaWZpZXJbbmFtZUtleV0ubmFtZSArXG4gICAgICAgICAgJ1xcJyBhcyBpZGVudGlmaWVyIGZvciBkZWZhdWx0IGV4cG9ydC4nKVxuXG4gICAgICB9XG4gICAgfVxuICAgIHJldHVybiB7XG4gICAgICAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcic6IGNoZWNrRGVmYXVsdC5iaW5kKG51bGwsICdsb2NhbCcpLFxuICAgICAgJ0V4cG9ydERlZmF1bHRTcGVjaWZpZXInOiBjaGVja0RlZmF1bHQuYmluZChudWxsLCAnZXhwb3J0ZWQnKSxcbiAgICB9XG4gIH0sXG59XG4iXX0=