# ioBroker_WearV2
WearOS App für Interaktion mit ioBroker 
  
Video:
https://www.youtube.com/watch?v=gGTmlj9mHgY

Icons:
https://github.com/ioBroker/ioBroker.icons-ultimate-png

Wenn die Uhr per WLAN verbunden ist, nutzt die App immer die direkte WLAN-Verbindung.  
Wenn die Uhr nicht per WLAN mit Netzwerk verbunden ist, wird über den Bluetooth-Proxy des Smartphones kommuniziert.  


## Konfiguration ioBroker
&#x1F534; Ab Version 2.6 muss der [Websockets-Adapter](https://github.com/ioBroker/ioBroker.ws) installiert sein. &#x1F534;  
  
Vor Version 2.6 muss der [SocketIO-Adapter](https://github.com/ioBroker/ioBroker.socketio) installiert sein.  
Standardmäig nutzt dieser den Port 8084, den merken wir uns für später.  
&#x1F6A7; Sollte der Websocket-Adapter bei euch noch nicht installiert sein, bitte unter der Instanz einen freien Port z.B. 8085 angeben, sonst gibt es Probleme mit dem Socket.IO Adapter.  
Ich habe bei mir den Socket.IO Adapter deinstalliert und den Websocketsadapter installiert und habe keine Probleme damit. Ich kann jedoch nicht versprechen dass es bei euch genauso ist.  

**Grundkonfiguration**
- Raum "WearOS" unter Aufzählung->Räume erstellen
- Die auf der Uhr dargestellten Objects/States müssen dem Raum "WearOS" zugeteilt werden -> Siehe Punkt "Objects-Anordnung"

![image](https://user-images.githubusercontent.com/28166743/150635785-b8b4d6be-9404-412b-8c38-814864c167bb.png)



**Objekt Eigenschaften**
- Um ein Object/State nach eigenen Wünschen daruzustellen, gibt es folgende Parameter im Object-JSON:

Bild | JSON-Param                   | Funktion              | Datentyp    | Bemerkung  |
---- | ---------------------------- | --------------------- | ----------- | ---------- |
1    | "common"-"name"              | Anzeigename           | Zeichenk.   |
2    | "common"-"unit"              | Einheit (%,°C, etc)   | Zeichenk.   |
3    | "common"-"icon"              | Icon                  | Zeichenk.   | Setzen über "common"-Reiter
4    | "common"-"write"             | ReadOnly wenn false   | Boolean     | 
5    | "common"-"color"             | Farbe bei aktiv.      | Zeichenk.   | Setzen über "common"-Reiter
6    | "common"-"min"               | Bei Slider Min Wert   | Zahl        | Setzen über "common"-Reiter
7    | "common"-"max"               | Bei Slider Man Wert   | Zahl        | Setzen über "common"-Reiter
7    | "common"-"steps"             | Bei Slider Schritte   | Zahl        | Setzen über "common"-Reiter
8    | "common"-"role"              | Anzeigetyp            | Zeichenk.   | Setzen über "common"-Reiter
8    | "common"-"color-background"  | Chip-Hintergrundfarbe | Zeichenk.   | Nimmt als Wert Color-Code wie "color"-Parameter



![image](https://user-images.githubusercontent.com/28166743/150635645-0b8cd1ad-fecb-432a-87d0-0ceeb7a98afd.png)
![image](https://user-images.githubusercontent.com/28166743/150635717-40ae2677-9da2-4fe0-8f90-2699425a278c.png)


** Anzeigetyp **  
Die Rolle eines Objekt definiert den Anzeigetyp:  

Rolle           | Anzeige-Typ      | Extra           |
--------------- | ---------------- | ----------------
switch*         | Toggle           |                 |
scene.states*   | Toggle (Taster)  | "desc": "press" |
scene.states*   | Toggle           |                 |
level*          | Slider           |                 |
Alles andere    | Wertanzeige      |                 |

Um den Taster anzuzeigen im Objekt den Wert "desc" auf "press" ohne " setzen.

  
## Anordnung der Objekte
Leider ist es aktuell in ioBroker nicht möglich Objekte in Räumen per "Drag&Drop" zu sortieren.  
Die Objekte in den Räume können hier in der Reihenfolge geändert werden:  
Objekt-Baum mit Expertenansicht öffnen und mit dem Schraubenschlüssel bei "enum.rooms.WearOS" in der JSON die Reihenfolge von "members" bearbeiten.  
! Bei einer Zuordnung von Object über den Objekt-Baum wird die Reihenfolge zurückgesetzt!
! Räume am Ende anfügen geht über "Aufzählungen-Rooms-WearOS" per Drag&Drop
! Sobald die Räume geändert wurden, muss die App neu gestartet werden. Dazu reicht es den "Back"-Knopf an der Uhr zu drücken und die App neu zu öffnen.


## RED X
Sollte ein Fehler auftren wird fast immer ein rotes X irgendwo auftreten.  
- Sollte ein roten X anstatt ein ICON dargestellt werden, kann das ICON nicht ausgelesen werden -> Bitte Issue eröffnen mit Objekt-JSON aus ioBroker
- Im Status-Indicator: Grüner Haken -> SocketIO ist verbunden mit ioBroker; Rotes X -> SocketIO hat die Verbindung zu ioBroker verloren


## APK installieren
**... über den Wear Installer:**  
https://youtu.be/8HsfWPTFGQI
  
**...Alternativ über das Program adb.exe:**  
https://dl.google.com/android/repository/platform-tools-latest-windows.zip <- adb.exe extrahieren nach z.B. C:\tmp  
ioWearV2_5.apk nach C:\tmp\ kopieren  
  
- per Kommandozeile (CMD) in den Ordner C:\tmp\ wechseln
- Auf der Uhr unter Entwickleroptionen "adb debugging" aktivieren
- adb connect 192.168.1.100 <- ersetze IP mit der IP der Uhr, kopplung auf der uhr zulassen
- adb install ioWearV2_5.apk
- anstatt C:\tmp könnt Ihr natürlich auch einen anderen Ordner nutzen
  
Sollte bei einem Update ein Fehler bei der Installation auftreten, die App im Menü mit einem langen Klick deinstallieren.  
Alternativ mit folgendem adb-Kommando:  

adb uninstall "com.schnup.iobrokerw"
  
  
## Konfiguration APP
Server URL definieren im Format: http://192.168.10.4:8084
  
  
## Fehlermeldungen
Fehlermeldungen werden in der Objekt-Liste ganz oben angezeigt, hier die möglichen Fehlermeldungen und Lösungen:

Fehlermeldung          | Lösung                 | 
---------------------- | ---------------------  | 
Bitte Server angeben   | URL konfigurieren          |  
Not reachable   | Der Websocket-Dienst ist unter der angegeben URL nicht erreichbar. URL sollte, wenn von Webbrowser aufgerufen "Not implemented" anzeigen  | 
Enum WearOS not found  | Der Raum wurde in ioBroker nicht gefunden, bitte auf Groß und Kleinschreibung achten. Mit aktiviertem Expertenmodus in der Objektansicht des ioBroker sollte folgendes Objekt exisiteren: enum.rooms.WearOS   |     
     
  
  
## Von Unterwegs die APP nutzen
Die Uhr (galaxy watch4, andere nicht getestet) baut die Verbindung in der Regel über den Bluetooth-Proxy über das Telefon auf.  
Das bedeutet dass die App über das SmartPhone zum ioBroker kommuniziert.  
Ist das SmartPhone nicht verfügbar wird die App, wenn vorhanden, direkt über WLAN die Verbindung zum ioBroker aufbauen.  
Den Bluetooth-Proxy können wir uns zu nutze machen wenn wir unterwegs sind, denn sobald das SmartPhone über einen VPN-Tunnel den ioBroker erreicht, wird damit auch die Uhr bzw. die APP eine Verbindung von unterwegs aufbauen können.  
  
  
## Schnelltaste
Damit die APP schnell geöffnet werden kann, habe ich diese auf die "Doppelklick"-Tastenfunktion der Uhr-Taste gelegt.  
Zu finden hier: Einstellungen - Erweiterte Funktionen - Anpassen von Tasten > Hier kann die App verlinkt werden



## Known Bugs / Verbesserungen
- <s>Sporadisch kurzer Verbindungsverlust, siehe Websocket vs PollingXHR</s> -  Fixed V2.6
- <s>Manchmal werden mehrere "Instanzen" erstellt </s>
- <s>Wenn Server URL definiert ist und nicht erreichbar bleibt die App  Startbildschirm</s>  - Fixed V2.1
- "Swipe" zu schließen aktivieren wenn kein Slider konfiguriert ist
- <s>Rückgabewert anhand des definierten Typ Boolean/"ON","OFF"</s> <-ToggleChip ist jetzt immer boolean
- <s>Connection Manager implementieren und bei WiFi immer dieses zuerst nutzen</s>  - Sollte kein Problem mehr sein, V2.6

In der aktellen Version sollte die App auch mit den bekannten Bugs zuverlässig laufen


## Jetpack Compose...
...ist das neuste Toolkit zur Android APP-Entwicklung:  
https://developer.android.com/jetpack/compose

Leider ist dieses noch relativ "neu" und es mussten im Code einige (vermultiche) Bugs brücksichtigt bzw. Workarounds implementiert werden.  
Bzgl. der Performance hoffe ich hier auf einige Updates in der Zukunft.  



## Changelog

### 2.9 (2023-02-06)
* (schnup89) Bugfix: Issue #13 Abfangen von Fehlerszenarien (Kein Connect, Enum nicht korrekt)
* (schnup89) Bugfix: Issue #14 Slider Korrektur, bei mehr als einem Slider wurde alle gleichzeitig aktiv
* (schnup89) Sonstiges: Grade-Upgrade + Automatic Keystore signing

### 2.8 (2023-01-16)
* (schnup89) Bugfix: Issue #10 Langsamer start durch Websockets-Timer gelöst
* (MDeeApp) Bugfix: PR #11 Slider Steps und RÜckgabewert korrigiert, danke @MDeeApp
* (schnup89) Funktion: Versionsanzeige auf "Settings"-Schaltfläche

### 2.7.1 (2023-01-07)
* (schnup89) Bugfix: Issue #9 Bei "Taster" wurde Icon falsch eingefärbt

### 2.7 (2023-01-06)
* (schnup89) Funktion: Icon's werden nicht mehr standardmäßig Gelb eingefärbt, dafür kann der "color" Parameter verwendet werden
* (schnup89) Funktion: Slider Unterstützt nun auch den "Step" Parameter von ioBroker
* (schnup89) Bugfix: Issue #8 Bezel scroll funktioniert manchmal nicht
* (schnup89) Bugfix: Issue #7 Farben werden immer eingefärbt wurde angepasst
* (schnup89) Bugfix: Issue #6 Slider Absturz
* (schnup89) Bugfix: Issue #4 App startet länger &#x1F534;  Build Optionen gefixed, evtl. Neuinstallation der App notwendig
* (schnup89) Bugfix: Issue #5 Freier Platz am oberen Rand der Liste gefixed 

### 2.6 (2023-01-03)
* (schnup89) Funktion: Farbauswahl für "Chips" hinzugefügt
* (schnup89) Funktion: Taster (Schalter) hinzugefügt
* (schnup89) Funktion: (Compose, Kotlin) Libraries etc. auf aktuellen Stand importiert
* (schnup89) Funktion: !!! Socket.IO wird ioBroker-Seitig nicht mehr weiterentwickelt, deshalb umstieg auf Websockockets, anderer Adapter notwendig

### 2.5 (2022-02-02)
* (schnup89) Funktion: scene.states als ToggleChip hinzugefügt

### 2.4 (2022-01-28)
* (schnup89) Funktion: WLAN wird nun standardmäßig genutzt, wenn verbunden. Alternativ Bluetooth-Proxy
* (schnup89) Funktion: Exit-Button hinzugefügt

### 2.3 (2022-01-25)
* (schnup89) BUGFIX: Color-Code Error Handling implementiert

### 2.2 (2022-01-24)
* (schnup89) BUGFIX: App kann nicht mehr parallel ausgeführt werden, nur noch eine Instanz zugelassen
* (schnup89) BUGFIX: ToggleChip schreibt nun boolean anstatt string in ioBroker
* (schnup89) BUGFIX: Slider-Fixed bei -1 oder "out-of-range"

### 2.1 (2022-01-23)
* (schnup89) BUGFIX: V2 konnte nicht installiert werden -> Sollte behoben sein
* (schnup89) BUGFIX: Wenn Server URL definiert ist und nicht erreichbar bleibt die App Startbildschirm

### 2.0 (2022-01-22)
* (schnup89) Initial Release



