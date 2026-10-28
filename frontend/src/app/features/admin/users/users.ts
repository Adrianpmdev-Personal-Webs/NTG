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
import { resolveApiError } from '@shared/utils/api-error';
import { NotificationCenter } from '@core/notifications/notification-center';
import { TagApi } from '@core/tags/tag-api';
import { UserApi } from '@core/users/user-api';
import { ModalOverlay } from '@shared/directives/modal-overlay';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-usuarios',
    imports: [ModalOverlay, FormsModule],
    templateUrl: './users.html',
    styleUrl: './users.scss',
})
export class Users implements OnInit {
    private readonly userApi = inject(UserApi);
    private readonly tagApi = inject(TagApi);
    private readonly notificationCenter = inject(NotificationCenter);

    protected readonly users = signal<User[]>([]);
    protected readonly filteredUsers = signal<User[]>([]);
    protected readonly loading = signal(true);
    protected readonly submitting = signal(false);
    protected readonly inviting = signal<string | null>(null);
    protected readonly availableTags = signal<Tag[]>([]);
    protected readonly modalMode = signal<'create' | 'edit' | 'delete' | null>(null);
    protected readonly selectedUser = signal<User | null>(null);
    protected readonly editTarget = signal<User | null>(null);
    protected readonly roleFilter = signal('');

    protected searchTerm = '';

    protected form: CreateUserRequest = {
        email: '',
        username: '',
        tags: [],
    };

    protected editForm: UpdateUserRequest = {
        username: '',
        avatarUrl: '',
    };

    protected editTags: string[] = [];

    ngOnInit(): void {
        this.loadUsers();
        this.loadTags();
    }

    private loadTags(): void {
        this.tagApi.getAllTags().subscribe({
            next: (tags) => this.availableTags.set(tags),
            error: (error) => this.notificationCenter.error(resolveApiError(error, 'Error al cargar etiquetas')),
        });
    }

    protected toggleCreateTag(key: string): void {
        this.form.tags = this.toggle(this.form.tags, key);
    }

    protected toggleEditTag(key: string): void {
        this.editTags = this.toggle(this.editTags, key);
    }

    private loadUsers(): void {
        this.loading.set(true);

        this.userApi.getAllUsers().subscribe({
            next: (users) => {
                this.users.set(users);
                this.applyFilters();
                this.loading.set(false);
            },
            error: (error) => {
                this.notificationCenter.error(resolveApiError(error, 'Error al cargar usuarios'));
                this.loading.set(false);
            },
        });
    }

    protected searchUsers(): void {
        this.applyFilters();
    }

    protected setRoleFilter(role: string): void {
        this.roleFilter.set(role);
        this.applyFilters();
    }

    protected selectUser(user: User): void {
        this.selectedUser.set(this.selectedUser()?.id === user.id ? null : user);
    }

    protected openCreate(): void {
        this.form = {
            email: '',
            username: '',
            tags: [],
        };

        this.modalMode.set('create');
    }

    protected openEdit(user: User): void {
        this.editTarget.set(user);
        this.editForm = {
            username: user.username,
            avatarUrl: user.avatarUrl,
        };
        this.editTags = [...user.tags];

        this.modalMode.set('edit');
    }

    protected openDelete(user: User): void {
        this.editTarget.set(user);
        this.modalMode.set('delete');
    }

    protected closeModal(): void {
        this.modalMode.set(null);
        this.editTarget.set(null);
        this.submitting.set(false);
    }

    protected sendInvite(user: User): void {
        if (this.inviting()) {
            return;
        }

        this.inviting.set(user.id);

        this.userApi.resendInvite(user.id).subscribe({
            next: () => {
                this.notificationCenter.success(`Correo enviado a ${user.email}`);
                this.inviting.set(null);
            },
            error: (error) => {
                this.notificationCenter.error(resolveApiError(error, 'Error al enviar el correo'));
                this.inviting.set(null);
            },
        });
    }

    protected submitCreate(): void {
        if (!this.form.email.trim() || !this.form.username.trim()) {
            this.notificationCenter.warning('El email y el username son obligatorios');
            return;
        }

        this.submitting.set(true);

        const request: CreateUserRequest = {
            email: this.form.email.trim(),
            username: this.form.username.trim(),
            tags: this.form.tags,
        };

        this.userApi.createUser(request).subscribe({
            next: (result) => {
                if (result.inviteEmailSent) {
                    this.notificationCenter.success(
                        `Usuario ${result.user.username} creado. Se ha enviado un correo a ${result.user.email} para que elija su contraseña`,
                    );
                } else {
                    this.notificationCenter.warning(
                        `Usuario ${result.user.username} creado, pero el correo NO se pudo enviar. Usa INVITAR para reenviarlo`,
                    );
                }

                this.closeModal();
                this.loadUsers();
            },
            error: (error) => {
                this.notificationCenter.error(resolveApiError(error, 'Error al crear usuario'));
                this.submitting.set(false);
            },
        });
    }

    protected submitEdit(): void {
        const target = this.editTarget();

        if (!target) {
            return;
        }

        if (!this.editForm.username?.trim()) {
            this.notificationCenter.warning('El username es obligatorio');
            return;
        }

        this.submitting.set(true);

        const update$ = this.userApi.updateUser(target.id, this.editForm);
        const request$ = this.sameTags(this.editTags, target.tags)
            ? update$
            : update$.pipe(switchMap(() => this.userApi.setUserTags(target.id, this.editTags)));

        request$.subscribe({
            next: () => {
                this.notificationCenter.success('Usuario actualizado correctamente');
                this.closeModal();
                this.loadUsers();
            },
            error: (error) => {
                this.notificationCenter.error(resolveApiError(error, 'Error al actualizar usuario'));
                this.submitting.set(false);
            },
        });
    }

    protected submitDelete(): void {
        const target = this.editTarget();

        if (!target) {
            return;
        }

        this.submitting.set(true);

        this.userApi.deleteUser(target.id).subscribe({
            next: () => {
                this.notificationCenter.success(`Usuario ${target.username} eliminado`);
                this.closeModal();
                this.loadUsers();
            },
            error: (error) => {
                this.notificationCenter.error(resolveApiError(error, 'Error al eliminar usuario'));
                this.submitting.set(false);
            },
        });
    }

    protected isAdminRole(role: string): boolean {
        return this.normalizeRole(role) === 'admin';
    }

    private applyFilters(): void {
        const searchTerm = this.searchTerm.toLowerCase().trim();
        const selectedRole = this.roleFilter();

        const filteredUsers = this.users().filter((user) => {
            const matchesSearch =
                !searchTerm ||
                user.username?.toLowerCase().includes(searchTerm) ||
                user.email?.toLowerCase().includes(searchTerm) ||
                user.role?.toLowerCase().includes(searchTerm);

            const matchesRole = !selectedRole || this.normalizeRole(user.role) === selectedRole;

            return matchesSearch && matchesRole;
        });

        this.filteredUsers.set(filteredUsers);
    }

    private toggle(list: string[], key: string): string[] {
        return list.includes(key) ? list.filter((item) => item !== key) : [...list, key];
    }

    private sameTags(first: string[], second: string[]): boolean {
        return first.length === second.length && first.every((tag) => second.includes(tag));
    }

    private normalizeRole(role: string): string {
        return role.toLowerCase().replace('role_', '');
    }
}
