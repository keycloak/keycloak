/**
 * Actions represent the type of change to a location value.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#action
 */
var Action;

(function (Action) {
  /**
   * A POP indicates a change to an arbitrary index in the history stack, such
   * as a back or forward navigation. It does not describe the direction of the
   * navigation, only that the current index changed.
   *
   * Note: This is the default action for newly created history objects.
   */
  Action["Pop"] = "POP";
  /**
   * A PUSH indicates a new entry being added to the history stack, such as when
   * a link is clicked and a new page loads. When this happens, all subsequent
   * entries in the stack are lost.
   */

  Action["Push"] = "PUSH";
  /**
   * A REPLACE indicates the entry at the current index in the history stack
   * being replaced by a new one.
   */

  Action["Replace"] = "REPLACE";
})(Action || (Action = {}));

const readOnly = obj => Object.freeze(obj) ;

function warning(cond, message) {
  if (!cond) {
    // eslint-disable-next-line no-console
    if (typeof console !== 'undefined') console.warn(message);

    try {
      // Welcome to debugging history!
      //
      // This error is thrown as a convenience so you can more easily
      // find the source for a warning that appears in the console by
      // enabling "pause on exceptions" in your JavaScript debugger.
      throw new Error(message); // eslint-disable-next-line no-empty
    } catch (e) {}
  }
}

const BeforeUnloadEventType = 'beforeunload';
const HashChangeEventType = 'hashchange';
const PopStateEventType = 'popstate';
/**
 * Browser history stores the location in regular URLs. This is the standard for
 * most web apps, but it requires some configuration on the server to ensure you
 * serve the same app at multiple URLs.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#createbrowserhistory
 */

function createBrowserHistory(options = {}) {
  let {
    window = document.defaultView
  } = options;
  let globalHistory = window.history;

  function getIndexAndLocation() {
    let {
      pathname,
      search,
      hash
    } = window.location;
    let state = globalHistory.state || {};
    return [state.idx, readOnly({
      pathname,
      search,
      hash,
      state: state.usr || null,
      key: state.key || 'default'
    })];
  }

  let blockedPopTx = null;

  function handlePop() {
    if (blockedPopTx) {
      blockers.call(blockedPopTx);
      blockedPopTx = null;
    } else {
      let nextAction = Action.Pop;
      let [nextIndex, nextLocation] = getIndexAndLocation();

      if (blockers.length) {
        if (nextIndex != null) {
          let delta = index - nextIndex;

          if (delta) {
            // Revert the POP
            blockedPopTx = {
              action: nextAction,
              location: nextLocation,

              retry() {
                go(delta * -1);
              }

            };
            go(delta);
          }
        } else {
          // Trying to POP to a location with no index. We did not create
          // this location, so we can't effectively block the navigation.
          warning(false, // TODO: Write up a doc that explains our blocking strategy in
          // detail and link to it here so people can understand better what
          // is going on and how to avoid it.
          `You are trying to block a POP navigation to a location that was not ` + `created by the history library. The block will fail silently in ` + `production, but in general you should do all navigation with the ` + `history library (instead of using window.history.pushState directly) ` + `to avoid this situation.`) ;
        }
      } else {
        applyTx(nextAction);
      }
    }
  }

  window.addEventListener(PopStateEventType, handlePop);
  let action = Action.Pop;
  let [index, location] = getIndexAndLocation();
  let listeners = createEvents();
  let blockers = createEvents();

  if (index == null) {
    index = 0;
    globalHistory.replaceState(Object.assign(Object.assign({}, globalHistory.state), {
      idx: index
    }), '');
  }

  function createHref(to) {
    return typeof to === 'string' ? to : createPath(to);
  } // state defaults to `null` because `window.history.state` does


  function getNextLocation(to, state = null) {
    return readOnly(Object.assign(Object.assign({
      pathname: location.pathname,
      hash: '',
      search: ''
    }, typeof to === 'string' ? parsePath(to) : to), {
      state,
      key: createKey()
    }));
  }

  function getHistoryStateAndUrl(nextLocation, index) {
    return [{
      usr: nextLocation.state,
      key: nextLocation.key,
      idx: index
    }, createHref(nextLocation)];
  }

  function allowTx(action, location, retry) {
    return !blockers.length || (blockers.call({
      action,
      location,
      retry
    }), false);
  }

  function applyTx(nextAction) {
    action = nextAction;
    [index, location] = getIndexAndLocation();
    listeners.call({
      action,
      location
    });
  }

  function push(to, state) {
    let nextAction = Action.Push;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      push(to, state);
    }

    if (allowTx(nextAction, nextLocation, retry)) {
      let [historyState, url] = getHistoryStateAndUrl(nextLocation, index + 1); // TODO: Support forced reloading
      // try...catch because iOS limits us to 100 pushState calls :/

      try {
        globalHistory.pushState(historyState, '', url);
      } catch (error) {
        // They are going to lose state here, but there is no real
        // way to warn them about it since the page will refresh...
        window.location.assign(url);
      }

      applyTx(nextAction);
    }
  }

  function replace(to, state) {
    let nextAction = Action.Replace;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      replace(to, state);
    }

    if (allowTx(nextAction, nextLocation, retry)) {
      let [historyState, url] = getHistoryStateAndUrl(nextLocation, index); // TODO: Support forced reloading

      globalHistory.replaceState(historyState, '', url);
      applyTx(nextAction);
    }
  }

  function go(delta) {
    globalHistory.go(delta);
  }

  let history = {
    get action() {
      return action;
    },

    get location() {
      return location;
    },

    createHref,
    push,
    replace,
    go,

    back() {
      go(-1);
    },

    forward() {
      go(1);
    },

    listen(listener) {
      return listeners.push(listener);
    },

    block(blocker) {
      let unblock = blockers.push(blocker);

      if (blockers.length === 1) {
        window.addEventListener(BeforeUnloadEventType, promptBeforeUnload);
      }

      return function () {
        unblock(); // Remove the beforeunload listener so the document may
        // still be salvageable in the pagehide event.
        // See https://html.spec.whatwg.org/#unloading-documents

        if (!blockers.length) {
          window.removeEventListener(BeforeUnloadEventType, promptBeforeUnload);
        }
      };
    }

  };
  return history;
}
/**
 * Hash history stores the location in window.location.hash. This makes it ideal
 * for situations where you don't want to send the location to the server for
 * some reason, either because you do cannot configure it or the URL space is
 * reserved for something else.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#createhashhistory
 */

