"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.Select = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const select_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Select/select"));
const badge_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Badge/badge"));
const form_control_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));
const button_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/Button/button"));
const react_styles_1 = require("@patternfly/react-styles");
const times_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-circle-icon'));
const check_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/check-circle-icon'));
const exclamation_triangle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-triangle-icon'));
const exclamation_circle_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/exclamation-circle-icon'));
const SelectMenu_1 = require("./SelectMenu");
const SelectOption_1 = require("./SelectOption");
const SelectGroup_1 = require("./SelectGroup");
const SelectToggle_1 = require("./SelectToggle");
const selectConstants_1 = require("./selectConstants");
const ChipGroup_1 = require("../ChipGroup");
const Chip_1 = require("../Chip");
const Spinner_1 = require("../Spinner");
const helpers_1 = require("../../helpers");
const constants_1 = require("../../helpers/constants");
const Divider_1 = require("../Divider");
const Popper_1 = require("../../helpers/Popper/Popper");
const favorites_1 = require("../../helpers/favorites");
const constants_2 = require("../../helpers/constants");
const util_1 = require("../../helpers/util");
// seed for the aria-labelledby ID
let currentId = 0;
class Select extends React.Component {
    constructor() {
        super(...arguments);
        this.parentRef = React.createRef();
        this.menuComponentRef = React.createRef();
        this.filterRef = React.createRef();
        this.clearRef = React.createRef();
        this.inputRef = React.createRef();
        this.refCollection = [[]];
        this.optionContainerRefCollection = [];
        this.footerRef = React.createRef();
        this.state = {
            focusFirstOption: false,
            typeaheadInputValue: null,
            typeaheadFilteredChildren: React.Children.toArray(this.props.children),
            favoritesGroup: [],
            typeaheadCurrIndex: -1,
            typeaheadStoredIndex: -1,
            creatableValue: '',
            tabbedIntoFavoritesMenu: false,
            ouiaStateId: helpers_1.getDefaultOUIAId(Select.displayName, this.props.variant),
            viewMoreNextIndex: -1
        };
        this.getTypeaheadActiveChild = (typeaheadCurrIndex) => this.refCollection[typeaheadCurrIndex] ? this.refCollection[typeaheadCurrIndex][0] : null;
        this.componentDidUpdate = (prevProps, prevState) => {
            if (this.props.hasInlineFilter) {
                this.refCollection[0][0] = this.filterRef.current;
            }
            // Move focus to top of the menu if state.focusFirstOption was updated to true and the menu does not have custom content
            if (!prevState.focusFirstOption && this.state.focusFirstOption && !this.props.customContent) {
                const firstRef = this.refCollection.find(ref => ref !== null);
                if (firstRef && firstRef[0]) {
                    firstRef[0].focus();
                }
            }
            else if (
            // if viewMoreNextIndex is not -1, view more was clicked, set focus on first newly loaded item
            this.state.viewMoreNextIndex !== -1 &&
                this.refCollection.length > this.state.viewMoreNextIndex &&
                this.props.loadingVariant !== 'spinner' &&
                this.refCollection[this.state.viewMoreNextIndex][0] &&
                this.props.variant !== 'typeahead' && // do not hard focus newly added items for typeahead variants
                this.props.variant !== 'typeaheadmulti') {
                this.refCollection[this.state.viewMoreNextIndex][0].focus();
                this.setState({ viewMoreNextIndex: -1 });
            }
            if (this.props.variant === 'typeahead' || this.props.variant === 'typeaheadmulti') {
                const checkUpdatedChildren = (prevChildren, currChildren) => Array.from(prevChildren).some((prevChild, index) => {
                    const prevChildProps = prevChild.props;
                    const currChild = currChildren[index];
                    const { props: currChildProps } = currChild;
                    if (prevChildProps && currChildProps) {
                        return (prevChildProps.value !== currChildProps.value ||
                            prevChildProps.label !== currChildProps.label ||
                            prevChildProps.isDisabled !== currChildProps.isDisabled ||
                            prevChildProps.isPlaceholder !== currChildProps.isPlaceholder);
                    }
                    else {
                        return prevChild !== currChild;
                    }
                });
                const hasUpdatedChildren = prevProps.children.length !== this.props.children.length ||
                    checkUpdatedChildren(prevProps.children, this.props.children) ||
                    (this.props.isGrouped &&
                        Array.from(prevProps.children).some((prevChild, index) => prevChild.type === SelectGroup_1.SelectGroup &&
                            prevChild.props.children &&
                            this.props.children[index].props.children &&
                            (prevChild.props.children.length !== this.props.children[index].props.children.length ||
                                checkUpdatedChildren(prevChild.props.children, this.props.children[index].props.children))));
                if (hasUpdatedChildren) {
                    this.updateTypeAheadFilteredChildren(prevState.typeaheadInputValue || '', null);
                }
            }
            // for menus with favorites,
            // if the number of favorites or typeahead filtered children has changed, the generated
            // list of favorites needs to be updated
            if (this.props.onFavorite &&
                (this.props.favorites.length !== prevProps.favorites.length ||
                    this.state.typeaheadFilteredChildren !== prevState.typeaheadFilteredChildren)) {
                const tempRenderableChildren = this.props.variant === 'typeahead' || this.props.variant === 'typeaheadmulti'
                    ? this.state.typeaheadFilteredChildren
                    : this.props.children;
                const renderableFavorites = favorites_1.createRenderableFavorites(tempRenderableChildren, this.props.isGrouped, this.props.favorites);
                const favoritesGroup = renderableFavorites.length
                    ? [
                        React.createElement(SelectGroup_1.SelectGroup, { key: "favorites", label: this.props.favoritesLabel }, renderableFavorites),
                        React.createElement(Divider_1.Divider, { key: "favorites-group-divider" })
                    ]
                    : [];
                this.setState({ favoritesGroup });
            }
        };
        this.onEnter = () => {
            this.setState({ focusFirstOption: true });
        };
        this.onToggle = (isExpanded, e) => {
            const { isInputValuePersisted, onSelect, onToggle, hasInlineFilter } = this.props;
            if (!isExpanded && isInputValuePersisted && onSelect) {
                onSelect(undefined, this.inputRef.current ? this.inputRef.current.value : '');
            }
            if (isExpanded && hasInlineFilter) {
                this.setState({
                    focusFirstOption: true
                });
            }
            onToggle(isExpanded, e);
        };
        this.onClose = () => {
            const { isInputFilterPersisted } = this.props;
            this.setState(Object.assign(Object.assign({ focusFirstOption: false, typeaheadInputValue: null }, (!isInputFilterPersisted && {
                typeaheadFilteredChildren: React.Children.toArray(this.props.children)
            })), { typeaheadCurrIndex: -1, tabbedIntoFavoritesMenu: false, viewMoreNextIndex: -1 }));
        };
        this.onChange = (e) => {
            if (e.target.value.toString() !== '' && !this.props.isOpen) {
                this.onToggle(true, e);
            }
            if (this.props.onTypeaheadInputChanged) {
                this.props.onTypeaheadInputChanged(e.target.value.toString());
            }
            this.setState({
                typeaheadCurrIndex: -1,
                typeaheadInputValue: e.target.value,
                creatableValue: e.target.value
            });
            this.updateTypeAheadFilteredChildren(e.target.value.toString(), e);
            this.refCollection = [[]];
        };
        this.updateTypeAheadFilteredChildren = (typeaheadInputValue, e) => {
            let typeaheadFilteredChildren;
            const { onFilter, isCreatable, onCreateOption, createText, noResultsFoundText, children, isGrouped, isCreateSelectOptionObject, loadingVariant } = this.props;
            if (onFilter) {
                /* The updateTypeAheadFilteredChildren callback is not only called on input changes but also when the children change.
                 * In this case the e is null but we can get the typeaheadInputValue from the state.
                 */
                typeaheadFilteredChildren = onFilter(e, e ? e.target.value : typeaheadInputValue) || children;
            }
            else {
                let input;
                try {
                    input = new RegExp(typeaheadInputValue.toString(), 'i');
                }
                catch (err) {
                    input = new RegExp(typeaheadInputValue.toString().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
                }
                const childrenArray = React.Children.toArray(children);
                if (isGrouped) {
                    const childFilter = (child) => child.props.value &&
                        child.props.value.toString &&
                        this.getDisplay(child.props.value.toString(), 'text').search(input) === 0;
                    typeaheadFilteredChildren =
                        typeaheadInputValue.toString() !== ''
                            ? React.Children.map(children, group => {
                                if (React.isValidElement(group) &&
                                    group.type === SelectGroup_1.SelectGroup) {
                                    const filteredGroupChildren = React.Children.toArray(group.props.children).filter(childFilter);
                                    if (filteredGroupChildren.length > 0) {
                                        return React.cloneElement(group, {
                                            titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                                            children: filteredGroupChildren
                                        });
                                    }
                                }
                                else {
                                    return React.Children.toArray(group).filter(childFilter);
                                }
                            })
                            : childrenArray;
                }
                else {
                    typeaheadFilteredChildren =
                        typeaheadInputValue.toString() !== ''
                            ? childrenArray.filter(child => {
                                const valueToCheck = child.props.value;
                                // Dividers don't have value and should not be filtered
                                if (!valueToCheck) {
                                    return true;
                                }
                                const isSelectOptionObject = typeof valueToCheck !== 'string' &&
                                    valueToCheck.toString &&
                                    valueToCheck.compareTo;
                                // View more option should be returned as not a match
                                if (loadingVariant !== 'spinner' && (loadingVariant === null || loadingVariant === void 0 ? void 0 : loadingVariant.text) === valueToCheck) {
                                    return true;
                                }
                                // spinner should be returned as not a match
                                if (loadingVariant === 'spinner' && valueToCheck === 'loading') {
                                    return true;
                                }
                                if (isSelectOptionObject) {
                                    return valueToCheck.compareTo(typeaheadInputValue);
                                }
                                else {
                                    return this.getDisplay(child.props.value.toString(), 'text').search(input) === 0;
                                }
                            })
                            : childrenArray;
                }
            }
            if (!typeaheadFilteredChildren) {
                typeaheadFilteredChildren = [];
            }
            if (typeaheadFilteredChildren.length === 0) {
                !isCreatable &&
                    typeaheadFilteredChildren.push(React.createElement(SelectOption_1.SelectOption, { isDisabled: true, key: "no-results", value: noResultsFoundText, isNoResultsOption: true }));
            }
            if (isCreatable && typeaheadInputValue !== '') {
                const newValue = typeaheadInputValue;
                if (!typeaheadFilteredChildren.find((i) => i.props.value && i.props.value.toString().toLowerCase() === newValue.toString().toLowerCase())) {
                    const newOptionValue = isCreateSelectOptionObject
                        ? {
                            toString: () => newValue,
                            compareTo: value => this.toString()
                                .toLowerCase()
                                .includes(value.toString().toLowerCase())
                        }
                        : newValue;
                    typeaheadFilteredChildren.push(React.createElement(SelectOption_1.SelectOption, { key: `create ${newValue}`, value: newOptionValue, onClick: () => onCreateOption && onCreateOption(newValue) },
                        createText,
                        " \"",
                        newValue,
                        "\""));
                }
            }
            this.setState({
                typeaheadFilteredChildren
            });
        };
        this.onClick = (e) => {
            if (!this.props.isOpen) {
                this.onToggle(true, e);
            }
        };
        this.clearSelection = (_e) => {
            this.setState({
                typeaheadInputValue: null,
                typeaheadFilteredChildren: React.Children.toArray(this.props.children),
                typeaheadCurrIndex: -1
            });
        };
        this.sendRef = (optionRef, favoriteRef, optionContainerRef, index) => {
            this.refCollection[index] = [optionRef, favoriteRef];
            this.optionContainerRefCollection[index] = optionContainerRef;
        };
        this.handleMenuKeys = (index, innerIndex, position) => {
            helpers_1.keyHandler(index, innerIndex, position, this.refCollection, this.refCollection);
            if (this.props.variant === selectConstants_1.SelectVariant.typeahead || this.props.variant === selectConstants_1.SelectVariant.typeaheadMulti) {
                if (position !== 'tab') {
                    this.handleTypeaheadKeys(position);
                }
            }
        };
        this.moveFocus = (nextIndex, updateCurrentIndex = true) => {
            const { isCreatable, createText } = this.props;
            const hasDescriptionElm = Boolean(this.refCollection[nextIndex][0] && this.refCollection[nextIndex][0].classList.contains('pf-m-description'));
            const isLoad = Boolean(this.refCollection[nextIndex][0] && this.refCollection[nextIndex][0].classList.contains('pf-m-load'));
            const optionTextElm = hasDescriptionElm
                ? this.refCollection[nextIndex][0].firstElementChild
                : this.refCollection[nextIndex][0];
            let typeaheadInputValue = '';
            if (isCreatable && optionTextElm.innerText.includes(createText)) {
                typeaheadInputValue = this.state.creatableValue;
            }
            else if (optionTextElm && !isLoad) {
                // !isLoad prevents the view more button text from appearing the typeahead input
                typeaheadInputValue = optionTextElm.innerText;
            }
            this.setState(prevState => ({
                typeaheadCurrIndex: updateCurrentIndex ? nextIndex : prevState.typeaheadCurrIndex,
                typeaheadStoredIndex: nextIndex,
                typeaheadInputValue
            }));
        };
        this.switchFocusToFavoriteMenu = () => {
            const { typeaheadCurrIndex, typeaheadStoredIndex } = this.state;
            let indexForFocus = 0;
            if (typeaheadCurrIndex !== -1) {
                indexForFocus = typeaheadCurrIndex;
            }
            else if (typeaheadStoredIndex !== -1) {
                indexForFocus = typeaheadStoredIndex;
            }
            if (this.refCollection[indexForFocus] !== null && this.refCollection[indexForFocus][0] !== null) {
                this.refCollection[indexForFocus][0].focus();
            }
            else {
                this.clearRef.current.focus();
            }
            this.setState({
                tabbedIntoFavoritesMenu: true,
                typeaheadCurrIndex: -1
            });
        };
        this.moveFocusToLastMenuItem = () => {
            const refCollectionLen = this.refCollection.length;
            if (refCollectionLen > 0 &&
                this.refCollection[refCollectionLen - 1] !== null &&
                this.refCollection[refCollectionLen - 1][0] !== null) {
                this.refCollection[refCollectionLen - 1][0].focus();
            }
        };
        this.handleTypeaheadKeys = (position, shiftKey = false) => {
            const { isOpen, onFavorite, isCreatable } = this.props;
            const { typeaheadCurrIndex, tabbedIntoFavoritesMenu } = this.state;
            const typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
            if (isOpen) {
                if (position === 'enter') {
                    if ((typeaheadCurrIndex !== -1 || (isCreatable && this.refCollection.length === 1)) && // do not allow selection without moving to an initial option unless it is a single create option
                        (typeaheadActiveChild || (this.refCollection[0] && this.refCollection[0][0]))) {
                        if (typeaheadActiveChild) {
                            if (!typeaheadActiveChild.classList.contains('pf-m-load')) {
                                const hasDescriptionElm = typeaheadActiveChild.childElementCount > 1;
                                const typeaheadActiveChildText = hasDescriptionElm
                                    ? typeaheadActiveChild.firstChild.innerText
                                    : typeaheadActiveChild.innerText;
                                this.setState({
                                    typeaheadInputValue: typeaheadActiveChildText
                                });
                            }
                        }
                        else if (this.refCollection[0] && this.refCollection[0][0]) {
                            this.setState({
                                typeaheadInputValue: this.refCollection[0][0].innerText
                            });
                        }
                        if (typeaheadActiveChild) {
                            typeaheadActiveChild.click();
                        }
                        else {
                            this.refCollection[0][0].click();
                        }
                    }
                }
                else if (position === 'tab') {
                    if (onFavorite) {
                        // if the input has focus, tab to the first item or the last item that was previously focused.
                        if (this.inputRef.current === document.activeElement) {
                            // If shift is also clicked and there is a footer, tab to the last item in tabbable footer
                            if (this.props.footer && shiftKey) {
                                const tabbableItems = util_1.findTabbableElements(this.footerRef, selectConstants_1.SelectFooterTabbableItems);
                                if (tabbableItems.length > 0) {
                                    if (tabbableItems[tabbableItems.length - 1]) {
                                        tabbableItems[tabbableItems.length - 1].focus();
                                    }
                                }
                            }
                            else {
                                this.switchFocusToFavoriteMenu();
                            }
                        }
                        else {
                            // focus is on menu or footer
                            if (this.props.footer) {
                                let tabbedIntoMenu = false;
                                const tabbableItems = util_1.findTabbableElements(this.footerRef, selectConstants_1.SelectFooterTabbableItems);
                                if (tabbableItems.length > 0) {
                                    // if current element is not in footer, tab to first tabbable element in footer,
                                    // if shift was clicked, tab to input since focus is on menu
                                    const currentElementIndex = tabbableItems.findIndex((item) => item === document.activeElement);
                                    if (currentElementIndex === -1) {
                                        if (shiftKey) {
                                            // currently in menu, shift back to input
                                            this.inputRef.current.focus();
                                        }
                                        else {
                                            // currently in menu, tab to first tabbable item in footer
                                            tabbableItems[0].focus();
                                        }
                                    }
                                    else {
                                        // already in footer
                                        if (shiftKey) {
                                            // shift to previous item
                                            if (currentElementIndex === 0) {
                                                // on first footer item, shift back to menu
                                                this.switchFocusToFavoriteMenu();
                                                tabbedIntoMenu = true;
                                            }
                                            else {
                                                // shift to previous footer item
                                                tabbableItems[currentElementIndex - 1].focus();
                                            }
                                        }
                                        else {
                                            // tab to next tabbable item in footer or to input.
                                            if (tabbableItems[currentElementIndex + 1]) {
                                                tabbableItems[currentElementIndex + 1].focus();
                                            }
                                            else {
                                                this.inputRef.current.focus();
                                            }
                                        }
                                    }
                                }
                                else {
                                    // no tabbable items in footer, tab to input
                                    this.inputRef.current.focus();
                                    tabbedIntoMenu = false;
                                }
                                this.setState({ tabbedIntoFavoritesMenu: tabbedIntoMenu });
                            }
                            else {
                                this.inputRef.current.focus();
                                this.setState({ tabbedIntoFavoritesMenu: false });
                            }
                        }
                    }
                    else {
                        // Close if there is no footer
                        if (!this.props.footer) {
                            this.onToggle(false, null);
                            this.onClose();
                        }
                        else {
                            // has footer
                            const tabbableItems = util_1.findTabbableElements(this.footerRef, selectConstants_1.SelectFooterTabbableItems);
                            const currentElementIndex = tabbableItems.findIndex((item) => item === document.activeElement);
                            if (this.inputRef.current === document.activeElement) {
                                if (shiftKey) {
                                    // close toggle if shift key and tab on input
                                    this.onToggle(false, null);
                                    this.onClose();
                                }
                                else {
                                    // tab to first tabbable item in footer
                                    if (tabbableItems[0]) {
                                        tabbableItems[0].focus();
                                    }
                                    else {
                                        this.onToggle(false, null);
                                        this.onClose();
                                    }
                                }
                            }
                            else {
                                // focus is in footer
                                if (shiftKey) {
                                    if (currentElementIndex === 0) {
                                        // shift tab back to input
                                        this.inputRef.current.focus();
                                    }
                                    else {
                                        // shift to previous footer item
                                        tabbableItems[currentElementIndex - 1].focus();
                                    }
                                }
                                else {
                                    // tab to next footer item or close tab if last item
                                    if (tabbableItems[currentElementIndex + 1]) {
                                        tabbableItems[currentElementIndex + 1].focus();
                                    }
                                    else {
                                        // no next item, close toggle
                                        this.onToggle(false, null);
                                        this.inputRef.current.focus();
                                        this.onClose();
                                    }
                                }
                            }
                        }
                    }
                }
                else if (!tabbedIntoFavoritesMenu) {
                    if (this.refCollection[0][0] === null) {
                        return;
                    }
                    let nextIndex;
                    if (typeaheadCurrIndex === -1 && position === 'down') {
                        nextIndex = 0;
                    }
                    else if (typeaheadCurrIndex === -1 && position === 'up') {
                        nextIndex = this.refCollection.length - 1;
                    }
                    else if (position !== 'left' && position !== 'right') {
                        nextIndex = helpers_1.getNextIndex(typeaheadCurrIndex, position, this.refCollection);
                    }
                    else {
                        nextIndex = typeaheadCurrIndex;
                    }
                    if (this.refCollection[nextIndex] === null) {
                        return;
                    }
                    this.moveFocus(nextIndex);
                }
                else {
                    const nextIndex = this.refCollection.findIndex(ref => ref !== undefined && (ref[0] === document.activeElement || ref[1] === document.activeElement));
                    this.moveFocus(nextIndex);
                }
            }
        };
        this.onClickTypeaheadToggleButton = () => {
            if (this.inputRef && this.inputRef.current) {
                this.inputRef.current.focus();
            }
        };
        this.getDisplay = (value, type = 'node') => {
            if (!value) {
                return;
            }
            const item = this.props.isGrouped
                ? React.Children.toArray(this.props.children)
                    .reduce((acc, curr) => [...acc, ...React.Children.toArray(curr.props.children)], [])
                    .find(child => child.props.value.toString() === value.toString())
                : React.Children.toArray(this.props.children).find(child => child.props.value &&
                    child.props.value.toString() === value.toString());
            if (item) {
                if (item && item.props.children) {
                    if (type === 'node') {
                        return item.props.children;
                    }
                    return this.findText(item);
                }
                return item.props.value.toString();
            }
            return value.toString();
        };
        this.findText = (item) => {
            if (typeof item === 'string') {
                return item;
            }
            else if (!React.isValidElement(item)) {
                return '';
            }
            else {
                const multi = [];
                React.Children.toArray(item.props.children).forEach(child => multi.push(this.findText(child)));
                return multi.join('');
            }
        };
        this.generateSelectedBadge = () => {
            const { customBadgeText, selections } = this.props;
            if (customBadgeText !== null) {
                return customBadgeText;
            }
            if (Array.isArray(selections) && selections.length > 0) {
                return selections.length;
            }
            return null;
        };
        this.setVieMoreNextIndex = () => {
            this.setState({ viewMoreNextIndex: this.refCollection.length - 1 });
        };
        this.isLastOptionBeforeFooter = (index) => this.props.footer && index === this.refCollection.length - 1 ? true : false;
    }
    extendTypeaheadChildren(typeaheadCurrIndex, favoritesGroup) {
        const { isGrouped, onFavorite } = this.props;
        const typeaheadChildren = favoritesGroup
            ? favoritesGroup.concat(this.state.typeaheadFilteredChildren)
            : this.state.typeaheadFilteredChildren;
        const activeElement = this.optionContainerRefCollection[typeaheadCurrIndex];
        let typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
        if (typeaheadActiveChild && typeaheadActiveChild.classList.contains('pf-m-description')) {
            typeaheadActiveChild = typeaheadActiveChild.firstElementChild;
        }
        this.refCollection = [[]];
        this.optionContainerRefCollection = [];
        if (isGrouped) {
            return React.Children.map(typeaheadChildren, (group) => {
                if (group.type === Divider_1.Divider) {
                    return group;
                }
                else if (group.type === SelectGroup_1.SelectGroup && onFavorite) {
                    return React.cloneElement(group, {
                        titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                        children: React.Children.map(group.props.children, (child) => child.type === Divider_1.Divider
                            ? child
                            : React.cloneElement(child, {
                                isFocused: activeElement &&
                                    (activeElement.id === child.props.id ||
                                        (this.props.isCreatable &&
                                            typeaheadActiveChild.innerText ===
                                                `{createText} "${group.props.value}"`))
                            }))
                    });
                }
                else if (group.type === SelectGroup_1.SelectGroup) {
                    return React.cloneElement(group, {
                        titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                        children: React.Children.map(group.props.children, (child) => child.type === Divider_1.Divider
                            ? child
                            : React.cloneElement(child, {
                                isFocused: typeaheadActiveChild &&
                                    (typeaheadActiveChild.innerText === child.props.value.toString() ||
                                        (this.props.isCreatable &&
                                            typeaheadActiveChild.innerText ===
                                                `{createText} "${child.props.value}"`))
                            }))
                    });
                }
                else {
                    // group has been filtered down to SelectOption
                    return React.cloneElement(group, {
                        isFocused: typeaheadActiveChild &&
                            (typeaheadActiveChild.innerText === group.props.value.toString() ||
                                (this.props.isCreatable && typeaheadActiveChild.innerText === `{createText} "${group.props.value}"`))
                    });
                }
            });
        }
        return typeaheadChildren.map((child, index) => {
            const childElement = child;
            return childElement.type.displayName === 'Divider'
                ? child
                : React.cloneElement(child, {
                    isFocused: typeaheadActiveChild
                        ? typeaheadActiveChild.innerText === child.props.value.toString() ||
                            (this.props.isCreatable &&
                                typeaheadActiveChild.innerText === `{createText} "${child.props.value}"`)
                        : index === typeaheadCurrIndex // fallback for view more + typeahead use cases, when the new expanded list is loaded and refCollection hasn't be updated yet
                });
        });
    }
    render() {
        const _a = this.props, { children, chipGroupProps, chipGroupComponent, className, customContent, variant, direction, onSelect, onClear, onBlur, toggleId, isOpen, isGrouped, isPlain, isDisabled, hasPlaceholderStyle, validated, selections: selectionsProp, typeAheadAriaLabel, typeAheadAriaDescribedby, clearSelectionsAriaLabel, toggleAriaLabel, removeSelectionAriaLabel, 'aria-label': ariaLabel, 'aria-labelledby': ariaLabelledBy, 'aria-describedby': ariaDescribedby, 'aria-invalid': ariaInvalid, placeholderText, width, maxHeight, toggleIcon, ouiaId, ouiaSafe, hasInlineFilter, isCheckboxSelectionBadgeHidden, inlineFilterPlaceholderText, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onFilter, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        onTypeaheadInputChanged, onCreateOption, isCreatable, onToggle, createText, noResultsFoundText, customBadgeText, inputIdPrefix, inputAutoComplete, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        isInputValuePersisted, isInputFilterPersisted, 
        /* eslint-enable @typescript-eslint/no-unused-vars */
        menuAppendTo, favorites, onFavorite, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        favoritesLabel, footer, loadingVariant, isCreateSelectOptionObject, shouldResetOnSelect, isFlipEnabled } = _a, props = tslib_1.__rest(_a, ["children", "chipGroupProps", "chipGroupComponent", "className", "customContent", "variant", "direction", "onSelect", "onClear", "onBlur", "toggleId", "isOpen", "isGrouped", "isPlain", "isDisabled", "hasPlaceholderStyle", "validated", "selections", "typeAheadAriaLabel", "typeAheadAriaDescribedby", "clearSelectionsAriaLabel", "toggleAriaLabel", "removeSelectionAriaLabel", 'aria-label', 'aria-labelledby', 'aria-describedby', 'aria-invalid', "placeholderText", "width", "maxHeight", "toggleIcon", "ouiaId", "ouiaSafe", "hasInlineFilter", "isCheckboxSelectionBadgeHidden", "inlineFilterPlaceholderText", "onFilter", "onTypeaheadInputChanged", "onCreateOption", "isCreatable", "onToggle", "createText", "noResultsFoundText", "customBadgeText", "inputIdPrefix", "inputAutoComplete", "isInputValuePersisted", "isInputFilterPersisted", "menuAppendTo", "favorites", "onFavorite", "favoritesLabel", "footer", "loadingVariant", "isCreateSelectOptionObject", "shouldResetOnSelect", "isFlipEnabled"]);
        const { focusFirstOption: openedOnEnter, typeaheadCurrIndex, typeaheadInputValue, typeaheadFilteredChildren, favoritesGroup } = this.state;
        const selectToggleId = toggleId || `pf-select-toggle-id-${currentId++}`;
        const selections = Array.isArray(selectionsProp) ? selectionsProp : [selectionsProp];
        // Find out if the selected option is a placeholder
        const selectedOption = React.Children.toArray(children).find((option) => option.props.value === selections[0]);
        const isSelectedPlaceholder = selectedOption && selectedOption.props.isPlaceholder;
        const hasAnySelections = Boolean(selections[0] && selections[0] !== '');
        const typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
        let childPlaceholderText = null;
        // If onFavorites is set,  add isFavorite prop to children and add a Favorites group to the SelectMenu
        let renderableItems = [];
        if (onFavorite) {
            // if variant is type-ahead call the extendTypeaheadChildren before adding favorites
            let tempExtendedChildren = children;
            if (variant === 'typeahead' || variant === 'typeaheadmulti') {
                tempExtendedChildren = this.extendTypeaheadChildren(typeaheadCurrIndex, favoritesGroup);
            }
            else if (onFavorite) {
                tempExtendedChildren = favoritesGroup.concat(children);
            }
            // mark items that are favorited with isFavorite
            renderableItems = favorites_1.extendItemsWithFavorite(tempExtendedChildren, isGrouped, favorites);
        }
        else {
            renderableItems = children;
        }
        if (!customContent) {
            if (!hasAnySelections && !placeholderText) {
                const childPlaceholder = React.Children.toArray(children).filter((child) => child.props.isPlaceholder === true);
                childPlaceholderText =
                    (childPlaceholder[0] && this.getDisplay(childPlaceholder[0].props.value, 'node')) ||
                        (children[0] && this.getDisplay(children[0].props.value, 'node'));
            }
        }
        if (isOpen) {
            if (renderableItems.find(item => { var _a; return ((_a = item) === null || _a === void 0 ? void 0 : _a.key) === 'loading'; }) === undefined) {
                if (loadingVariant === 'spinner') {
                    renderableItems.push(React.createElement(SelectOption_1.SelectOption, { isLoading: true, key: "loading", value: "loading" },
                        React.createElement(Spinner_1.Spinner, { size: "lg" })));
                }
                else if (loadingVariant === null || loadingVariant === void 0 ? void 0 : loadingVariant.text) {
                    renderableItems.push(React.createElement(SelectOption_1.SelectOption, { isLoad: true, key: "loading", value: loadingVariant.text, setViewMoreNextIndex: this.setVieMoreNextIndex, onClick: loadingVariant === null || loadingVariant === void 0 ? void 0 : loadingVariant.onClick }));
                }
            }
        }
        const hasOnClear = onClear !== Select.defaultProps.onClear;
        const clearBtn = (React.createElement("button", { className: react_styles_1.css(button_1.default.button, button_1.default.modifiers.plain, select_1.default.selectToggleClear), onClick: e => {
                this.clearSelection(e);
                onClear(e);
                e.stopPropagation();
            }, "aria-label": clearSelectionsAriaLabel, type: "button", disabled: isDisabled, ref: this.clearRef, onKeyDown: event => {
                if (event.key === constants_1.KeyTypes.Enter) {
                    this.clearRef.current.click();
                }
            } },
            React.createElement(times_circle_icon_1.default, { "aria-hidden": true })));
        let selectedChips = null;
        if (variant === selectConstants_1.SelectVariant.typeaheadMulti) {
            selectedChips = chipGroupComponent ? (chipGroupComponent) : (React.createElement(ChipGroup_1.ChipGroup, Object.assign({}, chipGroupProps), selections &&
                selections.map(item => (React.createElement(Chip_1.Chip, { key: item, onClick: (e) => onSelect(e, item), closeBtnAriaLabel: removeSelectionAriaLabel }, this.getDisplay(item, 'node'))))));
        }
        if (hasInlineFilter) {
            const filterBox = (React.createElement(React.Fragment, null,
                React.createElement("div", { key: "inline-filter", className: react_styles_1.css(select_1.default.selectMenuSearch) },
                    React.createElement("input", { key: "inline-filter-input", type: "search", className: react_styles_1.css(form_control_1.default.formControl, form_control_1.default.modifiers.search), onChange: this.onChange, placeholder: inlineFilterPlaceholderText, onKeyDown: event => {
                            if (event.key === constants_1.KeyTypes.ArrowUp) {
                                this.handleMenuKeys(0, 0, 'up');
                                event.preventDefault();
                            }
                            else if (event.key === constants_1.KeyTypes.ArrowDown) {
                                this.handleMenuKeys(0, 0, 'down');
                                event.preventDefault();
                            }
                            else if (event.key === constants_1.KeyTypes.ArrowLeft) {
                                this.handleMenuKeys(0, 0, 'left');
                                event.preventDefault();
                            }
                            else if (event.key === constants_1.KeyTypes.ArrowRight) {
                                this.handleMenuKeys(0, 0, 'right');
                                event.preventDefault();
                            }
                            else if (event.key === constants_1.KeyTypes.Tab && variant !== selectConstants_1.SelectVariant.checkbox && this.props.footer) {
                                // tab to footer or close menu if shift key
                                if (event.shiftKey) {
                                    this.onToggle(false, event);
                                }
                                else {
                                    const tabbableItems = util_1.findTabbableElements(this.footerRef, selectConstants_1.SelectFooterTabbableItems);
                                    if (tabbableItems.length > 0) {
                                        tabbableItems[0].focus();
                                        event.stopPropagation();
                                        event.preventDefault();
                                    }
                                    else {
                                        this.onToggle(false, event);
                                    }
                                }
                            }
                            else if (event.key === constants_1.KeyTypes.Tab && variant === selectConstants_1.SelectVariant.checkbox) {
                                // More modal-like experience for checkboxes
                                // Let SelectOption handle this
                                if (event.shiftKey) {
                                    this.handleMenuKeys(0, 0, 'up');
                                }
                                else {
                                    this.handleMenuKeys(0, 0, 'down');
                                }
                                event.stopPropagation();
                                event.preventDefault();
                            }
                        }, ref: this.filterRef, autoComplete: inputAutoComplete })),
                React.createElement(Divider_1.Divider, { key: "inline-filter-divider" })));
            renderableItems = [filterBox, ...typeaheadFilteredChildren].map((option, index) => React.cloneElement(option, { key: index }));
        }
        let variantProps;
        let variantChildren;
        if (customContent) {
            variantProps = {
                selected: selections,
                openedOnEnter,
                isCustomContent: true
            };
            variantChildren = customContent;
        }
        else {
            switch (variant) {
                case 'single':
                    variantProps = {
                        selected: selections[0],
                        hasInlineFilter,
                        openedOnEnter
                    };
                    variantChildren = renderableItems;
                    break;
                case 'checkbox':
                    variantProps = {
                        checked: selections,
                        isGrouped,
                        hasInlineFilter,
                        openedOnEnter
                    };
                    variantChildren = renderableItems;
                    break;
                case 'typeahead':
                    variantProps = {
                        selected: selections[0],
                        openedOnEnter
                    };
                    variantChildren = onFavorite ? renderableItems : this.extendTypeaheadChildren(typeaheadCurrIndex);
                    if (variantChildren.length === 0) {
                        variantChildren.push(React.createElement(SelectOption_1.SelectOption, { isDisabled: true, key: 0, value: noResultsFoundText, isNoResultsOption: true }));
                    }
                    break;
                case 'typeaheadmulti':
                    variantProps = {
                        selected: selections,
                        openedOnEnter
                    };
                    variantChildren = onFavorite ? renderableItems : this.extendTypeaheadChildren(typeaheadCurrIndex);
                    if (variantChildren.length === 0) {
                        variantChildren.push(React.createElement(SelectOption_1.SelectOption, { isDisabled: true, key: 0, value: noResultsFoundText, isNoResultsOption: true }));
                    }
                    break;
            }
        }
        const innerMenu = (React.createElement(SelectMenu_1.SelectMenu
        // This removes the `position: absolute` styling from the `.pf-c-select__menu`
        // allowing the menu to flip correctly
        , Object.assign({}, (isFlipEnabled && { style: { position: 'revert' } }), props, { isGrouped: isGrouped, selected: selections }, variantProps, { openedOnEnter: openedOnEnter, "aria-label": ariaLabel, "aria-labelledby": ariaLabelledBy, sendRef: this.sendRef, keyHandler: this.handleMenuKeys, maxHeight: maxHeight, ref: this.menuComponentRef, footer: footer, footerRef: this.footerRef, isLastOptionBeforeFooter: this.isLastOptionBeforeFooter }), variantChildren));
        const menuContainer = footer ? React.createElement("div", { className: react_styles_1.css(select_1.default.selectMenu) },
            " ",
            innerMenu,
            " ") : innerMenu;
        const popperContainer = (React.createElement("div", Object.assign({ className: react_styles_1.css(select_1.default.select, isOpen && select_1.default.modifiers.expanded, validated === constants_2.ValidatedOptions.success && select_1.default.modifiers.success, validated === constants_2.ValidatedOptions.warning && select_1.default.modifiers.warning, validated === constants_2.ValidatedOptions.error && select_1.default.modifiers.invalid, direction === selectConstants_1.SelectDirection.up && select_1.default.modifiers.top, className) }, (width && { style: { width } }), (ariaDescribedby && { 'aria-describedby': ariaDescribedby }), (validated !== constants_2.ValidatedOptions.default && { 'aria-invalid': ariaInvalid })), isOpen && menuContainer));
        const mainContainer = (React.createElement("div", Object.assign({ className: react_styles_1.css(select_1.default.select, isOpen && select_1.default.modifiers.expanded, validated === constants_2.ValidatedOptions.success && select_1.default.modifiers.success, validated === constants_2.ValidatedOptions.warning && select_1.default.modifiers.warning, validated === constants_2.ValidatedOptions.error && select_1.default.modifiers.invalid, direction === selectConstants_1.SelectDirection.up && select_1.default.modifiers.top, className), ref: this.parentRef }, helpers_1.getOUIAProps(Select.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe), (width && { style: { width } }), (ariaDescribedby && { 'aria-describedby': ariaDescribedby }), (validated !== constants_2.ValidatedOptions.default && { 'aria-invalid': ariaInvalid })),
            React.createElement(SelectToggle_1.SelectToggle, Object.assign({ id: selectToggleId, parentRef: this.parentRef, menuRef: this.menuComponentRef }, (footer && { footerRef: this.footerRef }), { isOpen: isOpen, isPlain: isPlain, hasPlaceholderStyle: hasPlaceholderStyle && (!selections.length || selections[0] === null || isSelectedPlaceholder), onToggle: this.onToggle, onEnter: this.onEnter, onClose: this.onClose, onBlur: onBlur, variant: variant, "aria-labelledby": `${ariaLabelledBy || ''} ${selectToggleId}`, "aria-label": toggleAriaLabel, handleTypeaheadKeys: this.handleTypeaheadKeys, moveFocusToLastMenuItem: this.moveFocusToLastMenuItem, isDisabled: isDisabled, hasClearButton: hasOnClear, hasFooter: footer !== undefined, onClickTypeaheadToggleButton: this.onClickTypeaheadToggleButton }),
                customContent && (React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleWrapper) },
                    toggleIcon && React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleIcon) }, toggleIcon),
                    React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleText) }, placeholderText))),
                variant === selectConstants_1.SelectVariant.single && !customContent && (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleWrapper) },
                        toggleIcon && React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleIcon) }, toggleIcon),
                        React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleText) }, this.getDisplay(selections[0], 'node') || placeholderText || childPlaceholderText)),
                    hasOnClear && hasAnySelections && clearBtn)),
                variant === selectConstants_1.SelectVariant.checkbox && !customContent && (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleWrapper) },
                        toggleIcon && React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleIcon) }, toggleIcon),
                        React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleText) }, placeholderText),
                        !isCheckboxSelectionBadgeHidden && hasAnySelections && (React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleBadge) },
                            React.createElement("span", { className: react_styles_1.css(badge_1.default.badge, badge_1.default.modifiers.read) }, this.generateSelectedBadge())))),
                    hasOnClear && hasAnySelections && clearBtn)),
                variant === selectConstants_1.SelectVariant.typeahead && !customContent && (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleWrapper) },
                        toggleIcon && React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleIcon) }, toggleIcon),
                        React.createElement("input", Object.assign({ className: react_styles_1.css(form_control_1.default.formControl, select_1.default.selectToggleTypeahead), "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id, id: `${selectToggleId}-select-typeahead`, "aria-label": typeAheadAriaLabel }, (typeAheadAriaDescribedby && { 'aria-describedby': typeAheadAriaDescribedby }), { placeholder: placeholderText, value: typeaheadInputValue !== null
                                ? typeaheadInputValue
                                : this.getDisplay(selections[0], 'text') || '', type: "text", onClick: this.onClick, onChange: this.onChange, autoComplete: inputAutoComplete, disabled: isDisabled, ref: this.inputRef }))),
                    hasOnClear && (selections[0] || typeaheadInputValue) && clearBtn)),
                variant === selectConstants_1.SelectVariant.typeaheadMulti && !customContent && (React.createElement(React.Fragment, null,
                    React.createElement("div", { className: react_styles_1.css(select_1.default.selectToggleWrapper) },
                        toggleIcon && React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleIcon) }, toggleIcon),
                        selections && Array.isArray(selections) && selections.length > 0 && selectedChips,
                        React.createElement("input", Object.assign({ className: react_styles_1.css(form_control_1.default.formControl, select_1.default.selectToggleTypeahead), "aria-activedescendant": typeaheadActiveChild && typeaheadActiveChild.id, id: `${selectToggleId}-select-multi-typeahead-typeahead`, "aria-label": typeAheadAriaLabel, "aria-invalid": validated === constants_2.ValidatedOptions.error }, (typeAheadAriaDescribedby && { 'aria-describedby': typeAheadAriaDescribedby }), { placeholder: placeholderText, value: typeaheadInputValue !== null ? typeaheadInputValue : '', type: "text", onChange: this.onChange, onClick: this.onClick, autoComplete: inputAutoComplete, disabled: isDisabled, ref: this.inputRef }))),
                    hasOnClear && ((selections && selections.length > 0) || typeaheadInputValue) && clearBtn)),
                validated === constants_2.ValidatedOptions.success && (React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleStatusIcon) },
                    React.createElement(check_circle_icon_1.default, { "aria-hidden": "true" }))),
                validated === constants_2.ValidatedOptions.error && (React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleStatusIcon) },
                    React.createElement(exclamation_circle_icon_1.default, { "aria-hidden": "true" }))),
                validated === constants_2.ValidatedOptions.warning && (React.createElement("span", { className: react_styles_1.css(select_1.default.selectToggleStatusIcon) },
                    React.createElement(exclamation_triangle_icon_1.default, { "aria-hidden": "true" })))),
            isOpen && menuAppendTo === 'inline' && menuContainer));
        const getParentElement = () => {
            if (this.parentRef && this.parentRef.current) {
                return this.parentRef.current.parentElement;
            }
            return null;
        };
        return (React.createElement(helpers_1.GenerateId, null, randomId => (React.createElement(selectConstants_1.SelectContext.Provider, { value: {
                onSelect,
                onFavorite,
                onClose: this.onClose,
                variant,
                inputIdPrefix: inputIdPrefix || randomId,
                shouldResetOnSelect
            } }, menuAppendTo === 'inline' ? (mainContainer) : (React.createElement(Popper_1.Popper, { trigger: mainContainer, popper: popperContainer, direction: direction, appendTo: menuAppendTo === 'parent' ? getParentElement() : menuAppendTo, isVisible: isOpen }))))));
    }
}
exports.Select = Select;
Select.displayName = 'Select';
Select.defaultProps = {
    children: [],
    className: '',
    position: selectConstants_1.SelectPosition.left,
    direction: selectConstants_1.SelectDirection.down,
    toggleId: null,
    isOpen: false,
    isGrouped: false,
    isPlain: false,
    isDisabled: false,
    hasPlaceholderStyle: false,
    isCreatable: false,
    validated: 'default',
    'aria-label': '',
    'aria-labelledby': '',
    'aria-describedby': '',
    'aria-invalid': false,
    typeAheadAriaLabel: '',
    typeAheadAriaDescribedby: '',
    clearSelectionsAriaLabel: 'Clear all',
    toggleAriaLabel: 'Options menu',
    removeSelectionAriaLabel: 'Remove',
    selections: [],
    createText: 'Create',
    placeholderText: '',
    noResultsFoundText: 'No results found',
    variant: selectConstants_1.SelectVariant.single,
    width: '',
    onClear: () => undefined,
    onCreateOption: () => undefined,
    toggleIcon: null,
    onFilter: null,
    onTypeaheadInputChanged: null,
    customContent: null,
    hasInlineFilter: false,
    inlineFilterPlaceholderText: null,
    customBadgeText: null,
    inputIdPrefix: '',
    inputAutoComplete: 'off',
    menuAppendTo: 'inline',
    favorites: [],
    favoritesLabel: 'Favorites',
    ouiaSafe: true,
    chipGroupComponent: null,
    isInputValuePersisted: false,
    isInputFilterPersisted: false,
    isCreateSelectOptionObject: false,
    shouldResetOnSelect: true,
    isFlipEnabled: false
};
//# sourceMappingURL=Select.js.map