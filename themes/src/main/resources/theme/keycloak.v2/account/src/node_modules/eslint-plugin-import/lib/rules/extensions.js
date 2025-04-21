'use strict';var _path = require('path');var _path2 = _interopRequireDefault(_path);

var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _importType = require('../core/importType');
var _moduleVisitor = require('eslint-module-utils/moduleVisitor');var _moduleVisitor2 = _interopRequireDefault(_moduleVisitor);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var enumValues = { 'enum': ['always', 'ignorePackages', 'never'] };
var patternProperties = {
  type: 'object',
  patternProperties: { '.*': enumValues } };

var properties = {
  type: 'object',
  properties: {
    'pattern': patternProperties,
    'ignorePackages': { type: 'boolean' } } };



function buildProperties(context) {

  var result = {
    defaultConfig: 'never',
    pattern: {},
    ignorePackages: false };


  context.options.forEach(function (obj) {

    // If this is a string, set defaultConfig to its value
    if (typeof obj === 'string') {
      result.defaultConfig = obj;
      return;
    }

    // If this is not the new structure, transfer all props to result.pattern
    if (obj.pattern === undefined && obj.ignorePackages === undefined) {
      Object.assign(result.pattern, obj);
      return;
    }

    // If pattern is provided, transfer all props
    if (obj.pattern !== undefined) {
      Object.assign(result.pattern, obj.pattern);
    }

    // If ignorePackages is provided, transfer it to result
    if (obj.ignorePackages !== undefined) {
      result.ignorePackages = obj.ignorePackages;
    }
  });

  if (result.defaultConfig === 'ignorePackages') {
    result.defaultConfig = 'always';
    result.ignorePackages = true;
  }

  return result;
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('extensions') },


    schema: {
      anyOf: [
      {
        type: 'array',
        items: [enumValues],
        additionalItems: false },

      {
        type: 'array',
        items: [
        enumValues,
        properties],

        additionalItems: false },

      {
        type: 'array',
        items: [properties],
        additionalItems: false },

      {
        type: 'array',
        items: [patternProperties],
        additionalItems: false },

      {
        type: 'array',
        items: [
        enumValues,
        patternProperties],

        additionalItems: false }] } },





  create: function () {function create(context) {

      var props = buildProperties(context);

      function getModifier(extension) {
        return props.pattern[extension] || props.defaultConfig;
      }

      function isUseOfExtensionRequired(extension, isPackage) {
        return getModifier(extension) === 'always' && (!props.ignorePackages || !isPackage);
      }

      function isUseOfExtensionForbidden(extension) {
        return getModifier(extension) === 'never';
      }

      function isResolvableWithoutExtension(file) {
        var extension = _path2['default'].extname(file);
        var fileWithoutExtension = file.slice(0, -extension.length);
        var resolvedFileWithoutExtension = (0, _resolve2['default'])(fileWithoutExtension, context);

        return resolvedFileWithoutExtension === (0, _resolve2['default'])(file, context);
      }

      function isExternalRootModule(file) {
        var slashCount = file.split('/').length - 1;

        if (slashCount === 0) return true;
        if ((0, _importType.isScoped)(file) && slashCount <= 1) return true;
        return false;
      }

      function checkFileExtension(source, node) {
        // bail if the declaration doesn't have a source, e.g. "export { foo };", or if it's only partially typed like in an editor
        if (!source || !source.value) return;

        var importPathWithQueryString = source.value;

        // don't enforce anything on builtins
        if ((0, _importType.isBuiltIn)(importPathWithQueryString, context.settings)) return;

        var importPath = importPathWithQueryString.replace(/\?(.*)$/, '');

        // don't enforce in root external packages as they may have names with `.js`.
        // Like `import Decimal from decimal.js`)
        if (isExternalRootModule(importPath)) return;

        var resolvedPath = (0, _resolve2['default'])(importPath, context);

        // get extension from resolved path, if possible.
        // for unresolved, use source value.
        var extension = _path2['default'].extname(resolvedPath || importPath).substring(1);

        // determine if this is a module
        var isPackage = (0, _importType.isExternalModule)(
        importPath,
        (0, _resolve2['default'])(importPath, context),
        context) ||
        (0, _importType.isScoped)(importPath);

        if (!extension || !importPath.endsWith('.' + String(extension))) {
          // ignore type-only imports
          if (node.importKind === 'type') return;
          var extensionRequired = isUseOfExtensionRequired(extension, isPackage);
          var extensionForbidden = isUseOfExtensionForbidden(extension);
          if (extensionRequired && !extensionForbidden) {
            context.report({
              node: source,
              message: 'Missing file extension ' + (
              extension ? '"' + String(extension) + '" ' : '') + 'for "' + String(importPathWithQueryString) + '"' });

          }
        } else if (extension) {
          if (isUseOfExtensionForbidden(extension) && isResolvableWithoutExtension(importPath)) {
            context.report({
              node: source,
              message: 'Unexpected use of file extension "' + String(extension) + '" for "' + String(importPathWithQueryString) + '"' });

          }
        }
      }

      return (0, _moduleVisitor2['default'])(checkFileExtension, { commonjs: true });
    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9leHRlbnNpb25zLmpzIl0sIm5hbWVzIjpbImVudW1WYWx1ZXMiLCJwYXR0ZXJuUHJvcGVydGllcyIsInR5cGUiLCJwcm9wZXJ0aWVzIiwiYnVpbGRQcm9wZXJ0aWVzIiwiY29udGV4dCIsInJlc3VsdCIsImRlZmF1bHRDb25maWciLCJwYXR0ZXJuIiwiaWdub3JlUGFja2FnZXMiLCJvcHRpb25zIiwiZm9yRWFjaCIsIm9iaiIsInVuZGVmaW5lZCIsIk9iamVjdCIsImFzc2lnbiIsIm1vZHVsZSIsImV4cG9ydHMiLCJtZXRhIiwiZG9jcyIsInVybCIsInNjaGVtYSIsImFueU9mIiwiaXRlbXMiLCJhZGRpdGlvbmFsSXRlbXMiLCJjcmVhdGUiLCJwcm9wcyIsImdldE1vZGlmaWVyIiwiZXh0ZW5zaW9uIiwiaXNVc2VPZkV4dGVuc2lvblJlcXVpcmVkIiwiaXNQYWNrYWdlIiwiaXNVc2VPZkV4dGVuc2lvbkZvcmJpZGRlbiIsImlzUmVzb2x2YWJsZVdpdGhvdXRFeHRlbnNpb24iLCJmaWxlIiwicGF0aCIsImV4dG5hbWUiLCJmaWxlV2l0aG91dEV4dGVuc2lvbiIsInNsaWNlIiwibGVuZ3RoIiwicmVzb2x2ZWRGaWxlV2l0aG91dEV4dGVuc2lvbiIsImlzRXh0ZXJuYWxSb290TW9kdWxlIiwic2xhc2hDb3VudCIsInNwbGl0IiwiY2hlY2tGaWxlRXh0ZW5zaW9uIiwic291cmNlIiwibm9kZSIsInZhbHVlIiwiaW1wb3J0UGF0aFdpdGhRdWVyeVN0cmluZyIsInNldHRpbmdzIiwiaW1wb3J0UGF0aCIsInJlcGxhY2UiLCJyZXNvbHZlZFBhdGgiLCJzdWJzdHJpbmciLCJlbmRzV2l0aCIsImltcG9ydEtpbmQiLCJleHRlbnNpb25SZXF1aXJlZCIsImV4dGVuc2lvbkZvcmJpZGRlbiIsInJlcG9ydCIsIm1lc3NhZ2UiLCJjb21tb25qcyJdLCJtYXBwaW5ncyI6ImFBQUEsNEI7O0FBRUEsc0Q7QUFDQTtBQUNBLGtFO0FBQ0EscUM7O0FB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bGUiOiJleHRlbnNpb25zLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHBhdGggZnJvbSAncGF0aCc7XG5cbmltcG9ydCByZXNvbHZlIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvcmVzb2x2ZSc7XG5pbXBvcnQgeyBpc0J1aWx0SW4sIGlzRXh0ZXJuYWxNb2R1bGUsIGlzU2NvcGVkIH0gZnJvbSAnLi4vY29yZS9pbXBvcnRUeXBlJztcbmltcG9ydCBtb2R1bGVWaXNpdG9yIGZyb20gJ2VzbGludC1tb2R1bGUtdXRpbHMvbW9kdWxlVmlzaXRvcic7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuY29uc3QgZW51bVZhbHVlcyA9IHsgZW51bTogWyAnYWx3YXlzJywgJ2lnbm9yZVBhY2thZ2VzJywgJ25ldmVyJyBdIH07XG5jb25zdCBwYXR0ZXJuUHJvcGVydGllcyA9IHtcbiAgdHlwZTogJ29iamVjdCcsXG4gIHBhdHRlcm5Qcm9wZXJ0aWVzOiB7ICcuKic6IGVudW1WYWx1ZXMgfSxcbn07XG5jb25zdCBwcm9wZXJ0aWVzID0ge1xuICB0eXBlOiAnb2JqZWN0JyxcbiAgcHJvcGVydGllczoge1xuICAgICdwYXR0ZXJuJzogcGF0dGVyblByb3BlcnRpZXMsXG4gICAgJ2lnbm9yZVBhY2thZ2VzJzogeyB0eXBlOiAnYm9vbGVhbicgfSxcbiAgfSxcbn07XG5cbmZ1bmN0aW9uIGJ1aWxkUHJvcGVydGllcyhjb250ZXh0KSB7XG5cbiAgY29uc3QgcmVzdWx0ID0ge1xuICAgIGRlZmF1bHRDb25maWc6ICduZXZlcicsXG4gICAgcGF0dGVybjoge30sXG4gICAgaWdub3JlUGFja2FnZXM6IGZhbHNlLFxuICB9O1xuXG4gIGNvbnRleHQub3B0aW9ucy5mb3JFYWNoKG9iaiA9PiB7XG5cbiAgICAvLyBJZiB0aGlzIGlzIGEgc3RyaW5nLCBzZXQgZGVmYXVsdENvbmZpZyB0byBpdHMgdmFsdWVcbiAgICBpZiAodHlwZW9mIG9iaiA9PT0gJ3N0cmluZycpIHtcbiAgICAgIHJlc3VsdC5kZWZhdWx0Q29uZmlnID0gb2JqO1xuICAgICAgcmV0dXJuO1xuICAgIH1cblxuICAgIC8vIElmIHRoaXMgaXMgbm90IHRoZSBuZXcgc3RydWN0dXJlLCB0cmFuc2ZlciBhbGwgcHJvcHMgdG8gcmVzdWx0LnBhdHRlcm5cbiAgICBpZiAob2JqLnBhdHRlcm4gPT09IHVuZGVmaW5lZCAmJiBvYmouaWdub3JlUGFja2FnZXMgPT09IHVuZGVmaW5lZCkge1xuICAgICAgT2JqZWN0LmFzc2lnbihyZXN1bHQucGF0dGVybiwgb2JqKTtcbiAgICAgIHJldHVybjtcbiAgICB9XG5cbiAgICAvLyBJZiBwYXR0ZXJuIGlzIHByb3ZpZGVkLCB0cmFuc2ZlciBhbGwgcHJvcHNcbiAgICBpZiAob2JqLnBhdHRlcm4gIT09IHVuZGVmaW5lZCkge1xuICAgICAgT2JqZWN0LmFzc2lnbihyZXN1bHQucGF0dGVybiwgb2JqLnBhdHRlcm4pO1xuICAgIH1cblxuICAgIC8vIElmIGlnbm9yZVBhY2thZ2VzIGlzIHByb3ZpZGVkLCB0cmFuc2ZlciBpdCB0byByZXN1bHRcbiAgICBpZiAob2JqLmlnbm9yZVBhY2thZ2VzICE9PSB1bmRlZmluZWQpIHtcbiAgICAgIHJlc3VsdC5pZ25vcmVQYWNrYWdlcyA9IG9iai5pZ25vcmVQYWNrYWdlcztcbiAgICB9XG4gIH0pO1xuXG4gIGlmIChyZXN1bHQuZGVmYXVsdENvbmZpZyA9PT0gJ2lnbm9yZVBhY2thZ2VzJykge1xuICAgIHJlc3VsdC5kZWZhdWx0Q29uZmlnID0gJ2Fsd2F5cyc7XG4gICAgcmVzdWx0Lmlnbm9yZVBhY2thZ2VzID0gdHJ1ZTtcbiAgfVxuXG4gIHJldHVybiByZXN1bHQ7XG59XG5cbm1vZHVsZS5leHBvcnRzID0ge1xuICBtZXRhOiB7XG4gICAgdHlwZTogJ3N1Z2dlc3Rpb24nLFxuICAgIGRvY3M6IHtcbiAgICAgIHVybDogZG9jc1VybCgnZXh0ZW5zaW9ucycpLFxuICAgIH0sXG5cbiAgICBzY2hlbWE6IHtcbiAgICAgIGFueU9mOiBbXG4gICAgICAgIHtcbiAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIGl0ZW1zOiBbZW51bVZhbHVlc10sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgICAge1xuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgaXRlbXM6IFtcbiAgICAgICAgICAgIGVudW1WYWx1ZXMsXG4gICAgICAgICAgICBwcm9wZXJ0aWVzLFxuICAgICAgICAgIF0sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgICAge1xuICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgaXRlbXM6IFtwcm9wZXJ0aWVzXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW3BhdHRlcm5Qcm9wZXJ0aWVzXSxcbiAgICAgICAgICBhZGRpdGlvbmFsSXRlbXM6IGZhbHNlLFxuICAgICAgICB9LFxuICAgICAgICB7XG4gICAgICAgICAgdHlwZTogJ2FycmF5JyxcbiAgICAgICAgICBpdGVtczogW1xuICAgICAgICAgICAgZW51bVZhbHVlcyxcbiAgICAgICAgICAgIHBhdHRlcm5Qcm9wZXJ0aWVzLFxuICAgICAgICAgIF0sXG4gICAgICAgICAgYWRkaXRpb25hbEl0ZW1zOiBmYWxzZSxcbiAgICAgICAgfSxcbiAgICAgIF0sXG4gICAgfSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuXG4gICAgY29uc3QgcHJvcHMgPSBidWlsZFByb3BlcnRpZXMoY29udGV4dCk7XG5cbiAgICBmdW5jdGlvbiBnZXRNb2RpZmllcihleHRlbnNpb24pIHtcbiAgICAgIHJldHVybiBwcm9wcy5wYXR0ZXJuW2V4dGVuc2lvbl0gfHwgcHJvcHMuZGVmYXVsdENvbmZpZztcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBpc1VzZU9mRXh0ZW5zaW9uUmVxdWlyZWQoZXh0ZW5zaW9uLCBpc1BhY2thZ2UpIHtcbiAgICAgIHJldHVybiBnZXRNb2RpZmllcihleHRlbnNpb24pID09PSAnYWx3YXlzJyAmJiAoIXByb3BzLmlnbm9yZVBhY2thZ2VzIHx8ICFpc1BhY2thZ2UpO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzVXNlT2ZFeHRlbnNpb25Gb3JiaWRkZW4oZXh0ZW5zaW9uKSB7XG4gICAgICByZXR1cm4gZ2V0TW9kaWZpZXIoZXh0ZW5zaW9uKSA9PT0gJ25ldmVyJztcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBpc1Jlc29sdmFibGVXaXRob3V0RXh0ZW5zaW9uKGZpbGUpIHtcbiAgICAgIGNvbnN0IGV4dGVuc2lvbiA9IHBhdGguZXh0bmFtZShmaWxlKTtcbiAgICAgIGNvbnN0IGZpbGVXaXRob3V0RXh0ZW5zaW9uID0gZmlsZS5zbGljZSgwLCAtZXh0ZW5zaW9uLmxlbmd0aCk7XG4gICAgICBjb25zdCByZXNvbHZlZEZpbGVXaXRob3V0RXh0ZW5zaW9uID0gcmVzb2x2ZShmaWxlV2l0aG91dEV4dGVuc2lvbiwgY29udGV4dCk7XG5cbiAgICAgIHJldHVybiByZXNvbHZlZEZpbGVXaXRob3V0RXh0ZW5zaW9uID09PSByZXNvbHZlKGZpbGUsIGNvbnRleHQpO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGlzRXh0ZXJuYWxSb290TW9kdWxlKGZpbGUpIHtcbiAgICAgIGNvbnN0IHNsYXNoQ291bnQgPSBmaWxlLnNwbGl0KCcvJykubGVuZ3RoIC0gMTtcblxuICAgICAgaWYgKHNsYXNoQ291bnQgPT09IDApICByZXR1cm4gdHJ1ZTtcbiAgICAgIGlmIChpc1Njb3BlZChmaWxlKSAmJiBzbGFzaENvdW50IDw9IDEpIHJldHVybiB0cnVlO1xuICAgICAgcmV0dXJuIGZhbHNlO1xuICAgIH1cblxuICAgIGZ1bmN0aW9uIGNoZWNrRmlsZUV4dGVuc2lvbihzb3VyY2UsIG5vZGUpIHtcbiAgICAgIC8vIGJhaWwgaWYgdGhlIGRlY2xhcmF0aW9uIGRvZXNuJ3QgaGF2ZSBhIHNvdXJjZSwgZS5nLiBcImV4cG9ydCB7IGZvbyB9O1wiLCBvciBpZiBpdCdzIG9ubHkgcGFydGlhbGx5IHR5cGVkIGxpa2UgaW4gYW4gZWRpdG9yXG4gICAgICBpZiAoIXNvdXJjZSB8fCAhc291cmNlLnZhbHVlKSByZXR1cm47XG4gICAgICBcbiAgICAgIGNvbnN0IGltcG9ydFBhdGhXaXRoUXVlcnlTdHJpbmcgPSBzb3VyY2UudmFsdWU7XG5cbiAgICAgIC8vIGRvbid0IGVuZm9yY2UgYW55dGhpbmcgb24gYnVpbHRpbnNcbiAgICAgIGlmIChpc0J1aWx0SW4oaW1wb3J0UGF0aFdpdGhRdWVyeVN0cmluZywgY29udGV4dC5zZXR0aW5ncykpIHJldHVybjtcblxuICAgICAgY29uc3QgaW1wb3J0UGF0aCA9IGltcG9ydFBhdGhXaXRoUXVlcnlTdHJpbmcucmVwbGFjZSgvXFw/KC4qKSQvLCAnJyk7XG5cbiAgICAgIC8vIGRvbid0IGVuZm9yY2UgaW4gcm9vdCBleHRlcm5hbCBwYWNrYWdlcyBhcyB0aGV5IG1heSBoYXZlIG5hbWVzIHdpdGggYC5qc2AuXG4gICAgICAvLyBMaWtlIGBpbXBvcnQgRGVjaW1hbCBmcm9tIGRlY2ltYWwuanNgKVxuICAgICAgaWYgKGlzRXh0ZXJuYWxSb290TW9kdWxlKGltcG9ydFBhdGgpKSByZXR1cm47XG5cbiAgICAgIGNvbnN0IHJlc29sdmVkUGF0aCA9IHJlc29sdmUoaW1wb3J0UGF0aCwgY29udGV4dCk7XG5cbiAgICAgIC8vIGdldCBleHRlbnNpb24gZnJvbSByZXNvbHZlZCBwYXRoLCBpZiBwb3NzaWJsZS5cbiAgICAgIC8vIGZvciB1bnJlc29sdmVkLCB1c2Ugc291cmNlIHZhbHVlLlxuICAgICAgY29uc3QgZXh0ZW5zaW9uID0gcGF0aC5leHRuYW1lKHJlc29sdmVkUGF0aCB8fCBpbXBvcnRQYXRoKS5zdWJzdHJpbmcoMSk7XG5cbiAgICAgIC8vIGRldGVybWluZSBpZiB0aGlzIGlzIGEgbW9kdWxlXG4gICAgICBjb25zdCBpc1BhY2thZ2UgPSBpc0V4dGVybmFsTW9kdWxlKFxuICAgICAgICBpbXBvcnRQYXRoLFxuICAgICAgICByZXNvbHZlKGltcG9ydFBhdGgsIGNvbnRleHQpLFxuICAgICAgICBjb250ZXh0LFxuICAgICAgKSB8fCBpc1Njb3BlZChpbXBvcnRQYXRoKTtcblxuICAgICAgaWYgKCFleHRlbnNpb24gfHwgIWltcG9ydFBhdGguZW5kc1dpdGgoYC4ke2V4dGVuc2lvbn1gKSkge1xuICAgICAgICAvLyBpZ25vcmUgdHlwZS1vbmx5IGltcG9ydHNcbiAgICAgICAgaWYgKG5vZGUuaW1wb3J0S2luZCA9PT0gJ3R5cGUnKSByZXR1cm47XG4gICAgICAgIGNvbnN0IGV4dGVuc2lvblJlcXVpcmVkID0gaXNVc2VPZkV4dGVuc2lvblJlcXVpcmVkKGV4dGVuc2lvbiwgaXNQYWNrYWdlKTtcbiAgICAgICAgY29uc3QgZXh0ZW5zaW9uRm9yYmlkZGVuID0gaXNVc2VPZkV4dGVuc2lvbkZvcmJpZGRlbihleHRlbnNpb24pO1xuICAgICAgICBpZiAoZXh0ZW5zaW9uUmVxdWlyZWQgJiYgIWV4dGVuc2lvbkZvcmJpZGRlbikge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGU6IHNvdXJjZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6XG4gICAgICAgICAgICAgIGBNaXNzaW5nIGZpbGUgZXh0ZW5zaW9uICR7ZXh0ZW5zaW9uID8gYFwiJHtleHRlbnNpb259XCIgYCA6ICcnfWZvciBcIiR7aW1wb3J0UGF0aFdpdGhRdWVyeVN0cmluZ31cImAsXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH0gZWxzZSBpZiAoZXh0ZW5zaW9uKSB7XG4gICAgICAgIGlmIChpc1VzZU9mRXh0ZW5zaW9uRm9yYmlkZGVuKGV4dGVuc2lvbikgJiYgaXNSZXNvbHZhYmxlV2l0aG91dEV4dGVuc2lvbihpbXBvcnRQYXRoKSkge1xuICAgICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICAgIG5vZGU6IHNvdXJjZSxcbiAgICAgICAgICAgIG1lc3NhZ2U6IGBVbmV4cGVjdGVkIHVzZSBvZiBmaWxlIGV4dGVuc2lvbiBcIiR7ZXh0ZW5zaW9ufVwiIGZvciBcIiR7aW1wb3J0UGF0aFdpdGhRdWVyeVN0cmluZ31cImAsXG4gICAgICAgICAgfSk7XG4gICAgICAgIH1cbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4gbW9kdWxlVmlzaXRvcihjaGVja0ZpbGVFeHRlbnNpb24sIHsgY29tbW9uanM6IHRydWUgfSk7XG4gIH0sXG59O1xuIl19