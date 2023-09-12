CREATE TABLE data_center
(
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(255) UNIQUE NOT NULL,
    short_name              VARCHAR(255) UNIQUE NOT NULL,
    organization            VARCHAR(255) NULL DEFAULT '',
    email                   VARCHAR(255) NOT NULL,
    ui_url                  VARCHAR(255) NOT NULL,
    gateway_url             VARCHAR(255) NOT NULL,
    analysis_song_code      VARCHAR(255) NOT NULL,
    analysis_song_url       VARCHAR(255) NOT NULL,
    analysis_score_url      VARCHAR(255) NOT NULL,
    submission_song_code    VARCHAR(255) NOT NULL,
    submission_song_url     VARCHAR(255) NOT NULL,
    submission_score_url    VARCHAR(255) NOT NULL
);

INSERT INTO data_center (id, name, short_name, organization, email, ui_url, gateway_url, analysis_song_code, analysis_song_url, analysis_score_url, submission_song_code, submission_song_url, submission_score_url) VALUES
(uuid_generate_v4(), 'DataCenter1', 'DC1', '', 'abc@xyz.com', 'https://dcc.icgc.org/datacenter/BRCA-US', 'https://dcc.icgc.org/datacenter/BRCA-US', 'ABC', 'https://dcc.icgc.org/datacenter/BRCA-US', 'https://dcc.icgc.org/datacenter/BRCA-US', 'XYZ', 'https://dcc.icgc.org/datacenter/BRCA-US', 'https://dcc.icgc.org/datacenter/BRCA-US');

ALTER TABLE program
ADD COLUMN data_center_id UUID NOT NULL,
FOREIGN KEY(data_center_id) REFERENCES data_center(id);