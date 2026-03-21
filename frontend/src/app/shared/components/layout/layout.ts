import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { Navbar } from '../navbar/navbar';
import { Footer } from '../footer/footer';
import { BottomNav } from '../bottom-nav/bottom-nav';

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, ToastModule, Navbar, Footer, BottomNav],
  providers: [MessageService],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {}
