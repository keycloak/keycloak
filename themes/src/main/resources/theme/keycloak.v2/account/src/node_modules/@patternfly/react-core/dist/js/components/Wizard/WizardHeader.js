"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.WizardHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const wizard_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Wizard/wizard"));
const react_styles_1 = require("@patternfly/react-styles");
const Button_1 = require("../Button");
const Title_1 = require("../Title");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const WizardHeader = ({ onClose = () => undefined, title, description, hideClose, closeButtonAriaLabel, titleId, descriptionComponent: Component = 'p', descriptionId }) => (React.createElement("div", { className: react_styles_1.css(wizard_1.default.wizardHeader) },
    !hideClose && (React.createElement(Button_1.Button, { variant: "plain", className: react_styles_1.css(wizard_1.default.wizardClose), "aria-label": closeButtonAriaLabel, onClick: onClose },
        React.createElement(times_icon_1.default, { "aria-hidden": "true" }))),
    React.createElement(Title_1.Title, { headingLevel: "h2", size: "3xl", className: react_styles_1.css(wizard_1.default.wizardTitle), "aria-label": title, id: titleId }, title || React.createElement(React.Fragment, null, "\u00A0")),
    description && (React.createElement(Component, { className: react_styles_1.css(wizard_1.default.wizardDescription), id: descriptionId }, description))));
exports.WizardHeader = WizardHeader;
exports.WizardHeader.displayName = 'WizardHeader';
//# sourceMappingURL=WizardHeader.js.map