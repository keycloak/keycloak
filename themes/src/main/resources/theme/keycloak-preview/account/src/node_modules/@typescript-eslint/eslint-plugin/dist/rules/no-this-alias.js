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
    name: 'no-this-alias',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow aliasing `this`',
            tslintRuleName: 'no-this-assignment',
            category: 'Best Practices',
            recommended: false,
        },
        schema: [
            {
                type: 'object',
                additionalProperties: false,
                properties: {
                    allowDestructuring: {
                        type: 'boolean',
                    },
                    allowedNames: {
                        type: 'array',
                        items: {
                            type: 'string',
                        },
                    },
                },
            },
        ],
        messages: {
            thisAssignment: "Unexpected aliasing of 'this' to local variable.",
            thisDestructure: "Unexpected aliasing of members of 'this' to local variables.",
        },
    },
    defaultOptions: [
        {
            allowDestructuring: false,
            allowedNames: [],
        },
    ],
    create(context, [{ allowDestructuring, allowedNames }]) {
        return {
            "VariableDeclarator[init.type='ThisExpression']"(node) {
                const { id } = node;
                if (allowDestructuring && id.type !== typescript_estree_1.AST_NODE_TYPES.Identifier) {
                    return;
                }
                const hasAllowedName = id.type === typescript_estree_1.AST_NODE_TYPES.Identifier
                    ? allowedNames.includes(id.name)
                    : false;
                if (!hasAllowedName) {
                    context.report({
                        node: id,
                        messageId: id.type === typescript_estree_1.AST_NODE_TYPES.Identifier
                            ? 'thisAssignment'
                            : 'thisDestructure',
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-this-alias.js.map