"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorControl = exports.DualListSelectorControlBase = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const Button_1 = require("../Button");
const Tooltip_1 = require("../Tooltip");
const DualListSelectorControlBase = (_a) => {
    var { innerRef, children = null, className, 'aria-label': ariaLabel, isDisabled = true, onClick = () => { }, tooltipContent, tooltipProps = {} } = _a, props = tslib_1.__rest(_a, ["innerRef", "children", "className", 'aria-label', "isDisabled", "onClick", "tooltipContent", "tooltipProps"]);
    const ref = innerRef || React.useRef(null);
    return (React.createElement("div", Object.assign({ className: react_styles_1.css('pf-c-dual-list-selector__controls-item', className) }, props),
        React.createElement(Button_1.Button, { isDisabled: isDisabled, "aria-disabled": isDisabled, variant: Button_1.ButtonVariant.plain, onClick: onClick, "aria-label": ariaLabel, tabIndex: -1, ref: ref }, children),
        tooltipContent && React.createElement(Tooltip_1.Tooltip, Object.assign({ content: tooltipContent, position: "left", reference: ref }, tooltipProps))));
};
exports.DualListSelectorControlBase = DualListSelectorControlBase;
exports.DualListSelectorControlBase.displayName = 'DualListSelectorControlBase';
exports.DualListSelectorControl = React.forwardRef((props, ref) => (React.createElement(exports.DualListSelectorControlBase, Object.assign({ innerRef: ref }, props))));
exports.DualListSelectorControl.displayName = 'DualListSelectorControl';
//# sourceMappingURL=DualListSelectorControl.js.map