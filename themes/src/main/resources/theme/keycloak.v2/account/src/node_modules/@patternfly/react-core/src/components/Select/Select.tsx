import * as React from 'react';
import styles from '@patternfly/react-styles/css/components/Select/select';
import badgeStyles from '@patternfly/react-styles/css/components/Badge/badge';
import formStyles from '@patternfly/react-styles/css/components/FormControl/form-control';
import buttonStyles from '@patternfly/react-styles/css/components/Button/button';
import { css } from '@patternfly/react-styles';
import TimesCircleIcon from '@patternfly/react-icons/dist/esm/icons/times-circle-icon';
import CheckCircleIcon from '@patternfly/react-icons/dist/esm/icons/check-circle-icon';
import ExclamationTriangleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-triangle-icon';
import ExclamationCircleIcon from '@patternfly/react-icons/dist/esm/icons/exclamation-circle-icon';
import { SelectMenu } from './SelectMenu';
import { SelectOption, SelectOptionObject } from './SelectOption';
import { SelectGroup, SelectGroupProps } from './SelectGroup';
import { SelectToggle } from './SelectToggle';
import {
  SelectContext,
  SelectVariant,
  SelectPosition,
  SelectDirection,
  SelectFooterTabbableItems
} from './selectConstants';
import { ChipGroup, ChipGroupProps } from '../ChipGroup';
import { Chip } from '../Chip';
import { Spinner } from '../Spinner';
import {
  keyHandler,
  getNextIndex,
  getOUIAProps,
  OUIAProps,
  getDefaultOUIAId,
  PickOptional,
  GenerateId
} from '../../helpers';
import { KeyTypes } from '../../helpers/constants';
import { Divider } from '../Divider';
import { ToggleMenuBaseProps, Popper } from '../../helpers/Popper/Popper';
import { createRenderableFavorites, extendItemsWithFavorite } from '../../helpers/favorites';
import { ValidatedOptions } from '../../helpers/constants';
import { findTabbableElements } from '../../helpers/util';

// seed for the aria-labelledby ID
let currentId = 0;

export interface SelectViewMoreObject {
  /** View more text */
  text: string;
  /** Callback for when the view more button is clicked */
  onClick: (event: React.MouseEvent | React.ChangeEvent) => void;
}
export interface SelectProps
  extends Omit<ToggleMenuBaseProps, 'menuAppendTo'>,
    Omit<React.HTMLProps<HTMLDivElement>, 'onSelect' | 'ref' | 'checked' | 'selected'>,
    OUIAProps {
  /** Content rendered inside the Select. Must be React.ReactElement<SelectGroupProps>[] */
  children?: React.ReactElement[];
  /** Classes applied to the root of the Select */
  className?: string;
  /** Indicates where menu will be aligned horizontally */
  position?: SelectPosition | 'right' | 'left';
  /** Flag specifying which direction the Select menu expands */
  direction?: 'up' | 'down';
  /** Flag to indicate if select is open */
  isOpen?: boolean;
  /** Flag to indicate if select options are grouped */
  isGrouped?: boolean;
  /** Display the toggle with no border or background */
  isPlain?: boolean;
  /** Flag to indicate if select is disabled */
  isDisabled?: boolean;
  /** Flag to indicate if the typeahead select allows new items */
  isCreatable?: boolean;
  /** Flag indicating if placeholder styles should be applied */
  hasPlaceholderStyle?: boolean;
  /** @beta Flag indicating if the creatable option should set its value as a SelectOptionObject */
  isCreateSelectOptionObject?: boolean;
  /** Value to indicate if the select is modified to show that validation state.
   * If set to success, select will be modified to indicate valid state.
   * If set to error, select will be modified to indicate error state.
   * If set to warning, select will be modified to indicate warning state.
   */
  validated?: 'success' | 'warning' | 'error' | 'default';
  /** @beta Loading variant to display either the spinner or the view more text button */
  loadingVariant?: 'spinner' | SelectViewMoreObject;
  /** Text displayed in typeahead select to prompt the user to create an item */
  createText?: string;
  /** Title text of Select */
  placeholderText?: string | React.ReactNode;
  /** Text to display in typeahead select when no results are found */
  noResultsFoundText?: string;
  /** Array of selected items for multi select variants. */
  selections?: string | SelectOptionObject | (string | SelectOptionObject)[];
  /** Flag indicating if selection badge should be hidden for checkbox variant,default false */
  isCheckboxSelectionBadgeHidden?: boolean;
  /** Id for select toggle element */
  toggleId?: string;
  /** Adds accessible text to Select */
  'aria-label'?: string;
  /** Id of label for the Select aria-labelledby */
  'aria-labelledby'?: string;
  /** Id of div for the select aria-labelledby */
  'aria-describedby'?: string;
  /** Flag indicating if the select is an invalid state */
  'aria-invalid'?: boolean;
  /** Label for input field of type ahead select variants */
  typeAheadAriaLabel?: string;
  /** Id of div for input field of type ahead select variants */
  typeAheadAriaDescribedby?: string;
  /** Label for clear selection button of type ahead select variants */
  clearSelectionsAriaLabel?: string;
  /** Label for toggle of type ahead select variants */
  toggleAriaLabel?: string;
  /** Label for remove chip button of multiple type ahead select variant */
  removeSelectionAriaLabel?: string;
  /** ID list of favorited select items */
  favorites?: string[];
  /** Label for the favorites group */
  favoritesLabel?: string;
  /** Enables favorites. Callback called when a select options's favorite button is clicked */
  onFavorite?: (itemId: string, isFavorite: boolean) => void;
  /** Callback for selection behavior */
  onSelect?: (
    event: React.MouseEvent | React.ChangeEvent,
    value: string | SelectOptionObject,
    isPlaceholder?: boolean
  ) => void;
  /** Callback for toggle button behavior */
  onToggle: (isExpanded: boolean, event: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent | Event) => void;
  /** Callback for toggle blur */
  onBlur?: (event?: any) => void;
  /** Callback for typeahead clear button */
  onClear?: (event: React.MouseEvent) => void;
  /** Optional callback for custom filtering */
  onFilter?: (e: React.ChangeEvent<HTMLInputElement> | null, value: string) => React.ReactElement[] | undefined;
  /** Optional callback for newly created options */
  onCreateOption?: (newOptionValue: string) => void;
  /** Optional event handler called each time the value in the typeahead input changes. */
  onTypeaheadInputChanged?: (value: string) => void;
  /** Variant of rendered Select */
  variant?: 'single' | 'checkbox' | 'typeahead' | 'typeaheadmulti';
  /** Width of the select container as a number of px or string percentage */
  width?: string | number;
  /** Max height of the select container as a number of px or string percentage */
  maxHeight?: string | number;
  /** Icon element to render inside the select toggle */
  toggleIcon?: React.ReactElement;
  /** Custom content to render in the select menu.  If this prop is defined, the variant prop will be ignored and the select will render with a single select toggle */
  customContent?: React.ReactNode;
  /** Flag indicating if select should have an inline text input for filtering */
  hasInlineFilter?: boolean;
  /** Placeholder text for inline filter */
  inlineFilterPlaceholderText?: string;
  /** Custom text for select badge */
  customBadgeText?: string | number;
  /** Prefix for the id of the input in the checkbox select variant*/
  inputIdPrefix?: string;
  /** Value for the typeahead and inline filtering input autocomplete attribute. When targeting Chrome this property should be a random string. */
  inputAutoComplete?: string;
  /** Optional props to pass to the chip group in the typeaheadmulti variant */
  chipGroupProps?: Omit<ChipGroupProps, 'children' | 'ref'>;
  /** Optional props to render custom chip group in the typeaheadmulti variant */
  chipGroupComponent?: React.ReactNode;
  /** Flag for retaining keyboard-entered value in typeahead text field when focus leaves input away */
  isInputValuePersisted?: boolean;
  /** @beta Flag for retaining filter results on blur from keyboard-entered typeahead text */
  isInputFilterPersisted?: boolean;
  /** Flag indicating the typeahead input value should reset upon selection */
  shouldResetOnSelect?: boolean;
  /** Content rendered in the footer of the select menu */
  footer?: React.ReactNode;
  /** The container to append the menu to. Defaults to 'inline'.
   * If your menu is being cut off you can append it to an element higher up the DOM tree.
   * Some examples:
   * menuAppendTo="parent"
   * menuAppendTo={() => document.body}
   * menuAppendTo={document.getElementById('target')}
   */
  menuAppendTo?: HTMLElement | (() => HTMLElement) | 'inline' | 'parent';
  /** Flag for indicating that the select menu should automatically flip vertically when
   * it reaches the boundary. This prop can only be used when the select component is not
   * appended inline, e.g. `menuAppendTo="parent"`
   */
  isFlipEnabled?: boolean;
}

