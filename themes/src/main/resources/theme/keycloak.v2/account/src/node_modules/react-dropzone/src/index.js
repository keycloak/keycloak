/* eslint prefer-template: 0 */

import React from 'react'
import { fromEvent } from 'file-selector'
import PropTypes from 'prop-types'
import {
  isDragDataWithFiles,
  supportMultiple,
  fileAccepted,
  allFilesAccepted,
  fileMatchSize,
  onDocumentDragOver,
  isIeOrEdge,
  composeEventHandlers,
  isPropagationStopped,
  isDefaultPrevented
} from './utils'

class Dropzone extends React.Component {
  state = {
    draggedFiles: [],
    acceptedFiles: [],
    rejectedFiles: []
  }

  componentDidMount() {
    const { preventDropOnDocument } = this.props
    this.dragTargets = []

    if (preventDropOnDocument) {
      document.addEventListener('dragover', onDocumentDragOver, false)
      document.addEventListener('drop', this.onDocumentDrop, false)
    }

    window.addEventListener('focus', this.onFileDialogCancel, false)
  }

  componentWillUnmount() {
    const { preventDropOnDocument } = this.props
    if (preventDropOnDocument) {
      document.removeEventListener('dragover', onDocumentDragOver)
      document.removeEventListener('drop', this.onDocumentDrop)
    }

    window.removeEventListener('focus', this.onFileDialogCancel, false)
  }

  isFileDialogActive = false

  onDocumentDrop = evt => {
    if (this.node && this.node.contains(evt.target)) {
      // if we intercepted an event for our instance, let it propagate down to the instance's onDrop handler
      return
    }
    evt.preventDefault()
    this.dragTargets = []
  }

  onDragStart = evt => {
    evt.persist()
    if (this.props.onDragStart && isDragDataWithFiles(evt)) {
      this.props.onDragStart.call(this, evt)
    }
  }

  onDragEnter = evt => {
    evt.preventDefault()

    // Count the dropzone and any children that are entered.
    if (this.dragTargets.indexOf(evt.target) === -1) {
      this.dragTargets.push(evt.target)
    }

    evt.persist()

    if (isDragDataWithFiles(evt)) {
      Promise.resolve(this.props.getDataTransferItems(evt)).then(draggedFiles => {
        if (isPropagationStopped(evt)) {
          return
        }

        this.setState({
          draggedFiles,
          // Do not rely on files for the drag state. It doesn't work in Safari.
          isDragActive: true
        })
      })

      if (this.props.onDragEnter) {
        this.props.onDragEnter.call(this, evt)
      }
    }
  }

  onDragOver = evt => {
    // eslint-disable-line class-methods-use-this
    evt.preventDefault()
    evt.persist()

    if (evt.dataTransfer) {
      evt.dataTransfer.dropEffect = 'copy'
    }

    if (this.props.onDragOver && isDragDataWithFiles(evt)) {
      this.props.onDragOver.call(this, evt)
    }

    return false
  }

  onDragLeave = evt => {
    evt.preventDefault()
    evt.persist()

    // Only deactivate once the dropzone and all children have been left.
    this.dragTargets = this.dragTargets.filter(el => el !== evt.target && this.node.contains(el))
    if (this.dragTargets.length > 0) {
      return
    }

    // Clear dragging files state
    this.setState({
      isDragActive: false,
      draggedFiles: []
    })

    if (this.props.onDragLeave && isDragDataWithFiles(evt)) {
      this.props.onDragLeave.call(this, evt)
    }
  }

  onDrop = evt => {
    const {
      onDrop,
      onDropAccepted,
      onDropRejected,
      multiple,
      accept,
      getDataTransferItems
    } = this.props

    // Stop default browser behavior
    evt.preventDefault()

    // Persist event for later usage
    evt.persist()

    // Reset the counter along with the drag on a drop.
    this.dragTargets = []
    this.isFileDialogActive = false

    // Clear files value
    this.draggedFiles = null

    // Reset drag state
    this.setState({
      isDragActive: false,
      draggedFiles: []
    })

    if (isDragDataWithFiles(evt)) {
      Promise.resolve(getDataTransferItems(evt)).then(fileList => {
        const acceptedFiles = []
        const rejectedFiles = []

        if (isPropagationStopped(evt)) {
          return
        }

        fileList.forEach(file => {
          if (
            fileAccepted(file, accept) &&
            fileMatchSize(file, this.props.maxSize, this.props.minSize)
          ) {
            acceptedFiles.push(file)
          } else {
            rejectedFiles.push(file)
          }
        })

        if (!multiple && acceptedFiles.length > 1) {
          // if not in multi mode add any extra accepted files to rejected.
          // This will allow end users to easily ignore a multi file drop in "single" mode.
          rejectedFiles.push(...acceptedFiles.splice(0))
        }

        // Update `acceptedFiles` and `rejectedFiles` state
        // This will make children render functions receive the appropriate
        // values
        this.setState({ acceptedFiles, rejectedFiles }, () => {
          if (onDrop) {
            onDrop.call(this, acceptedFiles, rejectedFiles, evt)
          }

          if (rejectedFiles.length > 0 && onDropRejected) {
            onDropRejected.call(this, rejectedFiles, evt)
          }

          if (acceptedFiles.length > 0 && onDropAccepted) {
            onDropAccepted.call(this, acceptedFiles, evt)
          }
        })
      })
    }
  }

