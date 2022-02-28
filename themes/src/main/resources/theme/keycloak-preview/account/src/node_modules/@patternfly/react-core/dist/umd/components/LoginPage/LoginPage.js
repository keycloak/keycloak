(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles", "../BackgroundImage", "../Brand", "../List", "./Login", "./LoginHeader", "./LoginFooter", "./LoginMainHeader", "./LoginMainBody", "./LoginMainFooter"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles"), require("../BackgroundImage"), require("../Brand"), require("../List"), require("./Login"), require("./LoginHeader"), require("./LoginFooter"), require("./LoginMainHeader"), require("./LoginMainBody"), require("./LoginMainFooter"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactStyles, global.BackgroundImage, global.Brand, global.List, global.Login, global.LoginHeader, global.LoginFooter, global.LoginMainHeader, global.LoginMainBody, global.LoginMainFooter);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactStyles, _BackgroundImage, _Brand, _List, _Login, _LoginHeader, _LoginFooter, _LoginMainHeader, _LoginMainBody, _LoginMainFooter) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.LoginPage = undefined;

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

  const LoginPage = exports.LoginPage = _ref => {
    let {
      children = null,
      className = '',
      brandImgSrc = '',
      brandImgAlt = '',
      backgroundImgSrc = '',
      backgroundImgAlt = '',
      footerListItems = null,
      textContent = '',
      footerListVariants,
      loginTitle,
      loginSubtitle,
      signUpForAccountMessage = null,
      forgotCredentials = null,
      socialMediaLoginContent = null
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "brandImgSrc", "brandImgAlt", "backgroundImgSrc", "backgroundImgAlt", "footerListItems", "textContent", "footerListVariants", "loginTitle", "loginSubtitle", "signUpForAccountMessage", "forgotCredentials", "socialMediaLoginContent"]);

    const HeaderBrand = React.createElement(React.Fragment, null, React.createElement(_Brand.Brand, {
      src: brandImgSrc,
      alt: brandImgAlt
    }));
    const Header = React.createElement(_LoginHeader.LoginHeader, {
      headerBrand: HeaderBrand
    });
    const Footer = React.createElement(_LoginFooter.LoginFooter, null, React.createElement("p", null, textContent), React.createElement(_List.List, {
      variant: footerListVariants
    }, footerListItems));
    return React.createElement(React.Fragment, null, backgroundImgSrc && React.createElement(_BackgroundImage.BackgroundImage, {
      src: backgroundImgSrc,
      alt: backgroundImgAlt
    }), React.createElement(_Login.Login, _extends({
      header: Header,
      footer: Footer,
      className: (0, _reactStyles.css)(className)
    }, props), React.createElement(_LoginMainHeader.LoginMainHeader, {
      title: loginTitle,
      subtitle: loginSubtitle
    }), React.createElement(_LoginMainBody.LoginMainBody, null, children), (socialMediaLoginContent || forgotCredentials || signUpForAccountMessage) && React.createElement(_LoginMainFooter.LoginMainFooter, {
      socialMediaLoginContent: socialMediaLoginContent,
      forgotCredentials: forgotCredentials,
      signUpForAccountMessage: signUpForAccountMessage
    })));
  };

  LoginPage.propTypes = {
    children: _propTypes2.default.node,
    className: _propTypes2.default.string,
    brandImgSrc: _propTypes2.default.string,
    brandImgAlt: _propTypes2.default.string,
    backgroundImgSrc: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.any]),
    backgroundImgAlt: _propTypes2.default.string,
    textContent: _propTypes2.default.string,
    footerListItems: _propTypes2.default.node,
    footerListVariants: _propTypes2.default.any,
    loginTitle: _propTypes2.default.string.isRequired,
    loginSubtitle: _propTypes2.default.string,
    signUpForAccountMessage: _propTypes2.default.node,
    forgotCredentials: _propTypes2.default.node,
    socialMediaLoginContent: _propTypes2.default.node
  };
});
//# sourceMappingURL=LoginPage.js.map