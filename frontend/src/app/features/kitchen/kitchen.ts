import { Component, inject, OnDestroy, signal } from '@angular/core';
import { Api } from '../../core/api';
import { errorMessage } from '../../core/error.interceptor';
import { Format } from '../../core/format';
import { TICKET, label } from '../../core/labels';
import { KitchenTicket } from '../../core/models';
import { Session } from '../../core/session';

@Component({
  selector: 'app-kitchen',
  templateUrl: './kitchen.html',
})
export class KitchenPage implements OnDestroy {
  readonly session = inject(Session);
  readonly format = inject(Format);
  private readonly api = inject(Api);
  readonly tickets = signal<KitchenTicket[]>([]);
  readonly history = signal(false);
  readonly station = signal('');
  private abort: AbortController | null = null;
  label = label;
  states = TICKET;

  constructor() {
    this.reload();
    this.connect();
  }

  ngOnDestroy(): void {
    this.abort?.abort();
  }

  reload(): void {
    const branch = this.session.branchId();
    if (!branch) return;
    this.api.tickets(branch, this.history()).subscribe({
      next: (rows) => this.tickets.set(rows),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  visible(): KitchenTicket[] {
    const name = this.station().trim().toLowerCase();
    return this.tickets().filter((ticket) => !name || ticket.stationName.toLowerCase().includes(name));
  }

  toggleHistory(): void {
    this.history.update((value) => !value);
    this.reload();
  }

  advance(ticket: KitchenTicket): void {
    if (!this.session.can('kitchen.update') || ticket.status === 'DELIVERED' || ticket.status === 'CANCELLED') return;
    const next = ticket.status === 'QUEUED' ? 'IN_PROGRESS' : ticket.status === 'IN_PROGRESS' ? 'READY' : 'DELIVERED';
    this.api.advanceTicket(ticket.id, next).subscribe({
      next: () => this.reload(),
      error: (error: unknown) => this.session.toast('error', errorMessage(error)),
    });
  }

  print(): void {
    window.print();
  }

  private connect(): void {
    const branch = this.session.branchId();
    const token = this.session.token();
    if (!branch || !token) return;
    this.abort = new AbortController();
    void fetch(`/api/kitchen/stream?branchId=${branch}`, {
      headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
      signal: this.abort.signal,
    }).then(async (response) => {
      const reader = response.body?.getReader();
      if (!reader) return;
      const decoder = new TextDecoder();
      let buffer = '';
      while (true) {
        const chunk = await reader.read();
        if (chunk.done) break;
        buffer += decoder.decode(chunk.value, { stream: true });
        if (buffer.includes('event:kitchen') || buffer.includes('event: kitchen')) {
          buffer = '';
          if (!this.history()) this.reload();
        }
      }
    }).catch(() => undefined);
  }
}
