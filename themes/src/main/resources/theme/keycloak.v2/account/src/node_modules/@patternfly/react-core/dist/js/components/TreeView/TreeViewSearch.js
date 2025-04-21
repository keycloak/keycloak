"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.TreeViewSearch = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const tree_view_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/TreeView/tree-view"));
const form_control_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));
const TreeViewSearch = (_a) => {
    var { className, onSearch, id, name, 'aria-label': ariaLabel } = _a, props = tslib_1.__rest(_a, ["className", "onSearch", "id", "name", 'aria-label']);
    return (React.createElement("div", { className: react_styles_1.css(tree_view_1.default.treeViewSearch, className) },
        React.createElement("input", Object.assign({ className: react_styles_1.css(form_control_1.default.formControl, form_control_1.default.modifiers.search), onChange: onSearch, id: id, name: name, "aria-label": ariaLabel, type: "search" }, props))));
};
exports.TreeViewSearch = TreeViewSearch;
exports.TreeViewSearch.displayName = 'TreeViewSearch';
//# sourceMappingURL=TreeViewSearch.js.map