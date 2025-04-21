'use strict';var _slicedToArray = function () {function sliceIterator(arr, i) {var _arr = [];var _n = true;var _d = false;var _e = undefined;try {for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {_arr.push(_s.value);if (i && _arr.length === i) break;}} catch (err) {_d = true;_e = err;} finally {try {if (!_n && _i["return"]) _i["return"]();} finally {if (_d) throw _e;}}return _arr;}return function (arr, i) {if (Array.isArray(arr)) {return arr;} else if (Symbol.iterator in Object(arr)) {return sliceIterator(arr, i);} else {throw new TypeError("Invalid attempt to destructure non-iterable instance");}};}();var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);
var _arrayIncludes = require('array-includes');var _arrayIncludes2 = _interopRequireDefault(_arrayIncludes);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

/*
                                                                                                                                                                                                            Notes on TypeScript namespaces aka TSModuleDeclaration:
                                                                                                                                                                                                            
                                                                                                                                                                                                            There are two forms:
                                                                                                                                                                                                            - active namespaces: namespace Foo {} / module Foo {}
                                                                                                                                                                                                            - ambient modules; declare module "eslint-plugin-import" {}
                                                                                                                                                                                                            
                                                                                                                                                                                                            active namespaces:
                                                                                                                                                                                                            - cannot contain a default export
                                                                                                                                                                                                            - cannot contain an export all
                                                                                                                                                                                                            - cannot contain a multi name export (export { a, b })
                                                                                                                                                                                                            - can have active namespaces nested within them
                                                                                                                                                                                                            
                                                                                                                                                                                                            ambient namespaces:
                                                                                                                                                                                                            - can only be defined in .d.ts files
                                                                                                                                                                                                            - cannot be nested within active namespaces
                                                                                                                                                                                                            - have no other restrictions
                                                                                                                                                                                                            */

var rootProgram = 'root';
var tsTypePrefix = 'type:';

/**
                             * Detect function overloads like:
                             * ```ts
                             * export function foo(a: number);
                             * export function foo(a: string);
                             * export function foo(a: number|string) { return a; }
                             * ```
                             * @param {Set<Object>} nodes
                             * @returns {boolean}
                             */
function isTypescriptFunctionOverloads(nodes) {
  var types = new Set(Array.from(nodes, function (node) {return node.parent.type;}));
  return types.has('TSDeclareFunction') && (

  types.size === 1 ||
  types.size === 2 && types.has('FunctionDeclaration'));

}

/**
   * Detect merging Namespaces with Classes, Functions, or Enums like:
   * ```ts
   * export class Foo { }
   * export namespace Foo { }
   * ```
   * @param {Set<Object>} nodes
   * @returns {boolean}
   */
function isTypescriptNamespaceMerging(nodes) {
  var types = new Set(Array.from(nodes, function (node) {return node.parent.type;}));
  var noNamespaceNodes = Array.from(nodes).filter(function (node) {return node.parent.type !== 'TSModuleDeclaration';});

  return types.has('TSModuleDeclaration') && (

  types.size === 1
  // Merging with functions
  || types.size === 2 && (types.has('FunctionDeclaration') || types.has('TSDeclareFunction')) ||
  types.size === 3 && types.has('FunctionDeclaration') && types.has('TSDeclareFunction')
  // Merging with classes or enums
  || types.size === 2 && (types.has('ClassDeclaration') || types.has('TSEnumDeclaration')) && noNamespaceNodes.length === 1);

}

/**
   * Detect if a typescript namespace node should be reported as multiple export:
   * ```ts
   * export class Foo { }
   * export function Foo();
   * export namespace Foo { }
   * ```
   * @param {Object} node
   * @param {Set<Object>} nodes
   * @returns {boolean}
   */
