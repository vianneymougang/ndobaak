import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';

import { AdminListingDetail } from './admin-listing-detail';

describe('AdminListingDetail', () => {
  let component: AdminListingDetail;
  let fixture: ComponentFixture<AdminListingDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminListingDetail],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        MessageService,
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminListingDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
