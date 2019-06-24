/*
 * 
 * Copyright (c) 2018 - 2019, Thought Network LLC
 * 
 * Based on code from Cuckoo Cycle, a memory-hard proof-of-work
 * Copyright (c) 2013-2016 John Tromp
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2, as 
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package live.thought.thoughtj.crypto;

class Edge
{
  int u;
  int v;

  public Edge(int x, int y)
  {
    u = x;
    v = y;
  }

  public int hashCode()
  {
    return (int) (u ^ v);
  }

  public boolean equals(Object o)
  {
    Edge f = (Edge) o;
    return u == f.u && v == f.v;
  }
}

public class Cuckoo
{
  public static final int EDGEBITS  = 23;
  public static final int NEDGES    = 1 << EDGEBITS;
  public static final int NODEBITS  = EDGEBITS + 1;
  public static final int NNODES    = 1 << NODEBITS;
  public static final int EDGEMASK  = NEDGES - 1;
  public static final int PROOFSIZE = 42;

  long                    k[]       = new long[4];
  SHA256d                 hasher    = new SHA256d(32);

  public static long u8(byte b)
  {
    return (long) (b) & 0xff;
  }

  public static long u8to64(byte[] p, int i)
  {
    return u8(p[i]) | u8(p[i + 1]) << 8 | u8(p[i + 2]) << 16 | u8(p[i + 3]) << 24 | u8(p[i + 4]) << 32 | u8(p[i + 5]) << 40
        | u8(p[i + 6]) << 48 | u8(p[i + 7]) << 56;
  }

  public Cuckoo(byte[] header)
  {
    byte[] hdrkey;

      hasher.update(header);
      hdrkey = hasher.digest();
      
      k[0] = u8to64(hdrkey, 0);
      k[1] = u8to64(hdrkey, 8);
      k[2] = u8to64(hdrkey, 16);
      k[3] = u8to64(hdrkey, 24);

  }

  public long siphash24(int nonce)
  {
    long v0, v1, v2, v3;

    v0 = k[0];
    v1 = k[1];
    v2 = k[2];
    v3 = k[3] ^ nonce;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    v0 ^= nonce;
    v2 ^= 0xff;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    v0 += v1;
    v2 += v3;
    v1 = (v1 << 13) | v1 >>> 51;
    v3 = (v3 << 16) | v3 >>> 48;
    v1 ^= v0;
    v3 ^= v2;
    v0 = (v0 << 32) | v0 >>> 32;
    v2 += v1;
    v0 += v3;
    v1 = (v1 << 17) | v1 >>> 47;
    v3 = (v3 << 21) | v3 >>> 43;
    v1 ^= v2;
    v3 ^= v0;
    v2 = (v2 << 32) | v2 >>> 32;

    return (v0 ^ v1) ^ (v2 ^ v3);
  }

  // generate edge in cuckoo graph
  public int sipnode(int nonce, int uorv)
  {
    return (int) siphash24(2 * nonce + uorv) & EDGEMASK;
  }

  // generate edge in cuckoo graph
  public Edge sipedge(int nonce)
  {
    return new Edge(sipnode(nonce, 0), sipnode(nonce, 1));
  }

  // verify that (ascending) nonces, all less than easiness, form a cycle in graph
  public Boolean verify(int[] nonces, int easiness)
  {
    int us[] = new int[PROOFSIZE], vs[] = new int[PROOFSIZE];
    int i = 0, n;
    int xor0 = 0;
    int xor1 = 0;
    for (n = 0; n < PROOFSIZE; n++)
    {
      if (nonces[n] >= easiness || (n != 0 && nonces[n] <= nonces[n - 1]))
        return false;
      us[n] = sipnode(nonces[n], 0);
      vs[n] = sipnode(nonces[n], 1);
      xor0 ^= us[n];
      xor1 ^= vs[n];
    }
    if (xor0 > 0 || xor1 > 0)
      return false;
    do
    { // follow cycle until we return to i==0; n edges left to visit
      int j = i;
      for (int k = 0; k < PROOFSIZE; k++) // find unique other j with same vs[j]
        if (k != i && vs[k] == vs[i])
        {
          if (j != i)
            return false;
          j = k;
        }
      if (j == i)
        return false;
      i = j;
      for (int k = 0; k < PROOFSIZE; k++) // find unique other i with same us[i]
        if (k != j && us[k] == us[j])
        {
          if (i != j)
            return false;
          i = k;
        }
      if (i == j)
        return false;
      n -= 2;
    }
    while (i != 0);
    return n == 0;
  }

}
