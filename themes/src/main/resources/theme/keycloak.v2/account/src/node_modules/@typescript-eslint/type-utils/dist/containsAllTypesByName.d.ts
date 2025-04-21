import * as ts from 'typescript';
/**
 * @param type Type being checked by name.
 * @param allowedNames Symbol names checking on the type.
 * @returns Whether the type is, extends, or contains all of the allowed names.
 */
export declare function containsAllTypesByName(type: ts.Type, allowAny: boolean, allowedNames: Set<string>): boolean;
//# sourceMappingURL=containsAllTypesByName.d.ts.map