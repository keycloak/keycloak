"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.OptionsMenuSeparator = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const Divider_1 = require("../Divider");
const OptionsMenuSeparator = (_a) => {
    var { component = 'li' } = _a, props = tslib_1.__rest(_a, ["component"]);
    return React.createElement(Divider_1.Divider, Object.assign({ component: component }, props));
};
exports.OptionsMenuSeparator = OptionsMenuSeparator;
exports.OptionsMenuSeparator.displayName = 'OptionsMenuSeparator';
//# sourceMappingURL=OptionsMenuSeparator.js.map