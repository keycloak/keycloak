'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-mutable-exports') },

    schema: [] },


  create: function () {function create(context) {
      function checkDeclaration(node) {var
        kind = node.kind;
        if (kind === 'var' || kind === 'let') {
          context.report(node, 'Exporting mutable \'' + String(kind) + '\' binding, use \'const\' instead.');
        }
      }

      function checkDeclarationsInScope(_ref, name) {var variables = _ref.variables;var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
          for (var _iterator = variables[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var variable = _step.value;
            if (variable.name === name) {var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {
                for (var _iterator2 = variable.defs[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var def = _step2.value;
                  if (def.type === 'Variable' && def.parent) {
                    checkDeclaration(def.parent);
                  }
                }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
            }
          }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
      }

      function handleExportDefault(node) {
        var scope = context.getScope();

        if (node.declaration.name) {
          checkDeclarationsInScope(scope, node.declaration.name);
        }
      }

      function handleExportNamed(node) {
        var scope = context.getScope();

        if (node.declaration) {
          checkDeclaration(node.declaration);
        } else if (!node.source) {var _iteratorNormalCompletion3 = true;var _didIteratorError3 = false;var _iteratorError3 = undefined;try {
            for (var _iterator3 = node.specifiers[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {var specifier = _step3.value;
              checkDeclarationsInScope(scope, specifier.local.name);
            }} catch (err) {_didIteratorError3 = true;_iteratorError3 = err;} finally {try {if (!_iteratorNormalCompletion3 && _iterator3['return']) {_iterator3['return']();}} finally {if (_didIteratorError3) {throw _iteratorError3;}}}
        }
      }

      return {
        'ExportDefaultDeclaration': handleExportDefault,
        'ExportNamedDeclaration': handleExportNamed };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1tdXRhYmxlLWV4cG9ydHMuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJjaGVja0RlY2xhcmF0aW9uIiwibm9kZSIsImtpbmQiLCJyZXBvcnQiLCJjaGVja0RlY2xhcmF0aW9uc0luU2NvcGUiLCJuYW1lIiwidmFyaWFibGVzIiwidmFyaWFibGUiLCJkZWZzIiwiZGVmIiwicGFyZW50IiwiaGFuZGxlRXhwb3J0RGVmYXVsdCIsInNjb3BlIiwiZ2V0U2NvcGUiLCJkZWNsYXJhdGlvbiIsImhhbmRsZUV4cG9ydE5hbWVkIiwic291cmNlIiwic3BlY2lmaWVycyIsInNwZWNpZmllciIsImxvY2FsIl0sIm1hcHBpbmdzIjoiYUFBQSxxQzs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsb0JBQVIsQ0FERCxFQUZGOztBQUtKQyxZQUFRLEVBTEosRUFEUzs7O0FBU2ZDLFFBVGUsK0JBU1JDLE9BVFEsRUFTQztBQUNkLGVBQVNDLGdCQUFULENBQTBCQyxJQUExQixFQUFnQztBQUN0QkMsWUFEc0IsR0FDYkQsSUFEYSxDQUN0QkMsSUFEc0I7QUFFOUIsWUFBSUEsU0FBUyxLQUFULElBQWtCQSxTQUFTLEtBQS9CLEVBQXNDO0FBQ3BDSCxrQkFBUUksTUFBUixDQUFlRixJQUFmLGtDQUEyQ0MsSUFBM0M7QUFDRDtBQUNGOztBQUVELGVBQVNFLHdCQUFULE9BQWlEQyxJQUFqRCxFQUF1RCxLQUFuQkMsU0FBbUIsUUFBbkJBLFNBQW1CO0FBQ3JELCtCQUF1QkEsU0FBdkIsOEhBQWtDLEtBQXZCQyxRQUF1QjtBQUNoQyxnQkFBSUEsU0FBU0YsSUFBVCxLQUFrQkEsSUFBdEIsRUFBNEI7QUFDMUIsc0NBQWtCRSxTQUFTQyxJQUEzQixtSUFBaUMsS0FBdEJDLEdBQXNCO0FBQy9CLHNCQUFJQSxJQUFJZixJQUFKLEtBQWEsVUFBYixJQUEyQmUsSUFBSUMsTUFBbkMsRUFBMkM7QUFDekNWLHFDQUFpQlMsSUFBSUMsTUFBckI7QUFDRDtBQUNGLGlCQUx5QjtBQU0zQjtBQUNGLFdBVG9EO0FBVXREOztBQUVELGVBQVNDLG1CQUFULENBQTZCVixJQUE3QixFQUFtQztBQUNqQyxZQUFNVyxRQUFRYixRQUFRYyxRQUFSLEVBQWQ7O0FBRUEsWUFBSVosS0FBS2EsV0FBTCxDQUFpQlQsSUFBckIsRUFBMkI7QUFDekJELG1DQUF5QlEsS0FBekIsRUFBZ0NYLEtBQUthLFdBQUwsQ0FBaUJULElBQWpEO0FBQ0Q7QUFDRjs7QUFFRCxlQUFTVSxpQkFBVCxDQUEyQmQsSUFBM0IsRUFBaUM7QUFDL0IsWUFBTVcsUUFBUWIsUUFBUWMsUUFBUixFQUFkOztBQUVBLFlBQUlaLEtBQUthLFdBQVQsRUFBdUI7QUFDckJkLDJCQUFpQkMsS0FBS2EsV0FBdEI7QUFDRCxTQUZELE1BRU8sSUFBSSxDQUFDYixLQUFLZSxNQUFWLEVBQWtCO0FBQ3ZCLGtDQUF3QmYsS0FBS2dCLFVBQTdCLG1JQUF5QyxLQUE5QkMsU0FBOEI7QUFDdkNkLHVDQUF5QlEsS0FBekIsRUFBZ0NNLFVBQVVDLEtBQVYsQ0FBZ0JkLElBQWhEO0FBQ0QsYUFIc0I7QUFJeEI7QUFDRjs7QUFFRCxhQUFPO0FBQ0wsb0NBQTRCTSxtQkFEdkI7QUFFTCxrQ0FBMEJJLGlCQUZyQixFQUFQOztBQUlELEtBckRjLG1CQUFqQiIsImZpbGUiOiJuby1tdXRhYmxlLWV4cG9ydHMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1tdXRhYmxlLWV4cG9ydHMnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW10sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBjaGVja0RlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgIGNvbnN0IHsga2luZCB9ID0gbm9kZTtcbiAgICAgIGlmIChraW5kID09PSAndmFyJyB8fCBraW5kID09PSAnbGV0Jykge1xuICAgICAgICBjb250ZXh0LnJlcG9ydChub2RlLCBgRXhwb3J0aW5nIG11dGFibGUgJyR7a2luZH0nIGJpbmRpbmcsIHVzZSAnY29uc3QnIGluc3RlYWQuYCk7XG4gICAgICB9XG4gICAgfVxuXG4gICAgZnVuY3Rpb24gY2hlY2tEZWNsYXJhdGlvbnNJblNjb3BlKHsgdmFyaWFibGVzIH0sIG5hbWUpIHtcbiAgICAgIGZvciAoY29uc3QgdmFyaWFibGUgb2YgdmFyaWFibGVzKSB7XG4gICAgICAgIGlmICh2YXJpYWJsZS5uYW1lID09PSBuYW1lKSB7XG4gICAgICAgICAgZm9yIChjb25zdCBkZWYgb2YgdmFyaWFibGUuZGVmcykge1xuICAgICAgICAgICAgaWYgKGRlZi50eXBlID09PSAnVmFyaWFibGUnICYmIGRlZi5wYXJlbnQpIHtcbiAgICAgICAgICAgICAgY2hlY2tEZWNsYXJhdGlvbihkZWYucGFyZW50KTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVFeHBvcnREZWZhdWx0KG5vZGUpIHtcbiAgICAgIGNvbnN0IHNjb3BlID0gY29udGV4dC5nZXRTY29wZSgpO1xuXG4gICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbi5uYW1lKSB7XG4gICAgICAgIGNoZWNrRGVjbGFyYXRpb25zSW5TY29wZShzY29wZSwgbm9kZS5kZWNsYXJhdGlvbi5uYW1lKTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVFeHBvcnROYW1lZChub2RlKSB7XG4gICAgICBjb25zdCBzY29wZSA9IGNvbnRleHQuZ2V0U2NvcGUoKTtcblxuICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24pICB7XG4gICAgICAgIGNoZWNrRGVjbGFyYXRpb24obm9kZS5kZWNsYXJhdGlvbik7XG4gICAgICB9IGVsc2UgaWYgKCFub2RlLnNvdXJjZSkge1xuICAgICAgICBmb3IgKGNvbnN0IHNwZWNpZmllciBvZiBub2RlLnNwZWNpZmllcnMpIHtcbiAgICAgICAgICBjaGVja0RlY2xhcmF0aW9uc0luU2NvcGUoc2NvcGUsIHNwZWNpZmllci5sb2NhbC5uYW1lKTtcbiAgICAgICAgfVxuICAgICAgfVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJzogaGFuZGxlRXhwb3J0RGVmYXVsdCxcbiAgICAgICdFeHBvcnROYW1lZERlY2xhcmF0aW9uJzogaGFuZGxlRXhwb3J0TmFtZWQsXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=