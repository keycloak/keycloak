---
id: Card
section: components
cssPrefix: pf-c-card
propComponents: ['Card', 'CardActions', 'CardHeader', 'CardHeaderMain', 'CardTitle', 'CardBody', 'CardFooter', 'CardExpandableContent']
ouia: true
---

import pfLogo from './pfLogo.svg';
import pfLogoSmall from './pf-logo-small.svg';

## Examples

### Basic

```ts file='./CardBasic.tsx'
```

### With modifiers

```ts file='./CardWithModifiers.tsx'
```

### With image and actions

```ts file='./CardWithImageAndActions.tsx'
```

### Header in card head

```ts file='./CardHeaderInCardHead.tsx'
```

### Only actions in card head (no header/footer)

```ts file='./CardOnlyActionsInCardHead.tsx'
```

### Only image in card head

```ts file='./CardOnlyImageInCardHead.tsx'
```

### With no footer

```ts file='./CardWithNoFooter.tsx'
```

### With no header

```ts file='./CardWithNoHeader.tsx'
```

### With only a body section

```ts file='./CardWithOnlyBodySection.tsx'
```

### With multiple body sections

```ts file='./CardWithMultipleBodySections.tsx'
```

### With only a body section that fills

```ts file='./CardWithBodySectionFills.tsx'
```

### Selectable

```ts file='./CardSelectable.tsx'
```

### Selectable accessibility highlight

This example demonstrates how the `hasSelectableInput` and `onSelectableInputChange` props improve accessibility for selectable cards.

The first card sets `hasSelectableInput` to true, which renders a checkbox input that is only visible to, and navigable by, screen readers. This input communicates to assistive technology users that a card is selectable, and if so, it communicates the current selection state as well.

By default this input will have an aria-label that corresponds to the title given to the card if using the card title component. If you don't use the card title component in your selectable card, you must pass a custom aria-label for this input using the `selectableInputAriaLabel` prop.

The first card also (by passing an onchange callback to `onSelectableInputChange`) enables the selection/deselection of the associated card by checking/unchecking the checkbox input.

The second card does not set `hasSelectableInput` to true, so the input is not rendered. It does not communicate to screen reader users that it is selectable or if it is currently selected.

To best understand this example it is encouraged that you navigate both of these cards using a screen reader.

```ts file='./CardSelectableA11yHighlight.tsx'
```

### With heading element

```ts file='./CardWithHeadingElement.tsx'
```

### Expandable

```ts file='./CardExpandable.tsx'
```

### Expandable with icon

```ts file='./CardExpandableWithIcon.tsx'
```

### Legacy selectable

```ts file='./CardLegacySelectable.tsx'
```
