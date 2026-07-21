# SOA-503 — Rapports d'erreurs Sudoc XLSX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produire `ErreursInsertion469.xlsx` et `ErreursCreations.xlsx`, les alimenter cumulativement avec les données KBART associées et les joindre aux emails existants.

**Architecture:** Un `ErrorWorkbookService` dédié transforme les erreurs et les lignes KBART en lignes XLSX, puis écrit les classeurs de manière synchronisée et atomique. `EmailService` filtre les erreurs, délègue la génération au nouveau service et conserve la responsabilité de l'envoi des pièces jointes.

**Tech Stack:** Java 21, Spring Boot 3.5, Maven, Apache POI 5.2.5, JUnit 5, Mockito, LibreOffice Calc.

## Global Constraints

- Les fichiers produits sont exactement `ErreursInsertion469.xlsx` et `ErreursCreations.xlsx`.
- Les TXT historiques ne sont ni migrés, ni modifiés, ni supprimés.
- Les colonnes sont exactement : `PPN`, `Commande WinIBW`, `Bouquet`, `Erreur`, `Titre`, `ISSN imprimé`, `ISSN en ligne`.
- Toutes les cellules de données sont des chaînes avec le format Excel `@`.
- Une erreur sans PPN conserve Bouquet et Erreur ; les cinq autres cellules restent vides.
- Les deux flux de création alimentent le même `ErreursCreations.xlsx`.
- Les emails conservent leur sujet, leur corps et leur fonctionnement en pièce jointe.
- Le contenu des notices Sudoc n'est pas ajouté aux classeurs.
- Les nouveaux classeurs sont cumulatifs, synchronisés et remplacés atomiquement.
- Java reste en version 21.

---

## File Map

- Create `src/main/java/fr/abes/kafkatosudoc/service/ErrorWorkbookService.java`: extraction, association KBART et persistance XLSX.
- Create `src/test/java/fr/abes/kafkatosudoc/service/ErrorWorkbookServiceTest.java`: comportement complet des deux classeurs.
- Modify `src/main/java/fr/abes/kafkatosudoc/service/EmailService.java`: délégation au service XLSX et envoi des nouvelles pièces jointes.
- Modify `src/test/java/fr/abes/kafkatosudoc/service/EmailServiceTest.java`: tests d'intégration des trois flux d'email et maintien des tests d'extraction.
- Modify `pom.xml`: dépendance Apache POI 5.2.5.

---

### Task 1: Créer le rapport XLSX des erreurs 469

**Files:**
- Modify: `pom.xml`
- Create: `src/main/java/fr/abes/kafkatosudoc/service/ErrorWorkbookService.java`
- Create: `src/test/java/fr/abes/kafkatosudoc/service/ErrorWorkbookServiceTest.java`

**Interfaces:**
- Consumes: `ErrorMessage`, `LigneKbartConnect`, `CheckFiles`.
- Produces:
  - `ErrorWorkbookService(String pathToErrors)`
  - `boolean appendInsertionErrors(String filename, List<ErrorMessage> errors, List<LigneKbartConnect> notices)`
  - `Path insertionWorkbookPath()`

- [ ] **Step 1: Ajouter Apache POI et écrire le premier test en échec**

Ajouter dans `pom.xml` :

