---
id: Page
section: components
cssPrefix: pf-c-page
wrapperTag: div
---## Examples

### Vertical nav

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">toggle</div>
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">pf-c-nav</div>
  </div>
  <main class="pf-c-page__main" tabindex="-1">
    <section class="pf-c-page__main-section pf-m-dark-100"></section>
    <section class="pf-c-page__main-section pf-m-dark-200"></section>
    <section class="pf-c-page__main-section pf-m-light"></section>
    <section class="pf-c-page__main-section"></section>
  </main>
</div>

```

### Horizontal nav

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-nav">pf-c-nav</div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <main class="pf-c-page__main" tabindex="-1">
    <section class="pf-c-page__main-section pf-m-dark-100"></section>
    <section class="pf-c-page__main-section pf-m-dark-200"></section>
    <section class="pf-c-page__main-section pf-m-light"></section>
    <section class="pf-c-page__main-section"></section>
  </main>
</div>

```

### With or without fill

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-nav">pf-c-nav</div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <main class="pf-c-page__main" tabindex="-1">
    <section class="pf-c-page__main-section pf-m-light"></section>
    <section
      class="pf-c-page__main-section pf-m-fill"
    >This section uses pf-m-fill to fill the available space.</section>
    <section
      class="pf-c-page__main-section pf-m-light pf-m-no-fill"
    >This section uses pf-m-no-fill to not fill the available space.</section>
  </main>
</div>

```

### Main section padding

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">toggle</div>
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">pf-c-nav</div>
  </div>
  <main class="pf-c-page__main" tabindex="-1">
    <section
      class="pf-c-page__main-section"
    >This `.pf-c-page__main-section` has default padding.</section>
    <section
      class="pf-c-page__main-section pf-m-no-padding pf-m-light"
    >This `.pf-c-page__main-section` uses `.pf-m-no-padding` to remove all padding.</section>
    <section
      class="pf-c-page__main-section pf-m-no-padding pf-m-padding-on-md"
    >This `.pf-c-page__main-section` uses `.pf-m-no-padding .pf-m-padding-on-md` to remove padding up to the `md` breakpoint.</section>
  </main>
</div>

```

### Main section variations

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">toggle</div>
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <div class="pf-c-page__sidebar">
    <div class="pf-c-page__sidebar-body">pf-c-nav</div>
  </div>
  <main class="pf-c-page__main" tabindex="-1">
    <section
      class="pf-c-page__main-nav"
    >`.pf-c-page__main-nav` for tertiary navigation</section>
    <section
      class="pf-c-page__main-breadcrumb"
    >`.pf-c-page__main-breadcrumb` for breadcrumbs</section>
    <section
      class="pf-c-page__main-section"
    >`.pf-c-page__main-section` for main sections</section>
    <section class="pf-c-page__main-tabs">`.pf-c-page__main-tabs` for tabs</section>
    <section
      class="pf-c-page__main-wizard"
    >`.pf-c-page__main-wizard` for wizards</section>
  </main>
</div>

```

### Centered section

```html
<div class="pf-c-page">
  <header class="pf-c-page__header">
    <div class="pf-c-page__header-brand">
      <div class="pf-c-page__header-brand-toggle">toggle</div>
      <a href="#" class="pf-c-page__header-brand-link">Logo</a>
    </div>
    <div class="pf-c-page__header-tools">header-tools</div>
  </header>
  <main class="pf-c-page__main" tabindex="-1">
    <section class="pf-c-page__main-section pf-m-limit-width pf-m-align-center">
      <div class="pf-c-page__main-body">
        <div class="pf-c-card">
          <div class="pf-c-card__body">
            When a width limited page section is wider than the value of
            <code>--pf-c-page--section--m-limit-width--MaxWidth</code>, the section will be centered in the main section.
            <br />
            <br />The content in this example is placed in a card to better illustrate how the section behaves when it is centered. A card is not required to center a page section.
          </div>
        </div>
      </div>
    </section>
  </main>
</div>

