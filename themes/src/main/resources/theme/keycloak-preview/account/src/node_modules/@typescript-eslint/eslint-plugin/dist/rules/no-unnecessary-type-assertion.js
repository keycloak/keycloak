"use strict";
var __importStar = (this && this.__importStar) || function (mod) {
    if (mod && mod.__esModule) return mod;
    var result = {};
    if (mod != null) for (var k in mod) if (Object.hasOwnProperty.call(mod, k)) result[k] = mod[k];
    result["default"] = mod;
    return result;
};
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const tsutils = __importStar(require("tsutils"));
const typescript_1 = __importDefault(require("typescript"));
const util = __importStar(require("../util"));
exports.default = util.createRule({
    name: 'no-unnecessary-type-assertion',
    meta: {
        docs: {
            description: 'Warns if a type assertion does not change the type of an expression',
            category: 'Best Practices',
            recommended: false,
            tslintRuleName: 'no-unnecessary-type-assertion',
        },
        fixable: 'code',
        messages: {
            unnecessaryAssertion: 'This assertion is unnecessary since it does not change the type of the expression.',
        },
        schema: [
            {
                type: 'object',
                properties: {
                    typesToIgnore: {
                        type: 'array',
                        items: {
                            type: 'string',
                        },
                    },
                },
            },
        ],
        type: 'suggestion',
    },
    defaultOptions: [{}],
    create(context, [options]) {
        const sourceCode = context.getSourceCode();
        const parserServices = util.getParserServices(context);
        /**
         * Sometimes tuple types don't have ObjectFlags.Tuple set, like when they're being matched against an inferred type.
         * So, in addition, check if there are integer properties 0..n and no other numeric keys
         */
        function couldBeTupleType(type) {
            const properties = type.getProperties();
            if (properties.length === 0) {
                return false;
            }
            let i = 0;
            for (; i < properties.length; ++i) {
                const name = properties[i].name;
                if (String(i) !== name) {
                    if (i === 0) {
                        // if there are no integer properties, this is not a tuple
                        return false;
                    }
                    break;
                }
            }
            for (; i < properties.length; ++i) {
                if (String(+properties[i].name) === properties[i].name) {
                    return false; // if there are any other numeric properties, this is not a tuple
                }
            }
            return true;
        }
        function checkNonNullAssertion(node, checker) {
            const originalNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const type = checker.getTypeAtLocation(originalNode.expression);
            if (type === checker.getNonNullableType(type)) {
                context.report({
                    node,
                    messageId: 'unnecessaryAssertion',
                    fix(fixer) {
                        return fixer.removeRange([
                            originalNode.expression.end,
                            originalNode.end,
                        ]);
                    },
                });
            }
        }
        function verifyCast(node, checker) {
            if (options &&
                options.typesToIgnore &&
                options.typesToIgnore.indexOf(sourceCode.getText(node.typeAnnotation)) !== -1) {
                return;
            }
            const originalNode = parserServices.esTreeNodeToTSNodeMap.get(node);
            const castType = checker.getTypeAtLocation(originalNode);
            if (tsutils.isTypeFlagSet(castType, typescript_1.default.TypeFlags.Literal) ||
                (tsutils.isObjectType(castType) &&
                    (tsutils.isObjectFlagSet(castType, typescript_1.default.ObjectFlags.Tuple) ||
                        couldBeTupleType(castType)))) {
                // It's not always safe to remove a cast to a literal type or tuple
                // type, as those types are sometimes widened without the cast.
                return;
            }
            const uncastType = checker.getTypeAtLocation(originalNode.expression);
            if (uncastType === castType) {
                context.report({
                    node,
                    messageId: 'unnecessaryAssertion',
                    fix(fixer) {
                        return originalNode.kind === typescript_1.default.SyntaxKind.TypeAssertionExpression
                            ? fixer.removeRange([
                                originalNode.getStart(),
                                originalNode.expression.getStart(),
                            ])
                            : fixer.removeRange([
                                originalNode.expression.end,
                                originalNode.end,
                            ]);
                    },
                });
            }
        }
        const checker = parserServices.program.getTypeChecker();
        return {
            TSNonNullExpression(node) {
                checkNonNullAssertion(node, checker);
            },
            TSTypeAssertion(node) {
                verifyCast(node, checker);
            },
            TSAsExpression(node) {
                verifyCast(node, checker);
            },
        };
    },
});
//# sourceMappingURL=no-unnecessary-type-assertion.js.map