/* eslint-disable @typescript-eslint/prefer-function-type */
/**
 * Added types from tippy.js and popper.js to preserve backwards compatibility
 * Can remove in next breaking change release
 */

import Popper from './DeprecatedPopperTypes';

export type BasePlacement = 'top' | 'bottom' | 'left' | 'right';

export type Placement = Popper.Placement;

export type Boundary = Popper.Boundary | HTMLElement;

export type Content = string | Element | ((ref: Element) => Element | string);

export type SingleTarget = Element;

export type MultipleTargets = string | Element[] | NodeList;

export type Targets = SingleTarget | MultipleTargets;

export interface ReferenceElement<TProps = Props> extends Element {
  _tippy?: Instance<TProps>;
}

export interface PopperElement<TProps = Props> extends HTMLDivElement {
  _tippy?: Instance<TProps>;
}

export interface PopperInstance extends Popper {
  // Undo the static so we can mutate values inside for `tippyDistance`
  modifiers: Popper.BaseModifier[];
}

export interface LifecycleHooks<TProps = Props> {
  onAfterUpdate(instance: Instance<TProps>, partialProps: Partial<TProps>): void;
  onBeforeUpdate(instance: Instance<TProps>, partialProps: Partial<TProps>): void;
  onCreate(instance: Instance<TProps>): void;
  onDestroy(instance: Instance<TProps>): void;
  onHidden(instance: Instance<TProps>): void;
  onHide(instance: Instance<TProps>): void | false;
  onMount(instance: Instance<TProps>): void;
  onShow(instance: Instance<TProps>): void | false;
  onShown(instance: Instance<TProps>): void;
  onTrigger(instance: Instance<TProps>, event: Event): void;
  onUntrigger(instance: Instance<TProps>, event: Event): void;
}

export interface Props extends LifecycleHooks {
  allowHTML: boolean;
  animateFill: boolean;
  animation: string;
  appendTo: 'parent' | Element | ((ref: Element) => Element);
  aria: 'describedby' | 'labelledby' | null;
  arrow: boolean | string | SVGElement;
  boundary: Boundary;
  content: Content;
  delay: number | [number | null, number | null];
  distance: number | string;
  duration: number | [number | null, number | null];
  flip: boolean;
  flipBehavior: 'flip' | Placement[];
  flipOnUpdate: boolean;
  followCursor: boolean | 'horizontal' | 'vertical' | 'initial';
  hideOnClick: boolean | 'toggle';
  ignoreAttributes: boolean;
  inertia: boolean;
  inlinePositioning: boolean;
  interactive: boolean;
  interactiveBorder: number;
  interactiveDebounce: number;
  lazy: boolean;
  maxWidth: number | string;
  multiple: boolean;
  offset: number | string;
  placement: Placement;
  plugins: Plugin[];
  popperOptions: Popper.PopperOptions;
  role: string;
  showOnCreate: boolean;
  sticky: boolean | 'reference' | 'popper';
  theme: string;
  touch: boolean | 'hold' | ['hold', number];
  trigger: string;
  triggerTarget: Element | Element[] | null;
  updateDuration: number;
  zIndex: number;
}

export interface DefaultProps extends Props {
  delay: number | [number, number];
  duration: number | [number, number];
}

export interface Instance<TProps = Props> {
  clearDelayTimeouts(): void;
  destroy(): void;
  disable(): void;
  enable(): void;
  hide(duration?: number): void;
  id: number;
  plugins: Plugin<TProps>[];
  popper: PopperElement<TProps>;
  popperChildren: PopperChildren;
  popperInstance: PopperInstance | null;
  props: TProps;
  reference: ReferenceElement<TProps>;
  setContent(content: Content): void;
  setProps(partialProps: Partial<TProps>): void;
  show(duration?: number): void;
  state: {
    currentPlacement: Placement | null;
    isEnabled: boolean;
    isVisible: boolean;
    isDestroyed: boolean;
    isMounted: boolean;
    isShown: boolean;
  };
}

export interface PopperChildren {
  tooltip: HTMLDivElement;
  content: HTMLDivElement;
  arrow: HTMLDivElement | null;
}

