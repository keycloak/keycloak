'use strict';var _importType = require('../core/importType');var _importType2 = _interopRequireDefault(_importType);
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function reportIfMissing(context, node, allowed, name) {
  if (allowed.indexOf(name) === -1 && (0, _importType2['default'])(name, context) === 'builtin') {
    context.report(node, 'Do not import Node.js builtin module "' + name + '"');
  }
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-nodejs-modules') },

    schema: [
    {
      type: 'object',
      properties: {
        allow: {
          type: 'array',
          uniqueItems: true,
          items: {
            type: 'string' } } },



      additionalProperties: false }] },




  create: function () {function create(context) {
      var options = context.options[0] || {};
      var allowed = options.allow || [];

      return (0, _moduleVisitor2['default'])(function (source, node) {
        reportIfMissing(context, node, allowed, source.value);
      }, { commonjs: true });
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1ub2RlanMtbW9kdWxlcy5qcyJdLCJuYW1lcyI6WyJyZXBvcnRJZk1pc3NpbmciLCJjb250ZXh0Iiwibm9kZSIsImFsbG93ZWQiLCJuYW1lIiwiaW5kZXhPZiIsInJlcG9ydCIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJwcm9wZXJ0aWVzIiwiYWxsb3ciLCJ1bmlxdWVJdGVtcyIsIml0ZW1zIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJjcmVhdGUiLCJvcHRpb25zIiwic291cmNlIiwidmFsdWUiLCJjb21tb25qcyJdLCJtYXBwaW5ncyI6ImFBQUEsZ0Q7QUFDQSxrRTtBQUNBLHFDOztBQUVBLFNBQVNBLGVBQVQsQ0FBeUJDLE9BQXpCLEVBQWtDQyxJQUFsQyxFQUF3Q0MsT0FBeEMsRUFBaURDLElBQWpELEVBQXVEO0FBQ3JELE1BQUlELFFBQVFFLE9BQVIsQ0FBZ0JELElBQWhCLE1BQTBCLENBQUMsQ0FBM0IsSUFBZ0MsNkJBQVdBLElBQVgsRUFBaUJILE9BQWpCLE1BQThCLFNBQWxFLEVBQTZFO0FBQzNFQSxZQUFRSyxNQUFSLENBQWVKLElBQWYsRUFBcUIsMkNBQTJDRSxJQUEzQyxHQUFrRCxHQUF2RTtBQUNEO0FBQ0Y7O0FBRURHLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLG1CQUFSLENBREQsRUFGRjs7QUFLSkMsWUFBUTtBQUNOO0FBQ0VILFlBQU0sUUFEUjtBQUVFSSxrQkFBWTtBQUNWQyxlQUFPO0FBQ0xMLGdCQUFNLE9BREQ7QUFFTE0sdUJBQWEsSUFGUjtBQUdMQyxpQkFBTztBQUNMUCxrQkFBTSxRQURELEVBSEYsRUFERyxFQUZkOzs7O0FBV0VRLDRCQUFzQixLQVh4QixFQURNLENBTEosRUFEUzs7Ozs7QUF1QmZDLFFBdkJlLCtCQXVCUmxCLE9BdkJRLEVBdUJDO0FBQ2QsVUFBTW1CLFVBQVVuQixRQUFRbUIsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUF0QztBQUNBLFVBQU1qQixVQUFVaUIsUUFBUUwsS0FBUixJQUFpQixFQUFqQzs7QUFFQSxhQUFPLGdDQUFjLFVBQUNNLE1BQUQsRUFBU25CLElBQVQsRUFBa0I7QUFDckNGLHdCQUFnQkMsT0FBaEIsRUFBeUJDLElBQXpCLEVBQStCQyxPQUEvQixFQUF3Q2tCLE9BQU9DLEtBQS9DO0FBQ0QsT0FGTSxFQUVKLEVBQUVDLFVBQVUsSUFBWixFQUZJLENBQVA7QUFHRCxLQTlCYyxtQkFBakIiLCJmaWxlIjoibm8tbm9kZWpzLW1vZHVsZXMuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgaW1wb3J0VHlwZSBmcm9tICcuLi9jb3JlL2ltcG9ydFR5cGUnO1xuaW1wb3J0IG1vZHVsZVZpc2l0b3IgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9tb2R1bGVWaXNpdG9yJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5mdW5jdGlvbiByZXBvcnRJZk1pc3NpbmcoY29udGV4dCwgbm9kZSwgYWxsb3dlZCwgbmFtZSkge1xuICBpZiAoYWxsb3dlZC5pbmRleE9mKG5hbWUpID09PSAtMSAmJiBpbXBvcnRUeXBlKG5hbWUsIGNvbnRleHQpID09PSAnYnVpbHRpbicpIHtcbiAgICBjb250ZXh0LnJlcG9ydChub2RlLCAnRG8gbm90IGltcG9ydCBOb2RlLmpzIGJ1aWx0aW4gbW9kdWxlIFwiJyArIG5hbWUgKyAnXCInKTtcbiAgfVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLW5vZGVqcy1tb2R1bGVzJyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtcbiAgICAgIHtcbiAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICBhbGxvdzoge1xuICAgICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICAgIHVuaXF1ZUl0ZW1zOiB0cnVlLFxuICAgICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgICAgdHlwZTogJ3N0cmluZycsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge307XG4gICAgY29uc3QgYWxsb3dlZCA9IG9wdGlvbnMuYWxsb3cgfHwgW107XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcigoc291cmNlLCBub2RlKSA9PiB7XG4gICAgICByZXBvcnRJZk1pc3NpbmcoY29udGV4dCwgbm9kZSwgYWxsb3dlZCwgc291cmNlLnZhbHVlKTtcbiAgICB9LCB7IGNvbW1vbmpzOiB0cnVlIH0pO1xuICB9LFxufTtcbiJdfQ==