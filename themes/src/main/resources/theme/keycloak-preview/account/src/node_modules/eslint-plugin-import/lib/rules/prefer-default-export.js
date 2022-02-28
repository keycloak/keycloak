'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('prefer-default-export')
    }
  },

  create: function (context) {
    let specifierExportCount = 0;
    let hasDefaultExport = false;
    let hasStarExport = false;
    let namedExportNode = null;

    function captureDeclaration(identifierOrPattern) {
      if (identifierOrPattern.type === 'ObjectPattern') {
        // recursively capture
        identifierOrPattern.properties.forEach(function (property) {
          captureDeclaration(property.value);
        });
      } else {
        // assume it's a single standard identifier
        specifierExportCount++;
      }
    }

    return {
      'ExportDefaultSpecifier': function () {
        hasDefaultExport = true;
      },

      'ExportSpecifier': function (node) {
        if (node.exported.name === 'default') {
          hasDefaultExport = true;
        } else {
          specifierExportCount++;
          namedExportNode = node;
        }
      },

      'ExportNamedDeclaration': function (node) {
        // if there are specifiers, node.declaration should be null
        if (!node.declaration) return;

        // don't count flow types exports
        if (node.exportKind === 'type') return;

        if (node.declaration.declarations) {
          node.declaration.declarations.forEach(function (declaration) {
            captureDeclaration(declaration.id);
          });
        } else {
          // captures 'export function foo() {}' syntax
          specifierExportCount++;
        }

        namedExportNode = node;
      },

      'ExportDefaultDeclaration': function () {
        hasDefaultExport = true;
      },

      'ExportAllDeclaration': function () {
        hasStarExport = true;
      },

      'Program:exit': function () {
        if (specifierExportCount === 1 && !hasDefaultExport && !hasStarExport) {
          context.report(namedExportNode, 'Prefer default export.');
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL3ByZWZlci1kZWZhdWx0LWV4cG9ydC5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwiY3JlYXRlIiwiY29udGV4dCIsInNwZWNpZmllckV4cG9ydENvdW50IiwiaGFzRGVmYXVsdEV4cG9ydCIsImhhc1N0YXJFeHBvcnQiLCJuYW1lZEV4cG9ydE5vZGUiLCJjYXB0dXJlRGVjbGFyYXRpb24iLCJpZGVudGlmaWVyT3JQYXR0ZXJuIiwicHJvcGVydGllcyIsImZvckVhY2giLCJwcm9wZXJ0eSIsInZhbHVlIiwibm9kZSIsImV4cG9ydGVkIiwibmFtZSIsImRlY2xhcmF0aW9uIiwiZXhwb3J0S2luZCIsImRlY2xhcmF0aW9ucyIsImlkIiwicmVwb3J0Il0sIm1hcHBpbmdzIjoiQUFBQTs7QUFFQTs7Ozs7O0FBRUFBLE9BQU9DLE9BQVAsR0FBaUI7QUFDZkMsUUFBTTtBQUNKQyxVQUFNLFlBREY7QUFFSkMsVUFBTTtBQUNKQyxXQUFLLHVCQUFRLHVCQUFSO0FBREQ7QUFGRixHQURTOztBQVFmQyxVQUFRLFVBQVNDLE9BQVQsRUFBa0I7QUFDeEIsUUFBSUMsdUJBQXVCLENBQTNCO0FBQ0EsUUFBSUMsbUJBQW1CLEtBQXZCO0FBQ0EsUUFBSUMsZ0JBQWdCLEtBQXBCO0FBQ0EsUUFBSUMsa0JBQWtCLElBQXRCOztBQUVBLGFBQVNDLGtCQUFULENBQTRCQyxtQkFBNUIsRUFBaUQ7QUFDL0MsVUFBSUEsb0JBQW9CVixJQUFwQixLQUE2QixlQUFqQyxFQUFrRDtBQUNoRDtBQUNBVSw0QkFBb0JDLFVBQXBCLENBQ0dDLE9BREgsQ0FDVyxVQUFTQyxRQUFULEVBQW1CO0FBQzFCSiw2QkFBbUJJLFNBQVNDLEtBQTVCO0FBQ0QsU0FISDtBQUlELE9BTkQsTUFNTztBQUNQO0FBQ0VUO0FBQ0Q7QUFDRjs7QUFFRCxXQUFPO0FBQ0wsZ0NBQTBCLFlBQVc7QUFDbkNDLDJCQUFtQixJQUFuQjtBQUNELE9BSEk7O0FBS0wseUJBQW1CLFVBQVNTLElBQVQsRUFBZTtBQUNoQyxZQUFJQSxLQUFLQyxRQUFMLENBQWNDLElBQWQsS0FBdUIsU0FBM0IsRUFBc0M7QUFDcENYLDZCQUFtQixJQUFuQjtBQUNELFNBRkQsTUFFTztBQUNMRDtBQUNBRyw0QkFBa0JPLElBQWxCO0FBQ0Q7QUFDRixPQVpJOztBQWNMLGdDQUEwQixVQUFTQSxJQUFULEVBQWU7QUFDdkM7QUFDQSxZQUFJLENBQUNBLEtBQUtHLFdBQVYsRUFBdUI7O0FBRXZCO0FBQ0EsWUFBSUgsS0FBS0ksVUFBTCxLQUFvQixNQUF4QixFQUFnQzs7QUFFaEMsWUFBSUosS0FBS0csV0FBTCxDQUFpQkUsWUFBckIsRUFBbUM7QUFDakNMLGVBQUtHLFdBQUwsQ0FBaUJFLFlBQWpCLENBQThCUixPQUE5QixDQUFzQyxVQUFTTSxXQUFULEVBQXNCO0FBQzFEVCwrQkFBbUJTLFlBQVlHLEVBQS9CO0FBQ0QsV0FGRDtBQUdELFNBSkQsTUFLSztBQUNIO0FBQ0FoQjtBQUNEOztBQUVERywwQkFBa0JPLElBQWxCO0FBQ0QsT0FoQ0k7O0FBa0NMLGtDQUE0QixZQUFXO0FBQ3JDVCwyQkFBbUIsSUFBbkI7QUFDRCxPQXBDSTs7QUFzQ0wsOEJBQXdCLFlBQVc7QUFDakNDLHdCQUFnQixJQUFoQjtBQUNELE9BeENJOztBQTBDTCxzQkFBZ0IsWUFBVztBQUN6QixZQUFJRix5QkFBeUIsQ0FBekIsSUFBOEIsQ0FBQ0MsZ0JBQS9CLElBQW1ELENBQUNDLGFBQXhELEVBQXVFO0FBQ3JFSCxrQkFBUWtCLE1BQVIsQ0FBZWQsZUFBZixFQUFnQyx3QkFBaEM7QUFDRDtBQUNGO0FBOUNJLEtBQVA7QUFnREQ7QUEzRWMsQ0FBakIiLCJmaWxlIjoicnVsZXMvcHJlZmVyLWRlZmF1bHQtZXhwb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsiJ3VzZSBzdHJpY3QnXG5cbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgncHJlZmVyLWRlZmF1bHQtZXhwb3J0JyksXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uKGNvbnRleHQpIHtcbiAgICBsZXQgc3BlY2lmaWVyRXhwb3J0Q291bnQgPSAwXG4gICAgbGV0IGhhc0RlZmF1bHRFeHBvcnQgPSBmYWxzZVxuICAgIGxldCBoYXNTdGFyRXhwb3J0ID0gZmFsc2VcbiAgICBsZXQgbmFtZWRFeHBvcnROb2RlID0gbnVsbFxuXG4gICAgZnVuY3Rpb24gY2FwdHVyZURlY2xhcmF0aW9uKGlkZW50aWZpZXJPclBhdHRlcm4pIHtcbiAgICAgIGlmIChpZGVudGlmaWVyT3JQYXR0ZXJuLnR5cGUgPT09ICdPYmplY3RQYXR0ZXJuJykge1xuICAgICAgICAvLyByZWN1cnNpdmVseSBjYXB0dXJlXG4gICAgICAgIGlkZW50aWZpZXJPclBhdHRlcm4ucHJvcGVydGllc1xuICAgICAgICAgIC5mb3JFYWNoKGZ1bmN0aW9uKHByb3BlcnR5KSB7XG4gICAgICAgICAgICBjYXB0dXJlRGVjbGFyYXRpb24ocHJvcGVydHkudmFsdWUpXG4gICAgICAgICAgfSlcbiAgICAgIH0gZWxzZSB7XG4gICAgICAvLyBhc3N1bWUgaXQncyBhIHNpbmdsZSBzdGFuZGFyZCBpZGVudGlmaWVyXG4gICAgICAgIHNwZWNpZmllckV4cG9ydENvdW50KytcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgJ0V4cG9ydERlZmF1bHRTcGVjaWZpZXInOiBmdW5jdGlvbigpIHtcbiAgICAgICAgaGFzRGVmYXVsdEV4cG9ydCA9IHRydWVcbiAgICAgIH0sXG5cbiAgICAgICdFeHBvcnRTcGVjaWZpZXInOiBmdW5jdGlvbihub2RlKSB7XG4gICAgICAgIGlmIChub2RlLmV4cG9ydGVkLm5hbWUgPT09ICdkZWZhdWx0Jykge1xuICAgICAgICAgIGhhc0RlZmF1bHRFeHBvcnQgPSB0cnVlXG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgc3BlY2lmaWVyRXhwb3J0Q291bnQrK1xuICAgICAgICAgIG5hbWVkRXhwb3J0Tm9kZSA9IG5vZGVcbiAgICAgICAgfVxuICAgICAgfSxcblxuICAgICAgJ0V4cG9ydE5hbWVkRGVjbGFyYXRpb24nOiBmdW5jdGlvbihub2RlKSB7XG4gICAgICAgIC8vIGlmIHRoZXJlIGFyZSBzcGVjaWZpZXJzLCBub2RlLmRlY2xhcmF0aW9uIHNob3VsZCBiZSBudWxsXG4gICAgICAgIGlmICghbm9kZS5kZWNsYXJhdGlvbikgcmV0dXJuXG5cbiAgICAgICAgLy8gZG9uJ3QgY291bnQgZmxvdyB0eXBlcyBleHBvcnRzXG4gICAgICAgIGlmIChub2RlLmV4cG9ydEtpbmQgPT09ICd0eXBlJykgcmV0dXJuXG5cbiAgICAgICAgaWYgKG5vZGUuZGVjbGFyYXRpb24uZGVjbGFyYXRpb25zKSB7XG4gICAgICAgICAgbm9kZS5kZWNsYXJhdGlvbi5kZWNsYXJhdGlvbnMuZm9yRWFjaChmdW5jdGlvbihkZWNsYXJhdGlvbikge1xuICAgICAgICAgICAgY2FwdHVyZURlY2xhcmF0aW9uKGRlY2xhcmF0aW9uLmlkKVxuICAgICAgICAgIH0pXG4gICAgICAgIH1cbiAgICAgICAgZWxzZSB7XG4gICAgICAgICAgLy8gY2FwdHVyZXMgJ2V4cG9ydCBmdW5jdGlvbiBmb28oKSB7fScgc3ludGF4XG4gICAgICAgICAgc3BlY2lmaWVyRXhwb3J0Q291bnQrK1xuICAgICAgICB9XG5cbiAgICAgICAgbmFtZWRFeHBvcnROb2RlID0gbm9kZVxuICAgICAgfSxcblxuICAgICAgJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbic6IGZ1bmN0aW9uKCkge1xuICAgICAgICBoYXNEZWZhdWx0RXhwb3J0ID0gdHJ1ZVxuICAgICAgfSxcblxuICAgICAgJ0V4cG9ydEFsbERlY2xhcmF0aW9uJzogZnVuY3Rpb24oKSB7XG4gICAgICAgIGhhc1N0YXJFeHBvcnQgPSB0cnVlXG4gICAgICB9LFxuXG4gICAgICAnUHJvZ3JhbTpleGl0JzogZnVuY3Rpb24oKSB7XG4gICAgICAgIGlmIChzcGVjaWZpZXJFeHBvcnRDb3VudCA9PT0gMSAmJiAhaGFzRGVmYXVsdEV4cG9ydCAmJiAhaGFzU3RhckV4cG9ydCkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KG5hbWVkRXhwb3J0Tm9kZSwgJ1ByZWZlciBkZWZhdWx0IGV4cG9ydC4nKVxuICAgICAgICB9XG4gICAgICB9LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==