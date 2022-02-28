# Require PascalCased class and interface names (class-name-casing)

This rule enforces PascalCased names for classes and interfaces.

## Rule Details

This rule aims to make it easy to differentiate classes from regular variables at a glance.

Examples of **incorrect** code for this rule:

```ts
class invalidClassName {}

class Another_Invalid_Class_Name {}

var bar = class invalidName {};

interface someInterface {}
```

Examples of **correct** code for this rule:

```ts
class ValidClassName {}

export default class {}

var foo = class {};

interface SomeInterface {}
```

## When Not To Use It

You should turn off this rule if you do not care about class name casing, or if
you use a different type of casing.

## Further Reading

- [`class-name`](https://palantir.github.io/tslint/rules/class-name/) in [TSLint](https://palantir.github.io/tslint/)
