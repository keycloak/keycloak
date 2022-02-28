import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';
import { DataToolbarGroup } from './DataToolbarGroup';
import { DataToolbarItem } from './DataToolbarItem';
import { Button } from '../../components/Button';
import { DataToolbarContext } from './DataToolbarUtils';
export class DataToolbarExpandableContent extends React.Component {
  render() {
    const _this$props = this.props,
          {
      className,
      expandableContentRef,
      chipContainerRef,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      isExpanded,
      clearAllFilters,
      clearFiltersButtonText,
      showClearFiltersButton
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "expandableContentRef", "chipContainerRef", "isExpanded", "clearAllFilters", "clearFiltersButtonText", "showClearFiltersButton"]);

    const {
      numberOfFilters
    } = this.context;

    const clearChipGroups = () => {
      clearAllFilters();
    };

    return React.createElement("div", _extends({
      className: css(styles.dataToolbarExpandableContent, className),
      ref: expandableContentRef
    }, props), React.createElement(DataToolbarGroup, null), numberOfFilters > 0 && React.createElement(DataToolbarGroup, {
      className: getModifier(styles, 'chip-container')
    }, React.createElement(DataToolbarGroup, {
      ref: chipContainerRef
    }), showClearFiltersButton && React.createElement(DataToolbarItem, {
      className: css(getModifier(styles, 'clear'))
    }, React.createElement(Button, {
      variant: "link",
      onClick: clearChipGroups,
      isInline: true
    }, clearFiltersButtonText))));
  }

}

_defineProperty(DataToolbarExpandableContent, "propTypes", {
  className: _pt.string,
  isExpanded: _pt.bool,
  expandableContentRef: _pt.any,
  chipContainerRef: _pt.any,
  clearAllFilters: _pt.func,
  clearFiltersButtonText: _pt.string,
  showClearFiltersButton: _pt.bool.isRequired
});

_defineProperty(DataToolbarExpandableContent, "contextType", DataToolbarContext);

_defineProperty(DataToolbarExpandableContent, "defaultProps", {
  isExpanded: false,
  clearFiltersButtonText: 'Clear all filters'
});
//# sourceMappingURL=DataToolbarExpandableContent.js.map