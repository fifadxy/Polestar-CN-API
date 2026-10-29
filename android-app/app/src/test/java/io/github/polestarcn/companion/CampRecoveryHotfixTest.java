package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public final class CampRecoveryHotfixTest {
 @Test public void offlineDoesNotInvalidateLogin(){
   assertEquals("WAITING_FOR_NETWORK",CampRecovery.classify(new java.net.SocketTimeoutException("Read timed out")));
   assertEquals("WAITING_FOR_NETWORK",CampRecovery.classify(new Exception("UNAVAILABLE")));
 }
 @Test public void rejectedRefreshAsksForInteractiveLogin(){
   assertEquals("LOGIN_REQUIRED",CampRecovery.classify(new Exception("HTTP 401")));
   assertEquals("LOGIN_REQUIRED",CampRecovery.classify(new Exception("刷新令牌被服务端拒绝")));
 }
}
