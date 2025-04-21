'use strict';var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var DEFAULT_MAX = 10;
var DEFAULT_IGNORE_TYPE_IMPORTS = false;
var TYPE_IMPORT = 'type';

var countDependencies = function countDependencies(dependencies, lastNode, context) {var _ref =
  context.options[0] || { max: DEFAULT_MAX },max = _ref.max;

  if (dependencies.size > max) {
    context.report(lastNode, 'Maximum number of dependencies (' + String(max) + ') exceeded.');
  }
};

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('max-dependencies') },


    schema: [
    {
      'type': 'object',
      'properties': {
        'max': { 'type': 'number' },
        'ignoreTypeImports': { 'type': 'boolean' } },

      'additionalProperties': false }] },




  create: function () {function create(context) {var _ref2 =


      context.options[0] || {},_ref2$ignoreTypeImpor = _ref2.ignoreTypeImports,ignoreTypeImports = _ref2$ignoreTypeImpor === undefined ? DEFAULT_IGNORE_TYPE_IMPORTS : _ref2$ignoreTypeImpor;

      var dependencies = new Set(); // keep track of dependencies
      var lastNode = void 0; // keep track of the last node to report on

      return Object.assign({
        'Program:exit': function () {function ProgramExit() {
            countDependencies(dependencies, lastNode, context);
          }return ProgramExit;}() },
      (0, _moduleVisitor2['default'])(function (source, _ref3) {var importKind = _ref3.importKind;
        if (importKind !== TYPE_IMPORT || !ignoreTypeImports) {
          dependencies.add(source.value);
        }
        lastNode = source;
      }, { commonjs: true }));
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9tYXgtZGVwZW5kZW5jaWVzLmpzIl0sIm5hbWVzIjpbIkRFRkFVTFRfTUFYIiwiREVGQVVMVF9JR05PUkVfVFlQRV9JTVBPUlRTIiwiVFlQRV9JTVBPUlQiLCJjb3VudERlcGVuZGVuY2llcyIsImRlcGVuZGVuY2llcyIsImxhc3ROb2RlIiwiY29udGV4dCIsIm9wdGlvbnMiLCJtYXgiLCJzaXplIiwicmVwb3J0IiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImlnbm9yZVR5cGVJbXBvcnRzIiwiU2V0IiwiT2JqZWN0IiwiYXNzaWduIiwic291cmNlIiwiaW1wb3J0S2luZCIsImFkZCIsInZhbHVlIiwiY29tbW9uanMiXSwibWFwcGluZ3MiOiJhQUFBLGtFO0FBQ0EscUM7O0FBRUEsSUFBTUEsY0FBYyxFQUFwQjtBQUNBLElBQU1DLDhCQUE4QixLQUFwQztBQUNBLElBQU1DLGNBQWMsTUFBcEI7O0FBRUEsSUFBTUMsb0JBQW9CLFNBQXBCQSxpQkFBb0IsQ0FBQ0MsWUFBRCxFQUFlQyxRQUFmLEVBQXlCQyxPQUF6QixFQUFxQztBQUM3Q0EsVUFBUUMsT0FBUixDQUFnQixDQUFoQixLQUFzQixFQUFFQyxLQUFLUixXQUFQLEVBRHVCLENBQ3JEUSxHQURxRCxRQUNyREEsR0FEcUQ7O0FBRzdELE1BQUlKLGFBQWFLLElBQWIsR0FBb0JELEdBQXhCLEVBQTZCO0FBQzNCRixZQUFRSSxNQUFSLENBQWVMLFFBQWYsOENBQTRERyxHQUE1RDtBQUNEO0FBQ0YsQ0FORDs7QUFRQUcsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsa0JBQVIsQ0FERCxFQUZGOzs7QUFNSkMsWUFBUTtBQUNOO0FBQ0UsY0FBUSxRQURWO0FBRUUsb0JBQWM7QUFDWixlQUFPLEVBQUUsUUFBUSxRQUFWLEVBREs7QUFFWiw2QkFBcUIsRUFBRSxRQUFRLFNBQVYsRUFGVCxFQUZoQjs7QUFNRSw4QkFBd0IsS0FOMUIsRUFETSxDQU5KLEVBRFM7Ozs7O0FBbUJmQyx1QkFBUSx5QkFBVzs7O0FBR2JaLGNBQVFDLE9BQVIsQ0FBZ0IsQ0FBaEIsS0FBc0IsRUFIVCwrQkFFZlksaUJBRmUsQ0FFZkEsaUJBRmUseUNBRUtsQiwyQkFGTDs7QUFLakIsVUFBTUcsZUFBZSxJQUFJZ0IsR0FBSixFQUFyQixDQUxpQixDQUtlO0FBQ2hDLFVBQUlmLGlCQUFKLENBTmlCLENBTUg7O0FBRWQsYUFBT2dCLE9BQU9DLE1BQVAsQ0FBYztBQUNuQixxQ0FBZ0IsdUJBQVk7QUFDMUJuQiw4QkFBa0JDLFlBQWxCLEVBQWdDQyxRQUFoQyxFQUEwQ0MsT0FBMUM7QUFDRCxXQUZELHNCQURtQixFQUFkO0FBSUosc0NBQWMsVUFBQ2lCLE1BQUQsU0FBNEIsS0FBakJDLFVBQWlCLFNBQWpCQSxVQUFpQjtBQUMzQyxZQUFJQSxlQUFldEIsV0FBZixJQUE4QixDQUFDaUIsaUJBQW5DLEVBQXNEO0FBQ3BEZix1QkFBYXFCLEdBQWIsQ0FBaUJGLE9BQU9HLEtBQXhCO0FBQ0Q7QUFDRHJCLG1CQUFXa0IsTUFBWDtBQUNELE9BTEUsRUFLQSxFQUFFSSxVQUFVLElBQVosRUFMQSxDQUpJLENBQVA7QUFVRCxLQWxCRCxpQkFuQmUsRUFBakIiLCJmaWxlIjoibWF4LWRlcGVuZGVuY2llcy5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBtb2R1bGVWaXNpdG9yIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvbW9kdWxlVmlzaXRvcic7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuY29uc3QgREVGQVVMVF9NQVggPSAxMDtcbmNvbnN0IERFRkFVTFRfSUdOT1JFX1RZUEVfSU1QT1JUUyA9IGZhbHNlO1xuY29uc3QgVFlQRV9JTVBPUlQgPSAndHlwZSc7XG5cbmNvbnN0IGNvdW50RGVwZW5kZW5jaWVzID0gKGRlcGVuZGVuY2llcywgbGFzdE5vZGUsIGNvbnRleHQpID0+IHtcbiAgY29uc3QgeyBtYXggfSA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7IG1heDogREVGQVVMVF9NQVggfTtcblxuICBpZiAoZGVwZW5kZW5jaWVzLnNpemUgPiBtYXgpIHtcbiAgICBjb250ZXh0LnJlcG9ydChsYXN0Tm9kZSwgYE1heGltdW0gbnVtYmVyIG9mIGRlcGVuZGVuY2llcyAoJHttYXh9KSBleGNlZWRlZC5gKTtcbiAgfVxufTtcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdtYXgtZGVwZW5kZW5jaWVzJyksXG4gICAgfSxcblxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICAndHlwZSc6ICdvYmplY3QnLFxuICAgICAgICAncHJvcGVydGllcyc6IHtcbiAgICAgICAgICAnbWF4JzogeyAndHlwZSc6ICdudW1iZXInIH0sXG4gICAgICAgICAgJ2lnbm9yZVR5cGVJbXBvcnRzJzogeyAndHlwZSc6ICdib29sZWFuJyB9LFxuICAgICAgICB9LFxuICAgICAgICAnYWRkaXRpb25hbFByb3BlcnRpZXMnOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGU6IGNvbnRleHQgPT4ge1xuICAgIGNvbnN0IHtcbiAgICAgIGlnbm9yZVR5cGVJbXBvcnRzID0gREVGQVVMVF9JR05PUkVfVFlQRV9JTVBPUlRTLFxuICAgIH0gPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge307XG5cbiAgICBjb25zdCBkZXBlbmRlbmNpZXMgPSBuZXcgU2V0KCk7IC8vIGtlZXAgdHJhY2sgb2YgZGVwZW5kZW5jaWVzXG4gICAgbGV0IGxhc3ROb2RlOyAvLyBrZWVwIHRyYWNrIG9mIHRoZSBsYXN0IG5vZGUgdG8gcmVwb3J0IG9uXG5cbiAgICByZXR1cm4gT2JqZWN0LmFzc2lnbih7XG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24gKCkge1xuICAgICAgICBjb3VudERlcGVuZGVuY2llcyhkZXBlbmRlbmNpZXMsIGxhc3ROb2RlLCBjb250ZXh0KTtcbiAgICAgIH0sXG4gICAgfSwgbW9kdWxlVmlzaXRvcigoc291cmNlLCB7IGltcG9ydEtpbmQgfSkgPT4ge1xuICAgICAgaWYgKGltcG9ydEtpbmQgIT09IFRZUEVfSU1QT1JUIHx8ICFpZ25vcmVUeXBlSW1wb3J0cykge1xuICAgICAgICBkZXBlbmRlbmNpZXMuYWRkKHNvdXJjZS52YWx1ZSk7XG4gICAgICB9XG4gICAgICBsYXN0Tm9kZSA9IHNvdXJjZTtcbiAgICB9LCB7IGNvbW1vbmpzOiB0cnVlIH0pKTtcbiAgfSxcbn07XG4iXX0=