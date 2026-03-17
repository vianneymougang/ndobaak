import { ComponentFixture, TestBed } from '@angular/core/testing';

import { BookingConfirmDialog } from './booking-confirm-dialog';

describe('BookingConfirmDialog', () => {
  let component: BookingConfirmDialog;
  let fixture: ComponentFixture<BookingConfirmDialog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BookingConfirmDialog]
    })
    .compileComponents();

    fixture = TestBed.createComponent(BookingConfirmDialog);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
