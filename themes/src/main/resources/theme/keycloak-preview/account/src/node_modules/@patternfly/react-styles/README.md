# @patternfly/react-styles

Library that provides CSS-in-JS capabilities along with build plugins to convert raw css imports to a consumable form for JS. This approach is very similar to how [css-modules][css-modules] works.

## Getting started

This library has 3 main parts.

1. A [babel][babel] plugin to transform css imports into a JS import
1. A `StyleSheet` helper that parses raw css and returns a JS object to reference classnames.
1. A `css` helper function to combine string CSS classes and any returned from the `StyleSheet`. It also takes care of doing the CSS injection.

## Detailed design

### `StyleSheet.parse(cssString): { [key: string]: PFStyleObject }`

Parses a string of CSS and extracts classes out so that they can be referenced from an object instead of as a string value. CSS is injected through the `css` utility. The keys provided are a mapping to a camel-cased version of the className with `pf-(l|c|p)-` removed.

pf-c-button --> button\
pf-is-primary --> isPrimary\
pf-l-grid --> grid\

Any modifiers are placed under a nested property `modifiers`:

pf-m-active --> modifiers.active
pf-m-block --> modifiers.block

#### Example

## Examples
```js
import { StyleSheet, css } from '@patternfly/react-styles';

const styles = StyleSheet.parse(`
  .pf-c-button { background: green }
  .pf-m-active { background: red }
`);

const btn = document.createElement('button');
btn.classList.add(css(styles.button, styles.modifiers.active));
// <button class="pf-c-button pf-is-active" />

// If you just need to inject all of the styles manually you can do this by calling the inject method on the styles object.
// Note: using css() does this in a more efficient manner and this should be only be used as an escape hatch.
styles.inject();
```

### `StyleSheet.create({ [key: string]: object | string | Array<object> }): { [key: string]: string }`

StyleSheet.create takes an object with each property calling `css` from emotion. This is largely provided for backwards compatibility, and will likely be removed in the future.

#### Example

```js
import { StyleSheet } from '@patternfly/react-styles';

const styles = StyleSheet.create({
  first: { backgroundColor: 'red' },
  second: `background-color: red`,
  third: [{ color: 'red' }, { backgroundColor: 'green' }]
});
```

For more info on how each property is handled see [emotion css docs](https://emotion.sh/docs/css).

### `css(...styles: Array<PFStyleObject | string | void>): string`

Joins classes together into a single space-delimited string. If a `PFStyleObject` or a result from `StyleSheet.create` is passed it will inject the CSS related to that object. This is similar to the [classnames][classnames] package.

#### Example

```jsx
import { css } from '@patternfly/react-styles';
import styles from './Button.css';

const Buttton = ({ isActive, isDisabled, children }) => (
  <button
    disabled={isDisabled}
    className={css(styles.button, isActive && styles.isActive, isDisabled && styles.isDisabled)}
  >
    {children}
  </button>
);
```

##### DOM output

```html
<button disabled="" class="pf-c-button pf-is-disabled">
  Hello World
</button>
```

### `getModifier(styles: { [key: string]: PFStyleObject }, modifier: string, defaultModifer?: string): PFStyleObject | null;`

Since PatternFly 4 Core is maintaining a pattern of using `pf-m-modifier` for modifiers we will provide a utility for consumers to easily get the modifier given the style object and the desired modifier. A default can be provided as well if the given variant does not exist. Returns `null` if none are found.

#### Example

```jsx
const styles = StyleSheet.parse(`
  .button {}
  .pf-m-secondary {}
  .pf-m-primary {}
`);

const Button = ({
  variant, // primary | secondary
  children,
}) => (
  <button
    className={css(
      styles.button,
      getModifier(styles, variant, 'primary'),
    )}
  >
    {children}
  </button>
);
```

### Server Rendering

Since the css is referenced from JS server rendering is supported. For an example of this see: [gatsby-ssr.js](../site/gatsby-ssr.js)

### Snapshot Testing

This package exports a snapshot serializer to produce more useful snapshots. Below is an example

#### Before

```
exports[`primary button 1`] = `
<button
  className="pf-c-button pf-m-primary"
  disabled={false}
  type="button"
/>
`;
```

#### After

```
exports[`primary button 1`] = `
.pf-c-button.pf-m-primary {
  display: inline-block;
  padding: 0.25rem 1.5rem 0.25rem 1.5rem;
  font-size: 1rem;
  font-weight: 400;
  line-height: 1.5;
  text-align: center;
  white-space: nowrap;
  background-color: #00659c;
  border: 0px;
  border-radius: 30em;
  box-shadow: inset 0 0 0 2px #00659c;
  color: #ffffff;
}

<button
  className="pf-c-button pf-m-primary"
  disabled={false}
  type="button"
/>
`;
```

Now if the background-color is changed the snapshot will fail, and your will see an output similar to below.

```diff
- Snapshot
+ Received
 .pf-c-button.pf-m-primary {
   display: inline-block;
   padding: 0.25rem 1.5rem 0.25rem 1.5rem;
   font-size: 1rem;
   font-weight: 400;
   line-height: 1.5;
   text-align: center;
   white-space: nowrap;
-  background-color: #00659c;
+  background-color: green;
   border: 0px;
   border-radius: 30em;
   box-shadow: inset 0 0 0 2px #00659c;
   color: #ffffff;
 }

 <button
   className="pf-c-button pf-m-primary"
   disabled={false}
   type="button"
 />
```

This is similar to the utilities [jest-aphrodite-react][jest-aphrodite-react], [jest-styled-components][jest-styled-components], and [jest-glamor-react][jest-glamor-react]

[classnames]: https://github.com/JedWatson/classnames
[css-modules]: https://github.com/css-modules/css-modules
[babel]: https://github.com/babel/babel
[jest-styled-components]: https://github.com/styled-components/jest-styled-components#snapshot-testing
[jest-glamor-react]: https://github.com/kentcdodds/jest-glamor-react
[jest-aphrodite-react]: https://github.com/dmiller9911/jest-aphrodite-react
