# Require explicit accessibility modifiers on class properties and methods (explicit-member-accessibility)

Leaving off accessibility modifier and making everything public can make
your interface hard to use by others.
If you make all internal pieces private or protected, your interface will
be easier to use.

## Rule Details

This rule aims to make code more readable and explicit about who can use
which properties.

The following patterns are considered warnings:

```ts
class Animal {
  name: string; // No accessibility modifier
  getName(): string {} // No accessibility modifier
}
```

The following patterns are not warnings:

```ts
class Animal {
  private name: string; // explicit accessibility modifier
  public getName(): string {} // explicit accessibility modifier
}
```

## When Not To Use It

If you think defaulting to public is a good default, then you will not need
this rule.

## Further Reading

- TypeScript [Accessibility Modifiers](https://www.typescriptlang.org/docs/handbook/classes.html#public-private-and-protected-modifiers)

## Compatibility

- TSLint: [member-access](http://palantir.github.io/tslint/rules/member-access/)
