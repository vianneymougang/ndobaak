import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminListingDetail } from './admin-listing-detail';

describe('AdminListingDetail', () => {
  let component: AdminListingDetail;
  let fixture: ComponentFixture<AdminListingDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdminListingDetail]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AdminListingDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
