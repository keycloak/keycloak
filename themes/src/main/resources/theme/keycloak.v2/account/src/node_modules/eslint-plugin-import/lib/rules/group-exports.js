'use strict';var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);
var _object = require('object.values');var _object2 = _interopRequireDefault(_object);
var _arrayPrototype = require('array.prototype.flat');var _arrayPrototype2 = _interopRequireDefault(_arrayPrototype);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var meta = {
  type: 'suggestion',
  docs: {
    url: (0, _docsUrl2['default'])('group-exports') } };


/* eslint-disable max-len */
var errors = {
  ExportNamedDeclaration: 'Multiple named export declarations; consolidate all named exports into a single export declaration',
  AssignmentExpression: 'Multiple CommonJS exports; consolidate all exports into a single assignment to `module.exports`' };

/* eslint-enable max-len */

/**
                             * Returns an array with names of the properties in the accessor chain for MemberExpression nodes
                             *
                             * Example:
                             *
                             * `module.exports = {}` => ['module', 'exports']
                             * `module.exports.property = true` => ['module', 'exports', 'property']
                             *
                             * @param     {Node}    node    AST Node (MemberExpression)
                             * @return    {Array}           Array with the property names in the chain
                             * @private
                             */
function accessorChain(node) {
  var chain = [];

  do {
    chain.unshift(node.property.name);

    if (node.object.type === 'Identifier') {
      chain.unshift(node.object.name);
      break;
    }

    node = node.object;
  } while (node.type === 'MemberExpression');

  return chain;
}

function create(context) {
  var nodes = {
    modules: {
      set: new Set(),
      sources: {} },

    types: {
      set: new Set(),
      sources: {} },

    commonjs: {
      set: new Set() } };



  return {
    ExportNamedDeclaration: function () {function ExportNamedDeclaration(node) {
        var target = node.exportKind === 'type' ? nodes.types : nodes.modules;
        if (!node.source) {
          target.set.add(node);
        } else if (Array.isArray(target.sources[node.source.value])) {
          target.sources[node.source.value].push(node);
        } else {
          target.sources[node.source.value] = [node];
        }
      }return ExportNamedDeclaration;}(),

    AssignmentExpression: function () {function AssignmentExpression(node) {
        if (node.left.type !== 'MemberExpression') {
          return;
        }

        var chain = accessorChain(node.left);

        // Assignments to module.exports
        // Deeper assignments are ignored since they just modify what's already being exported
        // (ie. module.exports.exported.prop = true is ignored)
        if (chain[0] === 'module' && chain[1] === 'exports' && chain.length <= 3) {
          nodes.commonjs.set.add(node);
          return;
        }

        // Assignments to exports (exports.* = *)
        if (chain[0] === 'exports' && chain.length === 2) {
          nodes.commonjs.set.add(node);
          return;
        }
      }return AssignmentExpression;}(),

    'Program:exit': function () {function onExit() {
        // Report multiple `export` declarations (ES2015 modules)
        if (nodes.modules.set.size > 1) {
          nodes.modules.set.forEach(function (node) {
            context.report({
              node: node,
              message: errors[node.type] });

          });
        }

        // Report multiple `aggregated exports` from the same module (ES2015 modules)
        (0, _arrayPrototype2['default'])((0, _object2['default'])(nodes.modules.sources).
        filter(function (nodesWithSource) {return Array.isArray(nodesWithSource) && nodesWithSource.length > 1;})).
        forEach(function (node) {
          context.report({
            node: node,
            message: errors[node.type] });

        });

        // Report multiple `export type` declarations (FLOW ES2015 modules)
        if (nodes.types.set.size > 1) {
          nodes.types.set.forEach(function (node) {
            context.report({
              node: node,
              message: errors[node.type] });

          });
        }

        // Report multiple `aggregated type exports` from the same module (FLOW ES2015 modules)
        (0, _arrayPrototype2['default'])((0, _object2['default'])(nodes.types.sources).
        filter(function (nodesWithSource) {return Array.isArray(nodesWithSource) && nodesWithSource.length > 1;})).
        forEach(function (node) {
          context.report({
            node: node,
            message: errors[node.type] });

        });

        // Report multiple `module.exports` assignments (CommonJS)
        if (nodes.commonjs.set.size > 1) {
          nodes.commonjs.set.forEach(function (node) {
            context.report({
              node: node,
              message: errors[node.type] });

          });
        }
      }return onExit;}() };

}

