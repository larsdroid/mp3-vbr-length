/*

Mp3Header.java - This class represents an MP3 file's header section.

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

import java.io.IOException;
import java.io.RandomAccessFile;

class Mp3Header {
    static final int FRAME_HEADER_SIZE = 4;
    private static final int LAYER_COUNT = 3;
    private static final int MIN_FRAME_SIZE = 21;
    private static final int BITRATE_TABLE[][][] = {
        { /* MPEG 2.0 */
            {0, 32, 48, 56, 64,  80,  96,  112, 128, 144, 160, 176, 192, 224, 256}, /* layer 1 */
            {0, 8,  16, 24, 32, 40, 48, 56,  64,  80,  96,  112, 128, 144, 160},    /* layer 2 */
            {0, 8,  16, 24, 32, 40, 48, 56, 64,  80,  96,  112, 128, 144, 160}      /* layer 3 */
        },

        { /* MPEG 1.0 */
            {0, 32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448}, /* layer 1 */
            {0, 32, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320, 384},    /* layer 2 */
            {0, 32, 40, 48, 56, 64, 80, 96, 112, 128, 160, 192, 224, 256, 320}      /* layer 3 */
        }
    };
    private static final int FREQUENCIES_TABLE[][] = {
        {22050, 24000, 16000, 50000},   /* MPEG 2.0 */
        {44100, 48000, 32000, 50000},   /* MPEG 1.0 */
        {11025, 12000, 8000,  50000}    /* MPEG 2.5 */
    };
    private static final int FRAME_SIZE_TABLE[] = {24000, 72000, 72000};

    private int bitRateIndex;
    private int version;
    private int layer;
    private int padding;
    private long sync;
    private int frequencyIndex;
    private int crc;
    private int mode;
    private int copyright;
    private int original;
    private int emphasis;
    private int extension;
    private int modeExtension;

    private Mp3Header() {
    }

    private Mp3Header(final Mp3Header that) {
        this.bitRateIndex = that.bitRateIndex;
        this.version = that.version;
        this.layer = that.layer;
        this.padding = that.padding;
        this.sync = that.sync;
        this.frequencyIndex = that.frequencyIndex;
        this.crc = that.crc;
        this.mode = that.mode;
        this.copyright = that.copyright;
        this.original = that.original;
        this.emphasis = that.emphasis;
        this.extension = that.extension;
        this.modeExtension = that.modeExtension;
    }

    void setBitRateIndex(int bitRateIndex) {
        this.bitRateIndex = bitRateIndex;
    }

    int getFrameLength() {
        return this.sync == 0xFFE ?
                (FRAME_SIZE_TABLE[FRAME_SIZE_TABLE.length - this.layer] * ((this.version == 1 ? 1 : 0) + 1) *
                        this.getBitrate() / getFrequency()) +
                        this.padding : 1;
    }

    int getBitRateIndex() {
        return this.bitRateIndex;
    }

    int getBitrate() {
        return BITRATE_TABLE[this.version == 1 ? 1 : 0][LAYER_COUNT - this.layer][this.bitRateIndex];
    }

    private int getFrequency() {
        return FREQUENCIES_TABLE[this.version][this.frequencyIndex];
    }

    static Mp3Header of(RandomAccessFile file) throws IOException {
        final Mp3Header header = new Mp3Header();

        int[] buffer = new int[FRAME_HEADER_SIZE];
        for (int i = 0; i < FRAME_HEADER_SIZE; i++) {
            buffer[i] = file.readUnsignedByte();
        }

        header.sync = ((buffer[0] << 4) | ((buffer[1] & 0xE0) >> 4));
        if ((buffer[1] & 0x10) != 0) {
            header.version = (buffer[1] >> 3) & 1;
        } else {
            header.version = 2;
        }
        header.layer = (buffer[1] >> 1) & 3;
        header.bitRateIndex = (buffer[2] >> 4) & 0x0F;
        // Sanity check: bitrate 1111b is reserved (invalid)
        if ((header.sync != 0xFFE) || (header.layer != 1) || (header.bitRateIndex == 0xF)) {
            header.sync = 0;
            return null;
        }
        header.crc = buffer[1] & 1;
        header.frequencyIndex = (buffer[2] >> 2) & 0x3;
        header.padding = (buffer[2] >> 1) & 0x1;
        header.extension = (buffer[2]) & 0x1;
        header.mode = (buffer[3] >> 6) & 0x3;
        header.modeExtension = (buffer[3] >> 4) & 0x3;
        header.copyright = (buffer[3] >> 3) & 0x1;
        header.original = (buffer[3] >> 2) & 0x1;
        header.emphasis = (buffer[3]) & 0x3;

        // Sanity checks: frequency 11b is reserved (invalid)
        if (header.frequencyIndex == 0x3) {
            return null;
        }

        return (header.getFrameLength() >= MIN_FRAME_SIZE) ? header : null;
    }

    static Mp3Header of(final Mp3Header that) {
        return new Mp3Header(that);
    }

    boolean sameConstant(Mp3Header that) {
        return (this.version == that.version) &&
                (this.layer == that.layer) &&
                (this.crc == that.crc) &&
                (this.frequencyIndex == that.frequencyIndex) &&
                (this.mode == that.mode) &&
                (this.copyright == that.copyright) &&
                (this.original == that.original) &&
                (this.emphasis == that.emphasis);
    }
}