function createHashHistory(options = {}) {
  let {
    window = document.defaultView
  } = options;
  let globalHistory = window.history;

  function getIndexAndLocation() {
    let {
      pathname = '/',
      search = '',
      hash = ''
    } = parsePath(window.location.hash.substr(1));
    let state = globalHistory.state || {};
    return [state.idx, readOnly({
      pathname,
      search,
      hash,
      state: state.usr || null,
      key: state.key || 'default'
    })];
  }

  let blockedPopTx = null;

  function handlePop() {
    if (blockedPopTx) {
      blockers.call(blockedPopTx);
      blockedPopTx = null;
    } else {
      let nextAction = Action.Pop;
      let [nextIndex, nextLocation] = getIndexAndLocation();

      if (blockers.length) {
        if (nextIndex != null) {
          let delta = index - nextIndex;

          if (delta) {
            // Revert the POP
            blockedPopTx = {
              action: nextAction,
              location: nextLocation,

              retry() {
                go(delta * -1);
              }

            };
            go(delta);
          }
        } else {
          // Trying to POP to a location with no index. We did not create
          // this location, so we can't effectively block the navigation.
          warning(false, // TODO: Write up a doc that explains our blocking strategy in
          // detail and link to it here so people can understand better
          // what is going on and how to avoid it.
          `You are trying to block a POP navigation to a location that was not ` + `created by the history library. The block will fail silently in ` + `production, but in general you should do all navigation with the ` + `history library (instead of using window.history.pushState directly) ` + `to avoid this situation.`) ;
        }
      } else {
        applyTx(nextAction);
      }
    }
  }

  window.addEventListener(PopStateEventType, handlePop); // popstate does not fire on hashchange in IE 11 and old (trident) Edge
  // https://developer.mozilla.org/de/docs/Web/API/Window/popstate_event

  window.addEventListener(HashChangeEventType, () => {
    let [, nextLocation] = getIndexAndLocation(); // Ignore extraneous hashchange events.

    if (createPath(nextLocation) !== createPath(location)) {
      handlePop();
    }
  });
  let action = Action.Pop;
  let [index, location] = getIndexAndLocation();
  let listeners = createEvents();
  let blockers = createEvents();

  if (index == null) {
    index = 0;
    globalHistory.replaceState(Object.assign(Object.assign({}, globalHistory.state), {
      idx: index
    }), '');
  }

  function getBaseHref() {
    let base = document.querySelector('base');
    let href = '';

    if (base && base.getAttribute('href')) {
      let url = window.location.href;
      let hashIndex = url.indexOf('#');
      href = hashIndex === -1 ? url : url.slice(0, hashIndex);
    }

    return href;
  }

  function createHref(to) {
    return getBaseHref() + '#' + (typeof to === 'string' ? to : createPath(to));
  }

  function getNextLocation(to, state = null) {
    return readOnly(Object.assign(Object.assign({
      pathname: location.pathname,
      hash: '',
      search: ''
    }, typeof to === 'string' ? parsePath(to) : to), {
      state,
      key: createKey()
    }));
  }

  function getHistoryStateAndUrl(nextLocation, index) {
    return [{
      usr: nextLocation.state,
      key: nextLocation.key,
      idx: index
    }, createHref(nextLocation)];
  }

  function allowTx(action, location, retry) {
    return !blockers.length || (blockers.call({
      action,
      location,
      retry
    }), false);
  }

  function applyTx(nextAction) {
    action = nextAction;
    [index, location] = getIndexAndLocation();
    listeners.call({
      action,
      location
    });
  }

  function push(to, state) {
    let nextAction = Action.Push;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      push(to, state);
    }

    warning(nextLocation.pathname.charAt(0) === '/', `Relative pathnames are not supported in hash history.push(${JSON.stringify(to)})`) ;

    if (allowTx(nextAction, nextLocation, retry)) {
      let [historyState, url] = getHistoryStateAndUrl(nextLocation, index + 1); // TODO: Support forced reloading
      // try...catch because iOS limits us to 100 pushState calls :/

      try {
        globalHistory.pushState(historyState, '', url);
      } catch (error) {
        // They are going to lose state here, but there is no real
        // way to warn them about it since the page will refresh...
        window.location.assign(url);
      }

      applyTx(nextAction);
    }
  }

  function replace(to, state) {
    let nextAction = Action.Replace;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      replace(to, state);
    }

    warning(nextLocation.pathname.charAt(0) === '/', `Relative pathnames are not supported in hash history.replace(${JSON.stringify(to)})`) ;

    if (allowTx(nextAction, nextLocation, retry)) {
      let [historyState, url] = getHistoryStateAndUrl(nextLocation, index); // TODO: Support forced reloading

      globalHistory.replaceState(historyState, '', url);
      applyTx(nextAction);
    }
  }

  function go(delta) {
    globalHistory.go(delta);
  }

  let history = {
    get action() {
      return action;
    },

    get location() {
      return location;
    },

    createHref,
    push,
    replace,
    go,

    back() {
      go(-1);
    },

    forward() {
      go(1);
    },

    listen(listener) {
      return listeners.push(listener);
    },

    block(blocker) {
      let unblock = blockers.push(blocker);

      if (blockers.length === 1) {
        window.addEventListener(BeforeUnloadEventType, promptBeforeUnload);
      }

      return function () {
        unblock(); // Remove the beforeunload listener so the document may
        // still be salvageable in the pagehide event.
        // See https://html.spec.whatwg.org/#unloading-documents

        if (!blockers.length) {
          window.removeEventListener(BeforeUnloadEventType, promptBeforeUnload);
        }
      };
    }

  };
  return history;
}
/**
 * Memory history stores the current location in memory. It is designed for use
 * in stateful non-browser environments like tests and React Native.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#creatememoryhistory
 */

