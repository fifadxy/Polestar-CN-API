package io.github.polestarcn.companion;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.*;
import org.junit.Test;
public class CarLinkDexContractTest {
  @Test public void exportsOnlyDefinedClassMethodsAndNeverArbitraryStrings()throws Exception{
    byte[] bytes=new byte[2048];ByteBuffer b=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);b.put("dex\n035\0".getBytes(StandardCharsets.US_ASCII));
    int ss=112,ts=136,ps=148,ms=160,cs=168,list=200,data=208;
    b.putInt(56,6).putInt(60,ss).putInt(64,3).putInt(68,ts).putInt(72,1).putInt(76,ps).putInt(88,1).putInt(92,ms).putInt(96,1).putInt(100,cs);
    String[] values={"Lcom/heytap/opluscarlink/server/ControlService;","onBind","Landroid/os/IBinder;","Landroid/content/Intent;","PRIVATE_TOKEN_VIN_DO_NOT_EXPORT","LL"};
    for(int i=0;i<values.length;i++){byte[] v=values[i].getBytes(StandardCharsets.UTF_8);b.putInt(ss+4*i,data);bytes[data++]=(byte)v.length;System.arraycopy(v,0,bytes,data,v.length);data+=v.length;bytes[data++]=0;}
    b.putInt(ts,0).putInt(ts+4,2).putInt(ts+8,3);b.putInt(ps,5).putInt(ps+4,1).putInt(ps+8,list);b.putShort(ms,(short)0).putShort(ms+2,(short)0).putInt(ms+4,1);b.putInt(cs,0).putInt(list,1).putShort(list+4,(short)2);
    File file=Files.createTempFile("carlink-contract-test",".apk").toFile();
    try{try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(file))){z.putNextEntry(new ZipEntry("classes.dex"));z.write(bytes);z.closeEntry();}
      String result=CarLinkDexContract.scan(file);assertTrue(result.contains("ControlService;->onBind(Landroid/content/Intent;)Landroid/os/IBinder;"));assertFalse(result.contains("PRIVATE_TOKEN_VIN"));
    }finally{file.delete();}
  }
}
