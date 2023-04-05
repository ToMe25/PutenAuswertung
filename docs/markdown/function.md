# Funktion
Dieses Dokument beschreibt die Haupt-Funktionalität des Programms.  
Die Daten-Auswertung ist in mehrere Phasen unterteilt:
 * [Zuordnungs-Dateien lesen](#zuordnungs-dateien-lesen)
 * [Ausfälle-Datei lesen](#ausfaelle-datei-lesen)
 * [Antennen-Daten lesen](#antennen-daten-lesen)
    * [Antennen-Aufzeichnung verarbeiten](#antennen-aufzeichnung-verarbeiten)
       * [Wenn eine Aufzeichnung ein anderes Datum hat](#wenn-eine-aufzeichnung-ein-anderes-datum-hat)
 * [Zeiten schreiben](#zeiten-schreiben)

[Fehlermeldung]: usage.md#status-meldungen "Status-Meldungen"
[AntennenDaten.csv]: input.md#antennendaten-csv

## Zuordnungs-Dateien lesen
In der ersten Phase der Datenverarbeitung werden die Zuordnungs-Dateien [Bereiche.csv](input.md#bereiche-csv) und [Puten.csv](input.md#puten-csv) in dieser Reihenfolge eingelesen.  
Ungültige Zeilen werden mit einer [Fehlermeldung] übersprungen.  
Falls mindestens eine dieser Dateien keine gültigen Daten enthält, beendet sich das Programm an diesem Punkt mit einer [Fehlermeldung].

## Ausfaelle-Datei lesen
Anschließend wird, falls vorhanden, die [Ausfälle.csv](input.md#ausfaelle-csv) Datei eingelesen.  
Wenn diese nicht vorhanden, oder leer ist wird dieser Schritt übersprungen.  
Ungültige Zeilen werden mit einer [Fehlermeldung] übersprungen.

## Antennen-Daten lesen
Danach wird [AntennenDaten.csv] Zeile für Zeile eingelesen.  
Falls eine Zeile ungültig ist wird eine [Fehlermeldung] ausgegeben, und diese Zeile übersprungen.  
Eine Zeile ist ungültig wenn diese nicht eingelesen werden kann, oder zeitlich vor einer vorherigen für die selbe Pute liegt.  
Eine Zeile ist außerdem ungültig wenn sie an einem Datum vor dem einer vorherigen Zeile liegt.

Falls ein Transponder eingelesen wird, der nicht in der [Puten.csv](input.md#puten-csv) Datei vorhanden ist, wird dieser als separate Pute behandelt.  
Diese Pute hat die Id des Transponders als ihre Id.  
Außerdem wird in diesem Fall eine [Fehlermeldung] geschrieben.

## Antennen-Aufzeichnung verarbeiten
Jedes mal wenn eine gültige Zeile eingelesen wurde, wird die interne Repräsentation der Pute die den aufgezeichneten Transponder trägt aktualisiert.

Die Verarbeitung hier hängt von der [konfigurierbaren](arguments.md) Mindest-Aufenthaltsdauer ab.  
Standardmäßig ist diese fünf Minuten.

**Falls eine Aufzeichnung während eines Ausfalles ist**, wird diese mit einer [Fehlermeldung] ignoriert, und die nächste eingelesen.

**Falls eine Aufzeichnung die erste nach einem Ausfall ist**, werden alle Puten so behandelt als hätten sie sich bis zum Anfang des Ausfalles in ihrem aktuellen Bereich aufgehalten.  
Danach wird alles so behandelt als hätten eine neue Aufzeichnung am Ende des Ausfalles begonnen.  
Mit der Ausnahme das die [Aufenthalts-Zeiten](output.md#putenauswertungaufenthalte-csv) für beide Aufzeichnungen zusammen sind.

**Falls diese bereits existiert, und zuletzt im selben Bereich aufgezeichnet wurde**, wird nur deren letzter Aufzeichnungs-Zeitpunkt geändert.

**Falls sie bereits existiert, aber zuletzt in einem anderen Bereich aufgezeichnet wurde**, passiert eine von drei Sachen:
 1. Falls das `fill-days` [Argument](arguments.md#argument-erklaerung) verwendet wird und die letzte Aufzeichnung von einem vorherigen Tag ist:  
    In diesem Fall wird die Pute so behandelt was wäre ihre letzte Aufzeichnung um Mitternacht, mehr als die Mindest-Aufenthaltsdauer her und in dem Bereich in dem sie nun ist.
 2. Falls der letzte aufgezeichnete Bereichswechsel mehr als die Mindest-Aufenthaltsdauer her ist:  
    In diesem Fall wird aufgezeichnet, das die Pute sich für den Zeitraum seit diese Bereichswechsel, in dem Bereich in dem sie zuvor Aufgezeichnet wurde befand.  
    Der vorläufige Endzeitpunkt dieses Aufenthalts ist der Zeitpunkt der neuen Aufzeichnung.  
    Außerdem wird der Aufenthalt vor dem nun beendeten in die Datei [PutenAuswertungAufenthalte.csv](output.md#putenauswertungaufenthalte-csv) geschrieben.
 3. Falls der letzte Aufenthalts-Zeitraum weniger als die Mindest-Aufenthaltsdauer lang war:  
    In diesem Fall wird aufgezeichnet, das die Pute sich bis zu der neusten Aufzeichnung im letzten Bereich aufhielt, in dem sie sich mehr als die Mindest-Aufenthaltsdauer am Stück aufgehalten hat.

Danach wird die Pute Vorläufig als in dem Bereich der neuesten Aufzeichnung befindlich markiert.  
Dann wird die Anzahl der Bereichswechsel aktualisiert.

**Falls die Pute noch nicht aufgezeichnet wurde**, wird eine neue interne Repräsentation für diese Pute erzeugt.  
Diese wird so behandelt, als ob sie sich seit dem Anfang der Aufzeichnungen im ersten Bereich in dem sie Aufgezeichnet wurde aufgehalten hätte.  
Egal ob dies mehr als die Mindest-Aufenthaltsdauer ist.  
Der Anfang der Aufzeichnungen kann je nach [Konfiguration](arguments.md) der Zeitpunkt der ersten Aufzeichnung der ersten Pute, oder der Anfang des Tages der ersten Aufzeichnung dieser Pute sein.

### Wenn eine Aufzeichnung ein anderes Datum hat
Was in diesem Fall passiert hängt davon ab ob das neue Datum der nächste Tag nach dem letzten Datum ist oder nicht.

**Falls das neue Datum vor dem alten ist**, wird eine [Fehlermeldung] geschrieben, und die Zeile ignoriert.

**Falls das neue Datum auf das alte folgt**, werden falls das `fill-days` [Argument](arguments.md#argument-erklaerung) übergeben wurde alle Puten so behandelt als hätte sie sich bis Mitternacht in ihrem momentanen Bereich aufgehalten.

**Falls das neue Datum nicht auf das vorherige folgt**, wird die alte Aufzeichnung als beendet betrachtet und eine neue gestartet.  
Das heißt falls das `fill-days` [Argument](arguments.md#argument-erklaerung) übergeben wurde werden alle Puten so behandelt als hätten sie sich bis Mitternacht in ihrem momentanen Bereich aufgehalten.  
Andernfalls werden alle Puten so behandelt als ob sie bis zu der letzten Aufzeichnung der letzten Pute in ihrem momentanen Bereich geblieben sind.  
An diesem Punkt werden die Aufenthalts-Zeiten pro Tag pro Pute in die `PutenAuswertungZeiten.csv` Datei geschrieben.

Danach wird alles so behandelt als ob hier der Anfang einer neuen [AntennenDaten.csv] wäre, mit zwei Ausnahmen.
 1. Der neue Anfangs-Zeitpunkt kann nicht vor dem Ende der letzten Aufzeichnung sein.
 2. Die Aufenthaltszeit-Summen am Ende der [PutenAuswertungZeiten.csv](output.md#putenauswertungzeiten-csv) sind für alle Aufzeichnungen in der [AntennenDaten.csv] zusammen.

## Zeiten schreiben
Wenn die [AntennenDaten.csv] Datei vollständig eingelesen wurde passieren drei Dinge.  
Erstens werden alle Puten aktualisiert, als hätten sie sich bis zu der letzten Aufzeichnung der letzten Pute, oder mit `fill-days` dem Ende des letzten Tages, in ihrem momentanen Bereich aufgehalten.  
Danach werden die letzten Aufenthalte in [PutenAuswertungAufenthalte.csv](output.md#putenauswertungaufenthalte-csv) geschrieben.  
Und zu allerletzt werden alle Aufenthaltszeiten in die [PutenAuswertungZeiten.csv](output.md#putenauswertungzeiten-csv) Datei geschrieben.
