'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isNonExportStatement(_ref) {
  let type = _ref.type;

  return type !== 'ExportDefaultDeclaration' && type !== 'ExportNamedDeclaration' && type !== 'ExportAllDeclaration';
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('exports-last')
    }
  },

  create: function (context) {
    return {
      Program: function (_ref2) {
        let body = _ref2.body;

        const lastNonExportStatementIndex = body.reduce(function findLastIndex(acc, item, index) {
          if (isNonExportStatement(item)) {
            return index;
          }
          return acc;
        }, -1);

        if (lastNonExportStatementIndex !== -1) {
          body.slice(0, lastNonExportStatementIndex).forEach(function checkNonExport(node) {
            if (!isNonExportStatement(node)) {
              context.report({
                node,
                message: 'Export statements should appear at the end of the file'
              });
            }
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL2V4cG9ydHMtbGFzdC5qcyJdLCJuYW1lcyI6WyJpc05vbkV4cG9ydFN0YXRlbWVudCIsInR5cGUiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJjcmVhdGUiLCJjb250ZXh0IiwiUHJvZ3JhbSIsImJvZHkiLCJsYXN0Tm9uRXhwb3J0U3RhdGVtZW50SW5kZXgiLCJyZWR1Y2UiLCJmaW5kTGFzdEluZGV4IiwiYWNjIiwiaXRlbSIsImluZGV4Iiwic2xpY2UiLCJmb3JFYWNoIiwiY2hlY2tOb25FeHBvcnQiLCJub2RlIiwicmVwb3J0IiwibWVzc2FnZSJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0FBRUEsU0FBU0Esb0JBQVQsT0FBd0M7QUFBQSxNQUFSQyxJQUFRLFFBQVJBLElBQVE7O0FBQ3RDLFNBQU9BLFNBQVMsMEJBQVQsSUFDTEEsU0FBUyx3QkFESixJQUVMQSxTQUFTLHNCQUZYO0FBR0Q7O0FBRURDLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKSCxVQUFNLFlBREY7QUFFSkksVUFBTTtBQUNKQyxXQUFLLHVCQUFRLGNBQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixXQUFPO0FBQ0xDLGVBQVMsaUJBQW9CO0FBQUEsWUFBUkMsSUFBUSxTQUFSQSxJQUFROztBQUMzQixjQUFNQyw4QkFBOEJELEtBQUtFLE1BQUwsQ0FBWSxTQUFTQyxhQUFULENBQXVCQyxHQUF2QixFQUE0QkMsSUFBNUIsRUFBa0NDLEtBQWxDLEVBQXlDO0FBQ3ZGLGNBQUloQixxQkFBcUJlLElBQXJCLENBQUosRUFBZ0M7QUFDOUIsbUJBQU9DLEtBQVA7QUFDRDtBQUNELGlCQUFPRixHQUFQO0FBQ0QsU0FMbUMsRUFLakMsQ0FBQyxDQUxnQyxDQUFwQzs7QUFPQSxZQUFJSCxnQ0FBZ0MsQ0FBQyxDQUFyQyxFQUF3QztBQUN0Q0QsZUFBS08sS0FBTCxDQUFXLENBQVgsRUFBY04sMkJBQWQsRUFBMkNPLE9BQTNDLENBQW1ELFNBQVNDLGNBQVQsQ0FBd0JDLElBQXhCLEVBQThCO0FBQy9FLGdCQUFJLENBQUNwQixxQkFBcUJvQixJQUFyQixDQUFMLEVBQWlDO0FBQy9CWixzQkFBUWEsTUFBUixDQUFlO0FBQ2JELG9CQURhO0FBRWJFLHlCQUFTO0FBRkksZUFBZjtBQUlEO0FBQ0YsV0FQRDtBQVFEO0FBQ0Y7QUFuQkksS0FBUDtBQXFCRDtBQTlCYyxDQUFqQiIsImZpbGUiOiJydWxlcy9leHBvcnRzLWxhc3QuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5mdW5jdGlvbiBpc05vbkV4cG9ydFN0YXRlbWVudCh7IHR5cGUgfSkge1xuICByZXR1cm4gdHlwZSAhPT0gJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbicgJiZcbiAgICB0eXBlICE9PSAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbicgJiZcbiAgICB0eXBlICE9PSAnRXhwb3J0QWxsRGVjbGFyYXRpb24nXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnZXhwb3J0cy1sYXN0JyksXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgIFByb2dyYW06IGZ1bmN0aW9uICh7IGJvZHkgfSkge1xuICAgICAgICBjb25zdCBsYXN0Tm9uRXhwb3J0U3RhdGVtZW50SW5kZXggPSBib2R5LnJlZHVjZShmdW5jdGlvbiBmaW5kTGFzdEluZGV4KGFjYywgaXRlbSwgaW5kZXgpIHtcbiAgICAgICAgICBpZiAoaXNOb25FeHBvcnRTdGF0ZW1lbnQoaXRlbSkpIHtcbiAgICAgICAgICAgIHJldHVybiBpbmRleFxuICAgICAgICAgIH1cbiAgICAgICAgICByZXR1cm4gYWNjXG4gICAgICAgIH0sIC0xKVxuXG4gICAgICAgIGlmIChsYXN0Tm9uRXhwb3J0U3RhdGVtZW50SW5kZXggIT09IC0xKSB7XG4gICAgICAgICAgYm9keS5zbGljZSgwLCBsYXN0Tm9uRXhwb3J0U3RhdGVtZW50SW5kZXgpLmZvckVhY2goZnVuY3Rpb24gY2hlY2tOb25FeHBvcnQobm9kZSkge1xuICAgICAgICAgICAgaWYgKCFpc05vbkV4cG9ydFN0YXRlbWVudChub2RlKSkge1xuICAgICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgICBtZXNzYWdlOiAnRXhwb3J0IHN0YXRlbWVudHMgc2hvdWxkIGFwcGVhciBhdCB0aGUgZW5kIG9mIHRoZSBmaWxlJyxcbiAgICAgICAgICAgICAgfSlcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9KVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==