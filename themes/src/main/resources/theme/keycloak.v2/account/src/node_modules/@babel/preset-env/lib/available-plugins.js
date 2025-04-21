"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.minVersions = exports.default = void 0;

var _pluginSyntaxAsyncGenerators = require("@babel/plugin-syntax-async-generators");

var _pluginSyntaxClassProperties = require("@babel/plugin-syntax-class-properties");

var _pluginSyntaxClassStaticBlock = require("@babel/plugin-syntax-class-static-block");

var _pluginSyntaxDynamicImport = require("@babel/plugin-syntax-dynamic-import");

var _pluginSyntaxExportNamespaceFrom = require("@babel/plugin-syntax-export-namespace-from");

var _pluginSyntaxImportAssertions = require("@babel/plugin-syntax-import-assertions");

var _pluginSyntaxJsonStrings = require("@babel/plugin-syntax-json-strings");

var _pluginSyntaxLogicalAssignmentOperators = require("@babel/plugin-syntax-logical-assignment-operators");

var _pluginSyntaxNullishCoalescingOperator = require("@babel/plugin-syntax-nullish-coalescing-operator");

var _pluginSyntaxNumericSeparator = require("@babel/plugin-syntax-numeric-separator");

var _pluginSyntaxObjectRestSpread = require("@babel/plugin-syntax-object-rest-spread");

var _pluginSyntaxOptionalCatchBinding = require("@babel/plugin-syntax-optional-catch-binding");

var _pluginSyntaxOptionalChaining = require("@babel/plugin-syntax-optional-chaining");

var _pluginSyntaxPrivatePropertyInObject = require("@babel/plugin-syntax-private-property-in-object");

var _pluginSyntaxTopLevelAwait = require("@babel/plugin-syntax-top-level-await");

var _pluginProposalAsyncGeneratorFunctions = require("@babel/plugin-proposal-async-generator-functions");

var _pluginProposalClassProperties = require("@babel/plugin-proposal-class-properties");

var _pluginProposalClassStaticBlock = require("@babel/plugin-proposal-class-static-block");

var _pluginProposalDynamicImport = require("@babel/plugin-proposal-dynamic-import");

var _pluginProposalExportNamespaceFrom = require("@babel/plugin-proposal-export-namespace-from");

var _pluginProposalJsonStrings = require("@babel/plugin-proposal-json-strings");

var _pluginProposalLogicalAssignmentOperators = require("@babel/plugin-proposal-logical-assignment-operators");

var _pluginProposalNullishCoalescingOperator = require("@babel/plugin-proposal-nullish-coalescing-operator");

var _pluginProposalNumericSeparator = require("@babel/plugin-proposal-numeric-separator");

var _pluginProposalObjectRestSpread = require("@babel/plugin-proposal-object-rest-spread");

var _pluginProposalOptionalCatchBinding = require("@babel/plugin-proposal-optional-catch-binding");

var _pluginProposalOptionalChaining = require("@babel/plugin-proposal-optional-chaining");

var _pluginProposalPrivateMethods = require("@babel/plugin-proposal-private-methods");

var _pluginProposalPrivatePropertyInObject = require("@babel/plugin-proposal-private-property-in-object");

var _pluginProposalUnicodePropertyRegex = require("@babel/plugin-proposal-unicode-property-regex");

var _pluginTransformAsyncToGenerator = require("@babel/plugin-transform-async-to-generator");

var _pluginTransformArrowFunctions = require("@babel/plugin-transform-arrow-functions");

var _pluginTransformBlockScopedFunctions = require("@babel/plugin-transform-block-scoped-functions");

var _pluginTransformBlockScoping = require("@babel/plugin-transform-block-scoping");

var _pluginTransformClasses = require("@babel/plugin-transform-classes");

var _pluginTransformComputedProperties = require("@babel/plugin-transform-computed-properties");

var _pluginTransformDestructuring = require("@babel/plugin-transform-destructuring");

var _pluginTransformDotallRegex = require("@babel/plugin-transform-dotall-regex");

var _pluginTransformDuplicateKeys = require("@babel/plugin-transform-duplicate-keys");

var _pluginTransformExponentiationOperator = require("@babel/plugin-transform-exponentiation-operator");

var _pluginTransformForOf = require("@babel/plugin-transform-for-of");

var _pluginTransformFunctionName = require("@babel/plugin-transform-function-name");

var _pluginTransformLiterals = require("@babel/plugin-transform-literals");

var _pluginTransformMemberExpressionLiterals = require("@babel/plugin-transform-member-expression-literals");

