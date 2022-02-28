# Require that interface names be prefixed with `I` (interface-name-prefix)

It can be hard to differentiate between classes and interfaces.
Prefixing interfaces with "I" can help telling them apart at a glance.

## Rule Details

This rule enforces consistency of interface naming prefix conventions.

## Options

This rule has a string option.

- `"never"` (default) disallows all interfaces being prefixed with `"I"`
- `"always"` requires all interfaces be prefixed with `"I"`

### never

TypeScript suggests [never prefixing](https://github.com/Microsoft/TypeScript/wiki/Coding-guidelines#names) interfaces with "I".

The following patterns are considered warnings:

```ts
interface IAnimal {
  name: string;
}
```

The following patterns are not warnings:

```ts
interface Animal {
  name: string;
}
```

### always

The following patterns are considered warnings:

```ts
interface Animal {
  name: string;
}
```

The following patterns are not warnings:

```ts
interface IAnimal {
  name: string;
}
```

## When Not To Use It

If you do not want to enforce interface name prefixing.

## Further Reading

TypeScript [Interfaces](https://www.typescriptlang.org/docs/handbook/interfaces.html)

## Compatibility

TSLint: [interface-name](https://palantir.github.io/tslint/rules/interface-name/)
