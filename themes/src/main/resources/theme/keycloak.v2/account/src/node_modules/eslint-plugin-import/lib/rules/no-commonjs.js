'use strict';




var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var EXPORT_MESSAGE = 'Expected "export" or "export default"'; /**
                                                               * @fileoverview Rule to prefer ES6 to CJS
                                                               * @author Jamund Ferguson
                                                               */var IMPORT_MESSAGE = 'Expected "import" instead of "require()"';function normalizeLegacyOptions(options) {
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

function allowConditionalRequire(node, options) {
  return options.allowConditionalRequire !== false;
}

function validateScope(scope) {
  return scope.variableScope.type === 'module';
}

// https://github.com/estree/estree/blob/HEAD/es5.md
function isConditional(node) {
  if (
  node.type === 'IfStatement' ||
  node.type === 'TryStatement' ||
  node.type === 'LogicalExpression' ||
  node.type === 'ConditionalExpression')
  return true;
  if (node.parent) return isConditional(node.parent);
  return false;
}

function isLiteralString(node) {
  return node.type === 'Literal' && typeof node.value === 'string' ||
  node.type === 'TemplateLiteral' && node.expressions.length === 0;
}

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

var schemaString = { 'enum': ['allow-primitive-modules'] };
var schemaObject = {
  type: 'object',
  properties: {
    allowPrimitiveModules: { 'type': 'boolean' },
    allowRequire: { 'type': 'boolean' },
    allowConditionalRequire: { 'type': 'boolean' } },

  additionalProperties: false };


module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-commonjs') },


    schema: {
      anyOf: [
      {
        type: 'array',
        items: [schemaString],
        additionalItems: false },

      {
        type: 'array',
        items: [schemaObject],
        additionalItems: false }] } },





  create: function () {function create(context) {
      var options = normalizeLegacyOptions(context.options);

      return {

        'MemberExpression': function () {function MemberExpression(node) {

            // module.exports
            if (node.object.name === 'module' && node.property.name === 'exports') {
              if (allowPrimitive(node, options)) return;
              context.report({ node: node, message: EXPORT_MESSAGE });
            }

            // exports.
            if (node.object.name === 'exports') {
              var isInScope = context.getScope().
              variables.
              some(function (variable) {return variable.name === 'exports';});
              if (!isInScope) {
                context.report({ node: node, message: EXPORT_MESSAGE });
              }
            }

          }return MemberExpression;}(),
        'CallExpression': function () {function CallExpression(call) {
            if (!validateScope(context.getScope())) return;

            if (call.callee.type !== 'Identifier') return;
            if (call.callee.name !== 'require') return;

            if (call.arguments.length !== 1) return;
            if (!isLiteralString(call.arguments[0])) return;

            if (allowRequire(call, options)) return;

            if (allowConditionalRequire(call, options) && isConditional(call.parent)) return;

            // keeping it simple: all 1-string-arg `require` calls are reported
            context.report({
              node: call.callee,
              message: IMPORT_MESSAGE });

          }return CallExpression;}() };


    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1jb21tb25qcy5qcyJdLCJuYW1lcyI6WyJFWFBPUlRfTUVTU0FHRSIsIklNUE9SVF9NRVNTQUdFIiwibm9ybWFsaXplTGVnYWN5T3B0aW9ucyIsIm9wdGlvbnMiLCJpbmRleE9mIiwiYWxsb3dQcmltaXRpdmVNb2R1bGVzIiwiYWxsb3dQcmltaXRpdmUiLCJub2RlIiwicGFyZW50IiwidHlwZSIsInJpZ2h0IiwiYWxsb3dSZXF1aXJlIiwiYWxsb3dDb25kaXRpb25hbFJlcXVpcmUiLCJ2YWxpZGF0ZVNjb3BlIiwic2NvcGUiLCJ2YXJpYWJsZVNjb3BlIiwiaXNDb25kaXRpb25hbCIsImlzTGl0ZXJhbFN0cmluZyIsInZhbHVlIiwiZXhwcmVzc2lvbnMiLCJsZW5ndGgiLCJzY2hlbWFTdHJpbmciLCJzY2hlbWFPYmplY3QiLCJwcm9wZXJ0aWVzIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJhbnlPZiIsIml0ZW1zIiwiYWRkaXRpb25hbEl0ZW1zIiwiY3JlYXRlIiwiY29udGV4dCIsIm9iamVjdCIsIm5hbWUiLCJwcm9wZXJ0eSIsInJlcG9ydCIsIm1lc3NhZ2UiLCJpc0luU2NvcGUiLCJnZXRTY29wZSIsInZhcmlhYmxlcyIsInNvbWUiLCJ2YXJpYWJsZSIsImNhbGwiLCJjYWxsZWUiLCJhcmd1bWVudHMiXSwibWFwcGluZ3MiOiI7Ozs7O0FBS0EscUM7O0FBRUEsSUFBTUEsaUJBQWlCLHVDQUF2QixDLENBUEE7OztpRUFRQSxJQUFNQyxpQkFBaUIsMENBQXZCLENBRUEsU0FBU0Msc0JBQVQsQ0FBZ0NDLE9BQWhDLEVBQXlDO0FBQ3ZDLE1BQUlBLFFBQVFDLE9BQVIsQ0FBZ0IseUJBQWhCLEtBQThDLENBQWxELEVBQXFEO0FBQ25ELFdBQU8sRUFBRUMsdUJBQXVCLElBQXpCLEVBQVA7QUFDRDtBQUNELFNBQU9GLFFBQVEsQ0FBUixLQUFj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b3ZlcnZpZXcgUnVsZSB0byBwcmVmZXIgRVM2IHRvIENKU1xuICogQGF1dGhvciBKYW11bmQgRmVyZ3Vzb25cbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuY29uc3QgRVhQT1JUX01FU1NBR0UgPSAnRXhwZWN0ZWQgXCJleHBvcnRcIiBvciBcImV4cG9ydCBkZWZhdWx0XCInO1xuY29uc3QgSU1QT1JUX01FU1NBR0UgPSAnRXhwZWN0ZWQgXCJpbXBvcnRcIiBpbnN0ZWFkIG9mIFwicmVxdWlyZSgpXCInO1xuXG5mdW5jdGlvbiBub3JtYWxpemVMZWdhY3lPcHRpb25zKG9wdGlvbnMpIHtcbiAgaWYgKG9wdGlvbnMuaW5kZXhPZignYWxsb3ctcHJpbWl0aXZlLW1vZHVsZXMnKSA+PSAwKSB7XG4gICAgcmV0dXJuIHsgYWxsb3dQcmltaXRpdmVNb2R1bGVzOiB0cnVlIH07XG4gIH1cbiAgcmV0dXJuIG9wdGlvbnNbMF0gfHwge307XG59XG5cbmZ1bmN0aW9uIGFsbG93UHJpbWl0aXZlKG5vZGUsIG9wdGlvbnMpIHtcbiAgaWYgKCFvcHRpb25zLmFsbG93UHJpbWl0aXZlTW9kdWxlcykgcmV0dXJuIGZhbHNlO1xuICBpZiAobm9kZS5wYXJlbnQudHlwZSAhPT0gJ0Fzc2lnbm1lbnRFeHByZXNzaW9uJykgcmV0dXJuIGZhbHNlO1xuICByZXR1cm4gKG5vZGUucGFyZW50LnJpZ2h0LnR5cGUgIT09ICdPYmplY3RFeHByZXNzaW9uJyk7XG59XG5cbmZ1bmN0aW9uIGFsbG93UmVxdWlyZShub2RlLCBvcHRpb25zKSB7XG4gIHJldHVybiBvcHRpb25zLmFsbG93UmVxdWlyZTtcbn1cblxuZnVuY3Rpb24gYWxsb3dDb25kaXRpb25hbFJlcXVpcmUobm9kZSwgb3B0aW9ucykge1xuICByZXR1cm4gb3B0aW9ucy5hbGxvd0NvbmRpdGlvbmFsUmVxdWlyZSAhPT0gZmFsc2U7XG59XG5cbmZ1bmN0aW9uIHZhbGlkYXRlU2NvcGUoc2NvcGUpIHtcbiAgcmV0dXJuIHNjb3BlLnZhcmlhYmxlU2NvcGUudHlwZSA9PT0gJ21vZHVsZSc7XG59XG5cbi8vIGh0dHBzOi8vZ2l0aHViLmNvbS9lc3RyZWUvZXN0cmVlL2Jsb2IvSEVBRC9lczUubWRcbmZ1bmN0aW9uIGlzQ29uZGl0aW9uYWwobm9kZSkge1xuICBpZiAoXG4gICAgbm9kZS50eXBlID09PSAnSWZTdGF0ZW1lbnQnXG4gICAgfHwgbm9kZS50eXBlID09PSAnVHJ5U3RhdGVtZW50J1xuICAgIHx8IG5vZGUudHlwZSA9PT0gJ0xvZ2ljYWxFeHByZXNzaW9uJ1xuICAgIHx8IG5vZGUudHlwZSA9PT0gJ0NvbmRpdGlvbmFsRXhwcmVzc2lvbidcbiAgKSByZXR1cm4gdHJ1ZTtcbiAgaWYgKG5vZGUucGFyZW50KSByZXR1cm4gaXNDb25kaXRpb25hbChub2RlLnBhcmVudCk7XG4gIHJldHVybiBmYWxzZTtcbn1cblxuZnVuY3Rpb24gaXNMaXRlcmFsU3RyaW5nKG5vZGUpIHtcbiAgcmV0dXJuIChub2RlLnR5cGUgPT09ICdMaXRlcmFsJyAmJiB0eXBlb2Ygbm9kZS52YWx1ZSA9PT0gJ3N0cmluZycpIHx8XG4gICAgKG5vZGUudHlwZSA9PT0gJ1RlbXBsYXRlTGl0ZXJhbCcgJiYgbm9kZS5leHByZXNzaW9ucy5sZW5ndGggPT09IDApO1xufVxuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5jb25zdCBzY2hlbWFTdHJpbmcgPSB7IGVudW06IFsnYWxsb3ctcHJpbWl0aXZlLW1vZHVsZXMnXSB9O1xuY29uc3Qgc2NoZW1hT2JqZWN0ID0ge1xuICB0eXBlOiAnb2JqZWN0JyxcbiAgcHJvcGVydGllczoge1xuICAgIGFsbG93UHJpbWl0aXZlTW9kdWxlczogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgIGFsbG93UmVxdWlyZTogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgIGFsbG93Q29uZGl0aW9uYWxSZXF1aXJlOiB7ICd0eXBlJzogJ2Jvb2xlYW4nIH0sXG4gIH0sXG4gIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbn07XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tY29tbW9uanMnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiB7XG4gICAgICBhbnlPZjogW1xuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW3NjaGVtYVN0cmluZ10sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgICAge1xuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgaXRlbXM6IFtzY2hlbWFPYmplY3RdLFxuICAgICAgICAgIGFkZGl0aW9uYWxJdGVtczogZmFsc2UsXG4gICAgICAgIH0sXG4gICAgICBdLFxuICAgIH0sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBvcHRpb25zID0gbm9ybWFsaXplTGVnYWN5T3B0aW9ucyhjb250ZXh0Lm9wdGlvbnMpO1xuXG4gICAgcmV0dXJuIHtcblxuICAgICAgJ01lbWJlckV4cHJlc3Npb24nOiBmdW5jdGlvbiAobm9kZSkge1xuXG4gICAgICAgIC8vIG1vZHVsZS5leHBvcnRzXG4gICAgICAgIGlmIChub2RlLm9iamVjdC5uYW1lID09PSAnbW9kdWxlJyAmJiBub2RlLnByb3BlcnR5Lm5hbWUgPT09ICdleHBvcnRzJykge1xuICAgICAgICAgIGlmIChhbGxvd1ByaW1pdGl2ZShub2RlLCBvcHRpb25zKSkgcmV0dXJuO1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogRVhQT1JUX01FU1NBR0UgfSk7XG4gICAgICAgIH1cblxuICAgICAgICAvLyBleHBvcnRzLlxuICAgICAgICBpZiAobm9kZS5vYmplY3QubmFtZSA9PT0gJ2V4cG9ydHMnKSB7XG4gICAgICAgICAgY29uc3QgaXNJblNjb3BlID0gY29udGV4dC5nZXRTY29wZSgpXG4gICAgICAgICAgICAudmFyaWFibGVzXG4gICAgICAgICAgICAuc29tZSh2YXJpYWJsZSA9PiB2YXJpYWJsZS5uYW1lID09PSAnZXhwb3J0cycpO1xuICAgICAgICAgIGlmICghIGlzSW5TY29wZSkge1xuICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoeyBub2RlLCBtZXNzYWdlOiBFWFBPUlRfTUVTU0FHRSB9KTtcbiAgICAgICAgICB9XG4gICAgICAgIH1cblxuICAgICAgfSxcbiAgICAgICdDYWxsRXhwcmVzc2lvbic6IGZ1bmN0aW9uIChjYWxsKSB7XG4gICAgICAgIGlmICghdmFsaWRhdGVTY29wZShjb250ZXh0LmdldFNjb3BlKCkpKSByZXR1cm47XG5cbiAgICAgICAgaWYgKGNhbGwuY2FsbGVlLnR5cGUgIT09ICdJZGVudGlmaWVyJykgcmV0dXJuO1xuICAgICAgICBpZiAoY2FsbC5jYWxsZWUubmFtZSAhPT0gJ3JlcXVpcmUnKSByZXR1cm47XG5cbiAgICAgICAgaWYgKGNhbGwuYXJndW1lbnRzLmxlbmd0aCAhPT0gMSkgcmV0dXJuO1xuICAgICAgICBpZiAoIWlzTGl0ZXJhbFN0cmluZyhjYWxsLmFyZ3VtZW50c1swXSkpIHJldHVybjtcblxuICAgICAgICBpZiAoYWxsb3dSZXF1aXJlKGNhbGwsIG9wdGlvbnMpKSByZXR1cm47XG5cbiAgICAgICAgaWYgKGFsbG93Q29uZGl0aW9uYWxSZXF1aXJlKGNhbGwsIG9wdGlvbnMpICYmIGlzQ29uZGl0aW9uYWwoY2FsbC5wYXJlbnQpKSByZXR1cm47XG5cbiAgICAgICAgLy8ga2VlcGluZyBpdCBzaW1wbGU6IGFsbCAxLXN0cmluZy1hcmcgYHJlcXVpcmVgIGNhbGxzIGFyZSByZXBvcnRlZFxuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbm9kZTogY2FsbC5jYWxsZWUsXG4gICAgICAgICAgbWVzc2FnZTogSU1QT1JUX01FU1NBR0UsXG4gICAgICAgIH0pO1xuICAgICAgfSxcbiAgICB9O1xuXG4gIH0sXG59O1xuIl19