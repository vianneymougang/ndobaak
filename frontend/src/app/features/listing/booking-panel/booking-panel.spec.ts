import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { Router } from '@angular/router';
import { BookingPanel } from './booking-panel';
import { AuthService } from '../../../core/services/auth';

describe('BookingPanel', () => {
  let component: BookingPanel;
  let fixture: ComponentFixture<BookingPanel>;
  let routerMock: { navigate: ReturnType<typeof vi.fn> };
  let authServiceMock: {
    user: ReturnType<typeof vi.fn>;
    isAuthenticated: ReturnType<typeof vi.fn>;
  };

  const mockListing = {
    id: 1,
    title: 'Studio Eyang',
    description: 'Un studio',
    address: 'Eyang',
    latitude: 3.8,
    longitude: 11.5,
    monthlyPrice: 50000,
    deposit: 100000,
    bedrooms: 1,
    area: 25,
    furnished: true,
    status: 'AVAILABLE',
    photos: [],
    landlord: { id: 10, firstName: 'Jean', lastName: 'Dupont' },
    createdAt: '2026-01-01',
  };

  beforeEach(async () => {
    routerMock = { navigate: vi.fn() };
    authServiceMock = {
      user: vi.fn().mockReturnValue({ id: 99 }),
      isAuthenticated: vi.fn().mockReturnValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [BookingPanel],
      providers: [
        { provide: Router, useValue: routerMock },
        { provide: AuthService, useValue: authServiceMock },
      ],
    })
      .overrideComponent(BookingPanel, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(BookingPanel);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('listing', mockListing);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute total amount', () => {
    expect((component as any).totalAmount()).toBe(500000);
  });

  it('should compute endDate when startDate is set', () => {
    (component as any).startDate.set(new Date(2026, 3, 1));
    expect((component as any).endDate()).toBeTruthy();
    expect((component as any).endDate()!.getMonth()).toBe(1);
  });

  it('should return null endDate when startDate is null', () => {
    expect((component as any).endDate()).toBeNull();
  });

  it('should detect owner', () => {
    authServiceMock.user.mockReturnValue({ id: 10 });
    fixture.detectChanges();
    expect((component as any).isOwner()).toBe(true);
  });

  it('should not detect owner when different user', () => {
    expect((component as any).isOwner()).toBe(false);
  });

  it('should redirect to login when unauthenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);
    fixture.detectChanges();
    (component as any).onReserveClick();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
  });

  it('should open confirm dialog when authenticated', () => {
    (component as any).onReserveClick();
    expect((component as any).confirmDialogVisible()).toBe(true);
  });
});
