"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DropdownGroup = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const dropdownConstants_1 = require("./dropdownConstants");
const DropdownGroup = (_a) => {
    var { children = null, className = '', label = '' } = _a, props = tslib_1.__rest(_a, ["children", "className", "label"]);
    return (React.createElement(dropdownConstants_1.DropdownContext.Consumer, null, ({ sectionClass, sectionTitleClass, sectionComponent }) => {
        const SectionComponent = sectionComponent;
        return (React.createElement(SectionComponent, Object.assign({ className: react_styles_1.css(sectionClass, className) }, props),
            label && (React.createElement("h1", { className: react_styles_1.css(sectionTitleClass), "aria-hidden": true }, label)),
            React.createElement("ul", { role: "none" }, children)));
    }));
};
exports.DropdownGroup = DropdownGroup;
exports.DropdownGroup.displayName = 'DropdownGroup';
//# sourceMappingURL=DropdownGroup.js.map