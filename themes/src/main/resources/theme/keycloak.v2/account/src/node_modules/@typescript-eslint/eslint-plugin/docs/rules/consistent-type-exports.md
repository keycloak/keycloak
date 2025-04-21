> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/consistent-type-exports** for documentation.

TypeScript 3.8 added support for type-only exports.

Type-only exports allow you to specify that 1 or more named exports are exported as type-only. This allows
transpilers to drop exports without knowing the types of the dependencies.

## Rule Details

This rule aims to standardize the use of type exports style across a codebase.

Given a class `Button`, and an interface `ButtonProps`, examples of code:

<!--tabs-->

### ❌ Incorrect

```ts
interface ButtonProps {
  onClick: () => void;
}
class Button implements ButtonProps {
  onClick() {
    console.log('button!');
  }
}
export { Button, ButtonProps };
```

### ✅ Correct

```ts
interface ButtonProps {
  onClick: () => void;
}
class Button implements ButtonProps {
  onClick() {
    console.log('button!');
  }
}
export { Button };
export type { ButtonProps };
```

## Options

```ts
interface Options {
  fixMixedExportsWithInlineTypeSpecifier?: boolean;
}

const defaultOptions: Options = {
  fixMixedExportsWithInlineTypeSpecifier: false,
};
```

### `fixMixedExportsWithInlineTypeSpecifier`

When this is set to true, the rule will autofix "mixed" export cases using TS 4.5's "inline type specifier".
If you are using a TypeScript version less than 4.5, then you will not be able to use this option.

For example the following code:

```ts
const x = 1;
type T = number;

export { x, T };
```

With `{fixMixedExportsWithInlineTypeSpecifier: true}` will be fixed to:

```ts
const x = 1;
type T = number;

export { x, type T };
```

With `{fixMixedExportsWithInlineTypeSpecifier: false}` will be fixed to:

```ts
const x = 1;
type T = number;

export type { T };
export { x };
```

<!--tabs-->

### ❌ Incorrect

```ts
export { Button } from 'some-library';
export type { ButtonProps } from 'some-library';
```

### ✅ Correct

```ts
export { Button, type ButtonProps } from 'some-library';
```

## When Not To Use It

- If you are using a TypeScript version less than 3.8, then you will not be able to use this rule as type exports are not supported.
- If you specifically want to use both export kinds for stylistic reasons, you can disable this rule.
- If you use `--isolatedModules` the compiler would error if a type is not re-exported using `export type`. If you also don't wish to enforce one style over the other, you can disable this rule.
