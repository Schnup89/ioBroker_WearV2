# ioBroker_WearV2
WearOS Application mit SocketIO und JetpackCompose


## Konfiguration ioBroker
Der [SocketIO-Adapter](https://github.com/ioBroker/ioBroker.socketio) muss installiert sein.
Standardmäig nutzt dieser den Port 8084, den merken wir uns für später.

**Grundkonfiguration**
- Raum "WearOS" unter Aufzählung->Räume erstellen
- Die auf der Uhr dargestellten Objects/States müssen dem Raum "WearOS" zugeteilt werden -> Siehe Punkt "Objects-Anordnung"

![image](https://user-images.githubusercontent.com/28166743/150635785-b8b4d6be-9404-412b-8c38-814864c167bb.png)



**Objekt Eigenschaften**
- Um ein Object/State nach eigenen Wünschen daruzustellen, gibt es folgende Parameter im Object-JSON:

Bild | JSON-Param       | Funktion                | Datentyp  | Bemerkung  |
---- | ---------------- | ----------------------- | --------- | ---------- |
1    | "common"-"name"  | Anzeigename           | Zeichenk.   |
2    | "common"-"unit"  | Einheit (%,°C, etc)   | Zeichenk.   |
3    | "common"-"icon"  | Icon                  | Zeichenk.   | Setzen über "common"-Reiter
4    | "common"-"write" | ReadOnly wenn false   | Boolean     | 
5    | "common"-"color" | Farbe bei aktiv.      | Zeichenk.   | Setzen über "common"-Reiter
6    | "common"-"min"   | Bei Slider Min Wert   | Zahl        | Setzen über "common"-Reiter
7    | "common"-"max"   | Bei Slider Man Wert   | Zahl        | Setzen über "common"-Reiter
8    | "common"-"role"  | Anzeigetyp            | Zeichenk.   | Setzen über "common"-Reiter



![image](https://user-images.githubusercontent.com/28166743/150635645-0b8cd1ad-fecb-432a-87d0-0ceeb7a98afd.png)
![image](https://user-images.githubusercontent.com/28166743/150635717-40ae2677-9da2-4fe0-8f90-2699425a278c.png)


** Anzeigetyp **  
Die Rolle eines Objekt definiert den Anzeigetyp:  
Switch = Toggle  
Level = Slider  
Alles andere = Anzeige  
  
  
  
## Anordnung der Objekte
Leider ist es aktuell in ioBroker nicht möglich Objekte in Räumen per "Drag&Drop" zu sortieren.  
Die Objekte in den Räume können hier in der Reihenfolge geändert werden:  
Objekt-Baum mit Expertenansicht öffnen und mit dem Schraubenschlüssel bei "enum.rooms.WearOS" in der JSON die Reihenfolge von "memers" bearbeiten.  
! Bei einer Zuordnung von Object über den Objekt-Baum wird die Reihenfolge zurückgesetzt!
! Räume am Ende anfügen geht über "Aufzählungen-Rooms-WearOS" per Drag&Drop
! Sobald die Räume geändert wurden, muss die App neu gestartet werden. Dazu reicht es den "Back"-Knopf an der Uhr zu drücken und die App neu zu öffnen.

## APK installieren
https://youtu.be/8HsfWPTFGQI


## Konfiguration APP
Server URL definieren im Format: http://192.168.10.4:8084


## Schnelltaste
Damit die APP schnell geöffnet werden kann, habe ich diese auf die "Doppelklick"-Tastenfunktion der Uhr-Taste gelegt.  
Zu finden hier: Einstellungen - Erweiterte Funktionen - Anpassen von Tasten > Hier kann die App verlinkt werden



## Known Bugs / Verbesserungen
- Sporadisch kurzer Verbindungsverlust, siehe Websocket vs PollingXHR
- <s>Manchmal werden mehrere "Instanzen" erstellt </s>
- <s>Wenn Server URL definiert ist und nicht erreichbar bleibt die App  Startbildschirm</s>  - Fixed V2.1
- "Swipe" zu schließen aktivieren wenn kein Slider konfiguriert ist
- <s>Rückgabewert anhand des definierten Typ Boolean/"ON","OFF"</s> <-ToggleChip ist jetzt immer boolean

In der aktellen Version sollte die App auch mit den bekannten Bugs zuverlässig laufen


## Jetpack Compose...
...ist das neuste Toolkit zur Android APP-Entwicklung:  
https://developer.android.com/jetpack/compose

Leider ist dieses noch relativ "neu" und es mussten im Code einige (vermultiche) Bugs brücksichtigt bzw. Workarounds implementiert werden.  
Bzgl. der Performance hoffe ich hier auf einige Updates in der Zukunft.  


## PollingXHR vs Websocket
https://github.com/Schnup89/ioBroker_WearV2/blob/265252d2d10f3a0d4c854a4bccde8b2aa92a5e3d/app/src/main/java/com/schnup/iobrokerw/SocketHandler.kt
-> Siehe "mOpts.transports ="  
"PollingXHR" 
+ Sobald die SocketIO Verbindung zum ioBroker unterbrochen ist, wird innerhalb von Sekunden ein rotes "X" angezeigt.
- Leider unterbricht (bei mir) die Verbindung manchmal für einen kurzen Moment (sichtbar am roten "X")
  
"Websocket"  
+ Stabile Verbindung ohne kurze abbrüche
- (Bei mir) erkennung eines Verbindungsverlust erst nach 1,5 Minuten

Da mit der Live-Status der Verbindung wichtig ist, und die kurzen Disconnects keine Fehler produzieren, habe ich Standardmäig diesen im Einsatz.


## Changelog



### 2.2 (2022-01-24)
* (schnup89) BUGFIX: App kann nicht mehr parallel ausgeführt werden, nur noch eine Instanz zugelassen
* (schnup89) BUGFIX: ToggleChip schreibt nun boolean anstatt string in ioBroker
* (schnup89) BUGFIX: Slider-Fixed bei -1 oder "out-of-range"

### 2.1 (2022-01-23)
* (schnup89) BUGFIX: V2 konnte nicht installiert werden -> Sollte behoben sein
* (schnup89) BUGFIX: Wenn Server URL definiert ist und nicht erreichbar bleibt die App Startbildschirm

### 2.0 (2022-01-22)
* (schnup89) Initial Release



