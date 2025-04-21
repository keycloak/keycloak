"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Badge = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const badge_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Badge/badge"));
const Badge = (_a) => {
    var { isRead = false, className = '', children = '' } = _a, props = tslib_1.__rest(_a, ["isRead", "className", "children"]);
    return (React.createElement("span", Object.assign({}, props, { className: react_styles_1.css(badge_1.default.badge, (isRead ? badge_1.default.modifiers.read : badge_1.default.modifiers.unread), className) }), children));
};
exports.Badge = Badge;
exports.Badge.displayName = 'Badge';
//# sourceMappingURL=Badge.js.map