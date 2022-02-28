'use strict';

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _has = require('has');

var _has2 = _interopRequireDefault(_has);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

/**
 * @fileoverview Rule to disallow anonymous default exports.
 * @author Duncan Beevers
 */

const defs = {
  ArrayExpression: {
    option: 'allowArray',
    description: 'If `false`, will report default export of an array',
    message: 'Assign array to a variable before exporting as module default'
  },
  ArrowFunctionExpression: {
    option: 'allowArrowFunction',
    description: 'If `false`, will report default export of an arrow function',
    message: 'Assign arrow function to a variable before exporting as module default'
  },
  CallExpression: {
    option: 'allowCallExpression',
    description: 'If `false`, will report default export of a function call',
    message: 'Assign call result to a variable before exporting as module default',
    default: true
  },
  ClassDeclaration: {
    option: 'allowAnonymousClass',
    description: 'If `false`, will report default export of an anonymous class',
    message: 'Unexpected default export of anonymous class',
    forbid: node => !node.declaration.id
  },
  FunctionDeclaration: {
    option: 'allowAnonymousFunction',
    description: 'If `false`, will report default export of an anonymous function',
    message: 'Unexpected default export of anonymous function',
    forbid: node => !node.declaration.id
  },
  Literal: {
    option: 'allowLiteral',
    description: 'If `false`, will report default export of a literal',
    message: 'Assign literal to a variable before exporting as module default'
  },
  ObjectExpression: {
    option: 'allowObject',
    description: 'If `false`, will report default export of an object expression',
    message: 'Assign object to a variable before exporting as module default'
  },
  TemplateLiteral: {
    option: 'allowLiteral',
    description: 'If `false`, will report default export of a literal',
    message: 'Assign literal to a variable before exporting as module default'
  }
};

const schemaProperties = Object.keys(defs).map(key => defs[key]).reduce((acc, def) => {
  acc[def.option] = {
    description: def.description,
    type: 'boolean'
  };

  return acc;
}, {});

