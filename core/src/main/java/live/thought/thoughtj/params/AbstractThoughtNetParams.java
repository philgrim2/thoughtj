/*
 * Copyright 2013 Google Inc.
 * Copyright 2015 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package live.thought.thoughtj.params;

import java.math.BigInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import live.thought.thoughtj.core.Block;
import live.thought.thoughtj.core.Coin;
import live.thought.thoughtj.core.CoinDefinition;
import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.core.StoredBlock;
import live.thought.thoughtj.core.ThoughtSerializer;
import live.thought.thoughtj.core.Transaction;
import live.thought.thoughtj.core.Utils;
import live.thought.thoughtj.core.VerificationException;
import live.thought.thoughtj.store.BlockStore;
import live.thought.thoughtj.store.BlockStoreException;
import live.thought.thoughtj.utils.MonetaryFormat;

/**
 * Parameters for Bitcoin-like networks.
 */
public abstract class AbstractThoughtNetParams extends NetworkParameters
{
  /**
   * Scheme part for Bitcoin URIs.
   */
  public static final String  COIN_SCHEME = CoinDefinition.coinURIScheme;

  private static final Logger log         = LoggerFactory.getLogger(AbstractThoughtNetParams.class);

  protected boolean           powAllowMinimumDifficulty;
  protected boolean           powNoRetargeting;

  public AbstractThoughtNetParams()
  {
    super();
  }

  protected long calculateNextDifficulty(StoredBlock storedPrev, Block nextBlock, BigInteger newTarget)
  {
    int currentBlockHeight = storedPrev.getHeight() + 1;
    BigInteger nProofOfWorkLimit = (currentBlockHeight >= cuckooHardForkBlockHeight) ? this.getMaxCuckooTarget()
        : this.getMaxTarget();
    if (newTarget.compareTo(nProofOfWorkLimit) > 0)
    {
      log.info("Difficulty hit proof of work limit: {}", newTarget.toString(16));
      newTarget = nProofOfWorkLimit;
    }

    int accuracyBytes = (int) (nextBlock.getDifficultyTarget() >>> 24) - 3;

    // The calculated difficulty is to a higher precision than received, so reduce
    // here.
    BigInteger mask = BigInteger.valueOf(0xFFFFFFL).shiftLeft(accuracyBytes * 8);
    newTarget = newTarget.and(mask);
    return Utils.encodeCompactBits(newTarget);
  }

  protected void verifyDifficulty(StoredBlock storedPrev, Block nextBlock, BigInteger newTarget) throws VerificationException
  {
    long newTargetCompact = calculateNextDifficulty(storedPrev, nextBlock, newTarget);
    long receivedTargetCompact = nextBlock.getDifficultyTarget();

    if (newTargetCompact != receivedTargetCompact)
      throw new VerificationException("Network provided difficulty bits do not match what was calculated: "
          + Long.toHexString(newTargetCompact) + " vs " + Long.toHexString(receivedTargetCompact));
  }

