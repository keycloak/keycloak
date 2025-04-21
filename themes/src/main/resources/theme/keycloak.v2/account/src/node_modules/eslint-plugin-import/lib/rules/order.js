'use strict';var _slicedToArray = function () {function sliceIterator(arr, i) {var _arr = [];var _n = true;var _d = false;var _e = undefined;try {for (var _i = arr[Symbol.iterator](), _s; !(_n = (_s = _i.next()).done); _n = true) {_arr.push(_s.value);if (i && _arr.length === i) break;}} catch (err) {_d = true;_e = err;} finally {try {if (!_n && _i["return"]) _i["return"]();} finally {if (_d) throw _e;}}return _arr;}return function (arr, i) {if (Array.isArray(arr)) {return arr;} else if (Symbol.iterator in Object(arr)) {return sliceIterator(arr, i);} else {throw new TypeError("Invalid attempt to destructure non-iterable instance");}};}();

var _minimatch = require('minimatch');var _minimatch2 = _interopRequireDefault(_minimatch);
var _importType = require('../core/importType');var _importType2 = _interopRequireDefault(_importType);
var _staticRequire = require('../core/staticRequire');var _staticRequire2 = _interopRequireDefault(_staticRequire);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}

var defaultGroups = ['builtin', 'external', 'parent', 'sibling', 'index'];

// REPORTING AND FIXING

function reverse(array) {
  return array.map(function (v) {
    return Object.assign({}, v, { rank: -v.rank });
  }).reverse();
}

function getTokensOrCommentsAfter(sourceCode, node, count) {
  var currentNodeOrToken = node;
  var result = [];
  for (var i = 0; i < count; i++) {
    currentNodeOrToken = sourceCode.getTokenOrCommentAfter(currentNodeOrToken);
    if (currentNodeOrToken == null) {
      break;
    }
    result.push(currentNodeOrToken);
  }
  return result;
}

function getTokensOrCommentsBefore(sourceCode, node, count) {
  var currentNodeOrToken = node;
  var result = [];
  for (var i = 0; i < count; i++) {
    currentNodeOrToken = sourceCode.getTokenOrCommentBefore(currentNodeOrToken);
    if (currentNodeOrToken == null) {
      break;
    }
    result.push(currentNodeOrToken);
  }
  return result.reverse();
}

function takeTokensAfterWhile(sourceCode, node, condition) {
  var tokens = getTokensOrCommentsAfter(sourceCode, node, 100);
  var result = [];
  for (var i = 0; i < tokens.length; i++) {
    if (condition(tokens[i])) {
      result.push(tokens[i]);
    } else {
      break;
    }
  }
  return result;
}

function takeTokensBeforeWhile(sourceCode, node, condition) {
  var tokens = getTokensOrCommentsBefore(sourceCode, node, 100);
  var result = [];
  for (var i = tokens.length - 1; i >= 0; i--) {
    if (condition(tokens[i])) {
      result.push(tokens[i]);
    } else {
      break;
    }
  }
  return result.reverse();
}

function findOutOfOrder(imported) {
  if (imported.length === 0) {
    return [];
  }
  var maxSeenRankNode = imported[0];
  return imported.filter(function (importedModule) {
    var res = importedModule.rank < maxSeenRankNode.rank;
    if (maxSeenRankNode.rank < importedModule.rank) {
      maxSeenRankNode = importedModule;
    }
    return res;
  });
}

function findRootNode(node) {
  var parent = node;
  while (parent.parent != null && parent.parent.body == null) {
    parent = parent.parent;
  }
  return parent;
}

function findEndOfLineWithComments(sourceCode, node) {
  var tokensToEndOfLine = takeTokensAfterWhile(sourceCode, node, commentOnSameLineAs(node));
  var endOfTokens = tokensToEndOfLine.length > 0 ?
  tokensToEndOfLine[tokensToEndOfLine.length - 1].range[1] :
  node.range[1];
  var result = endOfTokens;
  for (var i = endOfTokens; i < sourceCode.text.length; i++) {
    if (sourceCode.text[i] === '\n') {
      result = i + 1;
      break;
    }
    if (sourceCode.text[i] !== ' ' && sourceCode.text[i] !== '\t' && sourceCode.text[i] !== '\r') {
      break;
    }
    result = i + 1;
  }
  return result;
}

function commentOnSameLineAs(node) {
  return function (token) {return (token.type === 'Block' || token.type === 'Line') &&
    token.loc.start.line === token.loc.end.line &&
    token.loc.end.line === node.loc.end.line;};
}

function findStartOfLineWithComments(sourceCode, node) {
  var tokensToEndOfLine = takeTokensBeforeWhile(sourceCode, node, commentOnSameLineAs(node));
  var startOfTokens = tokensToEndOfLine.length > 0 ? tokensToEndOfLine[0].range[0] : node.range[0];
  var result = startOfTokens;
  for (var i = startOfTokens - 1; i > 0; i--) {
    if (sourceCode.text[i] !== ' ' && sourceCode.text[i] !== '\t') {
      break;
    }
    result = i;
  }
  return result;
}

function isPlainRequireModule(node) {
  if (node.type !== 'VariableDeclaration') {
    return false;
  }
  if (node.declarations.length !== 1) {
    return false;
  }
  var decl = node.declarations[0];
  var result = decl.id && (
  decl.id.type === 'Identifier' || decl.id.type === 'ObjectPattern') &&
  decl.init != null &&
  decl.init.type === 'CallExpression' &&
  decl.init.callee != null &&
  decl.init.callee.name === 'require' &&
  decl.init.arguments != null &&
  decl.init.arguments.length === 1 &&
  decl.init.arguments[0].type === 'Literal';
  return result;
}

function isPlainImportModule(node) {
  return node.type === 'ImportDeclaration' && node.specifiers != null && node.specifiers.length > 0;
}

function isPlainImportEquals(node) {
  return node.type === 'TSImportEqualsDeclaration' && node.moduleReference.expression;
}

function canCrossNodeWhileReorder(node) {
  return isPlainRequireModule(node) || isPlainImportModule(node) || isPlainImportEquals(node);
}

function canReorderItems(firstNode, secondNode) {
  var parent = firstNode.parent;var _sort =
  [
  parent.body.indexOf(firstNode),
  parent.body.indexOf(secondNode)].
  sort(),_sort2 = _slicedToArray(_sort, 2),firstIndex = _sort2[0],secondIndex = _sort2[1];
  var nodesBetween = parent.body.slice(firstIndex, secondIndex + 1);var _iteratorNormalCompletion = true;var _didIteratorError = false;var _iteratorError = undefined;try {
    for (var _iterator = nodesBetween[Symbol.iterator](), _step; !(_iteratorNormalCompletion = (_step = _iterator.next()).done); _iteratorNormalCompletion = true) {var nodeBetween = _step.value;
      if (!canCrossNodeWhileReorder(nodeBetween)) {
        return false;
      }
    }} catch (err) {_didIteratorError = true;_iteratorError = err;} finally {try {if (!_iteratorNormalCompletion && _iterator['return']) {_iterator['return']();}} finally {if (_didIteratorError) {throw _iteratorError;}}}
  return true;
}

function fixOutOfOrder(context, firstNode, secondNode, order) {
  var sourceCode = context.getSourceCode();

  var firstRoot = findRootNode(firstNode.node);
  var firstRootStart = findStartOfLineWithComments(sourceCode, firstRoot);
  var firstRootEnd = findEndOfLineWithComments(sourceCode, firstRoot);

  var secondRoot = findRootNode(secondNode.node);
  var secondRootStart = findStartOfLineWithComments(sourceCode, secondRoot);
  var secondRootEnd = findEndOfLineWithComments(sourceCode, secondRoot);
  var canFix = canReorderItems(firstRoot, secondRoot);

  var newCode = sourceCode.text.substring(secondRootStart, secondRootEnd);
  if (newCode[newCode.length - 1] !== '\n') {
    newCode = newCode + '\n';
  }

  var message = '`' + String(secondNode.displayName) + '` import should occur ' + String(order) + ' import of `' + String(firstNode.displayName) + '`';

  if (order === 'before') {
    context.report({
      node: secondNode.node,
      message: message,
      fix: canFix && function (fixer) {return (
          fixer.replaceTextRange(
          [firstRootStart, secondRootEnd],
          newCode + sourceCode.text.substring(firstRootStart, secondRootStart)));} });


  } else if (order === 'after') {
    context.report({
      node: secondNode.node,
      message: message,
      fix: canFix && function (fixer) {return (
          fixer.replaceTextRange(
          [secondRootStart, firstRootEnd],
          sourceCode.text.substring(secondRootEnd, firstRootEnd) + newCode));} });


  }
}

function reportOutOfOrder(context, imported, outOfOrder, order) {
  outOfOrder.forEach(function (imp) {
    var found = imported.find(function () {function hasHigherRank(importedItem) {
        return importedItem.rank > imp.rank;
      }return hasHigherRank;}());
    fixOutOfOrder(context, found, imp, order);
  });
}

function makeOutOfOrderReport(context, imported) {
  var outOfOrder = findOutOfOrder(imported);
  if (!outOfOrder.length) {
    return;
  }
  // There are things to report. Try to minimize the number of reported errors.
  var reversedImported = reverse(imported);
  var reversedOrder = findOutOfOrder(reversedImported);
  if (reversedOrder.length < outOfOrder.length) {
    reportOutOfOrder(context, reversedImported, reversedOrder, 'after');
    return;
  }
  reportOutOfOrder(context, imported, outOfOrder, 'before');
}

function getSorter(ascending) {
  var multiplier = ascending ? 1 : -1;

  return function () {function importsSorter(importA, importB) {
      var result = void 0;

      if (importA < importB) {
        result = -1;
      } else if (importA > importB) {
        result = 1;
      } else {
        result = 0;
      }

      return result * multiplier;
    }return importsSorter;}();
}

function mutateRanksToAlphabetize(imported, alphabetizeOptions) {
  var groupedByRanks = imported.reduce(function (acc, importedItem) {
    if (!Array.isArray(acc[importedItem.rank])) {
      acc[importedItem.rank] = [];
    }
    acc[importedItem.rank].push(importedItem);
    return acc;
  }, {});

  var groupRanks = Object.keys(groupedByRanks);

  var sorterFn = getSorter(alphabetizeOptions.order === 'asc');
  var comparator = alphabetizeOptions.caseInsensitive ?
  function (a, b) {return sorterFn(String(a.value).toLowerCase(), String(b.value).toLowerCase());} :
  function (a, b) {return sorterFn(a.value, b.value);};

  // sort imports locally within their group
  groupRanks.forEach(function (groupRank) {
    groupedByRanks[groupRank].sort(comparator);
  });

  // assign globally unique rank to each import
  var newRank = 0;
  var alphabetizedRanks = groupRanks.sort().reduce(function (acc, groupRank) {
    groupedByRanks[groupRank].forEach(function (importedItem) {
      acc[String(importedItem.value) + '|' + String(importedItem.node.importKind)] = parseInt(groupRank, 10) + newRank;
      newRank += 1;
    });
    return acc;
  }, {});

  // mutate the original group-rank with alphabetized-rank
  imported.forEach(function (importedItem) {
    importedItem.rank = alphabetizedRanks[String(importedItem.value) + '|' + String(importedItem.node.importKind)];
  });
}

// DETECTING

function computePathRank(ranks, pathGroups, path, maxPosition) {
  for (var i = 0, l = pathGroups.length; i < l; i++) {var _pathGroups$i =
    pathGroups[i],pattern = _pathGroups$i.pattern,patternOptions = _pathGroups$i.patternOptions,group = _pathGroups$i.group,_pathGroups$i$positio = _pathGroups$i.position,position = _pathGroups$i$positio === undefined ? 1 : _pathGroups$i$positio;
    if ((0, _minimatch2['default'])(path, pattern, patternOptions || { nocomment: true })) {
      return ranks[group] + position / maxPosition;
    }
  }
}

function computeRank(context, ranks, importEntry, excludedImportTypes) {
  var impType = void 0;
  var rank = void 0;
  if (importEntry.type === 'import:object') {
    impType = 'object';
  } else if (importEntry.node.importKind === 'type' && ranks.omittedTypes.indexOf('type') === -1) {
    impType = 'type';
  } else {
    impType = (0, _importType2['default'])(importEntry.value, context);
  }
  if (!excludedImportTypes.has(impType)) {
    rank = computePathRank(ranks.groups, ranks.pathGroups, importEntry.value, ranks.maxPosition);
  }
  if (typeof rank === 'undefined') {
    rank = ranks.groups[impType];
  }
  if (importEntry.type !== 'import' && !importEntry.type.startsWith('import:')) {
    rank += 100;
  }

  return rank;
}

function registerNode(context, importEntry, ranks, imported, excludedImportTypes) {
  var rank = computeRank(context, ranks, importEntry, excludedImportTypes);
  if (rank !== -1) {
    imported.push(Object.assign({}, importEntry, { rank: rank }));
  }
}

function getRequireBlock(node) {
  var n = node;
  // Handle cases like `const baz = require('foo').bar.baz`
  // and `const foo = require('foo')()`
  while (
  n.parent.type === 'MemberExpression' && n.parent.object === n ||
  n.parent.type === 'CallExpression' && n.parent.callee === n)
  {
    n = n.parent;
  }
  if (
  n.parent.type === 'VariableDeclarator' &&
  n.parent.parent.type === 'VariableDeclaration' &&
  n.parent.parent.parent.type === 'Program')
  {
    return n.parent.parent.parent;
  }
}

var types = ['builtin', 'external', 'internal', 'unknown', 'parent', 'sibling', 'index', 'object', 'type'];

// Creates an object with type-rank pairs.
// Example: { index: 0, sibling: 1, parent: 1, external: 1, builtin: 2, internal: 2 }
// Will throw an error if it contains a type that does not exist, or has a duplicate
function convertGroupsToRanks(groups) {
  var rankObject = groups.reduce(function (res, group, index) {
    if (typeof group === 'string') {
      group = [group];
    }
    group.forEach(function (groupItem) {
      if (types.indexOf(groupItem) === -1) {
        throw new Error('Incorrect configuration of the rule: Unknown type `' +
        JSON.stringify(groupItem) + '`');
      }
      if (res[groupItem] !== undefined) {
        throw new Error('Incorrect configuration of the rule: `' + groupItem + '` is duplicated');
      }
      res[groupItem] = index;
    });
    return res;
  }, {});

  var omittedTypes = types.filter(function (type) {
    return rankObject[type] === undefined;
  });

  var ranks = omittedTypes.reduce(function (res, type) {
    res[type] = groups.length;
    return res;
  }, rankObject);

  return { groups: ranks, omittedTypes: omittedTypes };
}

function convertPathGroupsForRanks(pathGroups) {
  var after = {};
  var before = {};

  var transformed = pathGroups.map(function (pathGroup, index) {var
    group = pathGroup.group,positionString = pathGroup.position;
    var position = 0;
    if (positionString === 'after') {
      if (!after[group]) {
        after[group] = 1;
      }
      position = after[group]++;
    } else if (positionString === 'before') {
      if (!before[group]) {
        before[group] = [];
      }
      before[group].push(index);
    }

    return Object.assign({}, pathGroup, { position: position });
  });

  var maxPosition = 1;

  Object.keys(before).forEach(function (group) {
    var groupLength = before[group].length;
    before[group].forEach(function (groupIndex, index) {
      transformed[groupIndex].position = -1 * (groupLength - index);
    });
    maxPosition = Math.max(maxPosition, groupLength);
  });

  Object.keys(after).forEach(function (key) {
    var groupNextPosition = after[key];
    maxPosition = Math.max(maxPosition, groupNextPosition - 1);
  });

  return {
    pathGroups: transformed,
    maxPosition: maxPosition > 10 ? Math.pow(10, Math.ceil(Math.log10(maxPosition))) : 10 };

}

function fixNewLineAfterImport(context, previousImport) {
  var prevRoot = findRootNode(previousImport.node);
  var tokensToEndOfLine = takeTokensAfterWhile(
  context.getSourceCode(), prevRoot, commentOnSameLineAs(prevRoot));

  var endOfLine = prevRoot.range[1];
  if (tokensToEndOfLine.length > 0) {
    endOfLine = tokensToEndOfLine[tokensToEndOfLine.length - 1].range[1];
  }
  return function (fixer) {return fixer.insertTextAfterRange([prevRoot.range[0], endOfLine], '\n');};
}

function removeNewLineAfterImport(context, currentImport, previousImport) {
  var sourceCode = context.getSourceCode();
  var prevRoot = findRootNode(previousImport.node);
  var currRoot = findRootNode(currentImport.node);
  var rangeToRemove = [
  findEndOfLineWithComments(sourceCode, prevRoot),
  findStartOfLineWithComments(sourceCode, currRoot)];

  if (/^\s*$/.test(sourceCode.text.substring(rangeToRemove[0], rangeToRemove[1]))) {
    return function (fixer) {return fixer.removeRange(rangeToRemove);};
  }
  return undefined;
}

function makeNewlinesBetweenReport(context, imported, newlinesBetweenImports) {
  var getNumberOfEmptyLinesBetween = function getNumberOfEmptyLinesBetween(currentImport, previousImport) {
    var linesBetweenImports = context.getSourceCode().lines.slice(
    previousImport.node.loc.end.line,
    currentImport.node.loc.start.line - 1);


    return linesBetweenImports.filter(function (line) {return !line.trim().length;}).length;
  };
  var previousImport = imported[0];

  imported.slice(1).forEach(function (currentImport) {
    var emptyLinesBetween = getNumberOfEmptyLinesBetween(currentImport, previousImport);

    if (newlinesBetweenImports === 'always' ||
    newlinesBetweenImports === 'always-and-inside-groups') {
      if (currentImport.rank !== previousImport.rank && emptyLinesBetween === 0) {
        context.report({
          node: previousImport.node,
          message: 'There should be at least one empty line between import groups',
          fix: fixNewLineAfterImport(context, previousImport) });

      } else if (currentImport.rank === previousImport.rank &&
      emptyLinesBetween > 0 &&
      newlinesBetweenImports !== 'always-and-inside-groups') {
        context.report({
          node: previousImport.node,
          message: 'There should be no empty line within import group',
          fix: removeNewLineAfterImport(context, currentImport, previousImport) });

      }
    } else if (emptyLinesBetween > 0) {
      context.report({
        node: previousImport.node,
        message: 'There should be no empty line between import groups',
        fix: removeNewLineAfterImport(context, currentImport, previousImport) });

    }

    previousImport = currentImport;
  });
}

function getAlphabetizeConfig(options) {
  var alphabetize = options.alphabetize || {};
  var order = alphabetize.order || 'ignore';
  var caseInsensitive = alphabetize.caseInsensitive || false;

  return { order: order, caseInsensitive: caseInsensitive };
}

