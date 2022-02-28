'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('export')
    }
  },

  create: function (context) {
    const named = new Map();

    function addNamed(name, node) {
      let nodes = named.get(name);

      if (nodes == null) {
        nodes = new Set();
        named.set(name, nodes);
      }

      nodes.add(node);
    }

    return {
      'ExportDefaultDeclaration': node => addNamed('default', node),

      'ExportSpecifier': function (node) {
        addNamed(node.exported.name, node.exported);
      },

      'ExportNamedDeclaration': function (node) {
        if (node.declaration == null) return;

        if (node.declaration.id != null) {
          addNamed(node.declaration.id.name, node.declaration.id);
        }

        if (node.declaration.declarations != null) {
          for (let declaration of node.declaration.declarations) {
            (0, _ExportMap.recursivePatternCapture)(declaration.id, v => addNamed(v.name, v));
          }
        }
      },

      'ExportAllDeclaration': function (node) {
        if (node.source == null) return; // not sure if this is ever true

        const remoteExports = _ExportMap2.default.get(node.source.value, context);
        if (remoteExports == null) return;

        if (remoteExports.errors.length) {
          remoteExports.reportErrors(context, node);
          return;
        }
        let any = false;
        remoteExports.forEach((v, name) => name !== 'default' && (any = true) && // poor man's filter
        addNamed(name, node));

        if (!any) {
          context.report(node.source, `No named exports found in module '${node.source.value}'.`);
        }
      },

      'Program:exit': function () {
        for (let _ref of named) {
          var _ref2 = _slicedToArray(_ref, 2);

          let name = _ref2[0];
          let nodes = _ref2[1];

          if (nodes.size <= 1) continue;

          for (let node of nodes) {
            if (name === 'default') {
              context.report(node, 'Multiple default exports.');
            } else context.report(node, `Multiple exports of name '${name}'.`);
          }
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL2V4cG9ydC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsIm5hbWVkIiwiTWFwIiwiYWRkTmFtZWQiLCJuYW1lIiwibm9kZSIsIm5vZGVzIiwiZ2V0IiwiU2V0Iiwic2V0IiwiYWRkIiwiZXhwb3J0ZWQiLCJkZWNsYXJhdGlvbiIsImlkIiwiZGVjbGFyYXRpb25zIiwidiIsInNvdXJjZSIsInJlbW90ZUV4cG9ydHMiLCJFeHBvcnRNYXAiLCJ2YWx1ZSIsImVycm9ycyIsImxlbmd0aCIsInJlcG9ydEVycm9ycyIsImFueSIsImZvckVhY2giLCJyZXBvcnQiLCJzaXplIl0sIm1hcHBpbmdzIjoiOzs7O0FBQUE7Ozs7QUFDQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLFFBQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixVQUFNQyxRQUFRLElBQUlDLEdBQUosRUFBZDs7QUFFQSxhQUFTQyxRQUFULENBQWtCQyxJQUFsQixFQUF3QkMsSUFBeEIsRUFBOEI7QUFDNUIsVUFBSUMsUUFBUUwsTUFBTU0sR0FBTixDQUFVSCxJQUFWLENBQVo7O0FBRUEsVUFBSUUsU0FBUyxJQUFiLEVBQW1CO0FBQ2pCQSxnQkFBUSxJQUFJRSxHQUFKLEVBQVI7QUFDQVAsY0FBTVEsR0FBTixDQUFVTCxJQUFWLEVBQWdCRSxLQUFoQjtBQUNEOztBQUVEQSxZQUFNSSxHQUFOLENBQVVMLElBQVY7QUFDRDs7QUFFRCxXQUFPO0FBQ0wsa0NBQTZCQSxJQUFELElBQVVGLFNBQVMsU0FBVCxFQUFvQkUsSUFBcEIsQ0FEakM7O0FBR0wseUJBQW1CLFVBQVVBLElBQVYsRUFBZ0I7QUFDakNGLGlCQUFTRSxLQUFLTSxRQUFMLENBQWNQLElBQXZCLEVBQTZCQyxLQUFLTSxRQUFsQztBQUNELE9BTEk7O0FBT0wsZ0NBQTBCLFVBQVVOLElBQVYsRUFBZ0I7QUFDeEMsWUFBSUEsS0FBS08sV0FBTCxJQUFvQixJQUF4QixFQUE4Qjs7QUFFOUIsWUFBSVAsS0FBS08sV0FBTCxDQUFpQkMsRUFBakIsSUFBdUIsSUFBM0IsRUFBaUM7QUFDL0JWLG1CQUFTRSxLQUFLTyxXQUFMLENBQWlCQyxFQUFqQixDQUFvQlQsSUFBN0IsRUFBbUNDLEtBQUtPLFdBQUwsQ0FBaUJDLEVBQXBEO0FBQ0Q7O0FBRUQsWUFBSVIsS0FBS08sV0FBTCxDQUFpQkUsWUFBakIsSUFBaUMsSUFBckMsRUFBMkM7QUFDekMsZUFBSyxJQUFJRixXQUFULElBQXdCUCxLQUFLTyxXQUFMLENBQWlCRSxZQUF6QyxFQUF1RDtBQUNyRCxvREFBd0JGLFlBQVlDLEVBQXBDLEVBQXdDRSxLQUFLWixTQUFTWSxFQUFFWCxJQUFYLEVBQWlCVyxDQUFqQixDQUE3QztBQUNEO0FBQ0Y7QUFDRixPQW5CSTs7QUFxQkwsOEJBQXdCLFVBQVVWLElBQVYsRUFBZ0I7QUFDdEMsWUFBSUEsS0FBS1csTUFBTCxJQUFlLElBQW5CLEVBQXlCLE9BRGEsQ0FDTjs7QUFFaEMsY0FBTUMsZ0JBQWdCQyxvQkFBVVgsR0FBVixDQUFjRixLQUFLVyxNQUFMLENBQVlHLEtBQTFCLEVBQWlDbkIsT0FBakMsQ0FBdEI7QUFDQSxZQUFJaUIsaUJBQWlCLElBQXJCLEVBQTJCOztBQUUzQixZQUFJQSxjQUFjRyxNQUFkLENBQXFCQyxNQUF6QixFQUFpQztBQUMvQkosd0JBQWNLLFlBQWQsQ0FBMkJ0QixPQUEzQixFQUFvQ0ssSUFBcEM7QUFDQTtBQUNEO0FBQ0QsWUFBSWtCLE1BQU0sS0FBVjtBQUNBTixzQkFBY08sT0FBZCxDQUFzQixDQUFDVCxDQUFELEVBQUlYLElBQUosS0FDcEJBLFNBQVMsU0FBVCxLQUNDbUIsTUFBTSxJQURQLEtBQ2dCO0FBQ2hCcEIsaUJBQVNDLElBQVQsRUFBZUMsSUFBZixDQUhGOztBQUtBLFlBQUksQ0FBQ2tCLEdBQUwsRUFBVTtBQUNSdkIsa0JBQVF5QixNQUFSLENBQWVwQixLQUFLVyxNQUFwQixFQUNHLHFDQUFvQ1gsS0FBS1csTUFBTCxDQUFZRyxLQUFNLElBRHpEO0FBRUQ7QUFDRixPQXpDSTs7QUEyQ0wsc0JBQWdCLFlBQVk7QUFDMUIseUJBQTBCbEIsS0FBMUIsRUFBaUM7QUFBQTs7QUFBQSxjQUF2QkcsSUFBdUI7QUFBQSxjQUFqQkUsS0FBaUI7O0FBQy9CLGNBQUlBLE1BQU1vQixJQUFOLElBQWMsQ0FBbEIsRUFBcUI7O0FBRXJCLGVBQUssSUFBSXJCLElBQVQsSUFBaUJDLEtBQWpCLEVBQXdCO0FBQ3RCLGdCQUFJRixTQUFTLFNBQWIsRUFBd0I7QUFDdEJKLHNCQUFReUIsTUFBUixDQUFlcEIsSUFBZixFQUFxQiwyQkFBckI7QUFDRCxhQUZELE1BRU9MLFFBQVF5QixNQUFSLENBQWVwQixJQUFmLEVBQXNCLDZCQUE0QkQsSUFBSyxJQUF2RDtBQUNSO0FBQ0Y7QUFDRjtBQXJESSxLQUFQO0FBdUREO0FBN0VjLENBQWpCIiwiZmlsZSI6InJ1bGVzL2V4cG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBFeHBvcnRNYXAsIHsgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUgfSBmcm9tICcuLi9FeHBvcnRNYXAnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ2V4cG9ydCcpLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGNvbnN0IG5hbWVkID0gbmV3IE1hcCgpXG5cbiAgICBmdW5jdGlvbiBhZGROYW1lZChuYW1lLCBub2RlKSB7XG4gICAgICBsZXQgbm9kZXMgPSBuYW1lZC5nZXQobmFtZSlcblxuICAgICAgaWYgKG5vZGVzID09IG51bGwpIHtcbiAgICAgICAgbm9kZXMgPSBuZXcgU2V0KClcbiAgICAgICAgbmFtZWQuc2V0KG5hbWUsIG5vZGVzKVxuICAgICAgfVxuXG4gICAgICBub2Rlcy5hZGQobm9kZSlcbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbic6IChub2RlKSA9PiBhZGROYW1lZCgnZGVmYXVsdCcsIG5vZGUpLFxuXG4gICAgICAnRXhwb3J0U3BlY2lmaWVyJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgYWRkTmFtZWQobm9kZS5leHBvcnRlZC5uYW1lLCBub2RlLmV4cG9ydGVkKVxuICAgICAgfSxcblxuICAgICAgJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nOiBmdW5jdGlvbiAobm9kZSkge1xuICAgICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbiA9PSBudWxsKSByZXR1cm5cblxuICAgICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbi5pZCAhPSBudWxsKSB7XG4gICAgICAgICAgYWRkTmFtZWQobm9kZS5kZWNsYXJhdGlvbi5pZC5uYW1lLCBub2RlLmRlY2xhcmF0aW9uLmlkKVxuICAgICAgICB9XG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zICE9IG51bGwpIHtcbiAgICAgICAgICBmb3IgKGxldCBkZWNsYXJhdGlvbiBvZiBub2RlLmRlY2xhcmF0aW9uLmRlY2xhcmF0aW9ucykge1xuICAgICAgICAgICAgcmVjdXJzaXZlUGF0dGVybkNhcHR1cmUoZGVjbGFyYXRpb24uaWQsIHYgPT4gYWRkTmFtZWQodi5uYW1lLCB2KSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG5cbiAgICAgICdFeHBvcnRBbGxEZWNsYXJhdGlvbic6IGZ1bmN0aW9uIChub2RlKSB7XG4gICAgICAgIGlmIChub2RlLnNvdXJjZSA9PSBudWxsKSByZXR1cm4gLy8gbm90IHN1cmUgaWYgdGhpcyBpcyBldmVyIHRydWVcblxuICAgICAgICBjb25zdCByZW1vdGVFeHBvcnRzID0gRXhwb3J0TWFwLmdldChub2RlLnNvdXJjZS52YWx1ZSwgY29udGV4dClcbiAgICAgICAgaWYgKHJlbW90ZUV4cG9ydHMgPT0gbnVsbCkgcmV0dXJuXG5cbiAgICAgICAgaWYgKHJlbW90ZUV4cG9ydHMuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICAgIHJlbW90ZUV4cG9ydHMucmVwb3J0RXJyb3JzKGNvbnRleHQsIG5vZGUpXG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cbiAgICAgICAgbGV0IGFueSA9IGZhbHNlXG4gICAgICAgIHJlbW90ZUV4cG9ydHMuZm9yRWFjaCgodiwgbmFtZSkgPT5cbiAgICAgICAgICBuYW1lICE9PSAnZGVmYXVsdCcgJiZcbiAgICAgICAgICAoYW55ID0gdHJ1ZSkgJiYgLy8gcG9vciBtYW4ncyBmaWx0ZXJcbiAgICAgICAgICBhZGROYW1lZChuYW1lLCBub2RlKSlcblxuICAgICAgICBpZiAoIWFueSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUuc291cmNlLFxuICAgICAgICAgICAgYE5vIG5hbWVkIGV4cG9ydHMgZm91bmQgaW4gbW9kdWxlICcke25vZGUuc291cmNlLnZhbHVlfScuYClcbiAgICAgICAgfVxuICAgICAgfSxcblxuICAgICAgJ1Byb2dyYW06ZXhpdCc6IGZ1bmN0aW9uICgpIHtcbiAgICAgICAgZm9yIChsZXQgW25hbWUsIG5vZGVzXSBvZiBuYW1lZCkge1xuICAgICAgICAgIGlmIChub2Rlcy5zaXplIDw9IDEpIGNvbnRpbnVlXG5cbiAgICAgICAgICBmb3IgKGxldCBub2RlIG9mIG5vZGVzKSB7XG4gICAgICAgICAgICBpZiAobmFtZSA9PT0gJ2RlZmF1bHQnKSB7XG4gICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUsICdNdWx0aXBsZSBkZWZhdWx0IGV4cG9ydHMuJylcbiAgICAgICAgICAgIH0gZWxzZSBjb250ZXh0LnJlcG9ydChub2RlLCBgTXVsdGlwbGUgZXhwb3J0cyBvZiBuYW1lICcke25hbWV9Jy5gKVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=