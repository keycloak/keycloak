"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ClipboardCopyToggle = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const angle_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-right-icon'));
const angle_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-down-icon'));
const Button_1 = require("../Button");
const ClipboardCopyToggle = (_a) => {
    var { onClick, id, textId, contentId, isExpanded = false } = _a, props = tslib_1.__rest(_a, ["onClick", "id", "textId", "contentId", "isExpanded"]);
    return (React.createElement(Button_1.Button, Object.assign({ type: "button", variant: "control", onClick: onClick, id: id, "aria-labelledby": `${id} ${textId}`, "aria-controls": `${id} ${contentId}`, "aria-expanded": isExpanded }, props), isExpanded ? React.createElement(angle_down_icon_1.default, { "aria-hidden": "true" }) : React.createElement(angle_right_icon_1.default, { "aria-hidden": "true" })));
};
exports.ClipboardCopyToggle = ClipboardCopyToggle;
exports.ClipboardCopyToggle.displayName = 'ClipboardCopyToggle';
//# sourceMappingURL=ClipboardCopyToggle.js.map