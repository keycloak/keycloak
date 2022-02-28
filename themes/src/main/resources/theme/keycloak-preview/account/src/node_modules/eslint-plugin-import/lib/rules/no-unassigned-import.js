'use strict';

var _path = require('path');

var _path2 = _interopRequireDefault(_path);

var _minimatch = require('minimatch');

var _minimatch2 = _interopRequireDefault(_minimatch);

var _staticRequire = require('../core/staticRequire');

var _staticRequire2 = _interopRequireDefault(_staticRequire);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

function report(context, node) {
  context.report({
    node,
    message: 'Imported module should be assigned'
  });
}

function testIsAllow(globs, filename, source) {
  if (!Array.isArray(globs)) {
    return false; // default doesn't allow any patterns
  }

  let filePath;

  if (source[0] !== '.' && source[0] !== '/') {
    // a node module
    filePath = source;
  } else {
    filePath = _path2.default.resolve(_path2.default.dirname(filename), source); // get source absolute path
  }

  return globs.find(glob => (0, _minimatch2.default)(filePath, glob) || (0, _minimatch2.default)(filePath, _path2.default.join(process.cwd(), glob))) !== undefined;
}

function create(context) {
  const options = context.options[0] || {};
  const filename = context.getFilename();
  const isAllow = source => testIsAllow(options.allow, filename, source);

  return {
    ImportDeclaration(node) {
      if (node.specifiers.length === 0 && !isAllow(node.source.value)) {
        report(context, node);
      }
    },
    ExpressionStatement(node) {
      if (node.expression.type === 'CallExpression' && (0, _staticRequire2.default)(node.expression) && !isAllow(node.expression.arguments[0].value)) {
        report(context, node.expression);
      }
    }
  };
}

