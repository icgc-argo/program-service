/*
 * Copyright (c) 2020 The Ontario Institute for Cancer Research. All rights reserved
 *
 * This program and the accompanying materials are made available under the terms of the GNU Affero General Public License v3.0.
 * You should have received a copy of the GNU Affero General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 * IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.icgc.argo.program_service.services;

import static java.lang.String.format;
import static org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity.Status.*;

import io.grpc.Status;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.model.entity.ProgramEntity;
import org.icgc.argo.program_service.model.exceptions.NotFoundException;
import org.icgc.argo.program_service.proto.UserRole;
import org.icgc.argo.program_service.repositories.JoinProgramInviteRepository;
import org.icgc.argo.program_service.services.ego.EgoService;
import org.icgc.argo.program_service.services.ego.model.entity.EgoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class InvitationService {

  private final MailService mailService;
  private final JoinProgramInviteRepository invitationRepository;
  private final EgoService egoService;

  @Autowired
  InvitationService(
      @NonNull MailService mailService,
      @NonNull JoinProgramInviteRepository invitationRepository,
      @NonNull EgoService egoService) {
    this.mailService = mailService;
    this.invitationRepository = invitationRepository;
    this.egoService = egoService;
  }

  public UUID inviteUser(
      @NotNull ProgramEntity program,
      @Email @NotNull String userEmail,
      @NotBlank @NotNull String firstName,
      @NotBlank @NotNull String lastName,
      @NotNull UserRole role) {
    val programShortName = program.getShortName();
    val invitation = new JoinProgramInviteEntity(program, userEmail, firstName, lastName, role);
    val previousInvitations =
        invitationRepository.findAllByProgramShortNameAndUserEmail(programShortName, userEmail);

    // 1) If there is an accepted invitation, then return an error saying the user has accepted the
    // invitation
    if (previousInvitations.stream().anyMatch(i -> i.getStatus() == ACCEPTED)) {
      throw Status.ALREADY_EXISTS
          .augmentDescription("Invitation was already accepted")
          .asRuntimeException();
    }
    // 2) If there is a PENDING invitation, then that previous invitation should be set to INVALID.
    // If a previous invitation is EXPIRED,REVOKED or INVALID, leave them as is.
    previousInvitations.stream()
        .filter(i -> i.getStatus() == PENDING)
        .map(i -> i.setStatus(INVALID))
        .forEach(i -> invitationRepository.save(i));

    invitationRepository.save(invitation);
    mailService.sendInviteEmail(invitation);
    return invitation.getId();
  }

  public EgoUser acceptInvite(@NonNull UUID invitationId) throws NotFoundException {
    val invitation =
        invitationRepository
            .findById(invitationId)
            .orElseThrow(
                () ->
                    new NotFoundException(
                        format("Cannot find invitation with id '%s' ", invitationId)));

    if (invitation.getStatus() != PENDING) {
      if (invitation.getStatus() == null) {
        throw Status.FAILED_PRECONDITION
            .augmentDescription(
                "Cannot accept invitation because it is in state(NULL), not PENDING")
            .asRuntimeException();
      }
      throw Status.FAILED_PRECONDITION
          .augmentDescription(
              format(
                  "Cannot accept invitation because it is in state(%s), not PENDING",
                  invitation.getStatus().toString()))
          .asRuntimeException();
    }

    invitation.accept();
    invitationRepository.save(invitation);
    egoService.joinProgram(
        invitation.getUserEmail(), invitation.getProgram().getShortName(), invitation.getRole());
    return egoService.convertInvitationToEgoUser(invitation);
  }

  public Optional<JoinProgramInviteEntity> getLatestInvitation(
      String programShortName, String email) {
    val invitations = listInvitations(programShortName, email);
    val validInvitations =
        invitations.stream()
            .filter(i -> i.getStatus() != INVALID && i.getStatus() != REVOKED)
            .collect(Collectors.toList());
    if (validInvitations.size() == 0) {
      return Optional.empty();
    }
    return Optional.of(validInvitations.get(0));
  }

  public void revoke(String programShortName, String email) {
    val previousInvitations =
        invitationRepository.findAllByProgramShortNameAndUserEmail(programShortName, email);
    previousInvitations.stream()
        .filter(i -> i.getStatus() == PENDING || i.getStatus() == ACCEPTED)
        .map(i -> i.setStatus(REVOKED))
        .forEach(i -> invitationRepository.save(i));
  }

  public Optional<JoinProgramInviteEntity> getInvitationById(UUID invitationId) {
    return invitationRepository.findById(invitationId);
  }

  public List<JoinProgramInviteEntity> listPendingInvitations(String programShortName) {
    return invitationRepository.findAllByProgramShortNameAndStatus(programShortName, PENDING);
  }

  public List<JoinProgramInviteEntity> listInvitations(String programShortName, String email) {
    return invitationRepository.findAllByProgramShortNameAndUserEmailOrderByCreatedAtDesc(
        programShortName, email);
  }
}
