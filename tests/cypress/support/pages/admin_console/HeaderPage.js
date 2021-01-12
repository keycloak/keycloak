export default class HeaderPage {

    constructor() {
        this.menuBtn = '#nav-toggle';
        this.logoBtn = 'img[alt="Logo"]';
        this.helpBtn = '#help';

        this.userDrpDwn = '[id*="pf-dropdown-toggle-id"]';
        this.manageAccountBtn = '.pf-c-page__header-tools-item [role*="menu"] li:nth-child(1)';
        this.serverInfoBtn = '.pf-c-page__header-tools-item [role*="menu"] li:nth-child(2)';
        this.signOutBtn = '.pf-c-page__header-tools-item [role*="menu"] li:nth-child(4)';

        this.notificationList = '.pf-c-alert-group.pf-m-toast';

        this.modalTitle = '.pf-c-modal-box .pf-c-modal-box__title-text';
        this.modalMessage = '.pf-c-modal-box .pf-c-modal-box__body';

        this.confirmModalBtn = '#modal-confirm';
        this.cancelModalBtn = '#modal-cancel';
        this.closeModalBtn = '.pf-c-modal-box .pf-m-plain';
    }

    goToAdminConsole() {
        cy.visit('');

        return this;
    }

    goToManageAccount() {
        cy.get(this.userDrpDwn).click();
        cy.get(this.manageAccountBtn).click();

        return this;
    }

    goToServerInfo() {
        cy.get(this.userDrpDwn).click();
        cy.get(this.serverInfoBtn).click();

        return this;
    }

    signOut() {
        cy.get(this.userDrpDwn).click();
        cy.get(this.signOutBtn).click();

        return this;
    }

    isAdminConsole() {
        cy.get(this.logoBtn).should('exist');
        cy.get(this.userDrpDwn).should('exist');

        return this;
    }

    checkNotificationMessage(message) {
        cy.contains(message).should('exist');

        return this;
    }

    confirmModal() {
        cy.get(this.confirmModalBtn).click();

        return this;
    }

    cancelModal() {
        cy.get(this.cancelModalBtn).click();

        return this;
    }

    closeModal() {
        cy.get(this.closeModalBtn).click();

        return this;
    }

    checkModalTitle(title) {
        cy.get(this.modalTitle).invoke('text').should('eq', title);

        return this;
    }

    checkModalMessage(message) {
        cy.get(this.modalMessage).invoke('text').should('eq', message);

        return this;
    }
}