export interface SelectState {
  focusFirstOption: boolean;
  typeaheadInputValue: string | null;
  typeaheadFilteredChildren: React.ReactNode[];
  favoritesGroup: React.ReactNode[];
  typeaheadCurrIndex: number;
  creatableValue: string;
  tabbedIntoFavoritesMenu: boolean;
  typeaheadStoredIndex: number;
  ouiaStateId: string;
  viewMoreNextIndex: number;
}

export class Select extends React.Component<SelectProps & OUIAProps, SelectState> {
  static displayName = 'Select';
  private parentRef = React.createRef<HTMLDivElement>();
  private menuComponentRef = React.createRef<HTMLElement>();
  private filterRef = React.createRef<HTMLInputElement>();
  private clearRef = React.createRef<HTMLButtonElement>();
  private inputRef = React.createRef<HTMLInputElement>();
  private refCollection: HTMLElement[][] = [[]];
  private optionContainerRefCollection: HTMLElement[] = [];
  private footerRef = React.createRef<HTMLDivElement>();

  static defaultProps: PickOptional<SelectProps> = {
    children: [] as React.ReactElement[],
    className: '',
    position: SelectPosition.left,
    direction: SelectDirection.down,
    toggleId: null as string,
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
    variant: SelectVariant.single,
    width: '',
    onClear: () => undefined as void,
    onCreateOption: () => undefined as void,
    toggleIcon: null as React.ReactElement,
    onFilter: null,
    onTypeaheadInputChanged: null,
    customContent: null,
    hasInlineFilter: false,
    inlineFilterPlaceholderText: null,
    customBadgeText: null,
    inputIdPrefix: '',
    inputAutoComplete: 'off',
    menuAppendTo: 'inline',
    favorites: [] as string[],
    favoritesLabel: 'Favorites',
    ouiaSafe: true,
    chipGroupComponent: null,
    isInputValuePersisted: false,
    isInputFilterPersisted: false,
    isCreateSelectOptionObject: false,
    shouldResetOnSelect: true,
    isFlipEnabled: false
  };

  state: SelectState = {
    focusFirstOption: false,
    typeaheadInputValue: null,
    typeaheadFilteredChildren: React.Children.toArray(this.props.children),
    favoritesGroup: [] as React.ReactNode[],
    typeaheadCurrIndex: -1,
    typeaheadStoredIndex: -1,
    creatableValue: '',
    tabbedIntoFavoritesMenu: false,
    ouiaStateId: getDefaultOUIAId(Select.displayName, this.props.variant),
    viewMoreNextIndex: -1
  };

  getTypeaheadActiveChild = (typeaheadCurrIndex: number) =>
    this.refCollection[typeaheadCurrIndex] ? this.refCollection[typeaheadCurrIndex][0] : null;

