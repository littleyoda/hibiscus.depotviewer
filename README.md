hibiscus.depotviewer
====================

Depot-Viewer-Plugin für Hibiscus

Mit Hilfe dieses Plugins können für die folgenden Banken Depot-Informationen abgerufen werden:
* Cortal Consors
* Fondsdepot Bank

Installation des Plugins:
- Menü Datei/Einstellungen
- Reiter "Updates"
- Neues Repository hinzufügen
- "http://www.open4me.de/hibiscus/" in die Textbox eintragen
- Doppel-Klick auf "http://www.open4me.de/hibiscus/"
- Die Einstallation des gewünschten Plugins durch Doppel-Klick beginnen
- Hibiscus neu starten

Einrichtung eines Kontos:

Unter dem Punkt "Konten" den Button "Konto manuell anlegen" anklicken.

Folgende Einstellungen sind im Dialog nötig:

Für CortalConsors

| Feld | Inhalt |
| --------- | ------ |
| Bezeichnung des Kontos | frei wählbar |
| Kontoinhalber | frei wählbar |
| IBAN | leer lassen |
| BIC | leer lassen |
| Verfahren | keine Auswahl nötig |
| Kundenkennung | Kontonummer bzw. UserID, wie auf der Webseite genutzt wird |
| Unterkontonummer | Depot Cortal Consors |
| Bankleitzahl | 0 |
| Offline-Konto | muss aktiv sein |

Für Fondsdepot Bank

| Feld | Inhalt |
| --------- | ------ |
| Bezeichnung des Kontos | frei wählbar |
| Kontoinhalber | frei wählbar |
| IBAN | leer lassen |
| BIC | leer lassen |
| Verfahren | keine Auswahl nötig |
| Kundenkennung | Zugangsnummer, wie auf der Webseite genutzt wird |
| Unterkontonummer | Depot Fondsdepot Bank |
| Bankleitzahl | 0 |
| Offline-Konto | muss aktiv sein |




Nach dem Speichern kann unter Synchronisierungsoptionen das Password hinterlegt werden.

Das ganze Programm basiert von den Beispiel Plugin für Jameica und Hibiscus von Olaf Willuhn.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
