'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-named-default')
    }
  },

  create: function (context) {
    return {
      'ImportDeclaration': function (node) {
        node.specifiers.forEach(function (im) {
          if (im.type === 'ImportSpecifier' && im.imported.name === 'default') {
            context.report({
              node: im.local,
              message: `Use default import syntax to import '${im.local.name}'.` });
          }
        });
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5hbWVkLWRlZmF1bHQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImNyZWF0ZSIsImNvbnRleHQiLCJub2RlIiwic3BlY2lmaWVycyIsImZvckVhY2giLCJpbSIsImltcG9ydGVkIiwibmFtZSIsInJlcG9ydCIsImxvY2FsIiwibWVzc2FnZSJdLCJtYXBwaW5ncyI6Ijs7QUFBQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLGtCQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVDLE9BQVYsRUFBbUI7QUFDekIsV0FBTztBQUNMLDJCQUFxQixVQUFVQyxJQUFWLEVBQWdCO0FBQ25DQSxhQUFLQyxVQUFMLENBQWdCQyxPQUFoQixDQUF3QixVQUFVQyxFQUFWLEVBQWM7QUFDcEMsY0FBSUEsR0FBR1IsSUFBSCxLQUFZLGlCQUFaLElBQWlDUSxHQUFHQyxRQUFILENBQVlDLElBQVosS0FBcUIsU0FBMUQsRUFBcUU7QUFDbkVOLG9CQUFRTyxNQUFSLENBQWU7QUFDYk4sb0JBQU1HLEdBQUdJLEtBREk7QUFFYkMsdUJBQVUsd0NBQXVDTCxHQUFHSSxLQUFILENBQVNGLElBQUssSUFGbEQsRUFBZjtBQUdEO0FBQ0YsU0FORDtBQU9EO0FBVEksS0FBUDtBQVdEO0FBcEJjLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLW5hbWVkLWRlZmF1bHQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLW5hbWVkLWRlZmF1bHQnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlY2xhcmF0aW9uJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgbm9kZS5zcGVjaWZpZXJzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgICAgaWYgKGltLnR5cGUgPT09ICdJbXBvcnRTcGVjaWZpZXInICYmIGltLmltcG9ydGVkLm5hbWUgPT09ICdkZWZhdWx0Jykge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgICBub2RlOiBpbS5sb2NhbCxcbiAgICAgICAgICAgICAgbWVzc2FnZTogYFVzZSBkZWZhdWx0IGltcG9ydCBzeW50YXggdG8gaW1wb3J0ICcke2ltLmxvY2FsLm5hbWV9Jy5gIH0pXG4gICAgICAgICAgfVxuICAgICAgICB9KVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=