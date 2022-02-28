import ts from 'typescript';
import { Extra } from './parser-options';
export default function astConverter(ast: ts.SourceFile, extra: Extra, shouldProvideParserServices: boolean): {
    estree: import("./ts-estree/ts-estree").Program;
    astMaps: {
        esTreeNodeToTSNodeMap: WeakMap<object, any>;
        tsNodeToESTreeNodeMap: WeakMap<object, any>;
    } | undefined;
};
//# sourceMappingURL=ast-converter.d.ts.map