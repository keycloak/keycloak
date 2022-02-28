# Changelog

## 3.1.2

- Fix reference to root element that caused errors within Shadow DOM.

## 3.1.1

- Allow module to be imported by non-browser JavaScript.

## 3.1.0

- Add `tabbable.isFocusable` and `tabbable.isTabbable` functions.

## 3.0.0

- Add `[contenteditable]` elements.

## 2.0.0

- Add `<audio>` and `<video>` elements with `controls` attributes.
- Only consider radio buttons tabbable if they are the `checked` on in their group, or if none in their group are `checked`.

## 1.1.3

- Fix bug causing SVG elements to precede elements they should follow in the tab order in IE.

## 1.1.2

- Ensure `querySelectorAll` receives a string argument.

## 1.1.1

- Fix crash when you call `tabbable(document)` (passing the `document` element).

## 1.1.0

- Add `includeContainer` option.

## 1.0.8

- Allows operation against elements that reside within iframes, by inspecting the element to determine its correct parent `document` (rather than relying on the global `document` object).

## 1.0.7

- Ensure stable sort of `tabindex`ed elements even in browsers that have an unstable `Array.prototype.sort`.

## 1.0.6

- Check `tabindex` attribute (via `getAttribute`), in addition to `node.tabIndex`, to fix handling of SVGs with `tabindex="-1"` in IE.

## 1.0.5

- Children of `visibility: hidden` elements that themselves have `visibility: visible` are considered tabbable.

## 1.0.4

- Fix IE9 compatibility.

## 1.0.3

- Further improvements to caching.

## 1.0.2

- Fix overaggressive caching that would prevent `tabbable` from knowing an element's children had changed.

## 1.0.1

- Fix handling of `<a>` elements with `tabindex="0"`.

## 1.0.0

- Initial release.
