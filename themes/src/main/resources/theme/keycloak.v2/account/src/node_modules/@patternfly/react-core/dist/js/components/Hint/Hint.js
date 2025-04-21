"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Hint = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const hint_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Hint/hint"));
const react_styles_1 = require("@patternfly/react-styles");
const Hint = (_a) => {
    var { children, className, actions } = _a, props = tslib_1.__rest(_a, ["children", "className", "actions"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(hint_1.default.hint, className) }, props),
        React.createElement("div", { className: react_styles_1.css(hint_1.default.hintActions) }, actions),
        children));
};
exports.Hint = Hint;
exports.Hint.displayName = 'Hint';
//# sourceMappingURL=Hint.js.map