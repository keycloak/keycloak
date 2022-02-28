(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "../../helpers", "@patternfly/react-styles/css/components/Title/title", "@patternfly/react-styles/css/layouts/Bullseye/bullseye", "@patternfly/react-styles", "../Backdrop/Backdrop", "./ModalBoxBody", "./ModalBoxHeader", "./ModalBoxCloseButton", "./ModalBox", "./ModalBoxFooter", "./ModalBoxDescription"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("../../helpers"), require("@patternfly/react-styles/css/components/Title/title"), require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"), require("@patternfly/react-styles"), require("../Backdrop/Backdrop"), require("./ModalBoxBody"), require("./ModalBoxHeader"), require("./ModalBoxCloseButton"), require("./ModalBox"), require("./ModalBoxFooter"), require("./ModalBoxDescription"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.helpers, global.title, global.bullseye, global.reactStyles, global.Backdrop, global.ModalBoxBody, global.ModalBoxHeader, global.ModalBoxCloseButton, global.ModalBox, global.ModalBoxFooter, global.ModalBoxDescription);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _helpers, _title, _bullseye, _reactStyles, _Backdrop, _ModalBoxBody, _ModalBoxHeader, _ModalBoxCloseButton, _ModalBox, _ModalBoxFooter, _ModalBoxDescription) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.ModalContent = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _title2 = _interopRequireDefault(_title);

  var _bullseye2 = _interopRequireDefault(_bullseye);

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

  const ModalContent = exports.ModalContent = _ref => {
    let {
      children,
      className = '',
      isOpen = false,
      header = null,
      description = null,
      title,
      hideTitle = false,
      showClose = true,
      footer = null,
      actions = [],
      isFooterLeftAligned = false,
      onClose = () => undefined,
      isLarge = false,
      isSmall = false,
      width = -1,
      ariaDescribedById = '',
      id = '',
      disableFocusTrap = false
    } = _ref,
        props = _objectWithoutProperties(_ref, ["children", "className", "isOpen", "header", "description", "title", "hideTitle", "showClose", "footer", "actions", "isFooterLeftAligned", "onClose", "isLarge", "isSmall", "width", "ariaDescribedById", "id", "disableFocusTrap"]);

    if (!isOpen) {
      return null;
    }

    const modalBoxHeader = header ? React.createElement("div", {
      className: (0, _reactStyles.css)(_title2.default.title)
    }, header) : React.createElement(_ModalBoxHeader.ModalBoxHeader, {
      hideTitle: hideTitle
    }, " ", title, " ");
    const modalBoxFooter = footer ? React.createElement(_ModalBoxFooter.ModalBoxFooter, {
      isLeftAligned: isFooterLeftAligned
    }, footer) : actions.length > 0 && React.createElement(_ModalBoxFooter.ModalBoxFooter, {
      isLeftAligned: isFooterLeftAligned
    }, actions);
    const boxStyle = width === -1 ? {} : {
      width
    };
    const modalBox = React.createElement(_ModalBox.ModalBox, {
      style: boxStyle,
      className: className,
      isLarge: isLarge,
      isSmall: isSmall,
      title: title,
      id: ariaDescribedById || id
    }, showClose && React.createElement(_ModalBoxCloseButton.ModalBoxCloseButton, {
      onClose: onClose
    }), modalBoxHeader, description && React.createElement(_ModalBoxDescription.ModalBoxDescription, {
      id: id
    }, description), React.createElement(_ModalBoxBody.ModalBoxBody, _extends({}, props, !description && {
      id
    }), children), modalBoxFooter);
    return React.createElement(_Backdrop.Backdrop, null, React.createElement(_helpers.FocusTrap, {
      active: !disableFocusTrap,
      focusTrapOptions: {
        clickOutsideDeactivates: true
      },
      className: (0, _reactStyles.css)(_bullseye2.default.bullseye)
    }, modalBox));
  };

  ModalContent.propTypes = {
    children: _propTypes2.default.node.isRequired,
    className: _propTypes2.default.string,
    isLarge: _propTypes2.default.bool,
    isSmall: _propTypes2.default.bool,
    isOpen: _propTypes2.default.bool,
    header: _propTypes2.default.node,
    description: _propTypes2.default.node,
    title: _propTypes2.default.string.isRequired,
    hideTitle: _propTypes2.default.bool,
    showClose: _propTypes2.default.bool,
    width: _propTypes2.default.oneOfType([_propTypes2.default.number, _propTypes2.default.string]),
    footer: _propTypes2.default.node,
    actions: _propTypes2.default.any,
    isFooterLeftAligned: _propTypes2.default.bool,
    onClose: _propTypes2.default.func,
    ariaDescribedById: _propTypes2.default.string,
    id: _propTypes2.default.string.isRequired,
    disableFocusTrap: _propTypes2.default.bool
  };
});
//# sourceMappingURL=ModalContent.js.map