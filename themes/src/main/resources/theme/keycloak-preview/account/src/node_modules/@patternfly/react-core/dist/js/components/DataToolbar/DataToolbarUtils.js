"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.globalBreakpoints = exports.DataToolbarContentContext = exports.DataToolbarContext = void 0;

var React = _interopRequireWildcard(require("react"));

var _global_breakpoint_md = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_md"));

var _global_breakpoint_lg = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_lg"));

var _global_breakpoint_xl = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_xl"));

var _global_breakpoint_2xl = _interopRequireDefault(require("@patternfly/react-tokens/dist/js/global_breakpoint_2xl"));

function _interopRequireDefault(obj) { return obj && obj.__esModule ? obj : { "default": obj }; }

function _getRequireWildcardCache() { if (typeof WeakMap !== "function") return null; var cache = new WeakMap(); _getRequireWildcardCache = function _getRequireWildcardCache() { return cache; }; return cache; }

function _interopRequireWildcard(obj) { if (obj && obj.__esModule) { return obj; } var cache = _getRequireWildcardCache(); if (cache && cache.has(obj)) { return cache.get(obj); } var newObj = {}; if (obj != null) { var hasPropertyDescriptor = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var key in obj) { if (Object.prototype.hasOwnProperty.call(obj, key)) { var desc = hasPropertyDescriptor ? Object.getOwnPropertyDescriptor(obj, key) : null; if (desc && (desc.get || desc.set)) { Object.defineProperty(newObj, key, desc); } else { newObj[key] = obj[key]; } } } } newObj["default"] = obj; if (cache) { cache.set(obj, newObj); } return newObj; }

var DataToolbarContext = React.createContext({
  isExpanded: false,
  toggleIsExpanded: function toggleIsExpanded() {},
  chipGroupContentRef: null,
  updateNumberFilters: function updateNumberFilters() {},
  numberOfFilters: 0
});
exports.DataToolbarContext = DataToolbarContext;
var DataToolbarContentContext = React.createContext({
  expandableContentRef: null,
  expandableContentId: '',
  chipContainerRef: null
});
exports.DataToolbarContentContext = DataToolbarContentContext;

var globalBreakpoints = function globalBreakpoints(breakpoint) {
  var breakpoints = {
    md: parseInt(_global_breakpoint_md["default"].value),
    lg: parseInt(_global_breakpoint_lg["default"].value),
    xl: parseInt(_global_breakpoint_xl["default"].value),
    '2xl': parseInt(_global_breakpoint_2xl["default"].value)
  };
  return breakpoints[breakpoint];
};

exports.globalBreakpoints = globalBreakpoints;
//# sourceMappingURL=DataToolbarUtils.js.map