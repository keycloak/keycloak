'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function getImportValue(node) {
  return node.type === 'ImportDeclaration' ?
  node.source.value :
  node.moduleReference.expression.value;
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('first') },

    fixable: 'code',
    schema: [
    {
      type: 'string',
      'enum': ['absolute-first', 'disable-absolute-first'] }] },




  create: function () {function create(context) {
      function isPossibleDirective(node) {
        return node.type === 'ExpressionStatement' &&
        node.expression.type === 'Literal' &&
        typeof node.expression.value === 'string';
      }

      return {
        'Program': function () {function Program(n) {
            var body = n.body;
            if (!body) {
              return;
            }
            var absoluteFirst = context.options[0] === 'absolute-first';
            var message = 'Import in body of module; reorder to top.';
            var sourceCode = context.getSourceCode();
            var originSourceCode = sourceCode.getText();
            var nonImportCount = 0;
            var anyExpressions = false;
            var anyRelative = false;
            var lastLegalImp = null;
            var errorInfos = [];
            var shouldSort = true;
            var lastSortNodesIndex = 0;
            body.forEach(function (node, index) {
              if (!anyExpressions && isPossibleDirective(node)) {
                return;
              }

              anyExpressions = true;

              if (node.type === 'ImportDeclaration' || node.type === 'TSImportEqualsDeclaration') {
                if (absoluteFirst) {
                  if (/^\./.test(getImportValue(node))) {
                    anyRelative = true;
                  } else if (anyRelative) {
                    context.report({
                      node: node.type === 'ImportDeclaration' ? node.source : node.moduleReference,
                      message: 'Absolute imports should come before relative imports.' });

                  }
                }
                if (nonImportCount > 0) {var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
                    for (var _iterator = context.getDeclaredVariables(node)[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var variable = _step.value;
                      if (!shouldSort) break;
                      var references = variable.references;
                      if (references.length) {var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {
                          for (var _iterator2 = references[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var reference = _step2.value;
                            if (reference.identifier.range[0] < node.range[1]) {
                              shouldSort = false;
                              break;
                            }
                          }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
                      }
                    }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
                  shouldSort && (lastSortNodesIndex = errorInfos.length);
                  errorInfos.push({
                    node: node,
                    range: [body[index - 1].range[1], node.range[1]] });

                } else {
                  lastLegalImp = node;
                }
              } else {
                nonImportCount++;
              }
            });
            if (!errorInfos.length) return;
            errorInfos.forEach(function (errorInfo, index) {
              var node = errorInfo.node;
              var infos = {
                node: node,
                message: message };

              if (index < lastSortNodesIndex) {
                infos.fix = function (fixer) {
                  return fixer.insertTextAfter(node, '');
                };
              } else if (index === lastSortNodesIndex) {
                var sortNodes = errorInfos.slice(0, lastSortNodesIndex + 1);
                infos.fix = function (fixer) {
                  var removeFixers = sortNodes.map(function (_errorInfo) {
                    return fixer.removeRange(_errorInfo.range);
                  });
                  var range = [0, removeFixers[removeFixers.length - 1].range[1]];
                  var insertSourceCode = sortNodes.map(function (_errorInfo) {
                    var nodeSourceCode = String.prototype.slice.apply(
                    originSourceCode, _errorInfo.range);

                    if (/\S/.test(nodeSourceCode[0])) {
                      return '\n' + nodeSourceCode;
                    }
                    return nodeSourceCode;
                  }).join('');
                  var insertFixer = null;
                  var replaceSourceCode = '';
                  if (!lastLegalImp) {
                    insertSourceCode =
                    insertSourceCode.trim() + insertSourceCode.match(/^(\s+)/)[0];
                  }
                  insertFixer = lastLegalImp ?
                  fixer.insertTextAfter(lastLegalImp, insertSourceCode) :
                  fixer.insertTextBefore(body[0], insertSourceCode);
                  var fixers = [insertFixer].concat(removeFixers);
                  fixers.forEach(function (computedFixer, i) {
                    replaceSourceCode += originSourceCode.slice(
                    fixers[i - 1] ? fixers[i - 1].range[1] : 0, computedFixer.range[0]) +
                    computedFixer.text;
                  });
                  return fixer.replaceTextRange(range, replaceSourceCode);
                };
              }
              context.report(infos);
            });
          }return Program;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9maXJzdC5qcyJdLCJuYW1lcyI6WyJnZXRJbXBvcnRWYWx1ZSIsIm5vZGUiLCJ0eXBlIiwic291cmNlIiwidmFsdWUiLCJtb2R1bGVSZWZlcmVuY2UiLCJleHByZXNzaW9uIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwiZml4YWJsZSIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJpc1Bvc3NpYmxlRGlyZWN0aXZlIiwibiIsImJvZHkiLCJhYnNvbHV0ZUZpcnN0Iiwib3B0aW9ucyIsIm1lc3NhZ2UiLCJzb3VyY2VDb2RlIiwiZ2V0U291cmNlQ29kZSIsIm9yaWdpblNvdXJjZUNvZGUiLCJnZXRUZXh0Iiwibm9uSW1wb3J0Q291bnQiLCJhbnlFeHByZXNzaW9ucyIsImFueVJlbGF0aXZlIiwibGFzdExlZ2FsSW1wIiwiZXJyb3JJbmZvcyIsInNob3VsZFNvcnQiLCJsYXN0U29ydE5vZGVzSW5kZXgiLCJmb3JFYWNoIiwiaW5kZXgiLCJ0ZXN0IiwicmVwb3J0IiwiZ2V0RGVjbGFyZWRWYXJpYWJsZXMiLCJ2YXJpYWJsZSIsInJlZmVyZW5jZXMiLCJsZW5ndGgiLCJyZWZlcmVuY2UiLCJpZGVudGlmaWVyIiwicmFuZ2UiLCJwdXNoIiwiZXJyb3JJbmZvIiwiaW5mb3MiLCJmaXgiLCJmaXhlciIsImluc2VydFRleHRBZnRlciIsInNvcnROb2RlcyIsInNsaWNlIiwicmVtb3ZlRml4ZXJzIiwibWFwIiwiX2Vycm9ySW5mbyIsInJlbW92ZVJhbmdlIiwiaW5zZXJ0U291cmNlQ29kZSIsIm5vZGVTb3VyY2VDb2RlIiwiU3RyaW5nIiwicHJvdG90eXBlIiwiYXBwbHkiLCJqb2luIiwiaW5zZXJ0Rml4ZXIiLCJyZXBsYWNlU291cmNlQ29kZSIsInRyaW0iLCJtYXRjaCIsImluc2VydFRleHRCZWZvcmUiLCJmaXhlcnMiLCJjb25jYXQiLCJjb21wdXRlZEZpeGVyIiwiaSIsInRleHQiLCJyZXBsYWNlVGV4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LENBQWhCLEdBQXlDLENBRHJCLEVBQ3dCMEIsY0FBYzFCLEtBQWQsQ0FBb0IsQ0FBcEIsQ0FEeEI7QUFFbEIwQixrQ0FBY0UsSUFGbEI7QUFHRCxtQkFKRDtBQUtBLHlCQUFPdkIsTUFBTXdCLGdCQUFOLENBQXVCN0IsS0FBdkIsRUFBOEJvQixpQkFBOUIsQ0FBUDtBQUNELGlCQTlCRDtBQStCRDtBQUNEaEQsc0JBQVFxQixNQUFSLENBQWVVLEtBQWY7QUFDRCxhQTdDRDtBQThDRCxXQTFHRCxrQkFESyxFQUFQOztBQTZHRCxLQW5JYyxtQkFBakIiLCJmaWxlIjoiZmlyc3QuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuZnVuY3Rpb24gZ2V0SW1wb3J0VmFsdWUobm9kZSkge1xuICByZXR1cm4gbm9kZS50eXBlID09PSAnSW1wb3J0RGVjbGFyYXRpb24nXG4gICAgPyBub2RlLnNvdXJjZS52YWx1ZVxuICAgIDogbm9kZS5tb2R1bGVSZWZlcmVuY2UuZXhwcmVzc2lvbi52YWx1ZTtcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdmaXJzdCcpLFxuICAgIH0sXG4gICAgZml4YWJsZTogJ2NvZGUnLFxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgZW51bTogWydhYnNvbHV0ZS1maXJzdCcsICdkaXNhYmxlLWFic29sdXRlLWZpcnN0J10sXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBpc1Bvc3NpYmxlRGlyZWN0aXZlKG5vZGUpIHtcbiAgICAgIHJldHVybiBub2RlLnR5cGUgPT09ICdFeHByZXNzaW9uU3RhdGVtZW50JyAmJlxuICAgICAgICBub2RlLmV4cHJlc3Npb24udHlwZSA9PT0gJ0xpdGVyYWwnICYmXG4gICAgICAgIHR5cGVvZiBub2RlLmV4cHJlc3Npb24udmFsdWUgPT09ICdzdHJpbmcnO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnUHJvZ3JhbSc6IGZ1bmN0aW9uIChuKSB7XG4gICAgICAgIGNvbnN0IGJvZHkgPSBuLmJvZHk7XG4gICAgICAgIGlmICghYm9keSkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBjb25zdCBhYnNvbHV0ZUZpcnN0ID0gY29udGV4dC5vcHRpb25zWzBdID09PSAnYWJzb2x1dGUtZmlyc3QnO1xuICAgICAgICBjb25zdCBtZXNzYWdlID0gJ0ltcG9ydCBpbiBib2R5IG9mIG1vZHVsZTsgcmVvcmRlciB0byB0b3AuJztcbiAgICAgICAgY29uc3Qgc291cmNlQ29kZSA9IGNvbnRleHQuZ2V0U291cmNlQ29kZSgpO1xuICAgICAgICBjb25zdCBvcmlnaW5Tb3VyY2VDb2RlID0gc291cmNlQ29kZS5nZXRUZXh0KCk7XG4gICAgICAgIGxldCBub25JbXBvcnRDb3VudCA9IDA7XG4gICAgICAgIGxldCBhbnlFeHByZXNzaW9ucyA9IGZhbHNlO1xuICAgICAgICBsZXQgYW55UmVsYXRpdmUgPSBmYWxzZTtcbiAgICAgICAgbGV0IGxhc3RMZWdhbEltcCA9IG51bGw7XG4gICAgICAgIGNvbnN0IGVycm9ySW5mb3MgPSBbXTtcbiAgICAgICAgbGV0IHNob3VsZFNvcnQgPSB0cnVlO1xuICAgICAgICBsZXQgbGFzdFNvcnROb2Rlc0luZGV4ID0gMDtcbiAgICAgICAgYm9keS5mb3JFYWNoKGZ1bmN0aW9uIChub2RlLCBpbmRleCkge1xuICAgICAgICAgIGlmICghYW55RXhwcmVzc2lvbnMgJiYgaXNQb3NzaWJsZURpcmVjdGl2ZShub2RlKSkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGFueUV4cHJlc3Npb25zID0gdHJ1ZTtcblxuICAgICAgICAgIGlmIChub2RlLnR5cGUgPT09ICdJbXBvcnREZWNsYXJhdGlvbicgfHwgbm9kZS50eXBlID09PSAnVFNJbXBvcnRFcXVhbHNEZWNsYXJhdGlvbicpIHtcbiAgICAgICAgICAgIGlmIChhYnNvbHV0ZUZpcnN0KSB7XG4gICAgICAgICAgICAgIGlmICgvXlxcLi8udGVzdChnZXRJbXBvcnRWYWx1ZShub2RlKSkpIHtcbiAgICAgICAgICAgICAgICBhbnlSZWxhdGl2ZSA9IHRydWU7XG4gICAgICAgICAgICAgIH0gZWxzZSBpZiAoYW55UmVsYXRpdmUpIHtcbiAgICAgICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgICAgICBub2RlOiBub2RlLnR5cGUgPT09ICdJbXBvcnREZWNsYXJhdGlvbicgPyBub2RlLnNvdXJjZSA6IG5vZGUubW9kdWxlUmVmZXJlbmNlLFxuICAgICAgICAgICAgICAgICAgbWVzc2FnZTogJ0Fic29sdXRlIGltcG9ydHMgc2hvdWxkIGNvbWUgYmVmb3JlIHJlbGF0aXZlIGltcG9ydHMuJyxcbiAgICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgaWYgKG5vbkltcG9ydENvdW50ID4gMCkge1xuICAgICAgICAgICAgICBmb3IgKGNvbnN0IHZhcmlhYmxlIG9mIGNvbnRleHQuZ2V0RGVjbGFyZWRWYXJpYWJsZXMobm9kZSkpIHtcbiAgICAgICAgICAgICAgICBpZiAoIXNob3VsZFNvcnQpIGJyZWFrO1xuICAgICAgICAgICAgICAgIGNvbnN0IHJlZmVyZW5jZXMgPSB2YXJpYWJsZS5yZWZlcmVuY2VzO1xuICAgICAgICAgICAgICAgIGlmIChyZWZlcmVuY2VzLmxlbmd0aCkge1xuICAgICAgICAgICAgICAgICAgZm9yIChjb25zdCByZWZlcmVuY2Ugb2YgcmVmZXJlbmNlcykge1xuICAgICAgICAgICAgICAgICAgICBpZiAocmVmZXJlbmNlLmlkZW50aWZpZXIucmFuZ2VbMF0gPCBub2RlLnJhbmdlWzFdKSB7XG4gICAgICAgICAgICAgICAgICAgICAgc2hvdWxkU29ydCA9IGZhbHNlO1xuICAgICAgICAgICAgICAgICAgICAgIGJyZWFrO1xuICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgIHNob3VsZFNvcnQgJiYgKGxhc3RTb3J0Tm9kZXNJbmRleCA9IGVycm9ySW5mb3MubGVuZ3RoKTtcbiAgICAgICAgICAgICAgZXJyb3JJbmZvcy5wdXNoKHtcbiAgICAgICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgICAgIHJhbmdlOiBbYm9keVtpbmRleCAtIDFdLnJhbmdlWzFdLCBub2RlLnJhbmdlWzFdXSxcbiAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICBsYXN0TGVnYWxJbXAgPSBub2RlO1xuICAgICAgICAgICAgfVxuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBub25JbXBvcnRDb3VudCsrO1xuICAgICAgICAgIH1cbiAgICAgICAgfSk7XG4gICAgICAgIGlmICghZXJyb3JJbmZvcy5sZW5ndGgpIHJldHVybjtcbiAgICAgICAgZXJyb3JJbmZvcy5mb3JFYWNoKGZ1bmN0aW9uIChlcnJvckluZm8sIGluZGV4KSB7XG4gICAgICAgICAgY29uc3Qgbm9kZSA9IGVycm9ySW5mby5ub2RlO1xuICAgICAgICAgIGNvbnN0IGluZm9zID0ge1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2UsXG4gICAgICAgICAgfTtcbiAgICAgICAgICBpZiAoaW5kZXggPCBsYXN0U29ydE5vZGVzSW5kZXgpIHtcbiAgICAgICAgICAgIGluZm9zLmZpeCA9IGZ1bmN0aW9uIChmaXhlcikge1xuICAgICAgICAgICAgICByZXR1cm4gZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKG5vZGUsICcnKTtcbiAgICAgICAgICAgIH07XG4gICAgICAgICAgfSBlbHNlIGlmIChpbmRleCA9PT0gbGFzdFNvcnROb2Rlc0luZGV4KSB7XG4gICAgICAgICAgICBjb25zdCBzb3J0Tm9kZXMgPSBlcnJvckluZm9zLnNsaWNlKDAsIGxhc3RTb3J0Tm9kZXNJbmRleCArIDEpO1xuICAgICAgICAgICAgaW5mb3MuZml4ID0gZnVuY3Rpb24gKGZpeGVyKSB7XG4gICAgICAgICAgICAgIGNvbnN0IHJlbW92ZUZpeGVycyA9IHNvcnROb2Rlcy5tYXAoZnVuY3Rpb24gKF9lcnJvckluZm8pIHtcbiAgICAgICAgICAgICAgICByZXR1cm4gZml4ZXIucmVtb3ZlUmFuZ2UoX2Vycm9ySW5mby5yYW5nZSk7XG4gICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgICBjb25zdCByYW5nZSA9IFswLCByZW1vdmVGaXhlcnNbcmVtb3ZlRml4ZXJzLmxlbmd0aCAtIDFdLnJhbmdlWzFdXTtcbiAgICAgICAgICAgICAgbGV0IGluc2VydFNvdXJjZUNvZGUgPSBzb3J0Tm9kZXMubWFwKGZ1bmN0aW9uIChfZXJyb3JJbmZvKSB7XG4gICAgICAgICAgICAgICAgY29uc3Qgbm9kZVNvdXJjZUNvZGUgPSBTdHJpbmcucHJvdG90eXBlLnNsaWNlLmFwcGx5KFxuICAgICAgICAgICAgICAgICAgb3JpZ2luU291cmNlQ29kZSwgX2Vycm9ySW5mby5yYW5nZSxcbiAgICAgICAgICAgICAgICApO1xuICAgICAgICAgICAgICAgIGlmICgvXFxTLy50ZXN0KG5vZGVTb3VyY2VDb2RlWzBdKSkge1xuICAgICAgICAgICAgICAgICAgcmV0dXJuICdcXG4nICsgbm9kZVNvdXJjZUNvZGU7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIHJldHVybiBub2RlU291cmNlQ29kZTtcbiAgICAgICAgICAgICAgfSkuam9pbignJyk7XG4gICAgICAgICAgICAgIGxldCBpbnNlcnRGaXhlciA9IG51bGw7XG4gICAgICAgICAgICAgIGxldCByZXBsYWNlU291cmNlQ29kZSA9ICcnO1xuICAgICAgICAgICAgICBpZiAoIWxhc3RMZWdhbEltcCkge1xuICAgICAgICAgICAgICAgIGluc2VydFNvdXJjZUNvZGUgPVxuICAgICAgICAgICAgICAgICAgICBpbnNlcnRTb3VyY2VDb2RlLnRyaW0oKSArIGluc2VydFNvdXJjZUNvZGUubWF0Y2goL14oXFxzKykvKVswXTtcbiAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICBpbnNlcnRGaXhlciA9IGxhc3RMZWdhbEltcCA/XG4gICAgICAgICAgICAgICAgZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKGxhc3RMZWdhbEltcCwgaW5zZXJ0U291cmNlQ29kZSkgOlxuICAgICAgICAgICAgICAgIGZpeGVyLmluc2VydFRleHRCZWZvcmUoYm9keVswXSwgaW5zZXJ0U291cmNlQ29kZSk7XG4gICAgICAgICAgICAgIGNvbnN0IGZpeGVycyA9IFtpbnNlcnRGaXhlcl0uY29uY2F0KHJlbW92ZUZpeGVycyk7XG4gICAgICAgICAgICAgIGZpeGVycy5mb3JFYWNoKGZ1bmN0aW9uIChjb21wdXRlZEZpeGVyLCBpKSB7XG4gICAgICAgICAgICAgICAgcmVwbGFjZVNvdXJjZUNvZGUgKz0gKG9yaWdpblNvdXJjZUNvZGUuc2xpY2UoXG4gICAgICAgICAgICAgICAgICBmaXhlcnNbaSAtIDFdID8gZml4ZXJzW2kgLSAxXS5yYW5nZVsxXSA6IDAsIGNvbXB1dGVkRml4ZXIucmFuZ2VbMF0sXG4gICAgICAgICAgICAgICAgKSArIGNvbXB1dGVkRml4ZXIudGV4dCk7XG4gICAgICAgICAgICAgIH0pO1xuICAgICAgICAgICAgICByZXR1cm4gZml4ZXIucmVwbGFjZVRleHRSYW5nZShyYW5nZSwgcmVwbGFjZVNvdXJjZUNvZGUpO1xuICAgICAgICAgICAgfTtcbiAgICAgICAgICB9XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoaW5mb3MpO1xuICAgICAgICB9KTtcbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=