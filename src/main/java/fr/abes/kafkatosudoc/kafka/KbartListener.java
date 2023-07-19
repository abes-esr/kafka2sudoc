package fr.abes.kafkatosudoc.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.kafkatosudoc.dto.PackageKbartDtoKafka;
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
        try{
            PackageKbartDtoKafka packageFromKafka = mapper.readValue(lignesKbart.value(), PackageKbartDtoKafka.class);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }
}
