'use strict';var _path = require('path');var _path2 = _interopRequireDefault(_path);
var _minimatch = require('minimatch');var _minimatch2 = _interopRequireDefault(_minimatch);

var _staticRequire = require('../core/staticRequire');var _staticRequire2 = _interopRequireDefault(_staticRequire);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function report(context, node) {
  context.report({
    node: node,
    message: 'Imported module should be assigned' });

}

function testIsAllow(globs, filename, source) {
  if (!Array.isArray(globs)) {
    return false; // default doesn't allow any patterns
  }

  var filePath = void 0;

  if (source[0] !== '.' && source[0] !== '/') {// a node module
    filePath = source;
  } else {
    filePath = _path2['default'].resolve(_path2['default'].dirname(filename), source); // get source absolute path
  }

  return globs.find(function (glob) {return (
      (0, _minimatch2['default'])(filePath, glob) ||
      (0, _minimatch2['default'])(filePath, _path2['default'].join(process.cwd(), glob)));}) !==
  undefined;
}

function create(context) {
  var options = context.options[0] || {};
  var filename = context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename();
  var isAllow = function isAllow(source) {return testIsAllow(options.allow, filename, source);};

  return {
    ImportDeclaration: function () {function ImportDeclaration(node) {
        if (node.specifiers.length === 0 && !isAllow(node.source.value)) {
          report(context, node);
        }
      }return ImportDeclaration;}(),
    ExpressionStatement: function () {function ExpressionStatement(node) {
        if (node.expression.type === 'CallExpression' &&
        (0, _staticRequire2['default'])(node.expression) &&
        !isAllow(node.expression.arguments[0].value)) {
          report(context, node.expression);
        }
      }return ExpressionStatement;}() };

}

