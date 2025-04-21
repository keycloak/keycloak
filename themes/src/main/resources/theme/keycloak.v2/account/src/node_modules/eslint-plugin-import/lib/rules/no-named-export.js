'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2['default'])('no-named-export') },
    schema: [] },


  create: function () {function create(context) {
      // ignore non-modules
      if (context.parserOptions.sourceType !== 'module') {
        return {};
      }

      var message = 'Named exports are not allowed.';

      return {
        ExportAllDeclaration: function () {function ExportAllDeclaration(node) {
            context.report({ node: node, message: message });
          }return ExportAllDeclaration;}(),

        ExportNamedDeclaration: function () {function ExportNamedDeclaration(node) {
            if (node.specifiers.length === 0) {
              return context.report({ node: node, message: message });
            }

            var someNamed = node.specifiers.some(function (specifier) {return (specifier.exported.name || specifier.exported.value) !== 'default';});
            if (someNamed) {
              context.report({ node: node, message: message });
            }
          }return ExportNamedDeclaration;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lZC1leHBvcnQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJwYXJzZXJPcHRpb25zIiwic291cmNlVHlwZSIsIm1lc3NhZ2UiLCJFeHBvcnRBbGxEZWNsYXJhdGlvbiIsIm5vZGUiLCJyZXBvcnQiLCJFeHBvcnROYW1lZERlY2xhcmF0aW9uIiwic3BlY2lmaWVycyIsImxlbmd0aCIsInNvbWVOYW1lZCIsInNvbWUiLCJzcGVjaWZpZXIiLCJleHBvcnRlZCIsIm5hbWUiLCJ2YWx1ZSJdLCJtYXBwaW5ncyI6ImFBQUEscUM7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTSxFQUFFQyxLQUFLLDBCQUFRLGlCQUFSLENBQVAsRUFGRjtBQUdKQyxZQUFRLEVBSEosRUFEUzs7O0FBT2ZDLFFBUGUsK0JBT1JDLE9BUFEsRUFPQztBQUNkO0FBQ0EsVUFBSUEsUUFBUUMsYUFBUixDQUFzQkMsVUFBdEIsS0FBcUMsUUFBekMsRUFBbUQ7QUFDakQsZUFBTyxFQUFQO0FBQ0Q7O0FBRUQsVUFBTUMsVUFBVSxnQ0FBaEI7O0FBRUEsYUFBTztBQUNMQyw0QkFESyw2Q0FDZ0JDLElBRGhCLEVBQ3NCO0FBQ3pCTCxvQkFBUU0sTUFBUixDQUFlLEVBQUVELFVBQUYsRUFBUUYsZ0JBQVIsRUFBZjtBQUNELFdBSEk7O0FBS0xJLDhCQUxLLCtDQUtrQkYsSUFMbEIsRUFLd0I7QUFDM0IsZ0JBQUlBLEtBQUtHLFVBQUwsQ0FBZ0JDLE1BQWhCLEtBQTJCLENBQS9CLEVBQWtDO0FBQ2hDLHFCQUFPVCxRQUFRTSxNQUFSLENBQWUsRUFBRUQsVUFBRixFQUFRRixnQkFBUixFQUFmLENBQVA7QUFDRDs7QUFFRCxnQkFBTU8sWUFBWUwsS0FBS0csVUFBTCxDQUFnQkcsSUFBaEIsQ0FBcUIsNkJBQWEsQ0FBQ0MsVUFBVUMsUUFBVixDQUFtQkMsSUFBbkIsSUFBMkJGLFVBQVVDLFFBQVYsQ0FBbUJFLEtBQS9DLE1BQTBELFNBQXZFLEVBQXJCLENBQWxCO0FBQ0EsZ0JBQUlMLFNBQUosRUFBZTtBQUNiVixzQkFBUU0sTUFBUixDQUFlLEVBQUVELFVBQUYsRUFBUUYsZ0JBQVIsRUFBZjtBQUNEO0FBQ0YsV0FkSSxtQ0FBUDs7QUFnQkQsS0EvQmMsbUJBQWpCIiwiZmlsZSI6Im5vLW5hbWVkLWV4cG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7IHVybDogZG9jc1VybCgnbm8tbmFtZWQtZXhwb3J0JykgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgLy8gaWdub3JlIG5vbi1tb2R1bGVzXG4gICAgaWYgKGNvbnRleHQucGFyc2VyT3B0aW9ucy5zb3VyY2VUeXBlICE9PSAnbW9kdWxlJykge1xuICAgICAgcmV0dXJuIHt9O1xuICAgIH1cblxuICAgIGNvbnN0IG1lc3NhZ2UgPSAnTmFtZWQgZXhwb3J0cyBhcmUgbm90IGFsbG93ZWQuJztcblxuICAgIHJldHVybiB7XG4gICAgICBFeHBvcnRBbGxEZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZSB9KTtcbiAgICAgIH0sXG5cbiAgICAgIEV4cG9ydE5hbWVkRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBpZiAobm9kZS5zcGVjaWZpZXJzLmxlbmd0aCA9PT0gMCkge1xuICAgICAgICAgIHJldHVybiBjb250ZXh0LnJlcG9ydCh7IG5vZGUsIG1lc3NhZ2UgfSk7XG4gICAgICAgIH1cblxuICAgICAgICBjb25zdCBzb21lTmFtZWQgPSBub2RlLnNwZWNpZmllcnMuc29tZShzcGVjaWZpZXIgPT4gKHNwZWNpZmllci5leHBvcnRlZC5uYW1lIHx8IHNwZWNpZmllci5leHBvcnRlZC52YWx1ZSkgIT09ICdkZWZhdWx0Jyk7XG4gICAgICAgIGlmIChzb21lTmFtZWQpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7IG5vZGUsIG1lc3NhZ2UgfSk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=