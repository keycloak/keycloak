# @patternfly/react-tokens

## Installation

```bash
yarn add @patternfly/react-tokens
```

or

```bash
npm install --save @patternfly/react-tokens
```

## Usage

All Tokens and their corresponding values can be viewed on the
[PatternFly React Tokens][token-page] page.

#### Import tokens

## Examples
```js
import global_BackgroundColor_100 from '@patternfly/react-tokens/dist/esm/global_-background-color_100';
```

#### Each token as three properties

- `name`: The CSS custom property name.
- `value`: The default value for the custom property.
- `var`: The property name wrapped in `var()`.

```js
import global_BackgroundColor_100 from '@patternfly/react-tokens/dist/esm/global_-background-color_100';

global_BackgroundColor_100.name === '--pf-global--BackgroundColor--100'; // true
global_BackgroundColor_100.value === '#fff'; // true
global_BackgroundColor_100.var === 'var(--pf-global--BackgroundColor--100)'; // true
```

[token-page]: https://patternfly-react.surge.sh/developer-resources/global-css-variables

