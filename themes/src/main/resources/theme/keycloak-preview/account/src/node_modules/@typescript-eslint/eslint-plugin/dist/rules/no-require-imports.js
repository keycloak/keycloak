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
    name: 'no-require-imports',
    meta: {
        type: 'problem',
        docs: {
            description: 'Disallows invocation of `require()`.',
            tslintName: 'no-require-imports',
            category: 'Best Practices',
            recommended: 'error',
        },
        schema: [],
        messages: {
            noRequireImports: 'A `require()` style import is forbidden.',
        },
    },
    defaultOptions: [],
    create(context) {
        return {
            'CallExpression > Identifier[name="require"]'(node) {
                context.report({
                    node: node.parent,
                    messageId: 'noRequireImports',
                });
            },
            TSExternalModuleReference(node) {
                context.report({
                    node,
                    messageId: 'noRequireImports',
                });
            },
        };
    },
});
//# sourceMappingURL=no-require-imports.js.map