```xml
<!-- Génération des rapports XLSX -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

Créer `ErrorWorkbookServiceTest` avec ce premier scénario :

```java
@Test
void createsInsertionWorkbookWithKbartData() throws IOException {
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
    LigneKbartConnect notice = connectNotice(
            "262071878", "Titre électronique", "1234-5678", "8765-4321");
    ErrorMessage error = new ErrorMessage(
            ERROR_TYPE.ADD469,
            "{PPN:262071878,Erreur:Zone 469 invalide,Ligne Kbart:...,Notice:...}");

    assertTrue(service.appendInsertionErrors(
            "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
            List.of(error),
            List.of(notice)));

    try (Workbook workbook = WorkbookFactory.create(
            service.insertionWorkbookPath().toFile())) {
        Sheet sheet = workbook.getSheet("ErreursInsertion469");
        assertEquals(List.of(
                "PPN", "Commande WinIBW", "Bouquet", "Erreur",
                "Titre", "ISSN imprimé", "ISSN en ligne"),
                rowValues(sheet.getRow(0)));
        assertEquals(List.of(
                "262071878",
                "che ppn 262071878",
                "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
                "Zone 469 invalide",
                "Titre électronique",
                "1234-5678",
                "8765-4321"),
                rowValues(sheet.getRow(1)));
    }
}
```

Le helper construit une vraie ligne Avro :

```java
private LigneKbartConnect connectNotice(
        String ppn, String title, String printIssn, String onlineIssn) {
    LigneKbartConnect notice = new LigneKbartConnect();
    notice.setBESTPPN(ppn);
    notice.setPUBLICATIONTITLE(title);
    notice.setPRINTIDENTIFIER(printIssn);
    notice.setONLINEIDENTIFIER(onlineIssn);
    return notice;
}
```

- [ ] **Step 2: Vérifier l'échec attendu**

Run:

```powershell
mvn -Dtest=ErrorWorkbookServiceTest test
```

Expected: compilation en échec car `ErrorWorkbookService` n'existe pas.

- [ ] **Step 3: Implémenter le minimum pour créer le premier classeur**

Créer le service Spring avec ces constantes et cette API :

```java
@Service
@Slf4j
public class ErrorWorkbookService {
    static final String INSERTION_FILENAME = "ErreursInsertion469.xlsx";
    static final String CREATION_FILENAME = "ErreursCreations.xlsx";
    private static final List<String> HEADERS = List.of(
            "PPN", "Commande WinIBW", "Bouquet", "Erreur",
            "Titre", "ISSN imprimé", "ISSN en ligne");

    private final Path outputDirectory;

    public ErrorWorkbookService(
            @Value("${abes.pathToErrors:tempLog/}") String pathToErrors) {
        this.outputDirectory = Path.of(pathToErrors);
    }

    public synchronized boolean appendInsertionErrors(
            String filename,
            List<ErrorMessage> errors,
            List<LigneKbartConnect> notices) throws IOException {
        List<ErrorWorkbookRow> rows = errors.stream()
                .map(error -> connectRow(filename, error, notices))
                .toList();
        return append(rows, insertionWorkbookPath(), "ErreursInsertion469");
    }

