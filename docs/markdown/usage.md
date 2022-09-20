# Nutzung
Dieses Programm ist für die Verwendung in einem Terminal(Eingabeaufforderung unter Windows) oder in Skripts vorgesehen.  
Es ist allerdings für die einfachsten Anwendungsfälle auch möglich es per Doppelklick zu starten.

Normalerweise wird das Programm mit `java -jar PutenAuswertung.jar` ausgeführt.

Das Programm per Doppelklick zu starten, startet es so als wären keine [Argumente](#argumente) beim starten übergeben worden, also genau so wie der Befehl oben.

Die [Input Dateien](input.md) müssen sich in dem Verzeichnis befinden, in dem das Programm ausgeführt wird.  
Wenn mindestens eine dieser Dateien nicht existiert, schreibt dieses Programm eine [Fehlermeldung](#status-meldungen "Status-Meldungen"), und beendet sich ohne eine Analyse zu starten.

**Info:** Wenn das Programm per Doppelklick gestartet wurde sollte **IMMER** die [Log Datei](#status-meldungen "Status-Meldungen") auf Fehlermeldungen überprüft werden.  
Bei der Ausführung in einem Terminal ist dies nicht notwendig, da dieses die Fehlermeldungen anzeigt.

## Argumente
Dieses Programm soll über Programm-Argumente Konfiguriert werden, dies ist allerdings noch nicht implementiert.  
Informationen über diese Argumente werden in der Dokumentation(hier oder in einer separaten Datei), und mit `java -jar PutenAuswertung.jar --help` verfügbar sein.

## Status-Meldungen
Dieses Programm schreibt Statusmeldungen sowohl in das Terminal in dem es ausgeführt wird, als auch in eine Datei namens `PutenAuswertung.log` in dem Verzeichnis in dem es ausgeführt wird.  
Diese Datei könnte zum Beispiel so aussehen wenn es keine Probleme gab:

```
Reading antenna records input file "/path/to/AntennenDaten.csv".
Reading turkey mappings input file "/path/to/Puten.csv".
Reading zone mappings input file "/path/to/Bereiche.csv".
Valid header line "Transponder	Date	Time	Antenne" found. Reordering columns.
Finished data analysis. Exiting.
```

Diese Datei enthält sowohl Statusmeldungen, wie im Beispiel oben, als auch Fehlermeldungen.
