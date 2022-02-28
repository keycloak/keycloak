"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.FileUpload = void 0;

var _propTypes = _interopRequireDefault(require("prop-types"));

var React = _interopRequireWildcard(require("react"));

var _reactDropzone = _interopRequireDefault(require("react-dropzone"));

var _FileUploadField = require("./FileUploadField");

var _fileUtils = require("../../helpers/fileUtils");

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

var FileUpload = function FileUpload(_ref) {
  var id = _ref.id,
      type = _ref.type,
      _ref$value = _ref.value,
      value = _ref$value === void 0 ? type === _fileUtils.fileReaderType.text || type === _fileUtils.fileReaderType.dataURL ? '' : null : _ref$value,
      _ref$filename = _ref.filename,
      filename = _ref$filename === void 0 ? '' : _ref$filename,
      _ref$children = _ref.children,
      children = _ref$children === void 0 ? null : _ref$children,
      _ref$onChange = _ref.onChange,
      onChange = _ref$onChange === void 0 ? function () {} : _ref$onChange,
      _ref$onReadStarted = _ref.onReadStarted,
      onReadStarted = _ref$onReadStarted === void 0 ? function () {} : _ref$onReadStarted,
      _ref$onReadFinished = _ref.onReadFinished,
      onReadFinished = _ref$onReadFinished === void 0 ? function () {} : _ref$onReadFinished,
      _ref$onReadFailed = _ref.onReadFailed,
      onReadFailed = _ref$onReadFailed === void 0 ? function () {} : _ref$onReadFailed,
      _ref$dropzoneProps = _ref.dropzoneProps,
      dropzoneProps = _ref$dropzoneProps === void 0 ? {} : _ref$dropzoneProps,
      props = _objectWithoutProperties(_ref, ["id", "type", "value", "filename", "children", "onChange", "onReadStarted", "onReadFinished", "onReadFailed", "dropzoneProps"]);

  var onDropAccepted = function onDropAccepted(acceptedFiles, event) {
    if (acceptedFiles.length > 0) {
      var _fileHandle = acceptedFiles[0];

      if (type === _fileUtils.fileReaderType.text || type === _fileUtils.fileReaderType.dataURL) {
        onChange('', _fileHandle.name, event); // Show the filename while reading

        onReadStarted(_fileHandle);
        (0, _fileUtils.readFile)(_fileHandle, type).then(function (data) {
          onReadFinished(_fileHandle);
          onChange(data, _fileHandle.name, event);
        })["catch"](function (error) {
          onReadFailed(error, _fileHandle);
          onReadFinished(_fileHandle);
          onChange('', '', event); // Clear the filename field on a failure
        });
      } else {
        onChange(_fileHandle, _fileHandle.name, event);
      }
    }

    dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
  };

  var onDropRejected = function onDropRejected(rejectedFiles, event) {
    if (rejectedFiles.length > 0) {
      onChange('', rejectedFiles[0].name, event);
    }

    dropzoneProps.onDropRejected && dropzoneProps.onDropRejected(rejectedFiles, event);
  };

  var onClearButtonClick = function onClearButtonClick(event) {
    onChange('', '', event);
  };

  return React.createElement(_reactDropzone["default"], _extends({
    multiple: false
  }, dropzoneProps, {
    onDropAccepted: onDropAccepted,
    onDropRejected: onDropRejected
  }), function (_ref2) {
    var getRootProps = _ref2.getRootProps,
        getInputProps = _ref2.getInputProps,
        isDragActive = _ref2.isDragActive,
        open = _ref2.open;
    return React.createElement(_FileUploadField.FileUploadField, _extends({}, getRootProps(_objectSpread({}, props, {
      refKey: 'containerRef',
      onClick: function onClick(event) {
        return event.preventDefault();
      } // Prevents clicking TextArea from opening file dialog

    })), {
      tabIndex: null // Omit the unwanted tabIndex from react-dropzone's getRootProps
      ,
      id: id,
      type: type,
      filename: filename,
      value: value,
      onChange: onChange,
      isDragActive: isDragActive,
      onBrowseButtonClick: open,
      onClearButtonClick: onClearButtonClick
    }), React.createElement("input", getInputProps()), children);
  });
};

exports.FileUpload = FileUpload;
FileUpload.propTypes = {
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
  hideDefaultPreview: _propTypes["default"].bool,
  allowEditingUploadedText: _propTypes["default"].bool,
  children: _propTypes["default"].node,
  onReadStarted: _propTypes["default"].func,
  onReadFinished: _propTypes["default"].func,
  onReadFailed: _propTypes["default"].func,
  dropzoneProps: _propTypes["default"].any
};
//# sourceMappingURL=FileUpload.js.map