  componentDidUpdate = (prevProps: SelectProps, prevState: SelectState) => {
    if (this.props.hasInlineFilter) {
      this.refCollection[0][0] = this.filterRef.current;
    }

    // Move focus to top of the menu if state.focusFirstOption was updated to true and the menu does not have custom content
    if (!prevState.focusFirstOption && this.state.focusFirstOption && !this.props.customContent) {
      const firstRef = this.refCollection.find(ref => ref !== null);
      if (firstRef && firstRef[0]) {
        firstRef[0].focus();
      }
    } else if (
      // if viewMoreNextIndex is not -1, view more was clicked, set focus on first newly loaded item
      this.state.viewMoreNextIndex !== -1 &&
      this.refCollection.length > this.state.viewMoreNextIndex &&
      this.props.loadingVariant !== 'spinner' &&
      this.refCollection[this.state.viewMoreNextIndex][0] &&
      this.props.variant !== 'typeahead' && // do not hard focus newly added items for typeahead variants
      this.props.variant !== 'typeaheadmulti'
    ) {
      this.refCollection[this.state.viewMoreNextIndex][0].focus();
      this.setState({ viewMoreNextIndex: -1 });
    }

    if (this.props.variant === 'typeahead' || this.props.variant === 'typeaheadmulti') {
      const checkUpdatedChildren = (prevChildren: React.ReactElement[], currChildren: React.ReactElement[]) =>
        Array.from(prevChildren).some((prevChild: React.ReactElement, index: number) => {
          const prevChildProps = prevChild.props;
          const currChild = currChildren[index];
          const { props: currChildProps } = currChild;

          if (prevChildProps && currChildProps) {
            return (
              prevChildProps.value !== currChildProps.value ||
              prevChildProps.label !== currChildProps.label ||
              prevChildProps.isDisabled !== currChildProps.isDisabled ||
              prevChildProps.isPlaceholder !== currChildProps.isPlaceholder
            );
          } else {
            return prevChild !== currChild;
          }
        });

      const hasUpdatedChildren =
        prevProps.children.length !== this.props.children.length ||
        checkUpdatedChildren(prevProps.children, this.props.children) ||
        (this.props.isGrouped &&
          Array.from(prevProps.children).some(
            (prevChild: React.ReactElement, index: number) =>
              prevChild.type === SelectGroup &&
              prevChild.props.children &&
              this.props.children[index].props.children &&
              (prevChild.props.children.length !== this.props.children[index].props.children.length ||
                checkUpdatedChildren(prevChild.props.children, this.props.children[index].props.children))
          ));

      if (hasUpdatedChildren) {
        this.updateTypeAheadFilteredChildren(prevState.typeaheadInputValue || '', null);
      }
    }

    // for menus with favorites,
    // if the number of favorites or typeahead filtered children has changed, the generated
    // list of favorites needs to be updated
    if (
      this.props.onFavorite &&
      (this.props.favorites.length !== prevProps.favorites.length ||
        this.state.typeaheadFilteredChildren !== prevState.typeaheadFilteredChildren)
    ) {
      const tempRenderableChildren =
        this.props.variant === 'typeahead' || this.props.variant === 'typeaheadmulti'
          ? this.state.typeaheadFilteredChildren
          : this.props.children;
      const renderableFavorites = createRenderableFavorites(
        tempRenderableChildren,
        this.props.isGrouped,
        this.props.favorites
      );
      const favoritesGroup = renderableFavorites.length
        ? [
            <SelectGroup key="favorites" label={this.props.favoritesLabel}>
              {renderableFavorites}
            </SelectGroup>,
            <Divider key="favorites-group-divider" />
          ]
        : [];
      this.setState({ favoritesGroup });
    }
  };

  onEnter = () => {
    this.setState({ focusFirstOption: true });
  };

