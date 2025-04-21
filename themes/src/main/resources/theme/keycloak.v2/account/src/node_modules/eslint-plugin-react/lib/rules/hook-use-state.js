/**
 * @fileoverview Ensure symmetric naming of useState hook value and setter variables
 * @author Duncan Beevers
 */

'use strict';

const Components = require('../util/Components');
const docsUrl = require('../util/docsUrl');
const report = require('../util/report');

// ------------------------------------------------------------------------------
// Rule Definition
// ------------------------------------------------------------------------------

const messages = {
  useStateErrorMessage: 'useState call is not destructured into value + setter pair',
};

module.exports = {
  meta: {
    docs: {
      description: 'Ensure symmetric naming of useState hook value and setter variables',
      category: 'Best Practices',
      recommended: false,
      url: docsUrl('hook-use-state'),
    },
    messages,
    schema: [],
    type: 'suggestion',
    hasSuggestions: true,
  },

  create: Components.detect((context, components, util) => ({
    CallExpression(node) {
      const isImmediateReturn = node.parent
        && node.parent.type === 'ReturnStatement';

      if (isImmediateReturn || !util.isReactHookCall(node, ['useState'])) {
        return;
      }

      const isDestructuringDeclarator = node.parent
        && node.parent.type === 'VariableDeclarator'
        && node.parent.id.type === 'ArrayPattern';

      if (!isDestructuringDeclarator) {
        report(
          context,
          messages.useStateErrorMessage,
          'useStateErrorMessage',
          { node }
        );
        return;
      }

      const variableNodes = node.parent.id.elements;
      const valueVariable = variableNodes[0];
      const setterVariable = variableNodes[1];

      const valueVariableName = valueVariable
        ? valueVariable.name
        : undefined;

      const setterVariableName = setterVariable
        ? setterVariable.name
        : undefined;

      const caseCandidateMatch = valueVariableName ? valueVariableName.match(/(^[a-z]+)(.*)/) : undefined;
      const upperCaseCandidatePrefix = caseCandidateMatch ? caseCandidateMatch[1] : undefined;
      const caseCandidateSuffix = caseCandidateMatch ? caseCandidateMatch[2] : undefined;
      const expectedSetterVariableNames = upperCaseCandidatePrefix ? [
        `set${upperCaseCandidatePrefix.charAt(0).toUpperCase()}${upperCaseCandidatePrefix.slice(1)}${caseCandidateSuffix}`,
        `set${upperCaseCandidatePrefix.toUpperCase()}${caseCandidateSuffix}`,
      ] : [];

      const isSymmetricGetterSetterPair = valueVariable
        && setterVariable
        && expectedSetterVariableNames.indexOf(setterVariableName) !== -1
        && variableNodes.length === 2;

      if (!isSymmetricGetterSetterPair) {
        const suggestions = [
          {
            desc: 'Destructure useState call into value + setter pair',
            fix: (fixer) => {
              if (expectedSetterVariableNames.length === 0) {
                return;
              }

              const fix = fixer.replaceTextRange(
                node.parent.id.range,
                `[${valueVariableName}, ${expectedSetterVariableNames[0]}]`
              );

              return fix;
            },
          },
        ];

        const defaultReactImports = components.getDefaultReactImports();
        const defaultReactImportSpecifier = defaultReactImports
          ? defaultReactImports[0]
          : undefined;

        const defaultReactImportName = defaultReactImportSpecifier
          ? defaultReactImportSpecifier.local.name
          : undefined;

        const namedReactImports = components.getNamedReactImports();
        const useStateReactImportSpecifier = namedReactImports
          ? namedReactImports.find((specifier) => specifier.imported.name === 'useState')
          : undefined;

        const isSingleGetter = valueVariable && variableNodes.length === 1;
        const isUseStateCalledWithSingleArgument = node.arguments.length === 1;
        if (isSingleGetter && isUseStateCalledWithSingleArgument) {
          const useMemoReactImportSpecifier = namedReactImports
            && namedReactImports.find((specifier) => specifier.imported.name === 'useMemo');

          let useMemoCode;
          if (useMemoReactImportSpecifier) {
            useMemoCode = useMemoReactImportSpecifier.local.name;
          } else if (defaultReactImportName) {
            useMemoCode = `${defaultReactImportName}.useMemo`;
          } else {
            useMemoCode = 'useMemo';
          }

          suggestions.unshift({
            desc: 'Replace useState call with useMemo',
            fix: (fixer) => [
              // Add useMemo import, if necessary
              useStateReactImportSpecifier
                  && (!useMemoReactImportSpecifier || defaultReactImportName)
                  && fixer.insertTextAfter(useStateReactImportSpecifier, ', useMemo'),
              // Convert single-value destructure to simple assignment
              fixer.replaceTextRange(node.parent.id.range, valueVariableName),
              // Convert useState call to useMemo + arrow function + dependency array
              fixer.replaceTextRange(
                node.range,
                `${useMemoCode}(() => ${context.getSourceCode().getText(node.arguments[0])}, [])`
              ),
            ].filter(Boolean),
          });
        }

        report(
          context,
          messages.useStateErrorMessage,
          'useStateErrorMessage',
          {
            node: node.parent.id,
            suggest: suggestions,
          }
        );
      }
    },
  })),
};
