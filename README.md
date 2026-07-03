Weather Forecast Java Swing App

This is a minimal Java Swing app that fetches real-time weather and a daily forecast from Open-Meteo and displays it in a simple UI with a purple gradient background.

Run (requires Maven and Java 11+):

```bash
mvn compile exec:java -Dexec.mainClass="com.example.weather.WeatherApp"
```

Enter latitude and longitude (defaults to San Francisco) and click "Fetch Forecast".

API used: https://open-meteo.com (no API key required)

You can run without installing Maven by using the included wrapper scripts:

- On Windows (PowerShell/cmd):

```
mvnw.cmd
```

- On macOS/Linux (or Git Bash):

```
./mvnw
```

The first run will download a small Maven distribution into the project's `.mvn` folder.
On Unix you may need to make the wrapper executable once:

```bash
chmod +x mvnw
```
