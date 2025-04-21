# @patternfly/react-styles

Library that provides CSS-in-JS capabilities 

#### Example

```jsx
import { css } from '@patternfly/react-styles';
import styles from './Button.css';

const Buttton = ({ isActive, isDisabled, children }) => (
  <button
    disabled={isDisabled}
    className={css(styles.button, isActive && styles.modifiers.active, isDisabled && styles.modifiers.disabled)}
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

[classnames]: https://github.com/JedWatson/classnames
[css-modules]: https://github.com/css-modules/css-modules
[babel]: https://github.com/babel/babel
[jest-styled-components]: https://github.com/styled-components/jest-styled-components#snapshot-testing
[jest-glamor-react]: https://github.com/kentcdodds/jest-glamor-react
[jest-aphrodite-react]: https://github.com/dmiller9911/jest-aphrodite-react
