<!-- GENERATED from settings.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
[Benutzerhandbuch](user-guide.md) › Einstellungen

# Einstellungen

Der Bildschirm Einstellungen ist scrollbar und in Abschnitte gegliedert.

## Solaranlagen-Einstellungen

Diese Felder verbinden EMA Companion mit deinem APsystems-Konto und deiner Solaranlage. Alle fünf Zugangsdaten-Felder sind Pflichtfelder — jedes Feld, das noch nicht ausgefüllt wurde, zeigt **Erforderlich** als Hinweis.

Alle Felder verwenden ein integriertes Bearbeitungsmuster: Tippe auf das Symbol **Bearbeiten** (Stift) neben einem Feld, gib einen Wert ein, tippe dann auf **Speichern** (Häkchen) oder drücke die Eingabetaste. Tippe auf **Abbrechen**, um die Änderung zu verwerfen. Es kann jeweils nur ein Feld gleichzeitig bearbeitet werden — das Öffnen eines zweiten Felds schließt automatisch das erste.

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
| **Sprache** | Tippen wählt aus: System (Gerätestandard), Englisch oder Deutsch. Wirkt sofort. |
| **Anzeigemodus** | Tippen wählt aus: System (folgt OS Hell/Dunkel-Modus), Hell oder Dunkel. Wirkt sofort. |
| **Benachrichtigungen aktiviert** | Schalter; standardmäßig aktiviert. Wirkt sofort — kein Speichern erforderlich. |
| **Verlaufszeitraum (Tage)** | Anzahl der Tage des gespeicherten Produktionsverlaufs (1–90); Standard ist 30. |

## API-Einstellungen

| Steuerung | Beschreibung |
|---|---|
| **API-Anfragelimit** | Maximale EMA API-Aufrufe pro Monat (1–2.678.400; Standard 1.000). Ein Fortschrittsbalken unterhalb des Felds zeigt, wie viele der **erfolgreichen** Lesevorgänge dieses Monats bereits verbraucht wurden. Der Zähler wird automatisch zu Beginn jedes Kalendermonats zurückgesetzt. Tippe auf ↺, um den Standard wiederherzustellen. |
| **Basis-URL** | Der API-Endpunkt (Standard: `https://api.apsystemsema.com:9282/user/api/v2/`). Muss eine gültige URL mit bis zu 2.048 Zeichen sein. Tippe auf ↺, um den Standard wiederherzustellen. |

## E-Mail-Benachrichtigungen

Sendet eine E-Mail, wenn sich der Modulstatus ändert (GRÜN → GELB oder ROT und zurück zu GRÜN). E-Mails werden über dein eigenes Gmail-Konto mit einem App-Passwort verschickt — kein Drittanbieter-Relay erforderlich.

**Voraussetzung:** Ein Gmail-Konto mit aktivierter 2-Schritt-Verifizierung.

### E-Mail-Benachrichtigungen aktivieren

1. Schalte **E-Mail-Benachrichtigungen** ein. Unterhalb des Schalters erscheint ein Einrichtungsformular.
2. Tippe auf **Google-Konto öffnen ↗**, um `myaccount.google.com/apppasswords` in deinem Browser zu öffnen.
3. Gehe in deinem Google-Konto zu **Sicherheit → App-Passwörter** und erstelle eines für „Mail" auf „Anderes Gerät". Kopiere das angezeigte 16-stellige Passwort.
4. Gib in EMA Companion deine Gmail-Adresse ein und füge das App-Passwort ein.
5. Tippe auf **Prüfen & Speichern**. Die App verbindet sich mit Gmail, um die Zugangsdaten zu bestätigen. Bei Erfolg wird das Einrichtungsformular durch eine Statuszeile mit der verwendeten Adresse ersetzt. Schlägt die Verbindung fehl, erscheint eine Fehlermeldung — überprüfe, ob das App-Passwort korrekt kopiert wurde.

### E-Mail-Benachrichtigungen deaktivieren

Tippe auf die Zeile **„E-Mail-Benachrichtigungen aktiv für: …"**. Ein Bestätigungsdialog erscheint — tippe auf **Deaktivieren**, um die Zugangsdaten zu entfernen und den E-Mail-Versand zu beenden.

### Wann E-Mails gesendet werden

Eine E-Mail wird einmal pro Statusänderung gesendet, nicht bei jeder Hintergrundprüfung. Wenn ein Modul zwei Tage offline war (GELB) und ein dritter Tag ohne Produktion vergeht (ROT), wird eine zweite E-Mail gesendet. Wenn alle Module wieder produzieren, wird eine Wiederherstellungs-E-Mail gesendet. Das Ändern der EMA-Zugangsdaten setzt den E-Mail-Verlauf zurück, sodass die nächste Prüfung bei Bedarf eine neue Benachrichtigung sendet.

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
