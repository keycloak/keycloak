---
title: Notification badge
section: components
cssPrefix: pf-c-notification-badge
---

## Examples
```hbs title=Basic
{{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Unread notifications"'}}
    {{#> notification-badge notification-badge--modifier="pf-m-unread"}}
      <i class="fas fa-bell" aria-hidden="true"></i>
    {{/notification-badge}}
{{/button}}

{{#> button button--modifier="pf-m-plain" button--attribute='aria-label="Notifications"'}}
    {{#> notification-badge notification-badge--modifier="pf-m-read"}}
      <i class="fas fa-bell" aria-hidden="true"></i>
    {{/notification-badge}}
{{/button}}
```

## Documentation
### Overview
Always add a modifier class. Never use the class `.pf-c-notification-badge` on its own.

### Accessibility
Be sure that the component associated with this indicator handles screenreader text indicating read or unread notifications.

### Usage
| Class | Applied to | Outcome |
| -- | -- | -- |
| `.pf-c-notification-badge` | `<div>` |  Initiates a notification badge. **Always use it with a modifier class.** |
| `.pf-m-read` | `.pf-c-notification-badge` |  Applies read notification badge styling. |
| `.pf-m-unread` | `.pf-c-notification-badge` |  Applies unread notification badge styling. |
