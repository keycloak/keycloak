---
title: Context selector
section: components
cssPrefix: pf-c-context-selector
---
import './ContextSelector.css'

## Examples
```hbs title=Basic
{{#> context-selector context-selector--id="context-selector-collapsed-example" context-selector--label-text="Selected project"}}
  {{#> context-selector-toggle context-selector-toggle--attribute=(concat 'id="' context-selector--id '-toggle"' 'aria-labelledby="' context-selector--id '-label ' context-selector--id '-toggle"')}}
    {{#> context-selector-toggle-text}}
      My project
    {{/context-selector-toggle-text}}
    {{#> context-selector-toggle-icon}}
    {{/context-selector-toggle-icon}}
  {{/context-selector-toggle}}
  {{#> context-selector-menu}}
    {{#> context-selector-menu-search}}
      {{#> input-group}}
        {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'type="search"' 'placeholder="Search"' 'id="textInput1"' 'name="textInput1"' 'aria-labelledby="' context-selector--id '-search-button"')}}
        {{/form-control}}
        {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' context-selector--id '-search-button"' 'aria-label="Search menu items"')}}
          <i class="fas fa-search" aria-hidden="true"></i>
        {{/button}}
      {{/input-group}}
    {{/context-selector-menu-search}}
    {{#> context-selector-menu-menu}}
      <li>
        {{#> context-selector-menu-menu-item}}
          My project
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          OpenShift cluster
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Production Ansible
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          AWS
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Azure
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          My project
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          OpenShift cluster
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Production Ansible
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          AWS
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Azure
        {{/context-selector-menu-menu-item}}
      </li>
    {{/context-selector-menu-menu}}
  {{/context-selector-menu}}
{{/context-selector}}

{{#> context-selector context-selector--id="context-selector-expanded-example" context-selector--label-text="Selected Project" context-selector--IsExpanded="true"}}
  {{#> context-selector-toggle context-selector-toggle--attribute=(concat 'id="' context-selector--id '-toggle"' 'aria-labelledby="' context-selector--id '-label ' context-selector--id '-toggle"')}}
    {{#> context-selector-toggle-text}}
      My project
    {{/context-selector-toggle-text}}
    {{#> context-selector-toggle-icon}}
    {{/context-selector-toggle-icon}}
  {{/context-selector-toggle}}
  {{#> context-selector-menu}}
    {{#> context-selector-menu-search}}
      {{#> input-group}}
        {{#> form-control controlType="input" input="true" form-control--attribute=(concat 'type="search" placeholder="Search" id="textInput2" name="textInput2" aria-labelledby="' context-selector--id '-search-button"')}}
        {{/form-control}}
        {{#> button button--modifier="pf-m-control" button--attribute=(concat 'id="' context-selector--id '-search-button"' 'aria-label="Search menu items"')}}
          <i class="fas fa-search" aria-hidden="true"></i>
        {{/button}}
      {{/input-group}}
    {{/context-selector-menu-search}}
    {{#> context-selector-menu-menu}}
      <li>
        {{#> context-selector-menu-menu-item}}
          My project
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          OpenShift cluster
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Production Ansible
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          AWS
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Azure
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          My project
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          OpenShift cluster
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Production Ansible
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          AWS
        {{/context-selector-menu-menu-item}}
      </li>
      <li>
        {{#> context-selector-menu-menu-item}}
          Azure
        {{/context-selector-menu-menu-item}}
      </li>
    {{/context-selector-menu-menu}}
  {{/context-selector-menu}}
{{/context-selector}}
```

## Documentation
### Accessibility
Added after React implementation.

| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-context-selector` | `<div>` | Initiates a context selector.|
| `.pf-c-context-selector__toggle` | `<button>` | Initiates a toggle. |
| `.pf-c-context-selector__toggle-text` | `<span>` | Initiates text inside the toggle. |
| `.pf-c-context-selector__toggle-icon` | `<span>` | Inititiates the toggle icon wrapper. |
| `.pf-c-context-selector__menu` | `<div>` | Initiaties a menu. |
| `.pf-c-context-selector__menu-search` | `<div>` | Initiates a container for the search input group. |
| `.pf-c-context-selector__menu-list` | `<ul>` | Initiaties an unordered list of menu items that sits under the input container. |
| `.pf-c-context-selector__menu-list-item` | `<li>` | Initiaties a menu item. |
| `.pf-m-expanded` | `.pf-c-context-selector` | Modifies for the expanded state. |
| `.pf-m-active` | `.pf-c-context-selector__toggle` | Forces display of the active state of the toggle. |