'use strict';

var _slicedToArray = function () { function sliceIterator(arr, i) { var _arr = []; var _n = true; var _d = false; var _e = undefined; try { for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) { _arr.push(_s.value); if (i && _arr.length === i) break; } } catch (err) { _d = true; _e = err; } finally { try { if (!_n && _i["return"]) _i["return"](); } finally { if (_d) throw _e; } } return _arr; } return function (arr, i) { if (Array.isArray(arr)) { return arr; } else if (Symbol.iterator in Object(arr)) { return sliceIterator(arr, i); } else { throw new TypeError("Invalid attempt to destructure non-iterable instance"); } }; }();

var _containsPath = require('contains-path');

var _containsPath2 = _interopRequireDefault(_containsPath);

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _resolve = require('eslint-module-utils/resolve');

var _resolve2 = _interopRequireDefault(_resolve);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('no-restricted-paths')
    },

    schema: [{
      type: 'object',
      properties: {
        zones: {
          type: 'array',
          minItems: 1,
          items: {
            type: 'object',
            properties: {
              target: { type: 'string' },
              from: { type: 'string' }
            },
            additionalProperties: false
          }
        },
        basePath: { type: 'string' }
      },
      additionalProperties: false
    }]
  },

  create: function noRestrictedPaths(context) {
    const options = context.options[0] || {};
    const restrictedPaths = options.zones || [];
    const basePath = options.basePath || process.cwd();
    const currentFilename = context.getFilename();
    const matchingZones = restrictedPaths.filter(zone => {
      const targetPath = _path2.default.resolve(basePath, zone.target);

      return (0, _containsPath2.default)(currentFilename, targetPath);
    });

    function checkForRestrictedImportPath(importPath, node) {
      const absoluteImportPath = (0, _resolve2.default)(importPath, context);

      if (!absoluteImportPath) {
        return;
      }

      matchingZones.forEach(zone => {
        const absoluteFrom = _path2.default.resolve(basePath, zone.from);

        if ((0, _containsPath2.default)(absoluteImportPath, absoluteFrom)) {
          context.report({
            node,
            message: `Unexpected path "${importPath}" imported in restricted zone.`
          });
        }
      });
    }

    return {
      ImportDeclaration(node) {
        checkForRestrictedImportPath(node.source.value, node.source);
      },
      CallExpression(node) {
        if ((0, _staticRequire2.default)(node)) {
          var _node$arguments = _slicedToArray(node.arguments, 1);

          const firstArgument = _node$arguments[0];


          checkForRestrictedImportPath(firstArgument.value, firstArgument);
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXJlc3RyaWN0ZWQtcGF0aHMuanMiXSwibmFtZXMiOlsibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJ6b25lcyIsIm1pbkl0ZW1zIiwiaXRlbXMiLCJ0YXJnZXQiLCJmcm9tIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJiYXNlUGF0aCIsImNyZWF0ZSIsIm5vUmVzdHJpY3RlZFBhdGhzIiwiY29udGV4dCIsIm9wdGlvbnMiLCJyZXN0cmljdGVkUGF0aHMiLCJwcm9jZXNzIiwiY3dkIiwiY3VycmVudEZpbGVuYW1lIiwiZ2V0RmlsZW5hbWUiLCJtYXRjaGluZ1pvbmVzIiwiZmlsdGVyIiwiem9uZSIsInRhcmdldFBhdGgiLCJwYXRoIiwicmVzb2x2ZSIsImNoZWNrRm9yUmVzdHJpY3RlZEltcG9ydFBhdGgiLCJpbXBvcnRQYXRoIiwibm9kZSIsImFic29sdXRlSW1wb3J0UGF0aCIsImZvckVhY2giLCJhYnNvbHV0ZUZyb20iLCJyZXBvcnQiLCJtZXNzYWdlIiwiSW1wb3J0RGVjbGFyYXRpb24iLCJzb3VyY2UiLCJ2YWx1ZSIsIkNhbGxFeHByZXNzaW9uIiwiYXJndW1lbnRzIiwiZmlyc3RBcmd1bWVudCJdLCJtYXBwaW5ncyI6Ijs7OztBQUFBOzs7O0FBQ0E7Ozs7QUFFQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxxQkFBUjtBQURELEtBRkY7O0FBTUpDLFlBQVEsQ0FDTjtBQUNFSCxZQUFNLFFBRFI7QUFFRUksa0JBQVk7QUFDVkMsZUFBTztBQUNMTCxnQkFBTSxPQUREO0FBRUxNLG9CQUFVLENBRkw7QUFHTEMsaUJBQU87QUFDTFAsa0JBQU0sUUFERDtBQUVMSSx3QkFBWTtBQUNWSSxzQkFBUSxFQUFFUixNQUFNLFFBQVIsRUFERTtBQUVWUyxvQkFBTSxFQUFFVCxNQUFNLFFBQVI7QUFGSSxhQUZQO0FBTUxVLGtDQUFzQjtBQU5qQjtBQUhGLFNBREc7QUFhVkMsa0JBQVUsRUFBRVgsTUFBTSxRQUFSO0FBYkEsT0FGZDtBQWlCRVUsNEJBQXNCO0FBakJ4QixLQURNO0FBTkosR0FEUzs7QUE4QmZFLFVBQVEsU0FBU0MsaUJBQVQsQ0FBMkJDLE9BQTNCLEVBQW9DO0FBQzFDLFVBQU1DLFVBQVVELFFBQVFDLE9BQVIsQ0FBZ0IsQ0FBaEIsS0FBc0IsRUFBdEM7QUFDQSxVQUFNQyxrQkFBa0JELFFBQVFWLEtBQVIsSUFBaUIsRUFBekM7QUFDQSxVQUFNTSxXQUFXSSxRQUFRSixRQUFSLElBQW9CTSxRQUFRQyxHQUFSLEVBQXJDO0FBQ0EsVUFBTUMsa0JBQWtCTCxRQUFRTSxXQUFSLEVBQXhCO0FBQ0EsVUFBTUMsZ0JBQWdCTCxnQkFBZ0JNLE1BQWhCLENBQXdCQyxJQUFELElBQVU7QUFDckQsWUFBTUMsYUFBYUMsZUFBS0MsT0FBTCxDQUFhZixRQUFiLEVBQXVCWSxLQUFLZixNQUE1QixDQUFuQjs7QUFFQSxhQUFPLDRCQUFhVyxlQUFiLEVBQThCSyxVQUE5QixDQUFQO0FBQ0QsS0FKcUIsQ0FBdEI7O0FBTUEsYUFBU0csNEJBQVQsQ0FBc0NDLFVBQXRDLEVBQWtEQyxJQUFsRCxFQUF3RDtBQUNwRCxZQUFNQyxxQkFBcUIsdUJBQVFGLFVBQVIsRUFBb0JkLE9BQXBCLENBQTNCOztBQUVBLFVBQUksQ0FBQ2dCLGtCQUFMLEVBQXlCO0FBQ3ZCO0FBQ0Q7O0FBRURULG9CQUFjVSxPQUFkLENBQXVCUixJQUFELElBQVU7QUFDOUIsY0FBTVMsZUFBZVAsZUFBS0MsT0FBTCxDQUFhZixRQUFiLEVBQXVCWSxLQUFLZCxJQUE1QixDQUFyQjs7QUFFQSxZQUFJLDRCQUFhcUIsa0JBQWIsRUFBaUNFLFlBQWpDLENBQUosRUFBb0Q7QUFDbERsQixrQkFBUW1CLE1BQVIsQ0FBZTtBQUNiSixnQkFEYTtBQUViSyxxQkFBVSxvQkFBbUJOLFVBQVc7QUFGM0IsV0FBZjtBQUlEO0FBQ0YsT0FURDtBQVVIOztBQUVELFdBQU87QUFDTE8sd0JBQWtCTixJQUFsQixFQUF3QjtBQUN0QkYscUNBQTZCRSxLQUFLTyxNQUFMLENBQVlDLEtBQXpDLEVBQWdEUixLQUFLTyxNQUFyRDtBQUNELE9BSEk7QUFJTEUscUJBQWVULElBQWYsRUFBcUI7QUFDbkIsWUFBSSw2QkFBZ0JBLElBQWhCLENBQUosRUFBMkI7QUFBQSwrQ0FDQ0EsS0FBS1UsU0FETjs7QUFBQSxnQkFDakJDLGFBRGlCOzs7QUFHekJiLHVDQUE2QmEsY0FBY0gsS0FBM0MsRUFBa0RHLGFBQWxEO0FBQ0Q7QUFDRjtBQVZJLEtBQVA7QUFZRDtBQXhFYyxDQUFqQiIsImZpbGUiOiJydWxlcy9uby1yZXN0cmljdGVkLXBhdGhzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGNvbnRhaW5zUGF0aCBmcm9tICdjb250YWlucy1wYXRoJ1xuaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcblxuaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJ1xuaW1wb3J0IGlzU3RhdGljUmVxdWlyZSBmcm9tICcuLi9jb3JlL3N0YXRpY1JlcXVpcmUnXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLXJlc3RyaWN0ZWQtcGF0aHMnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgem9uZXM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgICBtaW5JdGVtczogMSxcbiAgICAgICAgICAgIGl0ZW1zOiB7XG4gICAgICAgICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgICAgICAgdGFyZ2V0OiB7IHR5cGU6ICdzdHJpbmcnIH0sXG4gICAgICAgICAgICAgICAgZnJvbTogeyB0eXBlOiAnc3RyaW5nJyB9LFxuICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgIH0sXG4gICAgICAgICAgYmFzZVBhdGg6IHsgdHlwZTogJ3N0cmluZycgfSxcbiAgICAgICAgfSxcbiAgICAgICAgYWRkaXRpb25hbFByb3BlcnRpZXM6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gbm9SZXN0cmljdGVkUGF0aHMoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge31cbiAgICBjb25zdCByZXN0cmljdGVkUGF0aHMgPSBvcHRpb25zLnpvbmVzIHx8IFtdXG4gICAgY29uc3QgYmFzZVBhdGggPSBvcHRpb25zLmJhc2VQYXRoIHx8IHByb2Nlc3MuY3dkKClcbiAgICBjb25zdCBjdXJyZW50RmlsZW5hbWUgPSBjb250ZXh0LmdldEZpbGVuYW1lKClcbiAgICBjb25zdCBtYXRjaGluZ1pvbmVzID0gcmVzdHJpY3RlZFBhdGhzLmZpbHRlcigoem9uZSkgPT4ge1xuICAgICAgY29uc3QgdGFyZ2V0UGF0aCA9IHBhdGgucmVzb2x2ZShiYXNlUGF0aCwgem9uZS50YXJnZXQpXG5cbiAgICAgIHJldHVybiBjb250YWluc1BhdGgoY3VycmVudEZpbGVuYW1lLCB0YXJnZXRQYXRoKVxuICAgIH0pXG5cbiAgICBmdW5jdGlvbiBjaGVja0ZvclJlc3RyaWN0ZWRJbXBvcnRQYXRoKGltcG9ydFBhdGgsIG5vZGUpIHtcbiAgICAgICAgY29uc3QgYWJzb2x1dGVJbXBvcnRQYXRoID0gcmVzb2x2ZShpbXBvcnRQYXRoLCBjb250ZXh0KVxuXG4gICAgICAgIGlmICghYWJzb2x1dGVJbXBvcnRQYXRoKSB7XG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cblxuICAgICAgICBtYXRjaGluZ1pvbmVzLmZvckVhY2goKHpvbmUpID0+IHtcbiAgICAgICAgICBjb25zdCBhYnNvbHV0ZUZyb20gPSBwYXRoLnJlc29sdmUoYmFzZVBhdGgsIHpvbmUuZnJvbSlcblxuICAgICAgICAgIGlmIChjb250YWluc1BhdGgoYWJzb2x1dGVJbXBvcnRQYXRoLCBhYnNvbHV0ZUZyb20pKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICAgIG1lc3NhZ2U6IGBVbmV4cGVjdGVkIHBhdGggXCIke2ltcG9ydFBhdGh9XCIgaW1wb3J0ZWQgaW4gcmVzdHJpY3RlZCB6b25lLmAsXG4gICAgICAgICAgICB9KVxuICAgICAgICAgIH1cbiAgICAgICAgfSlcbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgSW1wb3J0RGVjbGFyYXRpb24obm9kZSkge1xuICAgICAgICBjaGVja0ZvclJlc3RyaWN0ZWRJbXBvcnRQYXRoKG5vZGUuc291cmNlLnZhbHVlLCBub2RlLnNvdXJjZSlcbiAgICAgIH0sXG4gICAgICBDYWxsRXhwcmVzc2lvbihub2RlKSB7XG4gICAgICAgIGlmIChpc1N0YXRpY1JlcXVpcmUobm9kZSkpIHtcbiAgICAgICAgICBjb25zdCBbIGZpcnN0QXJndW1lbnQgXSA9IG5vZGUuYXJndW1lbnRzXG5cbiAgICAgICAgICBjaGVja0ZvclJlc3RyaWN0ZWRJbXBvcnRQYXRoKGZpcnN0QXJndW1lbnQudmFsdWUsIGZpcnN0QXJndW1lbnQpXG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfVxuICB9LFxufVxuIl19