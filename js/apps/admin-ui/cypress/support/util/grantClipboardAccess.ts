export default function grantClipboardAccess() {
  // Use the Chrome debugger protocol to grant access to the clipboard.
  // https://chromedevtools.github.io/devtools-protocol/tot/Browser/#method-grantPermissions
  cy.wrap(
    Cypress.automation("remote:debugger:protocol", {
      command: "Browser.grantPermissions",
      params: {
        permissions: ["clipboardReadWrite", "clipboardSanitizedWrite"],
        origin: window.location.origin,
      },
    }),
  );
}
