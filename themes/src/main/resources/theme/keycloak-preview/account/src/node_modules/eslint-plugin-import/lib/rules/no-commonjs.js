'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

const EXPORT_MESSAGE = 'Expected "export" or "export default"',
      IMPORT_MESSAGE = 'Expected "import" instead of "require()"'; /**
                                                                    * @fileoverview Rule to prefer ES6 to CJS
                                                                    * @author Jamund Ferguson
                                                                    */

function normalizeLegacyOptions(options) {
  if (options.indexOf('allow-primitive-modules') >= 0) {
    return { allowPrimitiveModules: true };
  }
  return options[0] || {};
}

function allowPrimitive(node, options) {
  if (!options.allowPrimitiveModules) return false;
  if (node.parent.type !== 'AssignmentExpression') return false;
  return node.parent.right.type !== 'ObjectExpression';
}

function allowRequire(node, options) {
  return options.allowRequire;
}

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

const schemaString = { enum: ['allow-primitive-modules'] };
const schemaObject = {
  type: 'object',
  properties: {
    allowPrimitiveModules: { 'type': 'boolean' },
    allowRequire: { 'type': 'boolean' }
  },
  additionalProperties: false
};

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-commonjs')
    },

    schema: {
      anyOf: [{
        type: 'array',
        items: [schemaString],
        additionalItems: false
      }, {
        type: 'array',
        items: [schemaObject],
        additionalItems: false
      }]
    }
  },

  create: function (context) {
    const options = normalizeLegacyOptions(context.options);

    return {

      'MemberExpression': function (node) {

        // module.exports
        if (node.object.name === 'module' && node.property.name === 'exports') {
          if (allowPrimitive(node, options)) return;
          context.report({ node, message: EXPORT_MESSAGE });
        }

        // exports.
        if (node.object.name === 'exports') {
          const isInScope = context.getScope().variables.some(variable => variable.name === 'exports');
          if (!isInScope) {
            context.report({ node, message: EXPORT_MESSAGE });
          }
        }
      },
      'CallExpression': function (call) {
        if (context.getScope().type !== 'module') return;
        if (call.parent.type !== 'ExpressionStatement' && call.parent.type !== 'VariableDeclarator') return;

        if (call.callee.type !== 'Identifier') return;
        if (call.callee.name !== 'require') return;

        if (call.arguments.length !== 1) return;
        var module = call.arguments[0];

        if (module.type !== 'Literal') return;
        if (typeof module.value !== 'string') return;

        if (allowRequire(call, options)) return;

        // keeping it simple: all 1-string-arg `require` calls are reported
        context.report({
          node: call.callee,
          message: IMPORT_MESSAGE
        });
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWNvbW1vbmpzLmpzIl0sIm5hbWVzIjpbIkVYUE9SVF9NRVNTQUdFIiwiSU1QT1JUX01FU1NBR0UiLCJub3JtYWxpemVMZWdhY3lPcHRpb25zIiwib3B0aW9ucyIsImluZGV4T2YiLCJhbGxvd1ByaW1pdGl2ZU1vZHVsZXMiLCJhbGxvd1ByaW1pdGl2ZSIsIm5vZGUiLCJwYXJlbnQiLCJ0eXBlIiwicmlnaHQiLCJhbGxvd1JlcXVpcmUiLCJzY2hlbWFTdHJpbmciLCJlbnVtIiwic2NoZW1hT2JqZWN0IiwicHJvcGVydGllcyIsImFkZGl0aW9uYWxQcm9wZXJ0aWVzIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiYW55T2YiLCJpdGVtcyIsImFkZGl0aW9uYWxJdGVtcyIsImNyZWF0ZSIsImNvbnRleHQiLCJvYmplY3QiLCJuYW1lIiwicHJvcGVydHkiLCJyZXBvcnQiLCJtZXNzYWdlIiwiaXNJblNjb3BlIiwiZ2V0U2NvcGUiLCJ2YXJpYWJsZXMiLCJzb21lIiwidmFyaWFibGUiLCJjYWxsIiwiY2FsbGVlIiwiYXJndW1lbnRzIiwibGVuZ3RoIiwidmFsdWUiXSwibWFwcGluZ3MiOiI7O0FBS0E7Ozs7OztBQUVBLE1BQU1BLGlCQUFpQix1Q0FBdkI7QUFBQSxNQUNNQyxpQkFBaUIsMENBRHZCLEMsQ0FQQTs7Ozs7QUFVQSxTQUFTQyxzQkFBVCxDQUFnQ0MsT0FBaEMsRUFBeUM7QUFDdkMsTUFBSUEsUUFBUUMsT0FBUixDQUFnQix5QkFBaEIsS0FBOEMsQ0FBbEQsRUFBcUQ7QUFDbkQsV0FBTyxFQUFFQyx1QkFBdUIsSUFBekIsRUFBUDtBQUNEO0FBQ0QsU0FBT0YsUUFBUSxDQUFSLEtBQWMsRUFBckI7QUFDRDs7QUFFRCxTQUFTRyxjQUFULENBQXdCQyxJQUF4QixFQUE4QkosT0FBOUIsRUFBdUM7QUFDckMsTUFBSSxDQUFDQSxRQUFRRSxxQkFBYixFQUFvQyxPQUFPLEtBQVA7QUFDcEMsTUFBSUUs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QUFFakM7QUFDQXdCLGdCQUFRSSxNQUFSLENBQWU7QUFDYnhCLGdCQUFNK0IsS0FBS0MsTUFERTtBQUViUCxtQkFBUy9CO0FBRkksU0FBZjtBQUlEO0FBNUNJLEtBQVA7QUErQ0Q7QUF6RWMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbm8tY29tbW9uanMuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlb3ZlcnZpZXcgUnVsZSB0byBwcmVmZXIgRVM2IHRvIENKU1xuICogQGF1dGhvciBKYW11bmQgRmVyZ3Vzb25cbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5jb25zdCBFWFBPUlRfTUVTU0FHRSA9ICdFeHBlY3RlZCBcImV4cG9ydFwiIG9yIFwiZXhwb3J0IGRlZmF1bHRcIidcbiAgICAsIElNUE9SVF9NRVNTQUdFID0gJ0V4cGVjdGVkIFwiaW1wb3J0XCIgaW5zdGVhZCBvZiBcInJlcXVpcmUoKVwiJ1xuXG5mdW5jdGlvbiBub3JtYWxpemVMZWdhY3lPcHRpb25zKG9wdGlvbnMpIHtcbiAgaWYgKG9wdGlvbnMuaW5kZXhPZignYWxsb3ctcHJpbWl0aXZlLW1vZHVsZXMnKSA+PSAwKSB7XG4gICAgcmV0dXJuIHsgYWxsb3dQcmltaXRpdmVNb2R1bGVzOiB0cnVlIH1cbiAgfVxuICByZXR1cm4gb3B0aW9uc1swXSB8fCB7fVxufVxuXG5mdW5jdGlvbiBhbGxvd1ByaW1pdGl2ZShub2RlLCBvcHRpb25zKSB7XG4gIGlmICghb3B0aW9ucy5hbGxvd1ByaW1pdGl2ZU1vZHVsZXMpIHJldHVybiBmYWxzZVxuICBpZiAobm9kZS5wYXJlbnQudHlwZSAhPT0gJ0Fzc2lnbm1lbnRFeHByZXNzaW9uJykgcmV0dXJuIGZhbHNlXG4gIHJldHVybiAobm9kZS5wYXJlbnQucmlnaHQudHlwZSAhPT0gJ09iamVjdEV4cHJlc3Npb24nKVxufVxuXG5mdW5jdGlvbiBhbGxvd1JlcXVpcmUobm9kZSwgb3B0aW9ucykge1xuICByZXR1cm4gb3B0aW9ucy5hbGxvd1JlcXVpcmVcbn1cblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFJ1bGUgRGVmaW5pdGlvblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxuY29uc3Qgc2NoZW1hU3RyaW5nID0geyBlbnVtOiBbJ2FsbG93LXByaW1pdGl2ZS1tb2R1bGVzJ10gfVxuY29uc3Qgc2NoZW1hT2JqZWN0ID0ge1xuICB0eXBlOiAnb2JqZWN0JyxcbiAgcHJvcGVydGllczoge1xuICAgIGFsbG93UHJpbWl0aXZlTW9kdWxlczogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgIGFsbG93UmVxdWlyZTogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICB9LFxuICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tY29tbW9uanMnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiB7XG4gICAgICBhbnlPZjogW1xuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW3NjaGVtYVN0cmluZ10sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgICAge1xuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgaXRlbXM6IFtzY2hlbWFPYmplY3RdLFxuICAgICAgICAgIGFkZGl0aW9uYWxJdGVtczogZmFsc2UsXG4gICAgICAgIH0sXG4gICAgICBdLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBub3JtYWxpemVMZWdhY3lPcHRpb25zKGNvbnRleHQub3B0aW9ucylcblxuICAgIHJldHVybiB7XG5cbiAgICAgICdNZW1iZXJFeHByZXNzaW9uJzogZnVuY3Rpb24gKG5vZGUpIHtcblxuICAgICAgICAvLyBtb2R1bGUuZXhwb3J0c1xuICAgICAgICBpZiAobm9kZS5vYmplY3QubmFtZSA9PT0gJ21vZHVsZScgJiYgbm9kZS5wcm9wZXJ0eS5uYW1lID09PSAnZXhwb3J0cycpIHtcbiAgICAgICAgICBpZiAoYWxsb3dQcmltaXRpdmUobm9kZSwgb3B0aW9ucykpIHJldHVyblxuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogRVhQT1JUX01FU1NBR0UgfSlcbiAgICAgICAgfVxuXG4gICAgICAgIC8vIGV4cG9ydHMuXG4gICAgICAgIGlmIChub2RlLm9iamVjdC5uYW1lID09PSAnZXhwb3J0cycpIHtcbiAgICAgICAgICBjb25zdCBpc0luU2NvcGUgPSBjb250ZXh0LmdldFNjb3BlKClcbiAgICAgICAgICAgIC52YXJpYWJsZXNcbiAgICAgICAgICAgIC5zb21lKHZhcmlhYmxlID0+IHZhcmlhYmxlLm5hbWUgPT09ICdleHBvcnRzJylcbiAgICAgICAgICBpZiAoISBpc0luU2NvcGUpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogRVhQT1JUX01FU1NBR0UgfSlcbiAgICAgICAgICB9XG4gICAgICAgIH1cblxuICAgICAgfSxcbiAgICAgICdDYWxsRXhwcmVzc2lvbic6IGZ1bmN0aW9uIChjYWxsKSB7XG4gICAgICAgIGlmIChjb250ZXh0LmdldFNjb3BlKCkudHlwZSAhPT0gJ21vZHVsZScpIHJldHVyblxuICAgICAgICBpZiAoXG4gICAgICAgICAgY2FsbC5wYXJlbnQudHlwZSAhPT0gJ0V4cHJlc3Npb25TdGF0ZW1lbnQnXG4gICAgICAgICAgJiYgY2FsbC5wYXJlbnQudHlwZSAhPT0gJ1ZhcmlhYmxlRGVjbGFyYXRvcidcbiAgICAgICAgKSByZXR1cm5cblxuICAgICAgICBpZiAoY2FsbC5jYWxsZWUudHlwZSAhPT0gJ0lkZW50aWZpZXInKSByZXR1cm5cbiAgICAgICAgaWYgKGNhbGwuY2FsbGVlLm5hbWUgIT09ICdyZXF1aXJlJykgcmV0dXJuXG5cbiAgICAgICAgaWYgKGNhbGwuYXJndW1lbnRzLmxlbmd0aCAhPT0gMSkgcmV0dXJuXG4gICAgICAgIHZhciBtb2R1bGUgPSBjYWxsLmFyZ3VtZW50c1swXVxuXG4gICAgICAgIGlmIChtb2R1bGUudHlwZSAhPT0gJ0xpdGVyYWwnKSByZXR1cm5cbiAgICAgICAgaWYgKHR5cGVvZiBtb2R1bGUudmFsdWUgIT09ICdzdHJpbmcnKSByZXR1cm5cblxuICAgICAgICBpZiAoYWxsb3dSZXF1aXJlKGNhbGwsIG9wdGlvbnMpKSByZXR1cm5cblxuICAgICAgICAvLyBrZWVwaW5nIGl0IHNpbXBsZTogYWxsIDEtc3RyaW5nLWFyZyBgcmVxdWlyZWAgY2FsbHMgYXJlIHJlcG9ydGVkXG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBjYWxsLmNhbGxlZSxcbiAgICAgICAgICBtZXNzYWdlOiBJTVBPUlRfTUVTU0FHRSxcbiAgICAgICAgfSlcbiAgICAgIH0sXG4gICAgfVxuXG4gIH0sXG59XG4iXX0=