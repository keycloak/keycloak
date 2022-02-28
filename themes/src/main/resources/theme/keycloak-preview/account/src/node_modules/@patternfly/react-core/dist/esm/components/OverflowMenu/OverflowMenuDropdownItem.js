import _pt from "prop-types";
import * as React from 'react';
import { DropdownItem } from '../Dropdown';
import { OverflowMenuContext } from './OverflowMenuContext';
export const OverflowMenuDropdownItem = ({
  children,
  isShared = false
}) => React.createElement(OverflowMenuContext.Consumer, null, value => (!isShared || value.isBelowBreakpoint) && React.createElement(DropdownItem, {
  component: "button"
}, " ", children, " "));
OverflowMenuDropdownItem.propTypes = {
  children: _pt.any,
  isShared: _pt.bool
};
//# sourceMappingURL=OverflowMenuDropdownItem.js.map