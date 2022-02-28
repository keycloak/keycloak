'use strict';

var _moduleVisitor = require('eslint-module-utils/moduleVisitor');

var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _path = require('path');

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _importType = require('../core/importType');

var _importType2 = _interopRequireDefault(_importType);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-relative-parent-imports')
    },
    schema: [(0, _moduleVisitor.makeOptionsSchema)()]
  },

  create: function noRelativePackages(context) {
    const myPath = context.getFilename();
    if (myPath === '<text>') return {}; // can't check a non-file

    function checkSourceValue(sourceNode) {
      const depPath = sourceNode.value;

      if ((0, _importType2.default)(depPath, context) === 'external') {
        // ignore packages
        return;
      }

      const absDepPath = (0, _resolve2.default)(depPath, context);

      if (!absDepPath) {
        // unable to resolve path
        return;
      }

      const relDepPath = (0, _path.relative)((0, _path.dirname)(myPath), absDepPath);

      if ((0, _importType2.default)(relDepPath, context) === 'parent') {
        context.report({
          node: sourceNode,
          message: 'Relative imports from parent directories are not allowed. ' + `Please either pass what you're importing through at runtime ` + `(dependency injection), move \`${(0, _path.basename)(myPath)}\` to same ` + `directory as \`${depPath}\` or consider making \`${depPath}\` a package.`
        });
      }
    }

    return (0, _moduleVisitor2.default)(checkSourceValue, context.options[0]);
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXJlbGF0aXZlLXBhcmVudC1pbXBvcnRzLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJub1JlbGF0aXZlUGFja2FnZXMiLCJjb250ZXh0IiwibXlQYXRoIiwiZ2V0RmlsZW5hbWUiLCJjaGVja1NvdXJjZVZhbHVlIiwic291cmNlTm9kZSIsImRlcFBhdGgiLCJ2YWx1ZSIsImFic0RlcFBhdGgiLCJyZWxEZXBQYXRoIiwicmVwb3J0Iiwibm9kZSIsIm1lc3NhZ2UiLCJvcHRpb25zIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7QUFDQTs7QUFDQTs7OztBQUVBOzs7Ozs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsNEJBQVI7QUFERCxLQUZGO0FBS0pDLFlBQVEsQ0FBQyx1Q0FBRDtBQUxKLEdBRFM7O0FBU2ZDLFVBQVEsU0FBU0Msa0JBQVQsQ0FBNEJDLE9BQTVCLEVBQXFDO0FBQzNDLFVBQU1DLFNBQVNELFFBQVFFLFdBQVIsRUFBZjtBQUNBLFFBQUlELFdBQVcsUUFBZixFQUF5QixPQUFPLEVBQVAsQ0FGa0IsQ0FFUjs7QUFFbkMsYUFBU0UsZ0JBQVQsQ0FBMEJDLFVBQTFCLEVBQXNDO0FBQ3BDLFlBQU1DLFVBQVVELFdBQVdFLEtBQTNCOztBQUVBLFVBQUksMEJBQVdELE9BQVgsRUFBb0JMLE9BQXBCLE1BQWlDLFVBQXJDLEVBQWlEO0FBQUU7QUFDakQ7QUFDRDs7QUFFRCxZQUFNTyxhQUFhLHVCQUFRRixPQUFSLEVBQWlCTCxPQUFqQixDQUFuQjs7QUFFQSxVQUFJLENBQUNPLFVBQUwsRUFBaUI7QUFBRTtBQUNqQjtBQUNEOztBQUVELFlBQU1DLGFBQWEsb0JBQVMsbUJBQVFQLE1BQVIsQ0FBVCxFQUEwQk0sVUFBMUIsQ0FBbkI7O0FBRUEsVUFBSSwwQkFBV0MsVUFBWCxFQUF1QlIsT0FBdkIsTUFBb0MsUUFBeEMsRUFBa0Q7QUFDaERBLGdCQUFRUyxNQUFSLENBQWU7QUFDYkMsZ0JBQU1OLFVBRE87QUFFYk8sbUJBQVMsK0RBQ04sOERBRE0sR0FFTixrQ0FBaUMsb0JBQVNWLE1BQVQsQ0FBaUIsYUFGNUMsR0FHTixrQkFBaUJJLE9BQVEsMkJBQTBCQSxPQUFRO0FBTGpELFNBQWY7QUFPRDtBQUNGOztBQUVELFdBQU8sNkJBQWNGLGdCQUFkLEVBQWdDSCxRQUFRWSxPQUFSLENBQWdCLENBQWhCLENBQWhDLENBQVA7QUFDRDtBQXhDYyxDQUFqQiIsImZpbGUiOiJydWxlcy9uby1yZWxhdGl2ZS1wYXJlbnQtaW1wb3J0cy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcbmltcG9ydCB7IGJhc2VuYW1lLCBkaXJuYW1lLCByZWxhdGl2ZSB9IGZyb20gJ3BhdGgnXG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnXG5cbmltcG9ydCBpbXBvcnRUeXBlIGZyb20gJy4uL2NvcmUvaW1wb3J0VHlwZSdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1yZWxhdGl2ZS1wYXJlbnQtaW1wb3J0cycpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbbWFrZU9wdGlvbnNTY2hlbWEoKV0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiBub1JlbGF0aXZlUGFja2FnZXMoY29udGV4dCkge1xuICAgIGNvbnN0IG15UGF0aCA9IGNvbnRleHQuZ2V0RmlsZW5hbWUoKVxuICAgIGlmIChteVBhdGggPT09ICc8dGV4dD4nKSByZXR1cm4ge30gLy8gY2FuJ3QgY2hlY2sgYSBub24tZmlsZVxuXG4gICAgZnVuY3Rpb24gY2hlY2tTb3VyY2VWYWx1ZShzb3VyY2VOb2RlKSB7XG4gICAgICBjb25zdCBkZXBQYXRoID0gc291cmNlTm9kZS52YWx1ZVxuXG4gICAgICBpZiAoaW1wb3J0VHlwZShkZXBQYXRoLCBjb250ZXh0KSA9PT0gJ2V4dGVybmFsJykgeyAvLyBpZ25vcmUgcGFja2FnZXNcbiAgICAgICAgcmV0dXJuXG4gICAgICB9XG5cbiAgICAgIGNvbnN0IGFic0RlcFBhdGggPSByZXNvbHZlKGRlcFBhdGgsIGNvbnRleHQpXG5cbiAgICAgIGlmICghYWJzRGVwUGF0aCkgeyAvLyB1bmFibGUgdG8gcmVzb2x2ZSBwYXRoXG4gICAgICAgIHJldHVyblxuICAgICAgfVxuXG4gICAgICBjb25zdCByZWxEZXBQYXRoID0gcmVsYXRpdmUoZGlybmFtZShteVBhdGgpLCBhYnNEZXBQYXRoKVxuXG4gICAgICBpZiAoaW1wb3J0VHlwZShyZWxEZXBQYXRoLCBjb250ZXh0KSA9PT0gJ3BhcmVudCcpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgIG5vZGU6IHNvdXJjZU5vZGUsXG4gICAgICAgICAgbWVzc2FnZTogJ1JlbGF0aXZlIGltcG9ydHMgZnJvbSBwYXJlbnQgZGlyZWN0b3JpZXMgYXJlIG5vdCBhbGxvd2VkLiAnICtcbiAgICAgICAgICAgIGBQbGVhc2UgZWl0aGVyIHBhc3Mgd2hhdCB5b3UncmUgaW1wb3J0aW5nIHRocm91Z2ggYXQgcnVudGltZSBgICtcbiAgICAgICAgICAgIGAoZGVwZW5kZW5jeSBpbmplY3Rpb24pLCBtb3ZlIFxcYCR7YmFzZW5hbWUobXlQYXRoKX1cXGAgdG8gc2FtZSBgICtcbiAgICAgICAgICAgIGBkaXJlY3RvcnkgYXMgXFxgJHtkZXBQYXRofVxcYCBvciBjb25zaWRlciBtYWtpbmcgXFxgJHtkZXBQYXRofVxcYCBhIHBhY2thZ2UuYCxcbiAgICAgICAgfSlcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBjb250ZXh0Lm9wdGlvbnNbMF0pXG4gIH0sXG59XG4iXX0=