(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "../../helpers", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Backdrop/backdrop", "../../helpers/constants", "./ModalContent"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("../../helpers"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Backdrop/backdrop"), require("../../helpers/constants"), require("./ModalContent"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.helpers, global.reactStyles, global.backdrop, global.constants, global.ModalContent);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _helpers, _reactStyles, _backdrop, _constants, _ModalContent) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.Modal = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

  var _backdrop2 = _interopRequireDefault(_backdrop);

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

  function _extends() {
    _extends = Object.assign || function (target) {
      for (var i = 1; i < arguments.length; i++) {
        var source = arguments[i];

        for (var key in source) {
          if (Object.prototype.hasOwnProperty.call(source, key)) {
            target[key] = source[key];
          }
        }
      }

      return target;
    };

    return _extends.apply(this, arguments);
  }

  function _objectWithoutProperties(source, excluded) {
    if (source == null) return {};

    var target = _objectWithoutPropertiesLoose(source, excluded);

    var key, i;

    if (Object.getOwnPropertySymbols) {
      var sourceSymbolKeys = Object.getOwnPropertySymbols(source);

      for (i = 0; i < sourceSymbolKeys.length; i++) {
        key = sourceSymbolKeys[i];
        if (excluded.indexOf(key) >= 0) continue;
        if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue;
        target[key] = source[key];
      }
    }

    return target;
  }

  function _objectWithoutPropertiesLoose(source, excluded) {
    if (source == null) return {};
    var target = {};
    var sourceKeys = Object.keys(source);
    var key, i;

    for (i = 0; i < sourceKeys.length; i++) {
      key = sourceKeys[i];
      if (excluded.indexOf(key) >= 0) continue;
      target[key] = source[key];
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

  class Modal extends React.Component {
    constructor(props) {
      super(props);

      _defineProperty(this, "id", '');

      _defineProperty(this, "handleEscKeyClick", event => {
        if (event.keyCode === _constants.KEY_CODES.ESCAPE_KEY && this.props.isOpen) {
          this.props.onClose();
        }
      });

      _defineProperty(this, "getElement", appendTo => {
        let target;

        if (typeof appendTo === 'function') {
          target = appendTo();
        } else {
          target = appendTo;
        }

        return target;
      });

      _defineProperty(this, "toggleSiblingsFromScreenReaders", hide => {
        const {
          appendTo
        } = this.props;
        const target = this.getElement(appendTo);
        const bodyChildren = target.children;

        for (const child of Array.from(bodyChildren)) {
          if (child !== this.state.container) {
            hide ? child.setAttribute('aria-hidden', '' + hide) : child.removeAttribute('aria-hidden');
          }
        }
      });

      const newId = Modal.currentId++;
      this.id = `pf-modal-${newId}`;
      this.state = {
        container: undefined
      };
    }

    componentDidMount() {
      const {
        appendTo
      } = this.props;
      const target = this.getElement(appendTo);
      const container = document.createElement('div');
      this.setState({
        container
      });
      target.appendChild(container);
      target.addEventListener('keydown', this.handleEscKeyClick, false);

      if (this.props.isOpen) {
        target.classList.add((0, _reactStyles.css)(_backdrop2.default.backdropOpen));
      } else {
        target.classList.remove((0, _reactStyles.css)(_backdrop2.default.backdropOpen));
      }
    }

    componentDidUpdate() {
      const {
        appendTo
      } = this.props;
      const target = this.getElement(appendTo);

      if (this.props.isOpen) {
        target.classList.add((0, _reactStyles.css)(_backdrop2.default.backdropOpen));
        this.toggleSiblingsFromScreenReaders(true);
      } else {
        target.classList.remove((0, _reactStyles.css)(_backdrop2.default.backdropOpen));
        this.toggleSiblingsFromScreenReaders(false);
      }
    }

    componentWillUnmount() {
      const {
        appendTo
      } = this.props;
      const target = this.getElement(appendTo);

      if (this.state.container) {
        target.removeChild(this.state.container);
      }

      target.removeEventListener('keydown', this.handleEscKeyClick, false);
      target.classList.remove((0, _reactStyles.css)(_backdrop2.default.backdropOpen));
    }

    render() {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      const _this$props = this.props,
            {
        appendTo
      } = _this$props,
            props = _objectWithoutProperties(_this$props, ["appendTo"]);

      const {
        container
      } = this.state;

      if (!_helpers.canUseDOM || !container) {
        return null;
      }

      return ReactDOM.createPortal(React.createElement(_ModalContent.ModalContent, _extends({}, props, {
        title: this.props.title,
        id: this.id,
        ariaDescribedById: this.props.ariaDescribedById
      })), container);
    }

  }

  exports.Modal = Modal;

  _defineProperty(Modal, "propTypes", {
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    isOpen: _propTypes2.default.bool,
    header: _propTypes2.default.node,
    title: _propTypes2.default.string.isRequired,
    hideTitle: _propTypes2.default.bool,
    showClose: _propTypes2.default.bool,
    ariaDescribedById: _propTypes2.default.string,
    footer: _propTypes2.default.node,
    actions: _propTypes2.default.any,
    isFooterLeftAligned: _propTypes2.default.bool,
    onClose: _propTypes2.default.func,
    width: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    isLarge: _propTypes2.default.bool,
    isSmall: _propTypes2.default.bool,
    appendTo: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.func]),
    disableFocusTrap: _propTypes2.default.bool,
    description: _propTypes2.default.node
  });

  _defineProperty(Modal, "currentId", 0);

  _defineProperty(Modal, "defaultProps", {
    className: '',
    isOpen: false,
    hideTitle: false,
    showClose: true,
    ariaDescribedById: '',
    actions: [],
    isFooterLeftAligned: false,
    onClose: () => undefined,
    isLarge: false,
    isSmall: false,
    appendTo: typeof document !== 'undefined' && document.body || null
  });
});
//# sourceMappingURL=Modal.js.map