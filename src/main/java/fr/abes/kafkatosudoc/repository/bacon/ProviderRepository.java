package fr.abes.kafkatosudoc.repository.bacon;

import fr.abes.kafkatosudoc.configuration.BaconDbConfiguration;
import fr.abes.kafkatosudoc.entity.bacon.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@BaconDbConfiguration
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
    Optional<Provider> findByProvider(String provider);
}
