'use strict';var _slicedToArray = function () {function sliceIterator(arr, i) {var _arr = [];var _n = true;var _d = false;var _e = undefined;try {for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {_arr.push(_s.value);if (i && _arr.length === i) break;}} catch (err) {_d = true;_e = err;} finally {try {if (!_n && _i["return"]) _i["return"]();} finally {if (_d) throw _e;}}return _arr;}return function (arr, i) {if (Array.isArray(arr)) {return arr;} else if (Symbol.iterator in Object(arr)) {return sliceIterator(arr, i);} else {throw new TypeError("Invalid attempt to destructure non-iterable instance");}};}();var _resolve = require('eslint-module-utils/resolve');var _resolve2 = _interopRequireDefault(_resolve);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}function _toConsumableArray(arr) {if (Array.isArray(arr)) {for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) {arr2[i] = arr[i];}return arr2;} else {return Array.from(arr);}}function _toArray(arr) {return Array.isArray(arr) ? arr : Array.from(arr);}

function checkImports(imported, context) {var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
    for (var _iterator = imported.entries()[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var _ref = _step.value;var _ref2 = _slicedToArray(_ref, 2);var _module = _ref2[0];var nodes = _ref2[1];
      if (nodes.length > 1) {
        var message = '\'' + String(_module) + '\' imported multiple times.';var _nodes = _toArray(
        nodes),first = _nodes[0],rest = _nodes.slice(1);
        var sourceCode = context.getSourceCode();
        var fix = getFix(first, rest, sourceCode);

        context.report({
          node: first.source,
          message: message,
          fix: fix // Attach the autofix (if any) to the first import.
        });var _iteratorNormalCompletion2 = true;var _didIteratorError2 = false;var _iteratorError2 = undefined;try {

          for (var _iterator2 = rest[Symbol.iterator](), _step2; !(_iteratorNormalCompletion2 = (_step2 = _iterator2.next()).done); _iteratorNormalCompletion2 = true) {var node = _step2.value;
            context.report({
              node: node.source,
              message: message });

          }} catch (err) {_didIteratorError2 = true;_iteratorError2 = err;} finally {try {if (!_iteratorNormalCompletion2 && _iterator2['return']) {_iterator2['return']();}} finally {if (_didIteratorError2) {throw _iteratorError2;}}}
      }
    }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
}

function getFix(first, rest, sourceCode) {
  // Sorry ESLint <= 3 users, no autofix for you. Autofixing duplicate imports
  // requires multiple `fixer.whatever()` calls in the `fix`: We both need to
  // update the first one, and remove the rest. Support for multiple
  // `fixer.whatever()` in a single `fix` was added in ESLint 4.1.
  // `sourceCode.getCommentsBefore` was added in 4.0, so that's an easy thing to
  // check for.
  if (typeof sourceCode.getCommentsBefore !== 'function') {
    return undefined;
  }

  // Adjusting the first import might make it multiline, which could break
  // `eslint-disable-next-line` comments and similar, so bail if the first
  // import has comments. Also, if the first import is `import * as ns from
  // './foo'` there's nothing we can do.
  if (hasProblematicComments(first, sourceCode) || hasNamespace(first)) {
    return undefined;
  }

  var defaultImportNames = new Set(
  [first].concat(_toConsumableArray(rest)).map(getDefaultImportName).filter(Boolean));


  // Bail if there are multiple different default import names – it's up to the
  // user to choose which one to keep.
  if (defaultImportNames.size > 1) {
    return undefined;
  }

  // Leave it to the user to handle comments. Also skip `import * as ns from
  // './foo'` imports, since they cannot be merged into another import.
  var restWithoutComments = rest.filter(function (node) {return !(
    hasProblematicComments(node, sourceCode) ||
    hasNamespace(node));});


  var specifiers = restWithoutComments.
  map(function (node) {
    var tokens = sourceCode.getTokens(node);
    var openBrace = tokens.find(function (token) {return isPunctuator(token, '{');});
    var closeBrace = tokens.find(function (token) {return isPunctuator(token, '}');});

    if (openBrace == null || closeBrace == null) {
      return undefined;
    }

    return {
      importNode: node,
      text: sourceCode.text.slice(openBrace.range[1], closeBrace.range[0]),
      hasTrailingComma: isPunctuator(sourceCode.getTokenBefore(closeBrace), ','),
      isEmpty: !hasSpecifiers(node) };

  }).
  filter(Boolean);

  var unnecessaryImports = restWithoutComments.filter(function (node) {return (
      !hasSpecifiers(node) &&
      !hasNamespace(node) &&
      !specifiers.some(function (specifier) {return specifier.importNode === node;}));});


  var shouldAddDefault = getDefaultImportName(first) == null && defaultImportNames.size === 1;
  var shouldAddSpecifiers = specifiers.length > 0;
  var shouldRemoveUnnecessary = unnecessaryImports.length > 0;

  if (!(shouldAddDefault || shouldAddSpecifiers || shouldRemoveUnnecessary)) {
    return undefined;
  }

  return function (fixer) {
    var tokens = sourceCode.getTokens(first);
    var openBrace = tokens.find(function (token) {return isPunctuator(token, '{');});
    var closeBrace = tokens.find(function (token) {return isPunctuator(token, '}');});
    var firstToken = sourceCode.getFirstToken(first);var _defaultImportNames = _slicedToArray(
    defaultImportNames, 1),defaultImportName = _defaultImportNames[0];

    var firstHasTrailingComma =
    closeBrace != null &&
    isPunctuator(sourceCode.getTokenBefore(closeBrace), ',');
    var firstIsEmpty = !hasSpecifiers(first);var _specifiers$reduce =

    specifiers.reduce(
    function (_ref3, specifier) {var _ref4 = _slicedToArray(_ref3, 2),result = _ref4[0],needsComma = _ref4[1];
      return [
      needsComma && !specifier.isEmpty ? String(
      result) + ',' + String(specifier.text) : '' + String(
      result) + String(specifier.text),
      specifier.isEmpty ? needsComma : true];

    },
    ['', !firstHasTrailingComma && !firstIsEmpty]),_specifiers$reduce2 = _slicedToArray(_specifiers$reduce, 1),specifiersText = _specifiers$reduce2[0];


    var fixes = [];

    if (shouldAddDefault && openBrace == null && shouldAddSpecifiers) {
      // `import './foo'` → `import def, {...} from './foo'`
      fixes.push(
      fixer.insertTextAfter(firstToken, ' ' + String(defaultImportName) + ', {' + String(specifiersText) + '} from'));

    } else if (shouldAddDefault && openBrace == null && !shouldAddSpecifiers) {
      // `import './foo'` → `import def from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ' ' + String(defaultImportName) + ' from'));
    } else if (shouldAddDefault && openBrace != null && closeBrace != null) {
      // `import {...} from './foo'` → `import def, {...} from './foo'`
      fixes.push(fixer.insertTextAfter(firstToken, ' ' + String(defaultImportName) + ','));
      if (shouldAddSpecifiers) {
        // `import def, {...} from './foo'` → `import def, {..., ...} from './foo'`
        fixes.push(fixer.insertTextBefore(closeBrace, specifiersText));
      }
    } else if (!shouldAddDefault && openBrace == null && shouldAddSpecifiers) {
      if (first.specifiers.length === 0) {
        // `import './foo'` → `import {...} from './foo'`
        fixes.push(fixer.insertTextAfter(firstToken, ' {' + String(specifiersText) + '} from'));
      } else {
        // `import def from './foo'` → `import def, {...} from './foo'`
        fixes.push(fixer.insertTextAfter(first.specifiers[0], ', {' + String(specifiersText) + '}'));
      }
    } else if (!shouldAddDefault && openBrace != null && closeBrace != null) {
      // `import {...} './foo'` → `import {..., ...} from './foo'`
      fixes.push(fixer.insertTextBefore(closeBrace, specifiersText));
    }

    // Remove imports whose specifiers have been moved into the first import.
    var _iteratorNormalCompletion3 = true;var _didIteratorError3 = false;var _iteratorError3 = undefined;try {for (var _iterator3 = specifiers[Symbol.iterator](), _step3; !(_iteratorNormalCompletion3 = (_step3 = _iterator3.next()).done); _iteratorNormalCompletion3 = true) {var specifier = _step3.value;
        var importNode = specifier.importNode;
        fixes.push(fixer.remove(importNode));

        var charAfterImportRange = [importNode.range[1], importNode.range[1] + 1];
        var charAfterImport = sourceCode.text.substring(charAfterImportRange[0], charAfterImportRange[1]);
        if (charAfterImport === '\n') {
          fixes.push(fixer.removeRange(charAfterImportRange));
        }
      }

      // Remove imports whose default import has been moved to the first import,
      // and side-effect-only imports that are unnecessary due to the first
      // import.
    } catch (err) {_didIteratorError3 = true;_iteratorError3 = err;} finally {try {if (!_iteratorNormalCompletion3 && _iterator3['return']) {_iterator3['return']();}} finally {if (_didIteratorError3) {throw _iteratorError3;}}}var _iteratorNormalCompletion4 = true;var _didIteratorError4 = false;var _iteratorError4 = undefined;try {for (var _iterator4 = unnecessaryImports[Symbol.iterator](), _step4; !(_iteratorNormalCompletion4 = (_step4 = _iterator4.next()).done); _iteratorNormalCompletion4 = true) {var node = _step4.value;
        fixes.push(fixer.remove(node));

        var charAfterImportRange = [node.range[1], node.range[1] + 1];
        var charAfterImport = sourceCode.text.substring(charAfterImportRange[0], charAfterImportRange[1]);
        if (charAfterImport === '\n') {
          fixes.push(fixer.removeRange(charAfterImportRange));
        }
      }} catch (err) {_didIteratorError4 = true;_iteratorError4 = err;} finally {try {if (!_iteratorNormalCompletion4 && _iterator4['return']) {_iterator4['return']();}} finally {if (_didIteratorError4) {throw _iteratorError4;}}}

    return fixes;
  };
}

function isPunctuator(node, value) {
  return node.type === 'Punctuator' && node.value === value;
}

// Get the name of the default import of `node`, if any.
function getDefaultImportName(node) {
  var defaultSpecifier = node.specifiers.
  find(function (specifier) {return specifier.type === 'ImportDefaultSpecifier';});
  return defaultSpecifier != null ? defaultSpecifier.local.name : undefined;
}

// Checks whether `node` has a namespace import.
function hasNamespace(node) {
  var specifiers = node.specifiers.
  filter(function (specifier) {return specifier.type === 'ImportNamespaceSpecifier';});
  return specifiers.length > 0;
}

// Checks whether `node` has any non-default specifiers.
function hasSpecifiers(node) {
  var specifiers = node.specifiers.
  filter(function (specifier) {return specifier.type === 'ImportSpecifier';});
  return specifiers.length > 0;
}

// It's not obvious what the user wants to do with comments associated with
// duplicate imports, so skip imports with comments when autofixing.
function hasProblematicComments(node, sourceCode) {
  return (
    hasCommentBefore(node, sourceCode) ||
    hasCommentAfter(node, sourceCode) ||
    hasCommentInsideNonSpecifiers(node, sourceCode));

}

// Checks whether `node` has a comment (that ends) on the previous line or on
// the same line as `node` (starts).
function hasCommentBefore(node, sourceCode) {
  return sourceCode.getCommentsBefore(node).
  some(function (comment) {return comment.loc.end.line >= node.loc.start.line - 1;});
}

// Checks whether `node` has a comment (that starts) on the same line as `node`
// (ends).
function hasCommentAfter(node, sourceCode) {
  return sourceCode.getCommentsAfter(node).
  some(function (comment) {return comment.loc.start.line === node.loc.end.line;});
}

// Checks whether `node` has any comments _inside,_ except inside the `{...}`
// part (if any).
function hasCommentInsideNonSpecifiers(node, sourceCode) {
  var tokens = sourceCode.getTokens(node);
  var openBraceIndex = tokens.findIndex(function (token) {return isPunctuator(token, '{');});
  var closeBraceIndex = tokens.findIndex(function (token) {return isPunctuator(token, '}');});
  // Slice away the first token, since we're no looking for comments _before_
  // `node` (only inside). If there's a `{...}` part, look for comments before
  // the `{`, but not before the `}` (hence the `+1`s).
  var someTokens = openBraceIndex >= 0 && closeBraceIndex >= 0 ?
  tokens.slice(1, openBraceIndex + 1).concat(tokens.slice(closeBraceIndex + 1)) :
  tokens.slice(1);
  return someTokens.some(function (token) {return sourceCode.getCommentsBefore(token).length > 0;});
}

module.exports = {
  meta: {
    type: 'problem',
    docs: {
      url: (0, _docsUrl2['default'])('no-duplicates') },

    fixable: 'code',
    schema: [
    {
      type: 'object',
      properties: {
        considerQueryString: {
          type: 'boolean' } },


      additionalProperties: false }] },




  create: function () {function create(context) {
      // Prepare the resolver from options.
      var considerQueryStringOption = context.options[0] &&
      context.options[0]['considerQueryString'];
      var defaultResolver = function () {function defaultResolver(sourcePath) {return (0, _resolve2['default'])(sourcePath, context) || sourcePath;}return defaultResolver;}();
      var resolver = considerQueryStringOption ? function (sourcePath) {
        var parts = sourcePath.match(/^([^?]*)\?(.*)$/);
        if (!parts) {
          return defaultResolver(sourcePath);
        }
        return defaultResolver(parts[1]) + '?' + parts[2];
      } : defaultResolver;

      var moduleMaps = new Map();

      function getImportMap(n) {
        if (!moduleMaps.has(n.parent)) {
          moduleMaps.set(n.parent, {
            imported: new Map(),
            nsImported: new Map(),
            defaultTypesImported: new Map(),
            namedTypesImported: new Map() });

        }
        var map = moduleMaps.get(n.parent);
        if (n.importKind === 'type') {
          return n.specifiers.length > 0 && n.specifiers[0].type === 'ImportDefaultSpecifier' ? map.defaultTypesImported : map.namedTypesImported;
        }

        return hasNamespace(n) ? map.nsImported : map.imported;
      }

      return {
        ImportDeclaration: function () {function ImportDeclaration(n) {
            // resolved path will cover aliased duplicates
            var resolvedPath = resolver(n.source.value);
            var importMap = getImportMap(n);

            if (importMap.has(resolvedPath)) {
              importMap.get(resolvedPath).push(n);
            } else {
              importMap.set(resolvedPath, [n]);
            }
          }return ImportDeclaration;}(),

        'Program:exit': function () {function ProgramExit() {var _iteratorNormalCompletion5 = true;var _didIteratorError5 = false;var _iteratorError5 = undefined;try {
              for (var _iterator5 = moduleMaps.values()[Symbol.iterator](), _step5; !(_iteratorNormalCompletion5 = (_step5 = _iterator5.next()).done); _iteratorNormalCompletion5 = true) {var map = _step5.value;
                checkImports(map.imported, context);
                checkImports(map.nsImported, context);
                checkImports(map.defaultTypesImported, context);
                checkImports(map.namedTypesImported, context);
              }} catch (err) {_didIteratorError5 = true;_iteratorError5 = err;} finally {try {if (!_iteratorNormalCompletion5 && _iterator5['return']) {_iterator5['return']();}} finally {if (_didIteratorError5) {throw _iteratorError5;}}}
          }return ProgramExit;}() };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uby1kdXBsaWNhdGVzLmpzIl0sIm5hbWVzIjpbImNoZWNrSW1wb3J0cyIsImltcG9ydGVkIiwiY29udGV4dCIsImVudHJpZXMiLCJtb2R1bGUiLCJub2RlcyIsImxlbmd0aCIsIm1lc3NhZ2UiLCJmaXJzdCIsInJlc3QiLCJzb3VyY2VDb2RlIiwiZ2V0U291cmNlQ29kZSIsImZpeCIsImdldEZpeCIsInJlcG9ydCIsIm5vZGUiLCJzb3VyY2UiLCJnZXRDb21tZW50c0JlZm9yZSIsInVuZGVmaW5lZCIsImhhc1Byb2JsZW1hdGljQ29tbWVudHMiLCJoYXNOYW1lc3BhY2UiLCJkZWZhdWx0SW1wb3J0TmFtZXMiLCJTZXQiLCJtYXAiLCJnZXREZWZhdWx0SW1wb3J0TmFtZSIsImZpbHRlciIsIkJvb2xlYW4iLCJzaXplIiwicmVzdFdpdGhvdXRDb21tZW50cyIsInNwZWNpZmllcnMiLCJ0b2tlbnMiLCJnZXRUb2tlbnMiLCJvcGVuQnJhY2UiLCJmaW5kIiwiaXNQdW5jdHVhdG9yIiwidG9rZW4iLCJjbG9zZUJyYWNlIiwiaW1wb3J0Tm9kZSIsInRleHQiLCJzbGljZSIsInJhbmdlIiwiaGFzVHJhaWxpbmdDb21tYSIsImdldFRva2VuQmVmb3JlIiwiaXNFbXB0eSIsImhhc1NwZWNpZmllcnMiLCJ1bm5lY2Vzc2FyeUltcG9ydHMiLCJzb21lIiwic3BlY2lmaWVyIiwic2hvdWxkQWRkRGVmYXVsdCIsInNob3VsZEFkZFNwZWNpZmllcnMiLCJzaG91bGRSZW1vdmVVbm5lY2Vzc2FyeSIsImZpcnN0VG9rZW4iLCJnZXRGaXJzdFRva2VuIiwiZGVmYXVsdEltcG9ydE5hbWUiLCJmaXJzdEhhc1RyYWlsaW5nQ29tbWEiLCJmaXJzdElzRW1wdHkiLCJyZWR1Y2UiLCJyZXN1bHQiLCJuZWVkc0NvbW1hIiwic3BlY2lmaWVyc1RleHQiLCJmaXhlcyIsInB1c2giLCJmaXhlciIsImluc2VydFRleHRBZnRlciIsImluc2VydFRleHRCZWZvcmUiLCJyZW1vdmUiLCJjaGFyQWZ0ZXJJbXBvcnRSYW5nZSIsImNoYXJBZnRlckltcG9ydCIsInN1YnN0cmluZyIsInJlbW92ZVJhbmdlIiwidmFsdWUiLCJ0eXBlIiwiZGVmYXVsdFNwZWNpZmllciIsImxvY2FsIiwibmFtZSIsImhhc0NvbW1lbnRCZWZvcmUiLCJoYXNDb21tZW50QWZ0ZXIiLCJoYXNDb21tZW50SW5zaWRlTm9uU3BlY2lmaWVycyIsImNvbW1lbnQiLCJsb2MiLCJlbmQiLCJsaW5lIiwic3RhcnQiLCJnZXRDb21tZW50c0FmdGVyIiwib3BlbkJyYWNlSW5kZXgiLCJmaW5kSW5kZXgiLCJjbG9zZUJyYWNlSW5kZXgiLCJzb21lVG9rZW5zIiwiY29uY2F0IiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwiZml4YWJsZSIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJjb25zaWRlclF1ZXJ5U3RyaW5nIiwiYWRkaXRpb25hbFByb3BlcnRpZXMiLCJjcmVhdGUiLCJjb25zaWRlclF1ZXJ5U3RyaW5nT3B0aW9uIiwib3B0aW9ucyIsImRlZmF1bHRSZXNvbHZlciIsInNvdXJjZVBhdGgiLCJyZXNvbHZlciIsInBhcnRzIiwibWF0Y2giLCJtb2R1bGVNYXBzIiwiTWFwIiwiZ2V0SW1wb3J0TWFwIiwibiIsImhhcyIsInBhcmVudCIsInNldCIsIm5zSW1wb3J0ZWQiLCJkZWZhdWx0VHlwZXNJbXBvcnRlZCIsIm5hbWVkVHlwZXNJbXBvcnRlZCIsImdldCIsImltcG9ydEtpbmQiLCJJbXBvcnREZWNsYXJhdGlvbiIsInJlc29sdmVkUGF0aCIsImltcG9ydE1hcCIsInZhbHVlcyJdLCJtYXBwaW5ncyI6InFvQkFBQSxzRDtBQUNBLHFDOztBQUVBLFNBQVNBLFlBQVQsQ0FBc0JDLFFBQXRCLEVBQWdDQyxPQUFoQyxFQUF5QztBQUN2Qyx5QkFBOEJELFNBQVNFLE9BQVQsRUFBOUIsOEhBQWtELGdFQUF0Q0MsT0FBc0MsZ0JBQTlCQyxLQUE4QjtBQUNoRCxVQUFJQSxNQUFNQyxNQUFOLEdBQWUsQ0FB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QUFNa0QsWUFBWVosYUFBYUMsQ0FBYixDQUFsQjs7QUFFQSxnQkFBSVcsVUFBVVYsR0FBVixDQUFjUyxZQUFkLENBQUosRUFBaUM7QUFDL0JDLHdCQUFVSixHQUFWLENBQWNHLFlBQWQsRUFBNEIxRCxJQUE1QixDQUFpQ2dELENBQWpDO0FBQ0QsYUFGRCxNQUVPO0FBQ0xXLHdCQUFVUixHQUFWLENBQWNPLFlBQWQsRUFBNEIsQ0FBQ1YsQ0FBRCxDQUE1QjtBQUNEO0FBQ0YsV0FYSTs7QUFhTCxxQ0FBZ0IsdUJBQVk7QUFDMUIsb0NBQWtCSCxXQUFXZSxNQUFYLEVBQWxCLG1JQUF1QyxLQUE1QmxHLEdBQTRCO0FBQ3JDdkIsNkJBQWF1QixJQUFJdEIsUUFBakIsRUFBMkJDLE9BQTNCO0FBQ0FGLDZCQUFhdUIsSUFBSTBGLFVBQWpCLEVBQTZCL0csT0FBN0I7QUFDQUYsNkJBQWF1QixJQUFJMkYsb0JBQWpCLEVBQXVDaEgsT0FBdkM7QUFDQUYsNkJBQWF1QixJQUFJNEYsa0JBQWpCLEVBQXFDakgsT0FBckM7QUFDRCxlQU55QjtBQU8zQixXQVBELHNCQWJLLEVBQVA7O0FBc0JELEtBMUVjLG1CQUFqQiIsImZpbGUiOiJuby1kdXBsaWNhdGVzLmpzIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHJlc29sdmUgZnJvbSAnZXNsaW50LW1vZHVsZS11dGlscy9yZXNvbHZlJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5mdW5jdGlvbiBjaGVja0ltcG9ydHMoaW1wb3J0ZWQsIGNvbnRleHQpIHtcbiAgZm9yIChjb25zdCBbbW9kdWxlLCBub2Rlc10gb2YgaW1wb3J0ZWQuZW50cmllcygpKSB7XG4gICAgaWYgKG5vZGVzLmxlbmd0aCA+IDEpIHtcbiAgICAgIGNvbnN0IG1lc3NhZ2UgPSBgJyR7bW9kdWxlfScgaW1wb3J0ZWQgbXVsdGlwbGUgdGltZXMuYDtcbiAgICAgIGNvbnN0IFtmaXJzdCwgLi4ucmVzdF0gPSBub2RlcztcbiAgICAgIGNvbnN0IHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKTtcbiAgICAgIGNvbnN0IGZpeCA9IGdldEZpeChmaXJzdCwgcmVzdCwgc291cmNlQ29kZSk7XG5cbiAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgbm9kZTogZmlyc3Quc291cmNlLFxuICAgICAgICBtZXNzYWdlLFxuICAgICAgICBmaXgsIC8vIEF0dGFjaCB0aGUgYXV0b2ZpeCAoaWYgYW55KSB0byB0aGUgZmlyc3QgaW1wb3J0LlxuICAgICAgfSk7XG5cbiAgICAgIGZvciAoY29uc3Qgbm9kZSBvZiByZXN0KSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBub2RlLnNvdXJjZSxcbiAgICAgICAgICBtZXNzYWdlLFxuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9XG4gIH1cbn1cblxuZnVuY3Rpb24gZ2V0Rml4KGZpcnN0LCByZXN0LCBzb3VyY2VDb2RlKSB7XG4gIC8vIFNvcnJ5IEVTTGludCA8PSAzIHVzZXJzLCBubyBhdXRvZml4IGZvciB5b3UuIEF1dG9maXhpbmcgZHVwbGljYXRlIGltcG9ydHNcbiAgLy8gcmVxdWlyZXMgbXVsdGlwbGUgYGZpeGVyLndoYXRldmVyKClgIGNhbGxzIGluIHRoZSBgZml4YDogV2UgYm90aCBuZWVkIHRvXG4gIC8vIHVwZGF0ZSB0aGUgZmlyc3Qgb25lLCBhbmQgcmVtb3ZlIHRoZSByZXN0LiBTdXBwb3J0IGZvciBtdWx0aXBsZVxuICAvLyBgZml4ZXIud2hhdGV2ZXIoKWAgaW4gYSBzaW5nbGUgYGZpeGAgd2FzIGFkZGVkIGluIEVTTGludCA0LjEuXG4gIC8vIGBzb3VyY2VDb2RlLmdldENvbW1lbnRzQmVmb3JlYCB3YXMgYWRkZWQgaW4gNC4wLCBzbyB0aGF0J3MgYW4gZWFzeSB0aGluZyB0b1xuICAvLyBjaGVjayBmb3IuXG4gIGlmICh0eXBlb2Ygc291cmNlQ29kZS5nZXRDb21tZW50c0JlZm9yZSAhPT0gJ2Z1bmN0aW9uJykge1xuICAgIHJldHVybiB1bmRlZmluZWQ7XG4gIH1cblxuICAvLyBBZGp1c3RpbmcgdGhlIGZpcnN0IGltcG9ydCBtaWdodCBtYWtlIGl0IG11bHRpbGluZSwgd2hpY2ggY291bGQgYnJlYWtcbiAgLy8gYGVzbGludC1kaXNhYmxlLW5leHQtbGluZWAgY29tbWVudHMgYW5kIHNpbWlsYXIsIHNvIGJhaWwgaWYgdGhlIGZpcnN0XG4gIC8vIGltcG9ydCBoYXMgY29tbWVudHMuIEFsc28sIGlmIHRoZSBmaXJzdCBpbXBvcnQgaXMgYGltcG9ydCAqIGFzIG5zIGZyb21cbiAgLy8gJy4vZm9vJ2AgdGhlcmUncyBub3RoaW5nIHdlIGNhbiBkby5cbiAgaWYgKGhhc1Byb2JsZW1hdGljQ29tbWVudHMoZmlyc3QsIHNvdXJjZUNvZGUpIHx8IGhhc05hbWVzcGFjZShmaXJzdCkpIHtcbiAgICByZXR1cm4gdW5kZWZpbmVkO1xuICB9XG5cbiAgY29uc3QgZGVmYXVsdEltcG9ydE5hbWVzID0gbmV3IFNldChcbiAgICBbZmlyc3QsIC4uLnJlc3RdLm1hcChnZXREZWZhdWx0SW1wb3J0TmFtZSkuZmlsdGVyKEJvb2xlYW4pLFxuICApO1xuXG4gIC8vIEJhaWwgaWYgdGhlcmUgYXJlIG11bHRpcGxlIGRpZmZlcmVudCBkZWZhdWx0IGltcG9ydCBuYW1lcyDigJMgaXQncyB1cCB0byB0aGVcbiAgLy8gdXNlciB0byBjaG9vc2Ugd2hpY2ggb25lIHRvIGtlZXAuXG4gIGlmIChkZWZhdWx0SW1wb3J0TmFtZXMuc2l6ZSA+IDEpIHtcbiAgICByZXR1cm4gdW5kZWZpbmVkO1xuICB9XG5cbiAgLy8gTGVhdmUgaXQgdG8gdGhlIHVzZXIgdG8gaGFuZGxlIGNvbW1lbnRzLiBBbHNvIHNraXAgYGltcG9ydCAqIGFzIG5zIGZyb21cbiAgLy8gJy4vZm9vJ2AgaW1wb3J0cywgc2luY2UgdGhleSBjYW5ub3QgYmUgbWVyZ2VkIGludG8gYW5vdGhlciBpbXBvcnQuXG4gIGNvbnN0IHJlc3RXaXRob3V0Q29tbWVudHMgPSByZXN0LmZpbHRlcihub2RlID0+ICEoXG4gICAgaGFzUHJvYmxlbWF0aWNDb21tZW50cyhub2RlLCBzb3VyY2VDb2RlKSB8fFxuICAgIGhhc05hbWVzcGFjZShub2RlKVxuICApKTtcblxuICBjb25zdCBzcGVjaWZpZXJzID0gcmVzdFdpdGhvdXRDb21tZW50c1xuICAgIC5tYXAobm9kZSA9PiB7XG4gICAgICBjb25zdCB0b2tlbnMgPSBzb3VyY2VDb2RlLmdldFRva2Vucyhub2RlKTtcbiAgICAgIGNvbnN0IG9wZW5CcmFjZSA9IHRva2Vucy5maW5kKHRva2VuID0+IGlzUHVuY3R1YXRvcih0b2tlbiwgJ3snKSk7XG4gICAgICBjb25zdCBjbG9zZUJyYWNlID0gdG9rZW5zLmZpbmQodG9rZW4gPT4gaXNQdW5jdHVhdG9yKHRva2VuLCAnfScpKTtcblxuICAgICAgaWYgKG9wZW5CcmFjZSA9PSBudWxsIHx8IGNsb3NlQnJhY2UgPT0gbnVsbCkge1xuICAgICAgICByZXR1cm4gdW5kZWZpbmVkO1xuICAgICAgfVxuXG4gICAgICByZXR1cm4ge1xuICAgICAgICBpbXBvcnROb2RlOiBub2RlLFxuICAgICAgICB0ZXh0OiBzb3VyY2VDb2RlLnRleHQuc2xpY2Uob3BlbkJyYWNlLnJhbmdlWzFdLCBjbG9zZUJyYWNlLnJhbmdlWzBdKSxcbiAgICAgICAgaGFzVHJhaWxpbmdDb21tYTogaXNQdW5jdHVhdG9yKHNvdXJjZUNvZGUuZ2V0VG9rZW5CZWZvcmUoY2xvc2VCcmFjZSksICcsJyksXG4gICAgICAgIGlzRW1wdHk6ICFoYXNTcGVjaWZpZXJzKG5vZGUpLFxuICAgICAgfTtcbiAgICB9KVxuICAgIC5maWx0ZXIoQm9vbGVhbik7XG5cbiAgY29uc3QgdW5uZWNlc3NhcnlJbXBvcnRzID0gcmVzdFdpdGhvdXRDb21tZW50cy5maWx0ZXIobm9kZSA9PlxuICAgICFoYXNTcGVjaWZpZXJzKG5vZGUpICYmXG4gICAgIWhhc05hbWVzcGFjZShub2RlKSAmJlxuICAgICFzcGVjaWZpZXJzLnNvbWUoc3BlY2lmaWVyID0+IHNwZWNpZmllci5pbXBvcnROb2RlID09PSBub2RlKSxcbiAgKTtcblxuICBjb25zdCBzaG91bGRBZGREZWZhdWx0ID0gZ2V0RGVmYXVsdEltcG9ydE5hbWUoZmlyc3QpID09IG51bGwgJiYgZGVmYXVsdEltcG9ydE5hbWVzLnNpemUgPT09IDE7XG4gIGNvbnN0IHNob3VsZEFkZFNwZWNpZmllcnMgPSBzcGVjaWZpZXJzLmxlbmd0aCA+IDA7XG4gIGNvbnN0IHNob3VsZFJlbW92ZVVubmVjZXNzYXJ5ID0gdW5uZWNlc3NhcnlJbXBvcnRzLmxlbmd0aCA+IDA7XG5cbiAgaWYgKCEoc2hvdWxkQWRkRGVmYXVsdCB8fCBzaG91bGRBZGRTcGVjaWZpZXJzIHx8IHNob3VsZFJlbW92ZVVubmVjZXNzYXJ5KSkge1xuICAgIHJldHVybiB1bmRlZmluZWQ7XG4gIH1cblxuICByZXR1cm4gZml4ZXIgPT4ge1xuICAgIGNvbnN0IHRva2VucyA9IHNvdXJjZUNvZGUuZ2V0VG9rZW5zKGZpcnN0KTtcbiAgICBjb25zdCBvcGVuQnJhY2UgPSB0b2tlbnMuZmluZCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd7JykpO1xuICAgIGNvbnN0IGNsb3NlQnJhY2UgPSB0b2tlbnMuZmluZCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd9JykpO1xuICAgIGNvbnN0IGZpcnN0VG9rZW4gPSBzb3VyY2VDb2RlLmdldEZpcnN0VG9rZW4oZmlyc3QpO1xuICAgIGNvbnN0IFtkZWZhdWx0SW1wb3J0TmFtZV0gPSBkZWZhdWx0SW1wb3J0TmFtZXM7XG5cbiAgICBjb25zdCBmaXJzdEhhc1RyYWlsaW5nQ29tbWEgPVxuICAgICAgY2xvc2VCcmFjZSAhPSBudWxsICYmXG4gICAgICBpc1B1bmN0dWF0b3Ioc291cmNlQ29kZS5nZXRUb2tlbkJlZm9yZShjbG9zZUJyYWNlKSwgJywnKTtcbiAgICBjb25zdCBmaXJzdElzRW1wdHkgPSAhaGFzU3BlY2lmaWVycyhmaXJzdCk7XG5cbiAgICBjb25zdCBbc3BlY2lmaWVyc1RleHRdID0gc3BlY2lmaWVycy5yZWR1Y2UoXG4gICAgICAoW3Jlc3VsdCwgbmVlZHNDb21tYV0sIHNwZWNpZmllcikgPT4ge1xuICAgICAgICByZXR1cm4gW1xuICAgICAgICAgIG5lZWRzQ29tbWEgJiYgIXNwZWNpZmllci5pc0VtcHR5XG4gICAgICAgICAgICA/IGAke3Jlc3VsdH0sJHtzcGVjaWZpZXIudGV4dH1gXG4gICAgICAgICAgICA6IGAke3Jlc3VsdH0ke3NwZWNpZmllci50ZXh0fWAsXG4gICAgICAgICAgc3BlY2lmaWVyLmlzRW1wdHkgPyBuZWVkc0NvbW1hIDogdHJ1ZSxcbiAgICAgICAgXTtcbiAgICAgIH0sXG4gICAgICBbJycsICFmaXJzdEhhc1RyYWlsaW5nQ29tbWEgJiYgIWZpcnN0SXNFbXB0eV0sXG4gICAgKTtcblxuICAgIGNvbnN0IGZpeGVzID0gW107XG5cbiAgICBpZiAoc2hvdWxkQWRkRGVmYXVsdCAmJiBvcGVuQnJhY2UgPT0gbnVsbCAmJiBzaG91bGRBZGRTcGVjaWZpZXJzKSB7XG4gICAgICAvLyBgaW1wb3J0ICcuL2ZvbydgIOKGkiBgaW1wb3J0IGRlZiwgey4uLn0gZnJvbSAnLi9mb28nYFxuICAgICAgZml4ZXMucHVzaChcbiAgICAgICAgZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKGZpcnN0VG9rZW4sIGAgJHtkZWZhdWx0SW1wb3J0TmFtZX0sIHske3NwZWNpZmllcnNUZXh0fX0gZnJvbWApLFxuICAgICAgKTtcbiAgICB9IGVsc2UgaWYgKHNob3VsZEFkZERlZmF1bHQgJiYgb3BlbkJyYWNlID09IG51bGwgJiYgIXNob3VsZEFkZFNwZWNpZmllcnMpIHtcbiAgICAgIC8vIGBpbXBvcnQgJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgZGVmIGZyb20gJy4vZm9vJ2BcbiAgICAgIGZpeGVzLnB1c2goZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKGZpcnN0VG9rZW4sIGAgJHtkZWZhdWx0SW1wb3J0TmFtZX0gZnJvbWApKTtcbiAgICB9IGVsc2UgaWYgKHNob3VsZEFkZERlZmF1bHQgJiYgb3BlbkJyYWNlICE9IG51bGwgJiYgY2xvc2VCcmFjZSAhPSBudWxsKSB7XG4gICAgICAvLyBgaW1wb3J0IHsuLi59IGZyb20gJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgZGVmLCB7Li4ufSBmcm9tICcuL2ZvbydgXG4gICAgICBmaXhlcy5wdXNoKGZpeGVyLmluc2VydFRleHRBZnRlcihmaXJzdFRva2VuLCBgICR7ZGVmYXVsdEltcG9ydE5hbWV9LGApKTtcbiAgICAgIGlmIChzaG91bGRBZGRTcGVjaWZpZXJzKSB7XG4gICAgICAgIC8vIGBpbXBvcnQgZGVmLCB7Li4ufSBmcm9tICcuL2ZvbydgIOKGkiBgaW1wb3J0IGRlZiwgey4uLiwgLi4ufSBmcm9tICcuL2ZvbydgXG4gICAgICAgIGZpeGVzLnB1c2goZml4ZXIuaW5zZXJ0VGV4dEJlZm9yZShjbG9zZUJyYWNlLCBzcGVjaWZpZXJzVGV4dCkpO1xuICAgICAgfVxuICAgIH0gZWxzZSBpZiAoIXNob3VsZEFkZERlZmF1bHQgJiYgb3BlbkJyYWNlID09IG51bGwgJiYgc2hvdWxkQWRkU3BlY2lmaWVycykge1xuICAgICAgaWYgKGZpcnN0LnNwZWNpZmllcnMubGVuZ3RoID09PSAwKSB7XG4gICAgICAgIC8vIGBpbXBvcnQgJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgey4uLn0gZnJvbSAnLi9mb28nYFxuICAgICAgICBmaXhlcy5wdXNoKGZpeGVyLmluc2VydFRleHRBZnRlcihmaXJzdFRva2VuLCBgIHske3NwZWNpZmllcnNUZXh0fX0gZnJvbWApKTtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIC8vIGBpbXBvcnQgZGVmIGZyb20gJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgZGVmLCB7Li4ufSBmcm9tICcuL2ZvbydgXG4gICAgICAgIGZpeGVzLnB1c2goZml4ZXIuaW5zZXJ0VGV4dEFmdGVyKGZpcnN0LnNwZWNpZmllcnNbMF0sIGAsIHske3NwZWNpZmllcnNUZXh0fX1gKSk7XG4gICAgICB9XG4gICAgfSBlbHNlIGlmICghc2hvdWxkQWRkRGVmYXVsdCAmJiBvcGVuQnJhY2UgIT0gbnVsbCAmJiBjbG9zZUJyYWNlICE9IG51bGwpIHtcbiAgICAgIC8vIGBpbXBvcnQgey4uLn0gJy4vZm9vJ2Ag4oaSIGBpbXBvcnQgey4uLiwgLi4ufSBmcm9tICcuL2ZvbydgXG4gICAgICBmaXhlcy5wdXNoKGZpeGVyLmluc2VydFRleHRCZWZvcmUoY2xvc2VCcmFjZSwgc3BlY2lmaWVyc1RleHQpKTtcbiAgICB9XG5cbiAgICAvLyBSZW1vdmUgaW1wb3J0cyB3aG9zZSBzcGVjaWZpZXJzIGhhdmUgYmVlbiBtb3ZlZCBpbnRvIHRoZSBmaXJzdCBpbXBvcnQuXG4gICAgZm9yIChjb25zdCBzcGVjaWZpZXIgb2Ygc3BlY2lmaWVycykge1xuICAgICAgY29uc3QgaW1wb3J0Tm9kZSA9IHNwZWNpZmllci5pbXBvcnROb2RlO1xuICAgICAgZml4ZXMucHVzaChmaXhlci5yZW1vdmUoaW1wb3J0Tm9kZSkpO1xuXG4gICAgICBjb25zdCBjaGFyQWZ0ZXJJbXBvcnRSYW5nZSA9IFtpbXBvcnROb2RlLnJhbmdlWzFdLCBpbXBvcnROb2RlLnJhbmdlWzFdICsgMV07XG4gICAgICBjb25zdCBjaGFyQWZ0ZXJJbXBvcnQgPSBzb3VyY2VDb2RlLnRleHQuc3Vic3RyaW5nKGNoYXJBZnRlckltcG9ydFJhbmdlWzBdLCBjaGFyQWZ0ZXJJbXBvcnRSYW5nZVsxXSk7XG4gICAgICBpZiAoY2hhckFmdGVySW1wb3J0ID09PSAnXFxuJykge1xuICAgICAgICBmaXhlcy5wdXNoKGZpeGVyLnJlbW92ZVJhbmdlKGNoYXJBZnRlckltcG9ydFJhbmdlKSk7XG4gICAgICB9XG4gICAgfVxuXG4gICAgLy8gUmVtb3ZlIGltcG9ydHMgd2hvc2UgZGVmYXVsdCBpbXBvcnQgaGFzIGJlZW4gbW92ZWQgdG8gdGhlIGZpcnN0IGltcG9ydCxcbiAgICAvLyBhbmQgc2lkZS1lZmZlY3Qtb25seSBpbXBvcnRzIHRoYXQgYXJlIHVubmVjZXNzYXJ5IGR1ZSB0byB0aGUgZmlyc3RcbiAgICAvLyBpbXBvcnQuXG4gICAgZm9yIChjb25zdCBub2RlIG9mIHVubmVjZXNzYXJ5SW1wb3J0cykge1xuICAgICAgZml4ZXMucHVzaChmaXhlci5yZW1vdmUobm9kZSkpO1xuXG4gICAgICBjb25zdCBjaGFyQWZ0ZXJJbXBvcnRSYW5nZSA9IFtub2RlLnJhbmdlWzFdLCBub2RlLnJhbmdlWzFdICsgMV07XG4gICAgICBjb25zdCBjaGFyQWZ0ZXJJbXBvcnQgPSBzb3VyY2VDb2RlLnRleHQuc3Vic3RyaW5nKGNoYXJBZnRlckltcG9ydFJhbmdlWzBdLCBjaGFyQWZ0ZXJJbXBvcnRSYW5nZVsxXSk7XG4gICAgICBpZiAoY2hhckFmdGVySW1wb3J0ID09PSAnXFxuJykge1xuICAgICAgICBmaXhlcy5wdXNoKGZpeGVyLnJlbW92ZVJhbmdlKGNoYXJBZnRlckltcG9ydFJhbmdlKSk7XG4gICAgICB9XG4gICAgfVxuXG4gICAgcmV0dXJuIGZpeGVzO1xuICB9O1xufVxuXG5mdW5jdGlvbiBpc1B1bmN0dWF0b3Iobm9kZSwgdmFsdWUpIHtcbiAgcmV0dXJuIG5vZGUudHlwZSA9PT0gJ1B1bmN0dWF0b3InICYmIG5vZGUudmFsdWUgPT09IHZhbHVlO1xufVxuXG4vLyBHZXQgdGhlIG5hbWUgb2YgdGhlIGRlZmF1bHQgaW1wb3J0IG9mIGBub2RlYCwgaWYgYW55LlxuZnVuY3Rpb24gZ2V0RGVmYXVsdEltcG9ydE5hbWUobm9kZSkge1xuICBjb25zdCBkZWZhdWx0U3BlY2lmaWVyID0gbm9kZS5zcGVjaWZpZXJzXG4gICAgLmZpbmQoc3BlY2lmaWVyID0+IHNwZWNpZmllci50eXBlID09PSAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcicpO1xuICByZXR1cm4gZGVmYXVsdFNwZWNpZmllciAhPSBudWxsID8gZGVmYXVsdFNwZWNpZmllci5sb2NhbC5uYW1lIDogdW5kZWZpbmVkO1xufVxuXG4vLyBDaGVja3Mgd2hldGhlciBgbm9kZWAgaGFzIGEgbmFtZXNwYWNlIGltcG9ydC5cbmZ1bmN0aW9uIGhhc05hbWVzcGFjZShub2RlKSB7XG4gIGNvbnN0IHNwZWNpZmllcnMgPSBub2RlLnNwZWNpZmllcnNcbiAgICAuZmlsdGVyKHNwZWNpZmllciA9PiBzcGVjaWZpZXIudHlwZSA9PT0gJ0ltcG9ydE5hbWVzcGFjZVNwZWNpZmllcicpO1xuICByZXR1cm4gc3BlY2lmaWVycy5sZW5ndGggPiAwO1xufVxuXG4vLyBDaGVja3Mgd2hldGhlciBgbm9kZWAgaGFzIGFueSBub24tZGVmYXVsdCBzcGVjaWZpZXJzLlxuZnVuY3Rpb24gaGFzU3BlY2lmaWVycyhub2RlKSB7XG4gIGNvbnN0IHNwZWNpZmllcnMgPSBub2RlLnNwZWNpZmllcnNcbiAgICAuZmlsdGVyKHNwZWNpZmllciA9PiBzcGVjaWZpZXIudHlwZSA9PT0gJ0ltcG9ydFNwZWNpZmllcicpO1xuICByZXR1cm4gc3BlY2lmaWVycy5sZW5ndGggPiAwO1xufVxuXG4vLyBJdCdzIG5vdCBvYnZpb3VzIHdoYXQgdGhlIHVzZXIgd2FudHMgdG8gZG8gd2l0aCBjb21tZW50cyBhc3NvY2lhdGVkIHdpdGhcbi8vIGR1cGxpY2F0ZSBpbXBvcnRzLCBzbyBza2lwIGltcG9ydHMgd2l0aCBjb21tZW50cyB3aGVuIGF1dG9maXhpbmcuXG5mdW5jdGlvbiBoYXNQcm9ibGVtYXRpY0NvbW1lbnRzKG5vZGUsIHNvdXJjZUNvZGUpIHtcbiAgcmV0dXJuIChcbiAgICBoYXNDb21tZW50QmVmb3JlKG5vZGUsIHNvdXJjZUNvZGUpIHx8XG4gICAgaGFzQ29tbWVudEFmdGVyKG5vZGUsIHNvdXJjZUNvZGUpIHx8XG4gICAgaGFzQ29tbWVudEluc2lkZU5vblNwZWNpZmllcnMobm9kZSwgc291cmNlQ29kZSlcbiAgKTtcbn1cblxuLy8gQ2hlY2tzIHdoZXRoZXIgYG5vZGVgIGhhcyBhIGNvbW1lbnQgKHRoYXQgZW5kcykgb24gdGhlIHByZXZpb3VzIGxpbmUgb3Igb25cbi8vIHRoZSBzYW1lIGxpbmUgYXMgYG5vZGVgIChzdGFydHMpLlxuZnVuY3Rpb24gaGFzQ29tbWVudEJlZm9yZShub2RlLCBzb3VyY2VDb2RlKSB7XG4gIHJldHVybiBzb3VyY2VDb2RlLmdldENvbW1lbnRzQmVmb3JlKG5vZGUpXG4gICAgLnNvbWUoY29tbWVudCA9PiBjb21tZW50LmxvYy5lbmQubGluZSA+PSBub2RlLmxvYy5zdGFydC5saW5lIC0gMSk7XG59XG5cbi8vIENoZWNrcyB3aGV0aGVyIGBub2RlYCBoYXMgYSBjb21tZW50ICh0aGF0IHN0YXJ0cykgb24gdGhlIHNhbWUgbGluZSBhcyBgbm9kZWBcbi8vIChlbmRzKS5cbmZ1bmN0aW9uIGhhc0NvbW1lbnRBZnRlcihub2RlLCBzb3VyY2VDb2RlKSB7XG4gIHJldHVybiBzb3VyY2VDb2RlLmdldENvbW1lbnRzQWZ0ZXIobm9kZSlcbiAgICAuc29tZShjb21tZW50ID0+IGNvbW1lbnQubG9jLnN0YXJ0LmxpbmUgPT09IG5vZGUubG9jLmVuZC5saW5lKTtcbn1cblxuLy8gQ2hlY2tzIHdoZXRoZXIgYG5vZGVgIGhhcyBhbnkgY29tbWVudHMgX2luc2lkZSxfIGV4Y2VwdCBpbnNpZGUgdGhlIGB7Li4ufWBcbi8vIHBhcnQgKGlmIGFueSkuXG5mdW5jdGlvbiBoYXNDb21tZW50SW5zaWRlTm9uU3BlY2lmaWVycyhub2RlLCBzb3VyY2VDb2RlKSB7XG4gIGNvbnN0IHRva2VucyA9IHNvdXJjZUNvZGUuZ2V0VG9rZW5zKG5vZGUpO1xuICBjb25zdCBvcGVuQnJhY2VJbmRleCA9IHRva2Vucy5maW5kSW5kZXgodG9rZW4gPT4gaXNQdW5jdHVhdG9yKHRva2VuLCAneycpKTtcbiAgY29uc3QgY2xvc2VCcmFjZUluZGV4ID0gdG9rZW5zLmZpbmRJbmRleCh0b2tlbiA9PiBpc1B1bmN0dWF0b3IodG9rZW4sICd9JykpO1xuICAvLyBTbGljZSBhd2F5IHRoZSBmaXJzdCB0b2tlbiwgc2luY2Ugd2UncmUgbm8gbG9va2luZyBmb3IgY29tbWVudHMgX2JlZm9yZV9cbiAgLy8gYG5vZGVgIChvbmx5IGluc2lkZSkuIElmIHRoZXJlJ3MgYSBgey4uLn1gIHBhcnQsIGxvb2sgZm9yIGNvbW1lbnRzIGJlZm9yZVxuICAvLyB0aGUgYHtgLCBidXQgbm90IGJlZm9yZSB0aGUgYH1gIChoZW5jZSB0aGUgYCsxYHMpLlxuICBjb25zdCBzb21lVG9rZW5zID0gb3BlbkJyYWNlSW5kZXggPj0gMCAmJiBjbG9zZUJyYWNlSW5kZXggPj0gMFxuICAgID8gdG9rZW5zLnNsaWNlKDEsIG9wZW5CcmFjZUluZGV4ICsgMSkuY29uY2F0KHRva2Vucy5zbGljZShjbG9zZUJyYWNlSW5kZXggKyAxKSlcbiAgICA6IHRva2Vucy5zbGljZSgxKTtcbiAgcmV0dXJuIHNvbWVUb2tlbnMuc29tZSh0b2tlbiA9PiBzb3VyY2VDb2RlLmdldENvbW1lbnRzQmVmb3JlKHRva2VuKS5sZW5ndGggPiAwKTtcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAncHJvYmxlbScsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCduby1kdXBsaWNhdGVzJyksXG4gICAgfSxcbiAgICBmaXhhYmxlOiAnY29kZScsXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgY29uc2lkZXJRdWVyeVN0cmluZzoge1xuICAgICAgICAgICAgdHlwZTogJ2Jvb2xlYW4nLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgIGFkZGl0aW9uYWxQcm9wZXJ0aWVzOiBmYWxzZSxcbiAgICAgIH0sXG4gICAgXSxcbiAgfSxcblxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIC8vIFByZXBhcmUgdGhlIHJlc29sdmVyIGZyb20gb3B0aW9ucy5cbiAgICBjb25zdCBjb25zaWRlclF1ZXJ5U3RyaW5nT3B0aW9uID0gY29udGV4dC5vcHRpb25zWzBdICYmXG4gICAgICBjb250ZXh0Lm9wdGlvbnNbMF1bJ2NvbnNpZGVyUXVlcnlTdHJpbmcnXTtcbiAgICBjb25zdCBkZWZhdWx0UmVzb2x2ZXIgPSBzb3VyY2VQYXRoID0+IHJlc29sdmUoc291cmNlUGF0aCwgY29udGV4dCkgfHwgc291cmNlUGF0aDtcbiAgICBjb25zdCByZXNvbHZlciA9IGNvbnNpZGVyUXVlcnlTdHJpbmdPcHRpb24gPyAoc291cmNlUGF0aCA9PiB7XG4gICAgICBjb25zdCBwYXJ0cyA9IHNvdXJjZVBhdGgubWF0Y2goL14oW14/XSopXFw/KC4qKSQvKTtcbiAgICAgIGlmICghcGFydHMpIHtcbiAgICAgICAgcmV0dXJuIGRlZmF1bHRSZXNvbHZlcihzb3VyY2VQYXRoKTtcbiAgICAgIH1cbiAgICAgIHJldHVybiBkZWZhdWx0UmVzb2x2ZXIocGFydHNbMV0pICsgJz8nICsgcGFydHNbMl07XG4gICAgfSkgOiBkZWZhdWx0UmVzb2x2ZXI7XG5cbiAgICBjb25zdCBtb2R1bGVNYXBzID0gbmV3IE1hcCgpO1xuXG4gICAgZnVuY3Rpb24gZ2V0SW1wb3J0TWFwKG4pIHtcbiAgICAgIGlmICghbW9kdWxlTWFwcy5oYXMobi5wYXJlbnQpKSB7XG4gICAgICAgIG1vZHVsZU1hcHMuc2V0KG4ucGFyZW50LCB7XG4gICAgICAgICAgaW1wb3J0ZWQ6IG5ldyBNYXAoKSxcbiAgICAgICAgICBuc0ltcG9ydGVkOiBuZXcgTWFwKCksXG4gICAgICAgICAgZGVmYXVsdFR5cGVzSW1wb3J0ZWQ6IG5ldyBNYXAoKSxcbiAgICAgICAgICBuYW1lZFR5cGVzSW1wb3J0ZWQ6IG5ldyBNYXAoKSxcbiAgICAgICAgfSk7XG4gICAgICB9XG4gICAgICBjb25zdCBtYXAgPSBtb2R1bGVNYXBzLmdldChuLnBhcmVudCk7XG4gICAgICBpZiAobi5pbXBvcnRLaW5kID09PSAndHlwZScpIHtcbiAgICAgICAgcmV0dXJuIG4uc3BlY2lmaWVycy5sZW5ndGggPiAwICYmIG4uc3BlY2lmaWVyc1swXS50eXBlID09PSAnSW1wb3J0RGVmYXVsdFNwZWNpZmllcicgPyBtYXAuZGVmYXVsdFR5cGVzSW1wb3J0ZWQgOiBtYXAubmFtZWRUeXBlc0ltcG9ydGVkO1xuICAgICAgfVxuXG4gICAgICByZXR1cm4gaGFzTmFtZXNwYWNlKG4pID8gbWFwLm5zSW1wb3J0ZWQgOiBtYXAuaW1wb3J0ZWQ7XG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgIEltcG9ydERlY2xhcmF0aW9uKG4pIHtcbiAgICAgICAgLy8gcmVzb2x2ZWQgcGF0aCB3aWxsIGNvdmVyIGFsaWFzZWQgZHVwbGljYXRlc1xuICAgICAgICBjb25zdCByZXNvbHZlZFBhdGggPSByZXNvbHZlcihuLnNvdXJjZS52YWx1ZSk7XG4gICAgICAgIGNvbnN0IGltcG9ydE1hcCA9IGdldEltcG9ydE1hcChuKTtcblxuICAgICAgICBpZiAoaW1wb3J0TWFwLmhhcyhyZXNvbHZlZFBhdGgpKSB7XG4gICAgICAgICAgaW1wb3J0TWFwLmdldChyZXNvbHZlZFBhdGgpLnB1c2gobik7XG4gICAgICAgIH0gZWxzZSB7XG4gICAgICAgICAgaW1wb3J0TWFwLnNldChyZXNvbHZlZFBhdGgsIFtuXSk7XG4gICAgICAgIH1cbiAgICAgIH0sXG5cbiAgICAgICdQcm9ncmFtOmV4aXQnOiBmdW5jdGlvbiAoKSB7XG4gICAgICAgIGZvciAoY29uc3QgbWFwIG9mIG1vZHVsZU1hcHMudmFsdWVzKCkpIHtcbiAgICAgICAgICBjaGVja0ltcG9ydHMobWFwLmltcG9ydGVkLCBjb250ZXh0KTtcbiAgICAgICAgICBjaGVja0ltcG9ydHMobWFwLm5zSW1wb3J0ZWQsIGNvbnRleHQpO1xuICAgICAgICAgIGNoZWNrSW1wb3J0cyhtYXAuZGVmYXVsdFR5cGVzSW1wb3J0ZWQsIGNvbnRleHQpO1xuICAgICAgICAgIGNoZWNrSW1wb3J0cyhtYXAubmFtZWRUeXBlc0ltcG9ydGVkLCBjb250ZXh0KTtcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICB9O1xuICB9LFxufTtcbiJdfQ==