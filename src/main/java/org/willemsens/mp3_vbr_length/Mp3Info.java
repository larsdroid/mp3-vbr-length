/*

Mp3Info.java - This class represents information gathered from an
               MP3 file such as its total duration in seconds.

Copyright (C) 2018  Lars Willemsens <lars@willemsens.org>

This file is part of mp3-vbr-length
(https://github.com/larsdroid/mp3-vbr-length).

mp3-vbr-length is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or
(at your option) any later version.

mp3-vbr-length is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with mp3-vbr-length.  If not, see <http://www.gnu.org/licenses/>.

***********************************************************************

This file is based in part on MP3Info version 0.8.5a by Cedric Tefft.
(http://ibiblio.org/mp3info/)

*/

package org.willemsens.mp3_vbr_length;

import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.willemsens.mp3_vbr_length.Mp3Header.FRAME_HEADER_SIZE;

/**
 * An Mp3Info contains information about a physical MP3 file.
 * For now, only the length (duration) of an MP3 file is calculated.
 * Supports VBR format.
 */
public class Mp3Info {
    private static final int MIN_CONSEC_GOOD_FRAMES = 4;

    private long dataSize;
    private Mp3Header header;
    private int seconds;

    private Mp3Info(Path path) throws IOException {
        this.dataSize = Files.size(path);

        try (final RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")){
            int vbr_median = -1;
            int bitrate;

            if (this.getFirstHeader(file)) {
                bitrate = this.getNextHeader(file);
                int frame_type[] = new int[15];
                int frames = 0;
                while (bitrate != 0) {
                    frame_type[15 - bitrate]++;
                    frames++;
                    bitrate = this.getNextHeader(file);
                }
                final Mp3Header header = Mp3Header.of(this.getHeader());
                int frames_so_far = 0;
                float seconds = 0;
                for (int counter = 0; counter < 15; counter++) {
                    if (frame_type[counter] != 0) {
                        header.setBitRateIndex(counter);
                        frames_so_far += frame_type[counter];
                        seconds += (float) (header.getFrameLength() * frame_type[counter]) / (float) (header.getBitrate() * 125);
                        if ((vbr_median == -1) && (frames_so_far >= frames / 2)) {
                            vbr_median = counter;
                        }
                    }
                }
                this.setSeconds((int) (seconds + 0.5));
                this.getHeader().setBitRateIndex(vbr_median);
            }
        }
    }

    private Mp3Header getHeader() {
        return header;
    }

    private void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    public int getSeconds() {
        return seconds;
    }

    private boolean getFirstHeader(final RandomAccessFile file) throws IOException {
        file.seek(0L);

        while (true) {
            try {
                int c;
                do {
                    c = file.readUnsignedByte();
                } while (c != 255);

                file.seek(file.getFilePointer() - 1);
                final long valid_start = file.getFilePointer();
                final Mp3Header h = Mp3Header.of(file);
                if (h != null) {
                    int l = h.getFrameLength();
                    file.skipBytes(l - FRAME_HEADER_SIZE);
                    int k;
                    Mp3Header h2 = null;
                    for (k = 1; (k < MIN_CONSEC_GOOD_FRAMES) && (this.dataSize - file.getFilePointer() >= FRAME_HEADER_SIZE); k++) {
                        h2 = Mp3Header.of(file);
                        if (h2 == null) {
                            break;
                        }
                        l = h2.getFrameLength();
                        if (!h.sameConstant(h2)) {
                            break;
                        }

                        file.skipBytes(l - FRAME_HEADER_SIZE);
                    }
                    if (k == MIN_CONSEC_GOOD_FRAMES) {
                        file.seek(valid_start);
                        this.header = h2;
                        return true;
                    }
                }
            } catch (EOFException e) {
                return false;
            }
        }
    }

    private int getNextHeader(final RandomAccessFile file) throws IOException {
        while (true) {
            try {
                int c = file.readUnsignedByte();
                while (c != 255) {
                    c = file.readUnsignedByte();
                }

                file.seek(file.getFilePointer() - 1);
                final Mp3Header h = Mp3Header.of(file);
                if (h != null) {
                    int l = h.getFrameLength();
                    file.skipBytes(l - FRAME_HEADER_SIZE);
                    return 15 - h.getBitRateIndex();
                }
            } catch (EOFException e) {
                return 0;
            }
        }
    }

    /**
     * Create and parse the MP3 file denoted by the given file.
     * This is a lengthy operation since the entire file is processed!
     *
     * @param path The location of the MP3 file to load and analyse.
     * @return Information about the given file.
     * @throws IOException In case of unexpected and abnormal file structure
     *                     or other I/O errors.
     */
    public static Mp3Info of(Path path) throws IOException {
        return new Mp3Info(path);
    }
}
