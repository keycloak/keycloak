---
id: Notification badge
section: components
cssPrefix: pf-c-notification-badge
---import './NotificationBadge.css'

## Examples

### Basic

```html
<div class="pf-t-dark">
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Notifications"
  >
    <span class="pf-c-notification-badge pf-m-read">
      <i class="pf-icon-bell" aria-hidden="true"></i>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Unread notifications"
  >
    <span class="pf-c-notification-badge pf-m-unread">
      <i class="pf-icon-bell" aria-hidden="true"></i>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Attention notifications"
  >
    <span class="pf-c-notification-badge pf-m-attention">
      <i class="pf-icon-attention-bell" aria-hidden="true"></i>
    </span>
  </button>

  <br />
  <br />

  <button class="pf-c-button pf-m-plain" type="button" aria-label="Tasks">
    <span class="pf-c-notification-badge pf-m-read">
      <i class="pf-icon-task" aria-hidden="true"></i>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Unread tasks"
  >
    <span class="pf-c-notification-badge pf-m-unread">
      <i class="pf-icon-task" aria-hidden="true"></i>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Attention tasks"
  >
    <span class="pf-c-notification-badge pf-m-attention">
      <i class="pf-icon-task" aria-hidden="true"></i>
    </span>
  </button>
</div>

```

### With count

```html
<div class="pf-t-dark">
  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Notifications"
  >
    <span class="pf-c-notification-badge pf-m-read">
      <i class="pf-icon-bell" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">24</span>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Unread notifications"
  >
    <span class="pf-c-notification-badge pf-m-unread">
      <i class="pf-icon-bell" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">25</span>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Attention notifications"
  >
    <span class="pf-c-notification-badge pf-m-attention">
      <i class="pf-icon-attention-bell" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">26</span>
    </span>
  </button>

  <br />
  <br />

  <button class="pf-c-button pf-m-plain" type="button" aria-label="Tasks">
    <span class="pf-c-notification-badge pf-m-read">
      <i class="pf-icon-task" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">24</span>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Unread tasks"
  >
    <span class="pf-c-notification-badge pf-m-unread">
      <i class="pf-icon-task" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">25</span>
    </span>
  </button>

  <button
    class="pf-c-button pf-m-plain"
    type="button"
    aria-label="Attention tasks"
  >
    <span class="pf-c-notification-badge pf-m-attention">
      <i class="pf-icon-task" aria-hidden="true"></i>
      <span class="pf-c-notification-badge__count">26</span>
    </span>
  </button>
</div>

```

## Documentation

### Overview

Always add a modifier class. Never use the class `.pf-c-notification-badge` on its own. This component is designed to be used within a dark component such as the [masthead](/components/masthead).

Note: The [page component](/components/page) currently handles the selected state styling of the notification badge using the page header tools item styling. If this component is used elsewhere, custom styling may be needed to correctly indicate the selected state.

### Accessibility

Be sure that the component associated with this indicator handles screen reader text indicating read or unread notifications.

### Usage

| Class                             | Applied to                 | Outcome                                                                  |
| --------------------------------- | -------------------------- | ------------------------------------------------------------------------ |
| `.pf-c-notification-badge`        | `<div>`                    | Initiates a notification badge. **Always use it with a modifier class.** |
| `.pf-c-notification-badge__count` | `<span>`                   | Initiates a notification badge count.                                    |
| `.pf-m-read`                      | `.pf-c-notification-badge` | Applies read notification badge styling.                                 |
| `.pf-m-unread`                    | `.pf-c-notification-badge` | Applies unread notification badge styling.                               |
| `.pf-m-attention`                 | `.pf-c-notification-badge` | Applies attention notification badge styling.                            |
