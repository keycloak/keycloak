import * as ts from 'typescript';
/**
 * Gets all of the type flags in a type, iterating through unions automatically
 */
export declare function getTypeFlags(type: ts.Type): ts.TypeFlags;
/**
 * Checks if the given type is (or accepts) the given flags
 * @param isReceiver true if the type is a receiving type (i.e. the type of a called function's parameter)
 */
export declare function isTypeFlagSet(type: ts.Type, flagsToCheck: ts.TypeFlags, isReceiver?: boolean): boolean;
//# sourceMappingURL=typeFlagUtils.d.ts.map