import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import PopoverBase from '../../helpers/PopoverBase/PopoverBase';
import { KEY_CODES } from '../../helpers/constants';
import styles from '@patternfly/react-styles/css/components/Popover/popover';
import '@patternfly/react-styles/css/components/Tooltip/tippy.css';
import '@patternfly/react-styles/css/components/Tooltip/tippy-overrides.css';
import { css, getModifier } from '@patternfly/react-styles';
import { PopoverContent } from './PopoverContent';
import { PopoverBody } from './PopoverBody';
import { PopoverHeader } from './PopoverHeader';
import { PopoverFooter } from './PopoverFooter';
import { PopoverCloseButton } from './PopoverCloseButton';
import GenerateId from '../../helpers/GenerateId/GenerateId';
import popoverMaxWidth from '@patternfly/react-tokens/dist/js/c_popover_MaxWidth';
// Can't use ES6 imports :(
// The types for it are also wrong, we should probably ditch this dependency.
import { FocusTrap } from '../../helpers';
export let PopoverPosition;

(function (PopoverPosition) {
  PopoverPosition["auto"] = "auto";
  PopoverPosition["top"] = "top";
  PopoverPosition["bottom"] = "bottom";
  PopoverPosition["left"] = "left";
  PopoverPosition["right"] = "right";
})(PopoverPosition || (PopoverPosition = {}));

export class Popover extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "hideOrNotify", () => {
      if (this.props.isVisible === null) {
        // Handle closing
        this.tip.hide();
      } else {
        // notify consumer
        this.props.shouldClose(this.tip);
      }
    });

    _defineProperty(this, "handleEscOrEnterKey", event => {
      if (event.keyCode === KEY_CODES.ESCAPE_KEY && this.tip.state.isVisible) {
        this.hideOrNotify();
      } else if (!this.state.isOpen && event.keyCode === KEY_CODES.ENTER) {
        this.setState({
          focusTrapActive: true
        });
      }
    });

    _defineProperty(this, "storeTippyInstance", tip => {
      if (this.props.minWidth) {
        tip.popperChildren.tooltip.style.minWidth = this.props.minWidth;
      }

      tip.popperChildren.tooltip.classList.add(styles.popover);
      this.tip = tip;
    });

    _defineProperty(this, "closePopover", () => {
      this.hideOrNotify();
      this.setState({
        focusTrapActive: false
      });
    });

    _defineProperty(this, "hideAllPopovers", () => {
      document.querySelectorAll('.tippy-popper').forEach(popper => {
        if (popper._tippy) {
          popper._tippy.hide();
        }
      });
    });

    _defineProperty(this, "onHide", tip => {
      if (this.state.isOpen) {
        this.setState({
          isOpen: false
        });
      }

      return this.props.onHide(tip);
    });

    _defineProperty(this, "onHidden", tip => this.props.onHidden(tip));

    _defineProperty(this, "onMount", tip => this.props.onMount(tip));

    _defineProperty(this, "onShow", tip => {
      const {
        hideOnOutsideClick,
        isVisible,
        onShow
      } = this.props; // hide all other open popovers first if events are managed by us

      if (!hideOnOutsideClick && isVisible === null) {
        this.hideAllPopovers();
      }

      if (this.state.isOpen === false) {
        this.setState({
          isOpen: true
        });
      }

      return onShow(tip);
    });

    _defineProperty(this, "onShown", tip => this.props.onShown(tip));

    _defineProperty(this, "onContentMouseDown", () => {
      if (this.state.focusTrapActive) {
        this.setState({
          focusTrapActive: false
        });
      }
    });

    this.state = {
      isOpen: false,
      focusTrapActive: false
    };
  }

  componentDidMount() {
    document.addEventListener('keydown', this.handleEscOrEnterKey, false);
  }

  componentWillUnmount() {
    document.removeEventListener('keydown', this.handleEscOrEnterKey, false);
  }

  render() {
    /* eslint-disable @typescript-eslint/no-unused-vars */
    const _this$props = this.props,
          {
      position,
      enableFlip,
      children,
      className,
      'aria-label': ariaLabel,
      headerContent,
      bodyContent,
      footerContent,
      isVisible,
      shouldClose,
      appendTo,
      hideOnOutsideClick,
      onHide,
      onHidden,
      onShow,
      onShown,
      onMount,
      zIndex,
      minWidth,
      maxWidth,
      closeBtnAriaLabel,
      distance,
      boundary,
      flipBehavior,
      tippyProps
    } = _this$props,
          rest = _objectWithoutProperties(_this$props, ["position", "enableFlip", "children", "className", "aria-label", "headerContent", "bodyContent", "footerContent", "isVisible", "shouldClose", "appendTo", "hideOnOutsideClick", "onHide", "onHidden", "onShow", "onShown", "onMount", "zIndex", "minWidth", "maxWidth", "closeBtnAriaLabel", "distance", "boundary", "flipBehavior", "tippyProps"]);
    /* eslint-enable @typescript-eslint/no-unused-vars */


    if (!headerContent && !ariaLabel) {
      return new Error('aria-label is required when header is not used');
    }

    const content = this.state.isOpen ? React.createElement(GenerateId, null, randomId => React.createElement(FocusTrap, {
      active: this.state.focusTrapActive,
      focusTrapOptions: {
        clickOutsideDeactivates: true
      }
    }, React.createElement("div", _extends({
      className: css(!enableFlip && getModifier(styles, position, styles.modifiers.top), className),
      role: "dialog",
      "aria-modal": "true",
      "aria-label": headerContent ? undefined : ariaLabel,
      "aria-labelledby": headerContent ? `popover-${randomId}-header` : undefined,
      "aria-describedby": `popover-${randomId}-body`,
      onMouseDown: this.onContentMouseDown
    }, rest), React.createElement(PopoverContent, null, React.createElement(PopoverCloseButton, {
      onClose: this.closePopover,
      "aria-label": closeBtnAriaLabel
    }), headerContent && React.createElement(PopoverHeader, {
      id: `popover-${randomId}-header`
    }, headerContent), React.createElement(PopoverBody, {
      id: `popover-${randomId}-body`
    }, bodyContent), footerContent && React.createElement(PopoverFooter, null, footerContent))))) : React.createElement(React.Fragment, null);
    const handleEvents = isVisible === null;

    const shouldHideOnClick = () => {
      if (handleEvents) {
        if (hideOnOutsideClick === true) {
          return true;
        }

        return 'toggle';
      }

      return false;
    };

    return React.createElement(PopoverBase, _extends({}, tippyProps, {
      arrow: true,
      onCreate: this.storeTippyInstance,
      maxWidth: maxWidth,
      zIndex: zIndex,
      appendTo: appendTo,
      content: content,
      lazy: true,
      trigger: handleEvents ? 'click' : 'manual',
      isVisible: isVisible,
      hideOnClick: shouldHideOnClick(),
      theme: "pf-popover",
      interactive: true,
      interactiveBorder: 0,
      placement: position,
      distance: distance,
      flip: enableFlip,
      flipBehavior: flipBehavior,
      boundary: boundary,
      popperOptions: {
        modifiers: {
          preventOverflow: {
            enabled: enableFlip
          },
          hide: {
            enabled: enableFlip
          }
        }
      },
      onHide: tip => this.onHide(tip),
      onHidden: tip => this.onHidden(tip),
      onShow: tip => this.onShow(tip),
      onShown: tip => this.onShown(tip),
      onMount: tip => this.onMount(tip)
    }), children);
  }

}

