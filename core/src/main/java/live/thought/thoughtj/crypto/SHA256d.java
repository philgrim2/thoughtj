package live.thought.thoughtj.crypto;
/*
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
 * 
 * Based on code from the Nilestore project.
 * 
 * Copyright (C) (2011) Nile University (NU)
 *
 * This is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import java.nio.charset.Charset;

import org.spongycastle.crypto.digests.SHA256Digest;

// TODO: Auto-generated Javadoc
/**
 * The Class SHA256d.
 * 
 * @author Mahmoud Ismail <mahmoudahmedismail@gmail.com>
 */
public class SHA256d
{

  /** The hasher. */
  private SHA256Digest hasher;

  /** The digest. */
  private byte[]       digest;

  /** The truncate_to. */
  private int          truncate_to;

  /**
   * Instantiates a new sH a256d.
   * 
   * @param truncate_to
   *          the truncate_to
   */
  public SHA256d(int truncate_to)
  {

    this.hasher = new SHA256Digest();
    this.truncate_to = truncate_to > hasher.getDigestSize() ? hasher.getDigestSize() : truncate_to;
    this.digest = new byte[truncate_to];
    ;
  }

  /**
   * Instantiates a new SHA256d.
   */
  public SHA256d()
  {
    this.hasher = new SHA256Digest();
    this.digest = new byte[hasher.getDigestSize()];
    ;
    this.truncate_to = hasher.getDigestSize();
  }

  /**
   * Update.
   * 
   * @param data
   *          the data
   */
  public void update(String data)
  {
    update(data.getBytes(Charset.forName("UTF-8")));
  }

  /**
   * Update.
   * 
   * @param data
   *          the data
   */
  public void update(byte[] data)
  {
    hasher.update(data, 0, data.length);
  }

  /**
   * Digest.
   * 
   * @return the byte[]
   */
  public byte[] doubleDigest()
  {
    byte[] h1 = getDigest();
    update(h1);
    byte[] h2 = getDigest();
    System.arraycopy(h2, 0, digest, 0, truncate_to);
    return digest;
  }
  
  /**
   * Digest.
   * 
   * @return the byte[]
   */
  public byte[] digest()
  {
    byte[] h1 = getDigest();
    System.arraycopy(h1, 0, digest, 0, truncate_to);
    return digest;
  }

  /**
   * Gets the digest.
   * 
   * @return the digest
   */
  private byte[] getDigest()
  {
    byte[] out = new byte[hasher.getDigestSize()];
    hasher.doFinal(out, 0);
    return out;
  }
}