'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function checkImports(imported, context) {
  for (let _ref of imported.entries()) {
    var _ref2 = _slicedToArray(_ref, 2);

    let module = _ref2[0];
    let nodes = _ref2[1];

    if (nodes.size > 1) {
      for (let node of nodes) {
        context.report(node, `'${module}' imported multiple times.`);
      }
    }
  }
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-duplicates')
    }
  },

  create: function (context) {
    const imported = new Map();
    const typesImported = new Map();
    return {
      'ImportDeclaration': function (n) {
        // resolved path will cover aliased duplicates
        const resolvedPath = (0, _resolve2.default)(n.source.value, context) || n.source.value;
        const importMap = n.importKind === 'type' ? typesImported : imported;

        if (importMap.has(resolvedPath)) {
          importMap.get(resolvedPath).add(n.source);
        } else {
          importMap.set(resolvedPath, new Set([n.source]));
        }
      },

      'Program:exit': function () {
        checkImports(imported, context);
        checkImports(typesImported, context);
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWR1cGxpY2F0ZXMuanMiXSwibmFtZXMiOlsiY2hlY2tJbXBvcnRzIiwiaW1wb3J0ZWQiLCJjb250ZXh0IiwiZW50cmllcyIsIm1vZHVsZSIsIm5vZGVzIiwic2l6ZSIsIm5vZGUiLCJyZXBvcnQiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiTWFwIiwidHlwZXNJbXBvcnRlZCIsIm4iLCJyZXNvbHZlZFBhdGgiLCJzb3VyY2UiLCJ2YWx1ZSIsImltcG9ydE1hcCIsImltcG9ydEtpbmQiLCJoYXMiLCJnZXQiLCJhZGQiLCJzZXQiLCJTZXQiXSwibWFwcGluZ3MiOiI7Ozs7QUFBQTs7OztBQUNBOzs7Ozs7QUFFQSxTQUFTQSxZQUFULENBQXNCQyxRQUF0QixFQUFnQ0MsT0FBaEMsRUFBeUM7QUFDdkMsbUJBQTRCRCxTQUFTRSxPQUFULEVBQTVCLEVBQWdEO0FBQUE7O0FBQUEsUUFBdENDLE1BQXNDO0FBQUEsUUFBOUJDLEtBQThCOztBQUM5QyxRQUFJQSxNQUFNQyxJQUFOLEdBQWEsQ0FBakIsRUFBb0I7QUFDbEIsV0FBSyxJQUFJQyxJQUFULElBQWlCRixLQUFqQixFQUF3QjtBQUN0QkgsZ0JBQVFNLE1BQVIsQ0FBZUQsSUFBZixFQUFzQixJQUFHSCxNQUFPLDRCQUFoQztBQUNEO0FBQ0Y7QUFDRjtBQUNGOztBQUVEQSxPQUFPSyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxlQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVVaLE9BQVYsRUFBbUI7QUFDekIsVUFBTUQsV0FBVyxJQUFJYyxHQUFKLEVBQWpCO0FBQ0EsVUFBTUMsZ0JBQWdCLElBQUlELEdBQUosRUFBdEI7QUFDQSxXQUFPO0FBQ0wsMkJBQXFCLFVBQVVFLENBQVYsRUFBYTtBQUNoQztBQUNBLGNBQU1DLGVBQWUsdUJBQVFELEVBQUVFLE1BQUYsQ0FBU0MsS0FBakIsRUFBd0JsQixPQUF4QixLQUFvQ2UsRUFBRUUsTUFBRixDQUFTQyxLQUFsRTtBQUNBLGNBQU1DLFlBQVlKLEVBQUVLLFVBQUYsS0FBaUIsTUFBakIsR0FBMEJOLGFBQTFCLEdBQTBDZixRQUE1RDs7QUFFQSxZQUFJb0IsVUFBVUUsR0FBVixDQUFjTCxZQUFkLENBQUosRUFBaUM7QUFDL0JHLG9CQUFVRyxHQUFWLENBQWNOLFlBQWQsRUFBNEJPLEdBQTVCLENBQWdDUixFQUFFRSxNQUFsQztBQUNELFNBRkQsTUFFTztBQUNMRSxvQkFBVUssR0FBVixDQUFjUixZQUFkLEVBQTRCLElBQUlTLEdBQUosQ0FBUSxDQUFDVixFQUFFRSxNQUFILENBQVIsQ0FBNUI7QUFDRDtBQUNGLE9BWEk7O0FBYUwsc0JBQWdCLFlBQVk7QUFDMUJuQixxQkFBYUMsUUFBYixFQUF1QkMsT0FBdkI7QUFDQUYscUJBQWFnQixhQUFiLEVBQTRCZCxPQUE1QjtBQUNEO0FBaEJJLEtBQVA7QUFrQkQ7QUE3QmMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tZHVwbGljYXRlcy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIGNoZWNrSW1wb3J0cyhpbXBvcnRlZCwgY29udGV4dCkge1xuICBmb3IgKGxldCBbbW9kdWxlLCBub2Rlc10gb2YgaW1wb3J0ZWQuZW50cmllcygpKSB7XG4gICAgaWYgKG5vZGVzLnNpemUgPiAxKSB7XG4gICAgICBmb3IgKGxldCBub2RlIG9mIG5vZGVzKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUsIGAnJHttb2R1bGV9JyBpbXBvcnRlZCBtdWx0aXBsZSB0aW1lcy5gKVxuICAgICAgfVxuICAgIH1cbiAgfVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWR1cGxpY2F0ZXMnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBjb25zdCBpbXBvcnRlZCA9IG5ldyBNYXAoKVxuICAgIGNvbnN0IHR5cGVzSW1wb3J0ZWQgPSBuZXcgTWFwKClcbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlY2xhcmF0aW9uJzogZnVuY3Rpb24gKG4pIHtcbiAgICAgICAgLy8gcmVzb2x2ZWQgcGF0aCB3aWxsIGNvdmVyIGFsaWFzZWQgZHVwbGljYXRlc1xuICAgICAgICBjb25zdCByZXNvbHZlZFBhdGggPSByZXNvbHZlKG4uc291cmNlLnZhbHVlLCBjb250ZXh0KSB8fCBuLnNvdXJjZS52YWx1ZVxuICAgICAgICBjb25zdCBpbXBvcnRNYXAgPSBuLmltcG9ydEtpbmQgPT09ICd0eXBlJyA/IHR5cGVzSW1wb3J0ZWQgOiBpbXBvcnRlZFxuXG4gICAgICAgIGlmIChpbXBvcnRNYXAuaGFzKHJlc29sdmVkUGF0aCkpIHtcbiAgICAgICAgICBpbXBvcnRNYXAuZ2V0KHJlc29sdmVkUGF0aCkuYWRkKG4uc291cmNlKVxuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIGltcG9ydE1hcC5zZXQocmVzb2x2ZWRQYXRoLCBuZXcgU2V0KFtuLnNvdXJjZV0pKVxuICAgICAgICB9XG4gICAgICB9LFxuXG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24gKCkge1xuICAgICAgICBjaGVja0ltcG9ydHMoaW1wb3J0ZWQsIGNvbnRleHQpXG4gICAgICAgIGNoZWNrSW1wb3J0cyh0eXBlc0ltcG9ydGVkLCBjb250ZXh0KVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=