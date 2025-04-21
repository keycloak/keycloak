'use strict';

var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('prefer-default-export') },

    schema: [] },


  create: function () {function create(context) {
      var specifierExportCount = 0;
      var hasDefaultExport = false;
      var hasStarExport = false;
      var hasTypeExport = false;
      var namedExportNode = null;

      function captureDeclaration(identifierOrPattern) {
        if (identifierOrPattern && identifierOrPattern.type === 'ObjectPattern') {
          // recursively capture
          identifierOrPattern.properties.
          forEach(function (property) {
            captureDeclaration(property.value);
          });
        } else if (identifierOrPattern && identifierOrPattern.type === 'ArrayPattern') {
          identifierOrPattern.elements.
          forEach(captureDeclaration);
        } else {
          // assume it's a single standard identifier
          specifierExportCount++;
        }
      }

      return {
        'ExportDefaultSpecifier': function () {function ExportDefaultSpecifier() {
            hasDefaultExport = true;
          }return ExportDefaultSpecifier;}(),

        'ExportSpecifier': function () {function ExportSpecifier(node) {
            if ((node.exported.name || node.exported.value) === 'default') {
              hasDefaultExport = true;
            } else {
              specifierExportCount++;
              namedExportNode = node;
            }
          }return ExportSpecifier;}(),

        'ExportNamedDeclaration': function () {function ExportNamedDeclaration(node) {
            // if there are specifiers, node.declaration should be null
            if (!node.declaration) return;var

            type = node.declaration.type;

            if (
            type === 'TSTypeAliasDeclaration' ||
            type === 'TypeAlias' ||
            type === 'TSInterfaceDeclaration' ||
            type === 'InterfaceDeclaration')
            {
              specifierExportCount++;
              hasTypeExport = true;
              return;
            }

            if (node.declaration.declarations) {
              node.declaration.declarations.forEach(function (declaration) {
                captureDeclaration(declaration.id);
              });
            } else {
              // captures 'export function foo() {}' syntax
              specifierExportCount++;
            }

            namedExportNode = node;
          }return ExportNamedDeclaration;}(),

        'ExportDefaultDeclaration': function () {function ExportDefaultDeclaration() {
            hasDefaultExport = true;
          }return ExportDefaultDeclaration;}(),

        'ExportAllDeclaration': function () {function ExportAllDeclaration() {
            hasStarExport = true;
          }return ExportAllDeclaration;}(),

        'Program:exit': function () {function ProgramExit() {
            if (specifierExportCount === 1 && !hasDefaultExport && !hasStarExport && !hasTypeExport) {
              context.report(namedExportNode, 'Prefer default export.');
            }
          }return ProgramExit;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9wcmVmZXItZGVmYXVsdC1leHBvcnQuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJzcGVjaWZpZXJFeHBvcnRDb3VudCIsImhhc0RlZmF1bHRFeHBvcnQiLCJoYXNTdGFyRXhwb3J0IiwiaGFzVHlwZUV4cG9ydCIsIm5hbWVkRXhwb3J0Tm9kZSIsImNhcHR1cmVEZWNsYXJhdGlvbiIsImlkZW50aWZpZXJPclBhdHRlcm4iLCJwcm9wZXJ0aWVzIiwiZm9yRWFjaCIsInByb3BlcnR5IiwidmFsdWUiLCJlbGVtZW50cyIsIm5vZGUiLCJleHBvcnRlZCIsIm5hbWUiLCJkZWNsYXJhdGlvbiIsImRlY2xhcmF0aW9ucyIsImlkIiwicmVwb3J0Il0sIm1hcHBpbmdzIjoiQUFBQTs7QUFFQSxxQzs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssMEJBQVEsdUJBQVIsQ0FERCxFQUZGOztBQUtKQyxZQUFRLEVBTEosRUFEUzs7O0FBU2ZDLFFBVGUsK0JBU1JDLE9BVFEsRUFTQztBQUNkLFVBQUlDLHVCQUF1QixDQUEzQjtBQUNBLFVBQUlDLG1CQUFtQixLQUF2QjtBQUNBLFVBQUlDLGdCQUFnQixLQUFwQjtBQUNBLFVBQUlDLGdCQUFnQixLQUFwQjtBQUNBLFVBQUlDLGtCQUFrQixJQUF0Qjs7QUFFQSxlQUFTQyxrQkFBVCxDQUE0QkMsbUJBQTVCLEVBQWlEO0FBQy9DLFlBQUlBLHVCQUF1QkEsb0JBQW9CWixJQUFwQixLQUE2QixlQUF4RCxFQUF5RTtBQUN2RTtBQUNBWSw4QkFBb0JDLFVBQXBCO0FBQ0dDLGlCQURILENBQ1csVUFBVUMsUUFBVixFQUFvQjtBQUMzQkosK0JBQW1CSSxTQUFTQyxLQUE1QjtBQUNELFdBSEg7QUFJRCxTQU5ELE1BTU8sSUFBSUosdUJBQXVCQSxvQkFBb0JaLElBQXBCLEtBQTZCLGNBQXhELEVBQXdFO0FBQzdFWSw4QkFBb0JLLFFBQXBCO0FBQ0dILGlCQURILENBQ1dILGtCQURYO0FBRUQsU0FITSxNQUdDO0FBQ1I7QUFDRUw7QUFDRDtBQUNGOztBQUVELGFBQU87QUFDTCwrQ0FBMEIsa0NBQVk7QUFDcENDLCtCQUFtQixJQUFuQjtBQUNELFdBRkQsaUNBREs7O0FBS0wsd0NBQW1CLHlCQUFVVyxJQUFWLEVBQWdCO0FBQ2pDLGdCQUFJLENBQUNBLEtBQUtDLFFBQUwsQ0FBY0MsSUFBZCxJQUFzQkYsS0FBS0MsUUFBTCxDQUFjSCxLQUFyQyxNQUFnRCxTQUFwRCxFQUErRDtBQUM3RFQsaUNBQW1CLElBQW5CO0FBQ0QsYUFGRCxNQUVPO0FBQ0xEO0FBQ0FJLGdDQUFrQlEsSUFBbEI7QUFDRDtBQUNGLFdBUEQsMEJBTEs7O0FBY0wsK0NBQTBCLGdDQUFVQSxJQUFWLEVBQWdCO0FBQ3hDO0FBQ0EsZ0JBQUksQ0FBQ0EsS0FBS0csV0FBVixFQUF1QixPQUZpQjs7QUFJaENyQixnQkFKZ0MsR0FJdkJrQixLQUFLRyxXQUprQixDQUloQ3JCLElBSmdDOztBQU14QztBQUNFQSxxQkFBUyx3QkFBVDtBQUNBQSxxQkFBUyxXQURUO0FBRUFBLHFCQUFTLHdCQUZUO0FBR0FBLHFCQUFTLHNCQUpYO0FBS0U7QUFDQU07QUFDQUcsOEJBQWdCLElBQWhCO0FBQ0E7QUFDRDs7QUFFRCxnQkFBSVMsS0FBS0csV0FBTCxDQUFpQkMsWUFBckIsRUFBbUM7QUFDakNKLG1CQUFLRyxXQUFMLENBQWlCQyxZQUFqQixDQUE4QlIsT0FBOUIsQ0FBc0MsVUFBVU8sV0FBVixFQUF1QjtBQUMzRFYsbUNBQW1CVSxZQUFZRSxFQUEvQjtBQUNELGVBRkQ7QUFHRCxhQUpELE1BSU87QUFDTDtBQUNBakI7QUFDRDs7QUFFREksOEJBQWtCUSxJQUFsQjtBQUNELFdBM0JELGlDQWRLOztBQTJDTCxpREFBNEIsb0NBQVk7QUFDdENYLCtCQUFtQixJQUFuQjtBQUNELFdBRkQsbUNBM0NLOztBQStDTCw2Q0FBd0IsZ0NBQVk7QUFDbENDLDRCQUFnQixJQUFoQjtBQUNELFdBRkQsK0JBL0NLOztBQW1ETCxxQ0FBZ0IsdUJBQVk7QUFDMUIsZ0JBQUlGLHlCQUF5QixDQUF6QixJQUE4QixDQUFDQyxnQkFBL0IsSUFBbUQsQ0FBQ0MsYUFBcEQsSUFBcUUsQ0FBQ0MsYUFBMUUsRUFBeUY7QUFDdkZKLHNCQUFRbUIsTUFBUixDQUFlZCxlQUFmLEVBQWdDLHdCQUFoQztBQUNEO0FBQ0YsV0FKRCxzQkFuREssRUFBUDs7QUF5REQsS0F6RmMsbUJBQWpCIiwiZmlsZSI6InByZWZlci1kZWZhdWx0LWV4cG9ydC5qcyIsInNvdXJjZXNDb250ZW50IjpbIid1c2Ugc3RyaWN0JztcblxuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCc7XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgncHJlZmVyLWRlZmF1bHQtZXhwb3J0JyksXG4gICAgfSxcbiAgICBzY2hlbWE6IFtdLFxuICB9LFxuXG4gIGNyZWF0ZShjb250ZXh0KSB7XG4gICAgbGV0IHNwZWNpZmllckV4cG9ydENvdW50ID0gMDtcbiAgICBsZXQgaGFzRGVmYXVsdEV4cG9ydCA9IGZhbHNlO1xuICAgIGxldCBoYXNTdGFyRXhwb3J0ID0gZmFsc2U7XG4gICAgbGV0IGhhc1R5cGVFeHBvcnQgPSBmYWxzZTtcbiAgICBsZXQgbmFtZWRFeHBvcnROb2RlID0gbnVsbDtcblxuICAgIGZ1bmN0aW9uIGNhcHR1cmVEZWNsYXJhdGlvbihpZGVudGlmaWVyT3JQYXR0ZXJuKSB7XG4gICAgICBpZiAoaWRlbnRpZmllck9yUGF0dGVybiAmJiBpZGVudGlmaWVyT3JQYXR0ZXJuLnR5cGUgPT09ICdPYmplY3RQYXR0ZXJuJykge1xuICAgICAgICAvLyByZWN1cnNpdmVseSBjYXB0dXJlXG4gICAgICAgIGlkZW50aWZpZXJPclBhdHRlcm4ucHJvcGVydGllc1xuICAgICAgICAgIC5mb3JFYWNoKGZ1bmN0aW9uIChwcm9wZXJ0eSkge1xuICAgICAgICAgICAgY2FwdHVyZURlY2xhcmF0aW9uKHByb3BlcnR5LnZhbHVlKTtcbiAgICAgICAgICB9KTtcbiAgICAgIH0gZWxzZSBpZiAoaWRlbnRpZmllck9yUGF0dGVybiAmJiBpZGVudGlmaWVyT3JQYXR0ZXJuLnR5cGUgPT09ICdBcnJheVBhdHRlcm4nKSB7XG4gICAgICAgIGlkZW50aWZpZXJPclBhdHRlcm4uZWxlbWVudHNcbiAgICAgICAgICAuZm9yRWFjaChjYXB0dXJlRGVjbGFyYXRpb24pO1xuICAgICAgfSBlbHNlICB7XG4gICAgICAvLyBhc3N1bWUgaXQncyBhIHNpbmdsZSBzdGFuZGFyZCBpZGVudGlmaWVyXG4gICAgICAgIHNwZWNpZmllckV4cG9ydENvdW50Kys7XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgICdFeHBvcnREZWZhdWx0U3BlY2lmaWVyJzogZnVuY3Rpb24gKCkge1xuICAgICAgICBoYXNEZWZhdWx0RXhwb3J0ID0gdHJ1ZTtcbiAgICAgIH0sXG5cbiAgICAgICdFeHBvcnRTcGVjaWZpZXInOiBmdW5jdGlvbiAobm9kZSkge1xuICAgICAgICBpZiAoKG5vZGUuZXhwb3J0ZWQubmFtZSB8fCBub2RlLmV4cG9ydGVkLnZhbHVlKSA9PT0gJ2RlZmF1bHQnKSB7XG4gICAgICAgICAgaGFzRGVmYXVsdEV4cG9ydCA9IHRydWU7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgc3BlY2lmaWVyRXhwb3J0Q291bnQrKztcbiAgICAgICAgICBuYW1lZEV4cG9ydE5vZGUgPSBub2RlO1xuICAgICAgICB9XG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbic6IGZ1bmN0aW9uIChub2RlKSB7XG4gICAgICAgIC8vIGlmIHRoZXJlIGFyZSBzcGVjaWZpZXJzLCBub2RlLmRlY2xhcmF0aW9uIHNob3VsZCBiZSBudWxsXG4gICAgICAgIGlmICghbm9kZS5kZWNsYXJhdGlvbikgcmV0dXJuO1xuXG4gICAgICAgIGNvbnN0IHsgdHlwZSB9ID0gbm9kZS5kZWNsYXJhdGlvbjtcblxuICAgICAgICBpZiAoXG4gICAgICAgICAgdHlwZSA9PT0gJ1RTVHlwZUFsaWFzRGVjbGFyYXRpb24nIHx8XG4gICAgICAgICAgdHlwZSA9PT0gJ1R5cGVBbGlhcycgfHxcbiAgICAgICAgICB0eXBlID09PSAnVFNJbnRlcmZhY2VEZWNsYXJhdGlvbicgfHxcbiAgICAgICAgICB0eXBlID09PSAnSW50ZXJmYWNlRGVjbGFyYXRpb24nXG4gICAgICAgICkge1xuICAgICAgICAgIHNwZWNpZmllckV4cG9ydENvdW50Kys7XG4gICAgICAgICAgaGFzVHlwZUV4cG9ydCA9IHRydWU7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zKSB7XG4gICAgICAgICAgbm9kZS5kZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMuZm9yRWFjaChmdW5jdGlvbiAoZGVjbGFyYXRpb24pIHtcbiAgICAgICAgICAgIGNhcHR1cmVEZWNsYXJhdGlvbihkZWNsYXJhdGlvbi5pZCk7XG4gICAgICAgICAgfSk7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgLy8gY2FwdHVyZXMgJ2V4cG9ydCBmdW5jdGlvbiBmb28oKSB7fScgc3ludGF4XG4gICAgICAgICAgc3BlY2lmaWVyRXhwb3J0Q291bnQrKztcbiAgICAgICAgfVxuXG4gICAgICAgIG5hbWVkRXhwb3J0Tm9kZSA9IG5vZGU7XG4gICAgICB9LFxuXG4gICAgICAnRXhwb3J0RGVmYXVsdERlY2xhcmF0aW9uJzogZnVuY3Rpb24gKCkge1xuICAgICAgICBoYXNEZWZhdWx0RXhwb3J0ID0gdHJ1ZTtcbiAgICAgIH0sXG5cbiAgICAgICdFeHBvcnRBbGxEZWNsYXJhdGlvbic6IGZ1bmN0aW9uICgpIHtcbiAgICAgICAgaGFzU3RhckV4cG9ydCA9IHRydWU7XG4gICAgICB9LFxuXG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24gKCkge1xuICAgICAgICBpZiAoc3BlY2lmaWVyRXhwb3J0Q291bnQgPT09IDEgJiYgIWhhc0RlZmF1bHRFeHBvcnQgJiYgIWhhc1N0YXJFeHBvcnQgJiYgIWhhc1R5cGVFeHBvcnQpIHtcbiAgICAgICAgICBjb250ZXh0LnJlcG9ydChuYW1lZEV4cG9ydE5vZGUsICdQcmVmZXIgZGVmYXVsdCBleHBvcnQuJyk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=