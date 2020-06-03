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

package org.icgc.argo.program_service.utils;

import com.google.common.io.CharStreams;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

@Slf4j
public class NoOpJavaMailSender implements JavaMailSender {

  @Override
  public MimeMessage createMimeMessage() {
    return new MimeMessage((Session) null);
  }

  @Override
  @SneakyThrows
  public MimeMessage createMimeMessage(InputStream inputStream) throws MailException {
    return new MimeMessage(null, inputStream);
  }

  @SneakyThrows
  @Override
  public void send(MimeMessage mimeMessage) throws MailException {
    val message = CharStreams.toString(new InputStreamReader(mimeMessage.getInputStream()));
    log.debug("Attempted to send message: \n{}", message);
  }

  @Override
  public void send(MimeMessage... mimeMessages) throws MailException {
    Arrays.stream(mimeMessages).forEach(this::send);
  }

  @Override
  public void send(MimeMessagePreparator mimeMessagePreparator) throws MailException {
    throw new NotImplementedException();
  }

  @Override
  public void send(MimeMessagePreparator... mimeMessagePreparators) throws MailException {
    throw new NotImplementedException();
  }

  @Override
  public void send(SimpleMailMessage simpleMailMessage) throws MailException {
    throw new NotImplementedException();
  }

  @Override
  public void send(SimpleMailMessage... simpleMailMessages) throws MailException {
    throw new NotImplementedException();
  }
}
