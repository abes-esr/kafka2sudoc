package fr.abes.kafkatosudoc.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PackageKbartDtoKafka {
    private List<LigneKbartDto> kbartDtos;

    public PackageKbartDtoKafka() {
        this.kbartDtos = new ArrayList<>();
    }
}
