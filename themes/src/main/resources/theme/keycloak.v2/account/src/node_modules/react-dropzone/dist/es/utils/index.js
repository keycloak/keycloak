var _typeof = typeof Symbol === "function" && typeof Symbol.iterator === "symbol" ? function (obj) { return typeof obj; } : function (obj) { return obj && typeof Symbol === "function" && obj.constructor === Symbol && obj !== Symbol.prototype ? "symbol" : typeof obj; };

import accepts from 'attr-accept';

export var supportMultiple = typeof document !== 'undefined' && document && document.createElement ? 'multiple' in document.createElement('input') : true;

// Firefox versions prior to 53 return a bogus MIME type for every file drag, so dragovers with
// that MIME type will always be accepted
export function fileAccepted(file, accept) {
  return file.type === 'application/x-moz-file' || accepts(file, accept);
}

export function fileMatchSize(file, maxSize, minSize) {
  return file.size <= maxSize && file.size >= minSize;
}

export function allFilesAccepted(files, accept) {
  return files.every(function (file) {
    return fileAccepted(file, accept);
  });
}

// React's synthetic events has evt.isPropagationStopped,
// but to remain compatibility with other libs (Preact) fall back
// to check evt.cancelBubble
export function isPropagationStopped(evt) {
  if (typeof evt.isPropagationStopped === 'function') {
    return evt.isPropagationStopped();
  } else if (typeof evt.cancelBubble !== 'undefined') {
    return evt.cancelBubble;
  }
  return false;
}

// React's synthetic events has evt.isDefaultPrevented,
// but to remain compatibility with other libs (Preact) first
// check evt.defaultPrevented
export function isDefaultPrevented(evt) {
  if (typeof evt.defaultPrevented !== 'undefined') {
    return evt.defaultPrevented;
  } else if (typeof evt.isDefaultPrevented === 'function') {
    return evt.isDefaultPrevented();
  }
  return false;
}

export function isDragDataWithFiles(evt) {
  if (!evt.dataTransfer) {
    return true;
  }
  // https://developer.mozilla.org/en-US/docs/Web/API/DataTransfer/types
  // https://developer.mozilla.org/en-US/docs/Web/API/HTML_Drag_and_Drop_API/Recommended_drag_types#file
  return Array.prototype.some.call(evt.dataTransfer.types, function (type) {
    return type === 'Files' || type === 'application/x-moz-file';
  });
}

export function isKindFile(item) {
  return (typeof item === 'undefined' ? 'undefined' : _typeof(item)) === 'object' && item !== null && item.kind === 'file';
}

// allow the entire document to be a drag target
export function onDocumentDragOver(evt) {
  evt.preventDefault();
}

function isIe(userAgent) {
  return userAgent.indexOf('MSIE') !== -1 || userAgent.indexOf('Trident/') !== -1;
}

function isEdge(userAgent) {
  return userAgent.indexOf('Edge/') !== -1;
}

export function isIeOrEdge() {
  var userAgent = arguments.length > 0 && arguments[0] !== undefined ? arguments[0] : window.navigator.userAgent;

  return isIe(userAgent) || isEdge(userAgent);
}

/**
 * This is intended to be used to compose event handlers
 * They are executed in order until one of them calls `event.preventDefault()`.
 * Not sure this is the best way to do this, but it seems legit.
 * @param {Function} fns the event hanlder functions
 * @return {Function} the event handler to add to an element
 */
export function composeEventHandlers() {
  for (var _len = arguments.length, fns = Array(_len), _key = 0; _key < _len; _key++) {
    fns[_key] = arguments[_key];
  }

  return function (event) {
    for (var _len2 = arguments.length, args = Array(_len2 > 1 ? _len2 - 1 : 0), _key2 = 1; _key2 < _len2; _key2++) {
      args[_key2 - 1] = arguments[_key2];
    }

    return fns.some(function (fn) {
      fn && fn.apply(undefined, [event].concat(args));
      return event.defaultPrevented;
    });
  };
}