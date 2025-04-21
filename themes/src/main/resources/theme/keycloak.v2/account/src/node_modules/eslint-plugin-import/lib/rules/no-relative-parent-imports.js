'use strict';var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);
var _path = require('path');
var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);

var _importType = require('../core/importType');var _importType2 = _interopRequireDefault(_importType);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-relative-parent-imports') },

    schema: [(0, _moduleVisitor.makeOptionsSchema)()] },


  create: function () {function noRelativePackages(context) {
      var myPath = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();
      if (myPath === '<text>') return {}; // can't check a non-file

      function checkSourceValue(sourceNode) {
        var depPath = sourceNode.value;

        if ((0, _importType2['default'])(depPath, context) === 'external') {// ignore packages
          return;
        }

        var absDepPath = (0, _resolve2['default'])(depPath, context);

        if (!absDepPath) {// unable to resolve path
          return;
        }

        var relDepPath = (0, _path.relative)((0, _path.dirname)(myPath), absDepPath);

        if ((0, _importType2['default'])(relDepPath, context) === 'parent') {
          context.report({
            node: sourceNode,
            message: 'Relative imports from parent directories are not allowed. ' + 'Please either pass what you\'re importing through at runtime ' + ('(dependency injection), move `' + String(

            (0, _path.basename)(myPath)) + '` to same ') + ('directory as `' + String(
            depPath) + '` or consider making `' + String(depPath) + '` a package.') });

        }
      }

      return (0, _moduleVisitor2['default'])(checkSourceValue, context.options[0]);
    }return noRelativePackages;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1yZWxhdGl2ZS1wYXJlbnQtaW1wb3J0cy5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwibm9SZWxhdGl2ZVBhY2thZ2VzIiwiY29udGV4dCIsIm15UGF0aCIsImdldFBoeXNpY2FsRmlsZW5hbWUiLCJnZXRGaWxlbmFtZSIsImNoZWNrU291cmNlVmFsdWUiLCJzb3VyY2VOb2RlIiwiZGVwUGF0aCIsInZhbHVlIiwiYWJzRGVwUGF0aCIsInJlbERlcFBhdGgiLCJyZXBvcnQiLCJub2RlIiwibWVzc2FnZSIsIm9wdGlvbnMiXSwibWFwcGluZ3MiOiJhQUFBLGtFO0FBQ0EscUM7QUFDQTtBQUNBLHNEOztBQUVBLGdEOztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxZQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSywwQkFBUSw0QkFBUixDQURELEVBRkY7O0FBS0pDLFlBQVEsQ0FBQyx1Q0FBRCxDQUxKLEVBRFM7OztBQVNmQyx1QkFBUSxTQUFTQyxrQkFBVCxDQUE0QkMsT0FBNUIsRUFBcUM7QUFDM0MsVUFBTUMsU0FBU0QsUUFBUUUsbUJBQVIsR0FBOEJGLFFBQVFFLG1CQUFSLEVBQTlCLEdBQThERixRQUFRRyxXQUFSLEVBQTdFO0FBQ0EsVUFBSUYsV0FBVyxRQUFmLEVBQXlCLE9BQU8sRUFBUCxDQUZrQixDQUVQOztBQUVwQyxlQUFTRyxnQkFBVCxDQUEwQkMsVUFBMUIsRUFBc0M7QUFDcEMsWUFBTUMsVUFBVUQsV0FBV0UsS0FBM0I7O0FBRUEsWUFBSSw2QkFBV0QsT0FBWCxFQUFvQk4sT0FBcEIsTUFBaUMsVUFBckMsRUFBaUQsQ0FBRTtBQUNqRDtBQUNEOztBQUVELFlBQU1RLGFBQWEsMEJBQVFGLE9BQVIsRUFBaUJOLE9BQWpCLENBQW5COztBQUVBLFlBQUksQ0FBQ1EsVUFBTCxFQUFpQixDQUFFO0FBQ2pCO0FBQ0Q7O0FBRUQsWUFBTUMsYUFBYSxvQkFBUyxtQkFBUVIsTUFBUixDQUFULEVBQTBCTyxVQUExQixDQUFuQjs7QUFFQSxZQUFJLDZCQUFXQyxVQUFYLEVBQXVCVCxPQUF2QixNQUFvQyxRQUF4QyxFQUFrRDtBQUNoREEsa0JBQVFVLE1BQVIsQ0FBZTtBQUNiQyxrQkFBTU4sVUFETztBQUViTyxxQkFBUzs7QUFFMkIsZ0NBQVNYLE1BQVQsQ0FGM0I7QUFHV0ssbUJBSFgsc0NBRzZDQSxPQUg3QyxtQkFGSSxFQUFmOztBQU9EO0FBQ0Y7O0FBRUQsYUFBTyxnQ0FBY0YsZ0JBQWQsRUFBZ0NKLFFBQVFhLE9BQVIsQ0FBZ0IsQ0FBaEIsQ0FBaEMsQ0FBUDtBQUNELEtBL0JELE9BQWlCZCxrQkFBakIsSUFUZSxFQUFqQiIsImZpbGUiOiJuby1yZWxhdGl2ZS1wYXJlbnQtaW1wb3J0cy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuaW1wb3J0IHsgYmFzZW5hbWUsIGRpcm5hbWUsIHJlbGF0aXZlIH0gZnJvbSAncGF0aCc7XG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuXG5pbXBvcnQgaW1wb3J0VHlwZSBmcm9tICcuLi9jb3JlL2ltcG9ydFR5cGUnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXJlbGF0aXZlLXBhcmVudC1pbXBvcnRzJyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFttYWtlT3B0aW9uc1NjaGVtYSgpXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIG5vUmVsYXRpdmVQYWNrYWdlcyhjb250ZXh0KSB7XG4gICAgY29uc3QgbXlQYXRoID0gY29udGV4dC5nZXRQaHlzaWNhbEZpbGVuYW1lID8gY29udGV4dC5nZXRQaHlzaWNhbEZpbGVuYW1lKCkgOiBjb250ZXh0LmdldEZpbGVuYW1lKCk7XG4gICAgaWYgKG15UGF0aCA9PT0gJzx0ZXh0PicpIHJldHVybiB7fTsgLy8gY2FuJ3QgY2hlY2sgYSBub24tZmlsZVxuXG4gICAgZnVuY3Rpb24gY2hlY2tTb3VyY2VWYWx1ZShzb3VyY2VOb2RlKSB7XG4gICAgICBjb25zdCBkZXBQYXRoID0gc291cmNlTm9kZS52YWx1ZTtcblxuICAgICAgaWYgKGltcG9ydFR5cGUoZGVwUGF0aCwgY29udGV4dCkgPT09ICdleHRlcm5hbCcpIHsgLy8gaWdub3JlIHBhY2thZ2VzXG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY29uc3QgYWJzRGVwUGF0aCA9IHJlc29sdmUoZGVwUGF0aCwgY29udGV4dCk7XG5cbiAgICAgIGlmICghYWJzRGVwUGF0aCkgeyAvLyB1bmFibGUgdG8gcmVzb2x2ZSBwYXRoXG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY29uc3QgcmVsRGVwUGF0aCA9IHJlbGF0aXZlKGRpcm5hbWUobXlQYXRoKSwgYWJzRGVwUGF0aCk7XG5cbiAgICAgIGlmIChpbXBvcnRUeXBlKHJlbERlcFBhdGgsIGNvbnRleHQpID09PSAncGFyZW50Jykge1xuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZTogc291cmNlTm9kZSxcbiAgICAgICAgICBtZXNzYWdlOiAnUmVsYXRpdmUgaW1wb3J0cyBmcm9tIHBhcmVudCBkaXJlY3RvcmllcyBhcmUgbm90IGFsbG93ZWQuICcgK1xuICAgICAgICAgICAgYFBsZWFzZSBlaXRoZXIgcGFzcyB3aGF0IHlvdSdyZSBpbXBvcnRpbmcgdGhyb3VnaCBhdCBydW50aW1lIGAgK1xuICAgICAgICAgICAgYChkZXBlbmRlbmN5IGluamVjdGlvbiksIG1vdmUgXFxgJHtiYXNlbmFtZShteVBhdGgpfVxcYCB0byBzYW1lIGAgK1xuICAgICAgICAgICAgYGRpcmVjdG9yeSBhcyBcXGAke2RlcFBhdGh9XFxgIG9yIGNvbnNpZGVyIG1ha2luZyBcXGAke2RlcFBhdGh9XFxgIGEgcGFja2FnZS5gLFxuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBjb250ZXh0Lm9wdGlvbnNbMF0pO1xuICB9LFxufTtcbiJdfQ==