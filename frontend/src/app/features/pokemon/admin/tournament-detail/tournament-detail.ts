/**
 * TournamentDetail
 *
 * Tournament detail page component.
 * Coordinates tournament administration, participants, groups, bracket, standings and rewards.
 *
 * @author @adrianpmdev
 */

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
    BracketMatchResponse,
    BracketStageResponse,
    GroupMatchResponse,
    GroupParticipantResponse,
    RewardResponse,
    TournamentGroupResponse,
    TournamentParticipantResponse,
    TournamentResponse,
    TournamentStandingResponse,
    UserRewardResponse
} from '../../pokemon-models';
import { TournamentTab } from './tournament-tab';
import {
    BRACKET_MATCH_STATUS_OPTIONS,
    BRACKET_STAGE_STATUS_OPTIONS,
    GROUP_MATCH_STATUS_OPTIONS,
    TOURNAMENT_GROUP_STATUS_OPTIONS,
    TOURNAMENT_PARTICIPANT_STATUS_OPTIONS,
    TOURNAMENT_STATUS_OPTIONS,
    TOURNAMENT_TYPE_OPTIONS
} from '../../tournament-constants';
import { AuthService } from '@core/services/auth';
import { TournamentBracketFacade } from './tournament-bracket';
import { TournamentContextFacade } from './tournament-context';
import { TournamentGroupsFacade } from './tournament-groups';
import { TournamentListFacade } from './tournament-list';
import { TournamentParticipantsFacade } from './tournament-participants';
import { TournamentRewardsFacade } from './tournament-rewards';
import { TournamentStandingsFacade } from './tournament-standings';
import { TournamentUsersFacade } from './tournament-users';

@Component({
    changeDetection: ChangeDetectionStrategy.OnPush,
    selector: 'app-tournament-detail',
    imports: [FormsModule],
    templateUrl: './tournament-detail.html',
    styleUrl: './tournament-detail.scss'
})
export class TournamentDetail implements OnInit {

