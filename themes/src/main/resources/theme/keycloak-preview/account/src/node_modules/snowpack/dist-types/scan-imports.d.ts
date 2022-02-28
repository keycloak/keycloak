import glob from 'glob';
/**
 * An install target represents information about a dependency to install.
 * The specifier is the key pointing to the dependency, either as a package
 * name or as an actual file path within node_modules. All other properties
 * are metadata about what is actually being imported.
 */
export declare type InstallTarget = {
    specifier: string;
    all: boolean;
    default: boolean;
    namespace: boolean;
    named: string[];
};
export declare function scanDepList(depList: string[], cwd: string): InstallTarget[];
interface ScanImportsParams {
    include: string;
    exclude?: glob.IOptions['ignore'];
}
export declare function scanImports({ include, exclude }: ScanImportsParams): Promise<InstallTarget[]>;
export {};
