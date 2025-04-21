'use strict';

const pragmaUtil = require('./pragma');
const isDestructuredFromPragmaImport = require('./isDestructuredFromPragmaImport');

/**
 * Checks if the node is a createElement call
 * @param {ASTNode} node - The AST node being checked.
 * @param {Context} context - The AST node being checked.
 * @returns {Boolean} - True if node is a createElement call object literal, False if not.
*/
module.exports = function isCreateElement(node, context) {
  const pragma = pragmaUtil.getFromContext(context);
  if (
    node.callee
    && node.callee.type === 'MemberExpression'
    && node.callee.property.name === 'createElement'
    && node.callee.object
    && node.callee.object.name === pragma
  ) {
    return true;
  }

  if (
    node
    && node.callee
    && node.callee.name === 'createElement'
    && isDestructuredFromPragmaImport('createElement', context)
  ) {
    return true;
  }

  return false;
};
