"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.globalBreakpoints = exports.ToolbarContentContext = exports.ToolbarContext = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const global_breakpoint_md_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_md'));
const global_breakpoint_lg_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_lg'));
const global_breakpoint_xl_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_xl'));
const global_breakpoint_2xl_1 = tslib_1.__importDefault(require('@patternfly/react-tokens/dist/js/global_breakpoint_2xl'));
exports.ToolbarContext = React.createContext({
    isExpanded: false,
    toggleIsExpanded: () => { },
    chipGroupContentRef: null,
    updateNumberFilters: () => { },
    numberOfFilters: 0,
    clearAllFilters: () => { }
});
exports.ToolbarContentContext = React.createContext({
    expandableContentRef: null,
    expandableContentId: '',
    chipContainerRef: null
});
exports.globalBreakpoints = {
    md: parseInt(global_breakpoint_md_1.default.value),
    lg: parseInt(global_breakpoint_lg_1.default.value),
    xl: parseInt(global_breakpoint_xl_1.default.value),
    '2xl': parseInt(global_breakpoint_2xl_1.default.value)
};
//# sourceMappingURL=ToolbarUtils.js.map