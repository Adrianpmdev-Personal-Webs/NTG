CREATE TABLE wow_forever.setting (
  key character varying NOT NULL,
  value character varying NOT NULL,
  description character varying,
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT setting_pkey PRIMARY KEY (key)
);