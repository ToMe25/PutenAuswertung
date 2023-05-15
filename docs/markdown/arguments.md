# Argumente
Die Konfiguration dieses Programms erfolgt über Argumente die bei der Ausführung des Programms übergeben werden.

Übersicht:
 * [Nutzung](#nutzung)
    * [Kurze Argumente](#kurze-argumente)
    * [Werte](#werte)
       * [Leerzeichen](#leerzeichen)
 * [Argument Erklärung](#argument-erklaerung)
 * [Skripte](#skripte)
    * [Windows](#windows)
    * [Linux](#linux)

[Programm-Statusmeldungen]: usage.md#status-meldungen "Status-Meldungen"
[Statusmeldungen]: usage.md#status-meldungen "Status-Meldungen"
[Fehlermeldung]: usage.md#status-meldungen "Status-Meldungen"

## Nutzung
Generell werden Argumente verwendet indem diese an den Befehl zum starten des Programms angehängt werden.  
Wenn der Start-Befehl zum Beispiel `java -jar PutenAuswertung.jar` ist, und das `help` Argument verwendet werden soll, müsste dieser Befehl verwendet werden: `java -jar PutenAuswertung.jar --help`.

Die Reihenfolge der Argumente ist irrelevant, es ist allerdings darauf zu achten dass alle Argumente nach dem Dateinamen kommen müssen.  
Das heißt zum Beispiel dieser Befehl würde nicht funktionieren: `java --help -jar PutenAuswertung.jar`  
Dies ist der Fall da dieser Befehl das Argument `--help` an `java` übergeben würde, nicht an `PutenAuswertung.jar`.

Sowohl bei der kurzen Repräsentation von Argumenten, als auch bei der langen Repräsentation ist auf die Groß-/Klein-Schreibung zu achten.

### Kurze Argumente
Alle Argumente haben eine kurze und eine oder mehrere lange Repräsentationen.  
Die kurzen Argumente bestehen aus einem einzelnen Buchstaben.

Anders als die langen Repräsentationen können diese, wie bei vielen [Unix](https://de.wikipedia.org/wiki/Unix)(Linux, MacOS, etc.) Befehlen, kombiniert werden.  
Das heißt nach einem Bindestrich können mehrere Argumente folgen, ohne durch Leerzeichen und Bindestriche getrennt zu sein.  
Ein Beispiel mit den Argumenten `--debug`, `--docs` und `--help`: `java -jar PutenAuswertung.jar -dhD`.

Wie viele [Unix](https://de.wikipedia.org/wiki/Unix) Programme unterscheidet dieses Programm ob eine Zeichenkette als ein Langes oder mehrere kurze Argumente verarbeitet werden soll daran, ob ein oder zwei Bindestriche am Anfang der Zeichenkette stehen.  
Ein Bindestrich bedeutet dass die Zeichenkette als eine Kette kurzer Argumente verarbeitet werden soll.  
Zwei Bindestriche veranlassen das Programm die Zeichenkette als ein langes Argument zu behandeln.  
Dies hat zur Folge dass das folgende Beispiel nicht funktioniert: `java -jar PutenAuswertung.jar -docs`.

### Werte
Wenn ein Argument einen Wert übergeben soll, zum Beispiel das Ziel-Verzeichnis von `--docs`, muss dieser wert mit einem, oder mehreren, Leerzeichen von dem Argument getrennt sein.  
Dieser muss allerdings vor allen weiteren Argumenten kommen.

**Funktionierende Beispiele:**
 * `java -jar PutenAuswertung.jar --docs verzeichnis`
 * `java -jar PutenAuswertung.jar --help --docs verzeichnis`
 * `java -jar PutenAuswertung.jar --docs verzeichnis --help`

**Inkorrekte Beispiele:**
 * `java -jar PutenAuswertung.jar --docs --help verzeichnis`
 * `java -jar PutenAuswertung.jar --help verzeichnis --docs`
 * `java -jar PutenAuswertung.jar --docs=verzeichnis`

## Argument Erklaerung
Die Tabelle in diesem Abschnitt beschreibt die Funktion aller Argumente.

|Kurzes Argument|Langes Argument       |Beschreibung                                                                                                         |  
|---------------|----------------------|---------------------------------------------------------------------------------------------------------------------|  
| -d            | `--debug`            | Aktiviert erweiterte [Programm-Statusmeldungen].                                                                    |  
|               |                      | Hilfreich um Programmfehler zu beheben, wahrscheinlich nicht hilfreich für Anwender.                                |  
|               |                      | `--debug` und `--verbose` verursachen identisches verhalten.                                                        |  
| -v            | `--verbose`          | Aktiviert erweiterte [Programm-Statusmeldungen].                                                                    |  
|               |                      | Hilfreich um Programmfehler zu beheben, wahrscheinlich nicht hilfreich für Anwender.                                |  
|               |                      | `--debug` und `--verbose` verursachen identisches verhalten.                                                        |  
| -h            | `--help`             | Schreibt einen Hilfetext in die Konsole und beendet das Programm.                                                   |  
|               |                      | Schreibt nur eine Logdatei, wenn diese explizit angegeben wurde.                                                    |  
|               |                      | Schreibt den Hilfetext sogar wenn `--silent` ist angegeben.                                                         |  
|               |                      | Der Hilfetext den dieses Argument generiert kann unter dieser Tabelle gefunden werden.                              |  
| -V            | `--version`          | Schreibt die Version des Programms in die Konsole und beendet das Programm.                                         |  
|               |                      | Erzeugt nur eine Logdatei wenn diese explizit definiert wurde.                                                      |  
|               |                      | Funktioniert selbst wenn `--silent` angegeben ist.                                                                  |  
| -s            | `--silent`           | Deaktiviert alle [Programm-Statusmeldungen], inklusive Fehlermeldungen.                                             |  
|               |                      | Dies betrifft sowohl das Terminal als auch die Logdatei.                                                            |  
|               |                      | Der `--help` Hilfetext und die `--version` Versions-Informationen sind hiervon unbetroffen.                         |  
| -D            | `--docs`             | Kann als Optionalen Wert ein Verzeichnis verarbeiten.                                                               |  
|               |                      | Extrahiert diese Dokumentation von der Jar-Datei dieses Programms.                                                  |  
|               |                      | Diese wird dann in einen Ordner mit dem Namen `PutenAuswertung-docs` im aktuellen Verzeichnis geschrieben.          |  
|               |                      | Falls ein Wert angegeben wurde wird der `PutenAuswertung-docs` Order stattdessen in diesem erstellt.                |  
|               |                      | Dateien mit dem Namen einer zu extrahierenden Datei werden normalerweise nicht überschrieben.                       |  
|               |                      | Die `--override` Option kann verwendet werden um solche Dateien zu überschreiben.                                   |  
| -a            | `--antenna-data`,    | Erwartet als Wert eine existierende Datei.                                                                          |  
|               | `--antennadata`      | Das Programm liest dann die [Antennen-Daten](input.md#antennendaten-csv) aus dieser Datei.                          |  
|               |                      | Wenn dieses Argument nicht angegeben wurde, wird die Datei mit Namen `AntennenDaten.csv` verwendet.                 |  
| -t            | `--turkeys`          | Erwartet eine existierende Datei als Wert.                                                                          |  
|               |                      | Das Programm liest diese Datei dann als [Puten-Input-Datei](input.md#puten-csv) ein.                                |  
|               |                      | Wenn dieses Argument nicht angegeben wurde liest das Programm die `Puten.csv` Datei im aktuellen Verzeichnis.       |  
| -z            | `--zones`,           | Erwartet eine existierende Datei als Wert.                                                                          |  
|               | `--areas`            | Diese Datei wird dann als [Bereichs-Input-Datei](input.md#bereiche-csv) eingelesen.                                 |  
|               |                      | Wenn nicht angegeben, wird die Datei `Bereiche.csv` im aktuellen Verzeichnis stattdessen eingelesen.                |  
| -o            | `--downtimes`        | Kann ohne Wert oder mit einer existierenden Datei als Wert verwendet werden.                                        |  
|               | `--offline-times`    | Wenn kein Wert verwendet wird, deaktiviert dies die Verwendung einer [Ausfälle-Input-Datei](input.md#ausfaelle-csv).|  
|               | `--offlinetimes`     | Wenn eine nicht existierende Datei als Wert verwendet wird, wird das Programm mit einer [Fehlermeldung] beendet.    |  
|               | `--off-times`        | Wenn eine existierende Datei als angegeben wird, wird diese als [Ausfälle-Datei](input.md#ausfaelle-csv) eingelesen.|  
|               | `--offtimes`         | Wenn dieses Argument nicht verwendet wird, versucht das Programm die Datei `Ausfälle.csv` einzulesen.               |  
| -T            | `--totals`           | Erwartet eine Datei als Wert.                                                                                       |  
|               |                      | Setzt die Datei in welche das Programm die Zeit die eine Pute pro Tag in einem Bereich verbracht hat schreibt.      |  
|               |                      | Mehr Info über diese Ergebnisse [hier](output.md#putenauswertungzeiten-csv).                                        |  
|               |                      | **Achtung:** Wenn das Programm nicht in einem Terminal gestartet wird, überschreibt es diese Datei ohne Nachfrage.  |  
| -S            | `--stays`            | Erwartet eine Datei als Wert.                                                                                       |  
|               |                      | Setzt die Datei in welche das Programm die individuellen Aufenthalte einer Pute in einem Bereich schreibt.          |  
|               |                      | Mehr Informationen über diese Ergebnisse gibt es [hier](output.md#putenauswertungaufenthalte-csv).                  |  
|               |                      | **Achtung:** Wenn das Programm nicht in einem Terminal gestartet wird, überschreibt es diese Datei ohne Nachfrage.  |  
| -l            | `--log-file`,        | Kann optional eine Datei als Wert verarbeiten.                                                                      |  
|               | `--logfile`          | Ändert in welche Datei das Programm seine [Statusmeldungen] schreibt.                                               |  
|               |                      | Wenn kein Wert übergeben wird, wird keine Logdatei geschrieben.                                                     |  
|               |                      | Wenn dieses Argument nicht verwendet wird, wird die Datei `PutenAuswertung.log` verwendet.                          |  
|               |                      | **Achtung:** Die Logdatei wird ohne nachfrage überschrieben.                                                        |  
| -f            | `--fill-days`,       | Teilt dem Programm mit Puten sollen so behandelt werden als ob sie sich vor ihrer ersten und nach ihrer letzten     |  
|               | `--filldays`         | Aufzeichnung jeden Tag in dem Bereich aufgehalten in dem sie zuerste/zuletzt aufgezeichnet wurden.                  |  
| -m            | `--min-time`,        | Erwartet eine positive Zeit in Sekunden als Wert.                                                                   |  
|               | `--mintime`          | Setzt die Bereichs-Mindest-Aufenthaltsdauer für die Puten.                                                          |  
|               |                      | Nur Bereichs-Aufenthalte die länger als diese Dauer anhalten werden gewertet.                                       |  
|               |                      | Kürzere Aufenthalte werden dem letzten längeren Aufenthalt angerechnet.                                             |  
|               |                      | Ein Wert von 0 kann verwendet werden um die Mindestdauer zu deaktivieren.                                           |  
|               |                      | Wenn dieses Argument nicht verwendet wird, ist die Mindest-Aufenthaltsdauer 5 Minuten(300 Sekunden).                |  
| -O            | `--override`,        | Teilt dem Programm mit es soll alle Ergebnissdateien ohne Nachfrage überschreiben.                                  |  
|               | `--override-outputs` | Wenn diese Argument nicht übergeben wird fragt das Programm nach bevor es eine Ergebnissdatei überschreibt.         |  
|               | `--overrideoutputs`  | **Achtung:** Wenn das Programm nicht in einem Terminal gestartet wird, werden die Dateien auch überschrieben.       |  
|               |                      | Da in diesem Falle die Nachfrage nicht möglich ist, überschreibt das Programm sie ohne Nachfrage.                   |  
|               |                      | Kann auch verwendet werden um bei der extraktion der Dokumentation Dateien zu überschreiben.                        |  
| -c            | `--decimal-separator`| Erwartet einen Punkt oder ein Komma als Argument.                                                                   |  
|               | `--decimalseparator` | Teilt dem Programm mit welches Zeichen in [Ergebnis-Dateien](output.md) und [Statusmeldungen] Hundertstel           |  
|               | `--decimal-comma`    | von Sekunden getrennt werden sollen.                                                                                |  
|               | `--decimalcomma`     | Wenn dieses Argument nicht verwendet wird, nutzt das Programm Punkte um Sekunden und Hundertstel zu trennen.        |  
|               | `--comma`            |                                                                                                                     |  

Hier das Ergebnis von `--help`:

```
Usage: java -jar PutenAuswertung.jar [OPTION]...

Values for long options also apply to short options.

Options:
 -d, --debug                           Enables additional log output about issues and the current program state.
                                       Useful for debugging issues, but likely not useful for the average user.
                                       Verbose and debug are aliases of each other.
 -v, --verbose                         Enables additional log output about issues and the current program state.
                                       Useful for debugging issues, but likely not useful for the average user.
                                       Verbose and debug are aliases of each other.
 -s, --silent                          Disables all log output from this program.
                                       This includes both the standard out/err(shown in your terminal) as well as log files.
                                       The help text and version info still get written.
 -h, --help                            Prints this help text and exits.
                                       Gets written even with --silent.
 -V, --version                         Prints version info about this program and exits.
                                       Gets written even with --silent.
 -D, --docs [DIRECTORY]                Extracts this programs documentation from the jar, then exits.
                                       Puts the documentation in the specified directory, if any.
                                       If none is specified, puts it in the current directory.
 -a, --antenna-data, <FILE>            Sets the file this program should read antenna data records from.
     --antennadata <FILE>
 -t, --turkeys <FILE>                  Sets the file to read the turkey to transponder mappings from.
                                       These mappings tell the program which transponders are attached to the same turkey.
 -z, --zones, <FILE>                   Sets the file to read zone to antenna mappings from.
     --areas <FILE>                    These mappings tell the program which antennas are part of which zone.
 -o, --downtimes, [FILE]               Sets the file to read the system downtimes from.
     --offline-times, [FILE]           System downtimes are the times in which data could not be recorded,
     --offlinetimes, [FILE]            which thus shouldn't be part of the evaluation.
     --off-times, [FILE]               Use without a value to not read any downtimes file.
     --offtimes [FILE]
 -T, --totals <FILE>                   Sets the file to write the total zone times to.
 -S, --stays <FILE>                    Sets the file to write the individual zone stays to.
 -l, --log-file, [FILE]                Sets the file to write the logging messages to.
     --logfile [FILE]                  Use without a value to disable creating a log file entirely.
 -f, --fill-days,                      Makes the program pad the recording time to full days.
     --filldays                        This means it will assume that all turkeys were in the zone they were first recorded in until their first record.
                                       And that they all were in their last recorded zone until the end of each day.
 -m, --min-time, <TIME>                Sets the minimum time a turkey has to spend in a zone at a time for the stay to be counted.
     --mintime <TIME>                  Set to 0 to disable the check entirely.
                                       Stays shorter than this are counted towards the last stay longer than this value.
                                       The value is in seconds.
                                       The default is 300 seconds, aka 5 minutes.
 -O, --override,                       Tells the program to override output files if they already exist.
     --override-outputs,               If this argument is not specified the program will ask what to do if output files already exist.
     --overrideoutputs                 If the program is run non-interactively(in a script or by double-clicking) it overrides output files even without this argument.
 -c, --decimal-separator, <SEPARATOR>  Sets the character to be used to separate the seconds from hundredth in output times.
     --decimalseparator, <SEPARATOR>   This character will be used in both the output files, and log messages.
     --decimal-comma, <SEPARATOR>      Can only be either a dot or a comma.
     --decimalcomma, <SEPARATOR>
     --comma <SEPARATOR>
```

#### Leerzeichen
Da Leerzeichen als Trennzeichen zwischen Argumenten und Werten verwendet werden, können diese unintuitives verhalten verursachen.  
Dieses Programm versucht automatisch zu erkennen ob ein Leerzeichen teil eines Argumentes, oder ein Trennzeichen ist.  
Dies ist allerdings nicht immer Fehlerfrei möglich.

Ein Teil dieses Problems ist dadurch verursacht wie Terminals Leerzeichen in Argumenten behandeln.  
Dies ist sowohl auf Linux als auch modernem Windows der Fall.

Das Problem ist dass Terminals den Befehl bei jedem Leerzeichen trennen, und dann dem Programm alle nicht leeren Segmente als Argumente übergeben.  
Dies bedeutet dass aus Sicht des Programms 

```
java -jar PutenAuswertung.jar -D Some Dir
```

und

```
java -jar PutenAuswertung.jar -D Some  Dir
```

identisch sind.  
In beiden fällen bekommt das Programm `-D`, `Some` und `Dir` als Argumente.  
Das Programm nimmt dann an dass `Some` und `Dir` ein Wert sind, und verwendet das Verzeichnis `Some Dir`.

Das Programm könnte auch, zum Beispiel, 10 Leerzeichen nicht von einem einzelnen unterscheiden.

Die Lösung für dieses Problem ist den Wert in Anführungszeichen zu setzen.  
Beispiel:

```
java -jar PutenAuswertung.jar -D "Some  Dir"
```
Dies teilt dem Terminal mit dass diese Leerzeichen nicht zum trennen des Wertes verwendet werden sollen.

Eine Alternative ist es vor die Leerzeichen je einen Backslash zu machen.  
Auch diese teilt dem Terminal mit den Wert hier nicht zu trennen.  
Beispiel:

```
java -jar PutenAuswertung.jar -D Some\ \ Dir
```

Sowohl diese Anführungszeichen als auch einfache Backslashes werden dann vom Terminal entfernt.  
Das heißt das folgende würde nicht funktionieren:

```
java -jar PutenAuswertung.jar -D Some\ -Dir
```

Da der Backslash vom Terminal entfernt wird, erkennt das Programm nicht dass das `-Dir` noch Teil des Wertes ist, und versucht dies als separates Argument zu verarbeiten.

Der einfachste Weg dies zu vermeiden ist zwei Backslashes vor entweder das Leerzeichen direkt vor dem Bindestrich, oder vor den Bindestrich zu setzen.  
In diesem Fall entfernt das Terminal einen Backslash, und das Programm interpretiert den anderen als Markierung dass hier keine Argument-Trennung vorliegt.  
Beispiel:

```
java -jar PutenAuswertung.jar -D Some \\-Dir
```

## Skripte
Dieses Programm ist dafür vorgesehen auch in Skripten verwendet zu werden.  
Selbst in einfachen Anwendungsfällen hat dies den Vorteil das Argumente nicht bei jeder Ausführung neu angegeben werden müssen.

### Windows
Ein minimales Windows Batch-Skript für die Verwendung würde so aussehen:

```bat
@echo off

java -jar pfad\zu\PutenAuswertung.jar
```

Wie auch bei manueller Ausführung können Argumente nach dem Dateinamen angehängt werden.

Das `@echo off` in der ersten Zeile sorgt dafür dass das Terminal nicht jeden Befehl schreibt bevor er ausgeführt wird.  
Bei einem Skript mit nur einer Zeile wie diesem ist das nicht wirklich so hilfreich.  
Bei längeren Skripten ist es allerdings angenehm.  
Zum Beispiel falls das Programm für jeden Unterordner in einem Order ausgeführt werden soll kann dies hilfreich sein.

Die Dateiendung von Batch-Skripts muss `.bat` sein.

### Linux
Ein minimales Linux Shell-Skript für die Verwendung dieses Programms würde so aussehen:

```sh
#!/bin/sh

java -jar pfad/zu/PutenAuswertung.jar
```

Wie auch bei manueller Ausführung können Argumente nach dem Dateinamen angehängt werden.

Anders als unter [Windows](#windows) muss das Skript unter Linux erst als ausführbar markiert werden.  
Im Terminal kann dies mit `chmod +x SKRIPT-NAME` erledigt werden.  
Hierbei muss `SKRIPT-NAME` offensichtlich mit dem Dateinamen des Skriptes ersetzt werden.  
Eine Datei als ausführbar markieren sollte auch mit den meisten Dateiverwaltungen möglich sein.

Die erste Zeile dieser Datei verrät Linux welches Programm zum ausführen dieses Skripts verwendet werden soll.

Die Dateiendung eines Shell-Skripts sollte `.sh` sein.
