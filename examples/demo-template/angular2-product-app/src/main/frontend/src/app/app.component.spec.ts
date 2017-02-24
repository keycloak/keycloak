import { TestBed, async } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { KeycloakService } from './keycloak/keycloak.service';
import {
  HttpModule,
  XHRBackend,
  ResponseOptions,
  Response,
  RequestMethod
} from '@angular/http';
import {
  MockBackend,
  MockConnection
} from '@angular/http/testing/mock_backend';


describe('AppComponent', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpModule],
      providers: [
        {
          provide: XHRBackend,
          useClass: MockBackend
        },
        {
          provide: KeycloakService
        }
      ],
      declarations: [
        AppComponent
      ],
    });
    TestBed.compileComponents();
  });

  it('should create the app', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app).toBeTruthy();
  }));

  it(`should have as title 'Angular2 Product'`, async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.debugElement.componentInstance;
    expect(app.title).toEqual('Angular2 Product');
  }));

  it('should render title in a h1 tag', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const compiled = fixture.debugElement.nativeElement;
    expect(compiled.querySelector('h1').textContent).toContain('Angular2 Product');
  }));

  it('should render product list', async(() => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.componentInstance.products = ['iphone', 'ipad', 'ipod'];
    fixture.detectChanges();
    const compiled = fixture.debugElement.nativeElement;
    expect(compiled.querySelector('table thead tr th').textContent).toContain('Product Listing');
    expect(compiled.querySelectorAll('table tbody tr td')[0].textContent).toContain('iphone');
    expect(compiled.querySelectorAll('table tbody tr td')[1].textContent).toContain('ipad');
    expect(compiled.querySelectorAll('table tbody tr td')[2].textContent).toContain('ipod');
  }));
});
