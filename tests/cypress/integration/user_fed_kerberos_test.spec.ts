import LoginPage from "../support/pages/LoginPage";
import SidebarPage from "../support/pages/admin_console/SidebarPage";
// import ListingPage from "../support/pages/admin_console/ListingPage";
import CreateKerberosProviderPage from "../support/pages/admin_console/manage/providers/CreateKerberosProviderPage";
// import Masthead from "../support/pages/admin_console/Masthead";
// import { wait } from "@testing-library/react";

const loginPage = new LoginPage();
// const masthead = new Masthead();
const sidebarPage = new SidebarPage();
// const listingPage = new ListingPage();
const providersPage = new CreateKerberosProviderPage();

// config info
const kerberosName = "my-kerberos";
const kerberosRealm = "my-realm";
const kerberosPrincipal = "my-principal";
const kerberosKeytab = "my-keytab";
// const kerberosSecondName = "my-kerberos-2";

const policy = "EVICT_WEEKLY"
const kerberosDay = "Tuesday";
const kerberosHour = "11";
const kerberosMinute = "45";
// const kerberosLifespan = "24";

describe('User Fed Kerberos test', () => {
    it('Kerberos provider creation from empty state', () => {
        cy.visit("");
        loginPage.logIn();

        // CREATE FROM EMPTY STATE CARD PAGE
        sidebarPage.goToUserFederation();
        cy.get('[data-cy=kerberos-card]').click();
        // cy.get('[data-cy=kerberos-name]').type("my-kerberos-provider");
        providersPage.fillKerberosRequiredData(kerberosName, kerberosRealm, kerberosPrincipal, kerberosKeytab);
        providersPage.save();
        // TODO verify save message


        // UPDATE
        sidebarPage.goToUserFederation();
        cy.get('[data-cy="keycloak-card-title"]').contains("my-kerberos").click();


        providersPage.selectCacheType(policy);
        // cy.get('[data-cy="kerberos-cache-policy"]').select(policy);
        providersPage.fillCachedData( kerberosHour, kerberosMinute)




        // TODO verify update message


        // CREATE FROM USER FED CARD PAGE




   
    })
    
    // Messages:
    // User federation provider successfully created
    // The user federation provider has been deleted.




    // it("should open kerberos empty settings page by clicking card", function () {

    // //     // listingPage.goToCreateItem();
    // // cy.get('[data-cy=kerberos-card]').click();
    // // cy.get('[data-cy=kerberos-name]').type("my-kerberos-provider");
    // //     // providersPage.fillKerberosRequiredData(kerberosName, kerberosRealm, kerberosPrincipal, kerberosKeytab);
  
    // //     // providersPage.save();
  
    // //     // The error should inform about duplicated name/id (THIS MESSAGE DOES NOT HAVE QUOTES AS THE OTHERS)
    // //     // masthead.checkNotificationMessage(
    // //     //   "Could not create role: Role with name admin already exists"
    // //     // );
    //    });
})