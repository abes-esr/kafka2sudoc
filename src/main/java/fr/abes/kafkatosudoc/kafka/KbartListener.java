package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.cbs.notices.Zone;
import fr.abes.kafkatosudoc.dto.LigneKbartDto;
import fr.abes.kafkatosudoc.service.BaconService;
import fr.abes.kafkatosudoc.service.EmailService;
import fr.abes.kafkatosudoc.service.SudocService;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import fr.abes.kafkatosudoc.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KbartListener {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UtilsMapper utilsMapper;

    @Autowired
    private SudocService service;

    @Autowired
    private BaconService baconService;

    @Autowired
    private EmailService emailService;

    /**
     * Listener pour la modification de notices biblio bestPpn (ajout 469)
     * @param lignesKbart
     * @throws CBSException
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.toload}"}, groupId = "${topic.groupid.source.kbart}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToCreateFromKafka(ConsumerRecord<String, String> lignesKbart) throws CBSException {
        String filename = "";
        LigneKbartDto ligneKbartDto = new LigneKbartDto();
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            ligneKbartDto = mapper.readValue(lignesKbart.value(), LigneKbartDto.class);
            if (!ligneKbartDto.isBestPpnEmpty()) {
                service.authenticate();
                String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
                NoticeConcrete noticeBestPpn = service.getNoticeFromPpn(ligneKbartDto.getBestPpn());
                if (!service.isNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet)) {
                    service.addNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet);
                    service.sauvegarderNotice(noticeBestPpn);
                    log.debug("Ajout 469 : Notice " + ligneKbartDto.getBestPpn() + " modifiée avec succès");
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, ligneKbartDto, e);
        } finally {
            service.disconnect();
        }
    }

    /**
     * Listener pour modification notice biblio (suppression 469)
     * @param lignesKbart
     * @throws CBSException
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.todelete}"}, groupId = "lignesKbart", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToDeleteFromKafka(ConsumerRecord<String, String> lignesKbart) throws CBSException {
        String filename = "";
        LigneKbartDto ligneKbartDto = new LigneKbartDto();
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            ligneKbartDto = mapper.readValue(lignesKbart.value(), LigneKbartDto.class);
            if (!ligneKbartDto.isBestPpnEmpty()) {
                service.authenticate();
                String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
                NoticeConcrete noticeBestPpn = service.getNoticeFromPpn(ligneKbartDto.getBestPpn());
                if (service.isNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet)) {
                    service.supprimeNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet);
                    service.sauvegarderNotice(noticeBestPpn);
                    log.debug("Suppression 469 : Notice " + ligneKbartDto.getBestPpn() + " modifiée avec succès");
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, ligneKbartDto, e);
        }
        finally {
            service.disconnect();
        }
    }

    /**
     *
     * @param lignesKbart
     * @throws CBSException
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.exnihilo}"}, groupId = "lignesKbart", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaExNihilo(ConsumerRecord<String, String> lignesKbart) throws CBSException {
        LigneKbartDto ligneKbartDto = new LigneKbartDto();
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());

            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            ligneKbartDto = mapper.readValue(lignesKbart.value(), LigneKbartDto.class);
            service.authenticate();
            NoticeConcrete notice = utilsMapper.map(ligneKbartDto, NoticeConcrete.class);
            //Ajout provider display name en 214 $c 2è occurrence
            String providerDisplay = baconService.getProviderDisplayName(provider);
            if (providerDisplay != null) {
                notice.getNoticeBiblio().findZone("214", 1).addSubLabel("$c", providerDisplay);
            }
            //Ajout lien vers notice bouquet
            String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
            notice.getNoticeBiblio().addZone("469", "$0", ppnNoticeBouquet);
            service.sauvegarderNotice(notice);
            log.debug("Ajout notice exNihilo effectué");
        }
        catch (Exception e) {
                log.debug(e.getMessage(), e.getCause());
                emailService.sendErrorMail(filename, ligneKbartDto, e);
            }
        finally {
                service.disconnect();
            }
    }

    private String getFileNameFromHeader(Headers headers) {
        String filename = "";
        for (Header header : headers.toArray()) {
            if (header.key().equals("FileName")) {
                filename = new String(header.value());
            }
        }
        return filename;
    }
}
