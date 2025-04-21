"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.DualListSelectorPane = void 0;
const tslib_1 = require("tslib");
const React = tslib_1.__importStar(require("react"));
const dual_list_selector_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/DualListSelector/dual-list-selector"));
const react_styles_1 = require("@patternfly/react-styles");
const form_control_1 = tslib_1.__importDefault(require("@patternfly/react-styles/css/components/FormControl/form-control"));
const DualListSelectorTree_1 = require("./DualListSelectorTree");
const helpers_1 = require("../../helpers");
const DualListSelectorListWrapper_1 = require("./DualListSelectorListWrapper");
const DualListSelectorContext_1 = require("./DualListSelectorContext");
const DualListSelectorList_1 = require("./DualListSelectorList");
const DualListSelectorPane = (_a) => {
    var { isChosen = false, className = '', status = '', actions, searchInput, children, onOptionSelect, onOptionCheck, title = '', options = [], selectedOptions = [], isSearchable = false, searchInputAriaLabel = '', onFilterUpdate, onSearchInputChanged, filterOption, id = helpers_1.getUniqueId('dual-list-selector-pane'), isDisabled = false } = _a, props = tslib_1.__rest(_a, ["isChosen", "className", "status", "actions", "searchInput", "children", "onOptionSelect", "onOptionCheck", "title", "options", "selectedOptions", "isSearchable", "searchInputAriaLabel", "onFilterUpdate", "onSearchInputChanged", "filterOption", "id", "isDisabled"]);
    const [input, setInput] = React.useState('');
    const { isTree } = React.useContext(DualListSelectorContext_1.DualListSelectorContext);
    // only called when search input is dynamically built
    const onChange = (e) => {
        const newValue = e.target.value;
        let filtered;
        if (isTree) {
            filtered = options
                .map(opt => Object.assign({}, opt))
                .filter(item => filterInput(item, newValue));
        }
        else {
            filtered = options.filter(option => {
                if (displayOption(option)) {
                    return option;
                }
            });
        }
        onFilterUpdate(filtered, isChosen ? 'chosen' : 'available', newValue === '');
        if (onSearchInputChanged) {
            onSearchInputChanged(newValue, e);
        }
        setInput(newValue);
    };
    // only called when options are passed via options prop and isTree === true
    const filterInput = (item, input) => {
        if (filterOption) {
            return filterOption(item, input);
        }
        else {
            if (item.text.toLowerCase().includes(input.toLowerCase()) || input === '') {
                return true;
            }
        }
        if (item.children) {
            return ((item.children = item.children.map(opt => Object.assign({}, opt)).filter(child => filterInput(child, input)))
                .length > 0);
        }
    };
    // only called when options are passed via options prop and isTree === false
    const displayOption = (option) => {
        if (filterOption) {
            return filterOption(option, input);
        }
        else {
            return option
                .toString()
                .toLowerCase()
                .includes(input.toLowerCase());
        }
    };
    return (React.createElement("div", Object.assign({ className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorPane, isChosen ? dual_list_selector_1.default.modifiers.chosen : 'pf-m-available', className) }, props),
        title && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorHeader) },
            React.createElement("div", { className: "pf-c-dual-list-selector__title" },
                React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorTitleText) }, title)))),
        (actions || searchInput || isSearchable) && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorTools) },
            (isSearchable || searchInput) && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorToolsFilter) }, searchInput ? (searchInput) : (React.createElement("input", { className: react_styles_1.css(form_control_1.default.formControl, form_control_1.default.modifiers.search), type: "search", onChange: isDisabled ? undefined : onChange, "aria-label": searchInputAriaLabel, disabled: isDisabled })))),
            actions && React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorToolsActions) }, actions))),
        status && (React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorStatus) },
            React.createElement("div", { className: react_styles_1.css(dual_list_selector_1.default.dualListSelectorStatusText), id: `${id}-status` }, status))),
        React.createElement(DualListSelectorContext_1.DualListSelectorPaneContext.Provider, { value: { isChosen } },
            !isTree && (React.createElement(DualListSelectorListWrapper_1.DualListSelectorListWrapper, { "aria-labelledby": `${id}-status`, options: options, selectedOptions: selectedOptions, onOptionSelect: (e, index, id) => onOptionSelect(e, index, isChosen, id), displayOption: displayOption, id: `${id}-list`, isDisabled: isDisabled }, children)),
            isTree && (React.createElement(DualListSelectorListWrapper_1.DualListSelectorListWrapper, { "aria-labelledby": `${id}-status`, id: `${id}-list` }, options.length > 0 ? (React.createElement(DualListSelectorList_1.DualListSelectorList, null,
                React.createElement(DualListSelectorTree_1.DualListSelectorTree, { data: isSearchable
                        ? options
                            .map(opt => Object.assign({}, opt))
                            .filter(item => filterInput(item, input))
                        : options, onOptionCheck: onOptionCheck, id: `${id}-tree`, isDisabled: isDisabled }))) : (children))))));
};
exports.DualListSelectorPane = DualListSelectorPane;
exports.DualListSelectorPane.displayName = 'DualListSelectorPane';
//# sourceMappingURL=DualListSelectorPane.js.map