var _pluginTransformModulesAmd = require("@babel/plugin-transform-modules-amd");

var _pluginTransformModulesCommonjs = require("@babel/plugin-transform-modules-commonjs");

var _pluginTransformModulesSystemjs = require("@babel/plugin-transform-modules-systemjs");

var _pluginTransformModulesUmd = require("@babel/plugin-transform-modules-umd");

var _pluginTransformNamedCapturingGroupsRegex = require("@babel/plugin-transform-named-capturing-groups-regex");

var _pluginTransformNewTarget = require("@babel/plugin-transform-new-target");

var _pluginTransformObjectSuper = require("@babel/plugin-transform-object-super");

var _pluginTransformParameters = require("@babel/plugin-transform-parameters");

var _pluginTransformPropertyLiterals = require("@babel/plugin-transform-property-literals");

var _pluginTransformRegenerator = require("@babel/plugin-transform-regenerator");

var _pluginTransformReservedWords = require("@babel/plugin-transform-reserved-words");

var _pluginTransformShorthandProperties = require("@babel/plugin-transform-shorthand-properties");

var _pluginTransformSpread = require("@babel/plugin-transform-spread");

var _pluginTransformStickyRegex = require("@babel/plugin-transform-sticky-regex");

var _pluginTransformTemplateLiterals = require("@babel/plugin-transform-template-literals");

var _pluginTransformTypeofSymbol = require("@babel/plugin-transform-typeof-symbol");

var _pluginTransformUnicodeEscapes = require("@babel/plugin-transform-unicode-escapes");

var _pluginTransformUnicodeRegex = require("@babel/plugin-transform-unicode-regex");

var _transformAsyncArrowsInClass = require("@babel/preset-modules/lib/plugins/transform-async-arrows-in-class");

var _transformEdgeDefaultParameters = require("@babel/preset-modules/lib/plugins/transform-edge-default-parameters");

var _transformEdgeFunctionName = require("@babel/preset-modules/lib/plugins/transform-edge-function-name");

var _transformTaggedTemplateCaching = require("@babel/preset-modules/lib/plugins/transform-tagged-template-caching");

var _transformSafariBlockShadowing = require("@babel/preset-modules/lib/plugins/transform-safari-block-shadowing");

var _transformSafariForShadowing = require("@babel/preset-modules/lib/plugins/transform-safari-for-shadowing");

var _pluginBugfixSafariIdDestructuringCollisionInFunctionExpression = require("@babel/plugin-bugfix-safari-id-destructuring-collision-in-function-expression");

var _pluginBugfixV8SpreadParametersInOptionalChaining = require("@babel/plugin-bugfix-v8-spread-parameters-in-optional-chaining");

