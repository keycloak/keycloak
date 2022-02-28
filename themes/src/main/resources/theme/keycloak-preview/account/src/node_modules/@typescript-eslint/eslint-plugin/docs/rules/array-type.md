# Requires using either `T[]` or `Array<T>` for arrays (array-type)

```ts
class Foo<T = Array<Array<Bar>>> extends Bar<T, Array<T>>
  implements Baz<Array<T>> {
  private s: Array<T>;

  constructor(p: Array<T>) {
    return new Array();
  }
}
```

## Rule Details

This rule aims to standardise usage of array.

## Options

Default config:

```JSON
{
    "array-type": ["error", "array"]
}
```

- `array` enforces use of `T[]` for all types `T`.
- `generic` enforces use of `Array<T>` for all types `T`.
- `array-simple` enforces use of `T[]` if `T` is a simple type.

## Related to

- TSLint: [array-type](https://palantir.github.io/tslint/rules/array-type/)
