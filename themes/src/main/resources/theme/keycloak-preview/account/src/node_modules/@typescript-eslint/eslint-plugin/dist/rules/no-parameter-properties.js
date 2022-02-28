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
    name: 'no-parameter-properties',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallow the use of parameter properties in class constructors.',
            tslintRuleName: 'no-parameter-properties',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        messages: {
            noParamProp: 'Property {{parameter}} cannot be declared in the constructor.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    allows: {
                        type: 'array',
                        items: {
                            enum: [
                                'readonly',
                                'private',
                                'protected',
                                'public',
                                'private readonly',
                                'protected readonly',
                                'public readonly',
                            ],
                        },
                        minItems: 1,
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            allows: [],
        },
    ],
    create(context, [{ allows }]) {
        /**
         * Gets the modifiers of `node`.
         * @param node the node to be inspected.
         */
        function getModifiers(node) {
            const modifiers = [];
            if (node.accessibility) {
                modifiers.push(node.accessibility);
            }
            if (node.readonly) {
                modifiers.push('readonly');
            }
            return modifiers.filter(Boolean).join(' ');
        }
        return {
            TSParameterProperty(node) {
                const modifiers = getModifiers(node);
                if (allows.indexOf(modifiers) === -1) {
                    // HAS to be an identifier or assignment or TSC will throw
                    if (node.parameter.type !== typescript_estree_1.AST_NODE_TYPES.Identifier &&
                        node.parameter.type !== typescript_estree_1.AST_NODE_TYPES.AssignmentPattern) {
                        return;
                    }
                    const name = node.parameter.type === typescript_estree_1.AST_NODE_TYPES.Identifier
                        ? node.parameter.name
                        : // has to be an Identifier or TSC will throw an error
                            node.parameter.left.name;
                    context.report({
                        node,
                        messageId: 'noParamProp',
                        data: {
                            parameter: name,
                        },
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-parameter-properties.js.map