import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PLATFORM_ID } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';

import { AdminListings } from './admin-listings';

describe('AdminListings', () => {
  let component: AdminListings;
  let fixture: ComponentFixture<AdminListings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminListings],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        MessageService,
        { provide: PLATFORM_ID, useValue: 'server' },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdminListings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
