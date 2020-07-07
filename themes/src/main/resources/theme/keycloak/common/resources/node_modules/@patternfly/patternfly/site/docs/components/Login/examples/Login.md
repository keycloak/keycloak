---
title: Login
section: components
cssPrefix: pf-c-login
wrapperTag: div
---

## Examples
```hbs title=Basic isFullscreen
{{#> background-image}}{{/background-image}}
{{#> login}}
  {{#> login-container}}
    {{#> login-header}}
      {{#> brand brand--attribute='src="/assets/images/pf_logo_color.svg" alt="PatternFly Logo"'}}{{/brand}}
    {{/login-header}}
    {{#> login-main}}
      {{#> login-main-header}}
        {{#> title titleType="h1" title--modifier="pf-m-3xl"}}
          Log in to your account
        {{/title}}
        {{#> login-main-header-desc}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        {{/login-main-header-desc}}
        {{#> dropdown id="login-demo-dropdown-select" dropdown--IsSelect="true" dropdown--ItemIsSelected="true" dropdown--HasToggleIcon="true"}}
          {{#> dropdown-toggle-text}}
            English
          {{/dropdown-toggle-text}}
        {{/dropdown}}
      {{/login-main-header}}
      {{#> login-main-body}}
        {{#> form}}
          {{#> form-helper-text form-helper-text--modifier="pf-m-error pf-m-hidden"}}
            {{#> form-helper-text-icon}}
              <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
            {{/form-helper-text-icon}}
            Invalid login credentials.
          {{/form-helper-text}}
          {{#> form-group}}
            {{#> form-label form-label--attribute='for="login-demo-form-username"' required="true"}}Username{{/form-label}}
            {{#> form-control controlType="input" input="true" form-control--attribute='required input="true" type="text" id="login-demo-form-username" name="login-demo-form-username"'}}{{/form-control}}
          {{/form-group}}
          {{#> form-group}}
            {{#> form-label form-label--attribute='for="login-demo-form-password"' required="true"}}Password{{/form-label}}
            {{#> form-control controlType="input" input="true" form-control--attribute='required input="true" type="password" id="login-demo-form-password" name="login-demo-form-password"'}}{{/form-control}}
          {{/form-group}}
          {{#> form-group}}
          {{#> check}}
              {{#> check-input check-input--attribute='type="checkbox" id="login-demo-checkbox" name="login-demo-checkbox"'}}{{/check-input}}
              {{#> check-label check-label--attribute='for="login-demo-checkbox"'}}Keep me logged in for 30 days.{{/check-label}}
            {{/check}}
          {{/form-group}}
          {{#> form-group form-group--modifier="pf-m-action"}}
            {{#> button button--modifier="pf-m-primary pf-m-block" button--IsSubmit="true"}}
              Log in
            {{/button}}
          {{/form-group}}
        {{/form}}
      {{/login-main-body}}
      {{#> login-main-footer}}
        {{#> login-main-footer-links}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Google"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 488 512"><path d="M488 261.8C488 403.3 391.1 504 248 504 110.8 504 0 393.2 0 256S110.8 8 248 8c66.8 0 123 24.5 166.3 64.9l-67.5 64.9C258.5 52.6 94.3 116.6 94.3 256c0 86.5 69.1 156.6 153.7 156.6 98.2 0 135-70.4 140.8-106.9H248v-85.3h236.1c2.3 12.7 3.9 24.9 3.9 41.4z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Github"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 496 512"><path d="M165.9 397.4c0 2-2.3 3.6-5.2 3.6-3.3.3-5.6-1.3-5.6-3.6 0-2 2.3-3.6 5.2-3.6 3-.3 5.6 1.3 5.6 3.6zm-31.1-4.5c-.7 2 1.3 4.3 4.3 4.9 2.6 1 5.6 0 6.2-2s-1.3-4.3-4.3-5.2c-2.6-.7-5.5.3-6.2 2.3zm44.2-1.7c-2.9.7-4.9 2.6-4.6 4.9.3 2 2.9 3.3 5.9 2.6 2.9-.7 4.9-2.6 4.6-4.6-.3-1.9-3-3.2-5.9-2.9zM244.8 8C106.1 8 0 113.3 0 252c0 110.9 69.8 205.8 169.5 239.2 12.8 2.3 17.3-5.6 17.3-12.1 0-6.2-.3-40.4-.3-61.4 0 0-70 15-84.7-29.8 0 0-11.4-29.1-27.8-36.6 0 0-22.9-15.7 1.6-15.4 0 0 24.9 2 38.6 25.8 21.9 38.6 58.6 27.5 72.9 20.9 2.3-16 8.8-27.1 16-33.7-55.9-6.2-112.3-14.3-112.3-110.5 0-27.5 7.6-41.3 23.6-58.9-2.6-6.5-11.1-33.3 2.6-67.9 20.9-6.5 69 27 69 27 20-5.6 41.5-8.5 62.8-8.5s42.8 2.9 62.8 8.5c0 0 48.1-33.6 69-27 13.7 34.7 5.2 61.4 2.6 67.9 16 17.7 25.8 31.5 25.8 58.9 0 96.5-58.9 104.2-114.8 110.5 9.2 7.9 17 22.9 17 46.4 0 33.7-.3 75.4-.3 83.6 0 6.5 4.6 14.4 17.3 12.1C428.2 457.8 496 362.9 496 252 496 113.3 383.5 8 244.8 8zM97.2 352.9c-1.3 1-1 3.3.7 5.2 1.6 1.6 3.9 2.3 5.2 1 1.3-1 1-3.3-.7-5.2-1.6-1.6-3.9-2.3-5.2-1zm-10.8-8.1c-.7 1.3.3 2.9 2.3 3.9 1.6 1 3.6.7 4.3-.7.7-1.3-.3-2.9-2.3-3.9-2-.6-3.6-.3-4.3.7zm32.4 35.6c-1.6 1.3-1 4.3 1.3 6.2 2.3 2.3 5.2 2.6 6.5 1 1.3-1.3.7-4.3-1.3-6.2-2.2-2.3-5.2-2.6-6.5-1zm-11.4-14.7c-1.6 1-1.6 3.6 0 5.9 1.6 2.3 4.3 3.3 5.6 2.3 1.6-1.3 1.6-3.9 0-6.2-1.4-2.3-4-3.3-5.6-2z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Dropbox"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 528 512"><path d="M264.4 116.3l-132 84.3 132 84.3-132 84.3L0 284.1l132.3-84.3L0 116.3 132.3 32l132.1 84.3zM131.6 395.7l132-84.3 132 84.3-132 84.3-132-84.3zm132.8-111.6l132-84.3-132-83.6L395.7 32 528 116.3l-132.3 84.3L528 284.8l-132.3 84.3-131.3-85z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Facebook"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512"><path d="M448 56.7v398.5c0 13.7-11.1 24.7-24.7 24.7H309.1V306.5h58.2l8.7-67.6h-67v-43.2c0-19.6 5.4-32.9 33.5-32.9h35.8v-60.5c-6.2-.8-27.4-2.7-52.2-2.7-51.6 0-87 31.5-87 89.4v49.9h-58.4v67.6h58.4V480H24.7C11.1 480 0 468.9 0 455.3V56.7C0 43.1 11.1 32 24.7 32h398.5c13.7 0 24.8 11.1 24.8 24.7z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Gitlab"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M29.782 199.732L256 493.714 8.074 309.699c-6.856-5.142-9.712-13.996-7.141-21.993l28.849-87.974zm75.405-174.806c-3.142-8.854-15.709-8.854-18.851 0L29.782 199.732h131.961L105.187 24.926zm56.556 174.806L256 493.714l94.257-293.982H161.743zm349.324 87.974l-28.849-87.974L256 493.714l247.926-184.015c6.855-5.142 9.711-13.996 7.141-21.993zm-85.404-262.78c-3.142-8.854-15.709-8.854-18.851 0l-56.555 174.806h131.961L425.663 24.926z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
        {{/login-main-footer-links}}
        {{#> login-main-footer-band}}
          {{#> login-main-footer-band-item}}
            Need an account? <a href="https://www.patternfly.org/">Sign up.</a>
          {{/login-main-footer-band-item}}
          {{#> login-main-footer-band-item}}
            <a href="#">Forgot username or password?</a>
          {{/login-main-footer-band-item}}
        {{/login-main-footer-band}}
      {{/login-main-footer}}
    {{/login-main}}
    {{#> login-footer}}
      <p>This is placeholder text only. Use this area to place any information or introductory message about your application that may be relevant to users.</p>
      {{#> list list--modifier="pf-m-inline"}}
        <li>
          <a href="#">
            Terms of use
          </a>
        </li>
        <li>
          <a href="#">
            Help
          </a>
        </li>
        <li>
          <a href="#">
            Privacy policy
          </a>
        </li>
      {{/list}}
    {{/login-footer}}
  {{/login-container}}
{{/login}}
```

