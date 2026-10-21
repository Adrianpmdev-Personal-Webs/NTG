/**
 * modal-mode
 *
 * Shared modal mode models.
 * Defines common modal states used by application dialogs.
 *
 * @author @adrianpmdev
 */

export type ModalMode = 'create' | 'edit' | 'delete' | null;

export type RulesModalMode = ModalMode | 'rules';