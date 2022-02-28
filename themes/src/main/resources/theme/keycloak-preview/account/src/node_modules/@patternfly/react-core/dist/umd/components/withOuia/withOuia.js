(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "./ouia"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("./ouia"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.ouia);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _ouia) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.OuiaContext = undefined;
  exports.withOuiaContext = withOuiaContext;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

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

  const OuiaContext = exports.OuiaContext = React.createContext(null);
  /**
   * @param { React.ComponentClass | React.FunctionComponent } WrappedComponent - React component
   */

  function withOuiaContext(WrappedComponent) {
    /* eslint-disable react/display-name */
    return props => React.createElement(OuiaContext.Consumer, null, value => React.createElement(ComponentWithOuia, {
      consumerContext: value,
      component: WrappedComponent,
      componentProps: props
    }));
    /* eslint-enable react/display-name */
  }

  class ComponentWithOuia extends React.Component {
    constructor(props) {
      super(props);
      this.state = {
        isOuia: false,
        ouiaId: null
      };
    }
    /**
     * if either consumer set isOuia through context or local storage
     * then force a re-render
     */


    componentDidMount() {
      const {
        isOuia,
        ouiaId
      } = this.state;
      const {
        consumerContext
      } = this.props;
      const isOuiaEnv = (0, _ouia.isOUIAEnvironment)();

      if (consumerContext && consumerContext.isOuia !== undefined && consumerContext.isOuia !== isOuia || isOuiaEnv !== isOuia) {
        this.setState({
          isOuia: consumerContext && consumerContext.isOuia !== undefined ? consumerContext.isOuia : isOuiaEnv,
          ouiaId: consumerContext && consumerContext.ouiaId !== undefined ? consumerContext.ouiaId : (0, _ouia.generateOUIAId)() ? (0, _ouia.getUniqueId)() : ouiaId
        });
      }
    }

    render() {
      const {
        isOuia,
        ouiaId
      } = this.state;
      const {
        component: WrappedComponent,
        componentProps,
        consumerContext
      } = this.props;
      return React.createElement(OuiaContext.Provider, {
        value: {
          isOuia: consumerContext && consumerContext.isOuia || isOuia,
          ouiaId: consumerContext && consumerContext.ouiaId || ouiaId
        }
      }, React.createElement(OuiaContext.Consumer, null, value => React.createElement(WrappedComponent, _extends({}, componentProps, {
        ouiaContext: value
      }))));
    }

  }

  _defineProperty(ComponentWithOuia, "propTypes", {
    component: _propTypes2.default.any.isRequired,
    componentProps: _propTypes2.default.any.isRequired,
    consumerContext: _propTypes2.default.shape({
      isOuia: _propTypes2.default.bool,
      ouiaId: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string])
    })
  });
});
//# sourceMappingURL=withOuia.js.map