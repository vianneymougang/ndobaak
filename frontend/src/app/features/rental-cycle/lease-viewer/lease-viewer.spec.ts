import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LeaseViewer } from './lease-viewer';

describe('LeaseViewer', () => {
  let component: LeaseViewer;
  let fixture: ComponentFixture<LeaseViewer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaseViewer]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LeaseViewer);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
