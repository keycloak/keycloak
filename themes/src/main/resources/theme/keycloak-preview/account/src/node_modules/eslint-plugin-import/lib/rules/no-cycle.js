'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }(); /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          * @fileOverview Ensures that no imported module imports the linted module.
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          * @author Ben Mosher
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          */

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

// todo: cache cycles / deep relationships for faster repeat evaluation
module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2.default)('no-cycle') },
    schema: [(0, _moduleVisitor.makeOptionsSchema)({
      maxDepth: {
        description: 'maximum dependency depth to traverse',
        type: 'integer',
        minimum: 1
      }
    })]
  },

  create: function (context) {
    const myPath = context.getFilename();
    if (myPath === '<text>') return {}; // can't cycle-check a non-file

    const options = context.options[0] || {};
    const maxDepth = options.maxDepth || Infinity;

    function checkSourceValue(sourceNode, importer) {
      const imported = _ExportMap2.default.get(sourceNode.value, context);

      if (sourceNode.parent && sourceNode.parent.importKind === 'type') {
        return; // no Flow import resolution
      }

      if (sourceNode._babelType === 'Literal') {
        return; // no Flow import resolution, workaround for ESLint < 5.x
      }

      if (imported == null) {
        return; // no-unresolved territory
      }

      if (imported.path === myPath) {
        return; // no-self-import territory
      }

      const untraversed = [{ mget: () => imported, route: [] }];
      const traversed = new Set();
      function detectCycle(_ref) {
        let mget = _ref.mget,
            route = _ref.route;

        const m = mget();
        if (m == null) return;
        if (traversed.has(m.path)) return;
        traversed.add(m.path);

        for (let _ref2 of m.imports) {
          var _ref3 = _slicedToArray(_ref2, 2);

          let path = _ref3[0];
          var _ref3$ = _ref3[1];
          let getter = _ref3$.getter;
          let source = _ref3$.source;

          if (path === myPath) return true;
          if (traversed.has(path)) continue;
          if (route.length + 1 < maxDepth) {
            untraversed.push({
              mget: getter,
              route: route.concat(source)
            });
          }
        }
      }

      while (untraversed.length > 0) {
        const next = untraversed.shift(); // bfs!
        if (detectCycle(next)) {
          const message = next.route.length > 0 ? `Dependency cycle via ${routeString(next.route)}` : 'Dependency cycle detected.';
          context.report(importer, message);
          return;
        }
      }
    }

    return (0, _moduleVisitor2.default)(checkSourceValue, context.options[0]);
  }
};

