import ModalUtils from "../../../../../../util/ModalUtils";
import Masthead from "../../../../Masthead";

const modalUtils = new ModalUtils();
const masthead = new Masthead();

export default class IdentityProviderLinksTab {
  #linkedProvidersSection = ".kc-linked-idps";
  #availableProvidersSection = ".kc-available-idps";
  #linkAccountBtn = ".pf-v5-c-button.pf-m-link";
  #linkAccountModalIdentityProviderInput = "idpNameInput";
  #linkAccountModalUserIdInput = "userId";
  #linkAccountModalUsernameInput = "userName";

  public clickLinkAccount(idpName: string) {
    cy.get(this.#availableProvidersSection + " tr")
      .contains(idpName)
      .parent()
      .find(this.#linkAccountBtn)
      .click();

    return this;
  }

  public clickUnlinkAccount(idpName: string) {
    cy.get(this.#linkedProvidersSection + " tr")
      .contains(idpName)
      .parent()
      .parent()
      .find(this.#linkAccountBtn)
      .click();

    return this;
  }

  public typeLinkAccountModalUserId(userId: string) {
    cy.findByTestId(this.#linkAccountModalUserIdInput).type(userId);

    return this;
  }

  public typeLinkAccountModalUsername(username: string) {
    cy.findByTestId(this.#linkAccountModalUsernameInput).type(username);

    return this;
  }

  public clickLinkAccountModalLinkBtn() {
    modalUtils.confirmModal();
    return this;
  }

  public clickUnlinkAccountModalUnlinkBtn() {
    modalUtils.confirmModal();
    return this;
  }

  public assertNoIdentityProvidersLinkedMessageExist(exist: boolean) {
    cy.get(this.#linkedProvidersSection).should(
      (exist ? "" : "not.") + "contain.text",
      "No identity providers linked.",
    );

    return this;
  }

  public assertNoAvailableIdentityProvidersMessageExist(exist: boolean) {
    cy.get(this.#availableProvidersSection).should(
      (exist ? "" : "not.") + "contain.text",
      "No available identity providers.",
    );

    return this;
  }

  public assertLinkAccountModalTitleEqual(idpName: string) {
    modalUtils.assertModalTitleEqual(`Link account to ${idpName}`);

    return this;
  }

  public assertUnLinkAccountModalTitleEqual(idpName: string) {
    modalUtils.assertModalTitleEqual(`Unlink account from ${idpName}?`);

    return this;
  }

  public assertLinkAccountModalIdentityProviderInputEqual(idpName: string) {
    cy.findByTestId(this.#linkAccountModalIdentityProviderInput).should(
      "have.value",
      idpName,
    );

    return this;
  }

  public assertNotificationIdentityProviderLinked() {
    masthead.checkNotificationMessage("Identity provider has been linked");

    return this;
  }

  public assertNotificationAlreadyLinkedError() {
    masthead.checkNotificationMessage(
      "Could not link identity provider User is already linked with provider",
    );

    return this;
  }

  public assertNotificationPoviderLinkRemoved() {
    masthead.checkNotificationMessage("The provider link has been removed");

    return this;
  }

  public assertLinkedIdentityProvidersItemsEqual(count: number) {
    if (count > 0) {
      cy.get(this.#linkedProvidersSection + " tbody")
        .find("tr")
        .should("have.length", count);
    } else {
      cy.get(this.#linkedProvidersSection + " tbody").should("not.exist");
    }

    return this;
  }

  public assertAvailableIdentityProvidersItemsEqual(count: number) {
    if (count > 0) {
      cy.get(this.#availableProvidersSection + " tbody")
        .find("tr")
        .should("have.length", count);
    } else {
      cy.get(this.#availableProvidersSection + " tbody").should("not.exist");
    }
    return this;
  }

  public assertLinkedIdentityProviderExist(idpName: string, exist: boolean) {
    cy.get(this.#linkedProvidersSection).should(
      (exist ? "" : "not.") + "contain.text",
      idpName,
    );

    return this;
  }

  public assertAvailableIdentityProviderExist(idpName: string, exist: boolean) {
    cy.get(this.#availableProvidersSection).should(
      (exist ? "" : "not.") + "contain.text",
      idpName,
    );

    return this;
  }
}
