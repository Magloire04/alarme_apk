#!/usr/bin/env bash
# Génère les sonneries originales de l'appli (mono, 22 050 Hz, Vorbis).
# Chaque fichier est un cycle court joué en boucle par AlarmPlayer.
# Usage : tools/generate_sounds.sh (depuis la racine du projet, ffmpeg requis)
set -euo pipefail

OUT="app/src/main/res/raw"
RATE=22050
mkdir -p "$OUT"

encode() {
  local name=$1 expression=$2 duration=$3
  ffmpeg -hide_banner -loglevel error -y \
    -f lavfi -i "aevalsrc='$expression':s=$RATE:d=$duration" \
    -ac 1 -c:a libvorbis -q:a 2 "$OUT/$name.ogg"
}

# Doux : do puis mi, chaque note en fondu, puis une seconde de silence.
encode alarm_soft \
  "0.6*sin(2*PI*if(lt(t,0.5),523,659)*t)*if(lt(t,1),sin(PI*mod(t,0.5)/0.5),0)" 2

# Classique : quatre bips à 880 Hz de 150 ms, puis 600 ms de silence.
encode alarm_classic \
  "0.7*sin(2*PI*880*t)*lt(mod(t,0.25),0.15)*lt(t,1)" 1.6

# Strident : onde carrée qui alterne 1000 et 1400 Hz toutes les 120 ms.
encode alarm_strident \
  "0.5*sgn(sin(2*PI*if(lt(mod(t,0.24),0.12),1000,1400)*t))" 0.96

ls -la "$OUT"
