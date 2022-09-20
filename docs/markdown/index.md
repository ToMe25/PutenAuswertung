# Beschreibung
Dieses Programm dient der automatischen Auswertung von Bereichswechsel Daten.  
Es ist spezifisch für einen Anwendungsfall mit Puten entwickelt, ist aber deutlich universeller einsetzbar.

Es erzeugt Aufenthaltszeiten pro Bereich pro Tag, und eine Liste der Aufenthalte in einem Bereich.

## Inhaltsverzeichniss
 * [Nutzung](usage.md)
    * [Argumente](usage.md#argumente)
    * [Statusmeldungen](usage.md#status-meldungen)
 * [Input Dateien](input.md)
    * [Puten.csv](input.md#puten-csv)
    * [Bereiche.csv](input.md#bereiche-csv)
    * [AntennenDaten.csv](input.md#antennendaten-csv)
 * [Ergebnis Dateien](output.md)
    * [PutenAuswertungZeiten.csv](output.md#putenauswertungzeiten-csv)
    * [PutenAuswertungAufenthalte.csv](output.md#putenauswertungaufenthalte-csv)
 * [Formate](formats.md)
    * [CSV](formats.md#csv)
    * [Zeit](formats.md#zeit)
    * [Datum](formats.md#datum)
 * [Funktion](function.md)
    * [Zuordnungs-Dateien lesen](function.md#zuordnungs-dateien-lesen)
    * [Antennen Daten lesen](function.md#antennen-daten-lesen)
    * [Antennen Aufzeichnung verarbeiten](function.md#antennen-aufzeichnung-verarbeiten)
       * [Wenn eine Aufzeichnung ein anderes Datum hat](function.md#wenn-eine-aufzeichnung-ein-anderes-datum-hat)
    * [Zeiten schreiben](function.md#zeiten-schreiben)

**Achtung:** Diese Dokumentation wird als Archiv verbreitet, damit Links in dieser Funktionieren muss dieses höchst wahrscheinlich entpackt werden.
