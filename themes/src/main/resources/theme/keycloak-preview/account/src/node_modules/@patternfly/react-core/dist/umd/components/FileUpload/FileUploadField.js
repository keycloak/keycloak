(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "@patternfly/react-styles/css/components/FileUpload/file-upload", "@patternfly/react-styles", "../InputGroup", "../TextInput", "../Button", "../TextArea", "../Spinner", "../../helpers/fileUtils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("@patternfly/react-styles/css/components/FileUpload/file-upload"), require("@patternfly/react-styles"), require("../InputGroup"), require("../TextInput"), require("../Button"), require("../TextArea"), require("../Spinner"), require("../../helpers/fileUtils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.fileUpload, global.reactStyles, global.InputGroup, global.TextInput, global.Button, global.TextArea, global.Spinner, global.fileUtils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _fileUpload, _reactStyles, _InputGroup, _TextInput, _Button, _TextArea, _Spinner, _fileUtils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.FileUploadField = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _fileUpload2 = _interopRequireDefault(_fileUpload);

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

  const FileUploadField = exports.FileUploadField = _ref => {
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
      className: (0, _reactStyles.css)(_fileUpload2.default.fileUpload, isDragActive && _fileUpload2.default.modifiers.dragHover, isLoading && _fileUpload2.default.modifiers.loading, className),
      ref: containerRef
    }, props), React.createElement("div", {
      className: _fileUpload2.default.fileUploadFileSelect
    }, React.createElement(_InputGroup.InputGroup, null, React.createElement(_TextInput.TextInput, {
      isReadOnly: true // Always read-only regardless of isReadOnly prop (which is just for the TextArea)
      ,
      isDisabled: isDisabled,
      id: `${id}-filename`,
      name: `${id}-filename`,
      "aria-label": filenameAriaLabel,
      placeholder: filenamePlaceholder,
      "aria-describedby": `${id}-browse-button`,
      value: filename
    }), React.createElement(_Button.Button, {
      id: `${id}-browse-button`,
      variant: _Button.ButtonVariant.control,
      onClick: onBrowseButtonClick,
      isDisabled: isDisabled
    }, browseButtonText), React.createElement(_Button.Button, {
      variant: _Button.ButtonVariant.control,
      isDisabled: isDisabled || isClearButtonDisabled,
      onClick: onClearButtonClick
    }, clearButtonText))), React.createElement("div", {
      className: _fileUpload2.default.fileUploadFileDetails
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
      className: _fileUpload2.default.fileUploadFileDetailsSpinner
    }, React.createElement(_Spinner.Spinner, {
      size: _Spinner.spinnerSize.lg,
      "aria-valuetext": spinnerAriaValueText
    }))), children);
  };

  FileUploadField.propTypes = {
    id: _propTypes2.default.string.isRequired,
    type: _propTypes2.default.oneOf(['text', 'dataURL']),
    value: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.any]),
    filename: _propTypes2.default.string,
    onChange: _propTypes2.default.func,
    className: _propTypes2.default.string,
    isDisabled: _propTypes2.default.bool,
    isReadOnly: _propTypes2.default.bool,
    isLoading: _propTypes2.default.bool,
    spinnerAriaValueText: _propTypes2.default.string,
    isRequired: _propTypes2.default.bool,
    validated: _propTypes2.default.oneOf(['success', 'error', 'default']),
    'aria-label': _propTypes2.default.string,
    filenamePlaceholder: _propTypes2.default.string,
    filenameAriaLabel: _propTypes2.default.string,
    browseButtonText: _propTypes2.default.string,
    clearButtonText: _propTypes2.default.string,
    isClearButtonDisabled: _propTypes2.default.bool,
    hideDefaultPreview: _propTypes2.default.bool,
    allowEditingUploadedText: _propTypes2.default.bool,
    children: _propTypes2.default.node,
    onBrowseButtonClick: _propTypes2.default.func,
    onClearButtonClick: _propTypes2.default.func,
    isDragActive: _propTypes2.default.bool,
    containerRef: _propTypes2.default.oneOfType([_propTypes2.default.string, _propTypes2.default.func, _propTypes2.default.object])
  };
});
//# sourceMappingURL=FileUploadField.js.map