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
    name: 'no-angle-bracket-type-assertion',
    meta: {
        type: 'problem',
        docs: {
            description: 'Enforces the use of `as Type` assertions instead of `<Type>` assertions',
            tslintRuleName: 'no-angle-bracket-type-assertion',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        messages: {
            preferAs: "Prefer 'as {{cast}}' instead of '<{{cast}}>' when doing type assertions.",
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const sourceCode = context.getSourceCode();
        return {
            TSTypeAssertion(node) {
                context.report({
                    node,
                    messageId: 'preferAs',
                    data: {
                        cast: sourceCode.getText(node.typeAnnotation),
                    },
                });
            },
        };
    },
});
//# sourceMappingURL=no-angle-bracket-type-assertion.js.map