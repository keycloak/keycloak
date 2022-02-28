'use strict';

var _importType = require('../core/importType');

var _importType2 = _interopRequireDefault(_importType);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function reportIfMissing(context, node, allowed, name) {
  if (allowed.indexOf(name) === -1 && (0, _importType2.default)(name, context) === 'builtin') {
    context.report(node, 'Do not import Node.js builtin module "' + name + '"');
  }
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-nodejs-modules')
    }
  },

  create: function (context) {
    const options = context.options[0] || {};
    const allowed = options.allow || [];

    return {
      ImportDeclaration: function handleImports(node) {
        reportIfMissing(context, node, allowed, node.source.value);
      },
      CallExpression: function handleRequires(node) {
        if ((0, _staticRequire2.default)(node)) {
          reportIfMissing(context, node, allowed, node.arguments[0].value);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5vZGVqcy1tb2R1bGVzLmpzIl0sIm5hbWVzIjpbInJlcG9ydElmTWlzc2luZyIsImNvbnRleHQiLCJub2RlIiwiYWxsb3dlZCIsIm5hbWUiLCJpbmRleE9mIiwicmVwb3J0IiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImNyZWF0ZSIsIm9wdGlvbnMiLCJhbGxvdyIsIkltcG9ydERlY2xhcmF0aW9uIiwiaGFuZGxlSW1wb3J0cyIsInNvdXJjZSIsInZhbHVlIiwiQ2FsbEV4cHJlc3Npb24iLCJoYW5kbGVSZXF1aXJlcyIsImFyZ3VtZW50cyJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBLFNBQVNBLGVBQVQsQ0FBeUJDLE9BQXpCLEVBQWtDQyxJQUFsQyxFQUF3Q0MsT0FBeEMsRUFBaURDLElBQWpELEVBQXVEO0FBQ3JELE1BQUlELFFBQVFFLE9BQVIsQ0FBZ0JELElBQWhCLE1BQTBCLENBQUMsQ0FBM0IsSUFBZ0MsMEJBQVdBLElBQVgsRUFBaUJILE9BQWpCLE1BQThCLFNBQWxFLEVBQTZFO0FBQzNFQSxZQUFRSyxNQUFSLENBQWVKLElBQWYsRUFBcUIsMkNBQTJDRSxJQUEzQyxHQUFrRCxHQUF2RTtBQUNEO0FBQ0Y7O0FBRURHLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLG1CQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVaLE9BQVYsRUFBbUI7QUFDekIsVUFBTWEsVUFBVWIsUUFBUWEsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUF0QztBQUNBLFVBQU1YLFVBQVVXLFFBQVFDLEtBQVIsSUFBaUIsRUFBakM7O0FBRUEsV0FBTztBQUNMQyx5QkFBbUIsU0FBU0MsYUFBVCxDQUF1QmYsSUFBdkIsRUFBNkI7QUFDOUNGLHdCQUFnQkMsT0FBaEIsRUFBeUJDLElBQXpCLEVBQStCQyxPQUEvQixFQUF3Q0QsS0FBS2dCLE1BQUwsQ0FBWUMsS0FBcEQ7QUFDRCxPQUhJO0FBSUxDLHNCQUFnQixTQUFTQyxjQUFULENBQXdCbkIsSUFBeEIsRUFBOEI7QUFDNUMsWUFBSSw2QkFBZ0JBLElBQWhCLENBQUosRUFBMkI7QUFDekJGLDBCQUFnQkMsT0FBaEIsRUFBeUJDLElBQXpCLEVBQStCQyxPQUEvQixFQUF3Q0QsS0FBS29CLFNBQUwsQ0FBZSxDQUFmLEVBQWtCSCxLQUExRDtBQUNEO0FBQ0Y7QUFSSSxLQUFQO0FBVUQ7QUF0QmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tbm9kZWpzLW1vZHVsZXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgaW1wb3J0VHlwZSBmcm9tICcuLi9jb3JlL2ltcG9ydFR5cGUnXG5pbXBvcnQgaXNTdGF0aWNSZXF1aXJlIGZyb20gJy4uL2NvcmUvc3RhdGljUmVxdWlyZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIHJlcG9ydElmTWlzc2luZyhjb250ZXh0LCBub2RlLCBhbGxvd2VkLCBuYW1lKSB7XG4gIGlmIChhbGxvd2VkLmluZGV4T2YobmFtZSkgPT09IC0xICYmIGltcG9ydFR5cGUobmFtZSwgY29udGV4dCkgPT09ICdidWlsdGluJykge1xuICAgIGNvbnRleHQucmVwb3J0KG5vZGUsICdEbyBub3QgaW1wb3J0IE5vZGUuanMgYnVpbHRpbiBtb2R1bGUgXCInICsgbmFtZSArICdcIicpXG4gIH1cbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1ub2RlanMtbW9kdWxlcycpLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge31cbiAgICBjb25zdCBhbGxvd2VkID0gb3B0aW9ucy5hbGxvdyB8fCBbXVxuXG4gICAgcmV0dXJuIHtcbiAgICAgIEltcG9ydERlY2xhcmF0aW9uOiBmdW5jdGlvbiBoYW5kbGVJbXBvcnRzKG5vZGUpIHtcbiAgICAgICAgcmVwb3J0SWZNaXNzaW5nKGNvbnRleHQsIG5vZGUsIGFsbG93ZWQsIG5vZGUuc291cmNlLnZhbHVlKVxuICAgICAgfSxcbiAgICAgIENhbGxFeHByZXNzaW9uOiBmdW5jdGlvbiBoYW5kbGVSZXF1aXJlcyhub2RlKSB7XG4gICAgICAgIGlmIChpc1N0YXRpY1JlcXVpcmUobm9kZSkpIHtcbiAgICAgICAgICByZXBvcnRJZk1pc3NpbmcoY29udGV4dCwgbm9kZSwgYWxsb3dlZCwgbm9kZS5hcmd1bWVudHNbMF0udmFsdWUpXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19