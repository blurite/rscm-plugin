# RSCM (RuneScape Config Mapping) - IntelliJ IDEA Plugin

## Setup

Create a directory in your project in which to store your mappings. Mapping files consist of the following format:
```
abyssal_whip:4151
abyssal_whip_note:4152
```
Where `abyssal_whip` is the string representation of the id `4151`.
The name of the mapping file corresponds to the mapping 'type', e.g. `item.rscm` corresponds to `item.abyssal_whip`.

In IntelliJ settings search 'RSCM' and set the mappings directory to the folder you created.

## Features

- Highlighting of mapped strings
- Go to declaration of mapped strings
- Find usages of mapped strings
- Rename mapped strings
  - If using TOML definitions, the plugin will rename files that match the string
- Safe delete mapped strings
- Code completion for mapped strings
- Quick documentation for mapped strings


## Credits
- [ushort](https://github.com/ushort) (Chris)
- [z-kris](https://github.com/z-kris) (Kris)
- [notmeta](https://github.com/notmeta) (Corey)