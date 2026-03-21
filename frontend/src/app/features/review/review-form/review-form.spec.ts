import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { ReviewForm } from './review-form';
import { ReviewService } from '../../../core/services/review';
import { ReviewResponse } from '../../../core/models/review.models';

describe('ReviewForm', () => {
  let component: ReviewForm;
  let fixture: ComponentFixture<ReviewForm>;
  let reviewServiceMock: { createReview: ReturnType<typeof vi.fn> };
  let messageServiceMock: { add: ReturnType<typeof vi.fn> };

  const mockReview: ReviewResponse = {
    id: 1,
    studentName: 'Paul Biya',
    studentAvatar: null,
    rating: 4,
    comment: 'Great place to live near campus.',
    photoUrls: null,
    status: 'APPROVED',
    createdAt: '2026-03-19',
  };

  beforeEach(async () => {
    reviewServiceMock = { createReview: vi.fn() };
    messageServiceMock = { add: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [ReviewForm],
      providers: [
        { provide: ReviewService, useValue: reviewServiceMock },
        { provide: MessageService, useValue: messageServiceMock },
      ],
    })
      .overrideComponent(ReviewForm, {
        set: {
          imports: [ReactiveFormsModule],
          schemas: [NO_ERRORS_SCHEMA],
          template: '',
        },
      })
      .compileComponents();

    fixture = TestBed.createComponent(ReviewForm);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('listingId', 1);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should require rating', () => {
    const ratingCtrl = (component as any).form.controls.rating;
    ratingCtrl.setValue(null);
    expect(ratingCtrl.valid).toBe(false);
    expect(ratingCtrl.errors?.['required']).toBeTruthy();
  });

  it('should validate rating min value', () => {
    const ratingCtrl = (component as any).form.controls.rating;
    ratingCtrl.setValue(0);
    expect(ratingCtrl.valid).toBe(false);
    expect(ratingCtrl.errors?.['min']).toBeTruthy();
  });

  it('should validate rating max value', () => {
    const ratingCtrl = (component as any).form.controls.rating;
    ratingCtrl.setValue(6);
    expect(ratingCtrl.valid).toBe(false);
    expect(ratingCtrl.errors?.['max']).toBeTruthy();
  });

  it('should accept valid rating', () => {
    const ratingCtrl = (component as any).form.controls.rating;
    ratingCtrl.setValue(4);
    expect(ratingCtrl.valid).toBe(true);
  });

  it('should reject more than 3 photos', () => {
    (component as any).selectedPhotos.set([
      new File(['a'], 'a.jpg'),
      new File(['b'], 'b.jpg'),
    ]);

    const mockInput = {
      files: [new File(['c'], 'c.jpg'), new File(['d'], 'd.jpg')],
      value: '',
    };

    const mockEvent = { target: mockInput } as unknown as Event;

    (component as any).onFilesSelected(mockEvent);

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'warn' }),
    );
    expect((component as any).selectedPhotos().length).toBe(2);
  });

  it('should not submit with invalid form', () => {
    (component as any).form.controls.rating.setValue(null);
    (component as any).onSubmit();
    expect(reviewServiceMock.createReview).not.toHaveBeenCalled();
  });

  it('should submit valid review', () => {
    reviewServiceMock.createReview.mockReturnValue(of(mockReview));

    (component as any).form.controls.rating.setValue(4);
    (component as any).form.controls.comment.setValue('Great place to live near campus.');

    (component as any).onSubmit();

    expect(reviewServiceMock.createReview).toHaveBeenCalledWith(
      expect.objectContaining({ listingId: 1, rating: 4 }),
      undefined,
    );
  });

  it('should show success toast on submit success', () => {
    reviewServiceMock.createReview.mockReturnValue(of(mockReview));

    (component as any).form.controls.rating.setValue(4);
    (component as any).onSubmit();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success' }),
    );
  });

  it('should reset form after successful submit', () => {
    reviewServiceMock.createReview.mockReturnValue(of(mockReview));

    (component as any).form.controls.rating.setValue(4);
    (component as any).form.controls.comment.setValue('Nice place');

    (component as any).onSubmit();

    expect((component as any).form.controls.rating.value).toBeNull();
    expect((component as any).selectedPhotos().length).toBe(0);
    expect((component as any).photoPreviews().length).toBe(0);
  });

  it('should emit reviewCreated after successful submit', () => {
    reviewServiceMock.createReview.mockReturnValue(of(mockReview));
    const emitSpy = vi.spyOn(component.reviewCreated, 'emit');

    (component as any).form.controls.rating.setValue(4);
    (component as any).onSubmit();

    expect(emitSpy).toHaveBeenCalledWith(mockReview);
  });

  it('should show error toast on submit failure', () => {
    reviewServiceMock.createReview.mockReturnValue(
      throwError(() => new Error('Server error')),
    );

    (component as any).form.controls.rating.setValue(4);
    (component as any).onSubmit();

    expect(messageServiceMock.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
    expect((component as any).submitting()).toBe(false);
  });
});
