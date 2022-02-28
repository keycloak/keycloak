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
    name: 'prefer-namespace-keyword',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Require the use of the `namespace` keyword instead of the `module` keyword to declare custom TypeScript modules.',
            tslintRuleName: 'no-internal-module',
            category: 'Best Practices',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            useNamespace: "Use 'namespace' instead of 'module' to declare custom TypeScript modules.",
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const sourceCode = context.getSourceCode();
        return {
            TSModuleDeclaration(node) {
                // Do nothing if the name is a string.
                if (!node.id || node.id.type === typescript_estree_1.AST_NODE_TYPES.Literal) {
                    return;
                }
                // Get tokens of the declaration header.
                const moduleType = sourceCode.getTokenBefore(node.id);
                if (moduleType &&
                    moduleType.type === typescript_estree_1.AST_TOKEN_TYPES.Identifier &&
                    moduleType.value === 'module') {
                    context.report({
                        node,
                        messageId: 'useNamespace',
                        fix(fixer) {
                            return fixer.replaceText(moduleType, 'namespace');
                        },
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=prefer-namespace-keyword.js.map