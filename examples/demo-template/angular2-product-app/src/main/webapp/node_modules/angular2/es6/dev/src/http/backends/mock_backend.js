var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { Injectable } from 'angular2/core';
import { Request } from '../static_request';
import { ReadyState } from '../enums';
import { isPresent } from 'angular2/src/facade/lang';
import { BaseException } from 'angular2/src/facade/exceptions';
import { Subject } from 'rxjs/Subject';
import { ReplaySubject } from 'rxjs/subject/ReplaySubject';
import { take } from 'rxjs/operator/take';
/**
 *
 * Mock Connection to represent a {@link Connection} for tests.
 *
 **/
export class MockConnection {
    constructor(req) {
        this.response = take.call(new ReplaySubject(1), 1);
        this.readyState = ReadyState.Open;
        this.request = req;
    }
    /**
     * Sends a mock response to the connection. This response is the value that is emitted to the
     * {@link EventEmitter} returned by {@link Http}.
     *
     * ### Example
     *
     * ```
     * var connection;
     * backend.connections.subscribe(c => connection = c);
     * http.request('data.json').subscribe(res => console.log(res.text()));
     * connection.mockRespond(new Response('fake response')); //logs 'fake response'
     * ```
     *
     */
    mockRespond(res) {
        if (this.readyState === ReadyState.Done || this.readyState === ReadyState.Cancelled) {
            throw new BaseException('Connection has already been resolved');
        }
        this.readyState = ReadyState.Done;
        this.response.next(res);
        this.response.complete();
    }
    /**
     * Not yet implemented!
     *
     * Sends the provided {@link Response} to the `downloadObserver` of the `Request`
     * associated with this connection.
     */
    mockDownload(res) {
        // this.request.downloadObserver.onNext(res);
        // if (res.bytesLoaded === res.totalBytes) {
        //   this.request.downloadObserver.onCompleted();
        // }
    }
    // TODO(jeffbcross): consider using Response type
    /**
     * Emits the provided error object as an error to the {@link Response} {@link EventEmitter}
     * returned
     * from {@link Http}.
     */
    mockError(err) {
        // Matches XHR semantics
        this.readyState = ReadyState.Done;
        this.response.error(err);
    }
}
/**
 * A mock backend for testing the {@link Http} service.
 *
 * This class can be injected in tests, and should be used to override providers
 * to other backends, such as {@link XHRBackend}.
 *
 * ### Example
 *
 * ```
 * import {BaseRequestOptions, Http} from 'angular2/http';
 * import {MockBackend} from 'angular2/http/testing';
 * it('should get some data', inject([AsyncTestCompleter], (async) => {
 *   var connection;
 *   var injector = Injector.resolveAndCreate([
 *     MockBackend,
 *     provide(Http, {useFactory: (backend, options) => {
 *       return new Http(backend, options);
 *     }, deps: [MockBackend, BaseRequestOptions]})]);
 *   var http = injector.get(Http);
 *   var backend = injector.get(MockBackend);
 *   //Assign any newly-created connection to local variable
 *   backend.connections.subscribe(c => connection = c);
 *   http.request('data.json').subscribe((res) => {
 *     expect(res.text()).toBe('awesome');
 *     async.done();
 *   });
 *   connection.mockRespond(new Response('awesome'));
 * }));
 * ```
 *
 * This method only exists in the mock implementation, not in real Backends.
 **/