function shouldSkipTypescriptNamespace(node, nodes) {
  var types = new Set(Array.from(nodes, function (node) {return node.parent.type;}));

  return !isTypescriptNamespaceMerging(nodes) &&
  node.parent.type === 'TSModuleDeclaration' && (

  types.has('TSEnumDeclaration') ||
  types.has('ClassDeclaration') ||
  types.has('FunctionDeclaration') ||
  types.has('TSDeclareFunction'));

}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('export') },

    schema: [] },


  create: function () {function create(context) {
      var namespace = new Map([[rootProgram, new Map()]]);

      function addNamed(name, node, parent, isType) {
        if (!namespace.has(parent)) {
          namespace.set(parent, new Map());
        }
        var named = namespace.get(parent);

        var key = isType ? '' + tsTypePrefix + String(name) : name;
        var nodes = named.get(key);

        if (nodes == null) {
          nodes = new Set();
          named.set(key, nodes);
        }

        nodes.add(node);
      }

      function getParent(node) {
        if (node.parent && node.parent.type === 'TSModuleBlock') {
          return node.parent.parent;
        }

        // just in case somehow a non-ts namespace export declaration isn't directly
        // parented to the root Program node
        return rootProgram;
      }

      return {
        ExportDefaultDeclaration: function () {function ExportDefaultDeclaration(node) {
            addNamed('default', node, getParent(node));
          }return ExportDefaultDeclaration;}(),

        ExportSpecifier: function () {function ExportSpecifier(node) {
            addNamed(
            node.exported.name || node.exported.value,
            node.exported,
            getParent(node.parent));

          }return ExportSpecifier;}(),

        ExportNamedDeclaration: function () {function ExportNamedDeclaration(node) {
            if (node.declaration == null) return;

            var parent = getParent(node);
            // support for old TypeScript versions
            var isTypeVariableDecl = node.declaration.kind === 'type';

            if (node.declaration.id != null) {
              if ((0, _arrayIncludes2['default'])([
              'TSTypeAliasDeclaration',
              'TSInterfaceDeclaration'],
              node.declaration.type)) {
                addNamed(node.declaration.id.name, node.declaration.id, parent, true);
              } else {
                addNamed(node.declaration.id.name, node.declaration.id, parent, isTypeVariableDecl);
              }
            }

            if (node.declaration.declarations != null) {var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
                for (var _iterator = node.declaration.declarations[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var declaration = _step.value;
                  (0, _ExportMap.recursivePatternCapture)(declaration.id, function (v) {return (
                      addNamed(v.name, v, parent, isTypeVariableDecl));});
                }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
            }
          }return ExportNamedDeclaration;}(),

        ExportAllDeclaration: function () {function ExportAllDeclaration(node) {
            if (node.source == null) return; // not sure if this is ever true

            // `export * as X from 'path'` does not conflict
            if (node.exported && node.exported.name) return;

            var remoteExports = _ExportMap2['default'].get(node.source.value, context);
            if (remoteExports == null) return;

            if (remoteExports.errors.length) {
              remoteExports.reportErrors(context, node);
              return;
            }

            var parent = getParent(node);

            var any = false;
            remoteExports.forEach(function (v, name) {
              if (name !== 'default') {
                any = true; // poor man's filter
                addNamed(name, node, parent);
              }
            });

            if (!any) {
              context.report(
              node.source, 'No named exports found in module \'' + String(
              node.source.value) + '\'.');

            }
          }return ExportAllDeclaration;}(),

        'Program:exit': function () {function ProgramExit() {var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {
              for (var _iterator2 = namespace[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var _ref = _step2.value;var _ref2 = _slicedToArray(_ref, 2);var named = _ref2[1];var _iteratorNormalCompletion3 = true;var _didIteratorError3 = false;var _iteratorError3 = undefined;try {
                  for (var _iterator3 = named[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {var _ref3 = _step3.value;var _ref4 = _slicedToArray(_ref3, 2);var name = _ref4[0];var nodes = _ref4[1];
                    if (nodes.size <= 1) continue;

                    if (isTypescriptFunctionOverloads(nodes) || isTypescriptNamespaceMerging(nodes)) continue;var _iteratorNormalCompletion4 = true;var _didIteratorError4 = false;var _iteratorError4 = undefined;try {

                      for (var _iterator4 = nodes[Symbol.iterator](), _step4; !(_iteratorNormalCompletion4 = (_step4 = _iterator4.next()).done); _iteratorNormalCompletion4 = true) {var node = _step4.value;
                        if (shouldSkipTypescriptNamespace(node, nodes)) continue;

                        if (name === 'default') {
                          context.report(node, 'Multiple default exports.');
                        } else {
                          context.report(
                          node, 'Multiple exports of name \'' + String(
                          name.replace(tsTypePrefix, '')) + '\'.');

                        }
                      }} catch (err) {_didIteratorError4 = true;_iteratorError4 = err;} finally {try {if (!_iteratorNormalCompletion4 && _iterator4['return']) {_iterator4['return']();}} finally {if (_didIteratorError4) {throw _iteratorError4;}}}
                  }} catch (err) {_didIteratorError3 = true;_iteratorError3 = err;} finally {try {if (!_iteratorNormalCompletion3 && _iterator3['return']) {_iterator3['return']();}} finally {if (_didIteratorError3) {throw _iteratorError3;}}}
              }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
          }return ProgramExit;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9leHBvcnQuanMiXSwibmFtZXMiOlsicm9vdFByb2dyYW0iLCJ0c1R5cGVQcmVmaXgiLCJpc1R5cGVzY3JpcHRGdW5jdGlvbk92ZXJsb2FkcyIsIm5vZGVzIiwidHlwZXMiLCJTZXQiLCJBcnJheSIsImZyb20iLCJub2RlIiwicGFyZW50IiwidHlwZSIsImhhcyIsInNpemUiLCJpc1R5cGVzY3JpcHROYW1lc3BhY2VNZXJnaW5nIiwibm9OYW1lc3BhY2VOb2RlcyIsImZpbHRlciIsImxlbmd0aCIsInNob3VsZFNraXBUeXBlc2NyaXB0TmFtZXNwYWNlIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsIm5hbWVzcGFjZSIsIk1hcCIsImFkZE5hbWVkIiwibmFtZSIsImlzVHlwZSIsInNldCIsIm5hbWVkIiwiZ2V0Iiwia2V5IiwiYWRkIiwiZ2V0UGFyZW50IiwiRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uIiwiRXhwb3J0U3BlY2lmaWVyIiwiZXhwb3J0ZWQiLCJ2YWx1ZSIsIkV4cG9ydE5hbWVkRGVjbGFyYXRpb24iLCJkZWNsYXJhdGlvbiIsImlzVHlwZVZhcmlhYmxlRGVjbCIsImtpbmQiLCJpZCIsImRlY2xhcmF0aW9ucyIsInYiLCJFeHBvcnRBbGxEZWNsYXJhdGlvbiIsInNvdXJjZSIsInJlbW90ZUV4cG9ydHMiLCJFeHBvcnRNYXAiLCJlcnJvcnMiLCJyZXBvcnRFcnJvcnMiLCJhbnkiLCJmb3JFYWNoIiwicmVwb3J0IiwicmVwbGFjZSJdLCJtYXBwaW5ncyI6InFvQkFBQSx5QztBQUNBLHFDO0FBQ0EsK0M7O0FBRUE7Ozs7Ozs7Ozs7Ozs7Ozs7Ozs7QUFtQkEsSUFBTUEsY0FBYyxNQUFwQjtBQUNBLElBQU1DLGVBQWUsT0FBckI7O0FBRUE7Ozs7Ozs7Ozs7QUFVQSxTQUFTQyw2QkFBVCxDQUF1Q0MsS0FBdkMsRUFBOEM7QUFDNUMsTUFBTUMsUUFB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T0FBTCxDQUFheEQsWUFBYixFQUEyQixFQUEzQixDQUYvQjs7QUFJRDtBQUNGLHVCQWhCZ0M7QUFpQmxDLG1CQWxCZ0M7QUFtQmxDLGVBcEJ5QjtBQXFCM0IsV0FyQkQsc0JBdkVLLEVBQVA7O0FBOEZELEtBckljLG1CQUFqQiIsImZpbGUiOiJleHBvcnQuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgRXhwb3J0TWFwLCB7IHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlIH0gZnJvbSAnLi4vRXhwb3J0TWFwJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuaW1wb3J0IGluY2x1ZGVzIGZyb20gJ2FycmF5LWluY2x1ZGVzJztcblxuLypcbk5vdGVzIG9uIFR5cGVTY3JpcHQgbmFtZXNwYWNlcyBha2EgVFNNb2R1bGVEZWNsYXJhdGlvbjpcblxuVGhlcmUgYXJlIHR3byBmb3Jtczpcbi0gYWN0aXZlIG5hbWVzcGFjZXM6IG5hbWVzcGFjZSBGb28ge30gLyBtb2R1bGUgRm9vIHt9XG4tIGFtYmllbnQgbW9kdWxlczsgZGVjbGFyZSBtb2R1bGUgXCJlc2xpbnQtcGx1Z2luLWltcG9ydFwiIHt9XG5cbmFjdGl2ZSBuYW1lc3BhY2VzOlxuLSBjYW5ub3QgY29udGFpbiBhIGRlZmF1bHQgZXhwb3J0XG4tIGNhbm5vdCBjb250YWluIGFuIGV4cG9ydCBhbGxcbi0gY2Fubm90IGNvbnRhaW4gYSBtdWx0aSBuYW1lIGV4cG9ydCAoZXhwb3J0IHsgYSwgYiB9KVxuLSBjYW4gaGF2ZSBhY3RpdmUgbmFtZXNwYWNlcyBuZXN0ZWQgd2l0aGluIHRoZW1cblxuYW1iaWVudCBuYW1lc3BhY2VzOlxuLSBjYW4gb25seSBiZSBkZWZpbmVkIGluIC5kLnRzIGZpbGVzXG4tIGNhbm5vdCBiZSBuZXN0ZWQgd2l0aGluIGFjdGl2ZSBuYW1lc3BhY2VzXG4tIGhhdmUgbm8gb3RoZXIgcmVzdHJpY3Rpb25zXG4qL1xuXG5jb25zdCByb290UHJvZ3JhbSA9ICdyb290JztcbmNvbnN0IHRzVHlwZVByZWZpeCA9ICd0eXBlOic7XG5cbi8qKlxuICogRGV0ZWN0IGZ1bmN0aW9uIG92ZXJsb2FkcyBsaWtlOlxuICogYGBgdHNcbiAqIGV4cG9ydCBmdW5jdGlvbiBmb28oYTogbnVtYmVyKTtcbiAqIGV4cG9ydCBmdW5jdGlvbiBmb28oYTogc3RyaW5nKTtcbiAqIGV4cG9ydCBmdW5jdGlvbiBmb28oYTogbnVtYmVyfHN0cmluZykgeyByZXR1cm4gYTsgfVxuICogYGBgXG4gKiBAcGFyYW0ge1NldDxPYmplY3Q+fSBub2Rlc1xuICogQHJldHVybnMge2Jvb2xlYW59XG4gKi9cbmZ1bmN0aW9uIGlzVHlwZXNjcmlwdEZ1bmN0aW9uT3ZlcmxvYWRzKG5vZGVzKSB7XG4gIGNvbnN0IHR5cGVzID0gbmV3IFNldChBcnJheS5mcm9tKG5vZGVzLCBub2RlID0+IG5vZGUucGFyZW50LnR5cGUpKTtcbiAgcmV0dXJuIHR5cGVzLmhhcygnVFNEZWNsYXJlRnVuY3Rpb24nKVxuICAgICYmIChcbiAgICAgIHR5cGVzLnNpemUgPT09IDFcbiAgICAgIHx8ICh0eXBlcy5zaXplID09PSAyICYmIHR5cGVzLmhhcygnRnVuY3Rpb25EZWNsYXJhdGlvbicpKVxuICAgICk7XG59XG5cbi8qKlxuICogRGV0ZWN0IG1lcmdpbmcgTmFtZXNwYWNlcyB3aXRoIENsYXNzZXMsIEZ1bmN0aW9ucywgb3IgRW51bXMgbGlrZTpcbiAqIGBgYHRzXG4gKiBleHBvcnQgY2xhc3MgRm9vIHsgfVxuICogZXhwb3J0IG5hbWVzcGFjZSBGb28geyB9XG4gKiBgYGBcbiAqIEBwYXJhbSB7U2V0PE9iamVjdD59IG5vZGVzXG4gKiBAcmV0dXJucyB7Ym9vbGVhbn1cbiAqL1xuZnVuY3Rpb24gaXNUeXBlc2NyaXB0TmFtZXNwYWNlTWVyZ2luZyhub2Rlcykge1xuICBjb25zdCB0eXBlcyA9IG5ldyBTZXQoQXJyYXkuZnJvbShub2Rlcywgbm9kZSA9PiBub2RlLnBhcmVudC50eXBlKSk7XG4gIGNvbnN0IG5vTmFtZXNwYWNlTm9kZXMgPSBBcnJheS5mcm9tKG5vZGVzKS5maWx0ZXIoKG5vZGUpID0+IG5vZGUucGFyZW50LnR5cGUgIT09ICdUU01vZHVsZURlY2xhcmF0aW9uJyk7XG5cbiAgcmV0dXJuIHR5cGVzLmhhcygnVFNNb2R1bGVEZWNsYXJhdGlvbicpXG4gICAgJiYgKFxuICAgICAgdHlwZXMuc2l6ZSA9PT0gMVxuICAgICAgLy8gTWVyZ2luZyB3aXRoIGZ1bmN0aW9uc1xuICAgICAgfHwgKHR5cGVzLnNpemUgPT09IDIgJiYgKHR5cGVzLmhhcygnRnVuY3Rpb25EZWNsYXJhdGlvbicpIHx8IHR5cGVzLmhhcygnVFNEZWNsYXJlRnVuY3Rpb24nKSkpXG4gICAgICB8fCAodHlwZXMuc2l6ZSA9PT0gMyAmJiB0eXBlcy5oYXMoJ0Z1bmN0aW9uRGVjbGFyYXRpb24nKSAmJiB0eXBlcy5oYXMoJ1RTRGVjbGFyZUZ1bmN0aW9uJykpXG4gICAgICAvLyBNZXJnaW5nIHdpdGggY2xhc3NlcyBvciBlbnVtc1xuICAgICAgfHwgKHR5cGVzLnNpemUgPT09IDIgJiYgKHR5cGVzLmhhcygnQ2xhc3NEZWNsYXJhdGlvbicpIHx8IHR5cGVzLmhhcygnVFNFbnVtRGVjbGFyYXRpb24nKSkgJiYgbm9OYW1lc3BhY2VOb2Rlcy5sZW5ndGggPT09IDEpXG4gICAgKTtcbn1cblxuLyoqXG4gKiBEZXRlY3QgaWYgYSB0eXBlc2NyaXB0IG5hbWVzcGFjZSBub2RlIHNob3VsZCBiZSByZXBvcnRlZCBhcyBtdWx0aXBsZSBleHBvcnQ6XG4gKiBgYGB0c1xuICogZXhwb3J0IGNsYXNzIEZvbyB7IH1cbiAqIGV4cG9ydCBmdW5jdGlvbiBGb28oKTtcbiAqIGV4cG9ydCBuYW1lc3BhY2UgRm9vIHsgfVxuICogYGBgXG4gKiBAcGFyYW0ge09iamVjdH0gbm9kZVxuICogQHBhcmFtIHtTZXQ8T2JqZWN0Pn0gbm9kZXNcbiAqIEByZXR1cm5zIHtib29sZWFufVxuICovXG5mdW5jdGlvbiBzaG91bGRTa2lwVHlwZXNjcmlwdE5hbWVzcGFjZShub2RlLCBub2Rlcykge1xuICBjb25zdCB0eXBlcyA9IG5ldyBTZXQoQXJyYXkuZnJvbShub2Rlcywgbm9kZSA9PiBub2RlLnBhcmVudC50eXBlKSk7XG5cbiAgcmV0dXJuICFpc1R5cGVzY3JpcHROYW1lc3BhY2VNZXJnaW5nKG5vZGVzKVxuICAgICYmIG5vZGUucGFyZW50LnR5cGUgPT09ICdUU01vZHVsZURlY2xhcmF0aW9uJ1xuICAgICYmIChcbiAgICAgIHR5cGVzLmhhcygnVFNFbnVtRGVjbGFyYXRpb24nKVxuICAgICAgfHwgdHlwZXMuaGFzKCdDbGFzc0RlY2xhcmF0aW9uJylcbiAgICAgIHx8IHR5cGVzLmhhcygnRnVuY3Rpb25EZWNsYXJhdGlvbicpXG4gICAgICB8fCB0eXBlcy5oYXMoJ1RTRGVjbGFyZUZ1bmN0aW9uJylcbiAgICApO1xufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ2V4cG9ydCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIGNvbnN0IG5hbWVzcGFjZSA9IG5ldyBNYXAoW1tyb290UHJvZ3JhbSwgbmV3IE1hcCgpXV0pO1xuXG4gICAgZnVuY3Rpb24gYWRkTmFtZWQobmFtZSwgbm9kZSwgcGFyZW50LCBpc1R5cGUpIHtcbiAgICAgIGlmICghbmFtZXNwYWNlLmhhcyhwYXJlbnQpKSB7XG4gICAgICAgIG5hbWVzcGFjZS5zZXQocGFyZW50LCBuZXcgTWFwKCkpO1xuICAgICAgfVxuICAgICAgY29uc3QgbmFtZWQgPSBuYW1lc3BhY2UuZ2V0KHBhcmVudCk7XG5cbiAgICAgIGNvbnN0IGtleSA9IGlzVHlwZSA/IGAke3RzVHlwZVByZWZpeH0ke25hbWV9YCA6IG5hbWU7XG4gICAgICBsZXQgbm9kZXMgPSBuYW1lZC5nZXQoa2V5KTtcblxuICAgICAgaWYgKG5vZGVzID09IG51bGwpIHtcbiAgICAgICAgbm9kZXMgPSBuZXcgU2V0KCk7XG4gICAgICAgIG5hbWVkLnNldChrZXksIG5vZGVzKTtcbiAgICAgIH1cblxuICAgICAgbm9kZXMuYWRkKG5vZGUpO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGdldFBhcmVudChub2RlKSB7XG4gICAgICBpZiAobm9kZS5wYXJlbnQgJiYgbm9kZS5wYXJlbnQudHlwZSA9PT0gJ1RTTW9kdWxlQmxvY2snKSB7XG4gICAgICAgIHJldHVybiBub2RlLnBhcmVudC5wYXJlbnQ7XG4gICAgICB9XG5cbiAgICAgIC8vIGp1c3QgaW4gY2FzZSBzb21laG93IGEgbm9uLXRzIG5hbWVzcGFjZSBleHBvcnQgZGVjbGFyYXRpb24gaXNuJ3QgZGlyZWN0bHlcbiAgICAgIC8vIHBhcmVudGVkIHRvIHRoZSByb290IFByb2dyYW0gbm9kZVxuICAgICAgcmV0dXJuIHJvb3RQcm9ncmFtO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICBFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBhZGROYW1lZCgnZGVmYXVsdCcsIG5vZGUsIGdldFBhcmVudChub2RlKSk7XG4gICAgICB9LFxuXG4gICAgICBFeHBvcnRTcGVjaWZpZXIobm9kZSkge1xuICAgICAgICBhZGROYW1lZChcbiAgICAgICAgICBub2RlLmV4cG9ydGVkLm5hbWUgfHwgbm9kZS5leHBvcnRlZC52YWx1ZSxcbiAgICAgICAgICBub2RlLmV4cG9ydGVkLFxuICAgICAgICAgIGdldFBhcmVudChub2RlLnBhcmVudCksXG4gICAgICAgICk7XG4gICAgICB9LFxuXG4gICAgICBFeHBvcnROYW1lZERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24gPT0gbnVsbCkgcmV0dXJuO1xuXG4gICAgICAgIGNvbnN0IHBhcmVudCA9IGdldFBhcmVudChub2RlKTtcbiAgICAgICAgLy8gc3VwcG9ydCBmb3Igb2xkIFR5cGVTY3JpcHQgdmVyc2lvbnNcbiAgICAgICAgY29uc3QgaXNUeXBlVmFyaWFibGVEZWNsID0gbm9kZS5kZWNsYXJhdGlvbi5raW5kID09PSAndHlwZSc7XG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uaWQgIT0gbnVsbCkge1xuICAgICAgICAgIGlmIChpbmNsdWRlcyhbXG4gICAgICAgICAgICAnVFNUeXBlQWxpYXNEZWNsYXJhdGlvbicsXG4gICAgICAgICAgICAnVFNJbnRlcmZhY2VEZWNsYXJhdGlvbicsXG4gICAgICAgICAgXSwgbm9kZS5kZWNsYXJhdGlvbi50eXBlKSkge1xuICAgICAgICAgICAgYWRkTmFtZWQobm9kZS5kZWNsYXJhdGlvbi5pZC5uYW1lLCBub2RlLmRlY2xhcmF0aW9uLmlkLCBwYXJlbnQsIHRydWUpO1xuICAgICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgICBhZGROYW1lZChub2RlLmRlY2xhcmF0aW9uLmlkLm5hbWUsIG5vZGUuZGVjbGFyYXRpb24uaWQsIHBhcmVudCwgaXNUeXBlVmFyaWFibGVEZWNsKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cblxuICAgICAgICBpZiAobm9kZS5kZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMgIT0gbnVsbCkge1xuICAgICAgICAgIGZvciAoY29uc3QgZGVjbGFyYXRpb24gb2Ygbm9kZS5kZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMpIHtcbiAgICAgICAgICAgIHJlY3Vyc2l2ZVBhdHRlcm5DYXB0dXJlKGRlY2xhcmF0aW9uLmlkLCB2ID0+XG4gICAgICAgICAgICAgIGFkZE5hbWVkKHYubmFtZSwgdiwgcGFyZW50LCBpc1R5cGVWYXJpYWJsZURlY2wpKTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cbiAgICAgIH0sXG5cbiAgICAgIEV4cG9ydEFsbERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUuc291cmNlID09IG51bGwpIHJldHVybjsgLy8gbm90IHN1cmUgaWYgdGhpcyBpcyBldmVyIHRydWVcblxuICAgICAgICAvLyBgZXhwb3J0ICogYXMgWCBmcm9tICdwYXRoJ2AgZG9lcyBub3QgY29uZmxpY3RcbiAgICAgICAgaWYgKG5vZGUuZXhwb3J0ZWQgJiYgbm9kZS5leHBvcnRlZC5uYW1lKSByZXR1cm47XG5cbiAgICAgICAgY29uc3QgcmVtb3RlRXhwb3J0cyA9IEV4cG9ydE1hcC5nZXQobm9kZS5zb3VyY2UudmFsdWUsIGNvbnRleHQpO1xuICAgICAgICBpZiAocmVtb3RlRXhwb3J0cyA9PSBudWxsKSByZXR1cm47XG5cbiAgICAgICAgaWYgKHJlbW90ZUV4cG9ydHMuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICAgIHJlbW90ZUV4cG9ydHMucmVwb3J0RXJyb3JzKGNvbnRleHQsIG5vZGUpO1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIGNvbnN0IHBhcmVudCA9IGdldFBhcmVudChub2RlKTtcblxuICAgICAgICBsZXQgYW55ID0gZmFsc2U7XG4gICAgICAgIHJlbW90ZUV4cG9ydHMuZm9yRWFjaCgodiwgbmFtZSkgPT4ge1xuICAgICAgICAgIGlmIChuYW1lICE9PSAnZGVmYXVsdCcpIHtcbiAgICAgICAgICAgIGFueSA9IHRydWU7IC8vIHBvb3IgbWFuJ3MgZmlsdGVyXG4gICAgICAgICAgICBhZGROYW1lZChuYW1lLCBub2RlLCBwYXJlbnQpO1xuICAgICAgICAgIH1cbiAgICAgICAgfSk7XG5cbiAgICAgICAgaWYgKCFhbnkpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydChcbiAgICAgICAgICAgIG5vZGUuc291cmNlLFxuICAgICAgICAgICAgYE5vIG5hbWVkIGV4cG9ydHMgZm91bmQgaW4gbW9kdWxlICcke25vZGUuc291cmNlLnZhbHVlfScuYCxcbiAgICAgICAgICApO1xuICAgICAgICB9XG4gICAgICB9LFxuXG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24gKCkge1xuICAgICAgICBmb3IgKGNvbnN0IFssIG5hbWVkXSBvZiBuYW1lc3BhY2UpIHtcbiAgICAgICAgICBmb3IgKGNvbnN0IFtuYW1lLCBub2Rlc10gb2YgbmFtZWQpIHtcbiAgICAgICAgICAgIGlmIChub2Rlcy5zaXplIDw9IDEpIGNvbnRpbnVlO1xuXG4gICAgICAgICAgICBpZiAoaXNUeXBlc2NyaXB0RnVuY3Rpb25PdmVybG9hZHMobm9kZXMpIHx8IGlzVHlwZXNjcmlwdE5hbWVzcGFjZU1lcmdpbmcobm9kZXMpKSBjb250aW51ZTtcblxuICAgICAgICAgICAgZm9yIChjb25zdCBub2RlIG9mIG5vZGVzKSB7XG4gICAgICAgICAgICAgIGlmIChzaG91bGRTa2lwVHlwZXNjcmlwdE5hbWVzcGFjZShub2RlLCBub2RlcykpIGNvbnRpbnVlO1xuXG4gICAgICAgICAgICAgIGlmIChuYW1lID09PSAnZGVmYXVsdCcpIHtcbiAgICAgICAgICAgICAgICBjb250ZXh0LnJlcG9ydChub2RlLCAnTXVsdGlwbGUgZGVmYXVsdCBleHBvcnRzLicpO1xuICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgICAgICAgIGBNdWx0aXBsZSBleHBvcnRzIG9mIG5hbWUgJyR7bmFtZS5yZXBsYWNlKHRzVHlwZVByZWZpeCwgJycpfScuYCxcbiAgICAgICAgICAgICAgICApO1xuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH07XG4gIH0sXG59O1xuIl19