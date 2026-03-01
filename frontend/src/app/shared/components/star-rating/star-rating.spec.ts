import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { StarRating } from './star-rating';

describe('StarRating', () => {
  let component: StarRating;
  let fixture: ComponentFixture<StarRating>;

  async function createComponent(rating: number): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [StarRating],
    }).compileComponents();

    fixture = TestBed.createComponent(StarRating);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('rating', rating);
    fixture.detectChanges();
  }

  it('creates the component', async () => {
    await createComponent(4);
    expect(component).toBeTruthy();
  });

  it('renders 5 star elements total', async () => {
    await createComponent(3);
    const stars = fixture.debugElement.queryAll(By.css('i.pi'));
    expect(stars.length).toBe(5);
  });

  it('renders 3 filled stars for rating 3', async () => {
    await createComponent(3);
    const filledStars = fixture.debugElement.queryAll(By.css('i.pi-star-fill'));
    expect(filledStars.length).toBe(3);
  });

  it('renders 2 empty stars for rating 3', async () => {
    await createComponent(3);
    const emptyStars = fixture.debugElement.queryAll(By.css('i.pi-star'));
    expect(emptyStars.length).toBe(2);
  });

  it('renders 5 filled stars for rating 5', async () => {
    await createComponent(5);
    const filledStars = fixture.debugElement.queryAll(By.css('i.pi-star-fill'));
    expect(filledStars.length).toBe(5);
  });

  it('renders 0 filled stars for rating 0', async () => {
    await createComponent(0);
    const filledStars = fixture.debugElement.queryAll(By.css('i.pi-star-fill'));
    expect(filledStars.length).toBe(0);
  });

  it('displays the rating value formatted to 1 decimal', async () => {
    await createComponent(4.5);
    const valueEl = fixture.debugElement.query(By.css('.rating-value'));
    expect(valueEl.nativeElement.textContent.trim()).toBe('4.5');
  });

  it('has accessible aria-label with rating value', async () => {
    await createComponent(3);
    const host = fixture.debugElement.query(By.css('.star-rating'));
    expect(host.nativeElement.getAttribute('aria-label')).toBe('3 out of 5 stars');
  });

  it('rounds rating for filled star count', async () => {
    await createComponent(3.4);
    const filledStars = fixture.debugElement.queryAll(By.css('i.pi-star-fill'));
    expect(filledStars.length).toBe(3);
  });

  it('rounds up rating for filled star count at 0.5', async () => {
    await createComponent(3.5);
    const filledStars = fixture.debugElement.queryAll(By.css('i.pi-star-fill'));
    expect(filledStars.length).toBe(4);
  });
});
