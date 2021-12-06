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

import java.io.StringWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import javax.mail.MessagingException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.icgc.argo.program_service.model.entity.JoinProgramInviteEntity;
import org.icgc.argo.program_service.properties.AppProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
public class MailService {

  private final JavaMailSender mailSender;
  private final VelocityEngine velocityEngine;
  private final AppProperties appProperties;

  @Autowired
  public MailService(
      @NonNull JavaMailSender mailSender,
      @NonNull VelocityEngine velocityEngine,
      @NonNull AppProperties applicationProperties) {
    this.mailSender = mailSender;
    this.velocityEngine = velocityEngine;
    this.appProperties = applicationProperties;
  }

  boolean sendInviteEmail(JoinProgramInviteEntity invitation) {
    val msg = mailSender.createMimeMessage();

    try {
      val emailProps = this.appProperties.getEmail();
      val helper = new MimeMessageHelper(msg, false, "utf-8");
      helper.setTo(invitation.getUserEmail());
      helper.setFrom(emailProps.getFrom());
      helper.setSubject(emailProps.getInvitation().getSubject());
      val template = velocityEngine.getTemplate("emails/invite.vm");
      val sw = new StringWriter();
      val ctx = new VelocityContext();

      ctx.put("dacoLink", emailProps.getInvitation().getDacoLink());
      ctx.put("docLink", emailProps.getInvitation().getDocLink());
      ctx.put("contactLink", emailProps.getInvitation().getContactLink());
      ctx.put("privacyPolicyLink", emailProps.getInvitation().getPrivacyPolicyLink());
      ctx.put("platformLink", emailProps.getInvitation().getPlatformUrl());
      ctx.put("programAccessDocsLink", emailProps.getInvitation().getProgramAccessDocsLink());
      ctx.put("firstName", invitation.getFirstName());
      ctx.put("lastName", invitation.getLastName());
      ctx.put("invitationId", invitation.getId());
      ctx.put("programShortName", invitation.getProgram().getShortName());
      ctx.put("programName", invitation.getProgram().getName());
      ctx.put("role", invitation.getRole());
      ctx.put("email", invitation.getUserEmail());
      ctx.put("currentYear", Calendar.getInstance().get(Calendar.YEAR));
      ctx.put(
          "joinProgramLink",
          emailProps.getInvitation().getInvitationUrlPrefix() + invitation.getId());

      ctx.put(
          "expireTime",
          invitation
              .getExpiresAt()
              .atZone(ZoneId.of("UTC"))
              .format(DateTimeFormatter.ofPattern("LLLL d, yyyy 'at' hh:mm a VV")));

      template.merge(ctx, sw);

      msg.setContent(sw.toString(), "text/html");
    } catch (MessagingException e) {
      log.error("Cannot create invite email message", e);
      return false;
    }

    try {
      mailSender.send(msg);
      invitation.setEmailSent(true);
      return true;
    } catch (MailAuthenticationException e) {
      log.error("Cannot log in to mail server", e);
      return false;
    }
  }
}
