import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/DataList/data-list';
import { DataListContext } from './DataList';
import { KeyTypes } from '../Select';
export const DataListItem = (_ref) => {
  let {
    isExpanded = false,
    className = '',
    id = '',
    'aria-labelledby': ariaLabelledBy,
    children
  } = _ref,
      props = _objectWithoutProperties(_ref, ["isExpanded", "className", "id", "aria-labelledby", "children"]);

  return React.createElement(DataListContext.Consumer, null, ({
    isSelectable,
    selectedDataListItemId,
    updateSelectedDataListItem
  }) => {
    const selectDataListItem = event => {
      let target = event.target;

      while (event.currentTarget !== target) {
        if ('onclick' in target && target.onclick || target.parentNode.classList.contains(styles.dataListItemAction) || target.parentNode.classList.contains(styles.dataListItemControl)) {
          // check other event handlers are not present.
          return;
        } else {
          target = target.parentNode;
        }
      }

      updateSelectedDataListItem(id);
    };

    const onKeyDown = event => {
      if (event.key === KeyTypes.Enter) {
        updateSelectedDataListItem(id);
      }
    };

    return React.createElement("li", _extends({
      id: id,
      className: css(styles.dataListItem, isExpanded && styles.modifiers.expanded, isSelectable && styles.modifiers.selectable, selectedDataListItemId && selectedDataListItemId === id && styles.modifiers.selected, className),
      "aria-labelledby": ariaLabelledBy
    }, isSelectable && {
      tabIndex: 0,
      onClick: selectDataListItem,
      onKeyDown
    }, isSelectable && selectedDataListItemId === id && {
      'aria-selected': true
    }, props), React.Children.map(children, child => React.isValidElement(child) && React.cloneElement(child, {
      rowid: ariaLabelledBy
    })));
  });
};
DataListItem.propTypes = {
  isExpanded: _pt.bool,
  children: _pt.node.isRequired,
  className: _pt.string,
  'aria-labelledby': _pt.string.isRequired,
  id: _pt.string
};
//# sourceMappingURL=DataListItem.js.map