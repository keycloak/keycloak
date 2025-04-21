"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PanelMain = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const panel_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Panel/panel"));
const react_styles_1 = require("@patternfly/react-styles");
const PanelMain = (_a) => {
    var { className, children, maxHeight } = _a, props = tslib_1.__rest(_a, ["className", "children", "maxHeight"]);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(panel_1.default.panelMain, className), style: { '--pf-c-panel__main--MaxHeight': maxHeight } }, props), children));
};
exports.PanelMain = PanelMain;
exports.PanelMain.displayName = 'PanelMain';
//# sourceMappingURL=PanelMain.js.map