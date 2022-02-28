import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/ContextSelector/context-selector';
import { css } from '@patternfly/react-styles';
import SearchIcon from '@patternfly/react-icons/dist/js/icons/search-icon';
import { ContextSelectorToggle } from './ContextSelectorToggle';
import { ContextSelectorMenuList } from './ContextSelectorMenuList';
import { ContextSelectorContext } from './contextSelectorConstants';
import { Button, ButtonVariant } from '../Button';
import { TextInput } from '../TextInput';
import { InputGroup } from '../InputGroup';
import { KEY_CODES } from '../../helpers/constants';
import { FocusTrap } from '../../helpers'; // seed for the aria-labelledby ID

let currentId = 0;
const newId = currentId++;
export class ContextSelector extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "parentRef", React.createRef());

    _defineProperty(this, "onEnterPressed", event => {
      if (event.charCode === KEY_CODES.ENTER) {
        this.props.onSearchButtonClick();
      }
    });
  }

  render() {
    const toggleId = `pf-context-selector-toggle-id-${newId}`;
    const screenReaderLabelId = `pf-context-selector-label-id-${newId}`;
    const searchButtonId = `pf-context-selector-search-button-id-${newId}`;

    const _this$props = this.props,
          {
      children,
      className,
      isOpen,
      onToggle,
      onSelect,
      screenReaderLabel,
      toggleText,
      searchButtonAriaLabel,
      searchInputValue,
      onSearchInputChange,
      searchInputPlaceholder,
      onSearchButtonClick
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "isOpen", "onToggle", "onSelect", "screenReaderLabel", "toggleText", "searchButtonAriaLabel", "searchInputValue", "onSearchInputChange", "searchInputPlaceholder", "onSearchButtonClick"]);

    return React.createElement("div", _extends({
      className: css(styles.contextSelector, isOpen && styles.modifiers.expanded, className),
      ref: this.parentRef
    }, props), screenReaderLabel && React.createElement("span", {
      id: screenReaderLabelId,
      hidden: true
    }, screenReaderLabel), React.createElement(ContextSelectorToggle, {
      onToggle: onToggle,
      isOpen: isOpen,
      toggleText: toggleText,
      id: toggleId,
      parentRef: this.parentRef.current,
      "aria-labelledby": `${screenReaderLabelId} ${toggleId}`
    }), isOpen && React.createElement("div", {
      className: css(styles.contextSelectorMenu)
    }, isOpen && React.createElement(FocusTrap, {
      focusTrapOptions: {
        clickOutsideDeactivates: true
      }
    }, React.createElement("div", {
      className: css(styles.contextSelectorMenuInput)
    }, React.createElement(InputGroup, null, React.createElement(TextInput, {
      value: searchInputValue,
      type: "search",
      placeholder: searchInputPlaceholder,
      onChange: onSearchInputChange,
      onKeyPress: this.onEnterPressed,
      "aria-labelledby": searchButtonId
    }), React.createElement(Button, {
      variant: ButtonVariant.control,
      "aria-label": searchButtonAriaLabel,
      id: searchButtonId,
      onClick: onSearchButtonClick
    }, React.createElement(SearchIcon, {
      "aria-hidden": "true"
    })))), React.createElement(ContextSelectorContext.Provider, {
      value: {
        onSelect
      }
    }, React.createElement(ContextSelectorMenuList, {
      isOpen: isOpen
    }, children)))));
  }

}

_defineProperty(ContextSelector, "propTypes", {
  children: _pt.node,
  className: _pt.string,
  isOpen: _pt.bool,
  onToggle: _pt.func,
  onSelect: _pt.func,
  screenReaderLabel: _pt.string,
  toggleText: _pt.string,
  searchButtonAriaLabel: _pt.string,
  searchInputValue: _pt.string,
  onSearchInputChange: _pt.func,
  searchInputPlaceholder: _pt.string,
  onSearchButtonClick: _pt.func
});

_defineProperty(ContextSelector, "defaultProps", {
  children: null,
  className: '',
  isOpen: false,
  onToggle: () => undefined,
  onSelect: () => undefined,
  screenReaderLabel: '',
  toggleText: '',
  searchButtonAriaLabel: 'Search menu items',
  searchInputValue: '',
  onSearchInputChange: () => undefined,
  searchInputPlaceholder: 'Search',
  onSearchButtonClick: () => undefined
});
//# sourceMappingURL=ContextSelector.js.map