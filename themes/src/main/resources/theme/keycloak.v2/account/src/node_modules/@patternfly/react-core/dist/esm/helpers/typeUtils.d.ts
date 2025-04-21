export declare type RequiredKeys<T> = {
    [K in keyof T]-?: {} extends {
        [P in K]: T[K];
    } ? never : K;
}[keyof T];
export declare type OptionalKeys<T> = {
    [K in keyof T]-?: {} extends {
        [P in K]: T[K];
    } ? K : never;
}[keyof T];
export declare type PickOptional<T> = Pick<T, OptionalKeys<T>>;
export declare type PickAndRequireOptional<T> = Required<Pick<T, OptionalKeys<T>>>;
//# sourceMappingURL=typeUtils.d.ts.map