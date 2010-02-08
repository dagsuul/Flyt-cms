/*
 * Copyright 2009 Kantega AS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package no.kantega.publishing.modules.mailsender;

import no.kantega.commons.configuration.Configuration;
import no.kantega.commons.exception.ConfigurationException;
import no.kantega.commons.exception.SystemException;
import no.kantega.commons.log.Log;
import no.kantega.publishing.common.Aksess;
import no.kantega.publishing.spring.RootContext;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.Resource;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.io.StringWriter;
import java.util.Date;
import java.util.Map;
import java.util.Properties;


/**
 * Utility class for sending mail - with helper methods to create elements to use in a mail.
 *
 * @author Anders Skar, Vidar B. Tilrem
 */
public class MailSender {
    private static final String SOURCE = "Aksess.MailSender";

    /**
     * Sends a mail message. The message body is created by using a simple template mechanism. See MailTextReader.
     *
     * @param from           Sender's email address.
     * @param to             Recipient's email address.
     * @param subject        Subject text for the email.
     * @param contentFile    Template file.
     * @param replaceStrings Strings to insert into template.
     * @throws SystemException        if an unexpected error occurs.
     * @throws ConfigurationException if a configuration error occurs.
     */
    public static void send(String from, String to, String subject, String contentFile, String replaceStrings[]) throws SystemException, ConfigurationException {
        // Fyll inn content
        String text = MailTextReader.getContent(contentFile, replaceStrings);
        send(from, to, subject, text);

    }

    /**
     * Sends a mail message. The message body is created by using a Velocity template.
     *
     * @param from        Sender's email address.
     * @param to          Recipient's email address.
     * @param subject     Subject text for the email.
     * @param contentFile Velocity template file.
     * @param parameters  Parameters used by Velocity to merge values into the template.
     * @throws SystemException        if an unexpected error occurs.
     * @throws ConfigurationException if a configuration error occurs.
     */
    public static void send(String from, String to, String subject, String contentFile, Map parameters) throws SystemException, ConfigurationException {
        String content = createStringFromVelocityTemplate(contentFile, parameters);
        send(from, to, subject, content);
    }

    /**
     * Sends a mail message with a simple string as the message body.
     *
     * @param from    Sender's email address.
     * @param to      Recipient's email address.
     * @param subject Subject text for the email.
     * @param content The message body.
     * @throws SystemException        if an unexpected error occurs.
     * @throws ConfigurationException if a configuration error occurs.
     */
    public static void send(String from, String to, String subject, String content) throws ConfigurationException, SystemException {
        try {
            MimeBodyPart bp = createMimeBodyPartFromStringMessage(content);
            send(from, to, subject, new MimeBodyPart[]{bp});
        } catch (SystemException e) {
            throw new SystemException("Feil ved utsending av epost", SOURCE, e);
        }

    }

    /**
     * Sends a mail message. The content must be provided as MimeBodyPart objects.
     *
     * @param from      Sender's email address.
     * @param to        Recipient's email address.
     * @param subject   Subject text for the email.
     * @param bodyParts The body parts to insert into the message.
     * @throws SystemException        if an unexpected error occurs.
     * @throws ConfigurationException if a configuration error occurs.
     */
    public static void send(String from, String to, String subject, MimeBodyPart[] bodyParts) throws ConfigurationException, SystemException {
        try {
            Properties props = new Properties();

            Configuration config = Aksess.getConfiguration();
            String host = config.getString("mail.host");
            if (host == null) {
                throw new ConfigurationException("mail.host", SOURCE);
            }

            // I noen tilfeller �nsker vi at all epost skal g� til en testadresse
            String catchAllTo = config.getString("mail.catchall.to");
            if (catchAllTo != null && catchAllTo.indexOf("@") != -1) {
                subject = " (original recipient: " + to + ") " + subject;
                to = catchAllTo;
            }

            props.setProperty("mail.smtp.host", host);

            Session session = Session.getDefaultInstance(props);

            boolean debug = config.getBoolean("mail.debug", false);
            if (debug) {
                session.setDebug(true);
            }

            // Opprett message, sett attributter
            MimeMessage message = new MimeMessage(session);
            InternetAddress fromAddress = new InternetAddress(from);
            InternetAddress toAddress[] = InternetAddress.parse(to);

            message.setFrom(fromAddress);

            if (toAddress.length > 1) {
                message.setRecipients(Message.RecipientType.BCC, toAddress);
            } else {
                message.setRecipients(Message.RecipientType.TO, toAddress);
            }
            message.setSubject(subject, "ISO-8859-1");
            message.setSentDate(new Date());

            Multipart mp = new MimeMultipart();
            for (int i = 0; i < bodyParts.length; i++) {
                MimeBodyPart bodyPart = bodyParts[i];
                mp.addBodyPart(bodyPart);
            }
            message.setContent(mp);

            // Send meldingen
            Transport.send(message);

            // Logg sending
            Log.debug(SOURCE, "Sending email to " + to + " with subject " + subject, null, null);
        } catch (MessagingException e) {
            throw new SystemException("Error sending email to : " + to + " with subject " + subject, SOURCE, e);
        }
    }

