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
    name: 'no-inferrable-types',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallows explicit type declarations for variables or parameters initialized to a number, string, or boolean.',
            tslintRuleName: 'no-inferrable-types',
            category: 'Best Practices',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            noInferrableType: 'Type {{type}} trivially inferred from a {{type}} literal, remove type annotation.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    ignoreParameters: {
                        type: 'boolean',
                    },
                    ignoreProperties: {
                        type: 'boolean',
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            ignoreParameters: true,
            ignoreProperties: true,
        },
    ],
    create(context, [{ ignoreParameters, ignoreProperties }]) {
        /**
         * Returns whether a node has an inferrable value or not
         * @param node the node to check
         * @param init the initializer
         */
        function isInferrable(node, init) {
            if (node.type !== typescript_estree_1.AST_NODE_TYPES.TSTypeAnnotation ||
                !node.typeAnnotation) {
                return false;
            }
            const annotation = node.typeAnnotation;
            if (annotation.type === typescript_estree_1.AST_NODE_TYPES.TSStringKeyword) {
                if (init.type === typescript_estree_1.AST_NODE_TYPES.Literal) {
                    return typeof init.value === 'string';
                }
                return false;
            }
            if (annotation.type === typescript_estree_1.AST_NODE_TYPES.TSBooleanKeyword) {
                return init.type === typescript_estree_1.AST_NODE_TYPES.Literal;
            }
            if (annotation.type === typescript_estree_1.AST_NODE_TYPES.TSNumberKeyword) {
                // Infinity is special
                if ((init.type === typescript_estree_1.AST_NODE_TYPES.UnaryExpression &&
                    init.operator === '-' &&
                    init.argument.type === typescript_estree_1.AST_NODE_TYPES.Identifier &&
                    init.argument.name === 'Infinity') ||
                    (init.type === typescript_estree_1.AST_NODE_TYPES.Identifier && init.name === 'Infinity')) {
                    return true;
                }
                return (init.type === typescript_estree_1.AST_NODE_TYPES.Literal && typeof init.value === 'number');
            }
            return false;
        }
        /**
         * Reports an inferrable type declaration, if any
         * @param node the node being visited
         * @param typeNode the type annotation node
         * @param initNode the initializer node
         */
        function reportInferrableType(node, typeNode, initNode) {
            if (!typeNode || !initNode || !typeNode.typeAnnotation) {
                return;
            }
            if (!isInferrable(typeNode, initNode)) {
                return;
            }
            let type = null;
            if (typeNode.typeAnnotation.type === typescript_estree_1.AST_NODE_TYPES.TSBooleanKeyword) {
                type = 'boolean';
            }
            else if (typeNode.typeAnnotation.type === typescript_estree_1.AST_NODE_TYPES.TSNumberKeyword) {
                type = 'number';
            }
            else if (typeNode.typeAnnotation.type === typescript_estree_1.AST_NODE_TYPES.TSStringKeyword) {
                type = 'string';
            }
            else {
                // shouldn't happen...
                return;
            }
            context.report({
                node,
                messageId: 'noInferrableType',
                data: {
                    type,
                },
                fix: fixer => fixer.remove(typeNode),
            });
        }
        function inferrableVariableVisitor(node) {
            if (!node.id) {
                return;
            }
            reportInferrableType(node, node.id.typeAnnotation, node.init);
        }
        function inferrableParameterVisitor(node) {
            if (ignoreParameters || !node.params) {
                return;
            }
            node.params.filter(param => param.type === typescript_estree_1.AST_NODE_TYPES.AssignmentPattern &&
                param.left &&
                param.right).forEach(param => {
                reportInferrableType(param, param.left.typeAnnotation, param.right);
            });
        }
        function inferrablePropertyVisitor(node) {
            // We ignore `readonly` because of Microsoft/TypeScript#14416
            // Essentially a readonly property without a type
            // will result in its value being the type, leading to
            // compile errors if the type is stripped.
            if (ignoreProperties || node.readonly) {
                return;
            }
            reportInferrableType(node, node.typeAnnotation, node.value);
        }
        return {
            VariableDeclarator: inferrableVariableVisitor,
            FunctionExpression: inferrableParameterVisitor,
            FunctionDeclaration: inferrableParameterVisitor,
            ArrowFunctionExpression: inferrableParameterVisitor,
            ClassProperty: inferrablePropertyVisitor,
        };
    },
});
//# sourceMappingURL=no-inferrable-types.js.map