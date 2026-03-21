import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';

import { NotificationPreferences } from './notification-preferences';

describe('NotificationPreferences', () => {
  let component: NotificationPreferences;
  let fixture: ComponentFixture<NotificationPreferences>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationPreferences],
      providers: [
        provideHttpClient(),
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NotificationPreferences);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
