"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SelectMenu = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const select_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Select/select"));
const form_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Form/form"));
const react_styles_1 = require("@patternfly/react-styles");
const SelectOption_1 = require("./SelectOption");
const selectConstants_1 = require("./selectConstants");
const SelectGroup_1 = require("./SelectGroup");
const Divider_1 = require("../Divider/Divider");
class SelectMenuWithRef extends React.Component {
    extendChildren(randomId) {
        const { children, hasInlineFilter, isGrouped } = this.props;
        const childrenArray = children;
        let index = hasInlineFilter ? 1 : 0;
        if (isGrouped) {
            return React.Children.map(childrenArray, (group) => {
                if (group.type === SelectGroup_1.SelectGroup) {
                    return React.cloneElement(group, {
                        titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                        children: React.Children.map(group.props.children, (option) => this.cloneOption(option, index++, randomId))
                    });
                }
                else {
                    return this.cloneOption(group, index++, randomId);
                }
            });
        }
        return React.Children.map(childrenArray, (child) => this.cloneOption(child, index++, randomId));
    }
    cloneOption(child, index, randomId) {
        const { selected, sendRef, keyHandler } = this.props;
        const isSelected = this.checkForValue(child.props.value, selected);
        if (child.type === Divider_1.Divider) {
            return child;
        }
        return React.cloneElement(child, {
            inputId: `${randomId}-${index}`,
            isSelected,
            sendRef,
            keyHandler,
            index
        });
    }
    checkForValue(valueToCheck, options) {
        if (!options || !valueToCheck) {
            return false;
        }
        const isSelectOptionObject = typeof valueToCheck !== 'string' &&
            valueToCheck.toString &&
            valueToCheck.compareTo;
        if (Array.isArray(options)) {
            if (isSelectOptionObject) {
                return options.some(option => option.compareTo(valueToCheck));
            }
            else {
                return options.includes(valueToCheck);
            }
        }
        else {
            if (isSelectOptionObject) {
                return options.compareTo(valueToCheck);
            }
            else {
                return options === valueToCheck;
            }
        }
    }
    extendCheckboxChildren(children) {
        const { isGrouped, checked, sendRef, keyHandler, hasInlineFilter, isLastOptionBeforeFooter } = this.props;
        let index = hasInlineFilter ? 1 : 0;
        if (isGrouped) {
            return React.Children.map(children, (group) => {
                if (group.type === Divider_1.Divider) {
                    return group;
                }
                else if (group.type === SelectOption_1.SelectOption) {
                    return React.cloneElement(group, {
                        isChecked: this.checkForValue(group.props.value, checked),
                        sendRef,
                        keyHandler,
                        index: index++,
                        isLastOptionBeforeFooter
                    });
                }
                return React.cloneElement(group, {
                    titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                    children: group.props.children ? (React.createElement("fieldset", { "aria-labelledby": group.props.label && group.props.label.replace(/\W/g, '-'), className: react_styles_1.css(select_1.default.selectMenuFieldset) }, React.Children.map(group.props.children, (option) => option.type === Divider_1.Divider
                        ? option
                        : React.cloneElement(option, {
                            isChecked: this.checkForValue(option.props.value, checked),
                            sendRef,
                            keyHandler,
                            index: index++,
                            isLastOptionBeforeFooter
                        })))) : null
                });
            });
        }
        return React.Children.map(children, (child) => child.type === Divider_1.Divider
            ? child
            : React.cloneElement(child, {
                isChecked: this.checkForValue(child.props.value, checked),
                sendRef,
                keyHandler,
                index: index++,
                isLastOptionBeforeFooter
            }));
    }
    renderSelectMenu({ variant, inputIdPrefix }) {
        /* eslint-disable @typescript-eslint/no-unused-vars */
        const _a = this.props, { children, isCustomContent, className, isExpanded, openedOnEnter, selected, checked, isGrouped, position, sendRef, keyHandler, maxHeight, noResultsFoundText, createText, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, hasInlineFilter, innerRef, footer, footerRef, isLastOptionBeforeFooter } = _a, props = tslib_1.__rest(_a, ["children", "isCustomContent", "className", "isExpanded", "openedOnEnter", "selected", "checked", "isGrouped", "position", "sendRef", "keyHandler", "maxHeight", "noResultsFoundText", "createText", 'aria-label', 'aria-labelledby', "hasInlineFilter", "innerRef", "footer", "footerRef", "isLastOptionBeforeFooter"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        let Component = 'div';
        const variantProps = Object.assign({ ref: innerRef, className: react_styles_1.css(!footer ? select_1.default.selectMenu : 'pf-c-select__menu-list', position === selectConstants_1.SelectPosition.right && select_1.default.modifiers.alignRight, className) }, (maxHeight && { style: { maxHeight, overflow: 'auto' } }));
        const extendedChildren = () => variant === selectConstants_1.SelectVariant.checkbox
            ? this.extendCheckboxChildren(children)
            : this.extendChildren(inputIdPrefix);
        if (isCustomContent) {
            variantProps.children = children;
        }
        else if (hasInlineFilter) {
            if (React.Children.count(children) === 0) {
                variantProps.children = React.createElement("fieldset", { className: react_styles_1.css(select_1.default.selectMenuFieldset) });
            }
            else {
                variantProps.children = (React.createElement("fieldset", { "aria-label": ariaLabel, "aria-labelledby": (!ariaLabel && ariaLabelledBy) || null, className: react_styles_1.css(form_1.default.formFieldset) },
                    children.shift(),
                    extendedChildren()));
            }
        }
        else {
            variantProps.children = extendedChildren();
            if (!isGrouped) {
                Component = 'ul';
                variantProps.role = 'listbox';
                variantProps['aria-label'] = ariaLabel;
                variantProps['aria-labelledby'] = (!ariaLabel && ariaLabelledBy) || null;
            }
        }
        return (React.createElement(React.Fragment, null,
            React.createElement(Component, Object.assign({}, variantProps, props)),
            footer && (React.createElement("div", { className: react_styles_1.css(select_1.default.selectMenuFooter), ref: footerRef }, footer))));
    }
    render() {
        return React.createElement(selectConstants_1.SelectConsumer, null, context => this.renderSelectMenu(context));
    }
}
SelectMenuWithRef.displayName = 'SelectMenu';
SelectMenuWithRef.defaultProps = {
    className: '',
    isExpanded: false,
    isGrouped: false,
    openedOnEnter: false,
    selected: '',
    maxHeight: '',
    position: selectConstants_1.SelectPosition.left,
    sendRef: () => { },
    keyHandler: () => { },
    isCustomContent: false,
    hasInlineFilter: false,
    isLastOptionBeforeFooter: () => { }
};
exports.SelectMenu = React.forwardRef((props, ref) => (React.createElement(SelectMenuWithRef, Object.assign({ innerRef: ref }, props), props.children)));
//# sourceMappingURL=SelectMenu.js.map