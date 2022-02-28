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
exports.default = util.createRule({
    name: 'no-misused-new',
    meta: {
        type: 'problem',
        docs: {
            description: 'Enforce valid definition of `new` and `constructor`.',
            tslintRuleName: 'no-misused-new',
            category: 'Best Practices',
            recommended: 'error',
        },
        schema: [],
        messages: {
            errorMessageInterface: 'Interfaces cannot be constructed, only classes.',
            errorMessageClass: 'Class cannon have method named `new`.',
        },
    },
    defaultOptions: [],
    create(context) {
        /**
         * @param {ASTNode} node type to be inspected.
         * @returns name of simple type or null
         */
        function getTypeReferenceName(node) {
            if (node) {
                switch (node.type) {
                    case typescript_estree_1.AST_NODE_TYPES.TSTypeAnnotation:
                        return getTypeReferenceName(node.typeAnnotation);
                    case typescript_estree_1.AST_NODE_TYPES.TSTypeReference:
                        return getTypeReferenceName(node.typeName);
                    case typescript_estree_1.AST_NODE_TYPES.Identifier:
                        return node.name;
                    default:
                        break;
                }
            }
            return null;
        }
        /**
         * @param {ASTNode} parent parent node.
         * @param {ASTNode} returnType type to be compared
         */
        function isMatchingParentType(parent, returnType) {
            if (parent &&
                'id' in parent &&
                parent.id &&
                parent.id.type === typescript_estree_1.AST_NODE_TYPES.Identifier) {
                return getTypeReferenceName(returnType) === parent.id.name;
            }
            return false;
        }
        return {
            'TSInterfaceBody > TSConstructSignatureDeclaration'(node) {
                if (isMatchingParentType(node.parent.parent, node.returnType)) {
                    // constructor
                    context.report({
                        node,
                        messageId: 'errorMessageInterface',
                    });
                }
            },
            "TSMethodSignature[key.name='constructor']"(node) {
                context.report({
                    node,
                    messageId: 'errorMessageInterface',
                });
            },
            "ClassBody > MethodDefinition[key.name='new']"(node) {
                if (node.value.type === typescript_estree_1.AST_NODE_TYPES.TSEmptyBodyFunctionExpression) {
                    if (node.parent &&
                        isMatchingParentType(node.parent.parent, node.value.returnType)) {
                        context.report({
                            node,
                            messageId: 'errorMessageClass',
                        });
                    }
                }
            },
        };
    },
});
//# sourceMappingURL=no-misused-new.js.map