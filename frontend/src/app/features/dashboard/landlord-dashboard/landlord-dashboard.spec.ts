import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';

import { LandlordDashboard } from './landlord-dashboard';

describe('LandlordDashboard', () => {
  let component: LandlordDashboard;
  let fixture: ComponentFixture<LandlordDashboard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandlordDashboard],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        MessageService,
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LandlordDashboard);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
