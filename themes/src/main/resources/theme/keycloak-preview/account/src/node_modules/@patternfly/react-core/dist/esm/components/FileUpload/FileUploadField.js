import _pt from "prop-types";

function _extends() { _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; }; return _extends.apply(this, arguments); }

function _objectWithoutProperties(source, excluded) { if (source == null) return {}; var target = _objectWithoutPropertiesLoose(source, excluded); var key, i; if (Object.getOwnPropertySymbols) { var sourceSymbolKeys = Object.getOwnPropertySymbols(source); for (i = 0; i < sourceSymbolKeys.length; i++) { key = sourceSymbolKeys[i]; if (excluded.indexOf(key) >= 0) continue; if (!Object.prototype.propertyIsEnumerable.call(source, key)) continue; target[key] = source[key]; } } return target; }

function _objectWithoutPropertiesLoose(source, excluded) { if (source == null) return {}; var target = {}; var sourceKeys = Object.keys(source); var key, i; for (i = 0; i < sourceKeys.length; i++) { key = sourceKeys[i]; if (excluded.indexOf(key) >= 0) continue; target[key] = source[key]; } return target; }

import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/FileUpload/file-upload';
import { css } from '@patternfly/react-styles';
import { InputGroup } from '../InputGroup';
import { TextInput } from '../TextInput';
import { Button, ButtonVariant } from '../Button';
import { TextArea, TextAreResizeOrientation } from '../TextArea';
import { Spinner, spinnerSize } from '../Spinner';
import { fileReaderType } from '../../helpers/fileUtils';
export const FileUploadField = (_ref) => {
  let {
    id,
    type,
    value = '',
    filename = '',
    onChange = () => {},
    onBrowseButtonClick = () => {},
    onClearButtonClick = () => {},
    className = '',
    isDisabled = false,
    isReadOnly = false,
    isLoading = false,
    spinnerAriaValueText,
    isRequired = false,
    isDragActive = false,
    validated = 'default',
    'aria-label': ariaLabel = 'File upload',
    filenamePlaceholder = 'Drag a file here or browse to upload',
    filenameAriaLabel = filename ? 'Read only filename' : filenamePlaceholder,
    browseButtonText = 'Browse...',
    clearButtonText = 'Clear',
    isClearButtonDisabled = !filename && !value,
    containerRef = null,
    allowEditingUploadedText = false,
    hideDefaultPreview = false,
    children = null
  } = _ref,
      props = _objectWithoutProperties(_ref, ["id", "type", "value", "filename", "onChange", "onBrowseButtonClick", "onClearButtonClick", "className", "isDisabled", "isReadOnly", "isLoading", "spinnerAriaValueText", "isRequired", "isDragActive", "validated", "aria-label", "filenamePlaceholder", "filenameAriaLabel", "browseButtonText", "clearButtonText", "isClearButtonDisabled", "containerRef", "allowEditingUploadedText", "hideDefaultPreview", "children"]);

  const onTextAreaChange = (newValue, event) => {
    onChange(newValue, filename, event);
  };

  return React.createElement("div", _extends({
    className: css(styles.fileUpload, isDragActive && styles.modifiers.dragHover, isLoading && styles.modifiers.loading, className),
    ref: containerRef
  }, props), React.createElement("div", {
    className: styles.fileUploadFileSelect
  }, React.createElement(InputGroup, null, React.createElement(TextInput, {
    isReadOnly: true // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
    ,
    isDisabled: isDisabled,
    id: `${id}-filename`,
    name: `${id}-filename`,
    "aria-label": filenameAriaLabel,
    placeholder: filenamePlaceholder,
    "aria-describedby": `${id}-browse-button`,
    value: filename
  }), React.createElement(Button, {
    id: `${id}-browse-button`,
    variant: ButtonVariant.control,
    onClick: onBrowseButtonClick,
    isDisabled: isDisabled
  }, browseButtonText), React.createElement(Button, {
    variant: ButtonVariant.control,
    isDisabled: isDisabled || isClearButtonDisabled,
    onClick: onClearButtonClick
  }, clearButtonText))), React.createElement("div", {
    className: styles.fileUploadFileDetails
  }, !hideDefaultPreview && type === fileReaderType.text && React.createElement(TextArea, {
    readOnly: isReadOnly || !!filename && !allowEditingUploadedText,
    disabled: isDisabled,
    isRequired: isRequired,
    resizeOrientation: TextAreResizeOrientation.vertical,
    validated: validated,
    id: id,
    name: id,
    "aria-label": ariaLabel,
    value: value,
    onChange: onTextAreaChange
  }), isLoading && React.createElement("div", {
    className: styles.fileUploadFileDetailsSpinner
  }, React.createElement(Spinner, {
    size: spinnerSize.lg,
    "aria-valuetext": spinnerAriaValueText
  }))), children);
};
FileUploadField.propTypes = {
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
  isClearButtonDisabled: _pt.bool,
  hideDefaultPreview: _pt.bool,
  allowEditingUploadedText: _pt.bool,
  children: _pt.node,
  onBrowseButtonClick: _pt.func,
  onClearButtonClick: _pt.func,
  isDragActive: _pt.bool,
  containerRef: _pt.oneOfType([_pt.string, _pt.func, _pt.object])
};
//# sourceMappingURL=FileUploadField.js.map