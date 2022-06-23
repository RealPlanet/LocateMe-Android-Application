# LocateMe - Ruscica Fabio #1229076

Consegna per il progetto B del corso di Programmazione di sistemi embedded.

# Funzionalità

L'applicazione traccia la posizione in real-time dell'utente fornendo, tramite pulsanti appropriati, la possibilità di visualizzare in diversi modi i dati
registrati negli ultimi 5 minuti.

Le interfacce disponibili sono
*   Home, mostra la posizione attuale
*   Map, mostra una mappa del mondo con la posizione attuale segnata.
*   History, mostra una lista con elmementi ispezionabili di tutte le posizioni registrate fino ad ora
*   Chart, mostra la variazione delle coordinate registrate tramite dei grafici

# Google API Key

L'applicazione necessità di una chiave API per il funzionamento della mappa. Questa chiave deve essere definita tramite segreto di gradle con il seguente nome:"GOOGLE_MAPS_API_KEY".
