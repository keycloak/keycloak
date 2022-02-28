import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { InternalDropdownItem } from './InternalDropdownItem';
import { DropdownArrowContext } from './dropdownConstants';
export const DropdownItem = (_ref) => {
  let {
    children = null,
    className = '',
    component = 'a',
    variant = 'item',
    isDisabled = false,
    isHovered = false,
    href,
    tooltip = null,
    tooltipProps = {},
    listItemClassName,
    onClick,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref,
    // Types of Ref are different for React.FC vs React.Component
    additionalChild,
    customChild
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "component", "variant", "isDisabled", "isHovered", "href", "tooltip", "tooltipProps", "listItemClassName", "onClick", "ref", "additionalChild", "customChild"]);

  return React.createElement(DropdownArrowContext.Consumer, null, context => React.createElement(InternalDropdownItem, _extends({
    context: context,
    role: "menuitem",
    tabIndex: -1,
    className: className,
    component: component,
    variant: variant,
    isDisabled: isDisabled,
    isHovered: isHovered,
    href: href,
    tooltip: tooltip,
    tooltipProps: tooltipProps,
    listItemClassName: listItemClassName,
    onClick: onClick,
    additionalChild: additionalChild,
    customChild: customChild
  }, props), children));
};
DropdownItem.propTypes = {
  children: _pt.node,
  className: _pt.string,
  listItemClassName: _pt.string,
  component: _pt.node,
  variant: _pt.oneOf(['item', 'icon']),
  isDisabled: _pt.bool,
  isHovered: _pt.bool,
  href: _pt.string,
  tooltip: _pt.node,
  tooltipProps: _pt.any,
  additionalChild: _pt.node,
  customChild: _pt.node
};
//# sourceMappingURL=DropdownItem.js.map