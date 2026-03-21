import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { KpiCard } from './kpi-card';

describe('KpiCard', () => {
  let component: KpiCard;
  let fixture: ComponentFixture<KpiCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [KpiCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(KpiCard);
    fixture.componentRef.setInput('icon', 'pi-home');
    fixture.componentRef.setInput('label', 'Test');
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
