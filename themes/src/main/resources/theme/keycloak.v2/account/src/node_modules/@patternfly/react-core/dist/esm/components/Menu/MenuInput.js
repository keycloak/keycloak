import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Menu/menu';
import { css } from '@patternfly/react-styles';
export const MenuInput = React.forwardRef((props, ref) => (React.createElement("div", Object.assign({}, props, { className: css(styles.menuSearch, props.className), ref: ref }))));
MenuInput.displayName = 'MenuInput';
//# sourceMappingURL=MenuInput.js.map