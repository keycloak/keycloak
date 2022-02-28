import _pt from "prop-types";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/OverflowMenu/overflow-menu';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuContent = ({
  className,
  children,
  isPersistent
}) => React.createElement(OverflowMenuContext.Consumer, null, value => (!value.isBelowBreakpoint || isPersistent) && React.createElement("div", {
  className: css(styles.overflowMenuContent, className)
}, children));
OverflowMenuContent.propTypes = {
  children: _pt.any,
  className: _pt.string,
  isPersistent: _pt.bool
};
//# sourceMappingURL=OverflowMenuContent.js.map