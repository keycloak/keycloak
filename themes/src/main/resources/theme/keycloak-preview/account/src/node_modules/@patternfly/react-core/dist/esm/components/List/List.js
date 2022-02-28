import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/List/list';
import { css, getModifier } from '@patternfly/react-styles';
export let OrderType;

(function (OrderType) {
  OrderType["number"] = "1";
  OrderType["lowercaseLetter"] = "a";
  OrderType["uppercaseLetter"] = "A";
  OrderType["lowercaseRomanNumber"] = "i";
  OrderType["uppercaseRomanNumber"] = "I";
})(OrderType || (OrderType = {}));

export let ListVariant;

(function (ListVariant) {
  ListVariant["inline"] = "inline";
})(ListVariant || (ListVariant = {}));

export let ListComponent;

(function (ListComponent) {
  ListComponent["ol"] = "ol";
  ListComponent["ul"] = "ul";
})(ListComponent || (ListComponent = {}));

export const List = (_ref) => {
  let {
    className = '',
    children = null,
    variant = null,
    type = OrderType.number,
    ref = null,
    component = ListComponent.ul
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "children", "variant", "type", "ref", "component"]);

  return component === ListComponent.ol ? React.createElement("ol", _extends({
    ref: ref,
    type: type
  }, props, {
    className: css(styles.list, variant && getModifier(styles.modifiers, variant), className)
  }), children) : React.createElement("ul", _extends({
    ref: ref
  }, props, {
    className: css(styles.list, variant && getModifier(styles.modifiers, variant), className)
  }), children);
};
List.propTypes = {
  children: _pt.node,
  className: _pt.string,
  variant: _pt.any,
  type: _pt.any,
  component: _pt.oneOf(['ol', 'ul'])
};
//# sourceMappingURL=List.js.map