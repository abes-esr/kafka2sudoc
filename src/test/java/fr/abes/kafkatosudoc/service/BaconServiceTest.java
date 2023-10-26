package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.dto.PackageKbartDto;
import fr.abes.kafkatosudoc.entity.Provider;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import fr.abes.kafkatosudoc.repository.LigneKbartRepository;
import fr.abes.kafkatosudoc.repository.ProviderPackageRepository;
import fr.abes.kafkatosudoc.repository.ProviderRepository;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.*;

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


    PackageKbartDto packageKbartDto;
    List<ProviderPackage> providerPackageList;



    @BeforeEach
    void init() {
        ProviderPackage providerPackage0 = new ProviderPackage("test1", new GregorianCalendar(2023, Calendar.AUGUST, 1).getTime(), 1, 'N');
        ProviderPackage providerPackage1 = new ProviderPackage("test2", new GregorianCalendar(2023, Calendar.OCTOBER, 2).getTime(), 2, 'N');
        ProviderPackage providerPackage2 = new ProviderPackage("test3", new GregorianCalendar(2023, Calendar.SEPTEMBER, 28).getTime(), 3, 'N');
        ProviderPackage providerPackage3 = new ProviderPackage("test4", new GregorianCalendar(2023, Calendar.APRIL, 27).getTime(), 4, 'N');
        ProviderPackage providerPackage4 = new ProviderPackage("test5", new GregorianCalendar(2023, Calendar.NOVEMBER, 16).getTime(), 5, 'N');
        Calendar date = new GregorianCalendar(2023, Calendar.OCTOBER, 1);
        packageKbartDto = new PackageKbartDto("name", date.getTime(), "provider");
        providerPackageList = new ArrayList<>();
        providerPackageList.add(providerPackage0);
        providerPackageList.add(providerPackage1);
        providerPackageList.add(providerPackage2);
        providerPackageList.add(providerPackage3);
        providerPackageList.add(providerPackage4);
    }

    @Test
    void testFindLastVersionOfPackage1() {
        Provider provider = new Provider("testProvider");
        Mockito.when(providerRepository.findByProvider("provider")).thenReturn(Optional.of(provider));
        Mockito.when(providerPackageRepository.findAllByProviderIdtProviderAndPackageName(Mockito.any(), Mockito.any()))
                .thenReturn(providerPackageList);

        ProviderPackage providerPackage = baconService.findLastVersionOfPackage(packageKbartDto);
        Assertions.assertEquals("test3", providerPackage.getPackageName());
        Assertions.assertEquals(3, providerPackage.getProviderIdtProvider());

    }

    @Test
    void testFindLastVersionOfPackage2() {
        Mockito.when(providerRepository.findByProvider(Mockito.anyString())).thenReturn(Optional.empty());
        Assertions.assertNull(baconService.findLastVersionOfPackage(packageKbartDto));
    }

    @Test
    void testFindLastVersionOfPackage3() {
        Provider provider = new Provider("testProvider");
        Mockito.when(providerRepository.findByProvider(Mockito.anyString())).thenReturn(Optional.of(provider));
        Mockito.when(providerPackageRepository.findAllByProviderIdtProviderAndPackageName(Mockito.any(), Mockito.any()))
                .thenReturn(Lists.newArrayList());
        Assertions.assertNull(baconService.findLastVersionOfPackage(packageKbartDto));
    }

}