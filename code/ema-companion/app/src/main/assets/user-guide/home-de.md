<!-- GENERATED from home.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
[Benutzerhandbuch](user-guide.md) › Startseite

# Startseite

Die Startseite ist ein scrollbares Dashboard mit Kacheln. Wische auf dem Bildschirm nach unten, um alle Daten sofort neu abzurufen. Jede Kachel wird beim Öffnen der App oder beim Zurückkehren zur Startseite automatisch aktualisiert.

## Kachel „Aktuelle Produktion"

Zeigt den aktuellen Leistungswert deiner Solaranlage (z. B. „8000 W") sowie den Zeitpunkt der letzten Aktualisierung. Ein neuer Wert wird beim Öffnen der App und bei jeder Rückkehr zur Startseite abgerufen. Erfolgreiche Abrufe sind auf einmal alle 10 Minuten begrenzt; zwischen den Aktualisierungen bleibt der zuletzt abgerufene Wert sichtbar. Bis zur ersten erfolgreichen Abfrage wird ein neutraler Platzhalter („— W") angezeigt.

Schlägt eine Aktualisierung fehl, zeigt die Kachel eine kurze Statuszeile unterhalb des Werts und behält den letzten bekannten Wert bei:

- **Netzwerkproblem – Aktualisierung nicht möglich** — die App konnte den EMA-Dienst nicht erreichen (z. B. keine Verbindung)
- **Authentifizierung fehlgeschlagen – API-Zugangsdaten prüfen** — die Zugangsdaten wurden abgelehnt; prüfe sie in den Einstellungen
- **Produktionsdaten konnten nicht aktualisiert werden** — ein anderer Fehler, z. B. ungültige System-/ECU-ID oder ein Serverproblem

Die Statuszeile verschwindet automatisch, wenn ein späterer Abruf erfolgreich ist.

## Kachel „Heutige Produktion"

Zeigt den stündlichen Energieertrag des heutigen Tages als Liniendiagramm und als Tabelle (Vormittag/Nachmittag) sowie Zusammenfassungskarten.

**Diagramm:** Stellt jede aufgezeichnete Stunde von 06:00 Uhr bis zur aktuellen Stunde dar. Vergangene Stunden werden als durchgehende Linie gezeichnet; die aktuelle (noch laufende) Stunde wird gestrichelt dargestellt. Stunden ohne Daten werden ausgelassen. Das Y-Achsen-Maximum entspricht der konfigurierten Systemleistung (kW), sofern eingestellt; andernfalls skaliert es auf die Daten.

**Tabellen Vormittag / Nachmittag:** Zwei nebeneinander stehende Spalten listen jede Stunde von 00:00 bis 23:00 Uhr mit ihrem kWh-Wert auf. Fehlende Stunden zeigen „—".

**Zusammenfassungskarten** (unterhalb der Tabellen):

- **Heute** — Summe aller vorhandenen Stundenwerte, auf zwei Dezimalstellen gerundet
- **Bester Tag diesen Monat** — Datum und kWh-Gesamtwert des produktivsten Tages im aktuellen Kalendermonat
- **Bester Tag (N Tage)** — Datum und kWh-Gesamtwert des produktivsten Tages im konfigurierten Verlaufszeitraum

Nach dem ersten erfolgreichen Abruf erscheint unterhalb des Diagramms eine Zeile „Aktualisiert HH:mm". Die Daten werden maximal einmal pro Stunde aktualisiert; ein Wischen nach unten erzwingt eine sofortige Aktualisierung.

## Kachel „Produktionsverlauf"

Zeigt tägliche Energiemengen als farblich gekennzeichnetes Balkendiagramm über den konfigurierten Verlaufszeitraum.

**Diagramm:** Ein Balken pro Kalendertag. Balken sind nach Kalendermonat eingefärbt; eine Legende unterhalb des Diagramms zeigt die Farben der einzelnen Monate. Die X-Achse beschriftet jeden zweiten Tag. Das Y-Achsen-Maximum folgt derselben Systemleistungsregel wie das Diagramm „Heutige Produktion".

**Zeitraum-Gesamtwerte** (unterhalb der Legende):

- **Diesen Monat** — gesamte kWh für alle Tage im aktuellen Kalendermonat
- **Letzte 30 Tage** — gesamte kWh für den 30-Tage-Zeitraum bis heute

Tagessummen vergangener Tage werden dauerhaft gespeichert und nie erneut abgerufen. Nur der heutige Balken wird bei jedem Besuch aktualisiert (maximal einmal pro Stunde). Wischen nach unten erzwingt eine sofortige Aktualisierung des heutigen Balkens.

## Kachel „Modulstatus"

Zeigt, ob deine einzelnen Solarmodule in den letzten drei Tagen Energie produziert haben.

| Status | Symbol | Bedeutung |
|---|---|---|
| **Alle Module produzieren** (grün) | ✓ Häkchen | Jedes Modul hat an jedem der letzten drei Tage Energie geliefert |
| **Modul offline** (gelb) | ⚠ Warnung | Ein oder mehrere Module hatten 1–2 aufeinanderfolgende Tage ohne Produktion |
| **Modul offline – Handlung erforderlich** (rot) | ⚠ Warnung | Ein oder mehrere Module hatten 3 aufeinanderfolgende Tage ohne Produktion |
| **Prüfe…** (grau) | ✓ Häkchen | Die erste Hintergrundprüfung wurde noch nicht durchgeführt |

Sobald eine Prüfung abgeschlossen ist, erscheint eine Zeile „Geprüft [Datum] um [Uhrzeit]".

Schlägt eine Prüfung fehl, wechselt das Symbol zu einem grauen **?** und eine kurze Fehlerzeile erscheint:

- **Netzwerkproblem – Prüfung nicht möglich** — die App konnte den EMA-Dienst nicht erreichen
- **Authentifizierung fehlgeschlagen – API-Zugangsdaten prüfen** — die Zugangsdaten wurden abgelehnt
- **Modulstatus konnte nicht geprüft werden** — ein anderer Fehler

**Details anzeigen:** Tippe auf die Kachel bei gelbem oder rotem Status, um den Dialog **Offline-Module** zu öffnen, der jedes betroffene Modul und die Anzahl der Tage ohne Produktion auflistet. Bei grünem oder unbekanntem Status ist die Kachel nicht tippbar.

**Funktionsweise:** Ein Hintergrundjob läuft einmal täglich um 20:00 Uhr in der Zeitzone deiner Anlage. Er ruft drei Tage Moduldaten ab, klassifiziert jedes Modul und speichert vergangene Tage dauerhaft.

**Benachrichtigungen:** Wenn der Modulstatus gelb oder rot ist, sendet EMA Companion eine Push-Benachrichtigung. Unter Android 13 und höher fragt die App beim ersten Start nach der Benachrichtigungsberechtigung – deren Erteilung wird empfohlen.
