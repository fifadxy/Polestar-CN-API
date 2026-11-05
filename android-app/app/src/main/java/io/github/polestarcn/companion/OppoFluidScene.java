package io.github.polestarcn.companion;

/** Immutable single notification/card scene; synthetic test events cannot command the car. */
public final class OppoFluidScene {
  public final String id,title,detail,chip,firstAction,secondAction;
  public final long endsAt;
  public final boolean ongoing,synthetic;
  public OppoFluidScene(String id,String title,String detail,String chip,boolean ongoing,long endsAt,
      String firstAction,String secondAction,boolean synthetic){
    this.id=id;this.title=title;this.detail=detail;this.chip=chip;this.ongoing=ongoing;
    this.endsAt=endsAt;this.firstAction=firstAction;this.secondAction=secondAction;this.synthetic=synthetic;
  }
}
