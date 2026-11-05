package io.github.polestarcn.oppobridge;
import java.nio.charset.StandardCharsets;
public final class TestFeature {
 public static void main(String[] args){
   String fixture="const eventSummary = ['sportData', 'healthData'];\n"
    +"/**/ var _default = exports.default = {\n"
    +"carEventIconName: _config.carEventIconName,\n"
    +"buttonAction: function(item) { clickEvent(this.deviceInfo.id, item.id, Number(!item.state)); },\n"
    +"children: [], enabled: true, name: 'SingleDeviceCard4x2'\n"
    +"/***/ }),\n";
   byte[] original=fixture.getBytes(StandardCharsets.UTF_8);
   byte[] output=QuickAppCarCapability.patchFeature(original);
   if(output==null)throw new AssertionError("NEW RPK feature patch rejected validated fixture");
   String done=new String(output,StandardCharsets.UTF_8);
   if(!done.contains("'chesuo', 'kongtiao', 'shanguang', 'mingdi'"))throw new AssertionError("CAR IDs missing");
   if(!done.contains("var clickEvent = __webpack_require__"))throw new AssertionError("clickEvent binding missing");
   if(QuickAppCarCapability.patchFeature(output)!=null)throw new AssertionError("Repeated patch allowed");
   if(QuickAppCarCapability.patchFeature(new byte[]{1,2,3})!=null)throw new AssertionError("Arbitrary bytes accepted");
   System.out.println("Beta46 RPK feature patch: PASS; bytes="+original.length+" -> "+output.length);
 }
}
