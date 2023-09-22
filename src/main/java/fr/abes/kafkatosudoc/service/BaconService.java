package fr.abes.kafkatosudoc.service;

import fr.abes.kafkatosudoc.entity.Provider;
import fr.abes.kafkatosudoc.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BaconService {
    private final ProviderRepository providerRepository;

    public BaconService(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }


    public String getProviderDisplayName(String provider) {
        Optional<Provider> providerOpt = providerRepository.findByProvider(provider);
        if (providerOpt.isPresent()) {
            return providerOpt.get().getDisplayName();
        }
        return null;
    }
}
