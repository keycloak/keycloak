'use strict';

var _unambiguous = require('eslint-module-utils/unambiguous');

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * @fileOverview Report modules that could parse incorrectly as scripts.
 * @author Ben Mosher
 */

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('unambiguous')
    }
  },

  create: function (context) {
    // ignore non-modules
    if (context.parserOptions.sourceType !== 'module') {
      return {};
    }

    return {
      Program: function (ast) {
        if (!(0, _unambiguous.isModule)(ast)) {
          context.report({
            node: ast,
            message: 'This module could be parsed as a valid script.'
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL3VuYW1iaWd1b3VzLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJjcmVhdGUiLCJjb250ZXh0IiwicGFyc2VyT3B0aW9ucyIsInNvdXJjZVR5cGUiLCJQcm9ncmFtIiwiYXN0IiwicmVwb3J0Iiwibm9kZSIsIm1lc3NhZ2UiXSwibWFwcGluZ3MiOiI7O0FBS0E7O0FBQ0E7Ozs7OztBQU5BOzs7OztBQVFBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxZQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxhQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekI7QUFDQSxRQUFJQSxRQUFRQyxhQUFSLENBQXNCQyxVQUF0QixLQUFxQyxRQUF6QyxFQUFtRDtBQUNqRCxhQUFPLEVBQVA7QUFDRDs7QUFFRCxXQUFPO0FBQ0xDLGVBQVMsVUFBVUMsR0FBVixFQUFlO0FBQ3RCLFlBQUksQ0FBQywyQkFBU0EsR0FBVCxDQUFMLEVBQW9CO0FBQ2xCSixrQkFBUUssTUFBUixDQUFlO0FBQ2JDLGtCQUFNRixHQURPO0FBRWJHLHFCQUFTO0FBRkksV0FBZjtBQUlEO0FBQ0Y7QUFSSSxLQUFQO0FBV0Q7QUF6QmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvdW5hbWJpZ3VvdXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlT3ZlcnZpZXcgUmVwb3J0IG1vZHVsZXMgdGhhdCBjb3VsZCBwYXJzZSBpbmNvcnJlY3RseSBhcyBzY3JpcHRzLlxuICogQGF1dGhvciBCZW4gTW9zaGVyXG4gKi9cblxuaW1wb3J0IHsgaXNNb2R1bGUgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3VuYW1iaWd1b3VzJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCd1bmFtYmlndW91cycpLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIC8vIGlnbm9yZSBub24tbW9kdWxlc1xuICAgIGlmIChjb250ZXh0LnBhcnNlck9wdGlvbnMuc291cmNlVHlwZSAhPT0gJ21vZHVsZScpIHtcbiAgICAgIHJldHVybiB7fVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBQcm9ncmFtOiBmdW5jdGlvbiAoYXN0KSB7XG4gICAgICAgIGlmICghaXNNb2R1bGUoYXN0KSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGU6IGFzdCxcbiAgICAgICAgICAgIG1lc3NhZ2U6ICdUaGlzIG1vZHVsZSBjb3VsZCBiZSBwYXJzZWQgYXMgYSB2YWxpZCBzY3JpcHQuJyxcbiAgICAgICAgICB9KVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cblxuICB9LFxufVxuIl19