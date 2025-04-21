import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import checkStyles from '@patternfly/react-styles/css/components/Check/check';
import { css } from '@patternfly/react-styles';
import CheckIcon from '@patternfly/react-icons/dist/esm/icons/check-icon';
import { SelectConsumer, SelectVariant } from './selectConstants';
import StarIcon from '@patternfly/react-icons/dist/esm/icons/star-icon';
import { getUniqueId } from '../../helpers/util';
import { KeyTypes } from '../../helpers/constants';
export class SelectOption extends React.Component {
    constructor() {
        super(...arguments);
        this.ref = React.createRef();
        this.liRef = React.createRef();
        this.favoriteRef = React.createRef();
        this.onKeyDown = (event, innerIndex, onEnter, isCheckbox) => {
            const { index, keyHandler, isLastOptionBeforeFooter } = this.props;
            let isLastItemBeforeFooter = false;
            if (isLastOptionBeforeFooter !== undefined) {
                isLastItemBeforeFooter = isLastOptionBeforeFooter(index);
            }
            if (event.key === KeyTypes.Tab) {
                // More modal-like experience for checkboxes
                if (isCheckbox && !isLastItemBeforeFooter) {
                    if (event.shiftKey) {
                        keyHandler(index, innerIndex, 'up');
                    }
                    else {
                        keyHandler(index, innerIndex, 'down');
                    }
                    event.stopPropagation();
                }
                else {
                    if (event.shiftKey) {
                        keyHandler(index, innerIndex, 'up');
                    }
                    else {
                        keyHandler(index, innerIndex, 'tab');
                    }
                }
            }
            event.preventDefault();
            if (event.key === KeyTypes.ArrowUp) {
                keyHandler(index, innerIndex, 'up');
            }
            else if (event.key === KeyTypes.ArrowDown) {
                keyHandler(index, innerIndex, 'down');
            }
            else if (event.key === KeyTypes.ArrowLeft) {
                keyHandler(index, innerIndex, 'left');
            }
            else if (event.key === KeyTypes.ArrowRight) {
                keyHandler(index, innerIndex, 'right');
            }
            else if (event.key === KeyTypes.Enter) {
                if (onEnter !== undefined) {
                    onEnter();
                }
                else {
                    this.ref.current.click();
                }
            }
        };
    }
    componentDidMount() {
        this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.isDisabled ? null : this.favoriteRef.current, this.props.isDisabled ? null : this.liRef.current, this.props.index);
    }
    componentDidUpdate() {
        this.props.sendRef(this.props.isDisabled ? null : this.ref.current, this.props.isDisabled ? null : this.favoriteRef.current, this.props.isDisabled ? null : this.liRef.current, this.props.index);
    }
    render() {
        /* eslint-disable @typescript-eslint/no-unused-vars */
        const _a = this.props, { children, className, id, description, itemCount, value, onClick, isDisabled, isPlaceholder, isNoResultsOption, isSelected, isChecked, isFocused, sendRef, keyHandler, index, component, inputId, isFavorite, ariaIsFavoriteLabel = 'starred', ariaIsNotFavoriteLabel = 'not starred', isLoad, isLoading, setViewMoreNextIndex, 
        // eslint-disable-next-line no-console
        isLastOptionBeforeFooter, isGrouped = false } = _a, props = __rest(_a, ["children", "className", "id", "description", "itemCount", "value", "onClick", "isDisabled", "isPlaceholder", "isNoResultsOption", "isSelected", "isChecked", "isFocused", "sendRef", "keyHandler", "index", "component", "inputId", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "isLoad", "isLoading", "setViewMoreNextIndex", "isLastOptionBeforeFooter", "isGrouped"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        const Component = component;
        if (!id && isFavorite !== null) {
            // eslint-disable-next-line no-console
            console.error('Please provide an id to use the favorites feature.');
        }
        const generatedId = id || getUniqueId('select-option');
        const favoriteButton = (onFavorite) => (React.createElement("button", { className: css(styles.selectMenuItem, styles.modifiers.action, styles.modifiers.favoriteAction), "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel, onClick: () => {
                onFavorite(generatedId.replace('favorite-', ''), isFavorite);
            }, onKeyDown: event => {
                this.onKeyDown(event, 1, () => onFavorite(generatedId.replace('favorite-', ''), isFavorite));
            }, ref: this.favoriteRef },
            React.createElement("span", { className: css(styles.selectMenuItemActionIcon) },
                React.createElement(StarIcon, null))));
        const itemDisplay = itemCount ? (React.createElement("span", { className: css(styles.selectMenuItemRow) },
            React.createElement("span", { className: css(styles.selectMenuItemText) }, children || (value && value.toString && value.toString())),
            React.createElement("span", { className: css(styles.selectMenuItemCount) }, itemCount))) : (children || value.toString());
        const onViewMoreClick = (event) => {
            // Set the index for the next item to focus after view more clicked, then call view more callback
            setViewMoreNextIndex();
            onClick(event);
        };
        const renderOption = (onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect) => {
            if (variant !== SelectVariant.checkbox && isLoading && isGrouped) {
                return (React.createElement("div", { role: "presentation", className: css(styles.selectListItem, isLoading && styles.modifiers.loading, className) }, children));
            }
            else if (variant !== SelectVariant.checkbox && isLoad && isGrouped) {
                return (React.createElement("div", null,
                    React.createElement("button", Object.assign({}, props, { role: "presentation", className: css(styles.selectMenuItem, styles.modifiers.load, className), onClick: (event) => {
                            onViewMoreClick(event);
                            event.stopPropagation();
                        }, ref: this.ref, type: "button" }), children || value.toString())));
            }
            else if (variant !== SelectVariant.checkbox) {
                return (React.createElement("li", { id: generatedId, role: "presentation", className: css(isLoading && styles.selectListItem, !isLoading && styles.selectMenuWrapper, isFavorite && styles.modifiers.favorite, isFocused && styles.modifiers.focus, isLoading && styles.modifiers.loading), ref: this.liRef },
                    isLoading && children,
                    isLoad && !isGrouped && (React.createElement("button", Object.assign({}, props, { className: css(styles.selectMenuItem, styles.modifiers.load, className), onClick: (event) => {
                            onViewMoreClick(event);
                            event.stopPropagation();
                        }, ref: this.ref, onKeyDown: (event) => {
                            this.onKeyDown(event, 0);
                        }, type: "button" }), itemDisplay)),
                    !isLoading && !isLoad && (React.createElement(React.Fragment, null,
                        React.createElement(Component, Object.assign({}, props, { className: css(styles.selectMenuItem, isLoad && styles.modifiers.load, isSelected && styles.modifiers.selected, isDisabled && styles.modifiers.disabled, description && styles.modifiers.description, isFavorite !== null && styles.modifiers.link, className), onClick: (event) => {
                                if (!isDisabled) {
                                    onClick(event);
                                    onSelect(event, value, isPlaceholder);
                                    shouldResetOnSelect && onClose();
                                }
                            }, role: "option", "aria-selected": isSelected || null, ref: this.ref, onKeyDown: (event) => {
                                this.onKeyDown(event, 0);
                            }, type: "button" }),
                            description && (React.createElement(React.Fragment, null,
                                React.createElement("span", { className: css(styles.selectMenuItemMain) },
                                    itemDisplay,
                                    isSelected && (React.createElement("span", { className: css(styles.selectMenuItemIcon) },
                                        React.createElement(CheckIcon, { "aria-hidden": true })))),
                                React.createElement("span", { className: css(styles.selectMenuItemDescription) }, description))),
                            !description && (React.createElement(React.Fragment, null,
                                itemDisplay,
                                isSelected && (React.createElement("span", { className: css(styles.selectMenuItemIcon) },
                                    React.createElement(CheckIcon, { "aria-hidden": true })))))),
                        isFavorite !== null && id && favoriteButton(onFavorite)))));
            }
            else if (variant === SelectVariant.checkbox && isLoad) {
                return (React.createElement("button", { className: css(styles.selectMenuItem, styles.modifiers.load, isFocused && styles.modifiers.focus, className), onKeyDown: (event) => {
                        this.onKeyDown(event, 0, undefined, true);
                    }, onClick: (event) => {
                        onViewMoreClick(event);
                        event.stopPropagation();
                    }, ref: this.ref }, children || (value && value.toString && value.toString())));
            }
            else if (variant === SelectVariant.checkbox && isLoading) {
                return (React.createElement("div", { className: css(styles.selectListItem, isLoading && styles.modifiers.loading, className) }, children));
            }
            else if (variant === SelectVariant.checkbox && !isNoResultsOption && !isLoading && !isLoad) {
                return (React.createElement("label", Object.assign({}, props, { className: css(checkStyles.check, styles.selectMenuItem, isDisabled && styles.modifiers.disabled, description && styles.modifiers.description, className), onKeyDown: (event) => {
                        this.onKeyDown(event, 0, undefined, true);
                    } }),
                    React.createElement("input", { id: inputId || `${inputIdPrefix}-${value.toString()}`, className: css(checkStyles.checkInput), type: "checkbox", onChange: event => {
                            if (!isDisabled) {
                                onClick(event);
                                onSelect(event, value);
                            }
                        }, ref: this.ref, checked: isChecked || false, disabled: isDisabled }),
                    React.createElement("span", { className: css(checkStyles.checkLabel, isDisabled && styles.modifiers.disabled) }, itemDisplay),
                    description && React.createElement("div", { className: css(checkStyles.checkDescription) }, description)));
            }
            else if (variant === SelectVariant.checkbox && isNoResultsOption && !isLoading && !isLoad) {
                return (React.createElement("div", null,
                    React.createElement(Component, Object.assign({}, props, { className: css(styles.selectMenuItem, isSelected && styles.modifiers.selected, isDisabled && styles.modifiers.disabled, className), role: "option", "aria-selected": isSelected || null, ref: this.ref, onKeyDown: (event) => {
                            this.onKeyDown(event, 0, undefined, true);
                        }, type: "button" }), itemDisplay)));
            }
        };
        return (React.createElement(SelectConsumer, null, ({ onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect }) => (React.createElement(React.Fragment, null, renderOption(onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect)))));
    }
}
SelectOption.displayName = 'SelectOption';
SelectOption.defaultProps = {
    className: '',
    value: '',
    index: 0,
    isDisabled: false,
    isPlaceholder: false,
    isSelected: false,
    isChecked: false,
    isNoResultsOption: false,
    component: 'button',
    onClick: () => { },
    sendRef: () => { },
    keyHandler: () => { },
    inputId: '',
    isFavorite: null,
    isLoad: false,
    isLoading: false,
    setViewMoreNextIndex: () => { },
    isLastOptionBeforeFooter: () => false
};
//# sourceMappingURL=SelectOption.js.map