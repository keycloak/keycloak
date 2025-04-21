"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Label = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_1 = require("react");
const label_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Label/label"));
const label_group_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/LabelGroup/label-group"));
const Button_1 = require("../Button");
const Tooltip_1 = require("../Tooltip");
const react_styles_1 = require("@patternfly/react-styles");
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const helpers_1 = require("../../helpers");
const colorStyles = {
    blue: label_1.default.modifiers.blue,
    cyan: label_1.default.modifiers.cyan,
    green: label_1.default.modifiers.green,
    orange: label_1.default.modifiers.orange,
    purple: label_1.default.modifiers.purple,
    red: label_1.default.modifiers.red,
    gold: label_1.default.modifiers.gold,
    grey: ''
};
const Label = (_a) => {
    var { children, className = '', color = 'grey', variant = 'filled', isCompact = false, isEditable = false, editableProps, isTruncated = false, tooltipPosition, icon, onClose, onEditCancel, onEditComplete, closeBtn, closeBtnAriaLabel, closeBtnProps, href, isOverflowLabel, render } = _a, props = tslib_1.__rest(_a, ["children", "className", "color", "variant", "isCompact", "isEditable", "editableProps", "isTruncated", "tooltipPosition", "icon", "onClose", "onEditCancel", "onEditComplete", "closeBtn", "closeBtnAriaLabel", "closeBtnProps", "href", "isOverflowLabel", "render"]);
    const [isEditableActive, setIsEditableActive] = react_1.useState(false);
    const [currValue, setCurrValue] = react_1.useState(children);
    const editableButtonRef = React.useRef();
    const editableInputRef = React.useRef();
    React.useEffect(() => {
        document.addEventListener('mousedown', onDocMouseDown);
        document.addEventListener('keydown', onKeyDown);
        return () => {
            document.removeEventListener('mousedown', onDocMouseDown);
            document.removeEventListener('keydown', onKeyDown);
        };
    });
    const onDocMouseDown = (event) => {
        if (isEditableActive &&
            editableInputRef &&
            editableInputRef.current &&
            !editableInputRef.current.contains(event.target)) {
            if (editableInputRef.current.value) {
                onEditComplete && onEditComplete(editableInputRef.current.value);
            }
            setIsEditableActive(false);
        }
    };
    const onKeyDown = (event) => {
        const key = event.key;
        if ((!isEditableActive &&
            (!editableButtonRef ||
                !editableButtonRef.current ||
                !editableButtonRef.current.contains(event.target))) ||
            (isEditableActive &&
                (!editableInputRef || !editableInputRef.current || !editableInputRef.current.contains(event.target)))) {
            return;
        }
        if (isEditableActive && (key === 'Enter' || key === 'Tab')) {
            event.preventDefault();
            event.stopImmediatePropagation();
            if (editableInputRef.current.value) {
                onEditComplete && onEditComplete(editableInputRef.current.value);
            }
            setIsEditableActive(false);
        }
        if (isEditableActive && key === 'Escape') {
            event.preventDefault();
            event.stopImmediatePropagation();
            // Reset div text to initial children prop - pre-edit
            if (editableInputRef.current.value) {
                editableInputRef.current.value = children;
                onEditCancel && onEditCancel(children);
            }
            setIsEditableActive(false);
        }
        if (!isEditableActive && key === 'Enter') {
            event.preventDefault();
            event.stopImmediatePropagation();
            setIsEditableActive(true);
            // Set cursor position to end of text
            const el = event.target;
            const range = document.createRange();
            const sel = window.getSelection();
            range.selectNodeContents(el);
            range.collapse(false);
            sel.removeAllRanges();
            sel.addRange(range);
        }
    };
    const LabelComponent = (isOverflowLabel ? 'button' : 'span');
    const button = closeBtn ? (closeBtn) : (React.createElement(Button_1.Button, Object.assign({ type: "button", variant: "plain", onClick: onClose, "aria-label": closeBtnAriaLabel || `Close ${children}` }, closeBtnProps),
        React.createElement(times_icon_1.default, null)));
    const textRef = React.createRef();
    // ref to apply tooltip when rendered is used
    const componentRef = React.useRef();
    const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
    helpers_1.useIsomorphicLayoutEffect(() => {
        const currTextRef = isEditable ? editableButtonRef : textRef;
        if (!isEditableActive) {
            setIsTooltipVisible(currTextRef.current && currTextRef.current.offsetWidth < currTextRef.current.scrollWidth);
        }
    }, [isEditableActive]);
    const content = (React.createElement(React.Fragment, null,
        icon && React.createElement("span", { className: react_styles_1.css(label_1.default.labelIcon) }, icon),
        isTruncated && (React.createElement("span", { ref: textRef, className: react_styles_1.css(label_1.default.labelText) }, children)),
        !isTruncated && children));
    React.useEffect(() => {
        if (isEditableActive && editableInputRef) {
            editableInputRef.current && editableInputRef.current.focus();
        }
    }, [editableInputRef, isEditableActive]);
    const updateVal = () => {
        setCurrValue(editableInputRef.current.value);
    };
    let labelComponentChild = React.createElement("span", { className: react_styles_1.css(label_1.default.labelContent) }, content);
    if (href) {
        labelComponentChild = (React.createElement("a", { className: react_styles_1.css(label_1.default.labelContent), href: href }, content));
    }
    else if (isEditable) {
        labelComponentChild = (React.createElement("button", Object.assign({ ref: editableButtonRef, className: react_styles_1.css(label_1.default.labelContent), onClick: (e) => {
                setIsEditableActive(true);
                e.stopPropagation();
            } }, editableProps), content));
    }
    if (render) {
        labelComponentChild = (React.createElement(React.Fragment, null,
            isTooltipVisible && React.createElement(Tooltip_1.Tooltip, { reference: componentRef, content: children, position: tooltipPosition }),
            render({
                className: label_1.default.labelContent,
                content,
                componentRef
            })));
    }
    else if (isTooltipVisible) {
        labelComponentChild = (React.createElement(Tooltip_1.Tooltip, { content: children, position: tooltipPosition }, labelComponentChild));
    }
    return (React.createElement(LabelComponent, Object.assign({}, props, { className: react_styles_1.css(label_1.default.label, colorStyles[color], variant === 'outline' && label_1.default.modifiers.outline, isOverflowLabel && label_1.default.modifiers.overflow, isCompact && label_1.default.modifiers.compact, isEditable && label_group_1.default.modifiers.editable, isEditableActive && label_1.default.modifiers.editableActive, className) }),
        !isEditableActive && labelComponentChild,
        !isEditableActive && onClose && button,
        isEditableActive && (React.createElement("input", Object.assign({ className: react_styles_1.css(label_1.default.labelContent), type: "text", id: "editable-input", ref: editableInputRef, value: currValue, onChange: updateVal }, editableProps)))));
};
exports.Label = Label;
exports.Label.displayName = 'Label';
//# sourceMappingURL=Label.js.map