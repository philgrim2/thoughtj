/*
 * Copyright 2014 Piotr Włodarek
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

package live.thought.thoughtj.core;

import org.junit.Test;

import live.thought.thoughtj.core.Message;
import live.thought.thoughtj.core.NetworkParameters;
import live.thought.thoughtj.core.ProtocolException;
import live.thought.thoughtj.core.VarInt;
import live.thought.thoughtj.params.UnitTestParams;

public class MessageTest {

    // If readStr() is vulnerable this causes OutOfMemory
    @Test(expected = ProtocolException.class)
    public void readStrOfExtremeLength() throws Exception {
        NetworkParameters params = UnitTestParams.get();
        VarInt length = new VarInt(Integer.MAX_VALUE);
        byte[] payload = length.encode();
        new VarStrMessage(params, payload);
    }

    static class VarStrMessage extends Message {
        public VarStrMessage(NetworkParameters params, byte[] payload) {
            super(params, payload, 0);
        }

        @Override
        protected void parse() throws ProtocolException {
            readStr();
        }
    }

    // If readBytes() is vulnerable this causes OutOfMemory
    @Test(expected = ProtocolException.class)
    public void readByteArrayOfExtremeLength() throws Exception {
        NetworkParameters params = UnitTestParams.get();
        VarInt length = new VarInt(Integer.MAX_VALUE);
        byte[] payload = length.encode();
        new VarBytesMessage(params, payload);
    }

    static class VarBytesMessage extends Message {
        public VarBytesMessage(NetworkParameters params, byte[] payload) {
            super(params, payload, 0);
        }

        @Override
        protected void parse() throws ProtocolException {
            readByteArray();
        }
    }
}
