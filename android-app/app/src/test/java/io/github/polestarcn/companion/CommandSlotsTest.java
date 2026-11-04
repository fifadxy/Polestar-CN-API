package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import org.junit.Test;
public final class CommandSlotsTest {
 @Test public void sameControlFamilyIsMutuallyExclusive(){assertEquals("lock",CommandSlots.group("lock"));assertEquals("lock",CommandSlots.group("unlock"));assertEquals("climate",CommandSlots.group("climate-stop"));assertEquals("climate",CommandSlots.group("climate-start"));assertEquals("trunk",CommandSlots.group("trunk-close"));}
 @Test public void unrelatedCommandsUseDifferentFamilies(){assertNotEquals(CommandSlots.group("air-start"),CommandSlots.group("climate-start"));assertNotEquals(CommandSlots.group("lock"),CommandSlots.group("climate-start"));assertNotEquals(CommandSlots.group("windows-open"),CommandSlots.group("trunk-open"));}
 @Test public void allSignalActionsRemainOneFamily(){assertEquals("signal",CommandSlots.group("honk"));assertEquals("signal",CommandSlots.group("flash"));}
}
