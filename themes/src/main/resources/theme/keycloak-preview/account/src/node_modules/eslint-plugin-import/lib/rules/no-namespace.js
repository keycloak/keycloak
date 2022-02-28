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
      url: (0, _docsUrl2.default)('no-namespace')
    }
  },

  create: function (context) {
    return {
      'ImportNamespaceSpecifier': function (node) {
        context.report(node, `Unexpected namespace import.`);
      }
    };
  }
}; /**
    * @fileoverview Rule to disallow namespace import
    * @author Radek Benkel
    */
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5hbWVzcGFjZS5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsIm5vZGUiLCJyZXBvcnQiXSwibWFwcGluZ3MiOiI7O0FBS0E7Ozs7OztBQUVBO0FBQ0E7QUFDQTs7O0FBR0FBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLGNBQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixXQUFPO0FBQ0wsa0NBQTRCLFVBQVVDLElBQVYsRUFBZ0I7QUFDMUNELGdCQUFRRSxNQUFSLENBQWVELElBQWYsRUFBc0IsOEJBQXRCO0FBQ0Q7QUFISSxLQUFQO0FBS0Q7QUFkYyxDQUFqQixDLENBWkEiLCJmaWxlIjoicnVsZXMvbm8tbmFtZXNwYWNlLmpzIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAZmlsZW92ZXJ2aWV3IFJ1bGUgdG8gZGlzYWxsb3cgbmFtZXNwYWNlIGltcG9ydFxuICogQGF1dGhvciBSYWRlayBCZW5rZWxcbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbmFtZXNwYWNlJyksXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgcmV0dXJuIHtcbiAgICAgICdJbXBvcnROYW1lc3BhY2VTcGVjaWZpZXInOiBmdW5jdGlvbiAobm9kZSkge1xuICAgICAgICBjb250ZXh0LnJlcG9ydChub2RlLCBgVW5leHBlY3RlZCBuYW1lc3BhY2UgaW1wb3J0LmApXG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==