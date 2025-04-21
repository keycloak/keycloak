"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Masthead = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const masthead_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Masthead/masthead"));
const react_styles_1 = require("@patternfly/react-styles");
const util_1 = require("../../helpers/util");
const Page_1 = require("../Page/Page");
const Masthead = (_a) => {
    var { children, className, backgroundColor = 'dark', display = {
        md: 'inline'
    }, inset } = _a, props = tslib_1.__rest(_a, ["children", "className", "backgroundColor", "display", "inset"]);
    const { width, getBreakpoint } = React.useContext(Page_1.PageContext);
    return (React.createElement("header", Object.assign({ className: react_styles_1.css(masthead_1.default.masthead, util_1.formatBreakpointMods(display, masthead_1.default, 'display-', getBreakpoint(width)), util_1.formatBreakpointMods(inset, masthead_1.default, '', getBreakpoint(width)), backgroundColor === 'light' && masthead_1.default.modifiers.light, backgroundColor === 'light200' && masthead_1.default.modifiers.light_200, className) }, props), children));
};
exports.Masthead = Masthead;
exports.Masthead.displayName = 'Masthead';
//# sourceMappingURL=Masthead.js.map