  onToggle = (isExpanded: boolean, e: React.MouseEvent | React.ChangeEvent | React.KeyboardEvent | Event) => {
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

  onClose = () => {
    const { isInputFilterPersisted } = this.props;

    this.setState({
      focusFirstOption: false,
      typeaheadInputValue: null,
      ...(!isInputFilterPersisted && {
        typeaheadFilteredChildren: React.Children.toArray(this.props.children)
      }),
      typeaheadCurrIndex: -1,
      tabbedIntoFavoritesMenu: false,
      viewMoreNextIndex: -1
    });
  };

  onChange = (e: React.ChangeEvent<HTMLInputElement>) => {
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

  updateTypeAheadFilteredChildren = (typeaheadInputValue: string, e: React.ChangeEvent<HTMLInputElement> | null) => {
    let typeaheadFilteredChildren: any;

    const {
      onFilter,
      isCreatable,
      onCreateOption,
      createText,
      noResultsFoundText,
      children,
      isGrouped,
      isCreateSelectOptionObject,
      loadingVariant
    } = this.props;

    if (onFilter) {
      /* The updateTypeAheadFilteredChildren callback is not only called on input changes but also when the children change.
       * In this case the e is null but we can get the typeaheadInputValue from the state.
       */
      typeaheadFilteredChildren = onFilter(e, e ? e.target.value : typeaheadInputValue) || children;
    } else {
      let input: RegExp;
      try {
        input = new RegExp(typeaheadInputValue.toString(), 'i');
      } catch (err) {
        input = new RegExp(typeaheadInputValue.toString().replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i');
      }
      const childrenArray = React.Children.toArray(children) as React.ReactElement<SelectGroupProps>[];
      if (isGrouped) {
        const childFilter = (child: React.ReactElement<SelectGroupProps>) =>
          child.props.value &&
          child.props.value.toString &&
          this.getDisplay(child.props.value.toString(), 'text').search(input) === 0;
        typeaheadFilteredChildren =
          typeaheadInputValue.toString() !== ''
            ? React.Children.map(children, group => {
                if (
                  React.isValidElement<React.ComponentProps<typeof SelectGroup>>(group) &&
                  group.type === SelectGroup
                ) {
                  const filteredGroupChildren = (React.Children.toArray(group.props.children) as React.ReactElement<
                    SelectGroupProps
                  >[]).filter(childFilter);
                  if (filteredGroupChildren.length > 0) {
                    return React.cloneElement(group, {
                      titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
                      children: filteredGroupChildren as any
                    });
                  }
                } else {
                  return (React.Children.toArray(group) as React.ReactElement<SelectGroupProps>[]).filter(childFilter);
                }
              })
            : childrenArray;
      } else {
        typeaheadFilteredChildren =
          typeaheadInputValue.toString() !== ''
            ? childrenArray.filter(child => {
                const valueToCheck = child.props.value;
                // Dividers don't have value and should not be filtered
                if (!valueToCheck) {
                  return true;
                }

                const isSelectOptionObject =
                  typeof valueToCheck !== 'string' &&
                  (valueToCheck as SelectOptionObject).toString &&
                  (valueToCheck as SelectOptionObject).compareTo;

                // View more option should be returned as not a match
                if (loadingVariant !== 'spinner' && loadingVariant?.text === valueToCheck) {
                  return true;
                }

                // spinner should be returned as not a match
                if (loadingVariant === 'spinner' && valueToCheck === 'loading') {
                  return true;
                }

                if (isSelectOptionObject) {
                  return (valueToCheck as SelectOptionObject).compareTo(typeaheadInputValue);
                } else {
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
        typeaheadFilteredChildren.push(
          <SelectOption isDisabled key="no-results" value={noResultsFoundText} isNoResultsOption />
        );
    }
    if (isCreatable && typeaheadInputValue !== '') {
      const newValue = typeaheadInputValue;
      if (
        !typeaheadFilteredChildren.find(
          (i: React.ReactElement) =>
            i.props.value && i.props.value.toString().toLowerCase() === newValue.toString().toLowerCase()
        )
      ) {
        const newOptionValue = isCreateSelectOptionObject
          ? ({
              toString: () => newValue,
              compareTo: value =>
                this.toString()
                  .toLowerCase()
                  .includes(value.toString().toLowerCase())
            } as SelectOptionObject)
          : newValue;

        typeaheadFilteredChildren.push(
          <SelectOption
            key={`create ${newValue}`}
            value={newOptionValue}
            onClick={() => onCreateOption && onCreateOption(newValue)}
          >
            {createText} "{newValue}"
          </SelectOption>
        );
      }
    }

    this.setState({
      typeaheadFilteredChildren
    });
  };

  onClick = (e: React.MouseEvent) => {
    if (!this.props.isOpen) {
      this.onToggle(true, e);
    }
  };

  clearSelection = (_e: React.MouseEvent) => {
    this.setState({
      typeaheadInputValue: null,
      typeaheadFilteredChildren: React.Children.toArray(this.props.children),
      typeaheadCurrIndex: -1
    });
  };

  extendTypeaheadChildren(typeaheadCurrIndex: number, favoritesGroup?: React.ReactNode[]) {
    const { isGrouped, onFavorite } = this.props;
    const typeaheadChildren = favoritesGroup
      ? favoritesGroup.concat(this.state.typeaheadFilteredChildren)
      : this.state.typeaheadFilteredChildren;
    const activeElement = this.optionContainerRefCollection[typeaheadCurrIndex];

    let typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
    if (typeaheadActiveChild && typeaheadActiveChild.classList.contains('pf-m-description')) {
      typeaheadActiveChild = typeaheadActiveChild.firstElementChild as HTMLElement;
    }

    this.refCollection = [[]];
    this.optionContainerRefCollection = [];
    if (isGrouped) {
      return React.Children.map(typeaheadChildren as React.ReactElement[], (group: React.ReactElement) => {
        if (group.type === Divider) {
          return group;
        } else if (group.type === SelectGroup && onFavorite) {
          return React.cloneElement(group, {
            titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
            children: React.Children.map(group.props.children, (child: React.ReactElement) =>
              child.type === Divider
                ? child
                : React.cloneElement(child as React.ReactElement, {
                    isFocused:
                      activeElement &&
                      (activeElement.id === (child as React.ReactElement).props.id ||
                        (this.props.isCreatable &&
                          typeaheadActiveChild.innerText ===
                            `{createText} "${(group as React.ReactElement).props.value}"`))
                  })
            )
          });
        } else if (group.type === SelectGroup) {
          return React.cloneElement(group, {
            titleId: group.props.label && group.props.label.replace(/\W/g, '-'),
            children: React.Children.map(group.props.children, (child: React.ReactElement) =>
              child.type === Divider
                ? child
                : React.cloneElement(child as React.ReactElement, {
                    isFocused:
                      typeaheadActiveChild &&
                      (typeaheadActiveChild.innerText === (child as React.ReactElement).props.value.toString() ||
                        (this.props.isCreatable &&
                          typeaheadActiveChild.innerText ===
                            `{createText} "${(child as React.ReactElement).props.value}"`))
                  })
            )
          });
        } else {
          // group has been filtered down to SelectOption
          return React.cloneElement(group as React.ReactElement, {
            isFocused:
              typeaheadActiveChild &&
              (typeaheadActiveChild.innerText === group.props.value.toString() ||
                (this.props.isCreatable && typeaheadActiveChild.innerText === `{createText} "${group.props.value}"`))
          });
        }
      });
    }
    return typeaheadChildren.map((child: React.ReactNode, index) => {
      const childElement = child as any;
      return childElement.type.displayName === 'Divider'
        ? child
        : React.cloneElement(child as React.ReactElement, {
            isFocused: typeaheadActiveChild
              ? typeaheadActiveChild.innerText === (child as React.ReactElement).props.value.toString() ||
                (this.props.isCreatable &&
                  typeaheadActiveChild.innerText === `{createText} "${(child as React.ReactElement).props.value}"`)
              : index === typeaheadCurrIndex // fallback for view more + typeahead use cases, when the new expanded list is loaded and refCollection hasn't be updated yet
          });
    });
  }

  sendRef = (
    optionRef: React.ReactNode,
    favoriteRef: React.ReactNode,
    optionContainerRef: React.ReactNode,
    index: number
  ) => {
    this.refCollection[index] = [(optionRef as unknown) as HTMLElement, (favoriteRef as unknown) as HTMLElement];
    this.optionContainerRefCollection[index] = (optionContainerRef as unknown) as HTMLElement;
  };

  handleMenuKeys = (index: number, innerIndex: number, position: string) => {
    keyHandler(index, innerIndex, position, this.refCollection, this.refCollection);
    if (this.props.variant === SelectVariant.typeahead || this.props.variant === SelectVariant.typeaheadMulti) {
      if (position !== 'tab') {
        this.handleTypeaheadKeys(position);
      }
    }
  };

  moveFocus = (nextIndex: number, updateCurrentIndex: boolean = true) => {
    const { isCreatable, createText } = this.props;

    const hasDescriptionElm = Boolean(
      this.refCollection[nextIndex][0] && this.refCollection[nextIndex][0].classList.contains('pf-m-description')
    );
    const isLoad = Boolean(
      this.refCollection[nextIndex][0] && this.refCollection[nextIndex][0].classList.contains('pf-m-load')
    );
    const optionTextElm = hasDescriptionElm
      ? (this.refCollection[nextIndex][0].firstElementChild as HTMLElement)
      : this.refCollection[nextIndex][0];

    let typeaheadInputValue = '';
    if (isCreatable && optionTextElm.innerText.includes(createText)) {
      typeaheadInputValue = this.state.creatableValue;
    } else if (optionTextElm && !isLoad) {
      // !isLoad prevents the view more button text from appearing the typeahead input
      typeaheadInputValue = optionTextElm.innerText;
    }
    this.setState(prevState => ({
      typeaheadCurrIndex: updateCurrentIndex ? nextIndex : prevState.typeaheadCurrIndex,
      typeaheadStoredIndex: nextIndex,
      typeaheadInputValue
    }));
  };

  switchFocusToFavoriteMenu = () => {
    const { typeaheadCurrIndex, typeaheadStoredIndex } = this.state;
    let indexForFocus = 0;

    if (typeaheadCurrIndex !== -1) {
      indexForFocus = typeaheadCurrIndex;
    } else if (typeaheadStoredIndex !== -1) {
      indexForFocus = typeaheadStoredIndex;
    }

    if (this.refCollection[indexForFocus] !== null && this.refCollection[indexForFocus][0] !== null) {
      this.refCollection[indexForFocus][0].focus();
    } else {
      this.clearRef.current.focus();
    }

    this.setState({
      tabbedIntoFavoritesMenu: true,
      typeaheadCurrIndex: -1
    });
  };

  moveFocusToLastMenuItem = () => {
    const refCollectionLen = this.refCollection.length;
    if (
      refCollectionLen > 0 &&
      this.refCollection[refCollectionLen - 1] !== null &&
      this.refCollection[refCollectionLen - 1][0] !== null
    ) {
      this.refCollection[refCollectionLen - 1][0].focus();
    }
  };

  handleTypeaheadKeys = (position: string, shiftKey: boolean = false) => {
    const { isOpen, onFavorite, isCreatable } = this.props;
    const { typeaheadCurrIndex, tabbedIntoFavoritesMenu } = this.state;
    const typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
    if (isOpen) {
      if (position === 'enter') {
        if (
          (typeaheadCurrIndex !== -1 || (isCreatable && this.refCollection.length === 1)) && // do not allow selection without moving to an initial option unless it is a single create option
          (typeaheadActiveChild || (this.refCollection[0] && this.refCollection[0][0]))
        ) {
          if (typeaheadActiveChild) {
            if (!typeaheadActiveChild.classList.contains('pf-m-load')) {
              const hasDescriptionElm = typeaheadActiveChild.childElementCount > 1;
              const typeaheadActiveChildText = hasDescriptionElm
                ? (typeaheadActiveChild.firstChild as HTMLElement).innerText
                : typeaheadActiveChild.innerText;
              this.setState({
                typeaheadInputValue: typeaheadActiveChildText
              });
            }
          } else if (this.refCollection[0] && this.refCollection[0][0]) {
            this.setState({
              typeaheadInputValue: this.refCollection[0][0].innerText
            });
          }
          if (typeaheadActiveChild) {
            typeaheadActiveChild.click();
          } else {
            this.refCollection[0][0].click();
          }
        }
      } else if (position === 'tab') {
        if (onFavorite) {
          // if the input has focus, tab to the first item or the last item that was previously focused.
          if (this.inputRef.current === document.activeElement) {
            // If shift is also clicked and there is a footer, tab to the last item in tabbable footer
            if (this.props.footer && shiftKey) {
              const tabbableItems = findTabbableElements(this.footerRef, SelectFooterTabbableItems);
              if (tabbableItems.length > 0) {
                if (tabbableItems[tabbableItems.length - 1]) {
                  tabbableItems[tabbableItems.length - 1].focus();
                }
              }
            } else {
              this.switchFocusToFavoriteMenu();
            }
          } else {
            // focus is on menu or footer
            if (this.props.footer) {
              let tabbedIntoMenu = false;
              const tabbableItems = findTabbableElements(this.footerRef, SelectFooterTabbableItems);
              if (tabbableItems.length > 0) {
                // if current element is not in footer, tab to first tabbable element in footer,
                // if shift was clicked, tab to input since focus is on menu
                const currentElementIndex = tabbableItems.findIndex((item: any) => item === document.activeElement);
                if (currentElementIndex === -1) {
                  if (shiftKey) {
                    // currently in menu, shift back to input
                    this.inputRef.current.focus();
                  } else {
                    // currently in menu, tab to first tabbable item in footer
                    tabbableItems[0].focus();
                  }
                } else {
                  // already in footer
                  if (shiftKey) {
                    // shift to previous item
                    if (currentElementIndex === 0) {
                      // on first footer item, shift back to menu
                      this.switchFocusToFavoriteMenu();
                      tabbedIntoMenu = true;
                    } else {
                      // shift to previous footer item
                      tabbableItems[currentElementIndex - 1].focus();
                    }
                  } else {
                    // tab to next tabbable item in footer or to input.
                    if (tabbableItems[currentElementIndex + 1]) {
                      tabbableItems[currentElementIndex + 1].focus();
                    } else {
                      this.inputRef.current.focus();
                    }
                  }
                }
              } else {
                // no tabbable items in footer, tab to input
                this.inputRef.current.focus();
                tabbedIntoMenu = false;
              }
              this.setState({ tabbedIntoFavoritesMenu: tabbedIntoMenu });
            } else {
              this.inputRef.current.focus();
              this.setState({ tabbedIntoFavoritesMenu: false });
            }
          }
        } else {
          // Close if there is no footer
          if (!this.props.footer) {
            this.onToggle(false, null);
            this.onClose();
          } else {
            // has footer
            const tabbableItems = findTabbableElements(this.footerRef, SelectFooterTabbableItems);
            const currentElementIndex = tabbableItems.findIndex((item: any) => item === document.activeElement);
            if (this.inputRef.current === document.activeElement) {
              if (shiftKey) {
                // close toggle if shift key and tab on input
                this.onToggle(false, null);
                this.onClose();
              } else {
                // tab to first tabbable item in footer
                if (tabbableItems[0]) {
                  tabbableItems[0].focus();
                } else {
                  this.onToggle(false, null);
                  this.onClose();
                }
              }
            } else {
              // focus is in footer
              if (shiftKey) {
                if (currentElementIndex === 0) {
                  // shift tab back to input
                  this.inputRef.current.focus();
                } else {
                  // shift to previous footer item
                  tabbableItems[currentElementIndex - 1].focus();
                }
              } else {
                // tab to next footer item or close tab if last item
                if (tabbableItems[currentElementIndex + 1]) {
                  tabbableItems[currentElementIndex + 1].focus();
                } else {
                  // no next item, close toggle
                  this.onToggle(false, null);
                  this.inputRef.current.focus();
                  this.onClose();
                }
              }
            }
          }
        }
      } else if (!tabbedIntoFavoritesMenu) {
        if (this.refCollection[0][0] === null) {
          return;
        }
        let nextIndex;
        if (typeaheadCurrIndex === -1 && position === 'down') {
          nextIndex = 0;
        } else if (typeaheadCurrIndex === -1 && position === 'up') {
          nextIndex = this.refCollection.length - 1;
        } else if (position !== 'left' && position !== 'right') {
          nextIndex = getNextIndex(typeaheadCurrIndex, position, this.refCollection);
        } else {
          nextIndex = typeaheadCurrIndex;
        }
        if (this.refCollection[nextIndex] === null) {
          return;
        }
        this.moveFocus(nextIndex);
      } else {
        const nextIndex = this.refCollection.findIndex(
          ref => ref !== undefined && (ref[0] === document.activeElement || ref[1] === document.activeElement)
        );
        this.moveFocus(nextIndex);
      }
    }
  };

  onClickTypeaheadToggleButton = () => {
    if (this.inputRef && this.inputRef.current) {
      this.inputRef.current.focus();
    }
  };

  getDisplay = (value: string | SelectOptionObject, type: 'node' | 'text' = 'node') => {
    if (!value) {
      return;
    }
    const item = this.props.isGrouped
      ? (React.Children.toArray(this.props.children) as React.ReactElement[])
          .reduce((acc, curr) => [...acc, ...React.Children.toArray(curr.props.children)], [])
          .find(child => child.props.value.toString() === value.toString())
      : React.Children.toArray(this.props.children).find(
          child =>
            (child as React.ReactElement).props.value &&
            (child as React.ReactElement).props.value.toString() === value.toString()
        );
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

  findText = (item: React.ReactNode) => {
    if (typeof item === 'string') {
      return item;
    } else if (!React.isValidElement(item)) {
      return '';
    } else {
      const multi: string[] = [];
      React.Children.toArray(item.props.children).forEach(child => multi.push(this.findText(child)));
      return multi.join('');
    }
  };

  generateSelectedBadge = () => {
    const { customBadgeText, selections } = this.props;
    if (customBadgeText !== null) {
      return customBadgeText;
    }
    if (Array.isArray(selections) && selections.length > 0) {
      return selections.length;
    }
    return null;
  };

  setVieMoreNextIndex = () => {
    this.setState({ viewMoreNextIndex: this.refCollection.length - 1 });
  };

  isLastOptionBeforeFooter = (index: any) =>
    this.props.footer && index === this.refCollection.length - 1 ? true : false;

  render() {
    const {
      children,
      chipGroupProps,
      chipGroupComponent,
      className,
      customContent,
      variant,
      direction,
      onSelect,
      onClear,
      onBlur,
      toggleId,
      isOpen,
      isGrouped,
      isPlain,
      isDisabled,
      hasPlaceholderStyle,
      validated,
      selections: selectionsProp,
      typeAheadAriaLabel,
      typeAheadAriaDescribedby,
      clearSelectionsAriaLabel,
      toggleAriaLabel,
      removeSelectionAriaLabel,
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      'aria-describedby': ariaDescribedby,
      'aria-invalid': ariaInvalid,
      placeholderText,
      width,
      maxHeight,
      toggleIcon,
      ouiaId,
      ouiaSafe,
      hasInlineFilter,
      isCheckboxSelectionBadgeHidden,
      inlineFilterPlaceholderText,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      onFilter,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      onTypeaheadInputChanged,
      onCreateOption,
      isCreatable,
      onToggle,
      createText,
      noResultsFoundText,
      customBadgeText,
      inputIdPrefix,
      inputAutoComplete,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      isInputValuePersisted,
      isInputFilterPersisted,
      /* eslint-enable @typescript-eslint/no-unused-vars */
      menuAppendTo,
      favorites,
      onFavorite,
      /* eslint-disable @typescript-eslint/no-unused-vars */
      favoritesLabel,
      footer,
      loadingVariant,
      isCreateSelectOptionObject,
      shouldResetOnSelect,
      isFlipEnabled,
      ...props
    } = this.props;
    const {
      focusFirstOption: openedOnEnter,
      typeaheadCurrIndex,
      typeaheadInputValue,
      typeaheadFilteredChildren,
      favoritesGroup
    } = this.state;
    const selectToggleId = toggleId || `pf-select-toggle-id-${currentId++}`;
    const selections = Array.isArray(selectionsProp) ? selectionsProp : [selectionsProp];
    // Find out if the selected option is a placeholder
    const selectedOption = React.Children.toArray(children).find(
      (option: any) => option.props.value === selections[0]
    ) as any;
    const isSelectedPlaceholder = selectedOption && selectedOption.props.isPlaceholder;
    const hasAnySelections = Boolean(selections[0] && selections[0] !== '');
    const typeaheadActiveChild = this.getTypeaheadActiveChild(typeaheadCurrIndex);
    let childPlaceholderText = null as string;

    // If onFavorites is set,  add isFavorite prop to children and add a Favorites group to the SelectMenu
    let renderableItems: React.ReactNode[] = [];
    if (onFavorite) {
      // if variant is type-ahead call the extendTypeaheadChildren before adding favorites
      let tempExtendedChildren: (React.ReactElement | React.ReactNode | {})[] = children;
      if (variant === 'typeahead' || variant === 'typeaheadmulti') {
        tempExtendedChildren = this.extendTypeaheadChildren(typeaheadCurrIndex, favoritesGroup);
      } else if (onFavorite) {
        tempExtendedChildren = favoritesGroup.concat(children);
      }
      // mark items that are favorited with isFavorite
      renderableItems = extendItemsWithFavorite(tempExtendedChildren, isGrouped, favorites);
    } else {
      renderableItems = children;
    }

    if (!customContent) {
      if (!hasAnySelections && !placeholderText) {
        const childPlaceholder = React.Children.toArray(children).filter(
          (child: React.ReactNode) => (child as React.ReactElement).props.isPlaceholder === true
        );
        childPlaceholderText =
          (childPlaceholder[0] && this.getDisplay((childPlaceholder[0] as React.ReactElement).props.value, 'node')) ||
          (children[0] && this.getDisplay(children[0].props.value, 'node'));
      }
    }

    if (isOpen) {
      if (renderableItems.find(item => (item as any)?.key === 'loading') === undefined) {
        if (loadingVariant === 'spinner') {
          renderableItems.push(
            <SelectOption isLoading key="loading" value="loading">
              <Spinner size="lg" />
            </SelectOption>
          );
        } else if (loadingVariant?.text) {
          renderableItems.push(
            <SelectOption
              isLoad
              key="loading"
              value={loadingVariant.text}
              setViewMoreNextIndex={this.setVieMoreNextIndex}
              onClick={loadingVariant?.onClick}
            />
          );
        }
      }
    }

    const hasOnClear = onClear !== Select.defaultProps.onClear;
    const clearBtn = (
      <button
        className={css(buttonStyles.button, buttonStyles.modifiers.plain, styles.selectToggleClear)}
        onClick={e => {
          this.clearSelection(e);
          onClear(e);
          e.stopPropagation();
        }}
        aria-label={clearSelectionsAriaLabel}
        type="button"
        disabled={isDisabled}
        ref={this.clearRef}
        onKeyDown={event => {
          if (event.key === KeyTypes.Enter) {
            this.clearRef.current.click();
          }
        }}
      >
        <TimesCircleIcon aria-hidden />
      </button>
    );

    let selectedChips = null as any;
    if (variant === SelectVariant.typeaheadMulti) {
      selectedChips = chipGroupComponent ? (
        chipGroupComponent
      ) : (
        <ChipGroup {...chipGroupProps}>
          {selections &&
            (selections as string[]).map(item => (
              <Chip
                key={item}
                onClick={(e: React.MouseEvent) => onSelect(e, item)}
                closeBtnAriaLabel={removeSelectionAriaLabel}
              >
                {this.getDisplay(item, 'node')}
              </Chip>
            ))}
        </ChipGroup>
      );
    }

    if (hasInlineFilter) {
      const filterBox = (
        <React.Fragment>
          <div key="inline-filter" className={css(styles.selectMenuSearch)}>
            <input
              key="inline-filter-input"
              type="search"
              className={css(formStyles.formControl, formStyles.modifiers.search)}
              onChange={this.onChange}
              placeholder={inlineFilterPlaceholderText}
              onKeyDown={event => {
                if (event.key === KeyTypes.ArrowUp) {
                  this.handleMenuKeys(0, 0, 'up');
                  event.preventDefault();
                } else if (event.key === KeyTypes.ArrowDown) {
                  this.handleMenuKeys(0, 0, 'down');
                  event.preventDefault();
                } else if (event.key === KeyTypes.ArrowLeft) {
                  this.handleMenuKeys(0, 0, 'left');
                  event.preventDefault();
                } else if (event.key === KeyTypes.ArrowRight) {
                  this.handleMenuKeys(0, 0, 'right');
                  event.preventDefault();
                } else if (event.key === KeyTypes.Tab && variant !== SelectVariant.checkbox && this.props.footer) {
                  // tab to footer or close menu if shift key
                  if (event.shiftKey) {
                    this.onToggle(false, event);
                  } else {
                    const tabbableItems = findTabbableElements(this.footerRef, SelectFooterTabbableItems);
                    if (tabbableItems.length > 0) {
                      tabbableItems[0].focus();
                      event.stopPropagation();
                      event.preventDefault();
                    } else {
                      this.onToggle(false, event);
                    }
                  }
                } else if (event.key === KeyTypes.Tab && variant === SelectVariant.checkbox) {
                  // More modal-like experience for checkboxes
                  // Let SelectOption handle this
                  if (event.shiftKey) {
                    this.handleMenuKeys(0, 0, 'up');
                  } else {
                    this.handleMenuKeys(0, 0, 'down');
                  }
                  event.stopPropagation();
                  event.preventDefault();
                }
              }}
              ref={this.filterRef}
              autoComplete={inputAutoComplete}
            />
          </div>
          <Divider key="inline-filter-divider" />
        </React.Fragment>
      );
      renderableItems = [filterBox, ...(typeaheadFilteredChildren as React.ReactElement[])].map((option, index) =>
        React.cloneElement(option, { key: index })
      );
    }

    let variantProps: any;
    let variantChildren: any;
    if (customContent) {
      variantProps = {
        selected: selections,
        openedOnEnter,
        isCustomContent: true
      };
      variantChildren = customContent;
    } else {
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
            variantChildren.push(<SelectOption isDisabled key={0} value={noResultsFoundText} isNoResultsOption />);
          }
          break;
        case 'typeaheadmulti':
          variantProps = {
            selected: selections,
            openedOnEnter
          };
          variantChildren = onFavorite ? renderableItems : this.extendTypeaheadChildren(typeaheadCurrIndex);
          if (variantChildren.length === 0) {
            variantChildren.push(<SelectOption isDisabled key={0} value={noResultsFoundText} isNoResultsOption />);
          }
          break;
      }
    }

    const innerMenu = (
      <SelectMenu
        // This removes the `position: absolute` styling from the `.pf-c-select__menu`
        // allowing the menu to flip correctly
        {...(isFlipEnabled && { style: { position: 'revert' } })}
        {...props}
        isGrouped={isGrouped}
        selected={selections}
        {...variantProps}
        openedOnEnter={openedOnEnter}
        aria-label={ariaLabel}
        aria-labelledby={ariaLabelledBy}
        sendRef={this.sendRef}
        keyHandler={this.handleMenuKeys}
        maxHeight={maxHeight}
        ref={this.menuComponentRef}
        footer={footer}
        footerRef={this.footerRef}
        isLastOptionBeforeFooter={this.isLastOptionBeforeFooter}
      >
        {variantChildren}
      </SelectMenu>
    );

    const menuContainer = footer ? <div className={css(styles.selectMenu)}> {innerMenu} </div> : innerMenu;

    const popperContainer = (
      <div
        className={css(
          styles.select,
          isOpen && styles.modifiers.expanded,
          validated === ValidatedOptions.success && styles.modifiers.success,
          validated === ValidatedOptions.warning && styles.modifiers.warning,
          validated === ValidatedOptions.error && styles.modifiers.invalid,
          direction === SelectDirection.up && styles.modifiers.top,
          className
        )}
        {...(width && { style: { width } })}
        {...(ariaDescribedby && { 'aria-describedby': ariaDescribedby })}
        {...(validated !== ValidatedOptions.default && { 'aria-invalid': ariaInvalid })}
      >
        {isOpen && menuContainer}
      </div>
    );

    const mainContainer = (
      <div
        className={css(
          styles.select,
          isOpen && styles.modifiers.expanded,
          validated === ValidatedOptions.success && styles.modifiers.success,
          validated === ValidatedOptions.warning && styles.modifiers.warning,
          validated === ValidatedOptions.error && styles.modifiers.invalid,
          direction === SelectDirection.up && styles.modifiers.top,
          className
        )}
        ref={this.parentRef}
        {...getOUIAProps(Select.displayName, ouiaId !== undefined ? ouiaId : this.state.ouiaStateId, ouiaSafe)}
        {...(width && { style: { width } })}
        {...(ariaDescribedby && { 'aria-describedby': ariaDescribedby })}
        {...(validated !== ValidatedOptions.default && { 'aria-invalid': ariaInvalid })}
      >
        <SelectToggle
          id={selectToggleId}
          parentRef={this.parentRef}
          menuRef={this.menuComponentRef}
          {...(footer && { footerRef: this.footerRef })}
          isOpen={isOpen}
          isPlain={isPlain}
          hasPlaceholderStyle={
            hasPlaceholderStyle && (!selections.length || selections[0] === null || isSelectedPlaceholder)
          }
          onToggle={this.onToggle}
          onEnter={this.onEnter}
          onClose={this.onClose}
          onBlur={onBlur}
          variant={variant}
          aria-labelledby={`${ariaLabelledBy || ''} ${selectToggleId}`}
          aria-label={toggleAriaLabel}
          handleTypeaheadKeys={this.handleTypeaheadKeys}
          moveFocusToLastMenuItem={this.moveFocusToLastMenuItem}
          isDisabled={isDisabled}
          hasClearButton={hasOnClear}
          hasFooter={footer !== undefined}
          onClickTypeaheadToggleButton={this.onClickTypeaheadToggleButton}
        >
          {customContent && (
            <div className={css(styles.selectToggleWrapper)}>
              {toggleIcon && <span className={css(styles.selectToggleIcon)}>{toggleIcon}</span>}
              <span className={css(styles.selectToggleText)}>{placeholderText}</span>
            </div>
          )}
          {variant === SelectVariant.single && !customContent && (
            <React.Fragment>
              <div className={css(styles.selectToggleWrapper)}>
                {toggleIcon && <span className={css(styles.selectToggleIcon)}>{toggleIcon}</span>}
                <span className={css(styles.selectToggleText)}>
                  {this.getDisplay(selections[0] as string, 'node') || placeholderText || childPlaceholderText}
                </span>
              </div>
              {hasOnClear && hasAnySelections && clearBtn}
            </React.Fragment>
          )}
          {variant === SelectVariant.checkbox && !customContent && (
            <React.Fragment>
              <div className={css(styles.selectToggleWrapper)}>
                {toggleIcon && <span className={css(styles.selectToggleIcon)}>{toggleIcon}</span>}
                <span className={css(styles.selectToggleText)}>{placeholderText}</span>
                {!isCheckboxSelectionBadgeHidden && hasAnySelections && (
                  <div className={css(styles.selectToggleBadge)}>
                    <span className={css(badgeStyles.badge, badgeStyles.modifiers.read)}>
                      {this.generateSelectedBadge()}
                    </span>
                  </div>
                )}
              </div>
              {hasOnClear && hasAnySelections && clearBtn}
            </React.Fragment>
          )}
          {variant === SelectVariant.typeahead && !customContent && (
            <React.Fragment>
              <div className={css(styles.selectToggleWrapper)}>
                {toggleIcon && <span className={css(styles.selectToggleIcon)}>{toggleIcon}</span>}
                <input
                  className={css(formStyles.formControl, styles.selectToggleTypeahead)}
                  aria-activedescendant={typeaheadActiveChild && typeaheadActiveChild.id}
                  id={`${selectToggleId}-select-typeahead`}
                  aria-label={typeAheadAriaLabel}
                  {...(typeAheadAriaDescribedby && { 'aria-describedby': typeAheadAriaDescribedby })}
                  placeholder={placeholderText as string}
                  value={
                    typeaheadInputValue !== null
                      ? typeaheadInputValue
                      : this.getDisplay(selections[0] as string, 'text') || ''
                  }
                  type="text"
                  onClick={this.onClick}
                  onChange={this.onChange}
                  autoComplete={inputAutoComplete}
                  disabled={isDisabled}
                  ref={this.inputRef}
                />
              </div>
              {hasOnClear && (selections[0] || typeaheadInputValue) && clearBtn}
            </React.Fragment>
          )}
          {variant === SelectVariant.typeaheadMulti && !customContent && (
            <React.Fragment>
              <div className={css(styles.selectToggleWrapper)}>
                {toggleIcon && <span className={css(styles.selectToggleIcon)}>{toggleIcon}</span>}
                {selections && Array.isArray(selections) && selections.length > 0 && selectedChips}
                <input
                  className={css(formStyles.formControl, styles.selectToggleTypeahead)}
                  aria-activedescendant={typeaheadActiveChild && typeaheadActiveChild.id}
                  id={`${selectToggleId}-select-multi-typeahead-typeahead`}
                  aria-label={typeAheadAriaLabel}
                  aria-invalid={validated === ValidatedOptions.error}
                  {...(typeAheadAriaDescribedby && { 'aria-describedby': typeAheadAriaDescribedby })}
                  placeholder={placeholderText as string}
                  value={typeaheadInputValue !== null ? typeaheadInputValue : ''}
                  type="text"
                  onChange={this.onChange}
                  onClick={this.onClick}
                  autoComplete={inputAutoComplete}
                  disabled={isDisabled}
                  ref={this.inputRef}
                />
              </div>
              {hasOnClear && ((selections && selections.length > 0) || typeaheadInputValue) && clearBtn}
            </React.Fragment>
          )}
          {validated === ValidatedOptions.success && (
            <span className={css(styles.selectToggleStatusIcon)}>
              <CheckCircleIcon aria-hidden="true" />
            </span>
          )}
          {validated === ValidatedOptions.error && (
            <span className={css(styles.selectToggleStatusIcon)}>
              <ExclamationCircleIcon aria-hidden="true" />
            </span>
          )}
          {validated === ValidatedOptions.warning && (
            <span className={css(styles.selectToggleStatusIcon)}>
              <ExclamationTriangleIcon aria-hidden="true" />
            </span>
          )}
        </SelectToggle>
        {isOpen && menuAppendTo === 'inline' && menuContainer}
      </div>
    );

    const getParentElement = () => {
      if (this.parentRef && this.parentRef.current) {
        return this.parentRef.current.parentElement;
      }
      return null;
    };

    return (
      <GenerateId>
        {randomId => (
          <SelectContext.Provider
            value={{
              onSelect,
              onFavorite,
              onClose: this.onClose,
              variant,
              inputIdPrefix: inputIdPrefix || randomId,
              shouldResetOnSelect
            }}
          >
            {menuAppendTo === 'inline' ? (
              mainContainer
            ) : (
              <Popper
                trigger={mainContainer}
                popper={popperContainer}
                direction={direction}
                appendTo={menuAppendTo === 'parent' ? getParentElement() : menuAppendTo}
                isVisible={isOpen}
              />
            )}
          </SelectContext.Provider>
        )}
      </GenerateId>
    );
  }
}
