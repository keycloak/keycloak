import { Angular2ProductAppPage } from './app.po';

describe('angular2-product-app App', () => {
  let page: Angular2ProductAppPage;

  beforeEach(() => {
    page = new Angular2ProductAppPage();
  });

  it('should display message saying app works', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('app works!');
  });
});
