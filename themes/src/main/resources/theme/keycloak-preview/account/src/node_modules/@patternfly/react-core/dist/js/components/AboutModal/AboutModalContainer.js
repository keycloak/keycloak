"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.AboutModalContainer = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactStyles = require("@patternfly/react-styles");

var _bullseye = _interopRequireDefault(require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"));

var _helpers = require("../../helpers");

var _AboutModalBoxContent = require("./AboutModalBoxContent");

var _AboutModalBoxHeader = require("./AboutModalBoxHeader");

var _AboutModalBoxHero = require("./AboutModalBoxHero");

var _AboutModalBoxBrand = require("./AboutModalBoxBrand");

var _AboutModalBoxCloseButton = require("./AboutModalBoxCloseButton");

var _AboutModalBox = require("./AboutModalBox");

var _Backdrop = require("../Backdrop/Backdrop");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var AboutModalContainer = function AboutModalContainer(_ref) {
  var children = _ref.children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$onClose = _ref.onClose,
      onClose = _ref$onClose === void 0 ? function () {
    return undefined;
  } : _ref$onClose,
      _ref$productName = _ref.productName,
      productName = _ref$productName === void 0 ? '' : _ref$productName,
      trademark = _ref.trademark,
      brandImageSrc = _ref.brandImageSrc,
      brandImageAlt = _ref.brandImageAlt,
      backgroundImageSrc = _ref.backgroundImageSrc,
      ariaLabelledbyId = _ref.ariaLabelledbyId,
      ariaDescribedById = _ref.ariaDescribedById,
      closeButtonAriaLabel = _ref.closeButtonAriaLabel,
      props = _objectWithoutProperties(_ref, ["children", "className", "isOpen", "onClose", "productName", "trademark", "brandImageSrc", "brandImageAlt", "backgroundImageSrc", "ariaLabelledbyId", "ariaDescribedById", "closeButtonAriaLabel"]);

  if (!isOpen) {
    return null;
  }

  return React.createElement(_Backdrop.Backdrop, null, React.createElement(_helpers.FocusTrap, {
    focusTrapOptions: {
      clickOutsideDeactivates: true
    },
    className: (0, _reactStyles.css)(_bullseye["default"].bullseye)
  }, React.createElement(_AboutModalBox.AboutModalBox, {
    className: className,
    "aria-labelledby": ariaLabelledbyId,
    "aria-describedby": ariaDescribedById
  }, React.createElement(_AboutModalBoxBrand.AboutModalBoxBrand, {
    src: brandImageSrc,
    alt: brandImageAlt
  }), React.createElement(_AboutModalBoxCloseButton.AboutModalBoxCloseButton, {
    "aria-label": closeButtonAriaLabel,
    onClose: onClose
  }), productName && React.createElement(_AboutModalBoxHeader.AboutModalBoxHeader, {
    id: ariaLabelledbyId,
    productName: productName
  }), React.createElement(_AboutModalBoxContent.AboutModalBoxContent, _extends({
    trademark: trademark,
    id: ariaDescribedById,
    noAboutModalBoxContentContainer: false
  }, props), children), React.createElement(_AboutModalBoxHero.AboutModalBoxHero, {
    backgroundImageSrc: backgroundImageSrc
  }))));
};

exports.AboutModalContainer = AboutModalContainer;
AboutModalContainer.propTypes = {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isOpen: _propTypes["default"].bool,
  onClose: _propTypes["default"].func,
  productName: _propTypes["default"].string,
  trademark: _propTypes["default"].string,
  brandImageSrc: _propTypes["default"].string.isRequired,
  brandImageAlt: _propTypes["default"].string.isRequired,
  backgroundImageSrc: _propTypes["default"].string,
  ariaLabelledbyId: _propTypes["default"].string.isRequired,
  ariaDescribedById: _propTypes["default"].string.isRequired,
  closeButtonAriaLabel: _propTypes["default"].string
};
//# sourceMappingURL=AboutModalContainer.js.map