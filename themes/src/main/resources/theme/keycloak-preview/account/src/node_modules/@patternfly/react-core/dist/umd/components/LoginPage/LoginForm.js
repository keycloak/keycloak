(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../Form", "../TextInput", "../Button", "../Checkbox"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../Form"), require("../TextInput"), require("../Button"), require("../Checkbox"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.Form, global.TextInput, global.Button, global.Checkbox);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _Form, _TextInput, _Button, _Checkbox) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.LoginForm = undefined;

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

  const LoginForm = exports.LoginForm = _ref => {
    let {
      noAutoFocus = false,
      className = '',
      showHelperText = false,
      helperText = null,
      usernameLabel = 'Username',
      usernameValue = '',
      onChangeUsername = () => undefined,
      isValidUsername = true,
      passwordLabel = 'Password',
      passwordValue = '',
      onChangePassword = () => undefined,
      isValidPassword = true,
      loginButtonLabel = 'Log In',
      isLoginButtonDisabled = false,
      onLoginButtonClick = () => undefined,
      rememberMeLabel = '',
      isRememberMeChecked = false,
      onChangeRememberMe = () => undefined,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      rememberMeAriaLabel = ''
    } = _ref,
        props = _objectWithoutProperties(_ref, ["noAutoFocus", "className", "showHelperText", "helperText", "usernameLabel", "usernameValue", "onChangeUsername", "isValidUsername", "passwordLabel", "passwordValue", "onChangePassword", "isValidPassword", "loginButtonLabel", "isLoginButtonDisabled", "onLoginButtonClick", "rememberMeLabel", "isRememberMeChecked", "onChangeRememberMe", "rememberMeAriaLabel"]);

    return React.createElement(_Form.Form, _extends({
      className: className
    }, props), React.createElement(_Form.FormHelperText, {
      isError: !isValidUsername || !isValidPassword,
      isHidden: !showHelperText
    }, helperText), React.createElement(_Form.FormGroup, {
      label: usernameLabel,
      isRequired: true,
      isValid: isValidUsername,
      fieldId: "pf-login-username-id"
    }, React.createElement(_TextInput.TextInput, {
      autoFocus: !noAutoFocus,
      id: "pf-login-username-id",
      isRequired: true,
      isValid: isValidUsername,
      type: "text",
      name: "pf-login-username-id",
      value: usernameValue,
      onChange: onChangeUsername
    })), React.createElement(_Form.FormGroup, {
      label: passwordLabel,
      isRequired: true,
      isValid: isValidPassword,
      fieldId: "pf-login-password-id"
    }, React.createElement(_TextInput.TextInput, {
      isRequired: true,
      type: "password",
      id: "pf-login-password-id",
      name: "pf-login-password-id",
      isValid: isValidPassword,
      value: passwordValue,
      onChange: onChangePassword
    })), rememberMeLabel.length > 0 && React.createElement(_Form.FormGroup, {
      fieldId: "pf-login-remember-me-id"
    }, React.createElement(_Checkbox.Checkbox, {
      id: "pf-login-remember-me-id",
      label: rememberMeLabel,
      isChecked: isRememberMeChecked,
      onChange: onChangeRememberMe
    })), React.createElement(_Form.ActionGroup, null, React.createElement(_Button.Button, {
      variant: "primary",
      type: "submit",
      onClick: onLoginButtonClick,
      isBlock: true,
      isDisabled: isLoginButtonDisabled
    }, loginButtonLabel)));
  };

  LoginForm.propTypes = {
    noAutoFocus: _propTypes2.default.bool,
    className: _propTypes2.default.string,
    showHelperText: _propTypes2.default.bool,
    helperText: _propTypes2.default.node,
    usernameLabel: _propTypes2.default.string,
    usernameValue: _propTypes2.default.string,
    onChangeUsername: _propTypes2.default.func,
    isValidUsername: _propTypes2.default.bool,
    passwordLabel: _propTypes2.default.string,
    passwordValue: _propTypes2.default.string,
    onChangePassword: _propTypes2.default.func,
    isValidPassword: _propTypes2.default.bool,
    loginButtonLabel: _propTypes2.default.string,
    isLoginButtonDisabled: _propTypes2.default.bool,
    onLoginButtonClick: _propTypes2.default.func,
    rememberMeLabel: _propTypes2.default.string,
    isRememberMeChecked: _propTypes2.default.bool,
    onChangeRememberMe: _propTypes2.default.func,
    rememberMeAriaLabel: _propTypes2.default.string
  };
});
//# sourceMappingURL=LoginForm.js.map