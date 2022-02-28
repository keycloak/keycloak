import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import badgeStyles from '@patternfly/react-styles/css/components/Badge/badge';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import TimesCircleIcon from '@patternfly/react-icons/dist/js/icons/times-circle-icon';
import { SelectMenu } from './SelectMenu';
import { SelectOption } from './SelectOption';
import { SelectToggle } from './SelectToggle';
import { SelectContext, SelectVariant, SelectDirection, KeyTypes } from './selectConstants';
import { Chip, ChipGroup } from '../ChipGroup';
import { keyHandler, getNextIndex } from '../../helpers/util';
import { withOuiaContext } from '../withOuia';
import { Divider } from '../Divider'; // seed for the aria-labelledby ID

let currentId = 0;

class Select extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "parentRef", React.createRef());

    _defineProperty(this, "filterRef", React.createRef());

    _defineProperty(this, "refCollection", []);

    _defineProperty(this, "state", {
      openedOnEnter: false,
      typeaheadInputValue: null,
      typeaheadActiveChild: null,
      typeaheadFilteredChildren: React.Children.toArray(this.props.children),
      typeaheadCurrIndex: -1,
      creatableValue: ''
    });

    _defineProperty(this, "componentDidUpdate", (prevProps, prevState) => {
      if (this.props.hasInlineFilter) {
        this.refCollection[0] = this.filterRef.current;
      }

      if (!prevState.openedOnEnter && this.state.openedOnEnter) {
        this.refCollection[0].focus();
      }

      if (prevProps.children !== this.props.children) {
        this.setState({
          typeaheadFilteredChildren: React.Children.toArray(this.props.children)
        });
      }

      if (prevProps.selections !== this.props.selections && this.props.variant === SelectVariant.typeahead) {
        this.setState({
          typeaheadInputValue: this.props.selections
        });
      }
    });

    _defineProperty(this, "onEnter", () => {
      this.setState({
        openedOnEnter: true
      });
    });

    _defineProperty(this, "onClose", () => {
      this.setState({
        openedOnEnter: false,
        typeaheadInputValue: null,
        typeaheadActiveChild: null,
        typeaheadFilteredChildren: React.Children.toArray(this.props.children),
        typeaheadCurrIndex: -1
      });
    });

    _defineProperty(this, "onChange", e => {
      const {
        onFilter,
        isCreatable,
        onCreateOption,
        createText,
        noResultsFoundText,
        children
      } = this.props;
      let typeaheadFilteredChildren;

      if (onFilter) {
        typeaheadFilteredChildren = onFilter(e) || children;
      } else {
        let input;

        try {
          input = new RegExp(e.target.value.toString(), 'i');
        } catch (err) {
          input = new RegExp(e.target.value.toString().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
        }

        typeaheadFilteredChildren = e.target.value.toString() !== '' ? React.Children.toArray(this.props.children).filter(child => this.getDisplay(child.props.value.toString(), 'text').search(input) === 0) : React.Children.toArray(this.props.children);
      }

      if (!typeaheadFilteredChildren) {
        typeaheadFilteredChildren = [];
      }

      if (typeaheadFilteredChildren.length === 0) {
        !isCreatable && typeaheadFilteredChildren.push(React.createElement(SelectOption, {
          isDisabled: true,
          key: 0,
          value: noResultsFoundText,
          isNoResultsOption: true
        }));
      }

      if (isCreatable && e.target.value !== '') {
        const newValue = e.target.value;
        typeaheadFilteredChildren.push(React.createElement(SelectOption, {
          key: 0,
          value: newValue,
          onClick: () => onCreateOption && onCreateOption(newValue)
        }, createText, " \"", newValue, "\""));
      }

      this.setState({
        typeaheadInputValue: e.target.value,
        typeaheadCurrIndex: -1,
        typeaheadFilteredChildren,
        typeaheadActiveChild: null,
        creatableValue: e.target.value
      });
      this.refCollection = [];
    });

    _defineProperty(this, "onClick", e => {
      e.stopPropagation();
    });

    _defineProperty(this, "clearSelection", e => {
      e.stopPropagation();
      this.setState({
        typeaheadInputValue: null,
        typeaheadActiveChild: null,
        typeaheadFilteredChildren: React.Children.toArray(this.props.children),
        typeaheadCurrIndex: -1
      });
    });

    _defineProperty(this, "sendRef", (ref, index) => {
      this.refCollection[index] = ref;
    });

    _defineProperty(this, "handleArrowKeys", (index, position) => {
      keyHandler(index, 0, position, this.refCollection, this.refCollection);
    });

    _defineProperty(this, "handleFocus", () => {
      if (!this.props.isExpanded) {
        this.props.onToggle(true);
      }
    });

    _defineProperty(this, "handleTypeaheadKeys", position => {
      const {
        isExpanded,
        isCreatable,
        createText
      } = this.props;
      const {
        typeaheadActiveChild,
        typeaheadCurrIndex
      } = this.state;

      if (isExpanded) {
        if (position === 'enter' && (typeaheadActiveChild || this.refCollection[0])) {
          this.setState({
            typeaheadInputValue: typeaheadActiveChild && typeaheadActiveChild.innerText || this.refCollection[0].innerText
          });

          if (typeaheadActiveChild) {
            typeaheadActiveChild.click();
          } else {
            this.refCollection[0].click();
          }
        } else {
          let nextIndex;

          if (typeaheadCurrIndex === -1 && position === 'down') {
            nextIndex = 0;
          } else if (typeaheadCurrIndex === -1 && position === 'up') {
            nextIndex = this.refCollection.length - 1;
          } else {
            nextIndex = getNextIndex(typeaheadCurrIndex, position, this.refCollection);
          }

          this.setState({
            typeaheadCurrIndex: nextIndex,
            typeaheadActiveChild: this.refCollection[nextIndex],
            typeaheadInputValue: isCreatable && this.refCollection[nextIndex].innerText.includes(createText) ? this.state.creatableValue : this.refCollection[nextIndex].innerText
          });
        }
      }
    });

    _defineProperty(this, "getDisplay", (value, type = 'node') => {
      if (!value) {
        return;
      }

      const {
        children,
        isGrouped
      } = this.props;
      let item = children.filter(child => child.props.value !== undefined && child.props.value.toString() === value.toString())[0];

      if (isGrouped) {
        item = children.reduce((acc, curr) => [...acc, ...React.Children.toArray(curr.props.children)], []).filter(child => child.props.value.toString() === value.toString())[0];
      }

      if (item) {
        if (item && item.props.children) {
          if (type === 'node') {
            return item.props.children;
          }

          return this.findText(item);
        }

        return item.props.value.toString();
      }

      return value;
    });

    _defineProperty(this, "findText", item => {
      if (!item.props || !item.props.children) {
        if (typeof item !== 'string') {
          return '';
        }

        return item;
      }

      if (typeof item.props.children === 'string') {
        return item.props.children;
      }

      const multi = [];
      React.Children.toArray(item.props.children).forEach(child => multi.push(this.findText(child)));
      return multi.join('');
    });
  }

  extendTypeaheadChildren(typeaheadActiveChild) {
    return this.state.typeaheadFilteredChildren.map(child => React.cloneElement(child, {
      isFocused: typeaheadActiveChild && (typeaheadActiveChild.innerText === this.getDisplay(child.props.value.toString(), 'text') || this.props.isCreatable && typeaheadActiveChild.innerText === `{createText} "${child.props.value}"`)
    }));
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      children,
      className,
      customContent,
      variant,
      direction,
      onToggle,
      onSelect,
      onClear,
      onFilter,
      onCreateOption,
      toggleId,
      isExpanded,
      isGrouped,
      isPlain,
      isDisabled,
      isCreatable,
      selections,
      isCheckboxSelectionBadgeHidden,
      ariaLabelledBy,
      ariaLabelTypeAhead,
      ariaLabelClear,
      ariaLabelToggle,
      ariaLabelRemove,
      'aria-label': ariaLabel,
      placeholderText,
      width,
      maxHeight,
      toggleIcon,
      ouiaContext,
      ouiaId,
      createText,
      noResultsFoundText,
      hasInlineFilter
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "className", "customContent", "variant", "direction", "onToggle", "onSelect", "onClear", "onFilter", "onCreateOption", "toggleId", "isExpanded", "isGrouped", "isPlain", "isDisabled", "isCreatable", "selections", "isCheckboxSelectionBadgeHidden", "ariaLabelledBy", "ariaLabelTypeAhead", "ariaLabelClear", "ariaLabelToggle", "ariaLabelRemove", "aria-label", "placeholderText", "width", "maxHeight", "toggleIcon", "ouiaContext", "ouiaId", "createText", "noResultsFoundText", "hasInlineFilter"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    const {
      openedOnEnter,
      typeaheadInputValue,
      typeaheadActiveChild,
      typeaheadFilteredChildren
    } = this.state;
    const selectToggleId = toggleId || `pf-toggle-id-${currentId++}`;
    let childPlaceholderText = null;

    if (!customContent) {
      if (!selections && !placeholderText) {
        const childPlaceholder = React.Children.toArray(children.filter(child => child.props.isPlaceholder === true));
        childPlaceholderText = childPlaceholder[0] && this.getDisplay(childPlaceholder[0].props.value, 'node') || children[0] && this.getDisplay(children[0].props.value, 'node');
      }
    }

    const hasOnClear = onClear !== Select.defaultProps.onClear;
    const hasAnySelections = selections && (Array.isArray(selections) ? selections.length > 0 ? true : false : selections !== '');
    const clearBtn = React.createElement("button", {
      className: css(buttonStyles.button, buttonStyles.modifiers.plain, styles.selectToggleClear),
      onClick: e => {
        this.clearSelection(e);
        onClear(e);
      },
      "aria-label": ariaLabelClear,
      type: "button",
      disabled: isDisabled
    }, React.createElement(TimesCircleIcon, {
      "aria-hidden": true
    }));
    let selectedChips = null;

    if (variant === SelectVariant.typeaheadMulti) {
      selectedChips = React.createElement(ChipGroup, null, selections && selections.map(item => {
        const isItemDisabled = React.Children.toArray(children.filter(child => child.props.value === item))[0].props.isDisabled;
        return React.createElement(Chip, _extends({
          key: item,
          onClick: e => onSelect(e, item),
          closeBtnAriaLabel: ariaLabelRemove
        }, isItemDisabled && {
          isReadOnly: true
        }), this.getDisplay(item, 'node'));
      }));
    }

    let filterWithChildren = children;

    if (hasInlineFilter) {
      const filterBox = React.createElement(React.Fragment, null, React.createElement("div", {
        key: "inline-filter",
        className: css(styles.selectMenuInput)
      }, React.createElement("input", {
        key: "inline-filter-input",
        type: "search",
        className: css(formStyles.formControl, formStyles.modifiers.search),
        onChange: this.onChange,
        onKeyDown: event => {
          if (event.key === KeyTypes.ArrowUp) {
            this.handleArrowKeys(0, 'up');
          } else if (event.key === KeyTypes.ArrowDown) {
            this.handleArrowKeys(0, 'down');
          }
        },
        ref: this.filterRef,
        autoComplete: "off"
      })), React.createElement(Divider, {
        key: "inline-filter-divider"
      }));
      this.refCollection[0] = this.filterRef.current;
      filterWithChildren = [filterBox, ...typeaheadFilteredChildren].map((option, index) => React.cloneElement(option, {
        key: index
      }));
    }

    return React.createElement("div", _extends({
      className: css(styles.select, isExpanded && styles.modifiers.expanded, direction === SelectDirection.up && styles.modifiers.top, className),
      ref: this.parentRef,
      style: {
        width
      }
    }, ouiaContext.isOuia && {
      'data-ouia-component-type': 'Select',
      'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
    }), React.createElement(SelectContext.Provider, {
      value: {
        onSelect,
        onClose: this.onClose,
        variant
      }
    }, React.createElement(SelectToggle, {
      id: selectToggleId,
      parentRef: this.parentRef,
      isExpanded: isExpanded,
      isPlain: isPlain,
      onToggle: onToggle,
      onEnter: this.onEnter,
      onClose: this.onClose,
      ariaLabelledBy: `${ariaLabelledBy || ''} ${selectToggleId}`,
      variant: variant,
      ariaLabelToggle: ariaLabelToggle,
      handleTypeaheadKeys: this.handleTypeaheadKeys,
      isDisabled: isDisabled,
      hasClearButton: hasOnClear
    }, customContent && React.createElement("div", {
      className: css(styles.selectToggleWrapper)
    }, toggleIcon && React.createElement("span", {
      className: css(styles.selectToggleIcon)
    }, toggleIcon), React.createElement("span", {
      className: css(styles.selectToggleText)
    }, placeholderText)), variant === SelectVariant.single && !customContent && React.createElement("div", {
      className: css(styles.selectToggleWrapper)
    }, toggleIcon && React.createElement("span", {
      className: css(styles.selectToggleIcon)
    }, toggleIcon), React.createElement("span", {
      className: css(styles.selectToggleText)
    }, this.getDisplay(selections, 'node') || placeholderText || childPlaceholderText), hasOnClear && hasAnySelections && clearBtn), variant === SelectVariant.checkbox && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
      className: css(styles.selectToggleWrapper)
    }, toggleIcon && React.createElement("span", {
      className: css(styles.selectToggleIcon)
    }, toggleIcon), React.createElement("span", {
      className: css(styles.selectToggleText)
    }, placeholderText), !isCheckboxSelectionBadgeHidden && selections && Array.isArray(selections) && selections.length > 0 && React.createElement("div", {
      className: css(styles.selectToggleBadge)
    }, React.createElement("span", {
      className: css(badgeStyles.badge, badgeStyles.modifiers.read)
    }, selections.length))), hasOnClear && hasAnySelections && clearBtn), variant === SelectVariant.typeahead && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
      className: css(styles.selectToggleWrapper)
    }, toggleIcon && React.createElement("span", {
      className: css(styles.selectToggleIcon)
    }, toggleIcon), React.createElement("input", {
      className: css(formStyles.formControl, styles.selectToggleTypeahead),
      "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id,
      id: `${selectToggleId}-select-typeahead`,
      "aria-label": ariaLabelTypeAhead,
      placeholder: placeholderText,
      value: typeaheadInputValue !== null ? typeaheadInputValue : this.getDisplay(selections, 'text') || '',
      type: "text",
      onClick: this.onClick,
      onChange: this.onChange,
      onFocus: this.handleFocus,
      autoComplete: "off",
      disabled: isDisabled
    })), (selections || typeaheadInputValue) && clearBtn), variant === SelectVariant.typeaheadMulti && !customContent && React.createElement(React.Fragment, null, React.createElement("div", {
      className: css(styles.selectToggleWrapper)
    }, toggleIcon && React.createElement("span", {
      className: css(styles.selectToggleIcon)
    }, toggleIcon), selections && Array.isArray(selections) && selections.length > 0 && selectedChips, React.createElement("input", {
      className: css(formStyles.formControl, styles.selectToggleTypeahead),
      "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id,
      id: `${selectToggleId}-select-multi-typeahead-typeahead`,
      "aria-label": ariaLabelTypeAhead,
      placeholder: placeholderText,
      value: typeaheadInputValue !== null ? typeaheadInputValue : '',
      type: "text",
      onChange: this.onChange,
      onClick: this.onClick,
      onFocus: this.handleFocus,
      autoComplete: "off",
      disabled: isDisabled
    })), (selections && Array.isArray(selections) && selections.length > 0 || typeaheadInputValue) && clearBtn)), customContent && isExpanded && React.createElement(SelectMenu, _extends({}, props, {
      selected: selections,
      openedOnEnter: openedOnEnter,
      "aria-label": ariaLabel,
      "aria-labelledby": ariaLabelledBy,
      sendRef: this.sendRef,
      keyHandler: this.handleArrowKeys,
      maxHeight: maxHeight,
      isCustomContent: true
    }), customContent), variant === SelectVariant.single && isExpanded && !customContent && React.createElement(SelectMenu, _extends({}, props, {
      isGrouped: isGrouped,
      selected: selections,
      openedOnEnter: openedOnEnter,
      "aria-label": ariaLabel,
      "aria-labelledby": ariaLabelledBy,
      sendRef: this.sendRef,
      keyHandler: this.handleArrowKeys,
      maxHeight: maxHeight
    }), children), variant === SelectVariant.checkbox && isExpanded && !customContent && React.createElement(SelectMenu, _extends({}, props, {
      checked: selections ? selections : [],
      "aria-label": ariaLabel,
      "aria-labelledby": ariaLabelledBy,
      isGrouped: isGrouped,
      sendRef: this.sendRef,
      keyHandler: this.handleArrowKeys,
      maxHeight: maxHeight,
      hasInlineFilter: hasInlineFilter
    }), filterWithChildren), (variant === SelectVariant.typeahead || variant === SelectVariant.typeaheadMulti) && isExpanded && !customContent && React.createElement(SelectMenu, _extends({}, props, {
      selected: selections,
      openedOnEnter: openedOnEnter,
      "aria-label": ariaLabel,
      "aria-labelledby": ariaLabelledBy,
      sendRef: this.sendRef,
      keyHandler: this.handleArrowKeys,
      maxHeight: maxHeight
    }), this.extendTypeaheadChildren(typeaheadActiveChild))));
  }

}

