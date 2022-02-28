import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DataToolbar/data-toolbar';
import { css } from '@patternfly/react-styles';
import { DataToolbarContext } from './DataToolbarUtils';
import { DataToolbarChipGroupContent } from './DataToolbarChipGroupContent';
export class DataToolbar extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "chipGroupContentRef", React.createRef());

    _defineProperty(this, "isToggleManaged", () => !(this.props.isExpanded || !!this.props.toggleIsExpanded));

    _defineProperty(this, "toggleIsExpanded", () => {
      this.setState(prevState => ({
        isManagedToggleExpanded: !prevState.isManagedToggleExpanded
      }));
    });

    _defineProperty(this, "closeExpandableContent", () => {
      this.setState(() => ({
        isManagedToggleExpanded: false
      }));
    });

    _defineProperty(this, "updateNumberFilters", (categoryName, numberOfFilters) => {
      const filterInfoToUpdate = _objectSpread({}, this.state.filterInfo);

      if (!filterInfoToUpdate.hasOwnProperty(categoryName) || filterInfoToUpdate[categoryName] !== numberOfFilters) {
        filterInfoToUpdate[categoryName] = numberOfFilters;
        this.setState({
          filterInfo: filterInfoToUpdate
        });
      }
    });

    _defineProperty(this, "getNumberOfFilters", () => Object.values(this.state.filterInfo).reduce((acc, cur) => acc + cur, 0));

    this.state = {
      isManagedToggleExpanded: false,
      filterInfo: {}
    };
  }

  componentDidMount() {
    if (this.isToggleManaged()) {
      window.addEventListener('resize', this.closeExpandableContent);
    }

    if (process.env.NODE_ENV !== 'production' && !DataToolbar.hasWarnBeta) {
      // eslint-disable-next-line no-console
      console.warn('You are using a beta component (DataToolbar). These api parts are subject to change in the future.');
      DataToolbar.hasWarnBeta = true;
    }
  }

  componentWillUnmount() {
    if (this.isToggleManaged()) {
      window.removeEventListener('resize', this.closeExpandableContent);
    }
  }

  render() {
    const _this$props = this.props,
          {
      clearAllFilters,
      clearFiltersButtonText,
      collapseListedFiltersBreakpoint,
      isExpanded,
      toggleIsExpanded,
      className,
      children,
      id
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["clearAllFilters", "clearFiltersButtonText", "collapseListedFiltersBreakpoint", "isExpanded", "toggleIsExpanded", "className", "children", "id"]);

    const {
      isManagedToggleExpanded
    } = this.state;
    const isToggleManaged = this.isToggleManaged();
    const numberOfFilters = this.getNumberOfFilters();
    const showClearFiltersButton = numberOfFilters > 0;
    return React.createElement("div", _extends({
      className: css(styles.dataToolbar, className),
      id: id
    }, props), React.createElement(DataToolbarContext.Provider, {
      value: {
        isExpanded: this.isToggleManaged() ? isManagedToggleExpanded : isExpanded,
        toggleIsExpanded: isToggleManaged ? this.toggleIsExpanded : toggleIsExpanded,
        chipGroupContentRef: this.chipGroupContentRef,
        updateNumberFilters: this.updateNumberFilters,
        numberOfFilters
      }
    }, React.Children.map(children, child => {
      if (React.isValidElement(child)) {
        return React.cloneElement(child, {
          clearAllFilters,
          clearFiltersButtonText,
          showClearFiltersButton,
          isExpanded: isToggleManaged ? isManagedToggleExpanded : isExpanded,
          toolbarId: id
        });
      } else {
        return child;
      }
    }), React.createElement(DataToolbarChipGroupContent, {
      isExpanded: isToggleManaged ? isManagedToggleExpanded : isExpanded,
      chipGroupContentRef: this.chipGroupContentRef,
      clearAllFilters: clearAllFilters,
      showClearFiltersButton: showClearFiltersButton,
      clearFiltersButtonText: clearFiltersButtonText,
      numberOfFilters: numberOfFilters,
      collapseListedFiltersBreakpoint: collapseListedFiltersBreakpoint
    })));
  }

}

_defineProperty(DataToolbar, "propTypes", {
  clearAllFilters: _pt.func,
  clearFiltersButtonText: _pt.string,
  collapseListedFiltersBreakpoint: _pt.oneOf(['md', 'lg', 'xl', '2xl']),
  isExpanded: _pt.bool,
  toggleIsExpanded: _pt.func,
  className: _pt.string,
  children: _pt.node,
  id: _pt.string.isRequired
});

_defineProperty(DataToolbar, "hasWarnBeta", false);
//# sourceMappingURL=DataToolbar.js.map