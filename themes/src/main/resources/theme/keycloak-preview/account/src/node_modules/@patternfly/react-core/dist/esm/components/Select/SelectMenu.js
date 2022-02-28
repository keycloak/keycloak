import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import { default as formStyles } from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { SelectOption } from './SelectOption';
import { SelectConsumer, SelectVariant } from './selectConstants';
import { FocusTrap } from '../../helpers';
export class SelectMenu extends React.Component {
  extendChildren() {
    const {
      children,
      isGrouped
    } = this.props;
    const childrenArray = children;

    if (isGrouped) {
      let index = 0;
      return React.Children.map(childrenArray, group => React.cloneElement(group, {
        titleId: group.props.label.replace(/\W/g, '-'),
        children: group.props.children.map(option => this.cloneOption(option, index++))
      }));
    }

    return React.Children.map(childrenArray, (child, index) => this.cloneOption(child, index));
  }

  cloneOption(child, index) {
    const {
      selected,
      sendRef,
      keyHandler
    } = this.props;
    const isSelected = selected && selected.constructor === Array ? selected && Array.isArray(selected) && selected.includes(child.props.value) : selected === child.props.value;
    return React.cloneElement(child, {
      id: `${child.props.value ? child.props.value.toString() : ''}-${index}`,
      isSelected,
      sendRef,
      keyHandler,
      index
    });
  }

  extendCheckboxChildren(children) {
    const {
      isGrouped,
      checked,
      sendRef,
      keyHandler,
      hasInlineFilter
    } = this.props;
    let index = hasInlineFilter ? 1 : 0;

    if (isGrouped) {
      return React.Children.map(children, group => {
        if (group.type === SelectOption) {
          return group;
        }

        return React.cloneElement(group, {
          titleId: group.props.label.replace(/\W/g, '-'),
          children: React.createElement("fieldset", {
            "aria-labelledby": group.props.label.replace(/\W/g, '-'),
            className: css(styles.selectMenuFieldset)
          }, group.props.children.map(option => React.cloneElement(option, {
            isChecked: checked && checked.includes(option.props.value),
            sendRef,
            keyHandler,
            index: index++
          })))
        });
      });
    }

    return React.Children.map(children, child => React.cloneElement(child, {
      isChecked: checked && checked.includes(child.props.value),
      sendRef,
      keyHandler,
      index: index++
    }));
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      children,
      isCustomContent,
      className,
      isExpanded,
      openedOnEnter,
      selected,
      checked,
      isGrouped,
      sendRef,
      keyHandler,
      maxHeight,
      noResultsFoundText,
      createText,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      hasInlineFilter
    } = _this$props,
          props = _objectWithoutProperties(_this$props, ["children", "isCustomContent", "className", "isExpanded", "openedOnEnter", "selected", "checked", "isGrouped", "sendRef", "keyHandler", "maxHeight", "noResultsFoundText", "createText", "aria-label", "aria-labelledby", "hasInlineFilter"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    return React.createElement(SelectConsumer, null, ({
      variant
    }) => React.createElement(React.Fragment, null, isCustomContent && React.createElement("div", _extends({
      className: css(styles.selectMenu, className)
    }, maxHeight && {
      style: {
        maxHeight,
        overflow: 'auto'
      }
    }, props), children), variant !== SelectVariant.checkbox && !isCustomContent && React.createElement("ul", _extends({
      className: css(styles.selectMenu, className),
      role: "listbox"
    }, maxHeight && {
      style: {
        maxHeight,
        overflow: 'auto'
      }
    }, props), this.extendChildren()), variant === SelectVariant.checkbox && !isCustomContent && React.Children.count(children) > 0 && React.createElement(FocusTrap, {
      focusTrapOptions: {
        clickOutsideDeactivates: true
      }
    }, React.createElement("div", _extends({
      className: css(styles.selectMenu, className)
    }, maxHeight && {
      style: {
        maxHeight,
        overflow: 'auto'
      }
    }), React.createElement("fieldset", _extends({}, props, {
      "aria-label": ariaLabel,
      "aria-labelledby": !ariaLabel && ariaLabelledBy || null,
      className: css(formStyles.formFieldset)
    }), hasInlineFilter && [children.shift(), ...this.extendCheckboxChildren(children)], !hasInlineFilter && this.extendCheckboxChildren(children)))), variant === SelectVariant.checkbox && !isCustomContent && React.Children.count(children) === 0 && React.createElement("div", _extends({
      className: css(styles.selectMenu, className)
    }, maxHeight && {
      style: {
        maxHeight,
        overflow: 'auto'
      }
    }), React.createElement("fieldset", {
      className: css(styles.selectMenuFieldset)
    }))));
  }

}

_defineProperty(SelectMenu, "propTypes", {
  children: _pt.oneOfType([_pt.arrayOf(_pt.element), _pt.node]).isRequired,
  isCustomContent: _pt.bool,
  className: _pt.string,
  isExpanded: _pt.bool,
  isGrouped: _pt.bool,
  selected: _pt.oneOfType([_pt.string, _pt.any, _pt.arrayOf(_pt.oneOfType([_pt.string, _pt.any]))]),
  checked: _pt.arrayOf(_pt.oneOfType([_pt.string, _pt.any])),
  openedOnEnter: _pt.bool,
  maxHeight: _pt.oneOfType([_pt.string, _pt.number]),
  noResultsFoundText: _pt.string,
  createText: _pt.string,
  sendRef: _pt.func,
  keyHandler: _pt.func,
  hasInlineFilter: _pt.bool
});

_defineProperty(SelectMenu, "defaultProps", {
  className: '',
  isExpanded: false,
  isGrouped: false,
  openedOnEnter: false,
  selected: '',
  maxHeight: '',
  sendRef: () => {},
  keyHandler: () => {},
  isCustomContent: false,
  hasInlineFilter: false
});
//# sourceMappingURL=SelectMenu.js.map