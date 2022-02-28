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
    name: 'generic-type-naming',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Enforces naming of generic type variables',
            category: 'Stylistic Issues',
            recommended: false,
        },
        messages: {
            paramNotMatchRule: 'Type parameter {{name}} does not match rule {{rule}}.',
        },
        schema: [
            {
                type: 'string',
            },
        ],
    },
    defaultOptions: [
        // Matches: T , TA , TAbc , TA1Bca , T1 , T2
        '^T([A-Z0-9][a-zA-Z0-9]*){0,1}$',
    ],
    create(context, [rule]) {
        const regex = new RegExp(rule);
        return {
            TSTypeParameter(node) {
                const name = node.name.name;
                if (name && !regex.test(name)) {
                    context.report({
                        node,
                        messageId: 'paramNotMatchRule',
                        data: {
                            name,
                            rule,
                        },
                    });
                }
            },
        };
    },
});
//# sourceMappingURL=generic-type-naming.js.map