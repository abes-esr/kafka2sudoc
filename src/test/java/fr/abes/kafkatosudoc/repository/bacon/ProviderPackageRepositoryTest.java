package fr.abes.kafkatosudoc.repository.bacon;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
class ProviderPackageRepositoryTest {
    @Autowired
    ProviderPackageRepository providerPackageRepository;

    @BeforeEach
    void setUp() {
    }

    @Test
    void findAllByProviderPackageId_DatePAndProviderPackageId_ProviderIdtProviderAndProviderPackageId_PackageName() {

    }
}