export let MockBackend = class {
    constructor() {
        this.connectionsArray = [];
        this.connections = new Subject();
        this.connections.subscribe(connection => this.connectionsArray.push(connection));
        this.pendingConnections = new Subject();
    }
    /**
     * Checks all connections, and raises an exception if any connection has not received a response.
     *
     * This method only exists in the mock implementation, not in real Backends.
     */
    verifyNoPendingRequests() {
        let pending = 0;
        this.pendingConnections.subscribe(c => pending++);
        if (pending > 0)
            throw new BaseException(`${pending} pending connections to be resolved`);
    }
    /**
     * Can be used in conjunction with `verifyNoPendingRequests` to resolve any not-yet-resolve
     * connections, if it's expected that there are connections that have not yet received a response.
     *
     * This method only exists in the mock implementation, not in real Backends.
     */
    resolveAllConnections() { this.connections.subscribe(c => c.readyState = 4); }
    /**
     * Creates a new {@link MockConnection}. This is equivalent to calling `new
     * MockConnection()`, except that it also will emit the new `Connection` to the `connections`
     * emitter of this `MockBackend` instance. This method will usually only be used by tests
     * against the framework itself, not by end-users.
     */
    createConnection(req) {
        if (!isPresent(req) || !(req instanceof Request)) {
            throw new BaseException(`createConnection requires an instance of Request, got ${req}`);
        }
        let connection = new MockConnection(req);
        this.connections.next(connection);
        return connection;
    }
};
MockBackend = __decorate([
    Injectable(), 
    __metadata('design:paramtypes', [])
], MockBackend);
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoibW9ja19iYWNrZW5kLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsiYW5ndWxhcjIvc3JjL2h0dHAvYmFja2VuZHMvbW9ja19iYWNrZW5kLnRzIl0sIm5hbWVzIjpbIk1vY2tDb25uZWN0aW9uIiwiTW9ja0Nvbm5lY3Rpb24uY29uc3RydWN0b3IiLCJNb2NrQ29ubmVjdGlvbi5tb2NrUmVzcG9uZCIsIk1vY2tDb25uZWN0aW9uLm1vY2tEb3dubG9hZCIsIk1vY2tDb25uZWN0aW9uLm1vY2tFcnJvciIsIk1vY2tCYWNrZW5kIiwiTW9ja0JhY2tlbmQuY29uc3RydWN0b3IiLCJNb2NrQmFja2VuZC52ZXJpZnlOb1BlbmRpbmdSZXF1ZXN0cyIsIk1vY2tCYWNrZW5kLnJlc29sdmVBbGxDb25uZWN0aW9ucyIsIk1vY2tCYWNrZW5kLmNyZWF0ZUNvbm5lY3Rpb24iXSwibWFwcGluZ3MiOiI7Ozs7Ozs7OztPQUFPLEVBQUMsVUFBVSxFQUFDLE1BQU0sZUFBZTtPQUNqQyxFQUFDLE9BQU8sRUFBQyxNQUFNLG1CQUFtQjtPQUVsQyxFQUFDLFVBQVUsRUFBQyxNQUFNLFVBQVU7T0FFNUIsRUFBQyxTQUFTLEVBQUMsTUFBTSwwQkFBMEI7T0FDM0MsRUFBQyxhQUFhLEVBQW1CLE1BQU0sZ0NBQWdDO09BQ3ZFLEVBQUMsT0FBTyxFQUFDLE1BQU0sY0FBYztPQUM3QixFQUFDLGFBQWEsRUFBQyxNQUFNLDRCQUE0QjtPQUNqRCxFQUFDLElBQUksRUFBQyxNQUFNLG9CQUFvQjtBQUV2Qzs7OztJQUlJO0FBQ0o7SUFvQkVBLFlBQVlBLEdBQVlBO1FBQ3RCQyxJQUFJQSxDQUFDQSxRQUFRQSxHQUFHQSxJQUFJQSxDQUFDQSxJQUFJQSxDQUFDQSxJQUFJQSxhQUFhQSxDQUFDQSxDQUFDQSxDQUFDQSxFQUFFQSxDQUFDQSxDQUFDQSxDQUFDQTtRQUNuREEsSUFBSUEsQ0FBQ0EsVUFBVUEsR0FBR0EsVUFBVUEsQ0FBQ0EsSUFBSUEsQ0FBQ0E7UUFDbENBLElBQUlBLENBQUNBLE9B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LEdBQUdBLENBQUNBLENBQUNBO1FBQ3pDQSxJQUFJQSxDQUFDQSxXQUFXQSxDQUFDQSxJQUFJQSxDQUFDQSxVQUFVQSxDQUFDQSxDQUFDQTtRQUNsQ0EsTUFBTUEsQ0FBQ0EsVUFBVUEsQ0FBQ0E7SUFDcEJBLENBQUNBO0FBQ0hKLENBQUNBO0FBNUZEO0lBQUMsVUFBVSxFQUFFOztnQkE0Rlo7QUFBQSIsInNvdXJjZXNDb250ZW50IjpbImltcG9ydCB7SW5qZWN0YWJsZX0gZnJvbSAnYW5ndWxhcjIvY29yZSc7XG5pbXBvcnQge1JlcXVlc3R9IGZyb20gJy4uL3N0YXRpY19yZXF1ZXN0JztcbmltcG9ydCB7UmVzcG9uc2V9IGZyb20gJy4uL3N0YXRpY19yZXNwb25zZSc7XG5pbXBvcnQge1JlYWR5U3RhdGV9IGZyb20gJy4uL2VudW1zJztcbmltcG9ydCB7Q29ubmVjdGlvbiwgQ29ubmVjdGlvbkJhY2tlbmR9IGZyb20gJy4uL2ludGVyZmFjZXMnO1xuaW1wb3J0IHtpc1ByZXNlbnR9IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvbGFuZyc7XG5pbXBvcnQge0Jhc2VFeGNlcHRpb24sIFdyYXBwZWRFeGNlcHRpb259IGZyb20gJ2FuZ3VsYXIyL3NyYy9mYWNhZGUvZXhjZXB0aW9ucyc7XG5pbXBvcnQge1N1YmplY3R9IGZyb20gJ3J4anMvU3ViamVjdCc7XG5pbXBvcnQge1JlcGxheVN1YmplY3R9IGZyb20gJ3J4anMvc3ViamVjdC9SZXBsYXlTdWJqZWN0JztcbmltcG9ydCB7dGFrZX0gZnJvbSAncnhqcy9vcGVyYXRvci90YWtlJztcblxuLyoqXG4gKlxuICogTW9jayBDb25uZWN0aW9uIHRvIHJlcHJlc2VudCBhIHtAbGluayBDb25uZWN0aW9ufSBmb3IgdGVzdHMuXG4gKlxuICoqL1xuZXhwb3J0IGNsYXNzIE1vY2tDb25uZWN0aW9uIGltcGxlbWVudHMgQ29ubmVjdGlvbiB7XG4gIC8vIFRPRE8gTmFtZSBgcmVhZHlTdGF0ZWAgc2hvdWxkIGNoYW5nZSB0byBiZSBtb3JlIGdlbmVyaWMsIGFuZCBzdGF0ZXMgY291bGQgYmUgbWFkZSB0byBiZSBtb3JlXG4gIC8vIGRlc2NyaXB0aXZlIHRoYW4gWEhSIHN0YXRlcy5cbiAgLyoqXG4gICAqIERlc2NyaWJlcyB0aGUgc3RhdGUgb2YgdGhlIGNvbm5lY3Rpb24sIGJhc2VkIG9uIGBYTUxIdHRwUmVxdWVzdC5yZWFkeVN0YXRlYCwgYnV0IHdpdGhcbiAgICogYWRkaXRpb25hbCBzdGF0ZXMuIEZvciBleGFtcGxlLCBzdGF0ZSA1IGluZGljYXRlcyBhbiBhYm9ydGVkIGNvbm5lY3Rpb24uXG4gICAqL1xuICByZWFkeVN0YXRlOiBSZWFkeVN0YXRlO1xuXG4gIC8qKlxuICAgKiB7QGxpbmsgUmVxdWVzdH0gaW5zdGFuY2UgdXNlZCB0byBjcmVhdGUgdGhlIGNvbm5lY3Rpb24uXG4gICAqL1xuICByZXF1ZXN0OiBSZXF1ZXN0O1xuXG4gIC8qKlxuICAgKiB7QGxpbmsgRXZlbnRFbWl0dGVyfSBvZiB7QGxpbmsgUmVzcG9uc2V9LiBDYW4gYmUgc3Vic2NyaWJlZCB0byBpbiBvcmRlciB0byBiZSBub3RpZmllZCB3aGVuIGFcbiAgICogcmVzcG9uc2UgaXMgYXZhaWxhYmxlLlxuICAgKi9cbiAgcmVzcG9uc2U6IGFueTsgIC8vIFN1YmplY3Q8UmVzcG9uc2U+XG5cbiAgY29uc3RydWN0b3IocmVxOiBSZXF1ZXN0KSB7XG4gICAgdGhpcy5yZXNwb25zZSA9IHRha2UuY2FsbChuZXcgUmVwbGF5U3ViamVjdCgxKSwgMSk7XG4gICAgdGhpcy5yZWFkeVN0YXRlID0gUmVhZHlTdGF0ZS5PcGVuO1xuICAgIHRoaXMucmVxdWVzdCA9IHJlcTtcbiAgfVxuXG4gIC8qKlxuICAgKiBTZW5kcyBhIG1vY2sgcmVzcG9uc2UgdG8gdGhlIGNvbm5lY3Rpb24uIFRoaXMgcmVzcG9uc2UgaXMgdGhlIHZhbHVlIHRoYXQgaXMgZW1pdHRlZCB0byB0aGVcbiAgICoge0BsaW5rIEV2ZW50RW1pdHRlcn0gcmV0dXJuZWQgYnkge0BsaW5rIEh0dHB9LlxuICAgKlxuICAgKiAjIyMgRXhhbXBsZVxuICAgKlxuICAgKiBgYGBcbiAgICogdmFyIGNvbm5lY3Rpb247XG4gICAqIGJhY2tlbmQuY29ubmVjdGlvbnMuc3Vic2NyaWJlKGMgPT4gY29ubmVjdGlvbiA9IGMpO1xuICAgKiBodHRwLnJlcXVlc3QoJ2RhdGEuanNvbicpLnN1YnNjcmliZShyZXMgPT4gY29uc29sZS5sb2cocmVzLnRleHQoKSkpO1xuICAgKiBjb25uZWN0aW9uLm1vY2tSZXNwb25kKG5ldyBSZXNwb25zZSgnZmFrZSByZXNwb25zZScpKTsgLy9sb2dzICdmYWtlIHJlc3BvbnNlJ1xuICAgKiBgYGBcbiAgICpcbiAgICovXG4gIG1vY2tSZXNwb25kKHJlczogUmVzcG9uc2UpIHtcbiAgICBpZiAodGhpcy5yZWFkeVN0YXRlID09PSBSZWFkeVN0YXRlLkRvbmUgfHwgdGhpcy5yZWFkeVN0YXRlID09PSBSZWFkeVN0YXRlLkNhbmNlbGxlZCkge1xuICAgICAgdGhyb3cgbmV3IEJhc2VFeGNlcHRpb24oJ0Nvbm5lY3Rpb24gaGFzIGFscmVhZHkgYmVlbiByZXNvbHZlZCcpO1xuICAgIH1cbiAgICB0aGlzLnJlYWR5U3RhdGUgPSBSZWFkeVN0YXRlLkRvbmU7XG4gICAgdGhpcy5yZXNwb25zZS5uZXh0KHJlcyk7XG4gICAgdGhpcy5yZXNwb25zZS5jb21wbGV0ZSgpO1xuICB9XG5cbiAgLyoqXG4gICAqIE5vdCB5ZXQgaW1wbGVtZW50ZWQhXG4gICAqXG4gICAqIFNlbmRzIHRoZSBwcm92aWRlZCB7QGxpbmsgUmVzcG9uc2V9IHRvIHRoZSBgZG93bmxvYWRPYnNlcnZlcmAgb2YgdGhlIGBSZXF1ZXN0YFxuICAgKiBhc3NvY2lhdGVkIHdpdGggdGhpcyBjb25uZWN0aW9uLlxuICAgKi9cbiAgbW9ja0Rvd25sb2FkKHJlczogUmVzcG9uc2UpIHtcbiAgICAvLyB0aGlzLnJlcXVlc3QuZG93bmxvYWRPYnNlcnZlci5vbk5leHQocmVzKTtcbiAgICAvLyBpZiAocmVzLmJ5dGVzTG9hZGVkID09PSByZXMudG90YWxCeXRlcykge1xuICAgIC8vICAgdGhpcy5yZXF1ZXN0LmRvd25sb2FkT2JzZXJ2ZXIub25Db21wbGV0ZWQoKTtcbiAgICAvLyB9XG4gIH1cblxuICAvLyBUT0RPKGplZmZiY3Jvc3MpOiBjb25zaWRlciB1c2luZyBSZXNwb25zZSB0eXBlXG4gIC8qKlxuICAgKiBFbWl0cyB0aGUgcHJvdmlkZWQgZXJyb3Igb2JqZWN0IGFzIGFuIGVycm9yIHRvIHRoZSB7QGxpbmsgUmVzcG9uc2V9IHtAbGluayBFdmVudEVtaXR0ZXJ9XG4gICAqIHJldHVybmVkXG4gICAqIGZyb20ge0BsaW5rIEh0dHB9LlxuICAgKi9cbiAgbW9ja0Vycm9yKGVycj86IEVycm9yKSB7XG4gICAgLy8gTWF0Y2hlcyBYSFIgc2VtYW50aWNzXG4gICAgdGhpcy5yZWFkeVN0YXRlID0gUmVhZHlTdGF0ZS5Eb25lO1xuICAgIHRoaXMucmVzcG9uc2UuZXJyb3IoZXJyKTtcbiAgfVxufVxuXG4vKipcbiAqIEEgbW9jayBiYWNrZW5kIGZvciB0ZXN0aW5nIHRoZSB7QGxpbmsgSHR0cH0gc2VydmljZS5cbiAqXG4gKiBUaGlzIGNsYXNzIGNhbiBiZSBpbmplY3RlZCBpbiB0ZXN0cywgYW5kIHNob3VsZCBiZSB1c2VkIHRvIG92ZXJyaWRlIHByb3ZpZGVyc1xuICogdG8gb3RoZXIgYmFja2VuZHMsIHN1Y2ggYXMge0BsaW5rIFhIUkJhY2tlbmR9LlxuICpcbiAqICMjIyBFeGFtcGxlXG4gKlxuICogYGBgXG4gKiBpbXBvcnQge0Jhc2VSZXF1ZXN0T3B0aW9ucywgSHR0cH0gZnJvbSAnYW5ndWxhcjIvaHR0cCc7XG4gKiBpbXBvcnQge01vY2tCYWNrZW5kfSBmcm9tICdhbmd1bGFyMi9odHRwL3Rlc3RpbmcnO1xuICogaXQoJ3Nob3VsZCBnZXQgc29tZSBkYXRhJywgaW5qZWN0KFtBc3luY1Rlc3RDb21wbGV0ZXJdLCAoYXN5bmMpID0+IHtcbiAqICAgdmFyIGNvbm5lY3Rpb247XG4gKiAgIHZhciBpbmplY3RvciA9IEluamVjdG9yLnJlc29sdmVBbmRDcmVhdGUoW1xuICogICAgIE1vY2tCYWNrZW5kLFxuICogICAgIHByb3ZpZGUoSHR0cCwge3VzZUZhY3Rvcnk6IChiYWNrZW5kLCBvcHRpb25zKSA9PiB7XG4gKiAgICAgICByZXR1cm4gbmV3IEh0dHAoYmFja2VuZCwgb3B0aW9ucyk7XG4gKiAgICAgfSwgZGVwczogW01vY2tCYWNrZW5kLCBCYXNlUmVxdWVzdE9wdGlvbnNdfSldKTtcbiAqICAgdmFyIGh0dHAgPSBpbmplY3Rvci5nZXQoSHR0cCk7XG4gKiAgIHZhciBiYWNrZW5kID0gaW5qZWN0b3IuZ2V0KE1vY2tCYWNrZW5kKTtcbiAqICAgLy9Bc3NpZ24gYW55IG5ld2x5LWNyZWF0ZWQgY29ubmVjdGlvbiB0byBsb2NhbCB2YXJpYWJsZVxuICogICBiYWNrZW5kLmNvbm5lY3Rpb25zLnN1YnNjcmliZShjID0+IGNvbm5lY3Rpb24gPSBjKTtcbiAqICAgaHR0cC5yZXF1ZXN0KCdkYXRhLmpzb24nKS5zdWJzY3JpYmUoKHJlcykgPT4ge1xuICogICAgIGV4cGVjdChyZXMudGV4dCgpKS50b0JlKCdhd2Vzb21lJyk7XG4gKiAgICAgYXN5bmMuZG9uZSgpO1xuICogICB9KTtcbiAqICAgY29ubmVjdGlvbi5tb2NrUmVzcG9uZChuZXcgUmVzcG9uc2UoJ2F3ZXNvbWUnKSk7XG4gKiB9KSk7XG4gKiBgYGBcbiAqXG4gKiBUaGlzIG1ldGhvZCBvbmx5IGV4aXN0cyBpbiB0aGUgbW9jayBpbXBsZW1lbnRhdGlvbiwgbm90IGluIHJlYWwgQmFja2VuZHMuXG4gKiovXG5ASW5qZWN0YWJsZSgpXG5leHBvcnQgY2xhc3MgTW9ja0JhY2tlbmQgaW1wbGVtZW50cyBDb25uZWN0aW9uQmFja2VuZCB7XG4gIC8qKlxuICAgKiB7QGxpbmsgRXZlbnRFbWl0dGVyfVxuICAgKiBvZiB7QGxpbmsgTW9ja0Nvbm5lY3Rpb259IGluc3RhbmNlcyB0aGF0IGhhdmUgYmVlbiBjcmVhdGVkIGJ5IHRoaXMgYmFja2VuZC4gQ2FuIGJlIHN1YnNjcmliZWRcbiAgICogdG8gaW4gb3JkZXIgdG8gcmVzcG9uZCB0byBjb25uZWN0aW9ucy5cbiAgICpcbiAgICogIyMjIEV4YW1wbGVcbiAgICpcbiAgICogYGBgXG4gICAqIGltcG9ydCB7TW9ja0JhY2tlbmQsIEh0dHAsIEJhc2VSZXF1ZXN0T3B0aW9uc30gZnJvbSAnYW5ndWxhcjIvaHR0cCc7XG4gICAqIGltcG9ydCB7SW5qZWN0b3J9IGZyb20gJ2FuZ3VsYXIyL2NvcmUnO1xuICAgKlxuICAgKiBpdCgnc2hvdWxkIGdldCBhIHJlc3BvbnNlJywgKCkgPT4ge1xuICAgKiAgIHZhciBjb25uZWN0aW9uOyAvL3RoaXMgd2lsbCBiZSBzZXQgd2hlbiBhIG5ldyBjb25uZWN0aW9uIGlzIGVtaXR0ZWQgZnJvbSB0aGUgYmFja2VuZC5cbiAgICogICB2YXIgdGV4dDsgLy90aGlzIHdpbGwgYmUgc2V0IGZyb20gbW9jayByZXNwb25zZVxuICAgKiAgIHZhciBpbmplY3RvciA9IEluamVjdG9yLnJlc29sdmVBbmRDcmVhdGUoW1xuICAgKiAgICAgTW9ja0JhY2tlbmQsXG4gICAqICAgICBwcm92aWRlKEh0dHAsIHt1c2VGYWN0b3J5OiAoYmFja2VuZCwgb3B0aW9ucykge1xuICAgKiAgICAgICByZXR1cm4gbmV3IEh0dHAoYmFja2VuZCwgb3B0aW9ucyk7XG4gICAqICAgICB9LCBkZXBzOiBbTW9ja0JhY2tlbmQsIEJhc2VSZXF1ZXN0T3B0aW9uc119XSk7XG4gICAqICAgdmFyIGJhY2tlbmQgPSBpbmplY3Rvci5nZXQoTW9ja0JhY2tlbmQpO1xuICAgKiAgIHZhciBodHRwID0gaW5qZWN0b3IuZ2V0KEh0dHApO1xuICAgKiAgIGJhY2tlbmQuY29ubmVjdGlvbnMuc3Vic2NyaWJlKGMgPT4gY29ubmVjdGlvbiA9IGMpO1xuICAgKiAgIGh0dHAucmVxdWVzdCgnc29tZXRoaW5nLmpzb24nKS5zdWJzY3JpYmUocmVzID0+IHtcbiAgICogICAgIHRleHQgPSByZXMudGV4dCgpO1xuICAgKiAgIH0pO1xuICAgKiAgIGNvbm5lY3Rpb24ubW9ja1Jlc3BvbmQobmV3IFJlc3BvbnNlKHtib2R5OiAnU29tZXRoaW5nJ30pKTtcbiAgICogICBleHBlY3QodGV4dCkudG9CZSgnU29tZXRoaW5nJyk7XG4gICAqIH0pO1xuICAgKiBgYGBcbiAgICpcbiAgICogVGhpcyBwcm9wZXJ0eSBvbmx5IGV4aXN0cyBpbiB0aGUgbW9jayBpbXBsZW1lbnRhdGlvbiwgbm90IGluIHJlYWwgQmFja2VuZHMuXG4gICAqL1xuICBjb25uZWN0aW9uczogYW55OyAgLy88TW9ja0Nvbm5lY3Rpb24+XG5cbiAgLyoqXG4gICAqIEFuIGFycmF5IHJlcHJlc2VudGF0aW9uIG9mIGBjb25uZWN0aW9uc2AuIFRoaXMgYXJyYXkgd2lsbCBiZSB1cGRhdGVkIHdpdGggZWFjaCBjb25uZWN0aW9uIHRoYXRcbiAgICogaXMgY3JlYXRlZCBieSB0aGlzIGJhY2tlbmQuXG4gICAqXG4gICAqIFRoaXMgcHJvcGVydHkgb25seSBleGlzdHMgaW4gdGhlIG1vY2sgaW1wbGVtZW50YXRpb24sIG5vdCBpbiByZWFsIEJhY2tlbmRzLlxuICAgKi9cbiAgY29ubmVjdGlvbnNBcnJheTogTW9ja0Nvbm5lY3Rpb25bXTtcbiAgLyoqXG4gICAqIHtAbGluayBFdmVudEVtaXR0ZXJ9IG9mIHtAbGluayBNb2NrQ29ubmVjdGlvbn0gaW5zdGFuY2VzIHRoYXQgaGF2ZW4ndCB5ZXQgYmVlbiByZXNvbHZlZCAoaS5lLlxuICAgKiB3aXRoIGEgYHJlYWR5U3RhdGVgXG4gICAqIGxlc3MgdGhhbiA0KS4gVXNlZCBpbnRlcm5hbGx5IHRvIHZlcmlmeSB0aGF0IG5vIGNvbm5lY3Rpb25zIGFyZSBwZW5kaW5nIHZpYSB0aGVcbiAgICogYHZlcmlmeU5vUGVuZGluZ1JlcXVlc3RzYCBtZXRob2QuXG4gICAqXG4gICAqIFRoaXMgcHJvcGVydHkgb25seSBleGlzdHMgaW4gdGhlIG1vY2sgaW1wbGVtZW50YXRpb24sIG5vdCBpbiByZWFsIEJhY2tlbmRzLlxuICAgKi9cbiAgcGVuZGluZ0Nvbm5lY3Rpb25zOiBhbnk7ICAvLyBTdWJqZWN0PE1vY2tDb25uZWN0aW9uPlxuICBjb25zdHJ1Y3RvcigpIHtcbiAgICB0aGlzLmNvbm5lY3Rpb25zQXJyYXkgPSBbXTtcbiAgICB0aGlzLmNvbm5lY3Rpb25zID0gbmV3IFN1YmplY3QoKTtcbiAgICB0aGlzLmNvbm5lY3Rpb25zLnN1YnNjcmliZShjb25uZWN0aW9uID0+IHRoaXMuY29ubmVjdGlvbnNBcnJheS5wdXNoKGNvbm5lY3Rpb24pKTtcbiAgICB0aGlzLnBlbmRpbmdDb25uZWN0aW9ucyA9IG5ldyBTdWJqZWN0KCk7XG4gIH1cblxuICAvKipcbiAgICogQ2hlY2tzIGFsbCBjb25uZWN0aW9ucywgYW5kIHJhaXNlcyBhbiBleGNlcHRpb24gaWYgYW55IGNvbm5lY3Rpb24gaGFzIG5vdCByZWNlaXZlZCBhIHJlc3BvbnNlLlxuICAgKlxuICAgKiBUaGlzIG1ldGhvZCBvbmx5IGV4aXN0cyBpbiB0aGUgbW9jayBpbXBsZW1lbnRhdGlvbiwgbm90IGluIHJlYWwgQmFja2VuZHMuXG4gICAqL1xuICB2ZXJpZnlOb1BlbmRpbmdSZXF1ZXN0cygpIHtcbiAgICBsZXQgcGVuZGluZyA9IDA7XG4gICAgdGhpcy5wZW5kaW5nQ29ubmVjdGlvbnMuc3Vic2NyaWJlKGMgPT4gcGVuZGluZysrKTtcbiAgICBpZiAocGVuZGluZyA+IDApIHRocm93IG5ldyBCYXNlRXhjZXB0aW9uKGAke3BlbmRpbmd9IHBlbmRpbmcgY29ubmVjdGlvbnMgdG8gYmUgcmVzb2x2ZWRgKTtcbiAgfVxuXG4gIC8qKlxuICAgKiBDYW4gYmUgdXNlZCBpbiBjb25qdW5jdGlvbiB3aXRoIGB2ZXJpZnlOb1BlbmRpbmdSZXF1ZXN0c2AgdG8gcmVzb2x2ZSBhbnkgbm90LXlldC1yZXNvbHZlXG4gICAqIGNvbm5lY3Rpb25zLCBpZiBpdCdzIGV4cGVjdGVkIHRoYXQgdGhlcmUgYXJlIGNvbm5lY3Rpb25zIHRoYXQgaGF2ZSBub3QgeWV0IHJlY2VpdmVkIGEgcmVzcG9uc2UuXG4gICAqXG4gICAqIFRoaXMgbWV0aG9kIG9ubHkgZXhpc3RzIGluIHRoZSBtb2NrIGltcGxlbWVudGF0aW9uLCBub3QgaW4gcmVhbCBCYWNrZW5kcy5cbiAgICovXG4gIHJlc29sdmVBbGxDb25uZWN0aW9ucygpIHsgdGhpcy5jb25uZWN0aW9ucy5zdWJzY3JpYmUoYyA9PiBjLnJlYWR5U3RhdGUgPSA0KTsgfVxuXG4gIC8qKlxuICAgKiBDcmVhdGVzIGEgbmV3IHtAbGluayBNb2NrQ29ubmVjdGlvbn0uIFRoaXMgaXMgZXF1aXZhbGVudCB0byBjYWxsaW5nIGBuZXdcbiAgICogTW9ja0Nvbm5lY3Rpb24oKWAsIGV4Y2VwdCB0aGF0IGl0IGFsc28gd2lsbCBlbWl0IHRoZSBuZXcgYENvbm5lY3Rpb25gIHRvIHRoZSBgY29ubmVjdGlvbnNgXG4gICAqIGVtaXR0ZXIgb2YgdGhpcyBgTW9ja0JhY2tlbmRgIGluc3RhbmNlLiBUaGlzIG1ldGhvZCB3aWxsIHVzdWFsbHkgb25seSBiZSB1c2VkIGJ5IHRlc3RzXG4gICAqIGFnYWluc3QgdGhlIGZyYW1ld29yayBpdHNlbGYsIG5vdCBieSBlbmQtdXNlcnMuXG4gICAqL1xuICBjcmVhdGVDb25uZWN0aW9uKHJlcTogUmVxdWVzdCk6IENvbm5lY3Rpb24ge1xuICAgIGlmICghaXNQcmVzZW50KHJlcSkgfHwgIShyZXEgaW5zdGFuY2VvZiBSZXF1ZXN0KSkge1xuICAgICAgdGhyb3cgbmV3IEJhc2VFeGNlcHRpb24oYGNyZWF0ZUNvbm5lY3Rpb24gcmVxdWlyZXMgYW4gaW5zdGFuY2Ugb2YgUmVxdWVzdCwgZ290ICR7cmVxfWApO1xuICAgIH1cbiAgICBsZXQgY29ubmVjdGlvbiA9IG5ldyBNb2NrQ29ubmVjdGlvbihyZXEpO1xuICAgIHRoaXMuY29ubmVjdGlvbnMubmV4dChjb25uZWN0aW9uKTtcbiAgICByZXR1cm4gY29ubmVjdGlvbjtcbiAgfVxufVxuIl19