  onClick = evt => {
    const { onClick } = this.props

    // if onClick prop is given, run it first
    if (onClick) {
      onClick.call(this, evt)
    }

    // If the event hasn't been default prevented from within
    // the onClick listener, open the file dialog
    if (!isDefaultPrevented(evt)) {
      evt.stopPropagation()

      // in IE11/Edge the file-browser dialog is blocking, ensure this is behind setTimeout
      // this is so react can handle state changes in the onClick prop above above
      // see: https://github.com/react-dropzone/react-dropzone/issues/450
      if (isIeOrEdge()) {
        setTimeout(this.open, 0)
      } else {
        this.open()
      }
    }
  }

  onInputElementClick = evt => {
    evt.stopPropagation()
  }

  onFileDialogCancel = () => {
    // timeout will not recognize context of this method
    const { onFileDialogCancel } = this.props
    // execute the timeout only if the FileDialog is opened in the browser
    if (this.isFileDialogActive) {
      setTimeout(() => {
        if (this.input != null) {
          // Returns an object as FileList
          const { files } = this.input

          if (!files.length) {
            this.isFileDialogActive = false

            if (typeof onFileDialogCancel === 'function') {
              onFileDialogCancel()
            }
          }
        }
      }, 300)
    }
  }

  onFocus = evt => {
    const { onFocus } = this.props
    if (onFocus) {
      onFocus.call(this, evt)
    }
    if (!isDefaultPrevented(evt)) {
      this.setState({ isFocused: true })
    }
  }

  onBlur = evt => {
    const { onBlur } = this.props
    if (onBlur) {
      onBlur.call(this, evt)
    }
    if (!isDefaultPrevented(evt)) {
      this.setState({ isFocused: false })
    }
  }

  onKeyDown = evt => {
    const { onKeyDown } = this.props
    if (!this.node.isEqualNode(evt.target)) {
      return
    }

    if (onKeyDown) {
      onKeyDown.call(this, evt)
    }

    if (!isDefaultPrevented(evt) && (evt.keyCode === 32 || evt.keyCode === 13)) {
      evt.preventDefault()
      this.open()
    }
  }

  composeHandler = handler => {
    if (this.props.disabled) {
      return null
    }
    return handler
  }

  getRootProps = ({
    refKey = 'ref',
    onKeyDown,
    onFocus,
    onBlur,
    onClick,
    onDragStart,
    onDragEnter,
    onDragOver,
    onDragLeave,
    onDrop,
    ...rest
  } = {}) => ({
    onKeyDown: this.composeHandler(
      onKeyDown ? composeEventHandlers(onKeyDown, this.onKeyDown) : this.onKeyDown
    ),
    onFocus: this.composeHandler(
      onFocus ? composeEventHandlers(onFocus, this.onFocus) : this.onFocus
    ),
    onBlur: this.composeHandler(onBlur ? composeEventHandlers(onBlur, this.onBlur) : this.onBlur),
    onClick: this.composeHandler(
      onClick ? composeEventHandlers(onClick, this.onClick) : this.onClick
    ),
    onDragStart: this.composeHandler(
      onDragStart ? composeEventHandlers(onDragStart, this.onDragStart) : this.onDragStart
    ),
    onDragEnter: this.composeHandler(
      onDragEnter ? composeEventHandlers(onDragEnter, this.onDragEnter) : this.onDragEnter
    ),
    onDragOver: this.composeHandler(
      onDragOver ? composeEventHandlers(onDragOver, this.onDragOver) : this.onDragOver
    ),
    onDragLeave: this.composeHandler(
      onDragLeave ? composeEventHandlers(onDragLeave, this.onDragLeave) : this.onDragLeave
    ),
    onDrop: this.composeHandler(onDrop ? composeEventHandlers(onDrop, this.onDrop) : this.onDrop),
    [refKey]: this.setNodeRef,
    tabIndex: this.props.disabled ? -1 : 0,
    ...rest
  })

  getInputProps = ({ refKey = 'ref', onChange, onClick, ...rest } = {}) => {
    const { accept, multiple, name } = this.props
    const inputProps = {
      accept,
      type: 'file',
      style: { display: 'none' },
      multiple: supportMultiple && multiple,
      onChange: composeEventHandlers(onChange, this.onDrop),
      onClick: composeEventHandlers(onClick, this.onInputElementClick),
      autoComplete: 'off',
      tabIndex: -1,
      [refKey]: this.setInputRef
    }
    if (name && name.length) {
      inputProps.name = name
    }
    return {
      ...inputProps,
      ...rest
    }
  }

  setNodeRef = node => {
    this.node = node
  }

  setInputRef = input => {
    this.input = input
  }

  /**
   * Open system file upload dialog.
   *
   * @public
   */
  open = () => {
    this.isFileDialogActive = true
    if (this.input) {
      this.input.value = null
      this.input.click()
    }
  }

  render() {
    const { children, multiple, disabled } = this.props
    const { isDragActive, isFocused, draggedFiles, acceptedFiles, rejectedFiles } = this.state

    const filesCount = draggedFiles.length
    const isMultipleAllowed = multiple || filesCount <= 1
    const isDragAccept = filesCount > 0 && allFilesAccepted(draggedFiles, this.props.accept)
    const isDragReject = filesCount > 0 && (!isDragAccept || !isMultipleAllowed)

    return children({
      isDragActive,
      isDragAccept,
      isDragReject,
      draggedFiles,
      acceptedFiles,
      rejectedFiles,
      isFocused: isFocused && !disabled,
      getRootProps: this.getRootProps,
      getInputProps: this.getInputProps,
      open: this.open
    })
  }
}

export default Dropzone

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
}

Dropzone.defaultProps = {
  preventDropOnDocument: true,
  disabled: false,
  multiple: true,
  maxSize: Infinity,
  minSize: 0,
  getDataTransferItems: fromEvent
}
