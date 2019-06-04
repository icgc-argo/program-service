package org.icgc.argo.program_service.utils;

import com.google.common.io.CharStreams;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

@Slf4j
public class NoOpJavaMailSender implements JavaMailSender {

  @Override public MimeMessage createMimeMessage() {
    return new MimeMessage((Session)null);
  }

  @Override
  @SneakyThrows
  public MimeMessage createMimeMessage(InputStream inputStream) throws MailException {
    return new MimeMessage(null, inputStream);
  }

  @SneakyThrows
  @Override public void send(MimeMessage mimeMessage) throws MailException {
    val message = CharStreams.toString(new InputStreamReader(mimeMessage.getInputStream()));
    log.debug("Attempted to send message: \n{}", message);
  }

  @Override public void send(MimeMessage... mimeMessages) throws MailException {
    Arrays.stream(mimeMessages).forEach(this::send);
  }

  @Override public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
    throw new NotImplementedException();
  }

  @Override public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
    throw new NotImplementedException();
  }

  @Override public void send(SimpleMailMessage simpleMailMessage) throws MailException {
    throw new NotImplementedException();
  }

  @Override public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
    throw new NotImplementedException();
  }
}
