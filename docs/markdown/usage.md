# Nutzung
Dieses Programm ist für die Verwendung in einem Terminal(Eingabeaufforderung unter Windows) oder in Skripts vorgesehen.  
Es ist allerdings für die einfachsten Anwendungsfälle auch möglich es per Doppelklick zu starten.

## Ausfuehrung
Normalerweise wird das Programm mit `java -jar PutenAuswertung.jar` ausgeführt.

Wenn das Programm per Doppelklick gestartet wird, startet es so als wären keine [Argumente](arguments.md) beim starten übergeben worden.  
Also genau so wie der Befehl oben.

Die [Input-Dateien](input.md) müssen sich in dem Verzeichnis befinden, in dem das Programm ausgeführt wird.  
Wenn mindestens eine dieser Dateien nicht existiert, schreibt dieses Programm eine [Fehlermeldung](#status-meldungen "Status-Meldungen"), und beendet sich ohne eine Analyse zu starten.

**Info:** Wenn das Programm per Doppelklick gestartet wurde sollte **IMMER** die [Log Datei](#status-meldungen "Status-Meldungen") auf Fehlermeldungen überprüft werden.  
Bei der Ausführung in einem Terminal ist dies nicht notwendig, da dieses die Fehlermeldungen anzeigt.

Informationen über die [Argumente](arguments.md) die für die Konfiguration verwendet werden können kann [hier](arguments.md) gefunden werden.

## Status-Meldungen
Dieses Programm schreibt Statusmeldungen sowohl in das Terminal in dem es ausgeführt wird, als auch in eine Logdatei.  
Standardmäßig wird eine Datei namens `PutenAuswertung.log` in dem Verzeichnis in dem es ausgeführt wird als Logdatei verwendet.  
Die Logdatei kann mittels eines [Arguments](arguments.md) geändert werden.

**Achtung:** Die Logdatei wird ohne Nachfrage überschrieben.

Diese Datei könnte zum Beispiel so aussehen wenn es keine Probleme gab:

```
Reading antenna records input file "/path/to/AntennenDaten.csv".
Reading turkey mappings input file "/path/to/Puten.csv".
Reading zone mappings input file "/path/to/Bereiche.csv".
Writing totals to file "/path/to/PutenAuswertungZeiten.csv".
Writing zone stays to file "/path/to/PutenAuswertungAufenthalte.csv".
Valid header line "Transponder	Date	Time	Antenne" found. Reordering columns.
Finished data analysis. Exiting.
```

Diese Datei enthält sowohl Statusmeldungen, wie im Beispiel oben, als auch Fehlermeldungen.
