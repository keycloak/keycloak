"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.AccordionExpandedContentBody = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const accordion_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Accordion/accordion"));
const AccordionExpandedContentBody = ({ children = null }) => React.createElement("div", { className: react_styles_1.css(accordion_1.default.accordionExpandedContentBody) }, children);
exports.AccordionExpandedContentBody = AccordionExpandedContentBody;
exports.AccordionExpandedContentBody.displayName = 'AccordionExpandedContentBody';
//# sourceMappingURL=AccordionExpandedContentBody.js.map