var _default = {
  "bugfix/transform-async-arrows-in-class": () => _transformAsyncArrowsInClass,
  "bugfix/transform-edge-default-parameters": () => _transformEdgeDefaultParameters,
  "bugfix/transform-edge-function-name": () => _transformEdgeFunctionName,
  "bugfix/transform-safari-block-shadowing": () => _transformSafariBlockShadowing,
  "bugfix/transform-safari-for-shadowing": () => _transformSafariForShadowing,
  "bugfix/transform-safari-id-destructuring-collision-in-function-expression": () => _pluginBugfixSafariIdDestructuringCollisionInFunctionExpression.default,
  "bugfix/transform-tagged-template-caching": () => _transformTaggedTemplateCaching,
  "bugfix/transform-v8-spread-parameters-in-optional-chaining": () => _pluginBugfixV8SpreadParametersInOptionalChaining.default,
  "proposal-async-generator-functions": () => _pluginProposalAsyncGeneratorFunctions.default,
  "proposal-class-properties": () => _pluginProposalClassProperties.default,
  "proposal-class-static-block": () => _pluginProposalClassStaticBlock.default,
  "proposal-dynamic-import": () => _pluginProposalDynamicImport.default,
  "proposal-export-namespace-from": () => _pluginProposalExportNamespaceFrom.default,
  "proposal-json-strings": () => _pluginProposalJsonStrings.default,
  "proposal-logical-assignment-operators": () => _pluginProposalLogicalAssignmentOperators.default,
  "proposal-nullish-coalescing-operator": () => _pluginProposalNullishCoalescingOperator.default,
  "proposal-numeric-separator": () => _pluginProposalNumericSeparator.default,
  "proposal-object-rest-spread": () => _pluginProposalObjectRestSpread.default,
  "proposal-optional-catch-binding": () => _pluginProposalOptionalCatchBinding.default,
  "proposal-optional-chaining": () => _pluginProposalOptionalChaining.default,
  "proposal-private-methods": () => _pluginProposalPrivateMethods.default,
  "proposal-private-property-in-object": () => _pluginProposalPrivatePropertyInObject.default,
  "proposal-unicode-property-regex": () => _pluginProposalUnicodePropertyRegex.default,
  "syntax-async-generators": () => _pluginSyntaxAsyncGenerators,
  "syntax-class-properties": () => _pluginSyntaxClassProperties,
  "syntax-class-static-block": () => _pluginSyntaxClassStaticBlock,
  "syntax-dynamic-import": () => _pluginSyntaxDynamicImport,
  "syntax-export-namespace-from": () => _pluginSyntaxExportNamespaceFrom,
  "syntax-import-assertions": () => _pluginSyntaxImportAssertions.default,
  "syntax-json-strings": () => _pluginSyntaxJsonStrings,
  "syntax-logical-assignment-operators": () => _pluginSyntaxLogicalAssignmentOperators,
  "syntax-nullish-coalescing-operator": () => _pluginSyntaxNullishCoalescingOperator,
  "syntax-numeric-separator": () => _pluginSyntaxNumericSeparator,
  "syntax-object-rest-spread": () => _pluginSyntaxObjectRestSpread,
  "syntax-optional-catch-binding": () => _pluginSyntaxOptionalCatchBinding,
  "syntax-optional-chaining": () => _pluginSyntaxOptionalChaining,
  "syntax-private-property-in-object": () => _pluginSyntaxPrivatePropertyInObject,
  "syntax-top-level-await": () => _pluginSyntaxTopLevelAwait,
  "transform-arrow-functions": () => _pluginTransformArrowFunctions.default,
  "transform-async-to-generator": () => _pluginTransformAsyncToGenerator.default,
  "transform-block-scoped-functions": () => _pluginTransformBlockScopedFunctions.default,
  "transform-block-scoping": () => _pluginTransformBlockScoping.default,
  "transform-classes": () => _pluginTransformClasses.default,
  "transform-computed-properties": () => _pluginTransformComputedProperties.default,
  "transform-destructuring": () => _pluginTransformDestructuring.default,
  "transform-dotall-regex": () => _pluginTransformDotallRegex.default,
  "transform-duplicate-keys": () => _pluginTransformDuplicateKeys.default,
  "transform-exponentiation-operator": () => _pluginTransformExponentiationOperator.default,
  "transform-for-of": () => _pluginTransformForOf.default,
  "transform-function-name": () => _pluginTransformFunctionName.default,
  "transform-literals": () => _pluginTransformLiterals.default,
  "transform-member-expression-literals": () => _pluginTransformMemberExpressionLiterals.default,
  "transform-modules-amd": () => _pluginTransformModulesAmd.default,
  "transform-modules-commonjs": () => _pluginTransformModulesCommonjs.default,
  "transform-modules-systemjs": () => _pluginTransformModulesSystemjs.default,
  "transform-modules-umd": () => _pluginTransformModulesUmd.default,
  "transform-named-capturing-groups-regex": () => _pluginTransformNamedCapturingGroupsRegex.default,
  "transform-new-target": () => _pluginTransformNewTarget.default,
  "transform-object-super": () => _pluginTransformObjectSuper.default,
  "transform-parameters": () => _pluginTransformParameters.default,
  "transform-property-literals": () => _pluginTransformPropertyLiterals.default,
  "transform-regenerator": () => _pluginTransformRegenerator.default,
  "transform-reserved-words": () => _pluginTransformReservedWords.default,
  "transform-shorthand-properties": () => _pluginTransformShorthandProperties.default,
  "transform-spread": () => _pluginTransformSpread.default,
  "transform-sticky-regex": () => _pluginTransformStickyRegex.default,
  "transform-template-literals": () => _pluginTransformTemplateLiterals.default,
  "transform-typeof-symbol": () => _pluginTransformTypeofSymbol.default,
  "transform-unicode-escapes": () => _pluginTransformUnicodeEscapes.default,
  "transform-unicode-regex": () => _pluginTransformUnicodeRegex.default
};
exports.default = _default;
const minVersions = {
  "bugfix/transform-safari-id-destructuring-collision-in-function-expression": "7.16.0",
  "proposal-class-static-block": "7.12.0",
  "proposal-private-property-in-object": "7.10.0"
};
exports.minVersions = minVersions;