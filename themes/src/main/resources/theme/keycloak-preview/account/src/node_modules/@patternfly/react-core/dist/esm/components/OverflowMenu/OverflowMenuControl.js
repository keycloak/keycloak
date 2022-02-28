import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuControl = ({
  className,
  children,
  hasAdditionalOptions
}) => React.createElement(OverflowMenuContext.Consumer, null, value => (value.isBelowBreakpoint || hasAdditionalOptions) && React.createElement("div", {
  className: css(styles.overflowMenuControl, className)
}, " ", children, " "));
OverflowMenuControl.propTypes = {
  children: _pt.any,
  className: _pt.string,
  hasAdditionalOptions: _pt.bool
};
//# sourceMappingURL=OverflowMenuControl.js.map