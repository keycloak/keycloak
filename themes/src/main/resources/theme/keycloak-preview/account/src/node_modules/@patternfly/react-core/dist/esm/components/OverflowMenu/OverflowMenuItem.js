import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuItem = ({
  className,
  children,
  isPersistent = false
}) => React.createElement(OverflowMenuContext.Consumer, null, value => (isPersistent || !value.isBelowBreakpoint) && React.createElement("div", {
  className: css(styles.overflowMenuItem, className)
}, " ", children, " "));
OverflowMenuItem.propTypes = {
  children: _pt.any,
  className: _pt.string,
  isPersistent: _pt.bool
};
//# sourceMappingURL=OverflowMenuItem.js.map