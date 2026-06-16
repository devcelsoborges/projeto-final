import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class ProfileStateService {
  private editingSubject = new BehaviorSubject<boolean>(false);
  public editing$ = this.editingSubject.asObservable();

  constructor() {}

  setEditing(isEditing: boolean): void {
    this.editingSubject.next(isEditing);
  }

  getEditing(): boolean {
    return this.editingSubject.value;
  }

  toggleEditing(): void {
    this.setEditing(!this.editingSubject.value);
  }

  resetToView(): void {
    this.setEditing(false);
  }

  startEditing(): void {
    this.setEditing(true);
  }
}
