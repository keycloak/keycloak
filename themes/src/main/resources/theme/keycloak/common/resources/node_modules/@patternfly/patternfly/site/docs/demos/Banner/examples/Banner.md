---
title: 'Banner'
section: demos
beta: true
cssPrefix: pf-c-banner
wrapperTag: div
---

## Examples
```hbs title=Basic isFullscreen
{{#> page page--id="page-layout-table-simple"}}
  {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
    Skip to content
  {{/skip-to-content}}
  {{#> page-header}}
    {{> __banner-page-header}}
  {{/page-header}}
  {{#> page-sidebar}}
    {{> __banner-page-nav}}
  {{/page-sidebar}}
  {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
    {{#> banner banner--modifier="pf-m-sticky"}}
      {{> __banner-demo}}
    {{/banner}}
    {{#> page-main-section page-main-section--modifier="pf-m-light"}}
      {{> __banner-page-main-section-demo}}
    {{/page-main-section}}
    {{#> page-main-section page-main-section--modifier="pf-m-no-padding pf-m-padding-on-xl"}}
      {{#> card}}
        {{> __banner-table}}
      {{/card}}
    {{/page-main-section}}
  {{/page-main}}
{{/page}}
```

```hbs title=Top/bottom isFullscreen
{{#> l-flex l-flex--modifier="pf-m-column pf-m-nowrap pf-m-space-items-none" l-flex--attribute='style="height: 100%;"'}}
  {{#> l-flex-item}}
    {{#> banner banner--modifier="pf-m-sticky"}}
      {{> __banner-demo}}
    {{/banner}}
  {{/l-flex-item}}
  {{#> l-flex-item l-flex-item--modifier="pf-m-grow" l-flex-item--attribute='style="min-height: 0;"'}}
    {{#> page page--id="page-layout-table-top-bottom"}}
      {{#> skip-to-content skip-to-content--attribute=(concat 'href="#main-content-' page--id '"')}}
        Skip to content
      {{/skip-to-content}}
      {{#> page-header}}
        {{> __banner-page-header}}
      {{/page-header}}
      {{#> page-sidebar}}
        {{> __banner-page-nav}}
      {{/page-sidebar}}
      {{#> page-main page-main--attribute=(concat 'id="main-content-' page--id '"')}}
        {{#> page-main-section page-main-section--modifier="pf-m-light"}}
          {{> __banner-page-main-section-demo}}
        {{/page-main-section}}
        {{#> page-main-section page-main-section--modifier="pf-m-no-padding pf-m-padding-on-xl"}}
          {{#> card}}
            {{> __banner-table}}
          {{/card}}
        {{/page-main-section}}
      {{/page-main}}
    {{/page}}
  {{/l-flex-item}}
  {{#> l-flex-item}}
    {{#> banner banner--modifier="pf-m-sticky"}}
      {{> __banner-demo}}
    {{/banner}}
  {{/l-flex-item}}
{{/l-flex}}
```
