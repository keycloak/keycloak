'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('default')
    }
  },

  create: function (context) {

    function checkDefault(specifierType, node) {

      // poor man's Array.find
      let defaultSpecifier;
      node.specifiers.some(n => {
        if (n.type === specifierType) {
          defaultSpecifier = n;
          return true;
        }
      });

      if (!defaultSpecifier) return;
      var imports = _ExportMap2.default.get(node.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, node);
      } else if (imports.get('default') === undefined) {
        context.report(defaultSpecifier, 'No default export found in module.');
      }
    }

    return {
      'ImportDeclaration': checkDefault.bind(null, 'ImportDefaultSpecifier'),
      'ExportNamedDeclaration': checkDefault.bind(null, 'ExportDefaultSpecifier')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL2RlZmF1bHQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImNyZWF0ZSIsImNvbnRleHQiLCJjaGVja0RlZmF1bHQiLCJzcGVjaWZpZXJUeXBlIiwibm9kZSIsImRlZmF1bHRTcGVjaWZpZXIiLCJzcGVjaWZpZXJzIiwic29tZSIsIm4iLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwibGVuZ3RoIiwicmVwb3J0RXJyb3JzIiwidW5kZWZpbmVkIiwicmVwb3J0IiwiYmluZCJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7OztBQUNBOzs7Ozs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sU0FERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsU0FBUjtBQUREO0FBRkYsR0FEUzs7QUFRZkMsVUFBUSxVQUFVQyxPQUFWLEVBQW1COztBQUV6QixhQUFTQyxZQUFULENBQXNCQyxhQUF0QixFQUFxQ0MsSUFBckMsRUFBMkM7O0FBRXpDO0FBQ0EsVUFBSUMsZ0JBQUo7QUFDQUQsV0FBS0UsVUFBTCxDQUFnQkMsSUFBaEIsQ0FBc0JDLENBQUQsSUFBTztBQUMxQixZQUFJQSxFQUFFWCxJQUFGLEtBQVdNLGFBQWYsRUFBOEI7QUFDNUJFLDZCQUFtQkcsQ0FBbkI7QUFDQSxpQkFBTyxJQUFQO0FBQ0Q7QUFDRixPQUxEOztBQU9BLFVBQUksQ0FBQ0gsZ0JBQUwsRUFBdUI7QUFDdkIsVUFBSUksVUFBVUMsb0JBQVFDLEdBQVIsQ0FBWVAsS0FBS1EsTUFBTCxDQUFZQyxLQUF4QixFQUErQlosT0FBL0IsQ0FBZDtBQUNBLFVBQUlRLFdBQVcsSUFBZixFQUFxQjs7QUFFckIsVUFBSUEsUUFBUUssTUFBUixDQUFlQyxNQUFuQixFQUEyQjtBQUN6Qk4sZ0JBQVFPLFlBQVIsQ0FBcUJmLE9BQXJCLEVBQThCRyxJQUE5QjtBQUNELE9BRkQsTUFFTyxJQUFJSyxRQUFRRSxHQUFSLENBQVksU0FBWixNQUEyQk0sU0FBL0IsRUFBMEM7QUFDL0NoQixnQkFBUWlCLE1BQVIsQ0FBZWIsZ0JBQWYsRUFBaUMsb0NBQWpDO0FBQ0Q7QUFDRjs7QUFFRCxXQUFPO0FBQ0wsMkJBQXFCSCxhQUFhaUIsSUFBYixDQUFrQixJQUFsQixFQUF3Qix3QkFBeEIsQ0FEaEI7QUFFTCxnQ0FBMEJqQixhQUFhaUIsSUFBYixDQUFrQixJQUFsQixFQUF3Qix3QkFBeEI7QUFGckIsS0FBUDtBQUlEO0FBcENjLENBQWpCIiwiZmlsZSI6InJ1bGVzL2RlZmF1bHQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgRXhwb3J0cyBmcm9tICcuLi9FeHBvcnRNYXAnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ2RlZmF1bHQnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcblxuICAgIGZ1bmN0aW9uIGNoZWNrRGVmYXVsdChzcGVjaWZpZXJUeXBlLCBub2RlKSB7XG5cbiAgICAgIC8vIHBvb3IgbWFuJ3MgQXJyYXkuZmluZFxuICAgICAgbGV0IGRlZmF1bHRTcGVjaWZpZXJcbiAgICAgIG5vZGUuc3BlY2lmaWVycy5zb21lKChuKSA9PiB7XG4gICAgICAgIGlmIChuLnR5cGUgPT09IHNwZWNpZmllclR5cGUpIHtcbiAgICAgICAgICBkZWZhdWx0U3BlY2lmaWVyID0gblxuICAgICAgICAgIHJldHVybiB0cnVlXG4gICAgICAgIH1cbiAgICAgIH0pXG5cbiAgICAgIGlmICghZGVmYXVsdFNwZWNpZmllcikgcmV0dXJuXG4gICAgICB2YXIgaW1wb3J0cyA9IEV4cG9ydHMuZ2V0KG5vZGUuc291cmNlLnZhbHVlLCBjb250ZXh0KVxuICAgICAgaWYgKGltcG9ydHMgPT0gbnVsbCkgcmV0dXJuXG5cbiAgICAgIGlmIChpbXBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgbm9kZSlcbiAgICAgIH0gZWxzZSBpZiAoaW1wb3J0cy5nZXQoJ2RlZmF1bHQnKSA9PT0gdW5kZWZpbmVkKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KGRlZmF1bHRTcGVjaWZpZXIsICdObyBkZWZhdWx0IGV4cG9ydCBmb3VuZCBpbiBtb2R1bGUuJylcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlY2xhcmF0aW9uJzogY2hlY2tEZWZhdWx0LmJpbmQobnVsbCwgJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInKSxcbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogY2hlY2tEZWZhdWx0LmJpbmQobnVsbCwgJ0V4cG9ydERlZmF1bHRTcGVjaWZpZXInKSxcbiAgICB9XG4gIH0sXG59XG4iXX0=