    readonly authService = inject(AuthService);

    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);
    private readonly contextFacade = inject(TournamentContextFacade);
    private readonly usersFacade = inject(TournamentUsersFacade);
    private readonly tournamentListFacade = inject(TournamentListFacade);
    private readonly participantsFacade = inject(TournamentParticipantsFacade);
    private readonly groupsFacade = inject(TournamentGroupsFacade);
    private readonly bracketFacade = inject(TournamentBracketFacade);
    private readonly standingsFacade = inject(TournamentStandingsFacade);
    private readonly rewardsFacade = inject(TournamentRewardsFacade);

    readonly tournamentTypeOptions = TOURNAMENT_TYPE_OPTIONS;
    readonly tournamentStatusOptions = TOURNAMENT_STATUS_OPTIONS;
    readonly tournamentParticipantStatusOptions = TOURNAMENT_PARTICIPANT_STATUS_OPTIONS;
    readonly tournamentGroupStatusOptions = TOURNAMENT_GROUP_STATUS_OPTIONS;
    readonly groupMatchStatusOptions = GROUP_MATCH_STATUS_OPTIONS;
    readonly bracketStageStatusOptions = BRACKET_STAGE_STATUS_OPTIONS;
    readonly bracketMatchStatusOptions = BRACKET_MATCH_STATUS_OPTIONS;

    readonly gameId = this.contextFacade.gameId;
    readonly lockeId = this.contextFacade.lockeId;
    readonly game = this.contextFacade.game;
    readonly locke = this.contextFacade.locke;
    readonly activeTab = signal<TournamentTab>('tournaments');

    readonly allUsers = this.usersFacade.allUsers;
    readonly tournaments = this.tournamentListFacade.tournaments;
    readonly selectedTournament = this.tournamentListFacade.selectedTournament;
    readonly tourLoading = this.tournamentListFacade.tourLoading;
    readonly tourModal = this.tournamentListFacade.tourModal;
    readonly tourEditTarget = this.tournamentListFacade.tourEditTarget;
    readonly participants = this.participantsFacade.participants;
    readonly partLoading = this.participantsFacade.partLoading;
    readonly partModal = this.participantsFacade.partModal;
    readonly partEditTarget = this.participantsFacade.partEditTarget;
    readonly groups = this.groupsFacade.groups;
    readonly groupLoading = this.groupsFacade.groupLoading;
    readonly groupModal = this.groupsFacade.groupModal;
    readonly groupEditTarget = this.groupsFacade.groupEditTarget;
    readonly selectedGroup = this.groupsFacade.selectedGroup;
    readonly groupParticipants = this.groupsFacade.groupParticipants;
    readonly groupMatches = this.groupsFacade.groupMatches;
    readonly gpModal = this.groupsFacade.gpModal;
    readonly gpEditTarget = this.groupsFacade.gpEditTarget;
    readonly gmModal = this.groupsFacade.gmModal;
    readonly gmEditTarget = this.groupsFacade.gmEditTarget;
    readonly bracketStages = this.bracketFacade.bracketStages;
    readonly bracketLoading = this.bracketFacade.bracketLoading;
    readonly bsModal = this.bracketFacade.bsModal;
    readonly bsEditTarget = this.bracketFacade.bsEditTarget;
    readonly selectedStage = this.bracketFacade.selectedStage;
    readonly bracketMatches = this.bracketFacade.bracketMatches;
    readonly bmModal = this.bracketFacade.bmModal;
    readonly bmEditTarget = this.bracketFacade.bmEditTarget;
    readonly standings = this.standingsFacade.standings;
    readonly standLoading = this.standingsFacade.standLoading;
    readonly standModal = this.standingsFacade.standModal;
    readonly standEditTarget = this.standingsFacade.standEditTarget;
    readonly rewards = this.rewardsFacade.rewards;
    readonly userRewards = this.rewardsFacade.userRewards;
    readonly rewardLoading = this.rewardsFacade.rewardLoading;
    readonly rewardModal = this.rewardsFacade.rewardModal;
    readonly rewardEditTarget = this.rewardsFacade.rewardEditTarget;
    readonly urModal = this.rewardsFacade.urModal;
    readonly urEditTarget = this.rewardsFacade.urEditTarget;

    get tourForm() { return this.tournamentListFacade.tourForm; }
    set tourForm(value) { this.tournamentListFacade.tourForm = value; }
    get tourEditForm() { return this.tournamentListFacade.tourEditForm; }
    set tourEditForm(value) { this.tournamentListFacade.tourEditForm = value; }
    get partForm() { return this.participantsFacade.partForm; }
    set partForm(value) { this.participantsFacade.partForm = value; }
    get partEditForm() { return this.participantsFacade.partEditForm; }
    set partEditForm(value) { this.participantsFacade.partEditForm = value; }
    get groupForm() { return this.groupsFacade.groupForm; }
    set groupForm(value) { this.groupsFacade.groupForm = value; }
    get gpForm() { return this.groupsFacade.gpForm; }
    set gpForm(value) { this.groupsFacade.gpForm = value; }
    get gpEditForm() { return this.groupsFacade.gpEditForm; }
    set gpEditForm(value) { this.groupsFacade.gpEditForm = value; }
    get gmForm() { return this.groupsFacade.gmForm; }
    set gmForm(value) { this.groupsFacade.gmForm = value; }
    get gmEditForm() { return this.groupsFacade.gmEditForm; }
    set gmEditForm(value) { this.groupsFacade.gmEditForm = value; }
    get bsForm() { return this.bracketFacade.bsForm; }
    set bsForm(value) { this.bracketFacade.bsForm = value; }
    get bsEditForm() { return this.bracketFacade.bsEditForm; }
    set bsEditForm(value) { this.bracketFacade.bsEditForm = value; }
    get bmForm() { return this.bracketFacade.bmForm; }
    set bmForm(value) { this.bracketFacade.bmForm = value; }
    get bmEditForm() { return this.bracketFacade.bmEditForm; }
    set bmEditForm(value) { this.bracketFacade.bmEditForm = value; }
    get standForm() { return this.standingsFacade.standForm; }
    set standForm(value) { this.standingsFacade.standForm = value; }
    get standEditForm() { return this.standingsFacade.standEditForm; }
    set standEditForm(value) { this.standingsFacade.standEditForm = value; }
    get rewardForm() { return this.rewardsFacade.rewardForm; }
    set rewardForm(value) { this.rewardsFacade.rewardForm = value; }
    get rewardEditForm() { return this.rewardsFacade.rewardEditForm; }
    set rewardEditForm(value) { this.rewardsFacade.rewardEditForm = value; }
    get urForm() { return this.rewardsFacade.urForm; }
    set urForm(value) { this.rewardsFacade.urForm = value; }

    ngOnInit(): void {
        this.contextFacade.initialize(this.route);
        this.tournamentListFacade.loadTournaments(this.lockeId());
        this.usersFacade.loadAllUsers();
    }

    goToGames(): void {
        this.router.navigate(['/admin/pokemon/games']);
    }

    goToLockes(): void {
        this.router.navigate(['/admin/pokemon/games', this.gameId(), 'lockes'], {
            state: { game: this.game() }
        });
    }

    setTab(tab: TournamentTab): void {
        const tournament = this.selectedTournament();

        this.activeTab.set(tab);

        if (!tournament) {
            return;
        }

        switch (tab) {
            case 'participants':
                this.participantsFacade.loadParticipants(tournament.id);
                break;
            case 'groups':
                this.groupsFacade.loadGroups(tournament.id);
                break;
            case 'bracket':
                this.bracketFacade.loadBracketStages(tournament.id);
                break;
            case 'standings':
                this.standingsFacade.loadStandings(tournament.id);
                break;
            case 'rewards':
                this.rewardsFacade.loadRewards(tournament.id);
                break;
            case 'tournaments':
                break;
        }
    }

    selectTournament(tournament: TournamentResponse): void {
        this.tournamentListFacade.selectTournament(tournament);
        this.setTab('participants');
    }

    getUserName(userId: string): string {
        return this.usersFacade.getUserName(userId);
    }

    getParticipantLabel(participantId: string): string {
        return this.participantsFacade.getParticipantLabel(participantId);
    }

    getRewardName(rewardId: string): string {
        return this.rewardsFacade.getRewardName(rewardId);
    }

    formatDate(iso: string): string {
        if (!iso) {
            return '—';
        }

        return new Date(iso).toLocaleDateString('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric'
        });
    }

    formatDateTime(iso: string): string {
        if (!iso) {
            return '—';
        }

        return new Date(iso).toLocaleString('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    }

    statusClass(status: string): string {
        switch (status?.toLowerCase()) {
            case 'active':
            case 'activo':
            case 'in_progress':
            case 'en_curso':
                return 'status-active';
            case 'completed':
            case 'finished':
            case 'finalizado':
            case 'completado':
                return 'status-done';
            case 'cancelled':
            case 'cancelado':
                return 'status-cancel';
            default:
                return 'status-pending';
        }
    }

    openTourCreate(): void { this.tournamentListFacade.openTourCreate(this.lockeId()); }
    openTourEdit(tournament: TournamentResponse): void { this.tournamentListFacade.openTourEdit(tournament); }
    openTourDelete(tournament: TournamentResponse): void { this.tournamentListFacade.openTourDelete(tournament); }
    closeTourModal(): void { this.tournamentListFacade.closeTourModal(); }
    submitTourCreate(): void { this.tournamentListFacade.submitTourCreate(this.lockeId()); }
    submitTourEdit(): void { this.tournamentListFacade.submitTourEdit(this.lockeId()); }
    confirmTourDelete(): void { this.tournamentListFacade.confirmTourDelete(this.lockeId()); }

    loadParticipants(tournamentId: string): void { this.participantsFacade.loadParticipants(tournamentId); }
    openPartCreate(): void { this.runWithSelectedTournament(tournament => this.participantsFacade.openPartCreate(tournament.id)); }
    openPartEdit(participant: TournamentParticipantResponse): void { this.participantsFacade.openPartEdit(participant); }
    openPartDelete(participant: TournamentParticipantResponse): void { this.participantsFacade.openPartDelete(participant); }
    closePartModal(): void { this.participantsFacade.closePartModal(); }
    submitPartCreate(): void { this.runWithSelectedTournament(tournament => this.participantsFacade.submitPartCreate(tournament.id)); }
    submitPartEdit(): void { this.runWithSelectedTournament(tournament => this.participantsFacade.submitPartEdit(tournament.id)); }
    confirmPartDelete(): void { this.runWithSelectedTournament(tournament => this.participantsFacade.confirmPartDelete(tournament.id)); }

    loadGroups(tournamentId: string): void { this.groupsFacade.loadGroups(tournamentId); }
    openGroupCreate(): void { this.runWithSelectedTournament(tournament => this.groupsFacade.openGroupCreate(tournament.id)); }
    openGroupEdit(group: TournamentGroupResponse): void { this.groupsFacade.openGroupEdit(group); }
    openGroupDelete(group: TournamentGroupResponse): void { this.groupsFacade.openGroupDelete(group); }
    closeGroupModal(): void { this.groupsFacade.closeGroupModal(); }
    submitGroupCreate(): void { this.runWithSelectedTournament(tournament => this.groupsFacade.submitGroupCreate(tournament.id)); }
    submitGroupEdit(): void { this.runWithSelectedTournament(tournament => this.groupsFacade.submitGroupEdit(tournament.id)); }
    confirmGroupDelete(): void { this.runWithSelectedTournament(tournament => this.groupsFacade.confirmGroupDelete(tournament.id)); }
    selectGroup(group: TournamentGroupResponse): void { this.groupsFacade.selectGroup(group); }
    openGpCreate(): void { this.groupsFacade.openGpCreate(); }
    openGpEdit(groupParticipant: GroupParticipantResponse): void { this.groupsFacade.openGpEdit(groupParticipant); }
    openGpDelete(groupParticipant: GroupParticipantResponse): void { this.groupsFacade.openGpDelete(groupParticipant); }
    closeGpModal(): void { this.groupsFacade.closeGpModal(); }
    submitGpCreate(): void { this.groupsFacade.submitGpCreate(); }
    submitGpEdit(): void { this.groupsFacade.submitGpEdit(); }
    confirmGpDelete(): void { this.groupsFacade.confirmGpDelete(); }
    openGmCreate(): void { this.groupsFacade.openGmCreate(); }
    openGmEdit(groupMatch: GroupMatchResponse): void { this.groupsFacade.openGmEdit(groupMatch); }
    openGmDelete(groupMatch: GroupMatchResponse): void { this.groupsFacade.openGmDelete(groupMatch); }
    closeGmModal(): void { this.groupsFacade.closeGmModal(); }
    submitGmCreate(): void { this.groupsFacade.submitGmCreate(); }
    submitGmEdit(): void { this.groupsFacade.submitGmEdit(); }
    confirmGmDelete(): void { this.groupsFacade.confirmGmDelete(); }

    loadBracketStages(tournamentId: string): void { this.bracketFacade.loadBracketStages(tournamentId); }
    openBsCreate(): void { this.runWithSelectedTournament(tournament => this.bracketFacade.openBsCreate(tournament.id)); }
    openBsEdit(bracketStage: BracketStageResponse): void { this.bracketFacade.openBsEdit(bracketStage); }
    openBsDelete(bracketStage: BracketStageResponse): void { this.bracketFacade.openBsDelete(bracketStage); }
    closeBsModal(): void { this.bracketFacade.closeBsModal(); }
    submitBsCreate(): void { this.runWithSelectedTournament(tournament => this.bracketFacade.submitBsCreate(tournament.id)); }
    submitBsEdit(): void { this.runWithSelectedTournament(tournament => this.bracketFacade.submitBsEdit(tournament.id)); }
    confirmBsDelete(): void { this.runWithSelectedTournament(tournament => this.bracketFacade.confirmBsDelete(tournament.id)); }
    selectStage(bracketStage: BracketStageResponse): void { this.runWithSelectedTournament(tournament => this.bracketFacade.selectStage(bracketStage, tournament.id)); }
    openBmCreate(): void { this.bracketFacade.openBmCreate(); }
    openBmEdit(bracketMatch: BracketMatchResponse): void { this.bracketFacade.openBmEdit(bracketMatch); }
    openBmDelete(bracketMatch: BracketMatchResponse): void { this.bracketFacade.openBmDelete(bracketMatch); }
    closeBmModal(): void { this.bracketFacade.closeBmModal(); }
    submitBmCreate(): void { this.bracketFacade.submitBmCreate(); }
    submitBmEdit(): void { this.bracketFacade.submitBmEdit(); }
    confirmBmDelete(): void { this.bracketFacade.confirmBmDelete(); }

    loadStandings(tournamentId: string): void { this.standingsFacade.loadStandings(tournamentId); }
    openStandCreate(): void { this.runWithSelectedTournament(tournament => this.standingsFacade.openStandCreate(tournament.id)); }
    openStandEdit(standing: TournamentStandingResponse): void { this.standingsFacade.openStandEdit(standing); }
    openStandDelete(standing: TournamentStandingResponse): void { this.standingsFacade.openStandDelete(standing); }
    closeStandModal(): void { this.standingsFacade.closeStandModal(); }
    submitStandCreate(): void { this.runWithSelectedTournament(tournament => this.standingsFacade.submitStandCreate(tournament.id)); }
    submitStandEdit(): void { this.runWithSelectedTournament(tournament => this.standingsFacade.submitStandEdit(tournament.id)); }
    confirmStandDelete(): void { this.runWithSelectedTournament(tournament => this.standingsFacade.confirmStandDelete(tournament.id)); }

    loadRewards(tournamentId: string): void { this.rewardsFacade.loadRewards(tournamentId); }
    openRewardCreate(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.openRewardCreate(tournament.id)); }
    openRewardEdit(reward: RewardResponse): void { this.rewardsFacade.openRewardEdit(reward); }
    openRewardDelete(reward: RewardResponse): void { this.rewardsFacade.openRewardDelete(reward); }
    closeRewardModal(): void { this.rewardsFacade.closeRewardModal(); }
    submitRewardCreate(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.submitRewardCreate(tournament.id)); }
    submitRewardEdit(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.submitRewardEdit(tournament.id)); }
    confirmRewardDelete(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.confirmRewardDelete(tournament.id)); }
    openUrCreate(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.openUrCreate(tournament.id)); }
    openUrDelete(userReward: UserRewardResponse): void { this.rewardsFacade.openUrDelete(userReward); }
    closeUrModal(): void { this.rewardsFacade.closeUrModal(); }
    submitUrCreate(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.submitUrCreate(tournament.id)); }
    confirmUrDelete(): void { this.runWithSelectedTournament(tournament => this.rewardsFacade.confirmUrDelete(tournament.id)); }

    private runWithSelectedTournament(callback: (tournament: TournamentResponse) => void): void {
        const tournament = this.selectedTournament();

        if (!tournament) {
            return;
        }

        callback(tournament);
    }
}
