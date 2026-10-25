/**
 * TalentsApi
 *
 * HTTP service for the talent trees. The backend sends talents.json as it is; this is the only place that knows its format.
 *
 * @author @adrianpmdev
 */

import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { API_BASE_URL } from '@core/config/api';
import { Talent, TalentTree, TalentTreeFile, TalentsData, TalentsFile } from './talents-models';

const toTree = (file: TalentTreeFile): TalentTree => {
    const talents: Talent[] = file.talents.map(talent => ({
        id: talent.id,
        treeId: file.id,
        name: talent.name,
        row: talent.row,
        column: talent.column,
        maxRanks: talent.max_ranks,
        pointsRequired: talent.points_required,
        requires: talent.requires,
        spellId: talent.spell_id,
        iconId: talent.icon_id,
        descriptions: talent.description ?? []
    }));

    const rowOf = new Map(talents.map(talent => [talent.id, talent.row]));

    return {
        id: file.id,
        name: file.name,
        rows: file.rows,
        columns: file.columns,
        // A prerequisite must sit above the talent that needs it. The source sometimes also lists a talent from a
        // lower row (two talents pointing at each other), which would let both hold each other up.
        talents: talents.map(talent => ({
            ...talent,
            requires: talent.requires.filter(id => (rowOf.get(id) ?? Infinity) < talent.row)
        }))
    };
};

const toData = (file: TalentsFile): TalentsData => ({
    build: file.build,
    attribution: file.attribution,
    levels: file.levels_that_grant_a_point,
    classes: file.classes.map(wowClass => ({ slug: wowClass.slug, trees: wowClass.trees.map(toTree) }))
});

@Injectable({ providedIn: 'root' })
export class TalentsApi {

    private readonly http = inject(HttpClient);
    private readonly baseUrl = `${API_BASE_URL}/wow-forever`;

    /** Every class with its trees and talents. */
    getTalents(): Observable<TalentsData> {
        return this.http.get<TalentsFile>(`${this.baseUrl}/talents`).pipe(map(toData));
    }
}
