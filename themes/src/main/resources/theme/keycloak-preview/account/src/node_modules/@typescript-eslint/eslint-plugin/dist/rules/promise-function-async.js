"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'promise-function-async',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Requires any function or method that returns a Promise to be marked async.',
            tslintName: 'promise-function-async',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            missingAsync: 'Functions that return promises must be async.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allowedPromiseNames: {
                        type: 'array',
                        items: {
                            type: 'string',
                        },
                    },
                    checkArrowFunctions: {
                        type: 'boolean',
                    },
                    checkFunctionDeclarations: {
                        type: 'boolean',
                    },
                    checkFunctionExpressions: {
                        type: 'boolean',
                    },
                    checkMethodDeclarations: {
                        type: 'boolean',
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            allowedPromiseNames: [],
            checkArrowFunctions: true,
            checkFunctionDeclarations: true,
            checkFunctionExpressions: true,
            checkMethodDeclarations: true,
        },
    ],
    create(context, [{ allowedPromiseNames, checkArrowFunctions, checkFunctionDeclarations, checkFunctionExpressions, checkMethodDeclarations, },]) {
        const allAllowedPromiseNames = new Set([
            'Promise',
            ...allowedPromiseNames,
        ]);
        const parserServices = util.getParserServices(context);
        const checker = parserServices.program.getTypeChecker();
        function validateNode(node) {
            const originalNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const signatures = checker
                .getTypeAtLocation(originalNode)
                .getCallSignatures();
            if (!signatures.length) {
                return;
            }
            const returnType = checker.getReturnTypeOfSignature(signatures[0]);
            if (!util.containsTypeByName(returnType, allAllowedPromiseNames)) {
                return;
            }
            context.report({
                messageId: 'missingAsync',
                node,
            });
        }
        return {
            'ArrowFunctionExpression[async = false]'(node) {
                if (checkArrowFunctions) {
                    validateNode(node);
                }
            },
            'FunctionDeclaration[async = false]'(node) {
                if (checkFunctionDeclarations) {
                    validateNode(node);
                }
            },
            'FunctionExpression[async = false]'(node) {
                if (node.parent &&
                    'kind' in node.parent &&
                    node.parent.kind === 'method') {
                    if (checkMethodDeclarations) {
                        validateNode(node.parent);
                    }
                }
                else if (checkFunctionExpressions) {
                    validateNode(node);
                }
            },
        };
    },
});
//# sourceMappingURL=promise-function-async.js.map