# Formate
Diese Datei enthält Informationen über die Input-/Ergebnis- Formate die dieses Programm verwendet.  
Diese Formate sind häufige Formate, da allerdings verschieden Versionen des selben Formats existieren können, ist hier eine Erklärung der Version die dieses Programm verwendet.

Erklärte Formate:
 * [CSV](#csv)
 * [Zeit(Uhrzeit/vergangene Zeit)](#zeit)
 * [Datum](#datum)

[Input-Dateien]: input.md

## CSV
Dieses Programm verwendet eine [CSV](https://de.wikipedia.org/wiki/CSV_(Dateiformat)) Version für seine Input-/Ergebnis- Dateien.  
Das CSV Format das dieses Programm verwendet ist darauf ausgelegt die notwendigen Funktionen für dieses Programm zu haben, und möglichst universell einlesbar zu sein, nicht auf eine offizielle Spezifikation.

**Achtung:** Dieses CSV Format ist **NICHT VOLLSTÄNDIG [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180) KONFORM**.

In [Ergebnis-Dateien](output.md) verwendet dieses Programm Semikolons als Werte-Trennzeichen.  
In [Input-Dateien] für dieses Programm können Kommas, Semikolons und Tabulatoren als Trennzeichen verwendet werden.  
Diese können in der selben Datei gemischt sein.

In Ergebnissen dieses Programms haben alle Zeilen die selbe Anzahl Spalten.  
In [Input-Dateien] können Zeilen in der selben Datei verschieden viele Spalten haben.

[Ergebnis-Dateien](output.md) haben eine Titel-Zeile mit Namen für alle Spalten.  
[Input-Dateien] können eine Titel-Zeile als erste Zeile enthalten, müssen dies allerdings nicht.

Dieses Programm kann Anführungszeichen als Wert-Begrenzer nicht verarbeiten.  
Diese werden als Teil des Wertes verarbeitet, und die Zeile die diese enthält wird damit ungültig.

## Zeit
Zeiten können sowohl Uhrzeiten als auch Zeiten zwischen zwei Ereignissen sein.  
Dieses Format ist sowohl das Format das für Uhrzeiten in [Input-Dateien] verwendet werden muss, als auch das Format der Uhrzeiten und Aufenthaltszeiten in Ergebnissen.

Das Grundlegende Format ist `Stunden:Minuten:Sekunden.Hundertstel`.

In Ergebnissen ist Stunden zwei oder mehr Ziffern.  
Ergebnis-Zeiten haben immer zwei stellen für Minuten, Sekunden und Hundertstel.

In [Input-Dateien] können alle Teile(Stunden, Minuten, Sekunden, Hundertstel) ein oder zwei Ziffern sein.  
Hundertstel sind im Input optional.  
Der Input kann keine Millisekunden enthalten.  
Wenn Hundertstel nur eine Stelle hat wird diese als Zehntel verarbeitet.

## Datum
Das Datumsformat dieses Programms, sowohl für [Input-Dateien], als auch für Ergebnisse ist `DD.MM.YYYY`.

In Ergebnissen hat das Format immer zwei Ziffern für Tag und Monat, und vier\* für das Jahr.  
In Inputs kann ein Datum ein oder zwei Ziffern für den Tag und Monat, und ein oder mehr für das Jahr haben.

\* Außer wenn Inputs ein Datum mit mehr als vier Ziffern enthalten.