```hbs title=Invalid isFullscreen
{{#> background-image}}{{/background-image}}
{{#> login}}
  {{#> login-container}}
    {{#> login-header}}
      {{#> brand brand--attribute='src="/assets/images/pf_logo_color.svg" alt="PatternFly Logo"'}}{{/brand}}
    {{/login-header}}
    {{#> login-main}}
      {{#> login-main-header}}
        {{#> title titleType="h1" title--modifier="pf-m-3xl"}}
          Log in to your account
        {{/title}}
        {{#> login-main-header-desc}}
          Lorem ipsum dolor sit amet, consectetur adipiscing elit.
        {{/login-main-header-desc}}
        {{#> dropdown id="invalid-login-demo-dropdown-select" dropdown--IsSelect="true" dropdown--ItemIsSelected="true" dropdown--HasToggleIcon="true"}}
          {{#> dropdown-toggle-text}}
            English
          {{/dropdown-toggle-text}}
        {{/dropdown}}
      {{/login-main-header}}
      {{#> login-main-body}}
        {{#> form}}
          {{#> form-helper-text form-helper-text--modifier="pf-m-error" form-helper-text--attribute='aria-live="polite"'}}
            {{#> form-helper-text-icon}}
              <i class="fas fa-exclamation-circle" aria-hidden="true"></i>
            {{/form-helper-text-icon}}
            Invalid login credentials.
          {{/form-helper-text}}
          {{#> form-group}}
            {{#> form-label form-label--attribute='for="invalid-login-demo-form-username"' required="true"}}Username{{/form-label}}
            {{#> form-control controlType="input" input="true" form-control--attribute='required type="text" id="invalid-login-demo-form-username" name="invalid-login-demo-form-username" aria-invalid="true"'}}{{/form-control}}
          {{/form-group}}
          {{#> form-group}}
            {{#> form-label form-label--attribute='for="invalid-login-demo-form-password"' required="true"}}Password{{/form-label}}
            {{#> form-control controlType="input" input="true" form-control--attribute='required type="password" id="invalid-login-demo-form-password" name="invalid-login-demo-form-password" aria-invalid="true"'}}{{/form-control}}
          {{/form-group}}
          {{#> form-group}}
          {{#> check}}
              {{#> check-input check-input--attribute='type="checkbox" id="invalid-login-demo-checkbox" name="invalid-login-demo-checkbox"'}}{{/check-input}}
              {{#> check-label check-label--attribute='for="invalid-login-demo-checkbox"'}}Keep me logged in for 30 days.{{/check-label}}
            {{/check}}
          {{/form-group}}
          {{#> form-group form-group--modifier="pf-m-action"}}
            {{#> button button--modifier="pf-m-primary pf-m-block" button--IsSubmit="true"}}
              Log in
            {{/button}}
          {{/form-group}}
        {{/form}}
      {{/login-main-body}}
      {{#> login-main-footer}}
        {{#> login-main-footer-links}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Google"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 488 512"><path d="M488 261.8C488 403.3 391.1 504 248 504 110.8 504 0 393.2 0 256S110.8 8 248 8c66.8 0 123 24.5 166.3 64.9l-67.5 64.9C258.5 52.6 94.3 116.6 94.3 256c0 86.5 69.1 156.6 153.7 156.6 98.2 0 135-70.4 140.8-106.9H248v-85.3h236.1c2.3 12.7 3.9 24.9 3.9 41.4z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Github"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 496 512"><path d="M165.9 397.4c0 2-2.3 3.6-5.2 3.6-3.3.3-5.6-1.3-5.6-3.6 0-2 2.3-3.6 5.2-3.6 3-.3 5.6 1.3 5.6 3.6zm-31.1-4.5c-.7 2 1.3 4.3 4.3 4.9 2.6 1 5.6 0 6.2-2s-1.3-4.3-4.3-5.2c-2.6-.7-5.5.3-6.2 2.3zm44.2-1.7c-2.9.7-4.9 2.6-4.6 4.9.3 2 2.9 3.3 5.9 2.6 2.9-.7 4.9-2.6 4.6-4.6-.3-1.9-3-3.2-5.9-2.9zM244.8 8C106.1 8 0 113.3 0 252c0 110.9 69.8 205.8 169.5 239.2 12.8 2.3 17.3-5.6 17.3-12.1 0-6.2-.3-40.4-.3-61.4 0 0-70 15-84.7-29.8 0 0-11.4-29.1-27.8-36.6 0 0-22.9-15.7 1.6-15.4 0 0 24.9 2 38.6 25.8 21.9 38.6 58.6 27.5 72.9 20.9 2.3-16 8.8-27.1 16-33.7-55.9-6.2-112.3-14.3-112.3-110.5 0-27.5 7.6-41.3 23.6-58.9-2.6-6.5-11.1-33.3 2.6-67.9 20.9-6.5 69 27 69 27 20-5.6 41.5-8.5 62.8-8.5s42.8 2.9 62.8 8.5c0 0 48.1-33.6 69-27 13.7 34.7 5.2 61.4 2.6 67.9 16 17.7 25.8 31.5 25.8 58.9 0 96.5-58.9 104.2-114.8 110.5 9.2 7.9 17 22.9 17 46.4 0 33.7-.3 75.4-.3 83.6 0 6.5 4.6 14.4 17.3 12.1C428.2 457.8 496 362.9 496 252 496 113.3 383.5 8 244.8 8zM97.2 352.9c-1.3 1-1 3.3.7 5.2 1.6 1.6 3.9 2.3 5.2 1 1.3-1 1-3.3-.7-5.2-1.6-1.6-3.9-2.3-5.2-1zm-10.8-8.1c-.7 1.3.3 2.9 2.3 3.9 1.6 1 3.6.7 4.3-.7.7-1.3-.3-2.9-2.3-3.9-2-.6-3.6-.3-4.3.7zm32.4 35.6c-1.6 1.3-1 4.3 1.3 6.2 2.3 2.3 5.2 2.6 6.5 1 1.3-1.3.7-4.3-1.3-6.2-2.2-2.3-5.2-2.6-6.5-1zm-11.4-14.7c-1.6 1-1.6 3.6 0 5.9 1.6 2.3 4.3 3.3 5.6 2.3 1.6-1.3 1.6-3.9 0-6.2-1.4-2.3-4-3.3-5.6-2z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Dropbox"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 528 512"><path d="M264.4 116.3l-132 84.3 132 84.3-132 84.3L0 284.1l132.3-84.3L0 116.3 132.3 32l132.1 84.3zM131.6 395.7l132-84.3 132 84.3-132 84.3-132-84.3zm132.8-111.6l132-84.3-132-83.6L395.7 32 528 116.3l-132.3 84.3L528 284.8l-132.3 84.3-131.3-85z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Facebook"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512"><path d="M448 56.7v398.5c0 13.7-11.1 24.7-24.7 24.7H309.1V306.5h58.2l8.7-67.6h-67v-43.2c0-19.6 5.4-32.9 33.5-32.9h35.8v-60.5c-6.2-.8-27.4-2.7-52.2-2.7-51.6 0-87 31.5-87 89.4v49.9h-58.4v67.6h58.4V480H24.7C11.1 480 0 468.9 0 455.3V56.7C0 43.1 11.1 32 24.7 32h398.5c13.7 0 24.8 11.1 24.8 24.7z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
          {{#> login-main-footer-links-item}}
            {{#> login-main-footer-links-item-link login-main-footer-links-item-link--attribute='aria-label="Log in with Gitlab"'}}
              <svg aria-hidden="true" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512"><path d="M29.782 199.732L256 493.714 8.074 309.699c-6.856-5.142-9.712-13.996-7.141-21.993l28.849-87.974zm75.405-174.806c-3.142-8.854-15.709-8.854-18.851 0L29.782 199.732h131.961L105.187 24.926zm56.556 174.806L256 493.714l94.257-293.982H161.743zm349.324 87.974l-28.849-87.974L256 493.714l247.926-184.015c6.855-5.142 9.711-13.996 7.141-21.993zm-85.404-262.78c-3.142-8.854-15.709-8.854-18.851 0l-56.555 174.806h131.961L425.663 24.926z"/></svg>
            {{/login-main-footer-links-item-link}}
          {{/login-main-footer-links-item}}
        {{/login-main-footer-links}}
        {{#> login-main-footer-band}}
          {{#> login-main-footer-band-item}}
            Need an account? <a href="https://www.patternfly.org/">Sign up.</a>
          {{/login-main-footer-band-item}}
          {{#> login-main-footer-band-item}}
            <a href="#">Forgot username or password?</a>
          {{/login-main-footer-band-item}}
        {{/login-main-footer-band}}
      {{/login-main-footer}}
    {{/login-main}}
    {{#> login-footer}}
      <p>This is placeholder text only. Use this area to place any information or introductory message about your application that may be relevant to users.</p>
      {{#> list list--modifier="pf-m-inline"}}
        <li>
          <a href="#">
            Terms of use
          </a>
        </li>
        <li>
          <a href="#">
            Help
          </a>
        </li>
        <li>
          <a href="#">
            Privacy policy
          </a>
        </li>
      {{/list}}
    {{/login-footer}}
  {{/login-container}}
{{/login}}
```

