/* eslint-disable @typescript-eslint/consistent-type-definitions */

import * as React from 'react';
import { createPopper as defaultCreatePopper, Options as PopperOptions, VirtualElement } from '../popper-core/popper';
import { useIsomorphicLayoutEffect } from '../../../../helpers/useIsomorphicLayout';

type $Shape<T extends object> = Partial<T>;

const isEqual = (a: any, b: any) => JSON.stringify(a) === JSON.stringify(b);

/**
 * Simple ponyfill for Object.fromEntries
 */
const fromEntries = (entries: [string, any][]) =>
  entries.reduce((acc: any, [key, value]) => {
    acc[key] = value;
    return acc;
  }, {});

type Options = $Shape<
  PopperOptions & {
    createPopper: typeof defaultCreatePopper;
  }
>;

type State = {
  styles: {
    [key: string]: $Shape<CSSStyleDeclaration>;
  };
  attributes: {
    [key: string]: {
      [key: string]: string;
    };
  };
};

const EMPTY_MODIFIERS: any = [];

export const usePopper = (
  referenceElement: (Element | VirtualElement) | null | undefined,
  popperElement: HTMLElement | null | undefined,
  options: Options = {}
) => {
  const prevOptions = React.useRef<PopperOptions | null | undefined>(null);

  const optionsWithDefaults = {
    onFirstUpdate: options.onFirstUpdate,
    placement: options.placement || 'bottom',
    strategy: options.strategy || 'absolute',
    modifiers: options.modifiers || EMPTY_MODIFIERS
  };

  const [state, setState] = React.useState<State>({
    styles: {
      popper: {
        position: optionsWithDefaults.strategy,
        left: '0',
        top: '0'
      }
    },
    attributes: {}
  });

  const updateStateModifier = React.useMemo(
    () => ({
      name: 'updateState',
      enabled: true,
      phase: 'write',
      // eslint-disable-next-line no-shadow
      fn: ({ state }: any) => {
        const elements = Object.keys(state.elements);

        setState({
          styles: fromEntries(elements.map(element => [element, state.styles[element] || {}])),
          attributes: fromEntries(elements.map(element => [element, state.attributes[element]]))
        });
      },
      requires: ['computeStyles']
    }),
    []
  );

  const popperOptions = React.useMemo(() => {
    const newOptions = {
      onFirstUpdate: optionsWithDefaults.onFirstUpdate,
      placement: optionsWithDefaults.placement,
      strategy: optionsWithDefaults.strategy,
      modifiers: [...optionsWithDefaults.modifiers, updateStateModifier, { name: 'applyStyles', enabled: false }]
    };

    if (isEqual(prevOptions.current, newOptions)) {
      return prevOptions.current || newOptions;
    } else {
      prevOptions.current = newOptions;
      return newOptions;
    }
  }, [
    optionsWithDefaults.onFirstUpdate,
    optionsWithDefaults.placement,
    optionsWithDefaults.strategy,
    optionsWithDefaults.modifiers,
    updateStateModifier
  ]);

  const popperInstanceRef = React.useRef<any>();

  useIsomorphicLayoutEffect(() => {
    if (popperInstanceRef && popperInstanceRef.current) {
      popperInstanceRef.current.setOptions(popperOptions);
    }
  }, [popperOptions]);

  useIsomorphicLayoutEffect(() => {
    if (referenceElement == null || popperElement == null) {
      return;
    }

    const createPopper = options.createPopper || defaultCreatePopper;
    const popperInstance = createPopper(referenceElement, popperElement, popperOptions);

    popperInstanceRef.current = popperInstance;

    return () => {
      popperInstance.destroy();
      popperInstanceRef.current = null;
    };
  }, [referenceElement, popperElement, options.createPopper]);

  return {
    state: popperInstanceRef.current ? popperInstanceRef.current.state : null,
    styles: state.styles,
    attributes: state.attributes,
    update: popperInstanceRef.current ? popperInstanceRef.current.update : null,
    forceUpdate: popperInstanceRef.current ? popperInstanceRef.current.forceUpdate : null
  };
};
