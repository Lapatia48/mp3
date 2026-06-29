"""Genere un petit MP3 valide (silencieux) avec un tag ID3v2.3.
Sert uniquement aux tests du pipeline (scanner -> extracteur -> uploader).

Usage: python make_mp3.py <sortie.mp3> "Titre" "Artiste" "Album" "Genre" "Annee"
"""
import struct
import sys


def synchsafe(n: int) -> bytes:
    return bytes([(n >> 21) & 0x7F, (n >> 14) & 0x7F, (n >> 7) & 0x7F, n & 0x7F])


def text_frame(frame_id: str, text: str) -> bytes:
    payload = b"\x00" + text.encode("latin-1", "replace")  # encodage ISO-8859-1
    return frame_id.encode("ascii") + struct.pack(">I", len(payload)) + b"\x00\x00" + payload


def id3v2(title, artist, album, genre, year) -> bytes:
    frames = b"".join([
        text_frame("TIT2", title),
        text_frame("TPE1", artist),
        text_frame("TALB", album),
        text_frame("TCON", genre),
        text_frame("TYER", year),
    ])
    header = b"ID3" + b"\x03\x00" + b"\x00" + synchsafe(len(frames))
    return header + frames


def mpeg_frames(n_frames: int) -> bytes:
    # MPEG-1 Layer III, 128 kbps, 44100 Hz, mono, sans CRC.
    header = bytes([0xFF, 0xFB, 0x90, 0xC0])
    frame_len = 144 * 128000 // 44100  # = 417 octets
    body = b"\x00" * (frame_len - 4)
    return (header + body) * n_frames


def main():
    out = sys.argv[1]
    title = sys.argv[2] if len(sys.argv) > 2 else "Titre Test"
    artist = sys.argv[3] if len(sys.argv) > 3 else "Artiste Test"
    album = sys.argv[4] if len(sys.argv) > 4 else "Album Test"
    genre = sys.argv[5] if len(sys.argv) > 5 else "Rock"
    year = sys.argv[6] if len(sys.argv) > 6 else "1971"

    data = id3v2(title, artist, album, genre, year) + mpeg_frames(191)  # ~5 secondes
    with open(out, "wb") as f:
        f.write(data)
    print(f"Ecrit {out} ({len(data)} octets)")


if __name__ == "__main__":
    main()
