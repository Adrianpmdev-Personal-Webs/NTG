CREATE TABLE wow_forever.setting (
  key character varying NOT NULL,
  value character varying NOT NULL,
  description character varying,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT setting_pkey PRIMARY KEY (key)
);
CREATE TABLE wow_forever.wow_class (
  slug character varying NOT NULL UNIQUE CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text),
  name character varying NOT NULL,
  icon character varying NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  sort_order integer NOT NULL DEFAULT 0,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT wow_class_pkey PRIMARY KEY (id)
);
CREATE TABLE wow_forever.race (
  sort_order integer NOT NULL DEFAULT 0,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  slug character varying NOT NULL UNIQUE CHECK (slug::text ~ '^[a-z0-9][a-z0-9-]*$'::text),
  name character varying NOT NULL,
  faction character varying NOT NULL CHECK (faction::text = ANY (ARRAY['ALLIANCE'::character varying, 'HORDE'::character varying]::text[])),
  icon character varying NOT NULL,
  id uuid NOT NULL DEFAULT gen_random_uuid(),
  CONSTRAINT race_pkey PRIMARY KEY (id)
);
CREATE TABLE wow_forever.race_class (
  race_id uuid NOT NULL,
  class_id uuid NOT NULL,
  CONSTRAINT race_class_pkey PRIMARY KEY (race_id, class_id),
  CONSTRAINT race_class_race_id_fkey FOREIGN KEY (race_id) REFERENCES wow_forever.race(id),
  CONSTRAINT race_class_class_id_fkey FOREIGN KEY (class_id) REFERENCES wow_forever.wow_class(id)
);