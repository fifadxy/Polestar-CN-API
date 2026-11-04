package io.github.polestarcn.companion;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
/** Static DEX declarations only: no field values, strings dump, method bodies or account databases. */
final class CarLinkDexContract {
  static final int MAX_TEXT=160_000,MAX_DEX=20_000_000;
  static String scan(File apk)throws IOException{
    StringBuilder out=new StringBuilder();try(ZipFile z=new ZipFile(apk)){
      Enumeration<? extends ZipEntry> entries=z.entries();int count=0;
      while(entries.hasMoreElements()&&count<12&&out.length()<MAX_TEXT){ZipEntry e=entries.nextElement();if(!e.getName().matches("classes[0-9]*\\.dex"))continue;count++;
        if(e.getSize()<112||e.getSize()>MAX_DEX){out.append("DEX size limit\n");continue;}
        byte[] b;try(InputStream in=z.getInputStream(e);ByteArrayOutputStream buf=new ByteArrayOutputStream()){
          byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(buf.size()+n>MAX_DEX)throw new IOException("DEX too large");buf.write(block,0,n);}b=buf.toByteArray();}
        out.append("DEX ").append(e.getName()).append('\n');try{declarations(b,out);}catch(RuntimeException malformed){out.append("Malformed DEX declarations skipped\n");}
      }
    }return out.substring(0,Math.min(out.length(),MAX_TEXT));
  }
  static boolean relevant(String name){String n=name.toLowerCase(Locale.ROOT);return (n.startsWith("lcom/heytap/opluscarlink/")||n.startsWith("lcom/oplus/carlink/")||n.startsWith("lcom/oplus/ocs/carlink/"))&&(n.contains("control")||n.contains("service")||n.contains("sdk")||n.contains("bind")||n.contains("carapp")||n.contains("callback")||n.contains("model"));}
  private static void declarations(byte[] b,StringBuilder out){
    if(b.length<112||b[0]!='d'||b[1]!='e'||b[2]!='x')throw new IllegalArgumentException();
    int ns=u32(b,56),ss=u32(b,60),nt=u32(b,64),ts=u32(b,68),np=u32(b,72),ps=u32(b,76),nm=u32(b,88),ms=u32(b,92),nc=u32(b,96),cs=u32(b,100);
    bounds(b,ss,ns,4);bounds(b,ts,nt,4);bounds(b,ps,np,12);bounds(b,ms,nm,8);bounds(b,cs,nc,32);
    Map<Integer,String> types=new HashMap<>();Set<Integer> selected=new HashSet<>();
    for(int i=0;i<nc&&selected.size()<192;i++){int type=u32(b,cs+i*32);String name=type(b,type,ss,ns,ts,nt,types);if(relevant(name)){selected.add(type);out.append("CLASS ").append(name).append('\n');if(out.length()>=MAX_TEXT)return;}}
    for(int i=0;i<nm&&out.length()<MAX_TEXT;i++){int at=ms+i*8,owner=u16(b,at);if(!selected.contains(owner))continue;
      int proto=u16(b,at+2);if(proto>=np)continue;String name=str(b,u32(b,at+4),ss,ns);if(!name.matches("[A-Za-z0-9_$<>-]{1,150}"))continue;
      int pa=ps+proto*12;String result=type(b,u32(b,pa+4),ss,ns,ts,nt,types);int list=u32(b,pa+8);
      out.append(type(b,owner,ss,ns,ts,nt,types)).append("->").append(name).append('(');
      if(list!=0){int n=u32(b,list);if(n>32)throw new IllegalArgumentException();bounds(b,list+4,n,2);for(int j=0;j<n;j++)out.append(type(b,u16(b,list+4+j*2),ss,ns,ts,nt,types));}
      out.append(')').append(result).append('\n');
    }
  }
  private static String type(byte[] b,int idx,int ss,int ns,int ts,int nt,Map<Integer,String> cache){if(idx<0||idx>=nt)throw new IllegalArgumentException();String v=cache.get(idx);if(v==null){v=str(b,u32(b,ts+idx*4),ss,ns);if(!v.matches("[A-Za-z0-9_$/;\\[.-]{1,512}"))throw new IllegalArgumentException();cache.put(idx,v);}return v;}
  private static String str(byte[] b,int idx,int off,int count){if(idx<0||idx>=count)throw new IllegalArgumentException();int at=u32(b,off+idx*4);for(int i=0;i<5;i++){if(at>=b.length)throw new IllegalArgumentException();if((b[at++]&128)==0)break;if(i==4)throw new IllegalArgumentException();}int end=at;while(end<b.length&&end-at<2048&&b[end]!=0)end++;if(end>=b.length||end-at>=2048)throw new IllegalArgumentException();return new String(b,at,end-at,StandardCharsets.UTF_8);}
  private static int u16(byte[] b,int at){if(at<0||at>b.length-2)throw new IllegalArgumentException();return (b[at]&255)|((b[at+1]&255)<<8);}
  private static int u32(byte[] b,int at){if(at<0||at>b.length-4)throw new IllegalArgumentException();int n=u16(b,at)|(u16(b,at+2)<<16);if(n<0)throw new IllegalArgumentException();return n;}
  private static void bounds(byte[] b,int off,int count,int width){if(off<0||count<0||(long)off+(long)count*width>b.length)throw new IllegalArgumentException();}
}
