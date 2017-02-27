import {Injectable, ReflectiveInjector} from '@angular/core';
import {async, fakeAsync, tick} from '@angular/core/testing';
import {BaseRequestOptions, ConnectionBackend, Http, RequestOptions} from '@angular/http';
import {Response, ResponseOptions} from '@angular/http';
import {MockBackend, MockConnection} from '@angular/http/testing';

import { KeycloakHttp, KEYCLOAK_HTTP_PROVIDER, keycloakHttpFactory } from './keycloak.http';
import { KeycloakService } from './keycloak.service';

@Injectable()
class MockKeycloakService extends KeycloakService {
  getToken(): Promise<string> {
    return Promise.resolve('hello');
  }
}

describe('KeycloakHttp', () => {

  let injector: ReflectiveInjector;
  let backend: MockBackend;
  let lastConnection: MockConnection;
  let http: Http;

  beforeEach(() => {
    injector = ReflectiveInjector.resolveAndCreate([
      {provide: ConnectionBackend, useClass: MockBackend},
      {provide: RequestOptions, useClass: BaseRequestOptions},
      {provide: KeycloakService, useClass: MockKeycloakService},
      {
        provide: Http,
        useFactory: keycloakHttpFactory,
        deps: [ConnectionBackend, RequestOptions, KeycloakService]
      }
    ]);
    http = injector.get(Http);
    backend = injector.get(ConnectionBackend) as MockBackend;
    backend.connections.subscribe((c: MockConnection) => lastConnection = c);
  });

  it('should set Authorization header', fakeAsync(() => {
    http.get('foo').subscribe(r => console.log(r));
    tick();
    expect(lastConnection).toBeDefined('no http service connection at all?');
    expect(lastConnection.request.headers.get('Authorization')).toBe('Bearer hello');
  }));

});
