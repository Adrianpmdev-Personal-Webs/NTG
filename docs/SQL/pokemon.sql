CREATE TABLE pokemon.games (
  name character varying NOT NULL,
  description character varying,
  image_url character varying NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT games_pkey PRIMARY KEY (id)
);
CREATE TABLE pokemon.lockes (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  game_id uuid NOT NULL,
  name character varying NOT NULL,
  description character varying,
  rules jsonb,
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT lockes_pkey PRIMARY KEY (id),
  CONSTRAINT lockes_game_id_fkey FOREIGN KEY (game_id) REFERENCES pokemon.games(id)
);
CREATE TABLE pokemon.tournaments (
  locke_id uuid NOT NULL,
  name character varying NOT NULL,
  description character varying,
  tournament_type character varying NOT NULL CHECK (tournament_type::text = ANY (ARRAY['grupos'::character varying, 'bracket'::character varying, 'todos_vs_todos'::character varying]::text[])),
  max_participants smallint,
  start_date timestamp with time zone,
  end_date timestamp with time zone,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  status character varying NOT NULL DEFAULT 'draft'::character varying CHECK (status::text = ANY (ARRAY['draft'::character varying, 'grupos_en_curso'::character varying, 'grupos_finalizado'::character varying, 'bracket_en_curso'::character varying, 'finalizado'::character varying]::text[])),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT tournaments_pkey PRIMARY KEY (id),
  CONSTRAINT tournaments_locke_id_fkey FOREIGN KEY (locke_id) REFERENCES pokemon.lockes(id)
);
CREATE TABLE pokemon.tournament_participants (
  tournament_id uuid NOT NULL,
  user_id uuid NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  joined_at timestamp with time zone DEFAULT now(),
  status character varying NOT NULL DEFAULT 'activo'::character varying CHECK (status::text = ANY (ARRAY['activo'::character varying, 'eliminado'::character varying, 'retirado'::character varying]::text[])),
  CONSTRAINT tournament_participants_pkey PRIMARY KEY (id),
  CONSTRAINT tournament_participants_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id)
);
CREATE TABLE pokemon.tournament_groups (
  tournament_id uuid NOT NULL,
  group_letter character varying NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  status character varying NOT NULL DEFAULT 'en_curso'::character varying CHECK (status::text = ANY (ARRAY['en_curso'::character varying, 'finalizado'::character varying]::text[])),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT tournament_groups_pkey PRIMARY KEY (id),
  CONSTRAINT tournament_groups_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id)
);
CREATE TABLE pokemon.group_participants (
  group_id uuid NOT NULL,
  tournament_participant_id uuid NOT NULL,
  group_position smallint,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  wins smallint DEFAULT 0,
  losses smallint DEFAULT 0,
  points smallint DEFAULT 0,
  CONSTRAINT group_participants_pkey PRIMARY KEY (id),
  CONSTRAINT group_participants_group_id_fkey FOREIGN KEY (group_id) REFERENCES pokemon.tournament_groups(id),
  CONSTRAINT group_participants_tournament_participant_id_fkey FOREIGN KEY (tournament_participant_id) REFERENCES pokemon.tournament_participants(id)
);
CREATE TABLE pokemon.group_matches (
  group_id uuid NOT NULL,
  participant1_id uuid NOT NULL,
  participant2_id uuid NOT NULL,
  winner_id uuid,
  replay_url character varying,
  scheduled_at timestamp with time zone,
  completed_at timestamp with time zone,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  status character varying NOT NULL DEFAULT 'pendiente'::character varying CHECK (status::text = ANY (ARRAY['pendiente'::character varying, 'en_curso'::character varying, 'completado'::character varying]::text[])),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT group_matches_pkey PRIMARY KEY (id),
  CONSTRAINT group_matches_group_id_fkey FOREIGN KEY (group_id) REFERENCES pokemon.tournament_groups(id),
  CONSTRAINT group_matches_participant1_id_fkey FOREIGN KEY (participant1_id) REFERENCES pokemon.tournament_participants(id),
  CONSTRAINT group_matches_participant2_id_fkey FOREIGN KEY (participant2_id) REFERENCES pokemon.tournament_participants(id),
  CONSTRAINT group_matches_winner_id_fkey FOREIGN KEY (winner_id) REFERENCES pokemon.tournament_participants(id)
);
CREATE TABLE pokemon.bracket_stages (
  tournament_id uuid NOT NULL,
  stage_name character varying NOT NULL,
  stage_order smallint NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  status character varying NOT NULL DEFAULT 'pendiente'::character varying CHECK (status::text = ANY (ARRAY['pendiente'::character varying, 'en_curso'::character varying, 'finalizado'::character varying]::text[])),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT bracket_stages_pkey PRIMARY KEY (id),
  CONSTRAINT bracket_stages_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id)
);
CREATE TABLE pokemon.bracket_matches (
  tournament_id uuid NOT NULL,
  bracket_stage_id uuid NOT NULL,
  match_number smallint NOT NULL,
  participant1_id uuid,
  participant2_id uuid,
  winner_id uuid,
  replay_url character varying,
  scheduled_at timestamp with time zone,
  completed_at timestamp with time zone,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  status character varying NOT NULL DEFAULT 'pendiente'::character varying CHECK (status::text = ANY (ARRAY['pendiente'::character varying, 'en_curso'::character varying, 'completado'::character varying]::text[])),
  created_at timestamp with time zone DEFAULT now(),
  CONSTRAINT bracket_matches_pkey PRIMARY KEY (id),
  CONSTRAINT bracket_matches_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id),
  CONSTRAINT bracket_matches_bracket_stage_id_fkey FOREIGN KEY (bracket_stage_id) REFERENCES pokemon.bracket_stages(id),
  CONSTRAINT bracket_matches_participant1_id_fkey FOREIGN KEY (participant1_id) REFERENCES pokemon.tournament_participants(id),
  CONSTRAINT bracket_matches_participant2_id_fkey FOREIGN KEY (participant2_id) REFERENCES pokemon.tournament_participants(id),
  CONSTRAINT bracket_matches_winner_id_fkey FOREIGN KEY (winner_id) REFERENCES pokemon.tournament_participants(id)
);
CREATE TABLE pokemon.tournament_standings (
  tournament_id uuid NOT NULL,
  tournament_participant_id uuid NOT NULL,
  final_position smallint,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  total_wins smallint DEFAULT 0,
  total_losses smallint DEFAULT 0,
  total_points smallint DEFAULT 0,
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT tournament_standings_pkey PRIMARY KEY (id),
  CONSTRAINT tournament_standings_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id),
  CONSTRAINT tournament_standings_tournament_participant_id_fkey FOREIGN KEY (tournament_participant_id) REFERENCES pokemon.tournament_participants(id)
);
CREATE TABLE pokemon.global_standings (
  user_id uuid NOT NULL UNIQUE,
  ranking_position smallint,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  total_tournaments smallint DEFAULT 0,
  tournaments_won smallint DEFAULT 0,
  total_points integer DEFAULT 0,
  updated_at timestamp with time zone DEFAULT now(),
  CONSTRAINT global_standings_pkey PRIMARY KEY (id)
);
CREATE TABLE pokemon.rewards (
  tournament_id uuid NOT NULL,
  position smallint NOT NULL,
  reward_name character varying NOT NULL,
  reward_value smallint,
  image_url character varying,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  CONSTRAINT rewards_pkey PRIMARY KEY (id),
  CONSTRAINT rewards_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id)
);
CREATE TABLE pokemon.user_rewards (
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  claimed_at timestamp with time zone DEFAULT now(),
  user_id uuid NOT NULL,
  reward_id uuid NOT NULL,
  tournament_id uuid NOT NULL,
  CONSTRAINT user_rewards_pkey PRIMARY KEY (id),
  CONSTRAINT user_rewards_reward_id_fkey FOREIGN KEY (reward_id) REFERENCES pokemon.rewards(id),
  CONSTRAINT user_rewards_tournament_id_fkey FOREIGN KEY (tournament_id) REFERENCES pokemon.tournaments(id)
);