module.exports = {
  create: create,
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-unassigned-import') },

    schema: [
    {
      'type': 'object',
      'properties': {
        'devDependencies': { 'type': ['boolean', 'array'] },
        'optionalDependencies': { 'type': ['boolean', 'array'] },
        'peerDependencies': { 'type': ['boolean', 'array'] },
        'allow': {
          'type': 'array',
          'items': {
            'type': 'string' } } },



      'additionalProperties': false }] } };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby11bmFzc2lnbmVkLWltcG9ydC5qcyJdLCJuYW1lcyI6WyJyZXBvcnQiLCJjb250ZXh0Iiwibm9kZSIsIm1lc3NhZ2UiLCJ0ZXN0SXNBbGxvdyIsImdsb2JzIiwiZmlsZW5hbWUiLCJzb3VyY2UiLCJBcnJheSIsImlzQXJyYXkiLCJmaWxlUGF0aCIsInBhdGgiLCJyZXNvbHZlIiwiZGlybmFtZSIsImZpbmQiLCJnbG9iIiwiam9pbiIsInByb2Nlc3MiLCJjd2QiLCJ1bmRlZmluZWQiLCJjcmVhdGUiLCJvcHRpb25zIiwiZ2V0UGh5c2ljYWxGaWxlbmFtZSIsImdldEZpbGVuYW1lIiwiaXNBbGxvdyIsImFsbG93IiwiSW1wb3J0RGVjbGFyYXRpb24iLCJzcGVjaWZpZXJzIiwibGVuZ3RoIiwidmFsdWUiLCJFeHByZXNzaW9uU3RhdGVtZW50IiwiZXhwcmVzc2lvbiIsInR5cGUiLCJhcmd1bWVudHMiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiXSwibWFwcGluZ3MiOiJhQUFBLDRCO0FBQ0Esc0M7O0FBRUEsc0Q7QUFDQSxxQzs7QUFFQSxTQUFTQSxNQUFULENBQWdCQyxPQUFoQixFQUF5QkMsSUFBekIsRUFBK0I7QUFDN0JELFVBQVFELE1BQVIsQ0FBZTtBQUNiRSxjQURhO0FBRWJDLGFBQVMsb0NBRkksRUFBZjs7QUFJRDs7QUFFRCxTQUFTQyxXQUFULENBQXFCQyxLQUFyQixFQUE0QkMsUUFBNUIsRUFBc0NDLE1BQXRDLEVBQThDO0FBQzVDLE1BQUksQ0FBQ0MsTUFBTUMsT0FBTixDQUFjSixLQUFkLENBQUwsRUFBMkI7QUFDekIsV0FBTyxLQUFQLENBRHlCLENBQ1g7QUFDZjs7QUFFRCxNQUFJSyxpQkFBSjs7QUFFQSxNQUFJSCxPQUFPLENBQVAsTUFBYyxHQUFkLElBQXFCQSxPQUFPLENBQVAsTUFBYyxHQUF2QyxFQUE0QyxDQUFFO0FBQzVDRyxlQUFXSCxNQUFYO0FBQ0QsR0FGRCxNQUVPO0FBQ0xHLGVBQVdDLGtCQUFLQyxPQUFMLENBQWFELGtCQUFLRSxPQUFMLENBQWFQLFFBQWIsQ0FBYixFQUFxQ0MsTUFBckMsQ0FBWCxDQURLLENBQ29EO0FBQzFEOztBQUVELFNBQU9GLE1BQU1TLElBQU4sQ0FBVztBQUNoQixrQ0FBVUosUUFBVixFQUFvQkssSUFBcEI7QUFDQSxrQ0FBVUwsUUFBVixFQUFvQkMsa0JBQUtLLElBQUwsQ0FBVUMsUUFBUUMsR0FBUixFQUFWLEVBQXlCSCxJQUF6QixDQUFwQixDQUZnQixHQUFYO0FBR0FJLFdBSFA7QUFJRDs7QUFFRCxTQUFTQyxNQUFULENBQWdCbkIsT0FBaEIsRUFBeUI7QUFDdkIsTUFBTW9CLFVBQVVwQixRQUFRb0IsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUF0QztBQUNBLE1BQU1mLFdBQVdMLFFBQVFxQixtQkFBUixHQUE4QnJCLFFBQVFxQixtQkFBUixFQUE5QixHQUE4RHJCLFFBQVFzQixXQUFSLEVBQS9FO0FBQ0EsTUFBTUMsVUFBVSxTQUFWQSxPQUFVLGlCQUFVcEIsWUFBWWlCLFFBQVFJLEtBQXBCLEVBQTJCbkIsUUFBM0IsRUFBcUNDLE1BQXJDLENBQVYsRUFBaEI7O0FBRUEsU0FBTztBQUNMbUIscUJBREssMENBQ2F4QixJQURiLEVBQ21CO0FBQ3RCLFlBQUlBLEtBQUt5QixVQUFMLENBQWdCQyxNQUFoQixLQUEyQixDQUEzQixJQUFnQyxDQUFDSixRQUFRdEIsS0FBS0ssTUFBTCxDQUFZc0IsS0FBcEIsQ0FBckMsRUFBaUU7QUFDL0Q3QixpQkFBT0MsT0FBUCxFQUFnQkMsSUFBaEI7QUFDRDtBQUNGLE9BTEk7QUFNTDRCLHVCQU5LLDRDQU1lNUIsSUFOZixFQU1xQjtBQUN4QixZQUFJQSxLQUFLNkIsVUFBTCxDQUFnQkMsSUFBaEIsS0FBeUIsZ0JBQXpCO0FBQ0Ysd0NBQWdCOUIsS0FBSzZCLFVBQXJCLENBREU7QUFFRixTQUFDUCxRQUFRdEIsS0FBSzZCLFVBQUwsQ0FBZ0JFLFNBQWhCLENBQTBCLENBQTFCLEVBQTZCSixLQUFyQyxDQUZILEVBRWdEO0FBQzlDN0IsaUJBQU9DLE9BQVAsRUFBZ0JDLEtBQUs2QixVQUFyQjtBQUNEO0FBQ0YsT0FaSSxnQ0FBUDs7QUFjRDs7QUFFREcsT0FBT0MsT0FBUCxHQUFpQjtBQUNmZixnQkFEZTtBQUVmZ0IsUUFBTTtBQUNKSixVQUFNLFlBREY7QUFFSkssVUFBTTtBQUNKQyxXQUFLLDBCQUFRLHNCQUFSLENBREQsRUFGRjs7QUFLSkMsWUFBUTtBQUNOO0FBQ0UsY0FBUSxRQURWO0FBRUUsb0JBQWM7QUFDWiwyQkFBbUIsRUFBRSxRQUFRLENBQUMsU0FBRCxFQUFZLE9BQVosQ0FBVixFQURQO0FBRVosZ0NBQXdCLEVBQUUsUUFBUSxDQUFDLFNBQUQsRUFBWSxPQUFaLENBQVYsRUFGWjtBQUdaLDRCQUFvQixFQUFFLFFBQVEsQ0FBQyxTQUFELEVBQVksT0FBWixDQUFWLEVBSFI7QUFJWixpQkFBUztBQUNQLGtCQUFRLE9BREQ7QUFFUCxtQkFBUztBQUNQLG9CQUFRLFFBREQsRUFGRixFQUpHLEVBRmhCOzs7O0FBYUUsOEJBQXdCLEtBYjFCLEVBRE0sQ0FMSixFQUZTLEVBQWpCIiwiZmlsZSI6Im5vLXVuYXNzaWduZWQtaW1wb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHBhdGggZnJvbSAncGF0aCc7XG5pbXBvcnQgbWluaW1hdGNoIGZyb20gJ21pbmltYXRjaCc7XG5cbmltcG9ydCBpc1N0YXRpY1JlcXVpcmUgZnJvbSAnLi4vY29yZS9zdGF0aWNSZXF1aXJlJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5mdW5jdGlvbiByZXBvcnQoY29udGV4dCwgbm9kZSkge1xuICBjb250ZXh0LnJlcG9ydCh7XG4gICAgbm9kZSxcbiAgICBtZXNzYWdlOiAnSW1wb3J0ZWQgbW9kdWxlIHNob3VsZCBiZSBhc3NpZ25lZCcsXG4gIH0pO1xufVxuXG5mdW5jdGlvbiB0ZXN0SXNBbGxvdyhnbG9icywgZmlsZW5hbWUsIHNvdXJjZSkge1xuICBpZiAoIUFycmF5LmlzQXJyYXkoZ2xvYnMpKSB7XG4gICAgcmV0dXJuIGZhbHNlOyAvLyBkZWZhdWx0IGRvZXNuJ3QgYWxsb3cgYW55IHBhdHRlcm5zXG4gIH1cblxuICBsZXQgZmlsZVBhdGg7XG5cbiAgaWYgKHNvdXJjZVswXSAhPT0gJy4nICYmIHNvdXJjZVswXSAhPT0gJy8nKSB7IC8vIGEgbm9kZSBtb2R1bGVcbiAgICBmaWxlUGF0aCA9IHNvdXJjZTtcbiAgfSBlbHNlIHtcbiAgICBmaWxlUGF0aCA9IHBhdGgucmVzb2x2ZShwYXRoLmRpcm5hbWUoZmlsZW5hbWUpLCBzb3VyY2UpOyAvLyBnZXQgc291cmNlIGFic29sdXRlIHBhdGhcbiAgfVxuXG4gIHJldHVybiBnbG9icy5maW5kKGdsb2IgPT4gKFxuICAgIG1pbmltYXRjaChmaWxlUGF0aCwgZ2xvYikgfHxcbiAgICBtaW5pbWF0Y2goZmlsZVBhdGgsIHBhdGguam9pbihwcm9jZXNzLmN3ZCgpLCBnbG9iKSlcbiAgKSkgIT09IHVuZGVmaW5lZDtcbn1cblxuZnVuY3Rpb24gY3JlYXRlKGNvbnRleHQpIHtcbiAgY29uc3Qgb3B0aW9ucyA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7fTtcbiAgY29uc3QgZmlsZW5hbWUgPSBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUgPyBjb250ZXh0LmdldFBoeXNpY2FsRmlsZW5hbWUoKSA6IGNvbnRleHQuZ2V0RmlsZW5hbWUoKTtcbiAgY29uc3QgaXNBbGxvdyA9IHNvdXJjZSA9PiB0ZXN0SXNBbGxvdyhvcHRpb25zLmFsbG93LCBmaWxlbmFtZSwgc291cmNlKTtcblxuICByZXR1cm4ge1xuICAgIEltcG9ydERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgIGlmIChub2RlLnNwZWNpZmllcnMubGVuZ3RoID09PSAwICYmICFpc0FsbG93KG5vZGUuc291cmNlLnZhbHVlKSkge1xuICAgICAgICByZXBvcnQoY29udGV4dCwgbm9kZSk7XG4gICAgICB9XG4gICAgfSxcbiAgICBFeHByZXNzaW9uU3RhdGVtZW50KG5vZGUpIHtcbiAgICAgIGlmIChub2RlLmV4cHJlc3Npb24udHlwZSA9PT0gJ0NhbGxFeHByZXNzaW9uJyAmJlxuICAgICAgICBpc1N0YXRpY1JlcXVpcmUobm9kZS5leHByZXNzaW9uKSAmJlxuICAgICAgICAhaXNBbGxvdyhub2RlLmV4cHJlc3Npb24uYXJndW1lbnRzWzBdLnZhbHVlKSkge1xuICAgICAgICByZXBvcnQoY29udGV4dCwgbm9kZS5leHByZXNzaW9uKTtcbiAgICAgIH1cbiAgICB9LFxuICB9O1xufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgY3JlYXRlLFxuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tdW5hc3NpZ25lZC1pbXBvcnQnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICAndHlwZSc6ICdvYmplY3QnLFxuICAgICAgICAncHJvcGVydGllcyc6IHtcbiAgICAgICAgICAnZGV2RGVwZW5kZW5jaWVzJzogeyAndHlwZSc6IFsnYm9vbGVhbicsICdhcnJheSddIH0sXG4gICAgICAgICAgJ29wdGlvbmFsRGVwZW5kZW5jaWVzJzogeyAndHlwZSc6IFsnYm9vbGVhbicsICdhcnJheSddIH0sXG4gICAgICAgICAgJ3BlZXJEZXBlbmRlbmNpZXMnOiB7ICd0eXBlJzogWydib29sZWFuJywgJ2FycmF5J10gfSxcbiAgICAgICAgICAnYWxsb3cnOiB7XG4gICAgICAgICAgICAndHlwZSc6ICdhcnJheScsXG4gICAgICAgICAgICAnaXRlbXMnOiB7XG4gICAgICAgICAgICAgICd0eXBlJzogJ3N0cmluZycsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgICdhZGRpdGlvbmFsUHJvcGVydGllcyc6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxufTtcbiJdfQ==