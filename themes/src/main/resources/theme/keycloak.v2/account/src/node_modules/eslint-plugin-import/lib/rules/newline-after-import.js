'use strict';




var _staticRequire = require('../core/staticRequire');var _staticRequire2 = _interopRequireDefault(_staticRequire);
var _docsUrl = require('../docsUrl');var _docsUrl2 = _interopRequireDefault(_docsUrl);

var _debug = require('debug');var _debug2 = _interopRequireDefault(_debug);function _interopRequireDefault(obj) {return obj && obj.__esModule ? obj : { 'default': obj };}
var log = (0, _debug2['default'])('eslint-plugin-import:rules:newline-after-import');

//------------------------------------------------------------------------------
// Rule Definition
//------------------------------------------------------------------------------
/**
 * @fileoverview Rule to enforce new line after import not followed by another import.
 * @author Radek Benkel
 */function containsNodeOrEqual(outerNode, innerNode) {return outerNode.range[0] <= innerNode.range[0] && outerNode.range[1] >= innerNode.range[1];}

function getScopeBody(scope) {
  if (scope.block.type === 'SwitchStatement') {
    log('SwitchStatement scopes not supported');
    return null;
  }var

  body = scope.block.body;
  if (body && body.type === 'BlockStatement') {
    return body.body;
  }

  return body;
}

function findNodeIndexInScopeBody(body, nodeToFind) {
  return body.findIndex(function (node) {return containsNodeOrEqual(node, nodeToFind);});
}

function getLineDifference(node, nextNode) {
  return nextNode.loc.start.line - node.loc.end.line;
}

function isClassWithDecorator(node) {
  return node.type === 'ClassDeclaration' && node.decorators && node.decorators.length;
}

function isExportDefaultClass(node) {
  return node.type === 'ExportDefaultDeclaration' && node.declaration.type === 'ClassDeclaration';
}

function isExportNameClass(node) {

  return node.type === 'ExportNamedDeclaration' && node.declaration && node.declaration.type === 'ClassDeclaration';
}

module.exports = {
  meta: {
    type: 'layout',
    docs: {
      url: (0, _docsUrl2['default'])('newline-after-import') },

    fixable: 'whitespace',
    schema: [
    {
      'type': 'object',
      'properties': {
        'count': {
          'type': 'integer',
          'minimum': 1 } },


      'additionalProperties': false }] },



  create: function () {function create(context) {
      var level = 0;
      var requireCalls = [];

      function checkForNewLine(node, nextNode, type) {
        if (isExportDefaultClass(nextNode) || isExportNameClass(nextNode)) {
          var classNode = nextNode.declaration;

          if (isClassWithDecorator(classNode)) {
            nextNode = classNode.decorators[0];
          }
        } else if (isClassWithDecorator(nextNode)) {
          nextNode = nextNode.decorators[0];
        }

        var options = context.options[0] || { count: 1 };
        var lineDifference = getLineDifference(node, nextNode);
        var EXPECTED_LINE_DIFFERENCE = options.count + 1;

        if (lineDifference < EXPECTED_LINE_DIFFERENCE) {
          var column = node.loc.start.column;

          if (node.loc.start.line !== node.loc.end.line) {
            column = 0;
          }

          context.report({
            loc: {
              line: node.loc.end.line,
              column: column },

            message: 'Expected ' + String(options.count) + ' empty line' + (options.count > 1 ? 's' : '') + ' after ' + String(
            type) + ' statement not followed by another ' + String(type) + '.',
            fix: function () {function fix(fixer) {return fixer.insertTextAfter(
                node,
                '\n'.repeat(EXPECTED_LINE_DIFFERENCE - lineDifference));}return fix;}() });


        }
      }

      function incrementLevel() {
        level++;
      }
      function decrementLevel() {
        level--;
      }

      function checkImport(node) {var
        parent = node.parent;
        var nodePosition = parent.body.indexOf(node);
        var nextNode = parent.body[nodePosition + 1];

        // skip "export import"s
        if (node.type === 'TSImportEqualsDeclaration' && node.isExport) {
          return;
        }

        if (nextNode && nextNode.type !== 'ImportDeclaration' && (nextNode.type !== 'TSImportEqualsDeclaration' || nextNode.isExport)) {
          checkForNewLine(node, nextNode, 'import');
        }
      }

      return {
        ImportDeclaration: checkImport,
        TSImportEqualsDeclaration: checkImport,
        CallExpression: function () {function CallExpression(node) {
            if ((0, _staticRequire2['default'])(node) && level === 0) {
              requireCalls.push(node);
            }
          }return CallExpression;}(),
        'Program:exit': function () {function ProgramExit() {
            log('exit processing for', context.getPhysicalFilename ? context.getPhysicalFilename() : context.getFilename());
            var scopeBody = getScopeBody(context.getScope());
            log('got scope:', scopeBody);

            requireCalls.forEach(function (node, index) {
              var nodePosition = findNodeIndexInScopeBody(scopeBody, node);
              log('node position in scope:', nodePosition);

              var statementWithRequireCall = scopeBody[nodePosition];
              var nextStatement = scopeBody[nodePosition + 1];
              var nextRequireCall = requireCalls[index + 1];

              if (nextRequireCall && containsNodeOrEqual(statementWithRequireCall, nextRequireCall)) {
                return;
              }

              if (nextStatement && (
              !nextRequireCall || !containsNodeOrEqual(nextStatement, nextRequireCall))) {

                checkForNewLine(statementWithRequireCall, nextStatement, 'require');
              }
            });
          }return ProgramExit;}(),
        FunctionDeclaration: incrementLevel,
        FunctionExpression: incrementLevel,
        ArrowFunctionExpression: incrementLevel,
        BlockStatement: incrementLevel,
        ObjectExpression: incrementLevel,
        Decorator: incrementLevel,
        'FunctionDeclaration:exit': decrementLevel,
        'FunctionExpression:exit': decrementLevel,
        'ArrowFunctionExpression:exit': decrementLevel,
        'BlockStatement:exit': decrementLevel,
        'ObjectExpression:exit': decrementLevel,
        'Decorator:exit': decrementLevel };

    }return create;}() };
//# sourceMappingURL=data:application/json;charset=utf-8;base64,eyJ2ZXJzaW9uIjozLCJzb3VyY2VzIjpbIi4uLy4uL3NyYy9ydWxlcy9uZXdsaW5lLWFmdGVyLWltcG9ydC5qcyJdLCJuYW1lcyI6WyJsb2ciLCJjb250YWluc05vZGVPckVxdWFsIiwib3V0ZXJOb2RlIiwiaW5uZXJOb2RlIiwicmFuZ2UiLCJnZXRTY29wZUJvZHkiLCJzY29wZSIsImJsb2NrIiwidHlwZSIsImJvZHkiLCJmaW5kTm9kZUluZGV4SW5TY29wZUJvZHkiLCJub2RlVG9GaW5kIiwiZmluZEluZGV4Iiwibm9kZSIsImdldExpbmVEaWZmZXJlbmNlIiwibmV4dE5vZGUiLCJsb2MiLCJzdGFydCIsImxpbmUiLCJlbmQiLCJpc0NsYXNzV2l0aERlY29yYXRvciIsImRlY29yYXRvcnMiLCJsZW5ndGgiLCJpc0V4cG9ydERlZmF1bHRDbGFzcyIsImRlY2xhcmF0aW9uIiwiaXNFeHBvcnROYW1lQ2xhc3MiLCJtb2R1bGUiLCJleHBvcnRzIiwibWV0YSIsImRvY3MiLCJ1cmwiLCJmaXhhYmxlIiwic2NoZW1hIiwiY3JlYXRlIiwiY29udGV4dCIsImxldmVsIiwicmVxdWlyZUNhbGxzIiwiY2hlY2tGb3JOZXdMaW5lIiwiY2xhc3NOb2RlIiwib3B0aW9ucyIsImNvdW50IiwibGluZURpZmZlcmVuY2UiLCJFWFBFQ1RFRF9MSU5FX0RJRkZFUkVOQ0UiLCJjb2x1bW4iLCJyZXBvcnQiLCJtZXNzYWdlIiwiZml4IiwiZml4ZXIiLCJpbnNlcnRUZXh0QWZ0ZXIiLCJyZXBlYXQiLCJpbmNyZW1lbnRMZXZlbCIsImRlY3JlbWVudExldmVsIiwiY2hlY2tJbXBvcnQiLCJwYXJlbnQiLCJub2RlUG9zaXRpb24iLCJpbmRleE9mIiwiaXNFeHBvcnQiLCJJbXBvcnREZWNsYXJhdGlvbiIsIlRTSW1wb3J0RXF1YWxzRGVjbGFyYXRpb24iLCJDYWxsRXhwcmVzc2lvbiIsInB1c2giLCJnZXRQaHlzaWNhbEZpbGVuYW1lIiwiZ2V0RmlsZW5hbWUiLCJzY29wZUJvZHkiLCJnZXRTY29wZSIsImZvckVhY2giLCJpbmRleCIsInN0YXRlbWVudFdpdGhSZXF1aXJlQ2FsbCIsIm5leHRTdGF0ZW1lbnQiLCJuZXh0UmVxdWlyZUNhbGwiLCJGdW5jdGlvbkRlY2xhcmF0aW9uIiwiRnVuY3Rpb25FeHByZXNzaW9uIiwiQXJyb3dGdW5jdGlvbkV4cHJlc3Npb24iLCJCbG9ja1N0YXRlbWVudCIsIk9iamVjdEV4cHJlc3Npb24iLCJEZWNvcmF0b3IiXSwibWFwcGluZ3MiOiI7Ozs7O0FBS0Esc0Q7QUFDQSxxQzs7QUFFQSw4QjtBQUNBLElBQU1BLE1BQU0sd0JBQU0saURBQU4sQ0FBWjs7QUFFQTtBQUNBO0FBQ0E7QUFiQTs7O0dBZUEsU0FBU0MsbUJBQVQsQ0FBNkJDLFNBQTdCLEVBQXdDQyxTQUF4QyxFQUFtRCxDQUNqRCxPQUFPRCxVQUFVRSxLQUFWLENBQWdCLENBQWhCLEtBQXNCRCxVQUFVQyxLQUFWLENBQWdCLENBQWhCLENBQXRCLElBQTRDRixVQUFVRSxLQUFWLENBQWdCLENBQWhCLEtBQXNCRCxVQUFVQyxLQUFWLENBQWdCLENBQWhCLENBQXpFLENBQ0Q7O0FBRUQsU0FBU0MsWUFBVCxDQUFzQkMsS0FBdEIsRUFBNkI7QUFDM0IsTUFBSUEsTUFBTUMsS0FBTixDQUFZQyxJQUFaLEtBQXFCLGlCQUF6QixFQUE0QztBQUMxQ1IsUUFBSSxzQ0FBSjtBQUNBLFdBQU8sSUFBUDtBQUNELEdBSjBCOztBQU1uQlMsTUFObUIsR0FNVkgsTUFBTUMsS0FOSSxDQU1uQkUsSUFObUI7QUFPM0IsTUFBSUEsUUFBUUEsS0FBS0QsSUFBTCxLQUFjLGdCQUExQixFQUE0QztBQUMxQyxXQUFPQyxLQUFLQSxJQUFaO0FBQ0Q7O0FBRUQsU0FBT0EsSUFBUDtBQUNEOztBQUVELFNBQVNDLHdCQUFULENBQWtDRCxJQUFsQyxFQUF3Q0UsVUFBeEMsRUFBb0Q7QUFDbEQsU0FBT0YsS0FBS0csU0FBTCxDQUFlLFVBQUNDLElBQUQsVUFBVVosb0JBQW9CWSxJQUFwQixFQUEwQkYsVUFBMUIsQ0FBVixFQUFmLENBQVA7QUFDRDs7QUFFRCxTQUFTRyxp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Iiwic291cmNlc0NvbnRlbnQiOlsiLyoqXG4gKiBAZmlsZW92ZXJ2aWV3IFJ1bGUgdG8gZW5mb3JjZSBuZXcgbGluZSBhZnRlciBpbXBvcnQgbm90IGZvbGxvd2VkIGJ5IGFub3RoZXIgaW1wb3J0LlxuICogQGF1dGhvciBSYWRlayBCZW5rZWxcbiAqL1xuXG5pbXBvcnQgaXNTdGF0aWNSZXF1aXJlIGZyb20gJy4uL2NvcmUvc3RhdGljUmVxdWlyZSc7XG5pbXBvcnQgZG9jc1VybCBmcm9tICcuLi9kb2NzVXJsJztcblxuaW1wb3J0IGRlYnVnIGZyb20gJ2RlYnVnJztcbmNvbnN0IGxvZyA9IGRlYnVnKCdlc2xpbnQtcGx1Z2luLWltcG9ydDpydWxlczpuZXdsaW5lLWFmdGVyLWltcG9ydCcpO1xuXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuLy8gUnVsZSBEZWZpbml0aW9uXG4vLy0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLS0tLVxuXG5mdW5jdGlvbiBjb250YWluc05vZGVPckVxdWFsKG91dGVyTm9kZSwgaW5uZXJOb2RlKSB7XG4gIHJldHVybiBvdXRlck5vZGUucmFuZ2VbMF0gPD0gaW5uZXJOb2RlLnJhbmdlWzBdICYmIG91dGVyTm9kZS5yYW5nZVsxXSA+PSBpbm5lck5vZGUucmFuZ2VbMV07XG59XG5cbmZ1bmN0aW9uIGdldFNjb3BlQm9keShzY29wZSkge1xuICBpZiAoc2NvcGUuYmxvY2sudHlwZSA9PT0gJ1N3aXRjaFN0YXRlbWVudCcpIHtcbiAgICBsb2coJ1N3aXRjaFN0YXRlbWVudCBzY29wZXMgbm90IHN1cHBvcnRlZCcpO1xuICAgIHJldHVybiBudWxsO1xuICB9XG5cbiAgY29uc3QgeyBib2R5IH0gPSBzY29wZS5ibG9jaztcbiAgaWYgKGJvZHkgJiYgYm9keS50eXBlID09PSAnQmxvY2tTdGF0ZW1lbnQnKSB7XG4gICAgcmV0dXJuIGJvZHkuYm9keTtcbiAgfVxuXG4gIHJldHVybiBib2R5O1xufVxuXG5mdW5jdGlvbiBmaW5kTm9kZUluZGV4SW5TY29wZUJvZHkoYm9keSwgbm9kZVRvRmluZCkge1xuICByZXR1cm4gYm9keS5maW5kSW5kZXgoKG5vZGUpID0+IGNvbnRhaW5zTm9kZU9yRXF1YWwobm9kZSwgbm9kZVRvRmluZCkpO1xufVxuXG5mdW5jdGlvbiBnZXRMaW5lRGlmZmVyZW5jZShub2RlLCBuZXh0Tm9kZSkge1xuICByZXR1cm4gbmV4dE5vZGUubG9jLnN0YXJ0LmxpbmUgLSBub2RlLmxvYy5lbmQubGluZTtcbn1cblxuZnVuY3Rpb24gaXNDbGFzc1dpdGhEZWNvcmF0b3Iobm9kZSkge1xuICByZXR1cm4gbm9kZS50eXBlID09PSAnQ2xhc3NEZWNsYXJhdGlvbicgJiYgbm9kZS5kZWNvcmF0b3JzICYmIG5vZGUuZGVjb3JhdG9ycy5sZW5ndGg7XG59XG5cbmZ1bmN0aW9uIGlzRXhwb3J0RGVmYXVsdENsYXNzKG5vZGUpIHtcbiAgcmV0dXJuIG5vZGUudHlwZSA9PT0gJ0V4cG9ydERlZmF1bHREZWNsYXJhdGlvbicgJiYgbm9kZS5kZWNsYXJhdGlvbi50eXBlID09PSAnQ2xhc3NEZWNsYXJhdGlvbic7XG59XG5cbmZ1bmN0aW9uIGlzRXhwb3J0TmFtZUNsYXNzKG5vZGUpIHtcblxuICByZXR1cm4gbm9kZS50eXBlID09PSAnRXhwb3J0TmFtZWREZWNsYXJhdGlvbicgJiYgbm9kZS5kZWNsYXJhdGlvbiAmJiBub2RlLmRlY2xhcmF0aW9uLnR5cGUgPT09ICdDbGFzc0RlY2xhcmF0aW9uJztcbn1cblxubW9kdWxlLmV4cG9ydHMgPSB7XG4gIG1ldGE6IHtcbiAgICB0eXBlOiAnbGF5b3V0JyxcbiAgICBkb2NzOiB7XG4gICAgICB1cmw6IGRvY3NVcmwoJ25ld2xpbmUtYWZ0ZXItaW1wb3J0JyksXG4gICAgfSxcbiAgICBmaXhhYmxlOiAnd2hpdGVzcGFjZScsXG4gICAgc2NoZW1hOiBbXG4gICAgICB7XG4gICAgICAgICd0eXBlJzogJ29iamVjdCcsXG4gICAgICAgICdwcm9wZXJ0aWVzJzoge1xuICAgICAgICAgICdjb3VudCc6IHtcbiAgICAgICAgICAgICd0eXBlJzogJ2ludGVnZXInLFxuICAgICAgICAgICAgJ21pbmltdW0nOiAxLFxuICAgICAgICAgIH0sXG4gICAgICAgIH0sXG4gICAgICAgICdhZGRpdGlvbmFsUHJvcGVydGllcyc6IGZhbHNlLFxuICAgICAgfSxcbiAgICBdLFxuICB9LFxuICBjcmVhdGUoY29udGV4dCkge1xuICAgIGxldCBsZXZlbCA9IDA7XG4gICAgY29uc3QgcmVxdWlyZUNhbGxzID0gW107XG5cbiAgICBmdW5jdGlvbiBjaGVja0Zvck5ld0xpbmUobm9kZSwgbmV4dE5vZGUsIHR5cGUpIHtcbiAgICAgIGlmIChpc0V4cG9ydERlZmF1bHRDbGFzcyhuZXh0Tm9kZSkgfHwgaXNFeHBvcnROYW1lQ2xhc3MobmV4dE5vZGUpKSB7XG4gICAgICAgIGNvbnN0IGNsYXNzTm9kZSA9IG5leHROb2RlLmRlY2xhcmF0aW9uO1xuXG4gICAgICAgIGlmIChpc0NsYXNzV2l0aERlY29yYXRvcihjbGFzc05vZGUpKSB7XG4gICAgICAgICAgbmV4dE5vZGUgPSBjbGFzc05vZGUuZGVjb3JhdG9yc1swXTtcbiAgICAgICAgfVxuICAgICAgfSBlbHNlIGlmIChpc0NsYXNzV2l0aERlY29yYXRvcihuZXh0Tm9kZSkpIHtcbiAgICAgICAgbmV4dE5vZGUgPSBuZXh0Tm9kZS5kZWNvcmF0b3JzWzBdO1xuICAgICAgfVxuXG4gICAgICBjb25zdCBvcHRpb25zID0gY29udGV4dC5vcHRpb25zWzBdIHx8IHsgY291bnQ6IDEgfTtcbiAgICAgIGNvbnN0IGxpbmVEaWZmZXJlbmNlID0gZ2V0TGluZURpZmZlcmVuY2Uobm9kZSwgbmV4dE5vZGUpO1xuICAgICAgY29uc3QgRVhQRUNURURfTElORV9ESUZGRVJFTkNFID0gb3B0aW9ucy5jb3VudCArIDE7XG5cbiAgICAgIGlmIChsaW5lRGlmZmVyZW5jZSA8IEVYUEVDVEVEX0xJTkVfRElGRkVSRU5DRSkge1xuICAgICAgICBsZXQgY29sdW1uID0gbm9kZS5sb2Muc3RhcnQuY29sdW1uO1xuXG4gICAgICAgIGlmIChub2RlLmxvYy5zdGFydC5saW5lICE9PSBub2RlLmxvYy5lbmQubGluZSkge1xuICAgICAgICAgIGNvbHVtbiA9IDA7XG4gICAgICAgIH1cblxuICAgICAgICBjb250ZXh0LnJlcG9ydCh7XG4gICAgICAgICAgbG9jOiB7XG4gICAgICAgICAgICBsaW5lOiBub2RlLmxvYy5lbmQubGluZSxcbiAgICAgICAgICAgIGNvbHVtbixcbiAgICAgICAgICB9LFxuICAgICAgICAgIG1lc3NhZ2U6IGBFeHBlY3RlZCAke29wdGlvbnMuY291bnR9IGVtcHR5IGxpbmUke29wdGlvbnMuY291bnQgPiAxID8gJ3MnIDogJyd9IFxcXG5hZnRlciAke3R5cGV9IHN0YXRlbWVudCBub3QgZm9sbG93ZWQgYnkgYW5vdGhlciAke3R5cGV9LmAsXG4gICAgICAgICAgZml4OiBmaXhlciA9PiBmaXhlci5pbnNlcnRUZXh0QWZ0ZXIoXG4gICAgICAgICAgICBub2RlLFxuICAgICAgICAgICAgJ1xcbicucmVwZWF0KEVYUEVDVEVEX0xJTkVfRElGRkVSRU5DRSAtIGxpbmVEaWZmZXJlbmNlKSxcbiAgICAgICAgICApLFxuICAgICAgICB9KTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICBmdW5jdGlvbiBpbmNyZW1lbnRMZXZlbCgpIHtcbiAgICAgIGxldmVsKys7XG4gICAgfVxuICAgIGZ1bmN0aW9uIGRlY3JlbWVudExldmVsKCkge1xuICAgICAgbGV2ZWwtLTtcbiAgICB9XG5cbiAgICBmdW5jdGlvbiBjaGVja0ltcG9ydChub2RlKSB7XG4gICAgICBjb25zdCB7IHBhcmVudCB9ID0gbm9kZTtcbiAgICAgIGNvbnN0IG5vZGVQb3NpdGlvbiA9IHBhcmVudC5ib2R5LmluZGV4T2Yobm9kZSk7XG4gICAgICBjb25zdCBuZXh0Tm9kZSA9IHBhcmVudC5ib2R5W25vZGVQb3NpdGlvbiArIDFdO1xuXG4gICAgICAvLyBza2lwIFwiZXhwb3J0IGltcG9ydFwic1xuICAgICAgaWYgKG5vZGUudHlwZSA9PT0gJ1RTSW1wb3J0RXF1YWxzRGVjbGFyYXRpb24nICYmIG5vZGUuaXNFeHBvcnQpIHtcbiAgICAgICAgcmV0dXJuO1xuICAgICAgfVxuXG4gICAgICBpZiAobmV4dE5vZGUgJiYgbmV4dE5vZGUudHlwZSAhPT0gJ0ltcG9ydERlY2xhcmF0aW9uJyAmJiAobmV4dE5vZGUudHlwZSAhPT0gJ1RTSW1wb3J0RXF1YWxzRGVjbGFyYXRpb24nIHx8IG5leHROb2RlLmlzRXhwb3J0KSkge1xuICAgICAgICBjaGVja0Zvck5ld0xpbmUobm9kZSwgbmV4dE5vZGUsICdpbXBvcnQnKTtcbiAgICAgIH1cbiAgICB9XG5cbiAgICByZXR1cm4ge1xuICAgICAgSW1wb3J0RGVjbGFyYXRpb246IGNoZWNrSW1wb3J0LFxuICAgICAgVFNJbXBvcnRFcXVhbHNEZWNsYXJhdGlvbjogY2hlY2tJbXBvcnQsXG4gICAgICBDYWxsRXhwcmVzc2lvbihub2RlKSB7XG4gICAgICAgIGlmIChpc1N0YXRpY1JlcXVpcmUobm9kZSkgJiYgbGV2ZWwgPT09IDApIHtcbiAgICAgICAgICByZXF1aXJlQ2FsbHMucHVzaChub2RlKTtcbiAgICAgICAgfVxuICAgICAgfSxcbiAgICAgICdQcm9ncmFtOmV4aXQnOiBmdW5jdGlvbiAoKSB7XG4gICAgICAgIGxvZygnZXhpdCBwcm9jZXNzaW5nIGZvcicsIGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSA/IGNvbnRleHQuZ2V0UGh5c2ljYWxGaWxlbmFtZSgpIDogY29udGV4dC5nZXRGaWxlbmFtZSgpKTtcbiAgICAgICAgY29uc3Qgc2NvcGVCb2R5ID0gZ2V0U2NvcGVCb2R5KGNvbnRleHQuZ2V0U2NvcGUoKSk7XG4gICAgICAgIGxvZygnZ290IHNjb3BlOicsIHNjb3BlQm9keSk7XG5cbiAgICAgICAgcmVxdWlyZUNhbGxzLmZvckVhY2goZnVuY3Rpb24gKG5vZGUsIGluZGV4KSB7XG4gICAgICAgICAgY29uc3Qgbm9kZVBvc2l0aW9uID0gZmluZE5vZGVJbmRleEluU2NvcGVCb2R5KHNjb3BlQm9keSwgbm9kZSk7XG4gICAgICAgICAgbG9nKCdub2RlIHBvc2l0aW9uIGluIHNjb3BlOicsIG5vZGVQb3NpdGlvbik7XG5cbiAgICAgICAgICBjb25zdCBzdGF0ZW1lbnRXaXRoUmVxdWlyZUNhbGwgPSBzY29wZUJvZHlbbm9kZVBvc2l0aW9uXTtcbiAgICAgICAgICBjb25zdCBuZXh0U3RhdGVtZW50ID0gc2NvcGVCb2R5W25vZGVQb3NpdGlvbiArIDFdO1xuICAgICAgICAgIGNvbnN0IG5leHRSZXF1aXJlQ2FsbCA9IHJlcXVpcmVDYWxsc1tpbmRleCArIDFdO1xuXG4gICAgICAgICAgaWYgKG5leHRSZXF1aXJlQ2FsbCAmJiBjb250YWluc05vZGVPckVxdWFsKHN0YXRlbWVudFdpdGhSZXF1aXJlQ2FsbCwgbmV4dFJlcXVpcmVDYWxsKSkge1xuICAgICAgICAgICAgcmV0dXJuO1xuICAgICAgICAgIH1cblxuICAgICAgICAgIGlmIChuZXh0U3RhdGVtZW50ICYmXG4gICAgICAgICAgICAgKCFuZXh0UmVxdWlyZUNhbGwgfHwgIWNvbnRhaW5zTm9kZU9yRXF1YWwobmV4dFN0YXRlbWVudCwgbmV4dFJlcXVpcmVDYWxsKSkpIHtcblxuICAgICAgICAgICAgY2hlY2tGb3JOZXdMaW5lKHN0YXRlbWVudFdpdGhSZXF1aXJlQ2FsbCwgbmV4dFN0YXRlbWVudCwgJ3JlcXVpcmUnKTtcbiAgICAgICAgICB9XG4gICAgICAgIH0pO1xuICAgICAgfSxcbiAgICAgIEZ1bmN0aW9uRGVjbGFyYXRpb246IGluY3JlbWVudExldmVsLFxuICAgICAgRnVuY3Rpb25FeHByZXNzaW9uOiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgIEFycm93RnVuY3Rpb25FeHByZXNzaW9uOiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgIEJsb2NrU3RhdGVtZW50OiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgIE9iamVjdEV4cHJlc3Npb246IGluY3JlbWVudExldmVsLFxuICAgICAgRGVjb3JhdG9yOiBpbmNyZW1lbnRMZXZlbCxcbiAgICAgICdGdW5jdGlvbkRlY2xhcmF0aW9uOmV4aXQnOiBkZWNyZW1lbnRMZXZlbCxcbiAgICAgICdGdW5jdGlvbkV4cHJlc3Npb246ZXhpdCc6IGRlY3JlbWVudExldmVsLFxuICAgICAgJ0Fycm93RnVuY3Rpb25FeHByZXNzaW9uOmV4aXQnOiBkZWNyZW1lbnRMZXZlbCxcbiAgICAgICdCbG9ja1N0YXRlbWVudDpleGl0JzogZGVjcmVtZW50TGV2ZWwsXG4gICAgICAnT2JqZWN0RXhwcmVzc2lvbjpleGl0JzogZGVjcmVtZW50TGV2ZWwsXG4gICAgICAnRGVjb3JhdG9yOmV4aXQnOiBkZWNyZW1lbnRMZXZlbCxcbiAgICB9O1xuICB9LFxufTtcbiJdfQ==