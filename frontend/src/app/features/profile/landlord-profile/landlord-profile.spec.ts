import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';

import { LandlordProfile } from './landlord-profile';

describe('LandlordProfile', () => {
  let component: LandlordProfile;
  let fixture: ComponentFixture<LandlordProfile>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandlordProfile],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        MessageService,
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LandlordProfile);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
