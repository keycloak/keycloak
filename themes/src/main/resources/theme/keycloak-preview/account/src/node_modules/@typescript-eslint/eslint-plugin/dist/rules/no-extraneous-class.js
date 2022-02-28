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
    name: 'no-extraneous-class',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Forbids the use of classes as namespaces',
            tslintRuleName: 'no-unnecessary-class',
            category: 'Best Practices',
            recommended: false,
        },
        schema: [
            {
                type: 'object',
                additionalProperties: false,
                properties: {
                    allowConstructorOnly: {
                        type: 'boolean',
                    },
                    allowEmpty: {
                        type: 'boolean',
                    },
                    allowStaticOnly: {
                        type: 'boolean',
                    },
                },
            },
        ],
        messages: {
            empty: 'Unexpected empty class.',
            onlyStatic: 'Unexpected class with only static properties.',
            onlyConstructor: 'Unexpected class with only a constructor.',
        },
    },
    defaultOptions: [
        {
            allowConstructorOnly: false,
            allowEmpty: false,
            allowStaticOnly: false,
        },
    ],
    create(context, [{ allowConstructorOnly, allowEmpty, allowStaticOnly }]) {
        return {
            ClassBody(node) {
                const parent = node.parent;
                if (!parent || parent.superClass) {
                    return;
                }
                const reportNode = 'id' in parent && parent.id ? parent.id : parent;
                if (node.body.length === 0) {
                    if (allowEmpty) {
                        return;
                    }
                    context.report({
                        node: reportNode,
                        messageId: 'empty',
                    });
                    return;
                }
                let onlyStatic = true;
                let onlyConstructor = true;
                for (const prop of node.body) {
                    if ('kind' in prop && prop.kind === 'constructor') {
                        if (prop.value.params.some(param => param.type === typescript_estree_1.AST_NODE_TYPES.TSParameterProperty)) {
                            onlyConstructor = false;
                            onlyStatic = false;
                        }
                    }
                    else {
                        onlyConstructor = false;
                        if ('static' in prop && !prop.static) {
                            onlyStatic = false;
                        }
                    }
                    if (!(onlyStatic || onlyConstructor))
                        break;
                }
                if (onlyConstructor) {
                    if (!allowConstructorOnly) {
                        context.report({
                            node: reportNode,
                            messageId: 'onlyConstructor',
                        });
                    }
                    return;
                }
                if (onlyStatic && !allowStaticOnly) {
                    context.report({
                        node: reportNode,
                        messageId: 'onlyStatic',
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=no-extraneous-class.js.map