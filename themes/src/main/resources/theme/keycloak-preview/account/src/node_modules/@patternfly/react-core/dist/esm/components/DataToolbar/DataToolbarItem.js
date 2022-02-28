import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';
import { formatBreakpointMods } from '../../helpers/util';
export let DataToolbarItemVariant;

(function (DataToolbarItemVariant) {
  DataToolbarItemVariant["separator"] = "separator";
  DataToolbarItemVariant["bulk-select"] = "bulk-select";
  DataToolbarItemVariant["overflow-menu"] = "overflow-menu";
  DataToolbarItemVariant["pagination"] = "pagination";
  DataToolbarItemVariant["search-filter"] = "search-filter";
  DataToolbarItemVariant["label"] = "label";
  DataToolbarItemVariant["chip-group"] = "chip-group";
})(DataToolbarItemVariant || (DataToolbarItemVariant = {}));

export const DataToolbarItem = (_ref) => {
  let {
    className,
    variant,
    breakpointMods = [],
    id,
    children
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "variant", "breakpointMods", "id", "children"]);

  const labelVariant = variant === 'label';
  return React.createElement("div", _extends({
    className: css(styles.dataToolbarItem, variant && getModifier(styles, variant), formatBreakpointMods(breakpointMods, styles), className)
  }, labelVariant && {
    'aria-hidden': true
  }, {
    id: id
  }, props), children);
};
DataToolbarItem.propTypes = {
  className: _pt.string,
  variant: _pt.oneOfType([_pt.any, _pt.oneOf(['separator']), _pt.oneOf(['bulk-select']), _pt.oneOf(['overflow-menu']), _pt.oneOf(['pagination']), _pt.oneOf(['search-filter']), _pt.oneOf(['label']), _pt.oneOf(['chip-group'])]),
  breakpointMods: _pt.arrayOf(_pt.any),
  id: _pt.string,
  children: _pt.node
};
//# sourceMappingURL=DataToolbarItem.js.map