_defineProperty(Popover, "propTypes", {
  'aria-label': _pt.string,
  appendTo: _pt.oneOfType([_pt.element, _pt.func]),
  bodyContent: _pt.node.isRequired,
  boundary: _pt.oneOfType([_pt.oneOf(['scrollParent']), _pt.oneOf(['window']), _pt.oneOf(['viewport']), _pt.any]),
  children: _pt.element.isRequired,
  className: _pt.string,
  closeBtnAriaLabel: _pt.string,
  distance: _pt.number,
  enableFlip: _pt.bool,
  flipBehavior: _pt.oneOfType([_pt.oneOf(['flip']), _pt.arrayOf(_pt.oneOf(['top', 'bottom', 'left', 'right']))]),
  footerContent: _pt.node,
  headerContent: _pt.node,
  hideOnOutsideClick: _pt.bool,
  isVisible: _pt.bool,
  minWidth: _pt.string,
  maxWidth: _pt.string,
  onHidden: _pt.func,
  onHide: _pt.func,
  onMount: _pt.func,
  onShow: _pt.func,
  onShown: _pt.func,
  position: _pt.oneOf(['auto', 'top', 'bottom', 'left', 'right']),
  shouldClose: _pt.func,
  zIndex: _pt.number,
  tippyProps: _pt.any
});

_defineProperty(Popover, "defaultProps", {
  position: 'top',
  enableFlip: true,
  className: '',
  isVisible: null,
  shouldClose: () => null,
  'aria-label': '',
  headerContent: null,
  footerContent: null,
  appendTo: () => document.body,
  hideOnOutsideClick: true,
  onHide: () => null,
  onHidden: () => null,
  onShow: () => null,
  onShown: () => null,
  onMount: () => null,
  zIndex: 9999,
  maxWidth: popoverMaxWidth && popoverMaxWidth.value,
  closeBtnAriaLabel: 'Close',
  distance: 25,
  boundary: 'window',
  // For every initial starting position, there are 3 escape positions
  flipBehavior: ['top', 'right', 'bottom', 'left', 'top', 'right', 'bottom'],
  tippyProps: {}
});
//# sourceMappingURL=Popover.js.map