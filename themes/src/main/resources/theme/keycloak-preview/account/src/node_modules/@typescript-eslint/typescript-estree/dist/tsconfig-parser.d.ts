import ts from 'typescript';
import { Extra } from './parser-options';
/**
 * Calculate project environments using options provided by consumer and paths from config
 * @param code The code being linted
 * @param filePath The path of the file being parsed
 * @param extra.tsconfigRootDir The root directory for relative tsconfig paths
 * @param extra.project Provided tsconfig paths
 * @returns The programs corresponding to the supplied tsconfig paths
 */
export declare function calculateProjectParserOptions(code: string, filePath: string, extra: Extra): ts.Program[];
/**
 * Create program from single root file. Requires a single tsconfig to be specified.
 * @param code The code being linted
 * @param filePath The file being linted
 * @param extra.tsconfigRootDir The root directory for relative tsconfig paths
 * @param extra.project Provided tsconfig paths
 * @returns The program containing just the file being linted and associated library files
 */
export declare function createProgram(code: string, filePath: string, extra: Extra): ts.Program | undefined;
//# sourceMappingURL=tsconfig-parser.d.ts.map