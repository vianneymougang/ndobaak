import { ComponentFixture, TestBed } from '@angular/core/testing';

import { KeyHandover } from './key-handover';

describe('KeyHandover', () => {
  let component: KeyHandover;
  let fixture: ComponentFixture<KeyHandover>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KeyHandover]
    })
    .compileComponents();

    fixture = TestBed.createComponent(KeyHandover);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
