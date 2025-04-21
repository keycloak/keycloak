"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AccordionToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const accordion_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Accordion/accordion"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const AccordionContext_1 = require("./AccordionContext");
const AccordionToggle = (_a) => {
    var { className = '', id, isExpanded = false, children = null, component } = _a, props = tslib_1.__rest(_a, ["className", "id", "isExpanded", "children", "component"]);
    return (React.createElement(AccordionContext_1.AccordionContext.Consumer, null, ({ ToggleContainer }) => {
        const Container = component || ToggleContainer;
        return (React.createElement(Container, null,
            React.createElement("button", Object.assign({ id: id, className: react_styles_1.css(accordion_1.default.accordionToggle, isExpanded && accordion_1.default.modifiers.expanded, className), "aria-expanded": isExpanded, type: "button" }, props),
                React.createElement("span", { className: react_styles_1.css(accordion_1.default.accordionToggleText) }, children),
                React.createElement("span", { className: react_styles_1.css(accordion_1.default.accordionToggleIcon) },
                    React.createElement(angle_right_icon_1.default, null)))));
    }));
};
exports.AccordionToggle = AccordionToggle;
exports.AccordionToggle.displayName = 'AccordionToggle';
//# sourceMappingURL=AccordionToggle.js.map