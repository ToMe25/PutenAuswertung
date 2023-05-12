# PutenAuswertung
This is a data analysis program made to analyze area transponder records.  
In general it analyzes transponder records from zone borders to get the times each entity spent in each zone.  
It also tracks individual stays for each entity.

While it is specifically developed for a single use case with turkeys it should be usable for way more general use-cases.

## Inputs
This program uses three input files.  
One mapping transponder ids to turkey(entity) ids,  
one mapping antenna ids to zone ids,  
and one containing the actual antenna records to analyze.  
These all have to be in the directory the program is executed in.

Ids can contain letters, digits, spaces, and hyphens.  
Time has to be formatted as `HH:MM:SS.2`.  
Dates have to be formatted as `DD.MM.YYYY`.  
This program supports semicolons, commas, and tabs as csv separator chars.

The mappings files have to be csvs with first the entity(turkey)/zone id, and then an arbitrary amount of transponder/antenna ids.  
The have to be called `Puten.csv`(turkeys in german) and `Bereiche.csv`(zones/areas in german).

The antenna data file has to be called `AntennenDaten.csv` and has to contain the following columns:
 * transponder: The transponder that was recorded.
 * date: The date at which the transponder was recorded.
 * time: The time of day at which the transponder was recorded.
 * antenna: The antenna that recorded the transponder.

If the first line contains the titles(headers) for each column, the order doesn't matter.  
If it doesn't the order has to be as specified above.

## Outputs
This program produces two output files.  
One with the per day stay times per turkey per zone,  
and one with the individual stays in each zone.

The file containing the zone times per day(`PutenAuswertungZeiten.csv`) has these columns:
 * Tier: The entity that this line is about.
 * Datum: The date for which this line is.
 * Bereichswechsel: The number of times the specified turkey changed the zone it was in on the specified day.
 * One "Zeit in Zone X" column per zone: The time the specified turkey spent in the specified zone that day.

The file containing the individual stays(`PutenAuswertungAufenthalte.csv`) has the following columns:
 * Tier: The entity that stayed in the specified zone for the specified time.
 * Bereich: The zone the entity stayed in during the specified time.
 * Startdatum: The date the entity entered the zone.
 * Startzeit: The time of day at which the entity entered the zone.
 * Enddatum: The date at which the entity left the zone.
 * Endzeit: The time of day at which the entity left the zone.
 * Aufenthaltszeit: The time the entity spent in the specified zone.

Both of these files are semicolon separated csvs.

## Analysis
The analysis is done with these assumptions:
 * Each entity existed since the first record of any entity, and was in its first recorded zone since the beginning.
 * Each entity existed since the last record of any entity, and was in its last recorded zone until the end.
 * Stays of less than five minutes are errors/irrelevant, and are considered to be spent in the last zone in which the entity spent more than five minutes.
 * Stays of less than five minutes are possible at the very start and end of a recording.
