import { Angular2ProductAppPage } from './app.po';

describe('angular2-product-app App', () => {
  let page: Angular2ProductAppPage;

  beforeEach(() => {
    page = new Angular2ProductAppPage();
  });

  it('should display message saying Angular2 Product', () => {
    page.navigateTo();
    expect(page.getParagraphText()).toEqual('Angular2 Product');
  });

  it('should load Products', () => {
    page.navigateTo();
    const products = page.loadProducts();
    ['iphone', 'ipad', 'ipod'].forEach(e => expect(products).toContain(e));
  });
});
