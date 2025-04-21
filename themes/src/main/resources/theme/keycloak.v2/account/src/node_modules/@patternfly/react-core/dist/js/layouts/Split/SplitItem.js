"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SplitItem = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const split_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/layouts/Split/split"));
const react_styles_1 = require("@patternfly/react-styles");
const SplitItem = (_a) => {
    var { isFilled = false, className = '', children = null } = _a, props = tslib_1.__rest(_a, ["isFilled", "className", "children"]);
    return (React.createElement("div", Object.assign({}, props, { className: react_styles_1.css(split_1.default.splitItem, isFilled && split_1.default.modifiers.fill, className) }), children));
};
exports.SplitItem = SplitItem;
exports.SplitItem.displayName = 'SplitItem';
//# sourceMappingURL=SplitItem.js.map