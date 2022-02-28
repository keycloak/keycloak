import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { Button } from '../Button';
import TimesIcon from '@patternfly/react-icons/dist/js/icons/times-icon';
export const ModalBoxCloseButton = (_ref) => {
  let {
    className = '',
    onClose = () => undefined
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "onClose"]);

  return React.createElement(Button, _extends({
    className: className,
    variant: "plain",
    onClick: onClose,
    "aria-label": "Close"
  }, props), React.createElement(TimesIcon, null));
};
ModalBoxCloseButton.propTypes = {
  className: _pt.string,
  onClose: _pt.func
};
//# sourceMappingURL=ModalBoxCloseButton.js.map