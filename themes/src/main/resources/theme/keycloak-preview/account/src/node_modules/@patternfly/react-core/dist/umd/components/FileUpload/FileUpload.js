(function (global, factory) {
  if (typeof define === "function" && define.amd) {
    define(["exports", "prop-types", "react", "react-dropzone", "./FileUploadField", "../../helpers/fileUtils"], factory);
  } else if (typeof exports !== "undefined") {
    factory(exports, require("prop-types"), require("react"), require("react-dropzone"), require("./FileUploadField"), require("../../helpers/fileUtils"));
  } else {
    var mod = {
      exports: {}
    };
    factory(mod.exports, global.propTypes, global.react, global.reactDropzone, global.FileUploadField, global.fileUtils);
    global.undefined = mod.exports;
  }
})(this, function (exports, _propTypes, _react, _reactDropzone, _FileUploadField, _fileUtils) {
  "use strict";

  Object.defineProperty(exports, "__esModule", {
    value: true
  });
  exports.FileUpload = undefined;

  var _propTypes2 = _interopRequireDefault(_propTypes);

  var React = _interopRequireWildcard(_react);

  var _reactDropzone2 = _interopRequireDefault(_reactDropzone);

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

  function ownKeys(object, enumerableOnly) {
    var keys = Object.keys(object);

    if (Object.getOwnPropertySymbols) {
      var symbols = Object.getOwnPropertySymbols(object);
      if (enumerableOnly) symbols = symbols.filter(function (sym) {
        return Object.getOwnPropertyDescriptor(object, sym).enumerable;
      });
      keys.push.apply(keys, symbols);
    }

    return keys;
  }

  function _objectSpread(target) {
    for (var i = 1; i < arguments.length; i++) {
      var source = arguments[i] != null ? arguments[i] : {};

      if (i % 2) {
        ownKeys(source, true).forEach(function (key) {
          _defineProperty(target, key, source[key]);
        });
      } else if (Object.getOwnPropertyDescriptors) {
        Object.defineProperties(target, Object.getOwnPropertyDescriptors(source));
      } else {
        ownKeys(source).forEach(function (key) {
          Object.defineProperty(target, key, Object.getOwnPropertyDescriptor(source, key));
        });
      }
    }

    return target;
  }

  function _defineProperty(obj, key, value) {
    if (key in obj) {
      Object.defineProperty(obj, key, {
        value: value,
        enumerable: true,
        configurable: true,
        writable: true
      });
    } else {
      obj[key] = value;
    }

    return obj;
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

  const FileUpload = exports.FileUpload = _ref => {
    let {
      id,
      type,
      value = type === _fileUtils.fileReaderType.text || type === _fileUtils.fileReaderType.dataURL ? '' : null,
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

        if (type === _fileUtils.fileReaderType.text || type === _fileUtils.fileReaderType.dataURL) {
          onChange('', fileHandle.name, event); // Show the filename while reading

          onReadStarted(fileHandle);
          (0, _fileUtils.readFile)(fileHandle, type).then(data => {
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

    return React.createElement(_reactDropzone2.default, _extends({
      multiple: false
    }, dropzoneProps, {
      onDropAccepted: onDropAccepted,
      onDropRejected: onDropRejected
    }), ({
      getRootProps,
      getInputProps,
      isDragActive,
      open
    }) => React.createElement(_FileUploadField.FileUploadField, _extends({}, getRootProps(_objectSpread({}, props, {
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
    hideDefaultPreview: _propTypes2.default.bool,
    allowEditingUploadedText: _propTypes2.default.bool,
    children: _propTypes2.default.node,
    onReadStarted: _propTypes2.default.func,
    onReadFinished: _propTypes2.default.func,
    onReadFailed: _propTypes2.default.func,
    dropzoneProps: _propTypes2.default.any
  };
});
//# sourceMappingURL=FileUpload.js.map