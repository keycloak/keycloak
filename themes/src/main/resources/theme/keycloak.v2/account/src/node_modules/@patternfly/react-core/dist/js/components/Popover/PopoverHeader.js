"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.PopoverHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const popover_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Popover/popover"));
const Title_1 = require("../Title");
const PopoverHeaderIcon_1 = require("./PopoverHeaderIcon");
const PopoverHeaderText_1 = require("./PopoverHeaderText");
const PopoverHeader = (_a) => {
    var { children, icon, className, titleHeadingLevel = 'h6', alertSeverityVariant, id, alertSeverityScreenReaderText } = _a, props = tslib_1.__rest(_a, ["children", "icon", "className", "titleHeadingLevel", "alertSeverityVariant", "id", "alertSeverityScreenReaderText"]);
    const HeadingLevel = titleHeadingLevel;
    return icon || alertSeverityVariant ? (React.createElement("header", Object.assign({ className: react_styles_1.css('pf-c-popover__header', className), id: id }, props),
        React.createElement(HeadingLevel, { className: react_styles_1.css(popover_1.default.popoverTitle, icon && popover_1.default.modifiers.icon) },
            icon && React.createElement(PopoverHeaderIcon_1.PopoverHeaderIcon, null, icon),
            alertSeverityVariant && alertSeverityScreenReaderText && (React.createElement("span", { className: "pf-u-screen-reader" }, alertSeverityScreenReaderText)),
            React.createElement(PopoverHeaderText_1.PopoverHeaderText, null, children)))) : (React.createElement(Title_1.Title, Object.assign({ headingLevel: titleHeadingLevel, size: Title_1.TitleSizes.md, id: id, className: className }, props), children));
};
exports.PopoverHeader = PopoverHeader;
exports.PopoverHeader.displayName = 'PopoverHeader';
//# sourceMappingURL=PopoverHeader.js.map