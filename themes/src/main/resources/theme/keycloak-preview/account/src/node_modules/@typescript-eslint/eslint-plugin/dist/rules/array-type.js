"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const util = __importStar(require("../util"));
/**
 * Check whatever node can be considered as simple
 * @param node the node to be evaluated.
 */
function isSimpleType(node) {
    switch (node.type) {
        case typescript_estree_1.AST_NODE_TYPES.Identifier:
        case typescript_estree_1.AST_NODE_TYPES.TSAnyKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSBooleanKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSNeverKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSNumberKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSObjectKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSStringKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSSymbolKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSUnknownKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSVoidKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSNullKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSArrayType:
        case typescript_estree_1.AST_NODE_TYPES.TSUndefinedKeyword:
        case typescript_estree_1.AST_NODE_TYPES.TSThisType:
        case typescript_estree_1.AST_NODE_TYPES.TSQualifiedName:
            return true;
        case typescript_estree_1.AST_NODE_TYPES.TSTypeReference:
            if (node.typeName &&
                node.typeName.type === typescript_estree_1.AST_NODE_TYPES.Identifier &&
                node.typeName.name === 'Array') {
                if (!node.typeParameters) {
                    return true;
                }
                if (node.typeParameters.params.length === 1) {
                    return isSimpleType(node.typeParameters.params[0]);
                }
            }
            else {
                if (node.typeParameters) {
                    return false;
                }
                return isSimpleType(node.typeName);
            }
            return false;
        default:
            return false;
    }
}
/**
 * Check if node needs parentheses
 * @param node the node to be evaluated.
 */
function typeNeedsParentheses(node) {
    switch (node.type) {
        case typescript_estree_1.AST_NODE_TYPES.TSTypeReference:
            return typeNeedsParentheses(node.typeName);
        case typescript_estree_1.AST_NODE_TYPES.TSUnionType:
        case typescript_estree_1.AST_NODE_TYPES.TSFunctionType:
        case typescript_estree_1.AST_NODE_TYPES.TSIntersectionType:
        case typescript_estree_1.AST_NODE_TYPES.TSTypeOperator:
        case typescript_estree_1.AST_NODE_TYPES.TSInferType:
            return true;
        default:
            return false;
    }
}
exports.default = util.createRule({
    name: 'array-type',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Requires using either `T[]` or `Array<T>` for arrays',
            tslintRuleName: 'array-type',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            errorStringGeneric: "Array type using '{{type}}[]' is forbidden. Use 'Array<{{type}}>' instead.",
            errorStringGenericSimple: "Array type using '{{type}}[]' is forbidden for non-simple types. Use 'Array<{{type}}>' instead.",
            errorStringArray: "Array type using 'Array<{{type}}>' is forbidden. Use '{{type}}[]' instead.",
            errorStringArraySimple: "Array type using 'Array<{{type}}>' is forbidden for simple types. Use '{{type}}[]' instead.",
        },
        schema: [
            {
                enum: ['array', 'generic', 'array-simple'],
            },
        ],
    },
    defaultOptions: ['array'],
    create(context, [option]) {
        const sourceCode = context.getSourceCode();
        /**
         * Check if whitespace is needed before this node
         * @param node the node to be evaluated.
         */
        function requireWhitespaceBefore(node) {
            const prevToken = sourceCode.getTokenBefore(node);
            if (!prevToken) {
                return false;
            }
            if (node.range[0] - prevToken.range[1] > 0) {
                return false;
            }
            return prevToken.type === typescript_estree_1.AST_TOKEN_TYPES.Identifier;
        }
        /**
         * @param node the node to be evaluated.
         */
        function getMessageType(node) {
            if (node) {
                if (node.type === typescript_estree_1.AST_NODE_TYPES.TSParenthesizedType) {
                    return getMessageType(node.typeAnnotation);
                }
                if (isSimpleType(node)) {
                    return sourceCode.getText(node);
                }
            }
            return 'T';
        }
        return {
            TSArrayType(node) {
                if (option === 'array' ||
                    (option === 'array-simple' && isSimpleType(node.elementType))) {
                    return;
                }
                const messageId = option === 'generic'
                    ? 'errorStringGeneric'
                    : 'errorStringGenericSimple';
                context.report({
                    node,
                    messageId,
                    data: {
                        type: getMessageType(node.elementType),
                    },
                    fix(fixer) {
                        const startText = requireWhitespaceBefore(node);
                        const toFix = [
                            fixer.replaceTextRange([node.range[1] - 2, node.range[1]], '>'),
                            fixer.insertTextBefore(node, `${startText ? ' ' : ''}Array<`),
                        ];
                        if (node.elementType.type === typescript_estree_1.AST_NODE_TYPES.TSParenthesizedType) {
                            const first = sourceCode.getFirstToken(node.elementType);
                            const last = sourceCode.getLastToken(node.elementType);
                            if (!first || !last) {
                                return null;
                            }
                            toFix.push(fixer.remove(first));
                            toFix.push(fixer.remove(last));
                        }
                        return toFix;
                    },
                });
            },
            TSTypeReference(node) {
                if (option === 'generic' ||
                    node.typeName.type !== typescript_estree_1.AST_NODE_TYPES.Identifier ||
                    node.typeName.name !== 'Array') {
                    return;
                }
                const messageId = option === 'array' ? 'errorStringArray' : 'errorStringArraySimple';
                const typeParams = node.typeParameters && node.typeParameters.params;
                if (!typeParams || typeParams.length === 0) {
                    // Create an 'any' array
                    context.report({
                        node,
                        messageId,
                        data: {
                            type: 'any',
                        },
                        fix(fixer) {
                            return fixer.replaceText(node, 'any[]');
                        },
                    });
                    return;
                }
                if (typeParams.length !== 1 ||
                    (option === 'array-simple' && !isSimpleType(typeParams[0]))) {
                    return;
                }
                const type = typeParams[0];
                const parens = typeNeedsParentheses(type);
                context.report({
                    node,
                    messageId,
                    data: {
                        type: getMessageType(type),
                    },
                    fix(fixer) {
                        return [
                            fixer.replaceTextRange([node.range[0], type.range[0]], parens ? '(' : ''),
                            fixer.replaceTextRange([type.range[1], node.range[1]], parens ? ')[]' : '[]'),
                        ];
                    },
                });
            },
        };
    },
});
//# sourceMappingURL=array-type.js.map