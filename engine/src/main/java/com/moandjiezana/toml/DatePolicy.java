package com.moandjiezana.toml;


class DatePolicy {

  private final FaketimeZone timeZone;
  private final boolean showFractionalSeconds;
  
  DatePolicy(FaketimeZone timeZone, boolean showFractionalSeconds) {
    this.timeZone = timeZone;
    this.showFractionalSeconds = showFractionalSeconds;
  }

  FaketimeZone getTimeZone() {
    return timeZone;
  }

  boolean isShowFractionalSeconds() {
    return showFractionalSeconds;
  }
}
