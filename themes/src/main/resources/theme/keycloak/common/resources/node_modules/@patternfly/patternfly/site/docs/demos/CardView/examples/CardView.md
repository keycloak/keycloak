---
title: Card view
section: demos
wrapperTag: div
---

## Demos
```hbs title=Basic isFullscreen
{{#> page page--id="card-view"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{!-- Brand --}}
    {{#> page-header-brand}}
      {{#> page-header-brand-toggle}}
        {{#> button button--modifier="pf-m-plain" button--attribute=(concat 'id="' page--id '-nav-toggle" aria-label="Global navigation" aria-expanded="true" aria-controls="' page--id '-primary-nav"')}}
          <i class="fas fa-bars" aria-hidden="true"></i>
        {{/button}}
      {{/page-header-brand-toggle}}
      {{#> page-header-brand-link page-header-brand-link--href="#"}}
        {{#> brand brand--attribute='src="/assets/images/PF-Masthead-Logo.svg" alt="Patternfly Logo"'}}{{/brand}}
      {{/page-header-brand-link}}
    {{/page-header-brand}}

    {{#> page-template-header-tools-elements}}
    {{/page-template-header-tools-elements}}
  {{/page-header}}
  {{!-- Nav --}}
  {{#> page-sidebar}}
    {{#> nav nav--attribute=(concat 'id="' page--id '-primary-nav" aria-label="Global"')}}
      {{#> nav-list}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#" nav-link--current="true"}}
            System Panel
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Policy
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Authentication
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Network Services
          {{/nav-link}}
        {{/nav-item}}
        {{#> nav-item}}
          {{#> nav-link nav-link--href="#"}}
            Server
          {{/nav-link}}
        {{/nav-item}}
      {{/nav-list}}
    {{/nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> page-main-section page-main-section--modifier="pf-m-light"}}
      {{#> content}}
        <h1>Projects</h1>
        <p>This is a demo that showcases Patternfly Cards. </p>
      {{/content}}
    {{/page-main-section}}
    {{#> page-main-section}}
      {{> card-view-demo-template-gallery}}
    {{/page-main-section}}
  {{/page-main}}
{{/page}}
```
