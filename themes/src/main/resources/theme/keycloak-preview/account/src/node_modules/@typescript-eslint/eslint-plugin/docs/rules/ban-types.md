# Enforces that types will not to be used (ban-types)

Bans specific types from being used. Does not ban the corresponding runtime objects from being used.

## Rule Details

Examples of **incorrect** code for this rule `"String": "Use string instead"`

```ts
class Foo<F = String> extends Bar<String> implements Baz<String> {
  constructor(foo: String) {}

  exit(): Array<String> {
    const foo: String = 1 as String;
  }
}
```

Examples of **correct** code for this rule `"String": "Use string instead"`

```ts
class Foo<F = string> extends Bar<string> implements Baz<string> {
  constructor(foo: string) {}

  exit(): Array<string> {
    const foo: string = 1 as string;
  }
}
```

## Options

```CJSON
{
    "@typescript-eslint/ban-types": ["error", {
        "types": {
            // report usages of the type using the default error message
            "Foo": null,

            // add a custom message to help explain why not to use it
            "Bar": "Don't use bar!",

            // add a custom message, AND tell the plugin how to fix it
            "String": {
                "message": "Use string instead",
                "fixWith": "string"
            }
        }
    }
}
```

### Example

```json
{
  "@typescript-eslint/ban-types": [
    "error",
    {
      "types": {
        "Array": null,
        "Object": "Use {} instead",
        "String": {
          "message": "Use string instead",
          "fixWith": "string"
        }
      }
    }
  ]
}
```

## Compatibility

- TSLint: [ban-types](https://palantir.github.io/tslint/rules/ban-types/)
