<!-- GENERATED from settings.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
[Benutzerhandbuch](user-guide.md) › Einstellungen

# Einstellungen

Der Bildschirm Einstellungen ist scrollbar und in Abschnitte gegliedert.

## Solaranlagen-Einstellungen

Diese Felder verbinden EMA Companion mit deinem APsystems-Konto und deiner Solaranlage. Alle fünf Zugangsdaten-Felder sind Pflichtfelder — jedes Feld, das noch nicht ausgefüllt wurde, zeigt **Erforderlich** als Hinweis.

Alle Felder verwenden ein integriertes Bearbeitungsmuster: Tippe auf das Symbol **Bearbeiten** (Stift) neben einem Feld, gib einen Wert ein, tippe dann auf **Speichern** (Häkchen) oder drücke die Eingabetaste. Tippe auf **Abbrechen** (X), um die Änderung zu verwerfen. Es kann jeweils nur ein Feld gleichzeitig bearbeitet werden — das Öffnen eines zweiten Felds speichert automatisch den ausstehenden Wert des ersten Felds (oder lässt das erste Feld geöffnet mit einer Fehlermeldung, falls der Wert ungültig ist). Nur Abbrechen verwirft eine Änderung.

Jedes Feld hat eine Schaltfläche **Info** (ⓘ). Tippe darauf, um zu sehen, wo du diesen Wert in der EMA-App findest. Die Info-Schaltfläche wird ausgeblendet, wenn sich ein Feld im Bearbeitungsmodus befindet.

| Feld | Format | Hinweise |
|---|---|---|
| **EMA App-ID** | 32 alphanumerische Zeichen | Erforderlich; wird in Kleinbuchstaben gespeichert |
| **EMA App-Geheimnis** | 12 alphanumerische Zeichen | Erforderlich; wird maskiert angezeigt (nur die letzten 4 Zeichen sichtbar); das Feld wird beim Wechsel in den Bearbeitungsmodus geleert |
| **EMA System-ID** | 16 alphanumerische Zeichen | Erforderlich; wird in Großbuchstaben gespeichert |
| **EMA ECU-ID** | 12 Ziffern | Erforderlich; numerische Tastatur |
| **Systemleistung** | Positive Zahl bis 2.000 | Erforderlich; wird mit „kW"-Suffix angezeigt; bis zu 2 Dezimalstellen |
| **Zeitzone der Anlage** | IANA-Zeitzonenkennung | Tippen öffnet eine durchsuchbare Liste; Standard ist die Gerätezeitzone |

Wenn du einen Wert eingibst, der die Formatregeln nicht erfüllt, erscheint eine Fehlermeldung unterhalb des Felds und Speichern funktioniert erst, wenn die Eingabe korrigiert wurde.

Das Ändern der **Zeitzone der Anlage** plant den täglichen Modulstatus-Job sofort neu auf 20:00 Uhr in der neu gewählten Zeitzone.

### Wo du diese Werte in der EMA-App findest

> **Voraussetzung:** Die App-ID und das App-Geheimnis erscheinen erst, nachdem der OpenAPI-Zugang aktiviert wurde. Gehe in der EMA-App zu **Einstellungen → OpenAPI-Dienst** und aktiviere ihn zuerst.

| Feld | Wo zu finden |
|---|---|
| **EMA App-ID** | **Einstellungen → OpenAPI-Dienst → Entwickler-Autorisierung** — die APP ID |
| **EMA App-Geheimnis** | **Einstellungen → OpenAPI-Dienst → Entwickler-Autorisierung** — das APP Secret |
| **EMA System-ID** | **Einstellungen → Kontodetails** — der `sid`-Wert |
| **EMA ECU-ID** | **Einstellungen → ECU** — die ECU-ID |
| **Systemleistung** | **Startbildschirm** — der Kapazitätswert |

> **Wichtig:** Wenn die EMA API 6 aufeinanderfolgende Monate nicht genutzt wird, kann APsystems den Zugang automatisch widerrufen. Wenn EMA Companion keine Daten mehr abruft, öffne die EMA-App und prüfe, ob der OpenAPI-Zugang unter **Einstellungen → OpenAPI-Dienst** noch aktiv ist.

## App-Einstellungen

