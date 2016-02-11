'use strict';var collection_1 = require('angular2/src/facade/collection');
var lang_1 = require('angular2/src/facade/lang');
var async_1 = require('angular2/src/facade/async');
var profile_1 = require('../profile/profile');
/**
 * Stores error information; delivered via [NgZone.onError] stream.
 */
var NgZoneError = (function () {
    function NgZoneError(error, stackTrace) {
        this.error = error;
        this.stackTrace = stackTrace;
    }
    return NgZoneError;
})();
exports.NgZoneError = NgZoneError;
/**
 * An injectable service for executing work inside or outside of the Angular zone.
 *
 * The most common use of this service is to optimize performance when starting a work consisting of
 * one or more asynchronous tasks that don't require UI updates or error handling to be handled by
 * Angular. Such tasks can be kicked off via {@link #runOutsideAngular} and if needed, these tasks
 * can reenter the Angular zone via {@link #run}.
 *
 * <!-- TODO: add/fix links to:
 *   - docs explaining zones and the use of zones in Angular and change-detection
 *   - link to runOutsideAngular/run (throughout this file!)
 *   -->
 *
 * ### Example ([live demo](http://plnkr.co/edit/lY9m8HLy7z06vDoUaSN2?p=preview))
 * ```
 * import {Component, View, NgZone} from 'angular2/core';
 * import {NgIf} from 'angular2/common';
 *
 * @Component({
 *   selector: 'ng-zone-demo'.
 *   template: `
 *     <h2>Demo: NgZone</h2>
 *
 *     <p>Progress: {{progress}}%</p>
 *     <p *ngIf="progress >= 100">Done processing {{label}} of Angular zone!</p>
 *
 *     <button (click)="processWithinAngularZone()">Process within Angular zone</button>
 *     <button (click)="processOutsideOfAngularZone()">Process outside of Angular zone</button>
 *   `,
 *   directives: [NgIf]
 * })
 * export class NgZoneDemo {
 *   progress: number = 0;
 *   label: string;
 *
 *   constructor(private _ngZone: NgZone) {}
 *
 *   // Loop inside the Angular zone
 *   // so the UI DOES refresh after each setTimeout cycle
 *   processWithinAngularZone() {
 *     this.label = 'inside';
 *     this.progress = 0;
 *     this._increaseProgress(() => console.log('Inside Done!'));
 *   }
 *
 *   // Loop outside of the Angular zone
 *   // so the UI DOES NOT refresh after each setTimeout cycle
 *   processOutsideOfAngularZone() {
 *     this.label = 'outside';
 *     this.progress = 0;
 *     this._ngZone.runOutsideAngular(() => {
 *       this._increaseProgress(() => {
 *       // reenter the Angular zone and display done
 *       this._ngZone.run(() => {console.log('Outside Done!') });
 *     }}));
 *   }
 *
 *
 *   _increaseProgress(doneCallback: () => void) {
 *     this.progress += 1;
 *     console.log(`Current progress: ${this.progress}%`);
 *
 *     if (this.progress < 100) {
 *       window.setTimeout(() => this._increaseProgress(doneCallback)), 10)
 *     } else {
 *       doneCallback();
 *     }
 *   }
 * }
 * ```
 */
