import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import { css } from '@patternfly/react-styles';
import { ChipButton } from './ChipButton';
import { Tooltip } from '../Tooltip';
import TimesCircleIcon from '@patternfly/react-icons/dist/js/icons/times-circle-icon';
import styles from '@patternfly/react-styles/css/components/Chip/chip';
import GenerateId from '../../helpers/GenerateId/GenerateId';
import { withOuiaContext } from '../withOuia';

class Chip extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "span", React.createRef());

    _defineProperty(this, "renderOverflowChip", () => {
      const {
        children,
        className,
        onClick,
        ouiaContext,
        ouiaId
      } = this.props;
      const Component = this.props.component;
      return React.createElement(Component, _extends({
        className: css(styles.chip, styles.modifiers.overflow, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'OverflowChip',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement(ChipButton, {
        onClick: onClick
      }, React.createElement("span", {
        className: css(styles.chipText)
      }, children)));
    });

    _defineProperty(this, "renderChip", randomId => {
      const {
        children,
        closeBtnAriaLabel,
        tooltipPosition,
        className,
        onClick,
        isReadOnly,
        ouiaContext,
        ouiaId
      } = this.props;
      const Component = this.props.component;

      if (this.state.isTooltipVisible) {
        return React.createElement(Tooltip, {
          position: tooltipPosition,
          content: children
        }, React.createElement(Component, _extends({
          className: css(styles.chip, isReadOnly && styles.modifiers.readOnly, className),
          tabIndex: "0"
        }, ouiaContext.isOuia && {
          'data-ouia-component-type': 'Chip',
          'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
        }), React.createElement("span", {
          ref: this.span,
          className: css(styles.chipText),
          id: randomId
        }, children), !isReadOnly && React.createElement(ChipButton, {
          onClick: onClick,
          ariaLabel: closeBtnAriaLabel,
          id: `remove_${randomId}`,
          "aria-labelledby": `remove_${randomId} ${randomId}`
        }, React.createElement(TimesCircleIcon, {
          "aria-hidden": "true"
        }))));
      }

      return React.createElement(Component, _extends({
        className: css(styles.chip, isReadOnly && styles.modifiers.readOnly, className)
      }, ouiaContext.isOuia && {
        'data-ouia-component-type': 'Chip',
        'data-ouia-component-id': ouiaId || ouiaContext.ouiaId
      }), React.createElement("span", {
        ref: this.span,
        className: css(styles.chipText),
        id: randomId
      }, children), !isReadOnly && React.createElement(ChipButton, {
        onClick: onClick,
        ariaLabel: closeBtnAriaLabel,
        id: `remove_${randomId}`,
        "aria-labelledby": `remove_${randomId} ${randomId}`
      }, React.createElement(TimesCircleIcon, {
        "aria-hidden": "true"
      })));
    });

    this.state = {
      isTooltipVisible: false
    };
  }

  componentDidMount() {
    this.setState({
      isTooltipVisible: Boolean(this.span.current && this.span.current.offsetWidth < this.span.current.scrollWidth)
    });
  }

  render() {
    const {
      isOverflowChip
    } = this.props;
    return React.createElement(GenerateId, null, randomId => isOverflowChip ? this.renderOverflowChip() : this.renderChip(randomId));
  }

}

_defineProperty(Chip, "propTypes", {
  children: _pt.node,
  closeBtnAriaLabel: _pt.string,
  className: _pt.string,
  isOverflowChip: _pt.bool,
  isReadOnly: _pt.bool,
  onClick: _pt.func,
  component: _pt.node,
  tooltipPosition: _pt.oneOf(['auto', 'top', 'bottom', 'left', 'right'])
});

_defineProperty(Chip, "defaultProps", {
  closeBtnAriaLabel: 'close',
  className: '',
  isOverflowChip: false,
  isReadOnly: false,
  tooltipPosition: 'top',
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onClick: _e => undefined,
  component: 'div'
});

const ChipWithOuiaContext = withOuiaContext(Chip);
export { ChipWithOuiaContext as Chip };
//# sourceMappingURL=Chip.js.map