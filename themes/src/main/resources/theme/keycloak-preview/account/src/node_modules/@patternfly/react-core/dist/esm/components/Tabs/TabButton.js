import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';

const TabButtonBase = (_ref) => {
  let {
    children,
    className = '',
    tabContentRef
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "tabContentRef"]);

  const Component = props.href ? 'a' : 'button';
  return React.createElement(Component, _extends({}, props, {
    className: className,
    ref: tabContentRef
  }), children);
};

TabButtonBase.propTypes = {
  children: _pt.node,
  className: _pt.string,
  href: _pt.string,
  tabContentRef: _pt.oneOfType([_pt.string, _pt.func, _pt.object])
};
export const TabButton = React.forwardRef((props, ref) => React.createElement(TabButtonBase, _extends({}, props, {
  tabContentRef: ref
}))); // export const TabButton = withForwardedRef(TabButtonWithRef);
//# sourceMappingURL=TabButton.js.map