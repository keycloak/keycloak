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
    name: 'ban-types',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Enforces that types will not to be used',
            tslintRuleName: 'ban-types',
            category: 'Best Practices',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            bannedTypeMessage: "Don't use '{{name}}' as a type.{{customMessage}}",
        },
        schema: [
            {
                type: 'object',
                properties: {
                    types: {
                        type: 'object',
                        additionalProperties: {
                            oneOf: [
                                { type: 'null' },
                                { type: 'string' },
                                {
                                    type: 'object',
                                    properties: {
                                        message: { type: 'string' },
                                        fixWith: { type: 'string' },
                                    },
                                    additionalProperties: false,
                                },
                            ],
                        },
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            types: {
                String: {
                    message: 'Use string instead',
                    fixWith: 'string',
                },
                Boolean: {
                    message: 'Use boolean instead',
                    fixWith: 'boolean',
                },
                Number: {
                    message: 'Use number instead',
                    fixWith: 'number',
                },
                Object: {
                    message: 'Use Record<string, any> instead',
                    fixWith: 'Record<string, any>',
                },
                Symbol: {
                    message: 'Use symbol instead',
                    fixWith: 'symbol',
                },
            },
        },
    ],
    create(context, [{ types: bannedTypes }]) {
        return {
            'TSTypeReference Identifier'(node) {
                if (node.parent &&
                    node.parent.type !== typescript_estree_1.AST_NODE_TYPES.TSQualifiedName) {
                    if (node.name in bannedTypes) {
                        let customMessage = '';
                        const bannedCfgValue = bannedTypes[node.name];
                        let fix = null;
                        if (typeof bannedCfgValue === 'string') {
                            customMessage += ` ${bannedCfgValue}`;
                        }
                        else if (bannedCfgValue !== null) {
                            if (bannedCfgValue.message) {
                                customMessage += ` ${bannedCfgValue.message}`;
                            }
                            if (bannedCfgValue.fixWith) {
                                const fixWith = bannedCfgValue.fixWith;
                                fix = fixer => fixer.replaceText(node, fixWith);
                            }
                        }
                        context.report({
                            node,
                            messageId: 'bannedTypeMessage',
                            data: {
                                name: node.name,
                                customMessage,
                            },
                            fix,
                        });
                    }
                }
            },
        };
    },
});
//# sourceMappingURL=ban-types.js.map