import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/ChipGroup/chip-group';
import { ChipGroupContext } from './ChipGroup';
import { ChipButton } from './ChipButton';
import { Tooltip } from '../Tooltip';
import TimesIcon from '@patternfly/react-icons/dist/js/icons/times-icon';
import GenerateId from '../../helpers/GenerateId/GenerateId';
export class ChipGroupToolbarItem extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "heading", React.createRef());

    this.state = {
      isTooltipVisible: false
    };
  }

  componentDidMount() {
    this.setState({
      isTooltipVisible: Boolean(this.heading.current && this.heading.current.offsetWidth < this.heading.current.scrollWidth)
    });
  }

  render() {
    const _this$props = this.props,
          {
      categoryName,
      children,
      className,
      isClosable,
      closeBtnAriaLabel,
      onClick,
      tooltipPosition
    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["categoryName", "children", "className", "isClosable", "closeBtnAriaLabel", "onClick", "tooltipPosition"]);

    if (React.Children.count(children)) {
      const renderChipGroup = (id, HeadingLevel) => React.createElement("ul", _extends({
        className: css(styles.chipGroup, styles.modifiers.toolbar, className)
      }, rest), React.createElement("li", null, this.state.isTooltipVisible ? React.createElement(Tooltip, {
        position: tooltipPosition,
        content: categoryName
      }, React.createElement(HeadingLevel, {
        tabIndex: "0",
        ref: this.heading,
        className: css(styles.chipGroupLabel),
        id: id
      }, categoryName)) : React.createElement(HeadingLevel, {
        ref: this.heading,
        className: css(styles.chipGroupLabel),
        id: id
      }, categoryName), React.createElement("ul", {
        className: css(styles.chipGroup)
      }, children), isClosable && React.createElement("div", {
        className: "pf-c-chip-group__close"
      }, React.createElement(ChipButton, {
        "aria-label": closeBtnAriaLabel,
        onClick: onClick,
        id: `remove_group_${id}`,
        "aria-labelledby": `remove_group_${id} ${id}`
      }, React.createElement(TimesIcon, {
        "aria-hidden": "true"
      })))));

      return React.createElement(ChipGroupContext.Consumer, null, HeadingLevel => React.createElement(GenerateId, null, randomId => renderChipGroup(randomId, HeadingLevel)));
    }

    return null;
  }

}

_defineProperty(ChipGroupToolbarItem, "propTypes", {
  categoryName: _pt.string,
  children: _pt.node,
  className: _pt.string,
  isClosable: _pt.bool,
  onClick: _pt.func,
  closeBtnAriaLabel: _pt.string,
  tooltipPosition: _pt.oneOf(['auto', 'top', 'bottom', 'left', 'right'])
});

_defineProperty(ChipGroupToolbarItem, "defaultProps", {
  categoryName: '',
  children: null,
  className: '',
  isClosable: false,
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: _e => undefined,
  closeBtnAriaLabel: 'Close chip group',
  tooltipPosition: 'top'
});
//# sourceMappingURL=ChipGroupToolbarItem.js.map