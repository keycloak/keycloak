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
    name: 'no-empty-interface',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow the declaration of empty interfaces',
            tslintRuleName: 'no-empty-interface',
            category: 'Best Practices',
            recommended: 'error',
        },
        messages: {
            noEmpty: 'An empty interface is equivalent to `{}`.',
            noEmptyWithSuper: 'An interface declaring no members is equivalent to its supertype.',
        },
        schema: [
            {
                type: 'object',
                additionalProperties: false,
                properties: {
                    allowSingleExtends: {
                        type: 'boolean',
                    },
                },
            },
        ],
    },
    defaultOptions: [
        {
            allowSingleExtends: false,
        },
    ],
    create(context, [{ allowSingleExtends }]) {
        return {
            TSInterfaceDeclaration(node) {
                if (node.body.body.length !== 0) {
                    // interface contains members --> Nothing to report
                    return;
                }
                if (!node.extends || node.extends.length === 0) {
                    context.report({
                        node: node.id,
                        messageId: 'noEmpty',
                    });
                }
                else if (node.extends.length === 1) {
                    // interface extends exactly 1 interface --> Report depending on rule setting
                    if (allowSingleExtends) {
                        return;
                    }
                    else {
                        context.report({
                            node: node.id,
                            messageId: 'noEmptyWithSuper',
                        });
                    }
                }
            },
        };
    },
});
//# sourceMappingURL=no-empty-interface.js.map