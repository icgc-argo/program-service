CREATE INDEX idx_join_program_status ON join_program_invite(program_id, status);
CREATE INDEX idx_join_program_email ON join_program_invite(program_id, user_email);
CREATE INDEX idx_join_program_created_date ON join_program_invite(program_id, user_email, created_at);
