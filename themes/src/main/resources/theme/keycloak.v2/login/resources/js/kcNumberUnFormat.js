// @ts-check
import { formatNumber } from "./common.js";

const KC_NUMBER_UNFORMAT = 'kcNumberUnFormat';

const unFormatInputs = [];

export function unFormat() {
    const inputs = document.getElementsByTagName("input");

    for (let i = 0, length = inputs.length; i < length; i++) {
        const rawFormat = inputs[i].getAttribute(`data-${KC_NUMBER_UNFORMAT}`);
        if (rawFormat) {
            unFormatInputs.push({ input: inputs[i], rawFormat });
        }
    }
}

document.addEventListener("DOMContentLoaded", () => {
    for (let form of document.forms) {
        form.addEventListener('submit', () => {
            for (let i = 0; i < unFormatInputs.length; i++) {
                const element = unFormatInputs[i];
                element.input.value = formatNumber(element.value, element.rawFormat);
            }
        });
    }
})