"use strict";
/**
 * Note this file is rather type-unsafe in its current state.
 * This is due to some really funky type conversions between different node types.
 * This is done intentionally based on the internal implementation of the base indent rule.
 */
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const indent_1 = __importDefault(require("eslint/lib/rules/indent"));
const util = __importStar(require("../util"));
const KNOWN_NODES = new Set([
    // Class properties aren't yet supported by eslint...
    typescript_estree_1.AST_NODE_TYPES.ClassProperty,
    // ts keywords
    typescript_estree_1.AST_NODE_TYPES.TSAbstractKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSAnyKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSBooleanKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSNeverKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSNumberKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSStringKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSSymbolKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSUndefinedKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSUnknownKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSVoidKeyword,
    typescript_estree_1.AST_NODE_TYPES.TSNullKeyword,
    // ts specific nodes we want to support
    typescript_estree_1.AST_NODE_TYPES.TSAbstractClassProperty,
    typescript_estree_1.AST_NODE_TYPES.TSAbstractMethodDefinition,
    typescript_estree_1.AST_NODE_TYPES.TSArrayType,
    typescript_estree_1.AST_NODE_TYPES.TSAsExpression,
    typescript_estree_1.AST_NODE_TYPES.TSCallSignatureDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSConditionalType,
    typescript_estree_1.AST_NODE_TYPES.TSConstructorType,
    typescript_estree_1.AST_NODE_TYPES.TSConstructSignatureDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSDeclareFunction,
    typescript_estree_1.AST_NODE_TYPES.TSEmptyBodyFunctionExpression,
    typescript_estree_1.AST_NODE_TYPES.TSEnumDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSEnumMember,
    typescript_estree_1.AST_NODE_TYPES.TSExportAssignment,
    typescript_estree_1.AST_NODE_TYPES.TSExternalModuleReference,
    typescript_estree_1.AST_NODE_TYPES.TSFunctionType,
    typescript_estree_1.AST_NODE_TYPES.TSImportType,
    typescript_estree_1.AST_NODE_TYPES.TSIndexedAccessType,
    typescript_estree_1.AST_NODE_TYPES.TSIndexSignature,
    typescript_estree_1.AST_NODE_TYPES.TSInferType,
    typescript_estree_1.AST_NODE_TYPES.TSInterfaceBody,
    typescript_estree_1.AST_NODE_TYPES.TSInterfaceDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSInterfaceHeritage,
    typescript_estree_1.AST_NODE_TYPES.TSIntersectionType,
    typescript_estree_1.AST_NODE_TYPES.TSImportEqualsDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSLiteralType,
    typescript_estree_1.AST_NODE_TYPES.TSMappedType,
    typescript_estree_1.AST_NODE_TYPES.TSMethodSignature,
    'TSMinusToken',
    typescript_estree_1.AST_NODE_TYPES.TSModuleBlock,
    typescript_estree_1.AST_NODE_TYPES.TSModuleDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSNonNullExpression,
    typescript_estree_1.AST_NODE_TYPES.TSParameterProperty,
    typescript_estree_1.AST_NODE_TYPES.TSParenthesizedType,
    'TSPlusToken',
    typescript_estree_1.AST_NODE_TYPES.TSPropertySignature,
    typescript_estree_1.AST_NODE_TYPES.TSQualifiedName,
    typescript_estree_1.AST_NODE_TYPES.TSQuestionToken,
    typescript_estree_1.AST_NODE_TYPES.TSRestType,
    typescript_estree_1.AST_NODE_TYPES.TSThisType,
    typescript_estree_1.AST_NODE_TYPES.TSTupleType,
    typescript_estree_1.AST_NODE_TYPES.TSTypeAnnotation,
    typescript_estree_1.AST_NODE_TYPES.TSTypeLiteral,
    typescript_estree_1.AST_NODE_TYPES.TSTypeOperator,
    typescript_estree_1.AST_NODE_TYPES.TSTypeParameter,
    typescript_estree_1.AST_NODE_TYPES.TSTypeParameterDeclaration,
    typescript_estree_1.AST_NODE_TYPES.TSTypeReference,
    typescript_estree_1.AST_NODE_TYPES.TSUnionType,
]);
exports.default = util.createRule({
    name: 'indent',
    meta: {
        type: 'layout',
        docs: {
            description: 'Enforce consistent indentation',
            tslintRuleName: 'indent',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'whitespace',
        schema: indent_1.default.meta.schema,
        messages: indent_1.default.meta.messages,
    },
    defaultOptions: [
        // typescript docs and playground use 4 space indent
        4,
        {
            // typescript docs indent the case from the switch
            // https://www.typescriptlang.org/docs/handbook/release-notes/typescript-1-8.html#example-4
            SwitchCase: 1,
            flatTernaryExpressions: false,
            ignoredNodes: [],
        },
    ],
    create(context, optionsWithDefaults) {
        // because we extend the base rule, have to update opts on the context
        // the context defines options as readonly though...
        const contextWithDefaults = Object.create(context, {
            options: {
                writable: false,
                configurable: false,
                value: optionsWithDefaults,
            },
        });
        const rules = indent_1.default.create(contextWithDefaults);
        /**
         * Converts from a TSPropertySignature to a Property
         * @param node a TSPropertySignature node
         * @param [type] the type to give the new node
         * @returns a Property node
         */
        function TSPropertySignatureToProperty(node, type = typescript_estree_1.AST_NODE_TYPES.Property) {
            const base = {
                // indent doesn't actually use these
                key: null,
                value: null,
                // Property flags
                computed: false,
                method: false,
                kind: 'init',
                // this will stop eslint from interrogating the type literal
                shorthand: true,
                // location data
                parent: node.parent,
                range: node.range,
                loc: node.loc,
            };
            if (type === typescript_estree_1.AST_NODE_TYPES.Property) {
                return Object.assign({ type }, base);
            }
            else {
                return Object.assign({ type, static: false, readonly: false }, base);
            }
        }
        return Object.assign({}, rules, {
            // overwrite the base rule here so we can use our KNOWN_NODES list instead
            '*:exit'(node) {
                // For nodes we care about, skip the default handling, because it just marks the node as ignored...
                if (!KNOWN_NODES.has(node.type)) {
                    rules['*:exit'](node);
                }
            },
            TSAsExpression(node) {
                // transform it to a BinaryExpression
                return rules['BinaryExpression, LogicalExpression']({
                    type: typescript_estree_1.AST_NODE_TYPES.BinaryExpression,
                    operator: 'as',
                    left: node.expression,
                    // the first typeAnnotation includes the as token
                    right: node.typeAnnotation,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSConditionalType(node) {
                // transform it to a ConditionalExpression
                return rules.ConditionalExpression({
                    type: typescript_estree_1.AST_NODE_TYPES.ConditionalExpression,
                    test: {
                        type: typescript_estree_1.AST_NODE_TYPES.BinaryExpression,
                        operator: 'extends',
                        left: node.checkType,
                        right: node.extendsType,
                        // location data
                        range: [node.checkType.range[0], node.extendsType.range[1]],
                        loc: {
                            start: node.checkType.loc.start,
                            end: node.extendsType.loc.end,
                        },
                    },
                    consequent: node.trueType,
                    alternate: node.falseType,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            'TSEnumDeclaration, TSTypeLiteral'(node) {
                // transform it to an ObjectExpression
                return rules['ObjectExpression, ObjectPattern']({
                    type: typescript_estree_1.AST_NODE_TYPES.ObjectExpression,
                    properties: node.members.map(member => TSPropertySignatureToProperty(member)),
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSImportEqualsDeclaration(node) {
                // transform it to an VariableDeclaration
                // use VariableDeclaration instead of ImportDeclaration because it's essentially the same thing
                const { id, moduleReference } = node;
                return rules.VariableDeclaration({
                    type: typescript_estree_1.AST_NODE_TYPES.VariableDeclaration,
                    kind: 'const',
                    declarations: [
                        {
                            type: typescript_estree_1.AST_NODE_TYPES.VariableDeclarator,
                            range: [id.range[0], moduleReference.range[1]],
                            loc: {
                                start: id.loc.start,
                                end: moduleReference.loc.end,
                            },
                            id: id,
                            init: {
                                type: typescript_estree_1.AST_NODE_TYPES.CallExpression,
                                callee: {
                                    type: typescript_estree_1.AST_NODE_TYPES.Identifier,
                                    name: 'require',
                                    range: [
                                        moduleReference.range[0],
                                        moduleReference.range[0] + 'require'.length,
                                    ],
                                    loc: {
                                        start: moduleReference.loc.start,
                                        end: {
                                            line: moduleReference.loc.end.line,
                                            column: moduleReference.loc.start.line + 'require'.length,
                                        },
                                    },
                                },
                                arguments: 'expression' in moduleReference
                                    ? [moduleReference.expression]
                                    : [],
                                // location data
                                range: moduleReference.range,
                                loc: moduleReference.loc,
                            },
                        },
                    ],
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSIndexedAccessType(node) {
                // convert to a MemberExpression
                return rules['MemberExpression, JSXMemberExpression, MetaProperty']({
                    type: typescript_estree_1.AST_NODE_TYPES.MemberExpression,
                    object: node.objectType,
                    property: node.indexType,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSInterfaceBody(node) {
                // transform it to an ClassBody
                return rules['BlockStatement, ClassBody']({
                    type: typescript_estree_1.AST_NODE_TYPES.ClassBody,
                    body: node.body.map(p => TSPropertySignatureToProperty(p, typescript_estree_1.AST_NODE_TYPES.ClassProperty)),
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            'TSInterfaceDeclaration[extends.length > 0]'(node) {
                // transform it to a ClassDeclaration
                return rules['ClassDeclaration[superClass], ClassExpression[superClass]']({
                    type: typescript_estree_1.AST_NODE_TYPES.ClassDeclaration,
                    body: node.body,
                    id: undefined,
                    // TODO: This is invalid, there can be more than one extends in interface
                    superClass: node.extends[0].expression,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSMappedType(node) {
                const sourceCode = context.getSourceCode();
                const squareBracketStart = sourceCode.getTokenBefore(node.typeParameter);
                // transform it to an ObjectExpression
                return rules['ObjectExpression, ObjectPattern']({
                    type: typescript_estree_1.AST_NODE_TYPES.ObjectExpression,
                    properties: [
                        {
                            type: typescript_estree_1.AST_NODE_TYPES.Property,
                            key: node.typeParameter,
                            value: node.typeAnnotation,
                            // location data
                            range: [
                                squareBracketStart.range[0],
                                node.typeAnnotation
                                    ? node.typeAnnotation.range[1]
                                    : squareBracketStart.range[0],
                            ],
                            loc: {
                                start: squareBracketStart.loc.start,
                                end: node.typeAnnotation
                                    ? node.typeAnnotation.loc.end
                                    : squareBracketStart.loc.end,
                            },
                            kind: 'init',
                            computed: false,
                            method: false,
                            shorthand: false,
                        },
                    ],
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSModuleBlock(node) {
                // transform it to a BlockStatement
                return rules['BlockStatement, ClassBody']({
                    type: typescript_estree_1.AST_NODE_TYPES.BlockStatement,
                    body: node.body,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSQualifiedName(node) {
                return rules['MemberExpression, JSXMemberExpression, MetaProperty']({
                    type: typescript_estree_1.AST_NODE_TYPES.MemberExpression,
                    object: node.left,
                    property: node.right,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSTupleType(node) {
                // transform it to an ArrayExpression
                return rules['ArrayExpression, ArrayPattern']({
                    type: typescript_estree_1.AST_NODE_TYPES.ArrayExpression,
                    elements: node.elementTypes,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
            TSTypeParameterDeclaration(node) {
                const [name, ...attributes] = node.params;
                // JSX is about the closest we can get because the angle brackets
                // it's not perfect but it works!
                return rules.JSXOpeningElement({
                    type: typescript_estree_1.AST_NODE_TYPES.JSXOpeningElement,
                    selfClosing: false,
                    name: name,
                    attributes: attributes,
                    // location data
                    parent: node.parent,
                    range: node.range,
                    loc: node.loc,
                });
            },
        });
    },
});
//# sourceMappingURL=indent.js.map