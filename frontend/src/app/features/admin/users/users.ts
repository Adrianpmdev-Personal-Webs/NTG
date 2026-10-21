/**
 * Users
 *
 * Users management page component.
 * Handles user listing, filtering, creation (the user chooses their own password from an email link),
 * edition, tag assignment, deletion and resending the password email.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { switchMap } from 'rxjs';
import { CreateUserRequest, UpdateUserRequest, User } from '@shared/models/user';
import { Tag } from '@shared/models/tag';
import { NotificationService } from '@core/services/notification';
import { TagService } from '@core/services/tag';
import { UserService } from '@core/services/user';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-usuarios',
    imports: [FormsModule],
    templateUrl: './users.html',
    styleUrl: './users.scss'
})
export class Users implements OnInit {

    private readonly userService = inject(UserService);
    private readonly tagService = inject(TagService);
    private readonly notificationService = inject(NotificationService);

    readonly users = signal<User[]>([]);
    readonly filteredUsers = signal<User[]>([]);
    readonly loading = signal(true);
    readonly submitting = signal(false);
    readonly inviting = signal<string | null>(null);   // holds the id of the user being emailed
    readonly availableTags = signal<Tag[]>([]);
    readonly modalMode = signal<'create' | 'edit' | 'delete' | null>(null);
    readonly selectedUser = signal<User | null>(null);
    readonly editTarget = signal<User | null>(null);
    readonly roleFilter = signal('');

    searchTerm = '';

    form: CreateUserRequest = {
        email: '',
        username: '',
        tags: []
    };

    editForm: UpdateUserRequest = {
        username: '',
        avatarUrl: ''
    };

    editTags: string[] = [];

    ngOnInit(): void {
        this.loadUsers();
        this.loadTags();
    }

    loadTags(): void {
        this.tagService.getAllTags().subscribe({
            next: tags => this.availableTags.set(tags),
            error: error => this.notificationService.error(this.resolveError(error, 'Error al cargar etiquetas'))
        });
    }

    toggleCreateTag(key: string): void {
        this.form.tags = this.toggle(this.form.tags, key);
    }

    toggleEditTag(key: string): void {
        this.editTags = this.toggle(this.editTags, key);
    }

    loadUsers(): void {
        this.loading.set(true);

        this.userService.getAllUsers().subscribe({
            next: users => {
                this.users.set(users);
                this.applyFilters();
                this.loading.set(false);
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al cargar usuarios'));
                this.loading.set(false);
            }
        });
    }

    onSearch(): void {
        this.applyFilters();
    }

    setRoleFilter(role: string): void {
        this.roleFilter.set(role);
        this.applyFilters();
    }

    selectUser(user: User): void {
        this.selectedUser.set(this.selectedUser()?.id === user.id ? null : user);
    }

    openCreate(): void {
        this.form = {
            email: '',
            username: '',
            tags: []
        };

        this.modalMode.set('create');
    }

    openEdit(user: User): void {
        this.editTarget.set(user);
        this.editForm = {
            username: user.username,
            avatarUrl: user.avatarUrl
        };
        this.editTags = [...user.tags];

        this.modalMode.set('edit');
    }

    openDelete(user: User): void {
        this.editTarget.set(user);
        this.modalMode.set('delete');
    }

    closeModal(): void {
        this.modalMode.set(null);
        this.editTarget.set(null);
        this.submitting.set(false);
    }

    sendInvite(user: User): void {
        if (this.inviting()) {
            return;
        }

        this.inviting.set(user.id);

        this.userService.resendInvite(user.id).subscribe({
            next: () => {
                this.notificationService.success(`Correo enviado a ${user.email}`);
                this.inviting.set(null);
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al enviar el correo'));
                this.inviting.set(null);
            }
        });
    }

    submitCreate(): void {
        if (!this.form.email.trim() || !this.form.username.trim()) {
            this.notificationService.warning('El email y el username son obligatorios');
            return;
        }

        this.submitting.set(true);

        const request: CreateUserRequest = {
            email: this.form.email.trim(),
            username: this.form.username.trim(),
            tags: this.form.tags
        };

        this.userService.createUser(request).subscribe({
            next: result => {
                if (result.inviteEmailSent) {
                    this.notificationService.success(
                        `Usuario ${result.user.username} creado. Se ha enviado un correo a ${result.user.email} para que elija su contraseña`);
                } else {
                    this.notificationService.warning(
                        `Usuario ${result.user.username} creado, pero el correo NO se pudo enviar. Usa INVITAR para reenviarlo`);
                }

                this.closeModal();
                this.loadUsers();
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al crear usuario'));
                this.submitting.set(false);
            }
        });
    }

    submitEdit(): void {
        const target = this.editTarget();

        if (!target) {
            return;
        }

        if (!this.editForm.username?.trim()) {
            this.notificationService.warning('El username es obligatorio');
            return;
        }

        this.submitting.set(true);

        const update$ = this.userService.updateUser(target.id, this.editForm);
        const request$ = this.sameTags(this.editTags, target.tags)
            ? update$
            : update$.pipe(switchMap(() => this.userService.setUserTags(target.id, this.editTags)));

        request$.subscribe({
            next: () => {
                this.notificationService.success('Usuario actualizado correctamente');
                this.closeModal();
                this.loadUsers();
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al actualizar usuario'));
                this.submitting.set(false);
            }
        });
    }

    submitDelete(): void {
        const target = this.editTarget();

        if (!target) {
            return;
        }

        this.submitting.set(true);

        this.userService.deleteUser(target.id).subscribe({
            next: () => {
                this.notificationService.success(`Usuario ${target.username} eliminado`);
                this.closeModal();
                this.loadUsers();
            },
            error: error => {
                this.notificationService.error(this.resolveError(error, 'Error al eliminar usuario'));
                this.submitting.set(false);
            }
        });
    }

    isAdminRole(role: string): boolean {
        return this.normalizeRole(role) === 'admin';
    }

    private applyFilters(): void {
        const searchTerm = this.searchTerm.toLowerCase().trim();
        const selectedRole = this.roleFilter();

        const filteredUsers = this.users().filter(user => {
            const matchesSearch = !searchTerm
                || user.username?.toLowerCase().includes(searchTerm)
                || user.email?.toLowerCase().includes(searchTerm)
                || user.role?.toLowerCase().includes(searchTerm);

            const matchesRole = !selectedRole || this.normalizeRole(user.role) === selectedRole;

            return matchesSearch && matchesRole;
        });

        this.filteredUsers.set(filteredUsers);
    }

    private toggle(list: string[], key: string): string[] {
        return list.includes(key) ? list.filter(item => item !== key) : [...list, key];
    }

    private sameTags(first: string[], second: string[]): boolean {
        return first.length === second.length && first.every(tag => second.includes(tag));
    }

    private normalizeRole(role: string): string {
        return role.toLowerCase().replace('role_', '');
    }

    private resolveError(error: any, fallback: string): string {
        return error.error?.detail || error.error?.message || error.message || fallback;
    }
}
