type FocusableElement = HTMLElement | SVGElement;

export type CheckOptions = {
  displayCheck?: 'full' | 'non-zero-area' | 'none';
  getShadowRoot?: boolean | ((node: FocusableElement) => ShadowRoot | boolean | undefined);
};

export type TabbableOptions = {
  includeContainer?: boolean;
};

export declare function tabbable(
  container: Element,
  options?: TabbableOptions & CheckOptions
): FocusableElement[];

export declare function focusable(
  container: Element,
  options?: TabbableOptions & CheckOptions
): FocusableElement[];

export declare function isTabbable(
  element: Element,
  options?: CheckOptions
): boolean;

export declare function isFocusable(
  element: Element,
  options?: CheckOptions
): boolean;
