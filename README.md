# FragmentationTheater

FragmentationTheater er et simpelt biograf-bookingsystem, som kører via en indbygget HTTP-server. Hele applikationen startes gennem klassen `CinemaBookingApplication`, som automatisk initialiserer serveren og starter systemet.

## 🚀 Kom i gang

Følg denne guide for at hente projektet ned og køre applikationen lokalt.

---

## 📥 1. Hent projektet fra GitHub

Klon projektet ned fra GitHub:

```bash
git clone https://github.com/SoerenTP/FragmentationTheater.git
cd FragmentationTheater
```

---

## 📦 2. Åbn projektet i IntelliJ

Åbn projektet i **IntelliJ IDEA** (anbefalet for dette projekt).

Sørg for at have Java (fx Java 17 eller Java 21) installeret.

IntelliJ vil automatisk opdage projektstrukturen og importere nødvendige biblioteker.

---

## ▶️ 3. Start applikationen (HTTP-serveren starter automatisk)

For at starte systemet skal du blot køre klassen:

```
src/main/java/.../CinemaBookingApplication.java
```

I IntelliJ:

1. Find filen `CinemaBookingApplication`
2. Højreklik
3. Vælg **Run 'CinemaBookingApplication'**

Dette:

- starter den indbyggede HTTP-server  
- loader alle nødvendige ressourcer  
- gør applikationen klar til brug  

Du skal **ikke** starte en server manuelt — den kører automatisk gennem denne klasse.

---

## 🌐 4. Tilgå applikationen

Når applikationen kører, kan den normalt tilgås via:

```
http://localhost:8080/
```

(Portnummeret afhænger af konfigurationen.)

---

## 🧪 5. Kør testene i IntelliJ

Der findes testklasser i projektet, som kan køres direkte gennem IntelliJ.

Sådan gør du:

1. Åbn **test**-mappen i projektstrukturen (`src/test/java`)
2. Find den ønskede testklasse eller -metode
3. Højreklik → vælg **Run 'TestNavn'**

Du kan også køre **alle tests** på én gang ved at:

- Højreklikke på `test`-mappen  
- Vælge **Run 'All Tests'**

IntelliJ viser derefter resultaterne i bundpanelet.

