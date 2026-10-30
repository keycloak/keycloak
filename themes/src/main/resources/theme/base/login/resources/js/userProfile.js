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

/** @type {WeakMap<HTMLElement, Map<AnnotationDescriptor, (() => void) | void>>} */
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

    for (const element of [node, ...node.querySelectorAll('*')]) {
      if (!(element instanceof HTMLElement)) {
        continue;
      }

      const cleanups = cleanupFunctions.get(element);

      if (cleanups) {
        for (const cleanup of cleanups.values()) {
          if (cleanup) {
            cleanup();
          }
        }
      }

      cleanupFunctions.delete(element);
    }
  }

  const addedNodes = mutations.flatMap((mutation) => Array.from(mutation.addedNodes));

  for (const descriptor of descriptors) {
    const selector = `[data-${descriptor.name}]`;

    for (const node of addedNodes) {
      if (!(node instanceof HTMLElement)) {
        continue;
      }

      const elements = node.matches(selector) ? [node] : [];
      elements.push(...node.querySelectorAll(selector));

      for (const element of elements) {
        if (element instanceof HTMLElement) {
          handleNewElement(element, descriptor);
        }
      }
    }
  }
}

/**
 * @param {HTMLElement} element
 * @param {AnnotationDescriptor} descriptor
 */
function handleNewElement(element, descriptor) {
  let cleanups = cleanupFunctions.get(element);

  if (!cleanups) {
    cleanups = new Map();
    cleanupFunctions.set(element, cleanups);
  }

  if (!cleanups.has(descriptor)) {
    cleanups.set(descriptor, descriptor.onAdd(element));
  }
}
