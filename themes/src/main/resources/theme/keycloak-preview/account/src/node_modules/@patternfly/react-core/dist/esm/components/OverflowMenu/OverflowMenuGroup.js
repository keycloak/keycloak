import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuGroup = ({
  className,
  children,
  isPersistent = false,
  groupType
}) => React.createElement(OverflowMenuContext.Consumer, null, value => (isPersistent || !value.isBelowBreakpoint) && React.createElement("div", {
  className: css(styles.overflowMenuGroup, groupType === 'button' && styles.modifiers.buttonGroup, groupType === 'icon' && styles.modifiers.iconButtonGroup, className)
}, children));
OverflowMenuGroup.propTypes = {
  children: _pt.any,
  className: _pt.string,
  isPersistent: _pt.bool,
  groupType: _pt.oneOf(['button', 'icon'])
};
//# sourceMappingURL=OverflowMenuGroup.js.map