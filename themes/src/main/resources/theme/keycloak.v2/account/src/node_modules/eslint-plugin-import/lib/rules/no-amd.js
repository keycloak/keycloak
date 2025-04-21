'use strict';




var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-amd') },

    schema: [] },


  create: function () {function create(context) {
      return {
        'CallExpression': function () {function CallExpression(node) {
            if (context.getScope().type !== 'module') return;

            if (node.callee.type !== 'Identifier') return;
            if (node.callee.name !== 'require' &&
            node.callee.name !== 'define') return;

            // todo: capture define((require, module, exports) => {}) form?
            if (node.arguments.length !== 2) return;

            var modules = node.arguments[0];
            if (modules.type !== 'ArrayExpression') return;

            // todo: check second arg type? (identifier or callback)

            context.report(node, 'Expected imports instead of AMD ' + String(node.callee.name) + '().');
          }return CallExpression;}() };


    }return create;}() }; /**
                           * @fileoverview Rule to prefer imports to AMD
                           * @author Jamund Ferguson
                           */
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1hbWQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJub2RlIiwiZ2V0U2NvcGUiLCJjYWxsZWUiLCJuYW1lIiwiYXJndW1lbnRzIiwibGVuZ3RoIiwibW9kdWxlcyIsInJlcG9ydCJdLCJtYXBwaW5ncyI6Ijs7Ozs7QUFLQSxxQzs7QUFFQTtBQUNBO0FBQ0E7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLFFBQVIsQ0FERCxFQUZGOztBQUtKQyxZQUFRLEVBTEosRUFEUzs7O0FBU2ZDLFFBVGUsK0JBU1JDLE9BVFEsRUFTQztBQUNkLGFBQU87QUFDTCx1Q0FBa0Isd0JBQVVDLElBQVYsRUFBZ0I7QUFDaEMsZ0JBQUlELFFBQVFFLFFBQVIsR0FBbUJQLElBQW5CLEtBQTRCLFFBQWhDLEVBQTBDOztBQUUxQyxnQkFBSU0sS0FBS0UsTUFBTCxDQUFZUixJQUFaLEtBQXFCLFlBQXpCLEVBQXVDO0FBQ3ZDLGdCQUFJTSxLQUFLRSxNQUFMLENBQVlDLElBQVosS0FBcUIsU0FBckI7QUFDQUgsaUJBQUtFLE1BQUwsQ0FBWUMsSUFBWixLQUFxQixRQUR6QixFQUNtQzs7QUFFbkM7QUFDQSxnQkFBSUgsS0FBS0ksU0FBTCxDQUFlQyxNQUFmLEtBQTBCLENBQTlCLEVBQWlDOztBQUVqQyxnQkFBTUMsVUFBVU4sS0FBS0ksU0FBTCxDQUFlLENBQWYsQ0FBaEI7QUFDQSxnQkFBSUUsUUFBUVosSUFBUixLQUFpQixpQkFBckIsRUFBd0M7O0FBRXhDOztBQUVBSyxvQkFBUVEsTUFBUixDQUFlUCxJQUFmLDhDQUF3REEsS0FBS0UsTUFBTCxDQUFZQyxJQUFwRTtBQUNELFdBaEJELHlCQURLLEVBQVA7OztBQW9CRCxLQTlCYyxtQkFBakIsQyxDQVhBIiwiZmlsZSI6Im5vLWFtZC5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVvdmVydmlldyBSdWxlIHRvIHByZWZlciBpbXBvcnRzIHRvIEFNRFxuICogQGF1dGhvciBKYW11bmQgRmVyZ3Vzb25cbiAqL1xuXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cbi8vIFJ1bGUgRGVmaW5pdGlvblxuLy8tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1hbWQnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW10sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICByZXR1cm4ge1xuICAgICAgJ0NhbGxFeHByZXNzaW9uJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgaWYgKGNvbnRleHQuZ2V0U2NvcGUoKS50eXBlICE9PSAnbW9kdWxlJykgcmV0dXJuO1xuXG4gICAgICAgIGlmIChub2RlLmNhbGxlZS50eXBlICE9PSAnSWRlbnRpZmllcicpIHJldHVybjtcbiAgICAgICAgaWYgKG5vZGUuY2FsbGVlLm5hbWUgIT09ICdyZXF1aXJlJyAmJlxuICAgICAgICAgICAgbm9kZS5jYWxsZWUubmFtZSAhPT0gJ2RlZmluZScpIHJldHVybjtcblxuICAgICAgICAvLyB0b2RvOiBjYXB0dXJlIGRlZmluZSgocmVxdWlyZSwgbW9kdWxlLCBleHBvcnRzKSA9PiB7fSkgZm9ybT9cbiAgICAgICAgaWYgKG5vZGUuYXJndW1lbnRzLmxlbmd0aCAhPT0gMikgcmV0dXJuO1xuXG4gICAgICAgIGNvbnN0IG1vZHVsZXMgPSBub2RlLmFyZ3VtZW50c1swXTtcbiAgICAgICAgaWYgKG1vZHVsZXMudHlwZSAhPT0gJ0FycmF5RXhwcmVzc2lvbicpIHJldHVybjtcblxuICAgICAgICAvLyB0b2RvOiBjaGVjayBzZWNvbmQgYXJnIHR5cGU/IChpZGVudGlmaWVyIG9yIGNhbGxiYWNrKVxuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KG5vZGUsIGBFeHBlY3RlZCBpbXBvcnRzIGluc3RlYWQgb2YgQU1EICR7bm9kZS5jYWxsZWUubmFtZX0oKS5gKTtcbiAgICAgIH0sXG4gICAgfTtcblxuICB9LFxufTtcbiJdfQ==