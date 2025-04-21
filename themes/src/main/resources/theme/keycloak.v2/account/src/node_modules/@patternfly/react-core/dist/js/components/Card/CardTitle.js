"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.CardTitle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const card_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Card/card"));
const Card_1 = require("./Card");
const CardTitle = (_a) => {
    var { children = null, className = '', component = 'div' } = _a, props = tslib_1.__rest(_a, ["children", "className", "component"]);
    const { cardId, registerTitleId } = React.useContext(Card_1.CardContext);
    const Component = component;
    const titleId = cardId ? `${cardId}-title` : '';
    React.useEffect(() => {
        registerTitleId(titleId);
        return () => registerTitleId('');
    }, [registerTitleId, titleId]);
    return (React.createElement(Component, Object.assign({ className: react_styles_1.css(card_1.default.cardTitle, className), id: titleId || undefined }, props), children));
};
exports.CardTitle = CardTitle;
exports.CardTitle.displayName = 'CardTitle';
//# sourceMappingURL=CardTitle.js.map