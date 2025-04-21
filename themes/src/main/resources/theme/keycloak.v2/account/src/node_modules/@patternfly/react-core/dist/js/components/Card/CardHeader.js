"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardHeader = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const card_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Card/card"));
const Card_1 = require("./Card");
const Button_1 = require("../Button");
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const CardHeader = (_a) => {
    var { children = null, className = '', id, onExpand, toggleButtonProps, isToggleRightAligned } = _a, props = tslib_1.__rest(_a, ["children", "className", "id", "onExpand", "toggleButtonProps", "isToggleRightAligned"]);
    return (React.createElement(Card_1.CardContext.Consumer, null, ({ cardId }) => {
        const cardHeaderToggle = (React.createElement("div", { className: react_styles_1.css(card_1.default.cardHeaderToggle) },
            React.createElement(Button_1.Button, Object.assign({ variant: "plain", type: "button", onClick: evt => {
                    onExpand(evt, cardId);
                } }, toggleButtonProps),
                React.createElement("span", { className: react_styles_1.css(card_1.default.cardHeaderToggleIcon) },
                    React.createElement(angle_right_icon_1.default, { "aria-hidden": "true" })))));
        return (React.createElement("div", Object.assign({ className: react_styles_1.css(card_1.default.cardHeader, isToggleRightAligned && card_1.default.modifiers.toggleRight, className), id: id }, props),
            onExpand && !isToggleRightAligned && cardHeaderToggle,
            children,
            onExpand && isToggleRightAligned && cardHeaderToggle));
    }));
};
exports.CardHeader = CardHeader;
exports.CardHeader.displayName = 'CardHeader';
//# sourceMappingURL=CardHeader.js.map