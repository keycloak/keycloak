(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "@patternfly/react-styles/css/components/Login/login"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("@patternfly/react-styles/css/components/Login/login"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.login);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _login) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.LoginMainFooter = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _login2 = _interopRequireDefault(_login);

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

  const LoginMainFooter = exports.LoginMainFooter = _ref => {
    let {
      children = null,
      socialMediaLoginContent = null,
      signUpForAccountMessage = null,
      forgotCredentials = null,
      className = ''
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "socialMediaLoginContent", "signUpForAccountMessage", "forgotCredentials", "className"]);

    return React.createElement("div", _extends({
      className: (0, _reactStyles.css)(_login2.default.loginMainFooter, className)
    }, props), children, socialMediaLoginContent && React.createElement("ul", {
      className: (0, _reactStyles.css)(_login2.default.loginMainFooterLinks)
    }, socialMediaLoginContent), (signUpForAccountMessage || forgotCredentials) && React.createElement("div", {
      className: (0, _reactStyles.css)(_login2.default.loginMainFooterBand)
    }, signUpForAccountMessage, forgotCredentials));
  };

  LoginMainFooter.propTypes = {
    className: _propTypes2.default.string,
    children: _propTypes2.default.node,
    socialMediaLoginContent: _propTypes2.default.node,
    signUpForAccountMessage: _propTypes2.default.node,
    forgotCredentials: _propTypes2.default.node
  };
});
//# sourceMappingURL=LoginMainFooter.js.map