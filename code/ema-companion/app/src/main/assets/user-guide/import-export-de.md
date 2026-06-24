<!-- GENERATED from import-export.md by the write-user-guide skill. Source of truth is the English file. Do not hand-edit; edit the English and re-run the skill. -->
# Importieren und Exportieren

Einstellungen können zwischen Geräten übertragen oder gesichert werden, indem die Schaltflächen „Einstellungen importieren" und „Einstellungen exportieren" im Bildschirm [Einstellungen](settings.md) unter **Konfiguration** verwendet werden.

## Exportieren

1. Tippe auf **Einstellungen exportieren**.
2. Wähle **Ohne Verschlüsselung** für eine einfache JSON-Datei oder **Mit PIN verschlüsseln** und gib eine 4-stellige PIN ein.
3. Die Dateiauswahl des Systems öffnet sich — wähle einen Ordner und bestätige. Die Datei wird als `ema-companion-settings.json` gespeichert.

## Importieren

1. Tippe auf **Einstellungen importieren** und wähle eine zuvor exportierte Datei.
2. Wenn die Datei einfaches JSON ist, werden die Einstellungen sofort zusammengeführt.
3. Wenn die Datei verschlüsselt ist, wirst du zur Eingabe der PIN aufgefordert. Bei Eingabe einer falschen PIN wird ein Fehler angezeigt und alle Einstellungen bleiben unverändert.
4. Nur die in der Datei vorhandenen Felder werden aktualisiert; alle anderen Felder behalten ihre aktuellen Werte.

Ein Protokolleintrag wird erstellt, der die importierten Felder auflistet. Sensible Werte wie dein App-Geheimnis werden nie vollständig angezeigt — sie erscheinen als `[hidden]` im Protokoll.
