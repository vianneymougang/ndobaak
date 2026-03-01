import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { ListingCard } from './listing-card';
import { ListingSearchResponse } from '../../../core/models/listing.models';

function buildListing(overrides: Partial<ListingSearchResponse> = {}): ListingSearchResponse {
  return {
    id: 1,
    title: 'Chambre meublée à Melen',
    thumbnailUrl: undefined,
    monthlyPrice: 35000,
    roomType: 'SINGLE_ROOM',
    neighborhood: 'Melen',
    distanceToCampus: 1.2,
    avgRating: undefined,
    reviewCount: 0,
    amenities: [],
    verified: false,
    latitude: 3.848,
    longitude: 11.502,
    availableRooms: 2,
    ...overrides,
  };
}

describe('ListingCard', () => {
  let component: ListingCard;
  let fixture: ComponentFixture<ListingCard>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ListingCard],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(ListingCard);
    component = fixture.componentInstance;
  });

  it('creates the component', () => {
    fixture.componentRef.setInput('listing', buildListing());
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('displays the listing title', () => {
    fixture.componentRef.setInput('listing', buildListing({ title: 'Studio proche campus' }));
    fixture.detectChanges();
    const titleEl = fixture.debugElement.query(By.css('.card-title'));
    expect(titleEl.nativeElement.textContent.trim()).toBe('Studio proche campus');
  });

  it('displays formatted price with FCFA', () => {
    fixture.componentRef.setInput('listing', buildListing({ monthlyPrice: 35000 }));
    fixture.detectChanges();
    const priceEl = fixture.debugElement.query(By.css('.card-price'));
    expect(priceEl.nativeElement.textContent).toContain('FCFA');
    expect(priceEl.nativeElement.textContent).toContain('35');
  });

  it('displays SINGLE_ROOM label correctly', () => {
    fixture.componentRef.setInput('listing', buildListing({ roomType: 'SINGLE_ROOM' }));
    fixture.detectChanges();
    const typeEl = fixture.debugElement.query(By.css('.type-badge'));
    expect(typeEl.nativeElement.textContent.trim()).toBe('Chambre simple');
  });

  it('displays STUDIO label correctly', () => {
    fixture.componentRef.setInput('listing', buildListing({ roomType: 'STUDIO' }));
    fixture.detectChanges();
    const typeEl = fixture.debugElement.query(By.css('.type-badge'));
    expect(typeEl.nativeElement.textContent.trim()).toBe('Studio');
  });

  it('shows verified badge when verified', () => {
    fixture.componentRef.setInput('listing', buildListing({ verified: true }));
    fixture.detectChanges();
    const badge = fixture.debugElement.query(By.css('.verified-badge'));
    expect(badge).toBeTruthy();
  });

  it('hides verified badge when not verified', () => {
    fixture.componentRef.setInput('listing', buildListing({ verified: false }));
    fixture.detectChanges();
    const badge = fixture.debugElement.query(By.css('.verified-badge'));
    expect(badge).toBeNull();
  });

  it('shows no-image placeholder when no thumbnail', () => {
    fixture.componentRef.setInput('listing', buildListing({ thumbnailUrl: undefined }));
    fixture.detectChanges();
    const noImage = fixture.debugElement.query(By.css('.no-image'));
    expect(noImage).toBeTruthy();
  });

  it('shows image element when thumbnail is provided', () => {
    fixture.componentRef.setInput('listing', buildListing({ thumbnailUrl: 'http://example.com/img.jpg' }));
    fixture.detectChanges();
    const img = fixture.debugElement.query(By.css('img'));
    expect(img).toBeTruthy();
    expect(img.nativeElement.src).toBe('http://example.com/img.jpg');
  });

  it('displays distance label when distanceToCampus is set', () => {
    fixture.componentRef.setInput('listing', buildListing({ distanceToCampus: 1.2, neighborhood: 'Melen' }));
    fixture.detectChanges();
    const locationEl = fixture.debugElement.query(By.css('.card-location'));
    expect(locationEl.nativeElement.textContent).toContain('1.2 km du campus');
  });

  it('displays amenities when present', () => {
    fixture.componentRef.setInput('listing', buildListing({
      amenities: ['WiFi', 'Running Water', 'Electricity'],
    }));
    fixture.detectChanges();
    const tags = fixture.debugElement.queryAll(By.css('.amenity-tag'));
    expect(tags.length).toBe(3);
  });

  it('shows overflow count when more than 3 amenities', () => {
    fixture.componentRef.setInput('listing', buildListing({
      amenities: ['WiFi', 'Running Water', 'Electricity', 'Parking', 'Kitchen'],
    }));
    fixture.detectChanges();
    const more = fixture.debugElement.query(By.css('.amenity-more'));
    expect(more).toBeTruthy();
    expect(more.nativeElement.textContent.trim()).toBe('+2');
  });

  it('displays available rooms count', () => {
    fixture.componentRef.setInput('listing', buildListing({ availableRooms: 3 }));
    fixture.detectChanges();
    const roomsEl = fixture.debugElement.query(By.css('.rooms-available'));
    expect(roomsEl.nativeElement.textContent).toContain('3');
    expect(roomsEl.nativeElement.textContent).toContain('places');
  });

  it('displays singular form when 1 available room', () => {
    fixture.componentRef.setInput('listing', buildListing({ availableRooms: 1 }));
    fixture.detectChanges();
    const roomsEl = fixture.debugElement.query(By.css('.rooms-available'));
    expect(roomsEl.nativeElement.textContent).toContain('1 place');
    expect(roomsEl.nativeElement.textContent).not.toContain('places');
  });
});