  /**
   * Implement MIDAS averaging algorithm.
   * 
   * This is MIDAS (Multi Interval Difficulty Adjustment System), a novel
   * getnextwork algorithm. It responds quickly to huge changes in hashing power,
   * is immune to time warp attacks, and regulates the block rate to keep the
   * block height close to the block height expected given the nominal block
   * interval and the elapsed time. How close the correspondence between block
   * height and wall clock time is, depends on how stable the hashing power has
   * been. Maybe Bitcoin can wait 2 weeks between updates but no altcoin can.
   *
   * It is important that none of these intervals (5, 7, 9, 17) have any common
   * divisor; eliminating the existence of harmonics is an important part of
   * eliminating the effectiveness of timewarp attacks.
   * 
   * 
   * @param storedPrev
   * @param blockStore
   * @return
   */
  public void checkDifficulty(StoredBlock storedPrev, Block next, final BlockStore blockStore)
      throws VerificationException, BlockStoreException
  {
    Averages averages;
    long toofast;
    long tooslow;
    long difficultyfactor = 10000;
    long now;
    long BlockHeightTime;

    long nFastInterval = (TARGET_SPACING * 9) / 10; // seconds per block desired when far behind schedule
    long nSlowInterval = (TARGET_SPACING * 11) / 10; // seconds per block desired when far ahead of schedule
    long nIntervalDesired = TARGET_SPACING;

    int currentBlockHeight = storedPrev.getHeight() + 1;
    BigInteger nProofOfWorkLimit = (currentBlockHeight >= cuckooHardForkBlockHeight) ? this.getMaxCuckooTarget()
        : this.getMaxTarget();
    long cPowLimit = Utils.encodeCompactBits(nProofOfWorkLimit);
    BigInteger proposed = BigInteger.ZERO;

    if (storedPrev == null)
    {
      // Genesis Block
      proposed = nProofOfWorkLimit;
    }

    // Special rule for post-cuckoo fork, so that the difficulty can come down
    // far enough for mining.
    if (currentBlockHeight > cuckooHardForkBlockHeight && currentBlockHeight < cuckooHardForkBlockHeight + 50)
    {
      proposed = nProofOfWorkLimit;
    }

    if (powAllowMinimumDifficulty)
    {
      // mining of a min-difficulty block.
      if (next.getTimeSeconds() > storedPrev.getHeader().getTimeSeconds() + TARGET_SPACING * 2)
      {
        proposed = nProofOfWorkLimit;
      }
      else
      {
        // Return the last non-special-min-difficulty-rules-block
        StoredBlock pindex = storedPrev;
        while (pindex.getPrev(blockStore) != null && pindex.getHeight() % nIntervalDesired != 0
            && pindex.getHeader().getDifficultyTarget() == cPowLimit)
          pindex = pindex.getPrev(blockStore);
        proposed = Utils.decodeCompactBits(pindex.getHeader().getDifficultyTarget());
      }
    }

    if (proposed == BigInteger.ZERO)
    {
      // Regulate block times so as to remain synchronized in the long run with the
      // actual time. The first step is to
      // calculate what interval we want to use as our regulatory goal. It depends on
      // how far ahead of (or behind)
      // schedule we are. If we're more than an adjustment period ahead or behind, we
      // use the maximum (nSlowInterval) or minimum
      // (nFastInterval) values; otherwise we calculate a weighted average somewhere
      // in between them. The closer we are
      // to being exactly on schedule the closer our selected interval will be to our
      // nominal interval (TargetSpacing).
      StoredBlock curindex = storedPrev.getPrev(blockStore);
      while (curindex != null)
      {
        curindex = storedPrev.getPrev(blockStore);
      }
      long then = curindex.getHeader().getTimeSeconds();

      now = storedPrev.getHeader().getTimeSeconds();
      BlockHeightTime = then + storedPrev.getHeight() * TARGET_SPACING;

      if (now < BlockHeightTime + (TARGET_TIMESPAN / TARGET_SPACING) && now > BlockHeightTime)
      {
        // ahead of schedule by less than one interval.
        nIntervalDesired = (((TARGET_TIMESPAN / TARGET_SPACING) - (now - BlockHeightTime)) * TARGET_SPACING
            + (now - BlockHeightTime) * nFastInterval) / (TARGET_TIMESPAN / TARGET_SPACING);
      }
      else if (now + (TARGET_TIMESPAN / TARGET_SPACING) > BlockHeightTime && now < BlockHeightTime)
      {
        // behind schedule by less than one interval.
        nIntervalDesired = (((TARGET_TIMESPAN / TARGET_SPACING) - (BlockHeightTime - now)) * TARGET_SPACING
            + (BlockHeightTime - now) * nSlowInterval) / (TARGET_TIMESPAN / TARGET_SPACING);

        // ahead by more than one interval;
      }
      else if (now < BlockHeightTime)
      {
        nIntervalDesired = nSlowInterval;
      } // behind by more than an interval.
      else
      {
        nIntervalDesired = nFastInterval;
      }

      // find out what average intervals over last 5, 7, 9, and 17 blocks have been.
      averages = averageRecentTimestamps(storedPrev, blockStore);

      // check for emergency adjustments. These are to bring the diff up or down FAST
      // when a burst miner or multipool
      // jumps on or off. Once they kick in they can adjust difficulty very rapidly,
      // and they can kick in very rapidly
      // after massive hash power jumps on or off.

      // Important note: This is a self-damping adjustment because 8/5 and 5/8 are
      // closer to 1 than 3/2 and 2/3. Do not
      // screw with the constants in a way that breaks this relationship. Even though
      // self-damping, it will usually
      // overshoot slightly. But normal adjustment will handle damping without getting
      // back to emergency.
      toofast = (nIntervalDesired * 2) / 3;
      tooslow = (nIntervalDesired * 3) / 2;

      // both of these check the shortest interval to quickly stop when overshot.
      // Otherwise first is longer and second shorter.
      if (averages.avgOf5 < toofast && averages.avgOf9 < toofast && averages.avgOf17 < toofast)
      { // emergency adjustment, slow down (longer intervals because shorter blocks)
        // LogPrint(BCLog::MIDAS, "GetNextWorkRequired EMERGENCY RETARGET\n");
        difficultyfactor *= 8;
        difficultyfactor /= 5;
      }
      else if (averages.avgOf5 > tooslow && averages.avgOf7 > tooslow && averages.avgOf9 > tooslow)
      { // emergency adjustment, speed up (shorter intervals because longer blocks)
        // LogPrint(BCLog::MIDAS, "GetNextWorkRequired EMERGENCY RETARGET\n");
        difficultyfactor *= 5;
        difficultyfactor /= 8;
      }

      // If no emergency adjustment, check for normal adjustment.
      else if (((averages.avgOf5 > nIntervalDesired || averages.avgOf7 > nIntervalDesired) && averages.avgOf9 > nIntervalDesired
          && averages.avgOf17 > nIntervalDesired)
          || ((averages.avgOf5 < nIntervalDesired || averages.avgOf7 < nIntervalDesired) && averages.avgOf9 < nIntervalDesired
              && averages.avgOf17 < nIntervalDesired))
      { // At least 3 averages too high or at least 3 too low, including the two
        // longest. This will be executed 3/16 of
        // the time on the basis of random variation, even if the settings are perfect.
        // It regulates one-sixth of the way
        // to the calculated point.
        // LogPrint(BCLog::MIDAS, "GetNextWorkRequired RETARGET\n");
        difficultyfactor *= (6 * nIntervalDesired);
        difficultyfactor /= averages.avgOf17 + (5 * nIntervalDesired);
      }

      // limit to doubling or halving. There are no conditions where this will make a
      // difference unless there is an
      // unsuspected bug in the above code.
      if (difficultyfactor > 20000)
        difficultyfactor = 20000;
      if (difficultyfactor < 5000)
        difficultyfactor = 5000;

      proposed = Utils.decodeCompactBits(storedPrev.getHeader().getDifficultyTarget());

      if (difficultyfactor != 10000)
      {
        proposed.divide(BigInteger.valueOf(difficultyfactor));
        proposed.multiply(BigInteger.valueOf(10000L));
      }

    }

    verifyDifficulty(storedPrev, next, proposed);
  }

