"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SelectOption = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const select_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Select/select"));
const check_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Check/check"));
const react_styles_1 = require("@patternfly/react-styles");
const check_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-icon'));
const selectConstants_1 = require("./selectConstants");
const star_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/star-icon'));
const util_1 = require("../../helpers/util");
const constants_1 = require("../../helpers/constants");
class SelectOption extends React.Component {
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
            if (event.key === constants_1.KeyTypes.Tab) {
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
            if (event.key === constants_1.KeyTypes.ArrowUp) {
                keyHandler(index, innerIndex, 'up');
            }
            else if (event.key === constants_1.KeyTypes.ArrowDown) {
                keyHandler(index, innerIndex, 'down');
            }
            else if (event.key === constants_1.KeyTypes.ArrowLeft) {
                keyHandler(index, innerIndex, 'left');
            }
            else if (event.key === constants_1.KeyTypes.ArrowRight) {
                keyHandler(index, innerIndex, 'right');
            }
            else if (event.key === constants_1.KeyTypes.Enter) {
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
        isLastOptionBeforeFooter, isGrouped = false } = _a, props = tslib_1.__rest(_a, ["children", "className", "id", "description", "itemCount", "value", "onClick", "isDisabled", "isPlaceholder", "isNoResultsOption", "isSelected", "isChecked", "isFocused", "sendRef", "keyHandler", "index", "component", "inputId", "isFavorite", "ariaIsFavoriteLabel", "ariaIsNotFavoriteLabel", "isLoad", "isLoading", "setViewMoreNextIndex", "isLastOptionBeforeFooter", "isGrouped"]);
        /* eslint-enable @typescript-eslint/no-unused-vars */
        const Component = component;
        if (!id && isFavorite !== null) {
            // eslint-disable-next-line no-console
            console.error('Please provide an id to use the favorites feature.');
        }
        const generatedId = id || util_1.getUniqueId('select-option');
        const favoriteButton = (onFavorite) => (React.createElement("button", { className: react_styles_1.css(select_1.default.selectMenuItem, select_1.default.modifiers.action, select_1.default.modifiers.favoriteAction), "aria-label": isFavorite ? ariaIsFavoriteLabel : ariaIsNotFavoriteLabel, onClick: () => {
                onFavorite(generatedId.replace('favorite-', ''), isFavorite);
            }, onKeyDown: event => {
                this.onKeyDown(event, 1, () => onFavorite(generatedId.replace('favorite-', ''), isFavorite));
            }, ref: this.favoriteRef },
            React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemActionIcon) },
                React.createElement(star_icon_1.default, null))));
        const itemDisplay = itemCount ? (React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemRow) },
            React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemText) }, children || (value && value.toString && value.toString())),
            React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemCount) }, itemCount))) : (children || value.toString());
        const onViewMoreClick = (event) => {
            // Set the index for the next item to focus after view more clicked, then call view more callback
            setViewMoreNextIndex();
            onClick(event);
        };
        const renderOption = (onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect) => {
            if (variant !== selectConstants_1.SelectVariant.checkbox && isLoading && isGrouped) {
                return (React.createElement("div", { role: "presentation", className: react_styles_1.css(select_1.default.selectListItem, isLoading && select_1.default.modifiers.loading, className) }, children));
            }
            else if (variant !== selectConstants_1.SelectVariant.checkbox && isLoad && isGrouped) {
                return (React.createElement("div", null,
                    React.createElement("button", Object.assign({}, props, { role: "presentation", className: react_styles_1.css(select_1.default.selectMenuItem, select_1.default.modifiers.load, className), onClick: (event) => {
                            onViewMoreClick(event);
                            event.stopPropagation();
                        }, ref: this.ref, type: "button" }), children || value.toString())));
            }
            else if (variant !== selectConstants_1.SelectVariant.checkbox) {
                return (React.createElement("li", { id: generatedId, role: "presentation", className: react_styles_1.css(isLoading && select_1.default.selectListItem, !isLoading && select_1.default.selectMenuWrapper, isFavorite && select_1.default.modifiers.favorite, isFocused && select_1.default.modifiers.focus, isLoading && select_1.default.modifiers.loading), ref: this.liRef },
                    isLoading && children,
                    isLoad && !isGrouped && (React.createElement("button", Object.assign({}, props, { className: react_styles_1.css(select_1.default.selectMenuItem, select_1.default.modifiers.load, className), onClick: (event) => {
                            onViewMoreClick(event);
                            event.stopPropagation();
                        }, ref: this.ref, onKeyDown: (event) => {
                            this.onKeyDown(event, 0);
                        }, type: "button" }), itemDisplay)),
                    !isLoading && !isLoad && (React.createElement(React.Fragment, null,
                        React.createElement(Component, Object.assign({}, props, { className: react_styles_1.css(select_1.default.selectMenuItem, isLoad && select_1.default.modifiers.load, isSelected && select_1.default.modifiers.selected, isDisabled && select_1.default.modifiers.disabled, description && select_1.default.modifiers.description, isFavorite !== null && select_1.default.modifiers.link, className), onClick: (event) => {
                                if (!isDisabled) {
                                    onClick(event);
                                    onSelect(event, value, isPlaceholder);
                                    shouldResetOnSelect && onClose();
                                }
                            }, role: "option", "aria-selected": isSelected || null, ref: this.ref, onKeyDown: (event) => {
                                this.onKeyDown(event, 0);
                            }, type: "button" }),
                            description && (React.createElement(React.Fragment, null,
                                React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemMain) },
                                    itemDisplay,
                                    isSelected && (React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemIcon) },
                                        React.createElement(check_icon_1.default, { "aria-hidden": true })))),
                                React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemDescription) }, description))),
                            !description && (React.createElement(React.Fragment, null,
                                itemDisplay,
                                isSelected && (React.createElement("span", { className: react_styles_1.css(select_1.default.selectMenuItemIcon) },
                                    React.createElement(check_icon_1.default, { "aria-hidden": true })))))),
                        isFavorite !== null && id && favoriteButton(onFavorite)))));
            }
            else if (variant === selectConstants_1.SelectVariant.checkbox && isLoad) {
                return (React.createElement("button", { className: react_styles_1.css(select_1.default.selectMenuItem, select_1.default.modifiers.load, isFocused && select_1.default.modifiers.focus, className), onKeyDown: (event) => {
                        this.onKeyDown(event, 0, undefined, true);
                    }, onClick: (event) => {
                        onViewMoreClick(event);
                        event.stopPropagation();
                    }, ref: this.ref }, children || (value && value.toString && value.toString())));
            }
            else if (variant === selectConstants_1.SelectVariant.checkbox && isLoading) {
                return (React.createElement("div", { className: react_styles_1.css(select_1.default.selectListItem, isLoading && select_1.default.modifiers.loading, className) }, children));
            }
            else if (variant === selectConstants_1.SelectVariant.checkbox && !isNoResultsOption && !isLoading && !isLoad) {
                return (React.createElement("label", Object.assign({}, props, { className: react_styles_1.css(check_1.default.check, select_1.default.selectMenuItem, isDisabled && select_1.default.modifiers.disabled, description && select_1.default.modifiers.description, className), onKeyDown: (event) => {
                        this.onKeyDown(event, 0, undefined, true);
                    } }),
                    React.createElement("input", { id: inputId || `${inputIdPrefix}-${value.toString()}`, className: react_styles_1.css(check_1.default.checkInput), type: "checkbox", onChange: event => {
                            if (!isDisabled) {
                                onClick(event);
                                onSelect(event, value);
                            }
                        }, ref: this.ref, checked: isChecked || false, disabled: isDisabled }),
                    React.createElement("span", { className: react_styles_1.css(check_1.default.checkLabel, isDisabled && select_1.default.modifiers.disabled) }, itemDisplay),
                    description && React.createElement("div", { className: react_styles_1.css(check_1.default.checkDescription) }, description)));
            }
            else if (variant === selectConstants_1.SelectVariant.checkbox && isNoResultsOption && !isLoading && !isLoad) {
                return (React.createElement("div", null,
                    React.createElement(Component, Object.assign({}, props, { className: react_styles_1.css(select_1.default.selectMenuItem, isSelected && select_1.default.modifiers.selected, isDisabled && select_1.default.modifiers.disabled, className), role: "option", "aria-selected": isSelected || null, ref: this.ref, onKeyDown: (event) => {
                            this.onKeyDown(event, 0, undefined, true);
                        }, type: "button" }), itemDisplay)));
            }
        };
        return (React.createElement(selectConstants_1.SelectConsumer, null, ({ onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect }) => (React.createElement(React.Fragment, null, renderOption(onSelect, onClose, variant, inputIdPrefix, onFavorite, shouldResetOnSelect)))));
    }
}
exports.SelectOption = SelectOption;
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