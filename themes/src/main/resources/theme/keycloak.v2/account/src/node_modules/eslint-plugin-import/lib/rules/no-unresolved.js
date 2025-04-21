'use strict';




var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _ModuleCache = require('eslint-module-utils/ModuleCache');var _ModuleCache2 = _interopRequireDefault(_ModuleCache);
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };} /**
                                                                                                                                                                                       * @fileOverview Ensures that an imported path exists, given resolution rules.
                                                                                                                                                                                       * @author Ben Mosher
                                                                                                                                                                                       */module.exports = { meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('no-unresolved') },


    schema: [
    (0, _moduleVisitor.makeOptionsSchema)({
      caseSensitive: { type: 'boolean', 'default': true },
      caseSensitiveStrict: { type: 'boolean', 'default': false } })] },




  create: function () {function create(context) {
      var options = context.options[0] || {};

      function checkSourceValue(source, node) {
        // ignore type-only imports
        if (node.importKind === 'type') {
          return;
        }

        var caseSensitive = !_resolve.CASE_SENSITIVE_FS && options.caseSensitive !== false;
        var caseSensitiveStrict = !_resolve.CASE_SENSITIVE_FS && options.caseSensitiveStrict;

        var resolvedPath = (0, _resolve2['default'])(source.value, context);

        if (resolvedPath === undefined) {
          context.report(
          source, 'Unable to resolve path to module \'' + String(
          source.value) + '\'.');

        } else if (caseSensitive || caseSensitiveStrict) {
          var cacheSettings = _ModuleCache2['default'].getSettings(context.settings);
          if (!(0, _resolve.fileExistsWithCaseSync)(resolvedPath, cacheSettings, caseSensitiveStrict)) {
            context.report(
            source, 'Casing of ' + String(
            source.value) + ' does not match the underlying filesystem.');

          }
        }
      }

      return (0, _moduleVisitor2['default'])(checkSourceValue, options);
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11bnJlc29sdmVkLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjYXNlU2Vuc2l0aXZlIiwiY2FzZVNlbnNpdGl2ZVN0cmljdCIsImNyZWF0ZSIsImNvbnRleHQiLCJvcHRpb25zIiwiY2hlY2tTb3VyY2VWYWx1ZSIsInNvdXJjZSIsIm5vZGUiLCJpbXBvcnRLaW5kIiwiQ0FTRV9TRU5TSVRJVkVfRlMiLCJyZXNvbHZlZFBhdGgiLCJ2YWx1ZSIsInVuZGVmaW5lZCIsInJlcG9ydCIsImNhY2hlU2V0dGluZ3MiLCJNb2R1bGVDYWNoZSIsImdldFNldHRpbmdzIiwic2V0dGluZ3MiXSwibWFwcGluZ3MiOiI7Ozs7O0FBS0Esc0Q7QUFDQSw4RDtBQUNBLGtFO0FBQ0EscUMsaUpBUkE7Ozt5TEFVQUEsT0FBT0MsT0FBUCxHQUFpQixFQUNmQyxNQUFNO0FBQ0pDLFVBQU0sU0FERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsZUFBUixDQURELEVBRkY7OztBQU1KQyxZQUFRO0FBQ04sMENBQWtCO0FBQ2hCQyxxQkFBZSxFQUFFSixNQUFNLFNBQVIsRUFBbUIsV0FBUyxJQUE1QixFQURDO0FBRWhCSywyQkFBcUIsRUFBRUwsTUFBTSxTQUFSLEVBQW1CLFdBQVMsS0FBNUIsRUFGTCxFQUFsQixDQURNLENBTkosRUFEUzs7Ozs7QUFlZk0sUUFmZSwrQkFlUkMsT0FmUSxFQWVDO0FBQ2QsVUFBTUMsVUFBVUQsUUFBUUMsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUF0Qzs7QUFFQSxlQUFTQyxnQkFBVCxDQUEwQkMsTUFBMUIsRUFBa0NDLElBQWxDLEVBQXdDO0FBQ3RDO0FBQ0EsWUFBSUEsS0FBS0MsVUFBTCxLQUFvQixNQUF4QixFQUFnQztBQUM5QjtBQUNEOztBQUVELFlBQU1SLGdCQUFnQixDQUFDUywwQkFBRCxJQUFzQkwsUUFBUUosYUFBUixLQUEwQixLQUF0RTtBQUNBLFlBQU1DLHNCQUFzQixDQUFDUSwwQkFBRCxJQUFzQkwsUUFBUUgsbUJBQTFEOztBQUVBLFlBQU1TLGVBQWUsMEJBQVFKLE9BQU9LLEtBQWYsRUFBc0JSLE9BQXRCLENBQXJCOztBQUVBLFlBQUlPLGlCQUFpQkUsU0FBckIsRUFBZ0M7QUFDOUJULGtCQUFRVSxNQUFSO0FBQ0VQLGdCQURGO0FBRXVDQSxpQkFBT0ssS0FGOUM7O0FBSUQsU0FMRCxNQUtPLElBQUlYLGlCQUFpQkMsbUJBQXJCLEVBQTBDO0FBQy9DLGNBQU1hLGdCQUFnQkMseUJBQVlDLFdBQVosQ0FBd0JiLFFBQVFjLFFBQWhDLENBQXRCO0FBQ0EsY0FBSSxDQUFDLHFDQUF1QlAsWUFBdkIsRUFBcUNJLGFBQXJDLEVBQW9EYixtQkFBcEQsQ0FBTCxFQUErRTtBQUM3RUUsb0JBQVFVLE1BQVI7QUFDRVAsa0JBREY7QUFFZUEsbUJBQU9LLEtBRnRCOztBQUlEO0FBQ0Y7QUFDRjs7QUFFRCxhQUFPLGdDQUFjTixnQkFBZCxFQUFnQ0QsT0FBaEMsQ0FBUDtBQUNELEtBOUNjLG1CQUFqQiIsImZpbGUiOiJuby11bnJlc29sdmVkLmpzIiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAZmlsZU92ZXJ2aWV3IEVuc3VyZXMgdGhhdCBhbiBpbXBvcnRlZCBwYXRoIGV4aXN0cywgZ2l2ZW4gcmVzb2x1dGlvbiBydWxlcy5cbiAqIEBhdXRob3IgQmVuIE1vc2hlclxuICovXG5cbmltcG9ydCByZXNvbHZlLCB7IENBU0VfU0VOU0lUSVZFX0ZTLCBmaWxlRXhpc3RzV2l0aENhc2VTeW5jIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJztcbmltcG9ydCBNb2R1bGVDYWNoZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL01vZHVsZUNhY2hlJztcbmltcG9ydCBtb2R1bGVWaXNpdG9yLCB7IG1ha2VPcHRpb25zU2NoZW1hIH0gZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXVucmVzb2x2ZWQnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXG4gICAgICBtYWtlT3B0aW9uc1NjaGVtYSh7XG4gICAgICAgIGNhc2VTZW5zaXRpdmU6IHsgdHlwZTogJ2Jvb2xlYW4nLCBkZWZhdWx0OiB0cnVlIH0sXG4gICAgICAgIGNhc2VTZW5zaXRpdmVTdHJpY3Q6IHsgdHlwZTogJ2Jvb2xlYW4nLCBkZWZhdWx0OiBmYWxzZSB9LFxuICAgICAgfSksXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge307XG5cbiAgICBmdW5jdGlvbiBjaGVja1NvdXJjZVZhbHVlKHNvdXJjZSwgbm9kZSkge1xuICAgICAgLy8gaWdub3JlIHR5cGUtb25seSBpbXBvcnRzXG4gICAgICBpZiAobm9kZS5pbXBvcnRLaW5kID09PSAndHlwZScpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBjb25zdCBjYXNlU2Vuc2l0aXZlID0gIUNBU0VfU0VOU0lUSVZFX0ZTICYmIG9wdGlvbnMuY2FzZVNlbnNpdGl2ZSAhPT0gZmFsc2U7XG4gICAgICBjb25zdCBjYXNlU2Vuc2l0aXZlU3RyaWN0ID0gIUNBU0VfU0VOU0lUSVZFX0ZTICYmIG9wdGlvbnMuY2FzZVNlbnNpdGl2ZVN0cmljdDtcblxuICAgICAgY29uc3QgcmVzb2x2ZWRQYXRoID0gcmVzb2x2ZShzb3VyY2UudmFsdWUsIGNvbnRleHQpO1xuXG4gICAgICBpZiAocmVzb2x2ZWRQYXRoID09PSB1bmRlZmluZWQpIHtcbiAgICAgICAgY29udGV4dC5yZXBvcnQoXG4gICAgICAgICAgc291cmNlLFxuICAgICAgICAgIGBVbmFibGUgdG8gcmVzb2x2ZSBwYXRoIHRvIG1vZHVsZSAnJHtzb3VyY2UudmFsdWV9Jy5gLFxuICAgICAgICApO1xuICAgICAgfSBlbHNlIGlmIChjYXNlU2Vuc2l0aXZlIHx8IGNhc2VTZW5zaXRpdmVTdHJpY3QpIHtcbiAgICAgICAgY29uc3QgY2FjaGVTZXR0aW5ncyA9IE1vZHVsZUNhY2hlLmdldFNldHRpbmdzKGNvbnRleHQuc2V0dGluZ3MpO1xuICAgICAgICBpZiAoIWZpbGVFeGlzdHNXaXRoQ2FzZVN5bmMocmVzb2x2ZWRQYXRoLCBjYWNoZVNldHRpbmdzLCBjYXNlU2Vuc2l0aXZlU3RyaWN0KSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgICAgc291cmNlLFxuICAgICAgICAgICAgYENhc2luZyBvZiAke3NvdXJjZS52YWx1ZX0gZG9lcyBub3QgbWF0Y2ggdGhlIHVuZGVybHlpbmcgZmlsZXN5c3RlbS5gLFxuICAgICAgICAgICk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBvcHRpb25zKTtcbiAgfSxcbn07XG4iXX0=