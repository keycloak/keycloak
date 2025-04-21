'use strict';var _vm = require('vm');var _vm2 = _interopRequireDefault(_vm);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('dynamic-import-chunkname') },

    schema: [{
      type: 'object',
      properties: {
        importFunctions: {
          type: 'array',
          uniqueItems: true,
          items: {
            type: 'string' } },


        webpackChunknameFormat: {
          type: 'string' } } }] },





  create: function () {function create(context) {
      var config = context.options[0];var _ref =
      config || {},_ref$importFunctions = _ref.importFunctions,importFunctions = _ref$importFunctions === undefined ? [] : _ref$importFunctions;var _ref2 =
      config || {},_ref2$webpackChunknam = _ref2.webpackChunknameFormat,webpackChunknameFormat = _ref2$webpackChunknam === undefined ? '[0-9a-zA-Z-_/.]+' : _ref2$webpackChunknam;

      var paddedCommentRegex = /^ (\S[\s\S]+\S) $/;
      var commentStyleRegex = /^( \w+: (["'][^"']*["']|\d+|false|true),?)+ $/;
      var chunkSubstrFormat = ' webpackChunkName: ["\']' + String(webpackChunknameFormat) + '["\'],? ';
      var chunkSubstrRegex = new RegExp(chunkSubstrFormat);

      function run(node, arg) {
        var sourceCode = context.getSourceCode();
        var leadingComments = sourceCode.getCommentsBefore ?
        sourceCode.getCommentsBefore(arg) // This method is available in ESLint >= 4.
        : sourceCode.getComments(arg).leading; // This method is deprecated in ESLint 7.

        if (!leadingComments || leadingComments.length === 0) {
          context.report({
            node: node,
            message: 'dynamic imports require a leading comment with the webpack chunkname' });

          return;
        }

        var isChunknamePresent = false;var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {

          for (var _iterator = leadingComments[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var comment = _step.value;
            if (comment.type !== 'Block') {
              context.report({
                node: node,
                message: 'dynamic imports require a /* foo */ style comment, not a // foo comment' });

              return;
            }

            if (!paddedCommentRegex.test(comment.value)) {
              context.report({
                node: node,
                message: 'dynamic imports require a block comment padded with spaces - /* foo */' });

              return;
            }

            try {
              // just like webpack itself does
              _vm2['default'].runInNewContext('(function() {return {' + String(comment.value) + '}})()');
            }
            catch (error) {
              context.report({
                node: node,
                message: 'dynamic imports require a "webpack" comment with valid syntax' });

              return;
            }

            if (!commentStyleRegex.test(comment.value)) {
              context.report({
                node: node,
                message: 'dynamic imports require a leading comment in the form /*' +
                chunkSubstrFormat + '*/' });

              return;
            }

            if (chunkSubstrRegex.test(comment.value)) {
              isChunknamePresent = true;
            }
          }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}

        if (!isChunknamePresent) {
          context.report({
            node: node,
            message: 'dynamic imports require a leading comment in the form /*' +
            chunkSubstrFormat + '*/' });

        }
      }

      return {
        ImportExpression: function () {function ImportExpression(node) {
            run(node, node.source);
          }return ImportExpression;}(),

        CallExpression: function () {function CallExpression(node) {
            if (node.callee.type !== 'Import' && importFunctions.indexOf(node.callee.name) < 0) {
              return;
            }

            run(node, node.arguments[0]);
          }return CallExpression;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9keW5hbWljLWltcG9ydC1jaHVua25hbWUuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJpbXBvcnRGdW5jdGlvbnMiLCJ1bmlxdWVJdGVtcyIsIml0ZW1zIiwid2VicGFja0NodW5rbmFtZUZvcm1hdCIsImNyZWF0ZSIsImNvbnRleHQiLCJjb25maWciLCJvcHRpb25zIiwicGFkZGVkQ29tbWVudFJlZ2V4IiwiY29tbWVudFN0eWxlUmVnZXgiLCJjaHVua1N1YnN0ckZvcm1hdCIsImNodW5rU3Vic3RyUmVnZXgiLCJSZWdFeHAiLCJydW4iLCJub2RlIiwiYXJnIiwic291cmNlQ29kZSIsImdldFNvdXJjZUNvZGUiLCJsZWFkaW5nQ29tbWVudHMiLCJnZXRDb21tZW50c0JlZm9yZSIsImdldENvbW1lbnRzIiwibGVhZGluZyIsImxlbmd0aCIsInJlcG9ydCIsIm1lc3NhZ2UiLCJpc0NodW5rbmFtZVByZXNlbnQiLCJjb21tZW50IiwidGVzdCIsInZhbHVlIiwidm0iLCJydW5Jbk5ld0NvbnRleHQiLCJlcnJvciIsIkltcG9ydEV4cHJlc3Npb24iLCJzb3VyY2UiLCJDYWxsRXhwcmVzc2lvbiIsImNhbGxlZSIsImluZGV4T2YiLCJuYW1lIiwiYXJndW1lbnRzIl0sIm1hcHBpbmdzIjoiYUFBQSx3QjtBQUNBLHFDOztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxZQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSywwQkFBUSwwQkFBUixDQURELEVBRkY7O0FBS0pDLFlBQVEsQ0FBQztBQUNQSCxZQUFNLFFBREM7QUFFUEksa0JBQVk7QUFDVkMseUJBQWlCO0FBQ2ZMLGdCQUFNLE9BRFM7QUFFZk0sdUJBQWEsSUFGRTtBQUdmQyxpQkFBTztBQUNMUCxrQkFBTSxRQURELEVBSFEsRUFEUDs7O0FBUVZR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RUR4QixnQkFBSUMsSUFBSixFQUFVQSxLQUFLd0IsU0FBTCxDQUFlLENBQWYsQ0FBVjtBQUNELFdBWEksMkJBQVA7O0FBYUQsS0FsSGMsbUJBQWpCIiwiZmlsZSI6ImR5bmFtaWMtaW1wb3J0LWNodW5rbmFtZS5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB2bSBmcm9tICd2bSc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdkeW5hbWljLWltcG9ydC1jaHVua25hbWUnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW3tcbiAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgcHJvcGVydGllczoge1xuICAgICAgICBpbXBvcnRGdW5jdGlvbnM6IHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIHVuaXF1ZUl0ZW1zOiB0cnVlLFxuICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICB3ZWJwYWNrQ2h1bmtuYW1lRm9ybWF0OiB7XG4gICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgIH0sXG4gICAgICB9LFxuICAgIH1dLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgY29uc3QgY29uZmlnID0gY29udGV4dC5vcHRpb25zWzBdO1xuICAgIGNvbnN0IHsgaW1wb3J0RnVuY3Rpb25zID0gW10gfSA9IGNvbmZpZyB8fCB7fTtcbiAgICBjb25zdCB7IHdlYnBhY2tDaHVua25hbWVGb3JtYXQgPSAnWzAtOWEtekEtWi1fLy5dKycgfSA9IGNvbmZpZyB8fCB7fTtcblxuICAgIGNvbnN0IHBhZGRlZENvbW1lbnRSZWdleCA9IC9eIChcXFNbXFxzXFxTXStcXFMpICQvO1xuICAgIGNvbnN0IGNvbW1lbnRTdHlsZVJlZ2V4ID0gL14oIFxcdys6IChbXCInXVteXCInXSpbXCInXXxcXGQrfGZhbHNlfHRydWUpLD8pKyAkLztcbiAgICBjb25zdCBjaHVua1N1YnN0ckZvcm1hdCA9IGAgd2VicGFja0NodW5rTmFtZTogW1wiJ10ke3dlYnBhY2tDaHVua25hbWVGb3JtYXR9W1wiJ10sPyBgO1xuICAgIGNvbnN0IGNodW5rU3Vic3RyUmVnZXggPSBuZXcgUmVnRXhwKGNodW5rU3Vic3RyRm9ybWF0KTtcblxuICAgIGZ1bmN0aW9uIHJ1bihub2RlLCBhcmcpIHtcbiAgICAgIGNvbnN0IHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKTtcbiAgICAgIGNvbnN0IGxlYWRpbmdDb21tZW50cyA9IHNvdXJjZUNvZGUuZ2V0Q29tbWVudHNCZWZvcmVcbiAgICAgICAgPyBzb3VyY2VDb2RlLmdldENvbW1lbnRzQmVmb3JlKGFyZykgLy8gVGhpcyBtZXRob2QgaXMgYXZhaWxhYmxlIGluIEVTTGludCA+PSA0LlxuICAgICAgICA6IHNvdXJjZUNvZGUuZ2V0Q29tbWVudHMoYXJnKS5sZWFkaW5nOyAvLyBUaGlzIG1ldGhvZCBpcyBkZXByZWNhdGVkIGluIEVTTGludCA3LlxuXG4gICAgICBpZiAoIWxlYWRpbmdDb21tZW50cyB8fCBsZWFkaW5nQ29tbWVudHMubGVuZ3RoID09PSAwKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlLFxuICAgICAgICAgIG1lc3NhZ2U6ICdkeW5hbWljIGltcG9ydHMgcmVxdWlyZSBhIGxlYWRpbmcgY29tbWVudCB3aXRoIHRoZSB3ZWJwYWNrIGNodW5rbmFtZScsXG4gICAgICAgIH0pO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGxldCBpc0NodW5rbmFtZVByZXNlbnQgPSBmYWxzZTtcblxuICAgICAgZm9yIChjb25zdCBjb21tZW50IG9mIGxlYWRpbmdDb21tZW50cykge1xuICAgICAgICBpZiAoY29tbWVudC50eXBlICE9PSAnQmxvY2snKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6ICdkeW5hbWljIGltcG9ydHMgcmVxdWlyZSBhIC8qIGZvbyAqLyBzdHlsZSBjb21tZW50LCBub3QgYSAvLyBmb28gY29tbWVudCcsXG4gICAgICAgICAgfSk7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgaWYgKCFwYWRkZWRDb21tZW50UmVnZXgudGVzdChjb21tZW50LnZhbHVlKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiBgZHluYW1pYyBpbXBvcnRzIHJlcXVpcmUgYSBibG9jayBjb21tZW50IHBhZGRlZCB3aXRoIHNwYWNlcyAtIC8qIGZvbyAqL2AsXG4gICAgICAgICAgfSk7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgdHJ5IHtcbiAgICAgICAgICAvLyBqdXN0IGxpa2Ugd2VicGFjayBpdHNlbGYgZG9lc1xuICAgICAgICAgIHZtLnJ1bkluTmV3Q29udGV4dChgKGZ1bmN0aW9uKCkge3JldHVybiB7JHtjb21tZW50LnZhbHVlfX19KSgpYCk7XG4gICAgICAgIH1cbiAgICAgICAgY2F0Y2ggKGVycm9yKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGBkeW5hbWljIGltcG9ydHMgcmVxdWlyZSBhIFwid2VicGFja1wiIGNvbW1lbnQgd2l0aCB2YWxpZCBzeW50YXhgLFxuICAgICAgICAgIH0pO1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIGlmICghY29tbWVudFN0eWxlUmVnZXgudGVzdChjb21tZW50LnZhbHVlKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOlxuICAgICAgICAgICAgICBgZHluYW1pYyBpbXBvcnRzIHJlcXVpcmUgYSBsZWFkaW5nIGNvbW1lbnQgaW4gdGhlIGZvcm0gLyoke2NodW5rU3Vic3RyRm9ybWF0fSovYCxcbiAgICAgICAgICB9KTtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cblxuICAgICAgICBpZiAoY2h1bmtTdWJzdHJSZWdleC50ZXN0KGNvbW1lbnQudmFsdWUpKSB7XG4gICAgICAgICAgaXNDaHVua25hbWVQcmVzZW50ID0gdHJ1ZTtcbiAgICAgICAgfVxuICAgICAgfVxuXG4gICAgICBpZiAoIWlzQ2h1bmtuYW1lUHJlc2VudCkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZSxcbiAgICAgICAgICBtZXNzYWdlOlxuICAgICAgICAgICAgYGR5bmFtaWMgaW1wb3J0cyByZXF1aXJlIGEgbGVhZGluZyBjb21tZW50IGluIHRoZSBmb3JtIC8qJHtjaHVua1N1YnN0ckZvcm1hdH0qL2AsXG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBJbXBvcnRFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgcnVuKG5vZGUsIG5vZGUuc291cmNlKTtcbiAgICAgIH0sXG5cbiAgICAgIENhbGxFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUuY2FsbGVlLnR5cGUgIT09ICdJbXBvcnQnICYmIGltcG9ydEZ1bmN0aW9ucy5pbmRleE9mKG5vZGUuY2FsbGVlLm5hbWUpIDwgMCkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIHJ1bihub2RlLCBub2RlLmFyZ3VtZW50c1swXSk7XG4gICAgICB9LFxuICAgIH07XG4gIH0sXG59O1xuIl19