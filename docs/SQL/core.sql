CREATE TABLE core.user (
  id uuid NOT NULL,
  username text NOT NULL UNIQUE CHECK (length(username) <= 50),
  avatar_url text CHECK (length(avatar_url) <= 500),
  created_at timestamp without time zone DEFAULT now(),
  role text NOT NULL DEFAULT 'usuario'::text CHECK (length(role) <= 30),
  email character varying NOT NULL,
  CONSTRAINT user_pkey PRIMARY KEY (id)
);
CREATE TABLE core.tag (
  key character varying NOT NULL CHECK (key::text ~ '^[a-z0-9][a-z0-9_-]{1,29}$'::text),
  name character varying NOT NULL,
  description character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT tag_pkey PRIMARY KEY (key)
);
CREATE TABLE core.user_tag (
  user_id uuid NOT NULL,
  tag_key character varying NOT NULL,
  assigned_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT user_tag_pkey PRIMARY KEY (user_id, tag_key),
  CONSTRAINT user_tag_user_id_fkey FOREIGN KEY (user_id) REFERENCES core.user(id),
  CONSTRAINT user_tag_tag_key_fkey FOREIGN KEY (tag_key) REFERENCES core.tag(key)
);