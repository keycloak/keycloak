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
    name: 'prefer-interface',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Prefer an interface declaration over a type literal (type T = { ... })',
            tslintRuleName: 'interface-over-type-literal',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            interfaceOverType: 'Use an interface instead of a type literal.',
        },
        schema: [],
    },
    defaultOptions: [],
    create(context) {
        const sourceCode = context.getSourceCode();
        return {
            // VariableDeclaration with kind type has only one VariableDeclarator
            "TSTypeAliasDeclaration[typeAnnotation.type='TSTypeLiteral']"(node) {
                context.report({
                    node: node.id,
                    messageId: 'interfaceOverType',
                    fix(fixer) {
                        const typeNode = node.typeParameters || node.id;
                        const fixes = [];
                        const firstToken = sourceCode.getFirstToken(node);
                        if (firstToken) {
                            fixes.push(fixer.replaceText(firstToken, 'interface'));
                            fixes.push(fixer.replaceTextRange([typeNode.range[1], node.typeAnnotation.range[0]], ' '));
                        }
                        const afterToken = sourceCode.getTokenAfter(node.typeAnnotation);
                        if (afterToken &&
                            afterToken.type === 'Punctuator' &&
                            afterToken.value === ';') {
                            fixes.push(fixer.remove(afterToken));
                        }
                        return fixes;
                    },
                });
            },
        };
    },
});
//# sourceMappingURL=prefer-interface.js.map