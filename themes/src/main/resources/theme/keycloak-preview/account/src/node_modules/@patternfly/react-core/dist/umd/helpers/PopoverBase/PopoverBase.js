(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "tippy.js"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("tippy.js"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.tippy);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _tippy) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

  var _tippy2 = _interopRequireDefault(_tippy);

  function _getRequireWildcardCache() {
    if (typeof WeakMap !== "function") return null;
    var cache = new WeakMap();

    _getRequireWildcardCache = function () {
      return cache;
    };

    return cache;
  }

  function _interopRequireWildcard(obj) {
    if (obj && obj.__esModule) {
      return obj;
    }

    var cache = _getRequireWildcardCache();

    if (cache && cache.has(obj)) {
      return cache.get(obj);
    }

    var newObj = {};

    if (obj != null) {
      var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor;

      for (var key in obj) {
        if (Object.prototype.hasOwnProperty.call(obj, key)) {
          var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null;

          if (desc && (desc.get || desc.set)) {
            Object.defineProperty(newObj, key, desc);
          } else {
            newObj[key] = obj[key];
          }
        }
      }
    }

    newObj.default = obj;

    if (cache) {
      cache.set(obj, newObj);
    }

    return newObj;
  }

  function _interopRequireDefault(obj) {
    return obj && obj.__esModule ? obj : {
      default: obj
    };
  }

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
    }

    return target;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
  }

  // eslint-disable-next-line @typescript-eslint/interface-name-prefix
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

      this.tip = (0, _tippy2.default)(ReactDOM.findDOMNode(this), this.options);
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
    children: _propTypes2.default.node.isRequired,
    content: _propTypes2.default.node.isRequired,
    isEnabled: _propTypes2.default.bool,
    isVisible: _propTypes2.default.bool,
    onCreate: _propTypes2.default.func,
    trigger: _propTypes2.default.string
  });

  _defineProperty(PopoverBase, "defaultProps", {
    trigger: 'mouseenter focus'
  });

  exports.default = PopoverBase;
});
//# sourceMappingURL=PopoverBase.js.map