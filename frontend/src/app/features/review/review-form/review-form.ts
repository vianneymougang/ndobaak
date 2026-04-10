import {
  Component,
  ChangeDetectionStrategy,
  DestroyRef,
  input,
  output,
  signal,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RatingModule } from 'primeng/rating';
import { TextareaModule } from 'primeng/textarea';
import { ButtonModule } from 'primeng/button';
import { FloatLabelModule } from 'primeng/floatlabel';
import { MessageService } from 'primeng/api';
import { ReviewService } from '../../../core/services/review';
import { ReviewResponse } from '../../../core/models/review.models';

@Component({
  selector: 'app-review-form',
  imports: [
    ReactiveFormsModule,
    RatingModule,
    TextareaModule,
    ButtonModule,
    FloatLabelModule,
  ],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './review-form.html',
  styleUrl: './review-form.scss',
})
export class ReviewForm {
  listingId = input.required<number>();
  reviewCreated = output<ReviewResponse>();

  private fb = inject(FormBuilder);
  private reviewService = inject(ReviewService);
  private messageService = inject(MessageService);
  private destroyRef = inject(DestroyRef);

  protected submitting = signal(false);
  protected selectedPhotos = signal<File[]>([]);
  protected photoPreviews = signal<string[]>([]);

  protected form = this.fb.group({
    rating: [null as number | null, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.minLength(10), Validators.maxLength(1000)]],
  });

  protected onFilesSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files) return;

    const newFiles = Array.from(input.files);
    const currentFiles = this.selectedPhotos();

    if (currentFiles.length + newFiles.length > 3) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Limite atteinte',
        detail: 'Maximum 3 photos par avis.',
      });
      return;
    }

    const allFiles = [...currentFiles, ...newFiles];
    this.selectedPhotos.set(allFiles);

    const previews = allFiles.map((file) => URL.createObjectURL(file));
    this.photoPreviews().forEach((url) => URL.revokeObjectURL(url));
    this.photoPreviews.set(previews);

    input.value = '';
  }

  protected removePhoto(index: number): void {
    URL.revokeObjectURL(this.photoPreviews()[index]);

    this.selectedPhotos.update((files) => files.filter((_, i) => i !== index));
    this.photoPreviews.update((previews) => previews.filter((_, i) => i !== index));
  }

  protected onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);

    const request = {
      listingId: this.listingId(),
      rating: this.form.value.rating!,
      comment: this.form.value.comment || undefined,
    };

    const photos = this.selectedPhotos().length > 0 ? this.selectedPhotos() : undefined;

    this.reviewService.createReview(request, photos).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (review) => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Avis publié',
          detail: 'Merci pour votre avis !',
        });
        this.form.reset();
        this.photoPreviews().forEach((url) => URL.revokeObjectURL(url));
        this.selectedPhotos.set([]);
        this.photoPreviews.set([]);
        this.reviewCreated.emit(review);
      },
      error: () => {
        this.submitting.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: "Impossible de publier votre avis. Veuillez réessayer.",
        });
      },
    });
  }
}
