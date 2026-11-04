package io.github.polestarcn.oppobridge;
import static org.junit.Assert.*;
import android.os.Bundle;
import io.github.polestarcn.bridge.CarLinkBridgeContract;
import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;
@RunWith(RobolectricTestRunner.class)
public class Beta38ProtocolClientTest {
 @Test public void modernModuleSharesContractAndCreatesUniqueTickets(){Bundle a=CarLinkProtocolClient.request("unlock"),b=CarLinkProtocolClient.request("unlock");assertEquals("unlock",a.getString(CarLinkBridgeContract.COMMAND));assertNotEquals(a.getString(CarLinkBridgeContract.REQUEST),b.getString(CarLinkBridgeContract.REQUEST));assertEquals(1,CarLinkBridgeContract.VERSION);assertEquals(1001,CarLinkBridgeContract.LOCK);}
 @Test public void onlyExplicitCoreVocabularyUsesNewProtocol(){assertTrue(CarLinkProtocolClient.supports("climate-stop"));assertTrue(CarLinkProtocolClient.supports("windows-open"));assertFalse(CarLinkProtocolClient.supports("unlock-everything"));assertFalse(CarLinkProtocolClient.supports("trunk-open"));}
}
