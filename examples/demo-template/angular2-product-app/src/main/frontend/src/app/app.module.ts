import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpModule } from '@angular/http';
import { KeycloakService } from './keycloak/keycloak.service';
import { KeycloakHttp, KEYCLOAK_HTTP_PROVIDER } from './keycloak/keycloak.http';
import { AppComponent } from './app.component';

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
    KEYCLOAK_HTTP_PROVIDER
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
