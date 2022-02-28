import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { Title, TitleLevel } from '../Title';
export const ModalBoxHeader = (_ref) => {
  let {
    children = null,
    className = '',
    hideTitle = false,
    headingLevel = TitleLevel.h1
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "hideTitle", "headingLevel"]);

  return hideTitle ? null : React.createElement(React.Fragment, null, React.createElement(Title, _extends({
    size: "2xl",
    headingLevel: headingLevel,
    className: className
  }, props), children));
};
ModalBoxHeader.propTypes = {
  children: _pt.node,
  className: _pt.string,
  hideTitle: _pt.bool,
  headingLevel: _pt.oneOf(['h1', 'h2', 'h3', 'h4', 'h5', 'h6'])
};
//# sourceMappingURL=ModalBoxHeader.js.map