# babel-plugin-emotion

> Babel plugin for the minification and optimization of emotion styles.

`babel-plugin-emotion` is highly recommended, but not required in version 8 and
above of `emotion`.

## Features

<table>
  <thead>
    <tr>
      <th>Feature/Syntax</th>
      <th>Native</th>
      <th>Babel Plugin Required</th>
      <th>Notes</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td><code>css``</code></td>
      <td align="center">✅</td>
      <td align="center"></td>
      <td></td>
    </tr>
    <tr>
      <td><code>css(...)</code></td>
      <td align="center">✅</td>
      <td align="center"></td>
      <td>Generally used for object styles.</td>
    </tr>
    <tr>
      <td><code>styled('div')``</code> syntax</td>
      <td align="center">✅</td>
      <td align="center"></td>
      <td>Both string and object styles work without this plugin.</td>
    </tr>
    <tr>
      <td><code>styled.div``</code> syntax</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Supporting the shortcut syntax without the Babel plugin requires a large list of valid elements to be included in the bundle.</td>
    </tr>
    <tr>
      <td>components as selectors</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Allows an emotion component to be <a href="https://emotion.sh/docs/styled#targeting-another-emotion-component">used as a CSS selector</a>.</td>
    </tr>
    <tr>
      <td>Minification</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Any leading/trailing space between properties in your <code>css</code> and <code>styled</code> blocks is removed. This can reduce the size of your final bundle.</td>
    </tr>
    <tr>
      <td>Dead Code Elimination</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Uglifyjs will use the injected <code>/*#__PURE__*/</code> flag comments to mark your <code>css</code> and <code>styled</code> blocks as candidates for dead code elimination.</td>
    </tr>
    <tr>
      <td>Static Extraction (deprecated)</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Generated CSS that is eligible for extraction can be moved to an external css file.</td>
    </tr>
    <tr>
      <td>Source Maps</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>When enabled, navigate directly to the style declaration in your javascript file.</td>
    </tr>
    <tr>
      <td><code>css</code> as Prop</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Convenient helper for calling <code>css</code> and appending the generated className during compile time.</td>
    </tr>
    <tr>
      <td>Contextual Class Names</td>
      <td align="center"></td>
      <td align="center">✅</td>
      <td>Generated class names include the name of the variable or component they were defined in.</td>
    </tr>
  </tbody>
</table>

## Example

**In**

```javascript
const myStyles = css`
  font-size: 20px;
  @media (min-width: 420px) {
    color: blue;
    ${css`
      width: 96px;
      height: 96px;
    `};
    line-height: 26px;
  }
  background: green;
  ${{ backgroundColor: 'hotpink' }};
`
```

**Out**

```javascript
const myStyles = /* #__PURE__ */ css(
  'font-size:20px;@media(min-width:420px){color:blue;',
  /* #__PURE__ */ css('width:96px;height:96px;'),
  ';line-height:26px;}background:green;',
  { backgroundColor: 'hotpink' },
  ';'
)
```

## Installation

```bash
yarn add --dev babel-plugin-emotion
```

or if you prefer npm

```bash
npm install --save-dev babel-plugin-emotion
```

## Usage

### Via `.babelrc` (Recommended)

**.babelrc**

Without options:

```json
{
  "plugins": ["emotion"]
}
```

With options:

_Defaults Shown_

```json
{
  "plugins": [
    [
      "emotion",
      {
        "hoist": false,
        "sourceMap": false,
        "autoLabel": false,
        "labelFormat": "[local]",
        "extractStatic": false,
        "outputDir": "",
        "importedNames": {
          "styled": "styled",
          "css": "css",
          "keyframes": "keyframes",
          "injectGlobal": "injectGlobal",
          "merge": "merge"
        }
      }
    ]
  ]
}
```

Recommended Setup

