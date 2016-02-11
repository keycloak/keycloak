var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { CONST } from 'angular2/src/facade/lang';
import { BaseException } from 'angular2/src/facade/exceptions';
import { isListLikeIterable, iterateListLike } from 'angular2/src/facade/collection';
import { isBlank, isPresent, stringify, getMapKey, looseIdentical, isArray } from 'angular2/src/facade/lang';
export let DefaultIterableDifferFactory = class {
    supports(obj) { return isListLikeIterable(obj); }
    create(cdRef, trackByFn) {
        return new DefaultIterableDiffer(trackByFn);
    }
};
DefaultIterableDifferFactory = __decorate([
    CONST(), 
    __metadata('design:paramtypes', [])
], DefaultIterableDifferFactory);
var trackByIdentity = (index, item) => item;
export class DefaultIterableDiffer {
    constructor(_trackByFn) {
        this._trackByFn = _trackByFn;
        this._length = null;
        this._collection = null;
        // Keeps track of the used records at any point in time (during & across `_check()` calls)
        this._linkedRecords = null;
        // Keeps track of the removed records at any point in time during `_check()` calls.
        this._unlinkedRecords = null;
        this._previousItHead = null;
        this._itHead = null;
        this._itTail = null;
        this._additionsHead = null;
        this._additionsTail = null;
        this._movesHead = null;
        this._movesTail = null;
        this._removalsHead = null;
        this._removalsTail = null;
        this._trackByFn = isPresent(this._trackByFn) ? this._trackByFn : trackByIdentity;
    }
    get collection() { return this._collection; }
    get length() { return this._length; }
    forEachItem(fn) {
        var record;
        for (record = this._itHead; record !== null; record = record._next) {
            fn(record);
        }
    }
    forEachPreviousItem(fn) {
        var record;
        for (record = this._previousItHead; record !== null; record = record._nextPrevious) {
            fn(record);
        }
    }
    forEachAddedItem(fn) {
        var record;
        for (record = this._additionsHead; record !== null; record = record._nextAdded) {
            fn(record);
        }
    }
    forEachMovedItem(fn) {
        var record;
        for (record = this._movesHead; record !== null; record = record._nextMoved) {
            fn(record);
        }
    }
    forEachRemovedItem(fn) {
        var record;
        for (record = this._removalsHead; record !== null; record = record._nextRemoved) {
            fn(record);
        }
    }
    diff(collection) {
        if (isBlank(collection))
            collection = [];
        if (!isListLikeIterable(collection)) {
            throw new BaseException(`Error trying to diff '${collection}'`);
        }
        if (this.check(collection)) {
            return this;
        }
        else {
            return null;
        }
    }
    onDestroy() { }
    // todo(vicb): optim for UnmodifiableListView (frozen arrays)
    check(collection) {
        this._reset();
        var record = this._itHead;
        var mayBeDirty = false;
        var index;
        var item;
        var itemTrackBy;
        if (isArray(collection)) {
            var list = collection;
            this._length = collection.length;
            for (index = 0; index < this._length; index++) {
                item = list[index];
                itemTrackBy = this._trackByFn(index, item);
                if (record === null || !looseIdentical(record.trackById, itemTrackBy)) {
                    record = this._mismatch(record, item, itemTrackBy, index);
                    mayBeDirty = true;
                }
                else {
                    if (mayBeDirty) {
                        // TODO(misko): can we limit this to duplicates only?
                        record = this._verifyReinsertion(record, item, itemTrackBy, index);
                    }
                    record.item = item;
                }
                record = record._next;
            }
        }
        else {
            index = 0;
            iterateListLike(collection, (item) => {
                itemTrackBy = this._trackByFn(index, item);
                if (record === null || !looseIdentical(record.trackById, itemTrackBy)) {
                    record = this._mismatch(record, item, itemTrackBy, index);
                    mayBeDirty = true;
                }
                else if (mayBeDirty) {
                    // TODO(misko): can we limit this to duplicates only?
                    record = this._verifyReinsertion(record, item, itemTrackBy, index);
                }
                record = record._next;
                index++;
            });
            this._length = index;
        }
        this._truncate(record);
        this._collection = collection;
        return this.isDirty;
    }
    // CollectionChanges is considered dirty if it has any additions, moves or removals.
    get isDirty() {
        return this._additionsHead !== null || this._movesHead !== null || this._removalsHead !== null;
    }
    /**
     * Reset the state of the change objects to show no changes. This means set previousKey to
     * currentKey, and clear all of the queues (additions, moves, removals).
     * Set the previousIndexes of moved and added items to their currentIndexes
     * Reset the list of additions, moves and removals
     *
     * @internal
     */
    _reset() {
        if (this.isDirty) {
            var record;
            var nextRecord;
            for (record = this._previousItHead = this._itHead; record !== null; record = record._next) {
                record._nextPrevious = record._next;
            }
            for (record = this._additionsHead; record !== null; record = record._nextAdded) {
                record.previousIndex = record.currentIndex;
            }
            this._additionsHead = this._additionsTail = null;
            for (record = this._movesHead; record !== null; record = nextRecord) {
                record.previousIndex = record.currentIndex;
                nextRecord = record._nextMoved;
            }
            this._movesHead = this._movesTail = null;
            this._removalsHead = this._removalsTail = null;
        }
    }
    /**
     * This is the core function which handles differences between collections.
     *
     * - `record` is the record which we saw at this position last time. If null then it is a new
     *   item.
     * - `item` is the current item in the collection
     * - `index` is the position of the item in the collection
     *
     * @internal
     */
    _mismatch(record, item, itemTrackBy, index) {
        // The previous record after which we will append the current one.
        var previousRecord;
        if (record === null) {
            previousRecord = this._itTail;
        }
        else {
            previousRecord = record._prev;
            // Remove the record from the collection since we know it does not match the item.
            this._remove(record);
        }
        // Attempt to see if we have seen the item before.
        record = this._linkedRecords === null ? null : this._linkedRecords.get(itemTrackBy, index);
        if (record !== null) {
            // We have seen this before, we need to move it forward in the collection.
            this._moveAfter(record, previousRecord, index);
        }
        else {
            // Never seen it, check evicted list.
            record = this._unlinkedRecords === null ? null : this._unlinkedRecords.get(itemTrackBy);
            if (record !== null) {
                // It is an item which we have evicted earlier: reinsert it back into the list.
                this._reinsertAfter(record, previousRecord, index);
            }
            else {
                // It is a new item: add it.
                record =
                    this._addAfter(new CollectionChangeRecord(item, itemTrackBy), previousRecord, index);
            }
        }
        return record;
    }
    /**
     * This check is only needed if an array contains duplicates. (Short circuit of nothing dirty)
     *
     * Use case: `[a, a]` => `[b, a, a]`
     *
     * If we did not have this check then the insertion of `b` would:
     *   1) evict first `a`
     *   2) insert `b` at `0` index.
     *   3) leave `a` at index `1` as is. <-- this is wrong!
     *   3) reinsert `a` at index 2. <-- this is wrong!
     *
     * The correct behavior is:
     *   1) evict first `a`
     *   2) insert `b` at `0` index.
     *   3) reinsert `a` at index 1.
     *   3) move `a` at from `1` to `2`.
     *
     *
     * Double check that we have not evicted a duplicate item. We need to check if the item type may
     * have already been removed:
     * The insertion of b will evict the first 'a'. If we don't reinsert it now it will be reinserted
     * at the end. Which will show up as the two 'a's switching position. This is incorrect, since a
     * better way to think of it is as insert of 'b' rather then switch 'a' with 'b' and then add 'a'
     * at the end.
     *
     * @internal
     */
    _verifyReinsertion(record, item, itemTrackBy, index) {
        var reinsertRecord = this._unlinkedRecords === null ? null : this._unlinkedRecords.get(itemTrackBy);
        if (reinsertRecord !== null) {
            record = this._reinsertAfter(reinsertRecord, record._prev, index);
        }
        else if (record.currentIndex != index) {
            record.currentIndex = index;
            this._addToMoves(record, index);
        }
        record.item = item;
        return record;
    }
    /**
     * Get rid of any excess {@link CollectionChangeRecord}s from the previous collection
     *
     * - `record` The first excess {@link CollectionChangeRecord}.
     *
     * @internal
     */
    _truncate(record) {
        // Anything after that needs to be removed;
        while (record !== null) {
            var nextRecord = record._next;
            this._addToRemovals(this._unlink(record));
            record = nextRecord;
        }
        if (this._unlinkedRecords !== null) {
            this._unlinkedRecords.clear();
        }
        if (this._additionsTail !== null) {
            this._additionsTail._nextAdded = null;
        }
        if (this._movesTail !== null) {
            this._movesTail._nextMoved = null;
        }
        if (this._itTail !== null) {
            this._itTail._next = null;
        }
        if (this._removalsTail !== null) {
            this._removalsTail._nextRemoved = null;
        }
    }
    /** @internal */
    _reinsertAfter(record, prevRecord, index) {
        if (this._unlinkedRecords !== null) {
            this._unlinkedRecords.remove(record);
        }
        var prev = record._prevRemoved;
        var next = record._nextRemoved;
        if (prev === null) {
            this._removalsHead = next;
        }
        else {
            prev._nextRemoved = next;
        }
        if (next === null) {
            this._removalsTail = prev;
        }
        else {
            next._prevRemoved = prev;
        }
        this._insertAfter(record, prevRecord, index);
        this._addToMoves(record, index);
        return record;
    }
    /** @internal */
    _moveAfter(record, prevRecord, index) {
        this._unlink(record);
        this._insertAfter(record, prevRecord, index);
        this._addToMoves(record, index);
        return record;
    }
    /** @internal */
    _addAfter(record, prevRecord, index) {
        this._insertAfter(record, prevRecord, index);
        if (this._additionsTail === null) {
            // todo(vicb)
            // assert(this._additionsHead === null);
            this._additionsTail = this._additionsHead = record;
        }
        else {
            // todo(vicb)
            // assert(_additionsTail._nextAdded === null);
            // assert(record._nextAdded === null);
            this._additionsTail = this._additionsTail._nextAdded = record;
        }
        return record;
    }
    /** @internal */
    _insertAfter(record, prevRecord, index) {
        // todo(vicb)
        // assert(record != prevRecord);
        // assert(record._next === null);
        // assert(record._prev === null);
        var next = prevRecord === null ? this._itHead : prevRecord._next;
        // todo(vicb)
        // assert(next != record);
        // assert(prevRecord != record);
        record._next = next;
        record._prev = prevRecord;
        if (next === null) {
            this._itTail = record;
        }
        else {
            next._prev = record;
        }
        if (prevRecord === null) {
            this._itHead = record;
        }
        else {
            prevRecord._next = record;
        }
        if (this._linkedRecords === null) {
            this._linkedRecords = new _DuplicateMap();
        }
        this._linkedRecords.put(record);
        record.currentIndex = index;
        return record;
    }
    /** @internal */
    _remove(record) {
        return this._addToRemovals(this._unlink(record));
    }
    /** @internal */
    _unlink(record) {
        if (this._linkedRecords !== null) {
            this._linkedRecords.remove(record);
        }
        var prev = record._prev;
        var next = record._next;
        // todo(vicb)
        // assert((record._prev = null) === null);
        // assert((record._next = null) === null);
        if (prev === null) {
            this._itHead = next;
        }
        else {
            prev._next = next;
        }
        if (next === null) {
            this._itTail = prev;
        }
        else {
            next._prev = prev;
        }
        return record;
    }
    /** @internal */
    _addToMoves(record, toIndex) {
        // todo(vicb)
        // assert(record._nextMoved === null);
        if (record.previousIndex === toIndex) {
            return record;
        }
        if (this._movesTail === null) {
            // todo(vicb)
            // assert(_movesHead === null);
            this._movesTail = this._movesHead = record;
        }
        else {
            // todo(vicb)
            // assert(_movesTail._nextMoved === null);
            this._movesTail = this._movesTail._nextMoved = record;
        }
        return record;
    }
    /** @internal */
    _addToRemovals(record) {
        if (this._unlinkedRecords === null) {
            this._unlinkedRecords = new _DuplicateMap();
        }
        this._unlinkedRecords.put(record);
        record.currentIndex = null;
        record._nextRemoved = null;
        if (this._removalsTail === null) {
            // todo(vicb)
            // assert(_removalsHead === null);
            this._removalsTail = this._removalsHead = record;
            record._prevRemoved = null;
        }
        else {
            // todo(vicb)
            // assert(_removalsTail._nextRemoved === null);
            // assert(record._nextRemoved === null);
            record._prevRemoved = this._removalsTail;
            this._removalsTail = this._removalsTail._nextRemoved = record;
        }
        return record;
    }
    toString() {
        var list = [];
        this.forEachItem((record) => list.push(record));
        var previous = [];
        this.forEachPreviousItem((record) => previous.push(record));
        var additions = [];
        this.forEachAddedItem((record) => additions.push(record));
        var moves = [];
        this.forEachMovedItem((record) => moves.push(record));
        var removals = [];
        this.forEachRemovedItem((record) => removals.push(record));
        return "collection: " + list.join(', ') + "\n" + "previous: " + previous.join(', ') + "\n" +
            "additions: " + additions.join(', ') + "\n" + "moves: " + moves.join(', ') + "\n" +
            "removals: " + removals.join(', ') + "\n";
    }
}
export class CollectionChangeRecord {
    constructor(item, trackById) {
        this.item = item;
        this.trackById = trackById;
        this.currentIndex = null;
        this.previousIndex = null;
        /** @internal */
        this._nextPrevious = null;
        /** @internal */
        this._prev = null;
        /** @internal */
        this._next = null;
        /** @internal */
        this._prevDup = null;
        /** @internal */
        this._nextDup = null;
        /** @internal */
        this._prevRemoved = null;
        /** @internal */
        this._nextRemoved = null;
        /** @internal */
        this._nextAdded = null;
        /** @internal */
        this._nextMoved = null;
    }
    toString() {
        return this.previousIndex === this.currentIndex ?
            stringify(this.item) :
            stringify(this.item) + '[' + stringify(this.previousIndex) + '->' +
                stringify(this.currentIndex) + ']';
    }
}
// A linked list of CollectionChangeRecords with the same CollectionChangeRecord.item
class _DuplicateItemRecordList {
    constructor() {
        /** @internal */
        this._head = null;
        /** @internal */
        this._tail = null;
    }
    /**
     * Append the record to the list of duplicates.
     *
     * Note: by design all records in the list of duplicates hold the same value in record.item.
     */
    add(record) {
        if (this._head === null) {
            this._head = this._tail = record;
            record._nextDup = null;
            record._prevDup = null;
        }
        else {
            // todo(vicb)
            // assert(record.item ==  _head.item ||
            //       record.item is num && record.item.isNaN && _head.item is num && _head.item.isNaN);
            this._tail._nextDup = record;
            record._prevDup = this._tail;
            record._nextDup = null;
            this._tail = record;
        }
    }
    // Returns a CollectionChangeRecord having CollectionChangeRecord.trackById == trackById and
    // CollectionChangeRecord.currentIndex >= afterIndex
    get(trackById, afterIndex) {
        var record;
        for (record = this._head; record !== null; record = record._nextDup) {
            if ((afterIndex === null || afterIndex < record.currentIndex) &&
                looseIdentical(record.trackById, trackById)) {
                return record;
            }
        }
        return null;
    }
    /**
     * Remove one {@link CollectionChangeRecord} from the list of duplicates.
     *
     * Returns whether the list of duplicates is empty.
     */
    remove(record) {
        // todo(vicb)
        // assert(() {
        //  // verify that the record being removed is in the list.
        //  for (CollectionChangeRecord cursor = _head; cursor != null; cursor = cursor._nextDup) {
        //    if (identical(cursor, record)) return true;
        //  }
        //  return false;
        //});
        var prev = record._prevDup;
        var next = record._nextDup;
        if (prev === null) {
            this._head = next;
        }
        else {
            prev._nextDup = next;
        }
        if (next === null) {
            this._tail = prev;
        }
        else {
            next._prevDup = prev;
        }
        return this._head === null;
    }
}
class _DuplicateMap {
    constructor() {
        this.map = new Map();
    }
    put(record) {
        // todo(vicb) handle corner cases
        var key = getMapKey(record.trackById);
        var duplicates = this.map.get(key);
        if (!isPresent(duplicates)) {
            duplicates = new _DuplicateItemRecordList();
            this.map.set(key, duplicates);
        }
        duplicates.add(record);
    }
    /**
     * Retrieve the `value` using key. Because the CollectionChangeRecord value may be one which we
     * have already iterated over, we use the afterIndex to pretend it is not there.
     *
     * Use case: `[a, b, c, a, a]` if we are at index `3` which is the second `a` then asking if we
     * have any more `a`s needs to return the last `a` not the first or second.
     */
    get(trackById, afterIndex = null) {
        var key = getMapKey(trackById);
        var recordList = this.map.get(key);
        return isBlank(recordList) ? null : recordList.get(trackById, afterIndex);
    }
    /**
     * Removes a {@link CollectionChangeRecord} from the list of duplicates.
     *
     * The list of duplicates also is removed from the map if it gets empty.
     */
    remove(record) {
        var key = getMapKey(record.trackById);
        // todo(vicb)
        // assert(this.map.containsKey(key));
        var recordList = this.map.get(key);
        // Remove the list of duplicates when it gets empty
        if (recordList.remove(record)) {
            this.map.delete(key);
        }
        return record;
    }
    get isEmpty() { return this.map.size === 0; }
    clear() { this.map.clear(); }
    toString() { return '_DuplicateMap(' + stringify(this.map) + ')'; }
}
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoiZGVmYXVsdF9pdGVyYWJsZV9kaWZmZXIuanMiLCJzb3VyY2VSb290IjoiIiwic291cmNlcyI6WyJhbmd1bGFyMi9zcmMvY29yZS9jaGFuZ2VfZGV0ZWN0aW9uL2RpZmZlcnMvZGVmYXVsdF9pdGVyYWJsZV9kaWZmZXIudHMiXSwibmFtZXMiOlsiRGVmYXVsdEl0ZXJhYmxlRGlmZmVyRmFjdG9yeSIsIkRlZmF1bHRJdGVyYWJsZURpZmZlckZhY3Rvcnkuc3VwcG9ydHMiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXJGYWN0b3J5LmNyZWF0ZSIsIkRlZmF1bHRJdGVyYWJsZURpZmZlciIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5jb25zdHJ1Y3RvciIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5jb2xsZWN0aW9uIiwiRGVmYXVsdEl0ZXJhYmxlRGlmZmVyLmxlbmd0aCIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5mb3JFYWNoSXRlbSIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5mb3JFYWNoUHJldmlvdXNJdGVtIiwiRGVmYXVsdEl0ZXJhYmxlRGlmZmVyLmZvckVhY2hBZGRlZEl0ZW0iLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuZm9yRWFjaE1vdmVkSXRlbSIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5mb3JFYWNoUmVtb3ZlZEl0ZW0iLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuZGlmZiIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5vbkRlc3Ryb3kiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuY2hlY2siLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuaXNEaXJ0eSIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5fcmVzZXQiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuX21pc21hdGNoIiwiRGVmYXVsdEl0ZXJhYmxlRGlmZmVyLl92ZXJpZnlSZWluc2VydGlvbiIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5fdHJ1bmNhdGUiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuX3JlaW5zZXJ0QWZ0ZXIiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuX21vdmVBZnRlciIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5fYWRkQWZ0ZXIiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuX2luc2VydEFmdGVyIiwiRGVmYXVsdEl0ZXJhYmxlRGlmZmVyLl9yZW1vdmUiLCJEZWZhdWx0SXRlcmFibGVEaWZmZXIuX3VubGluayIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5fYWRkVG9Nb3ZlcyIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci5fYWRkVG9SZW1vdmFscyIsIkRlZmF1bHRJdGVyYWJsZURpZmZlci50b1N0cmluZyIsIkNvbGxlY3Rpb25DaGFuZ2VSZWNvcmQiLCJDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkLmNvbnN0cnVjdG9yIiwiQ29sbGVjdGlvbkNoYW5nZVJlY29yZC50b1N0cmluZyIsIl9EdXBsaWNhdGVJdGVtUmVjb3JkTGlzdCIsIl9EdXBsaWNhdGVJdGVtUmVjb3JkTGlzdC5jb25zdHJ1Y3RvciIsIl9EdXBsaWNhdGVJdGVtUmVjb3JkTGlzdC5hZGQiLCJfRHVwbGljYXRlSXRlbVJlY29yZExpc3QuZ2V0IiwiX0R1cGxpY2F0ZUl0ZW1SZWNvcmRMaXN0LnJlbW92ZSIsIl9EdXBsaWNhdGVNYXAiLCJfRHVwbGljYXRlTWFwLmNvbnN0cnVjdG9yIiwiX0R1cGxpY2F0ZU1hcC5wdXQiLCJfRHVwbGljYXRlTWFwLmdldCIsIl9EdXBsaWNhdGVNYXAucmVtb3ZlIiwiX0R1cGxpY2F0ZU1hcC5pc0VtcHR5IiwiX0R1cGxpY2F0ZU1hcC5jbGVhciIsIl9EdXBsaWNhdGVNYXAudG9TdHJpbmciXSwibWFwcGluZ3MiOiI7Ozs7Ozs7OztPQUFPLEVBQUMsS0FBSyxFQUFDLE1BQU0sMEJBQTBCO09BQ3ZDLEVBQUMsYUFBYSxFQUFDLE1BQU0sZ0NBQWdDO09BQ3JELEVBQUMsa0JBQWtCLEVBQUUsZUFBZSxFQUFjLE1BQU0sZ0NBQWdDO09BRXhGLEVBQ0wsT0FBTyxFQUNQLFNBQVMsRUFDVCxTQUFTLEVBQ1QsU0FBUyxFQUNU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UUFDYkEscUNBQXFDQTtRQUNyQ0EsSUFBSUEsVUFBVUEsR0FBNkJBLElBQUlBLENBQUNBLEdBQUdBLENBQUNBLEdBQUdBLENBQUNBLEdBQUdBLENBQUNBLENBQUNBO1FBQzdEQSxtREFBbURBO1FBQ25EQSxFQUFFQSxDQUFDQSxDQUFDQSxVQUFVQSxDQUFDQSxNQUFNQSxDQUFDQSxNQUFNQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtZQUM5QkEsSUFBSUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsTUFBTUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsQ0FBQ0E7UUFDdkJBLENBQUNBO1FBQ0RBLE1BQU1BLENBQUNBLE1BQU1BLENBQUNBO0lBQ2hCQSxDQUFDQTtJQUVESixJQUFJQSxPQUFPQSxLQUFjSyxNQUFNQSxDQUFDQSxJQUFJQSxDQUFDQSxHQUFHQSxDQUFDQSxJQUFJQSxLQUFLQSxDQUFDQSxDQUFDQSxDQUFDQSxDQUFDQTtJQUV0REwsS0FBS0EsS0FBS00sSUFBSUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsS0FBS0EsRUFBRUEsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7SUFFN0JOLFFBQVFBLEtBQWFPLE1BQU1BLENBQUNBLGdCQUFnQkEsR0FBR0EsU0FBU0EsQ0FBQ0EsSUFBSUEsQ0FBQ0EsR0FBR0EsQ0FBQ0EsR0FBR0EsR0FBR0EsQ0FBQ0EsQ0FBQ0EsQ0FBQ0E7QUFDN0VQLENBQUNBO0FBQUEiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQge0NPTlNUfSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2xhbmcnO1xuaW1wb3J0IHtCYXNlRXhjZXB0aW9ufSBmcm9tICdhbmd1bGFyMi9zcmMvZmFjYWRlL2V4Y2VwdGlvbnMnO1xuaW1wb3J0IHtpc0xpc3RMaWtlSXRlcmFibGUsIGl0ZXJhdGVMaXN0TGlrZSwgTGlzdFdyYXBwZXJ9IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvY29sbGVjdGlvbic7XG5cbmltcG9ydCB7XG4gIGlzQmxhbmssXG4gIGlzUHJlc2VudCxcbiAgc3RyaW5naWZ5LFxuICBnZXRNYXBLZXksXG4gIGxvb3NlSWRlbnRpY2FsLFxuICBpc0FycmF5XG59IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvbGFuZyc7XG5cbmltcG9ydCB7Q2hhbmdlRGV0ZWN0b3JSZWZ9IGZyb20gJy4uL2NoYW5nZV9kZXRlY3Rvcl9yZWYnO1xuaW1wb3J0IHtJdGVyYWJsZURpZmZlciwgSXRlcmFibGVEaWZmZXJGYWN0b3J5LCBUcmFja0J5Rm59IGZyb20gJy4uL2RpZmZlcnMvaXRlcmFibGVfZGlmZmVycyc7XG5cbkBDT05TVCgpXG5leHBvcnQgY2xhc3MgRGVmYXVsdEl0ZXJhYmxlRGlmZmVyRmFjdG9yeSBpbXBsZW1lbnRzIEl0ZXJhYmxlRGlmZmVyRmFjdG9yeSB7XG4gIHN1cHBvcnRzKG9iajogT2JqZWN0KTogYm9vbGVhbiB7IHJldHVybiBpc0xpc3RMaWtlSXRlcmFibGUob2JqKTsgfVxuICBjcmVhdGUoY2RSZWY6IENoYW5nZURldGVjdG9yUmVmLCB0cmFja0J5Rm4/OiBUcmFja0J5Rm4pOiBEZWZhdWx0SXRlcmFibGVEaWZmZXIge1xuICAgIHJldHVybiBuZXcgRGVmYXVsdEl0ZXJhYmxlRGlmZmVyKHRyYWNrQnlGbik7XG4gIH1cbn1cblxudmFyIHRyYWNrQnlJZGVudGl0eSA9IChpbmRleDogbnVtYmVyLCBpdGVtOiBhbnkpID0+IGl0ZW07XG5cbmV4cG9ydCBjbGFzcyBEZWZhdWx0SXRlcmFibGVEaWZmZXIgaW1wbGVtZW50cyBJdGVyYWJsZURpZmZlciB7XG4gIHByaXZhdGUgX2xlbmd0aDogbnVtYmVyID0gbnVsbDtcbiAgcHJpdmF0ZSBfY29sbGVjdGlvbiA9IG51bGw7XG4gIC8vIEtlZXBzIHRyYWNrIG9mIHRoZSB1c2VkIHJlY29yZHMgYXQgYW55IHBvaW50IGluIHRpbWUgKGR1cmluZyAmIGFjcm9zcyBgX2NoZWNrKClgIGNhbGxzKVxuICBwcml2YXRlIF9saW5rZWRSZWNvcmRzOiBfRHVwbGljYXRlTWFwID0gbnVsbDtcbiAgLy8gS2VlcHMgdHJhY2sgb2YgdGhlIHJlbW92ZWQgcmVjb3JkcyBhdCBhbnkgcG9pbnQgaW4gdGltZSBkdXJpbmcgYF9jaGVjaygpYCBjYWxscy5cbiAgcHJpdmF0ZSBfdW5saW5rZWRSZWNvcmRzOiBfRHVwbGljYXRlTWFwID0gbnVsbDtcbiAgcHJpdmF0ZSBfcHJldmlvdXNJdEhlYWQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSBudWxsO1xuICBwcml2YXRlIF9pdEhlYWQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSBudWxsO1xuICBwcml2YXRlIF9pdFRhaWw6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSBudWxsO1xuICBwcml2YXRlIF9hZGRpdGlvbnNIZWFkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcbiAgcHJpdmF0ZSBfYWRkaXRpb25zVGFpbDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIHByaXZhdGUgX21vdmVzSGVhZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIHByaXZhdGUgX21vdmVzVGFpbDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIHByaXZhdGUgX3JlbW92YWxzSGVhZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIHByaXZhdGUgX3JlbW92YWxzVGFpbDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG5cbiAgY29uc3RydWN0b3IocHJpdmF0ZSBfdHJhY2tCeUZuPzogVHJhY2tCeUZuKSB7XG4gICAgdGhpcy5fdHJhY2tCeUZuID0gaXNQcmVzZW50KHRoaXMuX3RyYWNrQnlGbikgPyB0aGlzLl90cmFja0J5Rm4gOiB0cmFja0J5SWRlbnRpdHk7XG4gIH1cblxuICBnZXQgY29sbGVjdGlvbigpIHsgcmV0dXJuIHRoaXMuX2NvbGxlY3Rpb247IH1cblxuICBnZXQgbGVuZ3RoKCk6IG51bWJlciB7IHJldHVybiB0aGlzLl9sZW5ndGg7IH1cblxuICBmb3JFYWNoSXRlbShmbjogRnVuY3Rpb24pIHtcbiAgICB2YXIgcmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkO1xuICAgIGZvciAocmVjb3JkID0gdGhpcy5faXRIZWFkOyByZWNvcmQgIT09IG51bGw7IHJlY29yZCA9IHJlY29yZC5fbmV4dCkge1xuICAgICAgZm4ocmVjb3JkKTtcbiAgICB9XG4gIH1cblxuICBmb3JFYWNoUHJldmlvdXNJdGVtKGZuOiBGdW5jdGlvbikge1xuICAgIHZhciByZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQ7XG4gICAgZm9yIChyZWNvcmQgPSB0aGlzLl9wcmV2aW91c0l0SGVhZDsgcmVjb3JkICE9PSBudWxsOyByZWNvcmQgPSByZWNvcmQuX25leHRQcmV2aW91cykge1xuICAgICAgZm4ocmVjb3JkKTtcbiAgICB9XG4gIH1cblxuICBmb3JFYWNoQWRkZWRJdGVtKGZuOiBGdW5jdGlvbikge1xuICAgIHZhciByZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQ7XG4gICAgZm9yIChyZWNvcmQgPSB0aGlzLl9hZGRpdGlvbnNIZWFkOyByZWNvcmQgIT09IG51bGw7IHJlY29yZCA9IHJlY29yZC5fbmV4dEFkZGVkKSB7XG4gICAgICBmbihyZWNvcmQpO1xuICAgIH1cbiAgfVxuXG4gIGZvckVhY2hNb3ZlZEl0ZW0oZm46IEZ1bmN0aW9uKSB7XG4gICAgdmFyIHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZDtcbiAgICBmb3IgKHJlY29yZCA9IHRoaXMuX21vdmVzSGVhZDsgcmVjb3JkICE9PSBudWxsOyByZWNvcmQgPSByZWNvcmQuX25leHRNb3ZlZCkge1xuICAgICAgZm4ocmVjb3JkKTtcbiAgICB9XG4gIH1cblxuICBmb3JFYWNoUmVtb3ZlZEl0ZW0oZm46IEZ1bmN0aW9uKSB7XG4gICAgdmFyIHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZDtcbiAgICBmb3IgKHJlY29yZCA9IHRoaXMuX3JlbW92YWxzSGVhZDsgcmVjb3JkICE9PSBudWxsOyByZWNvcmQgPSByZWNvcmQuX25leHRSZW1vdmVkKSB7XG4gICAgICBmbihyZWNvcmQpO1xuICAgIH1cbiAgfVxuXG4gIGRpZmYoY29sbGVjdGlvbjogYW55KTogRGVmYXVsdEl0ZXJhYmxlRGlmZmVyIHtcbiAgICBpZiAoaXNCbGFuayhjb2xsZWN0aW9uKSkgY29sbGVjdGlvbiA9IFtdO1xuICAgIGlmICghaXNMaXN0TGlrZUl0ZXJhYmxlKGNvbGxlY3Rpb24pKSB7XG4gICAgICB0aHJvdyBuZXcgQmFzZUV4Y2VwdGlvbihgRXJyb3IgdHJ5aW5nIHRvIGRpZmYgJyR7Y29sbGVjdGlvbn0nYCk7XG4gICAgfVxuXG4gICAgaWYgKHRoaXMuY2hlY2soY29sbGVjdGlvbikpIHtcbiAgICAgIHJldHVybiB0aGlzO1xuICAgIH0gZWxzZSB7XG4gICAgICByZXR1cm4gbnVsbDtcbiAgICB9XG4gIH1cblxuICBvbkRlc3Ryb3koKSB7fVxuXG4gIC8vIHRvZG8odmljYik6IG9wdGltIGZvciBVbm1vZGlmaWFibGVMaXN0VmlldyAoZnJvemVuIGFycmF5cylcbiAgY2hlY2soY29sbGVjdGlvbjogYW55KTogYm9vbGVhbiB7XG4gICAgdGhpcy5fcmVzZXQoKTtcblxuICAgIHZhciByZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSB0aGlzLl9pdEhlYWQ7XG4gICAgdmFyIG1heUJlRGlydHk6IGJvb2xlYW4gPSBmYWxzZTtcbiAgICB2YXIgaW5kZXg6IG51bWJlcjtcbiAgICB2YXIgaXRlbTtcbiAgICB2YXIgaXRlbVRyYWNrQnk7XG4gICAgaWYgKGlzQXJyYXkoY29sbGVjdGlvbikpIHtcbiAgICAgIHZhciBsaXN0ID0gY29sbGVjdGlvbjtcbiAgICAgIHRoaXMuX2xlbmd0aCA9IGNvbGxlY3Rpb24ubGVuZ3RoO1xuXG4gICAgICBmb3IgKGluZGV4ID0gMDsgaW5kZXggPCB0aGlzLl9sZW5ndGg7IGluZGV4KyspIHtcbiAgICAgICAgaXRlbSA9IGxpc3RbaW5kZXhdO1xuICAgICAgICBpdGVtVHJhY2tCeSA9IHRoaXMuX3RyYWNrQnlGbihpbmRleCwgaXRlbSk7XG4gICAgICAgIGlmIChyZWNvcmQgPT09IG51bGwgfHwgIWxvb3NlSWRlbnRpY2FsKHJlY29yZC50cmFja0J5SWQsIGl0ZW1UcmFja0J5KSkge1xuICAgICAgICAgIHJlY29yZCA9IHRoaXMuX21pc21hdGNoKHJlY29yZCwgaXRlbSwgaXRlbVRyYWNrQnksIGluZGV4KTtcbiAgICAgICAgICBtYXlCZURpcnR5ID0gdHJ1ZTtcbiAgICAgICAgfSBlbHNlIHtcbiAgICAgICAgICBpZiAobWF5QmVEaXJ0eSkge1xuICAgICAgICAgICAgLy8gVE9ETyhtaXNrbyk6IGNhbiB3ZSBsaW1pdCB0aGlzIHRvIGR1cGxpY2F0ZXMgb25seT9cbiAgICAgICAgICAgIHJlY29yZCA9IHRoaXMuX3ZlcmlmeVJlaW5zZXJ0aW9uKHJlY29yZCwgaXRlbSwgaXRlbVRyYWNrQnksIGluZGV4KTtcbiAgICAgICAgICB9XG4gICAgICAgICAgcmVjb3JkLml0ZW0gPSBpdGVtO1xuICAgICAgICB9XG5cbiAgICAgICAgcmVjb3JkID0gcmVjb3JkLl9uZXh0O1xuICAgICAgfVxuICAgIH0gZWxzZSB7XG4gICAgICBpbmRleCA9IDA7XG4gICAgICBpdGVyYXRlTGlzdExpa2UoY29sbGVjdGlvbiwgKGl0ZW0pID0+IHtcbiAgICAgICAgaXRlbVRyYWNrQnkgPSB0aGlzLl90cmFja0J5Rm4oaW5kZXgsIGl0ZW0pO1xuICAgICAgICBpZiAocmVjb3JkID09PSBudWxsIHx8ICFsb29zZUlkZW50aWNhbChyZWNvcmQudHJhY2tCeUlkLCBpdGVtVHJhY2tCeSkpIHtcbiAgICAgICAgICByZWNvcmQgPSB0aGlzLl9taXNtYXRjaChyZWNvcmQsIGl0ZW0sIGl0ZW1UcmFja0J5LCBpbmRleCk7XG4gICAgICAgICAgbWF5QmVEaXJ0eSA9IHRydWU7XG4gICAgICAgIH0gZWxzZSBpZiAobWF5QmVEaXJ0eSkge1xuICAgICAgICAgIC8vIFRPRE8obWlza28pOiBjYW4gd2UgbGltaXQgdGhpcyB0byBkdXBsaWNhdGVzIG9ubHk/XG4gICAgICAgICAgcmVjb3JkID0gdGhpcy5fdmVyaWZ5UmVpbnNlcnRpb24ocmVjb3JkLCBpdGVtLCBpdGVtVHJhY2tCeSwgaW5kZXgpO1xuICAgICAgICB9XG4gICAgICAgIHJlY29yZCA9IHJlY29yZC5fbmV4dDtcbiAgICAgICAgaW5kZXgrKztcbiAgICAgIH0pO1xuICAgICAgdGhpcy5fbGVuZ3RoID0gaW5kZXg7XG4gICAgfVxuXG4gICAgdGhpcy5fdHJ1bmNhdGUocmVjb3JkKTtcbiAgICB0aGlzLl9jb2xsZWN0aW9uID0gY29sbGVjdGlvbjtcbiAgICByZXR1cm4gdGhpcy5pc0RpcnR5O1xuICB9XG5cbiAgLy8gQ29sbGVjdGlvbkNoYW5nZXMgaXMgY29uc2lkZXJlZCBkaXJ0eSBpZiBpdCBoYXMgYW55IGFkZGl0aW9ucywgbW92ZXMgb3IgcmVtb3ZhbHMuXG4gIGdldCBpc0RpcnR5KCk6IGJvb2xlYW4ge1xuICAgIHJldHVybiB0aGlzLl9hZGRpdGlvbnNIZWFkICE9PSBudWxsIHx8IHRoaXMuX21vdmVzSGVhZCAhPT0gbnVsbCB8fCB0aGlzLl9yZW1vdmFsc0hlYWQgIT09IG51bGw7XG4gIH1cblxuICAvKipcbiAgICogUmVzZXQgdGhlIHN0YXRlIG9mIHRoZSBjaGFuZ2Ugb2JqZWN0cyB0byBzaG93IG5vIGNoYW5nZXMuIFRoaXMgbWVhbnMgc2V0IHByZXZpb3VzS2V5IHRvXG4gICAqIGN1cnJlbnRLZXksIGFuZCBjbGVhciBhbGwgb2YgdGhlIHF1ZXVlcyAoYWRkaXRpb25zLCBtb3ZlcywgcmVtb3ZhbHMpLlxuICAgKiBTZXQgdGhlIHByZXZpb3VzSW5kZXhlcyBvZiBtb3ZlZCBhbmQgYWRkZWQgaXRlbXMgdG8gdGhlaXIgY3VycmVudEluZGV4ZXNcbiAgICogUmVzZXQgdGhlIGxpc3Qgb2YgYWRkaXRpb25zLCBtb3ZlcyBhbmQgcmVtb3ZhbHNcbiAgICpcbiAgICogQGludGVybmFsXG4gICAqL1xuICBfcmVzZXQoKSB7XG4gICAgaWYgKHRoaXMuaXNEaXJ0eSkge1xuICAgICAgdmFyIHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZDtcbiAgICAgIHZhciBuZXh0UmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkO1xuXG4gICAgICBmb3IgKHJlY29yZCA9IHRoaXMuX3ByZXZpb3VzSXRIZWFkID0gdGhpcy5faXRIZWFkOyByZWNvcmQgIT09IG51bGw7IHJlY29yZCA9IHJlY29yZC5fbmV4dCkge1xuICAgICAgICByZWNvcmQuX25leHRQcmV2aW91cyA9IHJlY29yZC5fbmV4dDtcbiAgICAgIH1cblxuICAgICAgZm9yIChyZWNvcmQgPSB0aGlzLl9hZGRpdGlvbnNIZWFkOyByZWNvcmQgIT09IG51bGw7IHJlY29yZCA9IHJlY29yZC5fbmV4dEFkZGVkKSB7XG4gICAgICAgIHJlY29yZC5wcmV2aW91c0luZGV4ID0gcmVjb3JkLmN1cnJlbnRJbmRleDtcbiAgICAgIH1cbiAgICAgIHRoaXMuX2FkZGl0aW9uc0hlYWQgPSB0aGlzLl9hZGRpdGlvbnNUYWlsID0gbnVsbDtcblxuICAgICAgZm9yIChyZWNvcmQgPSB0aGlzLl9tb3Zlc0hlYWQ7IHJlY29yZCAhPT0gbnVsbDsgcmVjb3JkID0gbmV4dFJlY29yZCkge1xuICAgICAgICByZWNvcmQucHJldmlvdXNJbmRleCA9IHJlY29yZC5jdXJyZW50SW5kZXg7XG4gICAgICAgIG5leHRSZWNvcmQgPSByZWNvcmQuX25leHRNb3ZlZDtcbiAgICAgIH1cbiAgICAgIHRoaXMuX21vdmVzSGVhZCA9IHRoaXMuX21vdmVzVGFpbCA9IG51bGw7XG4gICAgICB0aGlzLl9yZW1vdmFsc0hlYWQgPSB0aGlzLl9yZW1vdmFsc1RhaWwgPSBudWxsO1xuXG4gICAgICAvLyB0b2RvKHZpY2IpIHdoZW4gYXNzZXJ0IGdldHMgc3VwcG9ydGVkXG4gICAgICAvLyBhc3NlcnQoIXRoaXMuaXNEaXJ0eSk7XG4gICAgfVxuICB9XG5cbiAgLyoqXG4gICAqIFRoaXMgaXMgdGhlIGNvcmUgZnVuY3Rpb24gd2hpY2ggaGFuZGxlcyBkaWZmZXJlbmNlcyBiZXR3ZWVuIGNvbGxlY3Rpb25zLlxuICAgKlxuICAgKiAtIGByZWNvcmRgIGlzIHRoZSByZWNvcmQgd2hpY2ggd2Ugc2F3IGF0IHRoaXMgcG9zaXRpb24gbGFzdCB0aW1lLiBJZiBudWxsIHRoZW4gaXQgaXMgYSBuZXdcbiAgICogICBpdGVtLlxuICAgKiAtIGBpdGVtYCBpcyB0aGUgY3VycmVudCBpdGVtIGluIHRoZSBjb2xsZWN0aW9uXG4gICAqIC0gYGluZGV4YCBpcyB0aGUgcG9zaXRpb24gb2YgdGhlIGl0ZW0gaW4gdGhlIGNvbGxlY3Rpb25cbiAgICpcbiAgICogQGludGVybmFsXG4gICAqL1xuICBfbWlzbWF0Y2gocmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkLCBpdGVtOiBhbnksIGl0ZW1UcmFja0J5OiBhbnksXG4gICAgICAgICAgICBpbmRleDogbnVtYmVyKTogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB7XG4gICAgLy8gVGhlIHByZXZpb3VzIHJlY29yZCBhZnRlciB3aGljaCB3ZSB3aWxsIGFwcGVuZCB0aGUgY3VycmVudCBvbmUuXG4gICAgdmFyIHByZXZpb3VzUmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkO1xuXG4gICAgaWYgKHJlY29yZCA9PT0gbnVsbCkge1xuICAgICAgcHJldmlvdXNSZWNvcmQgPSB0aGlzLl9pdFRhaWw7XG4gICAgfSBlbHNlIHtcbiAgICAgIHByZXZpb3VzUmVjb3JkID0gcmVjb3JkLl9wcmV2O1xuICAgICAgLy8gUmVtb3ZlIHRoZSByZWNvcmQgZnJvbSB0aGUgY29sbGVjdGlvbiBzaW5jZSB3ZSBrbm93IGl0IGRvZXMgbm90IG1hdGNoIHRoZSBpdGVtLlxuICAgICAgdGhpcy5fcmVtb3ZlKHJlY29yZCk7XG4gICAgfVxuXG4gICAgLy8gQXR0ZW1wdCB0byBzZWUgaWYgd2UgaGF2ZSBzZWVuIHRoZSBpdGVtIGJlZm9yZS5cbiAgICByZWNvcmQgPSB0aGlzLl9saW5rZWRSZWNvcmRzID09PSBudWxsID8gbnVsbCA6IHRoaXMuX2xpbmtlZFJlY29yZHMuZ2V0KGl0ZW1UcmFja0J5LCBpbmRleCk7XG4gICAgaWYgKHJlY29yZCAhPT0gbnVsbCkge1xuICAgICAgLy8gV2UgaGF2ZSBzZWVuIHRoaXMgYmVmb3JlLCB3ZSBuZWVkIHRvIG1vdmUgaXQgZm9yd2FyZCBpbiB0aGUgY29sbGVjdGlvbi5cbiAgICAgIHRoaXMuX21vdmVBZnRlcihyZWNvcmQsIHByZXZpb3VzUmVjb3JkLCBpbmRleCk7XG4gICAgfSBlbHNlIHtcbiAgICAgIC8vIE5ldmVyIHNlZW4gaXQsIGNoZWNrIGV2aWN0ZWQgbGlzdC5cbiAgICAgIHJlY29yZCA9IHRoaXMuX3VubGlua2VkUmVjb3JkcyA9PT0gbnVsbCA/IG51bGwgOiB0aGlzLl91bmxpbmtlZFJlY29yZHMuZ2V0KGl0ZW1UcmFja0J5KTtcbiAgICAgIGlmIChyZWNvcmQgIT09IG51bGwpIHtcbiAgICAgICAgLy8gSXQgaXMgYW4gaXRlbSB3aGljaCB3ZSBoYXZlIGV2aWN0ZWQgZWFybGllcjogcmVpbnNlcnQgaXQgYmFjayBpbnRvIHRoZSBsaXN0LlxuICAgICAgICB0aGlzLl9yZWluc2VydEFmdGVyKHJlY29yZCwgcHJldmlvdXNSZWNvcmQsIGluZGV4KTtcbiAgICAgIH0gZWxzZSB7XG4gICAgICAgIC8vIEl0IGlzIGEgbmV3IGl0ZW06IGFkZCBpdC5cbiAgICAgICAgcmVjb3JkID1cbiAgICAgICAgICAgIHRoaXMuX2FkZEFmdGVyKG5ldyBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkKGl0ZW0sIGl0ZW1UcmFja0J5KSwgcHJldmlvdXNSZWNvcmQsIGluZGV4KTtcbiAgICAgIH1cbiAgICB9XG4gICAgcmV0dXJuIHJlY29yZDtcbiAgfVxuXG4gIC8qKlxuICAgKiBUaGlzIGNoZWNrIGlzIG9ubHkgbmVlZGVkIGlmIGFuIGFycmF5IGNvbnRhaW5zIGR1cGxpY2F0ZXMuIChTaG9ydCBjaXJjdWl0IG9mIG5vdGhpbmcgZGlydHkpXG4gICAqXG4gICAqIFVzZSBjYXNlOiBgW2EsIGFdYCA9PiBgW2IsIGEsIGFdYFxuICAgKlxuICAgKiBJZiB3ZSBkaWQgbm90IGhhdmUgdGhpcyBjaGVjayB0aGVuIHRoZSBpbnNlcnRpb24gb2YgYGJgIHdvdWxkOlxuICAgKiAgIDEpIGV2aWN0IGZpcnN0IGBhYFxuICAgKiAgIDIpIGluc2VydCBgYmAgYXQgYDBgIGluZGV4LlxuICAgKiAgIDMpIGxlYXZlIGBhYCBhdCBpbmRleCBgMWAgYXMgaXMuIDwtLSB0aGlzIGlzIHdyb25nIVxuICAgKiAgIDMpIHJlaW5zZXJ0IGBhYCBhdCBpbmRleCAyLiA8LS0gdGhpcyBpcyB3cm9uZyFcbiAgICpcbiAgICogVGhlIGNvcnJlY3QgYmVoYXZpb3IgaXM6XG4gICAqICAgMSkgZXZpY3QgZmlyc3QgYGFgXG4gICAqICAgMikgaW5zZXJ0IGBiYCBhdCBgMGAgaW5kZXguXG4gICAqICAgMykgcmVpbnNlcnQgYGFgIGF0IGluZGV4IDEuXG4gICAqICAgMykgbW92ZSBgYWAgYXQgZnJvbSBgMWAgdG8gYDJgLlxuICAgKlxuICAgKlxuICAgKiBEb3VibGUgY2hlY2sgdGhhdCB3ZSBoYXZlIG5vdCBldmljdGVkIGEgZHVwbGljYXRlIGl0ZW0uIFdlIG5lZWQgdG8gY2hlY2sgaWYgdGhlIGl0ZW0gdHlwZSBtYXlcbiAgICogaGF2ZSBhbHJlYWR5IGJlZW4gcmVtb3ZlZDpcbiAgICogVGhlIGluc2VydGlvbiBvZiBiIHdpbGwgZXZpY3QgdGhlIGZpcnN0ICdhJy4gSWYgd2UgZG9uJ3QgcmVpbnNlcnQgaXQgbm93IGl0IHdpbGwgYmUgcmVpbnNlcnRlZFxuICAgKiBhdCB0aGUgZW5kLiBXaGljaCB3aWxsIHNob3cgdXAgYXMgdGhlIHR3byAnYSdzIHN3aXRjaGluZyBwb3NpdGlvbi4gVGhpcyBpcyBpbmNvcnJlY3QsIHNpbmNlIGFcbiAgICogYmV0dGVyIHdheSB0byB0aGluayBvZiBpdCBpcyBhcyBpbnNlcnQgb2YgJ2InIHJhdGhlciB0aGVuIHN3aXRjaCAnYScgd2l0aCAnYicgYW5kIHRoZW4gYWRkICdhJ1xuICAgKiBhdCB0aGUgZW5kLlxuICAgKlxuICAgKiBAaW50ZXJuYWxcbiAgICovXG4gIF92ZXJpZnlSZWluc2VydGlvbihyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQsIGl0ZW06IGFueSwgaXRlbVRyYWNrQnk6IGFueSxcbiAgICAgICAgICAgICAgICAgICAgIGluZGV4OiBudW1iZXIpOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgICB2YXIgcmVpbnNlcnRSZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPVxuICAgICAgICB0aGlzLl91bmxpbmtlZFJlY29yZHMgPT09IG51bGwgPyBudWxsIDogdGhpcy5fdW5saW5rZWRSZWNvcmRzLmdldChpdGVtVHJhY2tCeSk7XG4gICAgaWYgKHJlaW5zZXJ0UmVjb3JkICE9PSBudWxsKSB7XG4gICAgICByZWNvcmQgPSB0aGlzLl9yZWluc2VydEFmdGVyKHJlaW5zZXJ0UmVjb3JkLCByZWNvcmQuX3ByZXYsIGluZGV4KTtcbiAgICB9IGVsc2UgaWYgKHJlY29yZC5jdXJyZW50SW5kZXggIT0gaW5kZXgpIHtcbiAgICAgIHJlY29yZC5jdXJyZW50SW5kZXggPSBpbmRleDtcbiAgICAgIHRoaXMuX2FkZFRvTW92ZXMocmVjb3JkLCBpbmRleCk7XG4gICAgfVxuICAgIHJlY29yZC5pdGVtID0gaXRlbTtcbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgLyoqXG4gICAqIEdldCByaWQgb2YgYW55IGV4Y2VzcyB7QGxpbmsgQ29sbGVjdGlvbkNoYW5nZVJlY29yZH1zIGZyb20gdGhlIHByZXZpb3VzIGNvbGxlY3Rpb25cbiAgICpcbiAgICogLSBgcmVjb3JkYCBUaGUgZmlyc3QgZXhjZXNzIHtAbGluayBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkfS5cbiAgICpcbiAgICogQGludGVybmFsXG4gICAqL1xuICBfdHJ1bmNhdGUocmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkKSB7XG4gICAgLy8gQW55dGhpbmcgYWZ0ZXIgdGhhdCBuZWVkcyB0byBiZSByZW1vdmVkO1xuICAgIHdoaWxlIChyZWNvcmQgIT09IG51bGwpIHtcbiAgICAgIHZhciBuZXh0UmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gcmVjb3JkLl9uZXh0O1xuICAgICAgdGhpcy5fYWRkVG9SZW1vdmFscyh0aGlzLl91bmxpbmsocmVjb3JkKSk7XG4gICAgICByZWNvcmQgPSBuZXh0UmVjb3JkO1xuICAgIH1cbiAgICBpZiAodGhpcy5fdW5saW5rZWRSZWNvcmRzICE9PSBudWxsKSB7XG4gICAgICB0aGlzLl91bmxpbmtlZFJlY29yZHMuY2xlYXIoKTtcbiAgICB9XG5cbiAgICBpZiAodGhpcy5fYWRkaXRpb25zVGFpbCAhPT0gbnVsbCkge1xuICAgICAgdGhpcy5fYWRkaXRpb25zVGFpbC5fbmV4dEFkZGVkID0gbnVsbDtcbiAgICB9XG4gICAgaWYgKHRoaXMuX21vdmVzVGFpbCAhPT0gbnVsbCkge1xuICAgICAgdGhpcy5fbW92ZXNUYWlsLl9uZXh0TW92ZWQgPSBudWxsO1xuICAgIH1cbiAgICBpZiAodGhpcy5faXRUYWlsICE9PSBudWxsKSB7XG4gICAgICB0aGlzLl9pdFRhaWwuX25leHQgPSBudWxsO1xuICAgIH1cbiAgICBpZiAodGhpcy5fcmVtb3ZhbHNUYWlsICE9PSBudWxsKSB7XG4gICAgICB0aGlzLl9yZW1vdmFsc1RhaWwuX25leHRSZW1vdmVkID0gbnVsbDtcbiAgICB9XG4gIH1cblxuICAvKiogQGludGVybmFsICovXG4gIF9yZWluc2VydEFmdGVyKHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCwgcHJldlJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCxcbiAgICAgICAgICAgICAgICAgaW5kZXg6IG51bWJlcik6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQge1xuICAgIGlmICh0aGlzLl91bmxpbmtlZFJlY29yZHMgIT09IG51bGwpIHtcbiAgICAgIHRoaXMuX3VubGlua2VkUmVjb3Jkcy5yZW1vdmUocmVjb3JkKTtcbiAgICB9XG4gICAgdmFyIHByZXYgPSByZWNvcmQuX3ByZXZSZW1vdmVkO1xuICAgIHZhciBuZXh0ID0gcmVjb3JkLl9uZXh0UmVtb3ZlZDtcblxuICAgIGlmIChwcmV2ID09PSBudWxsKSB7XG4gICAgICB0aGlzLl9yZW1vdmFsc0hlYWQgPSBuZXh0O1xuICAgIH0gZWxzZSB7XG4gICAgICBwcmV2Ll9uZXh0UmVtb3ZlZCA9IG5leHQ7XG4gICAgfVxuICAgIGlmIChuZXh0ID09PSBudWxsKSB7XG4gICAgICB0aGlzLl9yZW1vdmFsc1RhaWwgPSBwcmV2O1xuICAgIH0gZWxzZSB7XG4gICAgICBuZXh0Ll9wcmV2UmVtb3ZlZCA9IHByZXY7XG4gICAgfVxuXG4gICAgdGhpcy5faW5zZXJ0QWZ0ZXIocmVjb3JkLCBwcmV2UmVjb3JkLCBpbmRleCk7XG4gICAgdGhpcy5fYWRkVG9Nb3ZlcyhyZWNvcmQsIGluZGV4KTtcbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfbW92ZUFmdGVyKHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCwgcHJldlJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCxcbiAgICAgICAgICAgICBpbmRleDogbnVtYmVyKTogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB7XG4gICAgdGhpcy5fdW5saW5rKHJlY29yZCk7XG4gICAgdGhpcy5faW5zZXJ0QWZ0ZXIocmVjb3JkLCBwcmV2UmVjb3JkLCBpbmRleCk7XG4gICAgdGhpcy5fYWRkVG9Nb3ZlcyhyZWNvcmQsIGluZGV4KTtcbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfYWRkQWZ0ZXIocmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkLCBwcmV2UmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkLFxuICAgICAgICAgICAgaW5kZXg6IG51bWJlcik6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQge1xuICAgIHRoaXMuX2luc2VydEFmdGVyKHJlY29yZCwgcHJldlJlY29yZCwgaW5kZXgpO1xuXG4gICAgaWYgKHRoaXMuX2FkZGl0aW9uc1RhaWwgPT09IG51bGwpIHtcbiAgICAgIC8vIHRvZG8odmljYilcbiAgICAgIC8vIGFzc2VydCh0aGlzLl9hZGRpdGlvbnNIZWFkID09PSBudWxsKTtcbiAgICAgIHRoaXMuX2FkZGl0aW9uc1RhaWwgPSB0aGlzLl9hZGRpdGlvbnNIZWFkID0gcmVjb3JkO1xuICAgIH0gZWxzZSB7XG4gICAgICAvLyB0b2RvKHZpY2IpXG4gICAgICAvLyBhc3NlcnQoX2FkZGl0aW9uc1RhaWwuX25leHRBZGRlZCA9PT0gbnVsbCk7XG4gICAgICAvLyBhc3NlcnQocmVjb3JkLl9uZXh0QWRkZWQgPT09IG51bGwpO1xuICAgICAgdGhpcy5fYWRkaXRpb25zVGFpbCA9IHRoaXMuX2FkZGl0aW9uc1RhaWwuX25leHRBZGRlZCA9IHJlY29yZDtcbiAgICB9XG4gICAgcmV0dXJuIHJlY29yZDtcbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX2luc2VydEFmdGVyKHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCwgcHJldlJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCxcbiAgICAgICAgICAgICAgIGluZGV4OiBudW1iZXIpOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgICAvLyB0b2RvKHZpY2IpXG4gICAgLy8gYXNzZXJ0KHJlY29yZCAhPSBwcmV2UmVjb3JkKTtcbiAgICAvLyBhc3NlcnQocmVjb3JkLl9uZXh0ID09PSBudWxsKTtcbiAgICAvLyBhc3NlcnQocmVjb3JkLl9wcmV2ID09PSBudWxsKTtcblxuICAgIHZhciBuZXh0OiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gcHJldlJlY29yZCA9PT0gbnVsbCA/IHRoaXMuX2l0SGVhZCA6IHByZXZSZWNvcmQuX25leHQ7XG4gICAgLy8gdG9kbyh2aWNiKVxuICAgIC8vIGFzc2VydChuZXh0ICE9IHJlY29yZCk7XG4gICAgLy8gYXNzZXJ0KHByZXZSZWNvcmQgIT0gcmVjb3JkKTtcbiAgICByZWNvcmQuX25leHQgPSBuZXh0O1xuICAgIHJlY29yZC5fcHJldiA9IHByZXZSZWNvcmQ7XG4gICAgaWYgKG5leHQgPT09IG51bGwpIHtcbiAgICAgIHRoaXMuX2l0VGFpbCA9IHJlY29yZDtcbiAgICB9IGVsc2Uge1xuICAgICAgbmV4dC5fcHJldiA9IHJlY29yZDtcbiAgICB9XG4gICAgaWYgKHByZXZSZWNvcmQgPT09IG51bGwpIHtcbiAgICAgIHRoaXMuX2l0SGVhZCA9IHJlY29yZDtcbiAgICB9IGVsc2Uge1xuICAgICAgcHJldlJlY29yZC5fbmV4dCA9IHJlY29yZDtcbiAgICB9XG5cbiAgICBpZiAodGhpcy5fbGlua2VkUmVjb3JkcyA9PT0gbnVsbCkge1xuICAgICAgdGhpcy5fbGlua2VkUmVjb3JkcyA9IG5ldyBfRHVwbGljYXRlTWFwKCk7XG4gICAgfVxuICAgIHRoaXMuX2xpbmtlZFJlY29yZHMucHV0KHJlY29yZCk7XG5cbiAgICByZWNvcmQuY3VycmVudEluZGV4ID0gaW5kZXg7XG4gICAgcmV0dXJuIHJlY29yZDtcbiAgfVxuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX3JlbW92ZShyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQpOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgICByZXR1cm4gdGhpcy5fYWRkVG9SZW1vdmFscyh0aGlzLl91bmxpbmsocmVjb3JkKSk7XG4gIH1cblxuICAvKiogQGludGVybmFsICovXG4gIF91bmxpbmsocmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkKTogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB7XG4gICAgaWYgKHRoaXMuX2xpbmtlZFJlY29yZHMgIT09IG51bGwpIHtcbiAgICAgIHRoaXMuX2xpbmtlZFJlY29yZHMucmVtb3ZlKHJlY29yZCk7XG4gICAgfVxuXG4gICAgdmFyIHByZXYgPSByZWNvcmQuX3ByZXY7XG4gICAgdmFyIG5leHQgPSByZWNvcmQuX25leHQ7XG5cbiAgICAvLyB0b2RvKHZpY2IpXG4gICAgLy8gYXNzZXJ0KChyZWNvcmQuX3ByZXYgPSBudWxsKSA9PT0gbnVsbCk7XG4gICAgLy8gYXNzZXJ0KChyZWNvcmQuX25leHQgPSBudWxsKSA9PT0gbnVsbCk7XG5cbiAgICBpZiAocHJldiA9PT0gbnVsbCkge1xuICAgICAgdGhpcy5faXRIZWFkID0gbmV4dDtcbiAgICB9IGVsc2Uge1xuICAgICAgcHJldi5fbmV4dCA9IG5leHQ7XG4gICAgfVxuICAgIGlmIChuZXh0ID09PSBudWxsKSB7XG4gICAgICB0aGlzLl9pdFRhaWwgPSBwcmV2O1xuICAgIH0gZWxzZSB7XG4gICAgICBuZXh0Ll9wcmV2ID0gcHJldjtcbiAgICB9XG5cbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfYWRkVG9Nb3ZlcyhyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQsIHRvSW5kZXg6IG51bWJlcik6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQge1xuICAgIC8vIHRvZG8odmljYilcbiAgICAvLyBhc3NlcnQocmVjb3JkLl9uZXh0TW92ZWQgPT09IG51bGwpO1xuXG4gICAgaWYgKHJlY29yZC5wcmV2aW91c0luZGV4ID09PSB0b0luZGV4KSB7XG4gICAgICByZXR1cm4gcmVjb3JkO1xuICAgIH1cblxuICAgIGlmICh0aGlzLl9tb3Zlc1RhaWwgPT09IG51bGwpIHtcbiAgICAgIC8vIHRvZG8odmljYilcbiAgICAgIC8vIGFzc2VydChfbW92ZXNIZWFkID09PSBudWxsKTtcbiAgICAgIHRoaXMuX21vdmVzVGFpbCA9IHRoaXMuX21vdmVzSGVhZCA9IHJlY29yZDtcbiAgICB9IGVsc2Uge1xuICAgICAgLy8gdG9kbyh2aWNiKVxuICAgICAgLy8gYXNzZXJ0KF9tb3Zlc1RhaWwuX25leHRNb3ZlZCA9PT0gbnVsbCk7XG4gICAgICB0aGlzLl9tb3Zlc1RhaWwgPSB0aGlzLl9tb3Zlc1RhaWwuX25leHRNb3ZlZCA9IHJlY29yZDtcbiAgICB9XG5cbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfYWRkVG9SZW1vdmFscyhyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQpOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgICBpZiAodGhpcy5fdW5saW5rZWRSZWNvcmRzID09PSBudWxsKSB7XG4gICAgICB0aGlzLl91bmxpbmtlZFJlY29yZHMgPSBuZXcgX0R1cGxpY2F0ZU1hcCgpO1xuICAgIH1cbiAgICB0aGlzLl91bmxpbmtlZFJlY29yZHMucHV0KHJlY29yZCk7XG4gICAgcmVjb3JkLmN1cnJlbnRJbmRleCA9IG51bGw7XG4gICAgcmVjb3JkLl9uZXh0UmVtb3ZlZCA9IG51bGw7XG5cbiAgICBpZiAodGhpcy5fcmVtb3ZhbHNUYWlsID09PSBudWxsKSB7XG4gICAgICAvLyB0b2RvKHZpY2IpXG4gICAgICAvLyBhc3NlcnQoX3JlbW92YWxzSGVhZCA9PT0gbnVsbCk7XG4gICAgICB0aGlzLl9yZW1vdmFsc1RhaWwgPSB0aGlzLl9yZW1vdmFsc0hlYWQgPSByZWNvcmQ7XG4gICAgICByZWNvcmQuX3ByZXZSZW1vdmVkID0gbnVsbDtcbiAgICB9IGVsc2Uge1xuICAgICAgLy8gdG9kbyh2aWNiKVxuICAgICAgLy8gYXNzZXJ0KF9yZW1vdmFsc1RhaWwuX25leHRSZW1vdmVkID09PSBudWxsKTtcbiAgICAgIC8vIGFzc2VydChyZWNvcmQuX25leHRSZW1vdmVkID09PSBudWxsKTtcbiAgICAgIHJlY29yZC5fcHJldlJlbW92ZWQgPSB0aGlzLl9yZW1vdmFsc1RhaWw7XG4gICAgICB0aGlzLl9yZW1vdmFsc1RhaWwgPSB0aGlzLl9yZW1vdmFsc1RhaWwuX25leHRSZW1vdmVkID0gcmVjb3JkO1xuICAgIH1cbiAgICByZXR1cm4gcmVjb3JkO1xuICB9XG5cbiAgdG9TdHJpbmcoKTogc3RyaW5nIHtcbiAgICB2YXIgbGlzdCA9IFtdO1xuICAgIHRoaXMuZm9yRWFjaEl0ZW0oKHJlY29yZCkgPT4gbGlzdC5wdXNoKHJlY29yZCkpO1xuXG4gICAgdmFyIHByZXZpb3VzID0gW107XG4gICAgdGhpcy5mb3JFYWNoUHJldmlvdXNJdGVtKChyZWNvcmQpID0+IHByZXZpb3VzLnB1c2gocmVjb3JkKSk7XG5cbiAgICB2YXIgYWRkaXRpb25zID0gW107XG4gICAgdGhpcy5mb3JFYWNoQWRkZWRJdGVtKChyZWNvcmQpID0+IGFkZGl0aW9ucy5wdXNoKHJlY29yZCkpO1xuXG4gICAgdmFyIG1vdmVzID0gW107XG4gICAgdGhpcy5mb3JFYWNoTW92ZWRJdGVtKChyZWNvcmQpID0+IG1vdmVzLnB1c2gocmVjb3JkKSk7XG5cbiAgICB2YXIgcmVtb3ZhbHMgPSBbXTtcbiAgICB0aGlzLmZvckVhY2hSZW1vdmVkSXRlbSgocmVjb3JkKSA9PiByZW1vdmFscy5wdXNoKHJlY29yZCkpO1xuXG4gICAgcmV0dXJuIFwiY29sbGVjdGlvbjogXCIgKyBsaXN0LmpvaW4oJywgJykgKyBcIlxcblwiICsgXCJwcmV2aW91czogXCIgKyBwcmV2aW91cy5qb2luKCcsICcpICsgXCJcXG5cIiArXG4gICAgICAgICAgIFwiYWRkaXRpb25zOiBcIiArIGFkZGl0aW9ucy5qb2luKCcsICcpICsgXCJcXG5cIiArIFwibW92ZXM6IFwiICsgbW92ZXMuam9pbignLCAnKSArIFwiXFxuXCIgK1xuICAgICAgICAgICBcInJlbW92YWxzOiBcIiArIHJlbW92YWxzLmpvaW4oJywgJykgKyBcIlxcblwiO1xuICB9XG59XG5cbmV4cG9ydCBjbGFzcyBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgY3VycmVudEluZGV4OiBudW1iZXIgPSBudWxsO1xuICBwcmV2aW91c0luZGV4OiBudW1iZXIgPSBudWxsO1xuXG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25leHRQcmV2aW91czogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX3ByZXY6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSBudWxsO1xuICAvKiogQGludGVybmFsICovXG4gIF9uZXh0OiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfcHJldkR1cDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25leHREdXA6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQgPSBudWxsO1xuICAvKiogQGludGVybmFsICovXG4gIF9wcmV2UmVtb3ZlZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG4gIC8qKiBAaW50ZXJuYWwgKi9cbiAgX25leHRSZW1vdmVkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfbmV4dEFkZGVkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfbmV4dE1vdmVkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcblxuICBjb25zdHJ1Y3RvcihwdWJsaWMgaXRlbTogYW55LCBwdWJsaWMgdHJhY2tCeUlkOiBhbnkpIHt9XG5cbiAgdG9TdHJpbmcoKTogc3RyaW5nIHtcbiAgICByZXR1cm4gdGhpcy5wcmV2aW91c0luZGV4ID09PSB0aGlzLmN1cnJlbnRJbmRleCA/XG4gICAgICAgICAgICAgICBzdHJpbmdpZnkodGhpcy5pdGVtKSA6XG4gICAgICAgICAgICAgICBzdHJpbmdpZnkodGhpcy5pdGVtKSArICdbJyArIHN0cmluZ2lmeSh0aGlzLnByZXZpb3VzSW5kZXgpICsgJy0+JyArXG4gICAgICAgICAgICAgICAgICAgc3RyaW5naWZ5KHRoaXMuY3VycmVudEluZGV4KSArICddJztcbiAgfVxufVxuXG4vLyBBIGxpbmtlZCBsaXN0IG9mIENvbGxlY3Rpb25DaGFuZ2VSZWNvcmRzIHdpdGggdGhlIHNhbWUgQ29sbGVjdGlvbkNoYW5nZVJlY29yZC5pdGVtXG5jbGFzcyBfRHVwbGljYXRlSXRlbVJlY29yZExpc3Qge1xuICAvKiogQGludGVybmFsICovXG4gIF9oZWFkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gbnVsbDtcbiAgLyoqIEBpbnRlcm5hbCAqL1xuICBfdGFpbDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCA9IG51bGw7XG5cbiAgLyoqXG4gICAqIEFwcGVuZCB0aGUgcmVjb3JkIHRvIHRoZSBsaXN0IG9mIGR1cGxpY2F0ZXMuXG4gICAqXG4gICAqIE5vdGU6IGJ5IGRlc2lnbiBhbGwgcmVjb3JkcyBpbiB0aGUgbGlzdCBvZiBkdXBsaWNhdGVzIGhvbGQgdGhlIHNhbWUgdmFsdWUgaW4gcmVjb3JkLml0ZW0uXG4gICAqL1xuICBhZGQocmVjb3JkOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkKTogdm9pZCB7XG4gICAgaWYgKHRoaXMuX2hlYWQgPT09IG51bGwpIHtcbiAgICAgIHRoaXMuX2hlYWQgPSB0aGlzLl90YWlsID0gcmVjb3JkO1xuICAgICAgcmVjb3JkLl9uZXh0RHVwID0gbnVsbDtcbiAgICAgIHJlY29yZC5fcHJldkR1cCA9IG51bGw7XG4gICAgfSBlbHNlIHtcbiAgICAgIC8vIHRvZG8odmljYilcbiAgICAgIC8vIGFzc2VydChyZWNvcmQuaXRlbSA9PSAgX2hlYWQuaXRlbSB8fFxuICAgICAgLy8gICAgICAgcmVjb3JkLml0ZW0gaXMgbnVtICYmIHJlY29yZC5pdGVtLmlzTmFOICYmIF9oZWFkLml0ZW0gaXMgbnVtICYmIF9oZWFkLml0ZW0uaXNOYU4pO1xuICAgICAgdGhpcy5fdGFpbC5fbmV4dER1cCA9IHJlY29yZDtcbiAgICAgIHJlY29yZC5fcHJldkR1cCA9IHRoaXMuX3RhaWw7XG4gICAgICByZWNvcmQuX25leHREdXAgPSBudWxsO1xuICAgICAgdGhpcy5fdGFpbCA9IHJlY29yZDtcbiAgICB9XG4gIH1cblxuICAvLyBSZXR1cm5zIGEgQ29sbGVjdGlvbkNoYW5nZVJlY29yZCBoYXZpbmcgQ29sbGVjdGlvbkNoYW5nZVJlY29yZC50cmFja0J5SWQgPT0gdHJhY2tCeUlkIGFuZFxuICAvLyBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkLmN1cnJlbnRJbmRleCA+PSBhZnRlckluZGV4XG4gIGdldCh0cmFja0J5SWQ6IGFueSwgYWZ0ZXJJbmRleDogbnVtYmVyKTogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB7XG4gICAgdmFyIHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZDtcbiAgICBmb3IgKHJlY29yZCA9IHRoaXMuX2hlYWQ7IHJlY29yZCAhPT0gbnVsbDsgcmVjb3JkID0gcmVjb3JkLl9uZXh0RHVwKSB7XG4gICAgICBpZiAoKGFmdGVySW5kZXggPT09IG51bGwgfHwgYWZ0ZXJJbmRleCA8IHJlY29yZC5jdXJyZW50SW5kZXgpICYmXG4gICAgICAgICAgbG9vc2VJZGVudGljYWwocmVjb3JkLnRyYWNrQnlJZCwgdHJhY2tCeUlkKSkge1xuICAgICAgICByZXR1cm4gcmVjb3JkO1xuICAgICAgfVxuICAgIH1cbiAgICByZXR1cm4gbnVsbDtcbiAgfVxuXG4gIC8qKlxuICAgKiBSZW1vdmUgb25lIHtAbGluayBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkfSBmcm9tIHRoZSBsaXN0IG9mIGR1cGxpY2F0ZXMuXG4gICAqXG4gICAqIFJldHVybnMgd2hldGhlciB0aGUgbGlzdCBvZiBkdXBsaWNhdGVzIGlzIGVtcHR5LlxuICAgKi9cbiAgcmVtb3ZlKHJlY29yZDogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCk6IGJvb2xlYW4ge1xuICAgIC8vIHRvZG8odmljYilcbiAgICAvLyBhc3NlcnQoKCkge1xuICAgIC8vICAvLyB2ZXJpZnkgdGhhdCB0aGUgcmVjb3JkIGJlaW5nIHJlbW92ZWQgaXMgaW4gdGhlIGxpc3QuXG4gICAgLy8gIGZvciAoQ29sbGVjdGlvbkNoYW5nZVJlY29yZCBjdXJzb3IgPSBfaGVhZDsgY3Vyc29yICE9IG51bGw7IGN1cnNvciA9IGN1cnNvci5fbmV4dER1cCkge1xuICAgIC8vICAgIGlmIChpZGVudGljYWwoY3Vyc29yLCByZWNvcmQpKSByZXR1cm4gdHJ1ZTtcbiAgICAvLyAgfVxuICAgIC8vICByZXR1cm4gZmFsc2U7XG4gICAgLy99KTtcblxuICAgIHZhciBwcmV2OiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gcmVjb3JkLl9wcmV2RHVwO1xuICAgIHZhciBuZXh0OiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkID0gcmVjb3JkLl9uZXh0RHVwO1xuICAgIGlmIChwcmV2ID09PSBudWxsKSB7XG4gICAgICB0aGlzLl9oZWFkID0gbmV4dDtcbiAgICB9IGVsc2Uge1xuICAgICAgcHJldi5fbmV4dER1cCA9IG5leHQ7XG4gICAgfVxuICAgIGlmIChuZXh0ID09PSBudWxsKSB7XG4gICAgICB0aGlzLl90YWlsID0gcHJldjtcbiAgICB9IGVsc2Uge1xuICAgICAgbmV4dC5fcHJldkR1cCA9IHByZXY7XG4gICAgfVxuICAgIHJldHVybiB0aGlzLl9oZWFkID09PSBudWxsO1xuICB9XG59XG5cbmNsYXNzIF9EdXBsaWNhdGVNYXAge1xuICBtYXAgPSBuZXcgTWFwPGFueSwgX0R1cGxpY2F0ZUl0ZW1SZWNvcmRMaXN0PigpO1xuXG4gIHB1dChyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQpIHtcbiAgICAvLyB0b2RvKHZpY2IpIGhhbmRsZSBjb3JuZXIgY2FzZXNcbiAgICB2YXIga2V5ID0gZ2V0TWFwS2V5KHJlY29yZC50cmFja0J5SWQpO1xuXG4gICAgdmFyIGR1cGxpY2F0ZXMgPSB0aGlzLm1hcC5nZXQoa2V5KTtcbiAgICBpZiAoIWlzUHJlc2VudChkdXBsaWNhdGVzKSkge1xuICAgICAgZHVwbGljYXRlcyA9IG5ldyBfRHVwbGljYXRlSXRlbVJlY29yZExpc3QoKTtcbiAgICAgIHRoaXMubWFwLnNldChrZXksIGR1cGxpY2F0ZXMpO1xuICAgIH1cbiAgICBkdXBsaWNhdGVzLmFkZChyZWNvcmQpO1xuICB9XG5cbiAgLyoqXG4gICAqIFJldHJpZXZlIHRoZSBgdmFsdWVgIHVzaW5nIGtleS4gQmVjYXVzZSB0aGUgQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB2YWx1ZSBtYXkgYmUgb25lIHdoaWNoIHdlXG4gICAqIGhhdmUgYWxyZWFkeSBpdGVyYXRlZCBvdmVyLCB3ZSB1c2UgdGhlIGFmdGVySW5kZXggdG8gcHJldGVuZCBpdCBpcyBub3QgdGhlcmUuXG4gICAqXG4gICAqIFVzZSBjYXNlOiBgW2EsIGIsIGMsIGEsIGFdYCBpZiB3ZSBhcmUgYXQgaW5kZXggYDNgIHdoaWNoIGlzIHRoZSBzZWNvbmQgYGFgIHRoZW4gYXNraW5nIGlmIHdlXG4gICAqIGhhdmUgYW55IG1vcmUgYGFgcyBuZWVkcyB0byByZXR1cm4gdGhlIGxhc3QgYGFgIG5vdCB0aGUgZmlyc3Qgb3Igc2Vjb25kLlxuICAgKi9cbiAgZ2V0KHRyYWNrQnlJZDogYW55LCBhZnRlckluZGV4OiBudW1iZXIgPSBudWxsKTogQ29sbGVjdGlvbkNoYW5nZVJlY29yZCB7XG4gICAgdmFyIGtleSA9IGdldE1hcEtleSh0cmFja0J5SWQpO1xuXG4gICAgdmFyIHJlY29yZExpc3QgPSB0aGlzLm1hcC5nZXQoa2V5KTtcbiAgICByZXR1cm4gaXNCbGFuayhyZWNvcmRMaXN0KSA/IG51bGwgOiByZWNvcmRMaXN0LmdldCh0cmFja0J5SWQsIGFmdGVySW5kZXgpO1xuICB9XG5cbiAgLyoqXG4gICAqIFJlbW92ZXMgYSB7QGxpbmsgQ29sbGVjdGlvbkNoYW5nZVJlY29yZH0gZnJvbSB0aGUgbGlzdCBvZiBkdXBsaWNhdGVzLlxuICAgKlxuICAgKiBUaGUgbGlzdCBvZiBkdXBsaWNhdGVzIGFsc28gaXMgcmVtb3ZlZCBmcm9tIHRoZSBtYXAgaWYgaXQgZ2V0cyBlbXB0eS5cbiAgICovXG4gIHJlbW92ZShyZWNvcmQ6IENvbGxlY3Rpb25DaGFuZ2VSZWNvcmQpOiBDb2xsZWN0aW9uQ2hhbmdlUmVjb3JkIHtcbiAgICB2YXIga2V5ID0gZ2V0TWFwS2V5KHJlY29yZC50cmFja0J5SWQpO1xuICAgIC8vIHRvZG8odmljYilcbiAgICAvLyBhc3NlcnQodGhpcy5tYXAuY29udGFpbnNLZXkoa2V5KSk7XG4gICAgdmFyIHJlY29yZExpc3Q6IF9EdXBsaWNhdGVJdGVtUmVjb3JkTGlzdCA9IHRoaXMubWFwLmdldChrZXkpO1xuICAgIC8vIFJlbW92ZSB0aGUgbGlzdCBvZiBkdXBsaWNhdGVzIHdoZW4gaXQgZ2V0cyBlbXB0eVxuICAgIGlmIChyZWNvcmRMaXN0LnJlbW92ZShyZWNvcmQpKSB7XG4gICAgICB0aGlzLm1hcC5kZWxldGUoa2V5KTtcbiAgICB9XG4gICAgcmV0dXJuIHJlY29yZDtcbiAgfVxuXG4gIGdldCBpc0VtcHR5KCk6IGJvb2xlYW4geyByZXR1cm4gdGhpcy5tYXAuc2l6ZSA9PT0gMDsgfVxuXG4gIGNsZWFyKCkgeyB0aGlzLm1hcC5jbGVhcigpOyB9XG5cbiAgdG9TdHJpbmcoKTogc3RyaW5nIHsgcmV0dXJuICdfRHVwbGljYXRlTWFwKCcgKyBzdHJpbmdpZnkodGhpcy5tYXApICsgJyknOyB9XG59XG4iXX0=