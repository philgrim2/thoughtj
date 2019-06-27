/*
 * Copyright 2014 Adam Mackler
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

package live.thought.thoughtj.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import live.thought.thoughtj.core.Coin;
import live.thought.thoughtj.utils.ThtAutoFormat;
import live.thought.thoughtj.utils.ThtFixedFormat;
import live.thought.thoughtj.utils.ThtFormat;

import java.math.BigDecimal;
import java.text.*;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static java.text.NumberFormat.Field.DECIMAL_SEPARATOR;
import static java.util.Locale.*;
import static live.thought.thoughtj.core.Coin.*;
import static live.thought.thoughtj.core.NetworkParameters.MAX_MONEY;
import static live.thought.thoughtj.utils.ThtAutoFormat.Style.CODE;
import static live.thought.thoughtj.utils.ThtAutoFormat.Style.SYMBOL;
import static live.thought.thoughtj.utils.ThtFixedFormat.REPEATING_DOUBLETS;
import static live.thought.thoughtj.utils.ThtFixedFormat.REPEATING_TRIPLETS;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class ThtFormatTest {

    @Parameters
    public static Set<Locale[]> data() {
        Set<Locale[]> localeSet = new HashSet<Locale[]>();
        for (Locale locale : Locale.getAvailableLocales()) {
            localeSet.add(new Locale[]{locale});
        }
        return localeSet;
    }

    public ThtFormatTest(Locale defaultLocale) {
        Locale.setDefault(defaultLocale);
    }
 
    @Test
    public void prefixTest() { // prefix b/c symbol is prefixed
        ThtFormat usFormat = ThtFormat.getSymbolInstance(Locale.US);
        assertEquals("T1.00", usFormat.format(COIN));
        assertEquals("T1.01", usFormat.format(101000000));
        assertEquals("₥T0.01", usFormat.format(1000));
        assertEquals("₥T1,011.00", usFormat.format(101100000));
        assertEquals("₥T1,000.01", usFormat.format(100001000));
        assertEquals("µT1,000,001.00", usFormat.format(100000100));
        assertEquals("µT1,000,000.10", usFormat.format(100000010));
        assertEquals("µT1,000,000.01", usFormat.format(100000001));
        assertEquals("µT1.00", usFormat.format(100));
        assertEquals("µT0.10", usFormat.format(10));
        assertEquals("µT0.01", usFormat.format(1));
    }

    @Test
    public void suffixTest() {
        ThtFormat deFormat = ThtFormat.getSymbolInstance(Locale.GERMANY);
        // int
        assertEquals("1,00 T", deFormat.format(100000000));
        assertEquals("1,01 T", deFormat.format(101000000));
        assertEquals("1.011,00 ₥T", deFormat.format(101100000));
        assertEquals("1.000,01 ₥T", deFormat.format(100001000));
        assertEquals("1.000.001,00 µT", deFormat.format(100000100));
        assertEquals("1.000.000,10 µT", deFormat.format(100000010));
        assertEquals("1.000.000,01 µT", deFormat.format(100000001));
    }

    @Test
    public void defaultLocaleTest() {
        assertEquals(
             "Default Locale is " + Locale.getDefault().toString(),
             ThtFormat.getInstance().pattern(), ThtFormat.getInstance(Locale.getDefault()).pattern()
        );
        assertEquals(
            "Default Locale is " + Locale.getDefault().toString(),
            ThtFormat.getCodeInstance().pattern(),
            ThtFormat.getCodeInstance(Locale.getDefault()).pattern()
       );
    }

    @Test
    public void symbolCollisionTest() {
        Locale[] locales = ThtFormat.getAvailableLocales();
        for (int i = 0; i < locales.length; ++i) {
            String cs = ((DecimalFormat)NumberFormat.getCurrencyInstance(locales[i])).
                        getDecimalFormatSymbols().getCurrencySymbol();
            if (cs.contains("T")) {
                ThtFormat bf = ThtFormat.getSymbolInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("T"));
                assertFalse(coin.contains("T"));
                String milli = bf.format(valueOf(10000));
                assertTrue(milli.contains("₥T"));
                assertFalse(milli.contains("T"));
                String micro = bf.format(valueOf(100));
                assertTrue(micro.contains("µT"));
                assertFalse(micro.contains("T"));
                ThtFormat ff = ThtFormat.builder().scale(0).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("T", ((ThtFixedFormat)ff).symbol());
                assertEquals("T", ff.coinSymbol());
                coin = ff.format(COIN);
                assertTrue(coin.contains("T"));
                assertFalse(coin.contains("T"));
                ThtFormat mlff = ThtFormat.builder().scale(3).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("₥T", ((ThtFixedFormat)mlff).symbol());
                assertEquals("T", mlff.coinSymbol());
                milli = mlff.format(valueOf(10000));
                assertTrue(milli.contains("₥T"));
                assertFalse(milli.contains("T"));
                ThtFormat mcff = ThtFormat.builder().scale(6).locale(locales[i]).pattern("¤#.#").build();
                assertEquals("µT", ((ThtFixedFormat)mcff).symbol());
                assertEquals("T", mcff.coinSymbol());
                micro = mcff.format(valueOf(100));
                assertTrue(micro.contains("µT"));
                assertFalse(micro.contains("T"));
            }
            if (cs.contains("T")) {  // NB: We don't know of any such existing locale, but check anyway.
                ThtFormat bf = ThtFormat.getInstance(locales[i]);
                String coin = bf.format(COIN);
                assertTrue(coin.contains("T"));
                assertFalse(coin.contains("T"));
                String milli = bf.format(valueOf(10000));
                assertTrue(milli.contains("₥T"));
                assertFalse(milli.contains("T"));
                String micro = bf.format(valueOf(100));
                assertTrue(micro.contains("µT"));
                assertFalse(micro.contains("T"));
            }
        }
    }

    @Test
    public void argumentTypeTest() {
        ThtFormat usFormat = ThtFormat.getSymbolInstance(Locale.US);
        // longs are tested above
        // Coin
        assertEquals("µT1,000,000.01", usFormat.format(COIN.add(valueOf(1))));
        // Integer
        assertEquals("µT21,474,836.47" ,usFormat.format(Integer.MAX_VALUE));
        assertEquals("(µT21,474,836.48)" ,usFormat.format(Integer.MIN_VALUE));
        // Long
        assertEquals("µT92,233,720,368,547,758.07" ,usFormat.format(Long.MAX_VALUE));
        assertEquals("(µT92,233,720,368,547,758.08)" ,usFormat.format(Long.MIN_VALUE));
        // BigInteger
        assertEquals("µT0.10" ,usFormat.format(java.math.BigInteger.TEN));
        assertEquals("T0.00" ,usFormat.format(java.math.BigInteger.ZERO));
        // BigDecimal
        assertEquals("T1.00" ,usFormat.format(java.math.BigDecimal.ONE));
        assertEquals("T0.00" ,usFormat.format(java.math.BigDecimal.ZERO));
        // use of Double not encouraged but no way to stop user from converting one to BigDecimal
        assertEquals(
            "T179,769,313,486,231,570,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000,000.00",
            usFormat.format(java.math.BigDecimal.valueOf(Double.MAX_VALUE)));
        assertEquals("T0.00", usFormat.format(java.math.BigDecimal.valueOf(Double.MIN_VALUE)));
        assertEquals(
            "T340,282,346,638,528,860,000,000,000,000,000,000,000.00",
            usFormat.format(java.math.BigDecimal.valueOf(Float.MAX_VALUE)));
        // Bad type
        try {
            usFormat.format("1");
            fail("should not have tried to format a String");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void columnAlignmentTest() {
        ThtFormat germany = ThtFormat.getCoinInstance(2,ThtFixedFormat.REPEATING_PLACES);
        char separator = germany.symbols().getDecimalSeparator();
        Coin[] rows = {MAX_MONEY, MAX_MONEY.subtract(NOTION), Coin.parseCoin("1234"),
                       COIN, COIN.add(NOTION), COIN.subtract(NOTION),
                        COIN.divide(1000).add(NOTION), COIN.divide(1000), COIN.divide(1000).subtract(NOTION),
                       valueOf(100), valueOf(1000), valueOf(10000),
                       NOTION};
        FieldPosition fp = new FieldPosition(DECIMAL_SEPARATOR);
        String[] output = new String[rows.length];
        int[] indexes = new int[rows.length];
        int maxIndex = 0;
        for (int i = 0; i < rows.length; i++) {
            output[i] = germany.format(rows[i], new StringBuffer(), fp).toString();
            indexes[i] = fp.getBeginIndex();
            if (indexes[i] > maxIndex) maxIndex = indexes[i];
        }
        for (int i = 0; i < output.length; i++) {
            // uncomment to watch printout
            // System.out.println(repeat(" ", (maxIndex - indexes[i])) + output[i]);
            assertEquals(output[i].indexOf(separator), indexes[i]);
        }
    }

    @Test
    public void repeatingPlaceTest() {
        ThtFormat mega = ThtFormat.getInstance(-6, US);
        Coin value = MAX_MONEY.subtract(NOTION);
        assertEquals("21.99999999999999", mega.format(value, 0, ThtFixedFormat.REPEATING_PLACES));
        assertEquals("21.99999999999999", mega.format(value, 0, ThtFixedFormat.REPEATING_PLACES));
        assertEquals("21.99999999999999", mega.format(value, 1, ThtFixedFormat.REPEATING_PLACES));
        assertEquals("21.99999999999999", mega.format(value, 2, ThtFixedFormat.REPEATING_PLACES));
        assertEquals("21.99999999999999", mega.format(value, 3, ThtFixedFormat.REPEATING_PLACES));
        assertEquals("21.99999999999999", mega.format(value, 0, ThtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("21.99999999999999", mega.format(value, 1, ThtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("21.99999999999999", mega.format(value, 2, ThtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("21.99999999999999", mega.format(value, 3, ThtFixedFormat.REPEATING_DOUBLETS));
        assertEquals("21.99999999999999", mega.format(value, 0, ThtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("21.99999999999999", mega.format(value, 1, ThtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("21.99999999999999", mega.format(value, 2, ThtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("21.99999999999999", mega.format(value, 3, ThtFixedFormat.REPEATING_TRIPLETS));
        assertEquals("1.00000005", ThtFormat.getCoinInstance(US).
                                   format(COIN.add(Coin.valueOf(5)), 0, ThtFixedFormat.REPEATING_PLACES));
    }

    @Test
    public void characterIteratorTest() {
        ThtFormat usFormat = ThtFormat.getInstance(Locale.US);
        AttributedCharacterIterator i = usFormat.formatToCharacterIterator(parseCoin("1234.5"));
        java.util.Set<Attribute> a = i.getAllAttributeKeys();
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));
        assertTrue("Missing integer attribute", a.contains(NumberFormat.Field.INTEGER));
        assertTrue("Missing fraction attribute", a.contains(NumberFormat.Field.FRACTION));
        assertTrue("Missing decimal separator attribute", a.contains(NumberFormat.Field.DECIMAL_SEPARATOR));
        assertTrue("Missing grouping separator attribute", a.contains(NumberFormat.Field.GROUPING_SEPARATOR));
        assertTrue("Missing currency attribute", a.contains(NumberFormat.Field.CURRENCY));

        char c;
        i = ThtFormat.getCodeInstance(Locale.US).formatToCharacterIterator(new BigDecimal("0.19246362747414458"));
        // formatted as "µTHT 192,463.63"
        assertEquals(0, i.getBeginIndex());
        assertEquals(16, i.getEndIndex());
        int n = 0;
        for(c = i.first(); i.getAttribute(NumberFormat.Field.CURRENCY) != null; c = i.next()) {
            n++;
        }
        assertEquals(5, n);
        n = 0;
        for(i.next(); i.getAttribute(NumberFormat.Field.INTEGER) != null && i.getAttribute(NumberFormat.Field.GROUPING_SEPARATOR) != NumberFormat.Field.GROUPING_SEPARATOR; c = i.next()) {
            n++;
        }
        assertEquals(3, n);
        assertEquals(NumberFormat.Field.INTEGER, i.getAttribute(NumberFormat.Field.INTEGER));
        n = 0;
        for(c = i.next(); i.getAttribute(NumberFormat.Field.INTEGER) != null; c = i.next()) {
            n++;
        }
        assertEquals(3, n);
        assertEquals(NumberFormat.Field.DECIMAL_SEPARATOR, i.getAttribute(NumberFormat.Field.DECIMAL_SEPARATOR));
        n = 0;
        for(c = i.next(); c != CharacterIterator.DONE; c = i.next()) {
            n++;
            assertNotNull(i.getAttribute(NumberFormat.Field.FRACTION));
        }
        assertEquals(2,n);

        // immutability check
        ThtFormat fa = ThtFormat.getSymbolInstance(US);
        ThtFormat fb = ThtFormat.getSymbolInstance(US);
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fa.formatToCharacterIterator(COIN.multiply(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
        fb.formatToCharacterIterator(COIN.divide(1000000));
        assertEquals(fa, fb);
        assertEquals(fa.hashCode(), fb.hashCode());
    }

    @Test
    public void parseTest() throws java.text.ParseException {
        ThtFormat us = ThtFormat.getSymbolInstance(Locale.US);
        ThtFormat usCoded = ThtFormat.getCodeInstance(Locale.US);
        // Coins
        assertEquals(valueOf(200000000), us.parseObject("THT2"));
        assertEquals(valueOf(200000000), us.parseObject("THT2"));
        assertEquals(valueOf(200000000), us.parseObject("T2"));
        assertEquals(valueOf(200000000), us.parseObject("T2"));
        assertEquals(valueOf(200000000), us.parseObject("2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), us.parseObject("T2.0"));
        assertEquals(valueOf(200000000), us.parseObject("T2.0"));
        assertEquals(valueOf(200000000), us.parseObject("2.0"));
        assertEquals(valueOf(200000000), us.parseObject("THT2.0"));
        assertEquals(valueOf(200000000), us.parseObject("THT2.0"));
        assertEquals(valueOf(200000000), usCoded.parseObject("T 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("T 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(202222420000000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("T2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("T2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("THT2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("THT2,022,224.20"));
        assertEquals(valueOf(220200000000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(2100000000000000L), us.parseObject("21000000.00000000"));
        // MilliCoins
        assertEquals(valueOf(200000), usCoded.parseObject("mTHT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mTHT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mT 2"));
        assertEquals(valueOf(200000), us.parseObject("mTHT2"));
        assertEquals(valueOf(200000), us.parseObject("mTHT2"));
        assertEquals(valueOf(200000), us.parseObject("₥T2"));
        assertEquals(valueOf(200000), us.parseObject("₥T2"));
        assertEquals(valueOf(200000), us.parseObject("₥2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2.00"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2.00"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥T 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥T 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥ 2"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥T2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("₥T2,022,224.20"));
        assertEquals(valueOf(202222400000L), us.parseObject("mT2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("mT2,022,224.20"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥THT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥THT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mTHT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mTHT2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("₥2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥T 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("₥T 2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mT 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("mT 2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥THT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥THT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mTHT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mTHT 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("₥ 2,022,224.20"));
        // Microcoins
        assertEquals(valueOf(435), us.parseObject("µT4.35"));
        assertEquals(valueOf(435), us.parseObject("uT4.35"));
        assertEquals(valueOf(435), us.parseObject("uT4.35"));
        assertEquals(valueOf(435), us.parseObject("µT4.35"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("µTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("µTHT4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("uTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("uTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("µTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("µTHT 4.35"));
        // fractional satoshi; round up
        assertEquals(valueOf(435), us.parseObject("uTHT4.345"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.345"));
        // negative with mu symbol
        assertEquals(valueOf(-1), usCoded.parseObject("(µT 0.01)"));
        assertEquals(valueOf(-10), us.parseObject("(µTHT0.100)"));
        assertEquals(valueOf(-10), us.parseObject("(µTHT0.100)"));

        // Same thing with addition of custom code, symbol
        us = ThtFormat.builder().locale(US).style(SYMBOL).symbol("£").code("XYZ").build();
        usCoded = ThtFormat.builder().locale(US).scale(0).symbol("£").code("XYZ").
                            pattern("¤ #,##0.00").build();
        // Coins
        assertEquals(valueOf(200000000), us.parseObject("XYZ2"));
        assertEquals(valueOf(200000000), us.parseObject("THT2"));
        assertEquals(valueOf(200000000), us.parseObject("THT2"));
        assertEquals(valueOf(200000000), us.parseObject("£2"));
        assertEquals(valueOf(200000000), us.parseObject("T2"));
        assertEquals(valueOf(200000000), us.parseObject("T|T2"));
        assertEquals(valueOf(200000000), us.parseObject("2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), us.parseObject("£2.0"));
        assertEquals(valueOf(200000000), us.parseObject("T2.0"));
        assertEquals(valueOf(200000000), us.parseObject("T2.0"));
        assertEquals(valueOf(200000000), us.parseObject("2.0"));
        assertEquals(valueOf(200000000), us.parseObject("XYZ2.0"));
        assertEquals(valueOf(200000000), us.parseObject("THT2.0"));
        assertEquals(valueOf(200000000), us.parseObject("THT2.0"));
        assertEquals(valueOf(200000000), usCoded.parseObject("£ 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("T 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("T 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject(" 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("XYZ 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(200000000), usCoded.parseObject("THT 2"));
        assertEquals(valueOf(202222420000000L), us.parseObject("2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("£2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("T2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("T2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("XYZ2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("THT2,022,224.20"));
        assertEquals(valueOf(202222420000000L), us.parseObject("THT2,022,224.20"));
        assertEquals(valueOf(220200000000L), us.parseObject("2,202.0"));
        assertEquals(valueOf(2100000000000000L), us.parseObject("21000000.00000000"));
        // MilliCoins
        assertEquals(valueOf(200000), usCoded.parseObject("mXYZ 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mTHT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mTHT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("m£ 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("mT 2"));
        assertEquals(valueOf(200000), us.parseObject("mXYZ2"));
        assertEquals(valueOf(200000), us.parseObject("mTHT2"));
        assertEquals(valueOf(200000), us.parseObject("mTHT2"));
        assertEquals(valueOf(200000), us.parseObject("₥£2"));
        assertEquals(valueOf(200000), us.parseObject("₥T2"));
        assertEquals(valueOf(200000), us.parseObject("₥T2"));
        assertEquals(valueOf(200000), us.parseObject("₥2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥XYZ 2.00"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2.00"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2.00"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥XYZ 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥THT 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥£ 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥T 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥T 2"));
        assertEquals(valueOf(200000), usCoded.parseObject("₥ 2"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥£2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥T2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("₥T2,022,224.20"));
        assertEquals(valueOf(202222400000L), us.parseObject("m£2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mT2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("mT2,022,224.20"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥XYZ2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥THT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("₥THT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mXYZ2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mTHT2,022,224"));
        assertEquals(valueOf(202222400000L), us.parseObject("mTHT2,022,224"));
        assertEquals(valueOf(202222420000L), us.parseObject("₥2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥£ 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥T 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("₥T 2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("m£ 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mT 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("mT 2,022,224.20"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥XYZ 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥THT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("₥THT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mXYZ 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mTHT 2,022,224"));
        assertEquals(valueOf(202222400000L), usCoded.parseObject("mTHT 2,022,224"));
        assertEquals(valueOf(202222420000L), usCoded.parseObject("₥ 2,022,224.20"));
        // Microcoins
        assertEquals(valueOf(435), us.parseObject("µ£4.35"));
        assertEquals(valueOf(435), us.parseObject("µT4.35"));
        assertEquals(valueOf(435), us.parseObject("uT4.35"));
        assertEquals(valueOf(435), us.parseObject("u£4.35"));
        assertEquals(valueOf(435), us.parseObject("uT4.35"));
        assertEquals(valueOf(435), us.parseObject("µT4.35"));
        assertEquals(valueOf(435), us.parseObject("uXYZ4.35"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("µXYZ4.35"));
        assertEquals(valueOf(435), us.parseObject("µTHT4.35"));
        assertEquals(valueOf(435), us.parseObject("µTHT4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("uXYZ 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("uTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("uTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("µXYZ 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("µTHT 4.35"));
        assertEquals(valueOf(435), usCoded.parseObject("µTHT 4.35"));
        // fractional satoshi; round up
        assertEquals(valueOf(435), us.parseObject("uXYZ4.345"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.345"));
        assertEquals(valueOf(435), us.parseObject("uTHT4.345"));
        // negative with mu symbol
        assertEquals(valueOf(-1), usCoded.parseObject("µ£ -0.01"));
        assertEquals(valueOf(-1), usCoded.parseObject("µT -0.01"));
        assertEquals(valueOf(-10), us.parseObject("(µXYZ0.100)"));
        assertEquals(valueOf(-10), us.parseObject("(µTHT0.100)"));
        assertEquals(valueOf(-10), us.parseObject("(µTHT0.100)"));

        // parse() method as opposed to parseObject
        try {
            ThtFormat.getInstance().parse("abc");
            fail("bad parse must raise exception");
        } catch (ParseException e) {}
    }

    @Test
    public void parseMetricTest() throws ParseException {
        ThtFormat cp = ThtFormat.getCodeInstance(Locale.US);
        ThtFormat sp = ThtFormat.getSymbolInstance(Locale.US);
        // coin
        assertEquals(parseCoin("1"), cp.parseObject("THT 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("THT1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("T 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("T1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("D⃦ 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("D⃦1.00"));
        assertEquals(parseCoin("1"), cp.parseObject("T 1.00"));
        assertEquals(parseCoin("1"), sp.parseObject("T1.00"));
        // milli
        assertEquals(parseCoin("0.001"), cp.parseObject("mTHT 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mTHT1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mT 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mT1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mD⃦ 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mD⃦1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("mT 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("mT1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥THT 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥THT1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥T 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥T1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥D⃦ 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥D⃦1.00"));
        assertEquals(parseCoin("0.001"), cp.parseObject("₥T 1.00"));
        assertEquals(parseCoin("0.001"), sp.parseObject("₥T1.00"));
        // micro
        assertEquals(parseCoin("0.000001"), cp.parseObject("uTHT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uTHT1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uT1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uD⃦ 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uD⃦1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("uT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("uT1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µTHT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µTHT1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µT1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µD⃦ 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µD⃦1.00"));
        assertEquals(parseCoin("0.000001"), cp.parseObject("µT 1.00"));
        assertEquals(parseCoin("0.000001"), sp.parseObject("µT1.00"));
        // satoshi
        assertEquals(parseCoin("0.00000001"), cp.parseObject("uTHT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("uTHT0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("uT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("uT0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("uD⃦ 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("uD⃦0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("uT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("uT0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("µTHT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("µTHT0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("µT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("µT0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("µD⃦ 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("µD⃦0.01"));
        assertEquals(parseCoin("0.00000001"), cp.parseObject("µT 0.01"));
        assertEquals(parseCoin("0.00000001"), sp.parseObject("µT0.01"));
        // cents
        assertEquals(parseCoin("0.01234567"), cp.parseObject("cTHT 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("cTHT1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("cT 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("cT1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("cD⃦ 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("cD⃦1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("cT 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("cT1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("¢THT 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("¢THT1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("¢T 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("¢T1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("¢D⃦ 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("¢D⃦1.234567"));
        assertEquals(parseCoin("0.01234567"), cp.parseObject("¢T 1.234567"));
        assertEquals(parseCoin("0.01234567"), sp.parseObject("¢T1.234567"));
        // dekacoins
        assertEquals(parseCoin("12.34567"), cp.parseObject("daTHT 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daTHT1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daT 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daT1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daD⃦ 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daD⃦1.234567"));
        assertEquals(parseCoin("12.34567"), cp.parseObject("daT 1.234567"));
        assertEquals(parseCoin("12.34567"), sp.parseObject("daT1.234567"));
        // hectocoins
        assertEquals(parseCoin("123.4567"), cp.parseObject("hTHT 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hTHT1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hT 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hT1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hD⃦ 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hD⃦1.234567"));
        assertEquals(parseCoin("123.4567"), cp.parseObject("hT 1.234567"));
        assertEquals(parseCoin("123.4567"), sp.parseObject("hT1.234567"));
        // kilocoins
        assertEquals(parseCoin("1234.567"), cp.parseObject("kTHT 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kTHT1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kT 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kT1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kD⃦ 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kD⃦1.234567"));
        assertEquals(parseCoin("1234.567"), cp.parseObject("kT 1.234567"));
        assertEquals(parseCoin("1234.567"), sp.parseObject("kT1.234567"));
        // megacoins
        assertEquals(parseCoin("1234567"), cp.parseObject("MTHT 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MTHT1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MT 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MT1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MD⃦ 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MD⃦1.234567"));
        assertEquals(parseCoin("1234567"), cp.parseObject("MT 1.234567"));
        assertEquals(parseCoin("1234567"), sp.parseObject("MT1.234567"));
    }

    @Test
    public void parsePositionTest() {
        ThtFormat usCoded = ThtFormat.getCodeInstance(Locale.US);
        // Test the field constants
        FieldPosition intField = new FieldPosition(NumberFormat.Field.INTEGER);
        assertEquals(
          "987,654,321",
          usCoded.format(valueOf(98765432123L), new StringBuffer(), intField).
          substring(intField.getBeginIndex(), intField.getEndIndex())
        );
        FieldPosition fracField = new FieldPosition(NumberFormat.Field.FRACTION);
        assertEquals(
          "23",
          usCoded.format(valueOf(98765432123L), new StringBuffer(), fracField).
          substring(fracField.getBeginIndex(), fracField.getEndIndex())
        );

        // for currency we use a locale that puts the units at the end
        ThtFormat de = ThtFormat.getSymbolInstance(Locale.GERMANY);
        ThtFormat deCoded = ThtFormat.getCodeInstance(Locale.GERMANY);
        FieldPosition currField = new FieldPosition(NumberFormat.Field.CURRENCY);
        assertEquals(
          "µT",
          de.format(valueOf(98765432123L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "µTHT",
          deCoded.format(valueOf(98765432123L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "₥T",
          de.format(valueOf(98765432000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "mTHT",
          deCoded.format(valueOf(98765432000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "T",
          de.format(valueOf(98765000000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
        assertEquals(
          "THT",
          deCoded.format(valueOf(98765000000L), new StringBuffer(), currField).
          substring(currField.getBeginIndex(), currField.getEndIndex())
        );
    }

    @Test
    public void currencyCodeTest() {
        /* Insert needed space AFTER currency-code */
        ThtFormat usCoded = ThtFormat.getCodeInstance(Locale.US);
        assertEquals("µTHT 0.01", usCoded.format(1));
        assertEquals("THT 1.00", usCoded.format(COIN));

        /* Do not insert unneeded space BEFORE currency-code */
        ThtFormat frCoded = ThtFormat.getCodeInstance(Locale.FRANCE);
        assertEquals("0,01 µTHT", frCoded.format(1));
        assertEquals("1,00 THT", frCoded.format(COIN));

        /* Insert needed space BEFORE currency-code: no known currency pattern does this? */

        /* Do not insert unneeded space AFTER currency-code */
        ThtFormat deCoded = ThtFormat.getCodeInstance(Locale.ITALY);
        assertEquals("µTHT 0,01", deCoded.format(1));
        assertEquals("THT 1,00", deCoded.format(COIN));
    }

    @Test
    public void coinScaleTest() throws Exception {
        ThtFormat coinFormat = ThtFormat.getCoinInstance(Locale.US);
        assertEquals("1.00", coinFormat.format(Coin.COIN));
        assertEquals("-1.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(1000000), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("1000"), coinFormat.parseObject("1000"));
    }

    @Test
    public void millicoinScaleTest() throws Exception {
        ThtFormat coinFormat = ThtFormat.getMilliInstance(Locale.US);
        assertEquals("1,000.00", coinFormat.format(Coin.COIN));
        assertEquals("-1,000.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(1000), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("1"), coinFormat.parseObject("1000"));
    }

    @Test
    public void microcoinScaleTest() throws Exception {
        ThtFormat coinFormat = ThtFormat.getMicroInstance(Locale.US);
        assertEquals("1,000,000.00", coinFormat.format(Coin.COIN));
        assertEquals("-1,000,000.00", coinFormat.format(Coin.COIN.negate()));
        assertEquals("1,000,000.10", coinFormat.format(Coin.COIN.add(valueOf(10))));
        assertEquals(Coin.parseCoin("0.000001"), coinFormat.parseObject("1.00"));
        assertEquals(valueOf(1), coinFormat.parseObject("0.01"));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1,000.00"));
        assertEquals(Coin.parseCoin("0.001"), coinFormat.parseObject("1000"));
    }

    @Test
    public void testGrouping() throws Exception {
        ThtFormat usCoin = ThtFormat.getInstance(0, Locale.US, 1, 2, 3);
        assertEquals("0.1", usCoin.format(Coin.parseCoin("0.1")));
        assertEquals("0.010", usCoin.format(Coin.parseCoin("0.01")));
        assertEquals("0.001", usCoin.format(Coin.parseCoin("0.001")));
        assertEquals("0.000100", usCoin.format(Coin.parseCoin("0.0001")));
        assertEquals("0.000010", usCoin.format(Coin.parseCoin("0.00001")));
        assertEquals("0.000001", usCoin.format(Coin.parseCoin("0.000001")));

        // no more than two fractional decimal places for the default coin-denomination
        assertEquals("0.01", ThtFormat.getCoinInstance(Locale.US).format(Coin.parseCoin("0.005")));

        ThtFormat usMilli = ThtFormat.getInstance(3, Locale.US, 1, 2, 3);
        assertEquals("0.1", usMilli.format(Coin.parseCoin("0.0001")));
        assertEquals("0.010", usMilli.format(Coin.parseCoin("0.00001")));
        assertEquals("0.001", usMilli.format(Coin.parseCoin("0.000001")));
        // even though last group is 3, that would result in fractional satoshis, which we don't do
        assertEquals("0.00010", usMilli.format(Coin.valueOf(10)));
        assertEquals("0.00001", usMilli.format(Coin.valueOf(1)));

        ThtFormat usMicro = ThtFormat.getInstance(6, Locale.US, 1, 2, 3);
        assertEquals("0.1", usMicro.format(Coin.valueOf(10)));
        // even though second group is 2, that would result in fractional satoshis, which we don't do
        assertEquals("0.01", usMicro.format(Coin.valueOf(1)));
    }


    /* These just make sure factory methods don't raise exceptions.
     * Other tests inspect their return values. */
    @Test
    public void factoryTest() {
        ThtFormat coded = ThtFormat.getInstance(0, 1, 2, 3);
        ThtFormat.getInstance(ThtAutoFormat.Style.CODE);
        ThtAutoFormat symbolic = (ThtAutoFormat)ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL);
        assertEquals(2, symbolic.fractionPlaces());
        ThtFormat.getInstance(ThtAutoFormat.Style.CODE, 3);
        assertEquals(3, ((ThtAutoFormat)ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL, 3)).fractionPlaces());
        ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL, Locale.US, 3);
        ThtFormat.getInstance(ThtAutoFormat.Style.CODE, Locale.US);
        ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL, Locale.US);
        ThtFormat.getCoinInstance(2, ThtFixedFormat.REPEATING_PLACES);
        ThtFormat.getMilliInstance(1, 2, 3);
        ThtFormat.getInstance(2);
        ThtFormat.getInstance(2, Locale.US);
        ThtFormat.getCodeInstance(3);
        ThtFormat.getSymbolInstance(3);
        ThtFormat.getCodeInstance(Locale.US, 3);
        ThtFormat.getSymbolInstance(Locale.US, 3);
        try {
            ThtFormat.getInstance(SMALLEST_UNIT_EXPONENT + 1);
            fail("should not have constructed an instance with denomination less than satoshi");
        } catch (IllegalArgumentException e) {}
    }
    @Test
    public void factoryArgumentsTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;
        assertEquals(ThtFormat.getInstance(), ThtFormat.getCodeInstance());
        assertEquals(ThtFormat.getInstance(locale), ThtFormat.getCodeInstance(locale));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.CODE), ThtFormat.getCodeInstance());
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL), ThtFormat.getSymbolInstance());
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.CODE,3), ThtFormat.getCodeInstance(3));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL,3), ThtFormat.getSymbolInstance(3));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.CODE,locale), ThtFormat.getCodeInstance(locale));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL,locale), ThtFormat.getSymbolInstance(locale));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.CODE,locale,3), ThtFormat.getCodeInstance(locale,3));
        assertEquals(ThtFormat.getInstance(ThtAutoFormat.Style.SYMBOL,locale,3), ThtFormat.getSymbolInstance(locale,3));
        assertEquals(ThtFormat.getCoinInstance(), ThtFormat.getInstance(0));
        assertEquals(ThtFormat.getMilliInstance(), ThtFormat.getInstance(3));
        assertEquals(ThtFormat.getMicroInstance(), ThtFormat.getInstance(6));
        assertEquals(ThtFormat.getCoinInstance(3), ThtFormat.getInstance(0,3));
        assertEquals(ThtFormat.getMilliInstance(3), ThtFormat.getInstance(3,3));
        assertEquals(ThtFormat.getMicroInstance(3), ThtFormat.getInstance(6,3));
        assertEquals(ThtFormat.getCoinInstance(3,4,5), ThtFormat.getInstance(0,3,4,5));
        assertEquals(ThtFormat.getMilliInstance(3,4,5), ThtFormat.getInstance(3,3,4,5));
        assertEquals(ThtFormat.getMicroInstance(3,4,5), ThtFormat.getInstance(6,3,4,5));
        assertEquals(ThtFormat.getCoinInstance(locale), ThtFormat.getInstance(0,locale));
        assertEquals(ThtFormat.getMilliInstance(locale), ThtFormat.getInstance(3,locale));
        assertEquals(ThtFormat.getMicroInstance(locale), ThtFormat.getInstance(6,locale));
        assertEquals(ThtFormat.getCoinInstance(locale,4,5), ThtFormat.getInstance(0,locale,4,5));
        assertEquals(ThtFormat.getMilliInstance(locale,4,5), ThtFormat.getInstance(3,locale,4,5));
        assertEquals(ThtFormat.getMicroInstance(locale,4,5), ThtFormat.getInstance(6,locale,4,5));
    }

    @Test
    public void autoDecimalTest() {
        ThtFormat codedZero = ThtFormat.getCodeInstance(Locale.US, 0);
        ThtFormat symbolZero = ThtFormat.getSymbolInstance(Locale.US, 0);
        assertEquals("T1", symbolZero.format(COIN));
        assertEquals("THT 1", codedZero.format(COIN));
        assertEquals("µT1,000,000", symbolZero.format(COIN.subtract(NOTION)));
        assertEquals("µTHT 1,000,000", codedZero.format(COIN.subtract(NOTION)));
        assertEquals("µT1,000,000", symbolZero.format(COIN.subtract(Coin.valueOf(50))));
        assertEquals("µTHT 1,000,000", codedZero.format(COIN.subtract(Coin.valueOf(50))));
        assertEquals("µT999,999", symbolZero.format(COIN.subtract(Coin.valueOf(51))));
        assertEquals("µTHT 999,999", codedZero.format(COIN.subtract(Coin.valueOf(51))));
        assertEquals("T1,000", symbolZero.format(COIN.multiply(1000)));
        assertEquals("THT 1,000", codedZero.format(COIN.multiply(1000)));
        assertEquals("µT1", symbolZero.format(Coin.valueOf(100)));
        assertEquals("µTHT 1", codedZero.format(Coin.valueOf(100)));
        assertEquals("µT1", symbolZero.format(Coin.valueOf(50)));
        assertEquals("µTHT 1", codedZero.format(Coin.valueOf(50)));
        assertEquals("µT0", symbolZero.format(Coin.valueOf(49)));
        assertEquals("µTHT 0", codedZero.format(Coin.valueOf(49)));
        assertEquals("µT0", symbolZero.format(Coin.valueOf(1)));
        assertEquals("µTHT 0", codedZero.format(Coin.valueOf(1)));
        assertEquals("µT500,000", symbolZero.format(Coin.valueOf(49999999)));
        assertEquals("µTHT 500,000", codedZero.format(Coin.valueOf(49999999)));

        assertEquals("µT499,500", symbolZero.format(Coin.valueOf(49950000)));
        assertEquals("µTHT 499,500", codedZero.format(Coin.valueOf(49950000)));
        assertEquals("µT499,500", symbolZero.format(Coin.valueOf(49949999)));
        assertEquals("µTHT 499,500", codedZero.format(Coin.valueOf(49949999)));
        assertEquals("µT500,490", symbolZero.format(Coin.valueOf(50049000)));
        assertEquals("µTHT 500,490", codedZero.format(Coin.valueOf(50049000)));
        assertEquals("µT500,490", symbolZero.format(Coin.valueOf(50049001)));
        assertEquals("µTHT 500,490", codedZero.format(Coin.valueOf(50049001)));
        assertEquals("µT500,000", symbolZero.format(Coin.valueOf(49999950)));
        assertEquals("µTHT 500,000", codedZero.format(Coin.valueOf(49999950)));
        assertEquals("µT499,999", symbolZero.format(Coin.valueOf(49999949)));
        assertEquals("µTHT 499,999", codedZero.format(Coin.valueOf(49999949)));
        assertEquals("µT500,000", symbolZero.format(Coin.valueOf(50000049)));
        assertEquals("µTHT 500,000", codedZero.format(Coin.valueOf(50000049)));
        assertEquals("µT500,001", symbolZero.format(Coin.valueOf(50000050)));
        assertEquals("µTHT 500,001", codedZero.format(Coin.valueOf(50000050)));

        ThtFormat codedTwo = ThtFormat.getCodeInstance(Locale.US, 2);
        ThtFormat symbolTwo = ThtFormat.getSymbolInstance(Locale.US, 2);
        assertEquals("T1.00", symbolTwo.format(COIN));
        assertEquals("THT 1.00", codedTwo.format(COIN));
        assertEquals("µT999,999.99", symbolTwo.format(COIN.subtract(NOTION)));
        assertEquals("µTHT 999,999.99", codedTwo.format(COIN.subtract(NOTION)));
        assertEquals("T1,000.00", symbolTwo.format(COIN.multiply(1000)));
        assertEquals("THT 1,000.00", codedTwo.format(COIN.multiply(1000)));
        assertEquals("µT1.00", symbolTwo.format(Coin.valueOf(100)));
        assertEquals("µTHT 1.00", codedTwo.format(Coin.valueOf(100)));
        assertEquals("µT0.50", symbolTwo.format(Coin.valueOf(50)));
        assertEquals("µTHT 0.50", codedTwo.format(Coin.valueOf(50)));
        assertEquals("µT0.49", symbolTwo.format(Coin.valueOf(49)));
        assertEquals("µTHT 0.49", codedTwo.format(Coin.valueOf(49)));
        assertEquals("µT0.01", symbolTwo.format(Coin.valueOf(1)));
        assertEquals("µTHT 0.01", codedTwo.format(Coin.valueOf(1)));

        ThtFormat codedThree = ThtFormat.getCodeInstance(Locale.US, 3);
        ThtFormat symbolThree = ThtFormat.getSymbolInstance(Locale.US, 3);
        assertEquals("T1.000", symbolThree.format(COIN));
        assertEquals("THT 1.000", codedThree.format(COIN));
        assertEquals("µT999,999.99", symbolThree.format(COIN.subtract(NOTION)));
        assertEquals("µTHT 999,999.99", codedThree.format(COIN.subtract(NOTION)));
        assertEquals("T1,000.000", symbolThree.format(COIN.multiply(1000)));
        assertEquals("THT 1,000.000", codedThree.format(COIN.multiply(1000)));
        assertEquals("₥T0.001", symbolThree.format(Coin.valueOf(100)));
        assertEquals("mTHT 0.001", codedThree.format(Coin.valueOf(100)));
        assertEquals("µT0.50", symbolThree.format(Coin.valueOf(50)));
        assertEquals("µTHT 0.50", codedThree.format(Coin.valueOf(50)));
        assertEquals("µT0.49", symbolThree.format(Coin.valueOf(49)));
        assertEquals("µTHT 0.49", codedThree.format(Coin.valueOf(49)));
        assertEquals("µT0.01", symbolThree.format(Coin.valueOf(1)));
        assertEquals("µTHT 0.01", codedThree.format(Coin.valueOf(1)));
    }


    @Test
    public void symbolsCodesTest() {
        ThtFixedFormat coin = (ThtFixedFormat)ThtFormat.getCoinInstance(US);
        assertEquals("THT", coin.code());
        assertEquals("T", coin.symbol());
        ThtFixedFormat cent = (ThtFixedFormat)ThtFormat.getInstance(2, US);
        assertEquals("cTHT", cent.code());
        assertEquals("¢T", cent.symbol());
        ThtFixedFormat milli = (ThtFixedFormat)ThtFormat.getInstance(3, US);
        assertEquals("mTHT", milli.code());
        assertEquals("₥T", milli.symbol());
        ThtFixedFormat micro = (ThtFixedFormat)ThtFormat.getInstance(6, US);
        assertEquals("µTHT", micro.code());
        assertEquals("µT", micro.symbol());
        ThtFixedFormat deka = (ThtFixedFormat)ThtFormat.getInstance(-1, US);
        assertEquals("daTHT", deka.code());
        assertEquals("daT", deka.symbol());
        ThtFixedFormat hecto = (ThtFixedFormat)ThtFormat.getInstance(-2, US);
        assertEquals("hTHT", hecto.code());
        assertEquals("hT", hecto.symbol());
        ThtFixedFormat kilo = (ThtFixedFormat)ThtFormat.getInstance(-3, US);
        assertEquals("kTHT", kilo.code());
        assertEquals("kT", kilo.symbol());
        ThtFixedFormat mega = (ThtFixedFormat)ThtFormat.getInstance(-6, US);
        assertEquals("MTHT", mega.code());
        assertEquals("MT", mega.symbol());
        ThtFixedFormat noSymbol = (ThtFixedFormat)ThtFormat.getInstance(4, US);
        try {
            noSymbol.symbol();
            fail("non-standard denomination has no symbol()");
        } catch (IllegalStateException e) {}
        try {
            noSymbol.code();
            fail("non-standard denomination has no code()");
        } catch (IllegalStateException e) {}

        ThtFixedFormat symbolCoin = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(0).
                                                              symbol("D\u20e6").build();
        assertEquals("THT", symbolCoin.code());
        assertEquals("D⃦", symbolCoin.symbol());
        ThtFixedFormat symbolCent = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(2).
                                                              symbol("D\u20e6").build();
        assertEquals("cTHT", symbolCent.code());
        assertEquals("¢D⃦", symbolCent.symbol());
        ThtFixedFormat symbolMilli = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(3).
                                                               symbol("D\u20e6").build();
        assertEquals("mTHT", symbolMilli.code());
        assertEquals("₥D⃦", symbolMilli.symbol());
        ThtFixedFormat symbolMicro = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(6).
                                                               symbol("D\u20e6").build();
        assertEquals("µTHT", symbolMicro.code());
        assertEquals("µD⃦", symbolMicro.symbol());
        ThtFixedFormat symbolDeka = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-1).
                                                              symbol("D\u20e6").build();
        assertEquals("daTHT", symbolDeka.code());
        assertEquals("daD⃦", symbolDeka.symbol());
        ThtFixedFormat symbolHecto = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-2).
                                                               symbol("D\u20e6").build();
        assertEquals("hTHT", symbolHecto.code());
        assertEquals("hD⃦", symbolHecto.symbol());
        ThtFixedFormat symbolKilo = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-3).
                                                              symbol("D\u20e6").build();
        assertEquals("kTHT", symbolKilo.code());
        assertEquals("kD⃦", symbolKilo.symbol());
        ThtFixedFormat symbolMega = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-6).
                                                              symbol("D\u20e6").build();
        assertEquals("MTHT", symbolMega.code());
        assertEquals("MD⃦", symbolMega.symbol());

        ThtFixedFormat codeCoin = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(0).
                                                            code("THT").build();
        assertEquals("THT", codeCoin.code());
        assertEquals("T", codeCoin.symbol());
        ThtFixedFormat codeCent = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(2).
                                                            code("THT").build();
        assertEquals("cTHT", codeCent.code());
        assertEquals("¢T", codeCent.symbol());
        ThtFixedFormat codeMilli = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(3).
                                                             code("THT").build();
        assertEquals("mTHT", codeMilli.code());
        assertEquals("₥T", codeMilli.symbol());
        ThtFixedFormat codeMicro = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(6).
                                                             code("THT").build();
        assertEquals("µTHT", codeMicro.code());
        assertEquals("µT", codeMicro.symbol());
        ThtFixedFormat codeDeka = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-1).
                                                            code("THT").build();
        assertEquals("daTHT", codeDeka.code());
        assertEquals("daT", codeDeka.symbol());
        ThtFixedFormat codeHecto = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-2).
                                                             code("THT").build();
        assertEquals("hTHT", codeHecto.code());
        assertEquals("hT", codeHecto.symbol());
        ThtFixedFormat codeKilo = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-3).
                                                            code("THT").build();
        assertEquals("kTHT", codeKilo.code());
        assertEquals("kT", codeKilo.symbol());
        ThtFixedFormat codeMega = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-6).
                                                            code("THT").build();
        assertEquals("MTHT", codeMega.code());
        assertEquals("MT", codeMega.symbol());

        ThtFixedFormat symbolCodeCoin = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(0).
                                                                  symbol("D\u20e6").code("THT").build();
        assertEquals("THT", symbolCodeCoin.code());
        assertEquals("D⃦", symbolCodeCoin.symbol());
        ThtFixedFormat symbolCodeCent = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(2).
                                                                  symbol("D\u20e6").code("THT").build();
        assertEquals("cTHT", symbolCodeCent.code());
        assertEquals("¢D⃦", symbolCodeCent.symbol());
        ThtFixedFormat symbolCodeMilli = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(3).
                                                                   symbol("D\u20e6").code("THT").build();
        assertEquals("mTHT", symbolCodeMilli.code());
        assertEquals("₥D⃦", symbolCodeMilli.symbol());
        ThtFixedFormat symbolCodeMicro = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(6).
                                                                   symbol("D\u20e6").code("THT").build();
        assertEquals("µTHT", symbolCodeMicro.code());
        assertEquals("µD⃦", symbolCodeMicro.symbol());
        ThtFixedFormat symbolCodeDeka = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-1).
                                                                  symbol("D\u20e6").code("THT").build();
        assertEquals("daTHT", symbolCodeDeka.code());
        assertEquals("daD⃦", symbolCodeDeka.symbol());
        ThtFixedFormat symbolCodeHecto = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-2).
                                                                   symbol("D\u20e6").code("THT").build();
        assertEquals("hTHT", symbolCodeHecto.code());
        assertEquals("hD⃦", symbolCodeHecto.symbol());
        ThtFixedFormat symbolCodeKilo = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-3).
                                                                  symbol("D\u20e6").code("THT").build();
        assertEquals("kTHT", symbolCodeKilo.code());
        assertEquals("kD⃦", symbolCodeKilo.symbol());
        ThtFixedFormat symbolCodeMega = (ThtFixedFormat)ThtFormat.builder().locale(US).scale(-6).
                                                                  symbol("D\u20e6").code("THT").build();
        assertEquals("MTHT", symbolCodeMega.code());
        assertEquals("MD⃦", symbolCodeMega.symbol());
    }

    /* copied from CoinFormatTest.java and modified */
    @Test
    public void parse() throws Exception {
        ThtFormat coin = ThtFormat.getCoinInstance(Locale.US);
        assertEquals(Coin.COIN, coin.parseObject("1"));
        assertEquals(Coin.COIN, coin.parseObject("1."));
        assertEquals(Coin.COIN, coin.parseObject("1.0"));
        assertEquals(Coin.COIN, ThtFormat.getCoinInstance(Locale.GERMANY).parseObject("1,0"));
        assertEquals(Coin.COIN, coin.parseObject("01.0000000000"));
        // TODO work with express positive sign
        // assertEquals(Coin.COIN, coin.parseObject("+1.0"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1"));
        assertEquals(Coin.COIN.negate(), coin.parseObject("-1.0"));

        assertEquals(Coin.CENT, coin.parseObject(".01"));

        ThtFormat milli = ThtFormat.getMilliInstance(Locale.US);
        assertEquals(Coin.MILLICOIN, milli.parseObject("1"));
        assertEquals(Coin.MILLICOIN, milli.parseObject("1.0"));
        assertEquals(Coin.MILLICOIN, milli.parseObject("01.0000000000"));
        // TODO work with express positive sign
        //assertEquals(Coin.MILLICOIN, milli.parseObject("+1.0"));
        assertEquals(Coin.MILLICOIN.negate(), milli.parseObject("-1"));
        assertEquals(Coin.MILLICOIN.negate(), milli.parseObject("-1.0"));

        ThtFormat micro = ThtFormat.getMicroInstance(Locale.US);
        assertEquals(Coin.MICROCOIN, micro.parseObject("1"));
        assertEquals(Coin.MICROCOIN, micro.parseObject("1.0"));
        assertEquals(Coin.MICROCOIN, micro.parseObject("01.0000000000"));
        // TODO work with express positive sign
        // assertEquals(Coin.MICROCOIN, micro.parseObject("+1.0"));
        assertEquals(Coin.MICROCOIN.negate(), micro.parseObject("-1"));
        assertEquals(Coin.MICROCOIN.negate(), micro.parseObject("-1.0"));
    }

    /* Copied (and modified) from CoinFormatTest.java */
    @Test
    public void btcRounding() throws Exception {
        ThtFormat coinFormat = ThtFormat.getCoinInstance(Locale.US);
        assertEquals("0", ThtFormat.getCoinInstance(Locale.US, 0).format(ZERO));
        assertEquals("0", coinFormat.format(ZERO, 0));
        assertEquals("0.00", ThtFormat.getCoinInstance(Locale.US, 2).format(ZERO));
        assertEquals("0.00", coinFormat.format(ZERO, 2));

        assertEquals("1", ThtFormat.getCoinInstance(Locale.US, 0).format(COIN));
        assertEquals("1", coinFormat.format(COIN, 0));
        assertEquals("1.0", ThtFormat.getCoinInstance(Locale.US, 1).format(COIN));
        assertEquals("1.0", coinFormat.format(COIN, 1));
        assertEquals("1.00", ThtFormat.getCoinInstance(Locale.US, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2));
        assertEquals("1.00", ThtFormat.getCoinInstance(Locale.US, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2));
        assertEquals("1.00", ThtFormat.getCoinInstance(Locale.US, 2, 2, 2, 2).format(COIN));
        assertEquals("1.00", coinFormat.format(COIN, 2, 2, 2, 2));
        assertEquals("1.000", ThtFormat.getCoinInstance(Locale.US, 3).format(COIN));
        assertEquals("1.000", coinFormat.format(COIN, 3));
        assertEquals("1.0000", ThtFormat.getCoinInstance(US, 4).format(COIN));
        assertEquals("1.0000", coinFormat.format(COIN, 4));

        final Coin justNot = COIN.subtract(NOTION);
        assertEquals("1", ThtFormat.getCoinInstance(US, 0).format(justNot));
        assertEquals("1", coinFormat.format(justNot, 0));
        assertEquals("1.0", ThtFormat.getCoinInstance(US, 1).format(justNot));
        assertEquals("1.0", coinFormat.format(justNot, 1));
        final Coin justNotUnder = Coin.valueOf(99995000);
        assertEquals("1.00", ThtFormat.getCoinInstance(US, 2, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2, 2));
        assertEquals("1.00", ThtFormat.getCoinInstance(US, 2, 2).format(justNotUnder));
        assertEquals("1.00", coinFormat.format(justNotUnder, 2, 2));
        assertEquals("1.00", ThtFormat.getCoinInstance(US, 2, 2, 2).format(justNot));
        assertEquals("1.00", coinFormat.format(justNot, 2, 2, 2));
        assertEquals("0.999950", ThtFormat.getCoinInstance(US, 2, 2, 2).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, 2, 2));
        assertEquals("0.99999999", ThtFormat.getCoinInstance(US, 2, 2, 2, 2).format(justNot));
        assertEquals("0.99999999", coinFormat.format(justNot, 2, 2, 2, 2));
        assertEquals("0.99999999", ThtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNot));
        assertEquals("0.99999999", coinFormat.format(justNot, 2, REPEATING_DOUBLETS));
        assertEquals("0.999950", ThtFormat.getCoinInstance(US, 2, 2, 2, 2).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, 2, 2, 2));
        assertEquals("0.999950", ThtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(justNotUnder));
        assertEquals("0.999950", coinFormat.format(justNotUnder, 2, REPEATING_DOUBLETS));
        assertEquals("1.000", ThtFormat.getCoinInstance(US, 3).format(justNot));
        assertEquals("1.000", coinFormat.format(justNot, 3));
        assertEquals("1.0000", ThtFormat.getCoinInstance(US, 4).format(justNot));
        assertEquals("1.0000", coinFormat.format(justNot, 4));

        final Coin slightlyMore = COIN.add(NOTION);
        assertEquals("1", ThtFormat.getCoinInstance(US, 0).format(slightlyMore));
        assertEquals("1", coinFormat.format(slightlyMore, 0));
        assertEquals("1.0", ThtFormat.getCoinInstance(US, 1).format(slightlyMore));
        assertEquals("1.0", coinFormat.format(slightlyMore, 1));
        assertEquals("1.00", ThtFormat.getCoinInstance(US, 2, 2).format(slightlyMore));
        assertEquals("1.00", coinFormat.format(slightlyMore, 2, 2));
        assertEquals("1.00", ThtFormat.getCoinInstance(US, 2, 2, 2).format(slightlyMore));
        assertEquals("1.00", coinFormat.format(slightlyMore, 2, 2, 2));
        assertEquals("1.00000001", ThtFormat.getCoinInstance(US, 2, 2, 2, 2).format(slightlyMore));
        assertEquals("1.00000001", coinFormat.format(slightlyMore, 2, 2, 2, 2));
        assertEquals("1.00000001", ThtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(slightlyMore));
        assertEquals("1.00000001", coinFormat.format(slightlyMore, 2, REPEATING_DOUBLETS));
        assertEquals("1.000", ThtFormat.getCoinInstance(US, 3).format(slightlyMore));
        assertEquals("1.000", coinFormat.format(slightlyMore, 3));
        assertEquals("1.0000", ThtFormat.getCoinInstance(US, 4).format(slightlyMore));
        assertEquals("1.0000", coinFormat.format(slightlyMore, 4));

        final Coin pivot = COIN.add(NOTION.multiply(5));
        assertEquals("1.00000005", ThtFormat.getCoinInstance(US, 8).format(pivot));
        assertEquals("1.00000005", coinFormat.format(pivot, 8));
        assertEquals("1.00000005", ThtFormat.getCoinInstance(US, 7, 1).format(pivot));
        assertEquals("1.00000005", coinFormat.format(pivot, 7, 1));
        assertEquals("1.0000001", ThtFormat.getCoinInstance(US, 7).format(pivot));
        assertEquals("1.0000001", coinFormat.format(pivot, 7));

        final Coin value = Coin.valueOf(1122334455667788l);
        assertEquals("11,223,345", ThtFormat.getCoinInstance(US, 0).format(value));
        assertEquals("11,223,345", coinFormat.format(value, 0));
        assertEquals("11,223,344.6", ThtFormat.getCoinInstance(US, 1).format(value));
        assertEquals("11,223,344.6", coinFormat.format(value, 1));
        assertEquals("11,223,344.5567", ThtFormat.getCoinInstance(US, 2, 2).format(value));
        assertEquals("11,223,344.5567", coinFormat.format(value, 2, 2));
        assertEquals("11,223,344.556678", ThtFormat.getCoinInstance(US, 2, 2, 2).format(value));
        assertEquals("11,223,344.556678", coinFormat.format(value, 2, 2, 2));
        assertEquals("11,223,344.55667788", ThtFormat.getCoinInstance(US, 2, 2, 2, 2).format(value));
        assertEquals("11,223,344.55667788", coinFormat.format(value, 2, 2, 2, 2));
        assertEquals("11,223,344.55667788", ThtFormat.getCoinInstance(US, 2, REPEATING_DOUBLETS).format(value));
        assertEquals("11,223,344.55667788", coinFormat.format(value, 2, REPEATING_DOUBLETS));
        assertEquals("11,223,344.557", ThtFormat.getCoinInstance(US, 3).format(value));
        assertEquals("11,223,344.557", coinFormat.format(value, 3));
        assertEquals("11,223,344.5567", ThtFormat.getCoinInstance(US, 4).format(value));
        assertEquals("11,223,344.5567", coinFormat.format(value, 4));

        ThtFormat megaFormat = ThtFormat.getInstance(-6, US);
        assertEquals("22.00", megaFormat.format(MAX_MONEY));
        assertEquals("22", megaFormat.format(MAX_MONEY, 0));
        assertEquals("11.22334455667788", megaFormat.format(value, 0, REPEATING_DOUBLETS));
        assertEquals("11.223344556677", megaFormat.format(Coin.valueOf(1122334455667700l), 0, REPEATING_DOUBLETS));
        assertEquals("11.22334455667788", megaFormat.format(value, 0, REPEATING_TRIPLETS));
        assertEquals("11.223344556677", megaFormat.format(Coin.valueOf(1122334455667700l), 0, REPEATING_TRIPLETS));
    }

    @Test
    public void negativeTest() throws Exception {
        assertEquals("-1,00 THT", ThtFormat.getInstance(FRANCE).format(COIN.multiply(-1)));
        assertEquals("THT -1,00", ThtFormat.getInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("T -1,00", ThtFormat.getSymbolInstance(ITALY).format(COIN.multiply(-1)));
        assertEquals("THT -1.00", ThtFormat.getInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("T-1.00", ThtFormat.getSymbolInstance(JAPAN).format(COIN.multiply(-1)));
        assertEquals("(THT 1.00)", ThtFormat.getInstance(US).format(COIN.multiply(-1)));
        assertEquals("(T1.00)", ThtFormat.getSymbolInstance(US).format(COIN.multiply(-1)));
        // assertEquals("THT -१.००", ThtFormat.getInstance(Locale.forLanguageTag("hi-IN")).format(COIN.multiply(-1)));
        assertEquals("THT -๑.๐๐", ThtFormat.getInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
        assertEquals("T-๑.๐๐", ThtFormat.getSymbolInstance(new Locale("th","TH","TH")).format(COIN.multiply(-1)));
    }

    /* Warning: these tests assume the state of Locale data extant on the platform on which
     * they were written: openjdk 7u21-2.3.9-5 */
    @Test
    public void equalityTest() throws Exception {
        // First, autodenominator
        assertEquals(ThtFormat.getInstance(), ThtFormat.getInstance());
        assertEquals(ThtFormat.getInstance().hashCode(), ThtFormat.getInstance().hashCode());

        assertNotEquals(ThtFormat.getCodeInstance(), ThtFormat.getSymbolInstance());
        assertNotEquals(ThtFormat.getCodeInstance().hashCode(), ThtFormat.getSymbolInstance().hashCode());

        assertEquals(ThtFormat.getSymbolInstance(5), ThtFormat.getSymbolInstance(5));
        assertEquals(ThtFormat.getSymbolInstance(5).hashCode(), ThtFormat.getSymbolInstance(5).hashCode());

        assertNotEquals(ThtFormat.getSymbolInstance(5), ThtFormat.getSymbolInstance(4));
        assertNotEquals(ThtFormat.getSymbolInstance(5).hashCode(), ThtFormat.getSymbolInstance(4).hashCode());

        /* The underlying formatter is mutable, and its currency code
         * and symbol may be reset each time a number is
         * formatted or parsed.  Here we check to make sure that state is
         * ignored when comparing for equality */
        // when formatting
        ThtAutoFormat a = (ThtAutoFormat)ThtFormat.getSymbolInstance(US);
        ThtAutoFormat b = (ThtAutoFormat)ThtFormat.getSymbolInstance(US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        // when parsing
        a = (ThtAutoFormat)ThtFormat.getSymbolInstance(US);
        b = (ThtAutoFormat)ThtFormat.getSymbolInstance(US);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.parseObject("mTHT2");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.parseObject("µT4.35");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        // FRANCE and GERMANY have different pattterns
        assertNotEquals(ThtFormat.getInstance(FRANCE).hashCode(), ThtFormat.getInstance(GERMANY).hashCode());
        // TAIWAN and CHINA differ only in the Locale and Currency, i.e. the patterns and symbols are
        // all the same (after setting the currency symbols to bitcoins)
        assertNotEquals(ThtFormat.getInstance(TAIWAN), ThtFormat.getInstance(CHINA));
        // but they hash the same because of the DecimalFormatSymbols.hashCode() implementation

        assertEquals(ThtFormat.getSymbolInstance(4), ThtFormat.getSymbolInstance(4));
        assertEquals(ThtFormat.getSymbolInstance(4).hashCode(), ThtFormat.getSymbolInstance(4).hashCode());

        assertNotEquals(ThtFormat.getSymbolInstance(4), ThtFormat.getSymbolInstance(5));
        assertNotEquals(ThtFormat.getSymbolInstance(4).hashCode(), ThtFormat.getSymbolInstance(5).hashCode());

        // Fixed-denomination
        assertEquals(ThtFormat.getCoinInstance(), ThtFormat.getCoinInstance());
        assertEquals(ThtFormat.getCoinInstance().hashCode(), ThtFormat.getCoinInstance().hashCode());

        assertEquals(ThtFormat.getMilliInstance(), ThtFormat.getMilliInstance());
        assertEquals(ThtFormat.getMilliInstance().hashCode(), ThtFormat.getMilliInstance().hashCode());

        assertEquals(ThtFormat.getMicroInstance(), ThtFormat.getMicroInstance());
        assertEquals(ThtFormat.getMicroInstance().hashCode(), ThtFormat.getMicroInstance().hashCode());

        assertEquals(ThtFormat.getInstance(-6), ThtFormat.getInstance(-6));
        assertEquals(ThtFormat.getInstance(-6).hashCode(), ThtFormat.getInstance(-6).hashCode());

        assertNotEquals(ThtFormat.getCoinInstance(), ThtFormat.getMilliInstance());
        assertNotEquals(ThtFormat.getCoinInstance().hashCode(), ThtFormat.getMilliInstance().hashCode());

        assertNotEquals(ThtFormat.getCoinInstance(), ThtFormat.getMicroInstance());
        assertNotEquals(ThtFormat.getCoinInstance().hashCode(), ThtFormat.getMicroInstance().hashCode());

        assertNotEquals(ThtFormat.getMilliInstance(), ThtFormat.getMicroInstance());
        assertNotEquals(ThtFormat.getMilliInstance().hashCode(), ThtFormat.getMicroInstance().hashCode());

        assertNotEquals(ThtFormat.getInstance(SMALLEST_UNIT_EXPONENT),
                        ThtFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1));
        assertNotEquals(ThtFormat.getInstance(SMALLEST_UNIT_EXPONENT).hashCode(),
                        ThtFormat.getInstance(SMALLEST_UNIT_EXPONENT - 1).hashCode());

        assertNotEquals(ThtFormat.getCoinInstance(TAIWAN), ThtFormat.getCoinInstance(CHINA));

        assertNotEquals(ThtFormat.getCoinInstance(2,3), ThtFormat.getCoinInstance(2,4));
        assertNotEquals(ThtFormat.getCoinInstance(2,3).hashCode(), ThtFormat.getCoinInstance(2,4).hashCode());

        assertNotEquals(ThtFormat.getCoinInstance(2,3), ThtFormat.getCoinInstance(2,3,3));
        assertNotEquals(ThtFormat.getCoinInstance(2,3).hashCode(), ThtFormat.getCoinInstance(2,3,3).hashCode());


    }

    @Test
    public void attributeTest() throws Exception {
        String codePat = ThtFormat.getCodeInstance(Locale.US).pattern();
        assertTrue(codePat.contains("THT") && ! codePat.contains("(^|[^T])T([^T]|$)") && ! codePat.contains("(^|[^¤])¤([^¤]|$)"));
        String symPat = ThtFormat.getSymbolInstance(Locale.US).pattern();
        assertTrue(symPat.contains("T") && !symPat.contains("THT") && !symPat.contains("¤¤"));

        assertEquals("THT #,##0.00;(THT #,##0.00)", ThtFormat.getCodeInstance(Locale.US).pattern());
        assertEquals("T#,##0.00;(T#,##0.00)", ThtFormat.getSymbolInstance(Locale.US).pattern());
        assertEquals('0', ThtFormat.getInstance(Locale.US).symbols().getZeroDigit());
        // assertEquals('०', ThtFormat.getInstance(Locale.forLanguageTag("hi-IN")).symbols().getZeroDigit());
        // TODO will this next line work with other JREs?
        assertEquals('๐', ThtFormat.getInstance(new Locale("th","TH","TH")).symbols().getZeroDigit());
    }

    @Test
    public void toStringTest() {
        assertEquals("Auto-format T#,##0.00;(T#,##0.00)", ThtFormat.getSymbolInstance(Locale.US).toString());
        assertEquals("Auto-format T#,##0.0000;(T#,##0.0000)", ThtFormat.getSymbolInstance(Locale.US, 4).toString());
        assertEquals("Auto-format THT #,##0.00;(THT #,##0.00)", ThtFormat.getCodeInstance(Locale.US).toString());
        assertEquals("Auto-format THT #,##0.0000;(THT #,##0.0000)", ThtFormat.getCodeInstance(Locale.US, 4).toString());
        assertEquals("Coin-format #,##0.00", ThtFormat.getCoinInstance(Locale.US).toString());
        assertEquals("Millicoin-format #,##0.00", ThtFormat.getMilliInstance(Locale.US).toString());
        assertEquals("Microcoin-format #,##0.00", ThtFormat.getMicroInstance(Locale.US).toString());
        assertEquals("Coin-format #,##0.000", ThtFormat.getCoinInstance(Locale.US,3).toString());
        assertEquals("Coin-format #,##0.000(####)(#######)", ThtFormat.getCoinInstance(Locale.US,3,4,7).toString());
        assertEquals("Kilocoin-format #,##0.000", ThtFormat.getInstance(-3,Locale.US,3).toString());
        assertEquals("Kilocoin-format #,##0.000(####)(#######)", ThtFormat.getInstance(-3,Locale.US,3,4,7).toString());
        assertEquals("Decicoin-format #,##0.000", ThtFormat.getInstance(1,Locale.US,3).toString());
        assertEquals("Decicoin-format #,##0.000(####)(#######)", ThtFormat.getInstance(1,Locale.US,3,4,7).toString());
        assertEquals("Dekacoin-format #,##0.000", ThtFormat.getInstance(-1,Locale.US,3).toString());
        assertEquals("Dekacoin-format #,##0.000(####)(#######)", ThtFormat.getInstance(-1,Locale.US,3,4,7).toString());
        assertEquals("Hectocoin-format #,##0.000", ThtFormat.getInstance(-2,Locale.US,3).toString());
        assertEquals("Hectocoin-format #,##0.000(####)(#######)", ThtFormat.getInstance(-2,Locale.US,3,4,7).toString());
        assertEquals("Megacoin-format #,##0.000", ThtFormat.getInstance(-6,Locale.US,3).toString());
        assertEquals("Megacoin-format #,##0.000(####)(#######)", ThtFormat.getInstance(-6,Locale.US,3,4,7).toString());
        assertEquals("Fixed (-4) format #,##0.000", ThtFormat.getInstance(-4,Locale.US,3).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)", ThtFormat.getInstance(-4,Locale.US,3,4).toString());
        assertEquals("Fixed (-4) format #,##0.000(####)(#######)",
                     ThtFormat.getInstance(-4, Locale.US, 3, 4, 7).toString());

        assertEquals("Auto-format T#,##0.00;(T#,##0.00)",
                     ThtFormat.builder().style(SYMBOL).code("USD").locale(US).build().toString());
        assertEquals("Auto-format #.##0,00 $",
                     ThtFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build().toString());
        assertEquals("Auto-format #.##0,0000 $",
                     ThtFormat.builder().style(SYMBOL).symbol("$").fractionDigits(4).locale(GERMANY).build().toString());
        assertEquals("Auto-format THT#,00T;THT-#,00T",
                     ThtFormat.builder().style(SYMBOL).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Coin-format THT#,00T;THT-#,00T",
                     ThtFormat.builder().scale(0).locale(GERMANY).pattern("¤¤#¤").build().toString());
        assertEquals("Millicoin-format THT#.00T;THT-#.00T",
                     ThtFormat.builder().scale(3).locale(US).pattern("¤¤#¤").build().toString());
    }

    @Test
    public void patternDecimalPlaces() {
        /* The pattern format provided by DecimalFormat includes specification of fractional digits,
         * but we ignore that because we have alternative mechanism for specifying that.. */
        ThtFormat f = ThtFormat.builder().locale(US).scale(3).pattern("¤¤ #.0").fractionDigits(3).build();
        assertEquals("Millicoin-format THT #.000;THT -#.000", f.toString());
        assertEquals("mTHT 1000.000", f.format(COIN));
    }

    @Test
    public void builderTest() {
        Locale locale;
        if (Locale.getDefault().equals(GERMANY)) locale = FRANCE;
        else locale = GERMANY;

        assertEquals(ThtFormat.builder().build(), ThtFormat.getCoinInstance());
        try {
            ThtFormat.builder().scale(0).style(CODE);
            fail("Invoking both scale() and style() on a Builder should raise exception");
        } catch (IllegalStateException e) {}
        try {
            ThtFormat.builder().style(CODE).scale(0);
            fail("Invoking both style() and scale() on a Builder should raise exception");
        } catch (IllegalStateException e) {}

        ThtFormat built = ThtFormat.builder().style(ThtAutoFormat.Style.CODE).fractionDigits(4).build();
        assertEquals(built, ThtFormat.getCodeInstance(4));
        built = ThtFormat.builder().style(ThtAutoFormat.Style.SYMBOL).fractionDigits(4).build();
        assertEquals(built, ThtFormat.getSymbolInstance(4));

        built = ThtFormat.builder().scale(0).build();
        assertEquals(built, ThtFormat.getCoinInstance());
        built = ThtFormat.builder().scale(3).build();
        assertEquals(built, ThtFormat.getMilliInstance());
        built = ThtFormat.builder().scale(6).build();
        assertEquals(built, ThtFormat.getMicroInstance());

        built = ThtFormat.builder().locale(locale).scale(0).build();
        assertEquals(built, ThtFormat.getCoinInstance(locale));
        built = ThtFormat.builder().locale(locale).scale(3).build();
        assertEquals(built, ThtFormat.getMilliInstance(locale));
        built = ThtFormat.builder().locale(locale).scale(6).build();
        assertEquals(built, ThtFormat.getMicroInstance(locale));

        built = ThtFormat.builder().minimumFractionDigits(3).scale(0).build();
        assertEquals(built, ThtFormat.getCoinInstance(3));
        built = ThtFormat.builder().minimumFractionDigits(3).scale(3).build();
        assertEquals(built, ThtFormat.getMilliInstance(3));
        built = ThtFormat.builder().minimumFractionDigits(3).scale(6).build();
        assertEquals(built, ThtFormat.getMicroInstance(3));

        built = ThtFormat.builder().fractionGroups(3,4).scale(0).build();
        assertEquals(built, ThtFormat.getCoinInstance(2,3,4));
        built = ThtFormat.builder().fractionGroups(3,4).scale(3).build();
        assertEquals(built, ThtFormat.getMilliInstance(2,3,4));
        built = ThtFormat.builder().fractionGroups(3,4).scale(6).build();
        assertEquals(built, ThtFormat.getMicroInstance(2,3,4));

        built = ThtFormat.builder().pattern("#,####.#").scale(6).locale(GERMANY).build();
        assertEquals("100.0000,00", built.format(COIN));
        built = ThtFormat.builder().pattern("#,####.#").scale(6).locale(GERMANY).build();
        assertEquals("-100.0000,00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().localizedPattern("#.####,#").scale(6).locale(GERMANY).build();
        assertEquals("100.0000,00", built.format(COIN));

        built = ThtFormat.builder().pattern("¤#,####.#").style(CODE).locale(GERMANY).build();
        assertEquals("T-1,00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("¤¤ #,####.#").style(SYMBOL).locale(GERMANY).build();
        assertEquals("THT -1,00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("¤¤##,###.#").scale(3).locale(US).build();
        assertEquals("mTHT1,000.00", built.format(COIN));
        built = ThtFormat.builder().pattern("¤ ##,###.#").scale(3).locale(US).build();
        assertEquals("₥T 1,000.00", built.format(COIN));

        try {
            ThtFormat.builder().pattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        try {
            ThtFormat.builder().localizedPattern("¤¤##,###.#").scale(4).locale(US).build().format(COIN);
            fail("Localized pattern with currency sign and non-standard denomination should raise exception");
        } catch (IllegalStateException e) {}

        built = ThtFormat.builder().style(SYMBOL).symbol("D\u20e6").locale(US).build();
        assertEquals("D⃦1.00", built.format(COIN));
        built = ThtFormat.builder().style(CODE).code("THT").locale(US).build();
        assertEquals("THT 1.00", built.format(COIN));
        built = ThtFormat.builder().style(SYMBOL).symbol("$").locale(GERMANY).build();
        assertEquals("1,00 $", built.format(COIN));
        // Setting the currency code on a DecimalFormatSymbols object can affect the currency symbol.
        built = ThtFormat.builder().style(SYMBOL).code("USD").locale(US).build();
        assertEquals("T1.00", built.format(COIN));

        built = ThtFormat.builder().style(SYMBOL).symbol("D\u20e6").locale(US).build();
        assertEquals("₥D⃦1.00", built.format(COIN.divide(1000)));
        built = ThtFormat.builder().style(CODE).code("THT").locale(US).build();
        assertEquals("mTHT 1.00", built.format(COIN.divide(1000)));

        built = ThtFormat.builder().style(SYMBOL).symbol("D\u20e6").locale(US).build();
        assertEquals("µD⃦1.00", built.format(valueOf(100)));
        built = ThtFormat.builder().style(CODE).code("THT").locale(US).build();
        assertEquals("µTHT 1.00", built.format(valueOf(100)));

        /* The prefix of a pattern can have number symbols in quotes.
         * Make sure our custom negative-subpattern creator handles this. */
        built = ThtFormat.builder().pattern("'#'¤#0").scale(0).locale(US).build();
        assertEquals("#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("'#0'¤#0").scale(0).locale(US).build();
        assertEquals("#0T-1.00", built.format(COIN.multiply(-1)));
        // this is an escaped quote between two hash marks in one set of quotes, not
        // two adjacent quote-enclosed hash-marks:
        built = ThtFormat.builder().pattern("'#''#'¤#0").scale(0).locale(US).build();
        assertEquals("#'#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("#0'#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("#0#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("'#0'E'#'¤#0").scale(0).locale(US).build();
        assertEquals("#0E#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("E'#0''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0'#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("E'#0#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("E'#0''''#'¤#0").scale(0).locale(US).build();
        assertEquals("E#0''#T-1.00", built.format(COIN.multiply(-1)));
        built = ThtFormat.builder().pattern("''#0").scale(0).locale(US).build();
        assertEquals("'-1.00", built.format(COIN.multiply(-1)));

        // immutability check for fixed-denomination formatters, w/ & w/o custom pattern
        ThtFormat a = ThtFormat.builder().scale(3).build();
        ThtFormat b = ThtFormat.builder().scale(3).build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a = ThtFormat.builder().scale(3).pattern("¤#.#").build();
        b = ThtFormat.builder().scale(3).pattern("¤#.#").build();
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        a.format(COIN.multiply(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        b.format(COIN.divide(1000000));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

    }

}
