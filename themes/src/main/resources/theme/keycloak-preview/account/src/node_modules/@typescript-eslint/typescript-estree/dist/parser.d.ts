import { TSESTree } from './ts-estree';
import { ParserOptions, ParserServices } from './parser-options';
declare type AST<T extends ParserOptions> = TSESTree.Program & (T['range'] extends true ? {
    range: [number, number];
} : {}) & (T['tokens'] extends true ? {
    tokens: TSESTree.Token[];
} : {}) & (T['comment'] extends true ? {
    comments: TSESTree.Comment[];
} : {});
export interface ParseAndGenerateServicesResult<T extends ParserOptions> {
    ast: AST<T>;
    services: ParserServices;
}
export declare const version: string;
export declare function parse<T extends ParserOptions = ParserOptions>(code: string, options?: T): AST<T>;
export declare function parseAndGenerateServices<T extends ParserOptions = ParserOptions>(code: string, options: T): ParseAndGenerateServicesResult<T>;
export { AST_NODE_TYPES, AST_TOKEN_TYPES, TSESTree } from './ts-estree';
export { ParserOptions, ParserServices };
//# sourceMappingURL=parser.d.ts.map