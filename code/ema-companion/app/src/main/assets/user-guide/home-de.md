<!-- GENERATED from home.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
# Startseite

Die Startseite ist ein Dashboard mit Kacheln. Jede Kachel ruft Live-Daten ab, wenn du die App öffnest oder zu diesem Bildschirm zurückkehrst.

## Kachel „Aktuelle Produktion"

Zeigt den aktuellen Leistungswert deiner Solaranlage (z. B. „8000 W") und die Zeit der letzten Aktualisierung. Ein neuer Wert wird abgerufen, wenn du die App öffnest und wenn du zur Startseite zurückkehrst. Erfolgreiche Messwerte sind auf einmal alle 10 Minuten begrenzt; zwischen den Abrufen bleibt der zuletzt abgerufene Wert in der Kachel. Bis zum ersten erfolgreichen Messwert wird ein neutraler Platzhalter („— W") angezeigt.

Schlägt eine Aktualisierung fehl, zeigt die Kachel eine kurze Statuszeile unterhalb des Werts und behält den zuletzt bekannten Wert bei:

- **Netzwerkproblem – keine Aktualisierung** — die App konnte den EMA-Dienst nicht erreichen (z. B. keine Verbindung)
- **Authentifizierung fehlgeschlagen – API-Zugangsdaten prüfen** — deine Zugangsdaten wurden abgelehnt; prüfe sie in den Einstellungen
- **Produktionsdaten konnten nicht aktualisiert werden** — ein anderer Fehler, z. B. eine ungültige System- oder ECU-ID oder ein Serverproblem

Der Status wird automatisch gelöscht, sobald ein späterer Abruf erfolgreich ist. Ein fehlgeschlagener Abruf zählt **nicht** gegen dein monatliches Anfragelimit und wird beim nächsten Öffnen oder Zurückkehren zur Startseite erneut versucht. Das Korrigieren deiner Zugangsdaten oder Verbindungseinstellungen in den Einstellungen löst beim Zurückkehren zur Startseite sofort einen neuen Versuch aus.

## Kachel „Modulstatus"

Zeigt an, ob deine einzelnen Solarmodule in den letzten drei Tagen produziert haben. Die Kachel folgt demselben Aufbau wie die Kachel „Aktuelle Produktion": eine Titelzeile, eine Inhaltszeile mit Statussymbol und Statustext nebeneinander sowie eine Fußzeile mit dem Zeitpunkt der letzten Prüfung.

| Status | Symbol | Bedeutung |
|---|---|---|
| **Alle Module produzieren** (grün) | ✓ Häkchen | Jedes Modul hat an jedem der letzten drei Tage Energie gemeldet |
| **Modul offline** (gelb) | ⚠ Warnung | Ein oder mehrere Module hatten 1–2 aufeinanderfolgende Tage keine Produktion |
| **Modul offline – Handlungsbedarf** (rot) | ⚠ Warnung | Ein oder mehrere Module hatten 3 aufeinanderfolgende Tage keine Produktion |
| **Wird geprüft…** (grau) | ✓ Häkchen | Die erste Hintergrundprüfung ist noch nicht durchgeführt worden |

Sobald eine Prüfung abgeschlossen ist, erscheint darunter eine Zeile „Geprüft am [Datum] um [Uhrzeit]".

Schlägt eine Prüfung fehl, zeigt die Kachel eine kurze Statuszeile unterhalb des Prüfzeitstempels:

- **Netzwerkproblem – Prüfung nicht möglich** — die App konnte den EMA-Dienst nicht erreichen
- **Authentifizierung fehlgeschlagen – API-Zugangsdaten prüfen** — deine Zugangsdaten wurden abgelehnt; prüfe sie in den Einstellungen
- **Modulstatus konnte nicht geprüft werden** — ein anderer Fehler, z. B. eine ungültige ECU-ID oder ein Serverproblem

**Details anzeigen:** Tippe auf die Kachel, wenn sie gelb oder rot zeigt, um den Dialog **Offline-Module** zu öffnen. Der Dialog listet jedes betroffene Modul mit seiner ID und der Anzahl der aufeinanderfolgenden Tage ohne Produktion auf. Die Kachel ist nicht antippbar, wenn alle Module grün sind oder der Status noch nicht bekannt ist.

**Funktionsweise der Prüfung:** Ein Hintergrundjob läuft automatisch einmal täglich um 20:00 Uhr in der Zeitzone deiner Anlage (in den Einstellungen konfiguriert). Er ruft die Energiedaten der letzten drei Tage pro Modul von der EMA API ab und klassifiziert jedes Modul einzeln. Die Daten von gestern und vorgestern werden zwischengespeichert; nur die heutigen Daten werden erneut abgerufen. Die Kachel zeigt das Ergebnis der letzten abgeschlossenen Prüfung sofort beim Öffnen der Startseite; der Hintergrundjob aktualisiert sie lautlos um 20:00 Uhr, ohne dass die App geöffnet sein muss.

**Benachrichtigungen:** Wenn die Modulstatus-Prüfung einen gelben oder roten Status findet, sendet EMA Companion eine Push-Benachrichtigung, sodass du auch bei geschlossener App informiert wirst. Die Benachrichtigung wird bei jeder täglichen Prüfung ersetzt (nicht gestapelt), bis der Status wieder grün wird. Unter Android 13 und höher fragt die App beim ersten Start nach der Benachrichtigungsberechtigung — es wird empfohlen, diese zu erteilen, um diese Meldungen zu erhalten.
