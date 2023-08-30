package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.entity.bacon.ProviderPackage;
import fr.abes.kafkatosudoc.repository.bacon.ProviderPackageRepository;
import fr.abes.kafkatosudoc.repository.bacon.ProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BaconService {
    private final ProviderPackageRepository providerPackageRepository;
    private final ProviderRepository providerRepository;

    public BaconService(ProviderPackageRepository providerPackageRepository, ProviderRepository providerRepository) {
        this.providerPackageRepository = providerPackageRepository;
        this.providerRepository = providerRepository;
    }


}
