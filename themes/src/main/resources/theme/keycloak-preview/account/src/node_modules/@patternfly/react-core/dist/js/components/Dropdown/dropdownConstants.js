"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.DropdownArrowContext = exports.DropdownContext = exports.DropdownDirection = exports.DropdownPosition = void 0;

var React = _interopRequireWildcard(require("react"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

var DropdownPosition;
exports.DropdownPosition = DropdownPosition;

(function (DropdownPosition) {
  DropdownPosition["right"] = "right";
  DropdownPosition["left"] = "left";
})(DropdownPosition || (exports.DropdownPosition = DropdownPosition = {}));

var DropdownDirection;
exports.DropdownDirection = DropdownDirection;

(function (DropdownDirection) {
  DropdownDirection["up"] = "up";
  DropdownDirection["down"] = "down";
})(DropdownDirection || (exports.DropdownDirection = DropdownDirection = {}));

var DropdownContext = React.createContext({
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  onSelect: function onSelect(event) {
    return undefined;
  },
  id: '',
  toggleIconClass: '',
  toggleTextClass: '',
  menuClass: '',
  itemClass: '',
  toggleClass: '',
  baseClass: '',
  baseComponent: 'div',
  sectionClass: '',
  sectionTitleClass: '',
  sectionComponent: 'section',
  disabledClass: '',
  hoverClass: '',
  separatorClass: '',
  menuComponent: 'ul'
});
exports.DropdownContext = DropdownContext;
var DropdownArrowContext = React.createContext({
  keyHandler: null,
  sendRef: null
});
exports.DropdownArrowContext = DropdownArrowContext;
//# sourceMappingURL=dropdownConstants.js.map