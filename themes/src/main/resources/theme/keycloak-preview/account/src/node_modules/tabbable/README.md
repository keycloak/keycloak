# tabbable

[![Build Status](https://travis-ci.org/davidtheclark/tabbable.svg?branch=master)](https://travis-ci.org/davidtheclark/tabbable)

Returns an array of all\* tabbable DOM nodes within a containing node. (\* "all" has some necessary caveats, which you'll learn about by reading below.)

The following are considered tabbable:

- `<button>`s
- `<input>`s
- `<select>`s
- `<textarea>`s
- `<a>`s with `href` or `xlink:href` attributes
- `<audio>`s and `<videos>`s with `controls` attributes
- `[contenteditable]` elements
- anything with a non-negative `tabindex`

Any of the above will *not* be considered tabbable, though, if any of the following are also true about it:

- negative `tabindex`
- `disabled`
- either the node itself *or an ancestor of it* is hidden via `display: none` or `visibility: hidden`
- it's an `<input type="radio">` and a different radio in its group is `checked`

**If you think a node should be included in your array of tabbables *but it's not*, all you need to do is add `tabindex="0"` to deliberately include it.** (Or if it is in your array but you don't want it, you can add `tabindex="-1"` to deliberately exclude it.) This will also result in more consistent cross-browser behavior. For information about why your special node might *not* be included, see ["More details"](#more-details), below.

## Goals

- Accurate (or, as accurate as possible & reasonable)
- No dependencies
- Small
- Fast

## Browser Support

Basically IE9+.

Why? It uses [Element.querySelectorAll()](https://developer.mozilla.org/en-US/docs/Web/API/Element/querySelectorAll) and [Window.getComputedStyle()](https://developer.mozilla.org/en-US/docs/Web/API/Window/getComputedStyle).

## Installation

```
npm install tabbable
```

Dependencies: *none*.

You'll need to be compiling CommonJS (via browserify or webpack).

## API

### tabbable

```
tabbable(rootNode, [options])
```

Returns an array of ordered tabbable node within the `rootNode`.

Summary of ordering principles:
- First include any nodes with positive `tabindex` attributes (1 or higher), ordered by ascending `tabindex` and source order.
- Then include any nodes with a zero `tabindex` and any element that by default receives focus (listed above) and does not have a positive `tabindex` set, in source order.

#### rootNode

Type: `Node`. **Required.**

#### options

##### includeContainer

Type: `boolean`. Default: `false`.

If set to `true`, `rootNode` will be included in the returned tabbable node array, if `rootNode` is tabbable.

### tabbable.isTabbable

```
tabbable.isTabbable(node)
```

Returns a boolean indicating whether the provided node is considered tabbable.

### tabbable.isFocusable

```
tabbable.isFocusable(node)
```

Returns a boolean indicating whether the provided node is considered *focusable*.

All tabbable elements are focusable, but not all focusable elements are tabbable. For example, elements with `tabindex="-1"` are focusable but not tabbable.

## More details

- **Tabbable tries to identify elements that are reliably tabbable across (not dead) browsers.** Browsers are stupidly inconsistent in their behavior, though — especially for edge-case elements like `<object>` and `<iframe>` — so this means *some* elements that you *can* tab to in *some* browsers will be left out of the results. (To learn more about that stupid inconsistency, see this [amazing table](https://allyjs.io/data-tables/focusable.html)). To provide better consistency across browsers and ensure the elements you *want* in your tabbables list show up there, **try adding `tabindex="0"` to edge-case elements that Tabbable ignores**.
- (Exemplifying the above ^^:) **The tabbability of `<iframe>`s, `<embed>`s, `<object>`s, and `<svg>`s is [inconsistent across browsers](https://allyjs.io/data-tables/focusable.html)**, so if you need an accurate read on one of these elements you should try giving it a `tabindex`. (You'll also need to pay attention to the `focusable` attribute on SVGs in IE & Edge.) But you also might *not* be able to get an accurate read — so you should avoid relying on it.
- **Radio groups have some edge cases, which you can avoid by always having a `checked` one in each group** (and that is what you should usually do anyway). If there is no `checked` radio in the radio group, *all* of the radios will be considered tabbable. (Some browsers do this, otherwise don't — there's not consistency.)
- If you're thinking, "Why not just use the right `querySelectorAll`?", you *may* be on to something ... but, as with most "just" statements, you're probably not. For example, a simple `querySelectorAll` approach will not figure out whether an element is *hidden*, and therefore not actually tabbable. (That said, if you do think Tabbable can be simplified or otherwise improved, I'd love to hear your idea.)
- jQuery UI's `:tabbable` selector ignores elements with height and width of `0`. I'm not sure why — because I've found that I can still tab to those elements. So I kept them in. Only elements hidden with `display: none` or `visibility: hidden` are left out.
- Although Tabbable tries to deal with positive tabindexes, **you should not use positive tabindexes**. Accessibility experts seem to be in (rare) unanimous and clear consent about this: rely on the order of elements in the document.
- Safari on Mac OS X does not Tab to `<a>` elements by default: you have to change a setting to get the standard behavior. Tabbable does not know whether you've changed that setting or not, so it will include `<a>` elements in its list.

***Feedback and contributions more than welcome!***
