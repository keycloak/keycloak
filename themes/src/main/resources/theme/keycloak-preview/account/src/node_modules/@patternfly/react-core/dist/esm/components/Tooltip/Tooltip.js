import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import PopoverBase from '../../helpers/PopoverBase/PopoverBase';
import styles from '@patternfly/react-styles/css/components/Tooltip/tooltip';
import '@patternfly/react-styles/css/components/Tooltip/tippy.css';
import '@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css';
import { css, getModifier } from '@patternfly/react-styles';
import { TooltipContent } from './TooltipContent';
import { KEY_CODES } from '../../helpers/constants';
import tooltipMaxWidth from '@patternfly/react-tokens/dist/js/c_tooltip_MaxWidth';
export let TooltipPosition;

(function (TooltipPosition) {
  TooltipPosition["auto"] = "auto";
  TooltipPosition["top"] = "top";
  TooltipPosition["bottom"] = "bottom";
  TooltipPosition["left"] = "left";
  TooltipPosition["right"] = "right";
})(TooltipPosition || (TooltipPosition = {}));

export class Tooltip extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "storeTippyInstance", tip => {
      tip.popperChildren.tooltip.classList.add(styles.tooltip);
      this.tip = tip;
    });

    _defineProperty(this, "handleEscKeyClick", event => {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.tip.state.isVisible) {
        this.tip.hide();
      }
    });
  }

  componentDidMount() {
    document.addEventListener('keydown', this.handleEscKeyClick, false);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleEscKeyClick, false);
  }

  extendChildren() {
    return React.cloneElement(this.props.children, {
      isAppLauncher: this.props.isAppLauncher
    });
  }

  render() {
    const _this$props = this.props,
          {
      position,
      trigger,
      isContentLeftAligned,
      isVisible,
      enableFlip,
      children,
      className,
      content: bodyContent,
      entryDelay,
      exitDelay,
      appendTo,
      zIndex,
      maxWidth,
      isAppLauncher,
      distance,
      aria,
      boundary,
      flipBehavior,
      tippyProps,
      id
    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["position", "trigger", "isContentLeftAligned", "isVisible", "enableFlip", "children", "className", "content", "entryDelay", "exitDelay", "appendTo", "zIndex", "maxWidth", "isAppLauncher", "distance", "aria", "boundary", "flipBehavior", "tippyProps", "id"]);

    const content = React.createElement("div", _extends({
      className: css(!enableFlip && getModifier(styles, position, styles.modifiers.top), className),
      role: "tooltip",
      id: id
    }, rest), React.createElement(TooltipContent, {
      isLeftAligned: isContentLeftAligned
    }, bodyContent));
    return React.createElement(PopoverBase, _extends({}, tippyProps, {
      arrow: true,
      aria: aria,
      onCreate: this.storeTippyInstance,
      maxWidth: maxWidth,
      zIndex: zIndex,
      appendTo: appendTo,
      content: content,
      lazy: true,
      theme: "pf-tooltip",
      placement: position,
      trigger: trigger,
      delay: [entryDelay, exitDelay],
      distance: distance,
      flip: enableFlip,
      flipBehavior: flipBehavior,
      boundary: boundary,
      isVisible: isVisible,
      popperOptions: {
        modifiers: {
          preventOverflow: {
            enabled: enableFlip
          },
          hide: {
            enabled: enableFlip
          }
        }
      }
    }), isAppLauncher ? this.extendChildren() : children);
  }

}

_defineProperty(Tooltip, "propTypes", {
  appendTo: _pt.oneOfType([_pt.element, _pt.func]),
  aria: _pt.oneOf(['describedby', 'labelledby']),
  boundary: _pt.oneOfType([_pt.oneOf(['scrollParent']), _pt.oneOf(['window']), _pt.oneOf(['viewport']), _pt.any]),
  children: _pt.element.isRequired,
  className: _pt.string,
  content: _pt.node.isRequired,
  distance: _pt.number,
  enableFlip: _pt.bool,
  entryDelay: _pt.number,
  exitDelay: _pt.number,
  flipBehavior: _pt.oneOfType([_pt.oneOf(['flip']), _pt.arrayOf(_pt.oneOf(['top', 'bottom', 'left', 'right']))]),
  isAppLauncher: _pt.bool,
  maxWidth: _pt.string,
  position: _pt.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  trigger: _pt.string,
  isContentLeftAligned: _pt.bool,
  isVisible: _pt.bool,
  zIndex: _pt.number,
  tippyProps: _pt.any,
  id: _pt.string
});

_defineProperty(Tooltip, "defaultProps", {
  position: 'top',
  trigger: 'mouseenter focus',
  isVisible: false,
  isContentLeftAligned: false,
  enableFlip: true,
  className: '',
  entryDelay: 500,
  exitDelay: 500,
  appendTo: () => document.body,
  zIndex: 9999,
  maxWidth: tooltipMaxWidth && tooltipMaxWidth.value,
  isAppLauncher: false,
  distance: 15,
  aria: 'describedby',
  boundary: 'window',
  // For every initial starting position, there are 3 escape positions
  flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
  tippyProps: {},
  id: ''
});
//# sourceMappingURL=Tooltip.js.map