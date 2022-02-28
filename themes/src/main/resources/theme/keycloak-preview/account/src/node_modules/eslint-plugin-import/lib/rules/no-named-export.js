'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2.default)('no-named-export') }
  },

  create(context) {
    // ignore non-modules
    if (context.parserOptions.sourceType !== 'module') {
      return {};
    }

    const message = 'Named exports are not allowed.';

    return {
      ExportAllDeclaration(node) {
        context.report({ node, message });
      },

      ExportNamedDeclaration(node) {
        if (node.specifiers.length === 0) {
          return context.report({ node, message });
        }

        const someNamed = node.specifiers.some(specifier => specifier.exported.name !== 'default');
        if (someNamed) {
          context.report({ node, message });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5hbWVkLWV4cG9ydC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsInBhcnNlck9wdGlvbnMiLCJzb3VyY2VUeXBlIiwibWVzc2FnZSIsIkV4cG9ydEFsbERlY2xhcmF0aW9uIiwibm9kZSIsInJlcG9ydCIsIkV4cG9ydE5hbWVkRGVjbGFyYXRpb24iLCJzcGVjaWZpZXJzIiwibGVuZ3RoIiwic29tZU5hbWVkIiwic29tZSIsInNwZWNpZmllciIsImV4cG9ydGVkIiwibmFtZSJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTSxFQUFFQyxLQUFLLHVCQUFRLGlCQUFSLENBQVA7QUFGRixHQURTOztBQU1mQyxTQUFPQyxPQUFQLEVBQWdCO0FBQ2Q7QUFDQSxRQUFJQSxRQUFRQyxhQUFSLENBQXNCQyxVQUF0QixLQUFxQyxRQUF6QyxFQUFtRDtBQUNqRCxhQUFPLEVBQVA7QUFDRDs7QUFFRCxVQUFNQyxVQUFVLGdDQUFoQjs7QUFFQSxXQUFPO0FBQ0xDLDJCQUFxQkMsSUFBckIsRUFBMkI7QUFDekJMLGdCQUFRTSxNQUFSLENBQWUsRUFBQ0QsSUFBRCxFQUFPRixPQUFQLEVBQWY7QUFDRCxPQUhJOztBQUtMSSw2QkFBdUJGLElBQXZCLEVBQTZCO0FBQzNCLFlBQUlBLEtBQUtHLFVBQUwsQ0FBZ0JDLE1BQWhCLEtBQTJCLENBQS9CLEVBQWtDO0FBQ2hDLGlCQUFPVCxRQUFRTSxNQUFSLENBQWUsRUFBQ0QsSUFBRCxFQUFPRixPQUFQLEVBQWYsQ0FBUDtBQUNEOztBQUVELGNBQU1PLFlBQVlMLEtBQUtHLFVBQUwsQ0FBZ0JHLElBQWhCLENBQXFCQyxhQUFhQSxVQUFVQyxRQUFWLENBQW1CQyxJQUFuQixLQUE0QixTQUE5RCxDQUFsQjtBQUNBLFlBQUlKLFNBQUosRUFBZTtBQUNiVixrQkFBUU0sTUFBUixDQUFlLEVBQUNELElBQUQsRUFBT0YsT0FBUCxFQUFmO0FBQ0Q7QUFDRjtBQWRJLEtBQVA7QUFnQkQ7QUE5QmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tbmFtZWQtZXhwb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczogeyB1cmw6IGRvY3NVcmwoJ25vLW5hbWVkLWV4cG9ydCcpIH0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICAvLyBpZ25vcmUgbm9uLW1vZHVsZXNcbiAgICBpZiAoY29udGV4dC5wYXJzZXJPcHRpb25zLnNvdXJjZVR5cGUgIT09ICdtb2R1bGUnKSB7XG4gICAgICByZXR1cm4ge31cbiAgICB9XG5cbiAgICBjb25zdCBtZXNzYWdlID0gJ05hbWVkIGV4cG9ydHMgYXJlIG5vdCBhbGxvd2VkLidcblxuICAgIHJldHVybiB7XG4gICAgICBFeHBvcnRBbGxEZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtub2RlLCBtZXNzYWdlfSlcbiAgICAgIH0sXG5cbiAgICAgIEV4cG9ydE5hbWVkRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBpZiAobm9kZS5zcGVjaWZpZXJzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAgIHJldHVybiBjb250ZXh0LnJlcG9ydCh7bm9kZSwgbWVzc2FnZX0pXG4gICAgICAgIH1cblxuICAgICAgICBjb25zdCBzb21lTmFtZWQgPSBub2RlLnNwZWNpZmllcnMuc29tZShzcGVjaWZpZXIgPT4gc3BlY2lmaWVyLmV4cG9ydGVkLm5hbWUgIT09ICdkZWZhdWx0JylcbiAgICAgICAgaWYgKHNvbWVOYW1lZCkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtub2RlLCBtZXNzYWdlfSlcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=