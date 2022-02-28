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
    name: 'ban-ts-ignore',
    meta: {
        type: 'problem',
        docs: {
            description: 'Bans “// @ts-ignore” comments from being used.',
            tslintRuleName: 'ban-ts-ignore',
            category: 'Best Practices',
            recommended: 'error',
        },
        schema: [],
        messages: {
            tsIgnoreComment: 'Do not use "// @ts-ignore" comments because they suppress compilation errors.',
        },
    },
    defaultOptions: [],
    create(context) {
        const tsIgnoreRegExp = /^\/*\s*@ts-ignore/;
        const sourceCode = context.getSourceCode();
        return {
            Program() {
                const comments = sourceCode.getAllComments();
                comments.forEach(comment => {
                    if (comment.type !== 'Line') {
                        return;
                    }
                    if (tsIgnoreRegExp.test(comment.value)) {
                        context.report({
                            node: comment,
                            messageId: 'tsIgnoreComment',
                        });
                    }
                });
            },
        };
    },
});
//# sourceMappingURL=ban-ts-ignore.js.map