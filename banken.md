Einrichtung eines Kontos
====================



HBCI
-------

Hierzu ist innerhalb von Hibiscus ein Konto anzulegen bzw. automatisch bei der Einrichtung des Bankzuganges anlegen zu lassen.

Anschließend ist die Kontenart auf "Wertpapierkonto" oder "Fonds-Depot" zu ändern.
Die Zugangsart ist auf HBCI zusetzen, nicht auf "Depot Viewer".


| Bank | Unterstützung Bestand (HKWPD)| Unterstützung Umsätze (HKWDU) | Anmerkungen |
| ---- | --------------------- | -------------------- | ----------- | 
| DKB | ja | ja | |
| Cortal Consors | ja | nicht via HBCI | Für diese Bank wurde eine Erweiterung für den Abruf der Umsatz geschrieben. Unter Einstellungen sind deshalb die Zugangsdaten für den Webzugang zu hinterlegen |

Sollte die Bank Umsätze via HBCI nicht unterstützen, äußert sich dieses in der Regel in der Fehlermeldung "Geschäftsvorfall WPDepotUms wird nicht unterstützt". In diesem Fall ist in den Einstellung die Option "Nur Bestand via HBCI abrufen" zu aktivieren!




Sonstige nicht HBCI-Banken
--------------------------
Hierzu ist innerhalb von Hibiscus manuell ein Konto anzulegen.

Für Fondsdepot Bank

| Feld | Inhalt |
| --------- | ------ |
| Bezeichnung des Kontos | frei wählbar |
| Kontoart | Wertpapierdepot
| Kontoinhalber | frei wählbar |
| Kundenkennung | Zugangsnummer, wie auf der Webseite genutzt wird |
| IBAN | leer lassen |
| Kontonummer | 0
| BIC | FODBDE77XXX |
| Bankleitzahl | 77322200
| Offline-Konto | muss deaktiviert(!) sein |
| Unterkontonummer | Depot Fondsdepot Bank |
| Zugangsverfahren | Depot Viewer |



