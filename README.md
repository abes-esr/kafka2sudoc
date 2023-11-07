# kafka2sudoc
API permettant de lire des topics Kafka et d'effectuer des actions dans le Sudoc

## Développement

### Génération de l'image docker
Vous pouvez avoir besoin de générer en local l'image docker de ``kafak2sudoc`` par exemple si vous cherchez à modifier des liens entre les conteneurs docker de l'application.

Pour générer l'image docker de ``kafka2sudoc`` en local voici la commande à lancer :
```bash
cd kafka2sudoc/
docker build -t abesesr/convergence:develop-kafka2sudoc
```

Cette commande aura pour effet de générer une image docker sur votre poste en local avec le tag ``develop-kafka2sudoc``. Vous pouvez alors déployer l'application en local avec docker en vous utilisant sur le [dépot ``convergence-bacon-docker``](https://github.com/abes-esr/convergence-bacon-docker) et en prenant soins de régler la variable ``BESTPPNAPI_VERSION`` sur la valeur ``develop-best-ppn-api`` (c'est sa [valeur par défaut](https://github.com/abes-esr/convergence-bacon-docker/blob/bdcd4302131eb86688ae729b0fc016d128f1ab9c/.env-dist#L9)) dans le fichier ``.env`` de votre déploiement [``convergence-bacon-docker``](https://github.com/abes-esr/convergence-bacon-docker).

Vous pouvez utiliser la même procédure pour générer en local les autres images docker applications composant l'architecture, la seule chose qui changera sera le nom du tag docker.


Cette commande suppose que vous disposez d'un environnement Docker en local : cf la [FAQ dans la poldev](https://github.com/abes-esr/abes-politique-developpement/blob/main/10-FAQ.md#configuration-dun-environnement-docker-sous-windows-10).

## Architecture de l'application
kafka2sudoc est architecturé autours de 4 listeners kafka qui écoute sur des topics spécifiques, et effectue des actions dans le Sudoc. Les topics écoutés sont les suivants : 
- bacon.kbart.withppn.toload : topic contenant des lignes kbart avec best-ppn pour mise à jour des zones 469 (ajout ou suppression de liens vers les notices bouquets)
- bacon.kbart.todelete.PROVIDER_PACKAGE_DELETED : topic contenant un nom de package et un provider. Ce topic est lu pour supprimer tous les liens de notices bibliographiques contenus dans une notice bouquet correspondant au package / provider lu
- bacon.kbart.sudoc.tocreate.exnihilo : topic contenant une ligne kbart destinée à la création d'une notice électronique exnihilo 
- bacon.kbart.sudoc.imprime.tocreate : topic contenant une ligne kbart et un ppn imprimé, permettant de créer une notice électronique par fusion de la notice imprimée et du kbart

### configuration Kafka en fonction des topics
La configuration de Kafka est différente en fonction des topics écoutés. Pour les topics : 
- bacon.kbart.withppn.toload, bacon.kbart.sudoc.tocreate.exnihilo, et bacon.kbart.sudoc.imprime.tocreate, la configuration Kafka utilise un deserializer Avro avec un schéma spécifique récupéré dans le schema-registry de Kafka
- bacon.kbart.todelete.PROVIDER_PACKAGE_DELETED, la configuration Kafka utilise un deserializer Avro avec un schéma générique géré automatiquement par Spring-kafka. 

Cette différence s'explique par la mode de production dans les différents topics. Le topic bacon.kbart.todelete.PROVIDER_PACKAGE_DELETED est alimenté via kafka-connect, alors que les 3 autres sont alimentés via du code Java. Dans le premier cas, un bug a été constaté dans la récupération du schéma, d'où la nécessité de passer par un schéma générique.

Ainsi, 2 configurations (Bean) sont disponibles dans la classe de configuration KafkaConfig et doivent être injectées en fonction de l'usage qui doit être fait (utilisation de schéma générique ou de schéma spécifiques dans le schema-registry de kafka)

### Actions réalisés dans le Sudoc par topic
Les modifications dans le Sudoc sont réalisées via l'ApiSudoc. A noter que l'utilisation de cette API est stateful. Il est nécessaire de reproduire à l'identiques les étapes qu'on réaliserait via le client lourd WinIBW en utilisant les méthodes de l'API. les modifications réalisées dans le Sudoc sont les suivantes : 
- bacon.kbart.withppn.toload : Ajout ou suppression de 469 avec le PPN de la notice bouquet correspondante au package en fonction du différenciel avec la dernière version de package disponible dans la base Bacon (méthode listenKbartToCreateFromKafka)
- bacon.kbart.todelete.PROVIDER_PACKAGE_DELETED : suppression des 469 contenant le PPN de la notice bouquet correspondant au package supprimé (méthode listenKbartToDeleteFromKafka)
- bacon.kbart.sudoc.tocreate.exnihilo : création notice bibliographique électronique à partir des informations du kbart (méthode listenKbartFromKafkaExNihilo)
- bacon.kbart.sudoc.imprime.tocreate : création notice bibliographique électronique par fusion d'une notice imprimée et des informations du kbart (méthode listenKbartFromKafkaImprime)


