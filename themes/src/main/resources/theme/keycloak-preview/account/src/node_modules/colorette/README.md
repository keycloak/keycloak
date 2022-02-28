# Colorette [![](https://img.shields.io/npm/v/colorette.svg?label=&color=0080ff)](https://www.npmjs.org/package/colorette) [![CI](https://img.shields.io/travis/jorgebucaran/colorette.svg?label=)](https://travis-ci.org/jorgebucaran/colorette)

> Color your terminal using pure idiomatic JavaScript.

Colorette is a Node.js library for embellishing your CLI tools with colors and styles using [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code).

- Up to ~10x faster than the alternatives ([run the benchmarks](#run-the-benchmarks)).
- No wonky prototype-based method chains.
- Automatic color support detection.
- ~80 LOC and no dependencies.
- [`NO_COLOR`](https://no-color.org) friendly.

## Quickstart

```console
npm i colorette
```

Load the [styles](#styles) you need. [Here](#supported-styles)'s the list of the styles you can use.

```js
const { red, blue, bold } = require("colorette")
```

Wrap your strings in one or more styles to produce the finish you're looking for.

```js
console.log(bold(blue("Engage!")))
```

Or mix it with [template literals](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Template_literals) to interpolate variables, expressions and create multi-line strings easily.

```js
console.log(`
  Beets are ${red("red")},
  Plums are ${blue("blue")},
  ${bold("Colorette!")}.
`)
```

Using `console.log`'s [string substitution](https://nodejs.org/api/console.html#console_console_log_data_args) can be useful too!

```js
console.log(bold("Total: $%f"), 1.99)
```

You can even nest styles without breaking existing escape codes.

```js
console.log(red(`Red Shirt ${blue("Blue Shirt")} Red Shirt`))
```

Feeling adventurous? Try the [pipeline operator](https://github.com/tc39/proposal-pipeline-operator).

```js
console.log("Make it so!" |> bold |> blue)
```

## Supported styles

Colorette supports the standard and bright color variations out-of-the-box. For true color support see [this issue](https://github.com/jorgebucaran/colorette/issues/27).

| Colors  | Background Colors | Bright Colors | Bright Background Colors | Modifiers         |
| ------- | ----------------- | ------------- | ------------------------ | ----------------- |
| black   | bgBlack           | blackBright   | bgBlackBright            | dim               |
| red     | bgRed             | redBright     | bgRedBright              | **bold**          |
| green   | bgGreen           | greenBright   | bgGreenBright            | hidden            |
| yellow  | bgYellow          | yellowBright  | bgYellowBright           | _italic_          |
| blue    | bgBlue            | blueBright    | bgBlueBright             | underline         |
| magenta | bgMagenta         | magentaBright | bgMagentaBright          | ~~strikethrough~~ |
| cyan    | bgCyan            | cyanBright    | bgCyanBright             | reset             |
| white   | bgWhite           | whiteBright   | bgWhiteBright            |                   |
| gray    |                   |               |                          |                   |

## API

### <code><i>style</i>(string)</code>

Returns a string wrapped in the corresponding ANSI escape code.

```js
red("Red Alert") //=> \u001b[31mRed Alert\u001b[39m
```

### `options.enabled`

Colorette is enabled if your terminal supports color, `FORCE_COLOR=1` or if `NO_COLOR` isn't in the environment, but you can always override it when you need to.

```js
const { options } = require("colorette")

options.enabled = false
```

## Run the benchmarks

```
npm i -C bench && node bench
```

<pre>
# Using Styles
chalk × 14,468 ops/sec
colorette × 901,148 ops/sec

# Combining Styles
chalk × 44,067 ops/sec
colorette × 2,566,778 ops/sec

# Nesting Styles
chalk × 40,165 ops/sec
colorette × 506,494 ops/sec
</pre>

## License

[MIT](LICENSE.md)