Use [Babel's `env` property](https://babeljs.io/docs/usage/babelrc/#env-option)

**.babelrc**

```json
{
  "env": {
    "production": {
      "plugins": [["emotion", { "hoist": true }]]
    },
    "development": {
      "plugins": [["emotion", { "sourceMap": true, "autoLabel": true }]]
    }
  }
}
```

### Via CLI

```bash
babel --plugins babel-plugin-emotion script.js
```

### Via Node API

```javascript
require('@babel/core').transform('code', {
  plugins: ['babel-plugin-emotion']
})
```

## Options

### `hoist`

`boolean`, defaults to `false`.

This option enables the following:

* Any argument supplied to `css` or `styled` is hoisted.

By hoisting the argument, or assigning the value to a variable, emotion is able to leverage the use of a [WeakMap](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap)[cache](https://github.com/emotion-js/emotion/blob/6257f0c9cb00db9cbd08a9d6995f335730808329/packages/emotion/src/index.js#L85-L116) to increase performance. Users of object styles with `css` prop will benefit the most from enabling this option.

**In**

```javascript
const Sample = () => <div css={{ background: 'brown' }} />
```

**Out**

```javascript
var _css = require('emotion').css
var _ref = { background: 'brown' }

const Sample = () => <div className={_css(_ref)} />
```

### `sourceMap`

`boolean`, defaults to `false`.

This option enables the following:

* Injected source maps for use in browser dev tools

[**Documentation**](https://emotion.sh/docs/source-maps)

### `autoLabel`

`boolean`, defaults to `false`.

This option enables the following:

* Automatically adds the `label` property to styles so that class names
  generated by `css` or `styled` include the name of the variable the result is
  assigned to.
* Please note that non word characters in the variable will be removed
  (Eg. `iconStyles$1` will become `iconStyles1`) because `$` is not valid
  [CSS ClassName Selector](https://stackoverflow.com/questions/448981/which-characters-are-valid-in-css-class-names-selectors#449000)

#### css

**In**

```javascript
const brownStyles = css({ color: 'brown' })
```

**Out**

```javascript
const brownStyles = /*#__PURE__*/ css({ color: 'brown' }, 'label:brownStyles;')
```

`brownStyles`'s value would be `css-1q8eu9e-brownStyles`

### `labelFormat`

`string`, defaults to `"[local]"`.

This option only works when `autoLabel` is set to `true`. It allows you to
define the format of the resulting `label`. The format is defined via string where
variable parts are enclosed in square brackets `[]`.
For example `labelFormat: "my-classname--[local]"`, where `[local]` will be replaced
with the name of the variable the result is assigned to.

Allowed values:

* `[local]` - the name of the variable the result of the `css` or `styled` expression is assigned to.
* `[filename]` - name of the file (without extension) where `css` or `styled` expression is located.

This format only affects the label property of the expression, meaning that `css` prefix and hash will
be prepended automatically.

#### css

**In**

```javascript
// BrownView.js
// autoLabel: true
// labelFormat: [filename]--[local]
const brownStyles = css({ color: 'brown' })
```

**Out**

```javascript
const brownStyles = /*#__PURE__*/ css(
  { color: 'brown' },
  'label:BrownView--brownStyles;'
)
```

`BrownView--brownStyles`'s value would be `css-1q8eu9e-BrownView--brownStyles`

#### styled

**In**

```javascript
const Profile = () => {
  const H1 = styled.h1({
    borderRadius: '50%',
    transition: 'transform 400ms ease-in-out',
    boxSizing: 'border-box',
    display: 'flex',
    ':hover': {
      transform: 'scale(1.2)'
    }
  })
}
```

**Out**

```javascript
const Profile = () => {
  const H1 = /*#__PURE__*/ styled('h1', {
    label: 'H1'
  })({
    borderRadius: '50%',
    transition: 'transform 400ms ease-in-out',
    boxSizing: 'border-box',
    display: 'flex',
    ':hover': {
      transform: 'scale(1.2)'
    }
  })
}
```

`H1`'s class name attribute would be `css-13djram-H1`

### `extractStatic`

`boolean`, defaults to `false`.

> Note:
>
> extractStatic is deprecated and will be removed in emotion@10. We recommend disabling extractStatic or using other libraries like [linaria](https://github.com/callstack-io/linaria) or [css-literal-loader](https://github.com/4Catalyzer/css-literal-loader)

This option enables the following:

* Extract static styles into CSS files.

[**Documentation**](https://emotion.sh/docs/extract-static)

### `outputDir`

`string`, defaults to `''`

This option only works when `extractStatic` is set to true. This option allows `babel-plugin-emotion` to set a different directory to where the extracted css files are saved to.

### `importedNames`

`object`, defaults to

```json
{
  "styled": "styled",
  "css": "css",
  "keyframes": "keyframes",
  "injectGlobal": "injectGlobal",
  "merge": "merge"
}
```

This option enables the following:

* Configurable import names

[**Documentation**](https://emotion.sh/docs/configurable-imports)

### `instances`

`Array<string>`, defaults to

```jsx
;['emotion', 'react-emotion', 'preact-emotion']
```

This option allows `babel-plugin-emotion` to know which imports to treat as
emotion imports and transform as such. This option is **only** required if you
use a custom instance of emotion created with `create-emotion` or you're
importing emotion from somewhere other than the paths above. Relative paths are
resolved relative to `process.cwd()`(the current working directory).

[**Documentation**](https://emotion.sh/docs/instances)

### `primaryInstance`

`string`, defaults to

```jsx
'emotion'
```

This option allows `babel-plugin-emotion` to know where to import emotion from
when it needs to import emotion. Currently this is only used for the css prop to
import `css` and `merge` but it could be used for other purposes in the future
so it's recommend to make sure that this instance exports everything returned
from `createEmotion`,
[an up-to-date example of this can be found in the emotion package's source](https://github.com/emotion-js/emotion/blob/master/packages/emotion/src/index.js).

[**Documentation**](https://emotion.sh/docs/instances)

## Babel Macros

Instead of using `babel-plugin-emotion`, you can use emotion with [`babel-plugin-macros`](https://github.com/kentcdodds/babel-plugin-macros). Add `babel-plugin-macros` to your babel config and import whatever you want from emotion but add `/macro` to the end. Currently every API except for the css prop is supported by the macro.

```jsx
import styled from 'react-emotion/macro'
import { css, keyframes, injectGlobal, flush, hydrate } from 'emotion/macro'
```

[create-react-app issue discussing macros](https://github.com/facebookincubator/create-react-app/issues/2730).