    /**
     * Helper method to create a string from a Velocity template.
     *
     * @param templateFile The name of the template file to use.
     * @param parameters   The values to merge into the template.
     * @return The result of the merge.
     * @throws SystemException if template handling fails.
     */
    public static String createStringFromVelocityTemplate(String templateFile, Map parameters) throws SystemException {
        try {
            Velocity.init();

            ResourceLoader source = (ResourceLoader) RootContext.getInstance().getBean("emailTemplateResourceLoader");
            Resource resource = source.getResource(templateFile);

            Configuration config = Aksess.getConfiguration();

            String encoding = config.getString("mail.templates.encoding", "ISO-8859-1");
            String templateText = IOUtils.toString(resource.getInputStream(), encoding);

            VelocityContext context = new VelocityContext(parameters);

            StringWriter textWriter = new StringWriter();
            Velocity.evaluate(context, textWriter, "body", templateText);

            return textWriter.toString();
        } catch (Exception e) {
            throw new SystemException(SOURCE, "Feil ved generering av mailtekst basert p� Velocity", e);
        }
    }

    /**
     * Helper method to create a MimeBodyPart from a string.
     *
     * @param content The string to insert into the MimeBodyPart.
     * @return The resulting MimeBodyPart.
     * @throws SystemException if the MimeBodyPart can't be created.
     */
    public static MimeBodyPart createMimeBodyPartFromStringMessage(String content) throws SystemException {
        try {
            MimeBodyPart bp = new MimeBodyPart();
            if (content.indexOf("<html>") != -1 || content.indexOf("<HTML>") != -1) {
                bp.setContent(content, "text/html; charset=iso-8859-1");
            } else {
                bp.setText(content, "ISO-8859-1");
                bp.setHeader("Content-Transfer-Encoding", "quoted-printable");
            }
            return bp;
        } catch (MessagingException e) {
            throw new SystemException(SOURCE, "Feil ved generering av MimeBodyPart fra string", e);
        }
    }


    /**
     * Helper method to create a MimeBodyPart from a binary file.
     *
     * @param pathToFile The complete path to the file - including file name.
     * @param contentType The Mime content type of the file.
     * @param fileName   The name of the file - as it will appear for the mail recipient.
     * @return The resulting MimeBodyPart.
     * @throws SystemException if the MimeBodyPart can't be created.
     */
    public static MimeBodyPart createMimeBodyPartFromBinaryFile(final String pathToFile, final String contentType, String fileName) throws SystemException {
        try {
            MimeBodyPart attachmentPart1 = new MimeBodyPart();
            FileDataSource fileDataSource1 = new FileDataSource(pathToFile) {
                @Override
                public String getContentType() {
                    return contentType;
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileName);
            return attachmentPart1;
        } catch (MessagingException e) {
            throw new SystemException(SOURCE, "Feil ved generering av MimeBodyPart fra bin�rfil", e);
        }
    }

    /**
     * Helper method to create a MimeBodyPart from a binary file.
     *
     * @param file     The file.
     * @param contentType The Mime content type of the file.
     * @param fileName The name of the file - as it will appear for the mail recipient.
     * @return The resulting MimeBodyPart.
     * @throws SystemException if the MimeBodyPart can't be created.
     */
    public static MimeBodyPart createMimeBodyPartFromBinaryFile(final File file, final String contentType, String fileName) throws SystemException {
        try {
            MimeBodyPart attachmentPart1 = new MimeBodyPart();
            FileDataSource fileDataSource1 = new FileDataSource(file) {
                @Override
                public String getContentType() {
                    return contentType;
                }
            };
            attachmentPart1.setDataHandler(new DataHandler(fileDataSource1));
            attachmentPart1.setFileName(fileName);
            return attachmentPart1;
        } catch (MessagingException e) {
            throw new SystemException(SOURCE, "Feil ved generering av MimeBodyPart fra bin�rfil", e);
        }
    }
}

