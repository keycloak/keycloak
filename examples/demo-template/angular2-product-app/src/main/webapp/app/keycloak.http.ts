import {Injectable} from "@angular/core";
import {Http, Request, ConnectionBackend, RequestOptions, RequestOptionsArgs, Response, Headers} from "@angular/http";

import {KeycloakService} from "./keycloak.service";
import {Observable} from 'rxjs/Rx';

/**
 * This provides a wrapper over the ng2 Http class that insures tokens are refreshed on each request.
 */
@Injectable()
export class KeycloakHttp extends Http {
    constructor(_backend: ConnectionBackend, _defaultOptions: RequestOptions, private _keycloakService:KeycloakService) {
        super(_backend, _defaultOptions);
    }

    private setToken(options: RequestOptionsArgs) {

        if (options == null || KeycloakService.auth == null || KeycloakService.auth.authz == null || KeycloakService.auth.authz.token == null) {
            console.log("Need a token, but no token is available, not setting bearer token.");
            return;
        }

        options.headers.set('Authorization', 'Bearer ' + KeycloakService.auth.authz.token);
    }

    private configureRequest(f:Function, url:string | Request, options:RequestOptionsArgs, body?: any):Observable<Response> {
        let tokenPromise:Promise<string> = this._keycloakService.getToken();
        let tokenObservable:Observable<string> = Observable.fromPromise(tokenPromise);
        let tokenUpdateObservable:Observable<any> = Observable.create((observer) => {
            if (options == null) {
                let headers = new Headers();
                options = new RequestOptions({ headers: headers });
            }

            this.setToken(options);
            observer.next();
            observer.complete();
        });
        let requestObservable:Observable<Response> = Observable.create((observer) => {
            let result;
            if (body) {
                result = f.apply(this, [url, body, options]);
            } else {
                result = f.apply(this, [url, options]);
            }

            result.subscribe((response) => {
                observer.next(response);
                observer.complete();
            });
        });

        return <Observable<Response>>Observable
            .merge(tokenObservable, tokenUpdateObservable, requestObservable, 1) // Insure no concurrency in the merged Observables
            .filter((response) => response instanceof Response);
    }

    /**
     * Performs any type of http request. First argument is required, and can either be a url or
     * a {@link Request} instance. If the first argument is a url, an optional {@link RequestOptions}
     * object can be provided as the 2nd argument. The options object will be merged with the values
     * of {@link BaseRequestOptions} before performing the request.
     */
    request(url: string | Request, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.request, url, options);
    }

    /**
     * Performs a request with `get` http method.
     */
    get(url: string, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.get, url, options);
    }

    /**
     * Performs a request with `post` http method.
     */
    post(url: string, body: any, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.post, url, options, body);
    }

    /**
     * Performs a request with `put` http method.
     */
    put(url: string, body: any, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.put, url, options, body);
    }

    /**
     * Performs a request with `delete` http method.
     */
    delete(url: string, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.delete, url, options);
    }

    /**
     * Performs a request with `patch` http method.
     */
    patch(url: string, body: any, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.patch, url, options, body);
    }

    /**
     * Performs a request with `head` http method.
     */
    head(url: string, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.head, url, options);
    }

    /**
     * Performs a request with `options` http method.
     */
    options(url: string, options?: RequestOptionsArgs): Observable<Response> {
        return this.configureRequest(super.options, url, options);
    }
}
