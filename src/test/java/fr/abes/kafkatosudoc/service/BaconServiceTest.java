package fr.abes.kafkatosudoc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.cbs.process.ProcessCBS;
import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackage;
import fr.abes.kafkatosudoc.entity.bacon.ProviderPackageId;
import fr.abes.kafkatosudoc.repository.bacon.LigneKbartRepository;
import fr.abes.kafkatosudoc.repository.bacon.ProviderPackageRepository;
import fr.abes.kafkatosudoc.repository.bacon.ProviderRepository;
import fr.abes.kafkatosudoc.utils.UtilsMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.support.PropertyComparator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {BaconService.class})
class BaconServiceTest {
    @Autowired
    BaconService baconService;
    @MockBean
    ProviderPackageRepository providerPackageRepository;
    @MockBean
    ProviderRepository providerRepository;
    @MockBean
    LigneKbartRepository ligneKbartRepository;
    @MockBean
    private UtilsMapper mapper;

    PackageKbartDto packageKbartDto;
    List<ProviderPackage> providerPackageList;
    ProviderPackage providerPackage0 = new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new Date(2023, 10, 01), 621), 'N');
    ProviderPackage providerPackage1 = new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new Date(2023, 10, 01), 621), 'N');
    ProviderPackage providerPackage2 = new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new Date(2023, 9, 28), 621), 'N');
    ProviderPackage providerPackage3 = new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new Date(2023, 9, 27), 621), 'N');
    ProviderPackage providerPackage4 = new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new Date(2023, 11, 16), 621), 'N');


    @BeforeEach
    void setUp() {
        packageKbartDto = new PackageKbartDto();
        providerPackageList = new ArrayList<>();
    }

    @Test
    void isAnteriorVersionExist() {
        packageKbartDto.setDatePackage(new Date(2023, 10, 01));
        providerPackageList.add(providerPackage0);
        providerPackageList.add(providerPackage1);
        providerPackageList.add(providerPackage2);
        providerPackageList.add(providerPackage3);
        providerPackageList.add(providerPackage4);

        assertTrue(baconService.isAnteriorVersionExist(Optional.of(providerPackageList), packageKbartDto.getDatePackage()));
    }

    @Test
    void isAnteriorVersionExist2() {
        //Même version à la fois dans le pckage issu du nom de fichier, et celui issu du retour de la requete hibernate
        packageKbartDto.setDatePackage(new GregorianCalendar(2023, Calendar.JULY, 1).getTime());
        packageKbartDto.setProvider("ISTEX");
        packageKbartDto.setPackageName("FRANCE_COLLEX");
        providerPackageList = new ArrayList<>();
        providerPackageList.add(new ProviderPackage(new ProviderPackageId("FRANCE_COLLEX", new GregorianCalendar(2023, Calendar.JULY, 1).getTime(), 621), 'N'));

        assertTrue(baconService.isAnteriorVersionExist(Optional.of(providerPackageList), packageKbartDto.getDatePackage()));
    }

    @Test
    void sortingProviderPackageByProviderPackageIdDate(){
        providerPackageList.add(providerPackage0);
        providerPackageList.add(providerPackage1);
        providerPackageList.add(providerPackage2);
        providerPackageList.add(providerPackage3);
        providerPackageList.add(providerPackage4);

        //Trie la collection d'objets ProviderPackage à partir du champ DATE_P de l'attribut ProviderPackageId
        Collections.sort(providerPackageList);
        //La liste d'objet ProviderPackage est triée du moins récent au plus récent
        assertTrue(providerPackageList.get(0).getProviderPackageId().getDateP().getTime() < providerPackageList.get(4).getProviderPackageId().getDateP().getTime());
    }

}