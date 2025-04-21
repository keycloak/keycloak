"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SimpleListGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const simple_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/SimpleList/simple-list"));
const SimpleListGroup = (_a) => {
    var { children = null, className = '', title = '', titleClassName = '', id = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "title", "titleClassName", "id"]);
    return (React.createElement("section", Object.assign({ className: react_styles_1.css(simple_list_1.default.simpleListSection) }, props),
        React.createElement("h2", { id: id, className: react_styles_1.css(simple_list_1.default.simpleListTitle, titleClassName), "aria-hidden": "true" }, title),
        React.createElement("ul", { className: react_styles_1.css(className), "aria-labelledby": id }, children)));
};
exports.SimpleListGroup = SimpleListGroup;
exports.SimpleListGroup.displayName = 'SimpleListGroup';
//# sourceMappingURL=SimpleListGroup.js.map