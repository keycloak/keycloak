import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import formStyles from '@patternfly/react-styles/css/components/Form/form';
import { css } from '@patternfly/react-styles';
import { SelectOption } from './SelectOption';
import { SelectConsumer, SelectPosition, SelectVariant } from './selectConstants';
import { SelectGroup } from './SelectGroup';
import { Divider } from '../Divider/Divider';
class SelectMenuWithRef extends React.Component {
    extendChildren(randomId) {
        const { children, hasInlineFilter, isGrouped } = this.props;
        const childrenArray = children;
        let index = hasInlineFilter ? 1 : 0;
        if (isGrouped) {
            return React.Children.map(childrenArray, (group) => {
                if (group.type === SelectGroup) {
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
        if (child.type === Divider) {
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
                if (group.type === Divider) {
                    return group;
                }
                else if (group.type === SelectOption) {
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
                    children: group.props.children ? (React.createElement("fieldset", { "aria-labelledby": group.props.label && group.props.label.replace(/\W/g, '-'), className: css(styles.selectMenuFieldset) }, React.Children.map(group.props.children, (option) => option.type === Divider
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
        return React.Children.map(children, (child) => child.type === Divider
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
        const _a = this.props, { children, isCustomContent, className, isExpanded, openedOnEnter, selected, checked, isGrouped, position, sendRef, keyHandler, maxHeight, noResultsFoundText, createText, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, hasInlineFilter, innerRef, footer, footerRef, isLastOptionBeforeFooter } = _a, props = __rest(_a, ["children", "isCustomContent", "className", "isExpanded", "openedOnEnter", "selected", "checked", "isGrouped", "position", "sendRef", "keyHandler", "maxHeight", "noResultsFoundText", "createText", 'aria-label', 'aria-labelledby', "hasInlineFilter", "innerRef", "footer", "footerRef", "isLastOptionBeforeFooter"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        let Component = 'div';
        const variantProps = Object.assign({ ref: innerRef, className: css(!footer ? styles.selectMenu : 'pf-c-select__menu-list', position === SelectPosition.right && styles.modifiers.alignRight, className) }, (maxHeight && { style: { maxHeight, overflow: 'auto' } }));
        const extendedChildren = () => variant === SelectVariant.checkbox
            ? this.extendCheckboxChildren(children)
            : this.extendChildren(inputIdPrefix);
        if (isCustomContent) {
            variantProps.children = children;
        }
        else if (hasInlineFilter) {
            if (React.Children.count(children) === 0) {
                variantProps.children = React.createElement("fieldset", { className: css(styles.selectMenuFieldset) });
            }
            else {
                variantProps.children = (React.createElement("fieldset", { "aria-label": ariaLabel, "aria-labelledby": (!ariaLabel && ariaLabelledBy) || null, className: css(formStyles.formFieldset) },
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
            footer && (React.createElement("div", { className: css(styles.selectMenuFooter), ref: footerRef }, footer))));
    }
    render() {
        return React.createElement(SelectConsumer, null, context => this.renderSelectMenu(context));
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
    position: SelectPosition.left,
    sendRef: () => { },
    keyHandler: () => { },
    isCustomContent: false,
    hasInlineFilter: false,
    isLastOptionBeforeFooter: () => { }
};
export const SelectMenu = React.forwardRef((props, ref) => (React.createElement(SelectMenuWithRef, Object.assign({ innerRef: ref }, props), props.children)));
//# sourceMappingURL=SelectMenu.js.map