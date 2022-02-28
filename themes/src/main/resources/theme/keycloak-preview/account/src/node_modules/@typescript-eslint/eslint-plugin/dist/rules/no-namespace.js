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
    name: 'no-namespace',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow the use of custom TypeScript modules and namespaces',
            tslintRuleName: 'no-namespace',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            moduleSyntaxIsPreferred: 'ES2015 module syntax is preferred over custom TypeScript modules and namespaces.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allowDeclarations: {
                        type: 'boolean',
                    },
                    allowDefinitionFiles: {
                        type: 'boolean',
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            allowDeclarations: false,
            allowDefinitionFiles: true,
        },
    ],
    create(context, [{ allowDeclarations, allowDefinitionFiles }]) {
        const filename = context.getFilename();
        return {
            "TSModuleDeclaration[global!=true][id.type='Identifier']"(node) {
                if ((node.parent &&
                    node.parent.type === typescript_estree_1.AST_NODE_TYPES.TSModuleDeclaration) ||
                    (allowDefinitionFiles && util.isDefinitionFile(filename)) ||
                    (allowDeclarations && node.declare === true)) {
                    return;
                }
                context.report({
                    node,
                    messageId: 'moduleSyntaxIsPreferred',
                });
            },
        };
    },
});
//# sourceMappingURL=no-namespace.js.map