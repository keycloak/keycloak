'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-amd')
    }
  },

  create: function (context) {
    return {
      'CallExpression': function (node) {
        if (context.getScope().type !== 'module') return;

        if (node.callee.type !== 'Identifier') return;
        if (node.callee.name !== 'require' && node.callee.name !== 'define') return;

        // todo: capture define((require, module, exports) => {}) form?
        if (node.arguments.length !== 2) return;

        const modules = node.arguments[0];
        if (modules.type !== 'ArrayExpression') return;

        // todo: check second arg type? (identifier or callback)

        context.report(node, `Expected imports instead of AMD ${node.callee.name}().`);
      }
    };
  }
}; /**
    * @fileoverview Rule to prefer imports to AMD
    * @author Jamund Ferguson
    */
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWFtZC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsIm5vZGUiLCJnZXRTY29wZSIsImNhbGxlZSIsIm5hbWUiLCJhcmd1bWVudHMiLCJsZW5ndGgiLCJtb2R1bGVzIiwicmVwb3J0Il0sIm1hcHBpbmdzIjoiOztBQUtBOzs7Ozs7QUFFQTtBQUNBO0FBQ0E7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLFFBQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixXQUFPO0FBQ0wsd0JBQWtCLFVBQVVDLElBQVYsRUFBZ0I7QUFDaEMsWUFBSUQsUUFBUUUsUUFBUixHQUFtQk4sSUFBbkIsS0FBNEIsUUFBaEMsRUFBMEM7O0FBRTFDLFlBQUlLLEtBQUtFLE1BQUwsQ0FBWVAsSUFBWixLQUFxQixZQUF6QixFQUF1QztBQUN2QyxZQUFJSyxLQUFLRSxNQUFMLENBQVlDLElBQVosS0FBcUIsU0FBckIsSUFDQUgsS0FBS0UsTUFBTCxDQUFZQyxJQUFaLEtBQXFCLFFBRHpCLEVBQ21DOztBQUVuQztBQUNBLFlBQUlILEtBQUtJLFNBQUwsQ0FBZUMsTUFBZixLQUEwQixDQUE5QixFQUFpQzs7QUFFakMsY0FBTUMsVUFBVU4sS0FBS0ksU0FBTCxDQUFlLENBQWYsQ0FBaEI7QUFDQSxZQUFJRSxRQUFRWCxJQUFSLEtBQWlCLGlCQUFyQixFQUF3Qzs7QUFFeEM7O0FBRUFJLGdCQUFRUSxNQUFSLENBQWVQLElBQWYsRUFBc0IsbUNBQWtDQSxLQUFLRSxNQUFMLENBQVlDLElBQUssS0FBekU7QUFDRDtBQWpCSSxLQUFQO0FBb0JEO0FBN0JjLENBQWpCLEMsQ0FYQSIsImZpbGUiOiJydWxlcy9uby1hbWQuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlb3ZlcnZpZXcgUnVsZSB0byBwcmVmZXIgaW1wb3J0cyB0byBBTURcbiAqIEBhdXRob3IgSmFtdW5kIEZlcmd1c29uXG4gKi9cblxuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFJ1bGUgRGVmaW5pdGlvblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1hbWQnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgJ0NhbGxFeHByZXNzaW9uJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgaWYgKGNvbnRleHQuZ2V0U2NvcGUoKS50eXBlICE9PSAnbW9kdWxlJykgcmV0dXJuXG5cbiAgICAgICAgaWYgKG5vZGUuY2FsbGVlLnR5cGUgIT09ICdJZGVudGlmaWVyJykgcmV0dXJuXG4gICAgICAgIGlmIChub2RlLmNhbGxlZS5uYW1lICE9PSAncmVxdWlyZScgJiZcbiAgICAgICAgICAgIG5vZGUuY2FsbGVlLm5hbWUgIT09ICdkZWZpbmUnKSByZXR1cm5cblxuICAgICAgICAvLyB0b2RvOiBjYXB0dXJlIGRlZmluZSgocmVxdWlyZSwgbW9kdWxlLCBleHBvcnRzKSA9PiB7fSkgZm9ybT9cbiAgICAgICAgaWYgKG5vZGUuYXJndW1lbnRzLmxlbmd0aCAhPT0gMikgcmV0dXJuXG5cbiAgICAgICAgY29uc3QgbW9kdWxlcyA9IG5vZGUuYXJndW1lbnRzWzBdXG4gICAgICAgIGlmIChtb2R1bGVzLnR5cGUgIT09ICdBcnJheUV4cHJlc3Npb24nKSByZXR1cm5cblxuICAgICAgICAvLyB0b2RvOiBjaGVjayBzZWNvbmQgYXJnIHR5cGU/IChpZGVudGlmaWVyIG9yIGNhbGxiYWNrKVxuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUsIGBFeHBlY3RlZCBpbXBvcnRzIGluc3RlYWQgb2YgQU1EICR7bm9kZS5jYWxsZWUubmFtZX0oKS5gKVxuICAgICAgfSxcbiAgICB9XG5cbiAgfSxcbn1cbiJdfQ==