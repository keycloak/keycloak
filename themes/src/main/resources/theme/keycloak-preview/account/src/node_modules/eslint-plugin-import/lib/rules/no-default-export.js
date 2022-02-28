'use strict';

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {}
  },

  create(context) {
    // ignore non-modules
    if (context.parserOptions.sourceType !== 'module') {
      return {};
    }

    const preferNamed = 'Prefer named exports.';
    const noAliasDefault = (_ref) => {
      let local = _ref.local;
      return `Do not alias \`${local.name}\` as \`default\`. Just export ` + `\`${local.name}\` itself instead.`;
    };

    return {
      ExportDefaultDeclaration(node) {
        context.report({ node, message: preferNamed });
      },

      ExportNamedDeclaration(node) {
        node.specifiers.forEach(specifier => {
          if (specifier.type === 'ExportDefaultSpecifier' && specifier.exported.name === 'default') {
            context.report({ node, message: preferNamed });
          } else if (specifier.type === 'ExportSpecifier' && specifier.exported.name === 'default') {
            context.report({ node, message: noAliasDefault(specifier) });
          }
        });
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWRlZmF1bHQtZXhwb3J0LmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJjcmVhdGUiLCJjb250ZXh0IiwicGFyc2VyT3B0aW9ucyIsInNvdXJjZVR5cGUiLCJwcmVmZXJOYW1lZCIsIm5vQWxpYXNEZWZhdWx0IiwibG9jYWwiLCJuYW1lIiwiRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uIiwibm9kZSIsInJlcG9ydCIsIm1lc3NhZ2UiLCJFeHBvcnROYW1lZERlY2xhcmF0aW9uIiwic3BlY2lmaWVycyIsImZvckVhY2giLCJzcGVjaWZpZXIiLCJleHBvcnRlZCJdLCJtYXBwaW5ncyI6Ijs7QUFBQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBRkYsR0FEUzs7QUFNZkMsU0FBT0MsT0FBUCxFQUFnQjtBQUNkO0FBQ0EsUUFBSUEsUUFBUUMsYUFBUixDQUFzQkMsVUFBdEIsS0FBcUMsUUFBekMsRUFBbUQ7QUFDakQsYUFBTyxFQUFQO0FBQ0Q7O0FBRUQsVUFBTUMsY0FBYyx1QkFBcEI7QUFDQSxVQUFNQyxpQkFBaUI7QUFBQSxVQUFFQyxLQUFGLFFBQUVBLEtBQUY7QUFBQSxhQUNwQixrQkFBaUJBLE1BQU1DLElBQUssaUNBQTdCLEdBQ0MsS0FBSUQsTUFBTUMsSUFBSyxvQkFGSztBQUFBLEtBQXZCOztBQUlBLFdBQU87QUFDTEMsK0JBQXlCQyxJQUF6QixFQUErQjtBQUM3QlIsZ0JBQVFTLE1BQVIsQ0FBZSxFQUFDRCxJQUFELEVBQU9FLFNBQVNQLFdBQWhCLEVBQWY7QUFDRCxPQUhJOztBQUtMUSw2QkFBdUJILElBQXZCLEVBQTZCO0FBQzNCQSxhQUFLSSxVQUFMLENBQWdCQyxPQUFoQixDQUF3QkMsYUFBYTtBQUNuQyxjQUFJQSxVQUFVakIsSUFBVixLQUFtQix3QkFBbkIsSUFDQWlCLFVBQVVDLFFBQVYsQ0FBbUJULElBQW5CLEtBQTRCLFNBRGhDLEVBQzJDO0FBQ3pDTixvQkFBUVMsTUFBUixDQUFlLEVBQUNELElBQUQsRUFBT0UsU0FBU1AsV0FBaEIsRUFBZjtBQUNELFdBSEQsTUFHTyxJQUFJVyxVQUFVakIsSUFBVixLQUFtQixpQkFBbkIsSUFDUGlCLFVBQVVDLFFBQVYsQ0FBbUJULElBQW5CLEtBQTRCLFNBRHpCLEVBQ29DO0FBQ3pDTixvQkFBUVMsTUFBUixDQUFlLEVBQUNELElBQUQsRUFBT0UsU0FBU04sZUFBZVUsU0FBZixDQUFoQixFQUFmO0FBQ0Q7QUFDRixTQVJEO0FBU0Q7QUFmSSxLQUFQO0FBaUJEO0FBbENjLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLWRlZmF1bHQtZXhwb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsibW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge30sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICAvLyBpZ25vcmUgbm9uLW1vZHVsZXNcbiAgICBpZiAoY29udGV4dC5wYXJzZXJPcHRpb25zLnNvdXJjZVR5cGUgIT09ICdtb2R1bGUnKSB7XG4gICAgICByZXR1cm4ge31cbiAgICB9XG5cbiAgICBjb25zdCBwcmVmZXJOYW1lZCA9ICdQcmVmZXIgbmFtZWQgZXhwb3J0cy4nXG4gICAgY29uc3Qgbm9BbGlhc0RlZmF1bHQgPSAoe2xvY2FsfSkgPT5cbiAgICAgIGBEbyBub3QgYWxpYXMgXFxgJHtsb2NhbC5uYW1lfVxcYCBhcyBcXGBkZWZhdWx0XFxgLiBKdXN0IGV4cG9ydCBgICtcbiAgICAgIGBcXGAke2xvY2FsLm5hbWV9XFxgIGl0c2VsZiBpbnN0ZWFkLmBcblxuICAgIHJldHVybiB7XG4gICAgICBFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7bm9kZSwgbWVzc2FnZTogcHJlZmVyTmFtZWR9KVxuICAgICAgfSxcblxuICAgICAgRXhwb3J0TmFtZWREZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIG5vZGUuc3BlY2lmaWVycy5mb3JFYWNoKHNwZWNpZmllciA9PiB7XG4gICAgICAgICAgaWYgKHNwZWNpZmllci50eXBlID09PSAnRXhwb3J0RGVmYXVsdFNwZWNpZmllcicgJiZcbiAgICAgICAgICAgICAgc3BlY2lmaWVyLmV4cG9ydGVkLm5hbWUgPT09ICdkZWZhdWx0Jykge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoe25vZGUsIG1lc3NhZ2U6IHByZWZlck5hbWVkfSlcbiAgICAgICAgICB9IGVsc2UgaWYgKHNwZWNpZmllci50eXBlID09PSAnRXhwb3J0U3BlY2lmaWVyJyAmJlxuICAgICAgICAgICAgICBzcGVjaWZpZXIuZXhwb3J0ZWQubmFtZSA9PT0gJ2RlZmF1bHQnKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7bm9kZSwgbWVzc2FnZTogbm9BbGlhc0RlZmF1bHQoc3BlY2lmaWVyKX0pXG4gICAgICAgICAgfVxuICAgICAgICB9KVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=