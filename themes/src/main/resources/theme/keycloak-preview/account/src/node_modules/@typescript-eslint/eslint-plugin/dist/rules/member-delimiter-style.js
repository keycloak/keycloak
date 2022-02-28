"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
Object.defineProperty(exports, "__esModule", { value: true });
const typescript_estree_1 = require("@typescript-eslint/typescript-estree");
const util = __importStar(require("../util"));
const definition = {
    type: 'object',
    properties: {
        multiline: {
            type: 'object',
            properties: {
                delimiter: { enum: ['none', 'semi', 'comma'] },
                requireLast: { type: 'boolean' },
            },
            additionalProperties: false,
        },
        singleline: {
            type: 'object',
            properties: {
                // note can't have "none" for single line delimiter as it's invlaid syntax
                delimiter: { enum: ['semi', 'comma'] },
                requireLast: { type: 'boolean' },
            },
            additionalProperties: false,
        },
    },
    additionalProperties: false,
};
exports.default = util.createRule({
    name: 'member-delimiter-style',
    meta: {
        type: 'suggestion',
        docs: {
            description: 'Require a specific member delimiter style for interfaces and type literals',
            category: 'Stylistic Issues',
            recommended: 'error',
        },
        fixable: 'code',
        messages: {
            unexpectedComma: 'Unexpected separator (,).',
            unexpectedSemi: 'Unexpected separator (;).',
            expectedComma: 'Expected a comma.',
            expectedSemi: 'Expected a semicolon.',
        },
        schema: [
            {
                type: 'object',
                properties: Object.assign({}, definition.properties, {
                    overrides: {
                        type: 'object',
                        properties: {
                            interface: definition,
                            typeLiteral: definition,
                        },
                        additionalProperties: false,
                    },
                }),
                additionalProperties: false,
            },
        ],
    },
    defaultOptions: [
        {
            multiline: {
                delimiter: 'semi',
                requireLast: true,
            },
            singleline: {
                delimiter: 'semi',
                requireLast: false,
            },
        },
    ],
    create(context, [options]) {
        const sourceCode = context.getSourceCode();
        // use the base options as the defaults for the cases
        const baseOptions = options;
        const overrides = baseOptions.overrides || {};
        const interfaceOptions = util.deepMerge(baseOptions, overrides.interface);
        const typeLiteralOptions = util.deepMerge(baseOptions, overrides.typeLiteral);
        /**
         * Check the last token in the given member.
         * @param member the member to be evaluated.
         * @param opts the options to be validated.
         * @param isLast a flag indicating `member` is the last in the interface or type literal.
         */
        function checkLastToken(member, opts, isLast) {
            /**
             * Resolves the boolean value for the given setting enum value
             * @param type the option name
             */
            function getOption(type) {
                if (isLast && !opts.requireLast) {
                    // only turn the option on if its expecting no delimiter for the last member
                    return type === 'none';
                }
                return opts.delimiter === type;
            }
            let messageId = null;
            let missingDelimiter = false;
            const lastToken = sourceCode.getLastToken(member, {
                includeComments: false,
            });
            if (!lastToken) {
                return;
            }
            const optsSemi = getOption('semi');
            const optsComma = getOption('comma');
            const optsNone = getOption('none');
            if (lastToken.value === ';') {
                if (optsComma) {
                    messageId = 'expectedComma';
                }
                else if (optsNone) {
                    missingDelimiter = true;
                    messageId = 'unexpectedSemi';
                }
            }
            else if (lastToken.value === ',') {
                if (optsSemi) {
                    messageId = 'expectedSemi';
                }
                else if (optsNone) {
                    missingDelimiter = true;
                    messageId = 'unexpectedComma';
                }
            }
            else {
                if (optsSemi) {
                    missingDelimiter = true;
                    messageId = 'expectedSemi';
                }
                else if (optsComma) {
                    missingDelimiter = true;
                    messageId = 'expectedComma';
                }
            }
            if (messageId) {
                context.report({
                    node: lastToken,
                    loc: {
                        start: {
                            line: lastToken.loc.end.line,
                            column: lastToken.loc.end.column,
                        },
                        end: {
                            line: lastToken.loc.end.line,
                            column: lastToken.loc.end.column,
                        },
                    },
                    messageId,
                    fix(fixer) {
                        if (optsNone) {
                            // remove the unneeded token
                            return fixer.remove(lastToken);
                        }
                        const token = optsSemi ? ';' : ',';
                        if (missingDelimiter) {
                            // add the missing delimiter
                            return fixer.insertTextAfter(lastToken, token);
                        }
                        // correct the current delimiter
                        return fixer.replaceText(lastToken, token);
                    },
                });
            }
        }
        /**
         * Check the member separator being used matches the delimiter.
         * @param {ASTNode} node the node to be evaluated.
         */
        function checkMemberSeparatorStyle(node) {
            const isSingleLine = node.loc.start.line === node.loc.end.line;
            const members = node.type === typescript_estree_1.AST_NODE_TYPES.TSInterfaceBody ? node.body : node.members;
            const typeOpts = node.type === typescript_estree_1.AST_NODE_TYPES.TSInterfaceBody
                ? interfaceOptions
                : typeLiteralOptions;
            const opts = isSingleLine ? typeOpts.singleline : typeOpts.multiline;
            members.forEach((member, index) => {
                checkLastToken(member, opts || {}, index === members.length - 1);
            });
        }
        return {
            TSInterfaceBody: checkMemberSeparatorStyle,
            TSTypeLiteral: checkMemberSeparatorStyle,
        };
    },
});
//# sourceMappingURL=member-delimiter-style.js.map