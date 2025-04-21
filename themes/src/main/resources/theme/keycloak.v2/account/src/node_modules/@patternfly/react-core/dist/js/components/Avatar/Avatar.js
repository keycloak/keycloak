"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Avatar = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const avatar_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Avatar/avatar"));
const react_styles_1 = require("@patternfly/react-styles");
const Avatar = (_a) => {
    var { className = '', src = '', alt, border, size } = _a, props = tslib_1.__rest(_a, ["className", "src", "alt", "border", "size"]);
    return (React.createElement("img", Object.assign({ src: src, alt: alt, className: react_styles_1.css(avatar_1.default.avatar, avatar_1.default.modifiers[size], border === 'light' && avatar_1.default.modifiers.light, border === 'dark' && avatar_1.default.modifiers.dark, className) }, props)));
};
exports.Avatar = Avatar;
exports.Avatar.displayName = 'Avatar';
//# sourceMappingURL=Avatar.js.map