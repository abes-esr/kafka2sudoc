package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.dto.PackageKbartDtoKafka;
import fr.abes.kafkatosudoc.exception.IllegalDateException;
import fr.abes.kafkatosudoc.exception.IllegalPackageException;
import fr.abes.kafkatosudoc.exception.IllegalProviderException;
import fr.abes.kafkatosudoc.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KbartListener {
    @Autowired
    private ObjectMapper mapper;

    @KafkaListener(topics = {"${topic.name.target.kbart}"}, groupId = "${topic.groupid.target.kbart}", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafka(ConsumerRecord<String, String> lignesKbart){
        PackageKbartDto packageKbartDto = new PackageKbartDto();
        try{
            packageKbartDto.setPackageName(Utils.extractPackageName(lignesKbart.key()));
            packageKbartDto.setProvider(Utils.extractProvider(lignesKbart.key()));
            packageKbartDto.setDatePackage(Utils.extractDate(lignesKbart.key()));
            PackageKbartDtoKafka packageFromKafka = mapper.readValue(lignesKbart.value(), PackageKbartDtoKafka.class);
            packageKbartDto.setLigneKbartDtos(packageFromKafka.getKbartDtos());
        } catch (IllegalPackageException | IllegalProviderException | IllegalDateException e) {
            log.error("Erreur dans les données en entrée, provider / nom de package ou format de date incorrect");
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