## Documentation
### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-login` | `<div>` |  Initializes the login component. **Required**|
| `.pf-c-login__container` | `<div>` |  Positions the login component content. **Required**|
| `.pf-c-login__header` | `<header>` |  Positions the login header. **Required**|
| `.pf-c-login__header .pf-c-brand` | `<img>` |  Creates a brand image inside of login header. **Required** |
| `.pf-c-login__main` | `<main>` |  Positions the login main area. **Required** |
| `.pf-c-login__main-header` | `<header>` |  Creates the header of the main area. **Required** |
| `.pf-c-login__main-header .pf-c-title` | `<h1>,<h2>,<h3>,<h4>,<h5>,<h6>` |  Creates a title in the main header area. **Required** |
| `.pf-c-login__main-header-desc` | `<p>` |  Creates the description in the main area header. |
| `.pf-c-login__main-header .pf-c-dropdown` | `<div>` |  Creates a language selector dropdown in the main header area. |
| `.pf-c-login__main-body` | `<div>` |  Creates the body of the main area. **Required** |
| `.pf-c-login__main-body .pf-c-form` | `<form>` |  Creates the login form in the main body area. **Required**|
| `.pf-c-login__main-body .pf-c-form .pf-c-form-helper-text.pf-m-error` | `<form>` | Creates the error messages shown when the form has errors. When not active, apply `.pf-m-hidden.` **Required** |
| `.pf-c-login__main-footer` | `<footer>` |  Creates the footer of the main area. **Required** |
| `.pf-c-login__main-footer-links` | `<ul>` |  Creates a list of links in the main footer. **Required** |
| `.pf-c-login__main-footer-links-item` | `<li>` |  Creates proper spacing for links in the main footer. **Required** |
| `.pf-c-login__main-footer-links-item-link` | `<a>` |  Creates link in links list in footer. **Required** |
| `.pf-c-login__main-footer-band` | `<div>` | Styles a band in the footer. |
| `.pf-c-login__main-footer-band-item` | `<p>` | Adds information to the band in the footer. |
| `.pf-c-login__footer` | `<footer>` |  Positions the login footer. **Required** |
| `.pf-c-login__footer .pf-c-list` | `<ul>` |  Creates a list of links in the login footer. **Required** |