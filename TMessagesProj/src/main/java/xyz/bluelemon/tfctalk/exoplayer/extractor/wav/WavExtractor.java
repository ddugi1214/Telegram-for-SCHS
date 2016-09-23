/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package xyz.bluelemon.tfctalk.exoplayer.extractor.wav;

import xyz.bluelemon.tfctalk.exoplayer.C;
import xyz.bluelemon.tfctalk.exoplayer.MediaFormat;
import xyz.bluelemon.tfctalk.exoplayer.ParserException;
import xyz.bluelemon.tfctalk.exoplayer.extractor.Extractor;
import xyz.bluelemon.tfctalk.exoplayer.extractor.ExtractorInput;
import xyz.bluelemon.tfctalk.exoplayer.extractor.ExtractorOutput;
import xyz.bluelemon.tfctalk.exoplayer.extractor.PositionHolder;
import xyz.bluelemon.tfctalk.exoplayer.extractor.SeekMap;
import xyz.bluelemon.tfctalk.exoplayer.extractor.TrackOutput;
import xyz.bluelemon.tfctalk.exoplayer.util.MimeTypes;

import java.io.IOException;

/** {@link Extractor} to extract samples from a WAV byte stream. */
public final class WavExtractor implements Extractor, SeekMap {

  /** Arbitrary maximum input size of 32KB, which is ~170ms of 16-bit stereo PCM audio at 48KHz. */
  private static final int MAX_INPUT_SIZE = 32 * 1024;

  private ExtractorOutput extractorOutput;
  private TrackOutput trackOutput;
  private WavHeader wavHeader;
  private int bytesPerFrame;
  private int pendingBytes;

  @Override
  public boolean sniff(ExtractorInput input) throws IOException, InterruptedException {
    return WavHeaderReader.peek(input) != null;
  }

  @Override
  public void init(ExtractorOutput output) {
    extractorOutput = output;
    trackOutput = output.track(0);
    wavHeader = null;
    output.endTracks();
  }

  @Override
  public void seek() {
    pendingBytes = 0;
  }

  @Override
  public void release() {
    // Do nothing
  }

  @Override
  public int read(ExtractorInput input, PositionHolder seekPosition)
      throws IOException, InterruptedException {

    if (wavHeader == null) {
      wavHeader = WavHeaderReader.peek(input);
      if (wavHeader == null) {
        // Someone tried to read a non-WAV or unsupported WAV without sniffing first.
        throw new ParserException("Error initializing WavHeader. Did you sniff first?");
      }
      bytesPerFrame = wavHeader.getBytesPerFrame();
    }

    // If we haven't read in the data start and size, read and store them.
    if (!wavHeader.hasDataBounds()) {
      WavHeaderReader.skipToData(input, wavHeader);

      trackOutput.format(
          MediaFormat.createAudioFormat(
              null,
              MimeTypes.AUDIO_RAW,
              wavHeader.getBitrate(),
              MAX_INPUT_SIZE,
              wavHeader.getDurationUs(),
              wavHeader.getNumChannels(),
              wavHeader.getSampleRateHz(),
              null,
              null,
              wavHeader.getEncoding()));
      extractorOutput.seekMap(this);
    }

    int bytesAppended = trackOutput.sampleData(input, MAX_INPUT_SIZE - pendingBytes, true);

    if (bytesAppended != RESULT_END_OF_INPUT) {
      pendingBytes += bytesAppended;
    }

    // Round down the pending number of bytes to the nearest frame.
    int frameBytes = pendingBytes / bytesPerFrame * bytesPerFrame;
    if (frameBytes > 0) {
      long sampleStartPosition = input.getPosition() - pendingBytes;
      pendingBytes -= frameBytes;
      trackOutput.sampleMetadata(
          wavHeader.getTimeUs(sampleStartPosition),
          C.SAMPLE_FLAG_SYNC,
          frameBytes,
          pendingBytes,
          null);
    }

    if (bytesAppended == RESULT_END_OF_INPUT) {
      return RESULT_END_OF_INPUT;
    }

    return RESULT_CONTINUE;
  }

  // SeekMap implementation.

  @Override
  public boolean isSeekable() {
    return true;
  }

  @Override
  public long getPosition(long timeUs) {
    return wavHeader.getPosition(timeUs);
  }
}