module.exports = {
  meta: {
    type: 'suggestion',
    docs: {
      url: (0, _docsUrl2['default'])('order') },


    fixable: 'code',
    schema: [
    {
      type: 'object',
      properties: {
        groups: {
          type: 'array' },

        pathGroupsExcludedImportTypes: {
          type: 'array' },

        pathGroups: {
          type: 'array',
          items: {
            type: 'object',
            properties: {
              pattern: {
                type: 'string' },

              patternOptions: {
                type: 'object' },

              group: {
                type: 'string',
                'enum': types },

              position: {
                type: 'string',
                'enum': ['after', 'before'] } },


            required: ['pattern', 'group'] } },


        'newlines-between': {
          'enum': [
          'ignore',
          'always',
          'always-and-inside-groups',
          'never'] },


        alphabetize: {
          type: 'object',
          properties: {
            caseInsensitive: {
              type: 'boolean',
              'default': false },

            order: {
              'enum': ['ignore', 'asc', 'desc'],
              'default': 'ignore' } },


          additionalProperties: false },

        warnOnUnassignedImports: {
          type: 'boolean',
          'default': false } },


      additionalProperties: false }] },




  create: function () {function importOrderRule(context) {
      var options = context.options[0] || {};
      var newlinesBetweenImports = options['newlines-between'] || 'ignore';
      var pathGroupsExcludedImportTypes = new Set(options['pathGroupsExcludedImportTypes'] || ['builtin', 'external', 'object']);
      var alphabetize = getAlphabetizeConfig(options);
      var ranks = void 0;

      try {var _convertPathGroupsFor =
        convertPathGroupsForRanks(options.pathGroups || []),pathGroups = _convertPathGroupsFor.pathGroups,maxPosition = _convertPathGroupsFor.maxPosition;var _convertGroupsToRanks =
        convertGroupsToRanks(options.groups || defaultGroups),groups = _convertGroupsToRanks.groups,omittedTypes = _convertGroupsToRanks.omittedTypes;
        ranks = {
          groups: groups,
          omittedTypes: omittedTypes,
          pathGroups: pathGroups,
          maxPosition: maxPosition };

      } catch (error) {
        // Malformed configuration
        return {
          Program: function () {function Program(node) {
              context.report(node, error.message);
            }return Program;}() };

      }
      var importMap = new Map();

      function getBlockImports(node) {
        if (!importMap.has(node)) {
          importMap.set(node, []);
        }
        return importMap.get(node);
      }

      return {
        ImportDeclaration: function () {function handleImports(node) {
            // Ignoring unassigned imports unless warnOnUnassignedImports is set
            if (node.specifiers.length || options.warnOnUnassignedImports) {
              var name = node.source.value;
              registerNode(
              context,
              {
                node: node,
                value: name,
                displayName: name,
                type: 'import' },

              ranks,
              getBlockImports(node.parent),
              pathGroupsExcludedImportTypes);

            }
          }return handleImports;}(),
        TSImportEqualsDeclaration: function () {function handleImports(node) {
            var displayName = void 0;
            var value = void 0;
            var type = void 0;
            // skip "export import"s
            if (node.isExport) {
              return;
            }
            if (node.moduleReference.type === 'TSExternalModuleReference') {
              value = node.moduleReference.expression.value;
              displayName = value;
              type = 'import';
            } else {
              value = '';
              displayName = context.getSourceCode().getText(node.moduleReference);
              type = 'import:object';
            }
            registerNode(
            context,
            {
              node: node,
              value: value,
              displayName: displayName,
              type: type },

            ranks,
            getBlockImports(node.parent),
            pathGroupsExcludedImportTypes);

          }return handleImports;}(),
        CallExpression: function () {function handleRequires(node) {
            if (!(0, _staticRequire2['default'])(node)) {
              return;
            }
            var block = getRequireBlock(node);
            if (!block) {
              return;
            }
            var name = node.arguments[0].value;
            registerNode(
            context,
            {
              node: node,
              value: name,
              displayName: name,
              type: 'require' },

            ranks,
            getBlockImports(block),
            pathGroupsExcludedImportTypes);

          }return handleRequires;}(),
        'Program:exit': function () {function reportAndReset() {
            importMap.forEach(function (imported) {
              if (newlinesBetweenImports !== 'ignore') {
                makeNewlinesBetweenReport(context, imported, newlinesBetweenImports);
              }

              if (alphabetize.order !== 'ignore') {
                mutateRanksToAlphabetize(imported, alphabetize);
              }

              makeOutOfOrderReport(context, imported);
            });

            importMap.clear();
          }return reportAndReset;}() };

    }return importOrderRule;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9vcmRlci5qcyJdLCJuYW1lcyI6WyJkZWZhdWx0R3JvdXBzIiwicmV2ZXJzZSIsImFycmF5IiwibWFwIiwidiIsIk9iamVjdCIsImFzc2lnbiIsInJhbmsiLCJnZXRUb2tlbnNPckNvbW1lbnRzQWZ0ZXIiLCJzb3VyY2VDb2RlIiwibm9kZSIsImNvdW50IiwiY3VycmVudE5vZGVPclRva2VuIiwicmVzdWx0IiwiaSIsImdldFRva2VuT3JDb21tZW50QWZ0ZXIiLCJwdXNoIiwiZ2V0VG9rZW5zT3JDb21tZW50c0JlZm9yZSIsImdldFRva2VuT3JDb21tZW50QmVmb3JlIiwidGFrZVRva2Vuc0FmdGVyV2hpbGUiLCJjb25kaXRpb24iLCJ0b2tlbnMiLCJsZW5ndGgiLCJ0YWtlVG9rZW5zQmVmb3JlV2hpbGUiLCJmaW5kT3V0T2ZPcmRlciIsImltcG9ydGVkIiwibWF4U2VlblJhbmtOb2RlIiwiZmlsdGVyIiwiaW1wb3J0ZWRNb2R1bGUiLCJyZXMiLCJmaW5kUm9vdE5vZGUiLCJwYXJlbnQiLCJib2R5IiwiZmluZEVuZE9mTGluZVdpdGhDb21tZW50cyIsInRva2Vuc1RvRW5kT2ZMaW5lIiwiY29tbWVudE9uU2FtZUxpbmVBcyIsImVuZE9mVG9rZW5zIiwicmFuZ2UiLCJ0ZXh0IiwidG9rZW4iLCJ0eXBlIiwibG9jIiwic3RhcnQiLCJsaW5lIiwiZW5kIiwiZmluZFN0YXJ0T2ZMaW5lV2l0aENvbW1lbnRzIiwic3RhcnRPZlRva2VucyIsImlzUGxhaW5SZXF1aXJlTW9kdWxlIiwiZGVjbGFyYXRpb25zIiwiZGVjbCIsImlkIiwiaW5pdCIsImNhbGxlZSIsIm5hbWUiLCJhcmd1bWVudHMiLCJpc1BsYWluSW1wb3J0TW9kdWxlIiwic3BlY2lmaWVycyIsImlzUGxhaW5JbXBvcnRFcXVhbHMiLCJtb2R1bGVSZWZlcmVuY2UiLCJleHByZXNzaW9uIiwiY2FuQ3Jvc3NOb2RlV2hpbGVSZW9yZGVyIiwiY2FuUmVvcmRlckl0ZW1zIiwiZmlyc3ROb2RlIiwic2Vjb25kTm9kZSIsImluZGV4T2YiLCJzb3J0IiwiZmlyc3RJbmRleCIsInNlY29uZEluZGV4Iiwibm9kZXNCZXR3ZWVuIiwic2xpY2UiLCJub2RlQmV0d2VlbiIsImZpeE91dE9mT3JkZXIiLCJjb250ZXh0Iiwib3JkZXIiLCJnZXRTb3VyY2VDb2RlIiwiZmlyc3RSb290IiwiZmlyc3RSb290U3RhcnQiLCJmaXJzdFJvb3RFbmQiLCJzZWNvbmRSb290Iiwic2Vjb25kUm9vdFN0YXJ0Iiwic2Vjb25kUm9vdEVuZCIsImNhbkZpeCIsIm5ld0NvZGUiLCJzdWJzdHJpbmciLCJtZXNzYWdlIiwiZGlzcGxheU5hbWUiLCJyZXBvcnQiLCJmaXgiLCJmaXhlciIsInJlcGxhY2VUZXh0UmFuZ2UiLCJyZXBvcnRPdXRPZk9yZGVyIiwib3V0T2ZPcmRlciIsImZvckVhY2giLCJpbXAiLCJmb3VuZCIsImZpbmQiLCJoYXNIaWdoZXJSYW5rIiwiaW1wb3J0ZWRJdGVtIiwibWFrZU91dE9mT3JkZXJSZXBvcnQiLCJyZXZlcnNlZEltcG9ydGVkIiwicmV2ZXJzZWRPcmRlciIsImdldFNvcnRlciIsImFzY2VuZGluZyIsIm11bHRpcGxpZXIiLCJpbXBvcnRzU29ydGVyIiwiaW1wb3J0QSIsImltcG9ydEIiLCJtdXRhdGVSYW5rc1RvQWxwaGFiZXRpemUiLCJhbHBoYWJldGl6ZU9wdGlvbnMiLCJncm91cGVkQnlSYW5rcyIsInJlZHVjZSIsImFjYyIsIkFycmF5IiwiaXNBcnJheSIsImdyb3VwUmFua3MiLCJrZXlzIiwic29ydGVyRm4iLCJjb21wYXJhdG9yIiwiY2FzZUluc2Vuc2l0aXZlIiwiYSIsImIiLCJTdHJpbmciLCJ2YWx1ZSIsInRvTG93ZXJDYXNlIiwiZ3JvdXBSYW5rIiwibmV3UmFuayIsImFscGhhYmV0aXplZFJhbmtzIiwiaW1wb3J0S2luZCIsInBhcnNlSW50IiwiY29tcHV0ZVBhdGhSYW5rIiwicmFua3MiLCJwYXRoR3JvdXBzIiwicGF0aCIsIm1heFBvc2l0aW9uIiwibCIsInBhdHRlcm4iLCJwYXR0ZXJuT3B0aW9ucyIsImdyb3VwIiwicG9zaXRpb24iLCJub2NvbW1lbnQiLCJjb21wdXRlUmFuayIsImltcG9ydEVudHJ5IiwiZXhjbHVkZWRJbXBvcnRUeXBlcyIsImltcFR5cGUiLCJvbWl0dGVkVHlwZXMiLCJoYXMiLCJncm91cHMiLCJzdGFydHNXaXRoIiwicmVnaXN0ZXJOb2RlIiwiZ2V0UmVxdWlyZUJsb2NrIiwibiIsIm9iamVjdCIsInR5cGVzIiwiY29udmVydEdyb3Vwc1RvUmFua3MiLCJyYW5rT2JqZWN0IiwiaW5kZXgiLCJncm91cEl0ZW0iLCJFcnJvciIsIkpTT04iLCJzdHJpbmdpZnkiLCJ1bmRlZmluZWQiLCJjb252ZXJ0UGF0aEdyb3Vwc0ZvclJhbmtzIiwiYWZ0ZXIiLCJiZWZvcmUiLCJ0cmFuc2Zvcm1lZCIsInBhdGhHcm91cCIsInBvc2l0aW9uU3RyaW5nIiwiZ3JvdXBMZW5ndGgiLCJncm91cEluZGV4IiwiTWF0aCIsIm1heCIsImtleSIsImdyb3VwTmV4dFBvc2l0aW9uIiwicG93IiwiY2VpbCIsImxvZzEwIiwiZml4TmV3TGluZUFmdGVySW1wb3J0IiwicHJldmlvdXNJbXBvcnQiLCJwcmV2Um9vdCIsImVuZE9mTGluZSIsImluc2VydFRleHRBZnRlclJhbmdlIiwicmVtb3ZlTmV3TGluZUFmdGVySW1wb3J0IiwiY3VycmVudEltcG9ydCIsImN1cnJSb290IiwicmFuZ2VUb1JlbW92ZSIsInRlc3QiLCJyZW1vdmVSYW5nZSIsIm1ha2VOZXdsaW5lc0JldHdlZW5SZXBvcnQiLCJuZXdsaW5lc0JldHdlZW5JbXBvcnRzIiwiZ2V0TnVtYmVyT2ZFbXB0eUxpbmVzQmV0d2VlbiIsImxpbmVzQmV0d2VlbkltcG9ydHMiLCJsaW5lcyIsInRyaW0iLCJlbXB0eUxpbmVzQmV0d2VlbiIsImdldEFscGhhYmV0aXplQ29uZmlnIiwib3B0aW9ucyIsImFscGhhYmV0aXplIiwibW9kdWxlIiwiZXhwb3J0cyIsIm1ldGEiLCJkb2NzIiwidXJsIiwiZml4YWJsZSIsInNjaGVtYSIsInByb3BlcnRpZXMiLCJwYXRoR3JvdXBzRXhjbHVkZWRJbXBvcnRUeXBlcyIsIml0ZW1zIiwicmVxdWlyZWQiLCJhZGRpdGlvbmFsUHJvcGVydGllcyIsIndhcm5PblVuYXNzaWduZWRJbXBvcnRzIiwiY3JlYXRlIiwiaW1wb3J0T3JkZXJSdWxlIiwiU2V0IiwiZXJyb3IiLCJQcm9ncmFtIiwiaW1wb3J0TWFwIiwiTWFwIiwiZ2V0QmxvY2tJbXBvcnRzIiwic2V0IiwiZ2V0IiwiSW1wb3J0RGVjbGFyYXRpb24iLCJoYW5kbGVJbXBvcnRzIiwic291cmNlIiwiVFNJbXBvcnRFcXVhbHNEZWNsYXJhdGlvbiIsImlzRXhwb3J0IiwiZ2V0VGV4dCIsIkNhbGxFeHByZXNzaW9uIiwiaGFuZGxlUmVxdWlyZXMiLCJibG9jayIsInJlcG9ydEFuZFJlc2V0IiwiY2xlYXIiXSwibWFwcGluZ3MiOiJBQUFBLGE7O0FBRUEsc0M7QUFDQSxnRDtBQUNBLHNEO0FBQ0EscUM7O0FBRUEsSUFBTUEsZ0JBQWdCLENBQUMsU0FBRCxFQUFZLFVBQVosRUFBd0IsUUFBeEIsRUFBa0MsU0FBbEMsRUFBNkMsT0FBN0MsQ0FBdEI7O0FBRUE7O0FBRUEsU0FBU0MsT0FBVCxDQUFpQkMsS0FBakIsRUFBd0I7QUFDdEIsU0FBT0EsTUFBTUMsR0FBTixDQUFVLFVBQVVDLENBQVYsRUFBYTtBQUM1QixXQUFPQyxPQUFPQyxNQUFQLENBQWMsRUFBZCxFQUFrQkYsQ0FBbEIsRUFBcUIsRUFBRUcsTUFBTSxDQUFDSCxFQUFFRyxJQUFYLEVBQXJCLENBQVA7QUFDRCxHQUZNLEVBRUpOLE9BRkksRUFBUDtBQUdEOztBQUVELFNBQVNPLHdCQUFULENBQWtDQyxVQUFsQyxFQUE4Q0MsSUFBOUMsRUFBb0RDLEtBQXBELEVBQTJEO0FBQ3pELE1BQUlDLHFCQUFxQkYsSUFBekI7QUFDQSxNQUFNRyxTQUFTLEVBQWY7QUFDQSxPQUFLLElBQUlDLElBQUksQ0FBYixFQUFnQkEsSUFBSUgsS0FBcEIsRUFBMkJHLEdBQTNCLEVBQWdDO0FBQzlCRix5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QUFZQThMLHNCQUFVZSxLQUFWO0FBQ0QsV0FkRCxPQUF5QkQsY0FBekIsSUF2RUssRUFBUDs7QUF1RkQsS0F4SEQsT0FBaUJsQixlQUFqQixJQXpFZSxFQUFqQiIsImZpbGUiOiJvcmRlci5qcyIsInNvdXJjZXNDb250ZW50IjpbIid1c2Ugc3RyaWN0JztcblxuaW1wb3J0IG1pbmltYXRjaCBmcm9tICdtaW5pbWF0Y2gnO1xuaW1wb3J0IGltcG9ydFR5cGUgZnJvbSAnLi4vY29yZS9pbXBvcnRUeXBlJztcbmltcG9ydCBpc1N0YXRpY1JlcXVpcmUgZnJvbSAnLi4vY29yZS9zdGF0aWNSZXF1aXJlJztcbmltcG9ydCBkb2NzVXJsIGZyb20gJy4uL2RvY3NVcmwnO1xuXG5jb25zdCBkZWZhdWx0R3JvdXBzID0gWydidWlsdGluJywgJ2V4dGVybmFsJywgJ3BhcmVudCcsICdzaWJsaW5nJywgJ2luZGV4J107XG5cbi8vIFJFUE9SVElORyBBTkQgRklYSU5HXG5cbmZ1bmN0aW9uIHJldmVyc2UoYXJyYXkpIHtcbiAgcmV0dXJuIGFycmF5Lm1hcChmdW5jdGlvbiAodikge1xuICAgIHJldHVybiBPYmplY3QuYXNzaWduKHt9LCB2LCB7IHJhbms6IC12LnJhbmsgfSk7XG4gIH0pLnJldmVyc2UoKTtcbn1cblxuZnVuY3Rpb24gZ2V0VG9rZW5zT3JDb21tZW50c0FmdGVyKHNvdXJjZUNvZGUsIG5vZGUsIGNvdW50KSB7XG4gIGxldCBjdXJyZW50Tm9kZU9yVG9rZW4gPSBub2RlO1xuICBjb25zdCByZXN1bHQgPSBbXTtcbiAgZm9yIChsZXQgaSA9IDA7IGkgPCBjb3VudDsgaSsrKSB7XG4gICAgY3VycmVudE5vZGVPclRva2VuID0gc291cmNlQ29kZS5nZXRUb2tlbk9yQ29tbWVudEFmdGVyKGN1cnJlbnROb2RlT3JUb2tlbik7XG4gICAgaWYgKGN1cnJlbnROb2RlT3JUb2tlbiA9PSBudWxsKSB7XG4gICAgICBicmVhaztcbiAgICB9XG4gICAgcmVzdWx0LnB1c2goY3VycmVudE5vZGVPclRva2VuKTtcbiAgfVxuICByZXR1cm4gcmVzdWx0O1xufVxuXG5mdW5jdGlvbiBnZXRUb2tlbnNPckNvbW1lbnRzQmVmb3JlKHNvdXJjZUNvZGUsIG5vZGUsIGNvdW50KSB7XG4gIGxldCBjdXJyZW50Tm9kZU9yVG9rZW4gPSBub2RlO1xuICBjb25zdCByZXN1bHQgPSBbXTtcbiAgZm9yIChsZXQgaSA9IDA7IGkgPCBjb3VudDsgaSsrKSB7XG4gICAgY3VycmVudE5vZGVPclRva2VuID0gc291cmNlQ29kZS5nZXRUb2tlbk9yQ29tbWVudEJlZm9yZShjdXJyZW50Tm9kZU9yVG9rZW4pO1xuICAgIGlmIChjdXJyZW50Tm9kZU9yVG9rZW4gPT0gbnVsbCkge1xuICAgICAgYnJlYWs7XG4gICAgfVxuICAgIHJlc3VsdC5wdXNoKGN1cnJlbnROb2RlT3JUb2tlbik7XG4gIH1cbiAgcmV0dXJuIHJlc3VsdC5yZXZlcnNlKCk7XG59XG5cbmZ1bmN0aW9uIHRha2VUb2tlbnNBZnRlcldoaWxlKHNvdXJjZUNvZGUsIG5vZGUsIGNvbmRpdGlvbikge1xuICBjb25zdCB0b2tlbnMgPSBnZXRUb2tlbnNPckNvbW1lbnRzQWZ0ZXIoc291cmNlQ29kZSwgbm9kZSwgMTAwKTtcbiAgY29uc3QgcmVzdWx0ID0gW107XG4gIGZvciAobGV0IGkgPSAwOyBpIDwgdG9rZW5zLmxlbmd0aDsgaSsrKSB7XG4gICAgaWYgKGNvbmRpdGlvbih0b2tlbnNbaV0pKSB7XG4gICAgICByZXN1bHQucHVzaCh0b2tlbnNbaV0pO1xuICAgIH0gZWxzZSB7XG4gICAgICBicmVhaztcbiAgICB9XG4gIH1cbiAgcmV0dXJuIHJlc3VsdDtcbn1cblxuZnVuY3Rpb24gdGFrZVRva2Vuc0JlZm9yZVdoaWxlKHNvdXJjZUNvZGUsIG5vZGUsIGNvbmRpdGlvbikge1xuICBjb25zdCB0b2tlbnMgPSBnZXRUb2tlbnNPckNvbW1lbnRzQmVmb3JlKHNvdXJjZUNvZGUsIG5vZGUsIDEwMCk7XG4gIGNvbnN0IHJlc3VsdCA9IFtdO1xuICBmb3IgKGxldCBpID0gdG9rZW5zLmxlbmd0aCAtIDE7IGkgPj0gMDsgaS0tKSB7XG4gICAgaWYgKGNvbmRpdGlvbih0b2tlbnNbaV0pKSB7XG4gICAgICByZXN1bHQucHVzaCh0b2tlbnNbaV0pO1xuICAgIH0gZWxzZSB7XG4gICAgICBicmVhaztcbiAgICB9XG4gIH1cbiAgcmV0dXJuIHJlc3VsdC5yZXZlcnNlKCk7XG59XG5cbmZ1bmN0aW9uIGZpbmRPdXRPZk9yZGVyKGltcG9ydGVkKSB7XG4gIGlmIChpbXBvcnRlZC5sZW5ndGggPT09IDApIHtcbiAgICByZXR1cm4gW107XG4gIH1cbiAgbGV0IG1heFNlZW5SYW5rTm9kZSA9IGltcG9ydGVkWzBdO1xuICByZXR1cm4gaW1wb3J0ZWQuZmlsdGVyKGZ1bmN0aW9uIChpbXBvcnRlZE1vZHVsZSkge1xuICAgIGNvbnN0IHJlcyA9IGltcG9ydGVkTW9kdWxlLnJhbmsgPCBtYXhTZWVuUmFua05vZGUucmFuaztcbiAgICBpZiAobWF4U2VlblJhbmtOb2RlLnJhbmsgPCBpbXBvcnRlZE1vZHVsZS5yYW5rKSB7XG4gICAgICBtYXhTZWVuUmFua05vZGUgPSBpbXBvcnRlZE1vZHVsZTtcbiAgICB9XG4gICAgcmV0dXJuIHJlcztcbiAgfSk7XG59XG5cbmZ1bmN0aW9uIGZpbmRSb290Tm9kZShub2RlKSB7XG4gIGxldCBwYXJlbnQgPSBub2RlO1xuICB3aGlsZSAocGFyZW50LnBhcmVudCAhPSBudWxsICYmIHBhcmVudC5wYXJlbnQuYm9keSA9PSBudWxsKSB7XG4gICAgcGFyZW50ID0gcGFyZW50LnBhcmVudDtcbiAgfVxuICByZXR1cm4gcGFyZW50O1xufVxuXG5mdW5jdGlvbiBmaW5kRW5kT2ZMaW5lV2l0aENvbW1lbnRzKHNvdXJjZUNvZGUsIG5vZGUpIHtcbiAgY29uc3QgdG9rZW5zVG9FbmRPZkxpbmUgPSB0YWtlVG9rZW5zQWZ0ZXJXaGlsZShzb3VyY2VDb2RlLCBub2RlLCBjb21tZW50T25TYW1lTGluZUFzKG5vZGUpKTtcbiAgY29uc3QgZW5kT2ZUb2tlbnMgPSB0b2tlbnNUb0VuZE9mTGluZS5sZW5ndGggPiAwXG4gICAgPyB0b2tlbnNUb0VuZE9mTGluZVt0b2tlbnNUb0VuZE9mTGluZS5sZW5ndGggLSAxXS5yYW5nZVsxXVxuICAgIDogbm9kZS5yYW5nZVsxXTtcbiAgbGV0IHJlc3VsdCA9IGVuZE9mVG9rZW5zO1xuICBmb3IgKGxldCBpID0gZW5kT2ZUb2tlbnM7IGkgPCBzb3VyY2VDb2RlLnRleHQubGVuZ3RoOyBpKyspIHtcbiAgICBpZiAoc291cmNlQ29kZS50ZXh0W2ldID09PSAnXFxuJykge1xuICAgICAgcmVzdWx0ID0gaSArIDE7XG4gICAgICBicmVhaztcbiAgICB9XG4gICAgaWYgKHNvdXJjZUNvZGUudGV4dFtpXSAhPT0gJyAnICYmIHNvdXJjZUNvZGUudGV4dFtpXSAhPT0gJ1xcdCcgJiYgc291cmNlQ29kZS50ZXh0W2ldICE9PSAnXFxyJykge1xuICAgICAgYnJlYWs7XG4gICAgfVxuICAgIHJlc3VsdCA9IGkgKyAxO1xuICB9XG4gIHJldHVybiByZXN1bHQ7XG59XG5cbmZ1bmN0aW9uIGNvbW1lbnRPblNhbWVMaW5lQXMobm9kZSkge1xuICByZXR1cm4gdG9rZW4gPT4gKHRva2VuLnR5cGUgPT09ICdCbG9jaycgfHwgIHRva2VuLnR5cGUgPT09ICdMaW5lJykgJiZcbiAgICAgIHRva2VuLmxvYy5zdGFydC5saW5lID09PSB0b2tlbi5sb2MuZW5kLmxpbmUgJiZcbiAgICAgIHRva2VuLmxvYy5lbmQubGluZSA9PT0gbm9kZS5sb2MuZW5kLmxpbmU7XG59XG5cbmZ1bmN0aW9uIGZpbmRTdGFydE9mTGluZVdpdGhDb21tZW50cyhzb3VyY2VDb2RlLCBub2RlKSB7XG4gIGNvbnN0IHRva2Vuc1RvRW5kT2ZMaW5lID0gdGFrZVRva2Vuc0JlZm9yZVdoaWxlKHNvdXJjZUNvZGUsIG5vZGUsIGNvbW1lbnRPblNhbWVMaW5lQXMobm9kZSkpO1xuICBjb25zdCBzdGFydE9mVG9rZW5zID0gdG9rZW5zVG9FbmRPZkxpbmUubGVuZ3RoID4gMCA/IHRva2Vuc1RvRW5kT2ZMaW5lWzBdLnJhbmdlWzBdIDogbm9kZS5yYW5nZVswXTtcbiAgbGV0IHJlc3VsdCA9IHN0YXJ0T2ZUb2tlbnM7XG4gIGZvciAobGV0IGkgPSBzdGFydE9mVG9rZW5zIC0gMTsgaSA+IDA7IGktLSkge1xuICAgIGlmIChzb3VyY2VDb2RlLnRleHRbaV0gIT09ICcgJyAmJiBzb3VyY2VDb2RlLnRleHRbaV0gIT09ICdcXHQnKSB7XG4gICAgICBicmVhaztcbiAgICB9XG4gICAgcmVzdWx0ID0gaTtcbiAgfVxuICByZXR1cm4gcmVzdWx0O1xufVxuXG5mdW5jdGlvbiBpc1BsYWluUmVxdWlyZU1vZHVsZShub2RlKSB7XG4gIGlmIChub2RlLnR5cGUgIT09ICdWYXJpYWJsZURlY2xhcmF0aW9uJykge1xuICAgIHJldHVybiBmYWxzZTtcbiAgfVxuICBpZiAobm9kZS5kZWNsYXJhdGlvbnMubGVuZ3RoICE9PSAxKSB7XG4gICAgcmV0dXJuIGZhbHNlO1xuICB9XG4gIGNvbnN0IGRlY2wgPSBub2RlLmRlY2xhcmF0aW9uc1swXTtcbiAgY29uc3QgcmVzdWx0ID0gZGVjbC5pZCAmJlxuICAgIChkZWNsLmlkLnR5cGUgPT09ICdJZGVudGlmaWVyJyB8fCBkZWNsLmlkLnR5cGUgPT09ICdPYmplY3RQYXR0ZXJuJykgJiZcbiAgICBkZWNsLmluaXQgIT0gbnVsbCAmJlxuICAgIGRlY2wuaW5pdC50eXBlID09PSAnQ2FsbEV4cHJlc3Npb24nICYmXG4gICAgZGVjbC5pbml0LmNhbGxlZSAhPSBudWxsICYmXG4gICAgZGVjbC5pbml0LmNhbGxlZS5uYW1lID09PSAncmVxdWlyZScgJiZcbiAgICBkZWNsLmluaXQuYXJndW1lbnRzICE9IG51bGwgJiZcbiAgICBkZWNsLmluaXQuYXJndW1lbnRzLmxlbmd0aCA9PT0gMSAmJlxuICAgIGRlY2wuaW5pdC5hcmd1bWVudHNbMF0udHlwZSA9PT0gJ0xpdGVyYWwnO1xuICByZXR1cm4gcmVzdWx0O1xufVxuXG5mdW5jdGlvbiBpc1BsYWluSW1wb3J0TW9kdWxlKG5vZGUpIHtcbiAgcmV0dXJuIG5vZGUudHlwZSA9PT0gJ0ltcG9ydERlY2xhcmF0aW9uJyAmJiBub2RlLnNwZWNpZmllcnMgIT0gbnVsbCAmJiBub2RlLnNwZWNpZmllcnMubGVuZ3RoID4gMDtcbn1cblxuZnVuY3Rpb24gaXNQbGFpbkltcG9ydEVxdWFscyhub2RlKSB7XG4gIHJldHVybiBub2RlLnR5cGUgPT09ICdUU0ltcG9ydEVxdWFsc0RlY2xhcmF0aW9uJyAmJiBub2RlLm1vZHVsZVJlZmVyZW5jZS5leHByZXNzaW9uO1xufVxuXG5mdW5jdGlvbiBjYW5Dcm9zc05vZGVXaGlsZVJlb3JkZXIobm9kZSkge1xuICByZXR1cm4gaXNQbGFpblJlcXVpcmVNb2R1bGUobm9kZSkgfHwgaXNQbGFpbkltcG9ydE1vZHVsZShub2RlKSB8fCBpc1BsYWluSW1wb3J0RXF1YWxzKG5vZGUpO1xufVxuXG5mdW5jdGlvbiBjYW5SZW9yZGVySXRlbXMoZmlyc3ROb2RlLCBzZWNvbmROb2RlKSB7XG4gIGNvbnN0IHBhcmVudCA9IGZpcnN0Tm9kZS5wYXJlbnQ7XG4gIGNvbnN0IFtmaXJzdEluZGV4LCBzZWNvbmRJbmRleF0gPSBbXG4gICAgcGFyZW50LmJvZHkuaW5kZXhPZihmaXJzdE5vZGUpLFxuICAgIHBhcmVudC5ib2R5LmluZGV4T2Yoc2Vjb25kTm9kZSksXG4gIF0uc29ydCgpO1xuICBjb25zdCBub2Rlc0JldHdlZW4gPSBwYXJlbnQuYm9keS5zbGljZShmaXJzdEluZGV4LCBzZWNvbmRJbmRleCArIDEpO1xuICBmb3IgKGNvbnN0IG5vZGVCZXR3ZWVuIG9mIG5vZGVzQmV0d2Vlbikge1xuICAgIGlmICghY2FuQ3Jvc3NOb2RlV2hpbGVSZW9yZGVyKG5vZGVCZXR3ZWVuKSkge1xuICAgICAgcmV0dXJuIGZhbHNlO1xuICAgIH1cbiAgfVxuICByZXR1cm4gdHJ1ZTtcbn1cblxuZnVuY3Rpb24gZml4T3V0T2ZPcmRlcihjb250ZXh0LCBmaXJzdE5vZGUsIHNlY29uZE5vZGUsIG9yZGVyKSB7XG4gIGNvbnN0IHNvdXJjZUNvZGUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKTtcblxuICBjb25zdCBmaXJzdFJvb3QgPSBmaW5kUm9vdE5vZGUoZmlyc3ROb2RlLm5vZGUpO1xuICBjb25zdCBmaXJzdFJvb3RTdGFydCA9IGZpbmRTdGFydE9mTGluZVdpdGhDb21tZW50cyhzb3VyY2VDb2RlLCBmaXJzdFJvb3QpO1xuICBjb25zdCBmaXJzdFJvb3RFbmQgPSBmaW5kRW5kT2ZMaW5lV2l0aENvbW1lbnRzKHNvdXJjZUNvZGUsIGZpcnN0Um9vdCk7XG5cbiAgY29uc3Qgc2Vjb25kUm9vdCA9IGZpbmRSb290Tm9kZShzZWNvbmROb2RlLm5vZGUpO1xuICBjb25zdCBzZWNvbmRSb290U3RhcnQgPSBmaW5kU3RhcnRPZkxpbmVXaXRoQ29tbWVudHMoc291cmNlQ29kZSwgc2Vjb25kUm9vdCk7XG4gIGNvbnN0IHNlY29uZFJvb3RFbmQgPSBmaW5kRW5kT2ZMaW5lV2l0aENvbW1lbnRzKHNvdXJjZUNvZGUsIHNlY29uZFJvb3QpO1xuICBjb25zdCBjYW5GaXggPSBjYW5SZW9yZGVySXRlbXMoZmlyc3RSb290LCBzZWNvbmRSb290KTtcblxuICBsZXQgbmV3Q29kZSA9IHNvdXJjZUNvZGUudGV4dC5zdWJzdHJpbmcoc2Vjb25kUm9vdFN0YXJ0LCBzZWNvbmRSb290RW5kKTtcbiAgaWYgKG5ld0NvZGVbbmV3Q29kZS5sZW5ndGggLSAxXSAhPT0gJ1xcbicpIHtcbiAgICBuZXdDb2RlID0gbmV3Q29kZSArICdcXG4nO1xuICB9XG5cbiAgY29uc3QgbWVzc2FnZSA9IGBcXGAke3NlY29uZE5vZGUuZGlzcGxheU5hbWV9XFxgIGltcG9ydCBzaG91bGQgb2NjdXIgJHtvcmRlcn0gaW1wb3J0IG9mIFxcYCR7Zmlyc3ROb2RlLmRpc3BsYXlOYW1lfVxcYGA7XG5cbiAgaWYgKG9yZGVyID09PSAnYmVmb3JlJykge1xuICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgIG5vZGU6IHNlY29uZE5vZGUubm9kZSxcbiAgICAgIG1lc3NhZ2UsXG4gICAgICBmaXg6IGNhbkZpeCAmJiAoZml4ZXIgPT5cbiAgICAgICAgZml4ZXIucmVwbGFjZVRleHRSYW5nZShcbiAgICAgICAgICBbZmlyc3RSb290U3RhcnQsIHNlY29uZFJvb3RFbmRdLFxuICAgICAgICAgIG5ld0NvZGUgKyBzb3VyY2VDb2RlLnRleHQuc3Vic3RyaW5nKGZpcnN0Um9vdFN0YXJ0LCBzZWNvbmRSb290U3RhcnQpLFxuICAgICAgICApKSxcbiAgICB9KTtcbiAgfSBlbHNlIGlmIChvcmRlciA9PT0gJ2FmdGVyJykge1xuICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgIG5vZGU6IHNlY29uZE5vZGUubm9kZSxcbiAgICAgIG1lc3NhZ2UsXG4gICAgICBmaXg6IGNhbkZpeCAmJiAoZml4ZXIgPT5cbiAgICAgICAgZml4ZXIucmVwbGFjZVRleHRSYW5nZShcbiAgICAgICAgICBbc2Vjb25kUm9vdFN0YXJ0LCBmaXJzdFJvb3RFbmRdLFxuICAgICAgICAgIHNvdXJjZUNvZGUudGV4dC5zdWJzdHJpbmcoc2Vjb25kUm9vdEVuZCwgZmlyc3RSb290RW5kKSArIG5ld0NvZGUsXG4gICAgICAgICkpLFxuICAgIH0pO1xuICB9XG59XG5cbmZ1bmN0aW9uIHJlcG9ydE91dE9mT3JkZXIoY29udGV4dCwgaW1wb3J0ZWQsIG91dE9mT3JkZXIsIG9yZGVyKSB7XG4gIG91dE9mT3JkZXIuZm9yRWFjaChmdW5jdGlvbiAoaW1wKSB7XG4gICAgY29uc3QgZm91bmQgPSBpbXBvcnRlZC5maW5kKGZ1bmN0aW9uIGhhc0hpZ2hlclJhbmsoaW1wb3J0ZWRJdGVtKSB7XG4gICAgICByZXR1cm4gaW1wb3J0ZWRJdGVtLnJhbmsgPiBpbXAucmFuaztcbiAgICB9KTtcbiAgICBmaXhPdXRPZk9yZGVyKGNvbnRleHQsIGZvdW5kLCBpbXAsIG9yZGVyKTtcbiAgfSk7XG59XG5cbmZ1bmN0aW9uIG1ha2VPdXRPZk9yZGVyUmVwb3J0KGNvbnRleHQsIGltcG9ydGVkKSB7XG4gIGNvbnN0IG91dE9mT3JkZXIgPSBmaW5kT3V0T2ZPcmRlcihpbXBvcnRlZCk7XG4gIGlmICghb3V0T2ZPcmRlci5sZW5ndGgpIHtcbiAgICByZXR1cm47XG4gIH1cbiAgLy8gVGhlcmUgYXJlIHRoaW5ncyB0byByZXBvcnQuIFRyeSB0byBtaW5pbWl6ZSB0aGUgbnVtYmVyIG9mIHJlcG9ydGVkIGVycm9ycy5cbiAgY29uc3QgcmV2ZXJzZWRJbXBvcnRlZCA9IHJldmVyc2UoaW1wb3J0ZWQpO1xuICBjb25zdCByZXZlcnNlZE9yZGVyID0gZmluZE91dE9mT3JkZXIocmV2ZXJzZWRJbXBvcnRlZCk7XG4gIGlmIChyZXZlcnNlZE9yZGVyLmxlbmd0aCA8IG91dE9mT3JkZXIubGVuZ3RoKSB7XG4gICAgcmVwb3J0T3V0T2ZPcmRlcihjb250ZXh0LCByZXZlcnNlZEltcG9ydGVkLCByZXZlcnNlZE9yZGVyLCAnYWZ0ZXInKTtcbiAgICByZXR1cm47XG4gIH1cbiAgcmVwb3J0T3V0T2ZPcmRlcihjb250ZXh0LCBpbXBvcnRlZCwgb3V0T2ZPcmRlciwgJ2JlZm9yZScpO1xufVxuXG5mdW5jdGlvbiBnZXRTb3J0ZXIoYXNjZW5kaW5nKSB7XG4gIGNvbnN0IG11bHRpcGxpZXIgPSBhc2NlbmRpbmcgPyAxIDogLTE7XG5cbiAgcmV0dXJuIGZ1bmN0aW9uIGltcG9ydHNTb3J0ZXIoaW1wb3J0QSwgaW1wb3J0Qikge1xuICAgIGxldCByZXN1bHQ7XG5cbiAgICBpZiAoaW1wb3J0QSA8IGltcG9ydEIpIHtcbiAgICAgIHJlc3VsdCA9IC0xO1xuICAgIH0gZWxzZSBpZiAoaW1wb3J0QSA+IGltcG9ydEIpIHtcbiAgICAgIHJlc3VsdCA9IDE7XG4gICAgfSBlbHNlIHtcbiAgICAgIHJlc3VsdCA9IDA7XG4gICAgfVxuXG4gICAgcmV0dXJuIHJlc3VsdCAqIG11bHRpcGxpZXI7XG4gIH07XG59XG5cbmZ1bmN0aW9uIG11dGF0ZVJhbmtzVG9BbHBoYWJldGl6ZShpbXBvcnRlZCwgYWxwaGFiZXRpemVPcHRpb25zKSB7XG4gIGNvbnN0IGdyb3VwZWRCeVJhbmtzID0gaW1wb3J0ZWQucmVkdWNlKGZ1bmN0aW9uIChhY2MsIGltcG9ydGVkSXRlbSkge1xuICAgIGlmICghQXJyYXkuaXNBcnJheShhY2NbaW1wb3J0ZWRJdGVtLnJhbmtdKSkge1xuICAgICAgYWNjW2ltcG9ydGVkSXRlbS5yYW5rXSA9IFtdO1xuICAgIH1cbiAgICBhY2NbaW1wb3J0ZWRJdGVtLnJhbmtdLnB1c2goaW1wb3J0ZWRJdGVtKTtcbiAgICByZXR1cm4gYWNjO1xuICB9LCB7fSk7XG5cbiAgY29uc3QgZ3JvdXBSYW5rcyA9IE9iamVjdC5rZXlzKGdyb3VwZWRCeVJhbmtzKTtcblxuICBjb25zdCBzb3J0ZXJGbiA9IGdldFNvcnRlcihhbHBoYWJldGl6ZU9wdGlvbnMub3JkZXIgPT09ICdhc2MnKTtcbiAgY29uc3QgY29tcGFyYXRvciA9IGFscGhhYmV0aXplT3B0aW9ucy5jYXNlSW5zZW5zaXRpdmVcbiAgICA/IChhLCBiKSA9PiBzb3J0ZXJGbihTdHJpbmcoYS52YWx1ZSkudG9Mb3dlckNhc2UoKSwgU3RyaW5nKGIudmFsdWUpLnRvTG93ZXJDYXNlKCkpXG4gICAgOiAoYSwgYikgPT4gc29ydGVyRm4oYS52YWx1ZSwgYi52YWx1ZSk7XG5cbiAgLy8gc29ydCBpbXBvcnRzIGxvY2FsbHkgd2l0aGluIHRoZWlyIGdyb3VwXG4gIGdyb3VwUmFua3MuZm9yRWFjaChmdW5jdGlvbiAoZ3JvdXBSYW5rKSB7XG4gICAgZ3JvdXBlZEJ5UmFua3NbZ3JvdXBSYW5rXS5zb3J0KGNvbXBhcmF0b3IpO1xuICB9KTtcblxuICAvLyBhc3NpZ24gZ2xvYmFsbHkgdW5pcXVlIHJhbmsgdG8gZWFjaCBpbXBvcnRcbiAgbGV0IG5ld1JhbmsgPSAwO1xuICBjb25zdCBhbHBoYWJldGl6ZWRSYW5rcyA9IGdyb3VwUmFua3Muc29ydCgpLnJlZHVjZShmdW5jdGlvbiAoYWNjLCBncm91cFJhbmspIHtcbiAgICBncm91cGVkQnlSYW5rc1tncm91cFJhbmtdLmZvckVhY2goZnVuY3Rpb24gKGltcG9ydGVkSXRlbSkge1xuICAgICAgYWNjW2Ake2ltcG9ydGVkSXRlbS52YWx1ZX18JHtpbXBvcnRlZEl0ZW0ubm9kZS5pbXBvcnRLaW5kfWBdID0gcGFyc2VJbnQoZ3JvdXBSYW5rLCAxMCkgKyBuZXdSYW5rO1xuICAgICAgbmV3UmFuayArPSAxO1xuICAgIH0pO1xuICAgIHJldHVybiBhY2M7XG4gIH0sIHt9KTtcblxuICAvLyBtdXRhdGUgdGhlIG9yaWdpbmFsIGdyb3VwLXJhbmsgd2l0aCBhbHBoYWJldGl6ZWQtcmFua1xuICBpbXBvcnRlZC5mb3JFYWNoKGZ1bmN0aW9uIChpbXBvcnRlZEl0ZW0pIHtcbiAgICBpbXBvcnRlZEl0ZW0ucmFuayA9IGFscGhhYmV0aXplZFJhbmtzW2Ake2ltcG9ydGVkSXRlbS52YWx1ZX18JHtpbXBvcnRlZEl0ZW0ubm9kZS5pbXBvcnRLaW5kfWBdO1xuICB9KTtcbn1cblxuLy8gREVURUNUSU5HXG5cbmZ1bmN0aW9uIGNvbXB1dGVQYXRoUmFuayhyYW5rcywgcGF0aEdyb3VwcywgcGF0aCwgbWF4UG9zaXRpb24pIHtcbiAgZm9yIChsZXQgaSA9IDAsIGwgPSBwYXRoR3JvdXBzLmxlbmd0aDsgaSA8IGw7IGkrKykge1xuICAgIGNvbnN0IHsgcGF0dGVybiwgcGF0dGVybk9wdGlvbnMsIGdyb3VwLCBwb3NpdGlvbiA9IDEgfSA9IHBhdGhHcm91cHNbaV07XG4gICAgaWYgKG1pbmltYXRjaChwYXRoLCBwYXR0ZXJuLCBwYXR0ZXJuT3B0aW9ucyB8fCB7IG5vY29tbWVudDogdHJ1ZSB9KSkge1xuICAgICAgcmV0dXJuIHJhbmtzW2dyb3VwXSArIChwb3NpdGlvbiAvIG1heFBvc2l0aW9uKTtcbiAgICB9XG4gIH1cbn1cblxuZnVuY3Rpb24gY29tcHV0ZVJhbmsoY29udGV4dCwgcmFua3MsIGltcG9ydEVudHJ5LCBleGNsdWRlZEltcG9ydFR5cGVzKSB7XG4gIGxldCBpbXBUeXBlO1xuICBsZXQgcmFuaztcbiAgaWYgKGltcG9ydEVudHJ5LnR5cGUgPT09ICdpbXBvcnQ6b2JqZWN0Jykge1xuICAgIGltcFR5cGUgPSAnb2JqZWN0JztcbiAgfSBlbHNlIGlmIChpbXBvcnRFbnRyeS5ub2RlLmltcG9ydEtpbmQgPT09ICd0eXBlJyAmJiByYW5rcy5vbWl0dGVkVHlwZXMuaW5kZXhPZigndHlwZScpID09PSAtMSkge1xuICAgIGltcFR5cGUgPSAndHlwZSc7XG4gIH0gZWxzZSB7XG4gICAgaW1wVHlwZSA9IGltcG9ydFR5cGUoaW1wb3J0RW50cnkudmFsdWUsIGNvbnRleHQpO1xuICB9XG4gIGlmICghZXhjbHVkZWRJbXBvcnRUeXBlcy5oYXMoaW1wVHlwZSkpIHtcbiAgICByYW5rID0gY29tcHV0ZVBhdGhSYW5rKHJhbmtzLmdyb3VwcywgcmFua3MucGF0aEdyb3VwcywgaW1wb3J0RW50cnkudmFsdWUsIHJhbmtzLm1heFBvc2l0aW9uKTtcbiAgfVxuICBpZiAodHlwZW9mIHJhbmsgPT09ICd1bmRlZmluZWQnKSB7XG4gICAgcmFuayA9IHJhbmtzLmdyb3Vwc1tpbXBUeXBlXTtcbiAgfVxuICBpZiAoaW1wb3J0RW50cnkudHlwZSAhPT0gJ2ltcG9ydCcgJiYgIWltcG9ydEVudHJ5LnR5cGUuc3RhcnRzV2l0aCgnaW1wb3J0OicpKSB7XG4gICAgcmFuayArPSAxMDA7XG4gIH1cblxuICByZXR1cm4gcmFuaztcbn1cblxuZnVuY3Rpb24gcmVnaXN0ZXJOb2RlKGNvbnRleHQsIGltcG9ydEVudHJ5LCByYW5rcywgaW1wb3J0ZWQsIGV4Y2x1ZGVkSW1wb3J0VHlwZXMpIHtcbiAgY29uc3QgcmFuayA9IGNvbXB1dGVSYW5rKGNvbnRleHQsIHJhbmtzLCBpbXBvcnRFbnRyeSwgZXhjbHVkZWRJbXBvcnRUeXBlcyk7XG4gIGlmIChyYW5rICE9PSAtMSkge1xuICAgIGltcG9ydGVkLnB1c2goT2JqZWN0LmFzc2lnbih7fSwgaW1wb3J0RW50cnksIHsgcmFuayB9KSk7XG4gIH1cbn1cblxuZnVuY3Rpb24gZ2V0UmVxdWlyZUJsb2NrKG5vZGUpIHtcbiAgbGV0IG4gPSBub2RlO1xuICAvLyBIYW5kbGUgY2FzZXMgbGlrZSBgY29uc3QgYmF6ID0gcmVxdWlyZSgnZm9vJykuYmFyLmJhemBcbiAgLy8gYW5kIGBjb25zdCBmb28gPSByZXF1aXJlKCdmb28nKSgpYFxuICB3aGlsZSAoXG4gICAgKG4ucGFyZW50LnR5cGUgPT09ICdNZW1iZXJFeHByZXNzaW9uJyAmJiBuLnBhcmVudC5vYmplY3QgPT09IG4pIHx8XG4gICAgKG4ucGFyZW50LnR5cGUgPT09ICdDYWxsRXhwcmVzc2lvbicgJiYgbi5wYXJlbnQuY2FsbGVlID09PSBuKVxuICApIHtcbiAgICBuID0gbi5wYXJlbnQ7XG4gIH1cbiAgaWYgKFxuICAgIG4ucGFyZW50LnR5cGUgPT09ICdWYXJpYWJsZURlY2xhcmF0b3InICYmXG4gICAgbi5wYXJlbnQucGFyZW50LnR5cGUgPT09ICdWYXJpYWJsZURlY2xhcmF0aW9uJyAmJlxuICAgIG4ucGFyZW50LnBhcmVudC5wYXJlbnQudHlwZSA9PT0gJ1Byb2dyYW0nXG4gICkge1xuICAgIHJldHVybiBuLnBhcmVudC5wYXJlbnQucGFyZW50O1xuICB9XG59XG5cbmNvbnN0IHR5cGVzID0gWydidWlsdGluJywgJ2V4dGVybmFsJywgJ2ludGVybmFsJywgJ3Vua25vd24nLCAncGFyZW50JywgJ3NpYmxpbmcnLCAnaW5kZXgnLCAnb2JqZWN0JywgJ3R5cGUnXTtcblxuLy8gQ3JlYXRlcyBhbiBvYmplY3Qgd2l0aCB0eXBlLXJhbmsgcGFpcnMuXG4vLyBFeGFtcGxlOiB7IGluZGV4OiAwLCBzaWJsaW5nOiAxLCBwYXJlbnQ6IDEsIGV4dGVybmFsOiAxLCBidWlsdGluOiAyLCBpbnRlcm5hbDogMiB9XG4vLyBXaWxsIHRocm93IGFuIGVycm9yIGlmIGl0IGNvbnRhaW5zIGEgdHlwZSB0aGF0IGRvZXMgbm90IGV4aXN0LCBvciBoYXMgYSBkdXBsaWNhdGVcbmZ1bmN0aW9uIGNvbnZlcnRHcm91cHNUb1JhbmtzKGdyb3Vwcykge1xuICBjb25zdCByYW5rT2JqZWN0ID0gZ3JvdXBzLnJlZHVjZShmdW5jdGlvbiAocmVzLCBncm91cCwgaW5kZXgpIHtcbiAgICBpZiAodHlwZW9mIGdyb3VwID09PSAnc3RyaW5nJykge1xuICAgICAgZ3JvdXAgPSBbZ3JvdXBdO1xuICAgIH1cbiAgICBncm91cC5mb3JFYWNoKGZ1bmN0aW9uIChncm91cEl0ZW0pIHtcbiAgICAgIGlmICh0eXBlcy5pbmRleE9mKGdyb3VwSXRlbSkgPT09IC0xKSB7XG4gICAgICAgIHRocm93IG5ldyBFcnJvcignSW5jb3JyZWN0IGNvbmZpZ3VyYXRpb24gb2YgdGhlIHJ1bGU6IFVua25vd24gdHlwZSBgJyArXG4gICAgICAgICAgSlNPTi5zdHJpbmdpZnkoZ3JvdXBJdGVtKSArICdgJyk7XG4gICAgICB9XG4gICAgICBpZiAocmVzW2dyb3VwSXRlbV0gIT09IHVuZGVmaW5lZCkge1xuICAgICAgICB0aHJvdyBuZXcgRXJyb3IoJ0luY29ycmVjdCBjb25maWd1cmF0aW9uIG9mIHRoZSBydWxlOiBgJyArIGdyb3VwSXRlbSArICdgIGlzIGR1cGxpY2F0ZWQnKTtcbiAgICAgIH1cbiAgICAgIHJlc1tncm91cEl0ZW1dID0gaW5kZXg7XG4gICAgfSk7XG4gICAgcmV0dXJuIHJlcztcbiAgfSwge30pO1xuXG4gIGNvbnN0IG9taXR0ZWRUeXBlcyA9IHR5cGVzLmZpbHRlcihmdW5jdGlvbiAodHlwZSkge1xuICAgIHJldHVybiByYW5rT2JqZWN0W3R5cGVdID09PSB1bmRlZmluZWQ7XG4gIH0pO1xuXG4gIGNvbnN0IHJhbmtzID0gb21pdHRlZFR5cGVzLnJlZHVjZShmdW5jdGlvbiAocmVzLCB0eXBlKSB7XG4gICAgcmVzW3R5cGVdID0gZ3JvdXBzLmxlbmd0aDtcbiAgICByZXR1cm4gcmVzO1xuICB9LCByYW5rT2JqZWN0KTtcblxuICByZXR1cm4geyBncm91cHM6IHJhbmtzLCBvbWl0dGVkVHlwZXMgfTtcbn1cblxuZnVuY3Rpb24gY29udmVydFBhdGhHcm91cHNGb3JSYW5rcyhwYXRoR3JvdXBzKSB7XG4gIGNvbnN0IGFmdGVyID0ge307XG4gIGNvbnN0IGJlZm9yZSA9IHt9O1xuXG4gIGNvbnN0IHRyYW5zZm9ybWVkID0gcGF0aEdyb3Vwcy5tYXAoKHBhdGhHcm91cCwgaW5kZXgpID0+IHtcbiAgICBjb25zdCB7IGdyb3VwLCBwb3NpdGlvbjogcG9zaXRpb25TdHJpbmcgfSA9IHBhdGhHcm91cDtcbiAgICBsZXQgcG9zaXRpb24gPSAwO1xuICAgIGlmIChwb3NpdGlvblN0cmluZyA9PT0gJ2FmdGVyJykge1xuICAgICAgaWYgKCFhZnRlcltncm91cF0pIHtcbiAgICAgICAgYWZ0ZXJbZ3JvdXBdID0gMTtcbiAgICAgIH1cbiAgICAgIHBvc2l0aW9uID0gYWZ0ZXJbZ3JvdXBdKys7XG4gICAgfSBlbHNlIGlmIChwb3NpdGlvblN0cmluZyA9PT0gJ2JlZm9yZScpIHtcbiAgICAgIGlmICghYmVmb3JlW2dyb3VwXSkge1xuICAgICAgICBiZWZvcmVbZ3JvdXBdID0gW107XG4gICAgICB9XG4gICAgICBiZWZvcmVbZ3JvdXBdLnB1c2goaW5kZXgpO1xuICAgIH1cblxuICAgIHJldHVybiBPYmplY3QuYXNzaWduKHt9LCBwYXRoR3JvdXAsIHsgcG9zaXRpb24gfSk7XG4gIH0pO1xuXG4gIGxldCBtYXhQb3NpdGlvbiA9IDE7XG5cbiAgT2JqZWN0LmtleXMoYmVmb3JlKS5mb3JFYWNoKChncm91cCkgPT4ge1xuICAgIGNvbnN0IGdyb3VwTGVuZ3RoID0gYmVmb3JlW2dyb3VwXS5sZW5ndGg7XG4gICAgYmVmb3JlW2dyb3VwXS5mb3JFYWNoKChncm91cEluZGV4LCBpbmRleCkgPT4ge1xuICAgICAgdHJhbnNmb3JtZWRbZ3JvdXBJbmRleF0ucG9zaXRpb24gPSAtMSAqIChncm91cExlbmd0aCAtIGluZGV4KTtcbiAgICB9KTtcbiAgICBtYXhQb3NpdGlvbiA9IE1hdGgubWF4KG1heFBvc2l0aW9uLCBncm91cExlbmd0aCk7XG4gIH0pO1xuXG4gIE9iamVjdC5rZXlzKGFmdGVyKS5mb3JFYWNoKChrZXkpID0+IHtcbiAgICBjb25zdCBncm91cE5leHRQb3NpdGlvbiA9IGFmdGVyW2tleV07XG4gICAgbWF4UG9zaXRpb24gPSBNYXRoLm1heChtYXhQb3NpdGlvbiwgZ3JvdXBOZXh0UG9zaXRpb24gLSAxKTtcbiAgfSk7XG5cbiAgcmV0dXJuIHtcbiAgICBwYXRoR3JvdXBzOiB0cmFuc2Zvcm1lZCxcbiAgICBtYXhQb3NpdGlvbjogbWF4UG9zaXRpb24gPiAxMCA/IE1hdGgucG93KDEwLCBNYXRoLmNlaWwoTWF0aC5sb2cxMChtYXhQb3NpdGlvbikpKSA6IDEwLFxuICB9O1xufVxuXG5mdW5jdGlvbiBmaXhOZXdMaW5lQWZ0ZXJJbXBvcnQoY29udGV4dCwgcHJldmlvdXNJbXBvcnQpIHtcbiAgY29uc3QgcHJldlJvb3QgPSBmaW5kUm9vdE5vZGUocHJldmlvdXNJbXBvcnQubm9kZSk7XG4gIGNvbnN0IHRva2Vuc1RvRW5kT2ZMaW5lID0gdGFrZVRva2Vuc0FmdGVyV2hpbGUoXG4gICAgY29udGV4dC5nZXRTb3VyY2VDb2RlKCksIHByZXZSb290LCBjb21tZW50T25TYW1lTGluZUFzKHByZXZSb290KSk7XG5cbiAgbGV0IGVuZE9mTGluZSA9IHByZXZSb290LnJhbmdlWzFdO1xuICBpZiAodG9rZW5zVG9FbmRPZkxpbmUubGVuZ3RoID4gMCkge1xuICAgIGVuZE9mTGluZSA9IHRva2Vuc1RvRW5kT2ZMaW5lW3Rva2Vuc1RvRW5kT2ZMaW5lLmxlbmd0aCAtIDFdLnJhbmdlWzFdO1xuICB9XG4gIHJldHVybiAoZml4ZXIpID0+IGZpeGVyLmluc2VydFRleHRBZnRlclJhbmdlKFtwcmV2Um9vdC5yYW5nZVswXSwgZW5kT2ZMaW5lXSwgJ1xcbicpO1xufVxuXG5mdW5jdGlvbiByZW1vdmVOZXdMaW5lQWZ0ZXJJbXBvcnQoY29udGV4dCwgY3VycmVudEltcG9ydCwgcHJldmlvdXNJbXBvcnQpIHtcbiAgY29uc3Qgc291cmNlQ29kZSA9IGNvbnRleHQuZ2V0U291cmNlQ29kZSgpO1xuICBjb25zdCBwcmV2Um9vdCA9IGZpbmRSb290Tm9kZShwcmV2aW91c0ltcG9ydC5ub2RlKTtcbiAgY29uc3QgY3VyclJvb3QgPSBmaW5kUm9vdE5vZGUoY3VycmVudEltcG9ydC5ub2RlKTtcbiAgY29uc3QgcmFuZ2VUb1JlbW92ZSA9IFtcbiAgICBmaW5kRW5kT2ZMaW5lV2l0aENvbW1lbnRzKHNvdXJjZUNvZGUsIHByZXZSb290KSxcbiAgICBmaW5kU3RhcnRPZkxpbmVXaXRoQ29tbWVudHMoc291cmNlQ29kZSwgY3VyclJvb3QpLFxuICBdO1xuICBpZiAoL15cXHMqJC8udGVzdChzb3VyY2VDb2RlLnRleHQuc3Vic3RyaW5nKHJhbmdlVG9SZW1vdmVbMF0sIHJhbmdlVG9SZW1vdmVbMV0pKSkge1xuICAgIHJldHVybiAoZml4ZXIpID0+IGZpeGVyLnJlbW92ZVJhbmdlKHJhbmdlVG9SZW1vdmUpO1xuICB9XG4gIHJldHVybiB1bmRlZmluZWQ7XG59XG5cbmZ1bmN0aW9uIG1ha2VOZXdsaW5lc0JldHdlZW5SZXBvcnQoY29udGV4dCwgaW1wb3J0ZWQsIG5ld2xpbmVzQmV0d2VlbkltcG9ydHMpIHtcbiAgY29uc3QgZ2V0TnVtYmVyT2ZFbXB0eUxpbmVzQmV0d2VlbiA9IChjdXJyZW50SW1wb3J0LCBwcmV2aW91c0ltcG9ydCkgPT4ge1xuICAgIGNvbnN0IGxpbmVzQmV0d2VlbkltcG9ydHMgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKS5saW5lcy5zbGljZShcbiAgICAgIHByZXZpb3VzSW1wb3J0Lm5vZGUubG9jLmVuZC5saW5lLFxuICAgICAgY3VycmVudEltcG9ydC5ub2RlLmxvYy5zdGFydC5saW5lIC0gMSxcbiAgICApO1xuXG4gICAgcmV0dXJuIGxpbmVzQmV0d2VlbkltcG9ydHMuZmlsdGVyKChsaW5lKSA9PiAhbGluZS50cmltKCkubGVuZ3RoKS5sZW5ndGg7XG4gIH07XG4gIGxldCBwcmV2aW91c0ltcG9ydCA9IGltcG9ydGVkWzBdO1xuXG4gIGltcG9ydGVkLnNsaWNlKDEpLmZvckVhY2goZnVuY3Rpb24gKGN1cnJlbnRJbXBvcnQpIHtcbiAgICBjb25zdCBlbXB0eUxpbmVzQmV0d2VlbiA9IGdldE51bWJlck9mRW1wdHlMaW5lc0JldHdlZW4oY3VycmVudEltcG9ydCwgcHJldmlvdXNJbXBvcnQpO1xuXG4gICAgaWYgKG5ld2xpbmVzQmV0d2VlbkltcG9ydHMgPT09ICdhbHdheXMnXG4gICAgICAgIHx8IG5ld2xpbmVzQmV0d2VlbkltcG9ydHMgPT09ICdhbHdheXMtYW5kLWluc2lkZS1ncm91cHMnKSB7XG4gICAgICBpZiAoY3VycmVudEltcG9ydC5yYW5rICE9PSBwcmV2aW91c0ltcG9ydC5yYW5rICYmIGVtcHR5TGluZXNCZXR3ZWVuID09PSAwKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBwcmV2aW91c0ltcG9ydC5ub2RlLFxuICAgICAgICAgIG1lc3NhZ2U6ICdUaGVyZSBzaG91bGQgYmUgYXQgbGVhc3Qgb25lIGVtcHR5IGxpbmUgYmV0d2VlbiBpbXBvcnQgZ3JvdXBzJyxcbiAgICAgICAgICBmaXg6IGZpeE5ld0xpbmVBZnRlckltcG9ydChjb250ZXh0LCBwcmV2aW91c0ltcG9ydCksXG4gICAgICAgIH0pO1xuICAgICAgfSBlbHNlIGlmIChjdXJyZW50SW1wb3J0LnJhbmsgPT09IHByZXZpb3VzSW1wb3J0LnJhbmtcbiAgICAgICAgJiYgZW1wdHlMaW5lc0JldHdlZW4gPiAwXG4gICAgICAgICYmIG5ld2xpbmVzQmV0d2VlbkltcG9ydHMgIT09ICdhbHdheXMtYW5kLWluc2lkZS1ncm91cHMnKSB7XG4gICAgICAgIGNvbnRleHQucmVwb3J0KHtcbiAgICAgICAgICBub2RlOiBwcmV2aW91c0ltcG9ydC5ub2RlLFxuICAgICAgICAgIG1lc3NhZ2U6ICdUaGVyZSBzaG91bGQgYmUgbm8gZW1wdHkgbGluZSB3aXRoaW4gaW1wb3J0IGdyb3VwJyxcbiAgICAgICAgICBmaXg6IHJlbW92ZU5ld0xpbmVBZnRlckltcG9ydChjb250ZXh0LCBjdXJyZW50SW1wb3J0LCBwcmV2aW91c0ltcG9ydCksXG4gICAgICAgIH0pO1xuICAgICAgfVxuICAgIH0gZWxzZSBpZiAoZW1wdHlMaW5lc0JldHdlZW4gPiAwKSB7XG4gICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgIG5vZGU6IHByZXZpb3VzSW1wb3J0Lm5vZGUsXG4gICAgICAgIG1lc3NhZ2U6ICdUaGVyZSBzaG91bGQgYmUgbm8gZW1wdHkgbGluZSBiZXR3ZWVuIGltcG9ydCBncm91cHMnLFxuICAgICAgICBmaXg6IHJlbW92ZU5ld0xpbmVBZnRlckltcG9ydChjb250ZXh0LCBjdXJyZW50SW1wb3J0LCBwcmV2aW91c0ltcG9ydCksXG4gICAgICB9KTtcbiAgICB9XG5cbiAgICBwcmV2aW91c0ltcG9ydCA9IGN1cnJlbnRJbXBvcnQ7XG4gIH0pO1xufVxuXG5mdW5jdGlvbiBnZXRBbHBoYWJldGl6ZUNvbmZpZyhvcHRpb25zKSB7XG4gIGNvbnN0IGFscGhhYmV0aXplID0gb3B0aW9ucy5hbHBoYWJldGl6ZSB8fCB7fTtcbiAgY29uc3Qgb3JkZXIgPSBhbHBoYWJldGl6ZS5vcmRlciB8fCAnaWdub3JlJztcbiAgY29uc3QgY2FzZUluc2Vuc2l0aXZlID0gYWxwaGFiZXRpemUuY2FzZUluc2Vuc2l0aXZlIHx8IGZhbHNlO1xuXG4gIHJldHVybiB7IG9yZGVyLCBjYXNlSW5zZW5zaXRpdmUgfTtcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnc3VnZ2VzdGlvbicsXG4gICAgZG9jczoge1xuICAgICAgdXJsOiBkb2NzVXJsKCdvcmRlcicpLFxuICAgIH0sXG5cbiAgICBmaXhhYmxlOiAnY29kZScsXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICBwcm9wZXJ0aWVzOiB7XG4gICAgICAgICAgZ3JvdXBzOiB7XG4gICAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgIH0sXG4gICAgICAgICAgcGF0aEdyb3Vwc0V4Y2x1ZGVkSW1wb3J0VHlwZXM6IHtcbiAgICAgICAgICAgIHR5cGU6ICdhcnJheScsXG4gICAgICAgICAgfSxcbiAgICAgICAgICBwYXRoR3JvdXBzOiB7XG4gICAgICAgICAgICB0eXBlOiAnYXJyYXknLFxuICAgICAgICAgICAgaXRlbXM6IHtcbiAgICAgICAgICAgICAgdHlwZTogJ29iamVjdCcsXG4gICAgICAgICAgICAgIHByb3BlcnRpZXM6IHtcbiAgICAgICAgICAgICAgICBwYXR0ZXJuOiB7XG4gICAgICAgICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIHBhdHRlcm5PcHRpb25zOiB7XG4gICAgICAgICAgICAgICAgICB0eXBlOiAnb2JqZWN0JyxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICAgIGdyb3VwOiB7XG4gICAgICAgICAgICAgICAgICB0eXBlOiAnc3RyaW5nJyxcbiAgICAgICAgICAgICAgICAgIGVudW06IHR5cGVzLFxuICAgICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgICAgcG9zaXRpb246IHtcbiAgICAgICAgICAgICAgICAgIHR5cGU6ICdzdHJpbmcnLFxuICAgICAgICAgICAgICAgICAgZW51bTogWydhZnRlcicsICdiZWZvcmUnXSxcbiAgICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICB9LFxuICAgICAgICAgICAgICByZXF1aXJlZDogWydwYXR0ZXJuJywgJ2dyb3VwJ10sXG4gICAgICAgICAgICB9LFxuICAgICAgICAgIH0sXG4gICAgICAgICAgJ25ld2xpbmVzLWJldHdlZW4nOiB7XG4gICAgICAgICAgICBlbnVtOiBbXG4gICAgICAgICAgICAgICdpZ25vcmUnLFxuICAgICAgICAgICAgICAnYWx3YXlzJyxcbiAgICAgICAgICAgICAgJ2Fsd2F5cy1hbmQtaW5zaWRlLWdyb3VwcycsXG4gICAgICAgICAgICAgICduZXZlcicsXG4gICAgICAgICAgICBdLFxuICAgICAgICAgIH0sXG4gICAgICAgICAgYWxwaGFiZXRpemU6IHtcbiAgICAgICAgICAgIHR5cGU6ICdvYmplY3QnLFxuICAgICAgICAgICAgcHJvcGVydGllczoge1xuICAgICAgICAgICAgICBjYXNlSW5zZW5zaXRpdmU6IHtcbiAgICAgICAgICAgICAgICB0eXBlOiAnYm9vbGVhbicsXG4gICAgICAgICAgICAgICAgZGVmYXVsdDogZmFsc2UsXG4gICAgICAgICAgICAgIH0sXG4gICAgICAgICAgICAgIG9yZGVyOiB7XG4gICAgICAgICAgICAgICAgZW51bTogWydpZ25vcmUnLCAnYXNjJywgJ2Rlc2MnXSxcbiAgICAgICAgICAgICAgICBkZWZhdWx0OiAnaWdub3JlJyxcbiAgICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIH0sXG4gICAgICAgICAgICBhZGRpdGlvbmFsUHJvcGVydGllczogZmFsc2UsXG4gICAgICAgICAgfSxcbiAgICAgICAgICB3YXJuT25VbmFzc2lnbmVkSW1wb3J0czoge1xuICAgICAgICAgICAgdHlwZTogJ2Jvb2xlYW4nLFxuICAgICAgICAgICAgZGVmYXVsdDogZmFsc2UsXG4gICAgICAgICAgfSxcbiAgICAgICAgfSxcbiAgICAgICAgYWRkaXRpb25hbFByb3BlcnRpZXM6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxuXG4gIGNyZWF0ZTogZnVuY3Rpb24gaW1wb3J0T3JkZXJSdWxlKGNvbnRleHQpIHtcbiAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHt9O1xuICAgIGNvbnN0IG5ld2xpbmVzQmV0d2VlbkltcG9ydHMgPSBvcHRpb25zWyduZXdsaW5lcy1iZXR3ZWVuJ10gfHwgJ2lnbm9yZSc7XG4gICAgY29uc3QgcGF0aEdyb3Vwc0V4Y2x1ZGVkSW1wb3J0VHlwZXMgPSBuZXcgU2V0KG9wdGlvbnNbJ3BhdGhHcm91cHNFeGNsdWRlZEltcG9ydFR5cGVzJ10gfHwgWydidWlsdGluJywgJ2V4dGVybmFsJywgJ29iamVjdCddKTtcbiAgICBjb25zdCBhbHBoYWJldGl6ZSA9IGdldEFscGhhYmV0aXplQ29uZmlnKG9wdGlvbnMpO1xuICAgIGxldCByYW5rcztcblxuICAgIHRyeSB7XG4gICAgICBjb25zdCB7IHBhdGhHcm91cHMsIG1heFBvc2l0aW9uIH0gPSBjb252ZXJ0UGF0aEdyb3Vwc0ZvclJhbmtzKG9wdGlvbnMucGF0aEdyb3VwcyB8fCBbXSk7XG4gICAgICBjb25zdCB7IGdyb3Vwcywgb21pdHRlZFR5cGVzIH0gPSBjb252ZXJ0R3JvdXBzVG9SYW5rcyhvcHRpb25zLmdyb3VwcyB8fCBkZWZhdWx0R3JvdXBzKTtcbiAgICAgIHJhbmtzID0ge1xuICAgICAgICBncm91cHMsXG4gICAgICAgIG9taXR0ZWRUeXBlcyxcbiAgICAgICAgcGF0aEdyb3VwcyxcbiAgICAgICAgbWF4UG9zaXRpb24sXG4gICAgICB9O1xuICAgIH0gY2F0Y2ggKGVycm9yKSB7XG4gICAgICAvLyBNYWxmb3JtZWQgY29uZmlndXJhdGlvblxuICAgICAgcmV0dXJuIHtcbiAgICAgICAgUHJvZ3JhbShub2RlKSB7XG4gICAgICAgICAgY29udGV4dC5yZXBvcnQobm9kZSwgZXJyb3IubWVzc2FnZSk7XG4gICAgICAgIH0sXG4gICAgICB9O1xuICAgIH1cbiAgICBjb25zdCBpbXBvcnRNYXAgPSBuZXcgTWFwKCk7XG5cbiAgICBmdW5jdGlvbiBnZXRCbG9ja0ltcG9ydHMobm9kZSkge1xuICAgICAgaWYgKCFpbXBvcnRNYXAuaGFzKG5vZGUpKSB7XG4gICAgICAgIGltcG9ydE1hcC5zZXQobm9kZSwgW10pO1xuICAgICAgfVxuICAgICAgcmV0dXJuIGltcG9ydE1hcC5nZXQobm9kZSk7XG4gICAgfVxuXG4gICAgcmV0dXJuIHtcbiAgICAgIEltcG9ydERlY2xhcmF0aW9uOiBmdW5jdGlvbiBoYW5kbGVJbXBvcnRzKG5vZGUpIHtcbiAgICAgICAgLy8gSWdub3JpbmcgdW5hc3NpZ25lZCBpbXBvcnRzIHVubGVzcyB3YXJuT25VbmFzc2lnbmVkSW1wb3J0cyBpcyBzZXRcbiAgICAgICAgaWYgKG5vZGUuc3BlY2lmaWVycy5sZW5ndGggfHwgb3B0aW9ucy53YXJuT25VbmFzc2lnbmVkSW1wb3J0cykge1xuICAgICAgICAgIGNvbnN0IG5hbWUgPSBub2RlLnNvdXJjZS52YWx1ZTtcbiAgICAgICAgICByZWdpc3Rlck5vZGUoXG4gICAgICAgICAgICBjb250ZXh0LFxuICAgICAgICAgICAge1xuICAgICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgICB2YWx1ZTogbmFtZSxcbiAgICAgICAgICAgICAgZGlzcGxheU5hbWU6IG5hbWUsXG4gICAgICAgICAgICAgIHR5cGU6ICdpbXBvcnQnLFxuICAgICAgICAgICAgfSxcbiAgICAgICAgICAgIHJhbmtzLFxuICAgICAgICAgICAgZ2V0QmxvY2tJbXBvcnRzKG5vZGUucGFyZW50KSxcbiAgICAgICAgICAgIHBhdGhHcm91cHNFeGNsdWRlZEltcG9ydFR5cGVzLFxuICAgICAgICAgICk7XG4gICAgICAgIH1cbiAgICAgIH0sXG4gICAgICBUU0ltcG9ydEVxdWFsc0RlY2xhcmF0aW9uOiBmdW5jdGlvbiBoYW5kbGVJbXBvcnRzKG5vZGUpIHtcbiAgICAgICAgbGV0IGRpc3BsYXlOYW1lO1xuICAgICAgICBsZXQgdmFsdWU7XG4gICAgICAgIGxldCB0eXBlO1xuICAgICAgICAvLyBza2lwIFwiZXhwb3J0IGltcG9ydFwic1xuICAgICAgICBpZiAobm9kZS5pc0V4cG9ydCkge1xuICAgICAgICAgIHJldHVybjtcbiAgICAgICAgfVxuICAgICAgICBpZiAobm9kZS5tb2R1bGVSZWZlcmVuY2UudHlwZSA9PT0gJ1RTRXh0ZXJuYWxNb2R1bGVSZWZlcmVuY2UnKSB7XG4gICAgICAgICAgdmFsdWUgPSBub2RlLm1vZHVsZVJlZmVyZW5jZS5leHByZXNzaW9uLnZhbHVlO1xuICAgICAgICAgIGRpc3BsYXlOYW1lID0gdmFsdWU7XG4gICAgICAgICAgdHlwZSA9ICdpbXBvcnQnO1xuICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgIHZhbHVlID0gJyc7XG4gICAgICAgICAgZGlzcGxheU5hbWUgPSBjb250ZXh0LmdldFNvdXJjZUNvZGUoKS5nZXRUZXh0KG5vZGUubW9kdWxlUmVmZXJlbmNlKTtcbiAgICAgICAgICB0eXBlID0gJ2ltcG9ydDpvYmplY3QnO1xuICAgICAgICB9XG4gICAgICAgIHJlZ2lzdGVyTm9kZShcbiAgICAgICAgICBjb250ZXh0LFxuICAgICAgICAgIHtcbiAgICAgICAgICAgIG5vZGUsXG4gICAgICAgICAgICB2YWx1ZSxcbiAgICAgICAgICAgIGRpc3BsYXlOYW1lLFxuICAgICAgICAgICAgdHlwZSxcbiAgICAgICAgICB9LFxuICAgICAgICAgIHJhbmtzLFxuICAgICAgICAgIGdldEJsb2NrSW1wb3J0cyhub2RlLnBhcmVudCksXG4gICAgICAgICAgcGF0aEdyb3Vwc0V4Y2x1ZGVkSW1wb3J0VHlwZXMsXG4gICAgICAgICk7XG4gICAgICB9LFxuICAgICAgQ2FsbEV4cHJlc3Npb246IGZ1bmN0aW9uIGhhbmRsZVJlcXVpcmVzKG5vZGUpIHtcbiAgICAgICAgaWYgKCFpc1N0YXRpY1JlcXVpcmUobm9kZSkpIHtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgYmxvY2sgPSBnZXRSZXF1aXJlQmxvY2sobm9kZSk7XG4gICAgICAgIGlmICghYmxvY2spIHtcbiAgICAgICAgICByZXR1cm47XG4gICAgICAgIH1cbiAgICAgICAgY29uc3QgbmFtZSA9IG5vZGUuYXJndW1lbnRzWzBdLnZhbHVlO1xuICAgICAgICByZWdpc3Rlck5vZGUoXG4gICAgICAgICAgY29udGV4dCxcbiAgICAgICAgICB7XG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgdmFsdWU6IG5hbWUsXG4gICAgICAgICAgICBkaXNwbGF5TmFtZTogbmFtZSxcbiAgICAgICAgICAgIHR5cGU6ICdyZXF1aXJlJyxcbiAgICAgICAgICB9LFxuICAgICAgICAgIHJhbmtzLFxuICAgICAgICAgIGdldEJsb2NrSW1wb3J0cyhibG9jayksXG4gICAgICAgICAgcGF0aEdyb3Vwc0V4Y2x1ZGVkSW1wb3J0VHlwZXMsXG4gICAgICAgICk7XG4gICAgICB9LFxuICAgICAgJ1Byb2dyYW06ZXhpdCc6IGZ1bmN0aW9uIHJlcG9ydEFuZFJlc2V0KCkge1xuICAgICAgICBpbXBvcnRNYXAuZm9yRWFjaCgoaW1wb3J0ZWQpID0+IHtcbiAgICAgICAgICBpZiAobmV3bGluZXNCZXR3ZWVuSW1wb3J0cyAhPT0gJ2lnbm9yZScpIHtcbiAgICAgICAgICAgIG1ha2VOZXdsaW5lc0JldHdlZW5SZXBvcnQoY29udGV4dCwgaW1wb3J0ZWQsIG5ld2xpbmVzQmV0d2VlbkltcG9ydHMpO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmIChhbHBoYWJldGl6ZS5vcmRlciAhPT0gJ2lnbm9yZScpIHtcbiAgICAgICAgICAgIG11dGF0ZVJhbmtzVG9BbHBoYWJldGl6ZShpbXBvcnRlZCwgYWxwaGFiZXRpemUpO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIG1ha2VPdXRPZk9yZGVyUmVwb3J0KGNvbnRleHQsIGltcG9ydGVkKTtcbiAgICAgICAgfSk7XG5cbiAgICAgICAgaW1wb3J0TWFwLmNsZWFyKCk7XG4gICAgICB9LFxuICAgIH07XG4gIH0sXG59O1xuIl19