import { ComponentFixture, TestBed } from '@angular/core/testing';

import { InspectionViewer } from './inspection-viewer';

describe('InspectionViewer', () => {
  let component: InspectionViewer;
  let fixture: ComponentFixture<InspectionViewer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [InspectionViewer]
    })
    .compileComponents();

    fixture = TestBed.createComponent(InspectionViewer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
