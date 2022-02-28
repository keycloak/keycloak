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
    name: 'no-var-requires',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallows the use of require statements except in import statements',
            tslintRuleName: 'no-var-requires',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            noVarReqs: 'Require statement not part of import statement.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        return {
            CallExpression(node) {
                if (node.callee.type === typescript_estree_1.AST_NODE_TYPES.Identifier &&
                    node.callee.name === 'require' &&
                    node.parent &&
                    node.parent.type === typescript_estree_1.AST_NODE_TYPES.VariableDeclarator) {
                    context.report({
                        node,
                        messageId: 'noVarReqs',
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-var-requires.js.map