'use strict';

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function reportIfNonStandard(context, node, name) {
  if (name.indexOf('!') !== -1) {
    context.report(node, `Unexpected '!' in '${name}'. ` + 'Do not use import syntax to configure webpack loaders.');
  }
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-webpack-loader-syntax')
    }
  },

  create: function (context) {
    return {
      ImportDeclaration: function handleImports(node) {
        reportIfNonStandard(context, node, node.source.value);
      },
      CallExpression: function handleRequires(node) {
        if ((0, _staticRequire2.default)(node)) {
          reportIfNonStandard(context, node, node.arguments[0].value);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXdlYnBhY2stbG9hZGVyLXN5bnRheC5qcyJdLCJuYW1lcyI6WyJyZXBvcnRJZk5vblN0YW5kYXJkIiwiY29udGV4dCIsIm5vZGUiLCJuYW1lIiwiaW5kZXhPZiIsInJlcG9ydCIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJjcmVhdGUiLCJJbXBvcnREZWNsYXJhdGlvbiIsImhhbmRsZUltcG9ydHMiLCJzb3VyY2UiLCJ2YWx1ZSIsIkNhbGxFeHByZXNzaW9uIiwiaGFuZGxlUmVxdWlyZXMiLCJhcmd1bWVudHMiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7QUFDQTs7Ozs7O0FBRUEsU0FBU0EsbUJBQVQsQ0FBNkJDLE9BQTdCLEVBQXNDQyxJQUF0QyxFQUE0Q0MsSUFBNUMsRUFBa0Q7QUFDaEQsTUFBSUEsS0FBS0MsT0FBTCxDQUFhLEdBQWIsTUFBc0IsQ0FBQyxDQUEzQixFQUE4QjtBQUM1QkgsWUFBUUksTUFBUixDQUFlSCxJQUFmLEVBQXNCLHNCQUFxQkMsSUFBSyxLQUEzQixHQUNuQix3REFERjtBQUdEO0FBQ0Y7O0FBRURHLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLDBCQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVYLE9BQVYsRUFBbUI7QUFDekIsV0FBTztBQUNMWSx5QkFBbUIsU0FBU0MsYUFBVCxDQUF1QlosSUFBdkIsRUFBNkI7QUFDOUNGLDRCQUFvQkMsT0FBcEIsRUFBNkJDLElBQTdCLEVBQW1DQSxLQUFLYSxNQUFMLENBQVlDLEtBQS9DO0FBQ0QsT0FISTtBQUlMQyxzQkFBZ0IsU0FBU0MsY0FBVCxDQUF3QmhCLElBQXhCLEVBQThCO0FBQzVDLFlBQUksNkJBQWdCQSxJQUFoQixDQUFKLEVBQTJCO0FBQ3pCRiw4QkFBb0JDLE9BQXBCLEVBQTZCQyxJQUE3QixFQUFtQ0EsS0FBS2lCLFNBQUwsQ0FBZSxDQUFmLEVBQWtCSCxLQUFyRDtBQUNEO0FBQ0Y7QUFSSSxLQUFQO0FBVUQ7QUFuQmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8td2VicGFjay1sb2FkZXItc3ludGF4LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGlzU3RhdGljUmVxdWlyZSBmcm9tICcuLi9jb3JlL3N0YXRpY1JlcXVpcmUnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5mdW5jdGlvbiByZXBvcnRJZk5vblN0YW5kYXJkKGNvbnRleHQsIG5vZGUsIG5hbWUpIHtcbiAgaWYgKG5hbWUuaW5kZXhPZignIScpICE9PSAtMSkge1xuICAgIGNvbnRleHQucmVwb3J0KG5vZGUsIGBVbmV4cGVjdGVkICchJyBpbiAnJHtuYW1lfScuIGAgK1xuICAgICAgJ0RvIG5vdCB1c2UgaW1wb3J0IHN5bnRheCB0byBjb25maWd1cmUgd2VicGFjayBsb2FkZXJzLidcbiAgICApXG4gIH1cbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby13ZWJwYWNrLWxvYWRlci1zeW50YXgnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgSW1wb3J0RGVjbGFyYXRpb246IGZ1bmN0aW9uIGhhbmRsZUltcG9ydHMobm9kZSkge1xuICAgICAgICByZXBvcnRJZk5vblN0YW5kYXJkKGNvbnRleHQsIG5vZGUsIG5vZGUuc291cmNlLnZhbHVlKVxuICAgICAgfSxcbiAgICAgIENhbGxFeHByZXNzaW9uOiBmdW5jdGlvbiBoYW5kbGVSZXF1aXJlcyhub2RlKSB7XG4gICAgICAgIGlmIChpc1N0YXRpY1JlcXVpcmUobm9kZSkpIHtcbiAgICAgICAgICByZXBvcnRJZk5vblN0YW5kYXJkKGNvbnRleHQsIG5vZGUsIG5vZGUuYXJndW1lbnRzWzBdLnZhbHVlKVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==