# Input Dateien
Dieses Programm nutzt drei(3) Input Dateien, welche die Daten enthalten die notwendig sind um die Auswertung durchzuführen.  
Weitere Konfiguration wird später mittels [Programm Argumenten](usage.md#argumente) möglich sein.

Die drei Input Dateien sind:
 * [Puten.csv](#puten-csv)
 * [Bereiche.csv](#bereiche-csv)
 * [AntennenDaten.csv](#antennendaten-csv)

Die ersten zwei dieser Dateien werden in dieser Dokumentation "Zuordnungs-Dateien" genannt.  
Dies ist da diese Zuordnungen von Ids zu anderen Ids enthalten.  
Diese werden an einigen Stellen gleich behandelt.

## Allgemeines
Diese Informationen gelten für alle Input Dateien.

 1. Jeder der Buchstaben des Namens muss entweder der groß/klein Schreibung von oben entsprechen, oder klein geschrieben sein.
 2. Das Dateiformat muss [CSV](formats.md#csv) sein.
 3. Zeiten müssen entsprechend der Zeitformate für dieses Programm formatiert sein.
 4. Diese Dateien müssen sich in dem Verzeichnis befinden in dem das Programm ausgeführt wird, nicht in dem Verzeichnis in dem das Programm gespeichert ist.

[Fehlermeldung]: usage.md#status-meldungen "Status Meldungen"

## Puten.csv
Die `Puten.csv` Datei muss zuerst eine Spalte mit der Id der Pute, und dann eine oder mehr Spalten mit den Transpondern die zu dieser Pute gehören enthalten.  
Diese Datei enthält eine Pute pro Zeile.  
Sowohl die Puten Ids als auch die Transponder Ids können Buchstaben, Ziffern und Leerzeichen enthalten.  
Nicht alle Puten müssen die selbe Anzahl an Transpondern haben.

Wenn das Programm mehrere Puten mit der selben Id findet, ignoriert es alle Zeilen die diese Id haben, außer der ersten.  
Wenn eine Transponder Id mehreren Puten zugeordnet ist, ordnet das Programm diese nur der ersten dieser Puten zu.  
Beide dieser Fehler verursachen eine [Fehlermeldung].

Die erste Zeile wird als Spalten Titel Zeile behandelt, falls sie mit `Tier` beginnt.  
Da die Spalten Titel für diese Datei irrelevant sind, wird diese dann ignoriert.

## Bereiche.csv
Die Datei namens `Bereiche.csv` muss zuerst eine Spalte mit dem Namen des Bereiches, und dann eine oder mehr Spalten mit den Ids der Antennen die diesem Bereich zugeordnet sind.  
Die Datei muss einen Bereich pro Zeile enthalten.  
Sowohl die Bereichs Namen als auch die Antennen Ids können Buchstaben, Ziffern und Leerzeichen enthalten.  
Verschiedene Zeilen können eine verschieden viele Antennen Ids enthalten.

Wenn das Programm mehrere Bereiche mit dem selben Namen findet, werden alle außer dem ersten ignoriert.  
Wenn eine Antenne mehreren Bereichen zugeordnet ist, ignoriert das Programm alle Zuordnungen außer der ersten.  
Beide dieser Probleme verursachen eine [Fehlermeldung].

Die erste Zeile dieser Datei wird als Spalten Titel Zeile angesehen, wenn sie mit `Bereich` anfängt.  
Da Spalten Titel in dieser Datei irrelevant sind, wird diese Zeile dann ignoriert.

## AntennenDaten.csv
Der Name dieser Datei muss `AntennenDaten.csv` sein.  
Diese Datei muss vier(4) Spalten pro Zeile enthalten.  
Diese sind:
 * Transponder: Der Transponder der von einer Antenne registriert wurde, und potentiell den Bereich gewechselt hat.
 * Antenne: Die Antenne die den Transponder empfangen hat.
 * Datum: Das Datum an welchem diese Aufzeichnung entstanden ist.
 * Zeit: Die Uhrzeit um welche der Transponder von der Antenne registriert wurde.

Die erste Zeile dieser Datei wird als Spalten Titel Zeile behandelt, wenn einer ihrer Werte `Transponder` ist.  
In diesem Fall wird die Zeile verwendet um die Reihenfolge der Spalten zu ermitteln.  
Das heißt falls eine Titel Zeile vorhanden ist, ist die Spalten Reihenfolge nicht durch das Programm vorgeschrieben.

Damit eine Spalte als Transponder Spalte erkannt wird, muss ihr Titel `Transponder` sein.  
Eine Antennen Spalte muss den Titel `Antenne` oder `Antenna` haben.  
Die Spalte die das Datum enthält muss den Titel `Datum` oder `Date` haben.  
Und die Spalte für Uhrzeiten muss `Zeit` oder `Time` als Titel haben.  
Die Groß/Klein Schreibung der Titel wird hierbei ignoriert.

Wenn Gültige keine Titel Zeile vorhanden ist verwendet das Programm diese Reihenfolge:  
`Transponder`, `Datum`, `Zeit` und `Antenne`.

Die Einträge in dieser Datei müssen nach Zeitpunkt sortiert sein.  
Andernfalls werden alle Zeilen deren Zeit vor der der Zeile davor sind ignoriert.  
Dies erzeugt eine [Fehlermeldung].

Transponder die nicht in der [Puten.csv](#puten-csv) Datei einer Pute zugeordnet sind, werden als separate Pute mit der Id des Transponders behandelt.  
Auch dies verursacht eine [Fehlermeldung].
