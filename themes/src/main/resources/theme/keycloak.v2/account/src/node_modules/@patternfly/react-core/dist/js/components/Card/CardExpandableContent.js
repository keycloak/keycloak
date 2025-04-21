"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardExpandableContent = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const card_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Card/card"));
const react_styles_1 = require("@patternfly/react-styles");
const Card_1 = require("./Card");
const CardExpandableContent = (_a) => {
    var { children = null, className = '' } = _a, props = tslib_1.__rest(_a, ["children", "className"]);
    return (React.createElement(Card_1.CardContext.Consumer, null, ({ isExpanded }) => isExpanded ? (React.createElement("div", Object.assign({ className: react_styles_1.css(card_1.default.cardExpandableContent, className) }, props), children)) : null));
};
exports.CardExpandableContent = CardExpandableContent;
exports.CardExpandableContent.displayName = 'CardExpandableContent';
//# sourceMappingURL=CardExpandableContent.js.map