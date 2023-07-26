package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.Biblio;
import fr.abes.cbs.notices.Zone;
import fr.abes.kafkatosudoc.dto.LigneKbartDto;
import fr.abes.kafkatosudoc.dto.PackageKbartDtoKafka;
import fr.abes.kafkatosudoc.service.EmailService;
import fr.abes.kafkatosudoc.service.SudocService;
import fr.abes.kafkatosudoc.utils.CheckFiles;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KbartListener {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private SudocService service;

    @Autowired
    private EmailService emailService;

    @KafkaListener(topics = {"${topic.name.target.kbart}"}, groupId = "${topic.groupid.target.kbart}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafka(ConsumerRecord<String, String> lignesKbart){
        String filename = "";
        LigneKbartDto ligneKbartDto = new LigneKbartDto();
        try {
            for (Header header : lignesKbart.headers().toArray()) {
                if(header.key().equals("FileName")){
                    filename = new String(header.value());
                }
            }
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            ligneKbartDto = mapper.readValue(lignesKbart.value(), LigneKbartDto.class);
            if (!ligneKbartDto.isBestPpnEmpty()) {
                service.authenticate();
                String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
                Biblio noticeBestPpn = service.getNoticeFromPpn(ligneKbartDto.getBestPpn());
                if (!service.isNoticeBouquetInBestPpn(noticeBestPpn, ppnNoticeBouquet)) {
                    service.addNoticeBouquetInBestPpn(noticeBestPpn, ppnNoticeBouquet);
                    service.sauvegarderNotice(noticeBestPpn);
                    log.debug("Notice " + ligneKbartDto.getBestPpn() + " modifiée avec succès");
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, ligneKbartDto, e);
        }
    }
}
