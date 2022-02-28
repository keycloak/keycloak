# Disallows explicit type declarations for variables or parameters initialized to a number, string, or boolean. (no-inferrable-types)

Explicit types where they can be easily inferred may add unnecessary verbosity.

## Rule Details

This rule disallows explicit type declarations on parameters, variables
and properties where the type can be easily inferred from its value.

## Options

This rule has an options object:

```json
{
  "ignoreProperties": false,
  "ignoreParameters": false
}
```

### Default

When none of the options are truthy, the following patterns are valid:

```ts
const foo = 5;
const bar = true;
const baz = 'str';

class Foo {
  prop = 5;
}

function fn(a = 5, b = true) {}

function fn(a: number, b: boolean, c: string) {}
```

The following are invalid:

```ts
const foo: number = 5;
const bar: boolean = true;
const baz: string = 'str';

class Foo {
  prop: number = 5;
}

function fn(a: number = 5, b: boolean = true) {}
```

### `ignoreProperties`

When set to true, the following pattern is considered valid:

```ts
class Foo {
  prop: number = 5;
}
```

### `ignoreParameters`

When set to true, the following pattern is considered valid:

```ts
function foo(a: number = 5, b: boolean = true) {
  // ...
}
```

## When Not To Use It

If you do not want to enforce inferred types.

## Further Reading

TypeScript [Inference](https://www.typescriptlang.org/docs/handbook/type-inference.html)

## Compatibility

TSLint: [no-inferrable-types](https://palantir.github.io/tslint/rules/no-inferrable-types/)
