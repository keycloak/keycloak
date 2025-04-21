'use strict';





var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _importDeclaration = require('../importDeclaration');var _importDeclaration2 = _interopRequireDefault(_importDeclaration);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-named-as-default-member') },

    schema: [] },


  create: function () {function create(context) {

      var fileImports = new Map();
      var allPropertyLookups = new Map();

      function handleImportDefault(node) {
        var declaration = (0, _importDeclaration2['default'])(context);
        var exportMap = _ExportMap2['default'].get(declaration.source.value, context);
        if (exportMap == null) return;

        if (exportMap.errors.length) {
          exportMap.reportErrors(context, declaration);
          return;
        }

        fileImports.set(node.local.name, {
          exportMap: exportMap,
          sourcePath: declaration.source.value });

      }

      function storePropertyLookup(objectName, propName, node) {
        var lookups = allPropertyLookups.get(objectName) || [];
        lookups.push({ node: node, propName: propName });
        allPropertyLookups.set(objectName, lookups);
      }

      function handlePropLookup(node) {
        var objectName = node.object.name;
        var propName = node.property.name;
        storePropertyLookup(objectName, propName, node);
      }

      function handleDestructuringAssignment(node) {
        var isDestructure =
        node.id.type === 'ObjectPattern' &&
        node.init != null &&
        node.init.type === 'Identifier';

        if (!isDestructure) return;

        var objectName = node.init.name;var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
          for (var _iterator = node.id.properties[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var _ref = _step.value;var key = _ref.key;
            if (key == null) continue; // true for rest properties
            storePropertyLookup(objectName, key.name, key);
          }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
      }

      function handleProgramExit() {
        allPropertyLookups.forEach(function (lookups, objectName) {
          var fileImport = fileImports.get(objectName);
          if (fileImport == null) return;var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {

            for (var _iterator2 = lookups[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var _ref2 = _step2.value;var propName = _ref2.propName,node = _ref2.node;
              // the default import can have a "default" property
              if (propName === 'default') continue;
              if (!fileImport.exportMap.namespace.has(propName)) continue;

              context.report({
                node: node,
                message:
                'Caution: `' + String(objectName) + '` also has a named export ' + ('`' + String(
                propName) + '`. Check if you meant to write ') + ('`import {' + String(
                propName) + '} from \'' + String(fileImport.sourcePath) + '\'` ') +
                'instead.' });


            }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
        });
      }

      return {
        'ImportDefaultSpecifier': handleImportDefault,
        'MemberExpression': handlePropLookup,
        'VariableDeclarator': handleDestructuringAssignment,
        'Program:exit': handleProgramExit };

    }return create;}() }; /**
                           * @fileoverview Rule to warn about potentially confused use of name exports
                           * @author Desmond Brand
                           * @copyright 2016 Desmond Brand. All rights reserved.
                           * See LICENSE in root directory for full license.
                           */
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1uYW1lZC1hcy1kZWZhdWx0LW1lbWJlci5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsImZpbGVJbXBvcnRzIiwiTWFwIiwiYWxsUHJvcGVydHlMb29rdXBzIiwiaGFuZGxlSW1wb3J0RGVmYXVsdCIsIm5vZGUiLCJkZWNsYXJhdGlvbiIsImV4cG9ydE1hcCIsIkV4cG9ydHMiLCJnZXQiLCJzb3VyY2UiLCJ2YWx1ZSIsImVycm9ycyIsImxlbmd0aCIsInJlcG9ydEVycm9ycyIsInNldCIsImxvY2FsIiwibmFtZSIsInNvdXJjZVBhdGgiLCJzdG9yZVByb3BlcnR5TG9va3VwIiwib2JqZWN0TmFtZSIsInByb3BOYW1lIiwibG9va3VwcyIsInB1c2giLCJoYW5kbGVQcm9wTG9va3VwIiwib2JqZWN0IiwicHJvcGVydHkiLCJoYW5kbGVEZXN0cnVjdHVyaW5nQXNzaWdubWVudCIsImlzRGVzdHJ1Y3R1cmUiLCJpZCIsImluaXQiLCJwcm9wZXJ0aWVzIiwia2V5IiwiaGFuZGxlUHJvZ3JhbUV4aXQiLCJmb3JFYWNoIiwiZmlsZUltcG9ydCIsIm5hbWVzcGFjZSIsImhhcyIsInJlcG9ydCIsIm1lc3NhZ2UiXSwibWFwcGluZ3MiOiI7Ozs7OztBQU1BLHlDO0FBQ0EseUQ7QUFDQSxxQzs7QUFFQTtBQUNBO0FBQ0E7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLDBCQUFRLDRCQUFSLENBREQsRUFGRjs7QUFLSkMsWUFBUSxFQUxKLEVBRFM7OztBQVNmQyxRQVRlLCtCQVNSQyxPQVRRLEVBU0M7O0FBRWQsVUFBTUMsY0FBYyxJQUFJQyxHQUFKLEVBQXBCO0FBQ0EsVUFBTUMscUJBQXFCLElBQUlELEdBQUosRUFBM0I7O0FBRUEsZUFBU0Us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QkFIakI7QUFJTCx3QkFBZ0JNLGlCQUpYLEVBQVA7O0FBTUQsS0F0RmMsbUJBQWpCLEMsQ0FkQSIsImZpbGUiOiJuby1uYW1lZC1hcy1kZWZhdWx0LW1lbWJlci5qcyIsInNvdXJjZXNDb250ZW50IjpbIi8qKlxuICogQGZpbGVvdmVydmlldyBSdWxlIHRvIHdhcm4gYWJvdXQgcG90ZW50aWFsbHkgY29uZnVzZWQgdXNlIG9mIG5hbWUgZXhwb3J0c1xuICogQGF1dGhvciBEZXNtb25kIEJyYW5kXG4gKiBAY29weXJpZ2h0IDIwMTYgRGVzbW9uZCBCcmFuZC4gQWxsIHJpZ2h0cyByZXNlcnZlZC5cbiAqIFNlZSBMSUNFTlNFIGluIHJvb3QgZGlyZWN0b3J5IGZvciBmdWxsIGxpY2Vuc2UuXG4gKi9cbmltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCc7XG5pbXBvcnQgaW1wb3J0RGVjbGFyYXRpb24gZnJvbSAnLi4vaW1wb3J0RGVjbGFyYXRpb24nO1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5cbi8vLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG4vLyBSdWxlIERlZmluaXRpb25cbi8vLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tbmFtZWQtYXMtZGVmYXVsdC1tZW1iZXInKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW10sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcblxuICAgIGNvbnN0IGZpbGVJbXBvcnRzID0gbmV3IE1hcCgpO1xuICAgIGNvbnN0IGFsbFByb3BlcnR5TG9va3VwcyA9IG5ldyBNYXAoKTtcblxuICAgIGZ1bmN0aW9uIGhhbmRsZUltcG9ydERlZmF1bHQobm9kZSkge1xuICAgICAgY29uc3QgZGVjbGFyYXRpb24gPSBpbXBvcnREZWNsYXJhdGlvbihjb250ZXh0KTtcbiAgICAgIGNvbnN0IGV4cG9ydE1hcCA9IEV4cG9ydHMuZ2V0KGRlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZSwgY29udGV4dCk7XG4gICAgICBpZiAoZXhwb3J0TWFwID09IG51bGwpIHJldHVybjtcblxuICAgICAgaWYgKGV4cG9ydE1hcC5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgIGV4cG9ydE1hcC5yZXBvcnRFcnJvcnMoY29udGV4dCwgZGVjbGFyYXRpb24pO1xuICAgICAgICByZXR1cm47XG4gICAgICB9XG5cbiAgICAgIGZpbGVJbXBvcnRzLnNldChub2RlLmxvY2FsLm5hbWUsIHtcbiAgICAgICAgZXhwb3J0TWFwLFxuICAgICAgICBzb3VyY2VQYXRoOiBkZWNsYXJhdGlvbi5zb3VyY2UudmFsdWUsXG4gICAgICB9KTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBzdG9yZVByb3BlcnR5TG9va3VwKG9iamVjdE5hbWUsIHByb3BOYW1lLCBub2RlKSB7XG4gICAgICBjb25zdCBsb29rdXBzID0gYWxsUHJvcGVydHlMb29rdXBzLmdldChvYmplY3ROYW1lKSB8fCBbXTtcbiAgICAgIGxvb2t1cHMucHVzaCh7IG5vZGUsIHByb3BOYW1lIH0pO1xuICAgICAgYWxsUHJvcGVydHlMb29rdXBzLnNldChvYmplY3ROYW1lLCBsb29rdXBzKTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVQcm9wTG9va3VwKG5vZGUpIHtcbiAgICAgIGNvbnN0IG9iamVjdE5hbWUgPSBub2RlLm9iamVjdC5uYW1lO1xuICAgICAgY29uc3QgcHJvcE5hbWUgPSBub2RlLnByb3BlcnR5Lm5hbWU7XG4gICAgICBzdG9yZVByb3BlcnR5TG9va3VwKG9iamVjdE5hbWUsIHByb3BOYW1lLCBub2RlKTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBoYW5kbGVEZXN0cnVjdHVyaW5nQXNzaWdubWVudChub2RlKSB7XG4gICAgICBjb25zdCBpc0Rlc3RydWN0dXJlID0gKFxuICAgICAgICBub2RlLmlkLnR5cGUgPT09ICdPYmplY3RQYXR0ZXJuJyAmJlxuICAgICAgICBub2RlLmluaXQgIT0gbnVsbCAmJlxuICAgICAgICBub2RlLmluaXQudHlwZSA9PT0gJ0lkZW50aWZpZXInXG4gICAgICApO1xuICAgICAgaWYgKCFpc0Rlc3RydWN0dXJlKSByZXR1cm47XG5cbiAgICAgIGNvbnN0IG9iamVjdE5hbWUgPSBub2RlLmluaXQubmFtZTtcbiAgICAgIGZvciAoY29uc3QgeyBrZXkgfSBvZiBub2RlLmlkLnByb3BlcnRpZXMpIHtcbiAgICAgICAgaWYgKGtleSA9PSBudWxsKSBjb250aW51ZTsgIC8vIHRydWUgZm9yIHJlc3QgcHJvcGVydGllc1xuICAgICAgICBzdG9yZVByb3BlcnR5TG9va3VwKG9iamVjdE5hbWUsIGtleS5uYW1lLCBrZXkpO1xuICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGhhbmRsZVByb2dyYW1FeGl0KCkge1xuICAgICAgYWxsUHJvcGVydHlMb29rdXBzLmZvckVhY2goKGxvb2t1cHMsIG9iamVjdE5hbWUpID0+IHtcbiAgICAgICAgY29uc3QgZmlsZUltcG9ydCA9IGZpbGVJbXBvcnRzLmdldChvYmplY3ROYW1lKTtcbiAgICAgICAgaWYgKGZpbGVJbXBvcnQgPT0gbnVsbCkgcmV0dXJuO1xuXG4gICAgICAgIGZvciAoY29uc3QgeyBwcm9wTmFtZSwgbm9kZSB9IG9mIGxvb2t1cHMpIHtcbiAgICAgICAgICAvLyB0aGUgZGVmYXVsdCBpbXBvcnQgY2FuIGhhdmUgYSBcImRlZmF1bHRcIiBwcm9wZXJ0eVxuICAgICAgICAgIGlmIChwcm9wTmFtZSA9PT0gJ2RlZmF1bHQnKSBjb250aW51ZTtcbiAgICAgICAgICBpZiAoIWZpbGVJbXBvcnQuZXhwb3J0TWFwLm5hbWVzcGFjZS5oYXMocHJvcE5hbWUpKSBjb250aW51ZTtcblxuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiAoXG4gICAgICAgICAgICAgIGBDYXV0aW9uOiBcXGAke29iamVjdE5hbWV9XFxgIGFsc28gaGFzIGEgbmFtZWQgZXhwb3J0IGAgK1xuICAgICAgICAgICAgICBgXFxgJHtwcm9wTmFtZX1cXGAuIENoZWNrIGlmIHlvdSBtZWFudCB0byB3cml0ZSBgICtcbiAgICAgICAgICAgICAgYFxcYGltcG9ydCB7JHtwcm9wTmFtZX19IGZyb20gJyR7ZmlsZUltcG9ydC5zb3VyY2VQYXRofSdcXGAgYCArXG4gICAgICAgICAgICAgICdpbnN0ZWFkLidcbiAgICAgICAgICAgICksXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH0pO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcic6IGhhbmRsZUltcG9ydERlZmF1bHQsXG4gICAgICAnTWVtYmVyRXhwcmVzc2lvbic6IGhhbmRsZVByb3BMb29rdXAsXG4gICAgICAnVmFyaWFibGVEZWNsYXJhdG9yJzogaGFuZGxlRGVzdHJ1Y3R1cmluZ0Fzc2lnbm1lbnQsXG4gICAgICAnUHJvZ3JhbTpleGl0JzogaGFuZGxlUHJvZ3JhbUV4aXQsXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=