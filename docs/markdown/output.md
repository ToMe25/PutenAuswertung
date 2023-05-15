# Ergebnis-Dateien
Dieses Programm schreibt die Ergebnisse der Verarbeitung der [Inputdaten](input.md) in zwei Ergebnis-Dateien.  
Die Namen und Positionen dieser können mittels [Programm-Argumenten](arguments.md) geändert werden.

Ergebnis-Dateien:
 * [PutenAuswertungZeiten.csv](#putenauswertungzeiten-csv)
 * [PutenAuswertungAufenthalte.csv](#putenauswertungaufenthalte-csv)

## Allgemein
Die Ergebnisdateien werden in dem Verzeichnis angelegt in dem das Programm ausgeführt wird.  
Also da wo sich auch die [Input-Dateien](input.md) befinden müssen.  
Wie auch die [Input-Dateien](input.md) sind die Ergebnis-Dateien im [CSV-Format](formats.md#csv).

Die Namen und Positionen dieser Dateien können mittels [Programm-Argumenten](arguments.md) verändert werden.

Falls eine Ergebnisdatei schon existiert fragt das Programm nach ob diese überschrieben, oder eine andere verwendet werden soll.

Diese Nachfrage sieht so aus:

```
Output file "PutenAuswertungZeiten.csv" already exists.
Do you want to [O]verride, [R]ename, or [C]ancel?
```

Falls `Rename` gewählt wird, kann im nächsten Schritt ein neuer Name angegeben werden.

Note: Diese Nachfrage wird nur in das Terminal geschrieben, sie taucht nicht in der Logdatei auf.

**Achtung:** Das Programm überschreibt die Ergebnis-Dateien, wenn es nicht in einem Terminal ausgeführt wird, ohne Nachfrage.

## PutenAuswertungZeiten.csv
Die `PutenAuswertungZeiten.csv` Datei enthält die Zeiten die eine Pute an einem Tag in einem Bereich verbracht hat.  
Sie hat die folgenden Spalten in dieser Reihenfolge:
 1. `Tier`: Die Id der Pute für die diese Zeile ist.
 2. `Datum`: Das Datum des Tages für welchen diese Zeile die Aufenthaltsdauern pro Bereich enthält.
 3. `Bereichswechsel`: Wie oft die Pute an diesem Tag den Bereich in der sie sich aufhält gewechselt hat.
 4. `Zeit in Zone X`: Eine Spalte pro Zone für die Zeit die diese Pute an diesem Tag in dem Bereich verbracht hat.
 5. `Unzuverlaessig`: Diese Spalte enthält ein `X` wenn die Daten dieser Pute für diesen Tag als unzuverlässig angesehen werden.

Die Datei `PutenAuswertungZeiten.csv` enthält eine Zeile pro Pute aus den [Input-Dateien](input.md) pro Tag an dem sie als Existent gewertet wurde.  
Je nach [Konfiguration](arguments.md) können Puten auch an Tagen an denen sie nicht Registriert wurden als existent gewertet werden.

Die Datei enthält außerdem eine Zeile pro Pute für die Summe der Zeiten für alle Tage.  
Diese Zeilen haben `total` als Datum.

Ein Zeitraum wird als unzuverlässig angesehen wenn eine Pute sich in einem Bereich ohne Essen befindet und 12 Stunden oder mehr keine Aufzeichnung in ihrem aktuellen Bereich hat.  
Ein Tag wird für eine Pute als unzuverlässig gewertet wenn ein unzuverlässiger Zeitraum ganz oder Teilweise an diesem Tag ist.  
Das heißt auch wenn der unzuverlässige Zeitraum 12:05 lang ist, und davon nur die letzten 3 Minuten an diesem Tag sind, wird dieser Tag trotzdem als unzuverlässig gewertet.

**Info:** Wenn die letzte Aufzeichnung der Letzten Puten ein Bereichswechsel ist, wird die Anzahl der Bereichswechsel erhöht, aber kein weiterer Aufenthalt geschrieben.

## PutenAuswertungAufenthalte.csv
Die Datei `PutenAuswertungAufenthalte.csv` enthält die individuellen Aufenthalte in einem Bereich pro Pute.  
Diese Datei hat die folgenden acht(8) Spalten in dieser Reihenfolge:
 1. `Tier`: Die Id der Pute der dieser Aufenthalt zugeordnet ist.
 2. `Bereich`: Der Bereich in dem sich die Pute aufgehalten hat.
 3. `Startdatum`: Das Datum an welchem die Pute den Bereich betreten hat.
 4. `Startzeit`: Die Uhrzeit um welche die Pute den Bereich betreten hat.
 5. `Enddatum`: Das Datum an welchem die Pute den Bereich verlassen hat.
 6. `Endzeit`: Die Uhrzeit um welche die Pute den Bereich verlassen hat.
 7. `Aufenthaltszeit`: Die Zeit die die Pute in dem Bereich verbracht hat.  
    Dies ist die Differenz zwischen der Startzeit und der Endzeit.
 8. `Unzuverlaessig`: Ob dieser Aufenthalt als unzuverlässig angesehen wird.
    Wenn ein Aufenthalt als unzuverlässig betrachtet wird, enthält diese Spalte ein `X`.

Die Datei enthält eine Zeile pro Aufenthalt pro Pute.  
Ein Aufenthalt kann mehrere Tage andauern, und ist erst als beendet angesehen wenn entweder
 1. die Pute in einem anderen Bereich registriert wurde,
 2. ein Zeitraum ohne Aufzeichnungen beginnt oder
 3. die Aufzeichnung endet.

Ein Zeitraum wird als unzuverlässig angesehen wenn eine Pute sich in einem Bereich ohne Essen befindet und 12 Stunden oder mehr keine Aufzeichnung in ihrem aktuellen Bereich hat.  
Ein Aufenthalt wird als unzuverlässig gewertet wenn dieser einen solchen Zeitraum beinhaltet.

Diese Datei ist effektiv unsortiert.
