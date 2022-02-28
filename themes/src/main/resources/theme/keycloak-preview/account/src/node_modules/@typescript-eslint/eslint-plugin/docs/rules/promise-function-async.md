# Functions that return promises must be async (promise-function-async)

Requires any function or method that returns a Promise to be marked async.
Ensures that each function is only capable of:

- returning a rejected promise, or
- throwing an Error object.

In contrast, non-`async` `Promise`-returning functions are technically capable of either.
Code that handles the results of those functions will often need to handle both cases, which can get complex.
This rule's practice removes a requirement for creating code to handle both cases.

## Rule Details

Examples of **incorrect** code for this rule

```ts
const arrowFunctionReturnsPromise = () => Promise.resolve('value');

function functionDeturnsPromise() {
  return Math.random() > 0.5 ? Promise.resolve('value') : false;
}
```

Examples of **correct** code for this rule

```ts
const arrowFunctionReturnsPromise = async () => 'value';

async function functionDeturnsPromise() {
  return Math.random() > 0.5 ? 'value' : false;
}
```

## Options

Options may be provided as an object with:

- `allowedPromiseNames` to indicate any extra names of classes or interfaces to be considered Promises when returned.

In addition, each of the following properties may be provided, and default to `true`:

- `checkArrowFunctions`
- `checkFunctionDeclarations`
- `checkFunctionExpressions`
- `checkMethodDeclarations`

```json
{
  "@typescript-eslint/promise-function-async": [
    "error",
    {
      "allowedPromiseNames": ["Thenable"],
      "checkArrowFunctions": true,
      "checkFunctionDeclarations": true,
      "checkFunctionExpressions": true,
      "checkMethodDeclarations": true
    }
  ]
}
```

## Related To

- TSLint: [promise-function-async](https://palantir.github.io/tslint/rules/promise-function-async)
