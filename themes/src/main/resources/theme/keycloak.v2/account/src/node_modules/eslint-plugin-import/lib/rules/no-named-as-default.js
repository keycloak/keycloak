'use strict';var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _importDeclaration = require('../importDeclaration');var _importDeclaration2 = _interopRequireDefault(_importDeclaration);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('no-named-as-default') },

    schema: [] },


  create: function () {function create(context) {
      function checkDefault(nameKey, defaultSpecifier) {
        // #566: default is a valid specifier
        if (defaultSpecifier[nameKey].name === 'default') return;

        var declaration = (0, _importDeclaration2['default'])(context);

        var imports = _ExportMap2['default'].get(declaration.source.value, context);
        if (imports == null) return;

        if (imports.errors.length) {
          imports.reportErrors(context, declaration);
          return;
        }

        if (imports.has('default') &&
        imports.has(defaultSpecifier[nameKey].name)) {

          context.report(defaultSpecifier,
          'Using exported name \'' + defaultSpecifier[nameKey].name +
          '\' as identifier for default export.');

        }
      }
      return {
        'ImportDefaultSpecifier': checkDefault.bind(null, 'local'),
        'ExportDefaultSpecifier': checkDefault.bind(null, 'exported') };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lZC1hcy1kZWZhdWx0LmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJzY2hlbWEiLCJjcmVhdGUiLCJjb250ZXh0IiwiY2hlY2tEZWZhdWx0IiwibmFtZUtleSIsImRlZmF1bHRTcGVjaWZpZXIiLCJuYW1lIiwiZGVjbGFyYXRpb24iLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwibGVuZ3RoIiwicmVwb3J0RXJyb3JzIiwiaGFzIiwicmVwb3J0IiwiYmluZCJdLCJtYXBwaW5ncyI6ImFBQUEseUM7QUFDQSx5RDtBQUNBLHFDOztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSywwQkFBUSxxQkFBUixDQURELEVBRkY7O0FBS0pDLFlBQVEsRUFMSixFQURTOzs7QUFTZkMsUUFUZSwrQkFTUkMsT0FUUSxFQVNDO0FBQ2QsZUFBU0MsWUFBVCxDQUFzQkMsT0FBdEIsRUFBK0JDLGdCQUEvQixFQUFpRDtBQUMvQztBQUNBLFlBQUlBLGlCQUFpQkQsT0FBakIsRUFBMEJFLElBQTFCLEtBQW1DLFNBQXZDLEVBQWtEOztBQUVsRCxZQUFNQyxjQUFjLG9DQUFrQkwsT0FBbEIsQ0FBcEI7O0FBRUEsWUFBTU0sVUFBVUMsdUJBQVFDLEdBQVIsQ0FBWUgsWUFBWUksTUFBWixDQUFtQkMsS0FBL0IsRUFBc0NWLE9BQXRDLENBQWhCO0FBQ0EsWUFBSU0sV0FBVyxJQUFmLEVBQXFCOztBQUVyQixZQUFJQSxRQUFRSyxNQUFSLENBQWVDLE1BQW5CLEVBQTJCO0FBQ3pCTixrQkFBUU8sWUFBUixDQUFxQmIsT0FBckIsRUFBOEJLLFdBQTlCO0FBQ0E7QUFDRDs7QUFFRCxZQUFJQyxRQUFRUSxHQUFSLENBQVksU0FBWjtBQUNBUixnQkFBUVEsR0FBUixDQUFZWCxpQkFBaUJELE9BQWpCLEVBQTBCRSxJQUF0QyxDQURKLEVBQ2lEOztBQUUvQ0osa0JBQVFlLE1BQVIsQ0FBZVosZ0JBQWY7QUFDRSxxQ0FBMkJBLGlCQUFpQkQsT0FBakIsRUFBMEJFLElBQXJEO0FBQ0EsZ0RBRkY7O0FBSUQ7QUFDRjtBQUNELGFBQU87QUFDTCxrQ0FBMEJILGFBQWFlLElBQWIsQ0FBa0IsSUFBbEIsRUFBd0IsT0FBeEIsQ0FEckI7QUFFTCxrQ0FBMEJmLGFBQWFlLElBQWIsQ0FBa0IsSUFBbEIsRUFBd0IsVUFBeEIsQ0FGckIsRUFBUDs7QUFJRCxLQXJDYyxtQkFBakIiLCJmaWxlIjoibm8tbmFtZWQtYXMtZGVmYXVsdC5qcyIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCc7XG5pbXBvcnQgaW1wb3J0RGVjbGFyYXRpb24gZnJvbSAnLi4vaW1wb3J0RGVjbGFyYXRpb24nO1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3Byb2JsZW0nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbmFtZWQtYXMtZGVmYXVsdCcpLFxuICAgIH0sXG4gICAgc2NoZW1hOiBbXSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIGZ1bmN0aW9uIGNoZWNrRGVmYXVsdChuYW1lS2V5LCBkZWZhdWx0U3BlY2lmaWVyKSB7XG4gICAgICAvLyAjNTY2OiBkZWZhdWx0IGlzIGEgdmFsaWQgc3BlY2lmaWVyXG4gICAgICBpZiAoZGVmYXVsdFNwZWNpZmllcltuYW1lS2V5XS5uYW1lID09PSAnZGVmYXVsdCcpIHJldHVybjtcblxuICAgICAgY29uc3QgZGVjbGFyYXRpb24gPSBpbXBvcnREZWNsYXJhdGlvbihjb250ZXh0KTtcblxuICAgICAgY29uc3QgaW1wb3J0cyA9IEV4cG9ydHMuZ2V0KGRlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZSwgY29udGV4dCk7XG4gICAgICBpZiAoaW1wb3J0cyA9PSBudWxsKSByZXR1cm47XG5cbiAgICAgIGlmIChpbXBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgZGVjbGFyYXRpb24pO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGlmIChpbXBvcnRzLmhhcygnZGVmYXVsdCcpICYmXG4gICAgICAgICAgaW1wb3J0cy5oYXMoZGVmYXVsdFNwZWNpZmllcltuYW1lS2V5XS5uYW1lKSkge1xuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KGRlZmF1bHRTcGVjaWZpZXIsXG4gICAgICAgICAgJ1VzaW5nIGV4cG9ydGVkIG5hbWUgXFwnJyArIGRlZmF1bHRTcGVjaWZpZXJbbmFtZUtleV0ubmFtZSArXG4gICAgICAgICAgJ1xcJyBhcyBpZGVudGlmaWVyIGZvciBkZWZhdWx0IGV4cG9ydC4nKTtcblxuICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInOiBjaGVja0RlZmF1bHQuYmluZChudWxsLCAnbG9jYWwnKSxcbiAgICAgICdFeHBvcnREZWZhdWx0U3BlY2lmaWVyJzogY2hlY2tEZWZhdWx0LmJpbmQobnVsbCwgJ2V4cG9ydGVkJyksXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=