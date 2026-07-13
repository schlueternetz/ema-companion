<!-- GENERATED from widgets.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
[Benutzerhandbuch](user-guide.md) › Startbildschirm-Widgets

# Startbildschirm-Widgets

EMA Companion bietet drei Android-Startbildschirm-Widgets, mit denen du deine Solarproduktion auf einen Blick siehst, ohne die App zu öffnen. Füge sie wie jedes andere Widget hinzu: Halte eine freie Stelle auf deinem Startbildschirm gedrückt, wähle **Widgets**, suche **EMA Companion** und ziehe eines auf deinen Startbildschirm.

## Heutige Produktion

Zeigt die heutige Stundenproduktion als Liniendiagramm (06:00 bis zur aktuellen Stunde) sowie eine laufende Gesamtsumme in kWh. Abgeschlossene Stunden werden durchgezogen dargestellt; die aktuelle, noch laufende Stunde gestrichelt. Das Maximum der Y-Achse folgt deiner konfigurierten Systemleistung, genau wie beim entsprechenden Diagramm auf der Startseite.

## Produktionsübersicht

Zeigt drei fett gedruckte Werte auf einen Blick: **Heute**, **Diesen Monat** und **Letzte 30 Tage**, jeweils in kWh. „Heute" stammt aus denselben Stundendaten wie das Widget „Heutige Produktion"; die beiden anderen Werte stammen aus deinem täglichen Produktionsverlauf.

## Produktionshistorie

Zeigt ein Balkendiagramm der Tagessummen über deinen konfigurierten Zeitraum (Einstellungen → Historische Datentage), farblich nach Kalendermonat codiert — dasselbe Diagramm wie die Kachel „Produktionshistorie" auf der Startseite, nur in Widget-Größe.

## In den Einstellungen deaktiviert

Jedes Widget lässt sich einzeln unter Einstellungen → Kacheln & Widgets deaktivieren. Da Android Apps keine Möglichkeit bietet, ein bereits platziertes Widget zu entfernen, verschwindet ein deaktiviertes Widget nicht — es zeigt stattdessen **„Dieses Widget wurde in den Einstellungen deaktiviert"** anstelle seines üblichen Diagramms oder seiner Werte, sowohl für bestehende als auch für neu platzierte Widgets. Aktiviere es in den Einstellungen wieder, um den normalen Inhalt wiederherzustellen.

## Noch nicht konfiguriert

Wenn du deine EMA-Zugangsdaten noch nicht in den Einstellungen eingegeben hast, zeigt jedes Widget stattdessen die neutrale Meldung **„Nicht konfiguriert – EMA Companion öffnen"** anstelle eines Diagramms oder von Werten. Ein Tippen auf das Widget öffnet die App direkt in den Einstellungen, damit du die Einrichtung abschließen kannst.

## Wenn eine Aktualisierung fehlschlägt

Widgets zeigen immer entweder aktuelle Daten oder einen klaren Grund, warum sie fehlen — nie ein möglicherweise veraltetes Diagramm ohne Erklärung. Ist der letzte Aktualisierungsversuch fehlgeschlagen, wird der betroffene Inhalt durch eine kurze Meldung ersetzt:

- **Netzwerkproblem – Stundendaten konnten nicht aktualisiert werden** — die App konnte den EMA-Dienst nicht erreichen
- **Authentifizierung fehlgeschlagen – API-Zugangsdaten prüfen** — deine Zugangsdaten wurden abgelehnt
- **Heutige Produktionsdaten konnten nicht aktualisiert werden** — ein anderer Fehler, z. B. ein Serverproblem

Beim Widget „Produktionsübersicht" zeigt nur der betroffene Wert einen Fehler — ein Fehler bei den Stundendaten ersetzt nur „Heute"; ein Fehler bei den Tagesdaten ersetzt nur „Diesen Monat" und „Letzte 30 Tage". Die Meldung verschwindet automatisch, sobald die nächste Aktualisierung erfolgreich ist.

Solange ein Widget einen Fehler anzeigt (oder die App noch nicht konfiguriert ist), öffnet ein Tippen darauf die Einstellungen statt der Startseite, damit du deine Zugangsdaten oder das API-Protokoll direkt prüfen kannst.

## Erscheinungsbild

Jedes Widget folgt der in den Einstellungen gewählten Anzeigemodus-Einstellung (System, Hell oder Dunkel) — derselben Wahl, die bereits in der App gilt. Änderst du den Anzeigemodus, passen die Widgets ihre Farben bei der nächsten Aktualisierung an.

## Aktualität

Widgets aktualisieren sich automatisch im Hintergrund etwa alle zwei Stunden, und sofort, wenn du die App öffnest oder eine Verbindungseinstellung änderst (Zugangsdaten, Basis-URL, Import oder Werksreset) in den Einstellungen. Hintergrundaktualisierungen teilen sich dieselben zwischengespeicherten Daten und dasselbe API-Aufrufbudget wie die Kacheln in der App — das Platzieren von Widgets verbraucht keine zusätzlichen API-Aufrufe über die der App hinaus.
