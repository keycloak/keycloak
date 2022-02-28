'use strict';

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function isImportingSelf(context, node, requireName) {
  const filePath = context.getFilename();

  // If the input is from stdin, this test can't fail
  if (filePath !== '<text>' && filePath === (0, _resolve2.default)(requireName, context)) {
    context.report({
      node,
      message: 'Module imports itself.'
    });
  }
} /**
   * @fileOverview Forbids a module from importing itself
   * @author Gio d'Amelio
   */

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      description: 'Forbid a module from importing itself',
      recommended: true,
      url: (0, _docsUrl2.default)('no-self-import')
    },

    schema: []
  },
  create: function (context) {
    return {
      ImportDeclaration(node) {
        isImportingSelf(context, node, node.source.value);
      },
      CallExpression(node) {
        if ((0, _staticRequire2.default)(node)) {
          isImportingSelf(context, node, node.arguments[0].value);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXNlbGYtaW1wb3J0LmpzIl0sIm5hbWVzIjpbImlzSW1wb3J0aW5nU2VsZiIsImNvbnRleHQiLCJub2RlIiwicmVxdWlyZU5hbWUiLCJmaWxlUGF0aCIsImdldEZpbGVuYW1lIiwicmVwb3J0IiwibWVzc2FnZSIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJkZXNjcmlwdGlvbiIsInJlY29tbWVuZGVkIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJzb3VyY2UiLCJ2YWx1ZSIsIkNhbGxFeHByZXNzaW9uIiwiYXJndW1lbnRzIl0sIm1hcHBpbmdzIjoiOztBQUtBOzs7O0FBQ0E7Ozs7QUFDQTs7Ozs7O0FBRUEsU0FBU0EsZUFBVCxDQUF5QkMsT0FBekIsRUFBa0NDLElBQWxDLEVBQXdDQyxXQUF4QyxFQUFxRDtBQUNuRCxRQUFNQyxXQUFXSCxRQUFRSSxXQUFSLEVBQWpCOztBQUVBO0FBQ0EsTUFBSUQsYUFBYSxRQUFiLElBQXlCQSxhQUFhLHVCQUFRRCxXQUFSLEVBQXFCRixPQUFyQixDQUExQyxFQUF5RTtBQUN2RUEsWUFBUUssTUFBUixDQUFlO0FBQ1hKLFVBRFc7QUFFWEssZUFBUztBQUZFLEtBQWY7QUFJRDtBQUNGLEMsQ0FuQkQ7Ozs7O0FBcUJBQyxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsbUJBQWEsdUNBRFQ7QUFFSkMsbUJBQWEsSUFGVDtBQUdKQyxXQUFLLHVCQUFRLGdCQUFSO0FBSEQsS0FGRjs7QUFRSkMsWUFBUTtBQVJKLEdBRFM7QUFXZkMsVUFBUSxVQUFVaEIsT0FBVixFQUFtQjtBQUN6QixXQUFPO0FBQ0xpQix3QkFBa0JoQixJQUFsQixFQUF3QjtBQUN0QkYsd0JBQWdCQyxPQUFoQixFQUF5QkMsSUFBekIsRUFBK0JBLEtBQUtpQixNQUFMLENBQVlDLEtBQTNDO0FBQ0QsT0FISTtBQUlMQyxxQkFBZW5CLElBQWYsRUFBcUI7QUFDbkIsWUFBSSw2QkFBZ0JBLElBQWhCLENBQUosRUFBMkI7QUFDekJGLDBCQUFnQkMsT0FBaEIsRUFBeUJDLElBQXpCLEVBQStCQSxLQUFLb0IsU0FBTCxDQUFlLENBQWYsRUFBa0JGLEtBQWpEO0FBQ0Q7QUFDRjtBQVJJLEtBQVA7QUFVRDtBQXRCYyxDQUFqQiIsImZpbGUiOiJydWxlcy9uby1zZWxmLWltcG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVPdmVydmlldyBGb3JiaWRzIGEgbW9kdWxlIGZyb20gaW1wb3J0aW5nIGl0c2VsZlxuICogQGF1dGhvciBHaW8gZCdBbWVsaW9cbiAqL1xuXG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnXG5pbXBvcnQgaXNTdGF0aWNSZXF1aXJlIGZyb20gJy4uL2NvcmUvc3RhdGljUmVxdWlyZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIGlzSW1wb3J0aW5nU2VsZihjb250ZXh0LCBub2RlLCByZXF1aXJlTmFtZSkge1xuICBjb25zdCBmaWxlUGF0aCA9IGNvbnRleHQuZ2V0RmlsZW5hbWUoKVxuXG4gIC8vIElmIHRoZSBpbnB1dCBpcyBmcm9tIHN0ZGluLCB0aGlzIHRlc3QgY2FuJ3QgZmFpbFxuICBpZiAoZmlsZVBhdGggIT09ICc8dGV4dD4nICYmIGZpbGVQYXRoID09PSByZXNvbHZlKHJlcXVpcmVOYW1lLCBjb250ZXh0KSkge1xuICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgbm9kZSxcbiAgICAgICAgbWVzc2FnZTogJ01vZHVsZSBpbXBvcnRzIGl0c2VsZi4nLFxuICAgIH0pXG4gIH1cbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgZGVzY3JpcHRpb246ICdGb3JiaWQgYSBtb2R1bGUgZnJvbSBpbXBvcnRpbmcgaXRzZWxmJyxcbiAgICAgIHJlY29tbWVuZGVkOiB0cnVlLFxuICAgICAgdXJsOiBkb2NzVXJsKCduby1zZWxmLWltcG9ydCcpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgIEltcG9ydERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgICAgaXNJbXBvcnRpbmdTZWxmKGNvbnRleHQsIG5vZGUsIG5vZGUuc291cmNlLnZhbHVlKVxuICAgICAgfSxcbiAgICAgIENhbGxFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKGlzU3RhdGljUmVxdWlyZShub2RlKSkge1xuICAgICAgICAgIGlzSW1wb3J0aW5nU2VsZihjb250ZXh0LCBub2RlLCBub2RlLmFyZ3VtZW50c1swXS52YWx1ZSlcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=