function routeString(route) {
  return route.map(s => `${s.value}:${s.loc.start.line}`).join('=>');
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWN5Y2xlLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJtYXhEZXB0aCIsImRlc2NyaXB0aW9uIiwibWluaW11bSIsImNyZWF0ZSIsImNvbnRleHQiLCJteVBhdGgiLCJnZXRGaWxlbmFtZSIsIm9wdGlvbnMiLCJJbmZpbml0eSIsImNoZWNrU291cmNlVmFsdWUiLCJzb3VyY2VOb2RlIiwiaW1wb3J0ZXIiLCJpbXBvcnRlZCIsIkV4cG9ydHMiLCJnZXQiLCJ2YWx1ZSIsInBhcmVudCIsImltcG9ydEtpbmQiLCJfYmFiZWxUeXBlIiwicGF0aCIsInVudHJhdmVyc2VkIiwibWdldCIsInJvdXRlIiwidHJhdmVyc2VkIiwiU2V0IiwiZGV0ZWN0Q3ljbGUiLCJtIiwiaGFzIiwiYWRkIiwiaW1wb3J0cyIsImdldHRlciIsInNvdXJjZSIsImxlbmd0aCIsInB1c2giLCJjb25jYXQiLCJuZXh0Iiwic2hpZnQiLCJtZXNzYWdlIiwicm91dGVTdHJpbmciLCJyZXBvcnQiLCJtYXAiLCJzIiwibG9jIiwic3RhcnQiLCJsaW5lIiwiam9pbiJdLCJtYXBwaW5ncyI6Ijs7eXBCQUFBOzs7OztBQUtBOzs7O0FBQ0E7Ozs7QUFDQTs7Ozs7O0FBRUE7QUFDQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNLEVBQUVDLEtBQUssdUJBQVEsVUFBUixDQUFQLEVBRkY7QUFHSkMsWUFBUSxDQUFDLHNDQUFrQjtBQUN6QkMsZ0JBQVM7QUFDUEMscUJBQWEsc0NBRE47QUFFUEwsY0FBTSxTQUZDO0FBR1BNLGlCQUFTO0FBSEY7QUFEZ0IsS0FBbEIsQ0FBRDtBQUhKLEdBRFM7O0FBYWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixVQUFNQyxTQUFTRCxRQUFRRSxXQUFSLEVBQWY7QUFDQSxRQUFJRCxXQUFXLFFBQWYsRUFBeUIsT0FB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ZU92ZXJ2aWV3IEVuc3VyZXMgdGhhdCBubyBpbXBvcnRlZCBtb2R1bGUgaW1wb3J0cyB0aGUgbGludGVkIG1vZHVsZS5cbiAqIEBhdXRob3IgQmVuIE1vc2hlclxuICovXG5cbmltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCdcbmltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxuLy8gdG9kbzogY2FjaGUgY3ljbGVzIC8gZGVlcCByZWxhdGlvbnNoaXBzIGZvciBmYXN0ZXIgcmVwZWF0IGV2YWx1YXRpb25cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHsgdXJsOiBkb2NzVXJsKCduby1jeWNsZScpIH0sXG4gICAgc2NoZW1hOiBbbWFrZU9wdGlvbnNTY2hlbWEoe1xuICAgICAgbWF4RGVwdGg6e1xuICAgICAgICBkZXNjcmlwdGlvbjogJ21heGltdW0gZGVwZW5kZW5jeSBkZXB0aCB0byB0cmF2ZXJzZScsXG4gICAgICAgIHR5cGU6ICdpbnRlZ2VyJyxcbiAgICAgICAgbWluaW11bTogMSxcbiAgICAgIH0sXG4gICAgfSldLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBteVBhdGggPSBjb250ZXh0LmdldEZpbGVuYW1lKClcbiAgICBpZiAobXlQYXRoID09PSAnPHRleHQ+JykgcmV0dXJuIHt9IC8vIGNhbid0IGN5Y2xlLWNoZWNrIGEgbm9uLWZpbGVcblxuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge31cbiAgICBjb25zdCBtYXhEZXB0aCA9IG9wdGlvbnMubWF4RGVwdGggfHwgSW5maW5pdHlcblxuICAgIGZ1bmN0aW9uIGNoZWNrU291cmNlVmFsdWUoc291cmNlTm9kZSwgaW1wb3J0ZXIpIHtcbiAgICAgIGNvbnN0IGltcG9ydGVkID0gRXhwb3J0cy5nZXQoc291cmNlTm9kZS52YWx1ZSwgY29udGV4dClcblxuICAgICAgaWYgKHNvdXJjZU5vZGUucGFyZW50ICYmIHNvdXJjZU5vZGUucGFyZW50LmltcG9ydEtpbmQgPT09ICd0eXBlJykge1xuICAgICAgICByZXR1cm4gLy8gbm8gRmxvdyBpbXBvcnQgcmVzb2x1dGlvblxuICAgICAgfVxuXG4gICAgICBpZiAoc291cmNlTm9kZS5fYmFiZWxUeXBlID09PSAnTGl0ZXJhbCcpIHtcbiAgICAgICAgcmV0dXJuIC8vIG5vIEZsb3cgaW1wb3J0IHJlc29sdXRpb24sIHdvcmthcm91bmQgZm9yIEVTTGludCA8IDUueFxuICAgICAgfVxuXG4gICAgICBpZiAoaW1wb3J0ZWQgPT0gbnVsbCkge1xuICAgICAgICByZXR1cm4gIC8vIG5vLXVucmVzb2x2ZWQgdGVycml0b3J5XG4gICAgICB9XG5cbiAgICAgIGlmIChpbXBvcnRlZC5wYXRoID09PSBteVBhdGgpIHtcbiAgICAgICAgcmV0dXJuICAvLyBuby1zZWxmLWltcG9ydCB0ZXJyaXRvcnlcbiAgICAgIH1cblxuICAgICAgY29uc3QgdW50cmF2ZXJzZWQgPSBbe21nZXQ6ICgpID0+IGltcG9ydGVkLCByb3V0ZTpbXX1dXG4gICAgICBjb25zdCB0cmF2ZXJzZWQgPSBuZXcgU2V0KClcbiAgICAgIGZ1bmN0aW9uIGRldGVjdEN5Y2xlKHttZ2V0LCByb3V0ZX0pIHtcbiAgICAgICAgY29uc3QgbSA9IG1nZXQoKVxuICAgICAgICBpZiAobSA9PSBudWxsKSByZXR1cm5cbiAgICAgICAgaWYgKHRyYXZlcnNlZC5oYXMobS5wYXRoKSkgcmV0dXJuXG4gICAgICAgIHRyYXZlcnNlZC5hZGQobS5wYXRoKVxuXG4gICAgICAgIGZvciAobGV0IFtwYXRoLCB7IGdldHRlciwgc291cmNlIH1dIG9mIG0uaW1wb3J0cykge1xuICAgICAgICAgIGlmIChwYXRoID09PSBteVBhdGgpIHJldHVybiB0cnVlXG4gICAgICAgICAgaWYgKHRyYXZlcnNlZC5oYXMocGF0aCkpIGNvbnRpbnVlXG4gICAgICAgICAgaWYgKHJvdXRlLmxlbmd0aCArIDEgPCBtYXhEZXB0aCkge1xuICAgICAgICAgICAgdW50cmF2ZXJzZWQucHVzaCh7XG4gICAgICAgICAgICAgIG1nZXQ6IGdldHRlcixcbiAgICAgICAgICAgICAgcm91dGU6IHJvdXRlLmNvbmNhdChzb3VyY2UpLFxuICAgICAgICAgICAgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH1cblxuICAgICAgd2hpbGUgKHVudHJhdmVyc2VkLmxlbmd0aCA+IDApIHtcbiAgICAgICAgY29uc3QgbmV4dCA9IHVudHJhdmVyc2VkLnNoaWZ0KCkgLy8gYmZzIVxuICAgICAgICBpZiAoZGV0ZWN0Q3ljbGUobmV4dCkpIHtcbiAgICAgICAgICBjb25zdCBtZXNzYWdlID0gKG5leHQucm91dGUubGVuZ3RoID4gMFxuICAgICAgICAgICAgPyBgRGVwZW5kZW5jeSBjeWNsZSB2aWEgJHtyb3V0ZVN0cmluZyhuZXh0LnJvdXRlKX1gXG4gICAgICAgICAgICA6ICdEZXBlbmRlbmN5IGN5Y2xlIGRldGVjdGVkLicpXG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1wb3J0ZXIsIG1lc3NhZ2UpXG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBjb250ZXh0Lm9wdGlvbnNbMF0pXG4gIH0sXG59XG5cbmZ1bmN0aW9uIHJvdXRlU3RyaW5nKHJvdXRlKSB7XG4gIHJldHVybiByb3V0ZS5tYXAocyA9PiBgJHtzLnZhbHVlfToke3MubG9jLnN0YXJ0LmxpbmV9YCkuam9pbignPT4nKVxufVxuIl19