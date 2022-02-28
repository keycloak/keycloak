"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const convert_1 = require("./convert");
const convert_comments_1 = require("./convert-comments");
const node_utils_1 = require("./node-utils");
function astConverter(ast, extra, shouldProvideParserServices) {
    /**
     * The TypeScript compiler produced fundamental parse errors when parsing the
     * source.
     */
    if (ast.parseDiagnostics.length) {
        throw convert_1.convertError(ast.parseDiagnostics[0]);
    }
    /**
     * Recursively convert the TypeScript AST into an ESTree-compatible AST
     */
    const instance = new convert_1.Converter(ast, {
        errorOnUnknownASTType: extra.errorOnUnknownASTType || false,
        useJSXTextNode: extra.useJSXTextNode || false,
        shouldProvideParserServices,
    });
    const estree = instance.convertProgram();
    /**
     * Optionally convert and include all tokens in the AST
     */
    if (extra.tokens) {
        estree.tokens = node_utils_1.convertTokens(ast);
    }
    /**
     * Optionally convert and include all comments in the AST
     */
    if (extra.comment) {
        estree.comments = convert_comments_1.convertComments(ast, extra.code);
    }
    const astMaps = shouldProvideParserServices
        ? instance.getASTMaps()
        : undefined;
    return { estree, astMaps };
}
exports.default = astConverter;
//# sourceMappingURL=ast-converter.js.map