const defaults = Object.keys(defs).map(key => defs[key]).reduce((acc, def) => {
  acc[def.option] = (0, _has2.default)(def, 'default') ? def.default : false;
  return acc;
}, {});

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-anonymous-default-export')
    },

    schema: [{
      type: 'object',
      properties: schemaProperties,
      'additionalProperties': false
    }]
  },

  create: function (context) {
    const options = Object.assign({}, defaults, context.options[0]);

    return {
      'ExportDefaultDeclaration': node => {
        const def = defs[node.declaration.type];

        // Recognized node type and allowed by configuration,
        //   and has no forbid check, or forbid check return value is truthy
        if (def && !options[def.option] && (!def.forbid || def.forbid(node))) {
          context.report({ node, message: def.message });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLWFub255bW91cy1kZWZhdWx0LWV4cG9ydC5qcyJdLCJuYW1lcyI6WyJkZWZzIiwiQXJyYXlFeHByZXNzaW9uIiwib3B0aW9uIiwiZGVzY3JpcHRpb24iLCJtZXNzYWdlIiwiQXJyb3dGdW5jdGlvbkV4cHJlc3Npb24iLCJDYWxsRXhwcmVzc2lvbiIsImRlZmF1bHQiLCJDbGFzc0RlY2xhcmF0aW9uIiwiZm9yYmlkIiwibm9kZSIsImRlY2xhcmF0aW9uIiwiaWQiLCJGdW5jdGlvbkRlY2xhcmF0aW9uIiwiTGl0ZXJhbCIsIk9iamVjdEV4cHJlc3Npb24iLCJUZW1wbGF0ZUxpdGVyYWwiLCJzY2hlbWFQcm9wZXJ0aWVzIiwiT2JqZWN0Iiwia2V5cyIsIm1hcCIsImtleSIsInJlZHVjZSIsImFjYyIsImRlZiIsInR5cGUiLCJkZWZhdWx0cyIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJjcmVhdGUiLCJjb250ZXh0Iiwib3B0aW9ucyIsImFzc2lnbiIsInJlcG9ydCJdLCJtYXBwaW5ncyI6Ijs7QUFLQTs7OztBQUNBOzs7Ozs7QUFOQTs7Ozs7QUFRQSxNQUFNQSxPQUFPO0FBQ1hDLG1CQUFpQjtBQUNmQyxZQUFRLFlBRE87QUFFZkMsaUJBQWEsb0RBRkU7QUFHZkMsYUFBUztBQUhNLEdBRE47QUFNWEMsMkJBQXlCO0FBQ3ZCSCxZQUFRLG9CQURlO0FBRXZCQyxpQkFBYSw2REFGVTtBQUd2QkMsYUFBUztBQUhjLEdBTmQ7QUFXWEUsa0JBQWdCO0FBQ2RKLFlBQVEscUJBRE07QUFFZEMsaUJBQWEsMkRBRkM7QUFHZEMsYUFBUyxxRUFISztBQUlkRyxhQUFTO0FBSkssR0FYTDtBQWlCWEMsb0JBQWtCO0FBQ2hCTixZQUFRLHFCQURRO0FBRWhCQyxpQkFBYSw4REFGRztBQUdoQkMsYUFBUyw4Q0FITztBQUloQkssWUFBU0MsSUFBRCxJQUFVLENBQUNBLEtBQUtDLFdBQUwsQ0FBaUJDO0FBSnBCLEdBakJQO0FBdUJYQyx1QkFBcUI7QUFDbkJYLFlBQVEsd0JBRFc7QUFFbkJDLGlCQUFhLGlFQUZNO0FBR25CQyxhQUFTLGlEQUhVO0FBSW5CSyxZQUFTQyxJQUFELElBQVUsQ0FBQ0EsS0FBS0MsV0FBTCxDQUFpQkM7QUFKakIsR0F2QlY7QUE2QlhFLFdBQVM7QUFDUFosWUFBUSxjQUREO0FBRVBDLGlCQUFhLHFEQUZOO0FBR1BDLGFBQVM7QUFIRixHQTdCRTtBQWtDWFcsb0JBQWtCO0FBQ2hCYixZQUFRLGFBRFE7QUFFaEJDLGlCQUFhLGdFQUZHO0FBR2hCQyxhQUFTO0FBSE8sR0FsQ1A7QUF1Q1hZLG1CQUFpQjtBQUNmZCxZQUFRLGNBRE87QUFFZkMsaUJBQWEscURBRkU7QUFHZkMsYUFBUztBQUhNO0FBdkNOLENBQWI7O0FBOENBLE1BQU1hLG1CQUFtQkMsT0FBT0MsSUFBUCxDQUFZbkIsSUFBWixFQUN0Qm9CLEdBRHNCLENBQ2pCQyxHQUFELElBQVNyQixLQUFLcUIsR0FBTCxDQURTLEVBRXRCQyxNQUZzQixDQUVmLENBQUNDLEdBQUQsRUFBTUMsR0FBTixLQUFjO0FBQ3BCRCxNQUFJQyxJQUFJdEIsTUFBUixJQUFrQjtBQUNoQkMsaUJBQWFxQixJQUFJckIsV0FERDtBQUVoQnNCLFVBQU07QUFGVSxHQUFsQjs7QUFLQSxTQUFPRixHQUFQO0FBQ0QsQ0FUc0IsRUFTcEIsRUFUb0IsQ0FBekI7O0FBV0EsTUFBTUcsV0FBV1IsT0FBT0MsSUFBUCxDQUFZbkIsSUFBWixFQUNkb0IsR0FEYyxDQUNUQyxHQUFELElBQVNyQixLQUFLcUIsR0FBTCxDQURDLEVBRWRDLE1BRmMsQ0FFUCxDQUFDQyxHQUFELEVBQU1DLEdBQU4sS0FBYztBQUNwQkQsTUFBSUMsSUFBSXRCLE1BQVIsSUFBa0IsbUJBQUlzQixHQUFKLEVBQVMsU0FBVCxJQUFzQkEsSUFBSWpCLE9BQTFCLEdBQW9DLEtBQXREO0FBQ0EsU0FBT2dCLEdBQVA7QUFDRCxDQUxjLEVBS1osRUFMWSxDQUFqQjs7QUFPQUksT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pKLFVBQU0sWUFERjtBQUVKSyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsNkJBQVI7QUFERCxLQUZGOztBQU1KQyxZQUFRLENBQ047QUFDRVAsWUFBTSxRQURSO0FBRUVRLGtCQUFZaEIsZ0JBRmQ7QUFHRSw4QkFBd0I7QUFIMUIsS0FETTtBQU5KLEdBRFM7O0FBZ0JmaUIsVUFBUSxVQUFVQyxPQUFWLEVBQW1CO0FBQ3pCLFVBQU1DLFVBQVVsQixPQUFPbUIsTUFBUCxDQUFjLEVBQWQsRUFBa0JYLFFBQWxCLEVBQTRCUyxRQUFRQyxPQUFSLENBQWdCLENBQWhCLENBQTVCLENBQWhCOztBQUVBLFdBQU87QUFDTCxrQ0FBNkIxQixJQUFELElBQVU7QUFDcEMsY0FBTWMsTUFBTXhCLEtBQUtVLEtBQUtDLFdBQUwsQ0FBaUJjLElBQXRCLENBQVo7O0FBRUE7QUFDQTtBQUNBLFlBQUlELE9BQU8sQ0FBQ1ksUUFBUVosSUFBSXRCLE1BQVosQ0FBUixLQUFnQyxDQUFDc0IsSUFBSWYsTUFBTCxJQUFlZSxJQUFJZixNQUFKLENBQVdDLElBQVgsQ0FBL0MsQ0FBSixFQUFzRTtBQUNwRXlCLGtCQUFRRyxNQUFSLENBQWUsRUFBRTVCLElBQUYsRUFBUU4sU0FBU29CLElBQUlwQixPQUFyQixFQUFmO0FBQ0Q7QUFDRjtBQVRJLEtBQVA7QUFXRDtBQTlCYyxDQUFqQiIsImZpbGUiOiJydWxlcy9uby1hbm9ueW1vdXMtZGVmYXVsdC1leHBvcnQuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlb3ZlcnZpZXcgUnVsZSB0byBkaXNhbGxvdyBhbm9ueW1vdXMgZGVmYXVsdCBleHBvcnRzLlxuICogQGF1dGhvciBEdW5jYW4gQmVldmVyc1xuICovXG5cbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnXG5pbXBvcnQgaGFzIGZyb20gJ2hhcydcblxuY29uc3QgZGVmcyA9IHtcbiAgQXJyYXlFeHByZXNzaW9uOiB7XG4gICAgb3B0aW9uOiAnYWxsb3dBcnJheScsXG4gICAgZGVzY3JpcHRpb246ICdJZiBgZmFsc2VgLCB3aWxsIHJlcG9ydCBkZWZhdWx0IGV4cG9ydCBvZiBhbiBhcnJheScsXG4gICAgbWVzc2FnZTogJ0Fzc2lnbiBhcnJheSB0byBhIHZhcmlhYmxlIGJlZm9yZSBleHBvcnRpbmcgYXMgbW9kdWxlIGRlZmF1bHQnLFxuICB9LFxuICBBcnJvd0Z1bmN0aW9uRXhwcmVzc2lvbjoge1xuICAgIG9wdGlvbjogJ2FsbG93QXJyb3dGdW5jdGlvbicsXG4gICAgZGVzY3JpcHRpb246ICdJZiBgZmFsc2VgLCB3aWxsIHJlcG9ydCBkZWZhdWx0IGV4cG9ydCBvZiBhbiBhcnJvdyBmdW5jdGlvbicsXG4gICAgbWVzc2FnZTogJ0Fzc2lnbiBhcnJvdyBmdW5jdGlvbiB0byBhIHZhcmlhYmxlIGJlZm9yZSBleHBvcnRpbmcgYXMgbW9kdWxlIGRlZmF1bHQnLFxuICB9LFxuICBDYWxsRXhwcmVzc2lvbjoge1xuICAgIG9wdGlvbjogJ2FsbG93Q2FsbEV4cHJlc3Npb24nLFxuICAgIGRlc2NyaXB0aW9uOiAnSWYgYGZhbHNlYCwgd2lsbCByZXBvcnQgZGVmYXVsdCBleHBvcnQgb2YgYSBmdW5jdGlvbiBjYWxsJyxcbiAgICBtZXNzYWdlOiAnQXNzaWduIGNhbGwgcmVzdWx0IHRvIGEgdmFyaWFibGUgYmVmb3JlIGV4cG9ydGluZyBhcyBtb2R1bGUgZGVmYXVsdCcsXG4gICAgZGVmYXVsdDogdHJ1ZSxcbiAgfSxcbiAgQ2xhc3NEZWNsYXJhdGlvbjoge1xuICAgIG9wdGlvbjogJ2FsbG93QW5vbnltb3VzQ2xhc3MnLFxuICAgIGRlc2NyaXB0aW9uOiAnSWYgYGZhbHNlYCwgd2lsbCByZXBvcnQgZGVmYXVsdCBleHBvcnQgb2YgYW4gYW5vbnltb3VzIGNsYXNzJyxcbiAgICBtZXNzYWdlOiAnVW5leHBlY3RlZCBkZWZhdWx0IGV4cG9ydCBvZiBhbm9ueW1vdXMgY2xhc3MnLFxuICAgIGZvcmJpZDogKG5vZGUpID0+ICFub2RlLmRlY2xhcmF0aW9uLmlkLFxuICB9LFxuICBGdW5jdGlvbkRlY2xhcmF0aW9uOiB7XG4gICAgb3B0aW9uOiAnYWxsb3dBbm9ueW1vdXNGdW5jdGlvbicsXG4gICAgZGVzY3JpcHRpb246ICdJZiBgZmFsc2VgLCB3aWxsIHJlcG9ydCBkZWZhdWx0IGV4cG9ydCBvZiBhbiBhbm9ueW1vdXMgZnVuY3Rpb24nLFxuICAgIG1lc3NhZ2U6ICdVbmV4cGVjdGVkIGRlZmF1bHQgZXhwb3J0IG9mIGFub255bW91cyBmdW5jdGlvbicsXG4gICAgZm9yYmlkOiAobm9kZSkgPT4gIW5vZGUuZGVjbGFyYXRpb24uaWQsXG4gIH0sXG4gIExpdGVyYWw6IHtcbiAgICBvcHRpb246ICdhbGxvd0xpdGVyYWwnLFxuICAgIGRlc2NyaXB0aW9uOiAnSWYgYGZhbHNlYCwgd2lsbCByZXBvcnQgZGVmYXVsdCBleHBvcnQgb2YgYSBsaXRlcmFsJyxcbiAgICBtZXNzYWdlOiAnQXNzaWduIGxpdGVyYWwgdG8gYSB2YXJpYWJsZSBiZWZvcmUgZXhwb3J0aW5nIGFzIG1vZHVsZSBkZWZhdWx0JyxcbiAgfSxcbiAgT2JqZWN0RXhwcmVzc2lvbjoge1xuICAgIG9wdGlvbjogJ2FsbG93T2JqZWN0JyxcbiAgICBkZXNjcmlwdGlvbjogJ0lmIGBmYWxzZWAsIHdpbGwgcmVwb3J0IGRlZmF1bHQgZXhwb3J0IG9mIGFuIG9iamVjdCBleHByZXNzaW9uJyxcbiAgICBtZXNzYWdlOiAnQXNzaWduIG9iamVjdCB0byBhIHZhcmlhYmxlIGJlZm9yZSBleHBvcnRpbmcgYXMgbW9kdWxlIGRlZmF1bHQnLFxuICB9LFxuICBUZW1wbGF0ZUxpdGVyYWw6IHtcbiAgICBvcHRpb246ICdhbGxvd0xpdGVyYWwnLFxuICAgIGRlc2NyaXB0aW9uOiAnSWYgYGZhbHNlYCwgd2lsbCByZXBvcnQgZGVmYXVsdCBleHBvcnQgb2YgYSBsaXRlcmFsJyxcbiAgICBtZXNzYWdlOiAnQXNzaWduIGxpdGVyYWwgdG8gYSB2YXJpYWJsZSBiZWZvcmUgZXhwb3J0aW5nIGFzIG1vZHVsZSBkZWZhdWx0JyxcbiAgfSxcbn1cblxuY29uc3Qgc2NoZW1hUHJvcGVydGllcyA9IE9iamVjdC5rZXlzKGRlZnMpXG4gIC5tYXAoKGtleSkgPT4gZGVmc1trZXldKVxuICAucmVkdWNlKChhY2MsIGRlZikgPT4ge1xuICAgIGFjY1tkZWYub3B0aW9uXSA9IHtcbiAgICAgIGRlc2NyaXB0aW9uOiBkZWYuZGVzY3JpcHRpb24sXG4gICAgICB0eXBlOiAnYm9vbGVhbicsXG4gICAgfVxuXG4gICAgcmV0dXJuIGFjY1xuICB9LCB7fSlcblxuY29uc3QgZGVmYXVsdHMgPSBPYmplY3Qua2V5cyhkZWZzKVxuICAubWFwKChrZXkpID0+IGRlZnNba2V5XSlcbiAgLnJlZHVjZSgoYWNjLCBkZWYpID0+IHtcbiAgICBhY2NbZGVmLm9wdGlvbl0gPSBoYXMoZGVmLCAnZGVmYXVsdCcpID8gZGVmLmRlZmF1bHQgOiBmYWxzZVxuICAgIHJldHVybiBhY2NcbiAgfSwge30pXG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnbm8tYW5vbnltb3VzLWRlZmF1bHQtZXhwb3J0JyksXG4gICAgfSxcblxuICAgIHNjaGVtYTogW1xuICAgICAge1xuICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgcHJvcGVydGllczogc2NoZW1hUHJvcGVydGllcyxcbiAgICAgICAgJ2FkZGl0aW9uYWxQcm9wZXJ0aWVzJzogZmFsc2UsXG4gICAgICB9LFxuICAgIF0sXG4gIH0sXG5cbiAgY3JlYXRlOiBmdW5jdGlvbiAoY29udGV4dCkge1xuICAgIGNvbnN0IG9wdGlvbnMgPSBPYmplY3QuYXNzaWduKHt9LCBkZWZhdWx0cywgY29udGV4dC5vcHRpb25zWzBdKVxuXG4gICAgcmV0dXJuIHtcbiAgICAgICdFeHBvcnREZWZhdWx0RGVjbGFyYXRpb24nOiAobm9kZSkgPT4ge1xuICAgICAgICBjb25zdCBkZWYgPSBkZWZzW25vZGUuZGVjbGFyYXRpb24udHlwZV1cblxuICAgICAgICAvLyBSZWNvZ25pemVkIG5vZGUgdHlwZSBhbmQgYWxsb3dlZCBieSBjb25maWd1cmF0aW9uLFxuICAgICAgICAvLyAgIGFuZCBoYXMgbm8gZm9yYmlkIGNoZWNrLCBvciBmb3JiaWQgY2hlY2sgcmV0dXJuIHZhbHVlIGlzIHRydXRoeVxuICAgICAgICBpZiAoZGVmICYmICFvcHRpb25zW2RlZi5vcHRpb25dICYmICghZGVmLmZvcmJpZCB8fCBkZWYuZm9yYmlkKG5vZGUpKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogZGVmLm1lc3NhZ2UgfSlcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=