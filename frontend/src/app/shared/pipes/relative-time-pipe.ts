import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'relativeTime',
})
export class RelativeTimePipe implements PipeTransform {
  transform(value: string | Date | null | undefined): string {
    if (!value) return '';

    const now = new Date();
    const date = typeof value === 'string' ? new Date(value) : value;
    const diffMs = now.getTime() - date.getTime();
    const diffMin = Math.floor(diffMs / 60_000);
    const diffH = Math.floor(diffMs / 3_600_000);
    const diffD = Math.floor(diffMs / 86_400_000);

    if (diffMin < 1) return "à l'instant";
    if (diffMin < 60) return `il y a ${diffMin} min`;
    if (diffH < 24) return `il y a ${diffH}h`;
    if (diffD === 1) return 'hier';
    if (diffD < 7) return `il y a ${diffD} jours`;
    if (diffD < 30) return `il y a ${Math.floor(diffD / 7)} sem.`;

    return date.toLocaleDateString('fr-FR', {
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
  }
}
