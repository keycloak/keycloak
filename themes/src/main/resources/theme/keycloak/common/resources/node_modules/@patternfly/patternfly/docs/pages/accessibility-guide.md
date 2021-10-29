---
id: Accessibility guide
---

<a href="/a11y-report.html">Current a11y status</a>

*Please note, this guide is a work in progress and will be updated regularly. We welcome your comments and feedback.*

The goal of software accessibility is to remove barriers and create inclusive product experiences that work for everyone, regardless of physical ability.

Since accessibility is best achieved when considered early in the design and development process, we ask everyone who contributes to or consumes PatternFly to understand accessibility needs and how they can be met. The following guide provides techniques and suggestions to help you design, develop, and test UIs to ensure that everyone has a good user experience.

## Understanding users’ needs

Great user experiences don’t just happen; they’re designed, tested, and refined with the user in mind. To develop inclusive products, it’s important to understand the varying needs of a wide range of users and consider the assistive tools and methods they use. This section provides information to help you better understand and address the needs of these [different user groups](https://a11yproject.com/posts/myth-accessibility-is-blind-people/).

Note: It’s possible for a user to fall into more than one group, or to use tools and devices designed for a different user group. One of the greatest benefits of an inclusive design practice is that methods designed for a specific user group will often provide benefits to everyone.

### No vision

Users with no vision rely on screen readers to access web sites and applications. Often, screen reader users will navigate a page by browsing specific elements, like headers, links, or form elements. Use semantic elements and check that labels are meaningful when pulled out of context.

### Low vision

Users with low vision can have different needs depending on the nature of their visual impairment. Users may have difficulty with color differentiation, blurriness, or lack of vision in central or peripheral areas. These needs mean that interfaces should not rely on color to communicate information, palettes need to have sufficient contrast, and layouts should be responsive when font sizes are increased.

### Motor

Users with poor motor control can use a range of devices to access contents. Users who rely on a keyboard need elements that are keyboard accessible and highly visible when in focus. Users who rely on a mouse or touch need target areas that are large enough to be hit easily.

### Cognitive

Users who have difficulty processing information benefit from well-written content. Information should clear, concise, and easy to scan. Consider visual hierarchy, chunk content into short, related sections, and avoid long paragraphs.

## Designing and developing for accessibility

Our goal is to meet [level AA in the Web Content Accessibility Guidelines 2.1](https://www.w3.org/WAI/WCAG21/quickref/?currentsidebar=%23col_customize&levels=aaa). To help you get started, the following sections break some of these down by area of focus.

## Checklists

### What PatternFly should address

If you use PatternFly, or contribute to PatternFly as a designer or developer, these are the items that are expected to be covered in PatternFly:

| Guideline  | Link  |  |  |  |  |
| --- | --- | --- | --- | --- | --- |
| Semantic html structures are used to accurately communicate purpose and relationship of UI elements | [WCAG 1.3.1](https://www.w3.org/WAI/WCAG21/quickref/#info-and-relationships) | `design` | `html` | `css` |  |
| Color is not the only method of communication. Providing meaning through color is supplementary to providing meaning with text | [WCAG 1.4.1](https://www.w3.org/WAI/WCAG21/quickref/#use-of-color) | `design` | `html` | `css` |  |
| Colors used provide sufficient contrast | [WCAG 1.4.3](https://www.w3.org/WAI/WCAG21/quickref/#contrast-minimum) and [1.4.11](https://www.w3.org/WAI/WCAG21/quickref/#non-text-contrast) |  |  | `css` |  |
| Font sizes can scale up to 200% without loss of content or functionality, and up to 400% without needing to scroll in more than one direction.  | [WCAG&nbsp;1.4.4](https://www.w3.org/WAI/WCAG21/quickref/#resize-text) and [1.4.10](https://www.w3.org/WAI/WCAG21/quickref/#reflow) |  |  | `css` |  |
| Styles that affect text spacing (line height, space between paragraphs, letter spacing, and word spacing) can be increased without loss of content or functionality | [WCAG 1.4.12](https://www.w3.org/WAI/WCAG21/quickref/#text-spacing) |  |  | `css` |  |
| Contents that appear on hover and focus are dismissable, hoverable, and persistent | [WCAG 1.4.13](https://www.w3.org/WAI/WCAG21/quickref/#content-on-hover-or-focus) |  | `html` | `css` | `js` |
| All functionality is keyboard accessible | [WCAG 2.1.1](https://www.w3.org/WAI/WCAG21/quickref/#keyboard) and [2.1.2](https://www.w3.org/WAI/WCAG21/quickref/#no-keyboard-trap) |  | `html` |  |  |
| Order of elements in the HTML and in the layout follow a logical order | [WCAG 1.3.2](https://www.w3.org/WAI/WCAG21/quickref/#meaningful-sequence) and [2.4.3](https://www.w3.org/WAI/WCAG21/quickref/#focus-order) | `design` | `html` | `css` |  |
| Elements with focus are clearly visible | [WCAG 2.4.7](https://www.w3.org/WAI/WCAG21/quickref/#focus-visible) |  |  | `css` |  |
| Flashing content | [WCAG 2.3.1](https://www.w3.org/WAI/WCAG21/quickref/?showtechniques=231#three-flashes-or-below-threshold) |  |  | `css` |  |
| Functionality that uses complex gestures can also be operated with a single pointer without a path based gesture | [WCAG 2.5.1](https://www.w3.org/WAI/WCAG21/quickref/#pointer-gestures) | `design` |  |  |  |
| Pointer events can be cancelled  | [WCAG 2.5.2](https://www.w3.org/WAI/WCAG21/quickref/#pointer-cancellation) | | | | `js` |
| Visible labels of UI components are either the same as the accessible name or used in the beginning of the accessible name | [WCAG 2.5.3](https://www.w3.org/WAI/WCAG21/quickref/#label-in-name) |  | `html` |  |  |
| The target area for clickable elements is at least 44 by 44 [CSS pixels](https://www.w3.org/TR/WCAG21/#dfn-css-pixels) | [WCAG 2.5.5 (AAA)](https://www.w3.org/WAI/WCAG21/quickref/#target-size)* |  |  | `css` |  |
| An accessible name is provided for all elements | [WCAG 4.1.2](https://www.w3.org/WAI/WCAG21/quickref/#name-role-value) | `design` | `html` |  |  |
| Status messages can be programmatically determined through role or properties | [WCAG 4.1.3](https://www.w3.org/WAI/WCAG21/quickref/#status-messages) |  | `html` |  |  |

*WCAG 2.5.5 is included for reference only. This guideline suggests a size that is larger than what PatternFly requires.

### What products should address

If you consume PatternFly in your product, these are the items that are outside the scope of PatternFly, and should be addressed by the product developers and designers:


| Guideline  | Link  |  |  |
| --- | --- | --- | --- |
| Skip to main links | [WCAG 2.4.1](https://www.w3.org/WAI/WCAG21/quickref/#bypass-blocks) |  | `development` |
| Page titles | [WCAG 2.4.2](https://www.w3.org/WAI/WCAG21/quickref/#page-titled) |  | `development` |
| Links — If more than one link has the same label, it should also have the same url. Screen reader users can access the list of links that are on a page, which pulls the links out of context. If you have links with different URLs but the same label, then add additional text to provide context to the screen reader user. | [WCAG&nbsp;2.4.4](https://www.w3.org/WAI/WCAG21/quickref/#link-purpose-in-context) | `design`  | `development` |
| Landmarks — Use landmark roles to clearly identify regions that communicate page structure. If more than one landmark role occurs in the page, use aria-label to differentiate the landmark elements | [ARIA11](https://www.w3.org/TR/WCAG20-TECHS/ARIA11.html) | `design`  | `development` |
| Headings — Heading text should be descriptive. Correct heading levels should be used to communicate the outline of the page. | [WCAG 2.4.10](https://www.w3.org/WAI/WCAG21/quickref/#section-headings) and [H42](https://www.w3.org/TR/WCAG20-TECHS/H42.html) | `design`  | `development` |
| Contents — Should be meaningful, clear, and concise |  | `design` |  |

## Guidelines and references

- [Web Content Accessibility Guidelines 2.1](https://www.w3.org/TR/WCAG21/)
- [WebAIM's WCAG 2.0 Checklist](https://webaim.org/standards/wcag/checklist)
- [A11Y Project Checklist](https://a11yproject.com/checklist)

### PatternFly guidelines
These are guidelines that we have defined for PatternFly.

#### Experience parity
  - There should be parity between the screen reader contents and visibly rendered contents (refer to the [first note for aria-hidden](https://www.w3.org/TR/wai-aria/#aria-hidden)).
  - There should be parity among all input types: touch, mouse, and keyboard.
      - Don’t optimize the experience for one input type at the expense of another.
      - Contents that a user can interact with using a mouse are also accessible using touch or keyboard.
      - Don’t show interactive elements on hover. Interactive elements that can display in a popup must display on click/touch/Enter events.
  - There should be parity between hover and focus events.
      - Any information that’s available on hover for the mouse user should be available on focus for the keyboard-only user, and also available to the screen reader user using `aria-describedby` (refer to [Tooltips & Toggletips example from Inclusive Components](https://inclusive-components.design/tooltips-toggletips/)).

## Techniques

The [WCAG 2.0 techniques](https://www.w3.org/TR/WCAG20-TECHS/Overview.html#contents) provide examples on how to meet accessibility guidelines. Any techniques that are adopted as standard within PatternFly for handling specific patterns are included below.

### Labels and accessible names

- #### Form fields
  - Use explicit linking between `label` and form input elements (e.g. `input`, `textarea`, or `select`) when both elements are present. Aside from providing an accessible name to screen readers, this method also increases the clickable area of the form element by making the label clickable, too. ([H44](https://www.w3.org/TR/WCAG20-TECHS/H44.html))
  - When a `label` element cannot accompany a form input element:
      - Provide the label using `aria-label` or `aria-labelledby`. ([ARIA14](https://www.w3.org/TR/WCAG20-TECHS/ARIA16.html))
      - In a single-field form, the submit button label can serve as the field label for sighted users ([G167](https://www.w3.org/TR/WCAG20-TECHS/general.html#G167)) as well as assistive devices when using `aria-labelledby`
- #### Landmark roles
  - Screen reader users can navigate to sections of a page when [landmark roles](https://www.w3.org/TR/wai-aria-1.1/#landmark_roles) are used. Whenever a landmark role is used more than once, provide a name using `aria-label` or `aria-labelledby` to provide context for that landmark. ([ARIA6](https://www.w3.org/TR/WCAG20-TECHS/ARIA6.html), [ARIA16](https://www.w3.org/TR/WCAG20-TECHS/ARIA16.html))
  - While [`toolbar`](https://www.w3.org/TR/wai-aria-1.1/#toolbar) is not a landmark role, the same rule applies to this role.
- #### Icons
  Icons can either be decorative or semantic. Icons are **decorative** if you can remove an icon without affecting the information that is presented on the page. Icons are **semantic** when they provide information that otherwise isn't present, such as indicating status, indicating type of alert message, or replacing text as button labels. When an icon is semantic, the meaning must be provided in alternative ways to the user. The following guidelines should be followed when using icons within PatternFly components.
  - Add `aria-hidden="true"` for all icons, either to the icon element or a parent element of the icon. This renders the icon as something that assistive devices can ignore.
  - Additionally, for **semantic** icons:
      - Add a label for the icon in tooltip text that displays on hover, and also on focus for focusable elements.
      - For interactive elements like `<a>` and `<button>` where an icon is used as the label instead of text, provide the label on the interactive element using `aria-label`. For example:
      ```html noLive
      <button class="..." aria-label="Close dialog">
        <i class="..." aria-hidden="true"></i>
      </button>
      ```
      - For non-interactive icons, include `.pf-screen-reader` text near the icon. Depending on the component, the `.pf-screen-reader` text might not be a direct sibling to the icon element. For example, in the alert component, the icon label text is adjacent to the message. This way, when `role="alert"` is added to `.pf-c-alert__body` for dynamically displayed alerts, the type of message is announced along with the message text.
      ```html noLive
      <div class="pf-c-alert pf-m-success" aria-label="Success alert">
        <div aria-hidden="true" class="pf-c-alert__icon">
          <i class="fas fa-check-circle"></i>
        </div>
        <div class="pf-c-alert__body">
          <h4 class="pf-c-alert__title">
            {{#> screen-reader}}Success:{{/screen-reader}} Success alert title
          </h4>
        </div>
      </div>
      ```


### Trapping focus
The recommended interaction pattern for the modal components like the modal or popover is to trap focus within the modal element of the component when it becomes visible. For keyboard-only users that use the tab key to navigate the interface, this means that focus cannot be shifted outside of the modal when using the tab key. Instead, when focus leaves the last focusable item, it should be placed on the first focusable item of the modal. For screen reader users, the other contents on the page should be hidden from the screen reader. 

The method we recommend <a href="#testing">based on the screen reader / browser combinations we use for testing</a> is to apply `aria-hidden="true"` to the parent wrapping element of the page contents. Note that the modal element of the component must not be a descendent of this element with `aria-hidden="true"` and should be included as a sibling to this element.



## Testing
Many accessibility issues can be found by doing a few simple checks:

1. Use an accessibility audit tool to check for violations. If you are using PatternFly in your project, we recommend using [aXe: The Accessibility Engine](https://www.deque.com/axe/) to check for accessibility violations. If you are contributing to PatternFly, refer to our [README.md](https://github.com/patternfly/patternfly/blob/main/README.md#testing-for-accessibility) on how to run this tool.
2. Test keyboard accessibility, and check that these requirements are met:
    - All functionality is keyboard accessible
    - Order of elements in the HTML and in the layout follow a logical order
    - Elements with focus are clearly visible
3. Disable styles, then test the information architecture and presence of adequate text labels. The [WAVE browser extension from WebAIM](https://wave.webaim.org/extension/) provides this feature if it isn't available in the browser you are using. 
4. Test with any screen reader available in your operating system. Screen readers that we target for testing PatternFly are:
    - JAWS with Chrome, Windows ([keyboard shortcuts](https://dequeuniversity.com/screenreaders/jaws-keyboard-shortcuts))
    - Voiceover with Safari, Mac ([keyboard shortcuts](https://dequeuniversity.com/screenreaders/voiceover-keyboard-shortcuts))
    - NVDA with Firefox, Windows ([keyboard shortcuts](https://dequeuniversity.com/screenreaders/nvda-keyboard-shortcuts))
5. Check color contrast for:
    - Text color against background color ([Understanding WCAG 1.4.3](https://www.w3.org/WAI/WCAG21/Understanding/contrast-minimum.html))
    - Text color against link color ([Technique G183](https://www.w3.org/TR/WCAG20-TECHS/G183.html))
    - Visible boundaries of buttons and form elements against adjacent background color ([Understanding WCAG 1.4.11](https://www.w3.org/WAI/WCAG21/Understanding/non-text-contrast.html))
