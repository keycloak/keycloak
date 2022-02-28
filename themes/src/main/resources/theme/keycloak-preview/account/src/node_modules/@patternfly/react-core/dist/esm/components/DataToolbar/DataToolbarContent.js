import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css } from '@patternfly/react-styles';
import { DataToolbarContentContext } from './DataToolbarUtils';
import { formatBreakpointMods } from '../../helpers/util';
import { DataToolbarExpandableContent } from './DataToolbarExpandableContent';
export class DataToolbarContent extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "expandableContentRef", React.createRef());

    _defineProperty(this, "chipContainerRef", React.createRef());
  }

  render() {
    const _this$props = this.props,
          {
      className,
      children,
      isExpanded,
      toolbarId,
      breakpointMods,
      clearAllFilters,
      showClearFiltersButton,
      clearFiltersButtonText
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["className", "children", "isExpanded", "toolbarId", "breakpointMods", "clearAllFilters", "showClearFiltersButton", "clearFiltersButtonText"]);

    const expandableContentId = `${toolbarId}-expandable-content-${DataToolbarContent.currentId++}`;
    return React.createElement("div", _extends({
      className: css(styles.dataToolbarContent, formatBreakpointMods(breakpointMods, styles), className)
    }, props), React.createElement(DataToolbarContentContext.Provider, {
      value: {
        expandableContentRef: this.expandableContentRef,
        expandableContentId,
        chipContainerRef: this.chipContainerRef
      }
    }, React.createElement("div", {
      className: css(styles.dataToolbarContentSection)
    }, children), React.createElement(DataToolbarExpandableContent, {
      id: expandableContentId,
      isExpanded: isExpanded,
      expandableContentRef: this.expandableContentRef,
      chipContainerRef: this.chipContainerRef,
      clearAllFilters: clearAllFilters,
      showClearFiltersButton: showClearFiltersButton,
      clearFiltersButtonText: clearFiltersButtonText
    })));
  }

}

_defineProperty(DataToolbarContent, "propTypes", {
  className: _pt.string,
  breakpointMods: _pt.arrayOf(_pt.any),
  children: _pt.node,
  isExpanded: _pt.bool,
  clearAllFilters: _pt.func,
  showClearFiltersButton: _pt.bool,
  clearFiltersButtonText: _pt.string,
  toolbarId: _pt.string
});

_defineProperty(DataToolbarContent, "currentId", 0);

_defineProperty(DataToolbarContent, "defaultProps", {
  isExpanded: false,
  breakpointMods: [],
  showClearFiltersButton: false
});
//# sourceMappingURL=DataToolbarContent.js.map