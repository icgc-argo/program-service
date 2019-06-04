/*
 * Copyright (c) 2019. Ontario Institute for Cancer Research
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package org.icgc.argo.program_service.services;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.icgc.argo.program_service.model.entity.JoinProgramInvite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import javax.mail.MessagingException;
import java.io.StringWriter;

@Slf4j
@Service
@Validated
public class MailService {

  private final JavaMailSender mailSender;
  private final VelocityEngine velocityEngine;

  @Autowired
  public MailService(@NonNull JavaMailSender mailSender,
      @NonNull VelocityEngine velocityEngine) {
    this.mailSender = mailSender;
    this.velocityEngine = velocityEngine;
  }

  public boolean sendInviteEmail(JoinProgramInvite invitation) {
    val msg = mailSender.createMimeMessage();

    try {
      val helper = new MimeMessageHelper(msg, false, "utf-8");
      helper.setTo(invitation.getUserEmail());
      helper.setFrom("noreply@oicr.on.ca");

      val template = velocityEngine.getTemplate("emails/invite.vm");
      val sw = new StringWriter();
      val ctx = new VelocityContext();
      ctx.put("firstName", invitation.getFirstName());
      ctx.put("lastName", invitation.getLastName());
      ctx.put("invitationId", invitation.getId());
      ctx.put("programShortName", invitation.getProgram().getShortName());
      // TODO: add join program link
      ctx.put("joinProgramLink", "#");
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