    public Path insertionWorkbookPath() {
        return outputDirectory.resolve(INSERTION_FILENAME);
    }
}
```

`connectRow` extrait le PPN et l'erreur, recherche `BEST_PPN`, puis renseigne les sept valeurs. `append` crée un `XSSFWorkbook`, la feuille, l'en-tête et les lignes. Pour ce premier cycle, il suffit de produire un nouveau fichier valide ; l'ajout cumulatif et l'écriture atomique arrivent dans la tâche 2.

- [ ] **Step 4: Vérifier le passage au vert**

Run:

```powershell
mvn -Dtest=ErrorWorkbookServiceTest test
```

Expected: 1 test, 0 échec.

- [ ] **Step 5: Commit**

```powershell
git add pom.xml src/main/java/fr/abes/kafkatosudoc/service/ErrorWorkbookService.java src/test/java/fr/abes/kafkatosudoc/service/ErrorWorkbookServiceTest.java
git -c user.name="Jerome Villiseck" -c user.email="jvk@abes.fr" commit -m "Ajouter le rapport XLSX des erreurs 469"
```

---

### Task 2: Compléter les deux classeurs cumulatifs

**Files:**
- Modify: `src/main/java/fr/abes/kafkatosudoc/service/ErrorWorkbookService.java`
- Modify: `src/test/java/fr/abes/kafkatosudoc/service/ErrorWorkbookServiceTest.java`

**Interfaces:**
- Consumes: API de la tâche 1.
- Produces:
  - `boolean appendCreationErrors(String filename, List<ErrorMessage> errors, List<LigneKbartConnect> notices)`
  - `boolean appendCreationErrorsFromPrint(String filename, List<ErrorMessage> errors, List<LigneKbartImprime> notices)`
  - `Path creationWorkbookPath()`

- [ ] **Step 1: Écrire les tests en échec pour les comportements restants**

Ajouter des tests distincts :

```java
@Test
void appendsRowsAndPreservesHistoricalTxt() throws IOException {
    Path txt = tempDir.resolve("ErreursInsertion469.txt");
    Files.writeString(txt, "historique");
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

    service.appendInsertionErrors("FIRST_PACKAGE_2025-11-02.tsv",
            List.of(error469("111111111", "Erreur 1")),
            List.of(connectNotice("111111111", "Titre 1", "1111-1111", "2222-2222")));
    service.appendInsertionErrors("SECOND_PACKAGE_2025-11-03.tsv",
            List.of(error469("222222222", "Erreur 2")),
            List.of(connectNotice("222222222", "Titre 2", "3333-3333", "4444-4444")));

    try (Workbook workbook = WorkbookFactory.create(
            service.insertionWorkbookPath().toFile())) {
        assertEquals(2, workbook.getSheetAt(0).getLastRowNum());
    }
    assertEquals("historique", Files.readString(txt));
}
```

```java
@Test
void createsCreationWorkbookFromConnectNotice() throws IOException {
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
    ErrorMessage error = new ErrorMessage(
            ERROR_TYPE.EXNIHILO, "{Ppn : 115575375, Erreur : Création impossible}");

    assertTrue(service.appendCreationErrors(
            "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
            List.of(error),
            List.of(connectNotice("115575375", "Titre ex nihilo", "1111-2222", "3333-4444"))));

    assertCreationRow(service.creationWorkbookPath(),
            List.of("115575375", "che ppn 115575375",
                    "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02", "Création impossible",
                    "Titre ex nihilo", "1111-2222", "3333-4444"));
}
```

```java
@Test
void createsCreationWorkbookFromPrintedNotice() throws IOException {
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
    LigneKbartImprime notice = printedNotice(
            "262071878", "Titre imprimé", "5555-6666", "7777-8888");
    ErrorMessage error = new ErrorMessage(
            ERROR_TYPE.FROMIMPRIME,
            "{Ppn : 262071878, Erreur : Dérivation impossible}");

    assertTrue(service.appendCreationErrorsFromPrint(
            "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
            List.of(error),
            List.of(notice)));

    assertCreationRow(service.creationWorkbookPath(),
            List.of("262071878", "che ppn 262071878",
                    "CAIRN_GLOBAL_ALLEBOOKS_2025-11-02", "Dérivation impossible",
                    "Titre imprimé", "5555-6666", "7777-8888"));
}
```

```java
@Test
void keepsUnavailableKbartFieldsEmptyAndDoesNotCreateForEmptyList() throws IOException {
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());
    ErrorMessage globalError = new ErrorMessage(
            ERROR_TYPE.CONNEXION, "Erreur : Connexion CBS impossible");

    assertTrue(service.appendInsertionErrors(
            "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02.tsv",
            List.of(globalError),
            List.of()));
    assertEquals(List.of(
            "", "", "JSTOR_GLOBAL_ALLEBOOKS_2025-11-02",
            "Connexion CBS impossible", "", "", ""),
            workbookRow(service.insertionWorkbookPath(), 1));

    Path emptyDir = tempDir.resolve("empty");
    ErrorWorkbookService emptyService = new ErrorWorkbookService(emptyDir.toString());
    assertFalse(emptyService.appendInsertionErrors("EMPTY.tsv", List.of(), List.of()));
    assertFalse(Files.exists(emptyService.insertionWorkbookPath()));
}
```

Ajouter aussi un test vérifiant pour chaque cellule de données
`CellType.STRING`, le format `@`, l'en-tête bleu/blanc, le filtre et le gel de
la première ligne.

Déplacer dans `ErrorWorkbookServiceTest` les scénarios existants qui couvrent
les quatre formats de PPN et d'erreur :

```java
@ParameterizedTest
@CsvSource(delimiter = '|', value = {
        "{Ppn : 115575375, Erreur : Création impossible}|115575375|Création impossible",
        "{Ppn : 262071878, Erreur : Dérivation impossible, Notice : 008 $aOax3}|262071878|Dérivation impossible",
        "{PPN:262071878,Erreur:Zone 469 invalide,Ligne Kbart:...,Notice:...}|262071878|Zone 469 invalide",
        "Erreur : CBS connection failed|''|CBS connection failed"
})
void extractsPpnAndErrorFromEverySupportedFormat(
        String message, String expectedPpn, String expectedError) {
    ErrorWorkbookService service = new ErrorWorkbookService(tempDir.toString());

    assertEquals(expectedPpn, service.extractPpn(message));
    assertEquals(expectedError, service.extractError(message));
}
```

Les deux méthodes d'extraction restent package-private afin d'être testées
sans exposer une nouvelle API publique.

- [ ] **Step 2: Vérifier les échecs attendus**

Run:

```powershell
mvn -Dtest=ErrorWorkbookServiceTest test
```

Expected: échecs de compilation pour les deux nouvelles méthodes, puis échecs fonctionnels tant que l'ajout cumulatif et la mise en forme ne sont pas présents.

- [ ] **Step 3: Implémenter les mappings et l'écriture sûre**

Ajouter les deux méthodes publiques :

```java
public synchronized boolean appendCreationErrors(
        String filename,
        List<ErrorMessage> errors,
        List<LigneKbartConnect> notices) throws IOException {
    return append(
            errors.stream().map(error -> connectRow(filename, error, notices)).toList(),
            creationWorkbookPath(),
            "ErreursCreations");
}

public synchronized boolean appendCreationErrorsFromPrint(
        String filename,
        List<ErrorMessage> errors,
        List<LigneKbartImprime> notices) throws IOException {
    return append(
            errors.stream().map(error -> printedRow(filename, error, notices)).toList(),
            creationWorkbookPath(),
            "ErreursCreations");
}
```

Le mapping imprimé recherche `notice.getPpn()`. Le mapping connecté recherche
`notice.getBESTPPN()`. Les valeurs `null` deviennent `""`.

Remplacer l'écriture directe par :

```java
private boolean append(
        List<ErrorWorkbookRow> rows,
        Path workbookPath,
        String sheetName) throws IOException {
    if (rows.isEmpty()) {
        return false;
    }
    Files.createDirectories(workbookPath.getParent());
    Path temporaryPath = workbookPath.resolveSibling(
            workbookPath.getFileName() + ".tmp");
    try (Workbook workbook = Files.exists(workbookPath)
            ? WorkbookFactory.create(workbookPath.toFile())
            : new XSSFWorkbook()) {
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            sheet = createSheet(workbook, sheetName);
        }
        CellStyle textStyle = textStyle(workbook);
        int rowIndex = sheet.getLastRowNum() + 1;
        for (ErrorWorkbookRow row : rows) {
            writeRow(sheet.createRow(rowIndex++), row.values(), textStyle);
        }
        updateAutoFilter(sheet);
        try (OutputStream output = Files.newOutputStream(temporaryPath)) {
            workbook.write(output);
        }
    }
    replaceAtomically(temporaryPath, workbookPath);
    return true;
}
```

`replaceAtomically` tente `ATOMIC_MOVE` avec `REPLACE_EXISTING`, puis reprend
avec `REPLACE_EXISTING` sur `AtomicMoveNotSupportedException`.

- [ ] **Step 4: Vérifier le passage au vert**

Run:

```powershell
mvn -Dtest=ErrorWorkbookServiceTest test
```

Expected: tous les tests du service XLSX passent.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/fr/abes/kafkatosudoc/service/ErrorWorkbookService.java src/test/java/fr/abes/kafkatosudoc/service/ErrorWorkbookServiceTest.java
git -c user.name="Jerome Villiseck" -c user.email="jvk@abes.fr" commit -m "Générer les rapports XLSX des erreurs Sudoc"
```

---

### Task 3: Joindre les classeurs aux trois emails

**Files:**
- Modify: `src/main/java/fr/abes/kafkatosudoc/service/EmailService.java`
- Modify: `src/test/java/fr/abes/kafkatosudoc/service/EmailServiceTest.java`

**Interfaces:**
- Consumes: les trois méthodes d'ajout et les deux chemins exposés par `ErrorWorkbookService`.
- Produces: les mêmes trois méthodes publiques d'email qu'avant, avec pièces jointes XLSX.

- [ ] **Step 1: Remplacer les anciens tests TSV par des tests d'intégration en échec**

Conserver les tests `extractPpn` et `extractErreur`. Retirer les tests de
`formatErrorLine`, `appendErrorsToFile`, `extractPpn` et `extractErreur`,
désormais couverts par `ErrorWorkbookServiceTest`.

Construire un service capturant le nom de la pièce jointe :

```java
private static class CapturingEmailService extends EmailService {
    private File attachment;

    CapturingEmailService(ErrorWorkbookService workbookService) {
        super(workbookService);
    }

    @Override
    protected void sendMailWithFile(String requestJson, File file) {
        this.attachment = file;
    }
}
```

Ajouter trois tests :

```java
@Test
void attachesInsertionWorkbookFor469Errors() throws IOException {
    WorkInProgress<LigneKbartConnect> work = connectWorkInProgress();
    work.addErrorMessages469(
            "262071878", work.getListeNotices().get(0), "",
            "Zone 469 invalide", ERROR_TYPE.ADD469);

    emailService.sendErrorsMessageCreateFromKafka(FILENAME, work);

    assertEquals("ErreursInsertion469.xlsx", emailService.attachment.getName());
    assertTrue(emailService.attachment.exists());
}
```

```java
@Test
void attachesCreationWorkbookForExNihiloErrors() throws IOException {
    WorkInProgress<LigneKbartConnect> work = connectWorkInProgress();
    work.addErrorMessageExNihilo("262071878", "Création impossible");

    emailService.sendErrorMessagesExNihilo(FILENAME, work);

    assertEquals("ErreursCreations.xlsx", emailService.attachment.getName());
}
```

```java
@Test
void attachesCreationWorkbookForPrintedErrors() throws IOException {
    WorkInProgress<LigneKbartImprime> work = printedWorkInProgress();
    work.addErrorMessagesImprime("262071878", "Dérivation impossible");

    emailService.sendErrorMessagesImprime(FILENAME, work);

    assertEquals("ErreursCreations.xlsx", emailService.attachment.getName());
}
```

- [ ] **Step 2: Vérifier les échecs attendus**

Run:

```powershell
mvn -Dtest=EmailServiceTest test
```

Expected: compilation en échec car le constructeur injectant
`ErrorWorkbookService` n'existe pas et les emails utilisent encore les TXT.

- [ ] **Step 3: Injecter le service et remplacer le flux TSV**

Ajouter l'injection :

```java
private final ErrorWorkbookService errorWorkbookService;

public EmailService(ErrorWorkbookService errorWorkbookService) {
    this.errorWorkbookService = errorWorkbookService;
}
```

Dans chaque méthode publique :

```java
if (errorWorkbookService.appendInsertionErrors(
        filename, errors, workInProgress.getListeNotices())) {
    sendErrorsEmailWithAttachment(
            filename,
            errorWorkbookService.insertionWorkbookPath(),
            SUBJECT_ERROR_LIEN_BOUQUET,
            errors.size());
}
```

Utiliser `appendCreationErrors` pour ex nihilo et
`appendCreationErrorsFromPrint` pour le flux imprimé.

Modifier l'envoi privé pour recevoir directement un `Path` :

```java
private void sendErrorsEmailWithAttachment(
        String filename, Path filePath, String subject, int nbErrors)
        throws IOException {
    if (!Files.exists(filePath)) {
        log.warn("Fichier d'erreurs non trouvé : {}", filePath);
        return;
    }
    String requestJson = mailToJSON(
            recipient,
            subject + getTag() + " " + filename,
            nbErrors + " erreur(s) lors du traitement sur le fichier "
                    + filename
                    + ". Fichier complet des erreurs accumulées en pièce jointe.");
    sendMailWithFile(requestJson, filePath.toFile());
    log.info("L'email a été correctement envoyé avec le fichier {} en pièce jointe.",
            filePath.getFileName());
}
```

