import { __rest } from "tslib";
import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/DualListSelector/dual-list-selector';
import { css } from '@patternfly/react-styles';
import AngleDoubleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-left-icon';
import AngleLeftIcon from '@patternfly/react-icons/dist/esm/icons/angle-left-icon';
import AngleDoubleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-double-right-icon';
import AngleRightIcon from '@patternfly/react-icons/dist/esm/icons/angle-right-icon';
import { DualListSelectorPane } from './DualListSelectorPane';
import { getUniqueId } from '../../helpers';
import { flattenTree, flattenTreeWithFolders, filterFolders, filterTreeItems, filterTreeItemsWithoutFolders, filterRestTreeItems } from './treeUtils';
import { DualListSelectorControlsWrapper } from './DualListSelectorControlsWrapper';
import { DualListSelectorControl } from './DualListSelectorControl';
import { DualListSelectorContext } from './DualListSelectorContext';
export class DualListSelector extends React.Component {
    constructor(props) {
        super(props);
        this.addAllButtonRef = React.createRef();
        this.addSelectedButtonRef = React.createRef();
        this.removeSelectedButtonRef = React.createRef();
        this.removeAllButtonRef = React.createRef();
        /** In dev environment, prevents circular structure during JSON stringification when
         * options passed in to the dual list selector include HTML elements.
         */
        this.replacer = (key, value) => {
            if (key[0] === '_') {
                return undefined;
            }
            return value;
        };
        this.onFilterUpdate = (newFilteredOptions, paneType, isSearchReset) => {
            const { isTree } = this.props;
            if (paneType === 'available') {
                if (isSearchReset) {
                    this.setState({
                        availableFilteredOptions: null,
                        availableTreeFilteredOptions: null
                    });
                    return;
                }
                if (isTree) {
                    this.setState({
                        availableTreeFilteredOptions: flattenTreeWithFolders(newFilteredOptions)
                    });
                }
                else {
                    this.setState({
                        availableFilteredOptions: newFilteredOptions
                    });
                }
            }
            else if (paneType === 'chosen') {
                if (isSearchReset) {
                    this.setState({
                        chosenFilteredOptions: null,
                        chosenTreeFilteredOptions: null
                    });
                    return;
                }
                if (isTree) {
                    this.setState({
                        chosenTreeFilteredOptions: flattenTreeWithFolders(newFilteredOptions)
                    });
                }
                else {
                    this.setState({
                        chosenFilteredOptions: newFilteredOptions
                    });
                }
            }
        };
        this.addAllVisible = () => {
            this.setState(prevState => {
                const itemsToRemove = [];
                const newAvailable = [];
                const movedOptions = prevState.availableFilteredOptions || prevState.availableOptions;
                prevState.availableOptions.forEach(value => {
                    if (movedOptions.indexOf(value) !== -1) {
                        itemsToRemove.push(value);
                    }
                    else {
                        newAvailable.push(value);
                    }
                });
                const newChosen = [...prevState.chosenOptions, ...itemsToRemove];
                this.props.addAll && this.props.addAll(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptions: newChosen,
                    availableOptions: newAvailable,
                    chosenOptionsSelected: [],
                    availableOptionsSelected: []
                };
            });
        };
        this.addAllTreeVisible = () => {
            this.setState(prevState => {
                const movedOptions = prevState.availableTreeFilteredOptions ||
                    flattenTreeWithFolders(prevState.availableOptions);
                const newAvailable = prevState.availableOptions
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterRestTreeItems(item, movedOptions));
                const currChosen = flattenTree(prevState.chosenOptions);
                const nextChosenOptions = currChosen.concat(movedOptions);
                const newChosen = this.createMergedCopy()
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterTreeItemsWithoutFolders(item, nextChosenOptions));
                this.props.addAll && this.props.addAll(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptions: newChosen,
                    chosenFilteredOptions: newChosen,
                    availableOptions: newAvailable,
                    availableFilteredOptions: newAvailable,
                    availableTreeOptionsChecked: [],
                    chosenTreeOptionsChecked: []
                };
            });
        };
        this.addSelected = () => {
            this.setState(prevState => {
                const itemsToRemove = [];
                const newAvailable = [];
                prevState.availableOptions.forEach((value, index) => {
                    if (prevState.availableOptionsSelected.indexOf(index) !== -1) {
                        itemsToRemove.push(value);
                    }
                    else {
                        newAvailable.push(value);
                    }
                });
                const newChosen = [...prevState.chosenOptions, ...itemsToRemove];
                this.props.addSelected && this.props.addSelected(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptionsSelected: [],
                    availableOptionsSelected: [],
                    chosenOptions: newChosen,
                    availableOptions: newAvailable
                };
            });
        };
        this.addTreeSelected = () => {
            this.setState(prevState => {
                // Remove selected available nodes from current available nodes
                const newAvailable = prevState.availableOptions
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterRestTreeItems(item, prevState.availableTreeOptionsChecked));
                // Get next chosen options from current + new nodes and remap from base
                const currChosen = flattenTree(prevState.chosenOptions);
                const nextChosenOptions = currChosen.concat(prevState.availableTreeOptionsChecked);
                const newChosen = this.createMergedCopy()
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterTreeItemsWithoutFolders(item, nextChosenOptions));
                this.props.addSelected && this.props.addSelected(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    availableTreeOptionsChecked: [],
                    chosenTreeOptionsChecked: [],
                    availableOptions: newAvailable,
                    chosenOptions: newChosen
                };
            });
        };
        this.removeAllVisible = () => {
            this.setState(prevState => {
                const itemsToRemove = [];
                const newChosen = [];
                const movedOptions = prevState.chosenFilteredOptions || prevState.chosenOptions;
                prevState.chosenOptions.forEach(value => {
                    if (movedOptions.indexOf(value) !== -1) {
                        itemsToRemove.push(value);
                    }
                    else {
                        newChosen.push(value);
                    }
                });
                const newAvailable = [...prevState.availableOptions, ...itemsToRemove];
                this.props.removeAll && this.props.removeAll(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptions: newChosen,
                    availableOptions: newAvailable,
                    chosenOptionsSelected: [],
                    availableOptionsSelected: []
                };
            });
        };
        this.removeAllTreeVisible = () => {
            this.setState(prevState => {
                const movedOptions = prevState.chosenTreeFilteredOptions ||
                    flattenTreeWithFolders(prevState.chosenOptions);
                const newChosen = prevState.chosenOptions
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterRestTreeItems(item, movedOptions));
                const currAvailable = flattenTree(prevState.availableOptions);
                const nextAvailableOptions = currAvailable.concat(movedOptions);
                const newAvailable = this.createMergedCopy()
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterTreeItemsWithoutFolders(item, nextAvailableOptions));
                this.props.removeAll && this.props.removeAll(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptions: newChosen,
                    availableOptions: newAvailable,
                    availableTreeOptionsChecked: [],
                    chosenTreeOptionsChecked: []
                };
            });
        };
        this.removeSelected = () => {
            this.setState(prevState => {
                const itemsToRemove = [];
                const newChosen = [];
                prevState.chosenOptions.forEach((value, index) => {
                    if (prevState.chosenOptionsSelected.indexOf(index) !== -1) {
                        itemsToRemove.push(value);
                    }
                    else {
                        newChosen.push(value);
                    }
                });
                const newAvailable = [...prevState.availableOptions, ...itemsToRemove];
                this.props.removeSelected && this.props.removeSelected(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    chosenOptionsSelected: [],
                    availableOptionsSelected: [],
                    chosenOptions: newChosen,
                    availableOptions: newAvailable
                };
            });
        };
        this.removeTreeSelected = () => {
            this.setState(prevState => {
                // Remove selected chosen nodes from current chosen nodes
                const newChosen = prevState.chosenOptions
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterRestTreeItems(item, prevState.chosenTreeOptionsChecked));
                // Get next chosen options from current and remap from base
                const currAvailable = flattenTree(prevState.availableOptions);
                const nextAvailableOptions = currAvailable.concat(prevState.chosenTreeOptionsChecked);
                const newAvailable = this.createMergedCopy()
                    .map(opt => Object.assign({}, opt))
                    .filter(item => filterTreeItemsWithoutFolders(item, nextAvailableOptions));
                this.props.removeSelected && this.props.removeSelected(newAvailable, newChosen);
                this.props.onListChange && this.props.onListChange(newAvailable, newChosen);
                return {
                    availableTreeOptionsChecked: [],
                    chosenTreeOptionsChecked: [],
                    availableOptions: newAvailable,
                    chosenOptions: newChosen
                };
            });
        };
        this.onOptionSelect = (e, index, isChosen, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        id, itemData, parentData
        /* eslint-enable @typescript-eslint/no-unused-vars */
        ) => {
            this.setState(prevState => {
                const originalArray = isChosen ? prevState.chosenOptionsSelected : prevState.availableOptionsSelected;
                let updatedArray = null;
                if (originalArray.indexOf(index) !== -1) {
                    updatedArray = originalArray.filter(value => value !== index);
                }
                else {
                    updatedArray = [...originalArray, index];
                }
                return {
                    chosenOptionsSelected: isChosen ? updatedArray : prevState.chosenOptionsSelected,
                    availableOptionsSelected: isChosen ? prevState.availableOptionsSelected : updatedArray
                };
            });
            this.props.onOptionSelect && this.props.onOptionSelect(e, index, isChosen, id, itemData, parentData);
        };
        this.isChecked = (treeItem, isChosen) => isChosen
            ? this.state.chosenTreeOptionsChecked.includes(treeItem.id)
            : this.state.availableTreeOptionsChecked.includes(treeItem.id);
        this.areAllDescendantsChecked = (treeItem, isChosen) => treeItem.children
            ? treeItem.children.every(child => this.areAllDescendantsChecked(child, isChosen))
            : this.isChecked(treeItem, isChosen);
        this.areSomeDescendantsChecked = (treeItem, isChosen) => treeItem.children
            ? treeItem.children.some(child => this.areSomeDescendantsChecked(child, isChosen))
            : this.isChecked(treeItem, isChosen);
        this.mapChecked = (item, isChosen) => {
            const hasCheck = this.areAllDescendantsChecked(item, isChosen);
            item.isChecked = false;
            if (hasCheck) {
                item.isChecked = true;
            }
            else {
                const hasPartialCheck = this.areSomeDescendantsChecked(item, isChosen);
                if (hasPartialCheck) {
                    item.isChecked = null;
                }
            }
            if (item.children) {
                return Object.assign(Object.assign({}, item), { children: item.children.map(child => this.mapChecked(child, isChosen)) });
            }
            return item;
        };
        this.onTreeOptionCheck = (evt, isChecked, itemData, isChosen) => {
            const { availableOptions, availableTreeFilteredOptions, chosenOptions, chosenTreeFilteredOptions } = this.state;
            let panelOptions;
            if (isChosen) {
                if (chosenTreeFilteredOptions) {
                    panelOptions = chosenOptions
                        .map(opt => Object.assign({}, opt))
                        .filter(item => filterTreeItemsWithoutFolders(item, chosenTreeFilteredOptions));
                }
                else {
                    panelOptions = chosenOptions;
                }
            }
            else {
                if (availableTreeFilteredOptions) {
                    panelOptions = availableOptions
                        .map(opt => Object.assign({}, opt))
                        .filter(item => filterTreeItemsWithoutFolders(item, availableTreeFilteredOptions));
                }
                else {
                    panelOptions = availableOptions;
                }
            }
            const checkedOptionTree = panelOptions
                .map(opt => Object.assign({}, opt))
                .filter(item => filterTreeItems(item, [itemData.id]));
            const flatTree = flattenTreeWithFolders(checkedOptionTree);
            const prevChecked = isChosen ? this.state.chosenTreeOptionsChecked : this.state.availableTreeOptionsChecked;
            let updatedChecked = [];
            if (isChecked) {
                updatedChecked = prevChecked.concat(flatTree.filter(id => !prevChecked.includes(id)));
            }
            else {
                updatedChecked = prevChecked.filter(id => !flatTree.includes(id));
            }
            this.setState(prevState => ({
                availableTreeOptionsChecked: isChosen ? prevState.availableTreeOptionsChecked : updatedChecked,
                chosenTreeOptionsChecked: isChosen ? updatedChecked : prevState.chosenTreeOptionsChecked
            }), () => {
                this.props.onOptionCheck && this.props.onOptionCheck(evt, isChecked, itemData.id, updatedChecked);
            });
        };
        this.state = {
            availableOptions: [...this.props.availableOptions],
            availableOptionsSelected: [],
            availableFilteredOptions: null,
            availableTreeFilteredOptions: null,
            chosenOptions: [...this.props.chosenOptions],
            chosenOptionsSelected: [],
            chosenFilteredOptions: null,
            chosenTreeFilteredOptions: null,
            availableTreeOptionsChecked: [],
            chosenTreeOptionsChecked: []
        };
    }
    // If the DualListSelector uses trees, concat the two initial arrays and merge duplicate folder IDs
    createMergedCopy() {
        const copyOfAvailable = JSON.parse(JSON.stringify(this.props.availableOptions));
        const copyOfChosen = JSON.parse(JSON.stringify(this.props.chosenOptions));
        return this.props.isTree
            ? Object.values(copyOfAvailable
                .concat(copyOfChosen)
                .reduce((mapObj, item) => {
                const key = item.id;
                if (mapObj[key]) {
                    // If map already has an item ID, add the dupe ID's children to the existing map
                    mapObj[key].children.push(...item.children);
                }
                else {
                    // Else clone the item data
                    mapObj[key] = Object.assign({}, item);
                }
                return mapObj;
            }, {}))
            : null;
    }
    componentDidUpdate() {
        if (JSON.stringify(this.props.availableOptions, this.replacer) !==
            JSON.stringify(this.state.availableOptions, this.replacer) ||
            JSON.stringify(this.props.chosenOptions, this.replacer) !==
                JSON.stringify(this.state.chosenOptions, this.replacer)) {
            this.setState({
                availableOptions: [...this.props.availableOptions],
                chosenOptions: [...this.props.chosenOptions]
            });
        }
    }
    render() {
        const _a = this.props, { availableOptionsTitle, availableOptionsActions, availableOptionsSearchAriaLabel, className, children, chosenOptionsTitle, chosenOptionsActions, chosenOptionsSearchAriaLabel, filterOption, isSearchable, chosenOptionsStatus, availableOptionsStatus, controlsAriaLabel, addAllAriaLabel, addSelectedAriaLabel, removeSelectedAriaLabel, removeAllAriaLabel, 
        /* eslint-disable @typescript-eslint/no-unused-vars */
        availableOptions: consumerPassedAvailableOptions, chosenOptions: consumerPassedChosenOptions, removeSelected, addAll, removeAll, addSelected, onListChange, onAvailableOptionsSearchInputChanged, onChosenOptionsSearchInputChanged, onOptionSelect, onOptionCheck, id, isTree, isDisabled, addAllTooltip, addAllTooltipProps, addSelectedTooltip, addSelectedTooltipProps, removeAllTooltip, removeAllTooltipProps, removeSelectedTooltip, removeSelectedTooltipProps } = _a, props = __rest(_a, ["availableOptionsTitle", "availableOptionsActions", "availableOptionsSearchAriaLabel", "className", "children", "chosenOptionsTitle", "chosenOptionsActions", "chosenOptionsSearchAriaLabel", "filterOption", "isSearchable", "chosenOptionsStatus", "availableOptionsStatus", "controlsAriaLabel", "addAllAriaLabel", "addSelectedAriaLabel", "removeSelectedAriaLabel", "removeAllAriaLabel", "availableOptions", "chosenOptions", "removeSelected", "addAll", "removeAll", "addSelected", "onListChange", "onAvailableOptionsSearchInputChanged", "onChosenOptionsSearchInputChanged", "onOptionSelect", "onOptionCheck", "id", "isTree", "isDisabled", "addAllTooltip", "addAllTooltipProps", "addSelectedTooltip", "addSelectedTooltipProps", "removeAllTooltip", "removeAllTooltipProps", "removeSelectedTooltip", "removeSelectedTooltipProps"]);
        const { availableOptions, chosenOptions, chosenOptionsSelected, availableOptionsSelected, chosenTreeOptionsChecked, availableTreeOptionsChecked } = this.state;
        const availableOptionsStatusToDisplay = availableOptionsStatus ||
            (isTree
                ? `${filterFolders(availableOptions, availableTreeOptionsChecked)
                    .length} of ${flattenTree(availableOptions).length} items selected`
                : `${availableOptionsSelected.length} of ${availableOptions.length} items selected`);
        const chosenOptionsStatusToDisplay = chosenOptionsStatus ||
            (isTree
                ? `${filterFolders(chosenOptions, chosenTreeOptionsChecked).length} of ${flattenTree(chosenOptions).length} items selected`
                : `${chosenOptionsSelected.length} of ${chosenOptions.length} items selected`);
        const available = (isTree
            ? availableOptions.map(item => this.mapChecked(item, false))
            : availableOptions);
        const chosen = (isTree
            ? chosenOptions.map(item => this.mapChecked(item, true))
            : chosenOptions);
        return (React.createElement(DualListSelectorContext.Provider, { value: { isTree } },
            React.createElement("div", Object.assign({ className: css(styles.dualListSelector, className), id: id }, props), children === '' ? (React.createElement(React.Fragment, null,
                React.createElement(DualListSelectorPane, { isSearchable: isSearchable, onFilterUpdate: this.onFilterUpdate, searchInputAriaLabel: availableOptionsSearchAriaLabel, filterOption: filterOption, onSearchInputChanged: onAvailableOptionsSearchInputChanged, status: availableOptionsStatusToDisplay, title: availableOptionsTitle, options: available, selectedOptions: isTree ? availableTreeOptionsChecked : availableOptionsSelected, onOptionSelect: this.onOptionSelect, onOptionCheck: (e, isChecked, itemData) => this.onTreeOptionCheck(e, isChecked, itemData, false), actions: availableOptionsActions, id: `${id}-available-pane`, isDisabled: isDisabled }),
                React.createElement(DualListSelectorControlsWrapper, { "aria-label": controlsAriaLabel },
                    React.createElement(DualListSelectorControl, { isDisabled: (isTree ? availableTreeOptionsChecked.length === 0 : availableOptionsSelected.length === 0) ||
                            isDisabled, onClick: isTree ? this.addTreeSelected : this.addSelected, ref: this.addSelectedButtonRef, "aria-label": addSelectedAriaLabel, tooltipContent: addSelectedTooltip, tooltipProps: addSelectedTooltipProps },
                        React.createElement(AngleRightIcon, null)),
                    React.createElement(DualListSelectorControl, { isDisabled: availableOptions.length === 0 || isDisabled, onClick: isTree ? this.addAllTreeVisible : this.addAllVisible, ref: this.addAllButtonRef, "aria-label": addAllAriaLabel, tooltipContent: addAllTooltip, tooltipProps: addAllTooltipProps },
                        React.createElement(AngleDoubleRightIcon, null)),
                    React.createElement(DualListSelectorControl, { isDisabled: chosenOptions.length === 0 || isDisabled, onClick: isTree ? this.removeAllTreeVisible : this.removeAllVisible, "aria-label": removeAllAriaLabel, ref: this.removeAllButtonRef, tooltipContent: removeAllTooltip, tooltipProps: removeAllTooltipProps },
                        React.createElement(AngleDoubleLeftIcon, null)),
                    React.createElement(DualListSelectorControl, { onClick: isTree ? this.removeTreeSelected : this.removeSelected, isDisabled: (isTree ? chosenTreeOptionsChecked.length === 0 : chosenOptionsSelected.length === 0) || isDisabled, ref: this.removeSelectedButtonRef, "aria-label": removeSelectedAriaLabel, tooltipContent: removeSelectedTooltip, tooltipProps: removeSelectedTooltipProps },
                        React.createElement(AngleLeftIcon, null))),
                React.createElement(DualListSelectorPane, { isChosen: true, isSearchable: isSearchable, onFilterUpdate: this.onFilterUpdate, searchInputAriaLabel: chosenOptionsSearchAriaLabel, filterOption: filterOption, onSearchInputChanged: onChosenOptionsSearchInputChanged, title: chosenOptionsTitle, status: chosenOptionsStatusToDisplay, options: chosen, selectedOptions: isTree ? chosenTreeOptionsChecked : chosenOptionsSelected, onOptionSelect: this.onOptionSelect, onOptionCheck: (e, isChecked, itemData) => this.onTreeOptionCheck(e, isChecked, itemData, true), actions: chosenOptionsActions, id: `${id}-chosen-pane`, isDisabled: isDisabled }))) : (children))));
    }
}
DualListSelector.displayName = 'DualListSelector';
DualListSelector.defaultProps = {
    children: '',
    availableOptions: [],
    availableOptionsTitle: 'Available options',
    availableOptionsSearchAriaLabel: 'Available search input',
    chosenOptions: [],
    chosenOptionsTitle: 'Chosen options',
    chosenOptionsSearchAriaLabel: 'Chosen search input',
    id: getUniqueId('dual-list-selector'),
    controlsAriaLabel: 'Selector controls',
    addAllAriaLabel: 'Add all',
    addSelectedAriaLabel: 'Add selected',
    removeSelectedAriaLabel: 'Remove selected',
    removeAllAriaLabel: 'Remove all',
    isTree: false,
    isDisabled: false
};
//# sourceMappingURL=DualListSelector.js.map