import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Nav/nav';
import { css } from '@patternfly/react-styles';
import { getUniqueId } from '../../helpers/util';
export const NavGroup = (_ref) => {
  let {
    title,
    children = null,
    className = '',
    id = getUniqueId()
  } = _ref,
      props = _objectWithoutProperties(_ref, ["title", "children", "className", "id"]);

  return React.createElement("section", _extends({
    className: css(styles.navSection, className),
    "aria-labelledby": id
  }, props), React.createElement("h2", {
    className: css(styles.navSectionTitle),
    id: id
  }, title), React.createElement("ul", {
    className: css(styles.navList)
  }, children));
};
NavGroup.propTypes = {
  title: _pt.string.isRequired,
  children: _pt.node,
  className: _pt.string,
  id: _pt.string
};
//# sourceMappingURL=NavGroup.js.map