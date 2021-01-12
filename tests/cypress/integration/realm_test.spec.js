import LoginPage from '../support/pages/LoginPage.js'
import SidebarPage from '../support/pages/admin_console/SidebarPage.js'
import CreateRealmPage from '../support/pages/admin_console/CreateRealmPage.js'
import HeaderPage from '../support/pages/admin_console/HeaderPage.js'

describe('Realms test', function () {

    const loginPage = new LoginPage();
    const sidebarPage = new SidebarPage();
    const createRealmPage = new CreateRealmPage();
    const headerPage = new HeaderPage();
  
    describe('Realm creation', function () {
      beforeEach(function () {
        cy.visit('')
      })

      it('should fail creating Master realm', function () {
          loginPage.logIn();

          sidebarPage.goToCreateRealm();
          createRealmPage
              .fillRealmName('master')
              .createRealm();

          headerPage.checkNotificationMessage('Error: Request failed with status code 409');
      });

      it('should create Test realm', function () {
          loginPage.logIn();

          sidebarPage.goToCreateRealm();
          createRealmPage
              .fillRealmName('Test')
              .createRealm();
          
          headerPage.checkNotificationMessage('Realm created');
      });

      it('should change to Test realm', function () {
          loginPage.logIn();

          sidebarPage.getCurrentRealm().should('eq', 'Master');

          sidebarPage
              .goToRealm('Test')
              .getCurrentRealm().should('eq', 'Test');
      });
    })
  })