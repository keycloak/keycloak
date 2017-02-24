import { Component } from '@angular/core';

import {Http, Headers, RequestOptions, Response} from '@angular/http';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import {KeycloakService} from './keycloak/keycloak.service';

import { environment } from '../environments/environment';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'Angular2 Product';

  products: string[] = [];

  constructor(private http: Http, private kc: KeycloakService) {}

  logout() {
    this.kc.logout();
  }

  reloadData() {
    this.http.get(environment.serviceBaseUrl + '/products')
      .map(res => res.json())
      .subscribe(prods => this.products = prods,
        error => console.log(error));
  }
}