function createMemoryHistory(options = {}) {
  let {
    initialEntries = ['/'],
    initialIndex
  } = options;
  let entries = initialEntries.map(entry => {
    let location = readOnly(Object.assign({
      pathname: '/',
      search: '',
      hash: '',
      state: null,
      key: createKey()
    }, typeof entry === 'string' ? parsePath(entry) : entry));
    warning(location.pathname.charAt(0) === '/', `Relative pathnames are not supported in createMemoryHistory({ initialEntries }) (invalid entry: ${JSON.stringify(entry)})`) ;
    return location;
  });
  let index = clamp(initialIndex == null ? entries.length - 1 : initialIndex, 0, entries.length - 1);
  let action = Action.Pop;
  let location = entries[index];
  let listeners = createEvents();
  let blockers = createEvents();

  function createHref(to) {
    return typeof to === 'string' ? to : createPath(to);
  }

  function getNextLocation(to, state = null) {
    return readOnly(Object.assign(Object.assign({
      pathname: location.pathname,
      search: '',
      hash: ''
    }, typeof to === 'string' ? parsePath(to) : to), {
      state,
      key: createKey()
    }));
  }

  function allowTx(action, location, retry) {
    return !blockers.length || (blockers.call({
      action,
      location,
      retry
    }), false);
  }

  function applyTx(nextAction, nextLocation) {
    action = nextAction;
    location = nextLocation;
    listeners.call({
      action,
      location
    });
  }

  function push(to, state) {
    let nextAction = Action.Push;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      push(to, state);
    }

    warning(location.pathname.charAt(0) === '/', `Relative pathnames are not supported in memory history.push(${JSON.stringify(to)})`) ;

    if (allowTx(nextAction, nextLocation, retry)) {
      index += 1;
      entries.splice(index, entries.length, nextLocation);
      applyTx(nextAction, nextLocation);
    }
  }

  function replace(to, state) {
    let nextAction = Action.Replace;
    let nextLocation = getNextLocation(to, state);

    function retry() {
      replace(to, state);
    }

    warning(location.pathname.charAt(0) === '/', `Relative pathnames are not supported in memory history.replace(${JSON.stringify(to)})`) ;

    if (allowTx(nextAction, nextLocation, retry)) {
      entries[index] = nextLocation;
      applyTx(nextAction, nextLocation);
    }
  }

  function go(delta) {
    let nextIndex = clamp(index + delta, 0, entries.length - 1);
    let nextAction = Action.Pop;
    let nextLocation = entries[nextIndex];

    function retry() {
      go(delta);
    }

    if (allowTx(nextAction, nextLocation, retry)) {
      index = nextIndex;
      applyTx(nextAction, nextLocation);
    }
  }

  let history = {
    get index() {
      return index;
    },

    get action() {
      return action;
    },

    get location() {
      return location;
    },

    createHref,
    push,
    replace,
    go,

    back() {
      go(-1);
    },

    forward() {
      go(1);
    },

    listen(listener) {
      return listeners.push(listener);
    },

    block(blocker) {
      return blockers.push(blocker);
    }

  };
  return history;
} ////////////////////////////////////////////////////////////////////////////////
// UTILS
////////////////////////////////////////////////////////////////////////////////

