"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Accordion = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const accordion_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Accordion/accordion"));
const AccordionContext_1 = require("./AccordionContext");
const Accordion = (_a) => {
    var { children = null, className = '', 'aria-label': ariaLabel = '', headingLevel = 'h3', asDefinitionList = true, isBordered = false, displaySize = 'default' } = _a, props = tslib_1.__rest(_a, ["children", "className", 'aria-label', "headingLevel", "asDefinitionList", "isBordered", "displaySize"]);
    const AccordionList = asDefinitionList ? 'dl' : 'div';
    return (React.createElement(AccordionList, Object.assign({ className: react_styles_1.css(accordion_1.default.accordion, isBordered && accordion_1.default.modifiers.bordered, displaySize === 'large' && accordion_1.default.modifiers.displayLg, className), "aria-label": ariaLabel }, props),
        React.createElement(AccordionContext_1.AccordionContext.Provider, { value: {
                ContentContainer: asDefinitionList ? 'dd' : 'div',
                ToggleContainer: asDefinitionList ? 'dt' : headingLevel
            } }, children)));
};
exports.Accordion = Accordion;
exports.Accordion.displayName = 'Accordion';
//# sourceMappingURL=Accordion.js.map