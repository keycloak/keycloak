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
    name: 'no-explicit-any',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow usage of the `any` type',
            tslintRuleName: 'no-any',
            category: 'Best Practices',
            recommended: 'warn',
        },
        messages: {
            unexpectedAny: 'Unexpected any. Specify a different type.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        return {
            TSAnyKeyword(node) {
                context.report({
                    node,
                    messageId: 'unexpectedAny',
                });
            },
        };
    },
});
//# sourceMappingURL=no-explicit-any.js.map