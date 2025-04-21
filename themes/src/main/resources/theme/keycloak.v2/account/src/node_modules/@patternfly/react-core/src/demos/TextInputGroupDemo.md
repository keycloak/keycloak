---
id: Text input group
section: components
beta: true
---

import SearchIcon from '@patternfly/react-icons/dist/esm/icons/search-icon';
import TimesIcon from '@patternfly/react-icons/dist/esm/icons/times-icon';

## Demos

### Attribute-value filtering

This demo showcases the selection of attribute-value pairs based on a predefined set of data.

Available attributes are shown in a menu when focus is placed on the text input. Once an attribute is selected the values for that attribute will be shown in the menu. When a value is selected for the attribute the pair will be converted into a chip and placed into the chip group. Typing in the text input will filter the entries shown in the menu.

Attributes and values can both be selected by: 
- clicking the entry shown in the menu
- hitting the up or down arrows (while focused on the text input) to switch focus to the menu, navigating the menu using the arrow keys, and hitting `enter` on an entry
- hitting `enter` (while focused on the text input) to select the first entry shown in the menu at the time

Additionally, attributes can be selected by typing the full (case sensitive) name of an attribute, then typing `:`.

Attributes can be deselected (returning you to attribute selection mode) by hitting `escape`, or by hitting `backspace` when the only text in the text input is the attribute.

```ts file="./examples/TextInputGroup/AttributeValueFiltering.tsx"
```
### Auto-complete search with typeahead

This demo showcases a search input with suggestions, which filters possible selections based on the text you've entered. Unlike the attribute-value filtering demo, it allows creation of new chip items when the text entered is not available in the list of suggestions.

The current text in the input can be converted to a chip at any time by hitting `enter`. Auto-complete suggestions can be chosen by clicking the corresponding entry in the menu, or by navigating to an entry using the up/down arrow keys and selecting it with `enter`.

Hitting `escape` while focused on the input or menu will close the menu, and the menu will reopen when text is entered.

When only one item remains in the suggestion list, a typeahead hint will show and tab can be used to auto-complete the typing of that item.

```ts file="./examples/TextInputGroup/AutoCompleteSearch.tsx"
```
