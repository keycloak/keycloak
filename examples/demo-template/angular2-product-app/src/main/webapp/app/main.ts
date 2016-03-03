import 'rxjs/Rx';
import {bootstrap}    from 'angular2/platform/browser';
import {HTTP_BINDINGS} from 'angular2/http';
import {KeycloakService} from './keycloak';
import {AppComponent} from './app';

KeycloakService.init().then(
    o=>{
        bootstrap(AppComponent,[HTTP_BINDINGS, KeycloakService]);
    },
    x=>{
        window.location.reload();
    }
);