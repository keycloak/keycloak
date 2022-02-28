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
    name: 'no-array-constructor',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow generic `Array` constructors',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            useLiteral: 'The array literal notation [] is preferrable.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        /**
         * Disallow construction of dense arrays using the Array constructor
         * @param node node to evaluate
         */
        function check(node) {
            if (node.arguments.length !== 1 &&
                node.callee.type === typescript_estree_1.AST_NODE_TYPES.Identifier &&
                node.callee.name === 'Array' &&
                !node.typeParameters) {
                context.report({
                    node,
                    messageId: 'useLiteral',
                    fix(fixer) {
                        if (node.arguments.length === 0) {
                            return fixer.replaceText(node, '[]');
                        }
                        const fullText = context.getSourceCode().getText(node);
                        const preambleLength = node.callee.range[1] - node.range[0];
                        return fixer.replaceText(node, `[${fullText.slice(preambleLength + 1, -1)}]`);
                    },
                });
            }
        }
        return {
            CallExpression: check,
            NewExpression: check,
        };
    },
});
//# sourceMappingURL=no-array-constructor.js.map