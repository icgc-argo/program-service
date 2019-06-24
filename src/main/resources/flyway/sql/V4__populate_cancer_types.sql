-- CREATE TYPE join_program_invite_status AS ENUM ('PENDING', 'ACCEPTED', 'REVOKED');
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

INSERT INTO cancer (id, name)
VALUES
(uuid_generate_v4(), 'Renal cancer'),
(uuid_generate_v4(), 'Blood cancer'),
(uuid_generate_v4(), 'Liver cancer'),
(uuid_generate_v4(), 'Prostate cancer'),
(uuid_generate_v4(), 'Breast cancer'),
(uuid_generate_v4(), 'Pancreatic cancer'),
(uuid_generate_v4(), 'Brain cancer'),
(uuid_generate_v4(), 'Lung cancer'),
(uuid_generate_v4(), 'Bone cancer'),
(uuid_generate_v4(), 'Gastric cancer'),
(uuid_generate_v4(), 'Ovarian cancer'),
(uuid_generate_v4(), 'Biliary Tract cancer'),
(uuid_generate_v4(), 'Bladder cancer'),
(uuid_generate_v4(), 'Esophageal cancer'),
(uuid_generate_v4(), 'Head and Neck cancer'),
(uuid_generate_v4(), 'Skin cancer'),
(uuid_generate_v4(), 'Soft Tissue cancer'),
(uuid_generate_v4(), 'Thyroid cancer'),
(uuid_generate_v4(), 'Cervical cancer'),
(uuid_generate_v4(), 'Chronic Lymphocytic Leukemia'),
(uuid_generate_v4(), 'Chronic Myeloid Disorders'),
(uuid_generate_v4(), 'Colon cancer'),
(uuid_generate_v4(), 'Colorectal cancer'),
(uuid_generate_v4(), 'Endometrial cancer'),
(uuid_generate_v4(), 'Lymphoproliferative Syndrome'),
(uuid_generate_v4(), 'Malignant Lymphoma'),
(uuid_generate_v4(), 'Melanoma'),
(uuid_generate_v4(), 'Nasopharyngeal cancer'),
(uuid_generate_v4(), 'Oral cancer'),
(uuid_generate_v4(), 'Pediatric Brain Tumor'),
(uuid_generate_v4(), 'Pediatric Brain Tumors'),
(uuid_generate_v4(), 'Pediatric Solid Tumor'),
(uuid_generate_v4(), 'Rare Pancreatic Tumors'),
(uuid_generate_v4(), 'Rectal cancer'),
(uuid_generate_v4(), 'Uterine cancer');

INSERT INTO primary_site (id, name) VALUES
(uuid_generate_v4(), 'Blood'),
(uuid_generate_v4(), 'Kidney'),
(uuid_generate_v4(), 'Liver'),
(uuid_generate_v4(), 'Brain'),
(uuid_generate_v4(), 'Pancreas'),
(uuid_generate_v4(), 'Prostate'),
(uuid_generate_v4(), 'Breast'),
(uuid_generate_v4(), 'Head and neck'),
(uuid_generate_v4(), 'Lung'),
(uuid_generate_v4(), 'Bone'),
(uuid_generate_v4(), 'Colorectal'),
(uuid_generate_v4(), 'Ovary'),
(uuid_generate_v4(), 'Skin'),
(uuid_generate_v4(), 'Stomach'),
(uuid_generate_v4(), 'Bladder'),
(uuid_generate_v4(), 'Esophagus'),
(uuid_generate_v4(), 'Gall Bladder'),
(uuid_generate_v4(), 'Mesenchymal'),
(uuid_generate_v4(), 'Uterus'),
(uuid_generate_v4(), 'Cervix'),
(uuid_generate_v4(), 'Nasopharynx'),
(uuid_generate_v4(), 'Nervous System');
