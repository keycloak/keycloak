"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Level = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const level_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Level/level"));
const Level = (_a) => {
    var { hasGutter, className = '', children = null } = _a, props = tslib_1.__rest(_a, ["hasGutter", "className", "children"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(level_1.default.level, hasGutter && level_1.default.modifiers.gutter, className) }), children));
};
exports.Level = Level;
exports.Level.displayName = 'Level';
//# sourceMappingURL=Level.js.map