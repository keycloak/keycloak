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
    name: 'no-triple-slash-reference',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Disallow `/// <reference path="" />` comments',
            tslintRuleName: 'no-reference',
            category: 'Best Practices',
            recommended: 'error',
        },
        schema: [],
        messages: {
            tripleSlashReference: 'Do not use a triple slash reference.',
        },
    },
    defaultOptions: [],
    create(context) {
        const referenceRegExp = /^\/\s*<reference\s*path=/;
        const sourceCode = context.getSourceCode();
        return {
            Program(program) {
                const commentsBefore = sourceCode.getCommentsBefore(program);
                commentsBefore.forEach(comment => {
                    if (comment.type !== 'Line') {
                        return;
                    }
                    if (referenceRegExp.test(comment.value)) {
                        context.report({
                            node: comment,
                            messageId: 'tripleSlashReference',
                        });
                    }
                });
            },
        };
    },
});
//# sourceMappingURL=no-triple-slash-reference.js.map