```

## Documentation

### Overview

This component provides the basic chrome for a page, including sidebar, header, and main areas. To make the page component take up the full height of the viewport, it is recommended to add `height: 100%;` to all ancestor elements of the page component.

### Accessibility

| Attribute                     | Applied to                                       | Outcome                                                                                                                                                                                                                                                                           |
| ----------------------------- | ------------------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `role="banner"`               | `.pf-c-page__header`                             | Identifies the element that serves as the banner region.                                                                                                                                                                                                                          |
| `role="main"`                 | `.pf-c-page__main`                               | Identifies the element that serves as the main region.                                                                                                                                                                                                                            |
| `tabindex="-1"`               | `.pf-c-page__main`                               | Allows the main region to receive programmatic focus. **Required**                                                                                                                                                                                                                |
| `id="[id]"`                   | `.pf-c-page__main`                               | Provides a hook for sending focus to new content. **Required**                                                                                                                                                                                                                    |
| `aria-expanded="true/false"`  | `.pf-c-page__header-brand-toggle > .pf-c-button` | Indicates that the expandable content is visible and the current state of the contents. **Required**                                                                                                                                                                              |
| `aria-controls="[id of nav]"` | `.pf-c-page__header-brand-toggle > .pf-c-button` | Identifies the element controlled by the toggle. **Required**                                                                                                                                                                                                                     |
| `tabindex="0"`                | `.pf-c-page__main-section.pf-m-overflow-scroll`  | If a page section has overflow content that triggers a scrollbar, to ensure that the content is keyboard accessible, the page section must include either a focusable element within the scrollable region or the page section itself must be focusable by adding `tabindex="0"`. |

### Usage

| Class                                                    | Applied to                                                        | Outcome                                                                                                                                                                                                                                                       |
| -------------------------------------------------------- | ----------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `.pf-c-page`                                             | `<div>`                                                           | Declares the page component.                                                                                                                                                                                                                                  |
| `.pf-c-page__header`                                     | `<header>`                                                        | Declares the page header.                                                                                                                                                                                                                                     |
| `.pf-c-page__header-brand`                               | `<div>`                                                           | Creates a header container to nest the brand component.                                                                                                                                                                                                       |
| `.pf-c-page__header-brand-toggle`                        | `<div>`                                                           | Creates a container to nest the sidebar toggle.                                                                                                                                                                                                               |
| `.pf-c-page__header-brand-link`                          | `<a>`, `<span>`                                                   | Creates a link for the brand logo. Use a `<span>` if there is no link.                                                                                                                                                                                        |
| `.pf-c-page__header-selector`                            | `<div>`                                                           | Creates a header container to nest the context selector component.                                                                                                                                                                                            |
| `.pf-c-page__header-nav`                                 | `<div>`                                                           | Creates a container to nest the navigation component in the header.                                                                                                                                                                                           |
| `.pf-c-page__header-tools`                               | `<div>`                                                           | Creates a container to nest the icons and menus in header.                                                                                                                                                                                                    |
| `.pf-c-page__header-tools-group`                         | `<div>`                                                           | Creates a container for grouping sets of icons and menus in header.                                                                                                                                                                                           |
| `.pf-c-page__header-tools-item`                          | `<div>`                                                           | Creates a container for an item in a header tools group.                                                                                                                                                                                                      |
| `.pf-c-page__sidebar`                                    | `<aside>`                                                         | Declares the page sidebar.                                                                                                                                                                                                                                    |
| `.pf-c-page__sidebar-body`                               | `<div>`                                                           | Creates a wrapper within the sidebar to hold content.                                                                                                                                                                                                         |
| `.pf-c-page__main`                                       | `<main>`                                                          | Declares the main page area.                                                                                                                                                                                                                                  |
| `.pf-c-page__main-nav`                                   | `<section>`                                                       | Creates a container to nest the navigation component in the main page area.                                                                                                                                                                                   |
| `.pf-c-page__main-breadcrumb`                            | `<section>`                                                       | Creates a container to nest the breadcrumb component in the main page area.                                                                                                                                                                                   |
| `.pf-c-page__main-section`                               | `<section>`                                                       | Creates a section container in the main page area. **Note: The last/only `.pf-c-page__main-section` element will grow to fill the availble vertical space. You can change this behavior using `.pf-m-fill` and `.pf-m-no-fill`, which are documented below.** |
| `.pf-c-page__main-tabs`                                  | `<section>`                                                       | Creates a container to nest the tabs component in the main page area.                                                                                                                                                                                         |
| `.pf-c-page__main-wizard`                                | `<section>`                                                       | Creates a container to nest the wizard component in the main page area.                                                                                                                                                                                       |
| `.pf-c-page__main-body`                                  | `<div>`                                                           | Creates the body section for a page section. **Required when using `.pf-m-limit-width` on `.pf-c-page__main-section`**                                                                                                                                        |
| `.pf-c-page__main-group`                                 | `<div>`                                                           | Creates the group of `.pf-c-page__main-*` sections. Can be used in combination with `.pf-m-sticky-[top/bottom]` to make multiple sections sticky.                                                                                                             |
| `.pf-c-page__drawer`                                     | `<div>`                                                           | Creates a container for the drawer component when placing the main page element in the drawer body.                                                                                                                                                           |
| `.pf-m-selected`                                         | `.pf-c-page__header-tools-item`                                   | Modifies a header tools item to indicate that the button inside is in the selected state.                                                                                                                                                                     |
| `.pf-m-expanded`                                         | `.pf-c-page__sidebar`                                             | Modifies the sidebar for the expanded state.                                                                                                                                                                                                                  |
| `.pf-m-collapsed`                                        | `.pf-c-page__sidebar`                                             | Modifies the sidebar for the collapsed state.                                                                                                                                                                                                                 |
| `.pf-m-light`                                            | `.pf-c-page__sidebar`                                             | Modifies the sidebar the light variation. **Note: for use with a light themed nav component**                                                                                                                                                                 |
| `.pf-m-light`                                            | `.pf-c-page__main-section`                                        | Modifies a main page section to have a light theme.                                                                                                                                                                                                           |
| `.pf-m-dark-200`                                         | `.pf-c-page__main-section`                                        | Modifies a main page section to have a dark theme and a dark transparent background.                                                                                                                                                                          |
| `.pf-m-dark-100`                                         | `.pf-c-page__main-section`                                        | Modifies a main page section to have a dark theme and a darker transparent background.                                                                                                                                                                        |
| `.pf-m-light-200`                                        | `.pf-c-page__main-wizard`                                         | Modifies a wizard page section to have a light 200 theme.                                                                                                                                                                                                     |
| `.pf-m-no-padding`, `.pf-m-no-padding{-on-[breakpoint]}` | `.pf-c-page__main-section`                                        | Removes padding from the main page section at an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                                    |
| `.pf-m-padding{-on-[breakpoint]}`                        | `.pf-c-page__main-section`                                        | Modifies the main page section to add padding back in at an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes). Should be used with pf-m-no-padding.                                                    |
| `.pf-m-fill`                                             | `.pf-c-page__main-section`                                        | Modifies a main page section to grow to fill the available vertical space.                                                                                                                                                                                    |
| `.pf-m-no-fill`                                          | `.pf-c-page__main-section`                                        | Modifies a main page section to not grow to fill the available vertical space.                                                                                                                                                                                |
| `.pf-m-hidden{-on-[breakpoint]}`                         | `.pf-c-page__header-tools-group`, `.pf-c-page__header-tools-item` | Hides a header tools group or item at an optional breakpoint, or hides it at all [breakpoints](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes) with `.pf-m-hidden`.                                                        |
| `.pf-m-visible{-on-[breakpoint]}`                        | `.pf-c-page__header-tools-group`, `.pf-c-page__header-tools-item` | Shows a header tools group or item at an optional [breakpoint](/developer-resources/global-css-variables#breakpoint-variables-and-class-suffixes).                                                                                                            |
| `.pf-m-limit-width`                                      | `.pf-c-page__main-section`                                        | Modifies a page section to limit the `max-width` of the content inside.                                                                                                                                                                                       |
| `.pf-m-align-center`                                     | `.pf-c-page__main-section.pf-m-limit-width`                       | Modifies a page section body to align center.                                                                                                                                                                                                                 |
| `.pf-m-sticky-top{-on-[breakpoint]-height}`              | `.pf-c-page__main-*`                                              | Modifies a section/group to be sticky to the top of its container at an optional height breakpoint.                                                                                                                                                           |
| `.pf-m-sticky-bottom{-on-[breakpoint]-height}`           | `.pf-c-page__main-*`                                              | Modifies a section/group to be sticky to the bottom of its container at an optional height breakpoint.                                                                                                                                                        |
| `.pf-m-shadow-bottom`                                    | `.pf-c-page__main-*`                                              | Modifies a section/group to have a bottom shadow.                                                                                                                                                                                                             |
| `.pf-m-shadow-top`                                       | `.pf-c-page__main-*`                                              | Modifies a section/group to have a top shadow.                                                                                                                                                                                                                |
| `.pf-m-overflow-scroll`                                  | `.pf-c-page__main-*`                                              | Modifies a section/group to show a scrollbar if it has overflow content.                                                                                                                                                                                      |
