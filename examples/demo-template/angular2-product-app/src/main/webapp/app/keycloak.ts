import {Injectable} from 'angular2/core';


declare var Keycloak: any;

@Injectable()
export class KeycloakService {

    static auth : any = {};

    static init() : Promise<any>{
        let keycloakAuth : any = new Keycloak('keycloak.json');
        KeycloakService.auth.loggedIn = false;

        return new Promise((resolve,reject)=>{
            keycloakAuth.init({ onLoad: 'login-required' })
                .success( () => {
                    KeycloakService.auth.loggedIn = true;
                    KeycloakService.auth.authz = keycloakAuth;
                    KeycloakService.auth.logoutUrl = keycloakAuth.authServerUrl + "/realms/demo/tokens/logout?redirect_uri=/angular2-product/index.html";
                    resolve(null);
                })
                .error(()=> {
                    reject(null);
                });
        });
    }

    logout(){
        console.log('*** LOGOUT');
        KeycloakService.auth.loggedIn = false;
        KeycloakService.auth.authz = null;

        window.location.href = KeycloakService.auth.logoutUrl;
    }

    getToken(): Promise<string>{
        return new Promise<string>((resolve,reject)=>{
            if (KeycloakService.auth.authz.token) {
                KeycloakService.auth.authz.updateToken(5).success(function() {
                    resolve(<string>KeycloakService.auth.authz.token);
                })
                .error(function() {
                    reject('Failed to refresh token');
                });
            }
        });
    }
}
