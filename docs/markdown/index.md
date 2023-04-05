# Beschreibung
Dieses Programm dient der automatischen Auswertung von Bereichswechsel Daten.  
Es ist spezifisch für einen Anwendungsfall mit Puten entwickelt, ist aber deutlich universeller einsetzbar.

Es erzeugt Aufenthaltszeiten pro Bereich pro Tag, und eine Liste der Aufenthalte in einem Bereich.

## Inhaltsverzeichnis
 * [Nutzung](usage.md)
    * [Ausführung](usage.md#ausfuehrung)
    * [Status-Meldungen](usage.md#status-meldungen)
 * [Argumente](arguments.md)
    * [Nutzung](arguments.md#nutzung)
       * [Kurze Argumente](arguments.md#kurze-argumente)
       * [Werte](arguments.md#werte)
          * [Leerzeichen](arguments.md#leerzeichen)
    * [Argument Erklärung](arguments.md#argument-erklaerung)
    * [Skripte](arguments.md#skripte)
       * [Windows](arguments.md#windows)
       * [Linux](arguments.md#linux)
 * [Input-Dateien](input.md)
    * [Allgemein](input.md#allgemein)
    * [Puten.csv](input.md#puten-csv)
    * [Bereiche.csv](input.md#bereiche-csv)
    * [Ausfälle.csv](input.md#ausfaelle-csv)
    * [AntennenDaten.csv](input.md#antennendaten-csv)
 * [Ergebnis-Dateien](output.md)
    * [Allgemein](output.md#allgemein)
    * [PutenAuswertungZeiten.csv](output.md#putenauswertungzeiten-csv)
    * [PutenAuswertungAufenthalte.csv](output.md#putenauswertungaufenthalte-csv)
 * [Formate](formats.md)
    * [CSV](formats.md#csv)
    * [Zeit](formats.md#zeit)
    * [Datum](formats.md#datum)
 * [Funktion](function.md)
    * [Zuordnungs-Dateien lesen](function.md#zuordnungs-dateien-lesen)
    * [Antennen-Daten lesen](function.md#antennen-daten-lesen)
       * [Antennen-Aufzeichnung verarbeiten](function.md#antennen-aufzeichnung-verarbeiten)
          * [Wenn eine Aufzeichnung ein anderes Datum hat](function.md#wenn-eine-aufzeichnung-ein-anderes-datum-hat)
    * [Zeiten schreiben](function.md#zeiten-schreiben)

**Achtung:** Diese Dokumentation wird als Archiv verbreitet, damit Links in dieser Funktionieren, muss dieses höchst wahrscheinlich entpackt werden.
