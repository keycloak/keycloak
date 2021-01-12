export default class ListingPage {

    constructor() {
        this.searchInput = '.pf-c-toolbar__item [type="search"]';
        this.itemsRows = '.pf-c-page__main-section tbody > tr';
        this.itemRowDrpDwn = '.pf-c-dropdown > button';
        this.exportBtn = '[role="menuitem"]:nth-child(1)';
        this.deleteBtn = '[role="menuitem"]:nth-child(2)';
        this.searchBtn = '.pf-c-page__main .pf-c-toolbar__content-section button.pf-m-control';
        this.createBtn = '.pf-c-page__main .pf-c-toolbar__content-section button.pf-m-primary';
        this.importBtn = '.pf-c-page__main .pf-c-toolbar__content-section button.pf-m-link';
    }

    goToCreateItem() {
        cy.get(this.createBtn).click();

        return this;
    }

    goToImportItem() {
        cy.get(this.importBtn).click();

        return this;
    }

    searchItem(searchValue) {
        cy.get(this.searchInput).type(searchValue);
        cy.get(this.searchBtn).click();

        return this;
    }

    itemExist(itemName, exist = true) {
        cy.get(this.itemsRows).contains(itemName).should((!exist ? 'not.': '') + 'exist')

        return this;
    }

    goToItemDetails(itemName) {
        cy.get(this.itemsRows).contains(itemName).click();

        return this;
    }

    deleteItem(itemName) {
        cy.get(this.itemsRows).contains(itemName).parentsUntil('tbody').find(this.itemRowDrpDwn).click();
        cy.get(this.itemsRows).contains('Delete').click();

        return this;
    }

    exportItem(itemName) {
        cy.get(this.itemsRows).contains(itemName).parentsUntil('tbody').find(this.itemRowDrpDwn).click();
        cy.get(this.itemsRows).contains('Export').click();

        return this;
    }
}