package io.github.polestarcn.companion;

import android.content.Context;
import android.widget.ImageView;

/** Static Polestar 2 front three-quarter image; original proportions fit within every viewport. */
final class VehicleHeroView extends ImageView {
  VehicleHeroView(Context context) {
    super(context);
    setImageResource(R.drawable.vehicle_hero);
    setScaleType(ScaleType.FIT_CENTER);
    setAdjustViewBounds(false);
  }
}
