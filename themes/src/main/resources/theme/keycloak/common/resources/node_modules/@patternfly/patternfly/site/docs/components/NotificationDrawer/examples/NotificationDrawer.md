---
title: Notification drawer
section: components
beta: true
cssPrefix: pf-c-notification-drawer
---

## Examples
```hbs title=Basic
{{#> notification-drawer notification-drawer--id="notification-drawer-basic"}}
  {{#> notification-drawer-header}}
    {{#> notification-drawer-header-title}}
      Notifications
    {{/notification-drawer-header-title}}
    {{#> notification-drawer-header-status}}
      3 unread
    {{/notification-drawer-header-status}}
    {{#> notification-drawer-header-action}}
      {{#> dropdown id=(concat notification-drawer--id "-header-action") dropdown-menu--modifier="pf-m-align-right" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
    {{/notification-drawer-header-action}}
  {{/notification-drawer-header}}
  {{#> notification-drawer-body}}
    {{> notification-drawer-basic-list}}
  {{/notification-drawer-body}}
{{/notification-drawer}}
```

```hbs title=Groups
{{#> notification-drawer notification-drawer--id="notification-drawer-groups"}}
  {{#> notification-drawer-header}}
    {{#> notification-drawer-header-title}}
      Notifications
    {{/notification-drawer-header-title}}
    {{#> notification-drawer-header-status}}
      3 unread
    {{/notification-drawer-header-status}}
    {{#> notification-drawer-header-action}}
      {{#> dropdown id=(concat notification-drawer--id "-header-action") dropdown-menu--modifier="pf-m-align-right" dropdown--IsActionMenu="true" dropdown-toggle--modifier="pf-m-plain" dropdown--HasKebabIcon="true" aria-label="Actions"}}{{/dropdown}}
    {{/notification-drawer-header-action}}
  {{/notification-drawer-header}}
  {{#> notification-drawer-body}}
    {{#> notification-drawer-group-list}}
      {{#> notification-drawer-group notification-drawer--id=(concat notification-drawer--id '-group1')}}
        <h1>
          {{#> notification-drawer-group-toggle}}
            {{#> notification-drawer-group-toggle-title}}
              First notification group
            {{/notification-drawer-group-toggle-title}}
            {{#> notification-drawer-group-toggle-count}}
              {{#> badge badge--modifier="pf-m-unread"}}2{{/badge}}
            {{/notification-drawer-group-toggle-count}}
            {{> notification-drawer-group-toggle-icon}}
          {{/notification-drawer-group-toggle}}
        </h1>
        {{> notification-drawer-basic-list}}
      {{/notification-drawer-group}}
      {{#> notification-drawer-group notification-drawer--id=(concat notification-drawer--id '-group2') notification-drawer-group--IsExpanded="true"}}
        <h1>
          {{#> notification-drawer-group-toggle}}
            {{#> notification-drawer-group-toggle-title}}
              Second notification group
            {{/notification-drawer-group-toggle-title}}
            {{#> notification-drawer-group-toggle-count}}
              {{#> badge badge--modifier="pf-m-unread"}}2{{/badge}}
            {{/notification-drawer-group-toggle-count}}
            {{> notification-drawer-group-toggle-icon}}
          {{/notification-drawer-group-toggle}}
        </h1>
        {{> notification-drawer-basic-list}}
      {{/notification-drawer-group}}
      {{#> notification-drawer-group notification-drawer--id=(concat notification-drawer--id '-group3')}}
        <h1>
          {{#> notification-drawer-group-toggle}}
            {{#> notification-drawer-group-toggle-title}}
              Third notification group
            {{/notification-drawer-group-toggle-title}}
            {{#> notification-drawer-group-toggle-count}}
              {{#> badge badge--modifier="pf-m-unread"}}2{{/badge}}
            {{/notification-drawer-group-toggle-count}}
            {{> notification-drawer-group-toggle-icon}}
          {{/notification-drawer-group-toggle}}
        </h1>
        {{> notification-drawer-basic-list}}
      {{/notification-drawer-group}}
      {{#> notification-drawer-group notification-drawer--id=(concat notification-drawer--id '-group4')}}
        <h1>
          {{#> notification-drawer-group-toggle}}
            {{#> notification-drawer-group-toggle-title}}
              Fourth notification group
            {{/notification-drawer-group-toggle-title}}
            {{#> notification-drawer-group-toggle-count}}
              {{#> badge badge--modifier="pf-m-unread"}}2{{/badge}}
            {{/notification-drawer-group-toggle-count}}
            {{> notification-drawer-group-toggle-icon}}
          {{/notification-drawer-group-toggle}}
        </h1>
        {{> notification-drawer-basic-list}}
      {{/notification-drawer-group}}
    {{/notification-drawer-group-list}}
  {{/notification-drawer-body}}
{{/notification-drawer}}
```