Supprimer `FILE_ERREURS_INSERTION_469`, `FILE_ERREURS_CREATIONS`,
`HEADER_TSV`, `formatErrorLine`, `appendErrorsToFile`, `extractPpn` et
`extractErreur`.

- [ ] **Step 4: Vérifier le passage au vert**

Run:

```powershell
mvn -Dtest=EmailServiceTest,ErrorWorkbookServiceTest test
```

PowerShell exige de protéger l'argument :

```powershell
mvn '-Dtest=EmailServiceTest,ErrorWorkbookServiceTest' test
```

Expected: tous les tests ciblés passent et les pièces jointes se terminent par
`.xlsx`.

- [ ] **Step 5: Commit**

```powershell
git add src/main/java/fr/abes/kafkatosudoc/service/EmailService.java src/test/java/fr/abes/kafkatosudoc/service/EmailServiceTest.java
git -c user.name="Jerome Villiseck" -c user.email="jvk@abes.fr" commit -m "Joindre les rapports XLSX aux emails Sudoc"
```

---

### Task 4: Vérification complète et compatibilité LibreOffice

**Files:**
- Verify only; no production file is added.

**Interfaces:**
- Consumes: application complète des tâches 1 à 3.
- Produces: preuves de compilation, de tests et d'ouverture LibreOffice.

- [ ] **Step 1: Exécuter la suite Maven complète sous Java 21**

Run:

```powershell
mvn clean test
```

Expected: les 59 tests de référence et tous les nouveaux tests passent, avec
0 échec et 0 erreur.

- [ ] **Step 2: Contrôler le diff**

Run:

```powershell
git diff --check
git status -sb
git log -5 --oneline
```

Expected: aucune erreur d'espacement, uniquement les fichiers prévus et les
commits français du plan.

- [ ] **Step 3: Générer un exemple représentatif**

Créer sous `target/xlsx-preview/` un script JShell non versionné qui instancie
`ErrorWorkbookService`, ajoute une erreur 469 et une erreur de création avec
des identifiants longs, puis affiche les deux chemins produits.

Compiler et construire le classpath :

```powershell
mvn -DskipTests compile
mvn dependency:build-classpath '-Dmdep.outputFile=target/xlsx-preview/classpath.txt'
```

Exécuter le script :

```powershell
$classpath = Get-Content target/xlsx-preview/classpath.txt
jshell --class-path "target/classes;$classpath" target/xlsx-preview/create-preview.jsh
```

Expected: les deux fichiers XLSX existent sous `target/xlsx-preview/reports/`.

- [ ] **Step 4: Vérifier avec Apache POI et LibreOffice**

Inspecter les cellules avec `WorkbookFactory`, puis demander à LibreOffice de
convertir chaque fichier en PDF :

```powershell
New-Item -ItemType Directory -Force -Path target/xlsx-preview/libreoffice | Out-Null
& 'C:\Program Files\LibreOffice\program\soffice.com' --headless --convert-to pdf --outdir target/xlsx-preview/libreoffice target/xlsx-preview/reports/ErreursInsertion469.xlsx
& 'C:\Program Files\LibreOffice\program\soffice.com' --headless --convert-to pdf --outdir target/xlsx-preview/libreoffice target/xlsx-preview/reports/ErreursCreations.xlsx
```

Expected: les deux conversions réussissent. Vérifier visuellement que les
sept en-têtes sont lisibles et que les identifiants longs ne sont pas affichés
en notation scientifique.

- [ ] **Step 5: Appliquer le contrôle de fin de branche**

Utiliser `superpowers:verification-before-completion`, puis
`superpowers:finishing-a-development-branch`. Ne pousser et ne créer une PR
que sur demande explicite de l'utilisateur.
