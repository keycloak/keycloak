"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DescriptionList = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const description_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DescriptionList/description-list"));
const helpers_1 = require("../../helpers");
const setBreakpointModifiers = (prefix, modifiers) => {
    const mods = modifiers;
    return Object.keys(mods || {}).reduce((acc, curr) => curr === 'default' ? Object.assign(Object.assign({}, acc), { [prefix]: mods[curr] }) : Object.assign(Object.assign({}, acc), { [`${prefix}-on-${curr}`]: mods[curr] }), {});
};
const DescriptionList = (_a) => {
    var { className = '', children = null, isHorizontal = false, isAutoColumnWidths, isAutoFit, isInlineGrid, isCompact, isFluid, isFillColumns, columnModifier, autoFitMinModifier, horizontalTermWidthModifier, orientation, style } = _a, props = tslib_1.__rest(_a, ["className", "children", "isHorizontal", "isAutoColumnWidths", "isAutoFit", "isInlineGrid", "isCompact", "isFluid", "isFillColumns", "columnModifier", "autoFitMinModifier", "horizontalTermWidthModifier", "orientation", "style"]);
    if (isAutoFit && autoFitMinModifier) {
        style = Object.assign(Object.assign({}, style), setBreakpointModifiers('--pf-c-description-list--GridTemplateColumns--min', autoFitMinModifier));
    }
    if (isHorizontal && horizontalTermWidthModifier) {
        style = Object.assign(Object.assign({}, style), setBreakpointModifiers('--pf-c-description-list--m-horizontal__term--width', horizontalTermWidthModifier));
    }
    return (React.createElement("dl", Object.assign({ className: react_styles_1.css(description_list_1.default.descriptionList, (isHorizontal || isFluid) && description_list_1.default.modifiers.horizontal, isAutoColumnWidths && description_list_1.default.modifiers.autoColumnWidths, isAutoFit && description_list_1.default.modifiers.autoFit, helpers_1.formatBreakpointMods(columnModifier, description_list_1.default), helpers_1.formatBreakpointMods(orientation, description_list_1.default), isInlineGrid && description_list_1.default.modifiers.inlineGrid, isCompact && description_list_1.default.modifiers.compact, isFluid && description_list_1.default.modifiers.fluid, isFillColumns && description_list_1.default.modifiers.fillColumns, className), style: style }, props), children));
};
exports.DescriptionList = DescriptionList;
exports.DescriptionList.displayName = 'DescriptionList';
//# sourceMappingURL=DescriptionList.js.map