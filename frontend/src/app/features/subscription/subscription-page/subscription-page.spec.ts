import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';

import { SubscriptionPage } from './subscription-page';

describe('SubscriptionPage', () => {
  let component: SubscriptionPage;
  let fixture: ComponentFixture<SubscriptionPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SubscriptionPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        MessageService,
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SubscriptionPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
