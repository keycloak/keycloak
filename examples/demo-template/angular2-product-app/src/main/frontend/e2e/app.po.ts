import { browser, element, by } from 'protractor';

export class Angular2ProductAppPage {
  navigateTo() {
    browser.ignoreSynchronization = true;
    browser.get('/');
    browser.getCurrentUrl().then(url => {
      if (url.includes('/auth/realms/demo')) {
        element(by.id('username')).sendKeys('bburke@redhat.com');
        element(by.id('password')).sendKeys('password');
        element(by.id('kc-login')).click();
      }
      browser.ignoreSynchronization = false;

    });
  }

  getParagraphText() {
    return element(by.css('app-root h1')).getText();
  }

  loadProducts() {
    const click = element(by.id('reload-data')).click();
    browser.wait(click, 2000, 'Products should load within 2 seconds');
    return element.all(by.css('table.table td')).getText();
  }

}
