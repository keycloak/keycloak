import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import AngleRightIcon from '@patternfly/react-icons/dist/js/icons/angle-right-icon';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { Button, ButtonVariant } from '../Button';
export const DataListToggle = (_ref) => {
  let {
    className = '',
    isExpanded = false,
    'aria-controls': ariaControls = '',
    'aria-label': ariaLabel = 'Details',
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    'aria-labelledby': ariaLabelledBy = '',
    rowid = '',
    id
  } = _ref,
      props = _objectWithoutProperties(_ref, ["className", "isExpanded", "aria-controls", "aria-label", "aria-labelledby", "rowid", "id"]);

  return React.createElement("div", _extends({
    className: css(styles.dataListItemControl, className)
  }, props), React.createElement("div", {
    className: css(styles.dataListToggle)
  }, React.createElement(Button, {
    id: id,
    variant: ButtonVariant.plain,
    "aria-controls": ariaControls !== '' && ariaControls,
    "aria-label": ariaLabel,
    "aria-labelledby": ariaLabel !== 'Details' ? null : `${rowid} ${id}`,
    "aria-expanded": isExpanded
  }, React.createElement(AngleRightIcon, null))));
};
DataListToggle.propTypes = {
  className: _pt.string,
  isExpanded: _pt.bool,
  id: _pt.string.isRequired,
  rowid: _pt.string,
  'aria-labelledby': _pt.string,
  'aria-label': _pt.string,
  'aria-controls': _pt.string
};
//# sourceMappingURL=DataListToggle.js.map