import { Program } from 'typescript';
import { Token, Comment, Node } from './ts-estree/ts-estree';
import { TSNode } from './ts-nodes';
export interface Extra {
    errorOnUnknownASTType: boolean;
    errorOnTypeScriptSyntacticAndSemanticIssues: boolean;
    useJSXTextNode: boolean;
    tokens: null | Token[];
    comment: boolean;
    code: string;
    range: boolean;
    loc: boolean;
    comments: Comment[];
    strict: boolean;
    jsx: boolean;
    log: Function;
    projects: string[];
    tsconfigRootDir: string;
    extraFileExtensions: string[];
}
export interface ParserOptions {
    range?: boolean;
    loc?: boolean;
    tokens?: boolean;
    comment?: boolean;
    jsx?: boolean;
    errorOnUnknownASTType?: boolean;
    errorOnTypeScriptSyntacticAndSemanticIssues?: boolean;
    useJSXTextNode?: boolean;
    loggerFn?: Function | false;
    project?: string | string[];
    filePath?: string;
    tsconfigRootDir?: string;
    extraFileExtensions?: string[];
}
export interface ParserWeakMap<TKey, TValueBase> {
    get<TValue extends TValueBase>(key: TKey): TValue;
    has(key: any): boolean;
}
export interface ParserServices {
    program: Program | undefined;
    esTreeNodeToTSNodeMap: ParserWeakMap<Node, TSNode> | undefined;
    tsNodeToESTreeNodeMap: ParserWeakMap<TSNode, Node> | undefined;
}
//# sourceMappingURL=parser-options.d.ts.map