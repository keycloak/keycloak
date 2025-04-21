/*!
* tabbable 5.3.3
* @license MIT, https://github.com/focus-trap/tabbable/blob/master/LICENSE
*/
(function (global, factory) {
  typeof exports === 'object' && typeof module !== 'undefined' ? factory(exports) :
  typeof define === 'function' && define.amd ? define(['exports'], factory) :
  (global = typeof globalThis !== 'undefined' ? globalThis : global || self, (function () {
    var current = global.tabbable;
    var exports = global.tabbable = {};
    factory(exports);
    exports.noConflict = function () { global.tabbable = current; return exports; };
  })());
})(this, (function (exports) { 'use strict';

  var candidateSelectors = ['input', 'select', 'textarea', 'a[href]', 'button', '[tabindex]:not(slot)', 'audio[controls]', 'video[controls]', '[contenteditable]:not([contenteditable="false"])', 'details>summary:first-of-type', 'details'];
  var candidateSelector = /* #__PURE__ */candidateSelectors.join(',');
  var NoElement = typeof Element === 'undefined';
  var matches = NoElement ? function () {} : Element.prototype.matches || Element.prototype.msMatchesSelector || Element.prototype.webkitMatchesSelector;
  var getRootNode = !NoElement && Element.prototype.getRootNode ? function (element) {
    return element.getRootNode();
  } : function (element) {
    return element.ownerDocument;
  };
  /**
   * @param {Element} el container to check in
   * @param {boolean} includeContainer add container to check
   * @param {(node: Element) => boolean} filter filter candidates
   * @returns {Element[]}
   */

  var getCandidates = function getCandidates(el, includeContainer, filter) {
    var candidates = Array.prototype.slice.apply(el.querySelectorAll(candidateSelector));

    if (includeContainer && matches.call(el, candidateSelector)) {
      candidates.unshift(el);
    }

    candidates = candidates.filter(filter);
    return candidates;
  };
  /**
   * @callback GetShadowRoot
   * @param {Element} element to check for shadow root
   * @returns {ShadowRoot|boolean} ShadowRoot if available or boolean indicating if a shadowRoot is attached but not available.
   */

  /**
   * @callback ShadowRootFilter
   * @param {Element} shadowHostNode the element which contains shadow content
   * @returns {boolean} true if a shadow root could potentially contain valid candidates.
   */

  /**
   * @typedef {Object} CandidatesScope
   * @property {Element} scope contains inner candidates
   * @property {Element[]} candidates
   */

  /**
   * @typedef {Object} IterativeOptions
   * @property {GetShadowRoot|boolean} getShadowRoot true if shadow support is enabled; falsy if not;
   *  if a function, implies shadow support is enabled and either returns the shadow root of an element
   *  or a boolean stating if it has an undisclosed shadow root
   * @property {(node: Element) => boolean} filter filter candidates
   * @property {boolean} flatten if true then result will flatten any CandidatesScope into the returned list
   * @property {ShadowRootFilter} shadowRootFilter filter shadow roots;
   */

  /**
   * @param {Element[]} elements list of element containers to match candidates from
   * @param {boolean} includeContainer add container list to check
   * @param {IterativeOptions} options
   * @returns {Array.<Element|CandidatesScope>}
   */


  var getCandidatesIteratively = function getCandidatesIteratively(elements, includeContainer, options) {
    var candidates = [];
    var elementsToCheck = Array.from(elements);

    while (elementsToCheck.length) {
      var element = elementsToCheck.shift();

      if (element.tagName === 'SLOT') {
        // add shadow dom slot scope (slot itself cannot be focusable)
        var assigned = element.assignedElements();
        var content = assigned.length ? assigned : element.children;
        var nestedCandidates = getCandidatesIteratively(content, true, options);

        if (options.flatten) {
          candidates.push.apply(candidates, nestedCandidates);
        } else {
          candidates.push({
            scope: element,
            candidates: nestedCandidates
          });
        }
      } else {
        // check candidate element
        var validCandidate = matches.call(element, candidateSelector);

        if (validCandidate && options.filter(element) && (includeContainer || !elements.includes(element))) {
          candidates.push(element);
        } // iterate over shadow content if possible


        var shadowRoot = element.shadowRoot || // check for an undisclosed shadow
        typeof options.getShadowRoot === 'function' && options.getShadowRoot(element);
        var validShadowRoot = !options.shadowRootFilter || options.shadowRootFilter(element);

        if (shadowRoot && validShadowRoot) {
          // add shadow dom scope IIF a shadow root node was given; otherwise, an undisclosed
          //  shadow exists, so look at light dom children as fallback BUT create a scope for any
          //  child candidates found because they're likely slotted elements (elements that are
          //  children of the web component element (which has the shadow), in the light dom, but
          //  slotted somewhere _inside_ the undisclosed shadow) -- the scope is created below,
          //  _after_ we return from this recursive call
          var _nestedCandidates = getCandidatesIteratively(shadowRoot === true ? element.children : shadowRoot.children, true, options);

          if (options.flatten) {
            candidates.push.apply(candidates, _nestedCandidates);
          } else {
            candidates.push({
              scope: element,
              candidates: _nestedCandidates
            });
          }
        } else {
          // there's not shadow so just dig into the element's (light dom) children
          //  __without__ giving the element special scope treatment
          elementsToCheck.unshift.apply(elementsToCheck, element.children);
        }
      }
    }

    return candidates;
  };

  var getTabindex = function getTabindex(node, isScope) {
    if (node.tabIndex < 0) {
      // in Chrome, <details/>, <audio controls/> and <video controls/> elements get a default
      // `tabIndex` of -1 when the 'tabindex' attribute isn't specified in the DOM,
      // yet they are still part of the regular tab order; in FF, they get a default
      // `tabIndex` of 0; since Chrome still puts those elements in the regular tab
      // order, consider their tab index to be 0.
      // Also browsers do not return `tabIndex` correctly for contentEditable nodes;
      // so if they don't have a tabindex attribute specifically set, assume it's 0.
      //
      // isScope is positive for custom element with shadow root or slot that by default
      // have tabIndex -1, but need to be sorted by document order in order for their
      // content to be inserted in the correct position
      if ((isScope || /^(AUDIO|VIDEO|DETAILS)$/.test(node.tagName) || node.isContentEditable) && isNaN(parseInt(node.getAttribute('tabindex'), 10))) {
        return 0;
      }
    }

    return node.tabIndex;
  };

  var sortOrderedTabbables = function sortOrderedTabbables(a, b) {
    return a.tabIndex === b.tabIndex ? a.documentOrder - b.documentOrder : a.tabIndex - b.tabIndex;
  };

  var isInput = function isInput(node) {
    return node.tagName === 'INPUT';
  };

  var isHiddenInput = function isHiddenInput(node) {
    return isInput(node) && node.type === 'hidden';
  };

  var isDetailsWithSummary = function isDetailsWithSummary(node) {
    var r = node.tagName === 'DETAILS' && Array.prototype.slice.apply(node.children).some(function (child) {
      return child.tagName === 'SUMMARY';
    });
    return r;
  };

  var getCheckedRadio = function getCheckedRadio(nodes, form) {
    for (var i = 0; i < nodes.length; i++) {
      if (nodes[i].checked && nodes[i].form === form) {
        return nodes[i];
      }
    }
  };

  var isTabbableRadio = function isTabbableRadio(node) {
    if (!node.name) {
      return true;
    }

    var radioScope = node.form || getRootNode(node);

    var queryRadios = function queryRadios(name) {
      return radioScope.querySelectorAll('input[type="radio"][name="' + name + '"]');
    };

    var radioSet;

    if (typeof window !== 'undefined' && typeof window.CSS !== 'undefined' && typeof window.CSS.escape === 'function') {
      radioSet = queryRadios(window.CSS.escape(node.name));
    } else {
      try {
        radioSet = queryRadios(node.name);
      } catch (err) {
        // eslint-disable-next-line no-console
        console.error('Looks like you have a radio button with a name attribute containing invalid CSS selector characters and need the CSS.escape polyfill: %s', err.message);
        return false;
      }
    }

    var checked = getCheckedRadio(radioSet, node.form);
    return !checked || checked === node;
  };

  var isRadio = function isRadio(node) {
    return isInput(node) && node.type === 'radio';
  };

  var isNonTabbableRadio = function isNonTabbableRadio(node) {
    return isRadio(node) && !isTabbableRadio(node);
  };

  var isZeroArea = function isZeroArea(node) {
    var _node$getBoundingClie = node.getBoundingClientRect(),
        width = _node$getBoundingClie.width,
        height = _node$getBoundingClie.height;

    return width === 0 && height === 0;
  };

  var isHidden = function isHidden(node, _ref) {
    var displayCheck = _ref.displayCheck,
        getShadowRoot = _ref.getShadowRoot;

    // NOTE: visibility will be `undefined` if node is detached from the document
    //  (see notes about this further down), which means we will consider it visible
    //  (this is legacy behavior from a very long way back)
    // NOTE: we check this regardless of `displayCheck="none"` because this is a
    //  _visibility_ check, not a _display_ check
    if (getComputedStyle(node).visibility === 'hidden') {
      return true;
    }

    var isDirectSummary = matches.call(node, 'details>summary:first-of-type');
    var nodeUnderDetails = isDirectSummary ? node.parentElement : node;

    if (matches.call(nodeUnderDetails, 'details:not([open]) *')) {
      return true;
    } // The root node is the shadow root if the node is in a shadow DOM; some document otherwise
    //  (but NOT _the_ document; see second 'If' comment below for more).
    // If rootNode is shadow root, it'll have a host, which is the element to which the shadow
    //  is attached, and the one we need to check if it's in the document or not (because the
    //  shadow, and all nodes it contains, is never considered in the document since shadows
    //  behave like self-contained DOMs; but if the shadow's HOST, which is part of the document,
    //  is hidden, or is not in the document itself but is detached, it will affect the shadow's
    //  visibility, including all the nodes it contains). The host could be any normal node,
    //  or a custom element (i.e. web component). Either way, that's the one that is considered
    //  part of the document, not the shadow root, nor any of its children (i.e. the node being
    //  tested).
    // If rootNode is not a shadow root, it won't have a host, and so rootNode should be the
    //  document (per the docs) and while it's a Document-type object, that document does not
    //  appear to be the same as the node's `ownerDocument` for some reason, so it's safer
    //  to ignore the rootNode at this point, and use `node.ownerDocument`. Otherwise,
    //  using `rootNode.contains(node)` will _always_ be true we'll get false-positives when
    //  node is actually detached.


    var nodeRootHost = getRootNode(node).host;
    var nodeIsAttached = (nodeRootHost === null || nodeRootHost === void 0 ? void 0 : nodeRootHost.ownerDocument.contains(nodeRootHost)) || node.ownerDocument.contains(node);

    if (!displayCheck || displayCheck === 'full') {
      if (typeof getShadowRoot === 'function') {
        // figure out if we should consider the node to be in an undisclosed shadow and use the
        //  'non-zero-area' fallback
        var originalNode = node;

        while (node) {
          var parentElement = node.parentElement;
          var rootNode = getRootNode(node);

          if (parentElement && !parentElement.shadowRoot && getShadowRoot(parentElement) === true // check if there's an undisclosed shadow
          ) {
            // node has an undisclosed shadow which means we can only treat it as a black box, so we
            //  fall back to a non-zero-area test
            return isZeroArea(node);
          } else if (node.assignedSlot) {
            // iterate up slot
            node = node.assignedSlot;
          } else if (!parentElement && rootNode !== node.ownerDocument) {
            // cross shadow boundary
            node = rootNode.host;
          } else {
            // iterate up normal dom
            node = parentElement;
          }
        }

        node = originalNode;
      } // else, `getShadowRoot` might be true, but all that does is enable shadow DOM support
      //  (i.e. it does not also presume that all nodes might have undisclosed shadows); or
      //  it might be a falsy value, which means shadow DOM support is disabled
      // Since we didn't find it sitting in an undisclosed shadow (or shadows are disabled)
      //  now we can just test to see if it would normally be visible or not, provided it's
      //  attached to the main document.
      // NOTE: We must consider case where node is inside a shadow DOM and given directly to
      //  `isTabbable()` or `isFocusable()` -- regardless of `getShadowRoot` option setting.


      if (nodeIsAttached) {
        // this works wherever the node is: if there's at least one client rect, it's
        //  somehow displayed; it also covers the CSS 'display: contents' case where the
        //  node itself is hidden in place of its contents; and there's no need to search
        //  up the hierarchy either
        return !node.getClientRects().length;
      } // Else, the node isn't attached to the document, which means the `getClientRects()`
      //  API will __always__ return zero rects (this can happen, for example, if React
      //  is used to render nodes onto a detached tree, as confirmed in this thread:
      //  https://github.com/facebook/react/issues/9117#issuecomment-284228870)
      //
      // It also means that even window.getComputedStyle(node).display will return `undefined`
      //  because styles are only computed for nodes that are in the document.
      //
      // NOTE: THIS HAS BEEN THE CASE FOR YEARS. It is not new, nor is it caused by tabbable
      //  somehow. Though it was never stated officially, anyone who has ever used tabbable
      //  APIs on nodes in detached containers has actually implicitly used tabbable in what
      //  was later (as of v5.2.0 on Apr 9, 2021) called `displayCheck="none"` mode -- essentially
      //  considering __everything__ to be visible because of the innability to determine styles.

    } else if (displayCheck === 'non-zero-area') {
      // NOTE: Even though this tests that the node's client rect is non-zero to determine
      //  whether it's displayed, and that a detached node will __always__ have a zero-area
      //  client rect, we don't special-case for whether the node is attached or not. In
      //  this mode, we do want to consider nodes that have a zero area to be hidden at all
      //  times, and that includes attached or not.
      return isZeroArea(node);
    } // visible, as far as we can tell, or per current `displayCheck` mode


    return false;
  }; // form fields (nested) inside a disabled fieldset are not focusable/tabbable
  //  unless they are in the _first_ <legend> element of the top-most disabled
  //  fieldset


  var isDisabledFromFieldset = function isDisabledFromFieldset(node) {
    if (/^(INPUT|BUTTON|SELECT|TEXTAREA)$/.test(node.tagName)) {
      var parentNode = node.parentElement; // check if `node` is contained in a disabled <fieldset>

      while (parentNode) {
        if (parentNode.tagName === 'FIELDSET' && parentNode.disabled) {
          // look for the first <legend> among the children of the disabled <fieldset>
          for (var i = 0; i < parentNode.children.length; i++) {
            var child = parentNode.children.item(i); // when the first <legend> (in document order) is found

            if (child.tagName === 'LEGEND') {
              // if its parent <fieldset> is not nested in another disabled <fieldset>,
              // return whether `node` is a descendant of its first <legend>
              return matches.call(parentNode, 'fieldset[disabled] *') ? true : !child.contains(node);
            }
          } // the disabled <fieldset> containing `node` has no <legend>


          return true;
        }

        parentNode = parentNode.parentElement;
      }
    } // else, node's tabbable/focusable state should not be affected by a fieldset's
    //  enabled/disabled state


    return false;
  };

  var isNodeMatchingSelectorFocusable = function isNodeMatchingSelectorFocusable(options, node) {
    if (node.disabled || isHiddenInput(node) || isHidden(node, options) || // For a details element with a summary, the summary element gets the focus
    isDetailsWithSummary(node) || isDisabledFromFieldset(node)) {
      return false;
    }

    return true;
  };

  var isNodeMatchingSelectorTabbable = function isNodeMatchingSelectorTabbable(options, node) {
    if (isNonTabbableRadio(node) || getTabindex(node) < 0 || !isNodeMatchingSelectorFocusable(options, node)) {
      return false;
    }

    return true;
  };

  var isValidShadowRootTabbable = function isValidShadowRootTabbable(shadowHostNode) {
    var tabIndex = parseInt(shadowHostNode.getAttribute('tabindex'), 10);

    if (isNaN(tabIndex) || tabIndex >= 0) {
      return true;
    } // If a custom element has an explicit negative tabindex,
    // browsers will not allow tab targeting said element's children.


    return false;
  };
  /**
   * @param {Array.<Element|CandidatesScope>} candidates
   * @returns Element[]
   */


  var sortByOrder = function sortByOrder(candidates) {
    var regularTabbables = [];
    var orderedTabbables = [];
    candidates.forEach(function (item, i) {
      var isScope = !!item.scope;
      var element = isScope ? item.scope : item;
      var candidateTabindex = getTabindex(element, isScope);
      var elements = isScope ? sortByOrder(item.candidates) : element;

      if (candidateTabindex === 0) {
        isScope ? regularTabbables.push.apply(regularTabbables, elements) : regularTabbables.push(element);
      } else {
        orderedTabbables.push({
          documentOrder: i,
          tabIndex: candidateTabindex,
          item: item,
          isScope: isScope,
          content: elements
        });
      }
    });
    return orderedTabbables.sort(sortOrderedTabbables).reduce(function (acc, sortable) {
      sortable.isScope ? acc.push.apply(acc, sortable.content) : acc.push(sortable.content);
      return acc;
    }, []).concat(regularTabbables);
  };

  var tabbable = function tabbable(el, options) {
    options = options || {};
    var candidates;

    if (options.getShadowRoot) {
      candidates = getCandidatesIteratively([el], options.includeContainer, {
        filter: isNodeMatchingSelectorTabbable.bind(null, options),
        flatten: false,
        getShadowRoot: options.getShadowRoot,
        shadowRootFilter: isValidShadowRootTabbable
      });
    } else {
      candidates = getCandidates(el, options.includeContainer, isNodeMatchingSelectorTabbable.bind(null, options));
    }

    return sortByOrder(candidates);
  };

  var focusable = function focusable(el, options) {
    options = options || {};
    var candidates;

    if (options.getShadowRoot) {
      candidates = getCandidatesIteratively([el], options.includeContainer, {
        filter: isNodeMatchingSelectorFocusable.bind(null, options),
        flatten: true,
        getShadowRoot: options.getShadowRoot
      });
    } else {
      candidates = getCandidates(el, options.includeContainer, isNodeMatchingSelectorFocusable.bind(null, options));
    }

    return candidates;
  };

  var isTabbable = function isTabbable(node, options) {
    options = options || {};

    if (!node) {
      throw new Error('No node provided');
    }

    if (matches.call(node, candidateSelector) === false) {
      return false;
    }

    return isNodeMatchingSelectorTabbable(options, node);
  };

  var focusableCandidateSelector = /* #__PURE__ */candidateSelectors.concat('iframe').join(',');

  var isFocusable = function isFocusable(node, options) {
    options = options || {};

    if (!node) {
      throw new Error('No node provided');
    }

    if (matches.call(node, focusableCandidateSelector) === false) {
      return false;
    }

    return isNodeMatchingSelectorFocusable(options, node);
  };

  exports.focusable = focusable;
  exports.isFocusable = isFocusable;
  exports.isTabbable = isTabbable;
  exports.tabbable = tabbable;

  Object.defineProperty(exports, '__esModule', { value: true });

}));
//# sourceMappingURL=index.umd.js.map
