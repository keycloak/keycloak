"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.KeyTypes = exports.SelectDirection = exports.SelectVariant = exports.SelectConsumer = exports.SelectProvider = exports.SelectContext = void 0;

var React = _interopRequireWildcard(require("react"));

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

var SelectContext = React.createContext(null);
exports.SelectContext = SelectContext;
var SelectProvider = SelectContext.Provider;
exports.SelectProvider = SelectProvider;
var SelectConsumer = SelectContext.Consumer;
exports.SelectConsumer = SelectConsumer;
var SelectVariant;
exports.SelectVariant = SelectVariant;

(function (SelectVariant) {
  SelectVariant["single"] = "single";
  SelectVariant["checkbox"] = "checkbox";
  SelectVariant["typeahead"] = "typeahead";
  SelectVariant["typeaheadMulti"] = "typeaheadmulti";
})(SelectVariant || (exports.SelectVariant = SelectVariant = {}));

var SelectDirection;
exports.SelectDirection = SelectDirection;

(function (SelectDirection) {
  SelectDirection["up"] = "up";
  SelectDirection["down"] = "down";
})(SelectDirection || (exports.SelectDirection = SelectDirection = {}));

var KeyTypes = {
  Tab: 'Tab',
  Space: ' ',
  Escape: 'Escape',
  Enter: 'Enter',
  ArrowUp: 'ArrowUp',
  ArrowDown: 'ArrowDown'
};
exports.KeyTypes = KeyTypes;
//# sourceMappingURL=selectConstants.js.map