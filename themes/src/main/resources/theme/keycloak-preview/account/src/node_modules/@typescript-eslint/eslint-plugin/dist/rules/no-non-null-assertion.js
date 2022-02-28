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
    name: 'no-non-null-assertion',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallows non-null assertions using the `!` postfix operator',
            tslintRuleName: 'no-non-null-assertion',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        messages: {
            noNonNull: 'Forbidden non-null assertion.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        return {
            TSNonNullExpression(node) {
                context.report({
                    node,
                    messageId: 'noNonNull',
                });
            },
        };
    },
});
//# sourceMappingURL=no-non-null-assertion.js.map