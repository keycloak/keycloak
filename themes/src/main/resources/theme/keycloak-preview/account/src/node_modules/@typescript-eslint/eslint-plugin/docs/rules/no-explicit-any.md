# Disallow usage of the `any` type (no-explicit-any)

Using the `any` type defeats the purpose of using TypeScript.
When `any` is used, all compiler type checks around that value are ignored.

## Rule Details

This rule goes doesn't allow `any` types to be defined.
It aims to keep TypeScript maximally useful.
TypeScript has a compiler flag for `--noImplicitAny` that will prevent
an `any` type from being implied by the compiler, but doesn't prevent
`any` from being explicitly used.

The following patterns are considered warnings:

```ts
const age: any = 'seventeen';
```

```ts
const ages: any[] = ['seventeen'];
```

```ts
const ages: Array<any> = ['seventeen'];
```

```ts
function greet(): any {}
```

```ts
function greet(): any[] {}
```

```ts
function greet(): Array<any> {}
```

```ts
function greet(): Array<Array<any>> {}
```

```ts
function greet(param: Array<any>): string {}
```

```ts
function greet(param: Array<any>): Array<any> {}
```

The following patterns are not warnings:

```ts
const age: number = 17;
```

```ts
const ages: number[] = [17];
```

```ts
const ages: Array<number> = [17];
```

```ts
function greet(): string {}
```

```ts
function greet(): string[] {}
```

```ts
function greet(): Array<string> {}
```

```ts
function greet(): Array<Array<string>> {}
```

```ts
function greet(param: Array<string>): string {}
```

```ts
function greet(param: Array<string>): Array<string> {}
```

## When Not To Use It

If an unknown type or a library without typings is used
and you want to be able to specify `any`.

## Further Reading

- TypeScript [any type](https://www.typescriptlang.org/docs/handbook/basic-types.html#any)

## Compatibility

- TSLint: [no-any](https://palantir.github.io/tslint/rules/no-any/)
