var _extends = Object.assign || function (target) { for (var i = 1; i < arguments.length; i++) { var source = arguments[i]; for (var key in source) { if (Object.prototype.hasOwnProperty.call(source, key)) { target[key] = source[key]; } } } return target; };

var _createClass = function () { function defineProperties(target, props) { for (var i = 0; i < props.length; i++) { var descriptor = props[i]; descriptor.enumerable = descriptor.enumerable || false; descriptor.configurable = true; if ("value" in descriptor) descriptor.writable = true; Object.defineProperty(target, descriptor.key, descriptor); } } return function (Constructor, protoProps, staticProps) { if (protoProps) defineProperties(Constructor.prototype, protoProps); if (staticProps) defineProperties(Constructor, staticProps); return Constructor; }; }();

function _defineProperty(obj, key, value) { if (key in obj) { Object.defineProperty(obj, key, { value: value, enumerable: true, configurable: true, writable: true }); } else { obj[key] = value; } return obj; }

function _objectWithoutProperties(obj, keys) { var target = {}; for (var i in obj) { if (keys.indexOf(i) >= 0) continue; if (!Object.prototype.hasOwnProperty.call(obj, i)) continue; target[i] = obj[i]; } return target; }

function _toConsumableArray(arr) { if (Array.isArray(arr)) { for (var i = 0, arr2 = Array(arr.length); i < arr.length; i++) { arr2[i] = arr[i]; } return arr2; } else { return Array.from(arr); } }

function _classCallCheck(instance, Constructor) { if (!(instance instanceof Constructor)) { throw new TypeError("Cannot call a class as a function"); } }

function _possibleConstructorReturn(self, call) { if (!self) { throw new ReferenceError("this hasn't been initialised - super() hasn't been called"); } return call && (typeof call === "object" || typeof call === "function") ? call : self; }

function _inherits(subClass, superClass) { if (typeof superClass !== "function" && superClass !== null) { throw new TypeError("Super expression must either be null or a function, not " + typeof superClass); } subClass.prototype = Object.create(superClass && superClass.prototype, { constructor: { value: subClass, enumerable: false, writable: true, configurable: true } }); if (superClass) Object.setPrototypeOf ? Object.setPrototypeOf(subClass, superClass) : subClass.__proto__ = superClass; }

/* eslint prefer-template: 0 */

import React from 'react';
import { fromEvent } from 'file-selector';
import PropTypes from 'prop-types';
import { isDragDataWithFiles, supportMultiple, fileAccepted, allFilesAccepted, fileMatchSize, onDocumentDragOver, isIeOrEdge, composeEventHandlers, isPropagationStopped, isDefaultPrevented } from './utils';

