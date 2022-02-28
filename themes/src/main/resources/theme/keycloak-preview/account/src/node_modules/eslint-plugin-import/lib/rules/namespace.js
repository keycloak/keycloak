'use strict';

var _declaredScope = require('eslint-module-utils/declaredScope');

var _declaredScope2 = _interopRequireDefault(_declaredScope);

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _importDeclaration = require('../importDeclaration');

var _importDeclaration2 = _interopRequireDefault(_importDeclaration);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2.default)('namespace')
    },

    schema: [{
      'type': 'object',
      'properties': {
        'allowComputed': {
          'description': 'If `false`, will report computed (and thus, un-lintable) references ' + 'to namespace members.',
          'type': 'boolean',
          'default': false
        }
      },
      'additionalProperties': false
    }]
  },

  create: function namespaceRule(context) {

    // read options
    var _ref = context.options[0] || {},
        _ref$allowComputed = _ref.allowComputed;

    const allowComputed = _ref$allowComputed === undefined ? false : _ref$allowComputed;


    const namespaces = new Map();

    function makeMessage(last, namepath) {
      return `'${last.name}' not found in` + (namepath.length > 1 ? ' deeply ' : ' ') + `imported namespace '${namepath.join('.')}'.`;
    }

    return {

      // pick up all imports at body entry time, to properly respect hoisting
      Program: function (_ref2) {
        let body = _ref2.body;

        function processBodyStatement(declaration) {
          if (declaration.type !== 'ImportDeclaration') return;

          if (declaration.specifiers.length === 0) return;

          const imports = _ExportMap2.default.get(declaration.source.value, context);
          if (imports == null) return null;

          if (imports.errors.length) {
            imports.reportErrors(context, declaration);
            return;
          }

          for (const specifier of declaration.specifiers) {
            switch (specifier.type) {
              case 'ImportNamespaceSpecifier':
                if (!imports.size) {
                  context.report(specifier, `No exported names found in module '${declaration.source.value}'.`);
                }
                namespaces.set(specifier.local.name, imports);
                break;
              case 'ImportDefaultSpecifier':
              case 'ImportSpecifier':
                {
                  const meta = imports.get(
                  // default to 'default' for default http://i.imgur.com/nj6qAWy.jpg
                  specifier.imported ? specifier.imported.name : 'default');
                  if (!meta || !meta.namespace) break;
                  namespaces.set(specifier.local.name, meta.namespace);
                  break;
                }
            }
          }
        }
        body.forEach(processBodyStatement);
      },

      // same as above, but does not add names to local map
      ExportNamespaceSpecifier: function (namespace) {
        var declaration = (0, _importDeclaration2.default)(context);

        var imports = _ExportMap2.default.get(declaration.source.value, context);
        if (imports == null) return null;

        if (imports.errors.length) {
          imports.reportErrors(context, declaration);
          return;
        }

        if (!imports.size) {
          context.report(namespace, `No exported names found in module '${declaration.source.value}'.`);
        }
      },

      // todo: check for possible redefinition

      MemberExpression: function (dereference) {
        if (dereference.object.type !== 'Identifier') return;
        if (!namespaces.has(dereference.object.name)) return;

        if (dereference.parent.type === 'AssignmentExpression' && dereference.parent.left === dereference) {
          context.report(dereference.parent, `Assignment to member of namespace '${dereference.object.name}'.`);
        }

        // go deep
        var namespace = namespaces.get(dereference.object.name);
        var namepath = [dereference.object.name];
        // while property is namespace and parent is member expression, keep validating
        while (namespace instanceof _ExportMap2.default && dereference.type === 'MemberExpression') {

          if (dereference.computed) {
            if (!allowComputed) {
              context.report(dereference.property, 'Unable to validate computed reference to imported namespace \'' + dereference.object.name + '\'.');
            }
            return;
          }

          if (!namespace.has(dereference.property.name)) {
            context.report(dereference.property, makeMessage(dereference.property, namepath));
            break;
          }

          const exported = namespace.get(dereference.property.name);
          if (exported == null) return;

          // stash and pop
          namepath.push(dereference.property.name);
          namespace = exported.namespace;
          dereference = dereference.parent;
        }
      },

      VariableDeclarator: function (_ref3) {
        let id = _ref3.id,
            init = _ref3.init;

        if (init == null) return;
        if (init.type !== 'Identifier') return;
        if (!namespaces.has(init.name)) return;

        // check for redefinition in intermediate scopes
        if ((0, _declaredScope2.default)(context, init.name) !== 'module') return;

        // DFS traverse child namespaces
        function testKey(pattern, namespace) {
          let path = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [init.name];

          if (!(namespace instanceof _ExportMap2.default)) return;

          if (pattern.type !== 'ObjectPattern') return;

          for (const property of pattern.properties) {
            if (property.type === 'ExperimentalRestProperty' || property.type === 'RestElement' || !property.key) {
              continue;
            }

            if (property.key.type !== 'Identifier') {
              context.report({
                node: property,
                message: 'Only destructure top-level names.'
              });
              continue;
            }

            if (!namespace.has(property.key.name)) {
              context.report({
                node: property,
                message: makeMessage(property.key, path)
              });
              continue;
            }

            path.push(property.key.name);
            testKey(property.value, namespace.get(property.key.name).namespace, path);
            path.pop();
          }
        }

        testKey(id, namespaces.get(init.name));
      },

      JSXMemberExpression: function (_ref4) {
        let object = _ref4.object,
            property = _ref4.property;

        if (!namespaces.has(object.name)) return;
        var namespace = namespaces.get(object.name);
        if (!namespace.has(property.name)) {
          context.report({
            node: property,
            message: makeMessage(property, [object.name])
          });
        }
      }
    };
  }
};
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25hbWVzcGFjZS5qcyJdLCJuYW1lcyI6WyJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsInR5cGUiLCJkb2NzIiwidXJsIiwic2NoZW1hIiwiY3JlYXRlIiwibmFtZXNwYWNlUnVsZSIsImNvbnRleHQiLCJvcHRpb25zIiwiYWxsb3dDb21wdXRlZCIsIm5hbWVzcGFjZXMiLCJNYXAiLCJtYWtlTWVzc2FnZSIsImxhc3QiLCJuYW1lcGF0aCIsIm5hbWUiLCJsZW5ndGgiLCJqb2luIiwiUHJvZ3JhbSIsImJvZHkiLCJwcm9jZXNzQm9keVN0YXRlbWVudCIsImRlY2xhcmF0aW9uIiwic3BlY2lmaWVycyIsImltcG9ydHMiLCJFeHBvcnRzIiwiZ2V0Iiwic291cmNlIiwidmFsdWUiLCJlcnJvcnMiLCJyZXBvcnRFcnJvcnMiLCJzcGVjaWZpZXIiLCJzaXplIiwicmVwb3J0Iiwic2V0IiwibG9jYWwiLCJpbXBvcnRlZCIsIm5hbWVzcGFjZSIsImZvckVhY2giLCJFeHBvcnROYW1lc3BhY2VTcGVjaWZpZXIiLCJNZW1iZXJFeHByZXNzaW9uIiwiZGVyZWZlcmVuY2UiLCJvYmplY3QiLCJoYXMiLCJwYXJlbnQiLCJsZWZ0IiwiY29tcHV0ZWQiLCJwcm9wZXJ0eSIsImV4cG9ydGVkIiwicHVzaCIsIlZhcmlhYmxlRGVjbGFyYXRvciIsImlkIiwiaW5pdCIsInRlc3RLZXkiLCJwYXR0ZXJuIiwicGF0aCIsInByb3BlcnRpZXMiLCJrZXkiLCJub2RlIiwibWVzc2FnZSIsInBvcCIsIkpTWE1lbWJlckV4cHJlc3Npb24iXSwibWFwcGluZ3MiOiI7O0FBQUE7Ozs7QUFDQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBQSxPQUFPQyxPQUFQLEdBQWlCO0FBQ2ZDLFFBQU07QUFDSkMsVUFBTSxTQURGO0FBRUpDLFVBQU07QUFDSkMsV0FBSyx1QkFBUSxXQUFSO0FBREQsS0FGRjs7QUFNSkMsWUFBUSxDQUNOO0FBQ0UsY0FBUSxRQURWO0FBRUUsb0JBQWM7QUFDWix5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QU1YLFFBRE87QUFFYlkscUJBQVM5QyxZQUFZa0MsUUFBWixFQUFzQixDQUFDTCxPQUFPMUIsSUFBUixDQUF0QjtBQUZJLFdBQWY7QUFJRDtBQUNIO0FBbEtJLEtBQVA7QUFvS0Q7QUEzTWMsQ0FBakIiLCJmaWxlIjoicnVsZXMvbmFtZXNwYWNlLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRlY2xhcmVkU2NvcGUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9kZWNsYXJlZFNjb3BlJ1xuaW1wb3J0IEV4cG9ydHMgZnJvbSAnLi4vRXhwb3J0TWFwJ1xuaW1wb3J0IGltcG9ydERlY2xhcmF0aW9uIGZyb20gJy4uL2ltcG9ydERlY2xhcmF0aW9uJ1xuaW1wb3J0IGRvY3NVcmwgZnJvbSAnLi4vZG9jc1VybCdcblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduYW1lc3BhY2UnKSxcbiAgICB9LFxuXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgICd0eXBlJzogJ29iamVjdCcsXG4gICAgICAgICdwcm9wZXJ0aWVzJzoge1xuICAgICAgICAgICdhbGxvd0NvbXB1dGVkJzoge1xuICAgICAgICAgICAgJ2Rlc2NyaXB0aW9uJzpcbiAgICAgICAgICAgICAgJ0lmIGBmYWxzZWAsIHdpbGwgcmVwb3J0IGNvbXB1dGVkIChhbmQgdGh1cywgdW4tbGludGFibGUpIHJlZmVyZW5jZXMgJyArXG4gICAgICAgICAgICAgICd0byBuYW1lc3BhY2UgbWVtYmVycy4nLFxuICAgICAgICAgICAgJ3R5cGUnOiAnYm9vbGVhbicsXG4gICAgICAgICAgICAnZGVmYXVsdCc6IGZhbHNlLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgICdhZGRpdGlvbmFsUHJvcGVydGllcyc6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gbmFtZXNwYWNlUnVsZShjb250ZXh0KSB7XG5cbiAgICAvLyByZWFkIG9wdGlvbnNcbiAgICBjb25zdCB7XG4gICAgICBhbGxvd0NvbXB1dGVkID0gZmFsc2UsXG4gICAgfSA9IGNvbnRleHQub3B0aW9uc1swXSB8fCB7fVxuXG4gICAgY29uc3QgbmFtZXNwYWNlcyA9IG5ldyBNYXAoKVxuXG4gICAgZnVuY3Rpb24gbWFrZU1lc3NhZ2UobGFzdCwgbmFtZXBhdGgpIHtcbiAgICAgICByZXR1cm4gYCcke2xhc3QubmFtZX0nIG5vdCBmb3VuZCBpbmAgK1xuICAgICAgICAgICAgICAobmFtZXBhdGgubGVuZ3RoID4gMSA/ICcgZGVlcGx5ICcgOiAnICcpICtcbiAgICAgICAgICAgICAgYGltcG9ydGVkIG5hbWVzcGFjZSAnJHtuYW1lcGF0aC5qb2luKCcuJyl9Jy5gXG4gICAgfVxuXG4gICAgcmV0dXJuIHtcblxuICAgICAgLy8gcGljayB1cCBhbGwgaW1wb3J0cyBhdCBib2R5IGVudHJ5IHRpbWUsIHRvIHByb3Blcmx5IHJlc3BlY3QgaG9pc3RpbmdcbiAgICAgIFByb2dyYW06IGZ1bmN0aW9uICh7IGJvZHkgfSkge1xuICAgICAgICBmdW5jdGlvbiBwcm9jZXNzQm9keVN0YXRlbWVudChkZWNsYXJhdGlvbikge1xuICAgICAgICAgIGlmIChkZWNsYXJhdGlvbi50eXBlICE9PSAnSW1wb3J0RGVjbGFyYXRpb24nKSByZXR1cm5cblxuICAgICAgICAgIGlmIChkZWNsYXJhdGlvbi5zcGVjaWZpZXJzLmxlbmd0aCA9PT0gMCkgcmV0dXJuXG5cbiAgICAgICAgICBjb25zdCBpbXBvcnRzID0gRXhwb3J0cy5nZXQoZGVjbGFyYXRpb24uc291cmNlLnZhbHVlLCBjb250ZXh0KVxuICAgICAgICAgIGlmIChpbXBvcnRzID09IG51bGwpIHJldHVybiBudWxsXG5cbiAgICAgICAgICBpZiAoaW1wb3J0cy5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgICAgICBpbXBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBkZWNsYXJhdGlvbilcbiAgICAgICAgICAgIHJldHVyblxuICAgICAgICAgIH1cblxuICAgICAgICAgIGZvciAoY29uc3Qgc3BlY2lmaWVyIG9mIGRlY2xhcmF0aW9uLnNwZWNpZmllcnMpIHtcbiAgICAgICAgICAgIHN3aXRjaCAoc3BlY2lmaWVyLnR5cGUpIHtcbiAgICAgICAgICAgICAgY2FzZSAnSW1wb3J0TmFtZXNwYWNlU3BlY2lmaWVyJzpcbiAgICAgICAgICAgICAgICBpZiAoIWltcG9ydHMuc2l6ZSkge1xuICAgICAgICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoc3BlY2lmaWVyLFxuICAgICAgICAgICAgICAgICAgICBgTm8gZXhwb3J0ZWQgbmFtZXMgZm91bmQgaW4gbW9kdWxlICcke2RlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZX0nLmApXG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIG5hbWVzcGFjZXMuc2V0KHNwZWNpZmllci5sb2NhbC5uYW1lLCBpbXBvcnRzKVxuICAgICAgICAgICAgICAgIGJyZWFrXG4gICAgICAgICAgICAgIGNhc2UgJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInOlxuICAgICAgICAgICAgICBjYXNlICdJbXBvcnRTcGVjaWZpZXInOiB7XG4gICAgICAgICAgICAgICAgY29uc3QgbWV0YSA9IGltcG9ydHMuZ2V0KFxuICAgICAgICAgICAgICAgICAgLy8gZGVmYXVsdCB0byAnZGVmYXVsdCcgZm9yIGRlZmF1bHQgaHR0cDovL2kuaW1ndXIuY29tL25qNnFBV3kuanBnXG4gICAgICAgICAgICAgICAgICBzcGVjaWZpZXIuaW1wb3J0ZWQgPyBzcGVjaWZpZXIuaW1wb3J0ZWQubmFtZSA6ICdkZWZhdWx0JylcbiAgICAgICAgICAgICAgICBpZiAoIW1ldGEgfHwgIW1ldGEubmFtZXNwYWNlKSBicmVha1xuICAgICAgICAgICAgICAgIG5hbWVzcGFjZXMuc2V0KHNwZWNpZmllci5sb2NhbC5uYW1lLCBtZXRhLm5hbWVzcGFjZSlcbiAgICAgICAgICAgICAgICBicmVha1xuICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9XG4gICAgICAgICAgfVxuICAgICAgICB9XG4gICAgICAgIGJvZHkuZm9yRWFjaChwcm9jZXNzQm9keVN0YXRlbWVudClcbiAgICAgIH0sXG5cbiAgICAgIC8vIHNhbWUgYXMgYWJvdmUsIGJ1dCBkb2VzIG5vdCBhZGQgbmFtZXMgdG8gbG9jYWwgbWFwXG4gICAgICBFeHBvcnROYW1lc3BhY2VTcGVjaWZpZXI6IGZ1bmN0aW9uIChuYW1lc3BhY2UpIHtcbiAgICAgICAgdmFyIGRlY2xhcmF0aW9uID0gaW1wb3J0RGVjbGFyYXRpb24oY29udGV4dClcblxuICAgICAgICB2YXIgaW1wb3J0cyA9IEV4cG9ydHMuZ2V0KGRlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZSwgY29udGV4dClcbiAgICAgICAgaWYgKGltcG9ydHMgPT0gbnVsbCkgcmV0dXJuIG51bGxcblxuICAgICAgICBpZiAoaW1wb3J0cy5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgZGVjbGFyYXRpb24pXG4gICAgICAgICAgcmV0dXJuXG4gICAgICAgIH1cblxuICAgICAgICBpZiAoIWltcG9ydHMuc2l6ZSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KG5hbWVzcGFjZSxcbiAgICAgICAgICAgIGBObyBleHBvcnRlZCBuYW1lcyBmb3VuZCBpbiBtb2R1bGUgJyR7ZGVjbGFyYXRpb24uc291cmNlLnZhbHVlfScuYClcbiAgICAgICAgfVxuICAgICAgfSxcblxuICAgICAgLy8gdG9kbzogY2hlY2sgZm9yIHBvc3NpYmxlIHJlZGVmaW5pdGlvblxuXG4gICAgICBNZW1iZXJFeHByZXNzaW9uOiBmdW5jdGlvbiAoZGVyZWZlcmVuY2UpIHtcbiAgICAgICAgaWYgKGRlcmVmZXJlbmNlLm9iamVjdC50eXBlICE9PSAnSWRlbnRpZmllcicpIHJldHVyblxuICAgICAgICBpZiAoIW5hbWVzcGFjZXMuaGFzKGRlcmVmZXJlbmNlLm9iamVjdC5uYW1lKSkgcmV0dXJuXG5cbiAgICAgICAgaWYgKGRlcmVmZXJlbmNlLnBhcmVudC50eXBlID09PSAnQXNzaWdubWVudEV4cHJlc3Npb24nICYmXG4gICAgICAgICAgICBkZXJlZmVyZW5jZS5wYXJlbnQubGVmdCA9PT0gZGVyZWZlcmVuY2UpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KGRlcmVmZXJlbmNlLnBhcmVudCxcbiAgICAgICAgICAgICAgICBgQXNzaWdubWVudCB0byBtZW1iZXIgb2YgbmFtZXNwYWNlICcke2RlcmVmZXJlbmNlLm9iamVjdC5uYW1lfScuYClcbiAgICAgICAgfVxuXG4gICAgICAgIC8vIGdvIGRlZXBcbiAgICAgICAgdmFyIG5hbWVzcGFjZSA9IG5hbWVzcGFjZXMuZ2V0KGRlcmVmZXJlbmNlLm9iamVjdC5uYW1lKVxuICAgICAgICB2YXIgbmFtZXBhdGggPSBbZGVyZWZlcmVuY2Uub2JqZWN0Lm5hbWVdXG4gICAgICAgIC8vIHdoaWxlIHByb3BlcnR5IGlzIG5hbWVzcGFjZSBhbmQgcGFyZW50IGlzIG1lbWJlciBleHByZXNzaW9uLCBrZWVwIHZhbGlkYXRpbmdcbiAgICAgICAgd2hpbGUgKG5hbWVzcGFjZSBpbnN0YW5jZW9mIEV4cG9ydHMgJiZcbiAgICAgICAgICAgICAgIGRlcmVmZXJlbmNlLnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJykge1xuXG4gICAgICAgICAgaWYgKGRlcmVmZXJlbmNlLmNvbXB1dGVkKSB7XG4gICAgICAgICAgICBpZiAoIWFsbG93Q29tcHV0ZWQpIHtcbiAgICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoZGVyZWZlcmVuY2UucHJvcGVydHksXG4gICAgICAgICAgICAgICAgJ1VuYWJsZSB0byB2YWxpZGF0ZSBjb21wdXRlZCByZWZlcmVuY2UgdG8gaW1wb3J0ZWQgbmFtZXNwYWNlIFxcJycgK1xuICAgICAgICAgICAgICAgIGRlcmVmZXJlbmNlLm9iamVjdC5uYW1lICsgJ1xcJy4nKVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgcmV0dXJuXG4gICAgICAgICAgfVxuXG4gICAgICAgICAgaWYgKCFuYW1lc3BhY2UuaGFzKGRlcmVmZXJlbmNlLnByb3BlcnR5Lm5hbWUpKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydChcbiAgICAgICAgICAgICAgZGVyZWZlcmVuY2UucHJvcGVydHksXG4gICAgICAgICAgICAgIG1ha2VNZXNzYWdlKGRlcmVmZXJlbmNlLnByb3BlcnR5LCBuYW1lcGF0aCkpXG4gICAgICAgICAgICBicmVha1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGNvbnN0IGV4cG9ydGVkID0gbmFtZXNwYWNlLmdldChkZXJlZmVyZW5jZS5wcm9wZXJ0eS5uYW1lKVxuICAgICAgICAgIGlmIChleHBvcnRlZCA9PSBudWxsKSByZXR1cm5cblxuICAgICAgICAgIC8vIHN0YXNoIGFuZCBwb3BcbiAgICAgICAgICBuYW1lcGF0aC5wdXNoKGRlcmVmZXJlbmNlLnByb3BlcnR5Lm5hbWUpXG4gICAgICAgICAgbmFtZXNwYWNlID0gZXhwb3J0ZWQubmFtZXNwYWNlXG4gICAgICAgICAgZGVyZWZlcmVuY2UgPSBkZXJlZmVyZW5jZS5wYXJlbnRcbiAgICAgICAgfVxuXG4gICAgICB9LFxuXG4gICAgICBWYXJpYWJsZURlY2xhcmF0b3I6IGZ1bmN0aW9uICh7IGlkLCBpbml0IH0pIHtcbiAgICAgICAgaWYgKGluaXQgPT0gbnVsbCkgcmV0dXJuXG4gICAgICAgIGlmIChpbml0LnR5cGUgIT09ICdJZGVudGlmaWVyJykgcmV0dXJuXG4gICAgICAgIGlmICghbmFtZXNwYWNlcy5oYXMoaW5pdC5uYW1lKSkgcmV0dXJuXG5cbiAgICAgICAgLy8gY2hlY2sgZm9yIHJlZGVmaW5pdGlvbiBpbiBpbnRlcm1lZGlhdGUgc2NvcGVzXG4gICAgICAgIGlmIChkZWNsYXJlZFNjb3BlKGNvbnRleHQsIGluaXQubmFtZSkgIT09ICdtb2R1bGUnKSByZXR1cm5cblxuICAgICAgICAvLyBERlMgdHJhdmVyc2UgY2hpbGQgbmFtZXNwYWNlc1xuICAgICAgICBmdW5jdGlvbiB0ZXN0S2V5KHBhdHRlcm4sIG5hbWVzcGFjZSwgcGF0aCA9IFtpbml0Lm5hbWVdKSB7XG4gICAgICAgICAgaWYgKCEobmFtZXNwYWNlIGluc3RhbmNlb2YgRXhwb3J0cykpIHJldHVyblxuXG4gICAgICAgICAgaWYgKHBhdHRlcm4udHlwZSAhPT0gJ09iamVjdFBhdHRlcm4nKSByZXR1cm5cblxuICAgICAgICAgIGZvciAoY29uc3QgcHJvcGVydHkgb2YgcGF0dGVybi5wcm9wZXJ0aWVzKSB7XG4gICAgICAgICAgICBpZiAoXG4gICAgICAgICAgICAgIHByb3BlcnR5LnR5cGUgPT09ICdFeHBlcmltZW50YWxSZXN0UHJvcGVydHknXG4gICAgICAgICAgICAgIHx8IHByb3BlcnR5LnR5cGUgPT09ICdSZXN0RWxlbWVudCdcbiAgICAgICAgICAgICAgfHwgIXByb3BlcnR5LmtleVxuICAgICAgICAgICAgKSB7XG4gICAgICAgICAgICAgIGNvbnRpbnVlXG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIGlmIChwcm9wZXJ0eS5rZXkudHlwZSAhPT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgICBub2RlOiBwcm9wZXJ0eSxcbiAgICAgICAgICAgICAgICBtZXNzYWdlOiAnT25seSBkZXN0cnVjdHVyZSB0b3AtbGV2ZWwgbmFtZXMuJyxcbiAgICAgICAgICAgICAgfSlcbiAgICAgICAgICAgICAgY29udGludWVcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgaWYgKCFuYW1lc3BhY2UuaGFzKHByb3BlcnR5LmtleS5uYW1lKSkge1xuICAgICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgICAgbm9kZTogcHJvcGVydHksXG4gICAgICAgICAgICAgICAgbWVzc2FnZTogbWFrZU1lc3NhZ2UocHJvcGVydHkua2V5LCBwYXRoKSxcbiAgICAgICAgICAgICAgfSlcbiAgICAgICAgICAgICAgY29udGludWVcbiAgICAgICAgICAgIH1cblxuICAgICAgICAgICAgcGF0aC5wdXNoKHByb3BlcnR5LmtleS5uYW1lKVxuICAgICAgICAgICAgdGVzdEtleShwcm9wZXJ0eS52YWx1ZSwgbmFtZXNwYWNlLmdldChwcm9wZXJ0eS5rZXkubmFtZSkubmFtZXNwYWNlLCBwYXRoKVxuICAgICAgICAgICAgcGF0aC5wb3AoKVxuICAgICAgICAgIH1cbiAgICAgICAgfVxuXG4gICAgICAgIHRlc3RLZXkoaWQsIG5hbWVzcGFjZXMuZ2V0KGluaXQubmFtZSkpXG4gICAgICB9LFxuXG4gICAgICBKU1hNZW1iZXJFeHByZXNzaW9uOiBmdW5jdGlvbih7b2JqZWN0LCBwcm9wZXJ0eX0pIHtcbiAgICAgICAgIGlmICghbmFtZXNwYWNlcy5oYXMob2JqZWN0Lm5hbWUpKSByZXR1cm5cbiAgICAgICAgIHZhciBuYW1lc3BhY2UgPSBuYW1lc3BhY2VzLmdldChvYmplY3QubmFtZSlcbiAgICAgICAgIGlmICghbmFtZXNwYWNlLmhhcyhwcm9wZXJ0eS5uYW1lKSkge1xuICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgICAgbm9kZTogcHJvcGVydHksXG4gICAgICAgICAgICAgbWVzc2FnZTogbWFrZU1lc3NhZ2UocHJvcGVydHksIFtvYmplY3QubmFtZV0pLFxuICAgICAgICAgICB9KVxuICAgICAgICAgfVxuICAgICAgfSxcbiAgICB9XG4gIH0sXG59XG4iXX0=