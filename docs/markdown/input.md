# Input-Dateien
Dieses Programm nutzt vier(4) Input-Dateien.  
Diese enthalten die notwendigen Daten für die Auswertung.  
Weitere Konfiguration ist mittels [Programm-Argumenten](arguments.md) möglich.

Die vier Input-Dateien sind:
 * [Puten.csv](#puten-csv)
 * [Bereiche.csv](#bereiche-csv)
 * [Ausfälle.csv](#ausfaelle-csv)
 * [AntennenDaten.csv](#antennendaten-csv)

Die ersten zwei dieser Dateien werden in dieser Dokumentation "Zuordnungs-Dateien" genannt.  
Dies ist da diese Zuordnungen von Ids zu anderen Ids enthalten.  
Diese werden an einigen Stellen gleich behandelt.

[Fehlermeldung]: usage.md#status-meldungen "Status-Meldungen"

## Allgemein
Dieser Abschnitt enthält allgemeine Informationen die für alle Input-Dateien gelten.

Alle Input-Dateien müssen dem [CSV-Format](formats.md#csv) entsprechen.  
Zeiten und Daten müssen entsprechend der [Zeit-](formats.md#zeit)/[Datums-](formats.md#datum)Formate für dieses Programm formatiert sein.  
Ungültige Zeilen werden mit einer [Fehlermeldung] ignoriert, führen also nicht zum Abbruch der Auswertung.  
Die Namen und Positionen der zu verwendenden Dateien können mittels [Programm-Argumenten](arguments.md) verändert werden.

Informationen über die Standard-Dateien die eingelesen werden, wenn diese nicht mittels Argumenten verändert werden:
 1. Die Groß/Klein-Schreibung muss den in dieser Datei verwendeten entsprechen, oder der gesamte Name muss klein geschrieben werden.
 2. Diese Dateien müssen sich in dem Verzeichnis befinden in dem das Programm ausgeführt wird, nicht in dem Verzeichnis in dem das Programm gespeichert ist.

## Puten.csv
Die `Puten.csv` Datei muss mindestens fünf(5) Spalten enthalten:  
 * Die erste Spalte enthält die Id der Pute,
 * Die zweite Spalte kann den Start-Bereich der Pute enthalten, oder leer sein,
 * Die dritte Spalte kann die End-Zeit der Pute enthalten, oder leer sein,
 * Die vierte Spalte kann das End-Datum der Pute enthalten, oder leer sein und
 * Alle weiteren Spalten, von welchen mindestens eine vorhanden sein muss, enthalten Transponder-Ids.

Diese Datei enthält Informationen über eine Pute pro Zeile.  
Sowohl die Puten-Ids als auch die Transponder-Ids können Groß-/Klein-Buchstaben, Ziffern, Leerzeichen und Bindestriche enthalten.  
Nicht alle Puten müssen die selbe Anzahl Transponder-Spalten haben.

Der Start-Bereich gibt an in welchem Bereich sich die Pute vom Anfang **der ersten Aufzeichnung**, bis zu ihrem ersten Auftreten in der [Antennen-Daten-Datei](#antennendaten-csv) befindet.

Die End-Zeit ist der Zeitpunkt zu welchem die Auswertung für diese Pute endet, falls sie andernfalls länger wäre.  
Wenn die Aufzeichnung aus anderen Gründen früher endet wird diese ignoriert.

**Achtung:** In dieser Datei ist die Zeit vor dem Datum, in allen anderen Dateien steht die Uhrzeit nach dem Datum.  
Dies ist aus technischen Gründen nötig, und wird sich voraussichtlich nicht ändern.

Wenn eine Zeile eine End-Uhrzeit, aber kein End-Datum enthält, wird diese End-Uhrzeit mit einer [Fehlermeldung] ignoriert.  
Wenn eine Zeile ein End-Datum, aber keine End-Uhrzeit enthält, wird eine [Fehlermeldung] ausgegeben, und die Pute am Anfang des Tages entfernt.

Wenn das Programm mehrere Puten mit der selben Id findet, ignoriert es alle Zeilen die diese Puten-Id haben, außer der ersten.  
Wenn eine Transponder Id mehreren Puten zugeordnet ist, ordnet das Programm diese nur der ersten dieser Puten zu.  
Beide dieser Fehler verursachen eine [Fehlermeldung].

Die erste Zeile wird als Spaltentitel-Zeile behandelt, falls sie mit `Tier` beginnt.  
Da das Programm nicht in der Lage ist die Spalten nach Titel zu sortieren, wird diese dann ignoriert.

## Bereiche.csv
Die Datei namens `Bereiche.csv` muss zuerst eine Spalte mit dem Namen des Bereiches, und dann eine oder mehr Spalten mit den Ids der Antennen die diesem Bereich zugeordnet sind, enthalten.  
Die Datei muss einen Bereich pro Zeile enthalten.  
Sowohl die Bereichs-Namen als auch die Antennen-Ids können Groß-/Klein-Buchstaben, Ziffern, Leerzeichen und Bindestriche enthalten.  
Verschiedene Bereiche können eine verschieden viele Antennen-Ids enthalten.

Wenn das Programm mehrere Bereiche mit dem selben Namen findet, werden alle diese außer dem ersten ignoriert.  
Wenn eine Antenne mehreren Bereichen zugeordnet ist, ignoriert das Programm alle Zuordnungen außer der ersten.  
Beide dieser Probleme verursachen eine [Fehlermeldung].

Die erste Zeile dieser Datei wird als Spaltentitel-Zeile angesehen, wenn sie mit `Bereich` anfängt.  
Da Spalten-Titel in dieser Datei irrelevant sind, wird diese Zeile dann ignoriert.

## Ausfaelle.csv
Anders als die drei anderen Input-Dateien ist die `Ausfälle.csv` Datei optional.  
Das heißt, wenn diese nicht vorhanden ist funktioniert das Programm wie normal.  
Der einzige unterschied zwischen der Ausführung mit einer leeren Datei und ohne Datei ist diese [Status-Meldung](usage.md#status-meldungen "Status-Meldungen"):

```
No downtimes file found. This program expects an optional file called "Ausfälle.csv" in the directory you are executing this command in.
```

Alternativ kann dass `-o` [Argument](arguments.md) ohne Wert verwendet werden um die Verwendung einer Ausfälle-Datei zu deaktivieren.

Diese Datei muss vier Spalten enthalten: `Start Datum`, `Start Zeit`, `End Datum` und `End Zeit`.  
Die vier Spalten müssen in dieser Reihenfolge verwendet werden.  
Jede Spalte muss einen Ausfall beschreiben.  
Die Zeiten und Daten in diesen Spalten müssen den Programmweiten [Zeit-](formats.md#zeit)/[Datums-](formats.md#datum)Formaten entsprechen.  
Zeilen die diesen Bedingungen nicht entsprechen werden mit einer [Fehlermeldung] ignoriert.

Ein Ausfall in dieser Datei hat die folgenden Effekte:
 * Alle Aufenthalte die bis zu dem Anfang des Ausfalles reichen, enden zu dem Zeitpunkt wo der Ausfall beginnt.
 * Alle Aufzeichnungen während einem Ausfall werden mit einer [Fehlermeldung] ignoriert.
 * Die Zeit während eines Ausfalls wird nicht als Aufenthalts-Zeit gerechnet.
 * Nach dem Ausfall beginnt eine neue Aufzeichnung, das bedeutet Puten die vor dem Ausfall existiert haben tun dies danach nicht zwangsläufig.

## AntennenDaten.csv
Der Name dieser Datei muss `AntennenDaten.csv` sein.  
Diese Datei muss vier(4) Spalten pro Zeile enthalten.  
Diese sind:
 * Transponder: Der Transponder der von einer Antenne registriert wurde, und potentiell den Bereich gewechselt hat.
 * Antenne: Die Antenne die den Transponder empfangen hat.
 * Datum: Das Datum an welchem der Transponder registriert wurde.
 * Zeit: Die Uhrzeit um welche der Transponder von der Antenne registriert wurde.

Die erste Zeile dieser Datei wird als Spaltentitel-Zeile behandelt, wenn einer ihrer Werte `Transponder` ist.  
In diesem Fall wird die Zeile verwendet um die Reihenfolge der Spalten zu ermitteln.  
Das heißt falls eine Spaltentitel-Zeile vorhanden ist, ist die Reihenfolge der Spalten nicht durch das Programm vorgeschrieben.

Damit eine Spalte als Transponder-Spalte erkannt wird, muss ihr Titel `Transponder` sein.  
Eine Antennen-Spalte muss den Titel `Antenne` oder `Antenna` haben.  
Die Spalte die das Datum enthält muss den Titel `Datum` oder `Date` haben.  
Und die Spalte für Uhrzeiten muss `Zeit` oder `Time` als Titel haben.  
Die Groß/Klein Schreibung der Titel wird hierbei ignoriert.

Wenn keine gültige Titel-Zeile vorhanden ist verwendet das Programm diese Reihenfolge:  
`Transponder`, `Datum`, `Zeit` und `Antenne`.

Eine Zeile in dieser Datei kein Datum bevor dem Datum der spätesten Zeile über dieser haben.  
Falls sie dennoch ein früheres Datum hat wird sie mit einer [Fehlermeldung] wie dieser ignoriert:

```
New antenna record on date 08.02.2022 is on a day before the previous date 09.02.2022. Skipping line.
```

Außerdem müssen alle Aufzeichnungen die die selbe Pute betreffen nach Zeitpunkt sortiert sein.  
Andernfalls werden sie mit einer [Fehlermeldung] wie dieser übersprungen:

```
New antenna record at 00.02.2022 04:29:36.09 for turkey "Turkey" is before the last one for the same turkey. Skipping line.
```

Transponder die nicht in der [Puten.csv](#puten-csv) Datei einer Pute zugeordnet sind, werden als separate Pute mit der Id des Transponders behandelt.  
Auch dies verursacht eine [Fehlermeldung].
