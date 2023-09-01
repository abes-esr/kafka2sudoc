package fr.abes.kafkatosudoc.kafka;

import fr.abes.kafkatosudoc.service.BaconService;
import fr.abes.kafkatosudoc.service.SudocService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {BaconService.class, SudocService.class})
class KbartListenerTest {
    @Autowired
    BaconService baconService;
    @Autowired
    SudocService sudocService;

    ConsumerRecord<String, String> unEnregistrementKafka;

    @BeforeEach
    void init() {
        this.unEnregistrementKafka = new ConsumerRecord<>(
                "bacon.kbart.withppn.toload",
                0,
                22,
                "",
                "{\"publication_title\":\"Library Hi Tech News\",\"print_identifier\":\"0741-9058\",\"online_identifier\":\"2054-1678\",\"date_first_issue_online\":\"1999\",\"num_first_vol_online\":null,\"num_first_issue_online\":null,\"date_last_issue_online\":\"\",\"num_last_vol_online\":null,\"num_last_issue_online\":null,\"title_url\":\"https://www.emerald.com/insight/publication/issn/0741-9058\",\"first_author\":\"\",\"title_id\":\"lhtn\",\"embargo_info\":\"\",\"coverage_depth\":\"fulltext\",\"notes\":\"\",\"publisher_name\":\"Emerald\",\"publication_type\":\"serial\",\"date_monograph_published_print\":\"\",\"date_monograph_published_online\":\"\",\"monograph_volume\":null,\"monograph_edition\":\"\",\"first_editor\":\"\",\"parent_publication_title_id\":\"\",\"preceding_publication_title_id\":\"\",\"access_type\":\"P\",\"bestPpn\":\"175787220\"}"
        );
        unEnregistrementKafka.headers().add("CurrentLine", "24".getBytes());
        unEnregistrementKafka.headers().add("FileName", "EMERALD_GLOBAL_111_2023-01-23.tsv".getBytes());
    }

    @Test
    void listenKbartFromKafka() {
        System.out.println(unEnregistrementKafka);
    }
}