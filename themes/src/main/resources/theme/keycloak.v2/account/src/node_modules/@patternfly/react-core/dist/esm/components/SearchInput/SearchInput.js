import { __rest } from "tslib";
import * as React from 'react';
import { css } from '@patternfly/react-styles';
import styles from '@patternfly/react-styles/css/components/SearchInput/search-input';
import { Button, ButtonVariant } from '../Button';
import { Badge } from '../Badge';
import AngleDownIcon from '@patternfly/react-icons/dist/esm/icons/angle-down-icon';
import AngleUpIcon from '@patternfly/react-icons/dist/esm/icons/angle-up-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';
import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import CaretDownIcon from '@patternfly/react-icons/dist/esm/icons/caret-down-icon';
import ArrowRightIcon from '@patternfly/react-icons/dist/esm/icons/arrow-right-icon';
import { AdvancedSearchMenu } from './AdvancedSearchMenu';
import { TextInputGroup, TextInputGroupMain, TextInputGroupUtilities } from '../TextInputGroup';
import { InputGroup } from '../InputGroup';
const SearchInputBase = (_a) => {
    var { className, value = '', attributes = [], formAdditionalItems, hasWordsAttrLabel = 'Has words', advancedSearchDelimiter, placeholder, hint, onChange, onSearch, onClear, onToggleAdvancedSearch, isAdvancedSearchOpen, resultsCount, onNextClick, onPreviousClick, innerRef, 'aria-label': ariaLabel = 'Search input', resetButtonLabel = 'Reset', openMenuButtonAriaLabel = 'Open advanced search', previousNavigationButtonAriaLabel = 'Previous', isPreviousNavigationButtonDisabled = false, isNextNavigationButtonDisabled = false, nextNavigationButtonAriaLabel = 'Next', submitSearchButtonLabel = 'Search', isDisabled = false } = _a, props = __rest(_a, ["className", "value", "attributes", "formAdditionalItems", "hasWordsAttrLabel", "advancedSearchDelimiter", "placeholder", "hint", "onChange", "onSearch", "onClear", "onToggleAdvancedSearch", "isAdvancedSearchOpen", "resultsCount", "onNextClick", "onPreviousClick", "innerRef", 'aria-label', "resetButtonLabel", "openMenuButtonAriaLabel", "previousNavigationButtonAriaLabel", "isPreviousNavigationButtonDisabled", "isNextNavigationButtonDisabled", "nextNavigationButtonAriaLabel", "submitSearchButtonLabel", "isDisabled"]);
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
        var searchInputProps = __rest(_a, []);
        return (React.createElement(TextInputGroup, Object.assign({ isDisabled: isDisabled }, searchInputProps),
            React.createElement(TextInputGroupMain, { hint: hint, icon: React.createElement(SearchIcon, null), innerRef: searchInputInputRef, value: searchValue, placeholder: placeholder, "aria-label": ariaLabel, onKeyDown: onEnter, onChange: onChangeHandler }),
            value && (React.createElement(TextInputGroupUtilities, null,
                resultsCount && React.createElement(Badge, { isRead: true }, resultsCount),
                !!onNextClick && !!onPreviousClick && (React.createElement("div", { className: "pf-c-text-input-group__group" },
                    React.createElement(Button, { variant: ButtonVariant.plain, "aria-label": previousNavigationButtonAriaLabel, isDisabled: isDisabled || isPreviousNavigationButtonDisabled, onClick: onPreviousClick },
                        React.createElement(AngleUpIcon, null)),
                    React.createElement(Button, { variant: ButtonVariant.plain, "aria-label": nextNavigationButtonAriaLabel, isDisabled: isDisabled || isNextNavigationButtonDisabled, onClick: onNextClick },
                        React.createElement(AngleDownIcon, null)))),
                !!onClear && (React.createElement(Button, { variant: ButtonVariant.plain, isDisabled: isDisabled, "aria-label": resetButtonLabel, onClick: onClearInput },
                    React.createElement(TimesIcon, null)))))));
    };
    const buildSearchTextInputGroupWithExtraButtons = (_a = {}) => {
        var searchInputProps = __rest(_a, []);
        return (React.createElement(InputGroup, Object.assign({}, searchInputProps),
            buildSearchTextInputGroup(),
            (attributes.length > 0 || onToggleAdvancedSearch) && (React.createElement(Button, { className: isSearchMenuOpen && 'pf-m-expanded', variant: ButtonVariant.control, "aria-label": openMenuButtonAriaLabel, onClick: onToggle, isDisabled: isDisabled, "aria-expanded": isSearchMenuOpen },
                React.createElement(CaretDownIcon, null))),
            !!onSearch && (React.createElement(Button, { type: "submit", variant: ButtonVariant.control, "aria-label": submitSearchButtonLabel, onClick: onSearchHandler, isDisabled: isDisabled || !searchValue },
                React.createElement(ArrowRightIcon, null)))));
    };
    const searchInputProps = Object.assign(Object.assign({}, props), { className: className && css(className), innerRef: searchInputRef });
    if (!!onSearch || attributes.length > 0 || !!onToggleAdvancedSearch) {
        if (attributes.length > 0) {
            return (React.createElement("div", Object.assign({ className: css(className, styles.searchInput), ref: searchInputRef }, props),
                buildSearchTextInputGroupWithExtraButtons(),
                React.createElement(AdvancedSearchMenu, { className: styles.searchInputMenu, value: value, parentRef: searchInputRef, parentInputRef: searchInputInputRef, onSearch: onSearch, onClear: onClear, onChange: onChange, onToggleAdvancedMenu: onToggle, resetButtonLabel: resetButtonLabel, submitSearchButtonLabel: submitSearchButtonLabel, attributes: attributes, formAdditionalItems: formAdditionalItems, hasWordsAttrLabel: hasWordsAttrLabel, advancedSearchDelimiter: advancedSearchDelimiter, getAttrValueMap: getAttrValueMap, isSearchMenuOpen: isSearchMenuOpen })));
        }
        return buildSearchTextInputGroupWithExtraButtons(Object.assign({}, searchInputProps));
    }
    return buildSearchTextInputGroup(searchInputProps);
};
SearchInputBase.displayName = 'SearchInputBase';
export const SearchInput = React.forwardRef((props, ref) => (React.createElement(SearchInputBase, Object.assign({}, props, { innerRef: ref }))));
SearchInput.displayName = 'SearchInput';
//# sourceMappingURL=SearchInput.js.map