'use strict';var _slicedToArray = function () {function sliceIterator(arr, i) {var _arr = [];var _n = true;var _d = false;var _e = undefined;try {for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {_arr.push(_s.value);if (i && _arr.length === i) break;}} catch (err) {_d = true;_e = err;} finally {try {if (!_n && _i["return"]) _i["return"]();} finally {if (_d) throw _e;}}return _arr;}return function (arr, i) {if (Array.isArray(arr)) {return arr;} else if (Symbol.iterator in Object(arr)) {return sliceIterator(arr, i);} else {throw new TypeError("Invalid attempt to destructure non-iterable instance");}};}(); /**
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       * @fileOverview Ensures that no imported module imports the linted module.
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       * @author Ben Mosher
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       */

var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _importType = require('../core/importType');
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}function _toConsumableArray(arr) {if (Array.isArray(arr)) {for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) {arr2[i] = arr[i];}return arr2;} else {return Array.from(arr);}}

// todo: cache cycles / deep relationships for faster repeat evaluation
module.exports = {
  meta: {
    type: 'suggestion',
    docs: { url: (0, _docsUrl2['default'])('no-cycle') },
    schema: [(0, _moduleVisitor.makeOptionsSchema)({
      maxDepth: {
        oneOf: [
        {
          description: 'maximum dependency depth to traverse',
          type: 'integer',
          minimum: 1 },

        {
          'enum': ['∞'],
          type: 'string' }] },



      ignoreExternal: {
        description: 'ignore external modules',
        type: 'boolean',
        'default': false } })] },




  create: function () {function create(context) {
      var myPath = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();
      if (myPath === '<text>') return {}; // can't cycle-check a non-file

      var options = context.options[0] || {};
      var maxDepth = typeof options.maxDepth === 'number' ? options.maxDepth : Infinity;
      var ignoreModule = function () {function ignoreModule(name) {return options.ignoreExternal && (0, _importType.isExternalModule)(
          name,
          (0, _resolve2['default'])(name, context),
          context);}return ignoreModule;}();


      function checkSourceValue(sourceNode, importer) {
        if (ignoreModule(sourceNode.value)) {
          return; // ignore external modules
        }

        if (
        importer.type === 'ImportDeclaration' && (
        // import type { Foo } (TS and Flow)
        importer.importKind === 'type' ||
        // import { type Foo } (Flow)
        importer.specifiers.every(function (_ref) {var importKind = _ref.importKind;return importKind === 'type';})))

        {
          return; // ignore type imports
        }

        var imported = _ExportMap2['default'].get(sourceNode.value, context);

        if (imported == null) {
          return; // no-unresolved territory
        }

        if (imported.path === myPath) {
          return; // no-self-import territory
        }

        var untraversed = [{ mget: function () {function mget() {return imported;}return mget;}(), route: [] }];
        var traversed = new Set();
        function detectCycle(_ref2) {var mget = _ref2.mget,route = _ref2.route;
          var m = mget();
          if (m == null) return;
          if (traversed.has(m.path)) return;
          traversed.add(m.path);var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {

            for (var _iterator = m.imports[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var _ref3 = _step.value;var _ref4 = _slicedToArray(_ref3, 2);var path = _ref4[0];var _ref4$ = _ref4[1];var getter = _ref4$.getter;var declarations = _ref4$.declarations;
              if (traversed.has(path)) continue;
              var toTraverse = [].concat(_toConsumableArray(declarations)).filter(function (_ref5) {var source = _ref5.source,isOnlyImportingTypes = _ref5.isOnlyImportingTypes;return (
                  !ignoreModule(source.value) &&
                  // Ignore only type imports
                  !isOnlyImportingTypes);});

              /*
                                             Only report as a cycle if there are any import declarations that are considered by
                                             the rule. For example:
                                              a.ts:
                                             import { foo } from './b' // should not be reported as a cycle
                                              b.ts:
                                             import type { Bar } from './a'
                                             */


              if (path === myPath && toTraverse.length > 0) return true;
              if (route.length + 1 < maxDepth) {var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {
                  for (var _iterator2 = toTraverse[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var _ref6 = _step2.value;var source = _ref6.source;
                    untraversed.push({ mget: getter, route: route.concat(source) });
                  }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
              }
            }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
        }

        while (untraversed.length > 0) {
          var next = untraversed.shift(); // bfs!
          if (detectCycle(next)) {
            var message = next.route.length > 0 ? 'Dependency cycle via ' + String(
            routeString(next.route)) :
            'Dependency cycle detected.';
            context.report(importer, message);
            return;
          }
        }
      }

      return (0, _moduleVisitor2['default'])(checkSourceValue, context.options[0]);
    }return create;}() };


function routeString(route) {
  return route.map(function (s) {return String(s.value) + ':' + String(s.loc.start.line);}).join('=>');
}
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1jeWNsZS5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwibWF4RGVwdGgiLCJvbmVPZiIsImRlc2NyaXB0aW9uIiwibWluaW11bSIsImlnbm9yZUV4dGVybmFsIiwiY3JlYXRlIiwiY29udGV4dCIsIm15UGF0aCIsImdldFBoeXNpY2FsRmlsZW5hbWUiLCJnZXRGaWxlbmFtZSIsIm9wdGlvbnMiLCJJbmZpbml0eSIsImlnbm9yZU1vZHVsZSIsIm5hbWUiLCJjaGVja1NvdXJjZVZhbHVlIiwic291cmNlTm9kZSIsImltcG9ydGVyIiwidmFsdWUiLCJpbXBvcnRLaW5kIiwic3BlY2lmaWVycyIsImV2ZXJ5IiwiaW1wb3J0ZWQiLCJFeHBvcnRzIiwiZ2V0IiwicGF0aCIsInVudHJhdmVyc2VkIiwibWdldCIsInJvdXRlIiwidHJhdmVyc2VkIiwiU2V0IiwiZGV0ZWN0Q3ljbGUiLCJtIiwiaGFzIiwiYWRkIiwiaW1wb3J0cyIsImdldHRlciIsImRlY2xhcmF0aW9ucyIsInRvVHJhdmVyc2UiLCJmaWx0ZXIiLCJzb3VyY2UiLCJpc09ubHlJbXBvcnRpbmdUeXBlcyIsImxlbmd0aCIsInB1c2giLCJjb25jYXQiLCJuZXh0Iiwic2hpZnQiLCJtZXNzYWdlIiwicm91dGVTdHJpbmciLCJyZXBvcnQiLCJtYXAiLCJzIiwibG9jIiwic3RhcnQiLCJsaW5lIiwiam9pbiJdLCJtYXBwaW5ncyI6InNvQkFBQTs7Ozs7QUFLQSxzRDtBQUNBLHlDO0FBQ0E7QUFDQSxrRTtBQUNBLHFDOztBQUVBO0FBQ0FBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTSxFQUFFQyxLQUFLLDBCQUFRLFVBQVIsQ0FBUCxFQUZGO0FBR0pDLFlBQVEsQ0FBQyxzQ0FBa0I7QUFDekJDLGdCQUFVO0FBQ1JDLGVBQU87QUFDTDtBQUNFQyx1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KlxuICogQGZpbGVPdmVydmlldyBFbnN1cmVzIHRoYXQgbm8gaW1wb3J0ZWQgbW9kdWxlIGltcG9ydHMgdGhlIGxpbnRlZCBtb2R1bGUuXG4gKiBAYXV0aG9yIEJlbiBNb3NoZXJcbiAqL1xuXG5pbXBvcnQgcmVzb2x2ZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL3Jlc29sdmUnO1xuaW1wb3J0IEV4cG9ydHMgZnJvbSAnLi4vRXhwb3J0TWFwJztcbmltcG9ydCB7IGlzRXh0ZXJuYWxNb2R1bGUgfSBmcm9tICcuLi9jb3JlL2ltcG9ydFR5cGUnO1xuaW1wb3J0IG1vZHVsZVZpc2l0b3IsIHsgbWFrZU9wdGlvbnNTY2hlbWEgfSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL21vZHVsZVZpc2l0b3InO1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5cbi8vIHRvZG86IGNhY2hlIGN5Y2xlcyAvIGRlZXAgcmVsYXRpb25zaGlwcyBmb3IgZmFzdGVyIHJlcGVhdCBldmFsdWF0aW9uXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7IHVybDogZG9jc1VybCgnbm8tY3ljbGUnKSB9LFxuICAgIHNjaGVtYTogW21ha2VPcHRpb25zU2NoZW1hKHtcbiAgICAgIG1heERlcHRoOiB7XG4gICAgICAgIG9uZU9mOiBbXG4gICAgICAgICAge1xuICAgICAgICAgICAgZGVzY3JpcHRpb246ICdtYXhpbXVtIGRlcGVuZGVuY3kgZGVwdGggdG8gdHJhdmVyc2UnLFxuICAgICAgICAgICAgdHlwZTogJ2ludGVnZXInLFxuICAgICAgICAgICAgbWluaW11bTogMSxcbiAgICAgICAgICB9LFxuICAgICAgICAgIHtcbiAgICAgICAgICAgIGVudW06IFsn4oieJ10sXG4gICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICB9LFxuICAgICAgICBdLFxuICAgICAgfSxcbiAgICAgIGlnbm9yZUV4dGVybmFsOiB7XG4gICAgICAgIGRlc2NyaXB0aW9uOiAnaWdub3JlIGV4dGVybmFsIG1vZHVsZXMnLFxuICAgICAgICB0eXBlOiAnYm9vbGVhbicsXG4gICAgICAgIGRlZmF1bHQ6IGZhbHNlLFxuICAgICAgfSxcbiAgICB9KV0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBteVBhdGggPSBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUgPyBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUoKSA6IGNvbnRleHQuZ2V0RmlsZW5hbWUoKTtcbiAgICBpZiAobXlQYXRoID09PSAnPHRleHQ+JykgcmV0dXJuIHt9OyAvLyBjYW4ndCBjeWNsZS1jaGVjayBhIG5vbi1maWxlXG5cbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9O1xuICAgIGNvbnN0IG1heERlcHRoID0gdHlwZW9mIG9wdGlvbnMubWF4RGVwdGggPT09ICdudW1iZXInID8gb3B0aW9ucy5tYXhEZXB0aCA6IEluZmluaXR5O1xuICAgIGNvbnN0IGlnbm9yZU1vZHVsZSA9IChuYW1lKSA9PiBvcHRpb25zLmlnbm9yZUV4dGVybmFsICYmIGlzRXh0ZXJuYWxNb2R1bGUoXG4gICAgICBuYW1lLFxuICAgICAgcmVzb2x2ZShuYW1lLCBjb250ZXh0KSxcbiAgICAgIGNvbnRleHQsXG4gICAgKTtcblxuICAgIGZ1bmN0aW9uIGNoZWNrU291cmNlVmFsdWUoc291cmNlTm9kZSwgaW1wb3J0ZXIpIHtcbiAgICAgIGlmIChpZ25vcmVNb2R1bGUoc291cmNlTm9kZS52YWx1ZSkpIHtcbiAgICAgICAgcmV0dXJuOyAvLyBpZ25vcmUgZXh0ZXJuYWwgbW9kdWxlc1xuICAgICAgfVxuXG4gICAgICBpZiAoXG4gICAgICAgIGltcG9ydGVyLnR5cGUgPT09ICdJbXBvcnREZWNsYXJhdGlvbicgJiYgKFxuICAgICAgICAgIC8vIGltcG9ydCB0eXBlIHsgRm9vIH0gKFRTIGFuZCBGbG93KVxuICAgICAgICAgIGltcG9ydGVyLmltcG9ydEtpbmQgPT09ICd0eXBlJyB8fFxuICAgICAgICAgIC8vIGltcG9ydCB7IHR5cGUgRm9vIH0gKEZsb3cpXG4gICAgICAgICAgaW1wb3J0ZXIuc3BlY2lmaWVycy5ldmVyeSgoeyBpbXBvcnRLaW5kIH0pID0+IGltcG9ydEtpbmQgPT09ICd0eXBlJylcbiAgICAgICAgKVxuICAgICAgKSB7XG4gICAgICAgIHJldHVybjsgLy8gaWdub3JlIHR5cGUgaW1wb3J0c1xuICAgICAgfVxuXG4gICAgICBjb25zdCBpbXBvcnRlZCA9IEV4cG9ydHMuZ2V0KHNvdXJjZU5vZGUudmFsdWUsIGNvbnRleHQpO1xuXG4gICAgICBpZiAoaW1wb3J0ZWQgPT0gbnVsbCkge1xuICAgICAgICByZXR1cm47ICAvLyBuby11bnJlc29sdmVkIHRlcnJpdG9yeVxuICAgICAgfVxuXG4gICAgICBpZiAoaW1wb3J0ZWQucGF0aCA9PT0gbXlQYXRoKSB7XG4gICAgICAgIHJldHVybjsgIC8vIG5vLXNlbGYtaW1wb3J0IHRlcnJpdG9yeVxuICAgICAgfVxuXG4gICAgICBjb25zdCB1bnRyYXZlcnNlZCA9IFt7IG1nZXQ6ICgpID0+IGltcG9ydGVkLCByb3V0ZTpbXSB9XTtcbiAgICAgIGNvbnN0IHRyYXZlcnNlZCA9IG5ldyBTZXQoKTtcbiAgICAgIGZ1bmN0aW9uIGRldGVjdEN5Y2xlKHsgbWdldCwgcm91dGUgfSkge1xuICAgICAgICBjb25zdCBtID0gbWdldCgpO1xuICAgICAgICBpZiAobSA9PSBudWxsKSByZXR1cm47XG4gICAgICAgIGlmICh0cmF2ZXJzZWQuaGFzKG0ucGF0aCkpIHJldHVybjtcbiAgICAgICAgdHJhdmVyc2VkLmFkZChtLnBhdGgpO1xuXG4gICAgICAgIGZvciAoY29uc3QgW3BhdGgsIHsgZ2V0dGVyLCBkZWNsYXJhdGlvbnMgfV0gb2YgbS5pbXBvcnRzKSB7XG4gICAgICAgICAgaWYgKHRyYXZlcnNlZC5oYXMocGF0aCkpIGNvbnRpbnVlO1xuICAgICAgICAgIGNvbnN0IHRvVHJhdmVyc2UgPSBbLi4uZGVjbGFyYXRpb25zXS5maWx0ZXIoKHsgc291cmNlLCBpc09ubHlJbXBvcnRpbmdUeXBlcyB9KSA9PlxuICAgICAgICAgICAgIWlnbm9yZU1vZHVsZShzb3VyY2UudmFsdWUpICYmXG4gICAgICAgICAgICAvLyBJZ25vcmUgb25seSB0eXBlIGltcG9ydHNcbiAgICAgICAgICAgICFpc09ubHlJbXBvcnRpbmdUeXBlcyxcbiAgICAgICAgICApO1xuICAgICAgICAgIC8qXG4gICAgICAgICAgT25seSByZXBvcnQgYXMgYSBjeWNsZSBpZiB0aGVyZSBhcmUgYW55IGltcG9ydCBkZWNsYXJhdGlvbnMgdGhhdCBhcmUgY29uc2lkZXJlZCBieVxuICAgICAgICAgIHRoZSBydWxlLiBGb3IgZXhhbXBsZTpcblxuICAgICAgICAgIGEudHM6XG4gICAgICAgICAgaW1wb3J0IHsgZm9vIH0gZnJvbSAnLi9iJyAvLyBzaG91bGQgbm90IGJlIHJlcG9ydGVkIGFzIGEgY3ljbGVcblxuICAgICAgICAgIGIudHM6XG4gICAgICAgICAgaW1wb3J0IHR5cGUgeyBCYXIgfSBmcm9tICcuL2EnXG4gICAgICAgICAgKi9cbiAgICAgICAgICBpZiAocGF0aCA9PT0gbXlQYXRoICYmIHRvVHJhdmVyc2UubGVuZ3RoID4gMCkgcmV0dXJuIHRydWU7XG4gICAgICAgICAgaWYgKHJvdXRlLmxlbmd0aCArIDEgPCBtYXhEZXB0aCkge1xuICAgICAgICAgICAgZm9yIChjb25zdCB7IHNvdXJjZSB9IG9mIHRvVHJhdmVyc2UpIHtcbiAgICAgICAgICAgICAgdW50cmF2ZXJzZWQucHVzaCh7IG1nZXQ6IGdldHRlciwgcm91dGU6IHJvdXRlLmNvbmNhdChzb3VyY2UpIH0pO1xuICAgICAgICAgICAgfVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuICAgICAgfVxuXG4gICAgICB3aGlsZSAodW50cmF2ZXJzZWQubGVuZ3RoID4gMCkge1xuICAgICAgICBjb25zdCBuZXh0ID0gdW50cmF2ZXJzZWQuc2hpZnQoKTsgLy8gYmZzIVxuICAgICAgICBpZiAoZGV0ZWN0Q3ljbGUobmV4dCkpIHtcbiAgICAgICAgICBjb25zdCBtZXNzYWdlID0gKG5leHQucm91dGUubGVuZ3RoID4gMFxuICAgICAgICAgICAgPyBgRGVwZW5kZW5jeSBjeWNsZSB2aWEgJHtyb3V0ZVN0cmluZyhuZXh0LnJvdXRlKX1gXG4gICAgICAgICAgICA6ICdEZXBlbmRlbmN5IGN5Y2xlIGRldGVjdGVkLicpO1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KGltcG9ydGVyLCBtZXNzYWdlKTtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja1NvdXJjZVZhbHVlLCBjb250ZXh0Lm9wdGlvbnNbMF0pO1xuICB9LFxufTtcblxuZnVuY3Rpb24gcm91dGVTdHJpbmcocm91dGUpIHtcbiAgcmV0dXJuIHJvdXRlLm1hcChzID0+IGAke3MudmFsdWV9OiR7cy5sb2Muc3RhcnQubGluZX1gKS5qb2luKCc9PicpO1xufVxuIl19