# ioBroker_WearV2
WearApplication mit SocketIO und JetpackCompose


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







## Known Bugs / Verbesserungen
- Sporadisch kurzer Verbindungsverlust, siehe Websocket vs PollingXHR
- Manchmal werden mehrere "Instanzen" erstellt -> Lifecycle checken

- "Swipe" zu schließen aktivieren wenn kein Slider konfiguriert ist

In der aktellen Version sollte die App auch mit den bekannten Bugs zuverlässig laufen


## PollingXHR vs Websocket
tdb  


