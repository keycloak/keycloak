import _pt from "prop-types";

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import tippy from 'tippy.js'; // eslint-disable-next-line @typescript-eslint/interface-name-prefix

// These props are not native to `tippy.js` and are specific to React only.
const REACT_ONLY_PROPS = ['children', 'onCreate', 'isVisible', 'isEnabled'];
/** Avoid Babel's large '_objectWithoutProperties' helper function.
 *
 * @param {object} props - Props object
 */

function getNativeTippyProps(props) {
  return Object.keys(props).filter(prop => !REACT_ONLY_PROPS.includes(prop)).reduce((acc, key) => {
    acc[key] = props[key];
    return acc;
  }, {});
}

class PopoverBase extends React.Component {
  constructor(...args) {
    super(...args);

    _defineProperty(this, "state", {
      isMounted: false
    });

    _defineProperty(this, "container", typeof document !== 'undefined' && document.createElement('div'));
  }

  get isReactElementContent() {
    return React.isValidElement(this.props.content);
  }

  get options() {
    return _objectSpread({}, getNativeTippyProps(this.props), {
      content: this.isReactElementContent ? this.container : this.props.content
    });
  }

  get isManualTrigger() {
    return this.props.trigger === 'manual';
  }

  componentDidMount() {
    this.setState({
      isMounted: true
    });
    /* eslint-disable-next-line */

    this.tip = tippy(ReactDOM.findDOMNode(this), this.options);
    const {
      onCreate,
      isEnabled,
      isVisible
    } = this.props;

    if (onCreate) {
      onCreate(this.tip);
    }

    if (isEnabled === false) {
      this.tip.disable();
    }

    if (this.isManualTrigger && isVisible === true) {
      this.tip.show();
    }
  }

  componentDidUpdate() {
    this.tip.setProps(this.options);
    const {
      isEnabled,
      isVisible
    } = this.props;

    if (isEnabled === true) {
      this.tip.enable();
    }

    if (isEnabled === false) {
      this.tip.disable();
    }

    if (this.isManualTrigger) {
      if (isVisible === true) {
        this.tip.show();
      }

      if (isVisible === false) {
        this.tip.hide();
      }
    }
  }

  componentWillUnmount() {
    this.tip.destroy();
    this.tip = null;
  }

  render() {
    return React.createElement(React.Fragment, null, this.props.children, this.isReactElementContent && this.state.isMounted && ReactDOM.createPortal(this.props.content, this.container));
  }

}

_defineProperty(PopoverBase, "propTypes", {
  children: _pt.node.isRequired,
  content: _pt.node.isRequired,
  isEnabled: _pt.bool,
  isVisible: _pt.bool,
  onCreate: _pt.func,
  trigger: _pt.string
});

_defineProperty(PopoverBase, "defaultProps", {
  trigger: 'mouseenter focus'
});

export default PopoverBase;
//# sourceMappingURL=PopoverBase.js.map