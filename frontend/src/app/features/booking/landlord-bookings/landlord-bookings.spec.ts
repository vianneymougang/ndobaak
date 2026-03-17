import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LandlordBookings } from './landlord-bookings';

describe('LandlordBookings', () => {
  let component: LandlordBookings;
  let fixture: ComponentFixture<LandlordBookings>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LandlordBookings]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LandlordBookings);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
