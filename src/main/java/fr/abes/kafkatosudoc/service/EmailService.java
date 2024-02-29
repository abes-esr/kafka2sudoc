package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kafkatosudoc.dto.mail.MailDto;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    protected String url;

    @Value("${spring.profiles.active}")
    private String env;

    public void sendErrorsMessage(String listErrorsMessages, String filename) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreurs lors du traitement sur le fichier " + filename, listErrorsMessages);
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailSuppressionPackage(String packageName, String provider, Exception e) {
        StringBuilder body =  new StringBuilder("Une erreur s'est produite lors de la recherche de la notice bouquet : ");
        body.append(packageName);
        body.append(" / ");
        body.append(provider);
        body.append("<br /><br />Avec le message d'erreur suivant : ");
        body.append(e.getMessage());

        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression du package " + packageName + " / " + provider, body.toString());
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    public void sendErrorMailProviderPackageDeleted(List<String> listError) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Erreur lors de la suppression des liens vers les notices bouquet", String.valueOf(listError));
        //  Envoi du message par mail
        sendMail(requestJson);
        log.info("L'email a été correctement envoyé.");
    }

    protected void sendMail(String requestJson) {
        RestTemplate restTemplate = new RestTemplate(); //appel ws qui envoie le mail
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestJson, headers);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            restTemplate.postForObject(url + "htmlMail/", entity, String.class); //appel du ws avec
        } catch (Exception e) {
            log.error("Erreur dans l'envoi du mail d'erreur Sudoc" + e);
        }
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost mail = new HttpPost(this.url + "htmlMail/");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(mail);
        } catch (IOException e) {
            log.error("Erreur lors de l'envoi du mail. " + e);
        }
    }

    protected String mailToJSON(String to, String subject, String text) {
        String json = "";
        ObjectMapper mapper = new ObjectMapper();
        MailDto mail = new MailDto();
        mail.setApp("kafka2sudoc");
        mail.setTo(to.split(";"));
        mail.setCc(new String[]{});
        mail.setCci(new String[]{});
        mail.setSubject(subject);
        mail.setText(text);
        try {
            json = mapper.writeValueAsString(mail);
        } catch (JsonProcessingException e) {
            log.error("Erreur lors du la création du mail. " + e);
        }
        return json;
    }

}
