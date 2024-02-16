import {formatNumber} from "./common.js";

const DATA_KC_NUMBER_FORMAT = 'data-kcNumberFormat';

document.querySelectorAll(`[${DATA_KC_NUMBER_FORMAT}]`)
    .forEach(input => {
        const format = input.getAttribute(DATA_KC_NUMBER_FORMAT);

        input.addEventListener('keyup', (event) => {
            input.value = formatNumber(input.value, format);
        });

        input.value = formatNumber(input.value, format);
    });