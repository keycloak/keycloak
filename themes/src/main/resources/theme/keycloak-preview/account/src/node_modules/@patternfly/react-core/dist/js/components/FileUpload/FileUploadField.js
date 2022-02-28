"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.FileUploadField = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _fileUpload = _interopRequireDefault(require("@patternfly/react-styles/css/components/FileUpload/file-upload"));

var _reactStyles = require("@patternfly/react-styles");

var _InputGroup = require("../InputGroup");

var _TextInput = require("../TextInput");

var _Button = require("../Button");

var _TextArea = require("../TextArea");

var _Spinner = require("../Spinner");

var _fileUtils = require("../../helpers/fileUtils");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var FileUploadField = function FileUploadField(_ref) {
  var id = _ref.id,
      type = _ref.type,
      _ref$value = _ref.value,
      value = _ref$value === void 0 ? '' : _ref$value,
      _ref$filename = _ref.filename,
      filename = _ref$filename === void 0 ? '' : _ref$filename,
      _ref$onChange = _ref.onChange,
      onChange = _ref$onChange === void 0 ? function () {} : _ref$onChange,
      _ref$onBrowseButtonCl = _ref.onBrowseButtonClick,
      onBrowseButtonClick = _ref$onBrowseButtonCl === void 0 ? function () {} : _ref$onBrowseButtonCl,
      _ref$onClearButtonCli = _ref.onClearButtonClick,
      onClearButtonClick = _ref$onClearButtonCli === void 0 ? function () {} : _ref$onClearButtonCli,
      _ref$className = _ref.className,
      className = _ref$className === void 0 ? '' : _ref$className,
      _ref$isDisabled = _ref.isDisabled,
      isDisabled = _ref$isDisabled === void 0 ? false : _ref$isDisabled,
      _ref$isReadOnly = _ref.isReadOnly,
      isReadOnly = _ref$isReadOnly === void 0 ? false : _ref$isReadOnly,
      _ref$isLoading = _ref.isLoading,
      isLoading = _ref$isLoading === void 0 ? false : _ref$isLoading,
      spinnerAriaValueText = _ref.spinnerAriaValueText,
      _ref$isRequired = _ref.isRequired,
      isRequired = _ref$isRequired === void 0 ? false : _ref$isRequired,
      _ref$isDragActive = _ref.isDragActive,
      isDragActive = _ref$isDragActive === void 0 ? false : _ref$isDragActive,
      _ref$validated = _ref.validated,
      validated = _ref$validated === void 0 ? 'default' : _ref$validated,
      _ref$ariaLabel = _ref['aria-label'],
      ariaLabel = _ref$ariaLabel === void 0 ? 'File upload' : _ref$ariaLabel,
      _ref$filenamePlacehol = _ref.filenamePlaceholder,
      filenamePlaceholder = _ref$filenamePlacehol === void 0 ? 'Drag a file here or browse to upload' : _ref$filenamePlacehol,
      _ref$filenameAriaLabe = _ref.filenameAriaLabel,
      filenameAriaLabel = _ref$filenameAriaLabe === void 0 ? filename ? 'Read only filename' : filenamePlaceholder : _ref$filenameAriaLabe,
      _ref$browseButtonText = _ref.browseButtonText,
      browseButtonText = _ref$browseButtonText === void 0 ? 'Browse...' : _ref$browseButtonText,
      _ref$clearButtonText = _ref.clearButtonText,
      clearButtonText = _ref$clearButtonText === void 0 ? 'Clear' : _ref$clearButtonText,
      _ref$isClearButtonDis = _ref.isClearButtonDisabled,
      isClearButtonDisabled = _ref$isClearButtonDis === void 0 ? !filename && !value : _ref$isClearButtonDis,
      _ref$containerRef = _ref.containerRef,
      containerRef = _ref$containerRef === void 0 ? null : _ref$containerRef,
      _ref$allowEditingUplo = _ref.allowEditingUploadedText,
      allowEditingUploadedText = _ref$allowEditingUplo === void 0 ? false : _ref$allowEditingUplo,
      _ref$hideDefaultPrevi = _ref.hideDefaultPreview,
      hideDefaultPreview = _ref$hideDefaultPrevi === void 0 ? false : _ref$hideDefaultPrevi,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      props = _objectWithoutProperties(_ref, ["id", "type", "value", "filename", "onChange", "onBrowseButtonClick", "onClearButtonClick", "className", "isDisabled", "isReadOnly", "isLoading", "spinnerAriaValueText", "isRequired", "isDragActive", "validated", "aria-label", "filenamePlaceholder", "filenameAriaLabel", "browseButtonText", "clearButtonText", "isClearButtonDisabled", "containerRef", "allowEditingUploadedText", "hideDefaultPreview", "children"]);

  var onTextAreaChange = function onTextAreaChange(newValue, event) {
    onChange(newValue, filename, event);
  };

  return React.createElement("div", _extends({
    className: (0, _reactStyles.css)(_fileUpload["default"].fileUpload, isDragActive && _fileUpload["default"].modifiers.dragHover, isLoading && _fileUpload["default"].modifiers.loading, className),
    ref: containerRef
  }, props), React.createElement("div", {
    className: _fileUpload["default"].fileUploadFileSelect
  }, React.createElement(_InputGroup.InputGroup, null, React.createElement(_TextInput.TextInput, {
    isReadOnly: true // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
    ,
    isDisabled: isDisabled,
    id: "".concat(id, "-filename"),
    name: "".concat(id, "-filename"),
    "aria-label": filenameAriaLabel,
    placeholder: filenamePlaceholder,
    "aria-describedby": "".concat(id, "-browse-button"),
    value: filename
  }), React.createElement(_Button.Button, {
    id: "".concat(id, "-browse-button"),
    variant: _Button.ButtonVariant.control,
    onClick: onBrowseButtonClick,
    isDisabled: isDisabled
  }, browseButtonText), React.createElement(_Button.Button, {
    variant: _Button.ButtonVariant.control,
    isDisabled: isDisabled || isClearButtonDisabled,
    onClick: onClearButtonClick
  }, clearButtonText))), React.createElement("div", {
    className: _fileUpload["default"].fileUploadFileDetails
  }, !hideDefaultPreview && type === _fileUtils.fileReaderType.text && React.createElement(_TextArea.TextArea, {
    readOnly: isReadOnly || !!filename && !allowEditingUploadedText,
    disabled: isDisabled,
    isRequired: isRequired,
    resizeOrientation: _TextArea.TextAreResizeOrientation.vertical,
    validated: validated,
    id: id,
    name: id,
    "aria-label": ariaLabel,
    value: value,
    onChange: onTextAreaChange
  }), isLoading && React.createElement("div", {
    className: _fileUpload["default"].fileUploadFileDetailsSpinner
  }, React.createElement(_Spinner.Spinner, {
    size: _Spinner.spinnerSize.lg,
    "aria-valuetext": spinnerAriaValueText
  }))), children);
};

