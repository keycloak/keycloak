"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DataListText = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const data_list_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DataList/data-list"));
const Tooltip_1 = require("../Tooltip");
const DataListText = (_a) => {
    var { children = null, className = '', component = 'span', wrapModifier = null, tooltip: tooltipProp = '', onMouseEnter: onMouseEnterProp = () => { } } = _a, props = tslib_1.__rest(_a, ["children", "className", "component", "wrapModifier", "tooltip", "onMouseEnter"]);
    const Component = component;
    const [tooltip, setTooltip] = React.useState('');
    const onMouseEnter = (event) => {
        if (event.target.offsetWidth < event.target.scrollWidth) {
            setTooltip(tooltipProp || event.target.innerHTML);
        }
        else {
            setTooltip('');
        }
        onMouseEnterProp(event);
    };
    const text = (React.createElement(Component, Object.assign({ onMouseEnter: onMouseEnter, className: react_styles_1.css(className, wrapModifier && data_list_1.default.modifiers[wrapModifier], data_list_1.default.dataListText) }, props), children));
    return tooltip !== '' ? (React.createElement(Tooltip_1.Tooltip, { content: tooltip, isVisible: true }, text)) : (text);
};
exports.DataListText = DataListText;
exports.DataListText.displayName = 'DataListText';
//# sourceMappingURL=DataListText.js.map