| Einstellung | Details |
|---|---|
| **Sprache** | Tippen wählt aus: System (Gerätestandard), Englisch oder Deutsch. Jeder Sprachname wird immer in dieser Sprache angezeigt (z. B. „Deutsch" für Deutsch), damit er auch erkennbar ist, wenn die App gerade auf eine Sprache eingestellt ist, die du nicht lesen kannst. Wirkt sofort. |
| **Anzeigemodus** | Tippen wählt aus: System (folgt OS Hell/Dunkel-Modus), Hell oder Dunkel. Wirkt sofort. |
| **Benachrichtigungen** | Tippen wählt aus: Aus, Nur Warnungen oder Alle. Standard ist Nur Warnungen. Wirkt sofort — siehe „Wann Benachrichtigungen gesendet werden" unter E-Mail-Benachrichtigungen. |
| **Verlaufszeitraum (Tage)** | Anzahl der Tage des gespeicherten Produktionsverlaufs (1–90); Standard ist 30. |

## Kacheln & Widgets

Wähle, welche Startseiten-Kacheln und Startbildschirm-Widgets aktiv sind. Alle sieben sind standardmäßig aktiviert.

- **Kacheln:** Heutige Produktion, Produktionsverlauf, Modulstatus. Deaktivierst du eine Kachel, verschwindet ihre Karte beim nächsten Aufruf der Startseite.
- **Widgets:** Heutige Produktion, Produktionsübersicht, Produktionshistorie. Android bietet Apps keine Möglichkeit, ein bereits platziertes Widget zu entfernen. Deaktivierst du ein Widget, zeigt es stattdessen **„Dieses Widget wurde in den Einstellungen deaktiviert"** anstelle seiner Daten — sowohl für bereits platzierte als auch für nachträglich platzierte Widgets.

Eine Schaltfläche **Alle auswählen / Alle abwählen** oben im Abschnitt schaltet alle Kontrollkästchen gleichzeitig um: Sie zeigt „Alle abwählen", solange alles aktiviert ist, und „Alle auswählen", sobald mindestens eines deaktiviert ist.

Daten werden nur für Kacheln oder Widgets von der EMA API abgerufen, die aktuell aktiviert sind — deaktivierst du alles, was von einer bestimmten Datenart abhängt (z. B. alle Verbraucher von Tagesdaten), stoppen diese API-Aufrufe vollständig.

Diese Einstellungen sind in Import/Export enthalten und werden beim Werksreset wie die übrigen Einstellungen auf „alle aktiviert" zurückgesetzt.

## API-Einstellungen

| Steuerung | Beschreibung |
|---|---|
| **API-Anfragelimit** | Maximale EMA API-Aufrufe pro Monat (1–2.678.400; Standard 1.000). Ein Fortschrittsbalken unterhalb des Felds zeigt, wie viele der **erfolgreichen** Lesevorgänge dieses Monats bereits verbraucht wurden. Der Zähler wird automatisch zu Beginn jedes Kalendermonats zurückgesetzt. Tippe auf ↺, um den Standard wiederherzustellen. |
| **Basis-URL** | Der API-Endpunkt (Standard: `https://api.apsystemsema.com:9282/user/api/v2/`). Muss eine gültige URL mit bis zu 2.048 Zeichen sein. Tippe auf ↺, um den Standard wiederherzustellen. |

> **Nur in Entwickler-Builds:** In Debug-Builds erscheint unterhalb von Basis-URL eine Schaltfläche **Lokalen Stub verwenden**, die die App auf einen lokalen Testserver statt auf die echte EMA API umleitet. In der veröffentlichten App ist sie nicht vorhanden.

## E-Mail-Benachrichtigungen

Sendet eine E-Mail zu deinem Modulstatus. E-Mails werden über dein eigenes Gmail-Konto mit einem App-Passwort verschickt — kein Drittanbieter-Relay erforderlich. Wie bei Benachrichtigungen wählst du bei **E-Mail-Benachrichtigungen** durch Tippen eine Stufe (Aus, Nur Warnungen oder Alle) — siehe „Wann Benachrichtigungen gesendet werden" unten.

**Voraussetzung:** Ein Gmail-Konto mit aktivierter 2-Schritt-Verifizierung.

### E-Mail-Benachrichtigungen einrichten

1. Tippe auf **E-Mail-Benachrichtigungen** und wähle **Nur Warnungen** oder **Alle**. Darunter erscheint ein Einrichtungsformular.
2. Tippe auf **Google-Konto öffnen ↗**, um `myaccount.google.com/apppasswords` in deinem Browser zu öffnen.
3. Gehe in deinem Google-Konto zu **Sicherheit → App-Passwörter** und erstelle eines für „Mail" auf „Anderes Gerät". Kopiere das angezeigte 16-stellige Passwort.
4. Gib in EMA Companion deine Gmail-Adresse ein und füge das App-Passwort ein. Leerzeichen im App-Passwort werden automatisch entfernt.
5. Tippe auf **Speichern**. Die App prüft das Format (eine gültige E-Mail-Adresse und ein 16-stelliges App-Passwort) und speichert sofort — eine Netzwerkverbindung ist nicht erforderlich. Bei ungültigem Format erscheint eine Fehlermeldung unterhalb der Schaltfläche.

### E-Mail-Benachrichtigungen verwalten

Sobald Zugangsdaten gespeichert sind, zeigt der Verwaltungsbereich deine konfigurierte E-Mail-Adresse und eine Schaltfläche **Zugangsdaten bearbeiten**. Tippe darauf, um das Formular mit vorausgefüllter E-Mail-Adresse und leerem Passwortfeld zu öffnen.

Im Bearbeitungsmodus erscheinen unterhalb der Speichern-Schaltfläche zwei weitere Aktionen:

| Aktion | Funktion |
|---|---|
| **Test-E-Mail senden** | Sendet eine echte Test-Nachricht an deine konfigurierte Adresse, um die Zugangsdaten vollständig zu prüfen. Das Ergebnis (Erfolg oder Fehler) wird direkt angezeigt. |
| **Zugangsdaten löschen** | Zeigt einen Bestätigungsdialog. Tippe auf **Löschen**, um deine gespeicherten Gmail-Zugangsdaten zu entfernen und alle Benachrichtigungs-E-Mails zu stoppen. |

Tippe auf **Abbrechen**, um den Bearbeitungsmodus ohne Speichern zu verlassen.

Wählst du **Aus**, werden Benachrichtigungen pausiert, aber der Verwaltungsbereich bleibt sichtbar (falls Zugangsdaten gespeichert sind), sodass du wieder einschalten kannst, ohne das App-Passwort erneut einzugeben. Das Speichern von Zugangsdaten im Einrichtungsformular schaltet Benachrichtigungen immer ein (Nur Warnungen), falls sie aus waren.

### Wann Benachrichtigungen gesendet werden

Benachrichtigungen und E-Mail-Benachrichtigungen haben jeweils eine eigene, unabhängige Stufe — du kannst z. B. eine tägliche E-Mail-Zusammenfassung erhalten und gleichzeitig Push-Benachrichtigungen auf Nur Warnungen belassen:

| Stufe | Verhalten |
|---|---|
| **Aus** | Dieser Kanal sendet nie. |
| **Nur Warnungen** | Sendet nur, wenn sich dein Modulstatus tatsächlich ändert — eine Verschlechterung (GRÜN → GELB → ROT) oder eine Wiederherstellung zurück zu GRÜN. Das ist das ursprüngliche Verhalten. |
| **Alle** | Sendet einmal täglich bei jeder Hintergrundprüfung, auch wenn der Status unverändert und weiterhin GRÜN ist — eine tägliche Bestätigung, dass die Prüfungen noch laufen, damit ein stiller Ausfall nicht unbemerkt bleibt. |

Das Ändern der EMA-Zugangsdaten setzt den Benachrichtigungsverlauf zurück, sodass die nächste Prüfung bei Bedarf eine neue Benachrichtigung sendet.

## Protokolle

Zeichnet EMA API-Aktivitäten auf, neueste zuerst. Jede Zeile zeigt Uhrzeit, Endpunkt, Dauer (Millisekunden) und Erfolg oder Fehler. Das Importieren von Einstellungen erstellt ebenfalls einen Protokolleintrag mit den importierten Feldern — sensible Werte werden nicht angezeigt. Wenn noch keine Einträge vorhanden sind, zeigt der Abschnitt „Noch keine API-Aufrufe aufgezeichnet".

Die Protokollliste ist in ihrem eigenen Bereich unabhängig scrollbar. Tippe auf einen Eintrag, um ein Detaildialog mit der vollständigen Anfrage und Antwort zu öffnen. Das Protokoll behält die letzten 100 Aufrufe; ältere Einträge werden automatisch gelöscht.

## Konfiguration

| Steuerung | Beschreibung |
|---|---|
| **Einstellungen importieren** | Öffnet die Dateiauswahl zum Auswählen einer JSON-Einstellungsdatei. Einfaches JSON wird sofort zusammengeführt; verschlüsselte Dateien fordern zur Eingabe einer 4-stelligen PIN auf. |
| **Einstellungen exportieren** | Speichert alle Einstellungen als `ema-companion-settings.json` an einem Ort deiner Wahl. Wähle keine Verschlüsselung oder eine 4-stellige PIN. |
| **Werksreset** | Löscht dauerhaft alle Einstellungen, den API-Anfragezähler, Protokolle und den Modulstatus-Verlauf. Vor dem Löschen erscheint ein Bestätigungsdialog. |

Schritt-für-Schritt-Anleitungen zum Importieren und Exportieren findest du unter [Importieren und Exportieren](import-export.md).
