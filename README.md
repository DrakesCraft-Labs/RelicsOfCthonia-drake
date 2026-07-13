# RelicsOfCthonia Drake

RelicsOfCthonia Drake es el port mantenido por DrakesCraft Labs del addon de
reliquias del Nether de FN_FAL113. Conserva los IDs, reliquias y archivo
`relic-settings.yml` existentes, pero actualiza la integración a Paper 1.21.11
y endurece los drops y trueques con piglins.

## Runtime compatible

| Componente | Objetivo |
|---|---|
| Minecraft / Paper / Purpur | **1.21.11** |
| Java | **21** |
| Slimefun | **Slimefun Drake 11** |

No se debe cargar junto al JAR upstream: ambos usan el mismo nombre de plugin e
IDs de Slimefun.

## Cambios Drake

- Migrado a los namespaces del core Slimefun Drake y Paper API 1.21.11.
- Sin autoactualizador ni descargas de JAR durante la ejecución.
- Los trueques con piglins guardan la reliquia, material y condición en PDC al
  iniciar. Si Paper u otro plugin normaliza el ítem de entrada, el trueque sigue
  siendo válido sin generar el error ni reemplazar la recompensa incorrecta.
- El material de barter temporal se libera incluso si el trade es cancelado o el
  piglin recibe daño.
- El minado usa una copia de la lista de fuentes, evitando mutación global entre
  eventos de bloque.

## Equilibrio de minería

El upstream permite que cada reliquia asociada a un mismo bloque tire su propia
probabilidad. Como Netherrack está asociado a varias reliquias, esto podía
producir ráfagas de drops.

La configuración por defecto de Drake es:

```yaml
mining:
  max-relic-drops-per-block: 1
  material-drop-chance-multiplier: 0.20
  netherrack-drop-chance-multiplier: 0.05
```

Con ello Netherrack conserva la posibilidad de descubrir reliquias, pero su
probabilidad agregada queda cerca de un 4% y nunca entrega más de una por
bloque. Los drops de mobs y recompensas de piglins no cambian.

## Actualización segura

1. Respalda `plugins/RelicsOfCthonia.vUnofficial-2.0.0.jar` y
   `plugins/RelicsOfCthonia/`.
2. Conserva `relic-settings.yml`: contiene el balance personalizado del
   servidor y no debe reemplazarse.
3. Instala `target/RelicsOfCthonia-drake.jar` durante una ventana de reinicio.
4. En staging valida una reliquia legacy, un trade de piglin y minería de
   Netherrack antes de retirar el JAR anterior.

## Build

```bash
mvn -B -ntp clean verify
```

## Créditos

Proyecto original de [FN_FAL113](https://github.com/FN-FAL113/RelicsOfCthonia),
creado para Slimefun Addon Jam 2022. Este fork conserva su autoría y adapta el
mantenimiento al ecosistema DrakesCraft.
