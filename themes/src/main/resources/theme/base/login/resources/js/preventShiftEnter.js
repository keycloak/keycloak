document
  .querySelector("#kc-form-login #username")
  ?.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && event.shiftKey) {
      event.preventDefault();
    }
  });
