#!/usr/bin/env python3
"""
Yandex TTS (yandex-tts-free). Читает текст из stdin, выводит MP3 в stdout.
Требования: pip install yandex-tts-free, в системе ffmpeg.
Использование: echo "Привет мир" | python yandex_tts.py
"""
import sys
import os
import tempfile

def main():
    text = sys.stdin.read().strip()
    if not text:
        sys.stderr.write("No text provided\n")
        sys.exit(1)
    # Ограничиваем длину (API лимиты)
    text = text[:5000]
    try:
        from yandex_tts_free import YandexFreeTTS
    except ImportError:
        sys.stderr.write("Install: pip install yandex-tts-free\n")
        sys.exit(2)
    tts = YandexFreeTTS()
    tmpdir = tempfile.mkdtemp()
    out_file = os.path.join(tmpdir, "out.mp3")
    try:
        tts.generate_speech_ya(tmpdir, "out.mp3", text, "oksana", "good")
        if os.path.exists(out_file):
            with open(out_file, "rb") as f:
                sys.stdout.buffer.write(f.read())
        else:
            sys.stderr.write("TTS did not produce output\n")
            sys.exit(3)
    finally:
        try:
            os.remove(out_file)
            os.rmdir(tmpdir)
        except Exception:
            pass

if __name__ == "__main__":
    main()