module.exports = {
  create,
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-unassigned-import')
    },
    schema: [{
      'type': 'object',
      'properties': {
        'devDependencies': { 'type': ['boolean', 'array'] },
        'optionalDependencies': { 'type': ['boolean', 'array'] },
        'peerDependencies': { 'type': ['boolean', 'array'] },
        'allow': {
          'type': 'array',
          'items': {
            'type': 'string'
          }
        }
      },
      'additionalProperties': false
    }]
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLXVuYXNzaWduZWQtaW1wb3J0LmpzIl0sIm5hbWVzIjpbInJlcG9ydCIsImNvbnRleHQiLCJub2RlIiwibWVzc2FnZSIsInRlc3RJc0FsbG93IiwiZ2xvYnMiLCJmaWxlbmFtZSIsInNvdXJjZSIsIkFycmF5IiwiaXNBcnJheSIsImZpbGVQYXRoIiwicGF0aCIsInJlc29sdmUiLCJkaXJuYW1lIiwiZmluZCIsImdsb2IiLCJqb2luIiwicHJvY2VzcyIsImN3ZCIsInVuZGVmaW5lZCIsImNyZWF0ZSIsIm9wdGlvbnMiLCJnZXRGaWxlbmFtZSIsImlzQWxsb3ciLCJhbGxvdyIsIkltcG9ydERlY2xhcmF0aW9uIiwic3BlY2lmaWVycyIsImxlbmd0aCIsInZhbHVlIiwiRXhwcmVzc2lvblN0YXRlbWVudCIsImV4cHJlc3Npb24iLCJ0eXBlIiwiYXJndW1lbnRzIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwic2NoZW1hIl0sIm1hcHBpbmdzIjoiOztBQUFBOzs7O0FBQ0E7Ozs7QUFFQTs7OztBQUNBOzs7Ozs7QUFFQSxTQUFTQSxNQUFULENBQWdCQyxPQUFoQixFQUF5QkMsSUFBekIsRUFBK0I7QUFDN0JELFVBQVFELE1BQVIsQ0FBZTtBQUNiRSxRQURhO0FBRWJDLGFBQVM7QUFGSSxHQUFmO0FBSUQ7O0FBRUQsU0FBU0MsV0FBVCxDQUFxQkMsS0FBckIsRUFBNEJDLFFBQTVCLEVBQXNDQyxNQUF0QyxFQUE4QztBQUM1QyxNQUFJLENBQUNDLE1BQU1DLE9BQU4sQ0FBY0osS0FBZCxDQUFMLEVBQTJCO0FBQ3pCLFdBQU8sS0FBUCxDQUR5QixDQUNaO0FBQ2Q7O0FBRUQsTUFBSUssUUFBSjs7QUFFQSxNQUFJSCxPQUFPLENBQVAsTUFBYyxHQUFkLElBQXFCQSxPQUFPLENBQVAsTUFBYyxHQUF2QyxFQUE0QztBQUFFO0FBQzVDRyxlQUFXSCxNQUFYO0FBQ0QsR0FGRCxNQUVPO0FBQ0xHLGVBQVdDLGVBQUtDLE9BQUwsQ0FBYUQsZUFBS0UsT0FBTCxDQUFhUCxRQUFiLENBQWIsRUFBcUNDLE1BQXJDLENBQVgsQ0FESyxDQUNtRDtBQUN6RDs7QUFFRCxTQUFPRixNQUFNUyxJQUFOLENBQVdDLFFBQ2hCLHlCQUFVTCxRQUFWLEVBQW9CSyxJQUFwQixLQUNBLHlCQUFVTCxRQUFWLEVBQW9CQyxlQUFLSyxJQUFMLENBQVVDLFFBQVFDLEdBQVIsRUFBVixFQUF5QkgsSUFBekIsQ0FBcEIsQ0FGSyxNQUdBSSxTQUhQO0FBSUQ7O0FBRUQsU0FBU0MsTUFBVCxDQUFnQm5CLE9BQWhCLEVBQXlCO0FBQ3ZCLFFBQU1vQixVQUFVcEIsUUFBUW9CLE9BQVIsQ0FBZ0IsQ0FBaEIsS0FBc0IsRUFBdEM7QUFDQSxRQUFNZixXQUFXTCxRQUFRcUIsV0FBUixFQUFqQjtBQUNBLFFBQU1DLFVBQVVoQixVQUFVSCxZQUFZaUIsUUFBUUcsS0FBcEIsRUFBMkJsQixRQUEzQixFQUFxQ0MsTUFBckMsQ0FBMUI7O0FBRUEsU0FBTztBQUNMa0Isc0JBQWtCdkIsSUFBbEIsRUFBd0I7QUFDdEIsVUFBSUEsS0FBS3dCLFVBQUwsQ0FBZ0JDLE1BQWhCLEtBQTJCLENBQTNCLElBQWdDLENBQUNKLFFBQVFyQixLQUFLSyxNQUFMLENBQVlxQixLQUFwQixDQUFyQyxFQUFpRTtBQUMvRDVCLGVBQU9DLE9BQVAsRUFBZ0JDLElBQWhCO0FBQ0Q7QUFDRixLQUxJO0FBTUwyQix3QkFBb0IzQixJQUFwQixFQUEwQjtBQUN4QixVQUFJQSxLQUFLNEIsVUFBTCxDQUFnQkMsSUFBaEIsS0FBeUIsZ0JBQXpCLElBQ0YsNkJBQWdCN0IsS0FBSzRCLFVBQXJCLENBREUsSUFFRixDQUFDUCxRQUFRckIsS0FBSzRCLFVBQUwsQ0FBZ0JFLFNBQWhCLENBQTBCLENBQTFCLEVBQTZCSixLQUFyQyxDQUZILEVBRWdEO0FBQzlDNUIsZUFBT0MsT0FBUCxFQUFnQkMsS0FBSzRCLFVBQXJCO0FBQ0Q7QUFDRjtBQVpJLEdBQVA7QUFjRDs7QUFFREcsT0FBT0MsT0FBUCxHQUFpQjtBQUNmZCxRQURlO0FBRWZlLFFBQU07QUFDSkosVUFBTSxZQURGO0FBRUpLLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxzQkFBUjtBQURELEtBRkY7QUFLSkMsWUFBUSxDQUNOO0FBQ0UsY0FBUSxRQURWO0FBRUUsb0JBQWM7QUFDWiwyQkFBbUIsRUFBRSxRQUFRLENBQUMsU0FBRCxFQUFZLE9BQVosQ0FBVixFQURQO0FBRVosZ0NBQXdCLEVBQUUsUUFBUSxDQUFDLFNBQUQsRUFBWSxPQUFaLENBQVYsRUFGWjtBQUdaLDRCQUFvQixFQUFFLFFBQVEsQ0FBQyxTQUFELEVBQVksT0FBWixDQUFWLEVBSFI7QUFJWixpQkFBUztBQUNQLGtCQUFRLE9BREQ7QUFFUCxtQkFBUztBQUNQLG9CQUFRO0FBREQ7QUFGRjtBQUpHLE9BRmhCO0FBYUUsOEJBQXdCO0FBYjFCLEtBRE07QUFMSjtBQUZTLENBQWpCIiwiZmlsZSI6InJ1bGVzL25vLXVuYXNzaWduZWQtaW1wb3J0LmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHBhdGggZnJvbSAncGF0aCdcbmltcG9ydCBtaW5pbWF0Y2ggZnJvbSAnbWluaW1hdGNoJ1xuXG5pbXBvcnQgaXNTdGF0aWNSZXF1aXJlIGZyb20gJy4uL2NvcmUvc3RhdGljUmVxdWlyZSdcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5cbmZ1bmN0aW9uIHJlcG9ydChjb250ZXh0LCBub2RlKSB7XG4gIGNvbnRleHQucmVwb3J0KHtcbiAgICBub2RlLFxuICAgIG1lc3NhZ2U6ICdJbXBvcnRlZCBtb2R1bGUgc2hvdWxkIGJlIGFzc2lnbmVkJyxcbiAgfSlcbn1cblxuZnVuY3Rpb24gdGVzdElzQWxsb3coZ2xvYnMsIGZpbGVuYW1lLCBzb3VyY2UpIHtcbiAgaWYgKCFBcnJheS5pc0FycmF5KGdsb2JzKSkge1xuICAgIHJldHVybiBmYWxzZSAvLyBkZWZhdWx0IGRvZXNuJ3QgYWxsb3cgYW55IHBhdHRlcm5zXG4gIH1cblxuICBsZXQgZmlsZVBhdGhcblxuICBpZiAoc291cmNlWzBdICE9PSAnLicgJiYgc291cmNlWzBdICE9PSAnLycpIHsgLy8gYSBub2RlIG1vZHVsZVxuICAgIGZpbGVQYXRoID0gc291cmNlXG4gIH0gZWxzZSB7XG4gICAgZmlsZVBhdGggPSBwYXRoLnJlc29sdmUocGF0aC5kaXJuYW1lKGZpbGVuYW1lKSwgc291cmNlKSAvLyBnZXQgc291cmNlIGFic29sdXRlIHBhdGhcbiAgfVxuXG4gIHJldHVybiBnbG9icy5maW5kKGdsb2IgPT4gKFxuICAgIG1pbmltYXRjaChmaWxlUGF0aCwgZ2xvYikgfHxcbiAgICBtaW5pbWF0Y2goZmlsZVBhdGgsIHBhdGguam9pbihwcm9jZXNzLmN3ZCgpLCBnbG9iKSlcbiAgKSkgIT09IHVuZGVmaW5lZFxufVxuXG5mdW5jdGlvbiBjcmVhdGUoY29udGV4dCkge1xuICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9XG4gIGNvbnN0IGZpbGVuYW1lID0gY29udGV4dC5nZXRGaWxlbmFtZSgpXG4gIGNvbnN0IGlzQWxsb3cgPSBzb3VyY2UgPT4gdGVzdElzQWxsb3cob3B0aW9ucy5hbGxvdywgZmlsZW5hbWUsIHNvdXJjZSlcblxuICByZXR1cm4ge1xuICAgIEltcG9ydERlY2xhcmF0aW9uKG5vZGUpIHtcbiAgICAgIGlmIChub2RlLnNwZWNpZmllcnMubGVuZ3RoID09PSAwICYmICFpc0FsbG93KG5vZGUuc291cmNlLnZhbHVlKSkge1xuICAgICAgICByZXBvcnQoY29udGV4dCwgbm9kZSlcbiAgICAgIH1cbiAgICB9LFxuICAgIEV4cHJlc3Npb25TdGF0ZW1lbnQobm9kZSkge1xuICAgICAgaWYgKG5vZGUuZXhwcmVzc2lvbi50eXBlID09PSAnQ2FsbEV4cHJlc3Npb24nICYmXG4gICAgICAgIGlzU3RhdGljUmVxdWlyZShub2RlLmV4cHJlc3Npb24pICYmXG4gICAgICAgICFpc0FsbG93KG5vZGUuZXhwcmVzc2lvbi5hcmd1bWVudHNbMF0udmFsdWUpKSB7XG4gICAgICAgIHJlcG9ydChjb250ZXh0LCBub2RlLmV4cHJlc3Npb24pXG4gICAgICB9XG4gICAgfSxcbiAgfVxufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgY3JlYXRlLFxuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tdW5hc3NpZ25lZC1pbXBvcnQnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICAndHlwZSc6ICdvYmplY3QnLFxuICAgICAgICAncHJvcGVydGllcyc6IHtcbiAgICAgICAgICAnZGV2RGVwZW5kZW5jaWVzJzogeyAndHlwZSc6IFsnYm9vbGVhbicsICdhcnJheSddIH0sXG4gICAgICAgICAgJ29wdGlvbmFsRGVwZW5kZW5jaWVzJzogeyAndHlwZSc6IFsnYm9vbGVhbicsICdhcnJheSddIH0sXG4gICAgICAgICAgJ3BlZXJEZXBlbmRlbmNpZXMnOiB7ICd0eXBlJzogWydib29sZWFuJywgJ2FycmF5J10gfSxcbiAgICAgICAgICAnYWxsb3cnOiB7XG4gICAgICAgICAgICAndHlwZSc6ICdhcnJheScsXG4gICAgICAgICAgICAnaXRlbXMnOiB7XG4gICAgICAgICAgICAgICd0eXBlJzogJ3N0cmluZycsXG4gICAgICAgICAgICB9LFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgICdhZGRpdGlvbmFsUHJvcGVydGllcyc6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxufVxuIl19