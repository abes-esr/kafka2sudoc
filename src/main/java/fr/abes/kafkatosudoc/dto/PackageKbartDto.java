package fr.abes.kafkatosudoc.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PackageKbartDto {
    private String packageName;
    private Date datePackage;
    private String provider;
}