export interface TippyStatics {
  readonly currentInput: { isTouch: boolean };
  readonly defaultProps: DefaultProps;
  readonly version: string;
  setDefaultProps(partialProps: Partial<DefaultProps>): void;
}

export interface Tippy<TProps = Props> extends TippyStatics {
  (
    targets: SingleTarget,
    optionalProps?: Partial<TProps>,
    /** @deprecated use Props.plugins */
    plugins?: Plugin<TProps>[]
  ): Instance<TProps>;
}

export interface Tippy<TProps = Props> extends TippyStatics {
  (
    targets: MultipleTargets,
    optionalProps?: Partial<TProps>,
    /** @deprecated use Props.plugins */
    plugins?: Plugin<TProps>[]
  ): Instance<TProps>[];
}

declare const tippy: Tippy;

// =============================================================================
// Addon types
// =============================================================================
export interface DelegateInstance<TProps = Props> extends Instance<TProps> {
  destroy(shouldDestroyTargetInstances?: boolean): void;
}

export interface Delegate<TProps = Props> {
  (
    targets: SingleTarget,
    props: Partial<TProps> & { target: string },
    /** @deprecated use Props.plugins */
    plugins?: Plugin<TProps>[]
  ): DelegateInstance<TProps>;
}

export interface Delegate<TProps = Props> {
  (
    targets: MultipleTargets,
    props: Partial<TProps> & { target: string },
    /** @deprecated use Props.plugins */
    plugins?: Plugin<TProps>[]
  ): DelegateInstance<TProps>[];
}

export type CreateSingleton<TProps = Props> = (
  tippyInstances: Instance<TProps | Props>[],
  optionalProps?: Partial<TProps>,
  /** @deprecated use Props.plugins */
  plugins?: Plugin<TProps>[]
) => Instance<TProps>;

declare const delegate: Delegate;
declare const createSingleton: CreateSingleton;

// =============================================================================
// Plugin types
// =============================================================================
export interface Plugin<TProps = Props> {
  name?: string;
  defaultValue?: any;
  fn(instance: Instance<TProps>): Partial<LifecycleHooks<TProps>>;
}

export interface AnimateFillInstance extends Instance {
  popperChildren: PopperChildren & {
    backdrop: HTMLDivElement | null;
  };
}

export interface AnimateFill extends Plugin {
  name: 'animateFill';
  defaultValue: false;
  fn(instance: AnimateFillInstance): Partial<LifecycleHooks>;
}

export interface FollowCursor extends Plugin {
  name: 'followCursor';
  defaultValue: false;
}

export interface InlinePositioning extends Plugin {
  name: 'inlinePositioning';
  defaultValue: false;
}

export interface Sticky extends Plugin {
  name: 'sticky';
  defaultValue: false;
}

declare const animateFill: AnimateFill;
declare const followCursor: FollowCursor;
declare const inlinePositioning: InlinePositioning;
declare const sticky: Sticky;

// =============================================================================
// Misc types
// =============================================================================
export interface HideAllOptions {
  duration?: number;
  exclude?: Instance | ReferenceElement;
}

export type HideAll = (options?: HideAllOptions) => void;

declare const hideAll: HideAll;
declare const roundArrow: string;

// =============================================================================
// Deprecated types - these will be removed in the next major
// =============================================================================
/**
 * @deprecated use tippy.setDefaultProps({plugins: [...]});
 */
export type CreateTippyWithPlugins = (outerPlugins: Plugin[]) => Tippy;
declare const createTippyWithPlugins: CreateTippyWithPlugins;

/** @deprecated */
export interface AnimateFillProps {
  animateFill: Props['animateFill'];
}

/** @deprecated */
export interface FollowCursorProps {
  followCursor: Props['followCursor'];
}

/** @deprecated */
export interface InlinePositioningProps {
  inlinePositioning: Props['inlinePositioning'];
}

/** @deprecated */
export interface StickyProps {
  sticky: Props['sticky'];
}

export default tippy;
export {
  hideAll,
  createTippyWithPlugins,
  delegate,
  createSingleton,
  animateFill,
  followCursor,
  inlinePositioning,
  sticky,
  roundArrow
};