exports.FileUploadField = FileUploadField;
FileUploadField.propTypes = {
  id: _propTypes["default"].string.isRequired,
  type: _propTypes["default"].oneOf(['text', 'dataURL']),
  value: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].any]),
  filename: _propTypes["default"].string,
  onChange: _propTypes["default"].func,
  className: _propTypes["default"].string,
  isDisabled: _propTypes["default"].bool,
  isReadOnly: _propTypes["default"].bool,
  isLoading: _propTypes["default"].bool,
  spinnerAriaValueText: _propTypes["default"].string,
  isRequired: _propTypes["default"].bool,
  validated: _propTypes["default"].oneOf(['success', 'error', 'default']),
  'aria-label': _propTypes["default"].string,
  filenamePlaceholder: _propTypes["default"].string,
  filenameAriaLabel: _propTypes["default"].string,
  browseButtonText: _propTypes["default"].string,
  clearButtonText: _propTypes["default"].string,
  isClearButtonDisabled: _propTypes["default"].bool,
  hideDefaultPreview: _propTypes["default"].bool,
  allowEditingUploadedText: _propTypes["default"].bool,
  children: _propTypes["default"].node,
  onBrowseButtonClick: _propTypes["default"].func,
  onClearButtonClick: _propTypes["default"].func,
  isDragActive: _propTypes["default"].bool,
  containerRef: _propTypes["default"].oneOfType([_propTypes["default"].string, _propTypes["default"].func, _propTypes["default"].object])
};
//# sourceMappingURL=FileUploadField.js.map