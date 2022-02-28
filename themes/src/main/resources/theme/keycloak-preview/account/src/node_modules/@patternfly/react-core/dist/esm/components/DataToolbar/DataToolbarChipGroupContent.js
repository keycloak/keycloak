import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css, getModifier } from '@patternfly/react-styles';
import { DataToolbarItem } from './DataToolbarItem';
import { Button } from '../../components/Button';
import { DataToolbarGroup } from './DataToolbarGroup';
import { globalBreakpoints } from './DataToolbarUtils';
export class DataToolbarChipGroupContent extends React.Component {
  render() {
    const _this$props = this.props,
          {
      className,
      isExpanded,
      chipGroupContentRef,
      clearAllFilters,
      showClearFiltersButton,
      clearFiltersButtonText,
      collapseListedFiltersBreakpoint,
      numberOfFilters
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "isExpanded", "chipGroupContentRef", "clearAllFilters", "showClearFiltersButton", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "numberOfFilters"]);

    const clearChipGroups = () => {
      clearAllFilters();
    };

    const collapseListedFilters = typeof window !== 'undefined' ? window.innerWidth < globalBreakpoints(collapseListedFiltersBreakpoint) : false;
    return React.createElement("div", _extends({
      className: css(styles.dataToolbarContent, (numberOfFilters === 0 || isExpanded) && getModifier(styles, 'hidden'), className)
    }, (numberOfFilters === 0 || isExpanded) && {
      hidden: true
    }, {
      ref: chipGroupContentRef
    }, props), React.createElement(DataToolbarGroup, _extends({
      className: css(collapseListedFilters && getModifier(styles, 'hidden'))
    }, collapseListedFilters && {
      hidden: true
    }, collapseListedFilters && {
      'aria-hidden': true
    })), collapseListedFilters && numberOfFilters > 0 && !isExpanded && React.createElement(DataToolbarGroup, {
      className: css(getModifier(styles, 'toggle-group-summary'), 'pf-m-filters-applied-message')
    }, React.createElement(DataToolbarItem, null, numberOfFilters, " filters applied")), showClearFiltersButton && !isExpanded && React.createElement(DataToolbarItem, {
      className: css(getModifier(styles, 'clear'))
    }, React.createElement(Button, {
      variant: "link",
      onClick: clearChipGroups,
      isInline: true
    }, clearFiltersButtonText)));
  }

}

_defineProperty(DataToolbarChipGroupContent, "propTypes", {
  className: _pt.string,
  isExpanded: _pt.bool,
  chipGroupContentRef: _pt.any,
  clearAllFilters: _pt.func,
  showClearFiltersButton: _pt.bool.isRequired,
  clearFiltersButtonText: _pt.string,
  numberOfFilters: _pt.number.isRequired,
  collapseListedFiltersBreakpoint: _pt.oneOf(['md', 'lg', 'xl', '2xl'])
});

_defineProperty(DataToolbarChipGroupContent, "defaultProps", {
  clearFiltersButtonText: 'Clear all filters',
  collapseListedFiltersBreakpoint: 'lg'
});
//# sourceMappingURL=DataToolbarChipGroupContent.js.map