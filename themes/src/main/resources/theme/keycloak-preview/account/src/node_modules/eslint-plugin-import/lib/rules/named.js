'use strict';

var _path = require('path');

var path = _interopRequireWildcard(_path);

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } else { var newObj = {}; if (obj != null) { for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) newObj[key] = obj[key]; } } newObj.default = obj; return newObj; } }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('named')
    }
  },

  create: function (context) {
    function checkSpecifiers(key, type, node) {
      // ignore local exports and type imports
      if (node.source == null || node.importKind === 'type') return;

      if (!node.specifiers.some(function (im) {
        return im.type === type;
      })) {
        return; // no named imports/exports
      }

      const imports = _ExportMap2.default.get(node.source.value, context);
      if (imports == null) return;

      if (imports.errors.length) {
        imports.reportErrors(context, node);
        return;
      }

      node.specifiers.forEach(function (im) {
        if (im.type !== type) return;

        // ignore type imports
        if (im.importKind === 'type') return;

        const deepLookup = imports.hasDeep(im[key].name);

        if (!deepLookup.found) {
          if (deepLookup.path.length > 1) {
            const deepPath = deepLookup.path.map(i => path.relative(path.dirname(context.getFilename()), i.path)).join(' -> ');

            context.report(im[key], `${im[key].name} not found via ${deepPath}`);
          } else {
            context.report(im[key], im[key].name + ' not found in \'' + node.source.value + '\'');
          }
        }
      });
    }

    return {
      'ImportDeclaration': checkSpecifiers.bind(null, 'imported', 'ImportSpecifier'),

      'ExportNamedDeclaration': checkSpecifiers.bind(null, 'local', 'ExportSpecifier')
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25hbWVkLmpzIl0sIm5hbWVzIjpbInBhdGgiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsImNoZWNrU3BlY2lmaWVycyIsImtleSIsIm5vZGUiLCJzb3VyY2UiLCJpbXBvcnRLaW5kIiwic3BlY2lmaWVycyIsInNvbWUiLCJpbSIsImltcG9ydHMiLCJFeHBvcnRzIiwiZ2V0IiwidmFsdWUiLCJlcnJvcnMiLCJsZW5ndGgiLCJyZXBvcnRFcnJvcnMiLCJmb3JFYWNoIiwiZGVlcExvb2t1cCIsImhhc0RlZXAiLCJuYW1lIiwiZm91bmQiLCJkZWVwUGF0aCIsIm1hcCIsImkiLCJyZWxhdGl2ZSIsImRpcm5hbWUiLCJnZXRGaWxlbmFtZSIsImpvaW4iLCJyZXBvcnQiLCJiaW5kIl0sIm1hcHBpbmdzIjoiOztBQUFBOztJQUFZQSxJOztBQUNaOzs7O0FBQ0E7Ozs7Ozs7O0FBRUFDLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFNBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLE9BQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBVUMsT0FBVixFQUFtQjtBQUN6QixhQUFTQyxlQUFULENBQXlCQyxHQUF6QixFQUE4Qk4sSUFBOUIsRUFBb0NPLElBQXBDLEVBQTBDO0FBQ3hDO0FBQ0EsVUFBSUEsS0FBS0MsTUFBTCxJQUFlLElBQWYsSUFBdUJELEtBQUtFLFVBQUwsS0FBb0IsTUFBL0MsRUFBdUQ7O0FBRXZELFVBQUksQ0FBQ0YsS0FBS0csVUFBTCxDQUNFQyxJQURGLENBQ08sVUFBVUMsRUFBVixFQUFjO0FBQUUsZUFBT0EsR0FBR1osSUFBSCxLQUFZQSxJQUFuQjtBQUF5QixPQURoRCxDQUFMLEVBQ3dEO0FBQ3RELGVBRHNELENBQy9DO0FBQ1I7O0FBRUQsWUFBTWEsVUFBVUMsb0JBQVFDLEdBQVIsQ0FBWVIsS0FBS0MsTUFBTCxDQUFZUSxLQUF4QixFQUErQlosT0FBL0IsQ0FBaEI7QUFDQSxVQUFJUyxXQUFXLElBQWYsRUFBcUI7O0FBRXJCLFVBQUlBLFFBQVFJLE1BQVIsQ0FBZUMsTUFBbkIsRUFBMkI7QUFDekJMLGdCQUFRTSxZQUFSLENBQXFCZixPQUFyQixFQUE4QkcsSUFBOUI7QUFDQTtBQUNEOztBQUVEQSxXQUFLRyxVQUFMLENBQWdCVSxPQUFoQixDQUF3QixVQUFVUixFQUFWLEVBQWM7QUFDcEMsWUFBSUEsR0FBR1osSUFBSCxLQUFZQSxJQUFoQixFQUFzQjs7QUFFdEI7QUFDQSxZQUFJWSxHQUFHSCxVQUFILEtBQWtCLE1BQXRCLEVBQThCOztBQUU5QixjQUFNWSxhQUFhUixRQUFRUyxPQUFSLENBQWdCVixHQUFHTixHQUFILEVBQVFpQixJQUF4QixDQUFuQjs7QUFFQSxZQUFJLENBQUNGLFdBQVdHLEtBQWhCLEVBQXVCO0FBQ3JCLGNBQUlILFdBQVd6QixJQUFYLENBQWdCc0IsTUFBaEIsR0FBeUIsQ0FBN0IsRUFBZ0M7QUFDOUIsa0JBQU1PLFdBQVdKLFdBQVd6QixJQUFYLENBQ2Q4QixHQURjLENBQ1ZDLEtBQUsvQixLQUFLZ0MsUUFBTCxDQUFjaEMsS0FBS2lDLE9BQUwsQ0FBYXpCLFFBQVEwQixXQUFSLEVBQWIsQ0FBZCxFQUFtREgsRUFBRS9CLElBQXJELENBREssRUFFZG1DLElBRmMsQ0FFVCxNQUZTLENBQWpCOztBQUlBM0Isb0JBQVE0QixNQUFSLENBQWVwQixHQUFHTixHQUFILENBQWYsRUFDRyxHQUFFTSxHQUFHTixHQUFILEVBQVFpQixJQUFLLGtCQUFpQkUsUUFBUyxFQUQ1QztBQUVELFdBUEQsTUFPTztBQUNMckIsb0JBQVE0QixNQUFSLENBQWVwQixHQUFHTixHQUFILENBQWYsRUFDRU0sR0FBR04sR0FBSCxFQUFRaUIsSUFBUixHQUFlLGtCQUFmLEdBQW9DaEIsS0FBS0MsTUFBTCxDQUFZUSxLQUFoRCxHQUF3RCxJQUQxRDtBQUVEO0FBQ0Y7QUFDRixPQXJCRDtBQXNCRDs7QUFFRCxXQUFPO0FBQ0wsMkJBQXFCWCxnQkFBZ0I0QixJQUFoQixDQUFzQixJQUF0QixFQUNzQixVQUR0QixFQUVzQixpQkFGdEIsQ0FEaEI7O0FBTUwsZ0NBQTBCNUIsZ0JBQWdCNEIsSUFBaEIsQ0FBc0IsSUFBdEIsRUFDc0IsT0FEdEIsRUFFc0IsaUJBRnRCO0FBTnJCLEtBQVA7QUFZRDtBQTlEYyxDQUFqQiIsImZpbGUiOiJydWxlcy9uYW1lZC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCAqIGFzIHBhdGggZnJvbSAncGF0aCdcbmltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbmFtZWQnKSxcbiAgICB9LFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gKGNvbnRleHQpIHtcbiAgICBmdW5jdGlvbiBjaGVja1NwZWNpZmllcnMoa2V5LCB0eXBlLCBub2RlKSB7XG4gICAgICAvLyBpZ25vcmUgbG9jYWwgZXhwb3J0cyBhbmQgdHlwZSBpbXBvcnRzXG4gICAgICBpZiAobm9kZS5zb3VyY2UgPT0gbnVsbCB8fCBub2RlLmltcG9ydEtpbmQgPT09ICd0eXBlJykgcmV0dXJuXG5cbiAgICAgIGlmICghbm9kZS5zcGVjaWZpZXJzXG4gICAgICAgICAgICAuc29tZShmdW5jdGlvbiAoaW0pIHsgcmV0dXJuIGltLnR5cGUgPT09IHR5cGUgfSkpIHtcbiAgICAgICAgcmV0dXJuIC8vIG5vIG5hbWVkIGltcG9ydHMvZXhwb3J0c1xuICAgICAgfVxuXG4gICAgICBjb25zdCBpbXBvcnRzID0gRXhwb3J0cy5nZXQobm9kZS5zb3VyY2UudmFsdWUsIGNvbnRleHQpXG4gICAgICBpZiAoaW1wb3J0cyA9PSBudWxsKSByZXR1cm5cblxuICAgICAgaWYgKGltcG9ydHMuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICBpbXBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBub2RlKVxuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgbm9kZS5zcGVjaWZpZXJzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgIGlmIChpbS50eXBlICE9PSB0eXBlKSByZXR1cm5cblxuICAgICAgICAvLyBpZ25vcmUgdHlwZSBpbXBvcnRzXG4gICAgICAgIGlmIChpbS5pbXBvcnRLaW5kID09PSAndHlwZScpIHJldHVyblxuXG4gICAgICAgIGNvbnN0IGRlZXBMb29rdXAgPSBpbXBvcnRzLmhhc0RlZXAoaW1ba2V5XS5uYW1lKVxuXG4gICAgICAgIGlmICghZGVlcExvb2t1cC5mb3VuZCkge1xuICAgICAgICAgIGlmIChkZWVwTG9va3VwLnBhdGgubGVuZ3RoID4gMSkge1xuICAgICAgICAgICAgY29uc3QgZGVlcFBhdGggPSBkZWVwTG9va3VwLnBhdGhcbiAgICAgICAgICAgICAgLm1hcChpID0+IHBhdGgucmVsYXRpdmUocGF0aC5kaXJuYW1lKGNvbnRleHQuZ2V0RmlsZW5hbWUoKSksIGkucGF0aCkpXG4gICAgICAgICAgICAgIC5qb2luKCcgLT4gJylcblxuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1ba2V5XSxcbiAgICAgICAgICAgICAgYCR7aW1ba2V5XS5uYW1lfSBub3QgZm91bmQgdmlhICR7ZGVlcFBhdGh9YClcbiAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoaW1ba2V5XSxcbiAgICAgICAgICAgICAgaW1ba2V5XS5uYW1lICsgJyBub3QgZm91bmQgaW4gXFwnJyArIG5vZGUuc291cmNlLnZhbHVlICsgJ1xcJycpXG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9KVxuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnSW1wb3J0RGVjbGFyYXRpb24nOiBjaGVja1NwZWNpZmllcnMuYmluZCggbnVsbFxuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAsICdpbXBvcnRlZCdcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnSW1wb3J0U3BlY2lmaWVyJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICApLFxuXG4gICAgICAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbic6IGNoZWNrU3BlY2lmaWVycy5iaW5kKCBudWxsXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnbG9jYWwnXG4gICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgLCAnRXhwb3J0U3BlY2lmaWVyJ1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICAgICksXG4gICAgfVxuXG4gIH0sXG59XG4iXX0=