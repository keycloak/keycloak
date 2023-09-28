const template = document.createElement("template");
template.innerHTML = `
    ${
        [...document.styleSheets].map((styleSheet) => `<link href="${styleSheet.href}" rel="stylesheet"/>`)
        .join("\n")
    }
    
    <style>
        .progress {
            margin-top: 1px;
            margin-bottom: 0;
        }
    </style>
    
    <div class="progress">
        <div class="progress-bar" role="status"></div>
    </div>
`;

class PasswordEntropy extends HTMLElement {
    constructor() {
        super();
    }

    connectedCallback() {
        const shadow = this.attachShadow({ mode: "open" });
        shadow.append(template.content.cloneNode(true));

        const passwordElement = this.parentElement.querySelector("input[type=password]");
        passwordElement.oninput = () => this.update(passwordElement.value);

        this.entropyStatusElement = this.shadowRoot.querySelector(".progress-bar");

        this.rating = [this.dataset.passwordPoor, this.dataset.passwordWeak, this.dataset.passwordGood, this.dataset.passwordStrong];
        this.ratingClassNames = ["progress-bar-danger", "progress-bar-warning", "progress-bar-info", "progress-bar-success"];
        this.ratingRange = 25;

        this.style.display = "none";
    }

    update = (password) => {
        if (!password.length) {
            this.style.display = "none";
            return;
        }

        const entropy = this.calculateEntropy(password);

        this.updateElements(entropy);
    }

    // 00-24    poor password
    // 25-49    weak password
    // 50-74    good password
    // 75-100   strong password
    // entropy source: https://www.baeldung.com/cs/password-entropy
    calculateEntropy = (password) => {
        let charSetSize = 0;
        if (/[a-z]/.test(password)) charSetSize += 26;
        if (/[A-Z]/.test(password)) charSetSize += 26;
        if (/[0-9]/.test(password)) charSetSize += 10;
        if (/\W|_/.test(password)) charSetSize += 32; // 149_878 available unicode chars
        return Math.log2(charSetSize ** password.length);
    }

    clamp = (number, min, max) => Math.max(min, Math.min(number, max));

    updateElements = (entropy) =>  {
        const ratingIndex = Math.trunc(entropy / this.ratingRange);
        const clampedIndex = this.clamp(ratingIndex, 0, this.rating.length - 1);

        this.ratingClassNames.forEach(clazz => this.entropyStatusElement.classList.remove(clazz));
        this.entropyStatusElement.innerText = this.rating[clampedIndex];
        this.entropyStatusElement.classList.add(this.ratingClassNames[clampedIndex]);
        this.entropyStatusElement.style.width = `${(clampedIndex+1) * this.ratingRange}%`;
        this.style.display = "block";
    }
}

customElements.define("password-entropy", PasswordEntropy);
