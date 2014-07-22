/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.bitalino.comm;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Abstract socket implementation that implements BITalino I/O streams handling.
 */
final class BITalinoSocket {

  private DataInputStream dis;
  private OutputStream os;

  public BITalinoSocket(final DataInputStream is, final OutputStream os) {
    checkNotNull(is, "Input stream was not provided.");
    checkNotNull(os, "Output stream was not provided.");
    this.dis = is;
    this.os = os;
  }

  /**
   * Reads data from open socket, if any.
   * 
   * @param numberOfSamples
   *          the number of samples to read
   * @param analogChannels
   *          the analog channels to read from
   * @param totalBytes
   *          total available bytes to read
   * @return an array of decoded {@link BITalinoFrame}s.
   * @throws BITalinoException
   */
  public BITalinoFrame[] read(final int[] analogChannels, final int totalBytes,
      final int numberOfSamples) throws BITalinoException {
    try {
      BITalinoFrame[] frames = new BITalinoFrame[numberOfSamples];
      byte[] buffer = new byte[totalBytes];
      byte[] bTemp = new byte[1];
      int sampleCounter = 0;

      // parse frames
      while (sampleCounter < numberOfSamples) {
        // read number_bytes from buffer
        dis.readFully(buffer, 0, totalBytes);
        // let's try to decode the buffer
        BITalinoFrame f = BITalinoFrameDecoder.decode(buffer, analogChannels, totalBytes);
        // if CRC isn't valid, sequence equals -1
        if (f.getSequence() == -1) {
          // we're missing data, so let's wait and try to rebuild the buffer or
          // throw exception
          System.out
              .println("Missed a sequence. Are we too far from BITalino? Retrying..");
          while (f.getSequence() == -1) {
            dis.readFully(bTemp, 0, 1);
            for (int j = totalBytes - 2; j >= 0; j--)
              buffer[j + 1] = buffer[j];
            buffer[0] = bTemp[0];
            f = BITalinoFrameDecoder.decode(buffer, analogChannels, totalBytes);
          }
        }
        frames[sampleCounter] = f;
        sampleCounter++;
      }
      return frames;
    } catch (Exception e) {
      throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
    }
  }

  /**
   * Writes data to socket.
   * 
   * @param command
   * @throws BITalinoException
   */
  public void write(final int data) throws BITalinoException {
    try {
      os.write(data);
      os.flush();
      Thread.sleep(1000);
    } catch (Exception e) {
      e.printStackTrace(System.err);
      throw new BITalinoException(BITalinoErrorTypes.LOST_COMMUNICATION);
    }
  }

  /**
   * Releases any open resources.
   * 
   * @throws BITalinoException
   */
  public void close() throws BITalinoException {
    try {
      dis.close();
      os.close();
    } catch (Exception e) {
      throw new BITalinoException(BITalinoErrorTypes.BT_DEVICE_NOT_CONNECTED);
    } finally {
      dis = null;
      os = null;
    }
  }

  public InputStream getInputStream() {
    return dis;
  }

  public OutputStream getOutputStream() {
    return os;
  }

}