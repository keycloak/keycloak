# Forbids the use of classes as namespaces (no-extraneous-class)

This rule warns when a class is accidentally used as a namespace.

## Rule Details

From TSLint’s docs:

> Users who come from a Java-style OO language may wrap their utility functions in an extra class,
> instead of putting them at the top level.

Examples of **incorrect** code for this rule:

```ts
class EmptyClass {}

class ConstructorOnly {
  constructor() {
    foo();
  }
}

// Use an object instead:
class StaticOnly {
  static version = 42;
  static hello() {
    console.log('Hello, world!');
  }
}
```

Examples of **correct** code for this rule:

```ts
class EmptyClass extends SuperClass {}

class ParameterProperties {
  constructor(public name: string) {}
}

const StaticOnly = {
  version: 42,
  hello() {
    console.log('Hello, world!');
  },
};
```

### Options

This rule accepts a single object option.

- `constructorOnly: true` will silence warnings about classes containing only a constructor.
- `allowEmpty: true` will silence warnings about empty classes.
- `staticOnly: true` will silence warnings about classes containing only static members.

## When Not To Use It

You can disable this rule if you don’t have anyone who would make these kinds of mistakes on your
team or if you use classes as namespaces.

## Compatibility

[`no-unnecessary-class`](https://palantir.github.io/tslint/rules/no-unnecessary-class/) from TSLint
