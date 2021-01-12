import LoginPage from '../support/pages/LoginPage.js'
import HeaderPage from '../support/pages/admin_console/HeaderPage.js'
import ListingPage from '../support/pages/admin_console/ListingPage.js'
import SidebarPage from '../support/pages/admin_console/SidebarPage.js'
import CreateClientScopePage from '../support/pages/admin_console/manage/client_scopes/CreateClientScopePage.js'

describe('Client Scopes test', function () {

    const itemId = 'client_scope_1';
    const loginPage = new LoginPage();
    const headerPage = new HeaderPage();
    const sidebarPage = new SidebarPage();
    const listingPage = new ListingPage();
    const createClientScopePage = new CreateClientScopePage();
  
    describe('Client Scope creation', function () {
      beforeEach(function () {
        cy.visit('')
      })

      it('should fail creating client scope', function () {
          loginPage.logIn();

          sidebarPage.goToClientScopes();

          listingPage.goToCreateItem();

          createClientScopePage
              .save()
              .checkClientNameRequiredMessage();

          createClientScopePage
              .fillClientScopeData('address')
              .save()
              .checkClientNameRequiredMessage(false);
          
          // The error should inform about duplicated name/id
          headerPage.checkNotificationMessage('Could not create client scope: \'Error: Request failed with status code 409\'');
      });

      it('should create client scope', function () {
          loginPage.logIn();

          sidebarPage.goToClientScopes();

          listingPage
              .itemExist(itemId, false)
              .goToCreateItem();

          createClientScopePage
              .fillClientScopeData(itemId)
              .save();

          headerPage.checkNotificationMessage('Client scope created');

          sidebarPage.goToClientScopes();
            
          listingPage
              .itemExist(itemId)
              .searchItem(itemId)
              .itemExist(itemId);
      });
    })

    describe('Client scope elimination', function () {
      beforeEach(function () {
        cy.visit('')
      })
      
      it('should delete client scope', function () {
          loginPage.logIn();

          sidebarPage.goToClientScopes();

          listingPage
              .itemExist(itemId)
              .deleteItem(itemId); // There should be a confirmation pop-up

          headerPage.checkNotificationMessage('The client scope has been deleted');

          listingPage
              .itemExist(itemId, false);
      });
    })
  })