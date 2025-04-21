"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListDragButton = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const grip_vertical_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/grip-vertical-icon'));
const DataList_1 = require("./DataList");
const DataListDragButton = (_a) => {
    var { className = '', isDisabled = false } = _a, props = tslib_1.__rest(_a, ["className", "isDisabled"]);
    return (React.createElement(DataList_1.DataListContext.Consumer, null, ({ dragKeyHandler }) => (React.createElement("button", Object.assign({ className: react_styles_1.css(data_list_1.default.dataListItemDraggableButton, isDisabled && data_list_1.default.modifiers.disabled, className), onKeyDown: dragKeyHandler, type: "button", disabled: isDisabled }, props),
        React.createElement("span", { className: react_styles_1.css(data_list_1.default.dataListItemDraggableIcon) },
            React.createElement(grip_vertical_icon_1.default, null))))));
};
exports.DataListDragButton = DataListDragButton;
exports.DataListDragButton.displayName = 'DataListDragButton';
//# sourceMappingURL=DataListDragButton.js.map