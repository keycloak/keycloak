"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.SearchInput = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const react_styles_1 = require("@patternfly/react-styles");
const search_input_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/SearchInput/search-input"));
const Button_1 = require("../Button");
const Badge_1 = require("../Badge");
const angle_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-down-icon'));
const angle_up_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/angle-up-icon'));
const times_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/times-icon'));
const search_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/search-icon'));
const caret_down_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/caret-down-icon'));
const arrow_right_icon_1 = tslib_1.__importDefault(require('@patternfly/react-icons/dist/js/icons/arrow-right-icon'));
const AdvancedSearchMenu_1 = require("./AdvancedSearchMenu");
const TextInputGroup_1 = require("../TextInputGroup");
const InputGroup_1 = require("../InputGroup");
const SearchInputBase = (_a) => {
    var { className, value = '', attributes = [], formAdditionalItems, hasWordsAttrLabel = 'Has words', advancedSearchDelimiter, placeholder, hint, onChange, onSearch, onClear, onToggleAdvancedSearch, isAdvancedSearchOpen, resultsCount, onNextClick, onPreviousClick, innerRef, 'aria-label': ariaLabel = 'Search input', resetButtonLabel = 'Reset', openMenuButtonAriaLabel = 'Open advanced search', previousNavigationButtonAriaLabel = 'Previous', isPreviousNavigationButtonDisabled = false, isNextNavigationButtonDisabled = false, nextNavigationButtonAriaLabel = 'Next', submitSearchButtonLabel = 'Search', isDisabled = false } = _a, props = tslib_1.__rest(_a, ["className", "value", "attributes", "formAdditionalItems", "hasWordsAttrLabel", "advancedSearchDelimiter", "placeholder", "hint", "onChange", "onSearch", "onClear", "onToggleAdvancedSearch", "isAdvancedSearchOpen", "resultsCount", "onNextClick", "onPreviousClick", "innerRef", 'aria-label', "resetButtonLabel", "openMenuButtonAriaLabel", "previousNavigationButtonAriaLabel", "isPreviousNavigationButtonDisabled", "isNextNavigationButtonDisabled", "nextNavigationButtonAriaLabel", "submitSearchButtonLabel", "isDisabled"]);
    const [isSearchMenuOpen, setIsSearchMenuOpen] = React.useState(false);
    const [searchValue, setSearchValue] = React.useState(value);
    const searchInputRef = React.useRef(null);
    const searchInputInputRef = innerRef || React.useRef(null);
    React.useEffect(() => {
        setSearchValue(value);
    }, [value]);
    React.useEffect(() => {
        if (attributes.length > 0 && !advancedSearchDelimiter) {
            // eslint-disable-next-line no-console
            console.error('An advancedSearchDelimiter prop is required when advanced search attributes are provided using the attributes prop');
        }
    });
    React.useEffect(() => {
        setIsSearchMenuOpen(isAdvancedSearchOpen);
    }, [isAdvancedSearchOpen]);
    const onChangeHandler = (value, event) => {
        if (onChange) {
            onChange(value, event);
        }
        setSearchValue(value);
    };
    const onToggle = (e) => {
        const isOpen = !isSearchMenuOpen;
        setIsSearchMenuOpen(isOpen);
        if (onToggleAdvancedSearch) {
            onToggleAdvancedSearch(e, isOpen);
        }
    };
    const onSearchHandler = (event) => {
        event.preventDefault();
        if (onSearch) {
            onSearch(value, event, getAttrValueMap());
        }
        setIsSearchMenuOpen(false);
    };
    const getAttrValueMap = () => {
        const attrValue = {};
        const pairs = searchValue.split(' ');
        pairs.map(pair => {
            const splitPair = pair.split(advancedSearchDelimiter);
            if (splitPair.length === 2) {
                attrValue[splitPair[0]] = splitPair[1];
            }
            else if (splitPair.length === 1) {
                attrValue.haswords = attrValue.hasOwnProperty('haswords')
                    ? `${attrValue.haswords} ${splitPair[0]}`
                    : splitPair[0];
            }
        });
        return attrValue;
    };
    const onEnter = (event) => {
        if (event.key === 'Enter') {
            onSearchHandler(event);
        }
    };
    const onClearInput = (e) => {
        if (onClear) {
            onClear(e);
        }
        if (searchInputInputRef && searchInputInputRef.current) {
            searchInputInputRef.current.focus();
        }
    };
    const buildSearchTextInputGroup = (_a = {}) => {
        var searchInputProps = tslib_1.__rest(_a, []);
        return (React.createElement(TextInputGroup_1.TextInputGroup, Object.assign({ isDisabled: isDisabled }, searchInputProps),
            React.createElement(TextInputGroup_1.TextInputGroupMain, { hint: hint, icon: React.createElement(search_icon_1.default, null), innerRef: searchInputInputRef, value: searchValue, placeholder: placeholder, "aria-label": ariaLabel, onKeyDown: onEnter, onChange: onChangeHandler }),
            value && (React.createElement(TextInputGroup_1.TextInputGroupUtilities, null,
                resultsCount && React.createElement(Badge_1.Badge, { isRead: true }, resultsCount),
                !!onNextClick && !!onPreviousClick && (React.createElement("div", { className: "pf-c-text-input-group__group" },
                    React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, "aria-label": previousNavigationButtonAriaLabel, isDisabled: isDisabled || isPreviousNavigationButtonDisabled, onClick: onPreviousClick },
                        React.createElement(angle_up_icon_1.default, null)),
                    React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, "aria-label": nextNavigationButtonAriaLabel, isDisabled: isDisabled || isNextNavigationButtonDisabled, onClick: onNextClick },
                        React.createElement(angle_down_icon_1.default, null)))),
                !!onClear && (React.createElement(Button_1.Button, { variant: Button_1.ButtonVariant.plain, isDisabled: isDisabled, "aria-label": resetButtonLabel, onClick: onClearInput },
                    React.createElement(times_icon_1.default, null)))))));
    };
    const buildSearchTextInputGroupWithExtraButtons = (_a = {}) => {
        var searchInputProps = tslib_1.__rest(_a, []);
        return (React.createElement(InputGroup_1.InputGroup, Object.assign({}, searchInputProps),
            buildSearchTextInputGroup(),
            (attributes.length > 0 || onToggleAdvancedSearch) && (React.createElement(Button_1.Button, { className: isSearchMenuOpen && 'pf-m-expanded', variant: Button_1.ButtonVariant.control, "aria-label": openMenuButtonAriaLabel, onClick: onToggle, isDisabled: isDisabled, "aria-expanded": isSearchMenuOpen },
                React.createElement(caret_down_icon_1.default, null))),
            !!onSearch && (React.createElement(Button_1.Button, { type: "submit", variant: Button_1.ButtonVariant.control, "aria-label": submitSearchButtonLabel, onClick: onSearchHandler, isDisabled: isDisabled || !searchValue },
                React.createElement(arrow_right_icon_1.default, null)))));
    };
    const searchInputProps = Object.assign(Object.assign({}, props), { className: className && react_styles_1.css(className), innerRef: searchInputRef });
    if (!!onSearch || attributes.length > 0 || !!onToggleAdvancedSearch) {
        if (attributes.length > 0) {
            return (React.createElement("div", Object.assign({ className: react_styles_1.css(className, search_input_1.default.searchInput), ref: searchInputRef }, props),
                buildSearchTextInputGroupWithExtraButtons(),
                React.createElement(AdvancedSearchMenu_1.AdvancedSearchMenu, { className: search_input_1.default.searchInputMenu, value: value, parentRef: searchInputRef, parentInputRef: searchInputInputRef, onSearch: onSearch, onClear: onClear, onChange: onChange, onToggleAdvancedMenu: onToggle, resetButtonLabel: resetButtonLabel, submitSearchButtonLabel: submitSearchButtonLabel, attributes: attributes, formAdditionalItems: formAdditionalItems, hasWordsAttrLabel: hasWordsAttrLabel, advancedSearchDelimiter: advancedSearchDelimiter, getAttrValueMap: getAttrValueMap, isSearchMenuOpen: isSearchMenuOpen })));
        }
        return buildSearchTextInputGroupWithExtraButtons(Object.assign({}, searchInputProps));
    }
    return buildSearchTextInputGroup(searchInputProps);
};
SearchInputBase.displayName = 'SearchInputBase';
exports.SearchInput = React.forwardRef((props, ref) => (React.createElement(SearchInputBase, Object.assign({}, props, { innerRef: ref }))));
exports.SearchInput.displayName = 'SearchInput';
//# sourceMappingURL=SearchInput.js.map