_defineProperty(Select, "propTypes", {
  children: _pt.arrayOf(_pt.element),
  className: _pt.string,
  direction: _pt.oneOf(['up', 'down']),
  isExpanded: _pt.bool,
  isGrouped: _pt.bool,
  isPlain: _pt.bool,
  isDisabled: _pt.bool,
  isCreatable: _pt.bool,
  createText: _pt.string,
  placeholderText: _pt.oneOfType([_pt.string, _pt.node]),
  noResultsFoundText: _pt.string,
  selections: _pt.oneOfType([_pt.string, _pt.any, _pt.arrayOf(_pt.oneOfType([_pt.string, _pt.any]))]),
  isCheckboxSelectionBadgeHidden: _pt.bool,
  toggleId: _pt.string,
  'aria-label': _pt.string,
  ariaLabelledBy: _pt.string,
  ariaLabelTypeAhead: _pt.string,
  ariaLabelClear: _pt.string,
  ariaLabelToggle: _pt.string,
  ariaLabelRemove: _pt.string,
  onSelect: _pt.func,
  onToggle: _pt.func.isRequired,
  onClear: _pt.func,
  onFilter: _pt.func,
  onCreateOption: _pt.func
});

_defineProperty(Select, "defaultProps", {
  children: [],
  className: '',
  direction: SelectDirection.down,
  toggleId: null,
  isExpanded: false,
  isGrouped: false,
  isPlain: false,
  isDisabled: false,
  isCreatable: false,
  'aria-label': '',
  ariaLabelledBy: '',
  ariaLabelTypeAhead: '',
  ariaLabelClear: 'Clear all',
  ariaLabelToggle: 'Options menu',
  ariaLabelRemove: 'Remove',
  selections: '',
  createText: 'Create',
  placeholderText: '',
  noResultsFoundText: 'No results found',
  variant: SelectVariant.single,
  width: '',
  onClear: () => undefined,
  onCreateOption: () => undefined,
  toggleIcon: null,
  onFilter: null,
  customContent: null,
  hasInlineFilter: false
});

const SelectWithOuiaContext = withOuiaContext(Select);
export { SelectWithOuiaContext as Select };
//# sourceMappingURL=Select.js.map