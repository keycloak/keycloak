"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.ModalContent = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _helpers = require("../../helpers");

var _title = _interopRequireDefault(require("@patternfly/react-styles/css/components/Title/title"));

var _bullseye = _interopRequireDefault(require("@patternfly/react-styles/css/layouts/Bullseye/bullseye"));

var _reactStyles = require("@patternfly/react-styles");

var _Backdrop = require("../Backdrop/Backdrop");

var _ModalBoxBody = require("./ModalBoxBody");

var _ModalBoxHeader = require("./ModalBoxHeader");

var _ModalBoxCloseButton = require("./ModalBoxCloseButton");

var _ModalBox = require("./ModalBox");

var _ModalBoxFooter = require("./ModalBoxFooter");

var _ModalBoxDescription = require("./ModalBoxDescription");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var ModalContent = function ModalContent(_ref) {
  var children = _ref.children,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isOpen = _ref.isOpen,
      isOpen = _ref$isOpen === void 0 ? false : _ref$isOpen,
      _ref$header = _ref.header,
      header = _ref$header === void 0 ? null : _ref$header,
      _ref$description = _ref.description,
      description = _ref$description === void 0 ? null : _ref$description,
      title = _ref.title,
      _ref$hideTitle = _ref.hideTitle,
      hideTitle = _ref$hideTitle === void 0 ? false : _ref$hideTitle,
      _ref$showClose = _ref.showClose,
      showClose = _ref$showClose === void 0 ? true : _ref$showClose,
      _ref$footer = _ref.footer,
      footer = _ref$footer === void 0 ? null : _ref$footer,
      _ref$actions = _ref.actions,
      actions = _ref$actions === void 0 ? [] : _ref$actions,
      _ref$isFooterLeftAlig = _ref.isFooterLeftAligned,
      isFooterLeftAligned = _ref$isFooterLeftAlig === void 0 ? false : _ref$isFooterLeftAlig,
      _ref$onClose = _ref.onClose,
      onClose = _ref$onClose === void 0 ? function () {
    return undefined;
  } : _ref$onClose,
      _ref$isLarge = _ref.isLarge,
      isLarge = _ref$isLarge === void 0 ? false : _ref$isLarge,
      _ref$isSmall = _ref.isSmall,
      isSmall = _ref$isSmall === void 0 ? false : _ref$isSmall,
      _ref$width = _ref.width,
      width = _ref$width === void 0 ? -1 : _ref$width,
      _ref$ariaDescribedByI = _ref.ariaDescribedById,
      ariaDescribedById = _ref$ariaDescribedByI === void 0 ? '' : _ref$ariaDescribedByI,
      _ref$id = _ref.id,
      id = _ref$id === void 0 ? '' : _ref$id,
      _ref$disableFocusTrap = _ref.disableFocusTrap,
      disableFocusTrap = _ref$disableFocusTrap === void 0 ? false : _ref$disableFocusTrap,
      props = _objectWithoutProperties(_ref, ["children", "className", "isOpen", "header", "description", "title", "hideTitle", "showClose", "footer", "actions", "isFooterLeftAligned", "onClose", "isLarge", "isSmall", "width", "ariaDescribedById", "id", "disableFocusTrap"]);

  if (!isOpen) {
    return null;
  }

  var modalBoxHeader = header ? React.createElement("div", {
    className: (0, _reactStyles.css)(_title["default"].title)
  }, header) : React.createElement(_ModalBoxHeader.ModalBoxHeader, {
    hideTitle: hideTitle
  }, " ", title, " ");
  var modalBoxFooter = footer ? React.createElement(_ModalBoxFooter.ModalBoxFooter, {
    isLeftAligned: isFooterLeftAligned
  }, footer) : actions.length > 0 && React.createElement(_ModalBoxFooter.ModalBoxFooter, {
    isLeftAligned: isFooterLeftAligned
  }, actions);
  var boxStyle = width === -1 ? {} : {
    width: width
  };
  var modalBox = React.createElement(_ModalBox.ModalBox, {
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
    id: id
  }), children), modalBoxFooter);
  return React.createElement(_Backdrop.Backdrop, null, React.createElement(_helpers.FocusTrap, {
    active: !disableFocusTrap,
    focusTrapOptions: {
      clickOutsideDeactivates: true
    },
    className: (0, _reactStyles.css)(_bullseye["default"].bullseye)
  }, modalBox));
};

exports.ModalContent = ModalContent;
ModalContent.propTypes = {
  children: _propTypes["default"].node.isRequired,
  className: _propTypes["default"].string,
  isLarge: _propTypes["default"].bool,
  isSmall: _propTypes["default"].bool,
  isOpen: _propTypes["default"].bool,
  header: _propTypes["default"].node,
  description: _propTypes["default"].node,
  title: _propTypes["default"].string.isRequired,
  hideTitle: _propTypes["default"].bool,
  showClose: _propTypes["default"].bool,
  width: _propTypes["default"].oneOfType([_propTypes["default"].number, _propTypes["default"].string]),
  footer: _propTypes["default"].node,
  actions: _propTypes["default"].any,
  isFooterLeftAligned: _propTypes["default"].bool,
  onClose: _propTypes["default"].func,
  ariaDescribedById: _propTypes["default"].string,
  id: _propTypes["default"].string.isRequired,
  disableFocusTrap: _propTypes["default"].bool
};
//# sourceMappingURL=ModalContent.js.map