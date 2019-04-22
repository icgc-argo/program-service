package org.icgc.argo.program_service.repositories;

import org.icgc.argo.program_service.model.entity.JoinProgramInvitation;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

public interface JoinProgramInvitationRepository extends CrudRepository<JoinProgramInvitation, UUID> {
}

