# focus-trap

Trap focus within a DOM node.

There may come a time when you find it important to trap focus within a DOM node — so that when a user hits `Tab` or `Shift+Tab` or clicks around, she can't escape a certain cycle of focusable elements.

You will definitely face this challenge when you are trying to build **accessible modals**.

This module is a little, modular **vanilla JS** solution to that problem.

Use it in your higher-level components. For example, if you are using React check out [focus-trap-react](https://github.com/davidtheclark/focus-trap-react), a light wrapper around this library. If you are not a React user, consider creating light wrappers in your framework-of-choice.

## What it does

When a focus trap is activated, this is what should happen:

- Some element within the focus trap receives focus. By default, this will be the first element in the focus trap's tab order (as determined by [tabbable](https://github.com/davidtheclark/tabbable)). Alternately, you can specify an element that should receive this initial focus.
- The `Tab` and `Shift+Tab` keys will cycle through the focus trap's tabbable elements *but will not leave the focus trap*.
- Clicks within the focus trap behave normally; but clicks *outside* the focus trap are blocked.
- The `Escape` key will deactivate the focus trap.

When the focus trap is deactivated, this is what should happen:

- Focus is passed to *whichever element had focus when the trap was activated* (e.g. the button that opened the modal or menu).
- Tabbing and clicking behave normally everywhere.

[Check out the demos.](http://davidtheclark.github.io/focus-trap/demo/)

For more advanced usage (e.g. focus traps within focus traps), you can also pause a focus trap's behavior without deactivating it entirely, then unpause at will.

## Installation

```
npm install focus-trap
```

You can also use a UMD version published to `unpkg.com` as `dist/focus-trap.js` and `dist/focus-trap.min.js`.

## Browser Support

IE9+

Why?
Because this module uses [`EventTarget.addEventListener()`](document.createElement('button')).
And its only dependency, tabbable, uses [a couple of IE9+ functions](https://github.com/davidtheclark/tabbable#browser-support).

## Usage

### focusTrap = createFocusTrap(element[, createOptions]);

Returns a new focus trap on `element`.

`element` can be
- a DOM node (the focus trap itself) or
- a selector string (which will be pass to `document.querySelector()` to find the DOM node).

`createOptions`:

- **onActivate** {function}: A function that will be called when the focus trap activates.
- **onDeactivate** {function}: A function that will be called when the focus trap deactivates,
- **initialFocus** {element|string|function}: By default, when a focus trap is activated the first element in the focus trap's tab order will receive focus. With this option you can specify a different element to receive that initial focus. Can be a DOM node, or a selector string (which will be passed to `document.querySelector()` to find the DOM node), or a function that returns a DOM node.
- **fallbackFocus** {element|string|function}: By default, an error will be thrown if the focus trap contains no elements in its tab order. With this option you can specify a fallback element to programmatically receive focus if no other tabbable elements are found. For example, you may want a popover's `<div>` to receive focus if the popover's content includes no tabbable elements. *Make sure the fallback element has a negative `tabindex` so it can be programmatically focused.* The option value can be a DOM node, a selector string (which will be passed to `document.querySelector()` to find the DOM node), or  a function that returns a DOM node.
- **escapeDeactivates** {boolean}: Default: `true`. If `false`, the `Escape` key will not trigger deactivation of the focus trap. This can be useful if you want to force the user to make a decision instead of allowing an easy way out.
- **clickOutsideDeactivates** {boolean}: Default: `false`. If `true`, a click outside the focus trap will deactivate the focus trap and allow the click event to do its thing.
- **returnFocusOnDeactivate** {boolean}: Default: `true`. If `false`, when the trap is deactivated, focus will *not* return to the element that had focus before activation.

### focusTrap.activate([activateOptions])

Activates the focus trap, adding various event listeners to the document.

If focus is already within it the trap, it remains unaffected. Otherwise, focus-trap will try to focus the following nodes, in order:

- `createOptions.initialFocus`
- The first tabbable node in the trap
- `createOptions.fallbackFocus`

If none of the above exist, an error will be thrown. You cannot have a focus trap that lacks focus.

Returns the `focusTrap`.

`activateOptions`:

These options are used to override the focus trap's default behavior for this particular activation.

- **onActivate** {function | null | false}: Default: whatever you chose for `createOptions.onActivate`. `null` or `false` are the equivalent of a `noop`.

### focusTrap.deactivate([deactivateOptions])

Deactivates the focus trap.

Returns the `focusTrap`.

`deactivateOptions`:

These options are used to override the focus trap's default behavior for this particular deactivation.

- **returnFocus** {boolean}: Default: whatever you chose for `createOptions.returnFocusOnDeactivate`.
- **onDeactivate** {function | null | false}: Default: whatever you chose for `createOptions.onDeactivate`. `null` or `false` are the equivalent of a `noop`.

### focusTrap.pause()

Pause an active focus trap's event listening without deactivating the trap.

If the focus trap has not been activated, nothing happens.

Returns the `focusTrap`.

Any `onDeactivate` callback will not be called, and focus will not return to the element that was focused before the trap's activation. But the trap's behavior will be paused.

This is useful in various cases, one of which is when you want one focus trap within another. `demo-six` exemplifies how you can implement this.

### focusTrap.unpause()

Unpause an active focus trap. (See `pause()`, above.)

Focus is forced into the trap just as described for `focusTrap.activate()`.

If the focus trap has not been activated or has not been paused, nothing happens.

Returns the `focusTrap`.

## Examples

Read code in `demo/` and [see how it works](http://davidtheclark.github.io/focus-trap/demo/).

Here's what happens in `demo-one.js`:

```js
var createFocusTrap = require('../../');

var containerOne = document.getElementById('demo-one');
var focusTrapOne = createFocusTrap('#demo-one', {
  onDeactivate: function () {
    containerOne.className = 'trap';
  },
});

document.getElementById('activate-one').addEventListener('click', function () {
  focusTrapOne.activate();
  containerOne.className = 'trap is-active';
});

document.getElementById('deactivate-one').addEventListener('click', function () {
  focusTrapOne.deactivate();
});
```

## Other details

### One at a time

*Only one focus trap can be listening at a time.* If a second focus trap is activated the first will automatically pause. The first trap is unpaused and again traps focus when the second is deactivated.

Focus trap manages a queue of traps: if A activates; then B activates, pausing A; then C activates, pausing B; when C then deactivates, B is unpaused; and when B then deactivates, A is unpaused.

### Use predictable elements for the first and last tabbable elements in your trap

The focus trap will work best if the *first* and *last* focusable elements in your trap are simple elements that all browsers treat the same, like buttons and inputs.**

Tabbing will work as expected with trickier, less predictable elements — like iframes, shadow trees, audio and video elements, etc. — as long as they are *between* more predictable elements (that is, if they are not the first or last tabbable element in the trap).

This limitation is ultimately rooted in browser inconsistencies and inadequacies, but it comes to focus-trap through its dependency [Tabbable](https://github.com/davidtheclark/tabbable). You can read about more details [in the Tabbable documentation](https://github.com/davidtheclark/tabbable#more-details).

### Your trap should include a tabbable element or a focusable container

You can't have a focus trap without focus, so an error will be thrown if you try to initialize focus-trap with an element that contains no tabbable nodes.

If you find yourself in this situation, you should give you container `tabindex="-1"` and set it as `initialFocus` or `fallbackFocus`. A couple of demos illustrate this.

## Development

Because of the nature of the functionality, involving keyboard and click and (especially) focus events, JavaScript unit tests didn't make sense. (If you disagree and can help out, please PR!) So the demo is also the test: run it in browsers and see how it works, checking the documented requirements.
