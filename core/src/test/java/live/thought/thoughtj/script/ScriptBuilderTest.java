/*
 * Copyright 2017 Nicola Atzei
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

package live.thought.thoughtj.script;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import live.thought.thoughtj.script.Script;
import live.thought.thoughtj.script.ScriptBuilder;
import live.thought.thoughtj.script.ScriptChunk;

public class ScriptBuilderTest {

    @Test
    public void testNumber() {
        for (int i = -100; i <= 100; i++) {
            Script s = new ScriptBuilder().number(i).build();
            for (ScriptChunk ch : s.chunks) {
                assertTrue(Integer.toString(i), ch.isShortestPossiblePushData());
            }
        }
    }
}
