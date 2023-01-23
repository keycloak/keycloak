## How to write tests

### Create PageObjects for the page screens to reuse the methods

Don't do this:
```typescript
cy.findByTestId("rs-keys-tab").click();
cy.findByTestId("rs-providers-tab").click();
cy.findAllByTestId("provider-name-link")
  .contains("test_hmac-generated")
  .click();
```

Instead use a page object as it is a lot easier to get the intended functionality of the test:
```typescript
realmSettings.goToProvidersTab();
realmSettings.fillOutForm({ name: "test" }).save();
```

#### Clean code
Write locators in the PageObject layer and in object variables when possible to avoid hardcoding.
Set type of element at the end of the variable name (e.g. saveBtn, nameInput, userDrpDwn…)
Avoid adding “click” to name methods, methods should be named as an action “goToX”, “openX”, “createX”, etc.
Consistent naming (e.g goToTab methods, somewhere named goToTab, other goToXTab)


### Test structure

We have `keycloakBefore` that will navigate to the main page, control request errors and wait for the load to finish.
You can then have multiple test or create a new `describe` block to setup some data for your test.

```typescript
describe("Realm roles test", () => {
  before(() => {
    keycloakBefore();
    loginPage.logIn();
  });

  beforeEach(() => {
    sidebarPage.goToRealmRoles();
  });
```

Example of `describe` to setup data for that test:

```typescript
import adminClient from "../support/util/AdminClient";

describe("Edit role details", () => {
  before(() => {
    adminClient.createRealmRole({
      name: editRoleName,
      description,
    });
  });

  after(() => {
    adminClient.deleteRealmRole(editRoleName);
  });

  it("Should edit realm role details", () => {
      // ...
```

### Waiting for results

Sometimes you will get the following error:
> Cypress failed because the element has been detached from the DOM

This is because the DOM has been updated in between selectors. You can remedy this by waiting on REST calls. 
You can see the rest calls in the cypress IDE.

```typescript
  cy.intercept("/admin/realms/master/").as("search");
  
  // ... pressing a button that will perform the search
  
  cy.wait(["@search"]); // wait for the call named search
```

If there were no calls and you still get this error, try using `{force: true}`, but try not to use it everywhere. For example, there could be an unexpected open modal blocking the element, so even the user wouldn’t be able to use that element.

### Some more reading:

* [Moises document](https://docs.google.com/document/d/11sm1IpEvVLHO59JEVmwgNOUD0zoP4YMvInIU4v5iVNk/edit)
* [Cypress blog do not get too detached](https://www.cypress.io/blog/2020/07/22/do-not-get-too-detached/)
* [See the clients_test.spec as an example](./cypress/e2e/clients_test.spec.ts)