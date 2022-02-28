(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dom", "../../helpers", "./AlertGroupInline"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dom"), require("../../helpers"), require("./AlertGroupInline"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDom, global.helpers, global.AlertGroupInline);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDom, _helpers, _AlertGroupInline) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.AlertGroup = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var ReactDOM = _interopRequireWildcard(_reactDom);

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

  class AlertGroup extends React.Component {
    constructor(...args) {
      super(...args);

      _defineProperty(this, "state", {
        container: undefined
      });
    }

    componentDidMount() {
      const container = document.createElement('div');
      const target = this.getTargetElement();
      this.setState({
        container
      });
      target.appendChild(container);
    }

    componentWillUnmount() {
      const target = this.getTargetElement();

      if (this.state.container) {
        target.removeChild(this.state.container);
      }
    }

    getTargetElement() {
      const appendTo = this.props.appendTo;

      if (typeof appendTo === 'function') {
        return appendTo();
      }

      return appendTo || document.body;
    }

    render() {
      const {
        className,
        children,
        isToast
      } = this.props;
      const alertGroup = React.createElement(_AlertGroupInline.AlertGroupInline, {
        className: className,
        isToast: isToast
      }, children);

      if (!this.props.isToast) {
        return alertGroup;
      }

      const container = this.state.container;

      if (!_helpers.canUseDOM || !container) {
        return null;
      }

      return ReactDOM.createPortal(alertGroup, container);
    }

  }

  exports.AlertGroup = AlertGroup;

  _defineProperty(AlertGroup, "propTypes", {
    className: _propTypes2.default.string,
    children: _propTypes2.default.node,
    isToast: _propTypes2.default.bool,
    appendTo: _propTypes2.default.oneOfType([_propTypes2.default.any, _propTypes2.default.func])
  });
});
//# sourceMappingURL=AlertGroup.js.map