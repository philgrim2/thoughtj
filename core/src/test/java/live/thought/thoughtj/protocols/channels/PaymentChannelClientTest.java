/*
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

package live.thought.thoughtj.protocols.channels;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.spongycastle.crypto.params.KeyParameter;

import live.thought.thoughtj.core.*;
import live.thought.thoughtj.paymentchannel.Protos;
import live.thought.thoughtj.protocols.channels.IPaymentChannelClient;
import live.thought.thoughtj.protocols.channels.PaymentChannelClient;
import live.thought.thoughtj.wallet.Wallet;
import live.thought.thoughtj.wallet.WalletExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import static live.thought.thoughtj.paymentchannel.Protos.TwoWayChannelMessage;
import static live.thought.thoughtj.paymentchannel.Protos.TwoWayChannelMessage.MessageType.*;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class PaymentChannelClientTest {

    private Wallet wallet;
    private ECKey ecKey;
    private Sha256Hash serverHash;
    private IPaymentChannelClient.ClientConnection connection;
    public Coin maxValue;
    public Capture<TwoWayChannelMessage> clientVersionCapture;
    public int defaultTimeWindow = 86340;

    /**
     * We use parameterized tests to run the client channel tests with each
     * version of the channel.
     */
    @Parameterized.Parameters(name = "{index}: PaymentChannelClientTest({0})")
    public static Collection<PaymentChannelClient.VersionSelector> data() {
        return Arrays.asList(
                PaymentChannelClient.VersionSelector.VERSION_1,
                PaymentChannelClient.VersionSelector.VERSION_2_ALLOW_1,
                PaymentChannelClient.VersionSelector.VERSION_2
        );
    }

    @Parameterized.Parameter
    public PaymentChannelClient.VersionSelector versionSelector;

    @Before
    public void before() {
        wallet = createMock(Wallet.class);
        ecKey = createMock(ECKey.class);
        maxValue = Coin.COIN;
        serverHash = Sha256Hash.of("serverId".getBytes());
        connection = createMock(IPaymentChannelClient.ClientConnection.class);
        clientVersionCapture = new Capture<TwoWayChannelMessage>();
    }

    @Test
    public void shouldSendClientVersionOnChannelOpen() throws Exception {
        PaymentChannelClient dut = new PaymentChannelClient(wallet, ecKey, maxValue, serverHash, connection, versionSelector);
        connection.sendToServer(capture(clientVersionCapture));
        EasyMock.expect(wallet.getExtensions()).andReturn(new HashMap<String, WalletExtension>());
        replay(connection, wallet);
        dut.connectionOpen();
        assertClientVersion(defaultTimeWindow);
    }
    @Test
    public void shouldSendTimeWindowInClientVersion() throws Exception {
        long timeWindow = 4000;
        KeyParameter userKey = null;
        PaymentChannelClient dut =
                new PaymentChannelClient(wallet, ecKey, maxValue, serverHash, timeWindow, userKey, connection, versionSelector);
        connection.sendToServer(capture(clientVersionCapture));
        EasyMock.expect(wallet.getExtensions()).andReturn(new HashMap<String, WalletExtension>());
        replay(connection, wallet);
        dut.connectionOpen();
        assertClientVersion(4000);
    }

    private void assertClientVersion(long expectedTimeWindow) {
        final TwoWayChannelMessage response = clientVersionCapture.getValue();
        final TwoWayChannelMessage.MessageType type = response.getType();
        assertEquals("Wrong type " + type, CLIENT_VERSION, type);
        final Protos.ClientVersion clientVersion = response.getClientVersion();
        final int major = clientVersion.getMajor();
        final int requestedVersion = versionSelector.getRequestedMajorVersion();
        assertEquals("Wrong major version " + major, requestedVersion, major);
        final long actualTimeWindow = clientVersion.getTimeWindowSecs();
        assertEquals("Wrong timeWindow " + actualTimeWindow, expectedTimeWindow, actualTimeWindow );
    }
}
