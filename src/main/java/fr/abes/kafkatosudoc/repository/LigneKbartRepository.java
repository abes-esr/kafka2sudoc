package fr.abes.kafkatosudoc.repository;

import fr.abes.kafkatosudoc.configuration.BaconDbConfiguration;
import fr.abes.kafkatosudoc.entity.LigneKbart;
import fr.abes.kafkatosudoc.entity.ProviderPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@BaconDbConfiguration
public interface LigneKbartRepository extends JpaRepository<LigneKbart, Integer> {
    List<LigneKbart> findAllByProviderPackage(ProviderPackage providerPackage);
}
