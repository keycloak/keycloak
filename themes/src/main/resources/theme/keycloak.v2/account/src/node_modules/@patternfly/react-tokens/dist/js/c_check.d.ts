export const c_check: {
  ".pf-c-check": {
    "c_check_GridGap": {
      "name": "--pf-c-check--GridGap",
      "value": "0.25rem 0.5rem",
      "values": [
        "--pf-global--spacer--xs --pf-global--spacer--sm",
        "$pf-global--spacer--xs $pf-global--spacer--sm",
        "pf-size-prem(4px) pf-size-prem(8px)",
        "0.25rem 0.5rem"
      ]
    },
    "c_check__label_disabled_Color": {
      "name": "--pf-c-check__label--disabled--Color",
      "value": "#6a6e73",
      "values": [
        "--pf-global--disabled-color--100",
        "$pf-global--disabled-color--100",
        "$pf-color-black-600",
        "#6a6e73"
      ]
    },
    "c_check__label_Color": {
      "name": "--pf-c-check__label--Color",
      "value": "#151515",
      "values": [
        "--pf-global--Color--100",
        "$pf-global--Color--100",
        "$pf-color-black-900",
        "#151515"
      ]
    },
    "c_check__label_FontWeight": {
      "name": "--pf-c-check__label--FontWeight",
      "value": "400",
      "values": [
        "--pf-global--FontWeight--normal",
        "$pf-global--FontWeight--normal",
        "400"
      ]
    },
    "c_check__label_FontSize": {
      "name": "--pf-c-check__label--FontSize",
      "value": "1rem",
      "values": [
        "--pf-global--FontSize--md",
        "$pf-global--FontSize--md",
        "pf-font-prem(16px)",
        "1rem"
      ]
    },
    "c_check__label_LineHeight": {
      "name": "--pf-c-check__label--LineHeight",
      "value": "1.3",
      "values": [
        "--pf-global--LineHeight--sm",
        "$pf-global--LineHeight--sm",
        "1.3"
      ]
    },
    "c_check__input_Height": {
      "name": "--pf-c-check__input--Height",
      "value": "1rem",
      "values": [
        "--pf-c-check__label--FontSize",
        "--pf-global--FontSize--md",
        "$pf-global--FontSize--md",
        "pf-font-prem(16px)",
        "1rem"
      ]
    },
    "c_check__input_MarginTop": {
      "name": "--pf-c-check__input--MarginTop",
      "value": "calc(((1rem * 1.3) - 1rem) / 2)",
      "values": [
        "calc(((--pf-c-check__label--FontSize * --pf-c-check__label--LineHeight) - --pf-c-check__input--Height) / 2)",
        "calc(((--pf-global--FontSize--md * --pf-global--LineHeight--sm) - --pf-c-check__label--FontSize) / 2)",
        "calc((($pf-global--FontSize--md * $pf-global--LineHeight--sm) - --pf-global--FontSize--md) / 2)",
        "calc((($pf-global--FontSize--md * $pf-global--LineHeight--sm) - $pf-global--FontSize--md) / 2)",
        "calc(((pf-font-prem(16px) * 1.3) - pf-font-prem(16px)) / 2)",
        "calc(((1rem * 1.3) - 1rem) / 2)"
      ]
    },
    "c_check__description_FontSize": {
      "name": "--pf-c-check__description--FontSize",
      "value": "0.875rem",
      "values": [
        "--pf-global--FontSize--sm",
        "$pf-global--FontSize--sm",
        "pf-font-prem(14px)",
        "0.875rem"
      ]
    },
    "c_check__description_Color": {
      "name": "--pf-c-check__description--Color",
      "value": "#6a6e73",
      "values": [
        "--pf-global--Color--200",
        "$pf-global--Color--200",
        "$pf-color-black-600",
        "#6a6e73"
      ]
    },
    "c_check__body_MarginTop": {
      "name": "--pf-c-check__body--MarginTop",
      "value": "0.5rem",
      "values": [
        "--pf-global--spacer--sm",
        "$pf-global--spacer--sm",
        "pf-size-prem(8px)",
        "0.5rem"
      ]
    }
  },
  ".pf-c-check.pf-m-standalone": {
    "c_check_GridGap": {
      "name": "--pf-c-check--GridGap",
      "value": "0"
    },
    "c_check__input_Height": {
      "name": "--pf-c-check__input--Height",
      "value": "auto"
    },
    "c_check__input_MarginTop": {
      "name": "--pf-c-check__input--MarginTop",
      "value": "0"
    }
  },
  ".pf-c-check__label:disabled": {
    "c_check__label_Color": {
      "name": "--pf-c-check__label--Color",
      "value": "#6a6e73",
      "values": [
        "--pf-c-check__label--disabled--Color",
        "--pf-global--disabled-color--100",
        "$pf-global--disabled-color--100",
        "$pf-color-black-600",
        "#6a6e73"
      ]
    }
  }
};
export default c_check;