package io.github.polestarcn.oppobridge;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
/** Standard system entry, independent of MyDevices discovery. Never sends a command. */
public final class VehicleControlTileService extends TileService {
  @Override public void onStartListening() {
    Tile tile=getQsTile();if(tile!=null){tile.setLabel("极星车控");tile.setState(Tile.STATE_INACTIVE);tile.updateTile();}
  }
  @Override public void onClick() {
    unlockAndRun(() -> {
      Intent intent=new Intent(this,VehicleCardDetailActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if(Build.VERSION.SDK_INT>=34)startActivityAndCollapse(PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
      else startActivityAndCollapse(intent);
    });
  }
}
