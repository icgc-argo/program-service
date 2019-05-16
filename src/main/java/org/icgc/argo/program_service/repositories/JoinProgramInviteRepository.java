package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface JoinProgramInviteRepository extends JpaRepository<JoinProgramInvite, UUID> {
}