var Dropzone = function (_React$Component) {
  _inherits(Dropzone, _React$Component);

  function Dropzone() {
    var _ref;

    var _temp, _this, _ret;

    _classCallCheck(this, Dropzone);

    for (var _len = arguments.length, args = Array(_len), _key = 0; _key < _len; _key++) {
      args[_key] = arguments[_key];
    }

    return _ret = (_temp = (_this = _possibleConstructorReturn(this, (_ref = Dropzone.__proto__ || Object.getPrototypeOf(Dropzone)).call.apply(_ref, [this].concat(args))), _this), _this.state = {
      draggedFiles: [],
      acceptedFiles: [],
      rejectedFiles: []
    }, _this.isFileDialogActive = false, _this.onDocumentDrop = function (evt) {
      if (_this.node && _this.node.contains(evt.target)) {
        // if we intercepted an event for our instance, let it propagate down to the instance's onDrop handler
        return;
      }
      evt.preventDefault();
      _this.dragTargets = [];
    }, _this.onDragStart = function (evt) {
      evt.persist();
      if (_this.props.onDragStart && isDragDataWithFiles(evt)) {
        _this.props.onDragStart.call(_this, evt);
      }
    }, _this.onDragEnter = function (evt) {
      evt.preventDefault();

      // Count the dropzone and any children that are entered.
      if (_this.dragTargets.indexOf(evt.target) === -1) {
        _this.dragTargets.push(evt.target);
      }

      evt.persist();

      if (isDragDataWithFiles(evt)) {
        Promise.resolve(_this.props.getDataTransferItems(evt)).then(function (draggedFiles) {
          if (isPropagationStopped(evt)) {
            return;
          }

          _this.setState({
            draggedFiles: draggedFiles,
            // Do not rely on files for the drag state. It doesn't work in Safari.
            isDragActive: true
          });
        });

        if (_this.props.onDragEnter) {
          _this.props.onDragEnter.call(_this, evt);
        }
      }
    }, _this.onDragOver = function (evt) {
      // eslint-disable-line class-methods-use-this
      evt.preventDefault();
      evt.persist();

      if (evt.dataTransfer) {
        evt.dataTransfer.dropEffect = 'copy';
      }

      if (_this.props.onDragOver && isDragDataWithFiles(evt)) {
        _this.props.onDragOver.call(_this, evt);
      }

      return false;
    }, _this.onDragLeave = function (evt) {
      evt.preventDefault();
      evt.persist();

      // Only deactivate once the dropzone and all children have been left.
      _this.dragTargets = _this.dragTargets.filter(function (el) {
        return el !== evt.target && _this.node.contains(el);
      });
      if (_this.dragTargets.length > 0) {
        return;
      }

      // Clear dragging files state
      _this.setState({
        isDragActive: false,
        draggedFiles: []
      });

      if (_this.props.onDragLeave && isDragDataWithFiles(evt)) {
        _this.props.onDragLeave.call(_this, evt);
      }
    }, _this.onDrop = function (evt) {
      var _this$props = _this.props,
          onDrop = _this$props.onDrop,
          onDropAccepted = _this$props.onDropAccepted,
          onDropRejected = _this$props.onDropRejected,
          multiple = _this$props.multiple,
          accept = _this$props.accept,
          getDataTransferItems = _this$props.getDataTransferItems;

      // Stop default browser behavior

      evt.preventDefault();

      // Persist event for later usage
      evt.persist();

      // Reset the counter along with the drag on a drop.
      _this.dragTargets = [];
      _this.isFileDialogActive = false;

      // Clear files value
      _this.draggedFiles = null;

      // Reset drag state
      _this.setState({
        isDragActive: false,
        draggedFiles: []
      });

      if (isDragDataWithFiles(evt)) {
        Promise.resolve(getDataTransferItems(evt)).then(function (fileList) {
          var acceptedFiles = [];
          var rejectedFiles = [];

          if (isPropagationStopped(evt)) {
            return;
          }

          fileList.forEach(function (file) {
            if (fileAccepted(file, accept) && fileMatchSize(file, _this.props.maxSize, _this.props.minSize)) {
              acceptedFiles.push(file);
            } else {
              rejectedFiles.push(file);
            }
          });

          if (!multiple && acceptedFiles.length > 1) {
            // if not in multi mode add any extra accepted files to rejected.
            // This will allow end users to easily ignore a multi file drop in "single" mode.
            rejectedFiles.push.apply(rejectedFiles, _toConsumableArray(acceptedFiles.splice(0)));
          }

          // Update `acceptedFiles` and `rejectedFiles` state
          // This will make children render functions receive the appropriate
          // values
          _this.setState({ acceptedFiles: acceptedFiles, rejectedFiles: rejectedFiles }, function () {
            if (onDrop) {
              onDrop.call(_this, acceptedFiles, rejectedFiles, evt);
            }

            if (rejectedFiles.length > 0 && onDropRejected) {
              onDropRejected.call(_this, rejectedFiles, evt);
            }

            if (acceptedFiles.length > 0 && onDropAccepted) {
              onDropAccepted.call(_this, acceptedFiles, evt);
            }
          });
        });
      }
    }, _this.onClick = function (evt) {
      var onClick = _this.props.onClick;

      // if onClick prop is given, run it first

      if (onClick) {
        onClick.call(_this, evt);
      }

      // If the event hasn't been default prevented from within
      // the onClick listener, open the file dialog
      if (!isDefaultPrevented(evt)) {
        evt.stopPropagation();

        // in IE11/Edge the file-browser dialog is blocking, ensure this is behind setTimeout
        // this is so react can handle state changes in the onClick prop above above
        // see: https://github.com/react-dropzone/react-dropzone/issues/450
        if (isIeOrEdge()) {
          setTimeout(_this.open, 0);
        } else {
          _this.open();
        }
      }
    }, _this.onInputElementClick = function (evt) {
      evt.stopPropagation();
    }, _this.onFileDialogCancel = function () {
      // timeout will not recognize context of this method
      var onFileDialogCancel = _this.props.onFileDialogCancel;
      // execute the timeout only if the FileDialog is opened in the browser

      if (_this.isFileDialogActive) {
        setTimeout(function () {
          if (_this.input != null) {
            // Returns an object as FileList
            var files = _this.input.files;


            if (!files.length) {
              _this.isFileDialogActive = false;

              if (typeof onFileDialogCancel === 'function') {
                onFileDialogCancel();
              }
            }
          }
        }, 300);
      }
    }, _this.onFocus = function (evt) {
      var onFocus = _this.props.onFocus;

      if (onFocus) {
        onFocus.call(_this, evt);
      }
      if (!isDefaultPrevented(evt)) {
        _this.setState({ isFocused: true });
      }
    }, _this.onBlur = function (evt) {
      var onBlur = _this.props.onBlur;

      if (onBlur) {
        onBlur.call(_this, evt);
      }
      if (!isDefaultPrevented(evt)) {
        _this.setState({ isFocused: false });
      }
    }, _this.onKeyDown = function (evt) {
      var onKeyDown = _this.props.onKeyDown;

      if (!_this.node.isEqualNode(evt.target)) {
        return;
      }

      if (onKeyDown) {
        onKeyDown.call(_this, evt);
      }

      if (!isDefaultPrevented(evt) && (evt.keyCode === 32 || evt.keyCode === 13)) {
        evt.preventDefault();
        _this.open();
      }
    }, _this.composeHandler = function (handler) {
      if (_this.props.disabled) {
        return null;
      }
      return handler;
    }, _this.getRootProps = function () {
      var _extends2;

      var _ref2 = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

      var _ref2$refKey = _ref2.refKey,
          refKey = _ref2$refKey === undefined ? 'ref' : _ref2$refKey,
          onKeyDown = _ref2.onKeyDown,
          onFocus = _ref2.onFocus,
          onBlur = _ref2.onBlur,
          onClick = _ref2.onClick,
          onDragStart = _ref2.onDragStart,
          onDragEnter = _ref2.onDragEnter,
          onDragOver = _ref2.onDragOver,
          onDragLeave = _ref2.onDragLeave,
          onDrop = _ref2.onDrop,
          rest = _objectWithoutProperties(_ref2, ['refKey', 'onKeyDown', 'onFocus', 'onBlur', 'onClick', 'onDragStart', 'onDragEnter', 'onDragOver', 'onDragLeave', 'onDrop']);

      return _extends((_extends2 = {
        onKeyDown: _this.composeHandler(onKeyDown ? composeEventHandlers(onKeyDown, _this.onKeyDown) : _this.onKeyDown),
        onFocus: _this.composeHandler(onFocus ? composeEventHandlers(onFocus, _this.onFocus) : _this.onFocus),
        onBlur: _this.composeHandler(onBlur ? composeEventHandlers(onBlur, _this.onBlur) : _this.onBlur),
        onClick: _this.composeHandler(onClick ? composeEventHandlers(onClick, _this.onClick) : _this.onClick),
        onDragStart: _this.composeHandler(onDragStart ? composeEventHandlers(onDragStart, _this.onDragStart) : _this.onDragStart),
        onDragEnter: _this.composeHandler(onDragEnter ? composeEventHandlers(onDragEnter, _this.onDragEnter) : _this.onDragEnter),
        onDragOver: _this.composeHandler(onDragOver ? composeEventHandlers(onDragOver, _this.onDragOver) : _this.onDragOver),
        onDragLeave: _this.composeHandler(onDragLeave ? composeEventHandlers(onDragLeave, _this.onDragLeave) : _this.onDragLeave),
        onDrop: _this.composeHandler(onDrop ? composeEventHandlers(onDrop, _this.onDrop) : _this.onDrop)
      }, _defineProperty(_extends2, refKey, _this.setNodeRef), _defineProperty(_extends2, 'tabIndex', _this.props.disabled ? -1 : 0), _extends2), rest);
    }, _this.getInputProps = function () {
      var _ref3 = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : {};

      var _ref3$refKey = _ref3.refKey,
          refKey = _ref3$refKey === undefined ? 'ref' : _ref3$refKey,
          onChange = _ref3.onChange,
          onClick = _ref3.onClick,
          rest = _objectWithoutProperties(_ref3, ['refKey', 'onChange', 'onClick']);

      var _this$props2 = _this.props,
          accept = _this$props2.accept,
          multiple = _this$props2.multiple,
          name = _this$props2.name;

      var inputProps = _defineProperty({
        accept: accept,
        type: 'file',
        style: { display: 'none' },
        multiple: supportMultiple && multiple,
        onChange: composeEventHandlers(onChange, _this.onDrop),
        onClick: composeEventHandlers(onClick, _this.onInputElementClick),
        autoComplete: 'off',
        tabIndex: -1
      }, refKey, _this.setInputRef);
      if (name && name.length) {
        inputProps.name = name;
      }
      return _extends({}, inputProps, rest);
    }, _this.setNodeRef = function (node) {
      _this.node = node;
    }, _this.setInputRef = function (input) {
      _this.input = input;
    }, _this.open = function () {
      _this.isFileDialogActive = true;
      if (_this.input) {
        _this.input.value = null;
        _this.input.click();
      }
    }, _temp), _possibleConstructorReturn(_this, _ret);
  }

  _createClass(Dropzone, [{
    key: 'componentDidMount',
    value: function componentDidMount() {
      var preventDropOnDocument = this.props.preventDropOnDocument;

      this.dragTargets = [];

      if (preventDropOnDocument) {
        document.addEventListener('dragover', onDocumentDragOver, false);
        document.addEventListener('drop', this.onDocumentDrop, false);
      }

      window.addEventListener('focus', this.onFileDialogCancel, false);
    }
  }, {
    key: 'componentWillUnmount',
    value: function componentWillUnmount() {
      var preventDropOnDocument = this.props.preventDropOnDocument;

      if (preventDropOnDocument) {
        document.removeEventListener('dragover', onDocumentDragOver);
        document.removeEventListener('drop', this.onDocumentDrop);
      }

      window.removeEventListener('focus', this.onFileDialogCancel, false);
    }

    /**
     * Open system file upload dialog.
     *
     * @public
     */

  }, {
    key: 'render',
    value: function render() {
      var _props = this.props,
          children = _props.children,
          multiple = _props.multiple,
          disabled = _props.disabled;
      var _state = this.state,
          isDragActive = _state.isDragActive,
          isFocused = _state.isFocused,
          draggedFiles = _state.draggedFiles,
          acceptedFiles = _state.acceptedFiles,
          rejectedFiles = _state.rejectedFiles;


      var filesCount = draggedFiles.length;
      var isMultipleAllowed = multiple || filesCount <= 1;
      var isDragAccept = filesCount > 0 && allFilesAccepted(draggedFiles, this.props.accept);
      var isDragReject = filesCount > 0 && (!isDragAccept || !isMultipleAllowed);

      return children({
        isDragActive: isDragActive,
        isDragAccept: isDragAccept,
        isDragReject: isDragReject,
        draggedFiles: draggedFiles,
        acceptedFiles: acceptedFiles,
        rejectedFiles: rejectedFiles,
        isFocused: isFocused && !disabled,
        getRootProps: this.getRootProps,
        getInputProps: this.getInputProps,
        open: this.open
      });
    }
  }]);

  return Dropzone;
}(React.Component);

