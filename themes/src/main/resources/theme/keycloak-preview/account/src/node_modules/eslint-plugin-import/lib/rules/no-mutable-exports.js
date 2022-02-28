'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-mutable-exports')
    }
  },

  create: function (context) {
    function checkDeclaration(node) {
      const kind = node.kind;

      if (kind === 'var' || kind === 'let') {
        context.report(node, `Exporting mutable '${kind}' binding, use 'const' instead.`);
      }
    }

    function checkDeclarationsInScope(_ref, name) {
      let variables = _ref.variables;

      for (let variable of variables) {
        if (variable.name === name) {
          for (let def of variable.defs) {
            if (def.type === 'Variable' && def.parent) {
              checkDeclaration(def.parent);
            }
          }
        }
      }
    }

    function handleExportDefault(node) {
      const scope = context.getScope();

      if (node.declaration.name) {
        checkDeclarationsInScope(scope, node.declaration.name);
      }
    }

    function handleExportNamed(node) {
      const scope = context.getScope();

      if (node.declaration) {
        checkDeclaration(node.declaration);
      } else if (!node.source) {
        for (let specifier of node.specifiers) {
          checkDeclarationsInScope(scope, specifier.local.name);
        }
      }
    }

    return {
      'ExportDefaultDeclaration': handleExportDefault,
      'ExportNamedDeclaration': handleExportNamed
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW11dGFibGUtZXhwb3J0cy5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsImNoZWNrRGVjbGFyYXRpb24iLCJub2RlIiwia2luZCIsInJlcG9ydCIsImNoZWNrRGVjbGFyYXRpb25zSW5TY29wZSIsIm5hbWUiLCJ2YXJpYWJsZXMiLCJ2YXJpYWJsZSIsImRlZiIsImRlZnMiLCJwYXJlbnQiLCJoYW5kbGVFeHBvcnREZWZhdWx0Iiwic2NvcGUiLCJnZXRTY29wZSIsImRlY2xhcmF0aW9uIiwiaGFuZGxlRXhwb3J0TmFtZWQiLCJzb3VyY2UiLCJzcGVjaWZpZXIiLCJzcGVjaWZpZXJzIiwibG9jYWwiXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7OztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxZQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxvQkFBUjtBQUREO0FBRkYsR0FEUzs7QUFRZkMsVUFBUSxVQUFVQyxPQUFWLEVBQW1CO0FBQ3pCLGFBQVNDLGdCQUFULENBQTBCQyxJQUExQixFQUFnQztBQUFBLFlBQ3ZCQyxJQUR1QixHQUNmRCxJQURlLENBQ3ZCQyxJQUR1Qjs7QUFFOUIsVUFBSUEsU0FBUyxLQUFULElBQWtCQSxTQUFTLEtBQS9CLEVBQXNDO0FBQ3BDSCxnQkFBUUksTUFBUixDQUFlRixJQUFmLEVBQXNCLHNCQUFxQkMsSUFBSyxpQ0FBaEQ7QUFDRDtBQUNGOztBQUVELGFBQVNFLHdCQUFULE9BQStDQyxJQUEvQyxFQUFxRDtBQUFBLFVBQWxCQyxTQUFrQixRQUFsQkEsU0FBa0I7O0FBQ25ELFdBQUssSUFBSUMsUUFBVCxJQUFxQkQsU0FBckIsRUFBZ0M7QUFDOUIsWUFBSUMsU0FBU0YsSUFBVCxLQUFrQkEsSUFBdEIsRUFBNEI7QUFDMUIsZUFBSyxJQUFJRyxHQUFULElBQWdCRCxTQUFTRSxJQUF6QixFQUErQjtBQUM3QixnQkFBSUQsSUFBSWIsSUFBSixLQUFhLFVBQWIsSUFBMkJhLElBQUlFLE1BQW5DLEVBQTJDO0FBQ3pDViwrQkFBaUJRLElBQUlFLE1BQXJCO0FBQ0Q7QUFDRjtBQUNGO0FBQ0Y7QUFDRjs7QUFFRCxhQUFTQyxtQkFBVCxDQUE2QlYsSUFBN0IsRUFBbUM7QUFDakMsWUFBTVcsUUFBUWIsUUFBUWMsUUFBUixFQUFkOztBQUVBLFVBQUlaLEtBQUthLFdBQUwsQ0FBaUJULElBQXJCLEVBQTJCO0FBQ3pCRCxpQ0FBeUJRLEtBQXpCLEVBQWdDWCxLQUFLYSxXQUFMLENBQWlCVCxJQUFqRDtBQUNEO0FBQ0Y7O0FBRUQsYUFBU1UsaUJBQVQsQ0FBMkJkLElBQTNCLEVBQWlDO0FBQy9CLFlBQU1XLFFBQVFiLFFBQVFjLFFBQVIsRUFBZDs7QUFFQSxVQUFJWixLQUFLYSxXQUFULEVBQXVCO0FBQ3JCZCx5QkFBaUJDLEtBQUthLFdBQXRCO0FBQ0QsT0FGRCxNQUVPLElBQUksQ0FBQ2IsS0FBS2UsTUFBVixFQUFrQjtBQUN2QixhQUFLLElBQUlDLFNBQVQsSUFBc0JoQixLQUFLaUIsVUFBM0IsRUFBdUM7QUFDckNkLG1DQUF5QlEsS0FBekIsRUFBZ0NLLFVBQVVFLEtBQVYsQ0FBZ0JkLElBQWhEO0FBQ0Q7QUFDRjtBQUNGOztBQUVELFdBQU87QUFDTCxrQ0FBNEJNLG1CQUR2QjtBQUVMLGdDQUEwQkk7QUFGckIsS0FBUDtBQUlEO0FBcERjLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLW11dGFibGUtZXhwb3J0cy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbXV0YWJsZS1leHBvcnRzJyksXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIChjb250ZXh0KSB7XG4gICAgZnVuY3Rpb24gY2hlY2tEZWNsYXJhdGlvbihub2RlKSB7XG4gICAgICBjb25zdCB7a2luZH0gPSBub2RlXG4gICAgICBpZiAoa2luZCA9PT0gJ3ZhcicgfHwga2luZCA9PT0gJ2xldCcpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQobm9kZSwgYEV4cG9ydGluZyBtdXRhYmxlICcke2tpbmR9JyBiaW5kaW5nLCB1c2UgJ2NvbnN0JyBpbnN0ZWFkLmApXG4gICAgICB9XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gY2hlY2tEZWNsYXJhdGlvbnNJblNjb3BlKHt2YXJpYWJsZXN9LCBuYW1lKSB7XG4gICAgICBmb3IgKGxldCB2YXJpYWJsZSBvZiB2YXJpYWJsZXMpIHtcbiAgICAgICAgaWYgKHZhcmlhYmxlLm5hbWUgPT09IG5hbWUpIHtcbiAgICAgICAgICBmb3IgKGxldCBkZWYgb2YgdmFyaWFibGUuZGVmcykge1xuICAgICAgICAgICAgaWYgKGRlZi50eXBlID09PSAnVmFyaWFibGUnICYmIGRlZi5wYXJlbnQpIHtcbiAgICAgICAgICAgICAgY2hlY2tEZWNsYXJhdGlvbihkZWYucGFyZW50KVxuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGhhbmRsZUV4cG9ydERlZmF1bHQobm9kZSkge1xuICAgICAgY29uc3Qgc2NvcGUgPSBjb250ZXh0LmdldFNjb3BlKClcblxuICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24ubmFtZSkge1xuICAgICAgICBjaGVja0RlY2xhcmF0aW9uc0luU2NvcGUoc2NvcGUsIG5vZGUuZGVjbGFyYXRpb24ubmFtZSlcbiAgICAgIH1cbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVFeHBvcnROYW1lZChub2RlKSB7XG4gICAgICBjb25zdCBzY29wZSA9IGNvbnRleHQuZ2V0U2NvcGUoKVxuXG4gICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbikgIHtcbiAgICAgICAgY2hlY2tEZWNsYXJhdGlvbihub2RlLmRlY2xhcmF0aW9uKVxuICAgICAgfSBlbHNlIGlmICghbm9kZS5zb3VyY2UpIHtcbiAgICAgICAgZm9yIChsZXQgc3BlY2lmaWVyIG9mIG5vZGUuc3BlY2lmaWVycykge1xuICAgICAgICAgIGNoZWNrRGVjbGFyYXRpb25zSW5TY29wZShzY29wZSwgc3BlY2lmaWVyLmxvY2FsLm5hbWUpXG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbic6IGhhbmRsZUV4cG9ydERlZmF1bHQsXG4gICAgICAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbic6IGhhbmRsZUV4cG9ydE5hbWVkLFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==