# Java Coding Rules — Ndobaak Backend

## RÈGLE ABSOLUE : Imports propres

### ❌ INTERDIT — Utilisation inline de noms de packages complets

```java
// ❌ JAMAIS FAIRE ÇA
if (status == com.ndobaak.backend.common.enums.DocumentStatus.APPROVED) {
    // ...
}

// ❌ JAMAIS FAIRE ÇA
com.ndobaak.backend.auth.entity.User user = getUser();

// ❌ JAMAIS FAIRE ÇA
return new com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse(...);
```

### ✅ OBLIGATOIRE — Imports explicites en haut du fichier

```java
// ✅ TOUJOURS FAIRE ÇA
import com.ndobaak.backend.common.enums.DocumentStatus;
import com.ndobaak.backend.auth.entity.User;
import com.ndobaak.backend.rentalcycle.dto.response.TenantApplicationResponse;

// Puis dans le code :
if (status == DocumentStatus.APPROVED) {
    // ...
}

User user = getUser();
return new TenantApplicationResponse(...);
```

## Justification

1. **Lisibilité** : Le code est plus court et plus clair
2. **Maintenabilité** : Si un package change, on modifie un seul import
3. **Convention Java** : C'est la pratique standard dans tout projet Java professionnel
4. **IDE** : Les IDEs organisent automatiquement les imports

## Exceptions

La seule exception acceptable est en cas de **conflit de noms** entre deux classes :

```java
import java.util.Date;
import com.ndobaak.backend.common.entity.Date; // Conflit

// Dans ce cas rare, on peut utiliser le nom complet pour l'une des deux :
java.util.Date utilDate = new java.util.Date();
Date customDate = new Date();
```

## Vérification automatique

Avant chaque commit, vérifier qu'aucun usage inline n'existe :

```bash
grep -r "com\.ndobaak\.backend\." backend/src/main/java --include="*.java" | grep -v "^.*import " | grep -v "^.*package "
```

Si cette commande retourne des résultats, corriger avant de commiter.

## Application stricte

Cette règle s'applique à **TOUS** les fichiers Java du projet, sans exception (sauf conflit de noms documenté).
