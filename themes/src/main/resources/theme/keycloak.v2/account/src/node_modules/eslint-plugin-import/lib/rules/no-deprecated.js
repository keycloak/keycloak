'use strict';var _declaredScope = require('eslint-module-utils/declaredScope');var _declaredScope2 = _interopRequireDefault(_declaredScope);
var _ExportMap = require('../ExportMap');var _ExportMap2 = _interopRequireDefault(_ExportMap);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

function message(deprecation) {
  return 'Deprecated' + (deprecation.description ? ': ' + deprecation.description : '.');
}

function getDeprecation(metadata) {
  if (!metadata || !metadata.doc) return;

  return metadata.doc.tags.find(function (t) {return t.title === 'deprecated';});
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('no-deprecated') },

    schema: [] },


  create: function () {function create(context) {
      var deprecated = new Map();
      var namespaces = new Map();

      function checkSpecifiers(node) {
        if (node.type !== 'ImportDeclaration') return;
        if (node.source == null) return; // local export, ignore

        var imports = _ExportMap2['default'].get(node.source.value, context);
        if (imports == null) return;

        var moduleDeprecation = imports.doc && imports.doc.tags.find(function (t) {return t.title === 'deprecated';});
        if (moduleDeprecation) {
          context.report({ node: node, message: message(moduleDeprecation) });
        }

        if (imports.errors.length) {
          imports.reportErrors(context, node);
          return;
        }

        node.specifiers.forEach(function (im) {
          var imported = void 0;var local = void 0;
          switch (im.type) {


            case 'ImportNamespaceSpecifier':{
                if (!imports.size) return;
                namespaces.set(im.local.name, imports);
                return;
              }

            case 'ImportDefaultSpecifier':
              imported = 'default';
              local = im.local.name;
              break;

            case 'ImportSpecifier':
              imported = im.imported.name;
              local = im.local.name;
              break;

            default:return; // can't handle this one
          }

          // unknown thing can't be deprecated
          var exported = imports.get(imported);
          if (exported == null) return;

          // capture import of deep namespace
          if (exported.namespace) namespaces.set(local, exported.namespace);

          var deprecation = getDeprecation(imports.get(imported));
          if (!deprecation) return;

          context.report({ node: im, message: message(deprecation) });

          deprecated.set(local, deprecation);

        });
      }

      return {
        'Program': function () {function Program(_ref) {var body = _ref.body;return body.forEach(checkSpecifiers);}return Program;}(),

        'Identifier': function () {function Identifier(node) {
            if (node.parent.type === 'MemberExpression' && node.parent.property === node) {
              return; // handled by MemberExpression
            }

            // ignore specifier identifiers
            if (node.parent.type.slice(0, 6) === 'Import') return;

            if (!deprecated.has(node.name)) return;

            if ((0, _declaredScope2['default'])(context, node.name) !== 'module') return;
            context.report({
              node: node,
              message: message(deprecated.get(node.name)) });

          }return Identifier;}(),

        'MemberExpression': function () {function MemberExpression(dereference) {
            if (dereference.object.type !== 'Identifier') return;
            if (!namespaces.has(dereference.object.name)) return;

            if ((0, _declaredScope2['default'])(context, dereference.object.name) !== 'module') return;

            // go deep
            var namespace = namespaces.get(dereference.object.name);
            var namepath = [dereference.object.name];
            // while property is namespace and parent is member expression, keep validating
            while (namespace instanceof _ExportMap2['default'] &&
            dereference.type === 'MemberExpression') {

              // ignore computed parts for now
              if (dereference.computed) return;

              var metadata = namespace.get(dereference.property.name);

              if (!metadata) break;
              var deprecation = getDeprecation(metadata);

              if (deprecation) {
                context.report({ node: dereference.property, message: message(deprecation) });
              }

              // stash and pop
              namepath.push(dereference.property.name);
              namespace = metadata.namespace;
              dereference = dereference.parent;
            }
          }return MemberExpression;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1kZXByZWNhdGVkLmpzIl0sIm5hbWVzIjpbIm1lc3NhZ2UiLCJkZXByZWNhdGlvbiIsImRlc2NyaXB0aW9uIiwiZ2V0RGVwcmVjYXRpb24iLCJtZXRhZGF0YSIsImRvYyIsInRhZ3MiLCJmaW5kIiwidCIsInRpdGxlIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJ0eXBlIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImNyZWF0ZSIsImNvbnRleHQiLCJkZXByZWNhdGVkIiwiTWFwIiwibmFtZXNwYWNlcyIsImNoZWNrU3BlY2lmaWVycyIsIm5vZGUiLCJzb3VyY2UiLCJpbXBvcnRzIiwiRXhwb3J0cyIsImdldCIsInZhbHVlIiwibW9kdWxlRGVwcmVjYXRpb24iLCJyZXBvcnQiLCJlcnJvcnMiLCJsZW5ndGgiLCJyZXBvcnRFcnJvcnMiLCJzcGVjaWZpZXJzIiwiZm9yRWFjaCIsImltIiwiaW1wb3J0ZWQiLCJsb2NhbCIsInNpemUiLCJzZXQiLCJuYW1lIiwiZXhwb3J0ZWQiLCJuYW1lc3BhY2UiLCJib2R5IiwicGFyZW50IiwicHJvcGVydHkiLCJzbGljZSIsImhhcyIsImRlcmVmZXJlbmNlIiwib2JqZWN0IiwibmFtZXBhdGgiLCJjb21wdXRlZCIsInB1c2giXSwibWFwcGluZ3MiOiJhQUFBLGtFO0FBQ0EseUM7QUFDQSxxQzs7QUFFQSxTQUFTQSxPQUFULENBQWlCQyxXQUFqQixFQUE4QjtBQUM1QixTQUFPLGdCQUFnQkEsWUFBWUMsV0FBWixHQUEwQixPQUFPRCxZQUFZQyxXQUE3QyxHQUEyRCxHQUEzRSxDQUFQO0FBQ0Q7O0FBRUQsU0FBU0MsY0FBVCxDQUF3QkMsUUFBeEIsRUFBa0M7QUFDaEMsTUFBSSxDQUFDQSxRQUFELElBQWEsQ0FBQ0EsU0FBU0MsR0FBM0IsRUFBZ0M7O0FBRWhDLFNBQU9ELFNBQVNDLEdBQVQsQ0FBYUMsSUFBYixDQUFrQkMsSUFBbEIsQ0FBdUIscUJBQUtDLEVBQUVDLEtBQUYsS0FBWSxZ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V0FBV3VDLFVBQVVoQixHQUFWLENBQWNzQixZQUFZSCxRQUFaLENBQXFCTCxJQUFuQyxDQUFqQjs7QUFFQSxrQkFBSSxDQUFDckMsUUFBTCxFQUFlO0FBQ2Ysa0JBQU1ILGNBQWNFLGVBQWVDLFFBQWYsQ0FBcEI7O0FBRUEsa0JBQUlILFdBQUosRUFBaUI7QUFDZmlCLHdCQUFRWSxNQUFSLENBQWUsRUFBRVAsTUFBTTBCLFlBQVlILFFBQXBCLEVBQThCOUMsU0FBU0EsUUFBUUMsV0FBUixDQUF2QyxFQUFmO0FBQ0Q7O0FBRUQ7QUFDQWtELHVCQUFTRSxJQUFULENBQWNKLFlBQVlILFFBQVosQ0FBcUJMLElBQW5DO0FBQ0FFLDBCQUFZdkMsU0FBU3VDLFNBQXJCO0FBQ0FNLDRCQUFjQSxZQUFZSixNQUExQjtBQUNEO0FBQ0YsV0E5QkQsMkJBcEJLLEVBQVA7O0FBb0RELEtBM0hjLG1CQUFqQiIsImZpbGUiOiJuby1kZXByZWNhdGVkLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IGRlY2xhcmVkU2NvcGUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9kZWNsYXJlZFNjb3BlJztcbmltcG9ydCBFeHBvcnRzIGZyb20gJy4uL0V4cG9ydE1hcCc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuZnVuY3Rpb24gbWVzc2FnZShkZXByZWNhdGlvbikge1xuICByZXR1cm4gJ0RlcHJlY2F0ZWQnICsgKGRlcHJlY2F0aW9uLmRlc2NyaXB0aW9uID8gJzogJyArIGRlcHJlY2F0aW9uLmRlc2NyaXB0aW9uIDogJy4nKTtcbn1cblxuZnVuY3Rpb24gZ2V0RGVwcmVjYXRpb24obWV0YWRhdGEpIHtcbiAgaWYgKCFtZXRhZGF0YSB8fCAhbWV0YWRhdGEuZG9jKSByZXR1cm47XG5cbiAgcmV0dXJuIG1ldGFkYXRhLmRvYy50YWdzLmZpbmQodCA9PiB0LnRpdGxlID09PSAnZGVwcmVjYXRlZCcpO1xufVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLWRlcHJlY2F0ZWQnKSxcbiAgICB9LFxuICAgIHNjaGVtYTogW10sXG4gIH0sXG5cbiAgY3JlYXRlKGNvbnRleHQpIHtcbiAgICBjb25zdCBkZXByZWNhdGVkID0gbmV3IE1hcCgpO1xuICAgIGNvbnN0IG5hbWVzcGFjZXMgPSBuZXcgTWFwKCk7XG5cbiAgICBmdW5jdGlvbiBjaGVja1NwZWNpZmllcnMobm9kZSkge1xuICAgICAgaWYgKG5vZGUudHlwZSAhPT0gJ0ltcG9ydERlY2xhcmF0aW9uJykgcmV0dXJuO1xuICAgICAgaWYgKG5vZGUuc291cmNlID09IG51bGwpIHJldHVybjsgLy8gbG9jYWwgZXhwb3J0LCBpZ25vcmVcblxuICAgICAgY29uc3QgaW1wb3J0cyA9IEV4cG9ydHMuZ2V0KG5vZGUuc291cmNlLnZhbHVlLCBjb250ZXh0KTtcbiAgICAgIGlmIChpbXBvcnRzID09IG51bGwpIHJldHVybjtcblxuICAgICAgY29uc3QgbW9kdWxlRGVwcmVjYXRpb24gPSBpbXBvcnRzLmRvYyAmJiBpbXBvcnRzLmRvYy50YWdzLmZpbmQodCA9PiB0LnRpdGxlID09PSAnZGVwcmVjYXRlZCcpO1xuICAgICAgaWYgKG1vZHVsZURlcHJlY2F0aW9uKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZSwgbWVzc2FnZTogbWVzc2FnZShtb2R1bGVEZXByZWNhdGlvbikgfSk7XG4gICAgICB9XG5cbiAgICAgIGlmIChpbXBvcnRzLmVycm9ycy5sZW5ndGgpIHtcbiAgICAgICAgaW1wb3J0cy5yZXBvcnRFcnJvcnMoY29udGV4dCwgbm9kZSk7XG4gICAgICAgIHJldHVybjtcbiAgICAgIH1cblxuICAgICAgbm9kZS5zcGVjaWZpZXJzLmZvckVhY2goZnVuY3Rpb24gKGltKSB7XG4gICAgICAgIGxldCBpbXBvcnRlZDsgbGV0IGxvY2FsO1xuICAgICAgICBzd2l0Y2ggKGltLnR5cGUpIHtcblxuXG4gICAgICAgIGNhc2UgJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllcic6e1xuICAgICAgICAgIGlmICghaW1wb3J0cy5zaXplKSByZXR1cm47XG4gICAgICAgICAgbmFtZXNwYWNlcy5zZXQoaW0ubG9jYWwubmFtZSwgaW1wb3J0cyk7XG4gICAgICAgICAgcmV0dXJuO1xuICAgICAgICB9XG5cbiAgICAgICAgY2FzZSAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcic6XG4gICAgICAgICAgaW1wb3J0ZWQgPSAnZGVmYXVsdCc7XG4gICAgICAgICAgbG9jYWwgPSBpbS5sb2NhbC5uYW1lO1xuICAgICAgICAgIGJyZWFrO1xuXG4gICAgICAgIGNhc2UgJ0ltcG9ydFNwZWNpZmllcic6XG4gICAgICAgICAgaW1wb3J0ZWQgPSBpbS5pbXBvcnRlZC5uYW1lO1xuICAgICAgICAgIGxvY2FsID0gaW0ubG9jYWwubmFtZTtcbiAgICAgICAgICBicmVhaztcblxuICAgICAgICBkZWZhdWx0OiByZXR1cm47IC8vIGNhbid0IGhhbmRsZSB0aGlzIG9uZVxuICAgICAgICB9XG5cbiAgICAgICAgLy8gdW5rbm93biB0aGluZyBjYW4ndCBiZSBkZXByZWNhdGVkXG4gICAgICAgIGNvbnN0IGV4cG9ydGVkID0gaW1wb3J0cy5nZXQoaW1wb3J0ZWQpO1xuICAgICAgICBpZiAoZXhwb3J0ZWQgPT0gbnVsbCkgcmV0dXJuO1xuXG4gICAgICAgIC8vIGNhcHR1cmUgaW1wb3J0IG9mIGRlZXAgbmFtZXNwYWNlXG4gICAgICAgIGlmIChleHBvcnRlZC5uYW1lc3BhY2UpIG5hbWVzcGFjZXMuc2V0KGxvY2FsLCBleHBvcnRlZC5uYW1lc3BhY2UpO1xuXG4gICAgICAgIGNvbnN0IGRlcHJlY2F0aW9uID0gZ2V0RGVwcmVjYXRpb24oaW1wb3J0cy5nZXQoaW1wb3J0ZWQpKTtcbiAgICAgICAgaWYgKCFkZXByZWNhdGlvbikgcmV0dXJuO1xuXG4gICAgICAgIGNvbnRleHQucmVwb3J0KHsgbm9kZTogaW0sIG1lc3NhZ2U6IG1lc3NhZ2UoZGVwcmVjYXRpb24pIH0pO1xuXG4gICAgICAgIGRlcHJlY2F0ZWQuc2V0KGxvY2FsLCBkZXByZWNhdGlvbik7XG5cbiAgICAgIH0pO1xuICAgIH1cblxuICAgIHJldHVybiB7XG4gICAgICAnUHJvZ3JhbSc6ICh7IGJvZHkgfSkgPT4gYm9keS5mb3JFYWNoKGNoZWNrU3BlY2lmaWVycyksXG5cbiAgICAgICdJZGVudGlmaWVyJzogZnVuY3Rpb24gKG5vZGUpIHtcbiAgICAgICAgaWYgKG5vZGUucGFyZW50LnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJyAmJiBub2RlLnBhcmVudC5wcm9wZXJ0eSA9PT0gbm9kZSkge1xuICAgICAgICAgIHJldHVybjsgLy8gaGFuZGxlZCBieSBNZW1iZXJFeHByZXNzaW9uXG4gICAgICAgIH1cblxuICAgICAgICAvLyBpZ25vcmUgc3BlY2lmaWVyIGlkZW50aWZpZXJzXG4gICAgICAgIGlmIChub2RlLnBhcmVudC50eXBlLnNsaWNlKDAsIDYpID09PSAnSW1wb3J0JykgcmV0dXJuO1xuXG4gICAgICAgIGlmICghZGVwcmVjYXRlZC5oYXMobm9kZS5uYW1lKSkgcmV0dXJuO1xuXG4gICAgICAgIGlmIChkZWNsYXJlZFNjb3BlKGNvbnRleHQsIG5vZGUubmFtZSkgIT09ICdtb2R1bGUnKSByZXR1cm47XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlLFxuICAgICAgICAgIG1lc3NhZ2U6IG1lc3NhZ2UoZGVwcmVjYXRlZC5nZXQobm9kZS5uYW1lKSksXG4gICAgICAgIH0pO1xuICAgICAgfSxcblxuICAgICAgJ01lbWJlckV4cHJlc3Npb24nOiBmdW5jdGlvbiAoZGVyZWZlcmVuY2UpIHtcbiAgICAgICAgaWYgKGRlcmVmZXJlbmNlLm9iamVjdC50eXBlICE9PSAnSWRlbnRpZmllcicpIHJldHVybjtcbiAgICAgICAgaWYgKCFuYW1lc3BhY2VzLmhhcyhkZXJlZmVyZW5jZS5vYmplY3QubmFtZSkpIHJldHVybjtcblxuICAgICAgICBpZiAoZGVjbGFyZWRTY29wZShjb250ZXh0LCBkZXJlZmVyZW5jZS5vYmplY3QubmFtZSkgIT09ICdtb2R1bGUnKSByZXR1cm47XG5cbiAgICAgICAgLy8gZ28gZGVlcFxuICAgICAgICBsZXQgbmFtZXNwYWNlID0gbmFtZXNwYWNlcy5nZXQoZGVyZWZlcmVuY2Uub2JqZWN0Lm5hbWUpO1xuICAgICAgICBjb25zdCBuYW1lcGF0aCA9IFtkZXJlZmVyZW5jZS5vYmplY3QubmFtZV07XG4gICAgICAgIC8vIHdoaWxlIHByb3BlcnR5IGlzIG5hbWVzcGFjZSBhbmQgcGFyZW50IGlzIG1lbWJlciBleHByZXNzaW9uLCBrZWVwIHZhbGlkYXRpbmdcbiAgICAgICAgd2hpbGUgKG5hbWVzcGFjZSBpbnN0YW5jZW9mIEV4cG9ydHMgJiZcbiAgICAgICAgICAgICAgIGRlcmVmZXJlbmNlLnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJykge1xuXG4gICAgICAgICAgLy8gaWdub3JlIGNvbXB1dGVkIHBhcnRzIGZvciBub3dcbiAgICAgICAgICBpZiAoZGVyZWZlcmVuY2UuY29tcHV0ZWQpIHJldHVybjtcblxuICAgICAgICAgIGNvbnN0IG1ldGFkYXRhID0gbmFtZXNwYWNlLmdldChkZXJlZmVyZW5jZS5wcm9wZXJ0eS5uYW1lKTtcblxuICAgICAgICAgIGlmICghbWV0YWRhdGEpIGJyZWFrO1xuICAgICAgICAgIGNvbnN0IGRlcHJlY2F0aW9uID0gZ2V0RGVwcmVjYXRpb24obWV0YWRhdGEpO1xuXG4gICAgICAgICAgaWYgKGRlcHJlY2F0aW9uKSB7XG4gICAgICAgICAgICBjb250ZXh0LnJlcG9ydCh7IG5vZGU6IGRlcmVmZXJlbmNlLnByb3BlcnR5LCBtZXNzYWdlOiBtZXNzYWdlKGRlcHJlY2F0aW9uKSB9KTtcbiAgICAgICAgICB9XG5cbiAgICAgICAgICAvLyBzdGFzaCBhbmQgcG9wXG4gICAgICAgICAgbmFtZXBhdGgucHVzaChkZXJlZmVyZW5jZS5wcm9wZXJ0eS5uYW1lKTtcbiAgICAgICAgICBuYW1lc3BhY2UgPSBtZXRhZGF0YS5uYW1lc3BhY2U7XG4gICAgICAgICAgZGVyZWZlcmVuY2UgPSBkZXJlZmVyZW5jZS5wYXJlbnQ7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgfTtcbiAgfSxcbn07XG4iXX0=