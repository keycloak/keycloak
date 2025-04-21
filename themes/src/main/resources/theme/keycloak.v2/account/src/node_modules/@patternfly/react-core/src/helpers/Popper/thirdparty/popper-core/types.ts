// @ts-nocheck

/* eslint-disable import/no-unused-modules */
import { Placement, ModifierPhases } from './enums';

import { PopperOffsetsModifier } from './modifiers/popperOffsets';
import { FlipModifier } from './modifiers/flip';
import { HideModifier } from './modifiers/hide';
import { OffsetModifier } from './modifiers/offset';
import { EventListenersModifier } from './modifiers/eventListeners';
import { ComputeStylesModifier } from './modifiers/computeStyles';
import { ArrowModifier } from './modifiers/arrow';
import { PreventOverflowModifier } from './modifiers/preventOverflow';
import { ApplyStylesModifier } from './modifiers/applyStyles';

export interface Obj {
  [key: string]: any;
}

export type VisualViewport = EventTarget & {
  width: number;
  height: number;
  offsetLeft: number;
  offsetTop: number;
  scale: number;
};

// This is a limited subset of the Window object, Flow doesn't provide one
// so we define our own, with just the properties we need
export interface Window {
  innerHeight: number;
  offsetHeight: number;
  innerWidth: number;
  offsetWidth: number;
  pageXOffset: number;
  pageYOffset: number;
  getComputedStyle: typeof getComputedStyle;
  addEventListener(type: any, listener: any, optionsOrUseCapture?: any): void;
  removeEventListener(type: any, listener: any, optionsOrUseCapture?: any): void;
  Element: Element;
  HTMLElement: HTMLElement;
  Node: Node;
  toString(): '[object Window]';
  devicePixelRatio: number;
  visualViewport?: VisualViewport;
}

export interface Rect {
  width: number;
  height: number;
  x: number;
  y: number;
}

export interface Offsets {
  y: number;
  x: number;
}

export type PositioningStrategy = 'absolute' | 'fixed';

export interface StateRects {
  reference: Rect;
  popper: Rect;
}

export interface StateOffsets {
  popper: Offsets;
  arrow?: Offsets;
}

/* :: type OffsetData = { [Placement]: Offsets }; */

/* ;; type OffsetData = { [key in Placement]: Offsets } */

export interface State {
  elements: {
    reference: Element | VirtualElement;
    popper: HTMLElement;
    arrow?: HTMLElement;
  };
  options: OptionsGeneric<any>;
  placement: Placement;
  strategy: PositioningStrategy;
  orderedModifiers: Modifier<any, any>[];
  rects: StateRects;
  scrollParents: {
    reference: (Element | Window | VisualViewport)[];
    popper: (Element | Window | VisualViewport)[];
  };
  styles: {
    [key: string]: Partial<CSSStyleDeclaration>;
  };
  attributes: {
    [key: string]: {
      [key: string]: string | boolean;
    };
  };
  modifiersData: {
    arrow?: {
      x?: number;
      y?: number;
      centerOffset: number;
    };
    hide?: {
      isReferenceHidden: boolean;
      hasPopperEscaped: boolean;
      referenceClippingOffsets: SideObject;
      popperEscapeOffsets: SideObject;
    };
    offset?: any; // OffsetData;
    preventOverflow?: Offsets;
    popperOffsets?: Offsets;

    [key: string]: any;
  };
  reset: boolean;
}

export interface Instance {
  state: State;
  destroy: () => void;
  forceUpdate: () => void;
  update: () => Promise<Partial<State>>;
  setOptions: (options: Partial<OptionsGeneric<any>>) => Promise<Partial<State>>;
}

export interface ModifierArguments<Options extends Obj> {
  state: State;
  instance: Instance;
  options: Partial<Options>;
  name: string;
}
export interface Modifier<Name, Options> {
  name: Name;
  enabled: boolean;
  phase: ModifierPhases;
  requires?: string[];
  requiresIfExists?: string[];
  fn: (arg0: ModifierArguments<Options>) => State | void;
  effect?: (arg0: ModifierArguments<Options>) => () => void | void;
  options?: Partial<Options>;
  data?: Obj;
}

export type StrictModifiers =
  | Partial<OffsetModifier>
  | Partial<ApplyStylesModifier>
  | Partial<ArrowModifier>
  | Partial<HideModifier>
  | Partial<ComputeStylesModifier>
  | Partial<EventListenersModifier>
  | Partial<FlipModifier>
  | Partial<PreventOverflowModifier>
  | Partial<PopperOffsetsModifier>;

export interface EventListeners {
  scroll: boolean;
  resize: boolean;
}

export interface Options {
  placement: Placement;
  modifiers: Partial<Modifier<any, any>>[];
  strategy: PositioningStrategy;
  onFirstUpdate?: (arg0: Partial<State>) => void;
}

export interface OptionsGeneric<TModifier> {
  placement: Placement;
  modifiers: TModifier[];
  strategy: PositioningStrategy;
  onFirstUpdate?: (arg0: Partial<State>) => void;
}

export type UpdateCallback = (arg0: State) => void;

export interface ClientRectObject {
  x: number;
  y: number;
  top: number;
  left: number;
  right: number;
  bottom: number;
  width: number;
  height: number;
}

export interface SideObject {
  top: number;
  left: number;
  right: number;
  bottom: number;
}

export type Padding = number | Partial<SideObject>;

export interface VirtualElement {
  getBoundingClientRect: () => ClientRect | DOMRect;
  contextElement?: Element;
}
