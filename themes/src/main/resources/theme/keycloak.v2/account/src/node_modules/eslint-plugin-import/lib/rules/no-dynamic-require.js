'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function isRequire(node) {
  return node &&
  node.callee &&
  node.callee.type === 'Identifier' &&
  node.callee.name === 'require' &&
  node.arguments.length >= 1;
}

function isDynamicImport(node) {
  return node &&
  node.callee &&
  node.callee.type === 'Import';
}

function isStaticValue(arg) {
  return arg.type === 'Literal' ||
  arg.type === 'TemplateLiteral' && arg.expressions.length === 0;
}

var dynamicImportErrorMessage = 'Calls to import() should use string literals';

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-dynamic-require') },

    schema: [
    {
      type: 'object',
      properties: {
        esmodule: {
          type: 'boolean' } },


      additionalProperties: false }] },




  create: function () {function create(context) {
      var options = context.options[0] || {};

      return {
        CallExpression: function () {function CallExpression(node) {
            if (!node.arguments[0] || isStaticValue(node.arguments[0])) {
              return;
            }
            if (isRequire(node)) {
              return context.report({
                node: node,
                message: 'Calls to require() should use string literals' });

            }
            if (options.esmodule && isDynamicImport(node)) {
              return context.report({
                node: node,
                message: dynamicImportErrorMessage });

            }
          }return CallExpression;}(),
        ImportExpression: function () {function ImportExpression(node) {
            if (!options.esmodule || isStaticValue(node.source)) {
              return;
            }
            return context.report({
              node: node,
              message: dynamicImportErrorMessage });

          }return ImportExpression;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1keW5hbWljLXJlcXVpcmUuanMiXSwibmFtZXMiOlsiaXNSZXF1aXJlIiwibm9kZSIsImNhbGxlZSIsInR5cGUiLCJuYW1lIiwiYXJndW1lbnRzIiwibGVuZ3RoIiwiaXNEeW5hbWljSW1wb3J0IiwiaXNTdGF0aWNWYWx1ZSIsImFyZyIsImV4cHJlc3Npb25zIiwiZHluYW1pY0ltcG9ydEVycm9yTWVzc2FnZSIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJlc21vZHVsZSIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwiY3JlYXRlIiwiY29udGV4dCIsIm9wdGlvbnMiLCJDYWxsRXhwcmVzc2lvbiIsInJlcG9ydCIsIm1lc3NhZ2UiLCJJbXBvcnRFeHByZXNzaW9uIiwic291cmNlIl0sIm1hcHBpbmdzIjoiYUFBQSxxQzs7QUFFQSxTQUFTQSxTQUFULENBQW1CQyxJQUFuQixFQUF5QjtBQUN2QixTQUFPQTtBQUNMQSxPQUFLQyxNQURBO0FBRUxELE9BQUtDLE1BQUwsQ0FBWUMsSUFBWixLQUFxQixZQUZoQjtBQUdMRixPQUFLQyxNQUFMLENBQVlFLElBQVosS0FBcUIsU0FIaEI7QUFJTEgsT0FBS0ksU0FBTCxDQUFlQyxNQUFmLElBQXlCLENBSjNCO0FBS0Q7O0FBRUQsU0FBU0MsZUFBVCxDQUF5Qk4sSUFBekIsRUFBK0I7QUFDN0IsU0FBT0E7QUFDTEEsT0FBS0MsTUFEQTtBQUVMRCxPQUFLQyxNQUFMLENBQVlDLElBQVosS0FBcUIsUUFGdkI7QUFHRDs7QUFFRCxTQUFTSyxhQUFULENBQXVCQyxHQUF2QixFQUE0QjtBQUMxQixTQUFPQSxJQUFJTixJQUFKLEtBQWEsU0FBYjtBQUNKTSxNQUFJTixJQUFKLEtBQWEsaUJBQWIsSUFBa0NNLElBQUlDLFdBQUosQ0FBZ0JKLE1BQWhCLEtBQTJCLENBRGhFO0FBRUQ7O0FBRUQsSUFBTUssNEJBQTRCLDhDQUFsQzs7QUFFQUMsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pYLFVBQU0sWUFERjtBQUVKWSxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsb0JBQVIsQ0FERCxFQUZGOztBQUtKQyxZQUFRO0FBQ047QUFDRWQsWUFBTSxRQURSO0FBRUVlLGtCQUFZO0FBQ1ZDLGtCQUFVO0FBQ1JoQixnQkFBTSxTQURFLEVBREEsRUFGZDs7O0FBT0VpQiw0QkFBc0IsS0FQeEIsRUFETSxDQUxKLEVBRFM7Ozs7O0FBbUJmQyxRQW5CZSwrQkFtQlJDLE9BbkJRLEVBbUJDO0FBQ2QsVUFBTUMsVUFBVUQsUUFBUUMsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUF0Qzs7QUFFQSxhQUFPO0FBQ0xDLHNCQURLLHVDQUNVdkIsSUFEVixFQUNnQjtBQUNuQixnQkFBSSxDQUFDQSxLQUFLSSxTQUFMLENBQWUsQ0FBZixDQUFELElBQXNCRyxjQUFjUCxLQUFLSSxTQUFMLENBQWUsQ0FBZixDQUFkLENBQTFCLEVBQTREO0FBQzFEO0FBQ0Q7QUFDRCxnQkFBSUwsVUFBVUMsSUFBVixDQUFKLEVBQXFCO0FBQ25CLHFCQUFPcUIsUUFBUUcsTUFBUixDQUFlO0FBQ3BCeEIsMEJBRG9CO0FBRXBCeUIseUJBQVMsK0NBRlcsRUFBZixDQUFQOztBQUlEO0FBQ0QsZ0JBQUlILFFBQVFKLFFBQVIsSUFBb0JaLGdCQUFnQk4sSUFBaEIsQ0FBeEIsRUFBK0M7QUFDN0MscUJBQU9xQixRQUFRRyxNQUFSLENBQWU7QUFDcEJ4QiwwQkFEb0I7QUFFcEJ5Qix5QkFBU2YseUJBRlcsRUFBZixDQUFQOztBQUlEO0FBQ0YsV0FqQkk7QUFrQkxnQix3QkFsQksseUNBa0JZMUIsSUFsQlosRUFrQmtCO0FBQ3JCLGdCQUFJLENBQUNzQixRQUFRSixRQUFULElBQXFCWCxjQUFjUCxLQUFLMkIsTUFBbkIsQ0FBekIsRUFBcUQ7QUFDbkQ7QUFDRDtBQUNELG1CQUFPTixRQUFRRyxNQUFSLENBQWU7QUFDcEJ4Qix3QkFEb0I7QUFFcEJ5Qix1QkFBU2YseUJBRlcsRUFBZixDQUFQOztBQUlELFdBMUJJLDZCQUFQOztBQTRCRCxLQWxEYyxtQkFBakIiLCJmaWxlIjoibm8tZHluYW1pYy1yZXF1aXJlLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5cbmZ1bmN0aW9uIGlzUmVxdWlyZShub2RlKSB7XG4gIHJldHVybiBub2RlICYmXG4gICAgbm9kZS5jYWxsZWUgJiZcbiAgICBub2RlLmNhbGxlZS50eXBlID09PSAnSWRlbnRpZmllcicgJiZcbiAgICBub2RlLmNhbGxlZS5uYW1lID09PSAncmVxdWlyZScgJiZcbiAgICBub2RlLmFyZ3VtZW50cy5sZW5ndGggPj0gMTtcbn1cblxuZnVuY3Rpb24gaXNEeW5hbWljSW1wb3J0KG5vZGUpIHtcbiAgcmV0dXJuIG5vZGUgJiZcbiAgICBub2RlLmNhbGxlZSAmJlxuICAgIG5vZGUuY2FsbGVlLnR5cGUgPT09ICdJbXBvcnQnO1xufVxuXG5mdW5jdGlvbiBpc1N0YXRpY1ZhbHVlKGFyZykge1xuICByZXR1cm4gYXJnLnR5cGUgPT09ICdMaXRlcmFsJyB8fFxuICAgIChhcmcudHlwZSA9PT0gJ1RlbXBsYXRlTGl0ZXJhbCcgJiYgYXJnLmV4cHJlc3Npb25zLmxlbmd0aCA9PT0gMCk7XG59XG5cbmNvbnN0IGR5bmFtaWNJbXBvcnRFcnJvck1lc3NhZ2UgPSAnQ2FsbHMgdG8gaW1wb3J0KCkgc2hvdWxkIHVzZSBzdHJpbmcgbGl0ZXJhbHMnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWR5bmFtaWMtcmVxdWlyZScpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgZXNtb2R1bGU6IHtcbiAgICAgICAgICAgIHR5cGU6ICdib29sZWFuJyxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9O1xuXG4gICAgcmV0dXJuIHtcbiAgICAgIENhbGxFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKCFub2RlLmFyZ3VtZW50c1swXSB8fCBpc1N0YXRpY1ZhbHVlKG5vZGUuYXJndW1lbnRzWzBdKSkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAoaXNSZXF1aXJlKG5vZGUpKSB7XG4gICAgICAgICAgcmV0dXJuIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiAnQ2FsbHMgdG8gcmVxdWlyZSgpIHNob3VsZCB1c2Ugc3RyaW5nIGxpdGVyYWxzJyxcbiAgICAgICAgICB9KTtcbiAgICAgICAgfVxuICAgICAgICBpZiAob3B0aW9ucy5lc21vZHVsZSAmJiBpc0R5bmFtaWNJbXBvcnQobm9kZSkpIHtcbiAgICAgICAgICByZXR1cm4gY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGR5bmFtaWNJbXBvcnRFcnJvck1lc3NhZ2UsXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgICBJbXBvcnRFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKCFvcHRpb25zLmVzbW9kdWxlIHx8IGlzU3RhdGljVmFsdWUobm9kZS5zb3VyY2UpKSB7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG4gICAgICAgIHJldHVybiBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZSxcbiAgICAgICAgICBtZXNzYWdlOiBkeW5hbWljSW1wb3J0RXJyb3JNZXNzYWdlLFxuICAgICAgICB9KTtcbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=