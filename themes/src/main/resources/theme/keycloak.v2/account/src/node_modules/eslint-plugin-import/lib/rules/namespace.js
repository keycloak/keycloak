'use strict';var _declaredScope = require('eslint-module-utils/declaredScope');var _declaredScope2 = _interopRequireDefault(_declaredScope);
var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _importDeclaration = require('../importDeclaration');var _importDeclaration2 = _interopRequireDefault(_importDeclaration);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function processBodyStatement(context, namespaces, declaration) {
  if (declaration.type !== 'ImportDeclaration') return;

  if (declaration.specifiers.length === 0) return;

  var imports = _ExportMap2['default'].get(declaration.source.value, context);
  if (imports == null) return null;

  if (imports.errors.length > 0) {
    imports.reportErrors(context, declaration);
    return;
  }

  declaration.specifiers.forEach(function (specifier) {
    switch (specifier.type) {
      case 'ImportNamespaceSpecifier':
        if (!imports.size) {
          context.report(
          specifier, 'No exported names found in module \'' + String(
          declaration.source.value) + '\'.');

        }
        namespaces.set(specifier.local.name, imports);
        break;
      case 'ImportDefaultSpecifier':
      case 'ImportSpecifier':{
          var meta = imports.get(
          // default to 'default' for default https://i.imgur.com/nj6qAWy.jpg
          specifier.imported ? specifier.imported.name || specifier.imported.value : 'default');

          if (!meta || !meta.namespace) {break;}
          namespaces.set(specifier.local.name, meta.namespace);
          break;
        }}

  });
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('namespace') },


    schema: [
    {
      type: 'object',
      properties: {
        allowComputed: {
          description: 'If `false`, will report computed (and thus, un-lintable) references to namespace members.',
          type: 'boolean',
          'default': false } },


      additionalProperties: false }] },




  create: function () {function namespaceRule(context) {

      // read options
      var _ref =

      context.options[0] || {},_ref$allowComputed = _ref.allowComputed,allowComputed = _ref$allowComputed === undefined ? false : _ref$allowComputed;

      var namespaces = new Map();

      function makeMessage(last, namepath) {
        return '\'' + String(last.name) + '\' not found in ' + (namepath.length > 1 ? 'deeply ' : '') + 'imported namespace \'' + String(namepath.join('.')) + '\'.';
      }

      return {
        // pick up all imports at body entry time, to properly respect hoisting
        Program: function () {function Program(_ref2) {var body = _ref2.body;
            body.forEach(function (x) {return processBodyStatement(context, namespaces, x);});
          }return Program;}(),

        // same as above, but does not add names to local map
        ExportNamespaceSpecifier: function () {function ExportNamespaceSpecifier(namespace) {
            var declaration = (0, _importDeclaration2['default'])(context);

            var imports = _ExportMap2['default'].get(declaration.source.value, context);
            if (imports == null) return null;

            if (imports.errors.length) {
              imports.reportErrors(context, declaration);
              return;
            }

            if (!imports.size) {
              context.report(
              namespace, 'No exported names found in module \'' + String(
              declaration.source.value) + '\'.');

            }
          }return ExportNamespaceSpecifier;}(),

        // todo: check for possible redefinition

        MemberExpression: function () {function MemberExpression(dereference) {
            if (dereference.object.type !== 'Identifier') return;
            if (!namespaces.has(dereference.object.name)) return;
            if ((0, _declaredScope2['default'])(context, dereference.object.name) !== 'module') return;

            if (dereference.parent.type === 'AssignmentExpression' && dereference.parent.left === dereference) {
              context.report(
              dereference.parent, 'Assignment to member of namespace \'' + String(
              dereference.object.name) + '\'.');

            }

            // go deep
            var namespace = namespaces.get(dereference.object.name);
            var namepath = [dereference.object.name];
            // while property is namespace and parent is member expression, keep validating
            while (namespace instanceof _ExportMap2['default'] && dereference.type === 'MemberExpression') {
              if (dereference.computed) {
                if (!allowComputed) {
                  context.report(
                  dereference.property, 'Unable to validate computed reference to imported namespace \'' + String(
                  dereference.object.name) + '\'.');

                }
                return;
              }

              if (!namespace.has(dereference.property.name)) {
                context.report(
                dereference.property,
                makeMessage(dereference.property, namepath));

                break;
              }

              var exported = namespace.get(dereference.property.name);
              if (exported == null) return;

              // stash and pop
              namepath.push(dereference.property.name);
              namespace = exported.namespace;
              dereference = dereference.parent;
            }
          }return MemberExpression;}(),

        VariableDeclarator: function () {function VariableDeclarator(_ref3) {var id = _ref3.id,init = _ref3.init;
            if (init == null) return;
            if (init.type !== 'Identifier') return;
            if (!namespaces.has(init.name)) return;

            // check for redefinition in intermediate scopes
            if ((0, _declaredScope2['default'])(context, init.name) !== 'module') return;

            // DFS traverse child namespaces
            function testKey(pattern, namespace) {var path = arguments.length > 2 && arguments[2] !== undefined ? arguments[2] : [init.name];
              if (!(namespace instanceof _ExportMap2['default'])) return;

              if (pattern.type !== 'ObjectPattern') return;var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {

                for (var _iterator = pattern.properties[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var property = _step.value;
                  if (
                  property.type === 'ExperimentalRestProperty' ||
                  property.type === 'RestElement' ||
                  !property.key)
                  {
                    continue;
                  }

                  if (property.key.type !== 'Identifier') {
                    context.report({
                      node: property,
                      message: 'Only destructure top-level names.' });

                    continue;
                  }

                  if (!namespace.has(property.key.name)) {
                    context.report({
                      node: property,
                      message: makeMessage(property.key, path) });

                    continue;
                  }

                  path.push(property.key.name);
                  var dependencyExportMap = namespace.get(property.key.name);
                  // could be null when ignored or ambiguous
                  if (dependencyExportMap !== null) {
                    testKey(property.value, dependencyExportMap.namespace, path);
                  }
                  path.pop();
                }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
            }

            testKey(id, namespaces.get(init.name));
          }return VariableDeclarator;}(),

        JSXMemberExpression: function () {function JSXMemberExpression(_ref4) {var object = _ref4.object,property = _ref4.property;
            if (!namespaces.has(object.name)) return;
            var namespace = namespaces.get(object.name);
            if (!namespace.has(property.name)) {
              context.report({
                node: property,
                message: makeMessage(property, [object.name]) });

            }
          }return JSXMemberExpression;}() };

    }return namespaceRule;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uYW1lc3BhY2UuanMiXSwibmFtZXMiOlsicHJvY2Vzc0JvZHlTdGF0ZW1lbnQiLCJjb250ZXh0IiwibmFtZXNwYWNlcyIsImRlY2xhcmF0aW9uIiwidHlwZSIsInNwZWNpZmllcnMiLCJsZW5ndGgiLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwicmVwb3J0RXJyb3JzIiwiZm9yRWFjaCIsInNwZWNpZmllciIsInNpemUiLCJyZXBvcnQiLCJzZXQiLCJsb2NhbCIsIm5hbWUiLCJtZXRhIiwiaW1wb3J0ZWQiLCJuYW1lc3BhY2UiLCJtb2R1bGUiLCJleHBvcnRzIiwiZG9jcyIsInVybCIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJhbGxvd0NvbXB1dGVkIiwiZGVzY3JpcHRpb24iLCJhZGRpdGlvbmFsUHJvcGVydGllcyIsImNyZWF0ZSIsIm5hbWVzcGFjZVJ1bGUiLCJvcHRpb25zIiwiTWFwIiwibWFrZU1lc3NhZ2UiLCJsYXN0IiwibmFtZXBhdGgiLCJqb2luIiwiUHJvZ3JhbSIsImJvZHkiLCJ4IiwiRXhwb3J0TmFtZXNwYWNlU3BlY2lmaWVyIiwiTWVtYmVyRXhwcmVzc2lvbiIsImRlcmVmZXJlbmNlIiwib2JqZWN0IiwiaGFzIiwicGFyZW50IiwibGVmdCIsImNvbXB1dGVkIiwicHJvcGVydHkiLCJleHBvcnRlZCIsInB1c2giLCJWYXJpYWJsZURlY2xhcmF0b3IiLCJpZCIsImluaXQiLCJ0ZXN0S2V5IiwicGF0dGVybiIsInBhdGgiLCJrZXkiLCJub2RlIiwibWVzc2FnZSIsImRlcGVuZGVuY3lFeHBvcnRNYXAiLCJwb3AiLCJKU1hNZW1iZXJFeHByZXNzaW9uIl0sIm1hcHBpbmdzIjoiYUFBQSxrRTtBQUNBLHlDO0FBQ0EseUQ7QUFDQSxxQzs7QUFFQSxTQUFTQSxvQkFBVCxDQUE4QkMsT0FBOUIsRUFBdUNDLFVBQXZDLEVBQW1EQyxXQUFuRCxFQUFnRTtBQUM5RCxNQUFJ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SUFBcEIsQ0FBWjtBQUNELFdBM0hJOztBQTZITDhDLDJCQTdISyxtREE2SHFDLEtBQXBCbkIsTUFBb0IsU0FBcEJBLE1BQW9CLENBQVpLLFFBQVksU0FBWkEsUUFBWTtBQUN4QyxnQkFBSSxDQUFDbEQsV0FBVzhDLEdBQVgsQ0FBZUQsT0FBTzNCLElBQXRCLENBQUwsRUFBa0M7QUFDbEMsZ0JBQU1HLFlBQVlyQixXQUFXTyxHQUFYLENBQWVzQyxPQUFPM0IsSUFBdEIsQ0FBbEI7QUFDQSxnQkFBSSxDQUFDRyxVQUFVeUIsR0FBVixDQUFjSSxTQUFTaEMsSUFBdkIsQ0FBTCxFQUFtQztBQUNqQ25CLHNCQUFRZ0IsTUFBUixDQUFlO0FBQ2I2QyxzQkFBTVYsUUFETztBQUViVyx5QkFBUzFCLFlBQVllLFFBQVosRUFBc0IsQ0FBQ0wsT0FBTzNCLElBQVIsQ0FBdEIsQ0FGSSxFQUFmOztBQUlEO0FBQ0YsV0F0SUksZ0NBQVA7O0FBd0lELEtBckpELE9BQWlCYyxhQUFqQixJQXRCZSxFQUFqQiIsImZpbGUiOiJuYW1lc3BhY2UuanMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgZGVjbGFyZWRTY29wZSBmcm9tICdlc2xpbnQtbW9kdWxlLXV0aWxzL2RlY2xhcmVkU2NvcGUnO1xuaW1wb3J0IEV4cG9ydHMgZnJvbSAnLi4vRXhwb3J0TWFwJztcbmltcG9ydCBpbXBvcnREZWNsYXJhdGlvbiBmcm9tICcuLi9pbXBvcnREZWNsYXJhdGlvbic7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuZnVuY3Rpb24gcHJvY2Vzc0JvZHlTdGF0ZW1lbnQoY29udGV4dCwgbmFtZXNwYWNlcywgZGVjbGFyYXRpb24pIHtcbiAgaWYgKGRlY2xhcmF0aW9uLnR5cGUgIT09ICdJbXBvcnREZWNsYXJhdGlvbicpIHJldHVybjtcblxuICBpZiAoZGVjbGFyYXRpb24uc3BlY2lmaWVycy5sZW5ndGggPT09IDApIHJldHVybjtcblxuICBjb25zdCBpbXBvcnRzID0gRXhwb3J0cy5nZXQoZGVjbGFyYXRpb24uc291cmNlLnZhbHVlLCBjb250ZXh0KTtcbiAgaWYgKGltcG9ydHMgPT0gbnVsbCkgcmV0dXJuIG51bGw7XG5cbiAgaWYgKGltcG9ydHMuZXJyb3JzLmxlbmd0aCA+IDApIHtcbiAgICBpbXBvcnRzLnJlcG9ydEVycm9ycyhjb250ZXh0LCBkZWNsYXJhdGlvbik7XG4gICAgcmV0dXJuO1xuICB9XG5cbiAgZGVjbGFyYXRpb24uc3BlY2lmaWVycy5mb3JFYWNoKChzcGVjaWZpZXIpID0+IHtcbiAgICBzd2l0Y2ggKHNwZWNpZmllci50eXBlKSB7XG4gICAgY2FzZSAnSW1wb3J0TmFtZXNwYWNlU3BlY2lmaWVyJzpcbiAgICAgIGlmICghaW1wb3J0cy5zaXplKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgIHNwZWNpZmllcixcbiAgICAgICAgICBgTm8gZXhwb3J0ZWQgbmFtZXMgZm91bmQgaW4gbW9kdWxlICcke2RlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZX0nLmAsXG4gICAgICAgICk7XG4gICAgICB9XG4gICAgICBuYW1lc3BhY2VzLnNldChzcGVjaWZpZXIubG9jYWwubmFtZSwgaW1wb3J0cyk7XG4gICAgICBicmVhaztcbiAgICBjYXNlICdJbXBvcnREZWZhdWx0U3BlY2lmaWVyJzpcbiAgICBjYXNlICdJbXBvcnRTcGVjaWZpZXInOiB7XG4gICAgICBjb25zdCBtZXRhID0gaW1wb3J0cy5nZXQoXG4gICAgICAgIC8vIGRlZmF1bHQgdG8gJ2RlZmF1bHQnIGZvciBkZWZhdWx0IGh0dHBzOi8vaS5pbWd1ci5jb20vbmo2cUFXeS5qcGdcbiAgICAgICAgc3BlY2lmaWVyLmltcG9ydGVkID8gKHNwZWNpZmllci5pbXBvcnRlZC5uYW1lIHx8IHNwZWNpZmllci5pbXBvcnRlZC52YWx1ZSkgOiAnZGVmYXVsdCcsXG4gICAgICApO1xuICAgICAgaWYgKCFtZXRhIHx8ICFtZXRhLm5hbWVzcGFjZSkgeyBicmVhazsgfVxuICAgICAgbmFtZXNwYWNlcy5zZXQoc3BlY2lmaWVyLmxvY2FsLm5hbWUsIG1ldGEubmFtZXNwYWNlKTtcbiAgICAgIGJyZWFrO1xuICAgIH1cbiAgICB9XG4gIH0pO1xufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdwcm9ibGVtJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25hbWVzcGFjZScpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IFtcbiAgICAgIHtcbiAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICBhbGxvd0NvbXB1dGVkOiB7XG4gICAgICAgICAgICBkZXNjcmlwdGlvbjogJ0lmIGBmYWxzZWAsIHdpbGwgcmVwb3J0IGNvbXB1dGVkIChhbmQgdGh1cywgdW4tbGludGFibGUpIHJlZmVyZW5jZXMgdG8gbmFtZXNwYWNlIG1lbWJlcnMuJyxcbiAgICAgICAgICAgIHR5cGU6ICdib29sZWFuJyxcbiAgICAgICAgICAgIGRlZmF1bHQ6IGZhbHNlLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uIG5hbWVzcGFjZVJ1bGUoY29udGV4dCkge1xuXG4gICAgLy8gcmVhZCBvcHRpb25zXG4gICAgY29uc3Qge1xuICAgICAgYWxsb3dDb21wdXRlZCA9IGZhbHNlLFxuICAgIH0gPSBjb250ZXh0Lm9wdGlvbnNbMF0gfHwge307XG5cbiAgICBjb25zdCBuYW1lc3BhY2VzID0gbmV3IE1hcCgpO1xuXG4gICAgZnVuY3Rpb24gbWFrZU1lc3NhZ2UobGFzdCwgbmFtZXBhdGgpIHtcbiAgICAgIHJldHVybiBgJyR7bGFzdC5uYW1lfScgbm90IGZvdW5kIGluICR7bmFtZXBhdGgubGVuZ3RoID4gMSA/ICdkZWVwbHkgJyA6ICcnfWltcG9ydGVkIG5hbWVzcGFjZSAnJHtuYW1lcGF0aC5qb2luKCcuJyl9Jy5gO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAvLyBwaWNrIHVwIGFsbCBpbXBvcnRzIGF0IGJvZHkgZW50cnkgdGltZSwgdG8gcHJvcGVybHkgcmVzcGVjdCBob2lzdGluZ1xuICAgICAgUHJvZ3JhbSh7IGJvZHkgfSkge1xuICAgICAgICBib2R5LmZvckVhY2goeCA9PiBwcm9jZXNzQm9keVN0YXRlbWVudChjb250ZXh0LCBuYW1lc3BhY2VzLCB4KSk7XG4gICAgICB9LFxuXG4gICAgICAvLyBzYW1lIGFzIGFib3ZlLCBidXQgZG9lcyBub3QgYWRkIG5hbWVzIHRvIGxvY2FsIG1hcFxuICAgICAgRXhwb3J0TmFtZXNwYWNlU3BlY2lmaWVyKG5hbWVzcGFjZSkge1xuICAgICAgICBjb25zdCBkZWNsYXJhdGlvbiA9IGltcG9ydERlY2xhcmF0aW9uKGNvbnRleHQpO1xuXG4gICAgICAgIGNvbnN0IGltcG9ydHMgPSBFeHBvcnRzLmdldChkZWNsYXJhdGlvbi5zb3VyY2UudmFsdWUsIGNvbnRleHQpO1xuICAgICAgICBpZiAoaW1wb3J0cyA9PSBudWxsKSByZXR1cm4gbnVsbDtcblxuICAgICAgICBpZiAoaW1wb3J0cy5lcnJvcnMubGVuZ3RoKSB7XG4gICAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgZGVjbGFyYXRpb24pO1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuXG4gICAgICAgIGlmICghaW1wb3J0cy5zaXplKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoXG4gICAgICAgICAgICBuYW1lc3BhY2UsXG4gICAgICAgICAgICBgTm8gZXhwb3J0ZWQgbmFtZXMgZm91bmQgaW4gbW9kdWxlICcke2RlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZX0nLmAsXG4gICAgICAgICAgKTtcbiAgICAgICAgfVxuICAgICAgfSxcblxuICAgICAgLy8gdG9kbzogY2hlY2sgZm9yIHBvc3NpYmxlIHJlZGVmaW5pdGlvblxuXG4gICAgICBNZW1iZXJFeHByZXNzaW9uKGRlcmVmZXJlbmNlKSB7XG4gICAgICAgIGlmIChkZXJlZmVyZW5jZS5vYmplY3QudHlwZSAhPT0gJ0lkZW50aWZpZXInKSByZXR1cm47XG4gICAgICAgIGlmICghbmFtZXNwYWNlcy5oYXMoZGVyZWZlcmVuY2Uub2JqZWN0Lm5hbWUpKSByZXR1cm47XG4gICAgICAgIGlmIChkZWNsYXJlZFNjb3BlKGNvbnRleHQsIGRlcmVmZXJlbmNlLm9iamVjdC5uYW1lKSAhPT0gJ21vZHVsZScpIHJldHVybjtcblxuICAgICAgICBpZiAoZGVyZWZlcmVuY2UucGFyZW50LnR5cGUgPT09ICdBc3NpZ25tZW50RXhwcmVzc2lvbicgJiYgZGVyZWZlcmVuY2UucGFyZW50LmxlZnQgPT09IGRlcmVmZXJlbmNlKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoXG4gICAgICAgICAgICBkZXJlZmVyZW5jZS5wYXJlbnQsXG4gICAgICAgICAgICBgQXNzaWdubWVudCB0byBtZW1iZXIgb2YgbmFtZXNwYWNlICcke2RlcmVmZXJlbmNlLm9iamVjdC5uYW1lfScuYCxcbiAgICAgICAgICApO1xuICAgICAgICB9XG5cbiAgICAgICAgLy8gZ28gZGVlcFxuICAgICAgICBsZXQgbmFtZXNwYWNlID0gbmFtZXNwYWNlcy5nZXQoZGVyZWZlcmVuY2Uub2JqZWN0Lm5hbWUpO1xuICAgICAgICBjb25zdCBuYW1lcGF0aCA9IFtkZXJlZmVyZW5jZS5vYmplY3QubmFtZV07XG4gICAgICAgIC8vIHdoaWxlIHByb3BlcnR5IGlzIG5hbWVzcGFjZSBhbmQgcGFyZW50IGlzIG1lbWJlciBleHByZXNzaW9uLCBrZWVwIHZhbGlkYXRpbmdcbiAgICAgICAgd2hpbGUgKG5hbWVzcGFjZSBpbnN0YW5jZW9mIEV4cG9ydHMgJiYgZGVyZWZlcmVuY2UudHlwZSA9PT0gJ01lbWJlckV4cHJlc3Npb24nKSB7XG4gICAgICAgICAgaWYgKGRlcmVmZXJlbmNlLmNvbXB1dGVkKSB7XG4gICAgICAgICAgICBpZiAoIWFsbG93Q29tcHV0ZWQpIHtcbiAgICAgICAgICAgICAgY29udGV4dC5yZXBvcnQoXG4gICAgICAgICAgICAgICAgZGVyZWZlcmVuY2UucHJvcGVydHksXG4gICAgICAgICAgICAgICAgYFVuYWJsZSB0byB2YWxpZGF0ZSBjb21wdXRlZCByZWZlcmVuY2UgdG8gaW1wb3J0ZWQgbmFtZXNwYWNlICcke2RlcmVmZXJlbmNlLm9iamVjdC5uYW1lfScuYCxcbiAgICAgICAgICAgICAgKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHJldHVybjtcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBpZiAoIW5hbWVzcGFjZS5oYXMoZGVyZWZlcmVuY2UucHJvcGVydHkubmFtZSkpIHtcbiAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KFxuICAgICAgICAgICAgICBkZXJlZmVyZW5jZS5wcm9wZXJ0eSxcbiAgICAgICAgICAgICAgbWFrZU1lc3NhZ2UoZGVyZWZlcmVuY2UucHJvcGVydHksIG5hbWVwYXRoKSxcbiAgICAgICAgICAgICk7XG4gICAgICAgICAgICBicmVhaztcbiAgICAgICAgICB9XG5cbiAgICAgICAgICBjb25zdCBleHBvcnRlZCA9IG5hbWVzcGFjZS5nZXQoZGVyZWZlcmVuY2UucHJvcGVydHkubmFtZSk7XG4gICAgICAgICAgaWYgKGV4cG9ydGVkID09IG51bGwpIHJldHVybjtcblxuICAgICAgICAgIC8vIHN0YXNoIGFuZCBwb3BcbiAgICAgICAgICBuYW1lcGF0aC5wdXNoKGRlcmVmZXJlbmNlLnByb3BlcnR5Lm5hbWUpO1xuICAgICAgICAgIG5hbWVzcGFjZSA9IGV4cG9ydGVkLm5hbWVzcGFjZTtcbiAgICAgICAgICBkZXJlZmVyZW5jZSA9IGRlcmVmZXJlbmNlLnBhcmVudDtcbiAgICAgICAgfVxuICAgICAgfSxcblxuICAgICAgVmFyaWFibGVEZWNsYXJhdG9yKHsgaWQsIGluaXQgfSkge1xuICAgICAgICBpZiAoaW5pdCA9PSBudWxsKSByZXR1cm47XG4gICAgICAgIGlmIChpbml0LnR5cGUgIT09ICdJZGVudGlmaWVyJykgcmV0dXJuO1xuICAgICAgICBpZiAoIW5hbWVzcGFjZXMuaGFzKGluaXQubmFtZSkpIHJldHVybjtcblxuICAgICAgICAvLyBjaGVjayBmb3IgcmVkZWZpbml0aW9uIGluIGludGVybWVkaWF0ZSBzY29wZXNcbiAgICAgICAgaWYgKGRlY2xhcmVkU2NvcGUoY29udGV4dCwgaW5pdC5uYW1lKSAhPT0gJ21vZHVsZScpIHJldHVybjtcblxuICAgICAgICAvLyBERlMgdHJhdmVyc2UgY2hpbGQgbmFtZXNwYWNlc1xuICAgICAgICBmdW5jdGlvbiB0ZXN0S2V5KHBhdHRlcm4sIG5hbWVzcGFjZSwgcGF0aCA9IFtpbml0Lm5hbWVdKSB7XG4gICAgICAgICAgaWYgKCEobmFtZXNwYWNlIGluc3RhbmNlb2YgRXhwb3J0cykpIHJldHVybjtcblxuICAgICAgICAgIGlmIChwYXR0ZXJuLnR5cGUgIT09ICdPYmplY3RQYXR0ZXJuJykgcmV0dXJuO1xuXG4gICAgICAgICAgZm9yIChjb25zdCBwcm9wZXJ0eSBvZiBwYXR0ZXJuLnByb3BlcnRpZXMpIHtcbiAgICAgICAgICAgIGlmIChcbiAgICAgICAgICAgICAgcHJvcGVydHkudHlwZSA9PT0gJ0V4cGVyaW1lbnRhbFJlc3RQcm9wZXJ0eSdcbiAgICAgICAgICAgICAgfHwgcHJvcGVydHkudHlwZSA9PT0gJ1Jlc3RFbGVtZW50J1xuICAgICAgICAgICAgICB8fCAhcHJvcGVydHkua2V5XG4gICAgICAgICAgICApIHtcbiAgICAgICAgICAgICAgY29udGludWU7XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIGlmIChwcm9wZXJ0eS5rZXkudHlwZSAhPT0gJ0lkZW50aWZpZXInKSB7XG4gICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgICBub2RlOiBwcm9wZXJ0eSxcbiAgICAgICAgICAgICAgICBtZXNzYWdlOiAnT25seSBkZXN0cnVjdHVyZSB0b3AtbGV2ZWwgbmFtZXMuJyxcbiAgICAgICAgICAgICAgfSk7XG4gICAgICAgICAgICAgIGNvbnRpbnVlO1xuICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICBpZiAoIW5hbWVzcGFjZS5oYXMocHJvcGVydHkua2V5Lm5hbWUpKSB7XG4gICAgICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgICAgICBub2RlOiBwcm9wZXJ0eSxcbiAgICAgICAgICAgICAgICBtZXNzYWdlOiBtYWtlTWVzc2FnZShwcm9wZXJ0eS5rZXksIHBhdGgpLFxuICAgICAgICAgICAgICB9KTtcbiAgICAgICAgICAgICAgY29udGludWU7XG4gICAgICAgICAgICB9XG5cbiAgICAgICAgICAgIHBhdGgucHVzaChwcm9wZXJ0eS5rZXkubmFtZSk7XG4gICAgICAgICAgICBjb25zdCBkZXBlbmRlbmN5RXhwb3J0TWFwID0gbmFtZXNwYWNlLmdldChwcm9wZXJ0eS5rZXkubmFtZSk7XG4gICAgICAgICAgICAvLyBjb3VsZCBiZSBudWxsIHdoZW4gaWdub3JlZCBvciBhbWJpZ3VvdXNcbiAgICAgICAgICAgIGlmIChkZXBlbmRlbmN5RXhwb3J0TWFwICE9PSBudWxsKSB7XG4gICAgICAgICAgICAgIHRlc3RLZXkocHJvcGVydHkudmFsdWUsIGRlcGVuZGVuY3lFeHBvcnRNYXAubmFtZXNwYWNlLCBwYXRoKTtcbiAgICAgICAgICAgIH1cbiAgICAgICAgICAgIHBhdGgucG9wKCk7XG4gICAgICAgICAgfVxuICAgICAgICB9XG5cbiAgICAgICAgdGVzdEtleShpZCwgbmFtZXNwYWNlcy5nZXQoaW5pdC5uYW1lKSk7XG4gICAgICB9LFxuXG4gICAgICBKU1hNZW1iZXJFeHByZXNzaW9uKHsgb2JqZWN0LCBwcm9wZXJ0eSB9KSB7XG4gICAgICAgIGlmICghbmFtZXNwYWNlcy5oYXMob2JqZWN0Lm5hbWUpKSByZXR1cm47XG4gICAgICAgIGNvbnN0IG5hbWVzcGFjZSA9IG5hbWVzcGFjZXMuZ2V0KG9iamVjdC5uYW1lKTtcbiAgICAgICAgaWYgKCFuYW1lc3BhY2UuaGFzKHByb3BlcnR5Lm5hbWUpKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQoe1xuICAgICAgICAgICAgbm9kZTogcHJvcGVydHksXG4gICAgICAgICAgICBtZXNzYWdlOiBtYWtlTWVzc2FnZShwcm9wZXJ0eSwgW29iamVjdC5uYW1lXSksXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=