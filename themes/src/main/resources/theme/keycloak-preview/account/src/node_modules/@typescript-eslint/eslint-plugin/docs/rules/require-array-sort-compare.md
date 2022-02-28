# Enforce giving `compare` argument to `Array#sort` (require-array-sort-compare)

This rule prevents to invoke `Array#sort()` method without `compare` argument.

`Array#sort()` method sorts that element by the alphabet order.

```ts
[1, 2, 3, 10, 20, 30].sort(); //→ [1, 10, 2, 20, 3, 30]
```

The language specification also noted this trap.

> NOTE 2: Method calls performed by the ToString abstract operations in steps 5 and 7 have the potential to cause SortCompare to not behave as a consistent comparison function.<br> > https://www.ecma-international.org/ecma-262/9.0/#sec-sortcompare

## Rule Details

This rule is aimed at preventing the calls of `Array#sort` method.
This rule ignores the `sort` methods of user-defined types.

Examples of **incorrect** code for this rule:

```ts
const array: any[];
const stringArray: string[];

array.sort();

// Even if a string array, warns it in favor of `String#localeCompare` method.
stringArray.sort();
```

Examples of **correct** code for this rule:

```ts
const array: any[];
const userDefinedType: { sort(): void };

array.sort((a, b) => a - b);
array.sort((a, b) => a.localeCompare(b));

userDefinedType.sort();
```

### Options

There is no option.

## When Not To Use It

If you understand the language specification enough, you can turn this rule off safely.
