import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { NotificationBell } from './notification-bell';
import { NotificationService } from '../../../core/services/notification';
import { NotificationResponse } from '../../../core/models/notification-models';
import { PaginatedResponse } from '../../../core/models/booking.models';

describe('NotificationBell', () => {
  let component: NotificationBell;
  let fixture: ComponentFixture<NotificationBell>;
  let notificationServiceMock: {
    getUnreadCount: ReturnType<typeof vi.fn>;
    getNotifications: ReturnType<typeof vi.fn>;
    markAsRead: ReturnType<typeof vi.fn>;
    markAllAsRead: ReturnType<typeof vi.fn>;
  };

  const mockNotification: NotificationResponse = {
    id: 1,
    type: 'BOOKING_CREATED',
    title: 'Nouvelle réservation',
    message: 'Un étudiant a réservé votre studio.',
    read: false,
    referenceId: 10,
    referenceType: 'BOOKING',
    createdAt: '2026-03-19T10:00:00',
  };

  const mockNotification2: NotificationResponse = {
    id: 2,
    type: 'PAYMENT_RECEIVED',
    title: 'Paiement reçu',
    message: 'Vous avez reçu un paiement.',
    read: false,
    referenceId: 20,
    referenceType: 'PAYMENT',
    createdAt: '2026-03-19T09:00:00',
  };

  const mockNotificationPage: PaginatedResponse<NotificationResponse> = {
    content: [mockNotification, mockNotification2],
    page: 0,
    size: 5,
    totalElements: 2,
    totalPages: 1,
    hasNext: false,
  };

  beforeEach(async () => {
    vi.useFakeTimers();

    notificationServiceMock = {
      getUnreadCount: vi.fn().mockReturnValue(of(3)),
      getNotifications: vi.fn().mockReturnValue(of(mockNotificationPage)),
      markAsRead: vi.fn().mockReturnValue(of(undefined)),
      markAllAsRead: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.configureTestingModule({
      imports: [NotificationBell],
      providers: [
        { provide: NotificationService, useValue: notificationServiceMock },
      ],
    })
      .overrideComponent(NotificationBell, {
        set: {
          imports: [],
          schemas: [NO_ERRORS_SCHEMA],
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(NotificationBell);
    component = fixture.componentInstance;
    fixture.detectChanges();
    vi.advanceTimersByTime(0);
  });

  afterEach(() => {
    vi.useRealTimers();
    component.ngOnDestroy();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should poll unread count on init', () => {
    expect(notificationServiceMock.getUnreadCount).toHaveBeenCalled();
    expect((component as any).unreadCount()).toBe(3);
  });

  it('should load notifications on popover show', () => {
    (component as any).onPopoverShow();

    expect(notificationServiceMock.getNotifications).toHaveBeenCalledWith(0, 5);
    expect((component as any).notifications().length).toBe(2);
  });

  it('should mark single notification as read', () => {
    (component as any).onPopoverShow();

    const event = new Event('click');
    vi.spyOn(event, 'stopPropagation');

    (component as any).markAsRead(mockNotification, event);

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(notificationServiceMock.markAsRead).toHaveBeenCalledWith(1);

    const updated = (component as any).notifications().find((n: NotificationResponse) => n.id === 1);
    expect(updated.read).toBe(true);
    expect((component as any).unreadCount()).toBe(2);
  });

  it('should not call markAsRead for already-read notification', () => {
    const readNotification: NotificationResponse = { ...mockNotification, read: true };
    const event = new Event('click');

    (component as any).markAsRead(readNotification, event);

    expect(notificationServiceMock.markAsRead).not.toHaveBeenCalled();
  });

  it('should mark all notifications as read', () => {
    (component as any).onPopoverShow();
    (component as any).markAllAsRead();

    expect(notificationServiceMock.markAllAsRead).toHaveBeenCalled();
    expect((component as any).unreadCount()).toBe(0);

    const allRead = (component as any).notifications().every((n: NotificationResponse) => n.read);
    expect(allRead).toBe(true);
  });

  it('should map notification types to correct icons', () => {
    expect((component as any).getIcon('BOOKING_CREATED')).toBe('pi-calendar-plus');
    expect((component as any).getIcon('BOOKING_CONFIRMED')).toBe('pi-check-circle');
    expect((component as any).getIcon('BOOKING_CANCELLED')).toBe('pi-times-circle');
    expect((component as any).getIcon('PAYMENT_RECEIVED')).toBe('pi-wallet');
    expect((component as any).getIcon('PAYMENT_FAILED')).toBe('pi-exclamation-triangle');
    expect((component as any).getIcon('PAYMENT_REMINDER')).toBe('pi-clock');
    expect((component as any).getIcon('REVIEW_POSTED')).toBe('pi-star');
    expect((component as any).getIcon('LISTING_APPROVED')).toBe('pi-verified');
    expect((component as any).getIcon('LISTING_REJECTED')).toBe('pi-ban');
    expect((component as any).getIcon('UNKNOWN_TYPE')).toBe('pi-bell');
  });

  it('should map reference types to correct routes', () => {
    expect((component as any).getRoute({ referenceId: 10, referenceType: 'BOOKING' })).toBe('/my-bookings');
    expect((component as any).getRoute({ referenceId: 5, referenceType: 'LISTING' })).toBe('/listings/5');
    expect((component as any).getRoute({ referenceId: 20, referenceType: 'PAYMENT' })).toBe('/transactions');
    expect((component as any).getRoute({ referenceId: 3, referenceType: 'REVIEW' })).toBe('/listings/3');
    expect((component as any).getRoute({ referenceId: null, referenceType: null })).toBeNull();
    expect((component as any).getRoute({ referenceId: 1, referenceType: 'UNKNOWN' })).toBeNull();
  });
});
