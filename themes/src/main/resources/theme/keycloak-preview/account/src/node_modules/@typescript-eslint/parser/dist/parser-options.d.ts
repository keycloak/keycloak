export interface ParserOptions {
    loc?: boolean;
    comment?: boolean;
    range?: boolean;
    tokens?: boolean;
    sourceType?: 'script' | 'module';
    ecmaVersion?: number;
    ecmaFeatures?: {
        globalReturn?: boolean;
        jsx?: boolean;
    };
    filePath?: string;
    project?: string | string[];
    useJSXTextNode?: boolean;
    errorOnUnknownASTType?: boolean;
    errorOnTypeScriptSyntacticAndSemanticIssues?: boolean;
    tsconfigRootDir?: string;
    extraFileExtensions?: string[];
    warnOnUnsupportedTypeScriptVersion?: boolean;
}
//# sourceMappingURL=parser-options.d.ts.map