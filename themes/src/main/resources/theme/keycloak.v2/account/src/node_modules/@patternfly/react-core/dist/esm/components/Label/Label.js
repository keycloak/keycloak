import { __rest } from "tslib";
import * as React from 'react';
import { useState } from 'react';
import styles from '@patternfly/react-styles/css/components/Label/label';
import labelGrpStyles from '@patternfly/react-styles/css/components/LabelGroup/label-group';
import { Button } from '../Button';
import { Tooltip } from '../Tooltip';
import { css } from '@patternfly/react-styles';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import { useIsomorphicLayoutEffect } from '../../helpers';
const colorStyles = {
    blue: styles.modifiers.blue,
    cyan: styles.modifiers.cyan,
    green: styles.modifiers.green,
    orange: styles.modifiers.orange,
    purple: styles.modifiers.purple,
    red: styles.modifiers.red,
    gold: styles.modifiers.gold,
    grey: ''
};
export const Label = (_a) => {
    var { children, className = '', color = 'grey', variant = 'filled', isCompact = false, isEditable = false, editableProps, isTruncated = false, tooltipPosition, icon, onClose, onEditCancel, onEditComplete, closeBtn, closeBtnAriaLabel, closeBtnProps, href, isOverflowLabel, render } = _a, props = __rest(_a, ["children", "className", "color", "variant", "isCompact", "isEditable", "editableProps", "isTruncated", "tooltipPosition", "icon", "onClose", "onEditCancel", "onEditComplete", "closeBtn", "closeBtnAriaLabel", "closeBtnProps", "href", "isOverflowLabel", "render"]);
    const [isEditableActive, setIsEditableActive] = useState(false);
    const [currValue, setCurrValue] = useState(children);
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
    const button = closeBtn ? (closeBtn) : (React.createElement(Button, Object.assign({ type: "button", variant: "plain", onClick: onClose, "aria-label": closeBtnAriaLabel || `Close ${children}` }, closeBtnProps),
        React.createElement(TimesIcon, null)));
    const textRef = React.createRef();
    // ref to apply tooltip when rendered is used
    const componentRef = React.useRef();
    const [isTooltipVisible, setIsTooltipVisible] = React.useState(false);
    useIsomorphicLayoutEffect(() => {
        const currTextRef = isEditable ? editableButtonRef : textRef;
        if (!isEditableActive) {
            setIsTooltipVisible(currTextRef.current && currTextRef.current.offsetWidth < currTextRef.current.scrollWidth);
        }
    }, [isEditableActive]);
    const content = (React.createElement(React.Fragment, null,
        icon && React.createElement("span", { className: css(styles.labelIcon) }, icon),
        isTruncated && (React.createElement("span", { ref: textRef, className: css(styles.labelText) }, children)),
        !isTruncated && children));
    React.useEffect(() => {
        if (isEditableActive && editableInputRef) {
            editableInputRef.current && editableInputRef.current.focus();
        }
    }, [editableInputRef, isEditableActive]);
    const updateVal = () => {
        setCurrValue(editableInputRef.current.value);
    };
    let labelComponentChild = React.createElement("span", { className: css(styles.labelContent) }, content);
    if (href) {
        labelComponentChild = (React.createElement("a", { className: css(styles.labelContent), href: href }, content));
    }
    else if (isEditable) {
        labelComponentChild = (React.createElement("button", Object.assign({ ref: editableButtonRef, className: css(styles.labelContent), onClick: (e) => {
                setIsEditableActive(true);
                e.stopPropagation();
            } }, editableProps), content));
    }
    if (render) {
        labelComponentChild = (React.createElement(React.Fragment, null,
            isTooltipVisible && React.createElement(Tooltip, { reference: componentRef, content: children, position: tooltipPosition }),
            render({
                className: styles.labelContent,
                content,
                componentRef
            })));
    }
    else if (isTooltipVisible) {
        labelComponentChild = (React.createElement(Tooltip, { content: children, position: tooltipPosition }, labelComponentChild));
    }
    return (React.createElement(LabelComponent, Object.assign({}, props, { className: css(styles.label, colorStyles[color], variant === 'outline' && styles.modifiers.outline, isOverflowLabel && styles.modifiers.overflow, isCompact && styles.modifiers.compact, isEditable && labelGrpStyles.modifiers.editable, isEditableActive && styles.modifiers.editableActive, className) }),
        !isEditableActive && labelComponentChild,
        !isEditableActive && onClose && button,
        isEditableActive && (React.createElement("input", Object.assign({ className: css(styles.labelContent), type: "text", id: "editable-input", ref: editableInputRef, value: currValue, onChange: updateVal }, editableProps)))));
};
Label.displayName = 'Label';
//# sourceMappingURL=Label.js.map