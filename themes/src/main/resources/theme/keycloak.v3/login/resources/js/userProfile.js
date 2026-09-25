// @ts-check
/**
 * @typedef {Object} AnnotationDescriptor
 * @property {string} name - The name of the field to register (e.g. `numberFormat`).
 * @property {(element: HTMLElement) => (() => void) | void} onAdd - The function to call when a new element is added to the DOM.
 */

const observer = new MutationObserver(onMutate);
observer.observe(document.body, { childList: true, subtree: true });

/** @type {AnnotationDescriptor[]} */
const descriptors = [];

/** @type {WeakMap<HTMLElement, () => void>} */
const cleanupFunctions = new WeakMap();

/**
 * @param {AnnotationDescriptor} descriptor
 */
export function registerElementAnnotatedBy(descriptor) {
  descriptors.push(descriptor);

  document.querySelectorAll(`[data-${descriptor.name}]`).forEach((element) => {
    if (element instanceof HTMLElement) {
      handleNewElement(element, descriptor);
    }
  });
}

/**
 * @type {MutationCallback}
 */
function onMutate(mutations) {
  const removedNodes = mutations.flatMap((mutation) => Array.from(mutation.removedNodes));

  for (const node of removedNodes) {
    if (!(node instanceof HTMLElement)) {
      continue;
    }

    const handleRemovedElement = cleanupFunctions.get(node);

    if (handleRemovedElement) {
      handleRemovedElement();
    }

    cleanupFunctions.delete(node);
  }

  const addedNodes = mutations.flatMap((mutation) => Array.from(mutation.addedNodes));

  for (const descriptor of descriptors) {
    for (const node of addedNodes) {
      const input = node.querySelector('input');
      if (input.hasAttribute(`data-${descriptor.name}`)) {
        handleNewElement(input, descriptor);
      }
    }
  }
}

/**
 * @param {HTMLElement} element
 * @param {AnnotationDescriptor} descriptor
 */
function handleNewElement(element, descriptor) {
  const cleanup = descriptor.onAdd(element);

  if (cleanup) {
    cleanupFunctions.set(element, cleanup);
  }
}
