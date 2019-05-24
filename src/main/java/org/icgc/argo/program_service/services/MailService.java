package org.icgc.argo.program_service.services;

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
import javax.validation.constraints.NotNull;
import java.io.StringWriter;

@Service
@Validated
@Slf4j
public class MailService {
  private JavaMailSender mailSender;

  private VelocityEngine velocityEngine;

  @Autowired
  public void setMailSender(JavaMailSender mailSender) {
    this.mailSender = mailSender;
  }

  @Autowired
  public void setVelocityEngine(VelocityEngine velocityEngine) {
    this.velocityEngine = velocityEngine;
  }

  boolean sendInviteEmail(@NotNull JoinProgramInvite invitation) {
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
      ctx.put("inviteId", invitation.getId());
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
