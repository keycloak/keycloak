> 🛑 This file is source code, not the primary documentation location! 🛑
>
> See **https://typescript-eslint.io/rules/no-namespace** for documentation.

Custom TypeScript modules (`module foo {}`) and namespaces (`namespace foo {}`) are considered outdated
ways to organize TypeScript code. ES2015 module syntax is now preferred (`import`/`export`).

This rule still allows the use of TypeScript module declarations to describe external APIs (`declare module 'foo' {}`).

## Rule Details

This rule aims to standardize the way modules are declared.

## Options

This rule, in its default state, does not require any argument. If you would like to enable one
or more of the following you may pass an object with the options set as follows:

- `allowDeclarations` set to `true` will allow you to `declare` custom TypeScript modules and namespaces (Default: `false`).
- `allowDefinitionFiles` set to `true` will allow you to `declare` and use custom TypeScript modules and namespaces
  inside definition files (Default: `true`).

Examples of code for the default `{ "allowDeclarations": false, "allowDefinitionFiles": true }` options:

<!--tabs-->

### ❌ Incorrect

```ts
module foo {}
namespace foo {}

declare module foo {}
declare namespace foo {}
```

### ✅ Correct

```ts
declare module 'foo' {}

// anything inside a d.ts file
```

<!--/tabs-->

### `allowDeclarations`

Examples of code for the `{ "allowDeclarations": true }` option:

<!--tabs-->

#### ❌ Incorrect

```ts
module foo {}
namespace foo {}
```

#### ✅ Correct

```ts
declare module 'foo' {}
declare module foo {}
declare namespace foo {}

declare global {
  namespace foo {}
}

declare module foo {
  namespace foo {}
}
```

<!--/tabs-->

Examples of code for the `{ "allowDeclarations": false }` option:

<!--tabs-->

#### ❌ Incorrect

```ts
module foo {}
namespace foo {}
declare module foo {}
declare namespace foo {}
```

#### ✅ Correct

```ts
declare module 'foo' {}
```

### `allowDefinitionFiles`

Examples of code for the `{ "allowDefinitionFiles": true }` option:

<!--tabs-->

#### ❌ Incorrect

```ts
// if outside a d.ts file
module foo {}
namespace foo {}

// if outside a d.ts file and allowDeclarations = false
module foo {}
namespace foo {}
declare module foo {}
declare namespace foo {}
```

#### ✅ Correct

```ts
declare module 'foo' {}

// anything inside a d.ts file
```

## When Not To Use It

If you are using the ES2015 module syntax, then you will not need this rule.

## Further Reading

- [Modules](https://www.typescriptlang.org/docs/handbook/modules.html)
- [Namespaces](https://www.typescriptlang.org/docs/handbook/namespaces.html)
- [Namespaces and Modules](https://www.typescriptlang.org/docs/handbook/namespaces-and-modules.html)