### Accessibility
| Attribute | Applied to | Outcome |
| -- | -- | -- |
| `aria-expanded="false"` | `.pf-c-notification-drawer__group-toggle` | Indicates that the group notification list is hidden. |
| `aria-expanded="true"` | `.pf-c-notification-drawer__group-toggle` | Indicates that the group notification list is visible. |
| `hidden` | `.pf-c-notification-drawer__list` | Indicates that the group notification list is hidden so that it isn't visible in the UI and isn't accessed by assistive technologies. |
| `tabindex="0"` | `.pf-c-notification-drawer__list-item.pf-m-hoverable` | Inserts the hoverable list item into the tab order of the page so that it is focusable. |
| `aria-hidden="true"` | `.pf-c-notification-drawer__group-toggle-icon > *`, `.pf-c-notification-drawer__list-item-header-icon > *` | Hides icon for assistive technologies. |

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-notification-drawer` | `<div>` | Initiates the notification drawer. **Required** |
| `.pf-c-notification-drawer__header` | `<div>` | Initiates the notification drawer header. **Required** |
| `.pf-c-notification-drawer__header-title` | `<h1>` | Initiates the notification drawer header title. **Required** |
| `.pf-c-notification-drawer__header-status` | `<span>` | Initiates the notification drawer header status. |
| `.pf-c-notification-drawer__header-action` | `<div>` | Initiates the notification drawer header action. |
| `.pf-c-notification-drawer__body` | `<div>` | Initiates the notification drawer body. **Required** |
| `.pf-c-notification-drawer__list` | `<ul>` | Initiates a notification list. **Required** |
| `.pf-c-notification-drawer__list-item` | `<li>` | Initiates a notification list item. **Always use with a state modifier - one of `.pf-m-info`, `.pf-m-warning`, `.pf-m-danger`, `.pf-m-success`.** **Required** |
| `.pf-c-notification-drawer__list-item-header` | `<div>` | Initiates a notification list item header. **Required** |
| `.pf-c-notification-drawer__list-item-header-icon` | `<span>` | Initiates a notification list item header icon. **Required** |
| `.pf-c-notification-drawer__list-item-header-title` | `<h2>` | Initiates a notification list item header title. **Required** |
| `.pf-c-notification-drawer__list-item-action` | `<div>` | Initiates a notification list item action. |
| `.pf-c-notification-drawer__list-item-description` | `<div>` | Initiates a notification list item description. **Required** |
| `.pf-c-notification-drawer__list-item-timestamp` | `<div>` | Initiates a notification list item timestamp. **Required** |
| `.pf-c-notification-drawer__group-list` | `<div>` | Initiates a notification group list. **Required when notifications are grouped** |
| `.pf-c-notification-drawer__group` | `<section>` | Initiates a notification group. **Required** |
| `.pf-c-notification-drawer__group-toggle` | `<button>` | Initiates a notification group toggle. **Required** |
| `.pf-c-notification-drawer__group-title` | `<div>` | Initiates a notification group toggle title. **Required** |
| `.pf-c-notification-drawer__group-count` | `<div>` | Initiates a notification group toggle count. |
| `.pf-c-notification-drawer__group-icon` | `<span>` | Initiates a notification group toggle icon. **Required** |
| `.pf-m-info` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item for the info state. |
| `.pf-m-warning` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item for the warning state. |
| `.pf-m-danger` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item for the danger state. |
| `.pf-m-success` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item for the success state. |
| `.pf-m-read` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item for the read state. |
| `.pf-m-hoverable` | `.pf-c-notification-drawer__list-item` | Modifies a notification list item hover states to inidicate it is clickable. |
| `.pf-m-expanded` | `.pf-c-notification-drawer__group` | Modifies a notification group for the expanded state. |
| `.pf-m-truncate` | `.pf-c-notification-drawer__list-item-header-title` |  Modifies the title to display a single line and truncate any overflow text with ellipses. **Note:** you can specify the max number of lines to show by setting the `--pf-c-notification-drawer__list-item-header-title--max-lines` (the default value is `1`). |
