CREATE TABLE public.song (
	id int4 NOT NULL,
	album varchar(255) NULL,
	artist varchar(255) NULL,
	duration varchar(255) NULL,
	"name" varchar(255) NULL,
	"year" varchar(255) NULL,
	CONSTRAINT song_pkey PRIMARY KEY (id)
);