module.exports = {
  meta: meta,
  create: create };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9ncm91cC1leHBvcnRzLmpzIl0sIm5hbWVzIjpbIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsImVycm9ycyIsIkV4cG9ydE5hbWVkRGVjbGFyYXRpb24iLCJBc3NpZ25tZW50RXhwcmVzc2lvbiIsImFjY2Vzc29yQ2hhaW4iLCJub2RlIiwiY2hhaW4iLCJ1bnNoaWZ0IiwicHJvcGVydHkiLCJuYW1lIiwib2JqZWN0IiwiY3JlYXRlIiwiY29udGV4dCIsIm5vZGVzIiwibW9kdWxlcyIsInNldCIsIlNldCIsInNvdXJjZXMiLCJ0eXBlcyIsImNvbW1vbmpzIiwidGFyZ2V0IiwiZXhwb3J0S2luZCIsInNvdXJjZSIsImFkZCIsIkFycmF5IiwiaXNBcnJheSIsInZhbHVlIiwicHVzaCIsImxlZnQiLCJsZW5ndGgiLCJvbkV4aXQiLCJzaXplIiwiZm9yRWFjaCIsInJlcG9ydCIsIm1lc3NhZ2UiLCJmaWx0ZXIiLCJub2Rlc1dpdGhTb3VyY2UiLCJtb2R1bGUiLCJleHBvcnRzIl0sIm1hcHBpbmdzIjoiYUFBQSxxQztBQUNBLHVDO0FBQ0Esc0Q7O0FBRUEsSUFBTUEsT0FBTztBQUNYQyxRQUFNLFlBREs7QUFFWEMsUUFBTTtBQUNKQyxTQUFLLDBCQUFRLGVBQVIsQ0FERCxFQUZLLEVBQWI7OztBQU1BO0FBQ0EsSUFBTUMsU0FBUztBQUNiQywwQkFBd0Isb0dBRFg7QUFFYkMsd0JBQXNCLGlHQUZULEVBQWY7O0FBSUE7O0FBRUE7Ozs7Ozs7Ozs7OztBQVlBLFNBQVNDLGFBQVQsQ0FBdUJDLElBQXZCLEVBQTZCO0FBQzNCLE1BQU1DLFFBQVEsRUFBZDs7QUFFQSxLQUFHO0FBQ0RBLFVBQU1DLE9BQU4sQ0FBY0YsS0FBS0csUUFBTCxDQUFjQyxJQUE1Qjs7QUFFQSxRQUFJSixLQUFLSyxNQUFMLENBQVlaLElBQVosS0FBcUIsWUFBekIsRUFBdUM7QUFDckNRLFlBQU1DLE9BQU4sQ0FBY0YsS0FBS0ssTUFBTCxDQUFZRCxJQUExQjtBQUNB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QU1LLEtBQU4sQ0FBWUgsR0FBWixDQUFnQmdCLElBQWhCLEdBQXVCLENBQTNCLEVBQThCO0FBQzVCbEIsZ0JBQU1LLEtBQU4sQ0FBWUgsR0FBWixDQUFnQmlCLE9BQWhCLENBQXdCLGdCQUFRO0FBQzlCcEIsb0JBQVFxQixNQUFSLENBQWU7QUFDYjVCLHdCQURhO0FBRWI2Qix1QkFBU2pDLE9BQU9JLEtBQUtQLElBQVosQ0FGSSxFQUFmOztBQUlELFdBTEQ7QUFNRDs7QUFFRDtBQUNBLHlDQUFLLHlCQUFPZSxNQUFNSyxLQUFOLENBQVlELE9BQW5CO0FBQ0ZrQixjQURFLENBQ0ssbUNBQW1CWCxNQUFNQyxPQUFOLENBQWNXLGVBQWQsS0FBa0NBLGdCQUFnQlAsTUFBaEIsR0FBeUIsQ0FBOUUsRUFETCxDQUFMO0FBRUdHLGVBRkgsQ0FFVyxVQUFDM0IsSUFBRCxFQUFVO0FBQ2pCTyxrQkFBUXFCLE1BQVIsQ0FBZTtBQUNiNUIsc0JBRGE7QUFFYjZCLHFCQUFTakMsT0FBT0ksS0FBS1AsSUFBWixDQUZJLEVBQWY7O0FBSUQsU0FQSDs7QUFTQTtBQUNBLFlBQUllLE1BQU1NLFFBQU4sQ0FBZUosR0FBZixDQUFtQmdCLElBQW5CLEdBQTBCLENBQTlCLEVBQWlDO0FBQy9CbEIsZ0JBQU1NLFFBQU4sQ0FBZUosR0FBZixDQUFtQmlCLE9BQW5CLENBQTJCLGdCQUFRO0FBQ2pDcEIsb0JBQVFxQixNQUFSLENBQWU7QUFDYjVCLHdCQURhO0FBRWI2Qix1QkFBU2pDLE9BQU9JLEtBQUtQLElBQVosQ0FGSSxFQUFmOztBQUlELFdBTEQ7QUFNRDtBQUNGLE9BbERELE9BQXlCZ0MsTUFBekIsSUFsQ0ssRUFBUDs7QUFzRkQ7O0FBRURPLE9BQU9DLE9BQVAsR0FBaUI7QUFDZnpDLFlBRGU7QUFFZmMsZ0JBRmUsRUFBakIiLCJmaWxlIjoiZ3JvdXAtZXhwb3J0cy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuaW1wb3J0IHZhbHVlcyBmcm9tICdvYmplY3QudmFsdWVzJztcbmltcG9ydCBmbGF0IGZyb20gJ2FycmF5LnByb3RvdHlwZS5mbGF0JztcblxuY29uc3QgbWV0YSA9IHtcbiAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICBkb2NzOiB7XG4gICAgdXJsOiBkb2NzVXJsKCdncm91cC1leHBvcnRzJyksXG4gIH0sXG59O1xuLyogZXNsaW50LWRpc2FibGUgbWF4LWxlbiAqL1xuY29uc3QgZXJyb3JzID0ge1xuICBFeHBvcnROYW1lZERlY2xhcmF0aW9uOiAnTXVsdGlwbGUgbmFtZWQgZXhwb3J0IGRlY2xhcmF0aW9uczsgY29uc29saWRhdGUgYWxsIG5hbWVkIGV4cG9ydHMgaW50byBhIHNpbmdsZSBleHBvcnQgZGVjbGFyYXRpb24nLFxuICBBc3NpZ25tZW50RXhwcmVzc2lvbjogJ011bHRpcGxlIENvbW1vbkpTIGV4cG9ydHM7IGNvbnNvbGlkYXRlIGFsbCBleHBvcnRzIGludG8gYSBzaW5nbGUgYXNzaWdubWVudCB0byBgbW9kdWxlLmV4cG9ydHNgJyxcbn07XG4vKiBlc2xpbnQtZW5hYmxlIG1heC1sZW4gKi9cblxuLyoqXG4gKiBSZXR1cm5zIGFuIGFycmF5IHdpdGggbmFtZXMgb2YgdGhlIHByb3BlcnRpZXMgaW4gdGhlIGFjY2Vzc29yIGNoYWluIGZvciBNZW1iZXJFeHByZXNzaW9uIG5vZGVzXG4gKlxuICogRXhhbXBsZTpcbiAqXG4gKiBgbW9kdWxlLmV4cG9ydHMgPSB7fWAgPT4gWydtb2R1bGUnLCAnZXhwb3J0cyddXG4gKiBgbW9kdWxlLmV4cG9ydHMucHJvcGVydHkgPSB0cnVlYCA9PiBbJ21vZHVsZScsICdleHBvcnRzJywgJ3Byb3BlcnR5J11cbiAqXG4gKiBAcGFyYW0gICAgIHtOb2RlfSAgICBub2RlICAgIEFTVCBOb2RlIChNZW1iZXJFeHByZXNzaW9uKVxuICogQHJldHVybiAgICB7QXJyYXl9ICAgICAgICAgICBBcnJheSB3aXRoIHRoZSBwcm9wZXJ0eSBuYW1lcyBpbiB0aGUgY2hhaW5cbiAqIEBwcml2YXRlXG4gKi9cbmZ1bmN0aW9uIGFjY2Vzc29yQ2hhaW4obm9kZSkge1xuICBjb25zdCBjaGFpbiA9IFtdO1xuXG4gIGRvIHtcbiAgICBjaGFpbi51bnNoaWZ0KG5vZGUucHJvcGVydHkubmFtZSk7XG5cbiAgICBpZiAobm9kZS5vYmplY3QudHlwZSA9PT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICBjaGFpbi51bnNoaWZ0KG5vZGUub2JqZWN0Lm5hbWUpO1xuICAgICAgYnJlYWs7XG4gICAgfVxuXG4gICAgbm9kZSA9IG5vZGUub2JqZWN0O1xuICB9IHdoaWxlIChub2RlLnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJyk7XG5cbiAgcmV0dXJuIGNoYWluO1xufVxuXG5mdW5jdGlvbiBjcmVhdGUoY29udGV4dCkge1xuICBjb25zdCBub2RlcyA9IHtcbiAgICBtb2R1bGVzOiB7XG4gICAgICBzZXQ6IG5ldyBTZXQoKSxcbiAgICAgIHNvdXJjZXM6IHt9LFxuICAgIH0sXG4gICAgdHlwZXM6IHtcbiAgICAgIHNldDogbmV3IFNldCgpLFxuICAgICAgc291cmNlczoge30sXG4gICAgfSxcbiAgICBjb21tb25qczoge1xuICAgICAgc2V0OiBuZXcgU2V0KCksXG4gICAgfSxcbiAgfTtcblxuICByZXR1cm4ge1xuICAgIEV4cG9ydE5hbWVkRGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgY29uc3QgdGFyZ2V0ID0gbm9kZS5leHBvcnRLaW5kID09PSAndHlwZScgPyBub2Rlcy50eXBlcyA6IG5vZGVzLm1vZHVsZXM7XG4gICAgICBpZiAoIW5vZGUuc291cmNlKSB7XG4gICAgICAgIHRhcmdldC5zZXQuYWRkKG5vZGUpO1xuICAgICAgfSBlbHNlIGlmIChBcnJheS5pc0FycmF5KHRhcmdldC5zb3VyY2VzW25vZGUuc291cmNlLnZhbHVlXSkpIHtcbiAgICAgICAgdGFyZ2V0LnNvdXJjZXNbbm9kZS5zb3VyY2UudmFsdWVdLnB1c2gobm9kZSk7XG4gICAgICB9IGVsc2Uge1xuICAgICAgICB0YXJnZXQuc291cmNlc1tub2RlLnNvdXJjZS52YWx1ZV0gPSBbbm9kZV07XG4gICAgICB9XG4gICAgfSxcblxuICAgIEFzc2lnbm1lbnRFeHByZXNzaW9uKG5vZGUpIHtcbiAgICAgIGlmIChub2RlLmxlZnQudHlwZSAhPT0gJ01lbWJlckV4cHJlc3Npb24nKSB7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgY29uc3QgY2hhaW4gPSBhY2Nlc3NvckNoYWluKG5vZGUubGVmdCk7XG5cbiAgICAgIC8vIEFzc2lnbm1lbnRzIHRvIG1vZHVsZS5leHBvcnRzXG4gICAgICAvLyBEZWVwZXIgYXNzaWdubWVudHMgYXJlIGlnbm9yZWQgc2luY2UgdGhleSBqdXN0IG1vZGlmeSB3aGF0J3MgYWxyZWFkeSBiZWluZyBleHBvcnRlZFxuICAgICAgLy8gKGllLiBtb2R1bGUuZXhwb3J0cy5leHBvcnRlZC5wcm9wID0gdHJ1ZSBpcyBpZ25vcmVkKVxuICAgICAgaWYgKGNoYWluWzBdID09PSAnbW9kdWxlJyAmJiBjaGFpblsxXSA9PT0gJ2V4cG9ydHMnICYmIGNoYWluLmxlbmd0aCA8PSAzKSB7XG4gICAgICAgIG5vZGVzLmNvbW1vbmpzLnNldC5hZGQobm9kZSk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgLy8gQXNzaWdubWVudHMgdG8gZXhwb3J0cyAoZXhwb3J0cy4qID0gKilcbiAgICAgIGlmIChjaGFpblswXSA9PT0gJ2V4cG9ydHMnICYmIGNoYWluLmxlbmd0aCA9PT0gMikge1xuICAgICAgICBub2Rlcy5jb21tb25qcy5zZXQuYWRkKG5vZGUpO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG4gICAgfSxcblxuICAgICdQcm9ncmFtOmV4aXQnOiBmdW5jdGlvbiBvbkV4aXQoKSB7XG4gICAgICAvLyBSZXBvcnQgbXVsdGlwbGUgYGV4cG9ydGAgZGVjbGFyYXRpb25zIChFUzIwMTUgbW9kdWxlcylcbiAgICAgIGlmIChub2Rlcy5tb2R1bGVzLnNldC5zaXplID4gMSkge1xuICAgICAgICBub2Rlcy5tb2R1bGVzLnNldC5mb3JFYWNoKG5vZGUgPT4ge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiBlcnJvcnNbbm9kZS50eXBlXSxcbiAgICAgICAgICB9KTtcbiAgICAgICAgfSk7XG4gICAgICB9XG5cbiAgICAgIC8vIFJlcG9ydCBtdWx0aXBsZSBgYWdncmVnYXRlZCBleHBvcnRzYCBmcm9tIHRoZSBzYW1lIG1vZHVsZSAoRVMyMDE1IG1vZHVsZXMpXG4gICAgICBmbGF0KHZhbHVlcyhub2Rlcy5tb2R1bGVzLnNvdXJjZXMpXG4gICAgICAgIC5maWx0ZXIobm9kZXNXaXRoU291cmNlID0+IEFycmF5LmlzQXJyYXkobm9kZXNXaXRoU291cmNlKSAmJiBub2Rlc1dpdGhTb3VyY2UubGVuZ3RoID4gMSkpXG4gICAgICAgIC5mb3JFYWNoKChub2RlKSA9PiB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGVycm9yc1tub2RlLnR5cGVdLFxuICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcblxuICAgICAgLy8gUmVwb3J0IG11bHRpcGxlIGBleHBvcnQgdHlwZWAgZGVjbGFyYXRpb25zIChGTE9XIEVTMjAxNSBtb2R1bGVzKVxuICAgICAgaWYgKG5vZGVzLnR5cGVzLnNldC5zaXplID4gMSkge1xuICAgICAgICBub2Rlcy50eXBlcy5zZXQuZm9yRWFjaChub2RlID0+IHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogZXJyb3JzW25vZGUudHlwZV0sXG4gICAgICAgICAgfSk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuXG4gICAgICAvLyBSZXBvcnQgbXVsdGlwbGUgYGFnZ3JlZ2F0ZWQgdHlwZSBleHBvcnRzYCBmcm9tIHRoZSBzYW1lIG1vZHVsZSAoRkxPVyBFUzIwMTUgbW9kdWxlcylcbiAgICAgIGZsYXQodmFsdWVzKG5vZGVzLnR5cGVzLnNvdXJjZXMpXG4gICAgICAgIC5maWx0ZXIobm9kZXNXaXRoU291cmNlID0+IEFycmF5LmlzQXJyYXkobm9kZXNXaXRoU291cmNlKSAmJiBub2Rlc1dpdGhTb3VyY2UubGVuZ3RoID4gMSkpXG4gICAgICAgIC5mb3JFYWNoKChub2RlKSA9PiB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGVycm9yc1tub2RlLnR5cGVdLFxuICAgICAgICAgIH0pO1xuICAgICAgICB9KTtcblxuICAgICAgLy8gUmVwb3J0IG11bHRpcGxlIGBtb2R1bGUuZXhwb3J0c2AgYXNzaWdubWVudHMgKENvbW1vbkpTKVxuICAgICAgaWYgKG5vZGVzLmNvbW1vbmpzLnNldC5zaXplID4gMSkge1xuICAgICAgICBub2Rlcy5jb21tb25qcy5zZXQuZm9yRWFjaChub2RlID0+IHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgbWVzc2FnZTogZXJyb3JzW25vZGUudHlwZV0sXG4gICAgICAgICAgfSk7XG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH0sXG4gIH07XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhLFxuICBjcmVhdGUsXG59O1xuIl19