var NgZone = (function () {
    /**
     * @param {bool} enableLongStackTrace whether to enable long stack trace. They should only be
     *               enabled in development mode as they significantly impact perf.
     */
    function NgZone(_a) {
        var enableLongStackTrace = _a.enableLongStackTrace;
        /** @internal */
        this._runScope = profile_1.wtfCreateScope("NgZone#run()");
        /** @internal */
        this._microtaskScope = profile_1.wtfCreateScope("NgZone#microtask()");
        // Number of microtasks pending from _innerZone (& descendants)
        /** @internal */
        this._pendingMicrotasks = 0;
        // Whether some code has been executed in the _innerZone (& descendants) in the current turn
        /** @internal */
        this._hasExecutedCodeInInnerZone = false;
        // run() call depth in _mountZone. 0 at the end of a macrotask
        // zone.run(() => {         // top-level call
        //   zone.run(() => {});    // nested call -> in-turn
        // });
        /** @internal */
        this._nestedRun = 0;
        /** @internal */
        this._inVmTurnDone = false;
        /** @internal */
        this._pendingTimeouts = [];
        if (lang_1.global.zone) {
            this._disabled = false;
            this._mountZone = lang_1.global.zone;
            this._innerZone = this._createInnerZone(this._mountZone, enableLongStackTrace);
        }
        else {
            this._disabled = true;
            this._mountZone = null;
        }
        this._onTurnStartEvents = new async_1.EventEmitter(false);
        this._onTurnDoneEvents = new async_1.EventEmitter(false);
        this._onEventDoneEvents = new async_1.EventEmitter(false);
        this._onErrorEvents = new async_1.EventEmitter(false);
    }
    /**
     * Sets the zone hook that is called just before a browser task that is handled by Angular
     * executes.
     *
     * The hook is called once per browser task that is handled by Angular.
     *
     * Setting the hook overrides any previously set hook.
     *
     * @deprecated this API will be removed in the future. Use `onTurnStart` instead.
     */
    NgZone.prototype.overrideOnTurnStart = function (onTurnStartHook) {
        this._onTurnStart = lang_1.normalizeBlank(onTurnStartHook);
    };
    Object.defineProperty(NgZone.prototype, "onTurnStart", {
        /**
         * Notifies subscribers just before Angular event turn starts.
         *
         * Emits an event once per browser task that is handled by Angular.
         */
        get: function () { return this._onTurnStartEvents; },
        enumerable: true,
        configurable: true
    });
    /** @internal */
    NgZone.prototype._notifyOnTurnStart = function (parentRun) {
        var _this = this;
        parentRun.call(this._innerZone, function () { _this._onTurnStartEvents.emit(null); });
    };
    /**
     * Sets the zone hook that is called immediately after Angular zone is done processing the current
     * task and any microtasks scheduled from that task.
     *
     * This is where we typically do change-detection.
     *
     * The hook is called once per browser task that is handled by Angular.
     *
     * Setting the hook overrides any previously set hook.
     *
     * @deprecated this API will be removed in the future. Use `onTurnDone` instead.
     */
    NgZone.prototype.overrideOnTurnDone = function (onTurnDoneHook) {
        this._onTurnDone = lang_1.normalizeBlank(onTurnDoneHook);
    };
    Object.defineProperty(NgZone.prototype, "onTurnDone", {
        /**
         * Notifies subscribers immediately after Angular zone is done processing
         * the current turn and any microtasks scheduled from that turn.
         *
         * Used by Angular as a signal to kick off change-detection.
         */
        get: function () { return this._onTurnDoneEvents; },
        enumerable: true,
        configurable: true
    });
    /** @internal */
    NgZone.prototype._notifyOnTurnDone = function (parentRun) {
        var _this = this;
        parentRun.call(this._innerZone, function () { _this._onTurnDoneEvents.emit(null); });
    };
    /**
     * Sets the zone hook that is called immediately after the `onTurnDone` callback is called and any
     * microstasks scheduled from within that callback are drained.
     *
     * `onEventDoneFn` is executed outside Angular zone, which means that we will no longer attempt to
     * sync the UI with any model changes that occur within this callback.
     *
     * This hook is useful for validating application state (e.g. in a test).
     *
     * Setting the hook overrides any previously set hook.
     *
     * @deprecated this API will be removed in the future. Use `onEventDone` instead.
     */
    NgZone.prototype.overrideOnEventDone = function (onEventDoneFn, opt_waitForAsync) {
        var _this = this;
        if (opt_waitForAsync === void 0) { opt_waitForAsync = false; }
        var normalizedOnEventDone = lang_1.normalizeBlank(onEventDoneFn);
        if (opt_waitForAsync) {
            this._onEventDone = function () {
                if (!_this._pendingTimeouts.length) {
                    normalizedOnEventDone();
                }
            };
        }
        else {
            this._onEventDone = normalizedOnEventDone;
        }
    };
    Object.defineProperty(NgZone.prototype, "onEventDone", {
        /**
         * Notifies subscribers immediately after the final `onTurnDone` callback
         * before ending VM event.
         *
         * This event is useful for validating application state (e.g. in a test).
         */
        get: function () { return this._onEventDoneEvents; },
        enumerable: true,
        configurable: true
    });
    /** @internal */
    NgZone.prototype._notifyOnEventDone = function () {
        var _this = this;
        this.runOutsideAngular(function () { _this._onEventDoneEvents.emit(null); });
    };
    Object.defineProperty(NgZone.prototype, "hasPendingMicrotasks", {
        /**
         * Whether there are any outstanding microtasks.
         */
        get: function () { return this._pendingMicrotasks > 0; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(NgZone.prototype, "hasPendingTimers", {
        /**
         * Whether there are any outstanding timers.
         */
        get: function () { return this._pendingTimeouts.length > 0; },
        enumerable: true,
        configurable: true
    });
    Object.defineProperty(NgZone.prototype, "hasPendingAsyncTasks", {
        /**
         * Whether there are any outstanding asynchronous tasks of any kind that are
         * scheduled to run within Angular zone.
         *
         * Useful as a signal of UI stability. For example, when a test reaches a
         * point when [hasPendingAsyncTasks] is `false` it might be a good time to run
         * test expectations.
         */
        get: function () { return this.hasPendingMicrotasks || this.hasPendingTimers; },
        enumerable: true,
        configurable: true
    });
    /**
     * Sets the zone hook that is called when an error is thrown in the Angular zone.
     *
     * Setting the hook overrides any previously set hook.
     *
     * @deprecated this API will be removed in the future. Use `onError` instead.
     */
    NgZone.prototype.overrideOnErrorHandler = function (errorHandler) {
        this._onErrorHandler = lang_1.normalizeBlank(errorHandler);
    };
    Object.defineProperty(NgZone.prototype, "onError", {
        get: function () { return this._onErrorEvents; },
        enumerable: true,
        configurable: true
    });
    /**
     * Executes the `fn` function synchronously within the Angular zone and returns value returned by
     * the function.
     *
     * Running functions via `run` allows you to reenter Angular zone from a task that was executed
     * outside of the Angular zone (typically started via {@link #runOutsideAngular}).
     *
     * Any future tasks or microtasks scheduled from within this function will continue executing from
     * within the Angular zone.
     */
    NgZone.prototype.run = function (fn) {
        if (this._disabled) {
            return fn();
        }
        else {
            var s = this._runScope();
            try {
                return this._innerZone.run(fn);
            }
            finally {
                profile_1.wtfLeave(s);
            }
        }
    };
    /**
     * Executes the `fn` function synchronously in Angular's parent zone and returns value returned by
     * the function.
     *
     * Running functions via `runOutsideAngular` allows you to escape Angular's zone and do work that
     * doesn't trigger Angular change-detection or is subject to Angular's error handling.
     *
     * Any future tasks or microtasks scheduled from within this function will continue executing from
     * outside of the Angular zone.
     *
     * Use {@link #run} to reenter the Angular zone and do work that updates the application model.
     */
    NgZone.prototype.runOutsideAngular = function (fn) {
        if (this._disabled) {
            return fn();
        }
        else {
            return this._mountZone.run(fn);
        }
    };
    /** @internal */
    NgZone.prototype._createInnerZone = function (zone, enableLongStackTrace) {
        var microtaskScope = this._microtaskScope;
        var ngZone = this;
        var errorHandling;
        if (enableLongStackTrace) {
            errorHandling = collection_1.StringMapWrapper.merge(Zone.longStackTraceZone, { onError: function (e) { ngZone._notifyOnError(this, e); } });
        }
        else {
            errorHandling = { onError: function (e) { ngZone._notifyOnError(this, e); } };
        }
        return zone.fork(errorHandling)
            .fork({
            '$run': function (parentRun) {
                return function () {
                    try {
                        ngZone._nestedRun++;
                        if (!ngZone._hasExecutedCodeInInnerZone) {
                            ngZone._hasExecutedCodeInInnerZone = true;
                            ngZone._notifyOnTurnStart(parentRun);
                            if (ngZone._onTurnStart) {
                                parentRun.call(ngZone._innerZone, ngZone._onTurnStart);
                            }
                        }
                        return parentRun.apply(this, arguments);
                    }
                    finally {
                        ngZone._nestedRun--;
                        // If there are no more pending microtasks, we are at the end of a VM turn (or in
                        // onTurnStart)
                        // _nestedRun will be 0 at the end of a macrotasks (it could be > 0 when there are
                        // nested calls
                        // to run()).
                        if (ngZone._pendingMicrotasks == 0 && ngZone._nestedRun == 0 &&
                            !this._inVmTurnDone) {
                            if (ngZone._hasExecutedCodeInInnerZone) {
                                try {
                                    this._inVmTurnDone = true;
                                    ngZone._notifyOnTurnDone(parentRun);
                                    if (ngZone._onTurnDone) {
                                        parentRun.call(ngZone._innerZone, ngZone._onTurnDone);
                                    }
                                }
                                finally {
                                    this._inVmTurnDone = false;
                                    ngZone._hasExecutedCodeInInnerZone = false;
                                }
                            }
                            if (ngZone._pendingMicrotasks === 0) {
                                ngZone._notifyOnEventDone();
                                if (lang_1.isPresent(ngZone._onEventDone)) {
                                    ngZone.runOutsideAngular(ngZone._onEventDone);
                                }
                            }
                        }
                    }
                };
            },
            '$scheduleMicrotask': function (parentScheduleMicrotask) {
                return function (fn) {
                    ngZone._pendingMicrotasks++;
                    var microtask = function () {
                        var s = microtaskScope();
                        try {
                            fn();
                        }
                        finally {
                            ngZone._pendingMicrotasks--;
                            profile_1.wtfLeave(s);
                        }
                    };
                    parentScheduleMicrotask.call(this, microtask);
                };
            },
            '$setTimeout': function (parentSetTimeout) {
                return function (fn, delay) {
                    var args = [];
                    for (var _i = 2; _i < arguments.length; _i++) {
                        args[_i - 2] = arguments[_i];
                    }
                    var id;
                    var cb = function () {
                        fn();
                        collection_1.ListWrapper.remove(ngZone._pendingTimeouts, id);
                    };
                    id = parentSetTimeout.call(this, cb, delay, args);
                    ngZone._pendingTimeouts.push(id);
                    return id;
                };
            },
            '$clearTimeout': function (parentClearTimeout) {
                return function (id) {
                    parentClearTimeout.call(this, id);
                    collection_1.ListWrapper.remove(ngZone._pendingTimeouts, id);
                };
            },
            _innerZone: true
        });
    };
    /** @internal */
    NgZone.prototype._notifyOnError = function (zone, e) {
        if (lang_1.isPresent(this._onErrorHandler) || async_1.ObservableWrapper.hasSubscribers(this._onErrorEvents)) {
            var trace = [lang_1.normalizeBlank(e.stack)];
            while (zone && zone.constructedAtException) {
                trace.push(zone.constructedAtException.get());
                zone = zone.parent;
            }
            if (async_1.ObservableWrapper.hasSubscribers(this._onErrorEvents)) {
                async_1.ObservableWrapper.callEmit(this._onErrorEvents, new NgZoneError(e, trace));
            }
            if (lang_1.isPresent(this._onErrorHandler)) {
                this._onErrorHandler(e, trace);
            }
        }
        else {
            console.log('## _notifyOnError ##');
            console.log(e.stack);
            throw e;
        }
    };
    return NgZone;
})();
exports.NgZone = NgZone;
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibmdfem9uZS5qcyIsInNvdXJjZVJvb3QiOiIiLCJzb3VyY2VzIjpbImFuZ3VsYXIyL3NyYy9jb3JlL3pvbmUvbmdfem9uZS50cyJdLCJuYW1lcyI6WyJOZ1pvbmVFcnJvciIsIk5nWm9uZUVycm9yLmNvbnN0cnVjdG9yIiwiTmdab25lIiwiTmdab25lLmNvbnN0cnVjdG9yIiwiTmdab25lLm92ZXJyaWRlT25UdXJuU3RhcnQiLCJOZ1pvbmUub25UdXJuU3RhcnQiLCJOZ1pvbmUuX25vdGlmeU9uVHVyblN0YXJ0IiwiTmdab25lLm92ZXJyaWRlT25UdXJuRG9uZSIsIk5nWm9uZS5vblR1cm5Eb25lIiwiTmdab25lLl9ub3RpZnlPblR1cm5Eb25lIiwiTmdab25lLm92ZXJyaWRlT25FdmVudERvbmUiLCJOZ1pvbmUub25FdmVudERvbmUiLCJOZ1pvbmUuX25vdGlmeU9uRXZlbnREb25lIiwiTmdab25lLmhhc1BlbmRpbmdNaWNyb3Rhc2tzIiwiTmdab25lLmhhc1BlbmRpbmdUaW1lcnMiLCJOZ1pvbmUuaGFzUGVuZGluZ0FzeW5jVGFza3MiLCJOZ1pvbmUub3ZlcnJpZGVPbkVycm9ySGFuZGxlciIsIk5nWm9uZS5vbkVycm9yIiwiTmdab25lLnJ1biIsIk5nWm9uZS5ydW5PdXRzaWRlQW5ndWxhciIsIk5nWm9uZS5fY3JlYXRlSW5uZXJab25lIiwiTmdab25lLl9ub3RpZnlPbkVycm9yIl0sIm1hcHBpbmdzIjoiQUFBQSwyQkFBNEMsZ0NBQWdDLENBQUMsQ0FBQTtBQUM3RSxxQkFBZ0QsMEJBQTBCLENBQUMsQ0FBQTtBQUMzRSxzQkFBOEMsMkJBQTJCLENBQUMsQ0FBQTtBQUMxRSx3QkFBbUQsb0JBQW9CLENBQUMsQ0FBQTtBQWlCeEU7O0dBRUc7QUFDSDtJQUNFQSxxQkFBbUJBLEtBQVVBLEVBQVNBLFVBQWVBO1FBQWxDQyxVQUFLQSxHQUFMQSxLQUFLQSxDQUFLQTtRQUFTQSxlQUFVQSxHQUFWQSxVQUFVQSxDQUFLQTtJQUFHQSxDQUFDQTtJQUMzREQsa0JBQUNB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QUVBLElBQUlBLFdBQVdBLENBQUNBLENBQUNBLEVBQUVBLEtBQUtBLENBQUNBLENBQUNBLENBQUNBO1lBQzdFQSxDQUFDQTtZQUNEQSxFQUFFQSxDQUFDQSxDQUFDQSxnQkFBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsZUFBZUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7Z0JBQ3BDQSxJQUFJQSxDQUFDQSxlQUFlQSxDQUFDQSxDQUFDQSxFQUFFQSxLQUFLQSxDQUFDQSxDQUFDQTtZQUNqQ0EsQ0FBQ0E7UUFDSEEsQ0FBQ0E7UUFBQ0EsSUFBSUEsQ0FBQ0EsQ0FBQ0E7WUFDTkEsT0FBT0EsQ0FBQ0EsR0FBR0EsQ0FBQ0Esc0JBQXNCQSxDQUFDQSxDQUFDQTtZQUNwQ0EsT0FBT0EsQ0FBQ0EsR0FBR0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0EsS0FBS0EsQ0FBQ0EsQ0FBQ0E7WUFDckJBLE1BQU1BLENBQUNBLENBQUNBO1FBQ1ZBLENBQUNBO0lBQ0hBLENBQUNBO0lBQ0huQixhQUFDQTtBQUFEQSxDQUFDQSxBQTFXRCxJQTBXQztBQTFXWSxjQUFNLFNBMFdsQixDQUFBIiwic291cmNlc0NvbnRlbnQiOlsiaW1wb3J0IHtMaXN0V3JhcHBlciwgU3RyaW5nTWFwV3JhcHBlcn0gZnJvbSAnYW5ndWxhcjIvc3JjL2ZhY2FkZS9jb2xsZWN0aW9uJztcbmltcG9ydCB7bm9ybWFsaXplQmxhbmssIGlzUHJlc2VudCwgZ2xvYmFsfSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2xhbmcnO1xuaW1wb3J0IHtPYnNlcnZhYmxlV3JhcHBlciwgRXZlbnRFbWl0dGVyfSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2FzeW5jJztcbmltcG9ydCB7d3RmTGVhdmUsIHd0ZkNyZWF0ZVNjb3BlLCBXdGZTY29wZUZufSBmcm9tICcuLi9wcm9maWxlL3Byb2ZpbGUnO1xuXG5leHBvcnQgaW50ZXJmYWNlIE5nWm9uZVpvbmUgZXh0ZW5kcyBab25lIHtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfaW5uZXJab25lOiBib29sZWFuO1xufVxuXG4vKipcbiAqIEludGVyZmFjZSBmb3IgYSBmdW5jdGlvbiB3aXRoIHplcm8gYXJndW1lbnRzLlxuICovXG5leHBvcnQgaW50ZXJmYWNlIFplcm9BcmdGdW5jdGlvbiB7ICgpOiB2b2lkOyB9XG5cbi8qKlxuICogRnVuY3Rpb24gdHlwZSBmb3IgYW4gZXJyb3IgaGFuZGxlciwgd2hpY2ggdGFrZXMgYW4gZXJyb3IgYW5kIGEgc3RhY2sgdHJhY2UuXG4gKi9cbmV4cG9ydCBpbnRlcmZhY2UgRXJyb3JIYW5kbGluZ0ZuIHsgKGVycm9yOiBhbnksIHN0YWNrVHJhY2U6IGFueSk6IHZvaWQ7IH1cblxuLyoqXG4gKiBTdG9yZXMgZXJyb3IgaW5mb3JtYXRpb247IGRlbGl2ZXJlZCB2aWEgW05nWm9uZS5vbkVycm9yXSBzdHJlYW0uXG4gKi9cbmV4cG9ydCBjbGFzcyBOZ1pvbmVFcnJvciB7XG4gIGNvbnN0cnVjdG9yKHB1YmxpYyBlcnJvcjogYW55LCBwdWJsaWMgc3RhY2tUcmFjZTogYW55KSB7fVxufVxuXG4vKipcbiAqIEFuIGluamVjdGFibGUgc2VydmljZSBmb3IgZXhlY3V0aW5nIHdvcmsgaW5zaWRlIG9yIG91dHNpZGUgb2YgdGhlIEFuZ3VsYXIgem9uZS5cbiAqXG4gKiBUaGUgbW9zdCBjb21tb24gdXNlIG9mIHRoaXMgc2VydmljZSBpcyB0byBvcHRpbWl6ZSBwZXJmb3JtYW5jZSB3aGVuIHN0YXJ0aW5nIGEgd29yayBjb25zaXN0aW5nIG9mXG4gKiBvbmUgb3IgbW9yZSBhc3luY2hyb25vdXMgdGFza3MgdGhhdCBkb24ndCByZXF1aXJlIFVJIHVwZGF0ZXMgb3IgZXJyb3IgaGFuZGxpbmcgdG8gYmUgaGFuZGxlZCBieVxuICogQW5ndWxhci4gU3VjaCB0YXNrcyBjYW4gYmUga2lja2VkIG9mZiB2aWEge0BsaW5rICNydW5PdXRzaWRlQW5ndWxhcn0gYW5kIGlmIG5lZWRlZCwgdGhlc2UgdGFza3NcbiAqIGNhbiByZWVudGVyIHRoZSBBbmd1bGFyIHpvbmUgdmlhIHtAbGluayAjcnVufS5cbiAqXG4gKiA8IS0tIFRPRE86IGFkZC9maXggbGlua3MgdG86XG4gKiAgIC0gZG9jcyBleHBsYWluaW5nIHpvbmVzIGFuZCB0aGUgdXNlIG9mIHpvbmVzIGluIEFuZ3VsYXIgYW5kIGNoYW5nZS1kZXRlY3Rpb25cbiAqICAgLSBsaW5rIHRvIHJ1bk91dHNpZGVBbmd1bGFyL3J1biAodGhyb3VnaG91dCB0aGlzIGZpbGUhKVxuICogICAtLT5cbiAqXG4gKiAjIyMgRXhhbXBsZSAoW2xpdmUgZGVtb10oaHR0cDovL3BsbmtyLmNvL2VkaXQvbFk5bThITHk3ejA2dkRvVWFTTjI/cD1wcmV2aWV3KSlcbiAqIGBgYFxuICogaW1wb3J0IHtDb21wb25lbnQsIFZpZXcsIE5nWm9uZX0gZnJvbSAnYW5ndWxhcjIvY29yZSc7XG4gKiBpbXBvcnQge05nSWZ9IGZyb20gJ2FuZ3VsYXIyL2NvbW1vbic7XG4gKlxuICogQENvbXBvbmVudCh7XG4gKiAgIHNlbGVjdG9yOiAnbmctem9uZS1kZW1vJy5cbiAqICAgdGVtcGxhdGU6IGBcbiAqICAgICA8aDI+RGVtbzogTmdab25lPC9oMj5cbiAqXG4gKiAgICAgPHA+UHJvZ3Jlc3M6IHt7cHJvZ3Jlc3N9fSU8L3A+XG4gKiAgICAgPHAgKm5nSWY9XCJwcm9ncmVzcyA+PSAxMDBcIj5Eb25lIHByb2Nlc3Npbmcge3tsYWJlbH19IG9mIEFuZ3VsYXIgem9uZSE8L3A+XG4gKlxuICogICAgIDxidXR0b24gKGNsaWNrKT1cInByb2Nlc3NXaXRoaW5Bbmd1bGFyWm9uZSgpXCI+UHJvY2VzcyB3aXRoaW4gQW5ndWxhciB6b25lPC9idXR0b24+XG4gKiAgICAgPGJ1dHRvbiAoY2xpY2spPVwicHJvY2Vzc091dHNpZGVPZkFuZ3VsYXJab25lKClcIj5Qcm9jZXNzIG91dHNpZGUgb2YgQW5ndWxhciB6b25lPC9idXR0b24+XG4gKiAgIGAsXG4gKiAgIGRpcmVjdGl2ZXM6IFtOZ0lmXVxuICogfSlcbiAqIGV4cG9ydCBjbGFzcyBOZ1pvbmVEZW1vIHtcbiAqICAgcHJvZ3Jlc3M6IG51bWJlciA9IDA7XG4gKiAgIGxhYmVsOiBzdHJpbmc7XG4gKlxuICogICBjb25zdHJ1Y3Rvcihwcml2YXRlIF9uZ1pvbmU6IE5nWm9uZSkge31cbiAqXG4gKiAgIC8vIExvb3AgaW5zaWRlIHRoZSBBbmd1bGFyIHpvbmVcbiAqICAgLy8gc28gdGhlIFVJIERPRVMgcmVmcmVzaCBhZnRlciBlYWNoIHNldFRpbWVvdXQgY3ljbGVcbiAqICAgcHJvY2Vzc1dpdGhpbkFuZ3VsYXJab25lKCkge1xuICogICAgIHRoaXMubGFiZWwgPSAnaW5zaWRlJztcbiAqICAgICB0aGlzLnByb2dyZXNzID0gMDtcbiAqICAgICB0aGlzLl9pbmNyZWFzZVByb2dyZXNzKCgpID0+IGNvbnNvbGUubG9nKCdJbnNpZGUgRG9uZSEnKSk7XG4gKiAgIH1cbiAqXG4gKiAgIC8vIExvb3Agb3V0c2lkZSBvZiB0aGUgQW5ndWxhciB6b25lXG4gKiAgIC8vIHNvIHRoZSBVSSBET0VTIE5PVCByZWZyZXNoIGFmdGVyIGVhY2ggc2V0VGltZW91dCBjeWNsZVxuICogICBwcm9jZXNzT3V0c2lkZU9mQW5ndWxhclpvbmUoKSB7XG4gKiAgICAgdGhpcy5sYWJlbCA9ICdvdXRzaWRlJztcbiAqICAgICB0aGlzLnByb2dyZXNzID0gMDtcbiAqICAgICB0aGlzLl9uZ1pvbmUucnVuT3V0c2lkZUFuZ3VsYXIoKCkgPT4ge1xuICogICAgICAgdGhpcy5faW5jcmVhc2VQcm9ncmVzcygoKSA9PiB7XG4gKiAgICAgICAvLyByZWVudGVyIHRoZSBBbmd1bGFyIHpvbmUgYW5kIGRpc3BsYXkgZG9uZVxuICogICAgICAgdGhpcy5fbmdab25lLnJ1bigoKSA9PiB7Y29uc29sZS5sb2coJ091dHNpZGUgRG9uZSEnKSB9KTtcbiAqICAgICB9fSkpO1xuICogICB9XG4gKlxuICpcbiAqICAgX2luY3JlYXNlUHJvZ3Jlc3MoZG9uZUNhbGxiYWNrOiAoKSA9PiB2b2lkKSB7XG4gKiAgICAgdGhpcy5wcm9ncmVzcyArPSAxO1xuICogICAgIGNvbnNvbGUubG9nKGBDdXJyZW50IHByb2dyZXNzOiAke3RoaXMucHJvZ3Jlc3N9JWApO1xuICpcbiAqICAgICBpZiAodGhpcy5wcm9ncmVzcyA8IDEwMCkge1xuICogICAgICAgd2luZG93LnNldFRpbWVvdXQoKCkgPT4gdGhpcy5faW5jcmVhc2VQcm9ncmVzcyhkb25lQ2FsbGJhY2spKSwgMTApXG4gKiAgICAgfSBlbHNlIHtcbiAqICAgICAgIGRvbmVDYWxsYmFjaygpO1xuICogICAgIH1cbiAqICAgfVxuICogfVxuICogYGBgXG4gKi9cbmV4cG9ydCBjbGFzcyBOZ1pvbmUge1xuICAvKiogQGludGVybmFsICovXG4gIF9ydW5TY29wZTogV3RmU2NvcGVGbiA9IHd0ZkNyZWF0ZVNjb3BlKGBOZ1pvbmUjcnVuKClgKTtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfbWljcm90YXNrU2NvcGU6IFd0ZlNjb3BlRm4gPSB3dGZDcmVhdGVTY29wZShgTmdab25lI21pY3JvdGFzaygpYCk7XG5cbiAgLy8gQ29kZSBleGVjdXRlZCBpbiBfbW91bnRab25lIGRvZXMgbm90IHRyaWdnZXIgdGhlIG9uVHVybkRvbmUuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX21vdW50Wm9uZTtcbiAgLy8gX2lubmVyWm9uZSBpcyB0aGUgY2hpbGQgb2YgX21vdW50Wm9uZS4gQW55IGNvZGUgZXhlY3V0ZWQgaW4gdGhpcyB6b25lIHdpbGwgdHJpZ2dlciB0aGVcbiAgLy8gb25UdXJuRG9uZSBob29rIGF0IHRoZSBlbmQgb2YgdGhlIGN1cnJlbnQgVk0gdHVybi5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfaW5uZXJab25lO1xuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX29uVHVyblN0YXJ0OiBaZXJvQXJnRnVuY3Rpb247XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX29uVHVybkRvbmU6IFplcm9BcmdGdW5jdGlvbjtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfb25FdmVudERvbmU6IFplcm9BcmdGdW5jdGlvbjtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfb25FcnJvckhhbmRsZXI6IEVycm9ySGFuZGxpbmdGbjtcblxuICAvKiogQGludGVybmFsICovXG4gIF9vblR1cm5TdGFydEV2ZW50czogRXZlbnRFbWl0dGVyPGFueT47XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX29uVHVybkRvbmVFdmVudHM6IEV2ZW50RW1pdHRlcjxhbnk+O1xuICAvKiogQGludGVybmFsICovXG4gIF9vbkV2ZW50RG9uZUV2ZW50czogRXZlbnRFbWl0dGVyPGFueT47XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX29uRXJyb3JFdmVudHM6IEV2ZW50RW1pdHRlcjxhbnk+O1xuXG4gIC8vIE51bWJlciBvZiBtaWNyb3Rhc2tzIHBlbmRpbmcgZnJvbSBfaW5uZXJab25lICgmIGRlc2NlbmRhbnRzKVxuICAvKiogQGludGVybmFsICovXG4gIF9wZW5kaW5nTWljcm90YXNrczogbnVtYmVyID0gMDtcbiAgLy8gV2hldGhlciBzb21lIGNvZGUgaGFzIGJlZW4gZXhlY3V0ZWQgaW4gdGhlIF9pbm5lclpvbmUgKCYgZGVzY2VuZGFudHMpIGluIHRoZSBjdXJyZW50IHR1cm5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfaGFzRXhlY3V0ZWRDb2RlSW5Jbm5lclpvbmU6IGJvb2xlYW4gPSBmYWxzZTtcbiAgLy8gcnVuKCkgY2FsbCBkZXB0aCBpbiBfbW91bnRab25lLiAwIGF0IHRoZSBlbmQgb2YgYSBtYWNyb3Rhc2tcbiAgLy8gem9uZS5ydW4oKCkgPT4geyAgICAgICAgIC8vIHRvcC1sZXZlbCBjYWxsXG4gIC8vICAgem9uZS5ydW4oKCkgPT4ge30pOyAgICAvLyBuZXN0ZWQgY2FsbCAtPiBpbi10dXJuXG4gIC8vIH0pO1xuICAvKiogQGludGVybmFsICovXG4gIF9uZXN0ZWRSdW46IG51bWJlciA9IDA7XG5cbiAgLy8gVE9ETyh2aWNiKTogaW1wbGVtZW50IHRoaXMgY2xhc3MgcHJvcGVybHkgZm9yIG5vZGUuanMgZW52aXJvbm1lbnRcbiAgLy8gVGhpcyBkaXNhYmxlZCBmbGFnIGlzIG9ubHkgaGVyZSB0byBwbGVhc2UgY2pzIHRlc3RzXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2Rpc2FibGVkOiBib29sZWFuO1xuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2luVm1UdXJuRG9uZTogYm9vbGVhbiA9IGZhbHNlO1xuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX3BlbmRpbmdUaW1lb3V0czogbnVtYmVyW10gPSBbXTtcblxuICAvKipcbiAgICogQHBhcmFtIHtib29sfSBlbmFibGVMb25nU3RhY2tUcmFjZSB3aGV0aGVyIHRvIGVuYWJsZSBsb25nIHN0YWNrIHRyYWNlLiBUaGV5IHNob3VsZCBvbmx5IGJlXG4gICAqICAgICAgICAgICAgICAgZW5hYmxlZCBpbiBkZXZlbG9wbWVudCBtb2RlIGFzIHRoZXkgc2lnbmlmaWNhbnRseSBpbXBhY3QgcGVyZi5cbiAgICovXG4gIGNvbnN0cnVjdG9yKHtlbmFibGVMb25nU3RhY2tUcmFjZX0pIHtcbiAgICBpZiAoZ2xvYmFsLnpvbmUpIHtcbiAgICAgIHRoaXMuX2Rpc2FibGVkID0gZmFsc2U7XG4gICAgICB0aGlzLl9tb3VudFpvbmUgPSBnbG9iYWwuem9uZTtcbiAgICAgIHRoaXMuX2lubmVyWm9uZSA9IHRoaXMuX2NyZWF0ZUlubmVyWm9uZSh0aGlzLl9tb3VudFpvbmUsIGVuYWJsZUxvbmdTdGFja1RyYWNlKTtcbiAgICB9IGVsc2Uge1xuICAgICAgdGhpcy5fZGlzYWJsZWQgPSB0cnVlO1xuICAgICAgdGhpcy5fbW91bnRab25lID0gbnVsbDtcbiAgICB9XG4gICAgdGhpcy5fb25UdXJuU3RhcnRFdmVudHMgPSBuZXcgRXZlbnRFbWl0dGVyKGZhbHNlKTtcbiAgICB0aGlzLl9vblR1cm5Eb25lRXZlbnRzID0gbmV3IEV2ZW50RW1pdHRlcihmYWxzZSk7XG4gICAgdGhpcy5fb25FdmVudERvbmVFdmVudHMgPSBuZXcgRXZlbnRFbWl0dGVyKGZhbHNlKTtcbiAgICB0aGlzLl9vbkVycm9yRXZlbnRzID0gbmV3IEV2ZW50RW1pdHRlcihmYWxzZSk7XG4gIH1cblxuICAvKipcbiAgICogU2V0cyB0aGUgem9uZSBob29rIHRoYXQgaXMgY2FsbGVkIGp1c3QgYmVmb3JlIGEgYnJvd3NlciB0YXNrIHRoYXQgaXMgaGFuZGxlZCBieSBBbmd1bGFyXG4gICAqIGV4ZWN1dGVzLlxuICAgKlxuICAgKiBUaGUgaG9vayBpcyBjYWxsZWQgb25jZSBwZXIgYnJvd3NlciB0YXNrIHRoYXQgaXMgaGFuZGxlZCBieSBBbmd1bGFyLlxuICAgKlxuICAgKiBTZXR0aW5nIHRoZSBob29rIG92ZXJyaWRlcyBhbnkgcHJldmlvdXNseSBzZXQgaG9vay5cbiAgICpcbiAgICogQGRlcHJlY2F0ZWQgdGhpcyBBUEkgd2lsbCBiZSByZW1vdmVkIGluIHRoZSBmdXR1cmUuIFVzZSBgb25UdXJuU3RhcnRgIGluc3RlYWQuXG4gICAqL1xuICBvdmVycmlkZU9uVHVyblN0YXJ0KG9uVHVyblN0YXJ0SG9vazogWmVyb0FyZ0Z1bmN0aW9uKTogdm9pZCB7XG4gICAgdGhpcy5fb25UdXJuU3RhcnQgPSBub3JtYWxpemVCbGFuayhvblR1cm5TdGFydEhvb2spO1xuICB9XG5cbiAgLyoqXG4gICAqIE5vdGlmaWVzIHN1YnNjcmliZXJzIGp1c3QgYmVmb3JlIEFuZ3VsYXIgZXZlbnQgdHVybiBzdGFydHMuXG4gICAqXG4gICAqIEVtaXRzIGFuIGV2ZW50IG9uY2UgcGVyIGJyb3dzZXIgdGFzayB0aGF0IGlzIGhhbmRsZWQgYnkgQW5ndWxhci5cbiAgICovXG4gIGdldCBvblR1cm5TdGFydCgpOiAvKiBTdWJqZWN0ICovIGFueSB7IHJldHVybiB0aGlzLl9vblR1cm5TdGFydEV2ZW50czsgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25vdGlmeU9uVHVyblN0YXJ0KHBhcmVudFJ1bik6IHZvaWQge1xuICAgIHBhcmVudFJ1bi5jYWxsKHRoaXMuX2lubmVyWm9uZSwgKCkgPT4geyB0aGlzLl9vblR1cm5TdGFydEV2ZW50cy5lbWl0KG51bGwpOyB9KTtcbiAgfVxuXG4gIC8qKlxuICAgKiBTZXRzIHRoZSB6b25lIGhvb2sgdGhhdCBpcyBjYWxsZWQgaW1tZWRpYXRlbHkgYWZ0ZXIgQW5ndWxhciB6b25lIGlzIGRvbmUgcHJvY2Vzc2luZyB0aGUgY3VycmVudFxuICAgKiB0YXNrIGFuZCBhbnkgbWljcm90YXNrcyBzY2hlZHVsZWQgZnJvbSB0aGF0IHRhc2suXG4gICAqXG4gICAqIFRoaXMgaXMgd2hlcmUgd2UgdHlwaWNhbGx5IGRvIGNoYW5nZS1kZXRlY3Rpb24uXG4gICAqXG4gICAqIFRoZSBob29rIGlzIGNhbGxlZCBvbmNlIHBlciBicm93c2VyIHRhc2sgdGhhdCBpcyBoYW5kbGVkIGJ5IEFuZ3VsYXIuXG4gICAqXG4gICAqIFNldHRpbmcgdGhlIGhvb2sgb3ZlcnJpZGVzIGFueSBwcmV2aW91c2x5IHNldCBob29rLlxuICAgKlxuICAgKiBAZGVwcmVjYXRlZCB0aGlzIEFQSSB3aWxsIGJlIHJlbW92ZWQgaW4gdGhlIGZ1dHVyZS4gVXNlIGBvblR1cm5Eb25lYCBpbnN0ZWFkLlxuICAgKi9cbiAgb3ZlcnJpZGVPblR1cm5Eb25lKG9uVHVybkRvbmVIb29rOiBaZXJvQXJnRnVuY3Rpb24pOiB2b2lkIHtcbiAgICB0aGlzLl9vblR1cm5Eb25lID0gbm9ybWFsaXplQmxhbmsob25UdXJuRG9uZUhvb2spO1xuICB9XG5cbiAgLyoqXG4gICAqIE5vdGlmaWVzIHN1YnNjcmliZXJzIGltbWVkaWF0ZWx5IGFmdGVyIEFuZ3VsYXIgem9uZSBpcyBkb25lIHByb2Nlc3NpbmdcbiAgICogdGhlIGN1cnJlbnQgdHVybiBhbmQgYW55IG1pY3JvdGFza3Mgc2NoZWR1bGVkIGZyb20gdGhhdCB0dXJuLlxuICAgKlxuICAgKiBVc2VkIGJ5IEFuZ3VsYXIgYXMgYSBzaWduYWwgdG8ga2ljayBvZmYgY2hhbmdlLWRldGVjdGlvbi5cbiAgICovXG4gIGdldCBvblR1cm5Eb25lKCkgeyByZXR1cm4gdGhpcy5fb25UdXJuRG9uZUV2ZW50czsgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25vdGlmeU9uVHVybkRvbmUocGFyZW50UnVuKTogdm9pZCB7XG4gICAgcGFyZW50UnVuLmNhbGwodGhpcy5faW5uZXJab25lLCAoKSA9PiB7IHRoaXMuX29uVHVybkRvbmVFdmVudHMuZW1pdChudWxsKTsgfSk7XG4gIH1cblxuICAvKipcbiAgICogU2V0cyB0aGUgem9uZSBob29rIHRoYXQgaXMgY2FsbGVkIGltbWVkaWF0ZWx5IGFmdGVyIHRoZSBgb25UdXJuRG9uZWAgY2FsbGJhY2sgaXMgY2FsbGVkIGFuZCBhbnlcbiAgICogbWljcm9zdGFza3Mgc2NoZWR1bGVkIGZyb20gd2l0aGluIHRoYXQgY2FsbGJhY2sgYXJlIGRyYWluZWQuXG4gICAqXG4gICAqIGBvbkV2ZW50RG9uZUZuYCBpcyBleGVjdXRlZCBvdXRzaWRlIEFuZ3VsYXIgem9uZSwgd2hpY2ggbWVhbnMgdGhhdCB3ZSB3aWxsIG5vIGxvbmdlciBhdHRlbXB0IHRvXG4gICAqIHN5bmMgdGhlIFVJIHdpdGggYW55IG1vZGVsIGNoYW5nZXMgdGhhdCBvY2N1ciB3aXRoaW4gdGhpcyBjYWxsYmFjay5cbiAgICpcbiAgICogVGhpcyBob29rIGlzIHVzZWZ1bCBmb3IgdmFsaWRhdGluZyBhcHBsaWNhdGlvbiBzdGF0ZSAoZS5nLiBpbiBhIHRlc3QpLlxuICAgKlxuICAgKiBTZXR0aW5nIHRoZSBob29rIG92ZXJyaWRlcyBhbnkgcHJldmlvdXNseSBzZXQgaG9vay5cbiAgICpcbiAgICogQGRlcHJlY2F0ZWQgdGhpcyBBUEkgd2lsbCBiZSByZW1vdmVkIGluIHRoZSBmdXR1cmUuIFVzZSBgb25FdmVudERvbmVgIGluc3RlYWQuXG4gICAqL1xuICBvdmVycmlkZU9uRXZlbnREb25lKG9uRXZlbnREb25lRm46IFplcm9BcmdGdW5jdGlvbiwgb3B0X3dhaXRGb3JBc3luYzogYm9vbGVhbiA9IGZhbHNlKTogdm9pZCB7XG4gICAgdmFyIG5vcm1hbGl6ZWRPbkV2ZW50RG9uZSA9IG5vcm1hbGl6ZUJsYW5rKG9uRXZlbnREb25lRm4pO1xuICAgIGlmIChvcHRfd2FpdEZvckFzeW5jKSB7XG4gICAgICB0aGlzLl9vbkV2ZW50RG9uZSA9ICgpID0+IHtcbiAgICAgICAgaWYgKCF0aGlzLl9wZW5kaW5nVGltZW91dHMubGVuZ3RoKSB7XG4gICAgICAgICAgbm9ybWFsaXplZE9uRXZlbnREb25lKCk7XG4gICAgICAgIH1cbiAgICAgIH07XG4gICAgfSBlbHNlIHtcbiAgICAgIHRoaXMuX29uRXZlbnREb25lID0gbm9ybWFsaXplZE9uRXZlbnREb25lO1xuICAgIH1cbiAgfVxuXG4gIC8qKlxuICAgKiBOb3RpZmllcyBzdWJzY3JpYmVycyBpbW1lZGlhdGVseSBhZnRlciB0aGUgZmluYWwgYG9uVHVybkRvbmVgIGNhbGxiYWNrXG4gICAqIGJlZm9yZSBlbmRpbmcgVk0gZXZlbnQuXG4gICAqXG4gICAqIFRoaXMgZXZlbnQgaXMgdXNlZnVsIGZvciB2YWxpZGF0aW5nIGFwcGxpY2F0aW9uIHN0YXRlIChlLmcuIGluIGEgdGVzdCkuXG4gICAqL1xuICBnZXQgb25FdmVudERvbmUoKSB7IHJldHVybiB0aGlzLl9vbkV2ZW50RG9uZUV2ZW50czsgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25vdGlmeU9uRXZlbnREb25lKCk6IHZvaWQge1xuICAgIHRoaXMucnVuT3V0c2lkZUFuZ3VsYXIoKCkgPT4geyB0aGlzLl9vbkV2ZW50RG9uZUV2ZW50cy5lbWl0KG51bGwpOyB9KTtcbiAgfVxuXG4gIC8qKlxuICAgKiBXaGV0aGVyIHRoZXJlIGFyZSBhbnkgb3V0c3RhbmRpbmcgbWljcm90YXNrcy5cbiAgICovXG4gIGdldCBoYXNQZW5kaW5nTWljcm90YXNrcygpOiBib29sZWFuIHsgcmV0dXJuIHRoaXMuX3BlbmRpbmdNaWNyb3Rhc2tzID4gMDsgfVxuXG4gIC8qKlxuICAgKiBXaGV0aGVyIHRoZXJlIGFyZSBhbnkgb3V0c3RhbmRpbmcgdGltZXJzLlxuICAgKi9cbiAgZ2V0IGhhc1BlbmRpbmdUaW1lcnMoKTogYm9vbGVhbiB7IHJldHVybiB0aGlzLl9wZW5kaW5nVGltZW91dHMubGVuZ3RoID4gMDsgfVxuXG4gIC8qKlxuICAgKiBXaGV0aGVyIHRoZXJlIGFyZSBhbnkgb3V0c3RhbmRpbmcgYXN5bmNocm9ub3VzIHRhc2tzIG9mIGFueSBraW5kIHRoYXQgYXJlXG4gICAqIHNjaGVkdWxlZCB0byBydW4gd2l0aGluIEFuZ3VsYXIgem9uZS5cbiAgICpcbiAgICogVXNlZnVsIGFzIGEgc2lnbmFsIG9mIFVJIHN0YWJpbGl0eS4gRm9yIGV4YW1wbGUsIHdoZW4gYSB0ZXN0IHJlYWNoZXMgYVxuICAgKiBwb2ludCB3aGVuIFtoYXNQZW5kaW5nQXN5bmNUYXNrc10gaXMgYGZhbHNlYCBpdCBtaWdodCBiZSBhIGdvb2QgdGltZSB0byBydW5cbiAgICogdGVzdCBleHBlY3RhdGlvbnMuXG4gICAqL1xuICBnZXQgaGFzUGVuZGluZ0FzeW5jVGFza3MoKTogYm9vbGVhbiB7IHJldHVybiB0aGlzLmhhc1BlbmRpbmdNaWNyb3Rhc2tzIHx8IHRoaXMuaGFzUGVuZGluZ1RpbWVyczsgfVxuXG4gIC8qKlxuICAgKiBTZXRzIHRoZSB6b25lIGhvb2sgdGhhdCBpcyBjYWxsZWQgd2hlbiBhbiBlcnJvciBpcyB0aHJvd24gaW4gdGhlIEFuZ3VsYXIgem9uZS5cbiAgICpcbiAgICogU2V0dGluZyB0aGUgaG9vayBvdmVycmlkZXMgYW55IHByZXZpb3VzbHkgc2V0IGhvb2suXG4gICAqXG4gICAqIEBkZXByZWNhdGVkIHRoaXMgQVBJIHdpbGwgYmUgcmVtb3ZlZCBpbiB0aGUgZnV0dXJlLiBVc2UgYG9uRXJyb3JgIGluc3RlYWQuXG4gICAqL1xuICBvdmVycmlkZU9uRXJyb3JIYW5kbGVyKGVycm9ySGFuZGxlcjogRXJyb3JIYW5kbGluZ0ZuKSB7XG4gICAgdGhpcy5fb25FcnJvckhhbmRsZXIgPSBub3JtYWxpemVCbGFuayhlcnJvckhhbmRsZXIpO1xuICB9XG5cbiAgZ2V0IG9uRXJyb3IoKSB7IHJldHVybiB0aGlzLl9vbkVycm9yRXZlbnRzOyB9XG5cbiAgLyoqXG4gICAqIEV4ZWN1dGVzIHRoZSBgZm5gIGZ1bmN0aW9uIHN5bmNocm9ub3VzbHkgd2l0aGluIHRoZSBBbmd1bGFyIHpvbmUgYW5kIHJldHVybnMgdmFsdWUgcmV0dXJuZWQgYnlcbiAgICogdGhlIGZ1bmN0aW9uLlxuICAgKlxuICAgKiBSdW5uaW5nIGZ1bmN0aW9ucyB2aWEgYHJ1bmAgYWxsb3dzIHlvdSB0byByZWVudGVyIEFuZ3VsYXIgem9uZSBmcm9tIGEgdGFzayB0aGF0IHdhcyBleGVjdXRlZFxuICAgKiBvdXRzaWRlIG9mIHRoZSBBbmd1bGFyIHpvbmUgKHR5cGljYWxseSBzdGFydGVkIHZpYSB7QGxpbmsgI3J1bk91dHNpZGVBbmd1bGFyfSkuXG4gICAqXG4gICAqIEFueSBmdXR1cmUgdGFza3Mgb3IgbWljcm90YXNrcyBzY2hlZHVsZWQgZnJvbSB3aXRoaW4gdGhpcyBmdW5jdGlvbiB3aWxsIGNvbnRpbnVlIGV4ZWN1dGluZyBmcm9tXG4gICAqIHdpdGhpbiB0aGUgQW5ndWxhciB6b25lLlxuICAgKi9cbiAgcnVuKGZuOiAoKSA9PiBhbnkpOiBhbnkge1xuICAgIGlmICh0aGlzLl9kaXNhYmxlZCkge1xuICAgICAgcmV0dXJuIGZuKCk7XG4gICAgfSBlbHNlIHtcbiAgICAgIHZhciBzID0gdGhpcy5fcnVuU2NvcGUoKTtcbiAgICAgIHRyeSB7XG4gICAgICAgIHJldHVybiB0aGlzLl9pbm5lclpvbmUucnVuKGZuKTtcbiAgICAgIH0gZmluYWxseSB7XG4gICAgICAgIHd0ZkxlYXZlKHMpO1xuICAgICAgfVxuICAgIH1cbiAgfVxuXG4gIC8qKlxuICAgKiBFeGVjdXRlcyB0aGUgYGZuYCBmdW5jdGlvbiBzeW5jaHJvbm91c2x5IGluIEFuZ3VsYXIncyBwYXJlbnQgem9uZSBhbmQgcmV0dXJucyB2YWx1ZSByZXR1cm5lZCBieVxuICAgKiB0aGUgZnVuY3Rpb24uXG4gICAqXG4gICAqIFJ1bm5pbmcgZnVuY3Rpb25zIHZpYSBgcnVuT3V0c2lkZUFuZ3VsYXJgIGFsbG93cyB5b3UgdG8gZXNjYXBlIEFuZ3VsYXIncyB6b25lIGFuZCBkbyB3b3JrIHRoYXRcbiAgICogZG9lc24ndCB0cmlnZ2VyIEFuZ3VsYXIgY2hhbmdlLWRldGVjdGlvbiBvciBpcyBzdWJqZWN0IHRvIEFuZ3VsYXIncyBlcnJvciBoYW5kbGluZy5cbiAgICpcbiAgICogQW55IGZ1dHVyZSB0YXNrcyBvciBtaWNyb3Rhc2tzIHNjaGVkdWxlZCBmcm9tIHdpdGhpbiB0aGlzIGZ1bmN0aW9uIHdpbGwgY29udGludWUgZXhlY3V0aW5nIGZyb21cbiAgICogb3V0c2lkZSBvZiB0aGUgQW5ndWxhciB6b25lLlxuICAgKlxuICAgKiBVc2Uge0BsaW5rICNydW59IHRvIHJlZW50ZXIgdGhlIEFuZ3VsYXIgem9uZSBhbmQgZG8gd29yayB0aGF0IHVwZGF0ZXMgdGhlIGFwcGxpY2F0aW9uIG1vZGVsLlxuICAgKi9cbiAgcnVuT3V0c2lkZUFuZ3VsYXIoZm46ICgpID0+IGFueSk6IGFueSB7XG4gICAgaWYgKHRoaXMuX2Rpc2FibGVkKSB7XG4gICAgICByZXR1cm4gZm4oKTtcbiAgICB9IGVsc2Uge1xuICAgICAgcmV0dXJuIHRoaXMuX21vdW50Wm9uZS5ydW4oZm4pO1xuICAgIH1cbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2NyZWF0ZUlubmVyWm9uZSh6b25lLCBlbmFibGVMb25nU3RhY2tUcmFjZSkge1xuICAgIHZhciBtaWNyb3Rhc2tTY29wZSA9IHRoaXMuX21pY3JvdGFza1Njb3BlO1xuICAgIHZhciBuZ1pvbmUgPSB0aGlzO1xuICAgIHZhciBlcnJvckhhbmRsaW5nO1xuXG4gICAgaWYgKGVuYWJsZUxvbmdTdGFja1RyYWNlKSB7XG4gICAgICBlcnJvckhhbmRsaW5nID0gU3RyaW5nTWFwV3JhcHBlci5tZXJnZShcbiAgICAgICAgICBab25lLmxvbmdTdGFja1RyYWNlWm9uZSwge29uRXJyb3I6IGZ1bmN0aW9uKGUpIHsgbmdab25lLl9ub3RpZnlPbkVycm9yKHRoaXMsIGUpOyB9fSk7XG4gICAgfSBlbHNlIHtcbiAgICAgIGVycm9ySGFuZGxpbmcgPSB7b25FcnJvcjogZnVuY3Rpb24oZSkgeyBuZ1pvbmUuX25vdGlmeU9uRXJyb3IodGhpcywgZSk7IH19O1xuICAgIH1cblxuICAgIHJldHVybiB6b25lLmZvcmsoZXJyb3JIYW5kbGluZylcbiAgICAgICAgLmZvcmsoe1xuICAgICAgICAgICckcnVuJzogZnVuY3Rpb24ocGFyZW50UnVuKSB7XG4gICAgICAgICAgICByZXR1cm4gZnVuY3Rpb24oKSB7XG4gICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgbmdab25lLl9uZXN0ZWRSdW4rKztcbiAgICAgICAgICAgICAgICBpZiAoIW5nWm9uZS5faGFzRXhlY3V0ZWRDb2RlSW5Jbm5lclpvbmUpIHtcbiAgICAgICAgICAgICAgICAgIG5nWm9uZS5faGFzRXhlY3V0ZWRDb2RlSW5Jbm5lclpvbmUgPSB0cnVlO1xuICAgICAgICAgICAgICAgICAgbmdab25lLl9ub3RpZnlPblR1cm5TdGFydChwYXJlbnRSdW4pO1xuICAgICAgICAgICAgICAgICAgaWYgKG5nWm9uZS5fb25UdXJuU3RhcnQpIHtcbiAgICAgICAgICAgICAgICAgICAgcGFyZW50UnVuLmNhbGwobmdab25lLl9pbm5lclpvbmUsIG5nWm9uZS5fb25UdXJuU3RhcnQpO1xuICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICByZXR1cm4gcGFyZW50UnVuLmFwcGx5KHRoaXMsIGFyZ3VtZW50cyk7XG4gICAgICAgICAgICAgIH0gZmluYWxseSB7XG4gICAgICAgICAgICAgICAgbmdab25lLl9uZXN0ZWRSdW4tLTtcbiAgICAgICAgICAgICAgICAvLyBJZiB0aGVyZSBhcmUgbm8gbW9yZSBwZW5kaW5nIG1pY3JvdGFza3MsIHdlIGFyZSBhdCB0aGUgZW5kIG9mIGEgVk0gdHVybiAob3IgaW5cbiAgICAgICAgICAgICAgICAvLyBvblR1cm5TdGFydClcbiAgICAgICAgICAgICAgICAvLyBfbmVzdGVkUnVuIHdpbGwgYmUgMCBhdCB0aGUgZW5kIG9mIGEgbWFjcm90YXNrcyAoaXQgY291bGQgYmUgPiAwIHdoZW4gdGhlcmUgYXJlXG4gICAgICAgICAgICAgICAgLy8gbmVzdGVkIGNhbGxzXG4gICAgICAgICAgICAgICAgLy8gdG8gcnVuKCkpLlxuICAgICAgICAgICAgICAgIGlmIChuZ1pvbmUuX3BlbmRpbmdNaWNyb3Rhc2tzID09IDAgJiYgbmdab25lLl9uZXN0ZWRSdW4gPT0gMCAmJlxuICAgICAgICAgICAgICAgICAgICAhdGhpcy5faW5WbVR1cm5Eb25lKSB7XG4gICAgICAgICAgICAgICAgICBpZiAobmdab25lLl9oYXNFeGVjdXRlZENvZGVJbklubmVyWm9uZSkge1xuICAgICAgICAgICAgICAgICAgICB0cnkge1xuICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX2luVm1UdXJuRG9uZSA9IHRydWU7XG4gICAgICAgICAgICAgICAgICAgICAgbmdab25lLl9ub3RpZnlPblR1cm5Eb25lKHBhcmVudFJ1bik7XG4gICAgICAgICAgICAgICAgICAgICAgaWYgKG5nWm9uZS5fb25UdXJuRG9uZSkge1xuICAgICAgICAgICAgICAgICAgICAgICAgcGFyZW50UnVuLmNhbGwobmdab25lLl9pbm5lclpvbmUsIG5nWm9uZS5fb25UdXJuRG9uZSk7XG4gICAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgICB9IGZpbmFsbHkge1xuICAgICAgICAgICAgICAgICAgICAgIHRoaXMuX2luVm1UdXJuRG9uZSA9IGZhbHNlO1xuICAgICAgICAgICAgICAgICAgICAgIG5nWm9uZS5faGFzRXhlY3V0ZWRDb2RlSW5Jbm5lclpvbmUgPSBmYWxzZTtcbiAgICAgICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgICAgICAgfVxuXG4gICAgICAgICAgICAgICAgICBpZiAobmdab25lLl9wZW5kaW5nTWljcm90YXNrcyA9PT0gMCkge1xuICAgICAgICAgICAgICAgICAgICBuZ1pvbmUuX25vdGlmeU9uRXZlbnREb25lKCk7XG4gICAgICAgICAgICAgICAgICAgIGlmIChpc1ByZXNlbnQobmdab25lLl9vbkV2ZW50RG9uZSkpIHtcbiAgICAgICAgICAgICAgICAgICAgICBuZ1pvbmUucnVuT3V0c2lkZUFuZ3VsYXIobmdab25lLl9vbkV2ZW50RG9uZSk7XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgIH1cbiAgICAgICAgICAgIH07XG4gICAgICAgICAgfSxcbiAgICAgICAgICAnJHNjaGVkdWxlTWljcm90YXNrJzogZnVuY3Rpb24ocGFyZW50U2NoZWR1bGVNaWNyb3Rhc2spIHtcbiAgICAgICAgICAgIHJldHVybiBmdW5jdGlvbihmbikge1xuICAgICAgICAgICAgICBuZ1pvbmUuX3BlbmRpbmdNaWNyb3Rhc2tzKys7XG4gICAgICAgICAgICAgIHZhciBtaWNyb3Rhc2sgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICB2YXIgcyA9IG1pY3JvdGFza1Njb3BlKCk7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgIGZuKCk7XG4gICAgICAgICAgICAgICAgfSBmaW5hbGx5IHtcbiAgICAgICAgICAgICAgICAgIG5nWm9uZS5fcGVuZGluZ01pY3JvdGFza3MtLTtcbiAgICAgICAgICAgICAgICAgIHd0ZkxlYXZlKHMpO1xuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgfTtcbiAgICAgICAgICAgICAgcGFyZW50U2NoZWR1bGVNaWNyb3Rhc2suY2FsbCh0aGlzLCBtaWNyb3Rhc2spO1xuICAgICAgICAgICAgfTtcbiAgICAgICAgICB9LFxuICAgICAgICAgICckc2V0VGltZW91dCc6IGZ1bmN0aW9uKHBhcmVudFNldFRpbWVvdXQpIHtcbiAgICAgICAgICAgIHJldHVybiBmdW5jdGlvbihmbjogRnVuY3Rpb24sIGRlbGF5OiBudW1iZXIsIC4uLmFyZ3MpIHtcbiAgICAgICAgICAgICAgdmFyIGlkO1xuICAgICAgICAgICAgICB2YXIgY2IgPSBmdW5jdGlvbigpIHtcbiAgICAgICAgICAgICAgICBmbigpO1xuICAgICAgICAgICAgICAgIExpc3RXcmFwcGVyLnJlbW92ZShuZ1pvbmUuX3BlbmRpbmdUaW1lb3V0cywgaWQpO1xuICAgICAgICAgICAgICB9O1xuICAgICAgICAgICAgICBpZCA9IHBhcmVudFNldFRpbWVvdXQuY2FsbCh0aGlzLCBjYiwgZGVsYXksIGFyZ3MpO1xuICAgICAgICAgICAgICBuZ1pvbmUuX3BlbmRpbmdUaW1lb3V0cy5wdXNoKGlkKTtcbiAgICAgICAgICAgICAgcmV0dXJuIGlkO1xuICAgICAgICAgICAgfTtcbiAgICAgICAgICB9LFxuICAgICAgICAgICckY2xlYXJUaW1lb3V0JzogZnVuY3Rpb24ocGFyZW50Q2xlYXJUaW1lb3V0KSB7XG4gICAgICAgICAgICByZXR1cm4gZnVuY3Rpb24oaWQ6IG51bWJlcikge1xuICAgICAgICAgICAgICBwYXJlbnRDbGVhclRpbWVvdXQuY2FsbCh0aGlzLCBpZCk7XG4gICAgICAgICAgICAgIExpc3RXcmFwcGVyLnJlbW92ZShuZ1pvbmUuX3BlbmRpbmdUaW1lb3V0cywgaWQpO1xuICAgICAgICAgICAgfTtcbiAgICAgICAgICB9LFxuICAgICAgICAgIF9pbm5lclpvbmU6IHRydWVcbiAgICAgICAgfSk7XG4gIH1cblxuICAvKiogQGludGVybmFsICovXG4gIF9ub3RpZnlPbkVycm9yKHpvbmUsIGUpOiB2b2lkIHtcbiAgICBpZiAoaXNQcmVzZW50KHRoaXMuX29uRXJyb3JIYW5kbGVyKSB8fCBPYnNlcnZhYmxlV3JhcHBlci5oYXNTdWJzY3JpYmVycyh0aGlzLl9vbkVycm9yRXZlbnRzKSkge1xuICAgICAgdmFyIHRyYWNlID0gW25vcm1hbGl6ZUJsYW5rKGUuc3RhY2spXTtcblxuICAgICAgd2hpbGUgKHpvbmUgJiYgem9uZS5jb25zdHJ1Y3RlZEF0RXhjZXB0aW9uKSB7XG4gICAgICAgIHRyYWNlLnB1c2goem9uZS5jb25zdHJ1Y3RlZEF0RXhjZXB0aW9uLmdldCgpKTtcbiAgICAgICAgem9uZSA9IHpvbmUucGFyZW50O1xuICAgICAgfVxuICAgICAgaWYgKE9ic2VydmFibGVXcmFwcGVyLmhhc1N1YnNjcmliZXJzKHRoaXMuX29uRXJyb3JFdmVudHMpKSB7XG4gICAgICAgIE9ic2VydmFibGVXcmFwcGVyLmNhbGxFbWl0KHRoaXMuX29uRXJyb3JFdmVudHMsIG5ldyBOZ1pvbmVFcnJvcihlLCB0cmFjZSkpO1xuICAgICAgfVxuICAgICAgaWYgKGlzUHJlc2VudCh0aGlzLl9vbkVycm9ySGFuZGxlcikpIHtcbiAgICAgICAgdGhpcy5fb25FcnJvckhhbmRsZXIoZSwgdHJhY2UpO1xuICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICBjb25zb2xlLmxvZygnIyMgX25vdGlmeU9uRXJyb3IgIyMnKTtcbiAgICAgIGNvbnNvbGUubG9nKGUuc3RhY2spO1xuICAgICAgdGhyb3cgZTtcbiAgICB9XG4gIH1cbn1cbiJdfQ==