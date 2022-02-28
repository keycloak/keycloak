import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { DropdownContext, DropdownArrowContext } from './dropdownConstants';
import { InternalDropdownItem } from './InternalDropdownItem';
export const DropdownSeparator = (_ref) => {
  let {
    className = '',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    ref
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "ref"]);

  return React.createElement(DropdownContext.Consumer, null, ({
    separatorClass
  }) => React.createElement(DropdownArrowContext.Consumer, null, context => React.createElement(InternalDropdownItem, _extends({}, props, {
    context: context,
    className: css(separatorClass, className),
    component: "div",
    role: "separator"
  }))));
};
DropdownSeparator.propTypes = {
  className: _pt.string,
  onClick: _pt.func
};
//# sourceMappingURL=DropdownSeparator.js.map