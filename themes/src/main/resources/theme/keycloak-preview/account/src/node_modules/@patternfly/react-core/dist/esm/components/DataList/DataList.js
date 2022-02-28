import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
export const DataListContext = React.createContext({
  isSelectable: false
});
export const DataList = (_ref) => {
  let {
    children = null,
    className = '',
    'aria-label': ariaLabel,
    selectedDataListItemId = '',
    onSelectDataListItem,
    isCompact = false
  } = _ref,
      props = _objectWithoutProperties(_ref, ["children", "className", "aria-label", "selectedDataListItemId", "onSelectDataListItem", "isCompact"]);

  const isSelectable = onSelectDataListItem !== undefined;

  const updateSelectedDataListItem = id => {
    onSelectDataListItem(id);
  };

  return React.createElement(DataListContext.Provider, {
    value: {
      isSelectable,
      selectedDataListItemId,
      updateSelectedDataListItem
    }
  }, React.createElement("ul", _extends({
    className: css(styles.dataList, isCompact && styles.modifiers.compact, className),
    "aria-label": ariaLabel
  }, props), children));
};
DataList.propTypes = {
  children: _pt.node,
  className: _pt.string,
  'aria-label': _pt.string.isRequired,
  onSelectDataListItem: _pt.func,
  selectedDataListItemId: _pt.string,
  isCompact: _pt.bool
};
//# sourceMappingURL=DataList.js.map