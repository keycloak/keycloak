'use strict';

var _vm = require('vm');

var _vm2 = _interopRequireDefault(_vm);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('dynamic-import-chunkname')
    },
    schema: [{
      type: 'object',
      properties: {
        importFunctions: {
          type: 'array',
          uniqueItems: true,
          items: {
            type: 'string'
          }
        },
        webpackChunknameFormat: {
          type: 'string'
        }
      }
    }]
  },

  create: function (context) {
    const config = context.options[0];

    var _ref = config || {},
        _ref$importFunctions = _ref.importFunctions;

    const importFunctions = _ref$importFunctions === undefined ? [] : _ref$importFunctions;

    var _ref2 = config || {},
        _ref2$webpackChunknam = _ref2.webpackChunknameFormat;

    const webpackChunknameFormat = _ref2$webpackChunknam === undefined ? '[0-9a-zA-Z-_/.]+' : _ref2$webpackChunknam;


    const paddedCommentRegex = /^ (\S[\s\S]+\S) $/;
    const commentStyleRegex = /^( \w+: ("[^"]*"|\d+|false|true),?)+ $/;
    const chunkSubstrFormat = ` webpackChunkName: "${webpackChunknameFormat}",? `;
    const chunkSubstrRegex = new RegExp(chunkSubstrFormat);

    return {
      CallExpression(node) {
        if (node.callee.type !== 'Import' && importFunctions.indexOf(node.callee.name) < 0) {
          return;
        }

        const sourceCode = context.getSourceCode();
        const arg = node.arguments[0];
        const leadingComments = sourceCode.getComments(arg).leading;

        if (!leadingComments || leadingComments.length === 0) {
          context.report({
            node,
            message: 'dynamic imports require a leading comment with the webpack chunkname'
          });
          return;
        }

        let isChunknamePresent = false;

        for (const comment of leadingComments) {
          if (comment.type !== 'Block') {
            context.report({
              node,
              message: 'dynamic imports require a /* foo */ style comment, not a // foo comment'
            });
            return;
          }

          if (!paddedCommentRegex.test(comment.value)) {
            context.report({
              node,
              message: `dynamic imports require a block comment padded with spaces - /* foo */`
            });
            return;
          }

          try {
            // just like webpack itself does
            _vm2.default.runInNewContext(`(function(){return {${comment.value}}})()`);
          } catch (error) {
            context.report({
              node,
              message: `dynamic imports require a "webpack" comment with valid syntax`
            });
            return;
          }

          if (!commentStyleRegex.test(comment.value)) {
            context.report({
              node,
              message: `dynamic imports require a leading comment in the form /*${chunkSubstrFormat}*/`
            });
            return;
          }

          if (chunkSubstrRegex.test(comment.value)) {
            isChunknamePresent = true;
          }
        }

        if (!isChunknamePresent) {
          context.report({
            node,
            message: `dynamic imports require a leading comment in the form /*${chunkSubstrFormat}*/`
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL2R5bmFtaWMtaW1wb3J0LWNodW5rbmFtZS5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwicHJvcGVydGllcyIsImltcG9ydEZ1bmN0aW9ucyIsInVuaXF1ZUl0ZW1zIiwiaXRlbXMiLCJ3ZWJwYWNrQ2h1bmtuYW1lRm9ybWF0IiwiY3JlYXRlIiwiY29udGV4dCIsImNvbmZpZyIsIm9wdGlvbnMiLCJwYWRkZWRDb21tZW50UmVnZXgiLCJjb21tZW50U3R5bGVSZWdleCIsImNodW5rU3Vic3RyRm9ybWF0IiwiY2h1bmtTdWJzdHJSZWdleCIsIlJlZ0V4cCIsIkNhbGxFeHByZXNzaW9uIiwibm9kZSIsImNhbGxlZSIsImluZGV4T2YiLCJuYW1lIiwic291cmNlQ29kZSIsImdldFNvdXJjZUNvZGUiLCJhcmciLCJhcmd1bWVudHMiLCJsZWFkaW5nQ29tbWVudHMiLCJnZXRDb21tZW50cyIsImxlYWRpbmciLCJsZW5ndGgiLCJyZXBvcnQiLCJtZXNzYWdlIiwiaXNDaHVua25hbWVQcmVzZW50IiwiY29tbWVudCIsInRlc3QiLCJ2YWx1ZSIsInZtIiwicnVuSW5OZXdDb250ZXh0IiwiZXJyb3IiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7QUFDQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLDBCQUFSO0FBREQsS0FGRjtBQUtKQyxZQUFRLENBQUM7QUFDUEgsWUFBTSxRQURDO0FBRVBJLGtCQUFZO0FBQ1ZDLHlCQUFpQjtBQUNmTCxnQkFBTSxPQURTO0FBRWZNLHVCQUFhLElBRkU7QUFHZkMsaUJBQU87QUFDTFAsa0JBQU07QUFERDtBQUhRLFNBRFA7QUFRVlEsZ0NBQXdCO0FBQ3RCUixnQkFBTTtBQURnQjtBQVJkO0FBRkwsS0FBRDtBQUxKLEdBRFM7O0FBdUJmUyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekIsVUFBTUMsU0FBU0QsUUFBUUUsT0FBUixDQUFnQixDQUFoQixDQUFmOztBQUR5QixlQUVRRCxVQUFVLEVBRmxCO0FBQUEsb0NBRWpCTixlQUZpQjs7QUFBQSxVQUVqQkEsZUFGaUIsd0NBRUMsRUFGRDs7QUFBQSxnQkFHK0JNLFVBQVUsRUFIekM7QUFBQSxzQ0FHakJILHNCQUhpQjs7QUFBQSxVQUdqQkEsc0JBSGlCLHlDQUdRLGtCQUhSOzs7QUFLekIsVUFBTUsscUJBQXFCLG1CQUEzQjtBQUNBLFVBQU1DLG9CQUFvQix3Q0FBMUI7QUFDQSxVQUFNQyxvQkFBcUIsdUJBQXNCUCxzQkFBdUIsTUFBeEU7QUFDQSxVQUFNUSxtQkFBbUIsSUFBSUMsTUFBSixDQUFXRixpQkFBWCxDQUF6Qjs7QUFFQSxXQUFPO0FBQ0xHLHFCQUFlQyxJQUFmLEVBQXFCO0FBQ25CLFlBQUlBLEtBQUtDLE1BQUwsQ0FBWXBCLElBQVosS0FBcUIsUUFBckIsSUFBaUNLLGdCQUFnQmdCLE9BQWhCLENBQXdCRixLQUFLQyxNQUFMLENBQVlFLElBQXBDLElBQTRDLENBQWpGLEVBQW9GO0FBQ2xGO0FBQ0Q7O0FBRUQsY0FBTUMsYUFBYWIsUUFBUWMsYUFBUixFQUFuQjtBQUNBLGNBQU1DLE1BQU1OLEtBQUtPLFNBQUwsQ0FBZSxDQUFmLENBQVo7QUFDQSxjQUFNQyxrQkFBa0JKLFdBQVdLLFdBQVgsQ0FBdUJILEdBQXZCLEVBQTRCSSxPQUFwRDs7QUFFQSxZQUFJLENBQUNGLGVBQUQsSUFBb0JBLGdCQUFnQkcsTUFBaEIsS0FBMkIsQ0FBbkQsRUFBc0Q7QUFDcERwQixrQkFBUXFCLE1BQVIsQ0FBZTtBQUNiWixnQkFEYTtBQUViYSxxQkFBUztBQUZJLFdBQWY7QUFJQTtBQUNEOztBQUVELFlBQUlDLHFCQUFxQixLQUF6Qjs7QUFFQSxhQUFLLE1BQU1DLE9BQVgsSUFBc0JQLGVBQXRCLEVBQXVDO0FBQ3JDLGNBQUlPLFFBQVFsQyxJQUFSLEtBQWlCLE9BQXJCLEVBQThCO0FBQzVCVSxvQkFBUXFCLE1BQVIsQ0FBZTtBQUNiWixrQkFEYTtBQUViYSx1QkFBUztBQUZJLGFBQWY7QUFJQTtBQUNEOztBQUVELGNBQUksQ0FBQ25CLG1CQUFtQnNCLElBQW5CLENBQXdCRCxRQUFRRSxLQUFoQyxDQUFMLEVBQTZDO0FBQzNDMUIsb0JBQVFxQixNQUFSLENBQWU7QUFDYlosa0JBRGE7QUFFYmEsdUJBQVU7QUFGRyxhQUFmO0FBSUE7QUFDRDs7QUFFRCxjQUFJO0FBQ0Y7QUFDQUsseUJBQUdDLGVBQUgsQ0FBb0IsdUJBQXNCSixRQUFRRSxLQUFNLE9BQXhEO0FBQ0QsV0FIRCxDQUlBLE9BQU9HLEtBQVAsRUFBYztBQUNaN0Isb0JBQVFxQixNQUFSLENBQWU7QUFDYlosa0JBRGE7QUFFYmEsdUJBQVU7QUFGRyxhQUFmO0FBSUE7QUFDRDs7QUFFRCxjQUFJLENBQUNsQixrQkFBa0JxQixJQUFsQixDQUF1QkQsUUFBUUUsS0FBL0IsQ0FBTCxFQUE0QztBQUMxQzFCLG9CQUFRcUIsTUFBUixDQUFlO0FBQ2JaLGtCQURhO0FBRWJhLHVCQUNHLDJEQUEwRGpCLGlCQUFrQjtBQUhsRSxhQUFmO0FBS0E7QUFDRDs7QUFFRCxjQUFJQyxpQkFBaUJtQixJQUFqQixDQUFzQkQsUUFBUUUsS0FBOUIsQ0FBSixFQUEwQztBQUN4Q0gsaUNBQXFCLElBQXJCO0FBQ0Q7QUFDRjs7QUFFRCxZQUFJLENBQUNBLGtCQUFMLEVBQXlCO0FBQ3ZCdkIsa0JBQVFxQixNQUFSLENBQWU7QUFDYlosZ0JBRGE7QUFFYmEscUJBQ0csMkRBQTBEakIsaUJBQWtCO0FBSGxFLFdBQWY7QUFLRDtBQUNGO0FBdEVJLEtBQVA7QUF3RUQ7QUF6R2MsQ0FBakIiLCJmaWxlIjoicnVsZXMvZHluYW1pYy1pbXBvcnQtY2h1bmtuYW1lLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHZtIGZyb20gJ3ZtJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdkeW5hbWljLWltcG9ydC1jaHVua25hbWUnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW3tcbiAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgcHJvcGVydGllczoge1xuICAgICAgICBpbXBvcnRGdW5jdGlvbnM6IHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIHVuaXF1ZUl0ZW1zOiB0cnVlLFxuICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICB9LFxuICAgICAgICB9LFxuICAgICAgICB3ZWJwYWNrQ2h1bmtuYW1lRm9ybWF0OiB7XG4gICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgIH0sXG4gICAgICB9LFxuICAgIH1dLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBjb25maWcgPSBjb250ZXh0Lm9wdGlvbnNbMF1cbiAgICBjb25zdCB7IGltcG9ydEZ1bmN0aW9ucyA9IFtdIH0gPSBjb25maWcgfHwge31cbiAgICBjb25zdCB7IHdlYnBhY2tDaHVua25hbWVGb3JtYXQgPSAnWzAtOWEtekEtWi1fLy5dKycgfSA9IGNvbmZpZyB8fCB7fVxuXG4gICAgY29uc3QgcGFkZGVkQ29tbWVudFJlZ2V4ID0gL14gKFxcU1tcXHNcXFNdK1xcUykgJC9cbiAgICBjb25zdCBjb21tZW50U3R5bGVSZWdleCA9IC9eKCBcXHcrOiAoXCJbXlwiXSpcInxcXGQrfGZhbHNlfHRydWUpLD8pKyAkL1xuICAgIGNvbnN0IGNodW5rU3Vic3RyRm9ybWF0ID0gYCB3ZWJwYWNrQ2h1bmtOYW1lOiBcIiR7d2VicGFja0NodW5rbmFtZUZvcm1hdH1cIiw/IGBcbiAgICBjb25zdCBjaHVua1N1YnN0clJlZ2V4ID0gbmV3IFJlZ0V4cChjaHVua1N1YnN0ckZvcm1hdClcblxuICAgIHJldHVybiB7XG4gICAgICBDYWxsRXhwcmVzc2lvbihub2RlKSB7XG4gICAgICAgIGlmIChub2RlLmNhbGxlZS50eXBlICE9PSAnSW1wb3J0JyAmJiBpbXBvcnRGdW5jdGlvbnMuaW5kZXhPZihub2RlLmNhbGxlZS5uYW1lKSA8IDApIHtcbiAgICAgICAgICByZXR1cm5cbiAgICAgICAgfVxuXG4gICAgICAgIGNvbnN0IHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKVxuICAgICAgICBjb25zdCBhcmcgPSBub2RlLmFyZ3VtZW50c1swXVxuICAgICAgICBjb25zdCBsZWFkaW5nQ29tbWVudHMgPSBzb3VyY2VDb2RlLmdldENvbW1lbnRzKGFyZykubGVhZGluZ1xuXG4gICAgICAgIGlmICghbGVhZGluZ0NvbW1lbnRzIHx8IGxlYWRpbmdDb21tZW50cy5sZW5ndGggPT09IDApIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogJ2R5bmFtaWMgaW1wb3J0cyByZXF1aXJlIGEgbGVhZGluZyBjb21tZW50IHdpdGggdGhlIHdlYnBhY2sgY2h1bmtuYW1lJyxcbiAgICAgICAgICB9KVxuICAgICAgICAgIHJldHVyblxuICAgICAgICB9XG5cbiAgICAgICAgbGV0IGlzQ2h1bmtuYW1lUHJlc2VudCA9IGZhbHNlXG5cbiAgICAgICAgZm9yIChjb25zdCBjb21tZW50IG9mIGxlYWRpbmdDb21tZW50cykge1xuICAgICAgICAgIGlmIChjb21tZW50LnR5cGUgIT09ICdCbG9jaycpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgbWVzc2FnZTogJ2R5bmFtaWMgaW1wb3J0cyByZXF1aXJlIGEgLyogZm9vICovIHN0eWxlIGNvbW1lbnQsIG5vdCBhIC8vIGZvbyBjb21tZW50JyxcbiAgICAgICAgICAgIH0pXG4gICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAoIXBhZGRlZENvbW1lbnRSZWdleC50ZXN0KGNvbW1lbnQudmFsdWUpKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICAgIG1lc3NhZ2U6IGBkeW5hbWljIGltcG9ydHMgcmVxdWlyZSBhIGJsb2NrIGNvbW1lbnQgcGFkZGVkIHdpdGggc3BhY2VzIC0gLyogZm9vICovYCxcbiAgICAgICAgICAgIH0pXG4gICAgICAgICAgICByZXR1cm5cbiAgICAgICAgICB9XG5cbiAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgLy8ganVzdCBsaWtlIHdlYnBhY2sgaXRzZWxmIGRvZXNcbiAgICAgICAgICAgIHZtLnJ1bkluTmV3Q29udGV4dChgKGZ1bmN0aW9uKCl7cmV0dXJuIHske2NvbW1lbnQudmFsdWV9fX0pKClgKVxuICAgICAgICAgIH1cbiAgICAgICAgICBjYXRjaCAoZXJyb3IpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgbWVzc2FnZTogYGR5bmFtaWMgaW1wb3J0cyByZXF1aXJlIGEgXCJ3ZWJwYWNrXCIgY29tbWVudCB3aXRoIHZhbGlkIHN5bnRheGAsXG4gICAgICAgICAgICB9KVxuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKCFjb21tZW50U3R5bGVSZWdleC50ZXN0KGNvbW1lbnQudmFsdWUpKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICAgIG1lc3NhZ2U6XG4gICAgICAgICAgICAgICAgYGR5bmFtaWMgaW1wb3J0cyByZXF1aXJlIGEgbGVhZGluZyBjb21tZW50IGluIHRoZSBmb3JtIC8qJHtjaHVua1N1YnN0ckZvcm1hdH0qL2AsXG4gICAgICAgICAgICB9KVxuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKGNodW5rU3Vic3RyUmVnZXgudGVzdChjb21tZW50LnZhbHVlKSkge1xuICAgICAgICAgICAgaXNDaHVua25hbWVQcmVzZW50ID0gdHJ1ZVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIGlmICghaXNDaHVua25hbWVQcmVzZW50KSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6XG4gICAgICAgICAgICAgIGBkeW5hbWljIGltcG9ydHMgcmVxdWlyZSBhIGxlYWRpbmcgY29tbWVudCBpbiB0aGUgZm9ybSAvKiR7Y2h1bmtTdWJzdHJGb3JtYXR9Ki9gLFxuICAgICAgICAgIH0pXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19