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
const definition = {
    type: 'object',
    properties: {
        before: { type: 'boolean' },
        after: { type: 'boolean' },
    },
    additionalProperties: false,
};
exports.default = util.createRule({
    name: 'type-annotation-spacing',
    meta: {
        type: 'layout',
        docs: {
            description: 'Require consistent spacing around type annotations',
            tslintRuleName: 'typedef-whitespace',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'whitespace',
        messages: {
            expectedSpaceAfter: "Expected a space after the '{{type}}'.",
            expectedSpaceBefore: "Expected a space before the '{{type}}'.",
            unexpectedSpaceAfter: "Unexpected a space after the '{{type}}'.",
            unexpectedSpaceBefore: "Unexpected a space before the '{{type}}'.",
        },
        schema: [
            {
                type: 'object',
                properties: {
                    before: { type: 'boolean' },
                    after: { type: 'boolean' },
                    overrides: {
                        type: 'object',
                        properties: {
                            colon: definition,
                            arrow: definition,
                        },
                        additionalProperties: false,
                    },
                },
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        // technically there is a default, but the overrides mean
        // that if we apply them here, it will break the no override case.
        {},
    ],
    create(context, [options]) {
        const punctuators = [':', '=>'];
        const sourceCode = context.getSourceCode();
        const overrides = options.overrides || { colon: {}, arrow: {} };
        const colonOptions = Object.assign({}, { before: false, after: true }, options, overrides.colon);
        const arrowOptions = Object.assign({}, { before: true, after: true }, options, overrides.arrow);
        /**
         * Checks if there's proper spacing around type annotations (no space
         * before colon, one space after).
         */
        function checkTypeAnnotationSpacing(typeAnnotation) {
            const nextToken = typeAnnotation;
            const punctuatorTokenEnd = sourceCode.getTokenBefore(nextToken);
            let punctuatorTokenStart = punctuatorTokenEnd;
            let previousToken = sourceCode.getTokenBefore(punctuatorTokenEnd);
            let type = punctuatorTokenEnd.value;
            if (punctuators.indexOf(type) === -1) {
                return;
            }
            const before = type === ':' ? colonOptions.before : arrowOptions.before;
            const after = type === ':' ? colonOptions.after : arrowOptions.after;
            if (type === ':' && previousToken.value === '?') {
                // shift the start to the ?
                type = '?:';
                punctuatorTokenStart = previousToken;
                previousToken = sourceCode.getTokenBefore(previousToken);
                // handle the +/- modifiers for optional modification operators
                if (previousToken.value === '+' || previousToken.value === '-') {
                    type = `${previousToken.value}?:`;
                    punctuatorTokenStart = previousToken;
                    previousToken = sourceCode.getTokenBefore(previousToken);
                }
            }
            const previousDelta = punctuatorTokenStart.range[0] - previousToken.range[1];
            const nextDelta = nextToken.range[0] - punctuatorTokenEnd.range[1];
            if (after && nextDelta === 0) {
                context.report({
                    node: punctuatorTokenEnd,
                    messageId: 'expectedSpaceAfter',
                    data: {
                        type,
                    },
                    fix(fixer) {
                        return fixer.insertTextAfter(punctuatorTokenEnd, ' ');
                    },
                });
            }
            else if (!after && nextDelta > 0) {
                context.report({
                    node: punctuatorTokenEnd,
                    messageId: 'unexpectedSpaceAfter',
                    data: {
                        type,
                    },
                    fix(fixer) {
                        return fixer.removeRange([
                            punctuatorTokenEnd.range[1],
                            nextToken.range[0],
                        ]);
                    },
                });
            }
            if (before && previousDelta === 0) {
                context.report({
                    node: punctuatorTokenStart,
                    messageId: 'expectedSpaceBefore',
                    data: {
                        type,
                    },
                    fix(fixer) {
                        return fixer.insertTextAfter(previousToken, ' ');
                    },
                });
            }
            else if (!before && previousDelta > 0) {
                context.report({
                    node: punctuatorTokenStart,
                    messageId: 'unexpectedSpaceBefore',
                    data: {
                        type,
                    },
                    fix(fixer) {
                        return fixer.removeRange([
                            previousToken.range[1],
                            punctuatorTokenStart.range[0],
                        ]);
                    },
                });
            }
        }
        return {
            TSMappedType(node) {
                if (node.typeAnnotation) {
                    checkTypeAnnotationSpacing(node.typeAnnotation);
                }
            },
            TSTypeAnnotation(node) {
                checkTypeAnnotationSpacing(node.typeAnnotation);
            },
        };
    },
});
//# sourceMappingURL=type-annotation-spacing.js.map