function clamp(n, lowerBound, upperBound) {
  return Math.min(Math.max(n, lowerBound), upperBound);
}

function promptBeforeUnload(event) {
  // Cancel the event.
  event.preventDefault(); // Chrome (and legacy IE) requires returnValue to be set.

  event.returnValue = '';
}

function createEvents() {
  let handlers = [];
  return {
    get length() {
      return handlers.length;
    },

    push(fn) {
      handlers.push(fn);
      return function () {
        handlers = handlers.filter(handler => handler !== fn);
      };
    },

    call(arg) {
      handlers.forEach(fn => fn && fn(arg));
    }

  };
}

function createKey() {
  return Math.random().toString(36).substr(2, 8);
}
/**
 * Creates a string URL path from the given pathname, search, and hash components.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#createpath
 */


function createPath({
  pathname = '/',
  search = '',
  hash = ''
}) {
  if (search && search !== '?') pathname += search.charAt(0) === '?' ? search : '?' + search;
  if (hash && hash !== '#') pathname += hash.charAt(0) === '#' ? hash : '#' + hash;
  return pathname;
}
/**
 * Parses a string URL path into its separate pathname, search, and hash components.
 *
 * @see https://github.com/remix-run/history/tree/main/docs/api-reference.md#parsepath
 */

function parsePath(path) {
  let parsedPath = {};

  if (path) {
    let hashIndex = path.indexOf('#');

    if (hashIndex >= 0) {
      parsedPath.hash = path.substr(hashIndex);
      path = path.substr(0, hashIndex);
    }

    let searchIndex = path.indexOf('?');

    if (searchIndex >= 0) {
      parsedPath.search = path.substr(searchIndex);
      path = path.substr(0, searchIndex);
    }

    if (path) {
      parsedPath.pathname = path;
    }
  }

  return parsedPath;
}

export { Action, createBrowserHistory, createHashHistory, createMemoryHistory, createPath, parsePath };
//# sourceMappingURL=history.development.js.map
