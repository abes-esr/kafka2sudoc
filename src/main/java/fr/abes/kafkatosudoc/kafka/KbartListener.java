package fr.abes.kafkatosudoc.kafka;

import fr.abes.cbs.exception.CBSException;
import fr.abes.cbs.exception.ZoneException;
import fr.abes.cbs.notices.NoticeConcrete;
import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.dto.connect.LigneKbartConnect;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackage;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
@Slf4j
public class KbartListener {
    @Autowired
    private UtilsMapper mapper;

    @Autowired
    private SudocService service;

    @Autowired
    private BaconService baconService;

    @Autowired
    private EmailService emailService;

    private final List<String> listeNotices = new ArrayList<>();

    /**
     * Listener pour la modification de notices biblio bestPpn (ajout 469)
     *
     * @param lignesKbart : ligne trouvée dans kafka
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.toload}"}, groupId = "${topic.groupid.source.kbart}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToCreateFromKafka(ConsumerRecord<String, LigneKbartConnect> lignesKbart) {
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            PackageKbartDto packageKbartDto = new PackageKbartDto(packageName, Calendar.getInstance().getTime(), provider);
            if (lignesKbart.value().equals("OK")) {
                traiterPackageDansSudoc(listeNotices, packageKbartDto);
            } else {
                if (!lignesKbart.value().getBESTPPN().isEmpty()) {
                    //on alimente la liste des notices d'un package qui sera traitée intégralement
                    listeNotices.add(lignesKbart.value().getBESTPPN().toString());
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, lignesKbart.value(), e);
        }
    }

    private void traiterPackageDansSudoc(List<String> listeNotices, PackageKbartDto packageKbartDto) throws CBSException, ZoneException {
        List<NoticeConcrete> noticeWithNewBestPpn = new ArrayList<>();
        List<NoticeConcrete> noticeWithDeletedBestPpn = new ArrayList<>();

        ProviderPackage lastProvider = baconService.findLastVersionOfPackage(packageKbartDto);

        service.authenticate();
        String ppnNoticeBouquet = service.getNoticeBouquet(packageKbartDto.getProvider(), packageKbartDto.getPackageName());
        //cas ou on a une version antérieure de package
        if (lastProvider != null) {
            List<String> ppnLastVersion = baconService.findAllPpnFromPackage(lastProvider);
            for (String ppn : ppnLastVersion) {
                if (!listeNotices.contains(ppn))
                    noticeWithDeletedBestPpn.add(service.getNoticeFromPpn(ppn));
            }
            for (String ppn : listeNotices) {
                if (!ppnLastVersion.contains(ppn))
                    noticeWithNewBestPpn.add(service.getNoticeFromPpn(ppn));
            }

        } else {
            //pas de version antérieure, tous les bestPpn sont nouveaux
            for (String ppn : listeNotices) {
                noticeWithNewBestPpn.add(service.getNoticeFromPpn(ppn));
            }
        }

        for (NoticeConcrete notice : noticeWithNewBestPpn) {
            if (!service.isNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.addNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice);
                log.debug("Ajout 469 : Notice " + notice.getNoticeBiblio().findZone("001", 0).getValeur() + " modifiée avec succès");
            }
        }

        for (NoticeConcrete notice : noticeWithDeletedBestPpn) {
            if (service.isNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet)) {
                service.supprimeNoticeBouquetInBestPpn(notice.getNoticeBiblio(), ppnNoticeBouquet);
                service.modifierNotice(notice);
                log.debug("Suppression 469 : Notice " + notice.getNoticeBiblio().findZone("001", 0).getValeur() + " modifiée avec succès");
            }
        }
        service.disconnect();


    }

    /**
     * Listener pour modification notice biblio (suppression 469)
     *
     * @param lignesKbart enregistrement dans kafka
     * @throws CBSException : erreur CBS
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.todelete}"}, groupId = "${topic.groupid.source.kbart}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartToDeleteFromKafka(ConsumerRecord<String, LigneKbartConnect> lignesKbart) throws CBSException {
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());
            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            if (!lignesKbart.value().getBESTPPN().isEmpty()) {
                service.authenticate();
                String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
                NoticeConcrete noticeBestPpn = service.getNoticeFromPpn(lignesKbart.value().getBESTPPN().toString());
                if (service.isNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet)) {
                    service.supprimeNoticeBouquetInBestPpn(noticeBestPpn.getNoticeBiblio(), ppnNoticeBouquet);
                    service.modifierNotice(noticeBestPpn);
                    log.debug("Suppression 469 : Notice " + lignesKbart.value().getBESTPPN().toString() + " modifiée avec succès");
                }
            }
        } catch (Exception e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, lignesKbart.value(), e);
        } finally {
            service.disconnect();
        }
    }

    /**
     * @param lignesKbart : enregistrement dans Kafka
     * @throws CBSException : erreur CBS
     */
    @KafkaListener(topics = {"${topic.name.source.kbart.exnihilo}"}, groupId = "${topic.groupid.source.kbart}Local", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafkaExNihilo(ConsumerRecord<String, LigneKbartConnect> lignesKbart) throws CBSException {
        String filename = "";
        try {
            filename = getFileNameFromHeader(lignesKbart.headers());

            String provider = CheckFiles.getProviderFromFilename(filename);
            String packageName = CheckFiles.getPackageFromFilename(filename);
            service.authenticate();
            LigneKbartConnect ligne = LigneKbartConnect.newBuilder()
                    .setPUBLICATIONTITLE(lignesKbart.value().getPUBLICATIONTITLE())
                    .setPRINTIDENTIFIER(lignesKbart.value().getPRINTIDENTIFIER())
                    .setONLINEIDENTIFIER(lignesKbart.value().getONLINEIDENTIFIER())
                    .setDATEFIRSTISSUEONLINE(lignesKbart.value().getDATEFIRSTISSUEONLINE())
                    .setNUMFIRSTVOLONLINE(lignesKbart.value().getNUMFIRSTVOLONLINE())
                    .setNUMFIRSTISSUEONLINE(lignesKbart.value().getNUMFIRSTISSUEONLINE())
                    .setDATELASTISSUEONLINE(lignesKbart.value().getDATELASTISSUEONLINE())
                    .setNUMLASTVOLONLINE(lignesKbart.value().getNUMLASTVOLONLINE())
                    .setDATELASTISSUEONLINE(lignesKbart.value().getDATELASTISSUEONLINE())
                    .setTITLEURL(lignesKbart.value().getTITLEURL())
                    .setFIRSTAUTHOR(lignesKbart.value().getFIRSTAUTHOR())
                    .setTITLEID(lignesKbart.value().getTITLEID())
                    .setEMBARGOINFO(lignesKbart.value().getEMBARGOINFO())
                    .setNOTES(lignesKbart.value().getNOTES())
                    .setPUBLISHERNAME(lignesKbart.value().getPUBLISHERNAME())
                    .setPUBLICATIONTYPE(lignesKbart.value().getPUBLICATIONTYPE())
                    .setDATEMONOGRAPHPUBLISHEDPRINT(lignesKbart.value().getDATEMONOGRAPHPUBLISHEDPRINT())
                    .setDATEMONOGRAPHPUBLISHEDPRINT(lignesKbart.value().getDATEMONOGRAPHPUBLISHEDPRINT())
                    .setMONOGRAPHVOLUME(lignesKbart.value().getMONOGRAPHVOLUME())
                    .setMONOGRAPHEDITION(lignesKbart.value().getMONOGRAPHEDITION())
                    .setFIRSTEDITOR(lignesKbart.value().getFIRSTEDITOR())
                    .setPARENTPUBLICATIONTITLEID(lignesKbart.value().getPARENTPUBLICATIONTITLEID())
                    .setPRECEDINGPUBLICATIONTITLEID(lignesKbart.value().getPRECEDINGPUBLICATIONTITLEID())
                    .setACCESSTYPE(lignesKbart.value().getACCESSTYPE())
                    .setPROVIDERPACKAGEPACKAGE(lignesKbart.value().getPROVIDERPACKAGEPACKAGE())
                    .setPROVIDERPACKAGEDATEP(lignesKbart.value().getPROVIDERPACKAGEDATEP())
                    .setPROVIDERPACKAGEIDTPROVIDER(lignesKbart.value().getPROVIDERPACKAGEIDTPROVIDER())
                    .setBESTPPN(lignesKbart.value().getBESTPPN())
                    .build();
            NoticeConcrete notice = mapper.map(ligne, NoticeConcrete.class);
            //Ajout provider display name en 214 $c 2è occurrence
            String providerDisplay = baconService.getProviderDisplayName(provider);
            if (providerDisplay != null) {
                notice.getNoticeBiblio().findZone("214", 1).addSubLabel("$c", providerDisplay);
            }
            //Ajout lien vers notice bouquet
            String ppnNoticeBouquet = service.getNoticeBouquet(provider, packageName);
            notice.getNoticeBiblio().addZone("469", "$0", ppnNoticeBouquet);
            service.creerNotice(notice);
            log.debug("Ajout notice exNihilo effectué");
        } catch (CBSException | ZoneException e) {
            log.debug(e.getMessage(), e.getCause());
            emailService.sendErrorMail(filename, lignesKbart.value(), e);
        } finally {
            service.disconnect();
        }
    }

    private String getFileNameFromHeader(Headers headers) {
        String filename = "";
        for (Header header : headers.toArray()) {
            if (header.key().equals("filename")) {
                filename = new String(header.value());
            }
        }
        return filename;
    }
}
