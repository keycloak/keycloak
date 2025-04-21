"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ToggleGroupItemElement = exports.ToggleGroupItemVariant = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const toggle_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ToggleGroup/toggle-group"));
var ToggleGroupItemVariant;
(function (ToggleGroupItemVariant) {
    ToggleGroupItemVariant["icon"] = "icon";
    ToggleGroupItemVariant["text"] = "text";
})(ToggleGroupItemVariant = exports.ToggleGroupItemVariant || (exports.ToggleGroupItemVariant = {}));
const ToggleGroupItemElement = ({ variant, children }) => (React.createElement("span", { className: react_styles_1.css(variant === 'icon' && toggle_group_1.default.toggleGroupIcon, variant === 'text' && toggle_group_1.default.toggleGroupText) }, children));
exports.ToggleGroupItemElement = ToggleGroupItemElement;
exports.ToggleGroupItemElement.displayName = 'ToggleGroupItemElement';
//# sourceMappingURL=ToggleGroupItemElement.js.map