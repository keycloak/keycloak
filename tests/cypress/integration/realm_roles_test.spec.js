import LoginPage from '../support/pages/LoginPage.js'
import HeaderPage from '../support/pages/admin_console/HeaderPage.js'
import ListingPage from '../support/pages/admin_console/ListingPage.js'
import SidebarPage from '../support/pages/admin_console/SidebarPage.js'
import CreateRealmRolePage from '../support/pages/admin_console/manage/realm_roles/CreateRealmRolePage.js'

describe('Realm roles test', function () {

    const itemId = 'realm_role_1';
    const loginPage = new LoginPage();
    const headerPage = new HeaderPage();
    const sidebarPage = new SidebarPage();
    const listingPage = new ListingPage();
    const createRealmRolePage = new CreateRealmRolePage();
  
    describe('Realm roles creation', function () {
      beforeEach(function () {
        cy.visit('')
      })

      it('should fail creating realm role', function () {
        loginPage.logIn();

        sidebarPage.goToRealmRoles();

        listingPage.goToCreateItem();

        createRealmRolePage
            .save()
            .checkRealmRoleNameRequiredMessage();
            
        createRealmRolePage
            .fillRealmRoleData('admin')
            .save();
              
        // The error should inform about duplicated name/id (THIS MESSAGE DOES NOT HAVE QUOTES AS THE OTHERS)
        headerPage.checkNotificationMessage('Could not create role: Role with name admin already exists');
      });

      it('should create realm role', function () {
        loginPage.logIn();

        sidebarPage.goToRealmRoles();

        listingPage
            .itemExist(itemId, false)
            .goToCreateItem();

        createRealmRolePage
            .fillRealmRoleData(itemId)
            .save();

        headerPage.checkNotificationMessage('Role created');

        sidebarPage.goToRealmRoles();
        
        listingPage
            .itemExist(itemId)
            .searchItem(itemId)
            .itemExist(itemId);
      });
    })

    describe('Realm roles elimination', function () {
      beforeEach(function () {
        cy.visit('')
      })
      
      it('should delete realm role', function () {
        loginPage.logIn();

        sidebarPage.goToRealmRoles();

        listingPage
            .itemExist(itemId)
            .deleteItem(itemId);

        headerPage
            .checkModalTitle('Delete role?')
            .confirmModal();

        headerPage.checkNotificationMessage('The role has been deleted');

        listingPage
            .itemExist(itemId, false);
      });
    })
  })