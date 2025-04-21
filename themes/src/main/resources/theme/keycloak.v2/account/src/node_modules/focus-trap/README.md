# focus-trap [![CI](https://github.com/focus-trap/focus-trap/workflows/CI/badge.svg?branch=master&event=push)](https://github.com/focus-trap/focus-trap/actions?query=workflow:CI+branch:master) [![license](https://badgen.now.sh/badge/license/MIT)](./LICENSE)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-22-orange.svg?style=flat-square)](#contributors)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

Trap focus within a DOM node.

There may come a time when you find it important to trap focus within a DOM node — so that when a user hits `Tab` or `Shift+Tab` or clicks around, she can't escape a certain cycle of focusable elements.

You will definitely face this challenge when you are trying to build **accessible modals**.

This module is a little, modular **vanilla JS** solution to that problem.

Use it in your higher-level components. For example, if you are using React check out [focus-trap-react](https://github.com/focus-trap/focus-trap-react), a light wrapper around this library. If you are not a React user, consider creating light wrappers in your framework-of-choice.

## What it does

When a focus trap is activated, this is what should happen:

- Some element within the focus trap receives focus. By default, this will be the first element in the focus trap's tab order (as determined by [tabbable](https://github.com/focus-trap/tabbable)). Alternately, you can specify an element that should receive this initial focus.
- The `Tab` and `Shift+Tab` keys will cycle through the focus trap's tabbable elements *but will not leave the focus trap*.
- Clicks within the focus trap behave normally; but clicks *outside* the focus trap are blocked.
- The `Escape` key will deactivate the focus trap.

When the focus trap is deactivated, this is what should happen:

- Focus is passed to *whichever element had focus when the trap was activated* (e.g. the button that opened the modal or menu).
- Tabbing and clicking behave normally everywhere.

[Check out the demos.](http://focus-trap.github.io/focus-trap/)

For more advanced usage (e.g. focus traps within focus traps), you can also pause a focus trap's behavior without deactivating it entirely, then unpause at will.

## Installation

```bash
npm install focus-trap
```

### UMD

You can also use a UMD version published to `unpkg.com` as `dist/focus-trap.umd.js` and `dist/focus-trap.umd.min.js`.

> NOTE: The UMD build does not bundle the `tabbable` dependency. Therefore you will have to also include that one, and include it *before* `focus-trap`.

```html
<head>
  <script src="https://unpkg.com/tabbable/dist/index.umd.js"></script>
  <script src="https://unpkg.com/focus-trap/dist/focus-trap.umd.js"></script>
</head>
```

## Browser Support

IE9+

Why?
Because this module uses [`EventTarget.addEventListener()`](document.createElement('button')).
And its only dependency, tabbable, uses [a couple of IE9+ functions](https://github.com/focus-trap/tabbable#browser-support).

## Usage

### createFocusTrap(element[, createOptions])

```javascript
import * as focusTrap from 'focus-trap'; // ESM
const focusTrap = require('focus-trap'); // CJS
// UMD: `focusTrap` is defined as a global on `window`

trap = focusTrap.createFocusTrap(element[, createOptions]);
```

Returns a new focus trap on `element` (one or more "containers" of tabbable nodes that, together, form the total set of nodes that can be visited, with clicks or the tab key, within the trap).

`element` can be:

- a DOM node (the focus trap itself);
- a selector string (which will be passed to `document.querySelector()` to find the DOM node); or
- an array of DOM nodes or selector strings (where the order determines where the focus will go after the last tabbable element of a DOM node/selector is reached).

> A focus trap must have at least one container with at least one tabbable/focusable node in it to be considered valid. While nodes can be added/removed at runtime, with the trap adjusting to added/removed tabbable nodes, **an error will be thrown** if the trap ever gets into a state where it determines none of its containers have any tabbable nodes in them *and* the `fallbackFocus` option does not resolve to an alternate node where focus can go.

#### createOptions

- **onActivate** `{() => void}`: A function that will be called **before** sending focus to the target element upon activation.
- **onPostActivate** `{() => void}`: A function that will be called **after** sending focus to the target element upon activation.
- **checkCanFocusTrap** `{(containers: Array<HTMLElement | SVGElement>) => Promise<void>}`: Animated dialogs have a small delay between when `onActivate` is called and when the focus trap is focusable. `checkCanFocusTrap` expects a promise to be returned. When that promise settles (resolves or rejects), focus will be sent to the first tabbable node (in tab order) in the focus trap (or the node configured in the `initialFocus` option). Due to the lack of Promise support, `checkCanFocusTrap` is not supported in IE unless you provide a Promise polyfill.
- **onDeactivate** `{() => void}`: A function that will be called **before** returning focus to the node that had focus prior to activation (or configured with the `setReturnFocus` option) upon deactivation.
- **onPostDeactivate** `{() => void}`: A function that will be called after the trap is deactivated, after `onDeactivate`. If the `returnFocus` deactivation option was set, it will be called **after** returning focus to the node that had focus prior to activation (or configured with the `setReturnFocus` option) upon deactivation; otherwise, it will be called after deactivation completes.
- **checkCanReturnFocus** `{(trigger: HTMLElement | SVGElement) => Promise<void>}`: An animated trigger button will have a small delay between when `onDeactivate` is called and when the focus is able to be sent back to the trigger. `checkCanReturnFocus` expects a promise to be returned. When that promise settles (resolves or rejects), focus will be sent to to the node that had focus prior to the activation of the trap (or the node configured in the `setReturnFocus` option). Due to the lack of Promise support, `checkCanReturnFocus` is not supported in IE unless you provide a Promise polyfill.
- **initialFocus** `{HTMLElement | SVGElement | string | false | (() => HTMLElement | SVGElement | false)}`: By default, when a focus trap is activated the first element in the focus trap's tab order will receive focus. With this option you can specify a different element to receive that initial focus. Can be a DOM node, or a selector string (which will be passed to `document.querySelector()` to find the DOM node), or a function that returns a DOM node. You can also set this option to `false` (or to a function that returns `false`) to prevent any initial focus at all when the trap activates.
  - 💬 Setting this option to `false` (or a function that returns `false`) will prevent the `fallbackFocus` option from being used.
  - ⚠️ See warning below about **Shadow DOM** and selector strings.
- **fallbackFocus** `{HTMLElement | SVGElement | string | () => HTMLElement | SVGElement}`: By default, an error will be thrown if the focus trap contains no elements in its tab order. With this option you can specify a fallback element to programmatically receive focus if no other tabbable elements are found. For example, you may want a popover's `<div>` to receive focus if the popover's content includes no tabbable elements. *Make sure the fallback element has a negative `tabindex` so it can be programmatically focused.* The option value can be a DOM node, a selector string (which will be passed to `document.querySelector()` to find the DOM node), or a function that returns a DOM node.
  - 💬 If `initialFocus` is `false` (or a function that returns `false`), this function will not be called when the trap is activated, and no element will be initially focused. This function may still be called while the trap is active if things change such that there are no longer any tabbable nodes in the trap.
  - ⚠️ See warning below about **Shadow DOM** and selector strings.
- **escapeDeactivates** `{boolean} | (e: KeyboardEvent) => boolean)`: Default: `true`. If `false` or returns `false`, the `Escape` key will not trigger deactivation of the focus trap. This can be useful if you want to force the user to make a decision instead of allowing an easy way out. Note that if a function is given, it's only called if the ESC key was pressed.
- **clickOutsideDeactivates** `{boolean | (e: MouseEvent | TouchEvent) => boolean}`: If `true` or returns `true`, a click outside the focus trap will immediately deactivate the focus trap and allow the click event to do its thing (i.e. to pass-through to the element that was clicked). This option **takes precedence** over `allowOutsideClick` when it's set to `true`. Default: `false`.
  - 💬 If a function is provided, it will be called up to **twice** (but only if the click occurs *outside* the trap's containers): First on the `mousedown` (or `touchstart` on mobile) event and, if `true` was returned, again on the `click` event. It will get the same node each time, and it's recommended that the returned value is also the same each time. Be sure to check the event type if the double call is an issue in your code.
  - ⚠️ If you're using a password manager such as 1Password, where the app adds a clickable icon to all fillable fields, you should avoid using this option, and instead use the `allowOutsideClick` option to better control exactly when the focus trap can be deactivated. The clickable icons are usually positioned absolutely, floating on top of the fields, and therefore *not* part of the container the trap is managing. When using the `clickOutsideDeactivates` option, clicking on a field's 1Password icon will likely cause the trap to be unintentionally deactivated.
- **allowOutsideClick** `{boolean | (e: MouseEvent | TouchEvent) => boolean}`: If set and is or returns `true`, a click outside the focus trap will not be prevented (letting focus temporarily escape the trap, without deactivating it), even if `clickOutsideDeactivates=false`. Default: `false`.
  - 💬 If this is a function, it will be called up to **twice** on every click (but only if the click occurs *outside* the trap's containers): First on `mousedown` (or `touchstart` on mobile), and then on the actual `click` if the function returned `true` on the first event. Be sure to check the event type if the double call is an issue in your code.
  - 💡 When `clickOutsideDeactivates=true`, this option is **ignored** (i.e. if it's a function, it will not be called).
  - Use this option to control if (and even which) clicks are allowed outside the trap in conjunction with `clickOutsideDeactivates=false`.
- **returnFocusOnDeactivate** `{boolean}`: Default: `true`. If `false`, when the trap is deactivated, focus will *not* return to the element that had focus before activation.
- **setReturnFocus** `{HTMLElement | SVGElement | string | (previousActiveElement: HTMLElement | SVGElement) => HTMLElement | SVGElement | false}`: By default, on **deactivation**, if `returnFocusOnDeactivate=true` (or if `returnFocus=true` in the [deactivation options](#trapdeactivatedeactivateoptions)), focus will be returned to the element that was focused just before activation. With this option, you can specify another element to programmatically receive focus after deactivation. It can be a DOM node, a selector string (which will be passed to `document.querySelector()` to find the DOM node **upon deactivation**), or a function that returns a DOM node to call **upon deactivation** (i.e. the selector and function options are only executed at the time the trap is deactivated), or `false` to leave focus where it is at the time of deactivation.
  - 💬 Using the selector or function options is a good way to return focus to a DOM node that may not even exist at the time the trap is activated.
  - ⚠️ See warning below about **Shadow DOM** and selector strings.
- **preventScroll** `{boolean}`: By default, focus() will scroll to the element if not in viewport. It can produce unintended effects like scrolling back to the top of a modal. If set to `true`, no scroll will happen.
- **delayInitialFocus** `{boolean}`: Default: `true`. Delays the autofocus to the next execution frame when the focus trap is activated. This prevents elements within the focusable element from capturing the event that triggered the focus trap activation.
- **document** {Document}: Default: `window.document`. Document where the focus trap will be active. This allows to use FocusTrap in an iFrame context.
- **tabbableOptions**: (optional) [tabbable options](https://github.com/focus-trap/tabbable#common-options) configurable on FocusTrap (all the *common options*).
  - ⚠️ See notes about **[testing in JSDom](#testing-in-jsdom)** (e.g. using Jest).

#### Shadow DOM

##### Selector strings

⚠️ Beware that putting a focus-trap **inside** an open Shadow DOM means you must either:

- **Not use selector strings** for options that support these (because nodes inside Shadow DOMs, even open shadows, are not visible via `document.querySelector()`); OR
- You must **use the `document` option** to configure the focus trap to use your *shadow host* element as its document. The downside of this option is that, while selector queries on nodes inside your trap will now work, the trap will not prevent focus from being set on nodes outside your Shadow DOM, which is the same drawback as putting a focus trap [inside an iframe](https://focus-trap.github.io/focus-trap/#demo-in-iframe).

##### Closed shadows

If you have closed shadow roots that you would like considered for tabbable/focusable nodes, use the `tabbableOptions.getShadowRoot` option to provide Tabbable (used internally) with a reference to a given node's shadow root so that it can be searched for candidates.

### trap.active

```typescript
trap.active: boolean
```

True if the trap is currently active.

### trap.paused

```typescript
trap.paused: boolean
```

True if the trap is currently paused.

### trap.activate()

```typescript
trap.activate([activateOptions]) => FocusTrap
```

Activates the focus trap, adding various event listeners to the document.

If focus is already within it the trap, it remains unaffected. Otherwise, focus-trap will try to focus the following nodes, in order:

- `createOptions.initialFocus`
- The first tabbable node in the trap
- `createOptions.fallbackFocus`

If none of the above exist, an error will be thrown. You cannot have a focus trap that lacks focus.

Returns the `trap`.

`activateOptions`:

These options are used to override the focus trap's default behavior for this particular activation.

- **onActivate** `{() => void}`: Default: whatever you chose for `createOptions.onActivate`. `null` or `false` are the equivalent of a `noop`.
- **onPostActivate** `{() => void}`: Default: whatever you chose for `createOptions.onPostActivate`. `null` or `false` are the equivalent of a `noop`.
- **checkCanFocusTrap** `{(containers: Array<HTMLElement | SVGElement>) => Promise<void>}`: Default: whatever you chose for `createOptions.checkCanFocusTrap`.

### trap.deactivate()

```typescript
trap.deactivate([deactivateOptions]) => FocusTrap
```

Deactivates the focus trap.

Returns the `trap`.

`deactivateOptions`:

These options are used to override the focus trap's default behavior for this particular deactivation.

- **returnFocus** `{boolean}`: Default: whatever you set for `createOptions.returnFocusOnDeactivate`. If `true`, then the `setReturnFocus` option (specified when the trap was created) is used to determine where focus will be returned.
- **onDeactivate** `{() => void}`: Default: whatever you set for `createOptions.onDeactivate`. `null` or `false` are the equivalent of a `noop`.
- **onPostDeactivate** `{() => void}`: Default: whatever you set for `createOptions.onPostDeactivate`. `null` or `false` are the equivalent of a `noop`.
- **checkCanReturnFocus** `{(trigger: HTMLElement | SVGElement) => Promise<void>}`: Default: whatever you set for `createOptions.checkCanReturnFocus`. Not called if the `returnFocus` option is falsy. `trigger` is either the originally focused node prior to activation, or the result of the `setReturnFocus` configuration option.

### trap.pause()

```typescript
trap.pause() => FocusTrap
```

Pause an active focus trap's event listening without deactivating the trap.

If the focus trap has not been activated, nothing happens.

Returns the `trap`.

Any `onDeactivate` callback will not be called, and focus will not return to the element that was focused before the trap's activation. But the trap's behavior will be paused.

This is useful in various cases, one of which is when you want one focus trap within another. `demo-six` exemplifies how you can implement this.

### trap.unpause()

```typescript
trap.unpause() => FocusTrap
```

Unpause an active focus trap. (See `pause()`, above.)

Focus is forced into the trap just as described for `focusTrap.activate()`.

If the focus trap has not been activated or has not been paused, nothing happens.

Returns the `trap`.

### trap.updateContainerElements()

```typescript
trap.updateContainerElements() => FocusTrap
```

Update the element(s) that are used as containers for the focus trap.

When you call the function `createFocusTrap`, you pass in an element (or selector), or an array of elements (or selectors) to keep the focus within.  This method simply allows you to update which elements to keep the focus within.

A use case for this is found in focus-trap-react, where React `ref`'s may not be initialized yet, but when they are you want to have them be a container element.

Returns the `trap`.

## Examples

Read code in `docs/` and [see how it works](http://focus-trap.github.io/focus-trap/).

Here's generally what happens in `default.js` (the "default behavior" demo):

```js
const { createFocusTrap } = require('../../index');

const container = document.getElementById('default');

const focusTrap = createFocusTrap('#default', {
  onActivate: () => container.classList.add('is-active'),
  onDeactivate: () => container.classList.remove('is-active'),
});

document
  .getElementById('activate-default')
  .addEventListener('click', focusTrap.activate);
document
  .getElementById('deactivate-default')
  .addEventListener('click', focusTrap.deactivate);
```

## Other details

### One at a time

*Only one focus trap can be listening at a time.* If a second focus trap is activated the first will automatically pause. The first trap is unpaused and again traps focus when the second is deactivated.

Focus trap manages a queue of traps: if A activates; then B activates, pausing A; then C activates, pausing B; when C then deactivates, B is unpaused; and when B then deactivates, A is unpaused.

### Use predictable elements for the first and last tabbable elements in your trap

The focus trap will work best if the *first* and *last* focusable elements in your trap are simple elements that all browsers treat the same, like buttons and inputs.**

Tabbing will work as expected with trickier, less predictable elements — like iframes, shadow trees, audio and video elements, etc. — as long as they are *between* more predictable elements (that is, if they are not the first or last tabbable element in the trap).

This limitation is ultimately rooted in browser inconsistencies and inadequacies, but it comes to focus-trap through its dependency [Tabbable](https://github.com/focus-trap/tabbable). You can read about more details [in the Tabbable documentation](https://github.com/focus-trap/tabbable#more-details).

### Your trap should include a tabbable element or a focusable container

You can't have a focus trap without focus, so an error will be thrown if you try to initialize focus-trap with an element that contains no tabbable nodes.

If you find yourself in this situation, you should give you container `tabindex="-1"` and set it as `initialFocus` or `fallbackFocus`. A couple of demos illustrate this.

## Development

Because of the nature of the functionality, involving keyboard and click and (especially) focus events, JavaScript unit tests don't make sense. After all, JSDom does not fully support focus events. Since the demo was developed to also be the test, we use Cypress to automate running through all demos in the demo page.

## Help

### Testing in JSDom

> ⚠️ JSDom is not officially supported. Your mileage may vary, and tests may break from one release to the next (even a patch or minor release).
>
> This topic is just here to help with what we know may affect your tests.

In general, a focus trap is best tested in a full browser environment such as Cypress, Playwright, or Nightwatch where a full DOM is available.

Sometimes, that's not entirely desirable, and depending on what you're testing, you may be able to get away with using JSDom (e.g. via Jest), but you'll have to configure your traps using the `tabbableOptions.displayCheck: 'none'` option.

See [Testing tabbable in JSDom](https://github.com/focus-trap/tabbable#testing-in-jsdom) for more details.

# Contributing

See [CONTRIBUTING](CONTRIBUTING.md).

## Contributors

In alphabetical order:

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/andersthorsen"><img src="https://avatars.githubusercontent.com/u/190081?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Anders Thorsen</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Aandersthorsen" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/bparish628"><img src="https://avatars1.githubusercontent.com/u/8492971?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Benjamin Parish</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Abparish628" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://clintgoodman.com"><img src="https://avatars3.githubusercontent.com/u/5473697?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Clint Goodman</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=cgood92" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=cgood92" title="Documentation">📖</a> <a href="#example-cgood92" title="Examples">💡</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=cgood92" title="Tests">⚠️</a></td>
    <td align="center"><a href="https://github.com/Dan503"><img src="https://avatars.githubusercontent.com/u/10610368?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Daniel Tonon</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=Dan503" title="Documentation">📖</a> <a href="#tool-Dan503" title="Tools">🔧</a> <a href="#a11y-Dan503" title="Accessibility">️️️️♿️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=Dan503" title="Code">💻</a></td>
    <td align="center"><a href="https://github.com/DaviDevMod"><img src="https://avatars.githubusercontent.com/u/98312056?v=4?s=100" width="100px;" alt=""/><br /><sub><b>DaviDevMod</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=DaviDevMod" title="Documentation">📖</a></td>
    <td align="center"><a href="http://davidtheclark.com/"><img src="https://avatars2.githubusercontent.com/u/628431?v=4?s=100" width="100px;" alt=""/><br /><sub><b>David Clark</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=davidtheclark" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Adavidtheclark" title="Bug reports">🐛</a> <a href="#infra-davidtheclark" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=davidtheclark" title="Tests">⚠️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=davidtheclark" title="Documentation">📖</a> <a href="#maintenance-davidtheclark" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/features/security"><img src="https://avatars1.githubusercontent.com/u/27347476?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Dependabot</b></sub></a><br /><a href="#maintenance-dependabot" title="Maintenance">🚧</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/michael-ar"><img src="https://avatars3.githubusercontent.com/u/18557997?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Michael Reynolds</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Amichael-ar" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/liunate"><img src="https://avatars2.githubusercontent.com/u/38996291?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Nate Liu</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=liunate" title="Tests">⚠️</a></td>
    <td align="center"><a href="https://github.com/far-fetched"><img src="https://avatars.githubusercontent.com/u/11621383?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Piotr Panek</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Afar-fetched" title="Bug reports">🐛</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=far-fetched" title="Documentation">📖</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=far-fetched" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=far-fetched" title="Tests">⚠️</a></td>
    <td align="center"><a href="https://github.com/randypuro"><img src="https://avatars2.githubusercontent.com/u/2579?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Randy Puro</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Arandypuro" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://github.com/sadick254"><img src="https://avatars2.githubusercontent.com/u/5238135?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sadick</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=sadick254" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=sadick254" title="Tests">⚠️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=sadick254" title="Documentation">📖</a></td>
    <td align="center"><a href="https://scottblinch.me/"><img src="https://avatars2.githubusercontent.com/u/4682114?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Scott Blinch</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=scottblinch" title="Documentation">📖</a></td>
    <td align="center"><a href="https://seanmcp.com/"><img src="https://avatars1.githubusercontent.com/u/6360367?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sean McPherson</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=SeanMcP" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=SeanMcP" title="Documentation">📖</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/skriems"><img src="https://avatars.githubusercontent.com/u/15573317?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Sebastian Kriems</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Askriems" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://recollectr.io"><img src="https://avatars2.githubusercontent.com/u/6835891?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Slapbox</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3ASlapbox" title="Bug reports">🐛</a></td>
    <td align="center"><a href="https://stefancameron.com/"><img src="https://avatars3.githubusercontent.com/u/2855350?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Stefan Cameron</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=stefcameron" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Astefcameron" title="Bug reports">🐛</a> <a href="#infra-stefcameron" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=stefcameron" title="Tests">⚠️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=stefcameron" title="Documentation">📖</a> <a href="#maintenance-stefcameron" title="Maintenance">🚧</a></td>
    <td align="center"><a href="http://tylerhawkins.info/201R/"><img src="https://avatars0.githubusercontent.com/u/13806458?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Tyler Hawkins</b></sub></a><br /><a href="#tool-thawkin3" title="Tools">🔧</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=thawkin3" title="Tests">⚠️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=thawkin3" title="Documentation">📖</a></td>
    <td align="center"><a href="https://github.com/wandroll"><img src="https://avatars.githubusercontent.com/u/4492317?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Wandrille Verlut</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=wandroll" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=wandroll" title="Tests">⚠️</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=wandroll" title="Documentation">📖</a> <a href="#tool-wandroll" title="Tools">🔧</a></td>
    <td align="center"><a href="http://willmruzek.com/"><img src="https://avatars.githubusercontent.com/u/108522?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Will Mruzek</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/commits?author=mruzekw" title="Code">💻</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=mruzekw" title="Documentation">📖</a> <a href="#example-mruzekw" title="Examples">💡</a> <a href="https://github.com/focus-trap/focus-trap/commits?author=mruzekw" title="Tests">⚠️</a> <a href="#question-mruzekw" title="Answering Questions">💬</a></td>
    <td align="center"><a href="https://github.com/zioth"><img src="https://avatars3.githubusercontent.com/u/945603?v=4?s=100" width="100px;" alt=""/><br /><sub><b>Zioth</b></sub></a><br /><a href="#ideas-zioth" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Azioth" title="Bug reports">🐛</a></td>
  </tr>
  <tr>
    <td align="center"><a href="https://github.com/jpveooys"><img src="https://avatars.githubusercontent.com/u/66470099?v=4?s=100" width="100px;" alt=""/><br /><sub><b>jpveooys</b></sub></a><br /><a href="https://github.com/focus-trap/focus-trap/issues?q=author%3Ajpveooys" title="Bug reports">🐛</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->