  /**
   * Storage class for MIDAS averages.
   * 
   * @author phil_000
   *
   */
  static class Averages
  {
    public long avgOf5;
    public long avgOf7;
    public long avgOf9;
    public long avgOf17;

    public Averages()
    {
      avgOf5 = 0;
      avgOf7 = 0;
      avgOf9 = 0;
      avgOf17 = 0;
    }
  }

  protected Averages averageRecentTimestamps(StoredBlock storedPrev, BlockStore blockStore)
  {
    Averages retval = new Averages();
    long blocktime = 0;
    long oldblocktime = 0;

    if (null != storedPrev)
    {
      blocktime = storedPrev.getHeader().getTimeSeconds();
    }

    StoredBlock prev = storedPrev;
    for (int blockoffset = 0; blockoffset < 17; blockoffset++)
    {
      oldblocktime = blocktime;
      if (null != storedPrev)
      {
        try
        {
          prev = prev.getPrev(blockStore);
          blocktime = prev.getHeader().getTimeSeconds();
        }
        catch (BlockStoreException e)
        {
          blocktime = 0;
        }
      }
      else
      {
        blocktime -= TARGET_SPACING;
      }
      // for each block, add interval.
      if (blockoffset < 5)
        retval.avgOf5 += (oldblocktime - blocktime);
      if (blockoffset < 7)
        retval.avgOf7 += (oldblocktime - blocktime);
      if (blockoffset < 9)
        retval.avgOf9 += (oldblocktime - blocktime);
      retval.avgOf17 += (oldblocktime - blocktime);
    }

    retval.avgOf5 /= 5;
    retval.avgOf7 /= 7;
    retval.avgOf9 /= 9;
    retval.avgOf17 /= 17;

    return retval;
  }

  @Override
  public Coin getMaxMoney()
  {
    return MAX_MONEY;
  }

  @Override
  public Coin getMinNonDustOutput()
  {
    return isDIP0001ActiveAtTip() ? Transaction.MIN_NONDUST_OUTPUT.div(10) : Transaction.MIN_NONDUST_OUTPUT;
  }

  @Override
  public MonetaryFormat getMonetaryFormat()
  {
    return new MonetaryFormat();
  }

  @Override
  public int getProtocolVersionNum(final ProtocolVersion version)
  {
    return version.getBitcoinProtocolVersion();
  }

  @Override
  public ThoughtSerializer getSerializer(boolean parseRetain)
  {
    return new ThoughtSerializer(this, parseRetain);
  }

  @Override
  public String getUriScheme()
  {
    return COIN_SCHEME;
  }

  @Override
  public boolean hasMaxMoney()
  {
    return true;
  }
}
