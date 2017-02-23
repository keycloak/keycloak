import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {HttpModule, Http, XHRBackend, RequestOptions} from '@angular/http';
import {KeycloakService} from './keycloak/keycloak.service';
import {KeycloakHttp} from './keycloak/keycloak.http';
import { AppComponent } from './app.component';

export function keycloakHttpFactory(backend: XHRBackend, defaultOptions: RequestOptions, keycloakService: KeycloakService) {
  return new KeycloakHttp(backend, defaultOptions, keycloakService);
}

@NgModule({
  declarations: [
    AppComponent
  ],
  imports: [
    BrowserModule,
    FormsModule,
    HttpModule
  ],
  providers: [
    KeycloakService,
    {
      provide: Http,
      useFactory: keycloakHttpFactory,
      deps: [XHRBackend, RequestOptions, KeycloakService]
    }
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
