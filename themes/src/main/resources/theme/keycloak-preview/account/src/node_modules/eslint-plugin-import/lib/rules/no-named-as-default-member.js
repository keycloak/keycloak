'use strict';

var _ExportMap = require('../ExportMap');

var _ExportMap2 = _interopRequireDefault(_ExportMap);

var _importDeclaration = require('../importDeclaration');

var _importDeclaration2 = _interopRequireDefault(_importDeclaration);

var _docsUrl = require('../docsUrl');

var _docsUrl2 = _interopRequireDefault(_docsUrl);

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { default: obj }; }

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2.default)('no-named-as-default-member')
    }
  },

  create: function (context) {

    const fileImports = new Map();
    const allPropertyLookups = new Map();

    function handleImportDefault(node) {
      const declaration = (0, _importDeclaration2.default)(context);
      const exportMap = _ExportMap2.default.get(declaration.source.value, context);
      if (exportMap == null) return;

      if (exportMap.errors.length) {
        exportMap.reportErrors(context, declaration);
        return;
      }

      fileImports.set(node.local.name, {
        exportMap,
        sourcePath: declaration.source.value
      });
    }

    function storePropertyLookup(objectName, propName, node) {
      const lookups = allPropertyLookups.get(objectName) || [];
      lookups.push({ node, propName });
      allPropertyLookups.set(objectName, lookups);
    }

    function handlePropLookup(node) {
      const objectName = node.object.name;
      const propName = node.property.name;
      storePropertyLookup(objectName, propName, node);
    }

    function handleDestructuringAssignment(node) {
      const isDestructure = node.id.type === 'ObjectPattern' && node.init != null && node.init.type === 'Identifier';
      if (!isDestructure) return;

      const objectName = node.init.name;
      for (const _ref of node.id.properties) {
        const key = _ref.key;

        if (key == null) continue; // true for rest properties
        storePropertyLookup(objectName, key.name, key);
      }
    }

    function handleProgramExit() {
      allPropertyLookups.forEach((lookups, objectName) => {
        const fileImport = fileImports.get(objectName);
        if (fileImport == null) return;

        for (const _ref2 of lookups) {
          const propName = _ref2.propName;
          const node = _ref2.node;

          // the default import can have a "default" property
          if (propName === 'default') continue;
          if (!fileImport.exportMap.namespace.has(propName)) continue;

          context.report({
            node,
            message: `Caution: \`${objectName}\` also has a named export ` + `\`${propName}\`. Check if you meant to write ` + `\`import {${propName}} from '${fileImport.sourcePath}'\` ` + 'instead.'
          });
        }
      });
    }

    return {
      'ImportDefaultSpecifier': handleImportDefault,
      'MemberExpression': handlePropLookup,
      'VariableDeclarator': handleDestructuringAssignment,
      'Program:exit': handleProgramExit
    };
  }
}; /**
    * @fileoverview Rule to warn about potentially confused use of name exports
    * @author Desmond Brand
    * @copyright 2016 Desmond Brand. All rights reserved.
    * See LICENSE in root directory for full license.
    */
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbInJ1bGVzL25vLW5hbWVkLWFzLWRlZmF1bHQtbWVtYmVyLmpzIl0sIm5hbWVzIjpbIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwidHlwZSIsImRvY3MiLCJ1cmwiLCJjcmVhdGUiLCJjb250ZXh0IiwiZmlsZUltcG9ydHMiLCJNYXAiLCJhbGxQcm9wZXJ0eUxvb2t1cHMiLCJoYW5kbGVJbXBvcnREZWZhdWx0Iiwibm9kZSIsImRlY2xhcmF0aW9uIiwiZXhwb3J0TWFwIiwiRXhwb3J0cyIsImdldCIsInNvdXJjZSIsInZhbHVlIiwiZXJyb3JzIiwibGVuZ3RoIiwicmVwb3J0RXJyb3JzIiwic2V0IiwibG9jYWwiLCJuYW1lIiwic291cmNlUGF0aCIsInN0b3JlUHJvcGVydHlMb29rdXAiLCJvYmplY3ROYW1lIiwicHJvcE5hbWUiLCJsb29rdXBzIiwicHVzaCIsImhhbmRsZVByb3BMb29rdXAiLCJvYmplY3QiLCJwcm9wZXJ0eSIsImhhbmRsZURlc3RydWN0dXJpbmdBc3NpZ25tZW50IiwiaXNEZXN0cnVjdHVyZSIsImlkIiwiaW5pdCIsInByb3BlcnRpZXMiLCJrZXkiLCJoYW5kbGVQcm9ncmFtRXhpdCIsImZvckVhY2giLCJmaWxlSW1wb3J0IiwibmFtZXNwYWNlIiwiaGFzIiwicmVwb3J0IiwibWVzc2FnZSJdLCJtYXBwaW5ncyI6Ijs7QUFNQTs7OztBQUNBOzs7O0FBQ0E7Ozs7OztBQUVBO0FBQ0E7QUFDQTs7QUFFQUEsT0FBT0MsT0FBUCxHQUFpQjtBQUNmQyxRQUFNO0FBQ0pDLFVBQU0sWUFERjtBQUVKQyxVQUFNO0FBQ0pDLFdBQUssdUJBQVEsNEJBQVI7QUFERDtBQUZGLEdBRFM7O0FBUWZDLFVBQVEsVUFBU0MsT0FBVCxFQUFrQjs7QUFFeEIsVUFBTUMsY0FBYyxJQUFJQyxHQUFKLEVBQXBCO0FBQ0EsVUFBTUMscUJBQXFCLElBQUlELEdBQUosRUFBM0I7O0FBRUEsYUFBU0UsbUJBQVQsQ0FBNkJDLElBQTdCLEVBQW1DO0FBQ2pDLFlBQU1DLGNB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YyxDQUFqQixDLENBZEEiLCJmaWxlIjoicnVsZXMvbm8tbmFtZWQtYXMtZGVmYXVsdC1tZW1iZXIuanMiLCJzb3VyY2VzQ29udGVudCI6WyIvKipcbiAqIEBmaWxlb3ZlcnZpZXcgUnVsZSB0byB3YXJuIGFib3V0IHBvdGVudGlhbGx5IGNvbmZ1c2VkIHVzZSBvZiBuYW1lIGV4cG9ydHNcbiAqIEBhdXRob3IgRGVzbW9uZCBCcmFuZFxuICogQGNvcHlyaWdodCAyMDE2IERlc21vbmQgQnJhbmQuIEFsbCByaWdodHMgcmVzZXJ2ZWQuXG4gKiBTZWUgTElDRU5TRSBpbiByb290IGRpcmVjdG9yeSBmb3IgZnVsbCBsaWNlbnNlLlxuICovXG5pbXBvcnQgRXhwb3J0cyBmcm9tICcuLi9FeHBvcnRNYXAnXG5pbXBvcnQgaW1wb3J0RGVjbGFyYXRpb24gZnJvbSAnLi4vaW1wb3J0RGVjbGFyYXRpb24nXG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJ1xuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5tb2R1bGUuZXhwb3J0cyA9IHtcbiAgbWV0YToge1xuICAgIHR5cGU6ICdzdWdnZXN0aW9uJyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25vLW5hbWVkLWFzLWRlZmF1bHQtbWVtYmVyJyksXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGU6IGZ1bmN0aW9uKGNvbnRleHQpIHtcblxuICAgIGNvbnN0IGZpbGVJbXBvcnRzID0gbmV3IE1hcCgpXG4gICAgY29uc3QgYWxsUHJvcGVydHlMb29rdXBzID0gbmV3IE1hcCgpXG5cbiAgICBmdW5jdGlvbiBoYW5kbGVJbXBvcnREZWZhdWx0KG5vZGUpIHtcbiAgICAgIGNvbnN0IGRlY2xhcmF0aW9uID0gaW1wb3J0RGVjbGFyYXRpb24oY29udGV4dClcbiAgICAgIGNvbnN0IGV4cG9ydE1hcCA9IEV4cG9ydHMuZ2V0KGRlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZSwgY29udGV4dClcbiAgICAgIGlmIChleHBvcnRNYXAgPT0gbnVsbCkgcmV0dXJuXG5cbiAgICAgIGlmIChleHBvcnRNYXAuZXJyb3JzLmxlbmd0aCkge1xuICAgICAgICBleHBvcnRNYXAucmVwb3J0RXJyb3JzKGNvbnRleHQsIGRlY2xhcmF0aW9uKVxuICAgICAgICByZXR1cm5cbiAgICAgIH1cblxuICAgICAgZmlsZUltcG9ydHMuc2V0KG5vZGUubG9jYWwubmFtZSwge1xuICAgICAgICBleHBvcnRNYXAsXG4gICAgICAgIHNvdXJjZVBhdGg6IGRlY2xhcmF0aW9uLnNvdXJjZS52YWx1ZSxcbiAgICAgIH0pXG4gICAgfVxuXG4gICAgZnVuY3Rpb24gc3RvcmVQcm9wZXJ0eUxvb2t1cChvYmplY3ROYW1lLCBwcm9wTmFtZSwgbm9kZSkge1xuICAgICAgY29uc3QgbG9va3VwcyA9IGFsbFByb3BlcnR5TG9va3Vwcy5nZXQob2JqZWN0TmFtZSkgfHwgW11cbiAgICAgIGxvb2t1cHMucHVzaCh7bm9kZSwgcHJvcE5hbWV9KVxuICAgICAgYWxsUHJvcGVydHlMb29rdXBzLnNldChvYmplY3ROYW1lLCBsb29rdXBzKVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGhhbmRsZVByb3BMb29rdXAobm9kZSkge1xuICAgICAgY29uc3Qgb2JqZWN0TmFtZSA9IG5vZGUub2JqZWN0Lm5hbWVcbiAgICAgIGNvbnN0IHByb3BOYW1lID0gbm9kZS5wcm9wZXJ0eS5uYW1lXG4gICAgICBzdG9yZVByb3BlcnR5TG9va3VwKG9iamVjdE5hbWUsIHByb3BOYW1lLCBub2RlKVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGhhbmRsZURlc3RydWN0dXJpbmdBc3NpZ25tZW50KG5vZGUpIHtcbiAgICAgIGNvbnN0IGlzRGVzdHJ1Y3R1cmUgPSAoXG4gICAgICAgIG5vZGUuaWQudHlwZSA9PT0gJ09iamVjdFBhdHRlcm4nICYmXG4gICAgICAgIG5vZGUuaW5pdCAhPSBudWxsICYmXG4gICAgICAgIG5vZGUuaW5pdC50eXBlID09PSAnSWRlbnRpZmllcidcbiAgICAgIClcbiAgICAgIGlmICghaXNEZXN0cnVjdHVyZSkgcmV0dXJuXG5cbiAgICAgIGNvbnN0IG9iamVjdE5hbWUgPSBub2RlLmluaXQubmFtZVxuICAgICAgZm9yIChjb25zdCB7IGtleSB9IG9mIG5vZGUuaWQucHJvcGVydGllcykge1xuICAgICAgICBpZiAoa2V5ID09IG51bGwpIGNvbnRpbnVlICAvLyB0cnVlIGZvciByZXN0IHByb3BlcnRpZXNcbiAgICAgICAgc3RvcmVQcm9wZXJ0eUxvb2t1cChvYmplY3ROYW1lLCBrZXkubmFtZSwga2V5KVxuICAgICAgfVxuICAgIH1cblxuICAgIGZ1bmN0aW9uIGhhbmRsZVByb2dyYW1FeGl0KCkge1xuICAgICAgYWxsUHJvcGVydHlMb29rdXBzLmZvckVhY2goKGxvb2t1cHMsIG9iamVjdE5hbWUpID0+IHtcbiAgICAgICAgY29uc3QgZmlsZUltcG9ydCA9IGZpbGVJbXBvcnRzLmdldChvYmplY3ROYW1lKVxuICAgICAgICBpZiAoZmlsZUltcG9ydCA9PSBudWxsKSByZXR1cm5cblxuICAgICAgICBmb3IgKGNvbnN0IHtwcm9wTmFtZSwgbm9kZX0gb2YgbG9va3Vwcykge1xuICAgICAgICAgIC8vIHRoZSBkZWZhdWx0IGltcG9ydCBjYW4gaGF2ZSBhIFwiZGVmYXVsdFwiIHByb3BlcnR5XG4gICAgICAgICAgaWYgKHByb3BOYW1lID09PSAnZGVmYXVsdCcpIGNvbnRpbnVlXG4gICAgICAgICAgaWYgKCFmaWxlSW1wb3J0LmV4cG9ydE1hcC5uYW1lc3BhY2UuaGFzKHByb3BOYW1lKSkgY29udGludWVcblxuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICBtZXNzYWdlOiAoXG4gICAgICAgICAgICAgIGBDYXV0aW9uOiBcXGAke29iamVjdE5hbWV9XFxgIGFsc28gaGFzIGEgbmFtZWQgZXhwb3J0IGAgK1xuICAgICAgICAgICAgICBgXFxgJHtwcm9wTmFtZX1cXGAuIENoZWNrIGlmIHlvdSBtZWFudCB0byB3cml0ZSBgICtcbiAgICAgICAgICAgICAgYFxcYGltcG9ydCB7JHtwcm9wTmFtZX19IGZyb20gJyR7ZmlsZUltcG9ydC5zb3VyY2VQYXRofSdcXGAgYCArXG4gICAgICAgICAgICAgICdpbnN0ZWFkLidcbiAgICAgICAgICAgICksXG4gICAgICAgICAgfSlcbiAgICAgICAgfVxuICAgICAgfSlcbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgJ0ltcG9ydERlZmF1bHRTcGVjaWZpZXInOiBoYW5kbGVJbXBvcnREZWZhdWx0LFxuICAgICAgJ01lbWJlckV4cHJlc3Npb24nOiBoYW5kbGVQcm9wTG9va3VwLFxuICAgICAgJ1ZhcmlhYmxlRGVjbGFyYXRvcic6IGhhbmRsZURlc3RydWN0dXJpbmdBc3NpZ25tZW50LFxuICAgICAgJ1Byb2dyYW06ZXhpdCc6IGhhbmRsZVByb2dyYW1FeGl0LFxuICAgIH1cbiAgfSxcbn1cbiJdfQ==