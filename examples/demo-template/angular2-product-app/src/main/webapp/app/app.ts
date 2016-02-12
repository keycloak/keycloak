import {Http, Headers,
    RequestOptions, Response}   from 'angular2/http';
import {Component}              from 'angular2/core';
import {Observable}             from 'rxjs/Observable';
import {KeycloakService}        from './keycloak';




@Component({
    selector: 'my-app',
    template:
`
<div id="content-area" class="col-md-9" role="main">
    <div id="content">
        <h1>Angular2 Product (Beta)</h1>
        <h2><span>Products</span></h2>
       
        <button type="button" (click)="logout()">Sign Out</button>
        <button type="button" (click)="reloadData()">Reload</button>
        <table class="table" [hidden]="!products.length">
            <thead>
            <tr>
                <th>Product Listing</th>
            </tr>
            </thead>
            <tbody>
            <tr *ngFor="#p of products">
                <td>{{p}}</td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
`
})
export class AppComponent {

    constructor(private _kc:KeycloakService, private http:Http){ }

    products : string[] = [];

    logout(){
        this._kc.logout();
    }

    reloadData() {
        //angular dont have http interceptor yet

        this._kc.getToken().then(
            token=>{
                let headers = new Headers({
                        'Accept': 'application/json',
                        'Authorization': 'Bearer ' + token
                    });

                let options = new RequestOptions({ headers: headers });

                this.http.get('/database/products', options)
                .map(res => <string[]> res.json())
                .subscribe(
                     prods => this.products = prods,
                     error =>  console.log(error));

            },
            error=>{
                console.log(error);
            }
        );

    }

    private handleError (error: Response) {
        console.error(error);
        return Observable.throw(error.json().error || 'Server error');
    }

}
