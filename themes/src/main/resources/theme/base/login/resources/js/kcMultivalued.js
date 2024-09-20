const DATA_KC_MULTIVALUED = 'data-kcMultivalued';
const KC_ADD_ACTION_PREFIX = "kc-add-";
const KC_REMOVE_ACTION_PREFIX = "kc-remove-";
const KC_ACTION_CLASS = "pf-c-button pf-m-inline pf-m-link";

function createAddAction(element) {
    const action = createAction("Add value",
        KC_ADD_ACTION_PREFIX,
        element,
        () => {
            const name = element.getAttribute("name");
            const elements = getInputElementsByName().get(name);
            const length = elements.length;

            if (length === 0) {
                return;
            }

            const lastNode = elements[length - 1];
            const newNode = lastNode.cloneNode(true);
            newNode.setAttribute("id", name + "-" + elements.length);
            newNode.value = "";
            lastNode.after(newNode);

            render();
        });

    element.after(action);
}

function createRemoveAction(element, isLastElement) {
    let text = "Remove";

    if (isLastElement) {
        text = text + " | ";
    }

    const action = createAction(text, KC_REMOVE_ACTION_PREFIX, element, () => {
        removeActions(element);
        element.remove();
        render();
    });

    element.insertAdjacentElement('afterend', action);
}

function getInputElementsByName() {
    const selector = document.querySelectorAll(`[${DATA_KC_MULTIVALUED}]`);
    const elementsByName = new Map();

    for (let element of Array.from(selector.values())) {
        let name = element.getAttribute("name");
        let elements = elementsByName.get(name);

        if (!elements) {
            elements = [];
            elementsByName.set(name, elements);
        }

        elements.push(element);
    }

    return elementsByName;
}

function removeActions(element) {
    for (let actionPrefix of [KC_ADD_ACTION_PREFIX, KC_REMOVE_ACTION_PREFIX]) {
        const action = document.getElementById(actionPrefix + element.getAttribute("id"));

        if (action) {
            action.remove();
        }
    }
}

function createAction(text, type, element, onClick) {
    const action = document.createElement("button")
    action.setAttribute("id", type + element.getAttribute("id"));
    action.setAttribute("type", "button");
    action.innerText = text;
    action.setAttribute("class", KC_ACTION_CLASS);
    action.addEventListener("click", onClick);
    return action;
}

function render() {
    getInputElementsByName().forEach((elements, name) => {
        elements.forEach((element, index) => {
            removeActions(element);

            element.setAttribute("id", name + "-" + index);

            const lastNode = element === elements[elements.length - 1];

            if (lastNode) {
                createAddAction(element);
            }

            if (elements.length > 1) {
                createRemoveAction(element, lastNode);
            }
        });
    });
}

render();