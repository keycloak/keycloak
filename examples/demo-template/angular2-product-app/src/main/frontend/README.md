# Angular2ProductApp

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 1.0.0-beta.32.3. Keycloak integration is based on Angular 

## Development server
Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

This application depends on `database-service` application API deployed at `http://localhost:8080/database-service` so you will have to make sure it allows cross-origin requests. It can be enabled for `database-service` in it's `keycloak.json`:

    {
        "realm" : "demo",
        "resource" : "database-service",
        ...
        "enable-cors": true
    }

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive/pipe/service/class/module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `-prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).
Before running the tests make sure you are serving the app via `ng serve` and Keycloak and `database-service` is up and running at `http://localhost:8080`.

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
