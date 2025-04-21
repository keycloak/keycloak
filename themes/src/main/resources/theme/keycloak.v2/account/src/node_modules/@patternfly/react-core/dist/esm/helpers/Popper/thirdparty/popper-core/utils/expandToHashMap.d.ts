/**
 * @param value
 * @param keys
 */
export default function expandToHashMap<T extends number | string | boolean, K extends string>(value: T, keys: K[]): {
    [key: string]: T;
};
//# sourceMappingURL=expandToHashMap.d.ts.map