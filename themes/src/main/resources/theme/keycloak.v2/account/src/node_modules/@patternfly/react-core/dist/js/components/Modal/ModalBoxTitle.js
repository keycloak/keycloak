"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ModalBoxTitle = exports.isVariantIcon = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const modal_box_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/ModalBox/modal-box"));
const react_styles_1 = require("@patternfly/react-styles");
const accessibility_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/utilities/Accessibility/accessibility"));
const helpers_1 = require("../../helpers");
const Tooltip_1 = require("../Tooltip");
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const info_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/info-circle-icon'));
const bell_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/bell-icon'));
const helpers_2 = require("../../helpers");
const isVariantIcon = (icon) => ['success', 'danger', 'warning', 'info', 'default'].includes(icon);
exports.isVariantIcon = isVariantIcon;
const ModalBoxTitle = (_a) => {
    var { className = '', id, title, titleIconVariant, titleLabel = '' } = _a, props = tslib_1.__rest(_a, ["className", "id", "title", "titleIconVariant", "titleLabel"]);
    const [hasTooltip, setHasTooltip] = React.useState(false);
    const h1 = React.useRef();
    const label = titleLabel || (exports.isVariantIcon(titleIconVariant) ? `${helpers_1.capitalize(titleIconVariant)} alert:` : titleLabel);
    const variantIcons = {
        success: React.createElement(check_circle_icon_1.default, null),
        danger: React.createElement(exclamation_circle_icon_1.default, null),
        warning: React.createElement(exclamation_triangle_icon_1.default, null),
        info: React.createElement(info_circle_icon_1.default, null),
        default: React.createElement(bell_icon_1.default, null)
    };
    const CustomIcon = !exports.isVariantIcon(titleIconVariant) && titleIconVariant;
    helpers_2.useIsomorphicLayoutEffect(() => {
        setHasTooltip(h1.current && h1.current.offsetWidth < h1.current.scrollWidth);
    }, []);
    const content = (React.createElement("h1", Object.assign({ id: id, ref: h1, className: react_styles_1.css(modal_box_1.default.modalBoxTitle, titleIconVariant && modal_box_1.default.modifiers.icon, className) }, props),
        titleIconVariant && (React.createElement("span", { className: react_styles_1.css(modal_box_1.default.modalBoxTitleIcon) }, exports.isVariantIcon(titleIconVariant) ? variantIcons[titleIconVariant] : React.createElement(CustomIcon, null))),
        label && React.createElement("span", { className: react_styles_1.css(accessibility_1.default.screenReader) }, label),
        React.createElement("span", { className: react_styles_1.css(modal_box_1.default.modalBoxTitleText) }, title)));
    return hasTooltip ? React.createElement(Tooltip_1.Tooltip, { content: title }, content) : content;
};
exports.ModalBoxTitle = ModalBoxTitle;
exports.ModalBoxTitle.displayName = 'ModalBoxTitle';
//# sourceMappingURL=ModalBoxTitle.js.map