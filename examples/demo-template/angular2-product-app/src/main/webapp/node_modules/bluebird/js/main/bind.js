"use strict";
module.exports = function(Promise, INTERNAL, tryConvertToPromise) {
function returnThis() { return this.value; }
function throwThis() { throw this.reason; }
function awaitBindingThenResolve(value) {
    return this._then(returnThis, null, null, {value: value}, undefined);
}
function awaitBindingThenReject(reason) {
    return this._then(throwThis, throwThis, null, {reason: reason}, undefined);
}
function setBinding(binding) { this._setBoundTo(binding); }
Promise.prototype.bind = function (thisArg) {
    var maybePromise = tryConvertToPromise(thisArg);
    if (maybePromise instanceof Promise) {
        if (maybePromise.isFulfilled()) {
            thisArg = maybePromise.value();
        } else if (maybePromise.isRejected()) {
            return Promise.reject(maybePromise.reason());
        } else {
            var ret = this.then();
            var parent = ret;
            ret = ret._then(awaitBindingThenResolve,
                            awaitBindingThenReject,
                            null, maybePromise, undefined);
            maybePromise._then(setBinding, ret._reject, null, ret, null);
            if (!ret._cancellable()) ret._setPendingCancellationParent(parent);
            return ret;
        }
    }
    var ret = this.then();
    ret._setBoundTo(thisArg);
    return ret;
};

Promise.bind = function (thisArg, value) {
    return Promise.resolve(value).bind(thisArg);
};

Promise.prototype._setPendingCancellationParent = function(parent) {
    this._settledValue = parent;
};

Promise.prototype._pendingCancellationParent = function() {
    if (this.isPending() && this._settledValue !== undefined) {
        var ret = this._settledValue;
        ret.cancellable();
        this._settledValue = undefined;
        return ret;
    }
};

Promise.prototype._setIsMigratingBinding = function () {
    this._bitField = this._bitField | 8388608;
};

Promise.prototype._unsetIsMigratingBinding = function () {
    this._bitField = this._bitField & (~8388608);
};

Promise.prototype._isMigratingBinding = function () {
    return (this._bitField & 8388608) === 8388608;
};

Promise.prototype._setBoundTo = function (obj) {
    this._boundTo = obj;
};

Promise.prototype._isBound = function () {
    return this._boundTo !== undefined;
};
};
