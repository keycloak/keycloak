'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isRequire(node) {
  return node && node.callee && node.callee.type === 'Identifier' && node.callee.name === 'require' && node.arguments.length >= 1;
}

function isStaticValue(arg) {
  return arg.type === 'Literal' || arg.type === 'TemplateLiteral' && arg.expressions.length === 0;
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-dynamic-require')
    }
  },

  create: function (context) {
    return {
      CallExpression(node) {
        if (isRequire(node) && !isStaticValue(node.arguments[0])) {
          context.report({
            node,
            message: 'Calls to require() should use string literals'
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWR5bmFtaWMtcmVxdWlyZS5qcyJdLCJuYW1lcyI6WyJpc1JlcXVpcmUiLCJub2RlIiwiY2FsbGVlIiwidHlwZSIsIm5hbWUiLCJhcmd1bWVudHMiLCJsZW5ndGgiLCJpc1N0YXRpY1ZhbHVlIiwiYXJnIiwiZXhwcmVzc2lvbnMiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJjcmVhdGUiLCJjb250ZXh0IiwiQ2FsbEV4cHJlc3Npb24iLCJyZXBvcnQiLCJtZXNzYWdlIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7Ozs7QUFFQSxTQUFTQSxTQUFULENBQW1CQyxJQUFuQixFQUF5QjtBQUN2QixTQUFPQSxRQUNMQSxLQUFLQyxNQURBLElBRUxELEtBQUtDLE1BQUwsQ0FBWUMsSUFBWixLQUFxQixZQUZoQixJQUdMRixLQUFLQyxNQUFMLENBQVlFLElBQVosS0FBcUIsU0FIaEIsSUFJTEgsS0FBS0ksU0FBTCxDQUFlQyxNQUFmLElBQXlCLENBSjNCO0FBS0Q7O0FBRUQsU0FBU0MsYUFBVCxDQUF1QkMsR0FBdkIsRUFBNEI7QUFDMUIsU0FBT0EsSUFBSUwsSUFBSixLQUFhLFNBQWIsSUFDSkssSUFBSUwsSUFBSixLQUFhLGlCQUFiLElBQWtDSyxJQUFJQyxXQUFKLENBQWdCSCxNQUFoQixLQUEyQixDQURoRTtBQUVEOztBQUVESSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSlQsVUFBTSxZQURGO0FBRUpVLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxvQkFBUjtBQUREO0FBRkYsR0FEUzs7QUFRZkMsVUFBUSxVQUFVQyxPQUFWLEVBQW1CO0FBQ3pCLFdBQU87QUFDTEMscUJBQWVoQixJQUFmLEVBQXFCO0FBQ25CLFlBQUlELFVBQVVDLElBQVYsS0FBbUIsQ0FBQ00sY0FBY04sS0FBS0ksU0FBTCxDQUFlLENBQWYsQ0FBZCxDQUF4QixFQUEwRDtBQUN4RFcsa0JBQVFFLE1BQVIsQ0FBZTtBQUNiakIsZ0JBRGE7QUFFYmtCLHFCQUFTO0FBRkksV0FBZjtBQUlEO0FBQ0Y7QUFSSSxLQUFQO0FBVUQ7QUFuQmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tZHluYW1pYy1yZXF1aXJlLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxuZnVuY3Rpb24gaXNSZXF1aXJlKG5vZGUpIHtcbiAgcmV0dXJuIG5vZGUgJiZcbiAgICBub2RlLmNhbGxlZSAmJlxuICAgIG5vZGUuY2FsbGVlLnR5cGUgPT09ICdJZGVudGlmaWVyJyAmJlxuICAgIG5vZGUuY2FsbGVlLm5hbWUgPT09ICdyZXF1aXJlJyAmJlxuICAgIG5vZGUuYXJndW1lbnRzLmxlbmd0aCA+PSAxXG59XG5cbmZ1bmN0aW9uIGlzU3RhdGljVmFsdWUoYXJnKSB7XG4gIHJldHVybiBhcmcudHlwZSA9PT0gJ0xpdGVyYWwnIHx8XG4gICAgKGFyZy50eXBlID09PSAnVGVtcGxhdGVMaXRlcmFsJyAmJiBhcmcuZXhwcmVzc2lvbnMubGVuZ3RoID09PSAwKVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWR5bmFtaWMtcmVxdWlyZScpLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIHJldHVybiB7XG4gICAgICBDYWxsRXhwcmVzc2lvbihub2RlKSB7XG4gICAgICAgIGlmIChpc1JlcXVpcmUobm9kZSkgJiYgIWlzU3RhdGljVmFsdWUobm9kZS5hcmd1bWVudHNbMF0pKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6ICdDYWxscyB0byByZXF1aXJlKCkgc2hvdWxkIHVzZSBzdHJpbmcgbGl0ZXJhbHMnLFxuICAgICAgICAgIH0pXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19