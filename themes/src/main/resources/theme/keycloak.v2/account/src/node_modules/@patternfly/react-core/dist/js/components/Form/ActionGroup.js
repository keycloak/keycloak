"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ActionGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const ActionGroup = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    const customClassName = react_styles_1.css(form_1.default.formGroup, form_1.default.modifiers.action, className);
    const formActionsComponent = React.createElement("div", { className: react_styles_1.css(form_1.default.formActions) }, children);
    return (React.createElement("div", Object.assign({}, props, { className: customClassName }),
        React.createElement("div", { className: react_styles_1.css(form_1.default.formGroupControl) }, formActionsComponent)));
};
exports.ActionGroup = ActionGroup;
exports.ActionGroup.displayName = 'ActionGroup';
//# sourceMappingURL=ActionGroup.js.map