import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function ownKeys(object, enumerableOnly) { var keys = Object.keys(object); if (Object.getOwnPropertySymbols) { var symbols = Object.getOwnPropertySymbols(object); if (enumerableOnly) symbols = symbols.filter(function (sym) { return Object.getOwnPropertyDescriptor(object, sym).enumerable; }); keys.push.apply(keys, symbols); } return keys; }

function _objectSpread(target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i] != null ? arguments[i] : {}; if (i % 2) { ownKeys(source, true).forEach(function (key) { _defineProperty(target, key, source[key]); }); } else if (Object.getOwnPropertyDescriptors) { Object.defineProperties(target, Object.getOwnPropertyDescriptors(source)); } else { ownKeys(source).forEach(function (key) { Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key)); }); } } return target; }

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import Dropzone from 'react-dropzone';
import { FileUploadField } from './FileUploadField';
import { readFile, fileReaderType } from '../../helpers/fileUtils';
export const FileUpload = (_ref) => {
  let {
    id,
    type,
    value = type === fileReaderType.text || type === fileReaderType.dataURL ? '' : null,
    filename = '',
    children = null,
    onChange = () => {},
    onReadStarted = () => {},
    onReadFinished = () => {},
    onReadFailed = () => {},
    dropzoneProps = {}
  } = _ref,
      props = _objectWithoutProperties(_ref, ["id", "type", "value", "filename", "children", "onChange", "onReadStarted", "onReadFinished", "onReadFailed", "dropzoneProps"]);

  const onDropAccepted = (acceptedFiles, event) => {
    if (acceptedFiles.length > 0) {
      const fileHandle = acceptedFiles[0];

      if (type === fileReaderType.text || type === fileReaderType.dataURL) {
        onChange('', fileHandle.name, event); // Show the filename while reading

        onReadStarted(fileHandle);
        readFile(fileHandle, type).then(data => {
          onReadFinished(fileHandle);
          onChange(data, fileHandle.name, event);
        }).catch(error => {
          onReadFailed(error, fileHandle);
          onReadFinished(fileHandle);
          onChange('', '', event); // Clear the filename field on a failure
        });
      } else {
        onChange(fileHandle, fileHandle.name, event);
      }
    }

    dropzoneProps.onDropAccepted && dropzoneProps.onDropAccepted(acceptedFiles, event);
  };

  const onDropRejected = (rejectedFiles, event) => {
    if (rejectedFiles.length > 0) {
      onChange('', rejectedFiles[0].name, event);
    }

    dropzoneProps.onDropRejected && dropzoneProps.onDropRejected(rejectedFiles, event);
  };

  const onClearButtonClick = event => {
    onChange('', '', event);
  };

  return React.createElement(Dropzone, _extends({
    multiple: false
  }, dropzoneProps, {
    onDropAccepted: onDropAccepted,
    onDropRejected: onDropRejected
  }), ({
    getRootProps,
    getInputProps,
    isDragActive,
    open
  }) => React.createElement(FileUploadField, _extends({}, getRootProps(_objectSpread({}, props, {
    refKey: 'containerRef',
    onClick: event => event.preventDefault() // Prevents clicking TextArea from opening file dialog

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
  }), React.createElement("input", getInputProps()), children));
};
FileUpload.propTypes = {
  id: _pt.string.isRequired,
  type: _pt.oneOf(['text', 'dataURL']),
  value: _pt.oneOfType([_pt.string, _pt.any]),
  filename: _pt.string,
  onChange: _pt.func,
  className: _pt.string,
  isDisabled: _pt.bool,
  isReadOnly: _pt.bool,
  isLoading: _pt.bool,
  spinnerAriaValueText: _pt.string,
  isRequired: _pt.bool,
  validated: _pt.oneOf(['success', 'error', 'default']),
  'aria-label': _pt.string,
  filenamePlaceholder: _pt.string,
  filenameAriaLabel: _pt.string,
  browseButtonText: _pt.string,
  clearButtonText: _pt.string,
  hideDefaultPreview: _pt.bool,
  allowEditingUploadedText: _pt.bool,
  children: _pt.node,
  onReadStarted: _pt.func,
  onReadFinished: _pt.func,
  onReadFailed: _pt.func,
  dropzoneProps: _pt.any
};
//# sourceMappingURL=FileUpload.js.map