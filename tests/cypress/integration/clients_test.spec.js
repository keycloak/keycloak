import LoginPage from '../support/pages/LoginPage.js'
import HeaderPage from '../support/pages/admin_console/HeaderPage.js'
import ListingPage from '../support/pages/admin_console/ListingPage.js'
import SidebarPage from '../support/pages/admin_console/SidebarPage.js'
import CreateClientPage from '../support/pages/admin_console/manage/clients/CreateClientPage.js'

describe('Clients test', function () {

    const itemId = 'client_1';
    const loginPage = new LoginPage();
    const headerPage = new HeaderPage();
    const sidebarPage = new SidebarPage();
    const listingPage = new ListingPage();
    const createClientPage = new CreateClientPage();
  
    describe('Client creation', function () {
      beforeEach(function () {
        cy.visit('')
      })

      it('should fail creating client', function () {
          loginPage.logIn();

          sidebarPage.goToClients();

          listingPage.goToCreateItem();

          createClientPage
              .continue()
              .checkClientTypeRequiredMessage()
              .checkClientIdRequiredMessage();
            
          createClientPage
              .fillClientData(itemId)
              .continue()
              .checkClientTypeRequiredMessage()
              .checkClientIdRequiredMessage(false);
            
          createClientPage
              .fillClientData('')
              .selectClientType('openid-connect')
              .continue()
              .checkClientTypeRequiredMessage(false)
              .checkClientIdRequiredMessage();
            
          createClientPage
              .fillClientData('account')
              .continue()
              .continue();
              
          // The error should inform about duplicated name/id
          headerPage.checkNotificationMessage('Could not create client: \'Error: Request failed with status code 409\'');
      });

      it('should create client', function () {
          loginPage.logIn();

          sidebarPage.goToClients();

          listingPage
              .itemExist(itemId, false)
              .goToCreateItem();

          createClientPage
              .selectClientType('openid-connect')
              .fillClientData(itemId)
              .continue()
              .continue();

          headerPage.checkNotificationMessage('Client created successfully');

          sidebarPage.goToClients();
            
          listingPage
              .itemExist(itemId)
              .searchItem(itemId)
              .itemExist(itemId);
      });
    })

    describe('Client elimination', function () {
      beforeEach(function () {
        cy.visit('')
      })
      
      it('should delete client', function () {
          loginPage.logIn();

          sidebarPage.goToClients();

          listingPage
              .itemExist(itemId)
              .deleteItem(itemId); // There should be a confirmation pop-up

          headerPage.checkNotificationMessage('The client has been deleted');

          listingPage
              .itemExist(itemId, false);
      });
    })
  })