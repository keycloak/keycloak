import _pt from "prop-types";

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import createFocusTrap from 'focus-trap';
export class FocusTrap extends React.Component {
  constructor(props) {
    super(props);

    _defineProperty(this, "divRef", React.createRef());

    if (typeof document !== 'undefined') {
      this.previouslyFocusedElement = document.activeElement;
    }
  }

  componentDidMount() {
    // We need to hijack the returnFocusOnDeactivate option,
    // because React can move focus into the element before we arrived at
    // this lifecycle hook (e.g. with autoFocus inputs). So the component
    // captures the previouslyFocusedElement in componentWillMount,
    // then (optionally) returns focus to it in componentWillUnmount.
    this.focusTrap = createFocusTrap(this.divRef.current, _objectSpread({}, this.props.focusTrapOptions, {
      returnFocusOnDeactivate: false
    }));

    if (this.props.active) {
      this.focusTrap.activate();
    }

    if (this.props.paused) {
      this.focusTrap.pause();
    }
  }

  componentDidUpdate(prevProps) {
    if (prevProps.active && !this.props.active) {
      const {
        returnFocusOnDeactivate
      } = this.props.focusTrapOptions;
      const returnFocus = returnFocusOnDeactivate || false;
      const config = {
        returnFocus
      };
      this.focusTrap.deactivate(config);
    } else if (!prevProps.active && this.props.active) {
      this.focusTrap.activate();
    }

    if (prevProps.paused && !this.props.paused) {
      this.focusTrap.unpause();
    } else if (!prevProps.paused && this.props.paused) {
      this.focusTrap.pause();
    }
  }

  componentWillUnmount() {
    this.focusTrap.deactivate();

    if (this.props.focusTrapOptions.returnFocusOnDeactivate !== false && this.previouslyFocusedElement && this.previouslyFocusedElement.focus) {
      this.previouslyFocusedElement.focus();
    }
  }

  render() {
    return React.createElement("div", {
      ref: this.divRef,
      className: this.props.className
    }, this.props.children);
  }

}

_defineProperty(FocusTrap, "propTypes", {
  children: _pt.node.isRequired,
  className: _pt.string,
  active: _pt.bool,
  paused: _pt.bool,
  focusTrapOptions: _pt.any
});

_defineProperty(FocusTrap, "defaultProps", {
  active: true,
  paused: false,
  focusTrapOptions: {}
});
//# sourceMappingURL=FocusTrap.js.map