export default Dropzone;

Dropzone.propTypes = {
  /**
   * Allow specific types of files. See https://github.com/okonet/attr-accept for more information.
   * Keep in mind that mime type determination is not reliable across platforms. CSV files,
   * for example, are reported as text/plain under macOS but as application/vnd.ms-excel under
   * Windows. In some cases there might not be a mime type set at all.
   * See: https://github.com/react-dropzone/react-dropzone/issues/276
   */
  accept: PropTypes.oneOfType([PropTypes.string, PropTypes.arrayOf(PropTypes.string)]),

  /**
   * Render function that renders the actual component
   *
   * @param {Object} props
   * @param {Function} props.getRootProps Returns the props you should apply to the root drop container you render
   * @param {Function} props.getInputProps Returns the props you should apply to hidden file input you render
   * @param {Function} props.open Open the native file selection dialog
   * @param {Boolean} props.isFocused Dropzone area is in focus
   * @param {Boolean} props.isDragActive Active drag is in progress
   * @param {Boolean} props.isDragAccept Dragged files are accepted
   * @param {Boolean} props.isDragReject Some dragged files are rejected
   * @param {Array} props.draggedFiles Files in active drag
   * @param {Array} props.acceptedFiles Accepted files
   * @param {Array} props.rejectedFiles Rejected files
   */
  children: PropTypes.func,

  /**
   * Enable/disable the dropzone entirely
   */
  disabled: PropTypes.bool,

  /**
   * If false, allow dropped items to take over the current browser window
   */
  preventDropOnDocument: PropTypes.bool,

  /**
   * Allow dropping multiple files
   */
  multiple: PropTypes.bool,

  /**
   * `name` attribute for the input tag
   */
  name: PropTypes.string,

  /**
   * Maximum file size (in bytes)
   */
  maxSize: PropTypes.number,

  /**
   * Minimum file size (in bytes)
   */
  minSize: PropTypes.number,

  /**
   * getDataTransferItems handler
   * @param {Event} event
   * @returns {Array} array of File objects
   */
  getDataTransferItems: PropTypes.func,

  /**
   * onClick callback
   * @param {Event} event
   */
  onClick: PropTypes.func,

  /**
   * onFocus callback
   */
  onFocus: PropTypes.func,

  /**
   * onBlur callback
   */
  onBlur: PropTypes.func,

  /**
   * onKeyDown callback
   */
  onKeyDown: PropTypes.func,

  /**
   * The `onDrop` method that accepts two arguments.
   * The first argument represents the accepted files and the second argument the rejected files.
   *
   * ```javascript
   * function onDrop(acceptedFiles, rejectedFiles) {
   *   // do stuff with files...
   * }
   * ```
   *
   * Files are accepted or rejected based on the `accept` prop.
   * This must be a valid [MIME type](http://www.iana.org/assignments/media-types/media-types.xhtml) according to [input element specification](https://www.w3.org/wiki/HTML/Elements/input/file) or a valid file extension.
   *
   * Note that the `onDrop` callback will always be called regardless if the dropped files were accepted or rejected.
   * You can use the `onDropAccepted`/`onDropRejected` props if you'd like to react to a specific event instead of the `onDrop` prop.
   *
   * The `onDrop` callback will provide you with an array of [Files](https://developer.mozilla.org/en-US/docs/Web/API/File) which you can then process and send to a server.
   * For example, with [SuperAgent](https://github.com/visionmedia/superagent) as a http/ajax library:
   *
   * ```javascript
   * function onDrop(acceptedFiles) {
   *   const req = request.post('/upload')
   *   acceptedFiles.forEach(file => {
   *     req.attach(file.name, file)
   *   })
   *   req.end(callback)
   * }
   * ```
   */
  onDrop: PropTypes.func,

  /**
   * onDropAccepted callback
   */
  onDropAccepted: PropTypes.func,

  /**
   * onDropRejected callback
   */
  onDropRejected: PropTypes.func,

  /**
   * onDragStart callback
   */
  onDragStart: PropTypes.func,

  /**
   * onDragEnter callback
   */
  onDragEnter: PropTypes.func,

  /**
   * onDragOver callback
   */
  onDragOver: PropTypes.func,

  /**
   * onDragLeave callback
   */
  onDragLeave: PropTypes.func,

  /**
   * Provide a callback on clicking the cancel button of the file dialog
   */
  onFileDialogCancel: PropTypes.func
};

Dropzone.defaultProps = {
  preventDropOnDocument: true,
  disabled: false,
  multiple: true,
  maxSize: Infinity,
  minSize: 0,
  getDataTransferItems: fromEvent
};