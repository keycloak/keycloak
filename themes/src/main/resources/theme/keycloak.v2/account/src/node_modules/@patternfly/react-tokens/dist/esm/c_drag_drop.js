export const c_drag_drop = {
  ".pf-c-draggable": {
    "c_draggable_Cursor": {
      "name": "--pf-c-draggable--Cursor",
      "value": "auto"
    },
    "c_draggable_m_dragging_Cursor": {
      "name": "--pf-c-draggable--m-dragging--Cursor",
      "value": "grabbing"
    },
    "c_draggable_m_dragging_BoxShadow": {
      "name": "--pf-c-draggable--m-dragging--BoxShadow",
      "value": "0 0.25rem 0.5rem 0rem rgba(3, 3, 3, 0.12), 0 0 0.25rem 0 rgba(3, 3, 3, 0.06)",
      "values": [
        "--pf-global--BoxShadow--md",
        "$pf-global--BoxShadow--md",
        "0 pf-size-prem(4px) pf-size-prem(8px) pf-size-prem(0) rgba($pf-color-black-1000, .12), 0 0 pf-size-prem(4px) 0 rgba($pf-color-black-1000, .06)",
        "0 pf-size-prem(4px) pf-size-prem(8px) pf-size-prem(0) rgba(#030303, .12), 0 0 pf-size-prem(4px) 0 rgba(#030303, .06)",
        "0 0.25rem 0.5rem 0rem rgba(3, 3, 3, 0.12), 0 0 0.25rem 0 rgba(3, 3, 3, 0.06)"
      ]
    },
    "c_draggable_m_dragging_after_BorderWidth": {
      "name": "--pf-c-draggable--m-dragging--after--BorderWidth",
      "value": "1px",
      "values": [
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_draggable_m_dragging_after_BorderColor": {
      "name": "--pf-c-draggable--m-dragging--after--BorderColor",
      "value": "#06c",
      "values": [
        "--pf-global--active-color--100",
        "$pf-global--active-color--100",
        "$pf-color-blue-400",
        "#06c"
      ]
    },
    "c_draggable_m_drag_outside_Cursor": {
      "name": "--pf-c-draggable--m-drag-outside--Cursor",
      "value": "not-allowed"
    },
    "c_draggable_m_drag_outside_after_BorderColor": {
      "name": "--pf-c-draggable--m-drag-outside--after--BorderColor",
      "value": "#c9190b",
      "values": [
        "--pf-global--danger-color--100",
        "$pf-global--danger-color--100",
        "$pf-color-red-100",
        "#c9190b"
      ]
    }
  },
  ".pf-c-draggable.pf-m-dragging": {
    "c_draggable_Cursor": {
      "name": "--pf-c-draggable--Cursor",
      "value": "grabbing",
      "values": [
        "--pf-c-draggable--m-dragging--Cursor",
        "grabbing"
      ]
    }
  },
  ".pf-c-draggable.pf-m-drag-outside": {
    "c_draggable_m_dragging_Cursor": {
      "name": "--pf-c-draggable--m-dragging--Cursor",
      "value": "not-allowed",
      "values": [
        "--pf-c-draggable--m-drag-outside--Cursor",
        "not-allowed"
      ]
    },
    "c_draggable_m_dragging_after_BorderColor": {
      "name": "--pf-c-draggable--m-dragging--after--BorderColor",
      "value": "#c9190b",
      "values": [
        "--pf-c-draggable--m-drag-outside--after--BorderColor",
        "--pf-global--danger-color--100",
        "$pf-global--danger-color--100",
        "$pf-color-red-100",
        "#c9190b"
      ]
    }
  },
  ".pf-c-droppable": {
    "c_droppable_m_dragging_after_BackgroundColor": {
      "name": "--pf-c-droppable--m-dragging--after--BackgroundColor",
      "value": "rgba(255, 255, 255, 0.6)"
    },
    "c_droppable_m_dragging_after_BorderWidth": {
      "name": "--pf-c-droppable--m-dragging--after--BorderWidth",
      "value": "1px",
      "values": [
        "--pf-global--BorderWidth--sm",
        "$pf-global--BorderWidth--sm",
        "1px"
      ]
    },
    "c_droppable_m_dragging_after_BorderColor": {
      "name": "--pf-c-droppable--m-dragging--after--BorderColor",
      "value": "#06c",
      "values": [
        "--pf-global--active-color--100",
        "$pf-global--active-color--100",
        "$pf-color-blue-400",
        "#06c"
      ]
    },
    "c_droppable_m_drag_outside_after_BorderColor": {
      "name": "--pf-c-droppable--m-drag-outside--after--BorderColor",
      "value": "#c9190b",
      "values": [
        "--pf-global--danger-color--100",
        "$pf-global--danger-color--100",
        "$pf-color-red-100",
        "#c9190b"
      ]
    }
  },
  ".pf-c-droppable.pf-m-drag-outside": {
    "c_droppable_m_dragging_after_BorderColor": {
      "name": "--pf-c-droppable--m-dragging--after--BorderColor",
      "value": "#c9190b",
      "values": [
        "--pf-c-droppable--m-drag-outside--after--BorderColor",
        "--pf-global--danger-color--100",
        "$pf-global--danger-color--100",
        "$pf-color-red-100",
        "#c9190b"
      ]
    }
  }
};
export default c_drag_drop;